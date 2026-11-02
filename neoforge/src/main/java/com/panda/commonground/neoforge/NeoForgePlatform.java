package com.panda.commonground.neoforge;

import com.panda.commonground.platform.CommonGroundPlatform;
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
}
