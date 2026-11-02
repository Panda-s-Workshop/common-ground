package com.panda.commonground.gametest;

import com.panda.commonground.CommonGround;
import com.panda.commonground.Ground;
import com.panda.commonground.Texts;
import com.panda.commonground.platform.Services;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.locale.Language;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Checks that run on a GameTest server. */
public final class GroundGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, CommonGround.MODID);
    private static final Identifier ARENA = CommonGround.id("test_arena");

    static {
        FUNCTIONS.register("mod_loaded", () -> GroundGameTests::modLoaded);
        FUNCTIONS.register("lang_coverage", () -> GroundGameTests::langCoverage);
        FillTests.add((name, test) -> FUNCTIONS.register(name, () -> test));
        RepairTests.add((name, test) -> FUNCTIONS.register(name, () -> test));
        FUNCTIONS.register("preset_applies_only_to_existing_biomes", () -> GroundGameTests::presetOnlyExisting);
    }

    private GroundGameTests() {}

    public static void register(IEventBus modBus) {
        FUNCTIONS.register(modBus);
        modBus.addListener(GroundGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(CommonGround.id("default"));
        // -Dcommonground.tests=a,b_*: only the tests named (each a name, * for any run of letters)
        String only = System.getProperty("commonground.tests", "");
        List<Pattern> wanted = Arrays.stream(only.split(",")).map(String::trim).filter(n -> !n.isEmpty())
                .map(n -> Pattern.compile(n.replaceAll("[^a-z0-9_*]", "").replace("*", "[a-z0-9_]*"))).toList();
        for (var entry : FUNCTIONS.getEntries()) {
            String path = entry.getId().getPath();
            if (!wanted.isEmpty() && wanted.stream().noneMatch(w -> w.matcher(path).matches())) continue;
            ResourceKey<Consumer<GameTestHelper>> key = ResourceKey.create(Registries.TEST_FUNCTION, entry.getId());
            event.registerTest(key.identifier(), new FunctionGameTestInstance(key, new TestData<>(env, ARENA, 100, 0, true)));
        }
    }

    /** The common side ran and the loader's platform service is found. */
    private static void modLoaded(GameTestHelper helper) {
        if (!CommonGround.initialised()) helper.fail("CommonGround.init() never ran");
        String loader = Services.PLATFORM.loaderName();
        if (!"neoforge".equals(loader)) helper.fail("the platform service is " + loader);
        helper.succeed();
    }

    /**
     * The shipped Terralith preset is loaded but maps nothing here (Terralith isn't installed), without a note; the
     * test pack's preset maps its three biomes.
     */
    private static void presetOnlyExisting(GameTestHelper helper) {
        var mapped = Ground.mappings().mapped();
        if (!mapped.keySet().equals(Set.of("cgtest:forest_like", "cgtest:two_targets", "cgtest:plain_value"))) {
            helper.fail("mapped " + mapped.keySet());
        }
        if (!mapped.get("cgtest:forest_like").origin().equals("preset cgtest:cgtest")) helper.fail("origin " + mapped.get("cgtest:forest_like"));
        if (Ground.mappings().notes().stream().anyMatch(n -> n.startsWith("terralith:"))) helper.fail("notes " + Ground.mappings().notes());
        if (!Ground.presetIds().contains("commonground:terralith")) helper.fail("presets loaded " + Ground.presetIds());
        helper.succeed();
    }

    /** Every translation key the mod uses has its English text. */
    private static void langCoverage(GameTestHelper helper) {
        Language lang = Language.getInstance();
        List<String> missing = Texts.ALL.stream().filter(key -> !lang.has(key)).toList();
        if (!missing.isEmpty()) helper.fail("no English text for " + missing);
        helper.succeed();
    }
}
