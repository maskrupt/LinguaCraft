package com.qsmp.translator.server;

import com.qsmp.translator.net.Net;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LangPrefStore {
    public static final ConcurrentHashMap<UUID, String> VIEWER_LANG = new ConcurrentHashMap<>();

    public static String getLang(UUID viewer, String fallback) {
        return VIEWER_LANG.getOrDefault(viewer, fallback);
    }

    public static void registerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(Net.LangPrefPayload.ID, (payload, ctx) -> {
            VIEWER_LANG.put(ctx.player().getUuid(), payload.lang());
        });
    }
}
