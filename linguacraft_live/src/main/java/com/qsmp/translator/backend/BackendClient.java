package com.qsmp.translator.backend;

import com.google.gson.Gson;
import com.qsmp.translator.config.Config;
import com.qsmp.translator.net.Net;
import com.qsmp.translator.server.LangPrefStore;
import com.qsmp.translator.server.RecipientTracker;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletionStage;

public class BackendClient {

    private static final Gson GSON = new Gson();
    private static volatile WebSocket ws;
    private static volatile MinecraftServer server;

    public static void setServer(MinecraftServer srv) { server = srv; }

    public static void connect() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            ws = client.newWebSocketBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .buildAsync(URI.create(Config.DATA.backendUrl), new Listener())
                    .join();
        } catch (Exception e) {
            ws = null;
            System.err.println("[linguacraft_live] Backend WS connect failed: " + e.getMessage());
        }
    }

    public static void close() {
        try { if (ws != null) ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye"); } catch (Exception ignored) {}
        ws = null;
    }

    public static void sendAudioChunk(UUID speaker, short[] pcm16k, int sampleRate, boolean isFinal) {
        if (ws == null) return;

        // encode PCM16LE base64
        ByteBuffer bb = ByteBuffer.allocate(pcm16k.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (short s : pcm16k) bb.putShort(s);

        String b64 = Base64.getEncoder().encodeToString(bb.array());

        // ask backend for these targets (global enabled list, capped)
        List<String> targets = Config.DATA.enabledTargetLangs;
        if (targets == null || targets.isEmpty()) targets = List.of("en");
        int cap = Math.max(1, Config.DATA.performance.maxTranslationsPerMessage);
        if (targets.size() > cap) targets = targets.subList(0, cap);

        var msg = new AudioChunk(
                "audio.chunk",
                speaker.toString(),
                Config.DATA.sourceLangDefault,
                isFinal,
                sampleRate,
                targets,
                b64
        );
        ws.sendText(GSON.toJson(msg), true);
    }

    private record AudioChunk(
            String type,
            String speakerUuid,
            String srcLang,
            boolean isFinal,
            int sampleRate,
            List<String> targets,
            String pcm16leB64
    ) {}

    private static class Listener implements WebSocket.Listener {
        private final StringBuilder sb = new StringBuilder();

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            sb.append(data);
            if (last) {
                String json = sb.toString();
                sb.setLength(0);
                handleBackendMessage(json);
            }
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            System.err.println("[linguacraft_live] Backend WS error: " + error.getMessage());
            WebSocket.Listener.super.onError(webSocket, error);
        }
    }

    @SuppressWarnings("unchecked")
    private static void handleBackendMessage(String json) {
        if (server == null) return;
        Map<String, Object> map;
        try {
            map = GSON.fromJson(json, Map.class);
        } catch (Exception e) {
            return;
        }
        String type = (String) map.get("type");
        String speakerStr = (String) map.get("speakerUuid");
        if (type == null || speakerStr == null) return;

        UUID speaker;
        try { speaker = UUID.fromString(speakerStr); } catch (Exception e) { return; }

        boolean isFinal = "transcript.final".equals(type);
        if (!isFinal && !"transcript.partial".equals(type)) return;

        String srcLang = (String) map.getOrDefault("srcLang", Config.DATA.sourceLangDefault);
        String textSrc = (String) map.getOrDefault("textSrc", "");

        Map<String, String> translations = new HashMap<>();
        Object trObj = map.get("translations");
        if (trObj instanceof Map<?, ?> trMap) {
            for (var e : trMap.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    translations.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
                }
            }
        }

        ServerPlayerEntity speakerPlayer = server.getPlayerManager().getPlayer(speaker);
        if (speakerPlayer == null) return;

        // Who can actually hear them via SVC (recently)?
        Set<UUID> viewers = RecipientTracker.currentAudible(speaker, Config.DATA.performance.audibleWindowMs);

        // Fallback to distance if none (e.g., event hooks not firing in some edge cases)
        if (viewers.isEmpty()) {
            double max = Config.DATA.ranges.fallbackProximityBlocks;
            for (ServerPlayerEntity v : server.getPlayerManager().getPlayerList()) {
                if (v.getWorld() != speakerPlayer.getWorld()) continue;
                if (v.squaredDistanceTo(speakerPlayer) <= max * max) viewers.add(v.getUuid());
            }
        }

        long now = System.currentTimeMillis();

        for (UUID viewerId : viewers) {
            ServerPlayerEntity viewer = server.getPlayerManager().getPlayer(viewerId);
            if (viewer == null) continue;

            String lang = LangPrefStore.getLang(viewerId, "en");
            String text = translations.get(lang);

            if (text == null) text = translations.getOrDefault("en", null);
            if (text == null || text.isBlank()) text = textSrc;

            var payload = new Net.BubblePayload(speaker, isFinal, now, text, srcLang);
            ServerPlayNetworking.send(viewer, payload);
        }
    }
}
