package net.fahr3n.unnecessarilycompressedcobblestone.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedShieldItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedSpearItem;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedTotemItem;
import net.fahr3n.unnecessarilycompressedcobblestone.util.BossSpoil;
import net.fahr3n.unnecessarilycompressedcobblestone.potion.ModMobEffects;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

import java.util.List;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.damage.ModDamageTypes;
import net.fahr3n.unnecessarilycompressedcobblestone.item.ModItems;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.DevSwordItem;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.CarvedCobblestoneBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.enchantment.ModEnchantments;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.AbstractCompressedDragonEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedDragonTier2Entity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.ArrowVeilEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.item.ModArmorMaterials;
import net.fahr3n.unnecessarilycompressedcobblestone.potion.ModPotions;
import net.fahr3n.unnecessarilycompressedcobblestone.util.DeferredFill;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.EngravingItem;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Augment;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Augments;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Engraving;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Corrosion;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Engravings;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Ghasted;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Sweep;
import net.fahr3n.unnecessarilycompressedcobblestone.util.DeferredStrikes;
import net.fahr3n.unnecessarilycompressedcobblestone.util.LightningSong;
import net.fahr3n.unnecessarilycompressedcobblestone.util.CompressionEnergy;
import net.fahr3n.unnecessarilycompressedcobblestone.util.ModTags;
import net.fahr3n.unnecessarilycompressedcobblestone.util.AbsoluteLimit;
import net.fahr3n.unnecessarilycompressedcobblestone.util.SelectiveImmunity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.fahr3n.unnecessarilycompressedcobblestone.item.custom.CompressedFishingRodItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.bus.api.EventPriority;
import net.minecraft.world.InteractionHand;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.OmniSlashEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.ScytheWaveEntity;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = UnnecessarilyCompressedCobblestone.MOD_ID)
public class ModEvents {
    /** What the Dev Compressed Cobblestone is worth per hit, and it is true damage - see below. */
    private static final float DEV_BLOCK_DAMAGE = 999.0F;

    private static final ResourceLocation CE_ATTACK_DAMAGE_ID =
            ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, "compression_energy_attack_damage");
    private static final ResourceLocation CE_MAX_HEALTH_ID =
            ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, "compression_energy_max_health");
    private static final ResourceLocation JUMP_SET_STEP_HEIGHT_ID =
            ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, "compression_jump_step_height");
    private static final ResourceLocation ULTIMATE_SET_STEP_HEIGHT_ID =
            ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, "ultimate_compressed_step_height");
    private static final ResourceLocation REACH_BLOCK_ID =
            ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, "reach_engraving_block");
    private static final ResourceLocation REACH_ENTITY_ID =
            ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, "reach_engraving_entity");
    private static final ResourceLocation SUPER_REACH_BLOCK_ID =
            ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, "super_reach_engraving_block");
    private static final ResourceLocation SUPER_REACH_ENTITY_ID =
            ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, "super_reach_engraving_entity");

    /** What a Reach engraving adds to both interaction ranges, in blocks. */
    private static final double REACH_ENGRAVING_BONUS = 1.0;

    /**
     * What a Super Reach engraving adds to both, in blocks. Chosen against a ceiling rather than
     * picked: {@code BLOCK_INTERACTION_RANGE} and {@code ENTITY_INTERACTION_RANGE} are
     * {@code RangedAttribute}s that stop at 64, and a player's bases are 4.5 and 3.0, so fifty is
     * near as far as either goes and a larger figure would be clamped with nothing saying so.
     */
    private static final double SUPER_REACH_ENGRAVING_BONUS = 50.0;

    /**
     * How often a full Compression Rain set sweeps the harmful effects off its wearer, in ticks.
     * Refusing new ones is the applicable event's job and happens the instant they arrive; this is
     * only for what was already there.
     */
    private static final int RAIN_SET_SWEEP_INTERVAL = 20;

    /** Speed II; the amplifier is one less than the numeral. */
    private static final int LIGHTNING_SET_SPEED_AMPLIFIER = 1;

    /** What a full Compression Lightning set leaves of a lightning strike: five percent of it. */
    private static final float LIGHTNING_SET_DAMAGE_TAKEN = 0.05F;

    /** What the Lightning Sigil leaves of a strike: fifteen percent of it. */
    private static final float LIGHTNING_SIGIL_DAMAGE_TAKEN = 0.15F;

    /** Speed III; the amplifier is one less than the numeral. */
    private static final int LIGHTNING_SIGIL_SPEED_AMPLIFIER = 2;

    /** How far the Explosion Sigil's blast reaches. */
    private static final double EXPLOSION_SIGIL_RADIUS = 3.0;

    /** What an anvil asks to fix a potion into an engraving. */
    private static final int POTION_ENGRAVING_COST = 5;

    /** What a full Compression Magic set leaves of anything a potion deals: half of it. */
    private static final float MAGIC_SET_DAMAGE_TAKEN = 0.5F;

    /** What each piece of the Compression Arrow set takes off a projectile: a fifth of it. */
    private static final float ARROW_SET_PROJECTILE_REDUCTION_PER_PIECE = 0.20F;

    /** How long a full Compression Arrow set lights up what the wearer's arrows hit, in ticks. */
    private static final int ARROW_SET_GLOWING_DURATION = 200;

    /**
     * What one Projectile Protection or Lightning Protection engraving is worth: fifteen percent of
     * the hit. They stack across the four armour slots, so four of a kind is sixty percent - and
     * they stack against whatever else is already reducing the damage by multiplying rather than by
     * adding, so nothing here can reach a hundred percent and start healing what it protects.
     */
    private static final float PROTECTION_ENGRAVING_REDUCTION = 0.15F;

    /**
     * How long the Vision engraving's night vision is granted for. Vanilla flashes night vision over
     * its last ten seconds, so this is kept well clear of that: the effect is refreshed long before
     * it can start blinking, and still lapses within a few seconds of the helmet coming off.
     */
    private static final int VISION_EFFECT_DURATION = 400;

    /** How far a Dash carries, in blocks, and how long it is locked out for: ten seconds. */
    private static final double DASH_DISTANCE = 5.0;
    private static final int DASH_COOLDOWN = 200;

    /** How wide the cut is, measured from the line the dash travelled along. */
    private static final double DASH_WIDTH = 1.5;

    /**
     * How many swings' worth of damage everything on the path takes.
     * <p>
     * It is a multiple of the wielder's own attack damage rather than a number of its own, which is
     * what keeps it honest at every point in the game: the katana's seven, the player's one and
     * whatever the Compression Inscriber has added are all already in that attribute, so a dash is
     * worth exactly one swing of the blade doing it and needs no retuning when the blade is
     * upgraded.
     */
    private static final double DASH_DAMAGE_MULTIPLIER = 1.0;

    /** How far short of a wall the dash stops, so it never ends inside one. */
    private static final double DASH_WALL_MARGIN = 0.6;

    /**
     * How hard the Uppercut engraving throws, in blocks per tick.
     * <p>
     * A hundred blocks of height, worked back through vanilla's own vertical motion rather than
     * through the schoolbook {@code v^2 / 2g}: a living entity's velocity is
     * {@code v = (v - 0.08) * 0.98} each tick, and the drag term makes the apex noticeably lower
     * than gravity alone would. Simulating that gives 5.33 for a hundred blocks, where the
     * frictionless answer would have said 3.5 and fallen forty blocks short.
     */
    private static final double UPPERCUT_VELOCITY = 5.33;

    /** How far the Exploding Sword reaches, and how loud the blast that goes with it is. */
    private static final double EXPLODING_SWORD_RADIUS = 5.0;
    private static final float EXPLODING_SWORD_POWER = 4.0F;
    private static final int EXPLODING_SWORD_COOLDOWN = 200;

    /**
     * The deepest compression level the Exploding Sword takes. Above this the stone stands, which is
     * the whole of what "strong enough to break tier 205" means - there is no power setting that
     * would say it, since every hardened level has the same three and a half million explosion
     * resistance and no explosion in the game moves any of them.
     */
    public static final int EXPLODING_SWORD_MAX_LEVEL = 205;

    /**
     * The Vapor Slash: how far it carries, and how wide a hole it leaves.
     * <p>
     * Far longer than the Dash's five, and that is now the point of difference between them rather
     * than an accident. The Dash is a step taken inside a fight - it closes a gap, and its length is
     * the length of a gap. This one carves the ground out in front of it, so what it is really for
     * is going somewhere, and a tunnelling move that ends five blocks later is a move that has not
     * gone anywhere. Thirty-two is a chunk's width: a slash crosses one, and the hole it leaves is
     * long enough to be a corridor rather than an alcove.
     * <p>
     * It stays affordable because the carve is bounded by the same figures. A capsule this long,
     * three across and three tall is a rectangle of thirty-eight columns by six, of which rather
     * fewer are inside the radius, times three heights - some five hundred positions on the tick it
     * is asked for, which is two orders short of anything {@code DeferredFill} exists for.
     * <p>
     * The radius is measured from the line travelled, as the cut is, which makes the hole a capsule
     * rather than a sphere at either end.
     */
    private static final double VAPOR_SLASH_DISTANCE = 32.0;
    private static final double VAPOR_SLASH_RADIUS = 3.0;

    /**
     * How tall the hole is: three blocks, from the wielder's feet to just over their head. It is a
     * separate figure from the radius rather than a sphere's, because a slash is a horizontal thing
     * - a sphere of radius three would drop the floor out from under the player at the far end of
     * every one.
     */
    private static final int VAPOR_SLASH_HEIGHT = 3;

    /**
     * What each rung is worth. Three swings of the blade doing it and then five, off
     * {@code Attributes.ATTACK_DAMAGE} for the reason {@link #DASH_DAMAGE_MULTIPLIER} gives: the
     * katana's own damage, the player's base and everything the Compression Inscriber added are
     * already in that attribute, so neither figure needs retuning as the weapon deepens.
     */
    private static final double VAPOR_SLASH_DAMAGE_MULTIPLIER = 3.0;
    private static final double VAPOR_SLASH_2_DAMAGE_MULTIPLIER = 5.0;

    /** Ten seconds, and then one. The gap between them is most of what the second rung buys. */
    private static final int VAPOR_SLASH_COOLDOWN = 200;
    private static final int VAPOR_SLASH_2_COOLDOWN = 20;

    /**
     * What the blocks are broken with. Nothing is ever consumed - it exists only to be the tool
     * context {@code Block.dropResources} reads - but it has to be a real pickaxe good enough for
     * anything the slash can reach, or stone would break and drop nothing, which is the one outcome
     * "it drops like it was mined" has to rule out. Netherite rather than diamond so deepslate,
     * obsidian and ancient debris all come up as well.
     */
    private static final ItemStack VAPOR_SLASH_TOOL = new ItemStack(Items.NETHERITE_PICKAXE);

    /**
     * How long an Omni Slash is locked out for: a full minute, six times the Vapor Slash's and the
     * Exploding Sword's ten seconds and sixty times the Vapor Slash 2's one.
     * <p>
     * It is the longest lockout in the mod and it is the whole of what prices the ability. Every
     * other right-click ability here is a move with a blow attached and is meant to be part of how a
     * weapon is swung; this one opens a tunnel to the horizon and lands a hit nothing in
     * the game reduces, so it is an opener rather than a rhythm - something a fight is started with
     * and then fought without.
     */
    private static final int OMNI_SLASH_COOLDOWN = 1200;

    /**
     * What an Omni Slash hits for, as a multiple of the wielder's {@code Attributes.ATTACK_DAMAGE}.
     * One, because the ability is described as a standard attack from the engraved weapon and that
     * attribute already is one: the weapon's own modifier, the player's base and every point the
     * Compression Inscriber added are in it, so the crescent stays priced at every point in the
     * game without naming a number here.
     * <p>
     * Enchantments are deliberately not folded in on top. The blow is a damage type of its own that
     * nothing reduces rather than a swing - see {@code ModDamageTypes.OMNI_SLASH} - and Sharpness
     * scaling an unblockable hit would be a second multiplier on the one attack in the mod that has
     * no answer.
     */
    private static final double OMNI_SLASH_DAMAGE_MULTIPLIER = 1.0;

    /**
     * How far in front of the wielder's eyes a crescent is born, in blocks. It is the carve radius
     * rounded up: the slash takes everything within two and a half blocks of the line it travels, so
     * anything shorter than this would open the ability by dropping the player who used it into a
     * hole of their own making.
     */
    private static final double OMNI_SLASH_MUZZLE = 3.0;

    /** Jump Boost X; the amplifier is one less than the numeral. */
    private static final int JUMP_ENGRAVING_AMPLIFIER = 9;

    /** How long each top-up of it lasts, so it lapses a second after the leggings come off. */
    private static final int JUMP_EFFECT_DURATION = 40;

    /** How long a Smash is locked out for: half a minute. */
    private static final int SMASH_COOLDOWN = 600;

    /** How fast the slam drives the player down, in blocks a tick. */
    private static final double SMASH_SPEED = 3.0;

    /**
     * What a smash is worth at no height at all, and what every block of fall adds to it.
     * <p>
     * Straight line and no ceiling: ten blocks is 105, thirty is 305, and a drop from build height
     * is a little over three and a half thousand. Nothing clamps it, so the only limit on a smash is
     * how far up the player was willing to climb - which is the whole cost of the ability, since the
     * climb is thirty seconds of cooldown and a fall they have to survive the landing of.
     */
    private static final float SMASH_BASE_DAMAGE = 5.0F;
    private static final float SMASH_DAMAGE_PER_BLOCK = 10.0F;

    /** How wide the landing reaches, at no height and at its widest. */
    private static final double SMASH_MIN_RADIUS = 4.0;
    private static final double SMASH_MAX_RADIUS = 16.0;

    /** How much of the fall each block of it adds to that radius. */
    private static final double SMASH_RADIUS_PER_BLOCK = 0.25;

    /**
     * Who is currently falling out of a Smash.
     * <p>
     * It cannot be a flag on the stack: the mace may be swapped, dropped or broken between the
     * right click and the landing, and the slam is the player's now rather than the weapon's. It is
     * cleared the moment they are on the ground or in water - see {@link #onSmashLandingTick} -
     * so the only way to leave an entry behind is to log out mid-fall, and that one is overwritten
     * on the next slam.
     */
    private static final Set<UUID> SMASHING = new HashSet<>();

    /**
     * Whoever an Uppercut has landed on and not yet thrown. It is a tick's worth of state and never
     * more - see {@link #onUppercutLaunch} for why the throw cannot happen where it is decided.
     */
    private static final Set<UUID> UPPERCUT_PENDING = new HashSet<>();

    /** Haste IV; the amplifier is one less than the numeral. */
    private static final int HASTE_ENGRAVING_AMPLIFIER = 3;

    /** Haste VI, which the second rung of the ladder hands out instead. */
    private static final int HASTE_2_ENGRAVING_AMPLIFIER = 5;

    /** And how long each top-up of it lasts, so it lapses a second after the katana is put away. */
    private static final int HASTE_EFFECT_DURATION = 40;

    /** What a Lifesteal engraving gives back to whoever swung it: a fifth of the damage dealt. */
    private static final float LIFESTEAL_FRACTION = 0.20F;

    /**
     * What a Quick Draw engraving takes off a draw. The draw is not shortened directly - nothing in
     * vanilla exposes a bow's draw time - so the charge the bow has built up is scaled instead,
     * which brings it to full power in three quarters of the ticks. The pull animation still runs at
     * its own speed, so a quick-drawn bow reaches full power a little before it looks like it has.
     */
    private static final float QUICK_DRAW_FRACTION = 0.25F;

    /**
     * How long a bow carrying the Sniper engraving takes to come to full power: ten seconds flat,
     * whatever bow it is. It is expressed the same way Quick Draw is, by scaling the charge the bow
     * has built - {@code BowItem.MAX_DRAW_DURATION} is the twenty ticks vanilla's own curve reaches
     * full power at, so charge scaled by that over this reaches it at this instead.
     */
    private static final int SNIPER_DRAW_TICKS = 200;

    /**
     * What the Sniper multiplies the arrow's speed by. A bow at full draw looses at three blocks a
     * tick; twenty-five times that is seventy-five, which crosses everything a player can see inside
     * two ticks and is as close to hitscan as an entity gets. It is a multiplier rather than a fixed
     * speed so a half-drawn shot is still a half-strength one.
     */
    private static final double SNIPER_VELOCITY_SCALE = 25.0;

    /** What each rung of the Extra Shot ladder adds to a volley. Additive, never multiplied. */
    private static final int EXTRA_SHOT_COUNT = 1;

    private static final int EXTRA_SHOT_2_COUNT = 2;

    /**
     * How far apart the extra shots are fanned, in degrees either side of the one that was fired.
     * Vanilla's Multishot spreads its two by ten, and this matches it so an engraved crossbow and a
     * Multishot one throw the same shape.
     */
    private static final double EXTRA_SHOT_SPREAD = 10.0;

    /**
     * Where a shooter records the tick it last had extra shots added, so a volley is answered once
     * however many projectiles it is made of.
     * <p>
     * It is kept on the shooter rather than in a map here for the plain reason that the shooter is
     * already a thing with a lifetime: nothing has to be swept when a player logs out. The value is
     * the game time plus one, because an unset tag reads as zero and so does the first tick of a
     * world.
     */
    private static final String EXTRA_SHOT_TICK_TAG = "ucc:extra_shot_tick";

    /** How often a mob killed by an engraved rod leaves its spawn egg behind: one kill in four. */
    private static final float EGG_ENGRAVING_CHANCE = 0.25F;

    /**
     * What a hook hits for before Hook and Compression Energy are applied. Vanilla's is zero, so
     * this number is the mod's own statement rather than a scaling of anything: it is deliberately
     * small, since the damage a bobber does is a novelty and the enchantment is what makes it worth
     * anything.
     */
    private static final float HOOK_BASE_DAMAGE = 1.0F;

    /** What one level of Hook multiplies that by. Five levels is a fifth over twice the damage. */
    private static final double HOOK_DAMAGE_PER_LEVEL = 1.2;

    /** Jump Boost V; the amplifier is one less than the numeral. */
    private static final int JUMP_SET_AMPLIFIER = 4;

    /**
     * How long a set's effect is granted for, in ticks. Long enough that refreshing it every tick is
     * cheap, short enough that it lapses soon after a piece comes off. Shared by both sets.
     */
    private static final int SET_EFFECT_DURATION = 40;

    /**
     * Applies the crafted books - the Compression family's, and the staff's Surge VI, Silent Cast V
     * and Multibolt III - which sit above their enchantment's registered max level and so are the
     * one case the vanilla anvil cannot handle: it clamps every enchantment it
     * merges to {@link Enchantment#getMaxLevel()}, which would quietly grind a crafted level back
     * down. Only merges that involve a crafted level are taken over here; Compression 1 to 3 comes
     * off an enchanting table like any other enchantment and vanilla already does the right thing
     * with it.
     * <p>
     * Super, Hyper and Giga Compression are craft-only, so every level of theirs is a crafted one
     * and every merge involving them lands here. The rules for a crafted level are that it never
     * grows (two Compression IV books do not make a V, and two Super Compression I books do not
     * make a II), never shrinks (a lesser book applied on top leaves it alone), and is charged as
     * if it were a level 1 book however deep the compression is.
     */
    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        // A Potion Engraving takes its effect from any potion laid beside it on an anvil, once and
        // for good. An anvil rather than a crafting grid because this is the one operation in the
        // mod that copies data from one item onto another, which is what an anvil is for - and
        // because a potion in a crafting grid would need a recipe type of its own to read.
        if (EngravingItem.of(left) == Engraving.POTION && right.has(DataComponents.POTION_CONTENTS)) {
            PotionContents contents = right.get(DataComponents.POTION_CONTENTS);
            if (left.has(DataComponents.POTION_CONTENTS) || contents == null || !contents.hasEffects()) {
                return;
            }

            ItemStack engraved = left.copyWithCount(1);
            engraved.set(DataComponents.POTION_CONTENTS, contents);
            event.setOutput(engraved);
            event.setCost(POTION_ENGRAVING_COST);
            event.setMaterialCost(1);
            return;
        }

        // A crafted level only ever travels on an enchanted book. Anything else - repairing such a
        // weapon with material, renaming it, merging two weapons - either never touches the
        // enchantment or is left to vanilla.
        if (!right.has(DataComponents.STORED_ENCHANTMENTS)) {
            return;
        }

        ItemEnchantments bookEnchantments = EnchantmentHelper.getEnchantmentsForCrafting(right);
        if (!hasCraftedLevel(bookEnchantments)
                && !hasCraftedLevel(EnchantmentHelper.getEnchantmentsForCrafting(left))) {
            return;
        }

        if (!EnchantmentHelper.canStoreEnchantments(left) || !left.isBookEnchantable(right)) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack output = left.copy();
        ItemEnchantments.Mutable merged = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(output));
        int work = 0;
        boolean anyApplied = false;

        // The same merge vanilla performs in AnvilMenu#createResult, minus the repair handling that
        // a book can never trigger, and with crafted Compression levels pinned.
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : bookEnchantments.entrySet()) {
            Holder<Enchantment> holder = entry.getKey();
            boolean hasCraftedLevels = ModEnchantments.hasCraftedLevels(holder);
            int current = merged.getLevel(holder);
            // Two matching books normally make the next level up. A crafted Compression level is
            // taken as it comes instead, so combining never reaches a level that has no book: the
            // only way up the family is to craft the deeper book. Compression 1 and 2 are still
            // below the table cap, so they level up the way they always have.
            boolean levelsUp = current == entry.getIntValue()
                    && !ModEnchantments.isCraftedLevel(holder, entry.getIntValue() + 1);
            int level = levelsUp ? entry.getIntValue() + 1
                    : Math.max(entry.getIntValue(), current);

            boolean supported = left.supportsEnchantment(holder) || player.hasInfiniteMaterials();
            for (Holder<Enchantment> other : merged.keySet()) {
                if (!other.equals(holder) && !Enchantment.areCompatible(holder, other)) {
                    supported = false;
                    work++;
                }
            }

            if (!supported) {
                continue;
            }

            anyApplied = true;
            if (!hasCraftedLevels) {
                level = Math.min(level, holder.value().getMaxLevel());
            }
            merged.set(holder, level);

            // Books cost half of an enchantment's anvil cost per level, at least one. Compression
            // is billed for a single level no matter which level is going on.
            int costPerLevel = Math.max(1, holder.value().getAnvilCost() / 2);
            work += costPerLevel * (hasCraftedLevels ? 1 : level);
            if (left.getCount() > 1) {
                work = 40;
            }
        }

        if (!anyApplied) {
            return;
        }

        String name = event.getName();
        if (name != null && !StringUtil.isBlank(name)) {
            if (!name.equals(left.getHoverName().getString())) {
                work++;
                output.set(DataComponents.CUSTOM_NAME, Component.literal(name));
            }
        } else if (left.has(DataComponents.CUSTOM_NAME)) {
            work++;
            output.remove(DataComponents.CUSTOM_NAME);
        }

        int leftPriorWork = left.getOrDefault(DataComponents.REPAIR_COST, 0);
        int rightPriorWork = right.getOrDefault(DataComponents.REPAIR_COST, 0);
        output.set(DataComponents.REPAIR_COST,
                AnvilMenu.calculateIncreasedRepairCost(Math.max(leftPriorWork, rightPriorWork)));
        EnchantmentHelper.setEnchantments(output, merged.toImmutable());

        event.setOutput(output);
        // Prior work can push this past the 40 levels vanilla refuses to go beyond; going through
        // the event means the anvil asks for the levels instead of giving up, which is the lesser
        // evil - cancelling the event here would leave a stale stack in the output slot.
        event.setCost((long) leftPriorWork + rightPriorWork + work);
        event.setMaterialCost(1);
    }

    /**
     * The brewing stand recipes for the Compression potions: awkward plus a tier 15 block, then
     * redstone to stretch the four minutes out to six.
     * <p>
     * Splash and lingering need nothing here. Gunpowder and dragon's breath are container recipes
     * in vanilla, applied to whatever potion is in the stand rather than to a listed set, so both
     * Compression brews can be thrown the moment they can be brewed at all - and, for the same
     * reason, tipped into arrows.
     */
    @SubscribeEvent
    public static void registerBrewingRecipes(RegisterBrewingRecipesEvent event) {
        PotionBrewing.Builder builder = event.getBuilder();

        builder.addMix(Potions.AWKWARD, ModBlocks.byLevel(15).get().asItem(), ModPotions.COMPRESSION_1);
        builder.addMix(ModPotions.COMPRESSION_1, Items.REDSTONE, ModPotions.LONG_COMPRESSION_1);

        // Milk in a bottle, off a bucket of it. No redstone or glowstone: it has no duration to
        // stretch and no level to deepen.
        builder.addMix(Potions.AWKWARD, Items.MILK_BUCKET, ModPotions.CLEANSING);

        // The five damage brews, all three forms of each. Which block a stand wants is the only
        // thing that tells one from another, so the loop is the whole recipe list.
        for (ModPotions.Brew brew : ModPotions.BREWS) {
            builder.addMix(Potions.AWKWARD, ModBlocks.byLevel(brew.blockTier()).get().asItem(), brew.base());
            builder.addMix(brew.base(), Items.REDSTONE, brew.extended());
            builder.addMix(brew.base(), Items.GLOWSTONE_DUST, brew.strong());
        }
    }

    /**
     * Drinking a Potion of Cleansing: every harmful effect comes off and nothing else is touched.
     * <p>
     * The potion carries no effects of its own, so there is nothing for vanilla to apply and nothing
     * would happen without this. It is caught on {@code Finish} rather than on use, so a drink that
     * was interrupted does nothing - and it covers the splash and lingering forms too, since those
     * are the same potion in a different bottle and land through the cloud rather than through here.
     */
    @SubscribeEvent
    public static void onCleansingPotionDrunk(LivingEntityUseItemEvent.Finish event) {
        PotionContents contents = event.getItem().get(DataComponents.POTION_CONTENTS);
        if (contents != null && contents.potion().filter(ModPotions.CLEANSING::equals).isPresent()) {
            ModPotions.cleanse(event.getEntity());
        }
    }

    /**
     * The Potion Engraving: an arrow out of an engraved bow carries the bow's potion into whatever
     * it hits.
     * <p>
     * The bow is read off the arrow rather than off the shooter's hand - {@code getWeaponItem} is
     * the stack that fired it, kept by the projectile - so a bow swapped, dropped or broken between
     * the shot and the landing changes nothing about the arrow already in the air.
     * <p>
     * A tipped arrow keeps its own effects: those are applied by vanilla's own
     * {@code Arrow.doPostHurtEffects} and this adds to them rather than replacing them, so a potion
     * arrow out of a potion bow lands both.
     */
    @SubscribeEvent
    public static void onPotionEngravingImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow)
                || !(event.getRayTraceResult() instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof LivingEntity target)
                || arrow.level().isClientSide()) {
            return;
        }

        ItemStack bow = arrow.getWeaponItem();
        if (bow == null || !Engravings.has(bow, Engraving.POTION)) {
            return;
        }

        PotionContents contents = bow.get(DataComponents.POTION_CONTENTS);
        if (contents != null) {
            contents.forEachEffect(effect -> {
                // An instantaneous effect is applied rather than added, the way a tipped arrow
                // applies harming: adding one would put a zero-length effect on and do nothing.
                if (effect.getEffect().value().isInstantenous()) {
                    effect.getEffect().value().applyInstantenousEffect(arrow.getOwner(), arrow.getOwner(),
                            target, effect.getAmplifier(), 1.0);
                } else {
                    target.addEffect(new MobEffectInstance(effect), arrow.getOwner());
                }
            });
        }
    }

    /**
     * The other half of the Arrow Veil, and the half that actually saves anybody.
     * <p>
     * The dome's own sweep runs once a tick, and entities tick in an order nobody controls: an arrow
     * that ticks before the veil has already ray-cast its path and hit whoever was standing there. At
     * ordinary arrow speeds that is a coin flip nobody notices; against something that crosses the
     * whole dome inside one tick it is the difference between a shield and a decoration. Catching the
     * impact instead is exact, because the impact is the only moment that matters and it is reported
     * here whatever order things ticked in.
     */
    /**
     * The Fishing enchantment, and the Compressed Fishing Rod's ten catches.
     * <p>
     * Both are extra <em>rolls</em> rather than a bigger single one, so the loot table is run again
     * from scratch for each: two independent casts' worth of luck, not one cast with the numbers
     * doubled. It has to be done by hand here because {@code ItemFishedEvent} copies the drop list
     * into itself and vanilla goes on to spawn the original - the event's own javadoc says as much -
     * so adding to {@code getDrops()} would change nothing. What is spawned instead is exactly what
     * vanilla spawns for the first catch: an item arcing back towards the angler, an experience orb
     * with it, and the fish-caught statistic where the catch was a fish.
     * <p>
     * The Compressed Fishing Rod's one catch in twenty comes up as a block of the stone the rod is
     * cut from instead of whatever the table rolled, and it is rolled per catch rather than per
     * cast, so ten catches are ten independent chances at it.
     */
    @SubscribeEvent
    public static void onItemFished(ItemFishedEvent event) {
        Player player = event.getEntity();
        FishingHook hook = event.getHookEntity();
        if (player == null || !(hook.level() instanceof ServerLevel level)) {
            return;
        }

        ItemStack rod = heldRod(player);
        // Vanilla has already given the first catch, so this owes the rest of them.
        int extra = catches(level, rod, player) - 1;
        if (extra <= 0) {
            return;
        }

        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, hook.position())
                .withParameter(LootContextParams.TOOL, rod)
                .withParameter(LootContextParams.THIS_ENTITY, hook)
                .withParameter(LootContextParams.ATTACKING_ENTITY, player)
                .withLuck(player.getLuck())
                .create(LootContextParamSets.FISHING);
        LootTable table = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);

        for (int i = 0; i < extra; i++) {
            if (rod.getItem() instanceof CompressedFishingRodItem
                    && level.random.nextFloat() < CompressedFishingRodItem.COBBLE_CHANCE) {
                reel(level, hook, player, new ItemStack(
                        ModBlocks.byLevel(CompressedFishingRodItem.COBBLE_TIER).get()));
                continue;
            }

            for (ItemStack caught : table.getRandomItems(params)) {
                reel(level, hook, player, caught);
            }
        }
    }

    /** How many times {@code rod} rolls its catch: what the rod does on its own, times Fishing. */
    private static int catches(ServerLevel level, ItemStack rod, Player player) {
        int fishing = EnchantmentHelper.getItemEnchantmentLevel(
                level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(ModEnchantments.FISHING), rod);

        return CompressedFishingRodItem.baseCatches(rod) * (fishing + 1);
    }

    /**
     * Vanilla's own reel-in, lifted out of {@code FishingHook#retrieve}: the catch arcs back towards
     * the angler rather than dropping where the bobber was, and brings an orb of experience with it.
     */
    private static void reel(ServerLevel level, FishingHook hook, Player player, ItemStack caught) {
        ItemEntity item = new ItemEntity(level, hook.getX(), hook.getY(), hook.getZ(), caught);
        double dx = player.getX() - hook.getX();
        double dy = player.getY() - hook.getY();
        double dz = player.getZ() - hook.getZ();
        item.setDeltaMovement(dx * 0.1,
                dy * 0.1 + Math.sqrt(Math.sqrt(dx * dx + dy * dy + dz * dz)) * 0.08, dz * 0.1);
        level.addFreshEntity(item);
        level.addFreshEntity(new ExperienceOrb(level, player.getX(), player.getY() + 0.5, player.getZ() + 0.5,
                level.random.nextInt(6) + 1));

        if (caught.is(ItemTags.FISHES)) {
            player.awardStat(Stats.FISH_CAUGHT, 1);
        }
    }

    /**
     * The Hook enchantment, and what a rod's Compression Energy buys.
     * <p>
     * Vanilla's bobber deals no damage at all - {@code FishingHook#onHitEntity} only records what it
     * caught - so there is nothing here to scale and a base has to be stated: a hook that carries
     * either Hook or Compression Energy hits for {@link #HOOK_BASE_DAMAGE} plus one per digit of
     * energy, multiplied by {@link #HOOK_DAMAGE_PER_LEVEL} for each level of Hook. A rod carrying
     * neither is left exactly as vanilla left it, which is why an ordinary bobber still does nothing.
     * <p>
     * The multiplier compounds rather than summing, for the reason the armour note gives at the
     * other end: a per-level fraction that adds is a straight line, and one that multiplies keeps
     * meaning the same thing at every level.
     */
    @SubscribeEvent
    public static void onFishingHookImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof FishingHook hook)
                || !(hook.level() instanceof ServerLevel level)
                || !(event.getRayTraceResult() instanceof EntityHitResult hit)) {
            return;
        }

        Player player = hook.getPlayerOwner();
        if (player == null || !(hit.getEntity() instanceof LivingEntity target)) {
            return;
        }

        ItemStack rod = heldRod(player);
        int hookLevel = EnchantmentHelper.getItemEnchantmentLevel(
                level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(ModEnchantments.HOOK), rod);
        int energy = CompressionEnergy.bonus(rod);
        if (hookLevel <= 0 && energy <= 0) {
            return;
        }

        float damage = (HOOK_BASE_DAMAGE + energy) * (float) Math.pow(HOOK_DAMAGE_PER_LEVEL, hookLevel);
        target.hurt(level.damageSources().thrown(hook, player), damage);
    }

    /**
     * The Egg engraving: a mob finished off by an engraved fishing rod drops its own spawn egg.
     * <p>
     * Which egg that is comes from {@code SpawnEggItem.byId}, vanilla's own map from an entity type
     * to the egg that spawns it, rather than from any list here - so a modded mob that ships an egg
     * drops that egg, and one that ships none (every boss in this mod included, since they are
     * called up through a summoning egg rather than a spawn egg) simply drops nothing extra. That is
     * the intended limit, not a gap: an entity with no registered egg has nothing this could hand out.
     * <p>
     * The kill is recognised by the damage source's <em>direct</em> entity being a fishing hook,
     * which is what {@code onFishingHookImpact} hurts with. The player behind it is the source's own
     * entity, and the rod is found in their hands the same way everything else here finds it.
     */
    @SubscribeEvent
    public static void onEggEngravingDrops(LivingDropsEvent event) {
        DamageSource source = event.getSource();
        if (!(source.getDirectEntity() instanceof FishingHook)
                || !(source.getEntity() instanceof Player player)) {
            return;
        }

        LivingEntity killed = event.getEntity();
        if (!Engravings.has(heldRod(player), Engraving.EGG)
                || killed.level().random.nextFloat() >= EGG_ENGRAVING_CHANCE) {
            return;
        }

        SpawnEggItem egg = SpawnEggItem.byId(killed.getType());
        if (egg == null) {
            return;
        }

        ItemEntity drop = new ItemEntity(killed.level(), killed.getX(), killed.getY(), killed.getZ(),
                new ItemStack(egg));
        drop.setDefaultPickUpDelay();
        event.getDrops().add(drop);
    }

    /**
     * The rod a fishing hook was cast from. Vanilla keeps no reference to it - {@code FishingHook}
     * is handed the stack only when it is reeled in - so it is found the same way
     * {@code FishingHook#shouldStopFishing} finds it: whichever hand is holding a rod, main hand
     * first. An empty stack is returned rather than null, so every caller can read enchantments and
     * energy off it without a guard.
     */
    private static ItemStack heldRod(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof FishingRodItem) {
            return main;
        }

        ItemStack off = player.getOffhandItem();
        return off.getItem() instanceof FishingRodItem ? off : ItemStack.EMPTY;
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (!(projectile.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 impact = event.getRayTraceResult().getLocation();
        List<ArrowVeilEntity> veils = level.getEntitiesOfClass(ArrowVeilEntity.class,
                AABB.ofSize(impact, ArrowVeilEntity.RADIUS * 2.0, ArrowVeilEntity.RADIUS * 2.0,
                        ArrowVeilEntity.RADIUS * 2.0));

        for (ArrowVeilEntity veil : veils) {
            if (veil.covers(impact) && veil.shields(projectile)) {
                veil.shatter(level, projectile);
                event.setCanceled(true);
                return;
            }
        }
    }

    /** Advances any block fill a TNT is still working through, a layer at a time. */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            DeferredFill.tick(serverLevel);
            DeferredStrikes.tick(serverLevel);
        }
    }

    /** A half finished fill must not outlive the world it was filling. */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        DeferredFill.clear();
        DeferredStrikes.clear();
        LightningSong.clearCache();
    }

    /**
     * Carving a level 11 block with shears, the way a pumpkin becomes a carved pumpkin. This lives
     * on the event bus rather than on the block because every compression level is registered as
     * the same plain block in a loop, and only this one level behaves differently.
     * <p>
     * Setting the block runs the carved block's own placement check, so carving in place can
     * finish a golem exactly as placing the head does.
     */
    @SubscribeEvent
    public static void onCarveCobblestone(PlayerInteractEvent.RightClickBlock event) {
        ItemStack tool = event.getItemStack();
        if (!tool.canPerformAction(ItemAbilities.SHEARS_CARVE)) {
            return;
        }

        Level level = event.getLevel();
        BlockPos pos = event.getPos();

        // Which head this stone becomes is the head's own business: every one of them names the
        // level it is cut from, so a new golem needs no line here.
        CarvedCobblestoneBlock head = null;
        for (DeferredBlock<CarvedCobblestoneBlock> candidate : ModBlocks.CARVED_HEADS) {
            if (level.getBlockState(pos).is(ModBlocks.byLevel(candidate.get().carvedFromLevel()).get())) {
                head = candidate.get();
                break;
            }
        }

        if (head == null) {
            return;
        }

        Player player = event.getEntity();
        if (!level.isClientSide) {
            Direction face = event.getFace();
            Direction facing = face == null || face.getAxis() == Direction.Axis.Y ? player.getDirection().getOpposite() : face;

            level.playSound(null, pos, SoundEvents.PUMPKIN_CARVE, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.setBlock(pos, head.defaultBlockState()
                    .setValue(CarvedCobblestoneBlock.FACING, facing), 11);
            tool.hurtAndBreak(1, player, LivingEntity.getSlotForHand(event.getHand()));
            level.gameEvent(player, GameEvent.SHEAR, pos);
            player.awardStat(Stats.ITEM_USED.get(Items.SHEARS));
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
    }

    /**
     * The armour and damage a piece of gear has earned from its Compression Energy. Adding the
     * modifiers here rather than writing them onto the stack keeps the energy component the only
     * thing that has to be right: change the energy and the stats follow on their own.
     * <p>
     * Each piece scales exactly one stat. Armour takes max health, one point per digit: vanilla
     * caps the armour attribute itself at 30, where deep energy would be wasted, while max health
     * runs to 1024, which a full set of the deepest gear very nearly reaches. The hearts are part
     * of the wearer while the armour is on rather than a pool that drains away. Attack damage has
     * no ceiling worth worrying about.
     */
    @SubscribeEvent
    public static void onItemAttributeModifiers(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();

        // The Reach engraving is an attribute and nothing else, so it belongs here rather than in a
        // tick handler: vanilla asks the held stack for its modifiers and both interaction ranges
        // follow, in survival and creative alike. Blocks and entities are two separate attributes
        // and both are moved, so a tool reaches a block and a mob equally far.
        if (Engravings.has(stack, Engraving.REACH)) {
            event.addModifier(Attributes.BLOCK_INTERACTION_RANGE,
                    new AttributeModifier(REACH_BLOCK_ID, REACH_ENGRAVING_BONUS, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);
            event.addModifier(Attributes.ENTITY_INTERACTION_RANGE,
                    new AttributeModifier(REACH_ENTITY_ID, REACH_ENGRAVING_BONUS, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);
        }

        // Super Reach is the same pair of attributes fifty times over, on a pickaxe. It carries
        // modifier ids of its own rather than reusing the first engraving's, so a pickaxe carrying
        // both gets both: two modifiers with one id would silently be one, and there is no reason
        // to make these a ladder - the first is a block of reach and this is a different way to
        // mine, and a player who paid for both should have paid for both.
        if (Engravings.has(stack, Engraving.SUPER_REACH)) {
            event.addModifier(Attributes.BLOCK_INTERACTION_RANGE,
                    new AttributeModifier(SUPER_REACH_BLOCK_ID, SUPER_REACH_ENGRAVING_BONUS,
                            AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);
            event.addModifier(Attributes.ENTITY_INTERACTION_RANGE,
                    new AttributeModifier(SUPER_REACH_ENTITY_ID, SUPER_REACH_ENGRAVING_BONUS,
                            AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);
        }

        int bonus = CompressionEnergy.bonus(stack);
        if (bonus <= 0) {
            return;
        }

        // Which stat the energy becomes is decided by tag rather than by class, so a new piece of
        // gear earns its bonus by joining #compression_armor or #compression_melee_weapon and
        // nothing here has to change.
        if (stack.is(ModTags.Items.COMPRESSION_ARMOR) && stack.getItem() instanceof ArmorItem armor) {
            event.addModifier(Attributes.MAX_HEALTH,
                    new AttributeModifier(CE_MAX_HEALTH_ID, bonus, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.bySlot(armor.getEquipmentSlot()));
        } else if (stack.is(ModTags.Items.COMPRESSION_MELEE_WEAPON)) {
            event.addModifier(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(CE_ATTACK_DAMAGE_ID, bonus, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);
        }
    }

    /**
     * The Compression Jump set bonus: a step height of one extra block, so the wearer walks up a
     * full block the way a horse does, and Jump Boost III. Neither is a property of any one piece,
     * so this is a tick check on the whole set rather than an attribute on the armour.
     * <p>
     * The effect is re-applied rather than held, and kept short, so taking a piece off lets it lapse
     * within a couple of seconds without this having to track who granted what. A stronger Jump
     * Boost from anywhere else is left alone.
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        boolean fullSet = isJumpPiece(player.getItemBySlot(EquipmentSlot.HEAD))
                && isJumpPiece(player.getItemBySlot(EquipmentSlot.CHEST))
                && isJumpPiece(player.getItemBySlot(EquipmentSlot.LEGS))
                && isJumpPiece(player.getItemBySlot(EquipmentSlot.FEET));

        AttributeInstance stepHeight = player.getAttribute(Attributes.STEP_HEIGHT);
        if (stepHeight != null) {
            boolean applied = stepHeight.getModifier(JUMP_SET_STEP_HEIGHT_ID) != null;
            if (fullSet && !applied) {
                stepHeight.addTransientModifier(
                        new AttributeModifier(JUMP_SET_STEP_HEIGHT_ID, 1.0, AttributeModifier.Operation.ADD_VALUE));
            } else if (!fullSet && applied) {
                stepHeight.removeModifier(JUMP_SET_STEP_HEIGHT_ID);
            }
        }

        if (!fullSet) {
            return;
        }

        MobEffectInstance jump = player.getEffect(MobEffects.JUMP);
        if (jump == null || jump.getAmplifier() < JUMP_SET_AMPLIFIER
                || (jump.getAmplifier() == JUMP_SET_AMPLIFIER && jump.getDuration() < SET_EFFECT_DURATION / 2)) {
            // Not ambient and not a visible particle cloud, but it still shows in the HUD so the
            // wearer can see the set is doing something.
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, SET_EFFECT_DURATION, JUMP_SET_AMPLIFIER,
                    false, false, true));
        }
    }

    /**
     * The Compression Rain set: while it is raining, nothing harmful will stick to the wearer.
     * <p>
     * This is the applicable event rather than a tick handler, and that is the important half - it
     * is fired from {@code LivingEntity#canBeAffected}, which is the one gate every path to an
     * effect goes through, so a potion thrown at the wearer, a mob's curse, a beacon, a lingering
     * cloud and another mod's whatever are all refused in the same place and none of them has to
     * know this set exists.
     * <p>
     * What counts as harmful is {@link MobEffectCategory#HARMFUL} and never a list of effects, the
     * same rule the Potion of Cleansing follows: another mod's effect is refused or allowed by
     * whatever that mod declared it as, and a beneficial effect is untouched however it arrived.
     * <p>
     * The condition is the level raining rather than rain falling on the wearer's own head. It is
     * the simpler statement, it is the one a player can check by looking up, and it means the set is
     * worth the same indoors and out - a set that switched itself off under a roof would be a set
     * nobody could rely on in the fight it was built for.
     */
    /**
     * Corrosion, going on: every application starts a clock of its own.
     * <p>
     * {@code MobEffectEvent.Added} is fired for every application vanilla accepts through
     * {@code canBeAffected}, and - the part that matters here - it is fired <em>before</em> the new
     * instance is merged into whatever was already there, and whether or not that merge changes
     * anything. So a second sting arriving on a target that is already corroding reaches this
     * handler even though vanilla is about to shrug it off, which is exactly what makes corrosion's
     * instances independent. See {@link Corrosion} for what a clock is and why it is not a
     * {@code MobEffect}.
     * <p>
     * Everything that refuses effects still refuses this one, because the applicable event has
     * already run by the time this fires: a Compression Rain set in the rain, a raised Compressed
     * Shield and another mod's immunity all stop the clock before it starts.
     */
    /**
     * Bleeding's real content: nothing heals while it lasts.
     * <p>
     * {@code LivingHealEvent} is fired from {@code LivingEntity#heal}, which is the single door
     * every restoration in the game comes through - Regeneration, a golden apple, a healing potion,
     * a beacon, the natural regeneration a full hunger bar buys, a mob's own passive healing and
     * whatever another mod does - so one cancelled event covers all of them and none of them has to
     * know the effect exists. There is no hook on {@code MobEffect} that could have done this.
     * <p>
     * It is a flat refusal rather than a reduction on purpose. A percentage would be one more number
     * competing with a fight's damage numbers; a lock is a fact about the fight, and it is the whole
     * reason to carry a Compressed Scythe against anything that out-heals what is being done to it.
     */
    @SubscribeEvent
    public static void onBleedingBlocksHealing(LivingHealEvent event) {
        if (event.getEntity().hasEffect(ModMobEffects.BLEEDING)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onCorrosionApplied(MobEffectEvent.Added event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (!event.getEntity().level().isClientSide() && instance.getEffect() == ModMobEffects.CORROSION) {
            Corrosion.apply(event.getEntity(), instance.getAmplifier(), instance.getDuration());
        }
    }

    /**
     * Corrosion, coming off: milk, the Potion of Cleansing, a Compression Rain sweep or anything
     * else that takes the effect away stops every clock with it.
     * <p>
     * {@code Remove} and not {@code Expired}. An instance that expires is the clocks running out on
     * their own, and vanilla's copy of the effect always carries the longest of them - it merges by
     * taking the greater duration - so its last tick is the last clock's last tick. Clearing there
     * would throw away the hit that was due on precisely that tick.
     */
    @SubscribeEvent
    public static void onCorrosionRemoved(MobEffectEvent.Remove event) {
        if (event.getEffect() == ModMobEffects.CORROSION) {
            Corrosion.clear(event.getEntity());
        }
    }

    /**
     * One tick of every corrosion clock on every living thing, which is where the damage actually
     * comes from.
     * <p>
     * It is a tick handler rather than {@code applyEffectTick} because there is no single instance
     * to hang it off: a creature stung six times is carrying six countdowns, and the effect map has
     * room for one. The cheap membership test comes first, so the cost on everything that is not
     * corroding is one lookup in a compound tag.
     */
    @SubscribeEvent
    public static void onCorrosionTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity entity && !entity.level().isClientSide()
                && Corrosion.isCorroding(entity)) {
            Corrosion.tick(entity);
        }
    }

    /**
     * Ghasted, arriving on somebody who already has it: the two durations are summed rather than the
     * longer being kept.
     * <p>
     * {@code Added} fires before vanilla merges the two instances and fires whether or not the merge
     * would change anything, which makes it the one place both durations are still readable. Nothing
     * is written to the effect here - see {@code Ghasted.onAdded} for why the sum is queued and
     * applied a tick later instead.
     */
    @SubscribeEvent
    public static void onGhastedApplied(MobEffectEvent.Added event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (!event.getEntity().level().isClientSide() && instance.getEffect() == ModMobEffects.GHASTED) {
            Ghasted.onAdded(event.getEntity(), event.getOldEffectInstance(), instance);
        }
    }

    /** Ghasted coming off drops the queued sum with it, so a cured effect stays cured. */
    @SubscribeEvent
    public static void onGhastedRemoved(MobEffectEvent.Remove event) {
        if (event.getEffect() == ModMobEffects.GHASTED) {
            Ghasted.onRemoved(event.getEntity());
        }
    }

    /**
     * One tick of the Ghasted flock: the queued sum applied, and the ghasts made up to the level.
     * <p>
     * The ghasts themselves are not ticked here - each one owns its own clock, which is the whole
     * point of the effect - so all this does is count them. The cheap effect lookup comes first, so
     * the cost on everything that is not ghasted is one map read.
     */
    @SubscribeEvent
    public static void onGhastedTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity entity
                && entity.level() instanceof ServerLevel serverLevel
                && Ghasted.isGhasted(entity)) {
            Ghasted.tick(serverLevel, entity);
        }
    }

    @SubscribeEvent
    public static void onRainSetEffectApplicable(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        if (event.getEffectInstance().getEffect().value().getCategory() == MobEffectCategory.HARMFUL
                && entity.level().isRaining()
                && wearsFullSet(entity, ModArmorMaterials.COMPRESSION_RAIN_ARMOR_MATERIAL)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    /**
     * The other half of the same bonus: anything harmful already on the wearer when the rain starts,
     * or on them before the set went on, is cleared.
     * <p>
     * Refusing new effects is not on its own the immunity that was asked for - a player who was
     * already withering when the weather turned would go on withering - so the set sweeps what is
     * there as well. It runs on the same cadence as the other sets' bonuses rather than every tick,
     * because walking a wearer's effect list is the expensive half and a second of Poison is not
     * worth doing it sixty times a second to avoid.
     */
    @SubscribeEvent
    public static void onRainSetTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % RAIN_SET_SWEEP_INTERVAL != 0
                || !player.level().isRaining()
                || !wearsFullSet(player, ModArmorMaterials.COMPRESSION_RAIN_ARMOR_MATERIAL)) {
            return;
        }

        // Collected first: removing an effect writes to the same map this is reading.
        List<Holder<MobEffect>> harmful = new ArrayList<>();
        for (MobEffectInstance effect : player.getActiveEffects()) {
            if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                harmful.add(effect.getEffect());
            }
        }

        for (Holder<MobEffect> effect : harmful) {
            player.removeEffect(effect);
        }
    }

    /**
     * The Compression Lightning set's speed: Speed II for as long as all four pieces are worn. It is
     * re-applied on a short duration rather than held, the same way the Jump set's boost is, so it
     * lapses on its own once a piece comes off.
     */
    @SubscribeEvent
    public static void onLightningSetTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !wearsFullSet(player, ModArmorMaterials.COMPRESSION_LIGHTNING_ARMOR_MATERIAL)) {
            return;
        }

        MobEffectInstance speed = player.getEffect(MobEffects.MOVEMENT_SPEED);
        if (speed == null || speed.getAmplifier() < LIGHTNING_SET_SPEED_AMPLIFIER
                || (speed.getAmplifier() == LIGHTNING_SET_SPEED_AMPLIFIER && speed.getDuration() < SET_EFFECT_DURATION / 2)) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, SET_EFFECT_DURATION,
                    LIGHTNING_SET_SPEED_AMPLIFIER, false, false, true));
        }
    }

    /**
     * The other half of the Compression Lightning set: a strike does a twentieth of what it would.
     * <p>
     * This goes through the damage event rather than through armour, for the reasons in the armour
     * note - armour cannot be aimed at one damage type, and past 100 it starts healing what it is
     * meant to protect. The filter is {@code #minecraft:is_lightning}, so anything another mod
     * deals as lightning is covered too, and the strike's own eight seconds of fire is not: that
     * arrives separately as {@code minecraft:on_fire}, which is a different tag and is left alone.
     * The wearer still burns.
     */
    @SubscribeEvent
    public static void onLightningDamage(LivingIncomingDamageEvent event) {
        if (!event.getSource().is(DamageTypeTags.IS_LIGHTNING) || isTrueDamage(event.getSource())) {
            return;
        }

        LivingEntity target = event.getEntity();

        // Three different things now answer lightning, and only the best of them counts. They are
        // compared as what each *leaves* of the hit, so the strongest is the smallest - and taking a
        // minimum rather than multiplying is what stops a player who has collected all three from
        // being immune to the one damage type this mod hands out most of.
        float left = 1.0F;
        if (wearsFullSet(target, ModArmorMaterials.COMPRESSION_LIGHTNING_ARMOR_MATERIAL)) {
            left = Math.min(left, LIGHTNING_SET_DAMAGE_TAKEN);
        }

        if (Engravings.wornCount(target, Engraving.LIGHTNING_SIGIL) > 0) {
            left = Math.min(left, LIGHTNING_SIGIL_DAMAGE_TAKEN);
        }

        left = Math.min(left, engravingReduction(target, Engraving.LIGHTNING_PROTECTION));
        event.setAmount(event.getAmount() * left);
    }

    /**
     * The other half of the Lightning Sigil: Speed III for as long as the boots are on. Re-applied
     * on a short duration rather than held, the same way the armour sets' effects are, so it lapses
     * on its own the moment the boots come off.
     */
    @SubscribeEvent
    public static void onLightningSigilTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()
                || Engravings.wornCount(player, Engraving.LIGHTNING_SIGIL) == 0) {
            return;
        }

        MobEffectInstance speed = player.getEffect(MobEffects.MOVEMENT_SPEED);
        if (speed == null || speed.getAmplifier() < LIGHTNING_SIGIL_SPEED_AMPLIFIER
                || (speed.getAmplifier() == LIGHTNING_SIGIL_SPEED_AMPLIFIER
                        && speed.getDuration() < SET_EFFECT_DURATION / 2)) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, SET_EFFECT_DURATION,
                    LIGHTNING_SIGIL_SPEED_AMPLIFIER, false, false, true));
        }
    }

    /**
     * The Explosion Sigil: a sword blow landed on something already burning goes off.
     * <p>
     * The blast is worth exactly what the blow was worth - what actually landed, after armour and
     * everything else, which is why this is {@code LivingDamageEvent.Post} like Lifesteal rather
     * than the incoming event - and it is dealt to everything within
     * {@value #EXPLOSION_SIGIL_RADIUS} blocks as {@code minecraft:explosion}, credited to whoever
     * swung. It breaks no ground: this is a weapon, and a sword that cratered the floor every few
     * swings would be unusable indoors.
     * <p>
     * The struck target has its invulnerability window cleared before the blast reaches it, and that
     * is the one liberty taken here. A living entity ignores any hit inside twenty ticks of the last
     * one unless it is larger, and this one is by construction exactly equal - so without that, the
     * blast would land on everything around the target and pass through the target itself.
     */
    @SubscribeEvent
    public static void onExplosionSigilDamage(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)
                || event.getSource().getDirectEntity() != attacker
                || !(attacker.level() instanceof ServerLevel level)) {
            return;
        }

        LivingEntity target = event.getEntity();
        if (!target.isOnFire() || event.getNewDamage() <= 0.0F
                || !Engravings.get(attacker.getMainHandItem()).contains(Engraving.EXPLOSION_SIGIL)) {
            return;
        }

        float damage = event.getNewDamage();
        for (LivingEntity caught : level.getEntitiesOfClass(LivingEntity.class,
                target.getBoundingBox().inflate(EXPLOSION_SIGIL_RADIUS),
                entity -> entity != attacker && entity.isAlive() && !attacker.isAlliedTo(entity))) {
            if (caught == target) {
                caught.invulnerableTime = 0;
            }

            caught.hurt(level.damageSources().explosion(attacker, attacker), damage);
        }

        level.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 0.5, target.getZ(),
                4, 0.6, 0.4, 0.6, 0.0);
        level.playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS, 2.0F, 1.2F);
    }

    /**
     * The Compression Magic set: half of anything a potion did.
     * <p>
     * The filter is {@code #ucc:potion_damage}, which is the mod's own tag rather than a class of
     * damage the game already names - there is no vanilla tag for "a potion did this", and the
     * nearest thing, {@code #bypasses_armor}, would sweep in the void, falling and suffocation with
     * it. What is in the tag and why is on {@code ModTags.DamageTypes.POTION_DAMAGE}.
     * <p>
     * A half rather than the Lightning set's twentieth because this covers five damage types where
     * that covers one, and because most of them are already ignoring the wearer's armour: for
     * magic, wither, the dragon's breath and freezing alike, this set is the only armour there is.
     */
    @SubscribeEvent
    public static void onPotionDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().is(ModTags.DamageTypes.POTION_DAMAGE) && !isTrueDamage(event.getSource())
                && wearsFullSet(event.getEntity(), ModArmorMaterials.COMPRESSION_MAGIC_ARMOR_MATERIAL)) {
            event.setAmount(event.getAmount() * MAGIC_SET_DAMAGE_TAKEN);
        }
    }

    /**
     * The Lifesteal engraving: part of what a melee weapon deals comes back to whoever swung it.
     * <p>
     * This is {@code LivingDamageEvent.Post} rather than the incoming event, so what is healed is
     * what actually landed - after armour, after resistance, after anything else that reduced it -
     * and a blow that was absorbed outright heals nothing. Only a direct swing counts: the attacker
     * has to be the thing that touched the target, so an arrow or an explosion set off by someone
     * holding an engraved sword gives nothing back.
     */
    @SubscribeEvent
    public static void onLifestealDamage(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)
                || event.getSource().getDirectEntity() != attacker
                || attacker.level().isClientSide()
                || !Engravings.has(attacker.getMainHandItem(), Engraving.LIFESTEAL)) {
            return;
        }

        float healed = event.getNewDamage() * LIFESTEAL_FRACTION;
        if (healed > 0F) {
            attacker.heal(healed);
        }
    }

    /**
     * The Quick Draw engraving. {@code ArrowLooseEvent} is what every bow runs its release through
     * and the charge it carries is exactly the number a bow turns into power, so scaling it here
     * shortens the draw with no mixin and nothing added to the bow itself. The engraving only fits
     * the Compressed Cobblestone Bow, so this fires on nothing else - the check is
     * {@code Engravings.has}, which is false for a bow that could never have been engraved.
     */
    @SubscribeEvent
    public static void onArrowLoose(ArrowLooseEvent event) {
        // The Sniper states a whole draw rather than a saving off one, so where a bow carries both
        // it is the Sniper that decides: ten seconds is ten seconds, not seven and a half.
        if (Engravings.has(event.getBow(), Engraving.SNIPER)) {
            event.setCharge(Math.round(event.getCharge() * (float) BowItem.MAX_DRAW_DURATION / SNIPER_DRAW_TICKS));
        } else if (Engravings.has(event.getBow(), Engraving.QUICK_DRAW)) {
            event.setCharge(Math.round(event.getCharge() / (1.0F - QUICK_DRAW_FRACTION)));
        }
    }

    /**
     * The other half of the Sniper engraving: the speed.
     * <p>
     * It is done as the arrow joins the level because that is the one moment both halves are
     * settled - vanilla builds the projectile, gives it its velocity and only then adds it, so the
     * delta here is the finished shot including Power, the draw and any inaccuracy. Multiplying it
     * keeps every one of those, which is why this is a scale rather than a speed.
     * <p>
     * The bow is read off the arrow with {@code getWeaponItem}, so this fires for a dispenser's
     * arrow as readily as a player's, and for another mod's bow as readily as this one's.
     */
    @SubscribeEvent
    public static void onSniperArrowFired(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof AbstractArrow arrow)) {
            return;
        }

        ItemStack bow = arrow.getWeaponItem();
        if (bow != null && Engravings.has(bow, Engraving.SNIPER)) {
            arrow.setDeltaMovement(arrow.getDeltaMovement().scale(SNIPER_VELOCITY_SCALE));
        }
    }

    /**
     * The two Extra Shot engravings: one more projectile out of every shot, or two.
     * <p>
     * There is no hook for "how many projectiles does this weapon draw". Vanilla's count comes out
     * of {@code EnchantmentHelper.processProjectileCount}, which reads an enchantment effect
     * component off the weapon, and an engraving is not an enchantment and has nowhere to put one.
     * So the extra shots are made the only other way they can be: the finished projectile is copied
     * as it joins the level - which, as the Sniper above says, is the one moment a shot exists with
     * its velocity, its Power and its inaccuracy already on it - and the copies are fanned out to
     * either side. A copy through NBT rather than a constructor is what makes this work for an
     * arrow of any kind, a bolt, a TNT Launcher's shell and another mod's projectile without naming
     * any of them; only the UUID has to be replaced, since two entities cannot share one.
     * <p>
     * What it reaches is exactly what reports a weapon it was fired from. That is every
     * {@code AbstractArrow}, which keeps the stack and saves it - but <em>not</em> a crossbow's
     * firework: {@code FireworkRocketEntity} does not override {@code getWeaponItem}, so a crossbow
     * loaded with a rocket is engraved and fires one all the same. That is vanilla's gap rather than
     * this handler's, and it is left alone rather than special-cased, since the moment it is
     * special-cased the rule stops being "whatever knows what fired it".
     * <p>
     * <em>Additive</em> is the reason for the marker on the shooter. Multishot fires three arrows,
     * so cloning each of them would give six rather than four - the volley has to be answered once,
     * and the only thing all of its projectiles have in common is the shooter and the tick. Marking
     * the shooter also covers the copies themselves, which join the level from inside this handler:
     * the tick is already recorded by the time they arrive, so they are skipped by the same test
     * rather than needing a flag of their own.
     * <p>
     * A weapon carrying both rungs fires the deeper one. They are a ladder, not a stack, exactly as
     * the two Vapor Slashes are.
     */
    @SubscribeEvent
    public static void onExtraShotFired(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Projectile projectile)) {
            return;
        }

        ItemStack weapon = projectile.getWeaponItem();
        int extra = weapon == null ? 0 : extraShots(weapon);
        if (extra <= 0 || !(projectile.getOwner() instanceof LivingEntity shooter)) {
            return;
        }

        CompoundTag data = shooter.getPersistentData();
        long volley = event.getLevel().getGameTime() + 1;
        if (data.getLong(EXTRA_SHOT_TICK_TAG) == volley) {
            return;
        }

        data.putLong(EXTRA_SHOT_TICK_TAG, volley);
        for (int i = 0; i < extra; i++) {
            // Ten degrees, then minus ten, then twenty: the fan grows outwards in pairs, which keeps
            // it centred on the shot that was actually aimed however many are added.
            double angle = Math.toRadians(EXTRA_SHOT_SPREAD * (i / 2 + 1) * (i % 2 == 0 ? 1 : -1));
            copyShot(projectile, angle);
        }
    }

    /** How many extra projectiles a weapon's engravings are worth, the deeper rung winning. */
    private static int extraShots(ItemStack weapon) {
        if (Engravings.has(weapon, Engraving.EXTRA_SHOT_2)) {
            return EXTRA_SHOT_2_COUNT;
        }

        return Engravings.has(weapon, Engraving.EXTRA_SHOT) ? EXTRA_SHOT_COUNT : 0;
    }

    /**
     * One more of whatever was just fired, turned {@code angle} radians about the vertical.
     * <p>
     * The copy is made by saving the projectile and loading it into a fresh one of its own type,
     * which carries everything the original had - its owner, its weapon, its damage, its
     * enchantments, its potion, whatever a modded projectile keeps - without this file knowing what
     * any of it is. The UUID is the one thing that must not be copied.
     */
    private static void copyShot(Projectile original, double angle) {
        Entity copy = original.getType().create(original.level());
        if (copy == null) {
            return;
        }

        copy.load(original.saveWithoutId(new CompoundTag()));
        copy.setUUID(UUID.randomUUID());

        // The velocity is taken from the original rather than read back off the copy: Entity#load
        // throws away any component over ten blocks a tick, which is a sane rule for a saved world
        // and would silently drop a Sniper's arrow - seventy-five a tick - to a standstill.
        Vec3 movement = original.getDeltaMovement();
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        copy.setDeltaMovement(movement.x * cos - movement.z * sin, movement.y,
                movement.x * sin + movement.z * cos);
        copy.setYRot(copy.getYRot() + (float) Math.toDegrees(angle));
        copy.yRotO = copy.getYRot();

        original.level().addFreshEntity(copy);
    }

    /**
     * What a piece of the Ultimate Compressed set adds, in the two figures that are not effects:
     * two blocks of step height off the leggings, so the wearer walks up a double step, and Speed
     * III off the boots.
     * <p>
     * The step height is the Compression Jump set's own bonus doubled, and it is well inside the
     * ceiling - {@code STEP_HEIGHT} is a {@code RangedAttribute} that stops at 10.
     */
    private static final double ULTIMATE_STEP_HEIGHT = 2.0;
    private static final int ULTIMATE_SPEED_AMPLIFIER = 2;

    /**
     * The Ultimate Compressed set: four abilities, one to a slot.
     * <p>
     * It is the only set in the mod that is not all-or-nothing, and that is the whole design of it.
     * Every other set here is a bonus that needs four pieces, which means it can never be mixed with
     * anything; this is night vision on the helmet, creative flight on the chestplate, step height
     * on the leggings and speed on the boots, each worth exactly what it is worth on its own, so a
     * player may take one piece of it and keep three of something else. Wearing all four is
     * therefore a choice rather than a requirement.
     * <p>
     * Three of the four follow this file's usual patterns - an effect re-applied on a short duration
     * rather than held, so it lapses on its own when the piece comes off, and a transient attribute
     * modifier under a fixed {@link ResourceLocation}, so adding it twice is the same as adding it
     * once. The flight is the exception and is {@link #updateUltimateFlight}.
     */
    @SubscribeEvent
    public static void onUltimateSetTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        if (isUltimatePiece(player.getItemBySlot(EquipmentSlot.HEAD))) {
            MobEffectInstance nightVision = player.getEffect(MobEffects.NIGHT_VISION);
            if (nightVision == null || nightVision.getDuration() < VISION_EFFECT_DURATION / 2) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, VISION_EFFECT_DURATION,
                        0, false, false, true));
            }
        }

        if (isUltimatePiece(player.getItemBySlot(EquipmentSlot.FEET))) {
            MobEffectInstance speed = player.getEffect(MobEffects.MOVEMENT_SPEED);
            if (speed == null || speed.getAmplifier() < ULTIMATE_SPEED_AMPLIFIER
                    || speed.getDuration() < VISION_EFFECT_DURATION / 2) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, VISION_EFFECT_DURATION,
                        ULTIMATE_SPEED_AMPLIFIER, false, false, true));
            }
        }

        boolean leggings = isUltimatePiece(player.getItemBySlot(EquipmentSlot.LEGS));
        AttributeInstance stepHeight = player.getAttribute(Attributes.STEP_HEIGHT);
        if (stepHeight != null) {
            boolean applied = stepHeight.getModifier(ULTIMATE_SET_STEP_HEIGHT_ID) != null;
            if (leggings && !applied) {
                stepHeight.addTransientModifier(new AttributeModifier(ULTIMATE_SET_STEP_HEIGHT_ID,
                        ULTIMATE_STEP_HEIGHT, AttributeModifier.Operation.ADD_VALUE));
            } else if (!leggings && applied) {
                stepHeight.removeModifier(ULTIMATE_SET_STEP_HEIGHT_ID);
            }
        }

        updateUltimateFlight(player, isUltimatePiece(player.getItemBySlot(EquipmentSlot.CHEST)));
    }

    /**
     * Creative flight from the Ultimate chestplate.
     * <p>
     * It cannot be an effect or an attribute - flight is a player <em>ability</em>, a flag on
     * {@code Player.getAbilities()} that has to be pushed to the client with
     * {@code onUpdateAbilities} - so unlike everything else here it has to be taken away as
     * deliberately as it is given, and the taking away is the whole of the care in this method.
     * <p>
     * Creative and spectator are left alone entirely. Both grant flight themselves and vanilla
     * rewrites the whole ability set when the game mode changes, so touching the flag there would
     * mean a chestplate that could switch a creative player's flight off. Outside those two,
     * {@code mayfly} is treated as this set's to own: it is set to match the chestplate, and
     * {@code flying} is cleared with it so taking the piece off in mid-air drops the wearer rather
     * than leaving them hanging with no way to move.
     * <p>
     * The one thing that is not handled, because it cannot be from here: another mod that also
     * grants survival flight through the same flag will fight this one for it, tick by tick. There
     * is no per-source ownership on an ability the way there is on an attribute modifier.
     */
    private static void updateUltimateFlight(Player player, boolean wearing) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        Abilities abilities = player.getAbilities();
        if (abilities.mayfly == wearing) {
            return;
        }

        abilities.mayfly = wearing;
        if (!wearing) {
            abilities.flying = false;
        }

        player.onUpdateAbilities();
    }

    /** Whether {@code stack} is a piece of the Ultimate set, by the material it is made of. */
    private static boolean isUltimatePiece(ItemStack stack) {
        return isPieceOf(stack, ModArmorMaterials.ULTIMATE_COMPRESSED_ARMOR_MATERIAL);
    }

    /**
     * The Vision engraving: night vision for as long as the helmet is worn. Re-applied on a short
     * duration rather than held, the way both set effects are, so it lapses on its own once the
     * helmet comes off and nothing has to track who granted it.
     */
    @SubscribeEvent
    public static void onVisionEngravingTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()
                || !Engravings.has(player.getItemBySlot(EquipmentSlot.HEAD), Engraving.VISION)) {
            return;
        }

        MobEffectInstance nightVision = player.getEffect(MobEffects.NIGHT_VISION);
        if (nightVision == null || nightVision.getDuration() < VISION_EFFECT_DURATION / 2) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, VISION_EFFECT_DURATION, 0,
                    false, false, true));
        }
    }

    /**
     * The Jump engraving: Jump Boost X for as long as the engraved leggings are worn. Re-applied on
     * a short duration rather than held, the way every other effect in this file is, so it lapses on
     * its own when the leggings come off.
     */
    @SubscribeEvent
    public static void onJumpEngravingTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()
                || !Engravings.has(player.getItemBySlot(EquipmentSlot.LEGS), Engraving.JUMP)) {
            return;
        }

        MobEffectInstance jump = player.getEffect(MobEffects.JUMP);
        if (jump == null || jump.getAmplifier() < JUMP_ENGRAVING_AMPLIFIER
                || jump.getDuration() < JUMP_EFFECT_DURATION / 2) {
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, JUMP_EFFECT_DURATION,
                    JUMP_ENGRAVING_AMPLIFIER, false, false, true));
        }
    }

    /**
     * The Dash engraving: right-clicking an engraved katana carries the wielder
     * {@link #DASH_DISTANCE} blocks forward and cuts everything on the way through.
     * <p>
     * The move is a <em>clipped</em> teleport rather than a shove, and both halves of that are
     * deliberate. Clipped, because a dash that went through walls would be a blink and this is a
     * step - the ray is cast at chest height and the landing is pulled {@link #DASH_WALL_MARGIN}
     * short of whatever it found, so the dash stops against a wall instead of ending inside one. A
     * teleport rather than velocity, because five blocks means five blocks: momentum would make the
     * distance depend on what the player was standing on.
     * <p>
     * It is horizontal, taking the look direction with the pitch thrown away. Aiming a five block
     * dash at the sky or at the floor is not a manoeuvre anyone wants, and a horizontal one is the
     * only version a player can place accurately while fighting.
     * <p>
     * The cut is a swept test against the line actually travelled, not a box around the destination:
     * the whole point is that everything between here and there is hit, and at five blocks in one
     * tick a box at either end would miss the middle entirely - the same reason the Compressed
     * Chicken Boss's ram is swept.
     */
    @SubscribeEvent
    public static void onDashEngravingUse(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        // A katana carrying a Vapor Slash as well fires that instead. Both are right-click abilities
        // on one item sharing one cooldown, so without this the two would race and whichever handler
        // happened to run first would win - which is not a thing a player can be told.
        if (!Engravings.has(stack, Engraving.DASH)
                || vaporSlashOn(stack) != null
                || Engravings.has(stack, Engraving.OMNI_SLASH)
                || player.getCooldowns().isOnCooldown(stack.getItem())) {
            return;
        }

        player.getCooldowns().addCooldown(stack.getItem(), DASH_COOLDOWN);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide()));

        // The client's copy is corrected by the teleport packet the server sends, so the whole move
        // is worked out once, on the side that owns the answer.
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        Vec3 heading = player.getLookAngle().multiply(1.0, 0.0, 1.0);
        if (heading.lengthSqr() < 1.0E-4) {
            // Looking straight up or straight down: fall back on the way the body is facing.
            heading = Vec3.directionFromRotation(0.0F, player.getYRot());
        }

        heading = heading.normalize();
        Vec3 from = player.position().add(0.0, player.getBbHeight() / 2.0, 0.0);
        Vec3 wanted = from.add(heading.scale(DASH_DISTANCE));

        BlockHitResult wall = player.level().clip(new ClipContext(from, wanted,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 to = wall.getType() == HitResult.Type.MISS
                ? wanted
                : wall.getLocation().subtract(heading.scale(DASH_WALL_MARGIN));

        cutAlong(serverPlayer, from, to);

        serverPlayer.connection.teleport(to.x, player.getY(), to.z, player.getYRot(), player.getXRot());
        serverPlayer.resetFallDistance();

        ServerLevel level = serverPlayer.serverLevel();
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, from.x, from.y, from.z, 8, 0.4, 0.2, 0.4, 0.0);
        level.sendParticles(ParticleTypes.CRIT, to.x, from.y, to.z, 20, 0.4, 0.4, 0.4, 0.2);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.5F, 1.4F);
    }

    /** One swing's worth of the wielder's own attack damage to everything along the dash. */
    private static void cutAlong(ServerPlayer player, Vec3 from, Vec3 to) {
        cutAlong(player, from, to, (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE)
                * DASH_DAMAGE_MULTIPLIER), DASH_WIDTH);
    }

    /**
     * The cut itself, shared by the Dash and the Vapor Slashes: {@code damage} to everything within
     * {@code width} of the line actually travelled.
     * <p>
     * It is a swept test against that line rather than a box around either end, because the whole
     * point is that everything <em>between</em> here and there is hit and five blocks are covered in
     * one tick - the same reason the Compressed Chicken Boss's ram is swept.
     */
    private static void cutAlong(ServerPlayer player, Vec3 from, Vec3 to, float damage, double width) {
        for (LivingEntity caught : player.level().getEntitiesOfClass(LivingEntity.class,
                Sweep.bounds(from, to, width),
                entity -> entity != player && entity.isAlive() && !(entity instanceof ArmorStand)
                        && !player.isAlliedTo(entity)
                        && Sweep.caught(entity, from, to, width))) {
            // As a player attack, so armour, Protection and Resistance answer it the way they would
            // answer the swing this is standing in for.
            caught.hurt(player.damageSources().playerAttack(player), damage);
        }
    }

    /**
     * The Vapor Slash engravings: the Dash again, with the world in the way of it.
     * <p>
     * The move is the Dash's - a clipped, horizontal teleport rather than a shove, for the reasons
     * that method gives - with the clip taken in the other order. The hole is carved <em>first</em>
     * and the ray cast afterwards, so the slash travels its full {@link #VAPOR_SLASH_DISTANCE}
     * through anything it is able to break and is still stopped dead by anything it is not. Bedrock,
     * the world's ceiling and this mod's own hardened compression levels therefore hold a player in
     * exactly as they would without the engraving, and nothing had to name them: they are the blocks
     * left standing after the carve, so the same ray answers both questions.
     * <p>
     * The blocks are <em>mined</em>, not deleted. Each one is dropped through
     * {@code Block.dropResources} against {@link #VAPOR_SLASH_TOOL}, so what comes out is what a
     * pickaxe would have got - cobblestone from stone, nothing from a leaf, and every other mod's
     * loot table honoured, since the drop is that block's own rather than a copy of it. Fortune and
     * Silk Touch are not involved: the tool is a bare pickaxe, so a slash is a fast way to move rock
     * and never a better way to mine it.
     * <p>
     * Blocks with a block entity are the one exception and are left standing. That is the rule the
     * Succ TNT's singularity already follows, and it matters more here: this is an ability a player
     * fires every second at head height, and a version of it that emptied chests, shulkers and
     * furnaces into the gravel would be unusable anywhere near a base.
     * <p>
     * A capsule of radius three, three blocks tall and {@link #VAPOR_SLASH_DISTANCE} long is a few
     * hundred blocks - two orders short of anything {@code DeferredFill} exists for - so it is
     * written on the tick it is asked for. Carving before the teleport is also what stops the player
     * being moved into a block that was about to be broken anyway.
     */
    @SubscribeEvent
    public static void onVaporSlashEngravingUse(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        Engraving slash = vaporSlashOn(stack);
        if (slash == null || Engravings.has(stack, Engraving.OMNI_SLASH)
                || player.getCooldowns().isOnCooldown(stack.getItem())) {
            return;
        }

        player.getCooldowns().addCooldown(stack.getItem(),
                slash == Engraving.VAPOR_SLASH_2 ? VAPOR_SLASH_2_COOLDOWN : VAPOR_SLASH_COOLDOWN);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide()));

        // The client's copy is corrected by the teleport packet and the block updates the server
        // sends, so the whole move is worked out once, on the side that owns the answer.
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        Vec3 heading = player.getLookAngle().multiply(1.0, 0.0, 1.0);
        if (heading.lengthSqr() < 1.0E-4) {
            // Looking straight up or straight down: fall back on the way the body is facing.
            heading = Vec3.directionFromRotation(0.0F, player.getYRot());
        }

        heading = heading.normalize();
        Vec3 from = player.position().add(0.0, player.getBbHeight() / 2.0, 0.0);
        Vec3 wanted = from.add(heading.scale(VAPOR_SLASH_DISTANCE));

        carveAlong(serverPlayer, from, wanted);

        // Cast the ray only now, against whatever the carve could not take: an unbreakable block is
        // still a wall, and everything else has already stopped being one.
        BlockHitResult wall = player.level().clip(new ClipContext(from, wanted,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 to = wall.getType() == HitResult.Type.MISS
                ? wanted
                : wall.getLocation().subtract(heading.scale(DASH_WALL_MARGIN));

        float damage = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE)
                * (slash == Engraving.VAPOR_SLASH_2
                        ? VAPOR_SLASH_2_DAMAGE_MULTIPLIER
                        : VAPOR_SLASH_DAMAGE_MULTIPLIER));
        cutAlong(serverPlayer, from, to, damage, VAPOR_SLASH_RADIUS);

        serverPlayer.connection.teleport(to.x, player.getY(), to.z, player.getYRot(), player.getXRot());
        serverPlayer.resetFallDistance();

        ServerLevel level = serverPlayer.serverLevel();
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, from.x, from.y, from.z, 16, 1.0, 0.5, 1.0, 0.0);
        level.sendParticles(ParticleTypes.CLOUD, to.x, from.y, to.z, 40, 1.2, 0.6, 1.2, 0.05);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 2.0F, 0.7F);
    }

    /**
     * Which slash a katana carries, or null. The deeper rung wins outright rather than the two
     * stacking or racing: they are the same ability at two settings, and a katana that has been
     * given both should behave like the better one and not like whichever handler ran first.
     */
    @Nullable
    private static Engraving vaporSlashOn(ItemStack stack) {
        if (Engravings.has(stack, Engraving.VAPOR_SLASH_2)) {
            return Engraving.VAPOR_SLASH_2;
        }

        return Engravings.has(stack, Engraving.VAPOR_SLASH) ? Engraving.VAPOR_SLASH : null;
    }

    /**
     * Mines out the capsule the slash travelled through: everything within
     * {@link #VAPOR_SLASH_RADIUS} of the line and {@link #VAPOR_SLASH_HEIGHT} blocks tall, measured
     * from the wielder's feet.
     * <p>
     * The candidate box is walked and each position tested against the line, rather than the line
     * being stepped along, so every block is looked at exactly once however the slash is aimed -
     * stepping would visit the same block several times on a diagonal and miss others at the corners.
     */
    private static void carveAlong(ServerPlayer player, Vec3 from, Vec3 to) {
        ServerLevel level = player.serverLevel();
        int feet = Mth.floor(player.getY());

        int minX = Mth.floor(Math.min(from.x, to.x) - VAPOR_SLASH_RADIUS);
        int maxX = Mth.floor(Math.max(from.x, to.x) + VAPOR_SLASH_RADIUS);
        int minZ = Mth.floor(Math.min(from.z, to.z) - VAPOR_SLASH_RADIUS);
        int maxZ = Mth.floor(Math.max(from.z, to.z) + VAPOR_SLASH_RADIUS);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                // The column's middle against the line the slash ran along, flattened, so the hole
                // is the same width at every height rather than a sphere.
                if (distanceToSegmentSqr(x + 0.5, z + 0.5, from, to)
                        > VAPOR_SLASH_RADIUS * VAPOR_SLASH_RADIUS) {
                    continue;
                }

                for (int y = feet; y < feet + VAPOR_SLASH_HEIGHT; y++) {
                    pos.set(x, y, z);
                    if (!level.isLoaded(pos)) {
                        continue;
                    }

                    BlockState state = level.getBlockState(pos);

                    // Air is nothing to break; a negative destroy speed is bedrock, the world's
                    // ceiling and this mod's hardened levels, which is how those stay unbreakable
                    // without being named here; and a block entity is a container, left alone.
                    if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0F
                            || state.hasBlockEntity()) {
                        continue;
                    }

                    // Removed first and dropped after, which is the order a player breaking a block
                    // goes in - vanilla's own playerDestroy drops once the state has already gone.
                    BlockPos at = pos.immutable();
                    level.destroyBlock(at, false, player);
                    Block.dropResources(state, level, at, null, player, VAPOR_SLASH_TOOL);
                }
            }
        }
    }

    /** How far {@code (x, z)} is from the segment {@code from} to {@code to}, flattened. Squared. */
    private static double distanceToSegmentSqr(double x, double z, Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        double lengthSqr = dx * dx + dz * dz;

        double along = lengthSqr < 1.0E-8
                ? 0.0
                : Mth.clamp(((x - from.x) * dx + (z - from.z) * dz) / lengthSqr, 0.0, 1.0);

        double offX = x - (from.x + along * dx);
        double offZ = z - (from.z + along * dz);
        return offX * offX + offZ * offZ;
    }

    /**
     * The Omni Slash engraving: right-clicking an engraved melee weapon looses a crescent five
     * blocks tall that flies until it stops being ticked, breaking every block short of bedrock it
     * crosses and cutting everything alive on the way for one standard attack of true damage.
     * <p>
     * Almost nothing of the ability is here. What the crescent does is entirely
     * {@code OmniSlashEntity}'s, which is the right place for it: the carve and the cut are both
     * measured against the stretch of line travelled in a tick, and there is no such stretch until
     * the thing is flying. All this method owns is the two figures the entity cannot work out for
     * itself - where it was aimed, and what the weapon that loosed it hits for.
     * <p>
     * It is aimed with the full look direction, pitch and all, unlike the Dash and the Vapor Slash
     * next to it. Those two are moves and a move that can be aimed at the sky is one nobody can
     * place; this is a shot, and a shot that could only be fired at the horizon would be the one
     * ability in the mod that cannot open a hole in the floor.
     * <p>
     * There is no attack-cooldown business of the kind the Scythe Wave has. That engraving replaces
     * the swing, so it has to spend one; this is a right-click ability like every other one in this
     * file, and the item cooldown is what prices it - which is also why the cooldown, and not a flag
     * of this file's own, is the lockout: the client checks {@code ItemCooldowns} before it posts
     * the event at all.
     */
    @SubscribeEvent
    public static void onOmniSlashEngravingUse(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        if (!Engravings.has(stack, Engraving.OMNI_SLASH)
                || player.getCooldowns().isOnCooldown(stack.getItem())) {
            return;
        }

        player.getCooldowns().addCooldown(stack.getItem(), OMNI_SLASH_COOLDOWN);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide()));

        // Only the server looses anything; the crescent arrives on the client as an ordinary tracked
        // entity, and the blocks it takes arrive as ordinary block updates.
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ServerLevel level = serverPlayer.serverLevel();
        OmniSlashEntity slash = new OmniSlashEntity(level, serverPlayer, stack);
        slash.setDamage((float) (serverPlayer.getAttributeValue(Attributes.ATTACK_DAMAGE)
                * OMNI_SLASH_DAMAGE_MULTIPLIER));

        // Fired from the eyes rather than from the feet, so where it goes is where the crosshair was
        // pointing, and with no inaccuracy at all - this is a swing being thrown, not a shot.
        //
        // Started an arm's length out in front rather than on top of the wielder. The carve is a
        // capsule of radius two and a half about the line travelled, so a crescent born at the eyes
        // would take the floor out from under the player who loosed it before it had gone anywhere;
        // OMNI_SLASH_MUZZLE is exactly the distance that puts the near end of that capsule at their
        // feet instead of under them.
        slash.setPos(serverPlayer.getEyePosition()
                .add(serverPlayer.getLookAngle().scale(OMNI_SLASH_MUZZLE)));
        slash.shootFromRotation(serverPlayer, serverPlayer.getXRot(), serverPlayer.getYRot(), 0.0F,
                OmniSlashEntity.SPEED, 0.0F);

        level.addFreshEntity(slash);

        serverPlayer.causeFoodExhaustion(0.3F);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, serverPlayer.getX(),
                serverPlayer.getY(0.6), serverPlayer.getZ(), 24, 1.5, 1.0, 1.5, 0.0);
        level.playSound(null, serverPlayer.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 3.0F, 0.5F);
    }

    /**
     * The Uppercut engraving: whatever an engraved melee weapon hits goes a hundred blocks up.
     * <p>
     * {@code LivingDamageEvent.Post} rather than an attack event, so it fires only when a hit
     * actually landed - a swing that was blocked, missed or refused by invulnerability should not
     * throw anybody. The weapon is read off the attacker's main hand, which is the hand the swing
     * came out of.
     * <p>
     * The horizontal velocity is kept and only the vertical is written, so a target that was running
     * carries on in the direction it was going and the throw reads as a launch rather than as a
     * teleport onto a pillar of air. {@code hurtMarked} is what actually sends it: delta movement
     * set on the server reaches the client only when the entity tracker is told the velocity
     * changed, and for a player it is their own client that does the moving.
     * <p>
     * The fall damage on the way down is deliberately left alone. It is most of what the engraving
     * is worth - and against a Compressed Snow Golem, which is invulnerable to everything else, it
     * is the only way in.
     */
    @SubscribeEvent
    public static void onUppercutEngravingHit(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)
                || attacker.level().isClientSide()) {
            return;
        }

        LivingEntity target = event.getEntity();
        if (target != attacker && Engravings.has(attacker.getMainHandItem(), Engraving.UPPERCUT)) {
            UPPERCUT_PENDING.add(target.getUUID());
        }
    }

    /**
     * The throw itself, one tick late and deliberately so.
     * <p>
     * It cannot be done where it is decided. {@code LivingDamageEvent.Post} is fired from inside
     * {@code actuallyHurt}, and {@code LivingEntity#hurt} applies its own knockback <em>after</em>
     * that returns - {@code knockback} writes {@code onGround ? Math.min(0.4, y / 2 + strength) : y}
     * into the vertical, so a velocity of 5.33 set from that event is flattened to 0.4 a few lines
     * later and the engraving does nothing at all. Vanilla's attack path then adds more knockback on
     * top after {@code hurt} has returned.
     * <p>
     * So the target is only marked there, and thrown from its own tick, which is always after every
     * part of the attack that resolved it. The wait is at most one tick and is not visible.
     */
    @SubscribeEvent
    public static void onUppercutLaunch(EntityTickEvent.Post event) {
        if (UPPERCUT_PENDING.isEmpty() || !(event.getEntity() instanceof LivingEntity target)
                || target.level().isClientSide() || !UPPERCUT_PENDING.remove(target.getUUID())) {
            return;
        }

        // The horizontal is kept, so a target that was running carries on the way it was going and
        // the throw reads as a launch rather than as a teleport onto a pillar of air. hurtMarked is
        // what actually sends it: a velocity written on the server reaches the client only when the
        // tracker is told it changed, and for a player it is their own client that does the moving.
        Vec3 movement = target.getDeltaMovement();
        target.setDeltaMovement(movement.x, UPPERCUT_VELOCITY, movement.z);
        target.hasImpulse = true;
        target.hurtMarked = true;
        target.resetFallDistance();

        target.level().playSound(null, target.blockPosition(), SoundEvents.WIND_CHARGE_THROW,
                SoundSource.PLAYERS, 1.5F, 0.5F);
    }

    /**
     * The Exploding Sword engraving: right-clicking clears a five block sphere, up to and including
     * tier 205 compressed cobblestone.
     * <p>
     * The blast and the hole are two separate things, and they have to be. A real
     * {@code Level.explode} is fired for the damage, the knockback, the particles and the noise, but
     * with {@code ExplosionInteraction.NONE} so it touches no blocks at all - because every hardened
     * compression level carries an explosion resistance of three and a half million and there is no
     * power setting that gets through one. The blocks are then taken by hand, each one tested
     * against {@link #EXPLODING_SWORD_MAX_LEVEL} through {@code ModBlocks.levelOf}, which is what
     * lets the sword be exactly strong enough to break one tier and not the next.
     * <p>
     * Blocks that are not compressed cobblestone at all fall back on their destroy speed, so bedrock
     * and the world's ceiling are refused the way they are everywhere else in this mod, and
     * everything ordinary goes. Nothing drops - see the engraving's own note for why a sphere this
     * size dropping what it broke would be a mine rather than an ability. A container's contents
     * still spill, because {@code destroyBlock} is what removes each block and a block entity gives
     * its contents up on removal; that is the same thing a TNT does to a chest and is not a way of
     * printing anything.
     */
    @SubscribeEvent
    public static void onExplodingSwordEngravingUse(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        if (!Engravings.has(stack, Engraving.EXPLODING_SWORD)
                || Engravings.has(stack, Engraving.OMNI_SLASH)
                || player.getCooldowns().isOnCooldown(stack.getItem())) {
            return;
        }

        player.getCooldowns().addCooldown(stack.getItem(), EXPLODING_SWORD_COOLDOWN);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide()));

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        // Everything but the blocks: damage, shove, particles and sound.
        level.explode(player, player.getX(), player.getY(), player.getZ(),
                EXPLODING_SWORD_POWER, Level.ExplosionInteraction.NONE);

        int radius = Mth.ceil(EXPLODING_SWORD_RADIUS);
        double radiusSqr = EXPLODING_SWORD_RADIUS * EXPLODING_SWORD_RADIUS;
        BlockPos centre = player.blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z > radiusSqr) {
                        continue;
                    }

                    pos.set(centre.getX() + x, centre.getY() + y, centre.getZ() + z);
                    if (!level.isLoaded(pos) || level.getBlockState(pos).isAir()) {
                        continue;
                    }

                    if (reachedByExplodingSword(level, pos)) {
                        level.destroyBlock(pos.immutable(), false, player);
                    }
                }
            }
        }
    }

    /**
     * Whether the Exploding Sword takes the block at {@code pos}.
     * <p>
     * Compressed cobblestone is answered by its level, so the cut is exact and lands between two
     * tiers that are otherwise identical to the game. Everything else is answered by its destroy
     * speed, which is the mod's usual test for "is this bedrock" and covers the world's ceiling and
     * every other mod's unbreakable block for free.
     */
    private static boolean reachedByExplodingSword(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Integer compression = ModBlocks.levelOf(state.getBlock());

        return compression != null
                ? compression <= EXPLODING_SWORD_MAX_LEVEL
                : state.getDestroySpeed(level, pos) >= 0.0F;
    }

    /**
     * The Compressed Shield's two extra blocks: lightning and potion damage, refused while it is
     * raised.
     * <p>
     * See {@link CompressedShieldItem} for why neither can be done with a tag. In short, the two
     * fail for opposite reasons - lightning because its damage source carries no position for
     * vanilla to measure an angle against, and potion damage because {@code magic} and
     * {@code indirect_magic} are inside {@code #minecraft:bypasses_armor} and so inside
     * {@code #minecraft:bypasses_shield} - and both types belong to the whole game rather than to
     * this shield, so the exception has to be stated here and only while the shield is up.
     * <p>
     * No angle is tested, unlike vanilla's own blocking. That is not a shortcut: a lightning source
     * has no position, so there is no direction to test it against, and applying an arc to the
     * potion half alone would make one of the two shield's blocks conditional and the other not for
     * reasons no player could see. Raised is raised.
     * <p>
     * Entity event 29 is the shield-block sound and the little knock on the shield, sent so the
     * block reads as the shield doing something rather than as damage that failed to arrive.
     */
    @SubscribeEvent
    public static void onCompressedShieldBlock(LivingIncomingDamageEvent event) {
        LivingEntity blocker = event.getEntity();
        if (!blocker.isBlocking() || !(blocker.getUseItem().getItem() instanceof CompressedShieldItem)) {
            return;
        }

        DamageSource source = event.getSource();
        if (isTrueDamage(source)
                || !source.is(DamageTypeTags.IS_LIGHTNING) && !source.is(ModTags.DamageTypes.POTION_DAMAGE)) {
            return;
        }

        event.setCanceled(true);
        blocker.level().broadcastEntityEvent(blocker, (byte) 29);
    }

    /**
     * The other half of blocking potions: the effects a splash bottle carries, refused while the
     * shield is up.
     * <p>
     * A potion is two separate things arriving - a little damage, if it is a harming one, and a
     * list of effects - and a shield that stopped only the first would not be blocking potions in
     * any sense a player means it. {@code MobEffectEvent.Applicable} is fired from
     * {@code LivingEntity#canBeAffected}, which is the one gate every path to an effect goes
     * through, so this covers a thrown bottle, a lingering cloud, a witch and another mod's curse
     * without any of them knowing the shield exists.
     * <p>
     * Filtered by {@link MobEffectCategory} rather than by a list of effects, so another mod's
     * effect is refused or allowed by that mod's own declaration - and so that a beneficial splash
     * potion still reaches somebody sheltering behind the shield, which is the whole point of
     * splashing one at a friend.
     */
    @SubscribeEvent
    public static void onCompressedShieldRefusesEffect(MobEffectEvent.Applicable event) {
        LivingEntity blocker = event.getEntity();
        if (blocker.isBlocking() && blocker.getUseItem().getItem() instanceof CompressedShieldItem
                && event.getEffectInstance().getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    /**
     * A boss dies and whoever killed it walks away with ten minutes of its stone.
     * <p>
     * {@code getKillCredit} rather than the damage source's own entity, because that is vanilla's
     * answer to "who killed this" and it already knows about the cases a raw source does not - a kill
     * with an arrow, a kill by something the player set on fire, a kill inside the hundred ticks
     * vanilla remembers a fight for. It is one player rather than everyone nearby: the spoil is the
     * kill, and spreading it over a radius would make standing near a boss fight worth as much as
     * being in one.
     * <p>
     * Which boss is worth what is not asked here at all - {@link BossSpoil#of} answers it, and a mob
     * that is not in that table is not a boss and is worth nothing.
     */
    @SubscribeEvent
    public static void onBossKilledForSpoils(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        BossSpoil spoil = BossSpoil.of(event.getEntity().getType());
        if (spoil == null || !(event.getEntity().getKillCredit() instanceof Player player)) {
            return;
        }

        // Not ambient, and its particles are shown: this is a reward with a clock on it, and a
        // player has to be able to see how much of it is left without opening their inventory.
        player.addEffect(new MobEffectInstance(ModMobEffects.SPOILS.get(spoil), BossSpoil.DURATION,
                0, false, true, true));
    }

    /**
     * What that effect actually does: stone broken by the holder comes up as the boss's own
     * compression level instead of as cobblestone.
     * <p>
     * The swap is written from both ends and needs both, because either alone is wrong. The block
     * has to be in {@code #c:stones} - NeoForge's own family of andesite, diorite, granite, stone,
     * deepslate and tuff, plus whatever another mod puts in it - and the drop has to be plain
     * cobblestone. Gating on the drop alone would turn cobblestone itself into deeper cobblestone,
     * which is a printer rather than a reward; gating on the block alone would rewrite a Silk Touch
     * pick's stone, or deepslate's cobbled deepslate, into something they never were. Between the
     * two, exactly one thing changes: the cobblestone that comes off stone.
     * <p>
     * {@code BlockDropsEvent} is where this belongs rather than {@code BreakEvent}, because the
     * drops have been decided by then and are a mutable list - so Fortune, Silk Touch, the block's
     * own loot table and every other mod's loot modifier have all already had their say, and this
     * only has to rename what came out. The count is untouched: one stone is one block of deep
     * stone, and nothing here multiplies.
     * <p>
     * A player holding several spoils keeps the best of them. Killing a second boss is not supposed
     * to be a downgrade, and holding two effects that disagree about the answer needs a rule rather
     * than an iteration order.
     */
    @SubscribeEvent
    public static void onStoneMinedWithSpoils(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof LivingEntity breaker)
                || !event.getState().is(Tags.Blocks.STONES)) {
            return;
        }

        BossSpoil best = deepestSpoil(breaker);
        if (best == null) {
            return;
        }

        Item deep = ModBlocks.byLevel(best.level()).get().asItem();
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (stack.is(Items.COBBLESTONE)) {
                drop.setItem(new ItemStack(deep, stack.getCount()));
            }
        }
    }

    /** The deepest compression level {@code entity} is currently carrying a spoil for, or null. */
    @Nullable
    private static BossSpoil deepestSpoil(LivingEntity entity) {
        BossSpoil best = null;
        for (BossSpoil spoil : BossSpoil.values()) {
            if (entity.hasEffect(ModMobEffects.SPOILS.get(spoil))
                    && (best == null || spoil.level() > best.level())) {
                best = spoil;
            }
        }

        return best;
    }

    /**
     * The Smash engraving, first half: right-clicking an engraved mace in mid-air fires the player
     * at the ground.
     * <p>
     * It is an interaction event rather than an item override because the engraving is written
     * against {@code #minecraft:enchantable/mace} - another mod's mace is a class this file has
     * never heard of, and there is nothing to override on it. The cooldown is put on the item so
     * vanilla draws the sweep over the icon for free, and it is started here rather than on landing:
     * a slam that missed the ground should still have cost something.
     */
    @SubscribeEvent
    public static void onSmashEngravingUse(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        if (!Engravings.has(stack, Engraving.SMASH) || player.onGround()
                || Engravings.has(stack, Engraving.OMNI_SLASH)
                || player.getCooldowns().isOnCooldown(stack.getItem())) {
            return;
        }

        player.getCooldowns().addCooldown(stack.getItem(), SMASH_COOLDOWN);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide()));

        // Straight down and nothing else: whatever sideways speed the player had would otherwise
        // carry the landing away from where they were aiming.
        player.setDeltaMovement(0.0, -SMASH_SPEED, 0.0);
        player.hasImpulse = true;
        player.hurtMarked = true;

        if (!player.level().isClientSide()) {
            SMASHING.add(player.getUUID());
            player.level().playSound(null, player.blockPosition(), SoundEvents.WIND_CHARGE_THROW,
                    SoundSource.PLAYERS, 2.0F, 0.6F);
        }
    }

    /**
     * The Smash engraving, second half: the landing.
     * <p>
     * The fall distance is read off the event rather than remembered from the right click, which is
     * what makes the number honest - it is the distance actually fallen, so a slam interrupted by a
     * ledge is worth the ledge and not the drop that was intended. The damage is a straight line in
     * that distance with nothing capping it - see {@link #SMASH_DAMAGE_PER_BLOCK}.
     * <p>
     * The <em>radius</em> is still bounded, and that is not an oversight left over from the damage:
     * it is the size of an entity query run on the tick of a landing, and an unbounded one would
     * walk every entity in the world.
     * <p>
     * The player's own fall damage is cancelled. That is the point of a slam rather than a mercy:
     * vanilla's mace already does exactly this when its smash attack connects, and a slam that
     * killed the person performing it would be a trap rather than an ability.
     */
    @SubscribeEvent
    public static void onSmashEngravingLand(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()
                || !SMASHING.remove(player.getUUID())) {
            return;
        }

        event.setCanceled(true);

        float fall = event.getDistance();
        float damage = SMASH_BASE_DAMAGE + SMASH_DAMAGE_PER_BLOCK * fall;
        double radius = Math.min(SMASH_MIN_RADIUS + fall * SMASH_RADIUS_PER_BLOCK, SMASH_MAX_RADIUS);

        for (LivingEntity caught : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(radius, radius / 2.0, radius),
                entity -> entity != player && entity.isAlive() && !(entity instanceof ArmorStand)
                        && !player.isAlliedTo(entity))) {
            // As a player attack, so armour, Protection, Resistance and every enchantment on the
            // mace's owner apply to it exactly as they would to a swing.
            caught.hurt(player.damageSources().playerAttack(player), damage);

            Vec3 away = caught.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
            if (away.lengthSqr() > 1.0E-4) {
                Vec3 shove = away.normalize();
                caught.push(shove.x, 0.4, shove.z);
            }
        }

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY(), player.getZ(),
                    (int) radius * 2, radius / 3.0, 0.2, radius / 3.0, 0.0);
            serverLevel.sendParticles(ParticleTypes.GUST, player.getX(), player.getY(), player.getZ(),
                    (int) radius, radius / 2.0, 0.1, radius / 2.0, 0.0);
        }

        player.level().playSound(null, player.blockPosition(), SoundEvents.MACE_SMASH_GROUND,
                SoundSource.PLAYERS, 4.0F, 0.7F);
    }

    /**
     * Drops the Smash mark when the fall is over by some means other than a landing - water, a boat,
     * a climb back up, creative flight. {@code LivingFallEvent} only fires when there is fall damage
     * to answer for, so without this a slam that ended in a lake would leave its owner armed with a
     * landing that went off the next time they stubbed a toe.
     */
    @SubscribeEvent
    public static void onSmashLandingTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || SMASHING.isEmpty()) {
            return;
        }

        if (player.onGround() || player.isInWater() || player.isInLava() || player.getAbilities().flying) {
            SMASHING.remove(player.getUUID());
        }
    }

    /**
     * The Haste engraving: Haste IV for as long as an engraved Compressed Katana is in the main
     * hand. Re-applied on a short duration rather than held, the way the Vision engraving and both
     * set effects are, so it lapses on its own the moment the katana is put away.
     * <p>
     * The main hand and not either hand, deliberately: the katana's reach and its swing speed are
     * {@code EquipmentSlotGroup.MAINHAND} modifiers, so that is already the only hand in which the
     * weapon is anything, and an engraving that worked in the other one would be the odd rule out.
     */
    @SubscribeEvent
    public static void onHasteEngravingTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        int amplifier = hasteAmplifier(player.getMainHandItem());
        if (amplifier < 0) {
            return;
        }

        MobEffectInstance haste = player.getEffect(MobEffects.DIG_SPEED);
        if (haste == null || haste.getAmplifier() < amplifier
                || haste.getDuration() < HASTE_EFFECT_DURATION / 2) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, HASTE_EFFECT_DURATION,
                    amplifier, false, false, true));
        }
    }

    /**
     * Which rung of the Haste ladder a katana is on, or -1 for neither. The deeper wins rather than
     * the two summing, the same rule the Vapor Slashes and the Extra Shots are read by.
     */
    private static int hasteAmplifier(ItemStack stack) {
        if (Engravings.has(stack, Engraving.HASTE_2)) {
            return HASTE_2_ENGRAVING_AMPLIFIER;
        }

        return Engravings.has(stack, Engraving.HASTE) ? HASTE_ENGRAVING_AMPLIFIER : -1;
    }

    /**
     * The Compression Arrow set's per-piece half: a fifth off every projectile that lands, per piece
     * worn, so a full set leaves a fifth of the arrow. Unlike the other two sets this one does not
     * wait for all four - each piece is worth wearing on its own, and the pieces are counted here
     * rather than being an attribute, since {@code Attributes.ARMOR} cannot be aimed at one damage
     * type and misbehaves badly at the values this mod reaches.
     * <p>
     * The filter is {@code #minecraft:is_projectile}, so another mod's projectiles are covered too.
     */
    @SubscribeEvent
    public static void onProjectileDamage(LivingIncomingDamageEvent event) {
        if (!event.getSource().is(DamageTypeTags.IS_PROJECTILE) || isTrueDamage(event.getSource())) {
            return;
        }

        int pieces = setPieces(event.getEntity(), ModArmorMaterials.COMPRESSION_ARROW_ARMOR_MATERIAL);
        float amount = event.getAmount();
        if (pieces > 0) {
            amount *= Math.max(0F, 1F - pieces * ARROW_SET_PROJECTILE_REDUCTION_PER_PIECE);
        }

        // The Projectile Protection engraving, which needs no set and stacks on top of one.
        event.setAmount(amount * engravingReduction(event.getEntity(), Engraving.PROJECTILE_PROTECTION));
    }

    /**
     * What is left of a hit after every {@code engraving} the target is wearing has taken its share.
     * Multiplied rather than summed, so the fourth engraving is worth a little less than the first
     * and the total never reaches one however many pieces are engraved - which is what keeps this
     * out of the trouble described in the armour note, where a reduction over a hundred percent
     * heals whatever it was meant to protect.
     */
    private static float engravingReduction(LivingEntity entity, Engraving engraving) {
        return (float) Math.pow(1.0F - PROTECTION_ENGRAVING_REDUCTION, Engravings.wornCount(entity, engraving));
    }

    /**
     * The other half, which does need all four: everything the wearer's arrows hit is lit up.
     * <p>
     * This is the impact rather than the damage, so a target that shrugs the hit off - invulnerable
     * from a hit a moment earlier, or armoured down to nothing - is still marked. The shooter is
     * skipped in case their own arrow comes back at them.
     */
    @SubscribeEvent
    public static void onArrowSetImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow)
                || !(event.getRayTraceResult() instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof LivingEntity target)
                || arrow.level().isClientSide()) {
            return;
        }

        // Two things paint a target: the full Compression Arrow set, and a Vision engraving on
        // whatever helmet the shooter happens to be wearing.
        if (arrow.getOwner() instanceof Player shooter && shooter != target
                && (wearsFullSet(shooter, ModArmorMaterials.COMPRESSION_ARROW_ARMOR_MATERIAL)
                        || Engravings.has(shooter.getItemBySlot(EquipmentSlot.HEAD), Engraving.VISION))) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, ARROW_SET_GLOWING_DURATION, 0, false, false, true));
        }
    }

    /** How many pieces of one material {@code entity} is wearing, across the four armour slots. */
    private static int setPieces(LivingEntity entity, Holder<ArmorMaterial> material) {
        int pieces = 0;
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (isPieceOf(entity.getItemBySlot(slot), material)) {
                pieces++;
            }
        }

        return pieces;
    }

    /** Whether {@code stack} is a piece of the Compression Jump set, by the material it is made of. */
    private static boolean isJumpPiece(ItemStack stack) {
        return isPieceOf(stack, ModArmorMaterials.COMPRESSION_JUMP_ARMOR_MATERIAL);
    }

    /**
     * Whether all four armour slots hold a piece of one material. Sets are identified by their
     * material rather than by item class, so a variant made of the same stuff still counts.
     */
    private static boolean wearsFullSet(LivingEntity entity, Holder<ArmorMaterial> material) {
        return isPieceOf(entity.getItemBySlot(EquipmentSlot.HEAD), material)
                && isPieceOf(entity.getItemBySlot(EquipmentSlot.CHEST), material)
                && isPieceOf(entity.getItemBySlot(EquipmentSlot.LEGS), material)
                && isPieceOf(entity.getItemBySlot(EquipmentSlot.FEET), material);
    }

    private static boolean isPieceOf(ItemStack stack, Holder<ArmorMaterial> material) {
        return stack.getItem() instanceof ArmorItem armor && armor.getMaterial().equals(material);
    }

    /**
     * The energy on a piece of gear, written the only way a 244 digit number can be read, and the
     * engravings cut into it. Both are data components, so this is the one place either is read for
     * display and no item class has to know about them.
     */
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        CompressionEnergy.get(event.getItemStack()).ifPresent(log10 -> event.getToolTip().add(
                Component.translatable("tooltip.unnecessarilycompressedcobblestone.compression_energy",
                        CompressionEnergy.format(log10), CompressionEnergy.digits(log10))
                        .withStyle(ChatFormatting.AQUA)));

        for (Engraving engraving : Engravings.get(event.getItemStack())) {
            event.getToolTip().add(engraving.displayName().copy().withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        // The augments on a laser, written the way its enchantments are: they are the same kind of
        // thing to a player, so they read the same way rather than being singled out by colour.
        for (Augment augment : Augments.get(event.getItemStack())) {
            event.getToolTip().add(augment.displayName().copy().withStyle(ChatFormatting.GRAY));
        }
    }

    /**
     * Whether {@code enchantments} carries a Compression level that only a crafted book can hold -
     * Compression IV and up, or any level of the three craft-only rungs. Those are the merges the
     * vanilla anvil would clamp, and so the ones taken over above.
     */

    /**
     * How many of a crop a Harvest Festival tool takes off one plant, as a multiple of what the
     * block would have dropped on its own.
     * <p>
     * It is flat rather than per level because the enchantment has exactly one level - see
     * {@code ModEnchantments.HARVEST_FESTIVAL}, which is registered at a maximum of one so that
     * neither the anvil nor a second book can climb. Sixty-four is a stack, which is the figure
     * that makes it legible: one wheat plant is one stack of wheat.
     */
    private static final int HARVEST_FESTIVAL_YIELD = 64;

    /**
     * How many extra item entities one harvested plant may produce beyond the first. A bound on an
     * entity count rather than on the reward, the way the Smash engraving's radius is bounded and
     * its damage is not: a crop that drops an unusual number of things should not be able to fill a
     * field with item entities.
     */
    private static final int HARVEST_FESTIVAL_MAX_STACKS = 64;

    /**
     * A swing with a scythe carrying the Scythe Wave engraving, aimed at something in reach.
     * <p>
     * Cancelled, and turned into a crescent instead. That is the whole of the engraving: the
     * ordinary melee attack never happens, and what lands on the target is the same attack carried
     * there by {@link ScytheWaveEntity} - so a mob standing next to the wielder is hit by a wave
     * that flew a fifth of a block, and one across a valley is hit by the same wave having flown
     * the whole way.
     * <p>
     * The event fires on both sides. Cancelling on the client is what stops it playing a hit it did
     * not land; only the server looses anything.
     */
    @SubscribeEvent
    public static void onScytheWaveAttack(AttackEntityEvent event) {
        Player player = event.getEntity();

        // A wave resolving its own hit calls Player#attack, which posts this event again with the
        // scythe still in hand. Without this guard the first swing of the game would fire crescents
        // until the stack ran out.
        if (ScytheWaveEntity.isResolving()
                || !Engravings.has(player.getMainHandItem(), Engraving.SCYTHE_WAVE)) {
            return;
        }

        event.setCanceled(true);
        if (player instanceof ServerPlayer serverPlayer) {
            swingScytheWave(serverPlayer);
        }
    }

    /**
     * The dev block's blow: 999 points of true damage, every swing, on top of whatever the punch
     * underneath it was worth.
     * <p>
     * It is here rather than in the item's {@code hurtEnemy} because that hook only fires when the
     * ordinary attack landed, and an ordinary attack with a block in hand is worth one point and is
     * swallowed by the twenty-tick invulnerability window every time but the first. This event
     * fires on the swing itself, so every hit is a hit. The damage type carries
     * {@code #minecraft:bypasses_cooldown} for the same reason - see {@code ModDamageTypes}.
     * <p>
     * Not cancelled: the punch is harmless and letting it through keeps the swing, the knockback and
     * the attack cooldown exactly as vanilla wrote them. The event fires on both sides, and only the
     * server deals damage.
     */
    @SubscribeEvent
    public static void onDevBlockAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()
                || !player.getMainHandItem().is(ModBlocks.DEV_COMPRESSED_COBBLESTONE.asItem())) {
            return;
        }

        event.getTarget().hurt(player.damageSources().source(ModDamageTypes.DEV_STRIKE, player),
                DEV_BLOCK_DAMAGE);
    }

    /**
     * Every mob in this mod that refuses a kind of damage, refusing it - by taking none of it rather
     * than by never having been hit.
     * <p>
     * The whole argument is on {@link SelectiveImmunity}, and it is short: {@code isInvulnerableTo}
     * makes {@code hurt} return before anything at all happens, so a boss written that way cannot be
     * reached by the engraving that is supposed to answer it. Clipping the damage here instead
     * leaves the blow itself intact - the flash, the knockback, the combat tracker, and every event
     * this mod hangs off a landed hit - and takes only the number.
     * <p>
     * {@code LivingDamageEvent.Pre} rather than the incoming event, so this speaks after armour,
     * Protection, Resistance and everything else rather than before them, and {@code LOWEST} so that
     * it speaks after every other handler on that event too. Both halves of that are the same point:
     * a boss's own rule about what may hurt it is the <b>last</b> word and not one opinion among
     * several. There is nothing left to argue with after zero, and anything that raised the figure
     * afterwards would be a way round the rule.
     * <p>
     * The dev sword is the case that makes the ordering matter, and it settles the right way now: it
     * raises its blow to {@code Float.MAX_VALUE} at {@code NORMAL}, and this then takes it back to
     * nothing. That is correct - it is an ordinary sword as far as a boss is concerned, and a creative
     * tool that could walk through the one rule a fight is built on would make the fight optional.
     * <p>
     * {@code lastHurt} is deliberately not touched. {@code hurt} records the blow at its full size
     * before this ever runs, so the twenty-tick window behaves exactly as it would on any other mob:
     * a second swing of the same weight still lands, rather than every other one being swallowed.
     */
    /**
     * Whether a blow is this mod's true damage, which every reduction it writes has to let past.
     * <p>
     * {@code #ucc:true_damage} is the mod's name for the damage types in all five vanilla
     * {@code bypasses_*} tags, so armour, shields, Protection, Resistance and enchantments already
     * let them through without being asked. The reductions in this file are not damage reduction the
     * game knows about - they are multipliers on the event, written that way because
     * {@code Attributes.ARMOR} cannot be aimed at one damage type and misbehaves at the figures this
     * mod reaches - so each of them has to state the exception itself or "gets past every form of
     * protection" would quietly stop being true the moment a set was worn.
     * <p>
     * The one thing true damage does not get past is {@link AbsoluteLimit}: see
     * {@link #onAbsoluteLimit}.
     */
    private static boolean isTrueDamage(DamageSource source) {
        return source.is(ModTags.DamageTypes.TRUE_DAMAGE);
    }

    /**
     * Every mob that states a ceiling on the size of a single blow, refusing one that reaches it.
     * <p>
     * This is the other half of the rule the mob states in its own {@code hurt}, and the half that
     * closes the hole. {@code hurt} measures the blow as it arrives, which is the right place and is
     * not the last word, because a blow can be <em>raised</em> afterwards: the dev sword sets the
     * figure to {@code Float.MAX_VALUE} at {@code NORMAL} on this very event. Stating the ceiling
     * again on the final number is what makes it true of the number that actually lands.
     * <p>
     * {@code LOWEST} for the same reason {@link #onSelectiveImmunity} runs there - a mob's own rule
     * about what may hurt it is the last word - and unconditional in the source, which is the whole
     * difference between this and that one. A {@code SelectiveImmunity} is a statement about kinds of
     * attack, so {@code #ucc:true_damage} is written to walk past it; a ceiling is a statement about
     * a number, and there is nothing about a damage type for it to have an opinion on. That is why
     * the dev block is priced a point under the Compressed Dragon's, and lands every point of it.
     * <p>
     * Like every rule of this shape it can only take a blow to nothing: the hit lands, flashes and
     * knocks the mob about and costs it not one point.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAbsoluteLimit(LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof AbsoluteLimit limited
                && limited.exceedsAbsoluteLimit(event.getNewDamage())) {
            event.setNewDamage(0.0F);
            limited.refuseAbsoluteLimit();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSelectiveImmunity(LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof SelectiveImmunity immune
                && immune.refusesDamage(event.getSource(), event.getNewDamage())) {
            event.setNewDamage(0.0F);
        }
    }

    /**
     * The dev sword's blow, raised to the largest number the game can carry it in.
     * <p>
     * {@code LivingDamageEvent.Pre} rather than the incoming event, because this fires after armour,
     * Protection, Resistance and every other reduction have already spoken: setting the figure here
     * is the difference between "an enormous blow that Resistance V still refuses" and "the maximum",
     * and the sword claims the maximum. {@code Float.MAX_VALUE} is that maximum - damage is a float
     * from end to end, so there is no larger blow to deal - and the number the tooltip prints is a
     * lie by three hundred orders of magnitude, which {@link DevSwordItem} says out loud.
     * <p>
     * {@code getWeaponItem} is the stack the blow was struck with, so this covers a swing and
     * nothing else, and a sword swapped out mid-fight stops mattering the moment it leaves the hand.
     * <p>
     * It does not get past a boss that has stated a rule of its own. {@link #onSelectiveImmunity}
     * runs at {@code LOWEST} and takes this back to nothing on anything that refuses the blow, and
     * the Compressed Dragon's second phase throws away a hit this size for being a hit this size -
     * so against the Compressed Ore Golem, the Snow Golem, the Spirit, the Composer and that dragon
     * the swing lands, flashes, knocks back and costs them nothing. That is the right answer rather
     * than a limitation: what makes this a dev tool is that it kills anything a number can kill, and
     * those five are not fights about numbers. The blow is still an ordinary
     * {@code minecraft:player_attack} in every other respect, which is exactly how each of those
     * rules is reading it.
     */
    @SubscribeEvent
    public static void onDevSwordDamage(LivingDamageEvent.Pre event) {
        if (event.getSource().getWeaponItem() != null
                && event.getSource().getWeaponItem().is(ModItems.DEV_SWORD.get())) {
            event.setNewDamage(Float.MAX_VALUE);
        }
    }

    /**
     * Looses one crescent, if the player is holding a scythe that should loose one.
     * <p>
     * Public because the other half of the engraving arrives as a packet rather than as an event:
     * a swing at empty air is never sent to the server as an attack, and empty air is exactly what
     * a player aiming at something forty blocks away is clicking - see
     * {@code SwingScythePayload}. Everything the wave needs is read here, off the sender, so that
     * packet is trusted for nothing but the fact that a swing happened.
     */
    public static void swingScytheWave(ServerPlayer player) {
        ItemStack scythe = player.getMainHandItem();
        if (!Engravings.has(scythe, Engraving.SCYTHE_WAVE)) {
            return;
        }

        // At most one crescent a tick. A swing already prices itself - the wave carries the charge
        // it was fired at, so a spammed one lands for a fifth of a hit the way a spammed melee does
        // - but the ticker being zero means a wave has already gone this tick, and a client sending
        // a hundred swing packets in one should not spawn a hundred entities.
        if (player.attackStrengthTicker <= 0) {
            return;
        }

        ServerLevel level = player.serverLevel();
        ScytheWaveEntity wave = new ScytheWaveEntity(level, player, scythe);
        wave.setAttackStrengthTicker(player.attackStrengthTicker);
        wave.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F,
                ScytheWaveEntity.SPEED, 0.0F);

        level.addFreshEntity(wave);

        // The swing is spent whether or not the crescent ever finds anything, exactly as a melee
        // swing at empty air is. This is also what keeps a scythe to one wave a second.
        player.resetAttackStrengthTicker();
        player.causeFoodExhaustion(0.1F);

        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    /**
     * A crop broken with a hoe or a scythe carrying Harvest Festival: sixty-four times the yield.
     * <p>
     * {@code BlockDropsEvent} rather than {@code BreakEvent} for the reason the boss spoils use it -
     * by the time it fires the drops are decided and are a mutable list, so Fortune, the block's own
     * loot table and every other mod's loot modifier have already had their say and this only
     * multiplies what came out. It multiplies <em>everything</em> that came out, seeds included,
     * which is the honest reading of "yield" and is what makes one enchanted hoe worth a field.
     * <p>
     * What counts as a crop is {@code #ucc:harvest}, so a datapack decides rather than this file;
     * and it has to be ripe, which is read off whatever {@code age} property the block carries
     * rather than off {@code CropBlock}, so nether wart, cocoa and another mod's crop all answer
     * the same question. Breaking a seedling is not a harvest, and multiplying one would make a
     * hoe a way of printing seeds out of a single planting rather than a way of farming.
     */
    @SubscribeEvent
    public static void onHarvestFestival(BlockDropsEvent event) {
        ItemStack tool = event.getTool();
        if (tool.isEmpty() || !event.getState().is(ModTags.Blocks.HARVEST) || !isRipe(event.getState())) {
            return;
        }

        int festival = EnchantmentHelper.getItemEnchantmentLevel(
                event.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(ModEnchantments.HARVEST_FESTIVAL), tool);
        if (festival <= 0) {
            return;
        }

        List<ItemEntity> extra = new ArrayList<>();
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            long total = (long) stack.getCount() * HARVEST_FESTIVAL_YIELD;
            int max = Math.max(1, stack.getMaxStackSize());

            // The first stack goes back into the drop that was already there, so anything that read
            // the list before this - another mod's loot modifier - keeps the entity it was handed.
            int first = (int) Math.min(total, max);
            drop.setItem(stack.copyWithCount(first));

            long remaining = total - first;
            while (remaining > 0 && extra.size() < HARVEST_FESTIVAL_MAX_STACKS) {
                int count = (int) Math.min(remaining, max);
                extra.add(new ItemEntity(event.getLevel(), drop.getX(), drop.getY(), drop.getZ(),
                        stack.copyWithCount(count)));
                remaining -= count;
            }
        }

        event.getDrops().addAll(extra);
    }

    /**
     * Whether {@code state} is a grown crop rather than a seedling.
     * <p>
     * Read off the block's own {@code age} property rather than off {@code CropBlock}: half the
     * things in {@code #ucc:harvest} are not that class - nether wart, cocoa and the berry bush all
     * grow through an age of their own - and a modded crop is under no obligation to be one either.
     * Anything in the tag with no age at all is taken as always ready, which is the right answer for
     * a plant that is harvested whenever it is broken.
     */
    private static boolean isRipe(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty age && "age".equals(age.getName())) {
                int ripe = age.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(0);
                return state.getValue(age) >= ripe;
            }
        }

        return true;
    }

    /**
     * The Compressed Spear's minimum range: a jab refuses anything inside
     * {@code CompressedSpearItem.MIN_RANGE} blocks.
     * <p>
     * This is the price of the spear's reach and is vanilla's own rule for the weapon. It belongs on
     * {@code AttackEntityEvent} because that event is precisely "a left click landed on this
     * creature" - the packet the client sends when it clicks something, before any damage is worked
     * out - so cancelling it is a swing that finds nothing rather than a swing that is absorbed.
     * <p>
     * Measured from the attacker's eye to the target's hitbox rather than between the two
     * positions, so a tall creature standing close is refused by where its body is and not by where
     * its feet are - which is the same distance the reach attribute itself is measured over.
     * <p>
     * The charge attack has no minimum and is untouched by this: see
     * {@link CompressedSpearItem#onUseTick} for why a lunge that refused what it had run into could
     * never land.
     */
    @SubscribeEvent
    public static void onSpearJab(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (!(player.getMainHandItem().getItem() instanceof CompressedSpearItem)) {
            return;
        }

        Vec3 eye = player.getEyePosition();
        double distance = Math.sqrt(event.getTarget().getBoundingBox().distanceToSqr(eye));
        if (distance < CompressedSpearItem.MIN_RANGE) {
            event.setCanceled(true);
            player.level().playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK,
                    SoundSource.PLAYERS, 0.4F, 1.8F);
        }
    }

    /**
     * A spear cannot land a critical hit, which is vanilla's second rule for the weapon.
     * <p>
     * {@code CriticalHitEvent} is fired from {@code Player#attack} with the decision vanilla has
     * already made, so refusing it here is refusing it everywhere a crit could have come from -
     * the jump attack included. Nothing is done to the sprint knockback that rule usually travels
     * with: that is applied inside {@code Player#attack} itself with no hook of its own, and on a
     * weapon whose charge is paid for in running speed it is the one half worth keeping anyway.
     */
    @SubscribeEvent
    public static void onSpearCriticalHit(CriticalHitEvent event) {
        if (event.getEntity().getMainHandItem().getItem() instanceof CompressedSpearItem) {
            event.setCriticalHit(false);
        }
    }

    /**
     * The Compressed Totem of Undying: every death, refused.
     * <p>
     * {@code LivingDeathEvent} is the single gate every death in the game goes through, so
     * cancelling it covers what vanilla's own totem cannot - {@code /kill}, the void, and everything
     * else in {@code #minecraft:bypasses_invulnerability}, all of which
     * {@code checkTotemDeathProtection} refuses outright by its first line. See
     * {@link CompressedTotemItem}.
     * <p>
     * The save has to be more than a cancel. Vanilla's totem leaves its holder on one point of
     * health because the blow that would have killed them has already been applied; a death
     * cancelled here leaves them on zero, which the next tick of anything at all would finish, so
     * the totem heals fully. Every effect comes off with it, the good with the bad, which is the
     * price of that.
     * <p>
     * At the highest priority, so a totem is spent before anything else in this mod - the boss
     * spoils among them - has decided a creature died. It is not restricted to players: a totem in
     * a mob's hand is a totem, exactly as vanilla's is.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCompressedTotemDeath(LivingDeathEvent event) {
        LivingEntity dying = event.getEntity();
        if (dying.level().isClientSide()) {
            return;
        }

        InteractionHand hand = CompressedTotemItem.heldIn(dying);
        if (hand == null) {
            return;
        }

        event.setCanceled(true);
        CompressedTotemItem.save(dying, hand);
    }

    /**
     * Nothing the Compressed Dragon calls up will turn on it.
     * <p>
     * It is an event rather than three overrides because the flock, the brood and the swarm the
     * brood hatches are three classes this file already shares with two other bosses and a summoning
     * staff - and what makes one of them the dragon's is a mark put on it when it was called up, not
     * what it is. {@code LivingChangeTargetEvent} is the single door every target acquisition goes
     * through, so one cancelled event covers the hunting goals, {@code HurtByTargetGoal} and
     * anything a datapack or another mod adds.
     * <p>
     * The dragon's own {@code isAlliedTo} answers the other direction - it never picks one of them
     * as a target either - and the two together are what "the summons do not fight the summoner"
     * means when the summons were never written to know about it.
     */
    @SubscribeEvent
    public static void onDragonSummonTarget(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof AbstractCompressedDragonEntity
                && CompressedDragonTier2Entity.isSummon(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private static boolean hasCraftedLevel(ItemEnchantments enchantments) {
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            if (ModEnchantments.isCraftedLevel(entry.getKey(), entry.getIntValue())) {
                return true;
            }
        }

        return false;
    }
}
