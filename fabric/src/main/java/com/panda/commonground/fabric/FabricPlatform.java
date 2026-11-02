package com.panda.commonground.fabric;

import com.panda.commonground.platform.CommonGroundPlatform;
import net.minecraft.core.Holder;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.fabricmc.loader.api.FabricLoader;

/** {@link CommonGroundPlatform} on Fabric. */
public final class FabricPlatform implements CommonGroundPlatform {
    @Override
    public String loaderName() {
        return "fabric";
    }

    @Override
    public String modName(String namespace) {
        return FabricLoader.getInstance().getModContainer(namespace).map(c -> c.getMetadata().getName()).orElse(namespace);
    }

    @Override
    public MobSpawnSettings finalSpawns(Holder<Biome> biome) {
        // Fabric API changes the biome's own map in place
        return biome.value().getAttributes().applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS, MobSpawnSettings.EMPTY);
    }
}
