package com.panda.commonground.tags;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/**
 * Tag kinship: a mapped biome joins every biome tag its targets are in (directly or through a nested tag, since a bound
 * tag lists every biome it reaches), so tag checks treat it as its targets: mob mods' own spawners and spawn rules,
 * vanilla's cold and warm animal variants, anything that asks {@code biome.is(tag)}. The tags are bound after the biome
 * modifiers ran, so no modifier ever selects a mapped biome through them; the fill brings those spawns instead.
 *
 * <p>Run again after each tag reload and each rebuild: what the last run added is taken out first (where the tags
 * haven't been reloaded since), so a mapping removed from the config leaves no tag behind.
 */
public final class TagKinship {
    /** A tag's contents and a biome's tag set as found, and as bound here, to undo the binding on the next run. */
    private record Bound<V>(V found, V ours) {}

    private static final Map<HolderSet.Named<Biome>, Bound<List<Holder<Biome>>>> tags = new IdentityHashMap<>();
    private static final Map<Holder.Reference<Biome>, Bound<Set<TagKey<Biome>>>> holders = new IdentityHashMap<>();

    private TagKinship() {}

    /**
     * Binds each source into its targets' tags that {@code rules} lets it join.
     *
     * @return per source, the tags it joined
     */
    public static synchronized Map<String, List<TagKey<Biome>>> bind(Registry<Biome> registry, Map<String, List<String>> mapped, TagRules rules) {
        undo();
        Map<String, List<TagKey<Biome>>> joined = new LinkedHashMap<>();
        Map<TagKey<Biome>, List<Holder<Biome>>> additions = new LinkedHashMap<>();
        // every target's tags as the data gave them, before any source is bound: a target that is itself mapped passes
        // on only its own tags, not ones it joins here (no chains)
        Map<String, List<TagKey<Biome>>> found = new LinkedHashMap<>();
        mapped.values().forEach(targets -> targets.forEach(target ->
                found.computeIfAbsent(target, t -> registry.get(key(t)).map(h -> h.tags().toList()).orElse(List.of()))));
        mapped.forEach((source, targets) -> {
            Holder.Reference<Biome> holder = registry.get(key(source)).orElse(null);
            if (holder == null) return;
            Set<TagKey<Biome>> own = new LinkedHashSet<>(holder.tags().toList());
            List<TagKey<Biome>> gained = new ArrayList<>();
            for (String target : targets) {
                for (TagKey<Biome> tag : found.get(target)) {
                    if (!own.contains(tag) && !gained.contains(tag) && rules.joins(tag.location().toString())) gained.add(tag);
                }
            }
            if (gained.isEmpty()) return;
            joined.put(source, List.copyOf(gained));
            gained.forEach(tag -> additions.computeIfAbsent(tag, t -> new ArrayList<>()).add(holder));
            Set<TagKey<Biome>> all = new LinkedHashSet<>(own);
            all.addAll(gained);
            Set<TagKey<Biome>> before = holder.tags;
            holder.bindTags(all);
            holders.put(holder, new Bound<>(before, holder.tags));
        });
        additions.forEach((tag, sources) -> registry.get(tag).ifPresent(set -> {
            List<Holder<Biome>> before = set.contents;
            List<Holder<Biome>> ours = new ArrayList<>(before);
            ours.addAll(sources);
            set.bind(List.copyOf(ours));
            tags.put(set, new Bound<>(before, set.contents));
        }));
        return joined;
    }

    /** Takes out what the last run bound, wherever the tags still hold what it bound (a reload replaces them). */
    public static synchronized void undo() {
        tags.forEach((set, bound) -> {
            if (set.contents == bound.ours()) set.bind(bound.found());
        });
        holders.forEach((holder, bound) -> {
            if (holder.tags == bound.ours()) holder.bindTags(bound.found());
        });
        tags.clear();
        holders.clear();
    }

    /** Forgets the bindings without undoing them: the server stopped and its registries are gone. */
    public static synchronized void forget() {
        tags.clear();
        holders.clear();
    }

    private static ResourceKey<Biome> key(String id) {
        return ResourceKey.create(Registries.BIOME, Identifier.parse(id));
    }
}
