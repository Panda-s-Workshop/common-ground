package com.panda.commonground.neoforge;

import com.panda.commonground.CommonGround;
import com.panda.commonground.Ground;
import com.panda.commonground.gametest.GroundGameTests;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** The NeoForge entry point: runs the common side and wires its hooks to NeoForge's events. */
@Mod(CommonGround.MODID)
public final class CommonGroundNeoForge {
    public CommonGroundNeoForge(IEventBus modBus, ModContainer container) {
        CommonGround.init();
        GroundGameTests.register(modBus);
        if (System.getProperty("commonground.testpack") != null) {
            // test runs only: test biomes, a preset for them and a stand-in mob mod's spawns (datapacks/cgtest)
            modBus.addListener((AddPackFindersEvent e) -> e.addPackFinders(CommonGround.id("datapacks/cgtest"), PackType.SERVER_DATA,
                    Component.literal("Common Ground test biomes"), PackSource.BUILT_IN, true, Pack.Position.TOP));
        }

        var game = NeoForge.EVENT_BUS;
        // the biome modifiers have run just before this
        game.addListener((ServerAboutToStartEvent e) -> Ground.start(e.getServer()));
        // no player: this is a /reload, not a login
        game.addListener((OnDatapackSyncEvent e) -> {
            if (e.getPlayer() == null) Ground.start(e.getPlayerList().getServer());
        });
        game.addListener((ServerStoppedEvent e) -> Ground.stop());
    }
}
