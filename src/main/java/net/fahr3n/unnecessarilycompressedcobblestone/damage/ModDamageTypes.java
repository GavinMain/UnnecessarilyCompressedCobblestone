package net.fahr3n.unnecessarilycompressedcobblestone.damage;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;

/**
 * The mod's own damage types. Like enchantments, these are a datapack registry in 1.21 rather than
 * code, so they are written by {@code ModDatapackProvider} and reached through a level's
 * {@code registryAccess()}.
 * <p>
 * A real registered type rather than a bare number is what lets everything else in the game have an
 * opinion about it: armour, Protection, Resistance, this mod's own set bonuses and any other mod's
 * reduction all key off damage type tags, so what a type <em>is</em> is entirely the tags it is put
 * in - see {@code ModDamageTypeTagProvider}.
 */
public class ModDamageTypes {
    /**
     * What a Compressed Silverfish bites for. It is one point of damage that nothing reduces: the
     * tag provider puts it in every {@code bypasses_*} tag that means "protection does not apply",
     * so armour, shields, Resistance, Protection and the mod's own sets all miss it.
     * <p>
     * Deliberately not in {@code #minecraft:bypasses_cooldown} or
     * {@code #minecraft:bypasses_invulnerability}. The first would let a swarm land one hit per fish
     * per tick, which is not "one true damage" but death; the second would reach a creative player
     * and would make {@code /kill} the only way out of a room full of them.
     */
    public static final ResourceKey<DamageType> COMPRESSED_SILVERFISH = key("compressed_silverfish");

    /**
     * What a Compressed Husk hits for, which is vanilla's own {@code minecraft:mob_attack} in every
     * respect but one: it is in {@code #minecraft:no_knockback}, so the blow does not push its
     * target.
     * <p>
     * That has to be a damage type rather than a number or a flag, because the shove is not the
     * attacker's doing at all - {@code LivingEntity#hurt} applies it to whatever was hit, gated on
     * exactly that tag, and there is nothing on the mob's side to switch off.
     * {@code Attributes.ATTACK_KNOCKBACK} is the <em>extra</em> knockback on top of it and is
     * already zero on a zombie.
     * <p>
     * Everything else is copied deliberately. The message id is vanilla's own {@code mob}, so the
     * death message reads the way being killed by any other mob does; the exhaustion and the scaling
     * are a mob attack's, so hunger and difficulty still mean what they usually mean; and the type
     * is in no {@code bypasses_*} tag at all, so armour, Protection, Resistance and every other
     * mod's reduction apply to it exactly as they would to a bite.
     */
    public static final ResourceKey<DamageType> COMPRESSED_HUSK = key("compressed_husk");

    /**
     * What corrosion eats through with. It is an ordinary damage type in every respect but one:
     * {@code #minecraft:bypasses_cooldown}, which is what "not affected by the immunity you get from
     * being hit" means in code.
     * <p>
     * That one tag is doing two separate jobs. A clock coming due a tick after a creeper went off
     * lands its full hundred rather than the difference; and two clocks coming due on the same tick
     * both land, which is the whole of what makes corrosion's instances independent - without it
     * {@code LivingEntity#hurt} would keep the larger and throw the rest away.
     * <p>
     * Deliberately in no {@code bypasses_*} tag beyond that. A hundred points a level is enormous
     * and armour, Protection, Resistance and the Compression Magic set are meant to have something
     * to say about it; what none of them can do is stop the clock.
     */
    public static final ResourceKey<DamageType> CORROSION = key("corrosion");

    /**
     * What bleeding costs on the way out. It is in {@code #minecraft:bypasses_armor} and nothing
     * else: a cut that is already open is not answered by the plate over it, but Resistance, a
     * shield's absence and every other reduction that is not armour still apply.
     * <p>
     * The damage is deliberately the small half of the effect. What bleeding is really for is in
     * {@code ModEvents}, which cancels every heal on anything carrying it.
     */
    public static final ResourceKey<DamageType> BLEEDING = key("bleeding");

    /**
     * What an Omni Slash cuts with, and the whole of what "true damage that cannot be blocked"
     * means: the tag provider puts it in every {@code bypasses_*} tag that says a form of
     * protection does not apply, so armour, shields, Resistance, Protection and this mod's own set
     * bonuses all miss it.
     * <p>
     * It is also in {@code #minecraft:bypasses_cooldown}, which the Compressed Silverfish's true
     * damage deliberately is not, and the difference is the shape of the two attacks. A swarm of
     * fish landing a hit each per tick is not one point of true damage but death; a slash hits any
     * one creature exactly once and is on a minute-long lockout, so the only thing that tag can do
     * here is stop the blow being swallowed by the invulnerability left over from the swing that
     * preceded it - which, since the slash is worth exactly one standard attack, would otherwise
     * mean it landed nothing at all.
     * <p>
     * Not in {@code #minecraft:bypasses_invulnerability}, for the reason nothing here is: it would
     * reach a creative player.
     */
    public static final ResourceKey<DamageType> OMNI_SLASH = key("omni_slash");

    /**
     * What a mini ghast's scream costs. It is an ordinary damage type in every respect but one:
     * {@code #minecraft:is_fire}, which is what makes Fire Resistance the answer to it.
     * <p>
     * That tag is the whole of the design. Ghasted is applied by fire charges and is measured in
     * screams that go on arriving long after the boss has stopped shooting, so it needs a counter
     * that is neither armour nor a dodge - and a fire potion is a thing a player already has, is
     * already carrying to a fight about fireballs, and can be drunk mid-flock. Everything else still
     * applies as usual: armour, Protection and Resistance all answer a scream in the ordinary way.
     */
    public static final ResourceKey<DamageType> GHAST_SCREAM = key("ghast_scream");

    /**
     * What a Singularity TNT's finishing blast kills with, in place of vanilla's
     * {@code minecraft:explosion}.
     * <p>
     * A type of its own for one reason: it is the only blast in the game worth several hundred
     * thousand points, and something has to be able to say "not that one" about it without also
     * saying it about every creeper. The Compressed Dragon's second phase is that something - it
     * fires one of these itself, and a boss killed by its own rare attack is a fight that ends by
     * accident.
     * <p>
     * Everything else is vanilla's explosion character for character: {@code #is_explosion} and
     * {@code #damages_helmet}, and no {@code bypasses_*} tag at all, so armour, Protection,
     * Resistance, blast protection and every other mod's answer to an explosion apply to it exactly
     * as they would to TNT. The scaling is {@code ALWAYS}, which is vanilla's for explosions, so
     * difficulty still means what it means.
     */
    public static final ResourceKey<DamageType> SINGULARITY = key("singularity");

    /**
     * What the Dev Compressed Cobblestone hits with: 999 points that nothing in the game reduces.
     * It is in the same five {@code bypasses_*} tags as the Omni Slash's cut, plus
     * {@code #minecraft:bypasses_cooldown}, and that last one is what "per hit" means - a block is
     * swung several times inside one twenty-tick invulnerability window, and without it every blow
     * after the first would land only the difference between 999 and 999, which is nothing.
     * <p>
     * Not in {@code #minecraft:bypasses_invulnerability}, for the reason nothing in this mod is: it
     * would reach a creative player, and this is a tool a creative player is holding.
     */
    public static final ResourceKey<DamageType> DEV_STRIKE = key("dev_strike");

    /**
     * What a black hole's core does to whatever reaches it. True damage - the five
     * {@code bypasses_*} tags and {@code #ucc:true_damage} - because nothing is armoured against
     * being swallowed, and a boss that refuses kinds of attack has nothing to say about this one
     * either. Not in {@code bypasses_cooldown}: it bites every ten ticks, which is exactly when
     * the invulnerability window stops swallowing a repeat of the same figure.
     */
    public static final ResourceKey<DamageType> BLACK_HOLE = key("black_hole");

    public static void bootstrap(BootstrapContext<DamageType> context) {
        // Exhaustion 0.1 is vanilla's figure for an ordinary mob attack; the scaling is the one
        // vanilla gives mob damage, so difficulty still means what it usually does.
        context.register(COMPRESSED_SILVERFISH, new DamageType("compressedSilverfish",
                DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1F, DamageEffects.HURT));

        // Vanilla's mob_attack, character for character - `new DamageType("mob", 0.1F)`. The whole
        // of the difference is which tags it is in.
        context.register(COMPRESSED_HUSK, new DamageType("mob", 0.1F));

        // No attacker is ever recorded - the clock does this, not whatever started it - so the
        // death message is the bare "%s corroded away" and there is no "whilst fighting" form.
        context.register(CORROSION, new DamageType("corrosion", 0.1F));

        // Exhaustion is a little above a mob attack's: bleeding costs the food that would have
        // healed it, which is the same statement the effect makes in a second way.
        context.register(BLEEDING, new DamageType("bleeding", 0.2F));

        // A player's own blow, sent on ahead, so it is scaled and exhausted the way a player attack
        // is - the crescent is the swing rather than a thing thrown by it.
        // No attacker is recorded on the type itself - the mini ghast passes itself as the direct
        // entity and the boss as the cause, so both death message forms work.
        context.register(GHAST_SCREAM, new DamageType("ghastScream", 0.1F));

        context.register(OMNI_SLASH, new DamageType("omniSlash",
                DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1F, DamageEffects.HURT));

        // Vanilla's own explosion, character for character - `new DamageType("explosion",
        // DamageScaling.ALWAYS, 0.1F)`. Only the name and the one thing that can refuse it differ.
        context.register(SINGULARITY, new DamageType("singularity", DamageScaling.ALWAYS, 0.1F));

        // A player's blow with a block in their hand: a player attack's exhaustion, and no
        // difficulty scaling, because a dev tool that hits for less on peaceful is not a dev tool.
        context.register(DEV_STRIKE, new DamageType("devStrike", DamageScaling.NEVER, 0.1F));

        context.register(BLACK_HOLE, new DamageType("blackHole", DamageScaling.NEVER, 0.0F));
    }

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE,
                ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, name));
    }
}
