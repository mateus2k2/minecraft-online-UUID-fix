package com.mateus.onlineuuidfix;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnlineUuidFixMod implements ModInitializer {

    public static final String MOD_ID = "online-uuid-fix";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[OnlineUuidFix] Loaded — offline-mode players will receive their Mojang UUIDs.");
    }
}
