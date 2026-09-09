package net.fahr3n.unnecessarilycompressedcobblestone.datagen;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.fahr3n.unnecessarilycompressedcobblestone.damage.ModDamageTypes;
import net.fahr3n.unnecessarilycompressedcobblestone.util.ModTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;

/**
 * The mod's own damage type tags, which today is the one the Compression Magic set reads.
 * <p>
 * It is a tag rather than a list in code for the usual reason: another mod's potion damage joins it
 * from a datapack, and a pack that disagrees about what counts as a potion doing it can say so
 * without touching this mod.
 */
public class ModDamageTypeTagProvider extends TagsProvider<DamageType> {
    public ModDamageTypeTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Registries.DAMAGE_TYPE, lookupProvider);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // Harming and poison both arrive as magic; indirect_magic is the same thing thrown by
        // somebody. Wither, the dragon's breath and freezing are the other three a potion is the
        // ordinary way to meet - the last of them this mod's own brew.
        tag(ModTags.DamageTypes.POTION_DAMAGE).add(
                DamageTypes.MAGIC,
                DamageTypes.INDIRECT_MAGIC,
                DamageTypes.WITHER,
                DamageTypes.DRAGON_BREATH,
                DamageTypes.FREEZE)
                .add(ModDamageTypes.CORROSION);

        // What "one point of true damage" actually means: every vanilla tag that says a form of
        // protection does not apply. Armour, shields, Resistance, Protection and this mod's own set
        // bonuses all key off these, so putting the type in all five is the whole implementation.
        //
        // The two bypass tags left out are as deliberate as the five in. bypasses_cooldown would let
        // every fish in a swarm land a hit every tick, which is not one damage but death;
        // bypasses_invulnerability would reach a creative player and make /kill the only way out of
        // a room full of them.
        for (TagKey<DamageType> bypass : List.of(DamageTypeTags.BYPASSES_ARMOR,
                DamageTypeTags.BYPASSES_SHIELD, DamageTypeTags.BYPASSES_EFFECTS,
                DamageTypeTags.BYPASSES_RESISTANCE, DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
            tag(bypass).add(ModDamageTypes.COMPRESSED_SILVERFISH);
        }

        // The Compressed Husk's blow: a mob attack that does not shove. no_knockback is the whole
        // of what makes it one - the push is applied by whatever was hit rather than by the mob, and
        // that tag is the only thing it asks. panic_causes is there because minecraft:mob_attack is
        // in it and nothing else, so an animal still bolts when the boss connects; every other tag
        // is deliberately left alone, since the point is a hit that armour and Protection answer in
        // the ordinary way.
        // Corrosion, and the one tag that makes it what it is. Being hit gives twenty ticks of
        // invulnerability and a second hit inside it only lands the difference; a corrosion clock
        // has to land its whole hundred whenever it comes due, and two clocks coming due together
        // have to land twice, or "independent instances" would be a promise the damage path breaks.
        // Nothing else is added: bypasses_invulnerability would reach a creative player, and the
        // other four bypasses would take away every answer to a number this size.
        tag(DamageTypeTags.BYPASSES_COOLDOWN).add(ModDamageTypes.CORROSION);

        // The Omni Slash's cut: the same five tags again, plus bypasses_cooldown. The extra tag is
        // right here and wrong on the silverfish for a reason of shape rather than of strength - a
        // slash touches any one creature exactly once, so the tag cannot let it hit twice, and all
        // it does is keep the cut from being swallowed by the invulnerability window a swing a
        // moment earlier opened. bypasses_invulnerability is left out the way it is everywhere else.
        for (TagKey<DamageType> bypass : List.of(DamageTypeTags.BYPASSES_ARMOR,
                DamageTypeTags.BYPASSES_SHIELD, DamageTypeTags.BYPASSES_EFFECTS,
                DamageTypeTags.BYPASSES_RESISTANCE, DamageTypeTags.BYPASSES_ENCHANTMENTS,
                DamageTypeTags.BYPASSES_COOLDOWN)) {
            tag(bypass).add(ModDamageTypes.OMNI_SLASH);
        }

        // The dev block's blow: the Omni Slash's six tags again, and for the same two reasons. The
        // five bypasses are the whole of "999 true damage" - armour, shields, Resistance, Protection
        // and this mod's set bonuses all key off them - and bypasses_cooldown is what makes it
        // "per hit", since a block is swung four times a second into a twenty-tick invulnerability
        // window and every blow after the first would otherwise land the difference, which is zero.
        for (TagKey<DamageType> bypass : List.of(DamageTypeTags.BYPASSES_ARMOR,
                DamageTypeTags.BYPASSES_SHIELD, DamageTypeTags.BYPASSES_EFFECTS,
                DamageTypeTags.BYPASSES_RESISTANCE, DamageTypeTags.BYPASSES_ENCHANTMENTS,
                DamageTypeTags.BYPASSES_COOLDOWN)) {
            tag(bypass).add(ModDamageTypes.DEV_STRIKE);
        }

        // Bleeding, and the one tag it wants: armour does not answer a cut that is already open.
        // Everything else still does, which matters because the damage is not the point of the
        // effect - the healing lock is, and no damage tag has anything to say about that.
        tag(DamageTypeTags.BYPASSES_ARMOR).add(ModDamageTypes.BLEEDING);

        // A scream is fire, and that one tag is the whole counterplay to the Ghasted effect: Fire
        // Resistance refuses it outright, through the check LivingEntity makes before anything else
        // in hurt. Nothing else is added - armour and Protection are meant to answer a scream too.
        tag(DamageTypeTags.IS_FIRE).add(ModDamageTypes.GHAST_SCREAM);

        // The Singularity's blast: an explosion in every respect the game asks about. It is in the
        // two tags vanilla's own explosion is in and in no bypasses_* tag, so armour, Protection,
        // Blast Protection and Resistance all still answer it - what makes it worth its own type is
        // only that something can now refuse it by name, which is what the dragon's second phase
        // does about the one it fires itself.
        tag(DamageTypeTags.IS_EXPLOSION).add(ModDamageTypes.SINGULARITY);
        tag(DamageTypeTags.DAMAGES_HELMET).add(ModDamageTypes.SINGULARITY);

        tag(DamageTypeTags.NO_KNOCKBACK).add(ModDamageTypes.COMPRESSED_HUSK);
        tag(DamageTypeTags.PANIC_CAUSES).add(ModDamageTypes.COMPRESSED_HUSK);

        // The mod's own name for its true damage: exactly the three types above that are in all
        // five bypasses_* tags. It exists because "gets past every form of protection" and "gets
        // past a boss that refuses damage outright" are different questions, and no vanilla tag
        // answers the second - see ModTags.DamageTypes.TRUE_DAMAGE and SelectiveImmunity.
        tag(ModTags.DamageTypes.TRUE_DAMAGE).add(ModDamageTypes.OMNI_SLASH, ModDamageTypes.DEV_STRIKE,
                ModDamageTypes.COMPRESSED_SILVERFISH);
    }
}
