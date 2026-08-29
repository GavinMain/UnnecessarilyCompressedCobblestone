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

    private CompressedTntEffect effect = CompressedTntEffect.BLAST_5X;

    /**
     * {@link PrimedTnt} keeps its owner private and offers no setter, so the reference is kept again
     * here and handed back through {@link #getOwner()}. Without it an explosion kill would be
     * credited to nobody.
     */
    @Nullable
    private LivingEntity compressedOwner;

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
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains(TAG_EFFECT)) {
            this.effect = CompressedTntEffect.byName(compound.getString(TAG_EFFECT));
        }
    }
}
