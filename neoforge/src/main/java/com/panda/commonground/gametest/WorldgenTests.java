package com.panda.commonground.gametest;

import static com.panda.commonground.gametest.FillTests.describe;
import static com.panda.commonground.gametest.FillTests.mobsAt;
import static com.panda.commonground.gametest.FillTests.setBiome;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/** The fill where world generation reads spawn lists: new chunks' creatures, and structures' own spawn lists. */
final class WorldgenTests {
    private WorldgenTests() {}

    static void add(BiConsumer<String, Consumer<GameTestHelper>> tests) {
        tests.accept("fill_chunk_generation_creatures", WorldgenTests::chunkGenerationCreatures);
        tests.accept("fill_leaves_structure_overrides", WorldgenTests::leavesStructureOverrides);
    }

    /**
     * Chunk generation places a new chunk's creatures through an attribute system it builds for the region with
     * {@code addStaticLayers}; one built the same way gives the forest-like biome its filled list.
     */
    private static void chunkGenerationCreatures(GameTestHelper helper) {
        BlockPos pos = setBiome(helper, "cgtest:forest_like");
        EnvironmentAttributeSystem region = EnvironmentAttributeSystem.builder().addStaticLayers(helper.getLevel()).build();
        MobSpawnSettings settings = region.getValue(EnvironmentAttributes.NATURAL_MOB_SPAWNS, pos);
        List<String> creatures = describe(settings.getMobsToSpawn(MobCategory.CREATURE).unwrap());
        if (!creatures.contains("minecraft:wolf 5 x4") || !creatures.contains("minecraft:pig 10 x4")) helper.fail("creatures " + creatures);
        helper.succeed();
    }

    /**
     * Inside a swamp hut in a mapped biome, the hut's own lists (its witch, its cat) are what spawns, as the game reads
     * a structure's spawn override before the biome's list.
     */
    private static void leavesStructureOverrides(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Holder.Reference<Structure> hut = level.registryAccess().lookupOrThrow(Registries.STRUCTURE)
                .getOrThrow(ResourceKey.create(Registries.STRUCTURE, Identifier.parse("minecraft:swamp_hut")));
        ChunkPos chunk = ChunkPos.containing(helper.absolutePos(BlockPos.ZERO));
        var generator = level.getChunkSource().getGenerator();
        var random = level.getChunkSource().randomState();
        StructureStart start = hut.value().generate(hut, level.dimension(), level.registryAccess(), generator, generator.getBiomeSource(),
                random.createClimateSampler(SamplerContext.EMPTY_UNCACHED), random, level.getStructureTemplateManager(), level.getSeed(),
                chunk, 0, level, b -> true);
        if (!start.isValid()) {
            helper.fail("no swamp hut start");
            return;
        }
        BoundingBox box = start.getBoundingBox();
        BlockPos inside = box.getCenter();
        Holder<Biome> forestLike = level.registryAccess().lookupOrThrow(Registries.BIOME)
                .getOrThrow(ResourceKey.create(Registries.BIOME, Identifier.parse("cgtest:forest_like")));
        FillBiomeCommand.fill(level, new BlockPos(box.minX(), box.minY(), box.minZ()), new BlockPos(box.maxX(), box.maxY(), box.maxZ()), forestLike);
        var access = level.getChunk(chunk.x(), chunk.z());
        access.setStartForStructure(hut.value(), start);
        access.addReferenceForStructure(hut.value(), chunk.pack());
        try {
            List<String> monsters = describe(mobsAt(helper, inside, MobCategory.MONSTER));
            List<String> creatures = describe(mobsAt(helper, inside, MobCategory.CREATURE));
            if (monsters.size() != 1 || !monsters.getFirst().startsWith("minecraft:witch ")) helper.fail("monsters in the hut " + monsters);
            if (creatures.size() != 1 || !creatures.getFirst().startsWith("minecraft:cat ")) helper.fail("creatures in the hut " + creatures);
        } finally {
            access.setStartForStructure(hut.value(), StructureStart.INVALID_START);
        }
        helper.succeed();
    }
}
