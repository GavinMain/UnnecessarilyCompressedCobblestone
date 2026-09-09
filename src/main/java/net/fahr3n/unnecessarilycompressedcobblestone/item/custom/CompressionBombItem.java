package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressionBombEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The Compression Bomb: a grenade thrown by hand, spent when it is thrown.
 * <p>
 * Everything it does is in {@link CompressionBombEntity} - this class is only the throw, which is
 * vanilla's egg and snowball path with the item's own projectile in place of theirs. It is a
 * consumable rather than a reusable thrower, so the stack shrinks and there is no cooldown to give
 * in place of the one it spends: the eight it stacks to is the whole of how many are in hand.
 * <p>
 * Deliberately not a {@code ProjectileWeaponItem} and deliberately not enchantable. It is not gear
 * - there is nothing on it to sharpen, nothing to make unbreakable and nothing for the inscriber to
 * pour energy into - so it stays out of {@code #inscribable} and out of every enchantable tag, and
 * what it is worth is entirely what its target was already carrying.
 */
public class CompressionBombItem extends Item {
    public CompressionBombItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 0.5F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!level.isClientSide()) {
            CompressionBombEntity bomb = new CompressionBombEntity(level, player);
            bomb.setItem(stack);
            bomb.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            level.addFreshEntity(bomb);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        stack.consume(1, player);

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
