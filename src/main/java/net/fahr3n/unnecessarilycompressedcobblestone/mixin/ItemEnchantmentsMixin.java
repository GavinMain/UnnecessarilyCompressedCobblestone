package net.fahr3n.unnecessarilycompressedcobblestone.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.fahr3n.unnecessarilycompressedcobblestone.util.FullEnchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Raising the ceiling on an enchantment level, which is the one number in this mod that no access
 * transformer could reach.
 * <p>
 * The mod's other ceiling raise, {@code util.ModAttributeCeilings}, is next door in shape and worlds
 * away in cost, and the difference is worth stating because it is why this file exists at all.
 * Vanilla's 1024 on max health is a <em>number in a field</em>, so widening the field was enough and
 * nothing else in the game had to be touched. 255 on an enchantment level is not a number anything
 * holds: it is a bounds check in {@code ItemEnchantments}'s constructor, which throws outside 0 to
 * 255, and the {@code Codec.intRange(0, 255)} its levels are read and written through. A transformer
 * can change what is visible; it cannot change what a method does. Hence a mixin, and hence this
 * mod having a mixin configuration for exactly two classes and nothing else.
 * <p>
 * Both halves have to go together, and each is load-bearing on its own path. The constructor is
 * where every stack in memory is built - the mutable builder {@code FullEnchantment} uses ends
 * there, and so does the network codec on the receiving client, which is why patching only the codec
 * would have left a level over 255 kicking whoever looked at it. The codec is where saving and
 * loading go, so patching only the constructor would have made a level over 255 something that
 * existed until the world was reloaded and then quietly vanished.
 * <p>
 * {@link ModifyConstant} rather than an overwrite: what is wanted is one number in each of those two
 * methods and nothing else about them, so the check still throws, the codec still refuses a level
 * out of range, and every other mod's expectation of how the component behaves is intact. Only the
 * figure moves.
 */
@Mixin(ItemEnchantments.class)
public class ItemEnchantmentsMixin {
    /**
     * The bounds check in the constructor, which is the gate every stack in memory goes through -
     * including one arriving from the network, where a throw would take the client down.
     */
    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 255))
    private int ucc$raiseConstructorCeiling(int ceiling) {
        return FullEnchantment.CEILING;
    }

    /**
     * The level codec's range, built in the class initialiser, which is the gate saving and loading
     * go through. A level the constructor now allows would otherwise be dropped by the disk.
     */
    @ModifyConstant(method = "<clinit>", constant = @Constant(intValue = 255))
    private static int ucc$raiseCodecCeiling(int ceiling) {
        return FullEnchantment.CEILING;
    }
}
