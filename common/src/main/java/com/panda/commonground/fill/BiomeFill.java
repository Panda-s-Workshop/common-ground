package com.panda.commonground.fill;

import com.panda.commonground.fill.SpawnFill.Entry;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.biome.MobSpawnSettings.MobSpawnCost;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;

/** {@link SpawnFill} on the game's spawn settings: one biome's list filled from its targets'. */
public final class BiomeFill {
    private BiomeFill() {}

    /** The source's spawn settings filled from the targets' (each already final, after every biome modifier). */
    public static MobSpawnSettings fill(MobSpawnSettings own, List<MobSpawnSettings> targets, Predicate<EntityType<?>> excluded) {
        Set<MobCategory> categories = EnumSet.noneOf(MobCategory.class);
        categories.addAll(own.definedCategories());
        targets.forEach(t -> categories.addAll(t.definedCategories()));
        MobSpawnSettings.Builder out = new MobSpawnSettings.Builder();
        Map<EntityType<?>, Integer> broughtBy = new HashMap<>();
        for (MobCategory category : categories) {
            List<Entry<EntityType<?>, IntProvider>> mine = entries(own, category);
            List<List<Entry<EntityType<?>, IntProvider>>> theirs = targets.stream().map(t -> entries(t, category)).toList();
            List<Entry<EntityType<?>, IntProvider>> merged = SpawnFill.merge(mine, theirs, excluded);
            // the merge takes targets in order, so a type came from the first target that lists it
            for (Entry<EntityType<?>, IntProvider> e : merged.subList(mine.size(), merged.size())) {
                for (int i = 0; i < theirs.size() && !broughtBy.containsKey(e.type()); i++) {
                    int target = i;
                    if (theirs.get(i).stream().anyMatch(t -> t.type() == e.type())) broughtBy.put(e.type(), target);
                }
            }
            out.addAllSpawns(category, WeightedList.of(merged.stream()
                    .map(e -> new Weighted<>(new SpawnerData(e.type(), e.data()), e.weight())).toList()));
        }
        Map<EntityType<?>, MobSpawnCost> costs = SpawnFill.costs(own.allSpawnCosts(), targets.stream().map(MobSpawnSettings::allSpawnCosts).toList(), broughtBy);
        out.addAllCosts(costs);
        return out.build();
    }

    private static List<Entry<EntityType<?>, IntProvider>> entries(MobSpawnSettings settings, MobCategory category) {
        return settings.getMobsToSpawn(category).unwrap().stream()
                .<Entry<EntityType<?>, IntProvider>>map(w -> new Entry<>(w.value().type(), w.weight(), w.value().count())).toList();
    }
}
