package com.panda.commonground.gametest;

import com.panda.commonground.CommonGround;
import com.panda.commonground.platform.Services;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.Weighted;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;

/**
 * The spawn-list fill, read the way the game reads it: the test area's biome is set to a test biome from the
 * {@code cgtest} pack, then natural spawning's own lookup ({@code ChunkGenerator.getMobsAt}) and the attribute are read
 * there. The pack's preset maps {@code cgtest:forest_like} to the forest, {@code cgtest:two_targets} to the plains then
 * the forest, and a stand-in mob mod adds polar bears (weight 7, groups of 1 to 2, with a spawn cost) to the forest.
 */
final class FillTests {
    private FillTests() {}

    static void add(BiConsumer<String, Consumer<GameTestHelper>> tests) {
        tests.accept("fill_adds_target_mobs", FillTests::addsTargetMobs);
        tests.accept("fill_keeps_own_entries", FillTests::keepsOwnEntries);
        tests.accept("fill_two_targets", FillTests::twoTargets);
        tests.accept("fill_spawn_costs", FillTests::spawnCosts);
        tests.accept("fill_leaves_unmapped_biomes", FillTests::leavesUnmapped);
        tests.accept("fill_read_cost", FillTests::readCost);
    }

    /** The forest's mobs reach the forest-like biome at the forest's weights and group sizes, a mob mod's included. */
    private static void addsTargetMobs(GameTestHelper helper) {
        BlockPos pos = setBiome(helper, "cgtest:forest_like");
        List<String> creatures = describe(mobsAt(helper, pos, MobCategory.CREATURE));
        List<String> monsters = describe(mobsAt(helper, pos, MobCategory.MONSTER));
        List<String> wrong = new ArrayList<>();
        for (String want : List.of("minecraft:wolf 5 x4", "minecraft:polar_bear 7 x1-2", "minecraft:pig 10 x4")) {
            if (!creatures.contains(want)) wrong.add("creature " + want);
        }
        for (String want : List.of("minecraft:spider 100 x4", "minecraft:enderman 10 x1-4", "minecraft:witch 5 x1")) {
            if (!monsters.contains(want)) wrong.add("monster " + want);
        }
        if (!wrong.isEmpty()) helper.fail("missing " + wrong + "; creatures " + creatures + ", monsters " + monsters);
        helper.succeed();
    }

    /** The biome's own entries stay first and as written; the forest's sheep and zombie don't come over on top. */
    private static void keepsOwnEntries(GameTestHelper helper) {
        BlockPos pos = setBiome(helper, "cgtest:forest_like");
        List<String> creatures = describe(mobsAt(helper, pos, MobCategory.CREATURE));
        List<String> monsters = describe(mobsAt(helper, pos, MobCategory.MONSTER));
        if (!creatures.getFirst().equals("minecraft:sheep 3 x2")) helper.fail("first creature " + creatures.getFirst());
        if (!monsters.getFirst().equals("minecraft:zombie 40 x2")) helper.fail("first monster " + monsters.getFirst());
        if (creatures.stream().filter(c -> c.startsWith("minecraft:sheep ")).count() != 1) helper.fail("sheep twice: " + creatures);
        if (monsters.stream().filter(c -> c.startsWith("minecraft:zombie ")).count() != 1) helper.fail("zombie twice: " + monsters);
        helper.succeed();
    }

    /** With two targets, the plains' list comes first and the forest adds only what the plains lack. */
    private static void twoTargets(GameTestHelper helper) {
        BlockPos pos = setBiome(helper, "cgtest:two_targets");
        List<String> got = describe(mobsAt(helper, pos, MobCategory.CREATURE));
        List<String> want = new ArrayList<>(describe(finalSpawns(helper, "minecraft:plains").getMobsToSpawn(MobCategory.CREATURE).unwrap()));
        for (String forest : describe(finalSpawns(helper, "minecraft:forest").getMobsToSpawn(MobCategory.CREATURE).unwrap())) {
            String type = forest.substring(0, forest.indexOf(' ') + 1);
            if (want.stream().noneMatch(w -> w.startsWith(type))) want.add(forest);
        }
        if (!got.equals(want)) helper.fail("creatures " + got + ", expected " + want);
        if (!got.contains("minecraft:horse 5 x2-6") || !got.contains("minecraft:wolf 5 x4")) helper.fail("no horse or no wolf: " + got);
        helper.succeed();
    }

    /** A copied mob brings its spawn cost from the target. */
    private static void spawnCosts(GameTestHelper helper) {
        BlockPos pos = setBiome(helper, "cgtest:forest_like");
        MobSpawnSettings settings = helper.getLevel().environmentAttributes().getValue(EnvironmentAttributes.NATURAL_MOB_SPAWNS, pos);
        MobSpawnSettings.MobSpawnCost cost = settings.getMobSpawnCost(EntityTypes.POLAR_BEAR);
        if (cost == null || cost.charge() != 0.7 || cost.energyBudget() != 0.15) helper.fail("polar bear cost " + cost);
        helper.succeed();
    }

    /** A biome nothing maps reads exactly its own final list. */
    private static void leavesUnmapped(GameTestHelper helper) {
        BlockPos pos = setBiome(helper, "minecraft:forest");
        for (MobCategory category : List.of(MobCategory.CREATURE, MobCategory.MONSTER, MobCategory.AMBIENT)) {
            List<String> got = describe(mobsAt(helper, pos, category));
            List<String> own = describe(finalSpawns(helper, "minecraft:forest").getMobsToSpawn(category).unwrap());
            if (!got.equals(own)) helper.fail(category + ": " + got + ", the forest's own " + own);
        }
        helper.succeed();
    }

    /** 100 000 reads at a mapped position take under 50 ms (500 ms on a shared CI runner, which times far less steadily). */
    private static void readCost(GameTestHelper helper) {
        BlockPos pos = setBiome(helper, "cgtest:forest_like");
        var attributes = helper.getLevel().environmentAttributes();
        int sink = 0;
        for (int i = 0; i < 20_000; i++) sink += attributes.getValue(EnvironmentAttributes.NATURAL_MOB_SPAWNS, pos).definedCategories().size();
        long start = System.nanoTime();
        for (int i = 0; i < 100_000; i++) sink += attributes.getValue(EnvironmentAttributes.NATURAL_MOB_SPAWNS, pos).definedCategories().size();
        long ms = (System.nanoTime() - start) / 1_000_000;
        long budget = System.getenv("CI") != null ? 500 : 50;
        CommonGround.LOGGER.info("fill_read_cost: 100000 reads in {} ms ({}), budget {} ms", ms, sink, budget);
        if (ms >= budget) helper.fail("100000 reads took " + ms + " ms");
        helper.succeed();
    }

    /** Sets the biome of the test area and gives the position to read at. */
    private static BlockPos setBiome(GameTestHelper helper, String id) {
        ServerLevel level = helper.getLevel();
        Holder<Biome> biome = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME, Identifier.parse(id)));
        BlockPos from = helper.absolutePos(BlockPos.ZERO);
        FillBiomeCommand.fill(level, from.offset(-8, -8, -8), from.offset(8, 8, 8), biome).ifRight(e -> helper.fail("fillbiome: " + e.getMessage()));
        return from.offset(1, 1, 1);
    }

    private static List<Weighted<SpawnerData>> mobsAt(GameTestHelper helper, BlockPos pos, MobCategory category) {
        ServerLevel level = helper.getLevel();
        return level.getChunkSource().getGenerator().getMobsAt(level, level.structureManager(), category, pos).unwrap();
    }

    private static MobSpawnSettings finalSpawns(GameTestHelper helper, String id) {
        return Services.PLATFORM.finalSpawns(helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME)
                .getOrThrow(ResourceKey.create(Registries.BIOME, Identifier.parse(id))));
    }

    /** "minecraft:wolf 5 x4": type, weight and group size, so a list compares as text. */
    private static List<String> describe(List<Weighted<SpawnerData>> entries) {
        return entries.stream().map(w -> {
            int min = w.value().count().minInclusive();
            int max = w.value().count().maxInclusive();
            return BuiltInRegistries.ENTITY_TYPE.getKey(w.value().type()) + " " + w.weight() + " x" + (min == max ? min : min + "-" + max);
        }).toList();
    }
}
