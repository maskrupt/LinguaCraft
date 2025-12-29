package com.qsmp.translator.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Config {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static Data DATA;

    public static class Data {
        public String backendUrl = "ws://127.0.0.1:8010/ws";
        public String sourceLangDefault = "es";
        public List<String> enabledTargetLangs = List.of("en");

        public Bubble bubble = new Bubble();
        public Ranges ranges = new Ranges();
        public Performance performance = new Performance();
    }

    public static class Bubble {
        public long ttlMsFinal = 6500;
        public long ttlMsPartial = 1200;
        public int maxWidthPx = 180;
        public boolean showOnlyWhenLooking = true;
        public boolean showOriginalBelow = false;
    }

    public static class Ranges {
        public int fallbackProximityBlocks = 24;
        public int whisper = 6, normal = 18, shout = 32;
    }

    public static class Performance {
        public int sendPartialEveryMs = 350;
        public int vadSilenceEndMs = 750;
        public int maxTranslationsPerMessage = 3;
        public int audibleWindowMs = 1300;
    }

    public static void load(Path configDir) {
        try {
            Path file = configDir.resolve("linguacraft_live.json");
            if (!Files.exists(file)) {
                DATA = loadDefault();
                Files.createDirectories(configDir);
                Files.writeString(file, GSON.toJson(DATA));
                return;
            }
            DATA = GSON.fromJson(Files.readString(file), Data.class);
            if (DATA == null) DATA = loadDefault();
        } catch (Exception e) {
            DATA = loadDefault();
        }
    }

    private static Data loadDefault() {
        try (InputStream in = FabricLoader.getInstance().getModContainer("linguacraft_live")
                .orElseThrow()
                .findPath("linguacraft_live.default.json")
                .map(p -> {
                    try { return Files.newInputStream(p); } catch (Exception e) { return null; }
                }).orElse(null)) {
            if (in != null) {
                return GSON.fromJson(new String(in.readAllBytes()), Data.class);
            }
        } catch (Exception ignored) {}
        return new Data();
    }
}
