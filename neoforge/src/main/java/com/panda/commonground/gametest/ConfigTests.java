package com.panda.commonground.gametest;

import static com.panda.commonground.gametest.FillTests.describe;
import static com.panda.commonground.gametest.FillTests.mobsAt;
import static com.panda.commonground.gametest.FillTests.setBiome;

import com.panda.commonground.Ground;
import com.panda.commonground.GroundConfig;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.MobCategory;

/**
 * The config file, written for real and read the way {@code /reload} reads it. Each test writes its lines, rebuilds the
 * mappings, checks, then puts the file back and rebuilds again, all in one call, so no other test sees its lines.
 */
final class ConfigTests {
    private ConfigTests() {}

    static void add(BiConsumer<String, Consumer<GameTestHelper>> tests) {
        tests.accept("config_line_overrides_preset", ConfigTests::lineOverridesPreset);
        tests.accept("config_none_unmaps", ConfigTests::noneUnmaps);
        tests.accept("config_skips_bad_lines", ConfigTests::skipsBadLines);
        tests.accept("fill_exclude_mob", ConfigTests::excludeMob);
    }

    /** A line maps the forest-like biome to the plains instead of the preset's forest: horses come, wolves don't. */
    private static void lineOverridesPreset(GameTestHelper helper) {
        BlockPos pos = setBiome(helper, "cgtest:forest_like");
        withConfig(helper, "cgtest:forest_like = minecraft:plains\n", () -> {
            var mapped = Ground.mappings().mapped().get("cgtest:forest_like");
            if (mapped == null || !mapped.origin().equals("config line 1")) helper.fail("mapped " + mapped);
            List<String> creatures = describe(mobsAt(helper, pos, MobCategory.CREATURE));
            if (!creatures.contains("minecraft:horse 5 x2-6")) helper.fail("no horse: " + creatures);
            if (creatures.stream().anyMatch(c -> c.startsWith("minecraft:wolf "))) helper.fail("the forest's wolf still comes: " + creatures);
        });
        helper.succeed();
    }

    /** {@code = none} takes the preset's mapping away: the biome reads its own list only. */
    private static void noneUnmaps(GameTestHelper helper) {
        BlockPos pos = setBiome(helper, "cgtest:forest_like");
        withConfig(helper, "# only this\ncgtest:forest_like = none\n", () -> {
            if (Ground.mappings().mapped().containsKey("cgtest:forest_like")) helper.fail("still mapped");
            if (!Ground.mappings().unmapped().contains("cgtest:forest_like")) helper.fail("not listed as set to none");
            List<String> creatures = describe(mobsAt(helper, pos, MobCategory.CREATURE));
            if (!creatures.equals(List.of("minecraft:sheep 3 x2"))) helper.fail("creatures " + creatures);
        });
        helper.succeed();
    }

    /** Bad lines are skipped with their numbers; the good line around them still applies and the preset stays. */
    private static void skipsBadLines(GameTestHelper helper) {
        withConfig(helper, "cgtest:nowhere = minecraft:forest\ncgtest:two_targets = minecraft:taiga\n@exclude-mob minecraft:dragonfly\nnot a line\n", () -> {
            List<String> skipped = Ground.mappings().skipped();
            for (String line : List.of("line 1:", "line 3:", "line 4:")) {
                if (skipped.stream().noneMatch(s -> s.startsWith(line))) helper.fail("no skip for " + line + " " + skipped);
            }
            if (skipped.size() != 3) helper.fail("skipped " + skipped);
            var two = Ground.mappings().mapped().get("cgtest:two_targets");
            if (two == null || !two.targets().equals(List.of("minecraft:taiga"))) helper.fail("two_targets " + two);
            if (!Ground.mappings().mapped().containsKey("cgtest:forest_like")) helper.fail("the preset's mapping went");
        });
        helper.succeed();
    }

    /** {@code @exclude-mob} keeps a mob from coming over from any target; the source's own entry of it stays. */
    private static void excludeMob(GameTestHelper helper) {
        BlockPos pos = setBiome(helper, "cgtest:forest_like");
        withConfig(helper, "@exclude-mob minecraft:wolf\n@exclude-mob minecraft:polar_bear\n@exclude-mob minecraft:sheep\n", () -> {
            List<String> creatures = describe(mobsAt(helper, pos, MobCategory.CREATURE));
            if (creatures.stream().anyMatch(c -> c.startsWith("minecraft:wolf ") || c.startsWith("minecraft:polar_bear "))) {
                helper.fail("an excluded mob came over: " + creatures);
            }
            if (!creatures.contains("minecraft:pig 10 x4")) helper.fail("the pig, not excluded, is missing: " + creatures);
            if (!creatures.getFirst().equals("minecraft:sheep 3 x2")) helper.fail("the biome's own sheep went: " + creatures);
        });
        helper.succeed();
    }

    /** Writes the config, rebuilds, runs the checks, then restores the file as it was (or removes it) and rebuilds. */
    static void withConfig(GameTestHelper helper, String text, Runnable checks) {
        Path file = GroundConfig.file();
        try {
            byte[] before = Files.exists(file) ? Files.readAllBytes(file) : null;
            Files.createDirectories(file.getParent());
            Files.writeString(file, text, StandardCharsets.UTF_8);
            try {
                Ground.start(helper.getLevel().getServer());
                checks.run();
            } finally {
                if (before != null) Files.write(file, before);
                else Files.deleteIfExists(file);
                Ground.start(helper.getLevel().getServer());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
