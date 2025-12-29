package com.qsmp.translator;

import com.qsmp.translator.config.Config;
import com.qsmp.translator.net.Net;
import com.qsmp.translator.server.LangPrefStore;
import com.qsmp.translator.server.ServerEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

public class QsmpTranslatorMod implements ModInitializer {
    @Override
    public void onInitialize() {
        Config.load(FabricLoader.getInstance().getConfigDir());
        Net.registerPayloads();
        ServerEvents.register();
        LangPrefStore.registerReceivers();

        ServerLifecycleEvents.SERVER_STARTED.register(ServerEvents::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(ServerEvents::onServerStopping);
    }
}
