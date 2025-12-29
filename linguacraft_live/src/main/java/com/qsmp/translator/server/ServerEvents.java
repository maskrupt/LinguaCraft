package com.qsmp.translator.server;

import com.qsmp.translator.backend.BackendClient;
import com.qsmp.translator.config.Config;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

public class ServerEvents {

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // light prune, once per second
            if (server.getTicks() % 20 == 0) {
                RecipientTracker.prune(Math.max(5000, Config.DATA.performance.audibleWindowMs * 4L));
            }
        });
    }

    public static void onServerStarted(MinecraftServer server) {
        BackendClient.setServer(server);
        BackendClient.connect();
    }

    public static void onServerStopping(MinecraftServer server) {
        BackendClient.close();
    }
}
