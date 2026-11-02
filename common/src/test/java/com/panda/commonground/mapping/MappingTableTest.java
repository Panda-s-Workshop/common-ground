package com.panda.commonground.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MappingTableTest {
    private static final Set<String> WORLD = Set.of("minecraft:forest", "minecraft:taiga", "minecraft:plains",
            "terralith:moonlight_grove", "terralith:lavender_forest");

    @Test
    void presetMapsOnlyBiomesThatExist() {
        var result = MappingTable.resolve(Map.of("terralith", Map.of(
                "terralith:moonlight_grove", List.of("minecraft:forest"),
                "terralith:skylands", List.of("minecraft:plains"))), WORLD);
        assertEquals(Map.of("terralith:moonlight_grove", new MappingTable.Mapped(List.of("minecraft:forest"), "preset terralith")), result.mapped());
        assertTrue(result.notes().isEmpty(), "a biome that isn't installed is not worth a note: " + result.notes());
    }

    @Test
    void missingTargetIsSkippedWithANote() {
        var result = MappingTable.resolve(Map.of("terralith", Map.of(
                "terralith:lavender_forest", List.of("minecraft:flower_forest", "minecraft:forest"))), WORLD);
        assertEquals(List.of("minecraft:forest"), result.mapped().get("terralith:lavender_forest").targets());
        assertEquals(1, result.notes().size());
    }

    @Test
    void onlyMissingTargetsMeansNoMapping() {
        var result = MappingTable.resolve(Map.of("x", Map.of("terralith:lavender_forest", List.of("minecraft:nowhere"))), WORLD);
        assertTrue(result.mapped().isEmpty());
    }

    @Test
    void firstPresetByIdWinsAClash() {
        var result = MappingTable.resolve(Map.of(
                "zz_pack", Map.of("terralith:moonlight_grove", List.of("minecraft:plains")),
                "terralith", Map.of("terralith:moonlight_grove", List.of("minecraft:forest"))), WORLD);
        assertEquals(List.of("minecraft:forest"), result.mapped().get("terralith:moonlight_grove").targets());
        assertEquals(1, result.notes().size());
    }

    @Test
    void targetsKeepTheirOrderAndABiomeNeverCountsAsItself() {
        var result = MappingTable.resolve(Map.of("p", Map.of("terralith:moonlight_grove",
                List.of("minecraft:taiga", "terralith:moonlight_grove", "minecraft:forest"))), WORLD);
        assertEquals(List.of("minecraft:taiga", "minecraft:forest"), result.mapped().get("terralith:moonlight_grove").targets());
    }

    @Test
    void aCopyOfAPresetUnderAnotherIdIsNoClash() {
        var table = Map.of("terralith:moonlight_grove", List.of("minecraft:forest"));
        var result = MappingTable.resolve(Map.of("terralith", table, "zz_pack", table), WORLD);
        assertEquals("preset terralith", result.mapped().get("terralith:moonlight_grove").origin());
        assertTrue(result.notes().isEmpty(), "notes " + result.notes());
    }
}
