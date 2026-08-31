package net.fahr3n.unnecessarilycompressedcobblestone.event;

import java.util.List;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.block.custom.CarvedCobblestoneBlock;
import net.fahr3n.unnecessarilycompressedcobblestone.enchantment.ModEnchantments;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.ArrowVeilEntity;
import net.fahr3n.unnecessarilycompressedcobblestone.item.ModArmorMaterials;
import net.fahr3n.unnecessarilycompressedcobblestone.potion.ModPotions;
import net.fahr3n.unnecessarilycompressedcobblestone.util.DeferredFill;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Engraving;
import net.fahr3n.unnecessarilycompressedcobblestone.util.Engravings;
import net.fahr3n.unnecessarilycompressedcobblestone.util.DeferredStrikes;
import net.fahr3n.unnecessarilycompressedcobblestone.util.LightningSong;
import net.fahr3n.unnecessarilycompressedcobblestone.util.CompressionEnergy;
import net.fahr3n.unnecessarilycompressedcobblestone.util.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = UnnecessarilyCompressedCobblestone.MOD_ID)
public class ModEvents {
    private static final ResourceLocation CE_ATTACK_DAMAGE_ID =
            ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, "compression_energy_attack_damage");
    private static final ResourceLocation CE_MAX_HEALTH_ID =
            ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, "compression_energy_max_health");
    private static final ResourceLocation JUMP_SET_STEP_HEIGHT_ID =
            ResourceLocation.fromNamespaceAndPath(UnnecessarilyCompressedCobblestone.MOD_ID, "compression_jump_step_height");

    /** Speed II; the amplifier is one less than the numeral. */
    private static final int LIGHTNING_SET_SPEED_AMPLIFIER = 1;

    /** What a full Compression Lightning set leaves of a lightning strike: five percent of it. */
    private static final float LIGHTNING_SET_DAMAGE_TAKEN = 0.05F;

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

    /** What a Lifesteal engraving gives back to whoever swung it: a fifth of the damage dealt. */
    private static final float LIFESTEAL_FRACTION = 0.20F;

    /**
     * What a Quick Draw engraving takes off a draw. The draw is not shortened directly - nothing in
     * vanilla exposes a bow's draw time - so the charge the bow has built up is scaled instead,
     * which brings it to full power in three quarters of the ticks. The pull animation still runs at
     * its own speed, so a quick-drawn bow reaches full power a little before it looks like it has.
     */
    private static final float QUICK_DRAW_FRACTION = 0.25F;

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
        if (!level.getBlockState(pos).is(ModBlocks.byLevel(CarvedCobblestoneBlock.CARVED_FROM_LEVEL).get())) {
            return;
        }

        Player player = event.getEntity();
        if (!level.isClientSide) {
            Direction face = event.getFace();
            Direction facing = face == null || face.getAxis() == Direction.Axis.Y ? player.getDirection().getOpposite() : face;

            level.playSound(null, pos, SoundEvents.PUMPKIN_CARVE, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.setBlock(pos, ModBlocks.CARVED_COBBLESTONE_TIER_1.get().defaultBlockState()
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
        if (!event.getSource().is(DamageTypeTags.IS_LIGHTNING)) {
            return;
        }

        float amount = event.getAmount();
        if (wearsFullSet(event.getEntity(), ModArmorMaterials.COMPRESSION_LIGHTNING_ARMOR_MATERIAL)) {
            amount *= LIGHTNING_SET_DAMAGE_TAKEN;
        }

        // The Lightning Protection engraving, which needs no set and stacks on top of one.
        event.setAmount(amount * engravingReduction(event.getEntity(), Engraving.LIGHTNING_PROTECTION));
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
        if (Engravings.has(event.getBow(), Engraving.QUICK_DRAW)) {
            event.setCharge(Math.round(event.getCharge() / (1.0F - QUICK_DRAW_FRACTION)));
        }
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
        if (!event.getSource().is(DamageTypeTags.IS_PROJECTILE)) {
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
    }

    /**
     * Whether {@code enchantments} carries a Compression level that only a crafted book can hold -
     * Compression IV and up, or any level of the three craft-only rungs. Those are the merges the
     * vanilla anvil would clamp, and so the ones taken over above.
     */
    private static boolean hasCraftedLevel(ItemEnchantments enchantments) {
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            if (ModEnchantments.isCraftedLevel(entry.getKey(), entry.getIntValue())) {
                return true;
            }
        }

        return false;
    }
}
