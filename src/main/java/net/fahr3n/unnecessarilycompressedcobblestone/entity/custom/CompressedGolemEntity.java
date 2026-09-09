package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;

/**
 * The Compressed Golem. It extends {@link IronGolem}, so it inherits the whole iron golem brain -
 * the melee attack, the village patrolling, the cracking as it takes damage - and changes only what
 * it is made of, how much of it there is, and who it is willing to hit.
 */
public class CompressedGolemEntity extends IronGolem {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            UnnecessarilyCompressedCobblestone.MOD_ID,
            "textures/entity/compressed_golem/compressed_golem.png");

    /** Protected because the deeper tiers rename and recolour it rather than keeping one of their own. */
    protected final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.unnecessarilycompressedcobblestone.compressed_golem"),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);

    public CompressedGolemEntity(EntityType<? extends IronGolem> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Which skin this golem is drawn in. It lives on the entity rather than in the renderer so that
     * a new golem stays what the class javadoc promises - a block registration, an entity and a
     * texture - with nothing to add on the client side.
     */
    public ResourceLocation texture() {
        return TEXTURE;
    }

    /**
     * Iron golem numbers with the health raised from 100 to 250. Knockback resistance is stated
     * again at 1.0 rather than inherited silently: that is the ceiling of the attribute, so the
     * golem cannot be knocked back at all, which is already where an iron golem sits.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return IronGolem.createAttributes()
                .add(Attributes.MAX_HEALTH, 250.0)
                .add(Attributes.MOVEMENT_SPEED, .5)
                .add(Attributes.STEP_HEIGHT, 2)
                .add(Attributes.ATTACK_DAMAGE, 25)
                .add(Attributes.ARMOR, 4)
                .add(Attributes.ARMOR_TOUGHNESS, 12)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    /**
     * The iron golem's goals, plus one that makes this golem hostile to the whole world: it hunts
     * any living thing on sight rather than waiting to be angered, and the only thing it will not
     * pick as a target is another Compressed Golem.
     * <p>
     * The vanilla goals are kept as well - they cost nothing and still handle being provoked.
     */
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                target -> !(target instanceof CompressedGolemEntity) && !(target instanceof ArmorStand)));
    }

    /**
     * Hostile to everything, including whoever built it and including creepers, which an iron golem
     * would leave alone. Two exceptions: its own kind, so a pack of these does not turn on itself
     * the moment it runs out of anything else to hit, and armour stands, which are living entities
     * as far as the targeting code is concerned but are furniture as far as anyone else is. A third,
     * the Summoner that may have called it up, is handled as an alliance rather than here - see
     * {@link #isAlliedTo}.
     */
    @Override
    public boolean canAttackType(EntityType<?> type) {
        return type != ModEntities.COMPRESSED_GOLEM.get() && type != EntityType.ARMOR_STAND;
    }

    /**
     * Whatever called it up is not something it will fight.
     * <p>
     * Alliance is vanilla's own faction check - the evoker uses exactly this to keep its vexes off
     * it - and {@code TargetingConditions} consults it before any acquisition in combat, so this
     * covers the hunting goal and the retaliation goal in one, without either of them needing to
     * know the Summoner exists. It says nothing about damage: a summoner standing inside its own
     * flock's blast still takes it.
     */
    @Override
    public boolean isAlliedTo(Entity entity) {
        return entity instanceof CompressedSummonerEntity || super.isAlliedTo(entity);
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
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }
}
