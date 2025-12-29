package com.qsmp.translator.client;

import com.qsmp.translator.config.Config;
import com.qsmp.translator.net.Net;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import com.mojang.brigadier.arguments.StringArgumentType;

public class QsmpTranslatorClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPrefs.load();

        ClientPlayNetworking.registerGlobalReceiver(Net.BubblePayload.ID, (payload, context) -> {
            context.client().execute(() -> BubbleState.put(payload.speaker(), payload.text(), payload.isFinal(), payload.tsMs()));
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // send preferred lang to server
            String lang = ClientPrefs.DATA.preferredLang;
            sender.sendPacket(new Net.LangPrefPayload(lang));
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    literal("lingua")
                            .then(argument("lang", StringArgumentType.word())
                                    .executes(ctx -> {
                                        String lang = StringArgumentType.getString(ctx, "lang").toLowerCase();
                                        ClientPrefs.DATA.preferredLang = lang;
                                        ClientPrefs.save();
                                        var client = MinecraftClient.getInstance();
                                        if (client.getNetworkHandler() != null) {
                                            ClientPlayNetworking.send(new Net.LangPrefPayload(lang));
                                        }
                                        ctx.getSource().sendFeedback(Text.of("LinguaCraft Live: preferred language set to " + lang));
                                        return 1;
                                    })
                            )
            );
        });

        WorldRenderEvents.LAST.register(ctx -> BubbleRenderer.render(ctx.matrixStack()));
    }
}
