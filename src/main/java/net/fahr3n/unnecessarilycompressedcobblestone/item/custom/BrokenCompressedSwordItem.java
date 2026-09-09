package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;


import net.fahr3n.unnecessarilycompressedcobblestone.util.FullEnchantment;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

/**
 * The sword that carries every enchantment in the game at once, at 255 apiece - including the ones
 * that mean nothing on a sword, and the curses.
 * <p>
 * The enchantments are stamped on at runtime rather than baked into the recipe, and that is the
 * whole point of the class. Enchantments are a datapack registry, so what "every enchantment" means
 * is not known until a world is loaded: a recipe written at datagen time could only ever list the
 * ones this mod and vanilla ship, and would silently miss whatever any other mod adds. Reading the
 * registry off the level instead means the sword picks up another mod's enchantments the first time
 * it is held, and picks up new ones again after a datapack reload adds them.
 * <p>
 * The check is a size comparison rather than a rebuild, so the usual case costs one integer compare;
 * it runs every {@link #TOP_UP_INTERVAL} ticks rather than every tick because counting the registry
 * is the expensive half, and a few seconds' delay on a sword that has just been reloaded into is
 * nothing. Levels are set rather than upgraded, so an anvil cannot be used to push one past 255 and
 * nothing here can produce the out-of-range level {@link ItemEnchantments} throws on.
 * <p>
 * Every one of them is listed in the tooltip, one line each, the way any other enchanted item's are.
 * A hundred and thirty enchantment lines is not a tooltip, it is a screen - which on this sword is
 * the point, not a problem to be tidied away behind a summary.
 */
public class BrokenCompressedSwordItem extends CompressedCobblestoneSwordItem {
    /** What every enchantment is set to; see {@link FullEnchantment#LEVEL} for why 255. */
    public static final int LEVEL = FullEnchantment.LEVEL;

    /** How often the sword checks whether the registry has grown under it, in ticks. */
    private static final int TOP_UP_INTERVAL = 100;

    public BrokenCompressedSwordItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    /**
     * Fills in anything the stack is missing. Called from the tick below and when the sword is
     * built for the creative tab, so a copy is fully enchanted before anyone ever holds it.
     */
    public static void enchantFully(ItemStack stack, HolderLookup.Provider registries) {
        FullEnchantment.apply(stack, registries, true);
    }

    /**
     * Puts back anything missing: a sword handed out by a command, taken from the creative tab
     * before this ran, or held while a datapack reload added an enchantment that was not there when
     * it was made.
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide() || entity.tickCount % TOP_UP_INTERVAL != 0) {
            return;
        }

        if (FullEnchantment.needsTopUp(stack, level.registryAccess(), true)) {
            enchantFully(stack, level.registryAccess());
        }
    }
}
