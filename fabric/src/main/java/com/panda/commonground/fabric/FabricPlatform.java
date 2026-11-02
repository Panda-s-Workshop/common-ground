package com.panda.commonground.fabric;

import com.panda.commonground.platform.CommonGroundPlatform;
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
}
