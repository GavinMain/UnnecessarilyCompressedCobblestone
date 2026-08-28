package net.fahr3n.unnecessarilycompressedcobblestone.enchantment;

import java.util.function.Supplier;

import com.mojang.serialization.MapCodec;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.enchantment.custom.CompressionEnchantmentEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEnchantmentEffects {
    public static final DeferredRegister<MapCodec<? extends EnchantmentEntityEffect>> ENTITY_ENCHANTMENT_EFFECTS =
            DeferredRegister.create(Registries.ENCHANTMENT_ENTITY_EFFECT_TYPE, UnnecessarilyCompressedCobblestone.MOD_ID);

    public static final Supplier<MapCodec<? extends EnchantmentEntityEffect>> COMPRESSION =
            ENTITY_ENCHANTMENT_EFFECTS.register("compression", () -> CompressionEnchantmentEffect.CODEC);

    public static void register(IEventBus eventBus) {
        ENTITY_ENCHANTMENT_EFFECTS.register(eventBus);
    }
}
