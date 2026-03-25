package com.mateus.onlineuuidfix.mixin;

import com.mateus.onlineuuidfix.MojangApiHelper;
import net.minecraft.core.UUIDUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(UUIDUtil.class)
public class PlayerManagerMixin {

    /**
     * Intercepts createOfflinePlayerUUID so that offline-mode servers assign the
     * same UUID a player would receive in online mode (fetched from Mojang API).
     *
     * Falls back to the vanilla offline UUID when:
     *  - The player has no Mojang account (cracked client)
     *  - The Mojang API is unreachable
     */
    @Inject(method = "createOfflinePlayerUUID", at = @At("HEAD"), cancellable = true)
    private static void injectCreateOfflinePlayerUUID(String playerName, CallbackInfoReturnable<UUID> cir) {
        UUID onlineUuid = MojangApiHelper.fetchOnlineUuid(playerName);
        if (onlineUuid != null) {
            cir.setReturnValue(onlineUuid);
        }
        // null → let the original method run (vanilla offline UUID)
    }
}
