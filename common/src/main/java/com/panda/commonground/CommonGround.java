package com.panda.commonground;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

/** The mod's common side. Each loader module has its own entry point, which calls {@link #init()} first. */
public final class CommonGround {
    public static final String MODID = "commonground";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static boolean initialised;

    private CommonGround() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    public static void init() {
        initialised = true;
    }

    public static boolean initialised() {
        return initialised;
    }
}
