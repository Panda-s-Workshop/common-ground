package com.panda.commonground.tags;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Which tags a mapped biome may join. Left out by default are the tags world generation reads while it places
 * structures, so a mapped biome's new chunks don't gain villages or monuments; {@code @exclude-tag} adds patterns and
 * {@code @include-tag} lets a tag back in. A pattern is a tag id where {@code *} stands for any run of characters.
 */
public final class TagRules {
    public static final List<String> LEFT_OUT = List.of("*:has_structure/*", "minecraft:mineshaft_blocking",
            "minecraft:stronghold_biased_to", "minecraft:required_ocean_monument_surrounding");

    private final List<Pattern> out;
    private final List<Pattern> in;

    public TagRules(List<String> exclude, List<String> include) {
        List<String> all = new ArrayList<>(LEFT_OUT);
        all.addAll(exclude);
        this.out = all.stream().map(TagRules::glob).toList();
        this.in = include.stream().map(TagRules::glob).toList();
    }

    public boolean joins(String tag) {
        if (in.stream().anyMatch(p -> p.matcher(tag).matches())) return true;
        return out.stream().noneMatch(p -> p.matcher(tag).matches());
    }

    private static Pattern glob(String pattern) {
        StringBuilder regex = new StringBuilder();
        for (String part : pattern.split("\\*", -1)) {
            if (!regex.isEmpty()) regex.append(".*");
            regex.append(Pattern.quote(part));
        }
        return Pattern.compile(regex.toString());
    }
}
