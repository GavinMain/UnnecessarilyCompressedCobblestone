package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.potion.ModMobEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * The Compressed Scythe. An axe in every list, tag and enchantment that reads one - it chops wood,
 * strips logs and knocks a shield out of the way the same as any other - and different from one in
 * exactly one thing: what it leaves in the wound.
 * <p>
 * Every hit that connects applies {@link ModMobEffects#BLEEDING}, and the damage bleeding does is
 * the smaller half of it. What it really does is stop the target healing at all: no regeneration,
 * no golden apple, no potion, no natural regeneration from a full hunger bar, and no
 * {@code heal()} from anything else in the game - see {@code ModEvents}, which cancels the heal
 * event outright. Against anything that outlasts a fight by out-healing it - a Compressed Golem
 * Tier 2 standing on its own ground, a player behind Regeneration - the scythe is not a damage
 * increase, it is the thing that makes the damage count.
 * <p>
 * It is an axe rather than a sword deliberately. An axe's shape is a slow, heavy swing that has to
 * be aimed, which is what a weapon whose whole value is landing a status effect should feel like;
 * and a sword's speed would make re-applying the bleed free rather than a choice.
 * <p>
 * Inscribing it increases its damage, which needs no code here: it is in
 * {@code #ucc:compression_melee_weapon}, so the Compression Inscriber turns its energy into attack
 * damage the way it does for the sword, the mace and the katana. Like every other tool in this mod
 * it keeps a real durability and never spends it - see {@link CompressedCobblestoneSwordItem} for
 * why the {@code minecraft:unbreakable} component is the wrong way to do that.
 */
public class CompressedScytheItem extends AxeItem {
    /** On top of the tier's own bonus, the way every other weapon here states its damage. */
    public static final float ATTACK_DAMAGE = 8.0F;

    /** A diamond axe is -3.0, which is one swing a second. A scythe is not a quick weapon. */
    public static final float ATTACK_SPEED = -3.0F;

    /** How long one cut bleeds for. */
    public static final int BLEEDING_DURATION = 8 * 20;

    /** Bleeding I. The amplifier is one less than the numeral. */
    public static final int BLEEDING_AMPLIFIER = 0;

    public CompressedScytheItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    /** An axe's own two modifiers at this weapon's figures. */
    public static ItemAttributeModifiers createAttributes(Tier tier) {
        return DiggerItem.createAttributes(tier, ATTACK_DAMAGE, ATTACK_SPEED);
    }

    /**
     * The cut. {@code hurtEnemy} is called only after the blow has actually landed, so a swing that
     * was blocked, missed or absorbed leaves nothing behind - which is what makes the bleed
     * something to land rather than something to spray.
     * <p>
     * It is re-applied rather than stacked: vanilla's merge takes the longer of the two durations,
     * so hitting again refreshes the eight seconds. That is the right shape here, because the
     * expensive half of bleeding is the healing lock and a lock does not get stronger by being
     * turned twice.
     */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        target.addEffect(new MobEffectInstance(ModMobEffects.BLEEDING, BLEEDING_DURATION,
                BLEEDING_AMPLIFIER), attacker);

        return super.hurtEnemy(stack, target, attacker);
    }

    /** Swallows every point of durability damage before vanilla can apply it. */
    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
        return 0;
    }

    /** The durability is never spent, so say so the way an unbreakable item does. */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("item.unbreakable").withStyle(ChatFormatting.BLUE));
    }
}
