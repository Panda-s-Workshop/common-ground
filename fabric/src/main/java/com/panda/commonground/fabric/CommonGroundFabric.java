package com.panda.commonground.fabric;

import com.panda.commonground.CommonGround;
import net.fabricmc.api.ModInitializer;

/** The Fabric entry point: runs the common side and wires its hooks to Fabric API's events. */
public final class CommonGroundFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        CommonGround.init();
    }
}
