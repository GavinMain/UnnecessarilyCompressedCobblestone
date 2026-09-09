package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.util.CompressionEnergy;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Engraving;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Engravings;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Sweep;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Compressed Spear: the 1.21.11 spear, cut from this mod's stone.
 * <p>
 * It is two weapons in one shaft, and the second is the reason to carry it.
 * <ul>
 * <li>The <em>jab</em> is the ordinary left click, and it is a sword's attack with a longer arm and
 * a hole in the middle: it reaches {@link #REACH_BONUS} further than a sword and refuses anything
 * inside {@link #MIN_RANGE} blocks. That minimum is not a bug and is the spear's whole character in
 * vanilla - a polearm is not a thing you use on something already standing on your feet. It is
 * enforced in {@code ModEvents}, on {@code AttackEntityEvent}, which is the one event a left click
 * on a creature goes through.</li>
 * <li>The <em>charge</em> is holding right click, and it is not a wind-up but a stance: the spear is
 * lowered and levelled, and it costs whatever runs onto it. What it hurts for is not the attack
 * damage attribute at all - it is {@link #CHARGE_MULTIPLIER} times how fast the wielder and the
 * target are closing on each other, in blocks a second, and below
 * {@link #DAMAGE_SPEED} of closing speed it is worth nothing whatsoever.</li>
 * </ul>
 * <p>
 * The closing speed is read off {@code getKnownMovement} on both parties rather than off their
 * positions, because for a player that method is the movement the client actually reported and is
 * the only figure the server and the client agree on. Projected onto the line between the two, so
 * running past something is worth nothing and running at it is worth everything.
 * <p>
 * The inscriber's bonus is added to the charge as well as to the jab, and it has to be added by
 * hand: the {@code #ucc:compression_melee_weapon} tag puts a point per digit onto
 * {@code Attributes.ATTACK_DAMAGE}, which the charge never reads. Without this the spear would be
 * the one weapon in the mod whose best attack could not be inscribed.
 * <p>
 * See {@link Engraving#SPEAR_DASH}, which is the other half of the design: the charge rewards
 * closing speed, and a player on foot can only supply a sprint's worth of it.
 */
public class CompressedSpearItem extends SwordItem {
    /** How much further than a sword the jab reaches, in blocks: vanilla's spear is 4.5 against 3. */
    public static final double REACH_BONUS = 1.5;

    /**
     * How close is too close for the jab, in blocks. Vanilla's own figure, and the price of the
     * reach above it.
     */
    public static final double MIN_RANGE = 2.0;

    /** On top of the tier's own bonus, the way every other weapon here states its damage. */
    public static final int ATTACK_DAMAGE = 3;

    /**
     * Slower than a sword's -2.4. A spear is a heavy thing to bring back round, and the jab is not
     * where its damage is.
     */
    public static final float ATTACK_SPEED = -2.8F;

    /**
     * What a block a second of closing speed is worth. Vanilla's ladder runs from 0.7 on a wooden
     * spear to 1.2 on a netherite one; this sits above the top of it, where a weapon cut from stone
     * this deep belongs.
     */
    public static final double CHARGE_MULTIPLIER = 2.0;

    /**
     * How fast the two have to be closing before the charge is worth anything at all, in blocks a
     * second. Vanilla's figure, and it is placed exactly where it is for a reason worth keeping: a
     * walk is about 4.3 and a sprint about 5.6, so the charge answers a player who is running and
     * ignores one who is standing.
     */
    public static final double DAMAGE_SPEED = 4.6;

    /** And how fast before it also throws what it hit. Vanilla's second figure. */
    public static final double KNOCKBACK_SPEED = 5.1;

    /** How wide the levelled point is, measured from the line the wielder is looking along. */
    private static final double CHARGE_WIDTH = 0.6;

    /**
     * How long the spear is out of use after a charge connects, in ticks.
     * <p>
     * It is the {@code ItemCooldowns} lockout rather than a field, for the reason every other
     * ability in this mod uses that one: it is checked by the client before the use packet is even
     * sent, it draws itself on the icon, and it cannot get out of step with the server. What it
     * locks out is the charge and nothing else - the jab is a left click and is untouched.
     */
    public static final int CHARGE_COOLDOWN = 20;

    /** How fast the lunge throws the wielder, in blocks a tick. */
    public static final double DASH_SPEED = 1.2;

    /**
     * How long the lunge is locked out for, in ticks. Longer than {@link #CHARGE_COOLDOWN}, because
     * what it hands out is a hit worth several times an ordinary one - and because it locks out the
     * charge with it, an engraved spear trades away the plain charge it could have had in between.
     */
    public static final int DASH_COOLDOWN = 60;

    private static final ResourceLocation REACH_ENTITY_ID = ResourceLocation.fromNamespaceAndPath(
            UnnecessarilyCompressedCobblestone.MOD_ID, "spear_entity_reach");
    private static final ResourceLocation REACH_BLOCK_ID = ResourceLocation.fromNamespaceAndPath(
            UnnecessarilyCompressedCobblestone.MOD_ID, "spear_block_reach");

    public CompressedSpearItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    /**
     * A sword's own two modifiers, restated, with both interaction ranges added.
     * <p>
     * {@code SwordItem.createAttributes} returns a finished {@code ItemAttributeModifiers} with
     * nothing to add to, so the two are rewritten against vanilla's {@code BASE_ATTACK_DAMAGE_ID}
     * and {@code BASE_ATTACK_SPEED_ID} - using vanilla's ids is what makes the tooltip read as a
     * weapon's stats rather than as two bonuses stapled onto one.
     * <p>
     * Block reach moves with entity reach, which is a deliberate departure from vanilla's spear:
     * that one lengthens only the arm that hits creatures. This mod's own rule - set by the katana
     * and the Reach engraving - is that the pair move together, because a weapon that hits a mob
     * six blocks off and not the block under it reads as broken.
     */
    public static ItemAttributeModifiers createAttributes(Tier tier) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(BASE_ATTACK_DAMAGE_ID,
                                ATTACK_DAMAGE + tier.getAttackDamageBonus(),
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(BASE_ATTACK_SPEED_ID, ATTACK_SPEED,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ENTITY_INTERACTION_RANGE,
                        new AttributeModifier(REACH_ENTITY_ID, REACH_BONUS,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.BLOCK_INTERACTION_RANGE,
                        new AttributeModifier(REACH_BLOCK_ID, REACH_BONUS,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    /** Levelled for as long as the button is held; there is no draw to complete. */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    /**
     * Lowering the spear, and - on an engraved one - the lunge that goes with it.
     * <p>
     * The dash is fired here rather than from a {@code RightClickItem} handler because the charge
     * already owns the right click: an engraved spear does not have a second button to put a
     * movement ability on, so the ability is what beginning to charge means. See
     * {@link Engraving#SPEAR_DASH}.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (Engravings.has(stack, Engraving.SPEAR_DASH)) {
            dash(level, player, stack);
        }

        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    /**
     * The Spear Dash engraving's lunge: {@link Engraving#SPEAR_DASH}.
     * <p>
     * It is velocity rather than a teleport, which is the opposite of what the katana's Dash does
     * and is the whole reason this is a separate engraving. A teleport covers ground and produces no
     * speed, and speed is exactly what this spear's charge is paid in; a shove of
     * {@link #DASH_SPEED} blocks a tick is {@code DASH_SPEED * 20} blocks a second of closing speed
     * for as long as it lasts, which is what the charge then reads.
     * <p>
     * Written on both sides. A player's own client is what moves them, and a delta set only on the
     * server arrives as a stutter; setting it on both and marking the server's copy is this mod's
     * usual answer.
     */
    private void dash(Level level, Player player, ItemStack stack) {
        if (player.getCooldowns().isOnCooldown(this)) {
            return;
        }

        Vec3 heading = player.getLookAngle().multiply(1.0, 0.0, 1.0);
        if (heading.lengthSqr() < 1.0E-4) {
            // Looking straight up or down: fall back on the way the body is facing, the way the
            // katana's Dash does.
            heading = Vec3.directionFromRotation(0.0F, player.getYRot());
        }

        Vec3 lunge = heading.normalize().scale(DASH_SPEED);
        player.setDeltaMovement(lunge.x, player.getDeltaMovement().y, lunge.z);
        player.hurtMarked = true;
        player.getCooldowns().addCooldown(this, DASH_COOLDOWN);

        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.0F, 0.6F);

        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.2, player.getZ(),
                    16, 0.3, 0.1, 0.3, 0.05);
        }
    }

    /**
     * One tick of the stance. Anything that has run onto the point pays for it.
     * <p>
     * Server side only, and not because the client would get it wrong: both sides can work the
     * answer out, since whether an entity is using an item, which item, and every party's known
     * movement are all synced. It is one-sided because the hit is - dealing it twice is dealing it
     * once and desynchronising the second.
     */
    @Override
    public void onUseTick(Level level, LivingEntity wielder, ItemStack stack, int remainingUseDuration) {
        if (!(level instanceof ServerLevel server) || !(wielder instanceof Player player)) {
            return;
        }

        LivingEntity target = levelledAt(player, stack);
        if (target == null) {
            return;
        }

        double closing = closingSpeed(player, target);
        if (closing < DAMAGE_SPEED) {
            return;
        }

        float damage = (float) (closing * CHARGE_MULTIPLIER) + CompressionEnergy.bonus(stack);

        // A player attack, so armour, Protection, Resistance and everything else answer it exactly
        // as they would answer the jab this is standing in for.
        target.hurt(player.damageSources().playerAttack(player), damage);

        if (closing >= KNOCKBACK_SPEED) {
            // Vanilla's own convention: the two arguments are the direction the blow came *from*,
            // and `knockback` pushes away from it.
            Vec3 push = player.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
            if (push.lengthSqr() > 1.0E-4) {
                target.knockback(closing / KNOCKBACK_SPEED * 0.5, push.x, push.z);
            }
        }

        player.stopUsingItem();

        // Only if nothing is already ticking: an engraved spear set the far longer lunge lockout
        // when this charge began, and overwriting it here would make the lunge a second's wait.
        if (!player.getCooldowns().isOnCooldown(this)) {
            player.getCooldowns().addCooldown(this, CHARGE_COOLDOWN);
        }

        server.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() / 2.0,
                target.getZ(), 20, 0.3, 0.3, 0.3, 0.4);
        server.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                SoundSource.PLAYERS, 1.0F, 0.7F);
    }

    /**
     * Whatever the levelled point is touching: the nearest living thing along the line the wielder
     * is looking, out to the reach the spear's own attribute grants.
     * <p>
     * It is a swept test rather than a box at the end of the reach, for the reason every fast thing
     * in this mod is swept: a charge covers ground in a tick, and a box would miss whatever the
     * point actually went through on the way. There is no minimum range here, unlike the jab -
     * a lunge that refused what it had run into could never land at all.
     */
    @Nullable
    private LivingEntity levelledAt(Player player, ItemStack stack) {
        double reach = player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getLookAngle().scale(reach));

        LivingEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (LivingEntity caught : player.level().getEntitiesOfClass(LivingEntity.class,
                Sweep.bounds(from, to, CHARGE_WIDTH),
                entity -> entity != player && entity.isAlive() && !(entity instanceof ArmorStand)
                        && !player.isAlliedTo(entity)
                        && Sweep.caught(entity, from, to, CHARGE_WIDTH))) {
            double distance = caught.distanceToSqr(from);
            if (distance < nearestDistance) {
                nearest = caught;
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    /**
     * How fast the two are closing on each other, in blocks a second.
     * <p>
     * The difference of the two movements projected onto the line between them, so only motion that
     * shortens the gap counts: sprinting past something at arm's length is worth nothing, and two
     * things running at each other are worth the sum of both. {@code getKnownMovement} rather than
     * {@code getDeltaMovement} because for a player the former is the movement the client reported
     * and the latter is very often nothing at all on the server's copy.
     */
    public static double closingSpeed(LivingEntity wielder, LivingEntity target) {
        Vec3 gap = target.position().subtract(wielder.position());
        if (gap.lengthSqr() < 1.0E-6) {
            return 0.0;
        }

        Vec3 relative = wielder.getKnownMovement().subtract(target.getKnownMovement());
        return Math.max(0.0, relative.dot(gap.normalize())) * 20.0;
    }

    /** Swallows every point of durability damage before vanilla can apply it. */
    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
        return 0;
    }

    /** The durability is never spent, so say so the way an unbreakable item does. */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("item.unbreakable").withStyle(ChatFormatting.BLUE));
    }
}
