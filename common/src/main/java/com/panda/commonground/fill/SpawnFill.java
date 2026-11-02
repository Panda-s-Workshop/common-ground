package com.panda.commonground.fill;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The fill, as plain data: a mapped biome keeps its own spawn entries and gains, from each of its targets in order,
 * every entry whose mob type it doesn't have yet. {@code T} is the mob type, {@code D} what else an entry carries
 * (the group size).
 */
public final class SpawnFill {
    /** One spawn entry: a mob type, its weight, and the rest of the entry. */
    public record Entry<T, D>(T type, int weight, D data) {}

    private SpawnFill() {}

    /**
     * The source's own entries, unchanged and first, then each target's entries of the types neither the source nor an
     * earlier target has. A target that lists one type twice (two group sizes) brings both. Excluded types never come
     * over.
     */
    public static <T, D> List<Entry<T, D>> merge(List<Entry<T, D>> own, List<List<Entry<T, D>>> targets, Predicate<T> excluded) {
        List<Entry<T, D>> out = new ArrayList<>(own);
        Set<T> present = new HashSet<>();
        own.forEach(e -> present.add(e.type()));
        for (List<Entry<T, D>> target : targets) {
            Set<T> brought = new HashSet<>();
            for (Entry<T, D> e : target) {
                if (present.contains(e.type()) || excluded.test(e.type())) continue;
                out.add(e);
                brought.add(e.type());
            }
            present.addAll(brought);
        }
        return out;
    }

    /**
     * Spawn costs: the source's own, plus, for each type the merge brought in that the source has no cost for, the cost
     * the target that brought it has (none if that target has none).
     *
     * @param broughtBy each added type and the index of the target it came from
     */
    public static <T, C> Map<T, C> costs(Map<T, C> own, List<Map<T, C>> targets, Map<T, Integer> broughtBy) {
        Map<T, C> out = new LinkedHashMap<>(own);
        broughtBy.forEach((type, target) -> {
            C cost = targets.get(target).get(type);
            if (cost != null) out.putIfAbsent(type, cost);
        });
        return out;
    }
}
