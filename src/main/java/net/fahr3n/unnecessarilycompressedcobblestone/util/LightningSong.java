package net.fahr3n.unnecessarilycompressedcobblestone.util;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.VanillaLightningBoltEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;

/**
 * A tune, as lightning plays it: a list of strikes, each with the tick it falls on, how loud it is,
 * and which piano key it strikes.
 * <p>
 * It began without that last part, and the first song here is still written without it. Lightning
 * has no pitch of its own - vanilla rolls a fresh random one for every bolt and nothing can set it -
 * so a song written for plain bolts is a rhythm and a dynamic line played on one note. The note
 * bolts are what gave it pitch: each of the eighty-eight carries a real sample of that key, so a
 * strike that names one is played on it and a strike that names none is the old, unpitched bolt.
 * <p>
 * Songs are plain text under {@code data/<namespace>/songs/<name>.txt}, one strike per line as
 * {@code tick volume [midi]}, where volume is 0 to 1 and {@code midi} is the key struck, 21 to 108 -
 * so a song is a datapack file rather than anything compiled in, and a pack can add or replace one
 * without touching the mod. A {@code length <ticks>} line sets how long the performance runs for;
 * without one it ends on its last strike. Blank lines and lines beginning with {@code #} are
 * ignored.
 */
public record LightningSong(List<Note> notes, int lengthTicks) {
    /**
     * One strike.
     *
     * @param tick   how long after the song starts it falls, in ticks
     * @param volume how loud, 0 to 1, where 1 is as loud as the sound engine will play anything
     * @param key    which piano key it strikes, 0 for A0 up to 87 for C8, or
     *               {@link VanillaLightningBoltEntity#NO_NOTE} for a bolt that plays thunder
     */
    public record Note(int tick, float volume, int key) {
    }

    /** An empty song plays nothing rather than crashing, which is what a bad file should do. */
    public static final LightningSong SILENCE = new LightningSong(List.of(), 0);

    /** Loaded songs, kept until the server stops. */
    private static final Map<ResourceLocation, LightningSong> CACHE = new HashMap<>();

    /**
     * The song under {@code data/<namespace>/songs/<path>.txt}, read once and kept. A song that is
     * missing or unreadable comes back as {@link #SILENCE}, because a Lightning TNT that does nothing
     * is a better failure than one that takes the server down.
     */
    public static LightningSong get(MinecraftServer server, ResourceLocation name) {
        LightningSong cached = CACHE.get(name);
        if (cached != null) {
            return cached;
        }

        ResourceLocation path = ResourceLocation.fromNamespaceAndPath(name.getNamespace(),
                "songs/" + name.getPath() + ".txt");
        Optional<Resource> resource = server.getResourceManager().getResource(path);

        LightningSong song = resource.map(LightningSong::parse).orElse(SILENCE);
        if (song.notes().isEmpty()) {
            UnnecessarilyCompressedCobblestone.LOGGER.warn("Lightning song {} is missing or empty", path);
        }

        CACHE.put(name, song);
        return song;
    }

    /** Dropped on shutdown, so an edited song is picked up next time the server runs. */
    public static void clearCache() {
        CACHE.clear();
    }

    private static LightningSong parse(Resource resource) {
        List<Note> notes = new ArrayList<>();
        int length = 0;

        try (BufferedReader reader = resource.openAsReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                String[] parts = trimmed.split("[\s,]+");
                if (parts.length < 2) {
                    continue;
                }

                if (parts[0].equalsIgnoreCase("length")) {
                    length = Math.max(length, Integer.parseInt(parts[1]));
                    continue;
                }

                int tick = Integer.parseInt(parts[0]);
                float volume = Mth.clamp(Float.parseFloat(parts[1]), 0.0F, 1.0F);

                // A line without a key is the older, pitchless kind of strike, and both kinds may
                // sit in one file: a song can be a tune over a rhythm played in thunder.
                int key = parts.length > 2
                        ? PianoNote.fromMidi(Integer.parseInt(parts[2]))
                        : VanillaLightningBoltEntity.NO_NOTE;

                notes.add(new Note(tick, volume, key));
                length = Math.max(length, tick);
            }
        } catch (Exception exception) {
            UnnecessarilyCompressedCobblestone.LOGGER.warn("Could not read a lightning song", exception);
            return SILENCE;
        }

        notes.sort((left, right) -> Integer.compare(left.tick(), right.tick()));
        return new LightningSong(List.copyOf(notes), length);
    }
}
