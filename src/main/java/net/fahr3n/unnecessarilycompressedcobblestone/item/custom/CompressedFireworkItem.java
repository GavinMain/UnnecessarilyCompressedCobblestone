package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import java.util.List;

import it.unimi.dsi.fastutil.ints.IntList;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A firework rocket that is never used up.
 * <p>
 * Both of vanilla's uses are overridden rather than extended, because the only thing that had to
 * change is one line in each: {@code FireworkRocketItem} shrinks the stack after spawning the
 * rocket, and there is no hook between spawning and shrinking to stand in. Everything else is left
 * to vanilla - the rocket is a plain {@link FireworkRocketEntity} built from this stack, so the
 * flight duration, the burst, the elytra boost and the damage all come off the item's own
 * {@code minecraft:fireworks} component exactly as they would on a crafted rocket.
 * <p>
 * A short cooldown stands in for the stack it does not spend. Without one the item is a held button
 * that fires a rocket every tick, and twenty rockets a second is both a wall of sound and a genuine
 * entity load; at {@link #COOLDOWN} it is still faster than reaching for the next rocket in a
 * stack, which is the whole point of it.
 */
public class CompressedFireworkItem extends FireworkRocketItem {
    /** How long between launches, in ticks. Short enough to keep an elytra climbing on it. */
    private static final int COOLDOWN = 10;

    /**
     * What it is loaded with, since nothing ever crafts a charge into it: three gunpowder's worth of
     * flight, which is a full elytra boost, and one large burst in the compressed set's own blues.
     */
    private static final Fireworks CHARGE = new Fireworks(3, List.of(new FireworkExplosion(
            FireworkExplosion.Shape.LARGE_BALL,
            IntList.of(0x464CB7, 0xBED8E4),
            IntList.of(0x2D316E),
            true, true)));

    public CompressedFireworkItem(Properties properties) {
        super(properties.component(DataComponents.FIREWORKS, CHARGE));
    }

    /** Vanilla's placement launch with the shrink taken out. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();

        if (!level.isClientSide()) {
            Vec3 click = context.getClickLocation();
            Direction face = context.getClickedFace();
            level.addFreshEntity(new FireworkRocketEntity(level, player,
                    click.x + face.getStepX() * ROCKET_PLACEMENT_OFFSET,
                    click.y + face.getStepY() * ROCKET_PLACEMENT_OFFSET,
                    click.z + face.getStepZ() * ROCKET_PLACEMENT_OFFSET,
                    context.getItemInHand()));
        }

        if (player != null) {
            player.getCooldowns().addCooldown(this, COOLDOWN);
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** Vanilla's elytra boost with the shrink taken out. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isFallFlying()) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide()) {
            level.addFreshEntity(new FireworkRocketEntity(level, stack, player));
            player.awardStat(Stats.ITEM_USED.get(this));
        }

        player.getCooldowns().addCooldown(this, COOLDOWN);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
                                TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.unnecessarilycompressedcobblestone.compressed_firework")
                .withStyle(ChatFormatting.BLUE));
    }
}
