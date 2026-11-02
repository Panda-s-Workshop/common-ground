package com.panda.commonground.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Which biome counts as which, worked out from the presets with biome ids as plain strings (so it is testable without a
 * world). A preset maps only biomes that exist; when two presets map one biome the preset whose id sorts first wins.
 */
public final class MappingTable {
    /** A source's targets and where the mapping came from ("preset terralith"). */
    public record Mapped(List<String> targets, String origin) {}

    /** The mappings in force, by source, and the notes worth a log line (clashes, missing targets). */
    public record Result(Map<String, Mapped> mapped, List<String> notes) {}

    private MappingTable() {}

    /**
     * @param presets preset id → (source → targets), as the data gave them
     * @param biomes  every biome id in the world's registry
     */
    public static Result resolve(Map<String, Map<String, List<String>>> presets, Set<String> biomes) {
        Map<String, Mapped> mapped = new LinkedHashMap<>();
        List<String> notes = new ArrayList<>();
        for (var preset : new TreeMap<>(presets).entrySet()) {
            for (var line : preset.getValue().entrySet()) {
                String source = line.getKey();
                if (!biomes.contains(source)) continue; // that biome mod isn't installed: not worth a line
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
        return new Result(Collections.unmodifiableMap(mapped), List.copyOf(notes));
    }
}
