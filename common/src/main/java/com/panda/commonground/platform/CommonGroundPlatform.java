package com.panda.commonground.platform;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;

/** What common code needs from the mod loader. Each loader module provides one, found with {@link Services}. */
public interface CommonGroundPlatform {
    /** "neoforge" or "fabric". */
    String loaderName();

    /** The display name of the mod that owns a namespace, or the namespace itself. */
    String modName(String namespace);

    /** A biome's spawn settings after every biome modifier the loader ran. */
    MobSpawnSettings finalSpawns(Holder<Biome> biome);
}
