package com.panda.commonground.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PresetsTest {
    @Test
    void readsListsAndSingleTargets() {
        var json = JsonParser.parseString("""
                {"mod": "terralith", "mappings": {
                  "terralith:moonlight_grove": ["minecraft:forest", "minecraft:taiga"],
                  "terralith:steppe": "minecraft:plains"}}""").getAsJsonObject();
        List<String> errors = new ArrayList<>();
        assertEquals(Map.of("terralith:moonlight_grove", List.of("minecraft:forest", "minecraft:taiga"),
                "terralith:steppe", List.of("minecraft:plains")), Presets.mappings("t", json, errors));
        assertEquals(List.of(), errors);
    }

    @Test
    void aTargetThatIsNotAnIdIsSkippedAloneWithAnError() {
        var json = JsonParser.parseString("""
                {"mappings": {"terralith:steppe": ["minecraft:plains", 3, {"id": "x"}], "terralith:highlands": ["minecraft:meadow"]}}""")
                .getAsJsonObject();
        List<String> errors = new ArrayList<>();
        var out = Presets.mappings("t", json, errors);
        assertEquals(List.of("minecraft:plains"), out.get("terralith:steppe"));
        assertEquals(List.of("minecraft:meadow"), out.get("terralith:highlands"));
        assertEquals(2, errors.size(), "errors " + errors);
    }
}
