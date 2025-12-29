package com.qsmp.translator.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ClientPrefs {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class Data {
        public String preferredLang = "en";
    }

    public static Data DATA;

    public static void load() {
        try {
            Path dir = FabricLoader.getInstance().getConfigDir();
            Path file = dir.resolve("linguacraft_live_client.json");
            if (!Files.exists(file)) {
                DATA = loadDefault();
                Files.writeString(file, GSON.toJson(DATA));
                return;
            }
            DATA = GSON.fromJson(Files.readString(file), Data.class);
            if (DATA == null) DATA = new Data();
        } catch (Exception e) {
            DATA = new Data();
        }
    }

    public static void save() {
        try {
            Path dir = FabricLoader.getInstance().getConfigDir();
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("linguacraft_live_client.json"), GSON.toJson(DATA));
        } catch (Exception ignored) {}
    }

    private static Data loadDefault() {
        try (InputStream in = FabricLoader.getInstance().getModContainer("linguacraft_live")
                .orElseThrow()
                .findPath("linguacraft_live_client.default.json")
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
