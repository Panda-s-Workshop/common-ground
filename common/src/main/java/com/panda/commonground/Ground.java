package com.panda.commonground;

import com.panda.commonground.fill.BiomeFill;
import com.panda.commonground.fill.SpawnLayer;
import com.panda.commonground.mapping.MappingLines;
import com.panda.commonground.mapping.MappingTable;
import com.panda.commonground.mapping.Presets;
import com.panda.commonground.platform.Services;
import com.panda.commonground.tags.TagKinship;
import com.panda.commonground.tags.TagRules;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;

/**
 * The server's mappings in force. Built once the loader's biome modifiers have run (NeoForge: server about to start;
 * Fabric: server starting) and again after {@code /reload} (just before the new tags go to players), from the presets
 * in the server's data and {@code config/commonground.txt}; cleared when the server stops.
 */
public final class Ground {
    private static volatile MappingTable.Result mappings = MappingTable.Result.EMPTY;
    private static volatile Set<String> presetIds = Set.of();
    private static volatile Map<String, List<TagKey<Biome>>> joined = Map.of();

    private Ground() {}

    public static void start(MinecraftServer server) {
        try {
            build(server);
        } catch (RuntimeException e) {
            // a broken mapping must not stop the server or a /reload; what was built before the error stays
            CommonGround.LOGGER.error("Common Ground: couldn't build the mappings", e);
        }
    }

    private static void build(MinecraftServer server) {
        Registry<Biome> biomes = server.registryAccess().lookupOrThrow(Registries.BIOME);
        Presets.Loaded presets = Presets.load(server.getResourceManager());
        MappingLines.Config config = GroundConfig.read();
        Set<String> ids = biomes.keySet().stream().map(Identifier::toString).collect(Collectors.toSet());
        MappingTable.Result result = MappingTable.resolve(presets.presets(), config, ids);
        List<String> skipped = new ArrayList<>(config.errors());
        Predicate<EntityType<?>> excluded = excluded(config, skipped);
        skipped.addAll(result.skipped());

        Map<ResourceKey<Biome>, MobSpawnSettings> filled = new HashMap<>();
        result.mapped().forEach((source, mapped) -> {
            List<MobSpawnSettings> targets = mapped.targets().stream().map(t -> spawns(biomes, t)).toList();
            filled.put(key(source), BiomeFill.fill(spawns(biomes, source), targets, excluded));
        });
        Map<String, List<String>> targetsBySource = new LinkedHashMap<>();
        result.mapped().forEach((source, mapped) -> targetsBySource.put(source, mapped.targets()));
        unknownTags(biomes, config.excludeTags(), skipped);
        unknownTags(biomes, config.includeTags(), skipped);
        TagRules rules = new TagRules(config.excludeTags().stream().map(MappingLines.Option::value).toList(),
                config.includeTags().stream().map(MappingLines.Option::value).toList());
        joined = Map.copyOf(TagKinship.bind(biomes, targetsBySource, rules));
        SpawnLayer.set(filled);
        mappings = new MappingTable.Result(result.mapped(), result.unmapped(), result.notes(), List.copyOf(skipped));
        presetIds = Set.copyOf(presets.presets().keySet());

        presets.errors().forEach(e -> CommonGround.LOGGER.warn("Common Ground: {}", e));
        result.notes().forEach(n -> CommonGround.LOGGER.info("Common Ground: {}", n));
        skipped.forEach(s -> CommonGround.LOGGER.warn("Common Ground: {}, {}", GroundConfig.file().getFileName(), s));
        CommonGround.LOGGER.info("Common Ground: {} biomes mapped from {} presets and {} config lines; they joined {} tags in all",
                result.mapped().size(), presets.presets().size(), config.lines().size(), joined.values().stream().mapToInt(List::size).sum());
        int repaired = Repair.repaired().size();
        if (repaired > 0) CommonGround.LOGGER.info("Common Ground: {} biomes write their spawn list as a bare value; biome modifiers may now add to them", repaired);
    }

    /** A tag option without {@code *} that names no biome tag is skipped, so a typo shows in the log. */
    private static void unknownTags(Registry<Biome> biomes, List<MappingLines.Option> options, List<String> skipped) {
        for (MappingLines.Option option : options) {
            if (option.value().contains("*")) continue;
            if (biomes.get(TagKey.create(Registries.BIOME, Identifier.parse(option.value()))).isEmpty()) {
                skipped.add("line " + option.number() + ": no biome tag " + option.value());
            }
        }
    }

    /**
     * {@code @exclude-mob} lines as a test on entity types; an id that names no entity type, or a namespace with none,
     * is skipped.
     */
    private static Predicate<EntityType<?>> excluded(MappingLines.Config config, List<String> skipped) {
        Set<EntityType<?>> types = new HashSet<>();
        Set<String> namespaces = new HashSet<>();
        for (MappingLines.Option option : config.excludeMobs()) {
            if (option.value().endsWith(":*")) {
                String namespace = option.value().substring(0, option.value().length() - 2);
                if (BuiltInRegistries.ENTITY_TYPE.keySet().stream().noneMatch(id -> id.getNamespace().equals(namespace))) {
                    skipped.add("line " + option.number() + ": no mobs in namespace " + namespace);
                }
                namespaces.add(namespace);
                continue;
            }
            var type = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(option.value()));
            if (type.isPresent()) types.add(type.get());
            else skipped.add("line " + option.number() + ": no mob " + option.value());
        }
        if (types.isEmpty() && namespaces.isEmpty()) return type -> false;
        return type -> types.contains(type) || namespaces.contains(BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace());
    }

    public static void stop() {
        SpawnLayer.clear();
        TagKinship.forget();
        joined = Map.of();
        Repair.reset();
        mappings = MappingTable.Result.EMPTY;
        presetIds = Set.of();
    }

    /** Per mapped biome, the tags it joined from its targets. */
    public static Map<String, List<TagKey<Biome>>> joinedTags() {
        return joined;
    }

    /** The ids of the presets the server's data holds ({@code ns:id}). */
    public static Set<String> presetIds() {
        return presetIds;
    }

    /**
     * The mappings in force now: by source biome id, its targets and where they came from; the biomes the config set to
     * none; and every config line skipped (parse errors, missing biomes and mobs), each with its line number.
     */
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
