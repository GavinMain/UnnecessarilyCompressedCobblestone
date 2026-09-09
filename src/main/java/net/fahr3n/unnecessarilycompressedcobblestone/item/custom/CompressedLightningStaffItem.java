package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import java.util.List;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.BoltProjectileEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.item.ModItems;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Composition;
import net.fahr3n.unnecessarilycompressedcobblestone.util.CompressionEnergy;
import net.fahr3n.unnecessarilycompressedcobblestone.util.DeferredStrikes;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Engraving;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Engravings;
import net.fahr3n.unnecessarilycompressedcobblestone.util.LightningSong;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A staff that calls lightning down on whatever its holder is looking at.
 * <p>
 * The bolt lands on whatever the line of sight meets first - an entity's hitbox, then a block, and
 * failing both the last loaded block along that line, so a shot at the horizon lands at the edge of
 * the world the caster can see rather than being wasted. Nothing about the strike needs line of sight; it is a real {@code LightningBolt},
 * so it sets fire and shocks whatever is within three blocks of where it lands, exactly as the sky's
 * own does.
 * <p>
 * Of the three staff enchantments, Surge is the one that means damage here, and Multicast is how many
 * bolts land in the second after a cast. How a staff is held, cast and released is all
 * {@link CompressedStaffItem}.
 */
public class CompressedLightningStaffItem extends CompressedStaffItem {
    /** Four seconds, before Silent Cast. */
    public static final int CAST_TICKS = 80;

    /** What a bolt does before Compression Energy and Surge, which is what vanilla lightning does. */
    public static final float BASE_DAMAGE = 5.0F;

    /** How far it will reach for something to strike. */
    public static final double RANGE = 256.0;

    /** One second, which is the unit Multicast is counted in. */
    private static final int TICKS_PER_SECOND = 20;

    /**
     * What the Composition engraving loads: the same datapack file the Compressed Composer plays, so
     * a pack that rewrites {@code fur_elise} rewrites both the boss's attack and this staff's.
     */
    public static final ResourceLocation COMPOSITION_SONG =
            ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, "fur_elise");

    /** How fast an engraved staff throws its bolt - a little quicker than a fully drawn launcher. */
    private static final float COMPOSITION_VELOCITY = 2.2F;

    /** How wide a Multicast volley scatters, so several performances are not all in one spot. */
    private static final float COMPOSITION_SPREAD = 3.0F;

    /**
     * What the line will stop on. Spectators and anything already dead are not there to be hit, and
     * neither is a passenger of the caster's own - but everything else is, players included: this is
     * a weapon, and where it is pointed is what it hits.
     */
    private static final Predicate<Entity> CAN_BE_STRUCK =
            entity -> entity.isAlive() && !entity.isSpectator() && entity.isPickable();

    public CompressedLightningStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    public int baseCastTicks() {
        return CAST_TICKS;
    }

    @Override
    protected void release(ServerLevel level, Player player, ItemStack stack) {
        if (Engravings.has(stack, Engraving.COMPOSITION)) {
            performComposition(level, player, stack);
            return;
        }

        int bolts = multicast(stack, level);

        // Multicast is a rate: the bolts of one cast are spread evenly across the second after it
        // rather than all landing on the tick the staff was let go.
        DeferredStrikes.queue(level, strikePos(level, player), damage(stack, level), bolts,
                Math.max(1, TICKS_PER_SECOND / bolts), player);
    }

    /**
     * The Composition engraving's cast: Composition Bolts written with {@link #COMPOSITION_SONG},
     * thrown where the staff was pointed.
     * <p>
     * Nothing about the strike is written here. A {@link BoltProjectileEntity} carries the bolt it
     * was fired as and asks it what to do wherever it stops, and a Composition Bolt's answer is to
     * play its sheet - so this is a delivery and no more, exactly as the Bolt Launcher is. The sheet
     * itself is the mod's own song file turned back into squares by {@link Composition#fromSong}, so
     * the piece is written down in one place rather than two.
     * <p>
     * The staff's own three dials still mean what they always did, and the middle one is the one to
     * read twice: Multicast is how many bolts a cast throws, Silent Cast is how quickly it may be
     * thrown, and Compression Energy and Surge are handed over as the signal - which for a song is
     * how far it carries, since the notes themselves hit for what lightning hits for wherever a song
     * is played from. Above a full signal there is no gain left to give and the rest becomes
     * distance, which is the only louder there is.
     */
    private void performComposition(ServerLevel level, Player player, ItemStack stack) {
        List<Integer> sheet = Composition.fromSong(LightningSong.get(level.getServer(), COMPOSITION_SONG));
        if (sheet.isEmpty()) {
            return;
        }

        ItemStack bolt = new ItemStack(ModItems.COMPOSITION_BOLT.get());
        Composition.set(bolt, sheet);

        int shots = multicast(stack, level);
        int signal = compositionSignal(stack, level);

        for (int shot = 0; shot < shots; shot++) {
            BoltProjectileEntity projectile = new BoltProjectileEntity(level, player, bolt, stack);
            projectile.setSignal(signal);
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F,
                    COMPOSITION_VELOCITY, shots == 1 ? 0.0F : COMPOSITION_SPREAD);
            level.addFreshEntity(projectile);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT,
                SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    /**
     * How hard an engraved cast calls its piece down: a full redstone signal, one more per digit of
     * Compression Energy, all of it multiplied by Surge. It is the same shape of number
     * {@link #damage} is, spent on the one thing a song has to spend it on.
     */
    public static int compositionSignal(ItemStack stack, @Nullable Level level) {
        return Math.round((BoltItem.MAX_SIGNAL + CompressionEnergy.bonus(stack)) * surge(stack, level));
    }

    /**
     * The base five, plus one per digit of Compression Energy inscribed into the staff, all of it then
     * multiplied by Surge. Surge stacking multiplicatively is what makes its last level worth more
     * than its first.
     */
    public static float damage(ItemStack stack, @Nullable Level level) {
        return (BASE_DAMAGE + CompressionEnergy.bonus(stack)) * surge(stack, level);
    }

    /**
     * Where the bolt lands: on anything whose hitbox the line passes through, else the open block
     * against whatever is being looked at, else - if there is nothing in reach to look at, which is
     * what the horizon is - the last block along that line that the world still has loaded.
     */
    private static BlockPos strikePos(Level level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 far = eye.add(look.scale(RANGE));
        BlockHitResult hit = level.clip(new ClipContext(eye, far,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        // The line stops at the first thing it meets, and a hitbox counts: aiming at something
        // standing in the open used to shoot straight past it at the ground behind, or at the
        // horizon, which put the bolt anywhere from a few blocks away to not at all.
        Vec3 end = hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : far;
        EntityHitResult struck = ProjectileUtil.getEntityHitResult(level, player, eye, end,
                new AABB(eye, end).inflate(1.0), CAN_BE_STRUCK);

        if (struck != null) {
            return struck.getEntity().blockPosition();
        }

        if (hit.getType() == HitResult.Type.BLOCK) {
            // The face that was hit, so the bolt stands on the block rather than inside it.
            return hit.getBlockPos().relative(hit.getDirection());
        }

        BlockPos last = BlockPos.containing(eye);
        for (double distance = 1.0; distance <= RANGE; distance++) {
            BlockPos pos = BlockPos.containing(eye.add(look.scale(distance)));
            if (!level.isLoaded(pos) || level.isOutsideBuildHeight(pos)) {
                break;
            }

            last = pos;
        }

        return last;
    }
}
