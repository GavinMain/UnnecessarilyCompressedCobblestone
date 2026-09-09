package net.fahr3n.unnecessarilycompressedcobblestone.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Every named Teleportation Gate in the world, and where it is.
 * <p>
 * A gate is reached by <em>name</em> rather than by coordinates, so something has to hold the map
 * from one to the other, and it cannot be the gates themselves: the destination is very often in a
 * chunk - or a dimension - that is not loaded, and walking every gate in the world to find one by
 * name would mean loading all of them. This is that map, and it is a {@link SavedData} on the
 * overworld's storage rather than per-level so that a gate in the Nether and a gate in the End are
 * in the same book.
 * <p>
 * Names are matched without regard to case, which is the only rule that is not surprising when the
 * name has to be typed in again at the other end. The name as it was typed is kept on the gate
 * itself for the screen to show; what is stored here is the key it is found by.
 */
public class TeleporterNetwork extends SavedData {
    /** The file the map is saved under, in the overworld's {@code data} folder. */
    private static final String FILE = "ucc_teleporter_gates";

    private static final String GATES = "gates";
    private static final String NAME = "name";
    private static final String DIMENSION = "dimension";
    private static final String X = "x";
    private static final String Y = "y";
    private static final String Z = "z";

    /** Where a named gate is. The dimension is a key rather than a level, since it is saved. */
    public record Gate(ResourceKey<Level> dimension, BlockPos pos) {
    }

    private final Map<String, Gate> gates = new HashMap<>();

    public static TeleporterNetwork get(ServerLevel level) {
        // The overworld's storage rather than the level's own: one book for every dimension, so a
        // gate can send to a gate anywhere.
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(TeleporterNetwork::new, TeleporterNetwork::load), FILE);
    }

    /** The form a name is looked up by. Empty means "this gate has no name and is not in the book". */
    public static String key(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    public Optional<Gate> find(String name) {
        String key = key(name);
        return key.isEmpty() ? Optional.empty() : Optional.ofNullable(gates.get(key));
    }

    /**
     * Puts {@code pos} in the book under {@code name}, replacing whatever was there.
     * <p>
     * Two gates may be given the same name; the later one wins, which is the same rule a second
     * gate given an existing name would run into however it was resolved, and refusing it would
     * mean a gate that silently is not where its own screen says it is.
     */
    public void bind(String name, ResourceKey<Level> dimension, BlockPos pos) {
        String key = key(name);
        if (key.isEmpty()) {
            return;
        }

        gates.put(key, new Gate(dimension, pos.immutable()));
        setDirty();
    }

    /**
     * Takes {@code name} out of the book, but only if it still points at {@code pos}. The guard is
     * what makes a gate safe to break after another gate has taken its name: the one that broke was
     * not the one in the book any more, so it has nothing to remove.
     */
    public void unbind(String name, BlockPos pos) {
        String key = key(name);
        Gate gate = gates.get(key);
        if (gate != null && gate.pos().equals(pos)) {
            gates.remove(key);
            setDirty();
        }
    }

    /**
     * Whether the book already holds {@code name} for somewhere other than {@code pos}. The gate
     * asks this on load so that a gate coming back out of a chunk does not quietly steal a name a
     * later gate has since taken.
     */
    public boolean claimedByOther(String name, ResourceKey<Level> dimension, BlockPos pos) {
        return find(name).filter(gate -> !gate.dimension().equals(dimension) || !gate.pos().equals(pos)).isPresent();
    }

    private static TeleporterNetwork load(CompoundTag tag, HolderLookup.Provider registries) {
        TeleporterNetwork network = new TeleporterNetwork();
        ListTag list = tag.getList(GATES, Tag.TAG_COMPOUND);

        for (int index = 0; index < list.size(); index++) {
            CompoundTag entry = list.getCompound(index);
            ResourceLocation dimension = ResourceLocation.tryParse(entry.getString(DIMENSION));
            if (dimension == null) {
                continue;
            }

            network.gates.put(entry.getString(NAME), new Gate(
                    ResourceKey.create(Registries.DIMENSION, dimension),
                    new BlockPos(entry.getInt(X), entry.getInt(Y), entry.getInt(Z))));
        }

        return network;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();

        gates.forEach((key, gate) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString(NAME, key);
            entry.putString(DIMENSION, gate.dimension().location().toString());
            entry.putInt(X, gate.pos().getX());
            entry.putInt(Y, gate.pos().getY());
            entry.putInt(Z, gate.pos().getZ());
            list.add(entry);
        });

        tag.put(GATES, list);
        return tag;
    }
}
