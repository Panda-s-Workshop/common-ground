package com.panda.commonground.mapping;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The config file, {@code config/commonground.txt}: one mapping line or option per line, {@code #} starts a comment.
 * <pre>
 * terralith:moonlight_grove = minecraft:forest
 * terralith:snowy_cherry_grove = minecraft:cherry_grove, minecraft:snowy_taiga
 * biomesoplenty:mediterranean_forest = none
 * terrestria:* = auto
 * &#64;guess on
 * &#64;exclude-mob minecraft:slime
 * </pre>
 * Parsing checks only the form; whether a biome, tag or mob exists is checked against the world when the mappings are
 * built. A line that doesn't parse is skipped with an error naming its line number.
 */
public final class MappingLines {
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Pattern NAMESPACE = Pattern.compile("([a-z0-9_.-]+):\\*");
    private static final Pattern TAG_PATTERN = Pattern.compile("[a-z0-9_.*-]+:[a-z0-9_./*-]+");
    private static final Pattern MOB_PATTERN = Pattern.compile("[a-z0-9_.-]+:([a-z0-9_./-]+|\\*)");

    /** {@code source = targets} on line {@code number}; no targets means {@code = none}. */
    public record Line(int number, String source, List<String> targets) {}

    /** {@code namespace:* = auto} ({@code auto} true) or {@code namespace:* = none}. */
    public record NamespaceLine(int number, String namespace, boolean auto) {}

    /** An option's value and the line it is on. */
    public record Option(int number, String value) {}

    public record Config(Map<String, Line> lines, Map<String, NamespaceLine> namespaces, boolean guess, boolean repair,
            List<Option> excludeMobs, List<Option> excludeTags, List<Option> includeTags, List<Option> presetOff,
            List<String> errors) {
        public static final Config EMPTY = parse("");
    }

    private MappingLines() {}

    public static Config parse(String text) {
        Map<String, Line> lines = new LinkedHashMap<>();
        Map<String, NamespaceLine> namespaces = new LinkedHashMap<>();
        boolean guess = true;
        boolean repair = true;
        List<Option> excludeMobs = new ArrayList<>();
        List<Option> excludeTags = new ArrayList<>();
        List<Option> includeTags = new ArrayList<>();
        List<Option> presetOff = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        // an editor may save a byte order mark before the first line
        if (text.startsWith("\uFEFF")) text = text.substring(1);
        String[] raw = text.split("\r?\n", -1);
        for (int i = 0; i < raw.length; i++) {
            int number = i + 1;
            String line = raw[i];
            int hash = line.indexOf('#');
            if (hash >= 0) line = line.substring(0, hash);
            line = line.strip();
            if (line.isEmpty()) continue;
            if (line.startsWith("@")) {
                String[] parts = line.substring(1).strip().split("\\s+");
                String name = parts[0];
                if (parts.length != 2) {
                    errors.add("line " + number + ": @" + name + " takes one value");
                    continue;
                }
                String value = parts[1];
                switch (name) {
                    case "guess", "repair" -> {
                        if (!value.equals("on") && !value.equals("off")) {
                            errors.add("line " + number + ": @" + name + " is on or off, not " + value);
                        } else if (name.equals("guess")) {
                            guess = value.equals("on");
                        } else {
                            repair = value.equals("on");
                        }
                    }
                    case "exclude-mob" -> add(excludeMobs, MOB_PATTERN, number, name, value, errors);
                    case "exclude-tag" -> add(excludeTags, TAG_PATTERN, number, name, value, errors);
                    case "include-tag" -> add(includeTags, TAG_PATTERN, number, name, value, errors);
                    case "preset-off" -> presetOff.add(new Option(number, value));
                    default -> errors.add("line " + number + ": no option @" + name);
                }
                continue;
            }
            int eq = line.indexOf('=');
            if (eq < 0) {
                errors.add("line " + number + ": a mapping line is source = target");
                continue;
            }
            String source = line.substring(0, eq).strip();
            List<String> right = new ArrayList<>();
            for (String t : line.substring(eq + 1).split(",")) {
                if (!t.isBlank()) right.add(t.strip());
            }
            var namespace = NAMESPACE.matcher(source);
            if (namespace.matches()) {
                if (right.equals(List.of("auto")) || right.equals(List.of("none"))) {
                    NamespaceLine earlier = namespaces.get(namespace.group(1));
                    if (earlier != null) errors.add("line " + number + ": " + source + " is also on line " + earlier.number() + "; the later line wins");
                    namespaces.put(namespace.group(1), new NamespaceLine(number, namespace.group(1), right.getFirst().equals("auto")));
                } else {
                    errors.add("line " + number + ": " + source + " takes auto or none");
                }
                continue;
            }
            if (!ID.matcher(source).matches()) {
                errors.add("line " + number + ": " + source + " isn't a biome id");
                continue;
            }
            if (right.equals(List.of("none"))) {
                put(lines, new Line(number, source, List.of()), errors);
                continue;
            }
            String bad = right.stream().filter(t -> !ID.matcher(t).matches()).findFirst().orElse(null);
            if (right.isEmpty() || bad != null) {
                errors.add("line " + number + ": " + (bad == null ? "no target" : bad + " isn't a biome id"));
                continue;
            }
            put(lines, new Line(number, source, List.copyOf(right)), errors);
        }
        return new Config(lines, namespaces, guess, repair, excludeMobs, excludeTags, includeTags, presetOff, errors);
    }

    private static void put(Map<String, Line> lines, Line line, List<String> errors) {
        Line earlier = lines.remove(line.source());
        if (earlier != null) errors.add("line " + line.number() + ": " + line.source() + " is also on line " + earlier.number() + "; the later line wins");
        lines.put(line.source(), line);
    }

    private static void add(List<Option> to, Pattern form, int number, String name, String value, List<String> errors) {
        if (form.matcher(value).matches()) to.add(new Option(number, value));
        else errors.add("line " + number + ": @" + name + " " + value + " isn't an id");
    }

    /** The file written on first start: every form, commented out. */
    public static final String DEFAULT_FILE = """
            # Common Ground: which biome counts as which for mob spawns.
            #
            # A biome mapped onto another keeps its own spawns and also spawns the other biome's mobs it lacks, at that
            # biome's weights, so mob mods that only know vanilla biomes reach modded ones. Presets for popular biome mods
            # are built in; lines here win over presets and over guesses. /reload reads this file again.
            #
            # Mapping lines (the first target wins a mob both targets have):
            #   terralith:moonlight_grove = minecraft:forest
            #   terralith:snowy_cherry_grove = minecraft:cherry_grove, minecraft:snowy_taiga
            #   biomesoplenty:mediterranean_forest = none      (no mapping, whatever a preset says)
            #   terrestria:* = auto                            (every unmapped biome of that mod gets a best guess)
            #   somemod:* = none                               (no presets and no guesses for that mod)
            #
            # Options:
            #   @guess on|off            best guess for biomes nothing maps (default on)
            #   @repair on|off           NeoForge: let biome modifiers add spawns to biomes that write their spawn list
            #                            as a bare value, as Fabric does (default on; needs a restart)
            #   @exclude-mob <id|ns:*>   never copy this mob from a target
            #   @exclude-tag <tag>       never join this tag (* allowed in the path)
            #   @include-tag <tag>       join it after all (structure tags are left out by default)
            #   @preset-off <preset id>  switch a built-in or data-pack preset off (e.g. terralith)
            """;
}
