package net.fahr3n.unnecessarilycompressedcobblestone.datagen;

import java.util.concurrent.CompletableFuture;

import net.fahr3n.unnecessarilycompressedcobblestone.potion.ModMobEffects;
import net.fahr3n.unnecessarilycompressedcobblestone.util.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

/**
 * The mod's own mob effect tags, which today is the one the Compression Bomb reads.
 * <p>
 * Vanilla ships no mob effect tags at all and nothing on a {@code MobEffect} says whether it hurts
 * its holder over time, so this is the only place that question can be answered - and a tag is the
 * right shape for it, because it is a question another mod's poison should be able to answer for
 * itself from a datapack. See {@code DamageOverTime}, which is the other half: this file says which
 * effects count and that class works out what each one is worth.
 */
public class ModMobEffectTagProvider extends TagsProvider<MobEffect> {
    public ModMobEffectTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Registries.MOB_EFFECT, lookupProvider);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // Vanilla's two, which are the shape everything else in here is modelled on: a point of
        // damage on a clock, with the clock's interval halving per level.
        tag(ModTags.MobEffects.DAMAGE_OVER_TIME)
                .add(MobEffects.POISON.unwrapKey().orElseThrow())
                .add(MobEffects.WITHER.unwrapKey().orElseThrow());

        // This mod's own. Every one of them hurts on a clock and does nothing else worth keeping, so
        // taking it off costs its holder nothing but the damage the bomb has just charged them for.
        //
        // Deliberately absent: BURNING, which sets fire rather than dealing damage - the burn is
        // vanilla's and would go on after the effect was taken, so the bomb would charge for
        // something it had not stopped. ZOOM and MENDING, which are not harmful at all.
        // COMPRESSED_INFESTATION, whose damage is silverfish and not a clock. GHASTED, whose damage
        // is a flock of real mini ghasts that outlive the effect. And every BossSpoil, which are
        // flags rather than effects.
        for (var effect : java.util.List.of(
                ModMobEffects.FREEZING,
                ModMobEffects.DETONATION,
                ModMobEffects.COLLAPSE,
                ModMobEffects.STORMSTRUCK,
                ModMobEffects.DRAGON_BREATH,
                ModMobEffects.BLEEDING,
                // Corrosion's clocks are not in activeEffects, so what this membership buys is the
                // *permission* to collapse them: DamageOverTime reads the clocks directly and only
                // if this effect is in this tag - so a pack that takes it out gets a bomb that
                // neither charges for corrosion nor clears it.
                ModMobEffects.CORROSION)) {
            tag(ModTags.MobEffects.DAMAGE_OVER_TIME).add(effect.unwrapKey().orElseThrow());
        }
    }
}
