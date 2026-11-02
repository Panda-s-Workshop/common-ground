package com.panda.commonground.tags;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TagRulesTest {
    @Test
    void structureTagsAreLeftOutByDefault() {
        TagRules rules = new TagRules(List.of(), List.of());
        assertFalse(rules.joins("minecraft:has_structure/village_plains"));
        assertFalse(rules.joins("somemod:has_structure/tower"));
        assertFalse(rules.joins("minecraft:mineshaft_blocking"));
        assertFalse(rules.joins("minecraft:stronghold_biased_to"));
        assertFalse(rules.joins("minecraft:required_ocean_monument_surrounding"));
        assertTrue(rules.joins("minecraft:is_forest"));
        assertTrue(rules.joins("c:is_cold/overworld"));
        assertTrue(rules.joins("minecraft:spawns_cold_variant_frogs"));
    }

    @Test
    void excludeAddsAndIncludeLetsBackIn() {
        TagRules rules = new TagRules(List.of("teeming:spawns_*", "c:is_rare"), List.of("minecraft:has_structure/village_*"));
        assertFalse(rules.joins("teeming:spawns_gate/hollow_mourner"));
        assertFalse(rules.joins("c:is_rare"));
        assertTrue(rules.joins("minecraft:has_structure/village_plains"));
        assertFalse(rules.joins("minecraft:has_structure/pillager_outpost"));
        assertTrue(rules.joins("teeming:share_capped"));
    }

    @Test
    void aDotInAPatternIsADot() {
        TagRules rules = new TagRules(List.of("a:b.c"), List.of());
        assertFalse(rules.joins("a:b.c"));
        assertTrue(rules.joins("a:bxc"));
    }
}
