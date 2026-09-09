package net.fahr3n.unnecessarilycompressedcobblestone.component;

import java.util.List;
import java.util.function.UnaryOperator;

import com.mojang.serialization.Codec;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Augment;
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

    /**
     * The augments fitted to a Ray of Laser, in the order they went on.
     * <p>
     * The same shape as {@link #ENGRAVINGS} and for the same reasons - a list rather than a set, and
     * dropped entirely when the last one comes off so a stripped laser stacks with one that was
     * never augmented. What keeps it to one of each kind is {@code Augments}, which reads the
     * augment's family rather than the augment itself: two levels of Damage are two different
     * constants and must still never be on one weapon at once.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<Augment>>> AUGMENTS =
            register("augments", builder -> builder
                    .persistent(Augment.CODEC.listOf())
                    .networkSynchronized(ByteBufCodecs.stringUtf8(64)
                            .map(Augment::byName, Augment::getSerializedName)
                            .apply(ByteBufCodecs.list())));

    /**
     * The song written on a Composition Bolt: one code per square of the sheet, a piano key or a
     * rest. See {@code Composition} for what the numbers mean - they are kept as plain integers so
     * the component is as cheap to sync as it is to save, and so a datapack or a command can write
     * one by hand.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<Integer>>> COMPOSITION =
            register("composition", builder -> builder
                    .persistent(Codec.INT.listOf())
                    .networkSynchronized(ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list())));

    private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(
            String name, UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
        return DATA_COMPONENT_TYPES.register(name, () -> builderOperator.apply(DataComponentType.builder()).build());
    }

    public static void register(IEventBus eventBus) {
        DATA_COMPONENT_TYPES.register(eventBus);
    }
}
