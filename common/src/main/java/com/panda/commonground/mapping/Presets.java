package com.panda.commonground.mapping;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * The presets in the server's data: {@code data/<ns>/commonground/preset/<id>.json}, each
 * {@code {"mod": ..., "mappings": {source: [targets]}}}. A data pack replaces one by giving the same file.
 */
public final class Presets {
    static final String FOLDER = "commonground/preset";

    /** Presets by id ({@code ns:id}), and what was wrong with the files that didn't read. */
    public record Loaded(Map<String, Map<String, List<String>>> presets, List<String> errors) {}

    private Presets() {}

    public static Loaded load(ResourceManager resources) {
        Map<String, Map<String, List<String>>> presets = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        for (Map.Entry<Identifier, Resource> file : resources.listResources(FOLDER, id -> id.getPath().endsWith(".json")).entrySet()) {
            String path = file.getKey().getPath();
            String id = file.getKey().getNamespace() + ":" + path.substring(FOLDER.length() + 1, path.length() - ".json".length());
            try (Reader reader = file.getValue().openAsReader()) {
                presets.put(id, mappings(id, JsonParser.parseReader(reader).getAsJsonObject(), errors));
            } catch (Exception e) {
                errors.add("preset " + id + " doesn't read: " + e.getMessage());
            }
        }
        return new Loaded(presets, errors);
    }

    static Map<String, List<String>> mappings(String id, JsonObject json, List<String> errors) {
        Map<String, List<String>> out = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> line : json.getAsJsonObject("mappings").entrySet()) {
            List<String> targets = new ArrayList<>();
            JsonElement value = line.getValue();
            // one target may be written without the list
            JsonArray list = value.isJsonArray() ? value.getAsJsonArray() : new JsonArray();
            if (!value.isJsonArray()) list.add(value);
            for (JsonElement target : list) {
                if (target.isJsonPrimitive() && target.getAsJsonPrimitive().isString()) targets.add(target.getAsString());
                else errors.add("preset " + id + ": " + line.getKey() + " has a target that isn't a biome id: " + target);
            }
            out.put(line.getKey(), targets);
        }
        return out;
    }
}
