package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import java.util.List;

import net.fahr3n.unnecessarilycompressedcobblestone.potion.ModMobEffects;
import net.fahr3n.unnecessarilycompressedcobblestone.potion.ModPotions;
import net.fahr3n.unnecessarilycompressedcobblestone.util.BossTargeting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Compressed Witch. A witch that has read the whole brewing book: every half second it either
 * drinks something that makes it harder to kill or throws something that makes whoever it is
 * fighting easier to kill, and when it has collected too much of the second kind itself, it drinks
 * milk.
 * <p>
 * Nothing about the fight is a damage race. It has five thousand health behind twenty-five armour,
 * and while it keeps its own buffs up it heals and shrugs; what a player can actually do is bury it
 * in the same debuffs it throws, faster than {@link #CLEANSE_THRESHOLD} lets it clear them, and take
 * the seconds either side of a cleanse - the moment after it drinks the milk is the moment it has
 * nothing on.
 * <p>
 * It picks by what is <em>missing</em> rather than at random, which is what makes the pattern
 * readable: the first seconds of the fight are it stacking every buff it owns, and after that it is
 * throwing, until something knocks a buff off and it stops to replace it.
 */
public class CompressedWitchEntity extends Witch {
    public static final float MAX_HEALTH = 5000.0F;

    /** Rather more plate than netherite, and a little more toughness than netherite's twelve. */
    public static final double ARMOR = 25.0;
    public static final double ARMOR_TOUGHNESS = 14.0;

    /** One action every half second: a drink, a throw or a cleanse. */
    private static final int ACTION_INTERVAL = 10;

    /** A minute of everything it drinks. */
    private static final int BUFF_DURATION = 60 * 20;

    /** Ten seconds of everything it throws. */
    private static final int DEBUFF_DURATION = 10 * 20;

    /** How many harmful effects it will carry before it stops to drink milk. */
    private static final int CLEANSE_THRESHOLD = 3;

    /** How far it looks for something to fight, and how far it will throw. */
    private static final double SEARCH_RADIUS = 32.0;

    /** Ticks between searches when it has nothing to fight; a search walks every entity in reach. */
    private static final int SEARCH_INTERVAL = 20;

    private static final String TAG_ACTION_COOLDOWN = "action_cooldown";

    /**
     * What it drinks, in the order it prefers them. Every one is a potion effect a player could brew
     * for themselves, at a level a player could not.
     */
    private static final List<BuffChoice> BUFFS = List.of(
            new BuffChoice(MobEffects.REGENERATION, 2),
            new BuffChoice(MobEffects.DAMAGE_RESISTANCE, 2),
            new BuffChoice(MobEffects.ABSORPTION, 3),
            new BuffChoice(MobEffects.MOVEMENT_SPEED, 1),
            new BuffChoice(MobEffects.FIRE_RESISTANCE, 0),
            new BuffChoice(MobEffects.DAMAGE_BOOST, 1));

    /**
     * What it throws. The mod's own brews are in here alongside vanilla's - a witch that has read
     * the whole book has read this mod's pages too - with one deliberate omission: Detonation breaks
     * ground, and a boss that craters its own arena twice a second is not a fight, it is a hole.
     */
    private static final List<BuffChoice> DEBUFFS = List.of(
            new BuffChoice(MobEffects.WEAKNESS, 1),
            new BuffChoice(MobEffects.MOVEMENT_SLOWDOWN, 2),
            new BuffChoice(MobEffects.POISON, 1),
            new BuffChoice(MobEffects.BLINDNESS, 0),
            new BuffChoice(MobEffects.WITHER, 1),
            new BuffChoice(ModMobEffects.FREEZING, 0),
            new BuffChoice(ModMobEffects.COLLAPSE, 0),
            new BuffChoice(ModMobEffects.STORMSTRUCK, 0),
            new BuffChoice(ModMobEffects.DRAGON_BREATH, 0));

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.unnecessarilycompressedcobblestone.compressed_witch"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);

    private int actionCooldown = ACTION_INTERVAL;
    private int searchCooldown;

    public CompressedWitchEntity(EntityType<? extends Witch> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 500;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Witch.createAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ARMOR, ARMOR)
                .add(Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, SEARCH_RADIUS);
    }

    /** A boss has no business wandering off to join somebody's village raid. */
    @Override
    public boolean canJoinRaid() {
        return false;
    }

    /**
     * Vanilla's witch throws a potion on its own sixty-tick clock. This one throws on its own, so
     * the goal is left in place for the movement it drives - it keeps the witch at a throwing
     * distance and strafing - and the attack itself is taken away from it.
     */
    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LivingEntity target = getTarget();
        if (target == null || !BossTargeting.isValid(this, target, SEARCH_RADIUS, this::isOwnKind)) {
            setTarget(null);
            if (this.searchCooldown-- <= 0) {
                this.searchCooldown = SEARCH_INTERVAL;
                setTarget(BossTargeting.choose(serverLevel, this, SEARCH_RADIUS, this::isOwnKind));
            }
        }

        if (this.actionCooldown-- > 0) {
            return;
        }

        this.actionCooldown = ACTION_INTERVAL;
        act(serverLevel, getTarget());
    }

    /**
     * One action. The order is the whole of its behaviour: clear itself if it is smothered, put back
     * whatever buff it is missing, and otherwise throw whatever the target has not got yet.
     */
    private void act(ServerLevel level, LivingEntity target) {
        if (harmfulEffects() >= CLEANSE_THRESHOLD) {
            cleanse();
            return;
        }

        for (BuffChoice buff : BUFFS) {
            if (!hasAtLeast(buff)) {
                drink(buff);
                return;
            }
        }

        if (target == null) {
            return;
        }

        for (BuffChoice debuff : DEBUFFS) {
            if (!target.hasEffect(debuff.effect())) {
                throwAt(level, target, debuff);
                return;
            }
        }

        // Nothing left it has not already got: fall back on the one thing that always lands.
        throwHarming(level, target);
    }

    /**
     * Drinks a potion of {@code buff}.
     * <p>
     * The drink is handed to the witch's own machinery rather than applied here: putting the bottle
     * in its hand and setting the using flag is what {@code Witch.aiStep} finishes off, applying
     * whatever {@link PotionContents} the bottle carries and emptying the hand. That machinery
     * counts down a private {@code usingTime} this cannot reach, which has one visible consequence -
     * the sip finishes on the next tick rather than after vanilla's thirty-two - and one useful one:
     * the animation, the game event and the effects all come from vanilla and cannot drift from it.
     */
    private void drink(BuffChoice buff) {
        setItemSlot(EquipmentSlot.MAINHAND, potion(new MobEffectInstance(
                buff.effect(), BUFF_DURATION, buff.amplifier())));
        setUsingItem(true);
        playSound(SoundEvents.WITCH_DRINK, 1.0F, 0.8F + this.random.nextFloat() * 0.4F);
    }

    /**
     * Drinks the milk. The clearing is done here rather than by the potion, because what makes that
     * potion clear anything is the item-use event a player goes through and a mob does not - see
     * {@code ModPotions.cleanse}, which is the one place the rule lives.
     */
    private void cleanse() {
        setItemSlot(EquipmentSlot.MAINHAND, PotionContents.createItemStack(Items.POTION, ModPotions.CLEANSING));
        setUsingItem(true);
        ModPotions.cleanse(this);
        playSound(SoundEvents.WITCH_DRINK, 1.0F, 0.6F);
    }

    /** Lobs a lingering potion of {@code debuff} at the target, the way a witch throws anything. */
    private void throwAt(ServerLevel level, LivingEntity target, BuffChoice debuff) {
        throwPotion(level, target, potion(Items.LINGERING_POTION,
                new MobEffectInstance(debuff.effect(), DEBUFF_DURATION, debuff.amplifier())));
    }

    /** The fallback: instant damage, which needs nothing to be missing to be worth throwing. */
    private void throwHarming(ServerLevel level, LivingEntity target) {
        throwPotion(level, target, PotionContents.createItemStack(Items.SPLASH_POTION,
                net.minecraft.world.item.alchemy.Potions.STRONG_HARMING));
    }

    /** Vanilla's own throwing arc, lifted off {@code Witch.performRangedAttack}. */
    private void throwPotion(ServerLevel level, LivingEntity target, ItemStack potion) {
        Vec3 lead = target.getDeltaMovement();
        double dx = target.getX() + lead.x - getX();
        double dy = target.getEyeY() - 1.1F - getY();
        double dz = target.getZ() + lead.z - getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);

        ThrownPotion thrown = new ThrownPotion(level, this);
        thrown.setItem(potion);
        thrown.setXRot(thrown.getXRot() - -20.0F);
        thrown.shoot(dx, dy + flat * 0.2, dz, 0.75F, 8.0F);
        level.addFreshEntity(thrown);

        playSound(SoundEvents.WITCH_THROW, 1.0F, 0.8F + this.random.nextFloat() * 0.4F);
    }

    /** A drinkable bottle carrying exactly these effects and nothing else. */
    private static ItemStack potion(MobEffectInstance... effects) {
        return potion(Items.POTION, effects);
    }

    private static ItemStack potion(net.minecraft.world.item.Item bottle, MobEffectInstance... effects) {
        ItemStack stack = new ItemStack(bottle);
        stack.set(DataComponents.POTION_CONTENTS,
                new PotionContents(java.util.Optional.empty(), java.util.Optional.empty(), List.of(effects)));
        return stack;
    }

    /** Whether it already has this buff at this strength or better. */
    private boolean hasAtLeast(BuffChoice buff) {
        MobEffectInstance current = getEffect(buff.effect());
        return current != null && current.getAmplifier() >= buff.amplifier();
    }

    /** How much it is carrying that it would rather not be. */
    private int harmfulEffects() {
        return (int) getActiveEffects().stream()
                .filter(effect -> effect.getEffect().value().getCategory()
                        == net.minecraft.world.effect.MobEffectCategory.HARMFUL)
                .count();
    }

    /** Its own kind, which is the one thing the boss rule says it will not attack. */
    private boolean isOwnKind(Entity entity) {
        return entity instanceof CompressedWitchEntity;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt(TAG_ACTION_COOLDOWN, this.actionCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.actionCooldown = compound.getInt(TAG_ACTION_COOLDOWN);
    }

    /** One thing it may drink or throw: the effect and how deep it wants it. */
    private record BuffChoice(Holder<MobEffect> effect, int amplifier) {
    }

    /* BOSS BAR */

    @Override
    public void startSeenByPlayer(ServerPlayer serverPlayer) {
        super.startSeenByPlayer(serverPlayer);
        this.bossEvent.addPlayer(serverPlayer);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer serverPlayer) {
        super.stopSeenByPlayer(serverPlayer);
        this.bossEvent.removePlayer(serverPlayer);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.bossEvent.setProgress(getHealth() / getMaxHealth());
    }
}
