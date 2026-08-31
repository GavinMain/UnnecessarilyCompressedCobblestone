package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.ArrowVeilEntity;
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
 * Throws up a dome that takes apart every projectile that was not fired by whoever raised it. Thirty
 * seconds of cover, and a minute before it can be raised again - the cooldown starts the moment it
 * goes up, so half of it runs while the dome is still standing and the other half is spent exposed.
 */
public class ArrowVeilItem extends Item {
    /** A minute, counted from the moment the veil is raised rather than from when it falls. */
    public static final int COOLDOWN = 1200;

    public ArrowVeilItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            level.addFreshEntity(new ArrowVeilEntity(level, player.position(), player.getUUID()));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE,
                    SoundSource.PLAYERS, 1.0F, 1.4F);
        }

        player.getCooldowns().addCooldown(this, COOLDOWN);
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
