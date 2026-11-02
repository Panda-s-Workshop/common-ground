package com.panda.commonground.fill;

import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributeLayer;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.MobSpawnSettings;

/**
 * The fill where the game reads spawn lists: one more layer on the {@code natural_mob_spawns} attribute, after the
 * biome layer. At a position whose biome is mapped it gives that biome's filled list; anywhere else, and before the
 * server has built the lists, it passes the value through. Natural spawning, the creatures placed with a new chunk,
 * spawn costs and any mod reading the attribute all go through it.
 *
 * <p>At a mapped position the filled list replaces the value it is given. The list was built from the source's and
 * targets' own lists over an empty one, so a spawn list a dimension type sets for the whole dimension (vanilla sets
 * none) doesn't reach mapped biomes.
 */
public final class SpawnLayer {
    /** Filled lists by mapped biome; swapped whole, so worldgen threads always see a complete map. */
    private static volatile Map<ResourceKey<Biome>, MobSpawnSettings> filled = Map.of();

    private SpawnLayer() {}

    public static void set(Map<ResourceKey<Biome>, MobSpawnSettings> lists) {
        filled = Map.copyOf(lists);
    }

    public static void clear() {
        filled = Map.of();
    }

    /** The mapped biomes' filled lists in use now. */
    public static Map<ResourceKey<Biome>, MobSpawnSettings> filled() {
        return filled;
    }

    /** The layer for one level, finding biomes the way the biome layer does. */
    public static EnvironmentAttributeLayer.Positional<MobSpawnSettings> layer(BiomeManager biomes) {
        boolean full = EnvironmentAttributes.NATURAL_MOB_SPAWNS.isFullResolutionBiomes();
        return (value, pos, interpolator) -> {
            Map<ResourceKey<Biome>, MobSpawnSettings> lists = filled;
            if (lists.isEmpty()) return value;
            Holder<Biome> biome = full ? biomes.getBiome(Mth.floor(pos.x), Mth.floor(pos.y), Mth.floor(pos.z))
                    : biomes.getNoiseBiomeAtPosition(pos.x, pos.y, pos.z);
            MobSpawnSettings list = biome.unwrapKey().map(lists::get).orElse(null);
            return list == null ? value : list;
        };
    }
}
