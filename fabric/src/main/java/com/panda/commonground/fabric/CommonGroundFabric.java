package com.panda.commonground.fabric;

import com.panda.commonground.CommonGround;
import com.panda.commonground.Ground;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

/** The Fabric entry point: runs the common side and wires its hooks to Fabric API's events. */
public final class CommonGroundFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        CommonGround.init();
        // Fabric API's biome modifications ran when the server was made
        ServerLifecycleEvents.SERVER_STARTING.register(Ground::start);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> Ground.stop());
    }
}
