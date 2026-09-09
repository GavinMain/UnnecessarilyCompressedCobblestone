package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import java.util.List;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.CompressedWolfEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * The Compressed Bone. Fed to a wolf, it tames it on the spot and hands back a
 * {@link CompressedWolfEntity} in its place.
 * <p>
 * Both halves matter. Vanilla's bone is a one-in-three chance per bone, which on a stack is a lot of
 * clicking and a lot of nothing; this is one bone, once, always. And what comes back is the same
 * wolf rather than a different mob wearing its skin - the variant, the collar colour, the name, the
 * age and the health it was at all come across, so the animal a player tamed is the animal they
 * keep.
 * <p>
 * This is {@code interactLivingEntity} rather than an event handler because the behaviour belongs to
 * the item and to nothing else. Vanilla runs {@code entity.interact} first and only falls through to
 * here when that did not consume the click, which is exactly right: a wolf offered a Compressed Bone
 * has no interaction of its own to run, so this is reached, and a wolf being fed or dyed or armoured
 * is handled before this is ever asked.
 */
public class CompressedBoneItem extends Item {
    public CompressedBoneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                  InteractionHand hand) {
        // Already compressed, or not a wolf at all: nothing to do, and the stack is not spent.
        if (!(target instanceof Wolf wolf) || wolf instanceof CompressedWolfEntity) {
            return InteractionResult.PASS;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return InteractionResult.sidedSuccess(true);
        }

        CompressedWolfEntity compressed = ModEntities.COMPRESSED_WOLF.get().create(level);
        if (compressed == null) {
            return InteractionResult.PASS;
        }

        // Everything about the animal that a player would notice is missing.
        compressed.moveTo(wolf.getX(), wolf.getY(), wolf.getZ(), wolf.getYRot(), wolf.getXRot());
        compressed.setVariant(wolf.getVariant());
        compressed.setCollarColor(wolf.getCollarColor());
        compressed.setAge(wolf.getAge());
        compressed.setBodyArmorItem(wolf.getBodyArmorItem().copy());
        if (wolf.hasCustomName()) {
            compressed.setCustomName(wolf.getCustomName());
            compressed.setCustomNameVisible(wolf.isCustomNameVisible());
        }

        // tame() before the level sees it, so applyTamingSideEffects has set the thousand health
        // and healed it to full by the time anything can look at the mob.
        compressed.tame(player);
        compressed.setOrderedToSit(false);

        wolf.discard();
        level.addFreshEntity(compressed);

        level.sendParticles(ParticleTypes.HEART, compressed.getX(), compressed.getY() + 1.0,
                compressed.getZ(), 12, 0.4, 0.4, 0.4, 0.0);
        level.playSound(null, compressed.blockPosition(), SoundEvents.WOLF_HOWL,
                SoundSource.NEUTRAL, 1.0F, 1.0F);

        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
                                TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable(
                "tooltip.unnecessarilycompressedcobblestone.compressed_bone").withStyle(ChatFormatting.GRAY));
    }
}
