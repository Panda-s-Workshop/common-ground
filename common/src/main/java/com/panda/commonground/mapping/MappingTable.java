package com.panda.commonground.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Which biome counts as which, worked out from the presets and the config with biome ids as plain strings (so it is
 * testable without a world). Config lines win over presets. A preset maps only biomes that exist; when two presets map
 * one biome the preset whose id sorts first wins.
 */
public final class MappingTable {
    /** A source's targets and where the mapping came from ("preset terralith", "config line 4"). */
    public record Mapped(List<String> targets, String origin) {}

    /**
     * The mappings in force, by source; the biomes a config line set to {@code none}; the notes worth a log line
     * (clashes, missing targets); and the config lines skipped, each with its reason.
     */
    public record Result(Map<String, Mapped> mapped, Set<String> unmapped, List<String> notes, List<String> skipped) {
        public static final Result EMPTY = new Result(Map.of(), Set.of(), List.of(), List.of());
    }

    private MappingTable() {}

    /**
     * @param presets preset id → (source → targets), as the data gave them
     * @param biomes  every biome id in the world's registry
     */
    public static Result resolve(Map<String, Map<String, List<String>>> presets, Set<String> biomes) {
        return resolve(presets, MappingLines.Config.EMPTY, biomes);
    }

    public static Result resolve(Map<String, Map<String, List<String>>> presets, MappingLines.Config config, Set<String> biomes) {
        Map<String, Mapped> mapped = new LinkedHashMap<>();
        List<String> notes = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        Set<String> off = new LinkedHashSet<>();
        for (MappingLines.Option option : config.presetOff()) {
            List<String> ids = presets.keySet().stream().filter(id -> id.equals(option.value()) || id.endsWith(":" + option.value())).toList();
            if (ids.isEmpty()) skipped.add("line " + option.number() + ": no preset " + option.value());
            off.addAll(ids);
        }
        for (var preset : new TreeMap<>(presets).entrySet()) {
            if (off.contains(preset.getKey())) continue;
            for (var line : preset.getValue().entrySet()) {
                String source = line.getKey();
                if (!biomes.contains(source)) continue; // that biome mod isn't installed: not worth a line
                var namespace = config.namespaces().get(namespaceOf(source));
                if (namespace != null && !namespace.auto()) continue;
                Mapped earlier = mapped.get(source);
                if (earlier != null) {
                    // a pack's copy of a shipped preset under its own id is no clash
                    if (!line.getValue().equals(earlier.targets())) {
                        notes.add(source + ": preset " + preset.getKey() + " also maps it; " + earlier.origin() + " wins");
                    }
                    continue;
                }
                List<String> targets = new ArrayList<>();
                for (String target : line.getValue()) {
                    if (target.equals(source)) continue;
                    if (biomes.contains(target)) targets.add(target);
                    else notes.add(source + ": target " + target + " doesn't exist (preset " + preset.getKey() + ")");
                }
                if (!targets.isEmpty()) mapped.put(source, new Mapped(List.copyOf(targets), "preset " + preset.getKey()));
            }
        }
        Set<String> unmapped = new LinkedHashSet<>();
        for (MappingLines.Line line : config.lines().values()) {
            String where = "line " + line.number() + ": ";
            if (!biomes.contains(line.source())) {
                skipped.add(where + "no biome " + line.source());
                continue;
            }
            if (line.targets().isEmpty()) {
                mapped.remove(line.source());
                unmapped.add(line.source());
                continue;
            }
            List<String> targets = new ArrayList<>();
            for (String target : line.targets()) {
                if (target.equals(line.source())) skipped.add(where + line.source() + " can't count as itself");
                else if (biomes.contains(target)) targets.add(target);
                else skipped.add(where + "no biome " + target);
            }
            if (!targets.isEmpty()) mapped.put(line.source(), new Mapped(List.copyOf(targets), "config line " + line.number()));
        }
        for (var namespace : config.namespaces().values()) {
            if (biomes.stream().noneMatch(b -> namespaceOf(b).equals(namespace.namespace()))) {
                skipped.add("line " + namespace.number() + ": no biomes in namespace " + namespace.namespace());
            }
        }
        return new Result(Collections.unmodifiableMap(mapped), Collections.unmodifiableSet(unmapped), List.copyOf(notes), List.copyOf(skipped));
    }

    private static String namespaceOf(String id) {
        return id.substring(0, id.indexOf(':'));
    }
}
