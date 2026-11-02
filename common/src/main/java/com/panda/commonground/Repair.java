package com.panda.commonground;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

/**
 * Spawn repair (NeoForge): a biome whose spawn list is written as a bare value, not an overlay, would lose every
 * mob mod's biome-modifier spawns there. The NeoForge module treats such a list as an overlay of itself, the way
 * Fabric API does, so the modifiers run; the biome's own list is unchanged.
 */
public final class Repair {
    private static volatile boolean enabled = true;
    private static final Set<ResourceKey<Biome>> repaired = new LinkedHashSet<>();

    private Repair() {}

    public static boolean enabled() {
        return enabled;
    }

    public static void enable(boolean on) {
        enabled = on;
    }

    public static synchronized void note(ResourceKey<Biome> biome) {
        repaired.add(biome);
    }

    /** The biomes whose bare spawn list was opened to biome modifiers since the server started making its biomes. */
    public static synchronized List<ResourceKey<Biome>> repaired() {
        return List.copyOf(repaired);
    }

    public static synchronized void reset() {
        repaired.clear();
    }
}
