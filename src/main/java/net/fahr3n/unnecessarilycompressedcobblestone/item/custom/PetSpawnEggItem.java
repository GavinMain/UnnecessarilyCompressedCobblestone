package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import java.util.function.Supplier;

import net.fahr3n.unnecessarilycompressedcobblestone.util.PetAi;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Spawner;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;

/**
 * A spawn egg whose mob comes out belonging to whoever used it.
 * <p>
 * It exists because vanilla's own path has nowhere to say that. {@code SpawnEggItem.useOn} hands
 * everything to {@code EntityType.spawn}, which builds the mob, finalizes it and adds it to the
 * level with no hook in between - and a pet that arrives ownerless is not a pet at all, it is a
 * monster wearing one's model, since {@code PetAi.isEnemyOf} answers the ownerless case by hunting
 * players. That is exactly what would happen to a Ghast Pet hatched out of an ordinary egg: it would
 * turn on the person who hatched it.
 * <p>
 * So the spawn is done here instead, and the one line vanilla has no room for -
 * {@link PetAi.Adoptable#setPetOwner} - is run between finalizing the mob and adding it. Handing a
 * type to a monster spawner is still worth doing and is still entirely vanilla's business; a pet out
 * of a spawner belongs to nobody, which is the honest answer to a block that has no owner either.
 */
public class PetSpawnEggItem extends DeferredSpawnEggItem {
    private final Supplier<? extends EntityType<? extends Mob>> pet;

    public PetSpawnEggItem(Supplier<? extends EntityType<? extends Mob>> pet, int backgroundColor,
                           int highlightColor, Properties properties) {
        super(pet, backgroundColor, highlightColor, properties);
        this.pet = pet;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        BlockPos clicked = context.getClickedPos();
        if (level.getBlockEntity(clicked) instanceof Spawner) {
            return super.useOn(context);
        }

        // The same square vanilla would have spawned the mob on: the clicked block if you can stand
        // in it, otherwise the one against the face you clicked.
        BlockPos pos = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty()
                ? clicked
                : clicked.relative(context.getClickedFace());

        Entity spawned = this.pet.get().spawn(serverLevel, context.getItemInHand(), context.getPlayer(),
                pos, MobSpawnType.SPAWN_EGG, true, false);
        if (spawned == null) {
            return InteractionResult.PASS;
        }

        if (spawned instanceof PetAi.Adoptable adoptable) {
            adoptable.setPetOwner(context.getPlayer());
        }

        level.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, pos);
        context.getItemInHand().consume(1, context.getPlayer());
        return InteractionResult.CONSUME;
    }
}
