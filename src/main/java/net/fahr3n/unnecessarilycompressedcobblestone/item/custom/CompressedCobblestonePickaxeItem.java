package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.util.CompressionEnergy;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The only tool that gets into the deepest compressed cobblestone. It is a {@link PickaxeItem} and
 * nothing more exotic than that, so it is in {@code #minecraft:pickaxes} and everything that keys
 * off a pickaxe - Efficiency, Fortune, Silk Touch, Unbreaking and Mending at a table or an anvil,
 * and any mod's upgrade system that looks at vanilla tools - treats it as one.
 * <p>
 * What Compression Energy buys it is speed: one point of mining speed per digit, which is the same
 * one-per-digit the inscriber gives armour and weapons. On ordinary stone that is a nicety; on the
 * hardened levels it is the difference between eight seconds a block and one, because
 * {@code HardenedCompressedBlock} works its progress out from exactly this number.
 */
public class CompressedCobblestonePickaxeItem extends PickaxeItem {
    public CompressedCobblestonePickaxeItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    /** The tier's own speed plus one for every digit of Compression Energy inscribed into it. */
    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float speed = super.getDestroySpeed(stack, state);

        // Only where the tool is the right one for the job: an inscribed pickaxe is still a pickaxe,
        // and being deeply compressed does not make it any better at chopping wood.
        return speed > 1.0F ? speed + CompressionEnergy.bonus(stack) : speed;
    }

    /** Swallows every point of durability damage before vanilla can apply it. */
    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
        return 0;
    }

    /** The durability is never spent, so say so the way an unbreakable item does. */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("item.unbreakable").withStyle(ChatFormatting.BLUE));
    }
}
