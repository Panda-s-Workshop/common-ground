package com.panda.commonground.neoforge;

import com.panda.commonground.CommonGround;
import com.panda.commonground.gametest.GroundGameTests;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/** The NeoForge entry point: runs the common side and wires its hooks to NeoForge's events. */
@Mod(CommonGround.MODID)
public final class CommonGroundNeoForge {
    public CommonGroundNeoForge(IEventBus modBus, ModContainer container) {
        CommonGround.init();
        GroundGameTests.register(modBus);
    }
}
