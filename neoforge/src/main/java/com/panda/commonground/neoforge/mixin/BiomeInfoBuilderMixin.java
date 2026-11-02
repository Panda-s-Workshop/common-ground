package com.panda.commonground.neoforge.mixin;

import com.panda.commonground.Repair;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.attribute.modifier.MobSpawnSettingsModifier;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.world.EnvironmentAttributeMapBuilder;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Before NeoForge decides whether biome modifiers may change a biome's spawns, a spawn list written as a bare value
 * becomes an overlay holding that same list, so the modifiers' spawns land instead of being dropped.
 */
@Mixin(ModifiableBiomeInfo.BiomeInfo.Builder.class)
abstract class BiomeInfoBuilderMixin {
    @ModifyVariable(method = "copyOf", at = @At("STORE"), ordinal = 0)
    private static EnvironmentAttributeMapBuilder commonground$spawnsAsOverlay(EnvironmentAttributeMapBuilder attributes,
            ResourceKey<Biome> biome, ModifiableBiomeInfo.BiomeInfo original) {
        if (!Repair.enabled()) return attributes;
        EnvironmentAttributeMap.Entry<MobSpawnSettings, ?> entry = attributes.get(EnvironmentAttributes.NATURAL_MOB_SPAWNS);
        if (entry == null || entry.modifier() == MobSpawnSettingsModifier.overlay()) return attributes;
        MobSpawnSettings list = entry.applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS.defaultValue());
        attributes.modify(EnvironmentAttributes.NATURAL_MOB_SPAWNS, MobSpawnSettingsModifier.overlay(), list);
        Repair.note(biome);
        return attributes;
    }
}
