package com.panda.commonground;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

/**
 * Spawn repair (NeoForge): a biome whose spawn list is written as a bare value, not an overlay, would lose every
 * mob mod's biome-modifier spawns there. The NeoForge module treats such a list as an overlay of itself, the way
 * Fabric API does, so the modifiers run; the biome's own list is unchanged. {@code @repair off} in the config switches
 * it off. The modifiers run before the rest of the mod's start, so the switch is read from the file when first asked.
 */
public final class Repair {
    private static volatile Boolean configured;
    private static volatile Boolean override;
    private static final Set<ResourceKey<Biome>> repaired = new LinkedHashSet<>();

    private Repair() {}

    public static boolean enabled() {
        Boolean forced = override;
        if (forced != null) return forced;
        Boolean on = configured;
        if (on == null) {
            on = GroundConfig.read().repair();
            configured = on;
        }
        return on;
    }

    /** For tests: forces the switch on or off; null goes back to the config file. */
    public static void override(Boolean on) {
        override = on;
    }

    public static synchronized void note(ResourceKey<Biome> biome) {
        repaired.add(biome);
    }

    /** The biomes whose bare spawn list was opened to biome modifiers since the server started making its biomes. */
    public static synchronized List<ResourceKey<Biome>> repaired() {
        return List.copyOf(repaired);
    }

    /** Forgets this server's repairs and the switch, so the next server reads the file again. */
    public static synchronized void reset() {
        repaired.clear();
        configured = null;
    }
}
