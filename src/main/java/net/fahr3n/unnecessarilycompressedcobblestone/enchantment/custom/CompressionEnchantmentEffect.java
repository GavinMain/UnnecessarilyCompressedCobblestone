package net.fahr3n.unnecessarilycompressedcobblestone.enchantment.custom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.phys.Vec3;

/**
 * Squeezes cobblestone out of whatever was just hit: the level is how many blocks fall, and the
 * level times {@code tierPerLevel} is how deeply compressed they are. Plain Compression presses one
 * tier per level, Super Compression ten and Hyper Compression twenty, so the same effect covers the
 * whole family and only the multiplier written into the enchantment changes.
 *
 * @param tierPerLevel how many compression levels each enchantment level is worth
 */
public record CompressionEnchantmentEffect(int tierPerLevel) implements EnchantmentEntityEffect {
    public static final MapCodec<CompressionEnchantmentEffect> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    // Optional so the plain one-tier-per-level form stays the terse thing it was.
                    Codec.intRange(1, ModBlocks.MAX_COMPRESSION_LEVEL)
                            .optionalFieldOf("tier_per_level", 1)
                            .forGetter(CompressionEnchantmentEffect::tierPerLevel)
            ).apply(instance, CompressionEnchantmentEffect::new));

    @Override
    public void apply(ServerLevel serverLevel, int enchantmentLevel, EnchantedItemInUse enchantedItemInUse, Entity entity, Vec3 vec3) {
        if (enchantmentLevel < 1) {
            return;
        }

        int tier = Math.min(enchantmentLevel * this.tierPerLevel, ModBlocks.MAX_COMPRESSION_LEVEL);
        entity.spawnAtLocation(new ItemStack(ModBlocks.byLevel(tier).get(), enchantmentLevel));
    }

    @Override
    public MapCodec<? extends EnchantmentEntityEffect> codec() {
        return CODEC;
    }
}
