package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorMaterial;

/**
 * A piece of the Compression Arrow set. Like every other set here it is
 * {@link CompressedCobblestoneArmorItem} underneath - unbreakable, otherwise ordinary - and exists
 * as its own type only so the set is easy to name. Both of its bonuses are in {@code ModEvents} and
 * look at the armor material rather than at this class, so it is the material that really marks a
 * piece as one of these.
 */
public class CompressionArrowArmorItem extends CompressedCobblestoneArmorItem {
    public CompressionArrowArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }
}
