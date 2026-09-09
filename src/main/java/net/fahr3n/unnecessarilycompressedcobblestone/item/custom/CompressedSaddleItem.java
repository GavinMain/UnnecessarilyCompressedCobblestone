package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import java.util.List;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedHorseEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

/**
 * The Compressed Saddle. Thrown over a tamed horse, it hands back a {@link CompressedHorseEntity} in
 * its place - and saddled, since a saddle that turned a horse into something nobody could ride would
 * be a strange thing to make.
 * <p>
 * The animal comes across rather than being replaced: the coat, the markings, the name, the age, the
 * owner, the temper and everything in its inventory are all carried over, so the horse a player
 * tamed is the horse they keep. That is the same rule the Compressed Bone follows and for the same
 * reason - a mob that arrives looking like somebody else's reads as a different animal, however
 * good its numbers are.
 * <p>
 * This is {@code interactLivingEntity} rather than an event handler because the behaviour belongs to
 * the item. {@code AbstractHorse.mobInteract} calls into it directly before it decides to be ridden,
 * so a saddle offered to a horse is answered here; a horse that is not tamed never reaches it, since
 * {@code Horse.mobInteract} rears at an untamed horse's handler first, which is exactly the
 * restriction wanted.
 */
public class CompressedSaddleItem extends Item {
    public CompressedSaddleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                  InteractionHand hand) {
        // Already compressed, not a horse, not tamed or still a foal: nothing to do, and the stack
        // is not spent. A foal is left alone because a compressed foal would grow into a horse whose
        // rolled attributes had already been thrown away, and because vanilla will not saddle one.
        if (!(target instanceof Horse horse) || horse instanceof CompressedHorseEntity
                || !horse.isTamed() || horse.isBaby()) {
            return InteractionResult.PASS;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return InteractionResult.sidedSuccess(true);
        }

        CompressedHorseEntity compressed = ModEntities.COMPRESSED_HORSE.get().create(level);
        if (compressed == null) {
            return InteractionResult.PASS;
        }

        compressed.moveTo(horse.getX(), horse.getY(), horse.getZ(), horse.getYRot(), horse.getXRot());

        // The coat. setVariant on its own keeps this horse's markings bits rather than the old
        // horse's, so both halves go across together through the access-transformed setter.
        compressed.setVariantAndMarkings(horse.getVariant(), horse.getMarkings());
        compressed.setAge(horse.getAge());
        compressed.setBodyArmorItem(horse.getBodyArmorItem().copy());
        if (horse.hasCustomName()) {
            compressed.setCustomName(horse.getCustomName());
            compressed.setCustomNameVisible(horse.isCustomNameVisible());
        }

        // Who it belongs to, and how well it already knew them.
        compressed.setTamed(true);
        compressed.setOwnerUUID(horse.getOwnerUUID() != null ? horse.getOwnerUUID() : player.getUUID());
        compressed.setTemper(horse.getTemper());

        // Saddled either way: with whatever it was already wearing if it was saddled, and with a
        // plain saddle if it was not. The Compressed Saddle itself is spent on the change rather
        // than worn, so it is a vanilla saddle that ends up in the slot.
        ItemStack existing = horse.getInventory().getItem(0);
        compressed.equipSaddle(existing.isEmpty() ? new ItemStack(Items.SADDLE) : existing.copy(), null);

        // Full health rather than the old horse's fraction of it: a horse at half of fifteen hearts
        // that arrived at half of five hundred would be a change nobody asked for.
        compressed.setHealth(compressed.getMaxHealth());

        horse.discard();
        level.addFreshEntity(compressed);

        level.sendParticles(ParticleTypes.HEART, compressed.getX(), compressed.getY() + 1.5,
                compressed.getZ(), 12, 0.6, 0.5, 0.6, 0.0);
        level.playSound(null, compressed.blockPosition(), SoundEvents.HORSE_SADDLE,
                SoundSource.NEUTRAL, 1.0F, 0.8F);

        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
                                TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable(
                "tooltip.unnecessarilycompressedcobblestone.compressed_saddle").withStyle(ChatFormatting.GRAY));
    }
}
