package com.panda.commonground.neoforge;

import com.panda.commonground.platform.CommonGroundPlatform;
import net.minecraft.core.Holder;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.fml.ModList;

/** {@link CommonGroundPlatform} on NeoForge. */
public final class NeoForgePlatform implements CommonGroundPlatform {
    @Override
    public String loaderName() {
        return "neoforge";
    }

    @Override
    public String modName(String namespace) {
        return ModList.get().getModContainerById(namespace).map(c -> c.getModInfo().getDisplayName()).orElse(namespace);
    }

    @Override
    public MobSpawnSettings finalSpawns(Holder<Biome> biome) {
        return biome.value().getModifiedEnvironmentAttributes().applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS, MobSpawnSettings.EMPTY);
    }
}
