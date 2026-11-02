package com.panda.commonground.mixin;

import com.panda.commonground.Ground;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * After {@code /reload} has loaded the new data and bound the new tags, and before players are sent those tags, the
 * mappings, the fill and the tag kinship are built again from the new presets and the config file.
 */
@Mixin(PlayerList.class)
abstract class PlayerListMixin {
    @Inject(method = "reloadResources", at = @At("HEAD"))
    private void commonground$rebuild(CallbackInfo ci) {
        Ground.start(((PlayerList) (Object) this).getServer());
    }
}
