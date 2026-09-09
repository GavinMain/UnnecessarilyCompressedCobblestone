package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorMaterial;

/**
 * A piece of the Ultimate Compressed set. It behaves exactly like
 * {@link CompressedCobblestoneArmorItem} - unbreakable, otherwise ordinary - and exists as its own
 * type only so the set is easy to name. What makes the set special is in {@code ModEvents}, which
 * looks at the armor material rather than at this class, so it is the material that is the real
 * marker of an Ultimate piece.
 * <p>
 * It is also the one set in the mod where the abilities are per slot rather than per set: night
 * vision from the helmet, creative flight from the chestplate, step height from the leggings and
 * speed from the boots, each worth exactly what it is worth on its own. That is the point of it -
 * every other set here is all four pieces or nothing, and this one can be mixed into whatever else
 * a player is wearing a piece at a time.
 */
public class UltimateCompressedArmorItem extends CompressedCobblestoneArmorItem {
    public UltimateCompressedArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }
}
