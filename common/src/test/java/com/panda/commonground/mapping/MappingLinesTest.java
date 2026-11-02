package com.panda.commonground.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MappingLinesTest {
    private static final Set<String> WORLD = Set.of("minecraft:forest", "minecraft:taiga", "minecraft:cherry_grove",
            "minecraft:snowy_taiga", "terralith:moonlight_grove", "terralith:snowy_cherry_grove", "terralith:steppe");
    private static final Map<String, Map<String, List<String>>> PRESETS = Map.of("commonground:terralith", Map.of(
            "terralith:moonlight_grove", List.of("minecraft:forest"),
            "terralith:steppe", List.of("minecraft:forest")));

    @Test
    void everyFormParses() {
        var config = MappingLines.parse("""
                # a comment
                terralith:moonlight_grove = minecraft:forest   # after a line too
                  terralith:snowy_cherry_grove=minecraft:cherry_grove,minecraft:snowy_taiga

                biomesoplenty:mediterranean_forest = none
                terrestria:* = auto
                somemod:* = none
                @guess off
                @repair off
                @exclude-mob minecraft:slime
                @exclude-mob alexsmobs:*
                @exclude-tag somemod:has_village_extras
                @include-tag minecraft:has_structure/village_*
                @preset-off nullscape
                """);
        assertEquals(List.of(), config.errors());
        assertEquals(List.of("minecraft:forest"), config.lines().get("terralith:moonlight_grove").targets());
        assertEquals(2, config.lines().get("terralith:moonlight_grove").number());
        assertEquals(List.of("minecraft:cherry_grove", "minecraft:snowy_taiga"), config.lines().get("terralith:snowy_cherry_grove").targets());
        assertEquals(List.of(), config.lines().get("biomesoplenty:mediterranean_forest").targets());
        assertTrue(config.namespaces().get("terrestria").auto());
        assertFalse(config.namespaces().get("somemod").auto());
        assertFalse(config.guess());
        assertFalse(config.repair());
        assertEquals(List.of("minecraft:slime", "alexsmobs:*"), config.excludeMobs().stream().map(MappingLines.Option::value).toList());
        assertEquals("somemod:has_village_extras", config.excludeTags().getFirst().value());
        assertEquals("minecraft:has_structure/village_*", config.includeTags().getFirst().value());
        assertEquals("nullscape", config.presetOff().getFirst().value());
    }

    @Test
    void defaultsAreOnAndTheDefaultFileHasNoActiveLine() {
        var config = MappingLines.parse(MappingLines.DEFAULT_FILE);
        assertTrue(config.guess());
        assertTrue(config.repair());
        assertTrue(config.lines().isEmpty());
        assertEquals(List.of(), config.errors());
    }

    @Test
    void eachBadLineIsSkippedWithItsNumber() {
        var config = MappingLines.parse("""
                terralith:steppe minecraft:forest
                Terralith:Steppe = minecraft:forest
                terralith:steppe = Forest
                terralith:steppe =
                terralith:* = minecraft:forest
                @guess maybe
                @fly on
                @exclude-mob
                @exclude-mob slime
                terralith:steppe = minecraft:forest, none
                """);
        assertEquals(10, config.errors().size(), "errors " + config.errors());
        for (int i = 0; i < 10; i++) assertTrue(config.errors().get(i).startsWith("line " + (i + 1) + ":"), config.errors().get(i));
        assertTrue(config.lines().isEmpty());
    }

    @Test
    void theLaterOfTwoLinesForOneBiomeWins() {
        var config = MappingLines.parse("terralith:steppe = minecraft:forest\nterralith:steppe = minecraft:taiga\n");
        assertEquals(List.of("minecraft:taiga"), config.lines().get("terralith:steppe").targets());
        assertEquals(1, config.errors().size());
    }

    @Test
    void configWinsOverPresets() {
        var result = MappingTable.resolve(PRESETS, MappingLines.parse("terralith:moonlight_grove = minecraft:taiga\n"), WORLD);
        assertEquals(new MappingTable.Mapped(List.of("minecraft:taiga"), "config line 1"), result.mapped().get("terralith:moonlight_grove"));
        assertEquals("preset commonground:terralith", result.mapped().get("terralith:steppe").origin());
    }

    @Test
    void noneTakesAPresetsMappingAway() {
        var result = MappingTable.resolve(PRESETS, MappingLines.parse("terralith:steppe = none\n"), WORLD);
        assertFalse(result.mapped().containsKey("terralith:steppe"));
        assertEquals(Set.of("terralith:steppe"), result.unmapped());
    }

    @Test
    void namespaceNoneDropsPresetsButNotConfigLines() {
        var result = MappingTable.resolve(PRESETS, MappingLines.parse("terralith:* = none\nterralith:steppe = minecraft:taiga\n"), WORLD);
        assertEquals(Set.of("terralith:steppe"), result.mapped().keySet());
    }

    @Test
    void presetOffByIdOrByName() {
        assertTrue(MappingTable.resolve(PRESETS, MappingLines.parse("@preset-off terralith\n"), WORLD).mapped().isEmpty());
        assertTrue(MappingTable.resolve(PRESETS, MappingLines.parse("@preset-off commonground:terralith\n"), WORLD).mapped().isEmpty());
        var unknown = MappingTable.resolve(PRESETS, MappingLines.parse("@preset-off nope\n"), WORLD);
        assertEquals(1, unknown.skipped().size());
    }

    @Test
    void missingBiomesAreSkippedWithTheLineNumber() {
        var result = MappingTable.resolve(PRESETS, MappingLines.parse("""
                terralith:skyland = minecraft:forest
                terralith:steppe = minecraft:nowhere, minecraft:taiga
                terralith:moonlight_grove = minecraft:nowhere
                """), WORLD);
        assertEquals(List.of("line 1: no biome terralith:skyland", "line 2: no biome minecraft:nowhere", "line 3: no biome minecraft:nowhere"), result.skipped());
        assertEquals(List.of("minecraft:taiga"), result.mapped().get("terralith:steppe").targets());
        // a line whose every target is missing leaves the preset's mapping in place
        assertEquals("preset commonground:terralith", result.mapped().get("terralith:moonlight_grove").origin());
    }

    @Test
    void aConfigLineMayUseAModdedTarget() {
        var result = MappingTable.resolve(Map.of(), MappingLines.parse("terralith:steppe = terralith:moonlight_grove\n"), WORLD);
        assertEquals(List.of("terralith:moonlight_grove"), result.mapped().get("terralith:steppe").targets());
    }

    @Test
    void aByteOrderMarkBeforeTheFirstLineIsIgnored() {
        var config = MappingLines.parse("﻿terralith:steppe = minecraft:forest\r\n");
        assertEquals(List.of(), config.errors());
        assertEquals(List.of("minecraft:forest"), config.lines().get("terralith:steppe").targets());
    }
}
