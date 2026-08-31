package net.fahr3n.unnecessarilycompressedcobblestone.item.custom;

import java.util.function.Supplier;

import net.fahr3n.unnecessarilycompressedcobblestone.entity.custom.BossSummonEggEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Spawner;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;

/**
 * A spawn egg that does not spawn anything on the tick it is used. It leaves a
 * {@link BossSummonEggEntity} instead - the dragon egg that spins, climbs and bursts - and the boss
 * arrives five seconds later, out of that.
 * <p>
 * Everything else a spawn egg does is inherited, including handing a monster spawner its type, which
 * is passed back to vanilla below rather than reimplemented.
 */
public class BossSpawnEggItem extends DeferredSpawnEggItem {
    private final Supplier<? extends EntityType<? extends Mob>> boss;

    public BossSpawnEggItem(Supplier<? extends EntityType<? extends Mob>> boss, int backgroundColor,
                            int highlightColor, Properties properties) {
        super(boss, backgroundColor, highlightColor, properties);
        this.boss = boss;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        // Putting a type into a spawner is still worth doing, and is entirely vanilla's business.
        BlockPos clicked = context.getClickedPos();
        if (level.getBlockEntity(clicked) instanceof Spawner) {
            return super.useOn(context);
        }

        // The same square vanilla would have spawned the mob on: the clicked block if you can stand
        // in it, otherwise the one against the face you clicked.
        BlockPos pos = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty()
                ? clicked
                : clicked.relative(context.getClickedFace());

        serverLevel.addFreshEntity(new BossSummonEggEntity(serverLevel,
                pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, this.boss.get()));
        level.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, pos);
        context.getItemInHand().consume(1, context.getPlayer());
        return InteractionResult.CONSUME;
    }
}
