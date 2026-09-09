package net.fahr3n.unnecessarilycompressedcobblestone.entity.custom;

import net.fahr3n.unnecessarilycompressedcobblestone.UnnecessarilyCompressedCobblestone;
import net.fahr3n.unnecessarilycompressedcobblestone.util.SelectiveImmunity;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The Compressed Ore Golem. It is built the way an iron golem is - a T of level
 * {@code ModBlocks.ORE_GOLEM_LEVEL} stone with a carved head on top - and it is the hardest thing in
 * the mod to hit, because almost nothing may hit it at all.
 * <p>
 * <b>Only a pickaxe hurts it.</b> That is {@link #refusesDamageFrom} filtered on
 * {@code #minecraft:pickaxes}, which is vanilla's own list, so another mod's pickaxe counts and no
 * sword, axe, bow, staff, potion, explosion or falling anvil does. It is a much stronger statement
 * than any amount of armour and it costs the armour curve nothing - see the note in CLAUDE.md about
 * why raising {@code ARMOR} is the wrong way to make something hard to kill. Everything else still
 * <em>hits</em> it and simply takes nothing off - see {@code SelectiveImmunity} for why that is not
 * the same as {@code isInvulnerableTo} - and {@code /kill}, the void and this mod's true damage are
 * past the rule entirely.
 * <p>
 * It is worth being honest about what that rule and this armour add up to together. Twenty-five
 * armour behind twenty toughness sits at vanilla's reduction ceiling of four fifths against
 * essentially any blow, so 2500 health is 12,500 taken; a netherite pickaxe lands about 1.2 of it a
 * swing. Every other boss in this mod pairs "only X can hurt it" with <em>no</em> armour precisely
 * so that the answer is a key rather than a grind - this one has both because it was asked for both,
 * and the two figures to change if that reads as a wall rather than a fight are {@link #ARMOR} and
 * {@link #MAX_HEALTH}.
 */
public class CompressedOreGolemEntity extends CompressedGolemEntity implements SelectiveImmunity {
    public static final float MAX_HEALTH = 2500.0F;

    /** Five short of the attribute's own ceiling of 30, and exactly at toughness's ceiling of 20. */
    public static final double ARMOR = 25.0;
    public static final double ARMOR_TOUGHNESS = 20.0;

    /** What one swing is worth. Past what a full set of netherite with Protection IV survives. */
    public static final double ATTACK_DAMAGE = 200.0;

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            UnnecessarilyCompressedCobblestone.MOD_ID,
            "textures/entity/compressed_golem/compressed_ore_golem.png");

    public CompressedOreGolemEntity(EntityType<? extends IronGolem> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 1000;
        this.bossEvent.setName(Component.translatable(
                "entity.unnecessarilycompressedcobblestone.compressed_ore_golem"));
        this.bossEvent.setColor(BossEvent.BossBarColor.GREEN);
        setPersistenceRequired();
    }

    /**
     * The golem family's numbers with the health, the plate and the blow raised. The armour is well
     * clear of the 100 the armour note warns about, so the ordinary vanilla curve applies and
     * nothing here can produce a reduction over 100%.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return CompressedGolemEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ARMOR, ARMOR)
                .add(Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    public ResourceLocation texture() {
        return TEXTURE;
    }

    /** It will not fight its own kind, whichever golem that is. */
    @Override
    public boolean canAttackType(EntityType<?> type) {
        return type != ModEntities.COMPRESSED_ORE_GOLEM.get() && super.canAttackType(type);
    }

    /**
     * Everything bounces off it except a pickaxe.
     * <p>
     * The test is on the weapon the blow arrived with rather than on the attacker, which is what
     * makes it read correctly for every path: a player's swing carries their main hand, an arrow
     * carries the bow that fired it, and a blast, a fall or a potion carry nothing at all. The tag
     * is vanilla's own {@code #minecraft:pickaxes}, so this mod's two pickaxes and every other
     * mod's are in it without a line here.
     */
    @Override
    public boolean refusesDamageFrom(DamageSource source) {
        if (!SelectiveImmunity.refusable(source)) {
            return false;
        }

        ItemStack weapon = source.getWeaponItem();
        return weapon == null || !weapon.is(ItemTags.PICKAXES);
    }
}
