package net.fahr3n.unnecessarilycompressedcobblestone.util;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.fahr3n.unnecessarilycompressedcobblestone.block.ModBlocks;
import net.fahr3n.unnecessarilycompressedcobblestone.entity.ModEntities;
import net.minecraft.world.entity.EntityType;

/**
 * What each boss leaves behind: ten minutes during which the stone a player breaks comes up as that
 * boss's own compression level instead of as cobblestone.
 * <p>
 * One constant per boss, and the constant is the whole of it. Each carries the boss it is earned
 * from, the compression level it hands out and the colour its particles are, and everything
 * downstream loops over this list - {@code ModMobEffects} registers one effect per constant,
 * {@code ModEvents} looks a dead boss up here to decide what to hand out and looks a mining player
 * up here to decide what to drop. Adding a boss to the family is a constant, a lang line and an
 * icon, and no code anywhere else.
 * <p>
 * The level is the one that boss already drops, so the effect is not a new reward so much as a
 * faster way of collecting the one that was always there - which is also why the levels here must
 * stay in step with {@code ModEntityLootTableProvider}: a spoil that paid out deeper stone than the
 * boss itself would make the corpse the lesser half of the fight.
 */
public enum BossSpoil {
    GOLEM("golem", 13, 0xD8D8D8, ModEntities.COMPRESSED_GOLEM),
    CREEPER("creeper", 27, 0x62C34F, ModEntities.COMPRESSED_CREEPER),
    CONJURER("conjurer", 44, 0x7FC8FF, ModEntities.COMPRESSED_CONJURER),
    SKELETON("skeleton", 67, 0xE8E4D8, ModEntities.COMPRESSED_SKELETON),
    SKELETON_TIER_2("skeleton_tier_2", 76, 0xF0A050, ModEntities.COMPRESSED_SKELETON_TIER_2),
    SKELETON_TIER_3("skeleton_tier_3", 84, 0xA070E0, ModEntities.COMPRESSED_SKELETON_TIER_3),
    SUMMONER("summoner", 100, 0x4FD8C8, ModEntities.COMPRESSED_SUMMONER),
    SPIRIT("spirit", 108, 0x6BE3E8, ModEntities.COMPRESSED_SPIRIT),

    /**
     * The second golem's, which is the one level here that is read rather than typed: its drop is
     * {@code GOLEM_TIER_2_LEVEL + 1} in the loot table, and repeating the arithmetic is how the two
     * would eventually disagree.
     */
    GOLEM_TIER_2("golem_tier_2", ModBlocks.GOLEM_TIER_2_LEVEL + 1, 0xF2D13B,
            ModEntities.COMPRESSED_GOLEM_TIER_2),

    WITCH("witch", 132, 0xB050D0, ModEntities.COMPRESSED_WITCH),
    CHICKEN_BOSS("chicken_boss", 157, 0xF0C040, ModEntities.COMPRESSED_CHICKEN_BOSS),
    CREEPER_TIER_2("creeper_tier_2", 164, 0x1D5A2A, ModEntities.COMPRESSED_CREEPER_TIER_2),
    COMPOSER("composer", 172, 0x3C5DE0, ModEntities.COMPRESSED_COMPOSER),
    GUARDIAN("guardian", 179, 0x3FA096, ModEntities.COMPRESSED_GUARDIAN),
    HUSK("husk", 197, 0xD9C8A0, ModEntities.COMPRESSED_HUSK),
    SNOW_GOLEM("snow_golem", 206, 0xEDF2F5, ModEntities.COMPRESSED_SNOW_GOLEM);

    /** Ten minutes, which is what one boss is worth. */
    public static final int DURATION = 12000;

    private final String name;
    private final int level;
    private final int color;
    private final Supplier<? extends EntityType<?>> boss;

    BossSpoil(String name, int level, int color, Supplier<? extends EntityType<?>> boss) {
        this.name = name;
        this.level = level;
        this.color = color;
        this.boss = boss;
    }

    /** The effect's registry id, which is also its lang key. */
    public String effectName() {
        return this.name + "_spoils";
    }

    /** The compression level the stone comes up as, which is the boss's own drop. */
    public int level() {
        return this.level;
    }

    /** What colour the effect's particles are. */
    public int color() {
        return this.color;
    }

    /**
     * The spoil {@code type} is worth, or null if it is not a boss.
     * <p>
     * A supplier is held rather than the type itself, because this enum is read while the entity
     * types are still registering - resolving one in a constant's arguments would be a class asking
     * another for something it has not finished making yet.
     */
    @Nullable
    public static BossSpoil of(EntityType<?> type) {
        for (BossSpoil spoil : values()) {
            if (spoil.boss.get() == type) {
                return spoil;
            }
        }

        return null;
    }
}
