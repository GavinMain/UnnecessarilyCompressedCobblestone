package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A fishing rod that pulls ten catches out of the water where an ordinary one pulls a single fish,
 * and that never wears out.
 * <p>
 * Everything about how it casts, hooks and reels in is vanilla's - it is a plain
 * {@link FishingRodItem} subclass, so Lure and Luck of the Sea and every mod that reads
 * {@code ItemAbilities.FISHING_ROD_CAST} work on it untouched. The two things that are its own are
 * both read off it from elsewhere rather than done here: {@code ModEvents} asks it how many times
 * to roll the catch and rolls the fishing loot table that many times, and the same handler turns its
 * Compression Energy into damage on the hook. Neither is a thing vanilla gives an item any say over,
 * which is why they are events and not overrides.
 * <p>
 * The durability is never spent, the same trick the sword and the armour use: the
 * {@code minecraft:unbreakable} component would make {@link ItemStack#isDamageableItem()} false, and
 * an item that is not damageable cannot be enchanted at a table - so the rod keeps a real bar and
 * simply refuses to spend it.
 */
public class CompressedFishingRodItem extends FishingRodItem {
    /**
     * How many times this rod rolls the catch. The Fishing enchantment multiplies this rather than
     * replacing it, so an enchanted compressed rod is ten times {@code level + 1}.
     */
    public static final int CATCHES = 10;

    /**
     * How often a catch is a block of the stone the rod is cut from, instead of whatever the loot
     * table rolled. One in twenty, on every one of the ten catches independently, so a cast lands
     * one about two casts in five.
     */
    public static final float COBBLE_CHANCE = 0.05F;

    /** The compression level a lucky catch brings up, which is the tier the rod is built at. */
    public static final int COBBLE_TIER = 142;

    public CompressedFishingRodItem(Properties properties) {
        super(properties);
    }

    /** Swallows every point of durability damage before vanilla can apply it. */
    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
        return 0;
    }

    /**
     * How many times {@code stack} rolls the catch before the Fishing enchantment is taken into
     * account: ten for this rod, one for every other rod in the game. It is written as a static test
     * on the stack rather than as an instance method so that {@code ModEvents} can ask the same
     * question of a vanilla rod, which is most of what it will ever be asked about.
     */
    public static int baseCatches(ItemStack stack) {
        return stack.getItem() instanceof CompressedFishingRodItem ? CATCHES : 1;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
                                TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable(
                "tooltip.unnecessarilycompressedcobblestone.compressed_fishing_rod", CATCHES)
                .withStyle(ChatFormatting.AQUA));
        tooltipComponents.add(Component.translatable("item.unbreakable").withStyle(ChatFormatting.BLUE));
    }
}
