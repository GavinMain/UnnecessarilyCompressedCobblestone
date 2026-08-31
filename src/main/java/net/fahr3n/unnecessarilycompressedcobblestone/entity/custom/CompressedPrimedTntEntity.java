package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Lit Compressed TNT of either strength. There is one entity type rather than one per block: the
 * blast is a number this carries and the block it draws itself as is already synced by
 * {@link PrimedTnt}, so the two TNTs differ only in the values they are handed when lit.
 */
public class CompressedPrimedTntEntity extends PrimedTnt {
    private static final String TAG_EFFECT = "compression_effect";

    /** What vanilla TNT explodes with, and the fallback if a saved entity has no power on it. */
    public static final float VANILLA_POWER = 4.0F;

    /** The fuse vanilla lights TNT with, copied because the constructor that sets it is unusable here. */
    private static final int DEFAULT_FUSE = 80;

    private static final String TAG_DAMAGE_SCALE = "compression_damage_scale";
    private static final String TAG_DETONATE_ON_LANDING = "compression_detonate_on_landing";

    /**
     * What the fuse is cut to the moment the ground is found. One, because {@link PrimedTnt#tick}
     * takes one off the fuse and goes off at zero, so it detonates on the very tick it lands.
     */
    private static final int LANDED_FUSE = 1;

    private CompressedTntEffect effect = CompressedTntEffect.BLAST_5X;

    /**
     * What this one's blast is worth against whatever it was built to do. It is one for anything a
     * player lit by hand; the Arrow TNT Staff sets it higher, which is how inscribing a staff turns
     * into a harder explosion without touching the TNT block itself.
     */
    private float damageScale = 1.0F;

    /**
     * {@link PrimedTnt} keeps its owner private and offers no setter, so the reference is kept again
     * here and handed back through {@link #getOwner()}. Without it an explosion kill would be
     * credited to nobody.
     */
    @Nullable
    private LivingEntity compressedOwner;

    /**
     * Whether this one waits until it has landed. Anything dropped from a height has a fuse that is
     * really a race against the fall - TNT takes 44 ticks to fall the 24 blocks the skeleton's rain
     * drops it from - and losing that race is not a small thing for the TNTs that throw something
     * outwards: an Arrow TNT that goes off twenty blocks up scatters its arrows over the whole sky
     * instead of across the ground. With this set the fuse is only a backstop, for TNT that finds no
     * ground at all.
     */
    private boolean detonateOnLanding;

    public CompressedPrimedTntEntity(EntityType<? extends CompressedPrimedTntEntity> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Lights a block of Compressed TNT. This repeats what vanilla's four-argument constructor does
     * rather than calling it, because that one pins the entity to {@code EntityType.TNT}.
     */
    public CompressedPrimedTntEntity(Level level, double x, double y, double z, @Nullable LivingEntity owner,
                                     BlockState renderedAs, CompressedTntEffect effect) {
        this(ModEntities.COMPRESSED_PRIMED_TNT.get(), level);

        this.setPos(x, y, z);
        double angle = level.random.nextDouble() * (float) (Math.PI * 2);
        this.setDeltaMovement(-Math.sin(angle) * 0.02, 0.2F, -Math.cos(angle) * 0.02);
        this.setFuse(DEFAULT_FUSE);
        this.xo = x;
        this.yo = y;
        this.zo = z;

        this.compressedOwner = owner;
        this.effect = effect;
        this.setBlockState(renderedAs);
    }

    @Nullable
    @Override
    public LivingEntity getOwner() {
        return this.compressedOwner;
    }

    public CompressedTntEffect getEffect() {
        return this.effect;
    }

    public float getDamageScale() {
        return this.damageScale;
    }

    public void setDamageScale(float damageScale) {
        this.damageScale = damageScale;
    }

    public void setDetonateOnLanding(boolean detonateOnLanding) {
        this.detonateOnLanding = detonateOnLanding;
    }

    /**
     * Cuts the fuse once there is ground underneath. {@code onGround} is last tick's answer, since
     * the move that sets it has not run yet this tick, which is exactly right: it goes off on the
     * tick after the one it touched down on, having bounced once the way vanilla TNT does.
     */
    @Override
    public void tick() {
        if (this.detonateOnLanding && !this.level().isClientSide() && this.onGround()
                && this.getFuse() > LANDED_FUSE) {
            this.setFuse(LANDED_FUSE);
        }

        super.tick();
    }

    /**
     * Hands off to whatever this TNT actually does, which for most of them is not an explosion at
     * all. Only the blast variants pass through to {@code Level#explode}, and they lose vanilla's
     * nether-portal damage calculator on the way - the flag behind it is private to
     * {@link PrimedTnt} - so a Compressed TNT carried through a portal will break the portal it came
     * out of, which plain TNT would not.
     */
    @Override
    protected void explode() {
        if (this.level() instanceof ServerLevel serverLevel) {
            this.effect.detonate(serverLevel, this);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString(TAG_EFFECT, this.effect.name());
        compound.putFloat(TAG_DAMAGE_SCALE, this.damageScale);
        compound.putBoolean(TAG_DETONATE_ON_LANDING, this.detonateOnLanding);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains(TAG_EFFECT)) {
            this.effect = CompressedTntEffect.byName(compound.getString(TAG_EFFECT));
        }

        if (compound.contains(TAG_DAMAGE_SCALE)) {
            this.damageScale = compound.getFloat(TAG_DAMAGE_SCALE);
        }

        this.detonateOnLanding = compound.getBoolean(TAG_DETONATE_ON_LANDING);
    }
}
