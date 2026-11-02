package com.panda.commonground.platform;

/** What common code needs from the mod loader. Each loader module provides one, found with {@link Services}. */
public interface CommonGroundPlatform {
    /** "neoforge" or "fabric". */
    String loaderName();

    /** The display name of the mod that owns a namespace, or the namespace itself. */
    String modName(String namespace);
}
