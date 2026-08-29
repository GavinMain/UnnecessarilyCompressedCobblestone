package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorMaterial;

/**
 * A piece of the Compression Jump set. It behaves exactly like
 * {@link CompressedCobblestoneArmorItem} - unbreakable, otherwise ordinary - and exists as its own
 * type only so the set is easy to name. What makes the set special is the bonus in {@code ModEvents},
 * which looks at the armor material rather than at this class, so it is the material that is the
 * real marker of a Jump piece.
 */
public class CompressionJumpArmorItem extends CompressedCobblestoneArmorItem {
    public CompressionJumpArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }
}
