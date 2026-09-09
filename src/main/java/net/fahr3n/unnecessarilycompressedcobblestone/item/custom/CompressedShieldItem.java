package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;

/**
 * The Compressed Shield. A shield in every respect the game reads - it raises the same way, blocks
 * the same arc, is disabled by an axe the same way - and different in two: it never breaks, and two
 * things a vanilla shield cannot stop bounce off it.
 * <p>
 * Both of those extra blocks are in {@code ModEvents} rather than here, because blocking is decided
 * inside {@code LivingEntity#hurt} on the entity being hit and there is nothing on the item to
 * override. What is worth recording is <em>why</em> each one needs the help, because the two fail
 * for opposite reasons:
 * <ul>
 * <li><b>Lightning</b> is not in {@code #minecraft:bypasses_shield} at all - vanilla is perfectly
 * willing to let a shield block it. What stops it is that
 * {@code DamageSources.lightningBolt()} is one bare source shared by every bolt in the world, with
 * no entity and no position on it, and {@code isDamageSourceBlocked} gives up the moment
 * {@code getSourcePosition()} comes back null because it cannot work out which way the hit came
 * from.</li>
 * <li><b>Potion damage</b> fails the other way round: {@code magic} and {@code indirect_magic} are
 * both in {@code #minecraft:bypasses_armor}, which {@code #minecraft:bypasses_shield} includes
 * wholesale, so those are refused before the angle is ever looked at.</li>
 * </ul>
 * Neither can be fixed with a tag, since both damage types belong to the whole game rather than to
 * this shield - so the shield states its own exception, and only while it is actually raised.
 * <p>
 * Like every other tool in this mod it keeps a real durability and never spends it; see
 * {@link CompressedCobblestoneSwordItem} for why the {@code minecraft:unbreakable} component is the
 * wrong way to say that.
 */
public class CompressedShieldItem extends ShieldItem {
    public CompressedShieldItem(Properties properties) {
        super(properties);
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
        tooltipComponents.add(Component.translatable(
                "tooltip.unnecessarilycompressedcobblestone.compressed_shield").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("item.unbreakable").withStyle(ChatFormatting.BLUE));
    }
}
