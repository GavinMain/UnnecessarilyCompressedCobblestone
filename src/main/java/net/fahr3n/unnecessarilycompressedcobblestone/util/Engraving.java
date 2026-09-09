package net.fahr3n.unnecessarilycompressedcobblestone.util;

import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;

import net.fahr3n.unnecessarilycompressedcobblestone.item.ModItems;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.BoltLauncherItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedArrowStaffItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedArrowTntStaffItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedCobblestoneArmorItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedCobblestoneBowItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedKatanaItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedLightningStaffItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedScytheItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedSpearItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedSummoningStaffItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.RayOfLaserItem;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.Tags;

/**
 * The engravings, and the one thing that separates them: what each may be cut into.
 * <p>
 * An engraving is an item a player crafts and an Engraving Table then moves onto a piece of gear,
 * where it lives as a {@code ModDataComponents.ENGRAVINGS} entry rather than as an enchantment. That
 * is the whole reason the system exists next to enchanting: an engraving comes back off the gear the
 * way it went on, with no anvil, no experience and no losing the thing.
 * <p>
 * What an engraving actually does is never in this enum - it is in whatever reads the component
 * ({@code ModEvents} for most of them, the staff itself for {@link #SPIRAL}). All that lives here is
 * the name it is saved under and the test for what it fits, and the test is written against item
 * tags wherever it can be, so another mod's helmet or bow is engravable without this file changing.
 */
public enum Engraving implements StringRepresentable {
    /**
     * The one engraving that is not a stat: it changes what an Arrow TNT Staff rains. Only that
     * staff has anywhere to put it, which is why this is the one applicability test written against
     * a class rather than a tag - the behaviour it swaps out is that item's own.
     */
    SPIRAL("spiral", 85, stack -> stack.getItem() instanceof CompressedArrowTntStaffItem),

    /** Night vision while it is worn, and the wearer's arrows light up what they hit. */
    VISION("vision", 87, stack -> stack.is(ItemTags.HEAD_ARMOR)),

    /** A slice off every projectile that lands, counted once per engraved piece worn. */
    PROJECTILE_PROTECTION("projectile_protection", 88, Engraving::isArmor),

    /** The same, for lightning. */
    LIGHTNING_PROTECTION("lightning_protection", 97, Engraving::isArmor),

    /** Part of the damage a melee weapon deals comes back as health. */
    LIFESTEAL("lifesteal", 91, stack -> stack.is(Tags.Items.MELEE_WEAPON_TOOLS)),

    /**
     * The Compressed Cobblestone Bow comes to full draw in three quarters of the time. This one is
     * deliberately not written against the bow tag: the draw it shortens is four times a vanilla
     * bow's, so a quarter off it is worth fifteen ticks here and five on anything else, and the
     * engraving is priced for the former.
     */
    QUICK_DRAW("quick_draw", 92, stack -> stack.getItem() instanceof CompressedCobblestoneBowItem),

    /**
     * Twice the damage and twice the carry out of every bolt a Bolt Launcher fires. Like the Spiral
     * it is written against a class rather than a tag, and for the same reason: what it doubles is
     * that weapon's own strike, and no other bow in the game has one.
     */
    BOLT("bolt", 111, stack -> stack.getItem() instanceof BoltLauncherItem),

    /**
     * A chestplate that glides. It keeps everything the piece already did - it is still armour, still
     * inscribed, still part of whatever set it belongs to - and gains an elytra's flight on top.
     * <p>
     * This is the one engraving that could not be written against {@code #minecraft:chest_armor},
     * however much it should be. Gliding is granted by the chestplate itself answering NeoForge's
     * {@code canElytraFly}, and there is no way to make another mod's item answer anything - so an
     * elytra engraving on a foreign chestplate would go on, sit there and do nothing. It is offered
     * only where it works.
     */
    ELYTRA("elytra", 112, stack -> stack.getItem() instanceof CompressedCobblestoneArmorItem
            && stack.is(ItemTags.CHEST_ARMOR)),

    /**
     * A sword that finishes what the fire started: a blow landed on something already burning sets
     * off a blast worth exactly what the blow was worth, on everything standing near it.
     */
    EXPLOSION_SIGIL("explosion", 128, Kind.SIGIL, stack -> stack.is(ItemTags.SWORDS)),

    /**
     * Boots that shrug off lightning and move quickly. The reduction is the largest a single piece
     * of gear gives, and it does not stack with the Compression Lightning set's or with the
     * Lightning Protection engraving's - see {@code ModEvents}, where the strongest of whatever is
     * worn is the one that counts.
     */
    LIGHTNING_SIGIL("lightning", 130, Kind.SIGIL, stack -> stack.is(ItemTags.FOOT_ARMOR)),

    /**
     * A bow whose arrows carry a potion. The engraving is blank when it is crafted and is given its
     * effect by being put on an anvil with any potion - permanently, and only once, since there is
     * nothing that takes it back off.
     * <p>
     * It is the only engraving that carries data of its own. The gear's engraving component is a
     * list of these constants and nothing else, so the potion travels alongside it as an ordinary
     * {@code minecraft:potion_contents} on the bow, moved across when the engraving goes on and
     * moved back when it comes off - see {@code EngravingTableBlockEntity}.
     * <p>
     * Its core is the Compressed Witch's heart rather than a diamond, which is what keeps its grid
     * clear of the other engravings' whatever tier it is cut from.
     */
    POTION("potion", 132, Kind.ENGRAVING, () -> ModItems.TIER_10_COMPRESSED_HEART.get(),
            stack -> stack.is(Tags.Items.TOOLS_BOW)),

    /**
     * A block and an arm's length further to reach, on whatever is held. It is written against
     * {@code #c:tools}, which is NeoForge's whole tool family - every pickaxe, axe, shovel, hoe and
     * sword in the game, and every {@code #c:tools/...} subtag another mod hangs its own gear off -
     * so a foreign tool is engravable the day that mod tags it, which is exactly what the tag rule
     * is for. That family also takes in bows and shields, and they are deliberately left in rather
     * than picked out one subtag at a time: an extra block of reach on a bow does nothing at all,
     * so excluding them would buy nothing and cost the one-tag test.
     */
    REACH("reach", 134, stack -> stack.is(Tags.Items.TOOLS)),

    /**
     * A fishing rod whose catch, when what it catches is alive and does not survive it, is that
     * creature's spawn egg. The egg is looked up rather than listed - {@code SpawnEggItem.byId} is
     * vanilla's own map from an entity type to its egg - so a modded mob that ships an egg drops
     * that egg here, and one that ships none simply drops nothing extra.
     * <p>
     * It is the only engraving offered on a fishing rod, and a rod only kills anything at all
     * because of Hook and the inscriber, so this is the end of that line rather than a thing on its
     * own: a rod that hits hard enough to finish a mob turns it into an egg.
     */
    EGG("egg", 146, stack -> stack.is(Tags.Items.TOOLS_FISHING_ROD)),

    /**
     * A Summoning Staff that calls up Compressed Silverfish instead of a phantom.
     * <p>
     * Written against the class rather than a tag, for the same reason the Spiral is: what it
     * changes is that one item's own cast, and no other staff in the game has one. The swarm it
     * makes is the same mob the Silverfish TNT throws, with an owner on it - which turns its
     * targeting round into a wolf's, so it follows the caster, joins their fights and never touches
     * them. What each one is worth is still the staff's Compression Energy and Surge; how many
     * arrive is still Multicast.
     */
    SILVERFISH("silverfish", 151,
            stack -> stack.getItem() instanceof CompressedSummoningStaffItem),

    /**
     * Haste IV for as long as a Compressed Katana is held.
     * <p>
     * It is written against the class rather than a tag, and unlike the Reach engraving next to it
     * that is deliberate rather than a shortcut. Haste is a mining effect, so on any other sword in
     * the game it would be four levels of dig speed hidden on a weapon - the engraving is priced
     * for the one blade fast enough that swinging and digging read as the same motion, which is the
     * same reason the Quick Draw engraving is restricted to one bow rather than tagged to every bow
     * there is.
     */
    HASTE("haste", 158, stack -> stack.getItem() instanceof CompressedKatanaItem),

    /**
     * A mace that can be slammed. Right-clicking in mid-air fires the wearer at the ground and
     * everything around the landing takes damage rising in a straight line with how far the fall was,
     * with no ceiling on it -
     * see {@code ModEvents}, which owns both halves.
     * <p>
     * The tag is {@code #minecraft:enchantable/mace}, which is vanilla's own list of every mace in
     * the game and is what another mod's mace would be added to. There is no {@code #c:tools/mace},
     * and {@code #c:tools/melee_weapon} would put this on every sword there is - which the whole
     * thing is wrong for, since what it scales off is fall distance and only a mace is built around
     * that.
     */
    SMASH("smash", 161, stack -> stack.is(ItemTags.MACE_ENCHANTABLE)),

    /**
     * Jump Boost X while the leggings are worn. Written against {@code #minecraft:leg_armor}, so
     * another mod's leggings take it: nothing about the effect is this mod's, which is exactly the
     * case the tag rule is for.
     */
    JUMP("jump", 162, stack -> stack.is(ItemTags.LEG_ARMOR)),

    /**
     * A katana that closes the distance: right-clicking carries the wielder five blocks forward and
     * cuts everything on the way through - see {@code ModEvents}, which owns it.
     * <p>
     * Written against the class rather than a tag, like the Haste engraving it shares a weapon with,
     * and for the same reason: a dash on a mace or a pickaxe is a movement ability with a weapon
     * attached, while on the one blade in the mod built around reach and speed it is the third thing
     * that weapon already was.
     * <p>
     * Its core is the Compressed Creeper Tier 2's heart rather than a diamond, which is what keeps
     * its grid clear of the other engravings' whatever tier it is cut from - the same trick the
     * Potion engraving plays with the Compressed Witch's.
     */
    DASH("dash", 164, Kind.ENGRAVING, () -> ModItems.TIER_12_COMPRESSED_HEART.get(),
            stack -> stack.getItem() instanceof CompressedKatanaItem),

    /**
     * An Arrow Staff whose rain is tipped: every arrow that falls on the marked target carries an
     * effect drawn at random out of the harmful half of the effect registry.
     * <p>
     * It is the Spiral engraving's opposite number - that one changes what the Arrow TNT Staff
     * rains, this one changes what the Arrow Staff rains - and is written against that staff's class
     * for the same reason: what it swaps out is that one item's own cast, and no other staff in the
     * game has one. What may be drawn is never listed; {@code ArrowEffects} filters the registry on
     * {@link net.minecraft.world.effect.MobEffectCategory}, so another mod's curses are in the bag
     * and its blessings are not, by that mod's own declaration.
     */
    ARROW_2("arrow_2", 166, stack -> stack.getItem() instanceof CompressedArrowStaffItem),

    /**
     * A bow that takes ten seconds to draw and then throws its arrow flat and instantly.
     * <p>
     * Both halves are one trade. The draw is a *fixed* ten seconds rather than a fraction off
     * whatever the bow already had, which is why - unlike Quick Draw next to it - this one is
     * written against {@code #c:tools/bow} and reaches every bow in the game: a fixed figure means
     * the same thing on all of them, where a fraction is worth four times as much on this mod's own
     * bow as on a vanilla one. What comes off the string travels so fast that where it lands is
     * where it was pointed, which is what makes the Hyper Compressed Arrow - which falls out of the
     * air the moment it is loosed from anything else - worth firing.
     * <p>
     * See {@code ModEvents}, which owns both halves: the draw is the loose event's charge, and the
     * speed is put on the arrow as it joins the level.
     */
    SNIPER("sniper", 167, stack -> stack.is(Tags.Items.TOOLS_BOW)),

    /**
     * A Lightning Staff that stops calling lightning down and starts playing it. Every cast fires a
     * Composition Bolt written with Fur Elise, which flies where it was aimed and performs the piece
     * wherever it lands.
     * <p>
     * Written against the class rather than a tag, like the Spiral and the Silverfish engravings and
     * for the same reason: what it swaps out is that one staff's own cast. Its core is the Compressed
     * Composer's heart, which is what keeps its grid clear of every other engraving's - the same
     * trick the Potion and Dash engravings play - and is also the whole of how it is earned, since
     * the Composer is the boss that has to be beaten in music to give one up.
     */
    COMPOSITION("composition", 172, Kind.ENGRAVING, () -> ModItems.TIER_13_COMPRESSED_HEART.get(),
            stack -> stack.getItem() instanceof CompressedLightningStaffItem),

    /**
     * A katana that cuts the ground open: right-clicking carries the wielder forward the way the
     * Dash does and takes the world with it, breaking every block within
     * {@code VAPOR_SLASH_RADIUS} of the line travelled and dealing three swings' worth of damage to
     * everything caught in it. The blocks are mined rather than deleted - they drop what a pickaxe
     * would have got - see {@code ModEvents}, which owns both this and {@link #VAPOR_SLASH_2}.
     * <p>
     * Written against the class rather than a tag, like the Haste and Dash engravings it shares a
     * weapon with. It is the strongest reason of the three: this one hands out a tunnelling tool as
     * well as an attack, and a tag would hand it to every sword in every mod installed.
     * <p>
     * Its core is the Compressed Husk's heart, which is what keeps its grid clear of every other
     * engraving's whatever tier it is cut from - the same trick the Potion, Dash and Composition
     * engravings play. It shares that core with {@link #VAPOR_SLASH_2}, which is why those two are
     * the pair whose tiers have to stay apart.
     */
    VAPOR_SLASH("vapor_slash", 197, Kind.ENGRAVING, () -> ModItems.TIER_15_COMPRESSED_HEART.get(),
            stack -> stack.getItem() instanceof CompressedKatanaItem),

    /**
     * The same slash, cut deeper: five swings' worth instead of three, and a second's lockout
     * instead of ten.
     * <p>
     * The cooldown is the whole of the upgrade and the damage is the smaller half of it. Ten seconds
     * to one second is not a tenth of the wait, it is the difference between an opener and a way of
     * moving - a katana carrying this one crosses ground, digs and fights in the same motion. Which
     * is why it is thirty tiers deeper than the first rather than the usual few.
     * <p>
     * A katana carrying both slashes fires this one; see {@code ModEvents}, where the deeper of the
     * two always wins rather than the two racing for the same cooldown.
     */
    VAPOR_SLASH_2("vapor_slash_2", 227, Kind.ENGRAVING, () -> ModItems.TIER_15_COMPRESSED_HEART.get(),
            stack -> stack.getItem() instanceof CompressedKatanaItem),

    /**
     * A melee weapon that throws whatever it hits a hundred blocks straight up.
     * <p>
     * Written against {@code #c:tools/melee_weapon}, which is NeoForge's own family of every sword,
     * axe and hitting-thing in the game, so another mod's weapon takes it the day that mod tags it.
     * That is right here where it would be wrong on the Dash or the Haste: what this does is the
     * same on any weapon at all, because it does not scale with the weapon and does not change how
     * the weapon is used - it is a consequence of the hit landing rather than a thing the blade does.
     * <p>
     * What comes back down is the whole of the trade. A target thrown a hundred blocks is out of the
     * fight for eight seconds and then lands taking about ninety-seven points of fall damage, which
     * is enormous against anything ordinary and is <em>the</em> way into a Compressed Snow Golem,
     * which takes damage from nothing else. It is also how a fight gets away from you: what went up
     * is out of reach until it does not want to be.
     */
    UPPERCUT("uppercut", 202, stack -> stack.is(Tags.Items.MELEE_WEAPON_TOOLS)),

    /**
     * A sword that blows a hole in the world: right-clicking clears everything within five blocks,
     * up to and including tier 205 compressed cobblestone.
     * <p>
     * The depth limit is the whole design of it, and it is the reason the blast is not an explosion.
     * Every hardened compression level carries an explosion resistance of three and a half million,
     * so no explosion vanilla can make will ever move one and no amount of power would change that -
     * a real blast is fired for the damage, the shove and the noise, and the blocks are taken by
     * reading each one's compression level and comparing it against
     * {@code ModEvents.EXPLODING_SWORD_MAX_LEVEL}. Bedrock and the world's ceiling are not compressed
     * at all and are refused by their destroy speed, exactly as they are everywhere else here.
     * <p>
     * Nothing drops. This is a way of moving stone rather than of collecting it, and five blocks of
     * radius is five hundred blocks - dropping them would make one right-click the fastest hardened
     * stone mine in the game.
     */
    EXPLODING_SWORD("exploding_sword", 203, stack -> stack.is(ItemTags.SWORDS)),

    /**
     * One more projectile out of every shot, on any ranged weapon in the game.
     * <p>
     * It is written against {@code #c:tools/ranged_weapon}, which is NeoForge's own family - bows,
     * crossbows and whatever a mod hangs off it - and that is right here for the reason the Uppercut
     * is: what this does is the same on all of them, because it does not scale with the weapon and
     * does not change how the weapon is used. An extra arrow off a vanilla bow and an extra shell
     * off a TNT Launcher are the same sentence.
     * <p>
     * <em>Additive</em> is the whole of the pricing, and it is what keeps this from being a second
     * Multishot. A crossbow carrying Multishot fires three; engraved it fires four, not six. See
     * {@code ModEvents}, which owns it and explains why the extra shot is a copy of the one that was
     * fired rather than a change to how many the weapon draws.
     * <p>
     * The extra shot costs no extra ammunition, exactly as Multishot's two do.
     */
    EXTRA_SHOT("extra_shot", 216, stack -> stack.is(Tags.Items.RANGED_WEAPON_TOOLS)),

    /**
     * Two more, rather than one.
     * <p>
     * A weapon carrying both engravings fires the deeper of the two rather than three extra - see
     * {@code ModEvents}, where the two are a ladder and not a stack, the same rule the two Vapor
     * Slashes are read by. Fourteen tiers deeper than the first, which is the usual price of a
     * second rung and is honest here: the second shot is worth exactly what the first one was.
     */
    EXTRA_SHOT_2("extra_shot_2", 230, stack -> stack.is(Tags.Items.RANGED_WEAPON_TOOLS)),

    /**
     * Haste VI instead of Haste IV, on the same katana.
     * <p>
     * Two levels rather than a new ability, which is what a second rung of a stat engraving should
     * be. It is written against the class for exactly the reason {@link #HASTE} is - Haste on any
     * other sword in the game is a mining effect hidden on a weapon - and a katana carrying both
     * gets the deeper one rather than the sum, the same rule the two Vapor Slashes and the two
     * Extra Shots are read by.
     * <p>
     * Worth knowing before pricing a third rung: {@code Player#getDigSpeed} switches on the
     * amplifier and keeps rising, but the block being mined is what decides whether that matters,
     * and past the point where a block breaks in one tick another level buys nothing at all.
     */
    HASTE_2("haste_2", 219, stack -> stack.getItem() instanceof CompressedKatanaItem),

    /**
     * Fifty blocks of reach, on a pickaxe.
     * <p>
     * It is the {@link #REACH} engraving's own two attributes at fifty times the figure, and the
     * whole of the difference is what it is offered on: {@code #minecraft:pickaxes}, vanilla's own
     * list, so another mod's pickaxe takes it and no sword, bow or shield can. That restriction is
     * the pricing. Fifty blocks of block reach is a tunnel dug from where you are standing and is a
     * genuinely different way to mine; fifty blocks of it on a sword would be a melee weapon that
     * outranges every bow in the game.
     * <p>
     * Both interaction ranges are still moved together, the way the first Reach moves them - a tool
     * that can break a block fifty blocks off but not hit the creature standing on it reads as
     * broken. Fifty is also chosen against a ceiling rather than picked: both attributes are
     * {@code RangedAttribute}s that stop at 64, and a player's bases are 4.5 and 3.0, so this is
     * close to as far as the game will go and anything larger would be silently clamped.
     */
    SUPER_REACH("super_reach", 220, stack -> stack.is(ItemTags.PICKAXES)),

    /**
     * A scythe whose swing leaves the blade. Every melee attack becomes a crescent instead, which
     * flies until it hits something and then does exactly what the swing would have done.
     * <p>
     * "Exactly" is meant literally rather than as a description - the crescent calls the wielder's
     * own {@code Player#attack} on what it caught, so the damage, the enchantments, the critical
     * hit, the knockback, the Compression Energy and the scythe's own bleed all arrive without any
     * of them being restated. See {@code ScytheWaveEntity}, and {@code ModEvents}, which catches
     * the swing.
     * <p>
     * How far it reaches is not a number here: the crescent keeps flying until it stops being
     * ticked, which happens when it leaves the chunks the server keeps loaded around its players.
     * The range is therefore the view distance, and a server that draws further throws further.
     * <p>
     * Written against the class rather than a tag, and it is the least arguable of the class-bound
     * engravings: what this hands out is a ranged weapon with no ammunition and a melee weapon's
     * damage, and a tag would hand it to every axe in every mod installed. The swing still costs a
     * swing - firing spends the attack cooldown and the crescent lands at the charge it was loosed
     * at - which is the whole of what keeps it from being twenty free hits a second.
     */
    SCYTHE_WAVE("scythe_wave", 224, stack -> stack.getItem() instanceof CompressedScytheItem),

    /**
     * A weapon that throws its swing at the horizon and takes the world with it. Right-clicking
     * looses a crescent five blocks tall that flies until it stops being ticked, passing through
     * everything: it breaks every block it crosses short of bedrock, and cuts everything alive on
     * the way for one standard attack's worth of damage that nothing at all reduces.
     * <p>
     * Written against {@code #c:tools/melee_weapon}, NeoForge's own family of every sword, axe and
     * hitting-thing in the game, for the reason the Uppercut is: what this does is the same on all
     * of them, because it does not change how the weapon is used and what it hits for is read off
     * {@code Attributes.ATTACK_DAMAGE} - the weapon's own modifier, the player's base and everything
     * the inscriber added, already added up. A foreign weapon is engravable the day that mod tags
     * it and is priced correctly the same day.
     * <p>
     * It is the deepest right-click ability in the mod and it wins outright over every other one -
     * the Dash, both Vapor Slashes, the Exploding Sword and the Smash all stand down on a weapon
     * carrying it, the same "deeper rung wins rather than the two racing" rule the Vapor Slashes and
     * the Extra Shots are read by. That matters here in a way it does not there, because this is the
     * first ability written against a family broad enough to share a weapon with any of them.
     * <p>
     * See {@code OmniSlashEntity}, which owns the flight, the carve and the cut, and
     * {@code ModDamageTypes.OMNI_SLASH}, which is what "cannot be blocked" actually means: a real
     * damage type in every {@code bypasses_*} tag that says a form of protection does not apply.
     */
    OMNI_SLASH("omni_slash", 228, stack -> stack.is(Tags.Items.MELEE_WEAPON_TOOLS)),

    /**
     * A Ray of Laser whose effect augments deepen what is already there instead of restarting it.
     * <p>
     * Every effect augment presses its effect for {@code RayOfLaserItem.EFFECT_TICKS} at level I,
     * and holding the beam on something simply keeps handing it those ten seconds back. Engraved,
     * a beam that lands on a target already carrying one of its effects raises that effect a level
     * and leaves the clock exactly where it was - so the weapon that fires every two seconds turns
     * a held beam into a ladder, and letting go is what lets the ladder run out.
     * <p>
     * Written against the class rather than a tag, like every other engraving that swaps out one
     * item's own behaviour: what it changes is the augment loop in {@code RayOfLaserItem#land}, and
     * no other weapon in the game has one. Its core is the Compressed Ghast's heart, which is the
     * whole of how it is earned and is also what keeps its grid clear of every other engraving's -
     * the same trick the Potion, Dash, Composition and Vapor Slash engravings play.
     */
    INCREMENTAL("incremental", 236, Kind.ENGRAVING, () -> ModItems.TIER_18_COMPRESSED_HEART.get(),
            stack -> stack.getItem() instanceof RayOfLaserItem),

    /**
     * A spear that closes the distance itself: beginning to charge throws the wielder
     * {@code CompressedSpearItem.DASH_SPEED} blocks a tick forward, and the charge then lands for
     * what that speed is worth.
     * <p>
     * It is the one engraving in this file that does not add an ability so much as feed one the
     * spear already had. The Compressed Spear's charge is paid in closing speed and a player on
     * foot can only supply a sprint's worth - about 5.6 blocks a second, which is barely over the
     * threshold at which the charge counts for anything. The lunge supplies twenty-four, so the
     * same attack that was worth eleven points becomes worth nearly fifty, and none of that number
     * is written down anywhere in the engraving: it is the spear's own arithmetic, handed a bigger
     * input.
     * <p>
     * Written against the class rather than a tag, like the Dash it is named after and for a
     * stronger version of the same reason. The katana's Dash is at least a coherent thing to put on
     * another blade; this one would do nothing at all anywhere else, because the only weapon in the
     * game that reads closing speed is this one.
     * <p>
     * The two are not the same ability twice. The katana's Dash is a clipped <em>teleport</em>,
     * because there five blocks has to mean five blocks; this is <em>velocity</em>, because the
     * speed is the whole payload and a teleport produces none. It also fires from the item's own
     * {@code use} rather than from a right-click handler, since on a spear the right click is
     * already the charge - see {@code CompressedSpearItem}, which owns both halves.
     */
    SPEAR_DASH("spear_dash", 251, stack -> stack.getItem() instanceof CompressedSpearItem);


    /**
     * The two families of the same system. A sigil is an engraving in everything that matters - the
     * same component, the same table, applied and taken off the same way - and differs in the grid
     * it is crafted in and the word on the item. That is not decoration: two recipes only collide
     * when they are the same shape, so giving sigils a core of their own means their tiers only
     * have to stay clear of each other rather than of every engraving as well.
     */
    public enum Kind {
        ENGRAVING("engraving", Items.DIAMOND),
        SIGIL("sigil", Items.AMETHYST_SHARD);

        private final String suffix;
        private final Item core;

        Kind(String suffix, Item core) {
            this.suffix = suffix;
            this.core = core;
        }

        /** What the crafted item's id ends in. */
        public String suffix() {
            return this.suffix;
        }

        /** What sits in the middle of the grid, and so what tells the two families' recipes apart. */
        public Item core() {
            return this.core;
        }
    }

    public static final Codec<Engraving> CODEC = StringRepresentable.fromEnum(Engraving::values);

    private final String name;
    private final int blockTier;
    private final Kind kind;
    @Nullable
    private final Supplier<Item> core;
    private final Predicate<ItemStack> appliesTo;

    Engraving(String name, int blockTier, Predicate<ItemStack> appliesTo) {
        this(name, blockTier, Kind.ENGRAVING, null, appliesTo);
    }

    Engraving(String name, int blockTier, Kind kind, Predicate<ItemStack> appliesTo) {
        this(name, blockTier, kind, null, appliesTo);
    }

    Engraving(String name, int blockTier, Kind kind, @Nullable Supplier<Item> core,
              Predicate<ItemStack> appliesTo) {
        this.name = name;
        this.blockTier = blockTier;
        this.kind = kind;
        this.core = core;
        this.appliesTo = appliesTo;
    }

    /**
     * What sits in the middle of this one's grid: its family's core unless it names one of its own.
     * A core of its own is a grid of its own, which is what lets an engraving share a tier with
     * anything else.
     * <p>
     * It is a supplier rather than an item because a core may be one of this mod's items, and
     * {@code ModItems} reads this enum while it is registering them - resolving one here would be a
     * class initialised in the middle of initialising itself.
     */
    public Item core() {
        return this.core != null ? this.core.get() : this.kind.core();
    }

    /** Which of the two families this belongs to, which is its grid and the word on its item. */
    public Kind kind() {
        return this.kind;
    }

    /**
     * The compressed cobblestone tier this engraving's item is cut from. Every member of a
     * {@link Kind} is crafted in that kind's one grid, so the tier is the only thing that tells two
     * of them apart: tiers must stay disjoint within a kind, or two recipes become the same recipe
     * and only one of them is craftable.
     */
    public int blockTier() {
        return this.blockTier;
    }

    /** Whether this engraving has anywhere to go on {@code stack}. */
    public boolean canApplyTo(ItemStack stack) {
        return !stack.isEmpty() && this.appliesTo.test(stack);
    }

    /** The item this engraving is crafted as, and the item an Engraving Table gives back. */
    public Item item() {
        return ModItems.ENGRAVINGS.get(this).get();
    }

    /** The item id of the engraving item this is cut from, which is also its lang key. */
    public String itemName() {
        return this.name + "_" + this.kind.suffix();
    }

    /** What the tooltip on an engraved piece of gear says. */
    public Component displayName() {
        return Component.translatable("engraving.unnecessarilycompressedcobblestone." + this.name);
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    /**
     * The engraving saved under {@code name}. This is what the component's network codec decodes
     * through, so an unknown name is a client and server that disagree about what engravings exist -
     * worth failing loudly rather than putting a null in a list nothing else expects one in.
     */
    public static Engraving byName(String name) {
        for (Engraving engraving : values()) {
            if (engraving.name.equals(name)) {
                return engraving;
            }
        }

        throw new IllegalArgumentException("Unknown engraving: " + name);
    }

    /** Any of the four armour slots, by the vanilla tags rather than by {@code ArmorItem}. */
    private static boolean isArmor(ItemStack stack) {
        return stack.is(ItemTags.HEAD_ARMOR) || stack.is(ItemTags.CHEST_ARMOR)
                || stack.is(ItemTags.LEG_ARMOR) || stack.is(ItemTags.FOOT_ARMOR);
    }
}
