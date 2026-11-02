package com.panda.commonground;

import com.panda.commonground.mapping.MappingLines;
import com.panda.commonground.platform.Services;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** {@code config/commonground.txt}: written with every form commented out on first start, read on each start and reload. */
public final class GroundConfig {
    private GroundConfig() {}

    public static Path file() {
        return Services.PLATFORM.configDir().resolve("commonground.txt");
    }

    /** The file's lines; a missing file is written first, an unreadable one counts as empty (and says so in the log). */
    public static MappingLines.Config read() {
        Path file = file();
        try {
            if (Files.notExists(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, MappingLines.DEFAULT_FILE, StandardCharsets.UTF_8);
            }
            // a byte that isn't UTF-8 spoils only its own line, not the file
            return MappingLines.parse(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
        } catch (IOException e) {
            CommonGround.LOGGER.error("Common Ground: couldn't read {}; no mapping lines or options apply", file, e);
            return MappingLines.Config.EMPTY;
        }
    }
}
