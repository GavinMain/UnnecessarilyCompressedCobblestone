package net.fahr3n.unnecessarilycompressedcobblestone.component;

import java.util.List;
import java.util.function.UnaryOperator;

import com.mojang.serialization.Codec;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Engraving;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.createDataComponents(UnnecessarilyCompressedCobblestone.MOD_ID);

    /**
     * The Compression Energy on a piece of gear, held as its base 10 logarithm.
     * <p>
     * The energy itself runs to 9^255, which is a 244 digit number, so it is never stored as a
     * number: one double holds the logarithm of any of it exactly as cheaply as it holds the
     * logarithm of 1, sums are a couple of floating point operations, and the digit count the gear
     * actually cares about falls straight out of the exponent. The trade is precision in the
     * leading digits, which nothing here needs.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> COMPRESSION_ENERGY =
            register("compression_energy", builder -> builder.persistent(Codec.DOUBLE).networkSynchronized(ByteBufCodecs.DOUBLE));

    /**
     * The engravings cut into a piece of gear, in the order they went on.
     * <p>
     * A list rather than a set because the Engraving Table gives them back last first, which is the
     * only ordering a player ever sees; {@code Engravings} is what keeps it to one of each kind. The
     * component is dropped entirely when the last engraving comes off, so stripped gear stacks with
     * gear that was never engraved.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<Engraving>>> ENGRAVINGS =
            register("engravings", builder -> builder
                    .persistent(Engraving.CODEC.listOf())
                    .networkSynchronized(ByteBufCodecs.stringUtf8(64)
                            .map(Engraving::byName, Engraving::getSerializedName)
                            .apply(ByteBufCodecs.list())));

    private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(
            String name, UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
        return DATA_COMPONENT_TYPES.register(name, () -> builderOperator.apply(DataComponentType.builder()).build());
    }

    public static void register(IEventBus eventBus) {
        DATA_COMPONENT_TYPES.register(eventBus);
    }
}
