package net.fahr3n.unnecessarilycompressedcobblestone.enchantment.custom;

import com.mojang.serialization.MapCodec;

import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.phys.Vec3;

/**
 * Squeezes cobblestone out of whatever was just hit: one level of Compression drops one
 * Compressed Cobblestone, two drops two Double Compressed, and so on, so the level is both how
 * many blocks fall and how deeply compressed they are.
 */
public record CompressionEnchantmentEffect() implements EnchantmentEntityEffect {
    public static final MapCodec<CompressionEnchantmentEffect> CODEC = MapCodec.unit(CompressionEnchantmentEffect::new);

    @Override
    public void apply(ServerLevel serverLevel, int enchantmentLevel, EnchantedItemInUse enchantedItemInUse, Entity entity, Vec3 vec3) {
        int level = Math.min(enchantmentLevel, ModBlocks.MAX_COMPRESSION_LEVEL);
        if (level < 1) {
            return;
        }

        entity.spawnAtLocation(new ItemStack(ModBlocks.byLevel(level).get(), level));
    }

    @Override
    public MapCodec<? extends EnchantmentEntityEffect> codec() {
        return CODEC;
    }
}
