package com.panda.commonground.neoforge.client;

import com.panda.commonground.CommonGround;
import com.panda.commonground.client.DevExit;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;

/** The NeoForge client entry point. The mod draws nothing; this only ends dev runs cleanly. */
@Mod(value = CommonGround.MODID, dist = Dist.CLIENT)
public final class CommonGroundNeoForgeClient {
    public CommonGroundNeoForgeClient(IEventBus modBus, ModContainer container) {
        NeoForge.EVENT_BUS.addListener((GameShuttingDownEvent e) -> DevExit.onClientStopping());
    }
}
