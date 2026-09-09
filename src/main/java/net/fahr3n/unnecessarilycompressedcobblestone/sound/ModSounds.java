package net.fahr3n.unnecessarilycompressedcobblestone.sound;

import java.util.ArrayList;
import java.util.List;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.util.PianoNote;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The sounds the mod adds, which today is one grand piano.
 * <p>
 * Every key of it is a real sample rather than a pitched copy of one, and it has to be: the sound
 * engine clamps playback pitch to between half and double, so no single sample can cover more than
 * two of the seven and a quarter octaves a piano has. The eighty-eight of them are synthesised by
 * {@code tools/build_note_bolts.py}, which writes the {@code .ogg} files and the {@code sounds.json}
 * that names them together, so nothing here is edited by hand either.
 */
public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, UnnecessarilyCompressedCobblestone.MOD_ID);

    /** One per piano key, indexed by {@link PianoNote}'s key number: 0 is A0 and 87 is C8. */
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> NOTES = registerNotes();

    private static List<DeferredHolder<SoundEvent, SoundEvent>> registerNotes() {
        List<DeferredHolder<SoundEvent, SoundEvent>> notes = new ArrayList<>(PianoNote.KEYS);

        for (int key = 0; key < PianoNote.KEYS; key++) {
            String name = "note." + PianoNote.name(key);
            notes.add(SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, name))));
        }

        return List.copyOf(notes);
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
