package com.panda.commonground.gametest;

import com.panda.commonground.Repair;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/**
 * Spawn repair on NeoForge. {@code cgtest:plain_value} writes its spawn list as a bare value (rabbits only), and a
 * stand-in mob mod's biome modifier adds goats (weight 6, groups of 2) to it.
 */
final class RepairTests {
    private static final ResourceKey<Biome> PLAIN = ResourceKey.create(Registries.BIOME, Identifier.parse("cgtest:plain_value"));

    private RepairTests() {}

    static void add(BiConsumer<String, Consumer<GameTestHelper>> tests) {
        tests.accept("repair_plain_spawn_value", RepairTests::plainSpawnValue);
    }

    /**
     * The mob mod's goats are in the biome's final list next to its own rabbits, and the biome is counted as repaired.
     * Then NeoForge's own biome builder, given the biome as written and the same addition: with the repair the goats
     * land, with it off they're dropped as NeoForge drops them.
     */
    private static void plainSpawnValue(GameTestHelper helper) {
        Holder.Reference<Biome> biome = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(PLAIN);
        MobSpawnSettings live = biome.value().getModifiedEnvironmentAttributes()
                .applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS, MobSpawnSettings.EMPTY);
        if (!has(live, EntityTypes.GOAT, 6)) helper.fail("no goat 6 in the final list: " + live.getMobsToSpawn(MobCategory.CREATURE));
        if (!has(live, EntityTypes.RABBIT, 4)) helper.fail("the biome's own rabbit 4 is gone: " + live.getMobsToSpawn(MobCategory.CREATURE));
        if (!Repair.repaired().contains(PLAIN)) helper.fail("not counted as repaired: " + Repair.repaired());

        ModifiableBiomeInfo.BiomeInfo written = biome.value().modifiableBiomeInfo().getOriginalBiomeInfo();
        boolean before = Repair.enabled();
        try {
            if (!has(withGoats(written, true), EntityTypes.GOAT, 6)) helper.fail("with the repair on, the added goats are dropped");
            if (has(withGoats(written, false), EntityTypes.GOAT, 6)) helper.fail("with the repair off, the goats still land");
        } finally {
            Repair.enable(before);
        }
        helper.succeed();
    }

    /** The biome's spawns after a biome modifier's goat spawn, as NeoForge's builder makes them. */
    private static MobSpawnSettings withGoats(ModifiableBiomeInfo.BiomeInfo written, boolean repair) {
        Repair.enable(repair);
        var builder = ModifiableBiomeInfo.BiomeInfo.Builder.copyOf(PLAIN, written);
        builder.getMobSpawnSettings().addSpawn(EntityTypes.GOAT, MobCategory.CREATURE, 6, ConstantInt.of(2));
        return builder.build().attributes().applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS, MobSpawnSettings.EMPTY);
    }

    private static boolean has(MobSpawnSettings settings, EntityType<?> type, int weight) {
        for (Weighted<MobSpawnSettings.SpawnerData> w : settings.getMobsToSpawn(MobCategory.CREATURE).unwrap()) {
            if (w.value().type() == type && w.weight() == weight) return true;
        }
        return false;
    }
}
