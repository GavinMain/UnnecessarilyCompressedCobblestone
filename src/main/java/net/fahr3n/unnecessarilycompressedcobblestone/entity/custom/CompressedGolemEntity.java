package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The Compressed Golem. It extends {@link IronGolem}, so it inherits the whole iron golem brain -
 * the melee attack, the village patrolling, the cracking as it takes damage - and changes only what
 * it is made of, how much of it there is, and who it is willing to hit.
 */
public class CompressedGolemEntity extends IronGolem {
    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.unnecessarilycompressedcobblestone.compressed_golem"),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);

    public CompressedGolemEntity(EntityType<? extends IronGolem> entityType, Level level) {
        super(entityType, level);
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
     * The iron golem's goals, plus one: it hunts players on sight instead of waiting to be angered.
     * The vanilla player goal is kept as well - it costs nothing and still handles being provoked.
     */
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /**
     * An iron golem refuses to attack players when it was built by one. This golem is hostile to
     * everyone who built it included, so only vanilla's creeper exception is kept.
     */
    @Override
    public boolean canAttackType(EntityType<?> type) {
        return type != EntityType.CREEPER;
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
