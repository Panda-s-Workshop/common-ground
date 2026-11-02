package com.panda.commonground.fabric.client;

import com.panda.commonground.client.DevExit;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

/** The Fabric client entry point. The mod draws nothing; this only ends dev runs cleanly. */
public final class CommonGroundFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> DevExit.onClientStopping());
    }
}
