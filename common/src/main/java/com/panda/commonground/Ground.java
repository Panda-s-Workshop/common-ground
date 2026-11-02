package com.panda.commonground;

import com.panda.commonground.fill.BiomeFill;
import com.panda.commonground.fill.SpawnLayer;
import com.panda.commonground.mapping.MappingTable;
import com.panda.commonground.mapping.Presets;
import com.panda.commonground.platform.Services;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;

/**
 * The server's mappings in force. Built once the loader's biome modifiers have run (NeoForge: server about to start;
 * Fabric: server starting) and again after {@code /reload}; cleared when the server stops.
 */
public final class Ground {
    private static volatile MappingTable.Result mappings = new MappingTable.Result(Map.of(), List.of());
    private static volatile Set<String> presetIds = Set.of();

    private Ground() {}

    public static void start(MinecraftServer server) {
        try {
            build(server);
        } catch (RuntimeException e) {
            // a broken mapping must not stop the server or a /reload; the fill stays as it was
            CommonGround.LOGGER.error("Common Ground: couldn't build the mappings", e);
        }
    }

    private static void build(MinecraftServer server) {
        Registry<Biome> biomes = server.registryAccess().lookupOrThrow(Registries.BIOME);
        Presets.Loaded presets = Presets.load(server.getResourceManager());
        Set<String> ids = biomes.keySet().stream().map(Identifier::toString).collect(Collectors.toSet());
        MappingTable.Result result = MappingTable.resolve(presets.presets(), ids);

        Map<ResourceKey<Biome>, MobSpawnSettings> filled = new HashMap<>();
        result.mapped().forEach((source, mapped) -> {
            List<MobSpawnSettings> targets = mapped.targets().stream().map(t -> spawns(biomes, t)).toList();
            filled.put(key(source), BiomeFill.fill(spawns(biomes, source), targets, type -> false));
        });
        SpawnLayer.set(filled);
        mappings = result;
        presetIds = Set.copyOf(presets.presets().keySet());

        presets.errors().forEach(e -> CommonGround.LOGGER.warn("Common Ground: {}", e));
        result.notes().forEach(n -> CommonGround.LOGGER.info("Common Ground: {}", n));
        CommonGround.LOGGER.info("Common Ground: {} biomes mapped from {} presets", result.mapped().size(), presets.presets().size());
        int repaired = Repair.repaired().size();
        if (repaired > 0) CommonGround.LOGGER.info("Common Ground: {} biomes write their spawn list as a bare value; biome modifiers may now add to them", repaired);
    }

    public static void stop() {
        SpawnLayer.clear();
        Repair.reset();
        mappings = new MappingTable.Result(Map.of(), List.of());
        presetIds = Set.of();
    }

    /** The ids of the presets the server's data holds ({@code ns:id}). */
    public static Set<String> presetIds() {
        return presetIds;
    }

    /** The mappings in force now: by source biome id, its targets and where they came from. */
    public static MappingTable.Result mappings() {
        return mappings;
    }

    private static MobSpawnSettings spawns(Registry<Biome> biomes, String id) {
        Holder<Biome> biome = biomes.getOrThrow(key(id));
        return Services.PLATFORM.finalSpawns(biome);
    }

    private static ResourceKey<Biome> key(String id) {
        return ResourceKey.create(Registries.BIOME, Identifier.parse(id));
    }
}
