package com.panda.commonground.mixin;

import com.panda.commonground.fill.SpawnLayer;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Puts the fill's layer after a server level's static layers (the dimension's, then the biomes'), so mapped biomes'
 * spawn lists are read already filled. This runs for levels and for the regions chunk generation works in; client
 * levels get no layer, so a singleplayer client reads what a multiplayer one does.
 */
@Mixin(EnvironmentAttributeSystem.class)
abstract class EnvironmentAttributeSystemMixin {
    @Inject(method = "addStaticLayers", at = @At("TAIL"))
    private static void commonground$fillLayer(EnvironmentAttributeSystem.Builder builder, LevelAccessor level, CallbackInfo ci) {
        if (level.isClientSide()) return;
        builder.addPositionalLayer(EnvironmentAttributes.NATURAL_MOB_SPAWNS, SpawnLayer.layer(level.getBiomeManager()));
    }
}
