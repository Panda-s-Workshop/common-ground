package com.panda.commonground.gametest;

import static com.panda.commonground.gametest.FillTests.setBiome;

import com.panda.commonground.tags.TagRules;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.biome.Biome;

/**
 * Tag kinship on the test biomes: {@code cgtest:forest_like} is mapped to the forest, and the test pack nests the
 * forest in {@code cgtest:nested_inner}, itself inside {@code cgtest:nested_outer}.
 */
final class TagTests {
    private static final TagRules DEFAULT_RULES = new TagRules(List.of(), List.of());

    private TagTests() {}

    static void add(BiConsumer<String, Consumer<GameTestHelper>> tests) {
        tests.accept("tags_join_target_tags", TagTests::joinTargetTags);
        tests.accept("tags_nested", TagTests::nested);
        tests.accept("tags_skip_structures", TagTests::skipStructures);
        tests.accept("tags_include_option", TagTests::includeOption);
        tests.accept("tags_variant_rule", TagTests::variantRule);
        tests.accept("tags_survive_reload", TagTests::surviveReload);
        tests.accept("tags_no_chains", TagTests::noChains);
    }

    /** The mapped biome is in every forest tag the default rules let it join, both ways: the holder and the tag's list. */
    private static void joinTargetTags(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        Holder<Biome> source = biome(helper, "cgtest:forest_like");
        for (TagKey<Biome> tag : biome(helper, "minecraft:forest").tags().toList()) {
            if (!DEFAULT_RULES.joins(tag.location().toString())) continue;
            if (!source.is(tag)) helper.fail("not in " + tag.location());
            // the tag's own list, which readers that walk a tag and the tags sent to players use
            if (registry.getOrThrow(tag).stream().noneMatch(h -> h.equals(source))) helper.fail(tag.location() + " doesn't list it");
        }
        if (!source.is(TagKey.create(Registries.BIOME, Identifier.parse("minecraft:is_forest")))) helper.fail("not a forest");
        // an unmapped test biome joins nothing
        if (biome(helper, "cgtest:unplaced").is(TagKey.create(Registries.BIOME, Identifier.parse("minecraft:is_forest")))) helper.fail("unplaced joined");
        helper.succeed();
    }

    /** A tag that holds the forest only through another tag holds the mapped biome too. */
    private static void nested(GameTestHelper helper) {
        Holder<Biome> source = biome(helper, "cgtest:forest_like");
        if (!source.is(tag("cgtest:nested_inner"))) helper.fail("not in the inner tag");
        if (!source.is(tag("cgtest:nested_outer"))) helper.fail("not in the outer tag, which holds the forest through the inner one");
        helper.succeed();
    }

    /** Structure tags stay out, so a mapped biome's new chunks get no structures its own data doesn't place. */
    private static void skipStructures(GameTestHelper helper) {
        Set<String> structures = biome(helper, "minecraft:forest").tags().map(t -> t.location().toString())
                .filter(t -> t.contains(":has_structure/")).collect(Collectors.toSet());
        if (structures.isEmpty()) helper.fail("the forest has no structure tag to test with");
        List<String> joined = biome(helper, "cgtest:forest_like").tags().map(t -> t.location().toString()).filter(structures::contains).toList();
        if (!joined.isEmpty()) helper.fail("joined structure tags " + joined);
        helper.succeed();
    }

    /** {@code @include-tag} lets a structure tag in; {@code @exclude-tag} keeps an ordinary one out. */
    private static void includeOption(GameTestHelper helper) {
        String structure = biome(helper, "minecraft:forest").tags().map(t -> t.location().toString())
                .filter(t -> t.contains(":has_structure/")).sorted().findFirst().orElse(null);
        if (structure == null) {
            helper.fail("the forest has no structure tag to test with");
            return;
        }
        ConfigTests.withConfig(helper, "@include-tag " + structure + "\n@exclude-tag minecraft:is_forest\n", () -> {
            Holder<Biome> source = biome(helper, "cgtest:forest_like");
            if (!source.is(tag(structure))) helper.fail("not in " + structure + " with @include-tag");
            if (source.is(tag("minecraft:is_forest"))) helper.fail("in is_forest despite @exclude-tag");
        });
        // and back as it was once the lines are gone
        if (biome(helper, "cgtest:forest_like").is(tag(structure))) helper.fail("still in " + structure + " after the lines went");
        if (!biome(helper, "cgtest:forest_like").is(tag("minecraft:is_forest"))) helper.fail("not back in is_forest");
        helper.succeed();
    }

    /** Vanilla's cold cow rule reads a biome tag: a cow spawned in a biome mapped to the snowy plains is a cold cow. */
    private static void variantRule(GameTestHelper helper) {
        BlockPos pos = setBiome(helper, "cgtest:two_targets").above(2);
        ConfigTests.withConfig(helper, "cgtest:two_targets = minecraft:snowy_plains\n", () -> {
            Cow cow = EntityTypes.COW.spawn(helper.getLevel(), pos, EntitySpawnReason.NATURAL);
            if (cow == null) {
                helper.fail("no cow");
                return;
            }
            String variant = cow.getVariant().unwrapKey().map(k -> k.identifier().toString()).orElse("?");
            cow.discard();
            if (!variant.equals("minecraft:cold")) helper.fail("the cow is " + variant);
        });
        helper.succeed();
    }

    /**
     * A biome mapped onto another mapped biome takes only that biome's own tags: the forest-like biome joins the
     * forest's tags, a biome mapped onto the forest-like one doesn't get them through it.
     */
    private static void noChains(GameTestHelper helper) {
        ConfigTests.withConfig(helper, "cgtest:two_targets = cgtest:forest_like\n", () -> {
            if (!biome(helper, "cgtest:forest_like").is(tag("minecraft:is_forest"))) helper.fail("forest_like isn't a forest");
            if (biome(helper, "cgtest:two_targets").is(tag("minecraft:is_forest"))) helper.fail("two_targets became a forest through forest_like");
        });
        helper.succeed();
    }

    /** {@code /reload} binds the tags from the data again; the mapped biome is in its target's tags after it. */
    private static void surviveReload(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        CompletableFuture<Void> reload = server.reloadResources(server.getPackRepository().getSelectedIds());
        helper.succeedWhen(() -> {
            if (!reload.isDone()) throw helper.assertionException("still reloading");
            if (reload.isCompletedExceptionally()) helper.fail("the reload failed");
            if (!biome(helper, "cgtest:forest_like").is(tag("minecraft:is_forest"))) helper.fail("not a forest after /reload");
            if (!biome(helper, "cgtest:forest_like").is(tag("cgtest:nested_outer"))) helper.fail("not in the nested tag after /reload");
        });
    }

    private static Holder<Biome> biome(GameTestHelper helper, String id) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME, Identifier.parse(id)));
    }

    private static TagKey<Biome> tag(String id) {
        return TagKey.create(Registries.BIOME, Identifier.parse(id));
    }
}
