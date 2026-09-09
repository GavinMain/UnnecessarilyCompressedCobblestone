package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.util.Augment;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Augments;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Engraving;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Engravings;
import net.fahr3n.unnecessarilycompressedcobblestone.util.CompressionEnergy;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A guardian's beam, in a player's hands.
 * <p>
 * It is held the way a staff is held - right click and keep holding - but it is not a staff, and the
 * difference is the whole weapon: a staff fires once, when it is let go, whereas this charges for
 * {@link #BASE_CHARGE_TICKS} ticks and then lands its beam on whatever is being looked at, and then
 * charges again, for as long as the button is down. That is exactly a guardian's own attack, wind-up
 * and all, which is why the beam takes as long as it does and why the charge is worth augmenting.
 * <p>
 * Two upgrade paths meet on it. The Compression Inscriber pours energy in, and the laser turns that
 * into damage at a point per digit, which is why it is listed in {@code #inscribable} directly next
 * to the staves rather than joining a stat tag - a beam has no velocity to buy. The augments are the
 * other, and there are three sorts:
 * <ul>
 * <li>{@code Damage} multiplies the whole beam, Compression Energy included, by a quarter per level.
 * <li>{@code Rate} takes {@value #RATE_TICKS_PER_LEVEL} ticks off the charge per level.
 * <li>The effect augments each press their effect onto whatever the beam hits, for
 *     {@value #EFFECT_TICKS} ticks at level I, and each being a family of its own they may all be
 *     fitted at once.
 * </ul>
 * What may sit beside what is {@code Augments}; what an augment is worth is here.
 * <p>
 * There is a third path on top of those two, and it changes only the last of the three: the
 * Incremental engraving makes every effect augment <em>deepen</em> what its beam already put on the
 * target rather than restart its clock, which turns a held beam from a status that never lapses into
 * a ladder that climbs for as long as the button is down. See {@link #land}.
 * <p>
 * The beam itself is drawn client side by {@code LaserBeamRenderer}, off nothing but state the
 * server already syncs - who is using an item, what it is, and how long they have held it - so
 * nothing about firing needs a packet of its own.
 */
public class RayOfLaserItem extends Item {
    /**
     * Two seconds, which is what a guardian's beam takes. Vanilla's own is 80 ticks and an elder
     * guardian's 60; this sits between them because it is the figure the Rate augments are priced
     * against, and 40 makes three tenths of a second a plain fifteen percent of the charge.
     */
    public static final int BASE_CHARGE_TICKS = 40;

    /** Three tenths of a second off the charge per level of Rate, so Rate III fires at 1.1 seconds. */
    public static final int RATE_TICKS_PER_LEVEL = 6;

    /**
     * However many Rate augments are ever added, a charge is never shorter than this. Three levels
     * is the deepest there is and leaves 22 ticks, so nothing is clamped away today - the floor is
     * only here so that a deeper level could not reach zero, which would fire the laser every tick.
     */
    public static final int MIN_CHARGE_TICKS = 5;

    /**
     * What one beam is worth before Compression Energy and the Damage augments. Ten hearts through
     * no armour: enough that the weapon is worth the wind-up, and far enough below the Compressed
     * Guardian's own 132 that being on the other end of one is still not what the boss is.
     */
    public static final float BASE_DAMAGE = 20.0F;

    /** What one level of Damage adds, as a fraction of the whole beam. Four levels double it. */
    public static final float DAMAGE_PER_LEVEL = 0.25F;

    /** How long an effect augment's effect lasts, at level I. Ten seconds, refreshed by every hit. */
    public static final int EFFECT_TICKS = 200;

    /**
     * As deep as the Incremental engraving will press an effect. An amplifier is written as an
     * unsigned byte, so 255 is not a balance figure but the hard edge of what a
     * {@code MobEffectInstance} can hold - a beam held long enough would otherwise stack past it and
     * fail to save. Nothing survives anywhere near this many rungs of anything.
     */
    public static final int MAX_AMPLIFIER = 255;

    /**
     * How far the beam reaches. It is the Compressed Guardian's own search radius, which is also how
     * far that boss's beam carries - so the fight it is built out of is a fight this can answer at
     * the same distance.
     */
    public static final double RANGE = 32.0;

    /** Held indefinitely, the way a bow is; the charge is measured from how long it has been held. */
    private static final int USE_DURATION = 72000;

    /**
     * What the beam will stop on. Spectators and anything already dead are not there to be hit, and
     * neither is the holder's own passenger - but everything else is, players included: this is a
     * weapon, and where it is pointed is what it hits.
     */
    private static final Predicate<Entity> CAN_BE_HIT =
            entity -> entity.isAlive() && !entity.isSpectator() && entity.isPickable();

    public RayOfLaserItem(Properties properties) {
        super(properties);
    }

    /**
     * How long one beam takes to charge, Rate included. Both sides need it - the server to know when
     * to fire, the client to know how far along the beam it is drawing should be - so it is static
     * and reads nothing but the stack.
     */
    public static int chargeTicks(ItemStack stack) {
        int rate = Augments.level(stack, Augment.Family.RATE);
        return Math.max(MIN_CHARGE_TICKS, BASE_CHARGE_TICKS - rate * RATE_TICKS_PER_LEVEL);
    }

    /**
     * What one beam does: the base twenty, plus a point per digit of Compression Energy, all of it
     * then multiplied by the Damage augment. The multiplier covers the inscribed points as well as
     * the base, so Damage IV is worth twice whatever the laser had become rather than twice what it
     * started as.
     */
    public static float damage(ItemStack stack) {
        int level = Augments.level(stack, Augment.Family.DAMAGE);
        return (BASE_DAMAGE + CompressionEnergy.bonus(stack)) * (1.0F + level * DAMAGE_PER_LEVEL);
    }

    /**
     * How far into the current charge {@code holder} is, from 0 at the start to 1 the tick the beam
     * lands. Zero if they are not holding this laser at all. This is what the renderer brightens the
     * beam with, and it is derived rather than sent: the tick count a use has been held for is
     * already synced for every player the client can see.
     */
    public static float chargeProgress(ItemStack stack, @Nullable LivingEntity holder) {
        if (holder == null || !holder.isUsingItem() || holder.getUseItem() != stack
                || !(stack.getItem() instanceof RayOfLaserItem)) {
            return 0.0F;
        }

        int charge = chargeTicks(stack);
        int held = stack.getUseDuration(holder) - holder.getUseItemRemainingTicks();

        return (float) (held % charge) / (float) charge;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        // BOW is what puts the holder at a fifth of their walking speed while the laser is charging,
        // which is the whole cost of holding the beam on something.
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    /**
     * The charge, and the beam at the end of it.
     * <p>
     * Unlike a staff there is no release: a full charge fires on its own and the next one starts on
     * the same tick, so holding the button is a beam every {@link #chargeTicks} ticks rather than one
     * beam whenever the player chooses to let go. That is a guardian's own rhythm, and it is what
     * makes the Rate augments a rate rather than a reflex test.
     */
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseTicks) {
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof Player player)) {
            return;
        }

        int held = getUseDuration(stack, entity) - remainingUseTicks;
        int charge = chargeTicks(stack);

        if (held % charge == 1) {
            // The guardian's own charge-up, at the top of every wind-up rather than only the first,
            // so a held laser sounds like the beam it is drawing.
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GUARDIAN_ATTACK, SoundSource.PLAYERS, 0.8F, 1.0F);
        }

        if (held > 0 && held % charge == 0) {
            fire(serverLevel, player, stack);
        }
    }

    /**
     * One beam. Whatever the line of sight meets first takes {@link #damage} and everything the
     * effect augments carry; a beam that meets nothing at all still costs its charge and draws, which
     * is what makes aiming it part of the weapon.
     */
    private void fire(ServerLevel level, Player player, ItemStack stack) {
        Vec3 end = beamEnd(level, player);
        LivingEntity target = beamTarget(level, player);

        level.sendParticles(ParticleTypes.BUBBLE_POP, end.x, end.y, end.z, 8, 0.2, 0.2, 0.2, 0.0);

        if (target == null) {
            return;
        }

        land(level, stack, target, player);
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    /**
     * What a beam does when it arrives: {@link #damage} once, and every effect the augments on
     * {@code laser} carry.
     * <p>
     * Public and static, and taking the laser as a stack rather than reading one out of a hand,
     * because the beam is no longer only a player's. The Laser TNT lands one off a stack it builds
     * itself and never gives to anybody, and doing that through this method rather than through a
     * copy of it is what keeps a new augment worth the same out of both - the augment loop is here
     * and there is only one of it.
     *
     * @param attacker whoever the hit is credited to, or null if nobody lit it
     */
    public static void land(ServerLevel level, ItemStack laser, LivingEntity target,
                            @Nullable LivingEntity attacker) {
        // One hit, the way the Compressed Guardian's beam lands one: a living entity ignores a
        // second hit inside twenty ticks of the first unless it is larger, so anything split in two
        // here would mostly be thrown away.
        target.hurt(attacker == null
                        ? level.damageSources().magic()
                        : level.damageSources().mobAttack(attacker),
                damage(laser));

        boolean incremental = Engravings.has(laser, Engraving.INCREMENTAL);

        for (Augment augment : Augments.get(laser)) {
            Holder<MobEffect> effect = augment.effect();
            if (effect == null) {
                continue;
            }

            // addEffect replaces an instance of the same level with a longer one, so a target that
            // is hit again before the ten seconds are up simply has them back - which is the wanted
            // behaviour and needs nothing said about it here.
            //
            // The Incremental engraving is the other half of that sentence: rather than handing the
            // ten seconds back it hands a level, and leaves the clock alone. Vanilla's own merge is
            // what makes that one call - an instance of a *higher* amplifier replaces both the level
            // and the duration of the one already there, so passing the remaining duration back with
            // the deeper level is exactly "one rung up, clock untouched".
            MobEffectInstance carried = incremental ? target.getEffect(effect) : null;
            if (carried == null) {
                target.addEffect(new MobEffectInstance(effect, EFFECT_TICKS, 0, false, true, true), attacker);
                continue;
            }

            target.addEffect(new MobEffectInstance(effect, carried.getDuration(),
                    Math.min(MAX_AMPLIFIER, carried.getAmplifier() + 1), false, true, true), attacker);
        }
    }

    /**
     * What the beam is pointed at, or null if it is pointed at nothing alive. Only living things can
     * be hit by it - the beam passes an armour stand or a boat the way a guardian's does.
     */
    @Nullable
    public static LivingEntity beamTarget(Level level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = blockEnd(level, player);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, eye, end,
                new AABB(eye, end).inflate(1.0), CAN_BE_HIT);

        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    /**
     * Where the beam ends: on whatever it hits, else on the block behind that, else at the end of its
     * reach. The renderer draws to this point and the damage is dealt at it, so both agree about
     * where the beam actually is without either having to tell the other.
     */
    public static Vec3 beamEnd(Level level, Player player) {
        LivingEntity target = beamTarget(level, player);
        return target != null ? target.position().add(0.0, target.getBbHeight() * 0.5, 0.0)
                : blockEnd(level, player);
    }

    /** The line of sight up to the first block it meets, or the full {@link #RANGE} if it meets none. */
    private static Vec3 blockEnd(Level level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 far = eye.add(player.getLookAngle().scale(RANGE));
        BlockHitResult hit = level.clip(new ClipContext(eye, far,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        return hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : far;
    }

    /**
     * Enchantable at a table. A plain {@link Item} has an enchantment value of zero, which is what
     * would otherwise keep the laser off the table entirely; nine is what the mod's armour, tools and
     * staves all use.
     */
    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 9;
    }

    /**
     * Never wears out, for the reason in {@link CompressedCobblestoneArmorItem}: the durability is
     * kept and simply never spent, because an item with the {@code unbreakable} component is not
     * damageable and so cannot be enchanted at a table at all.
     */
    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
        return 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("item.unbreakable").withStyle(ChatFormatting.BLUE));
        tooltipComponents.add(Component.translatable(
                        "tooltip.unnecessarilycompressedcobblestone.ray_of_laser",
                        String.format("%.1f", damage(stack)),
                        String.format("%.1f", chargeTicks(stack) / 20.0F))
                .withStyle(ChatFormatting.GRAY));
    }
}
