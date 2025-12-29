package com.qsmp.translator.server;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RecipientTracker {

    // speaker -> (viewer -> lastHeardTs)
    private static final ConcurrentHashMap<UUID, ConcurrentHashMap<UUID, Long>> AUDIBLE = new ConcurrentHashMap<>();

    public static void markAudible(UUID speaker, UUID viewer) {
        AUDIBLE.computeIfAbsent(speaker, k -> new ConcurrentHashMap<>())
                .put(viewer, System.currentTimeMillis());
    }

    public static Set<UUID> currentAudible(UUID speaker, long withinMs) {
        long now = System.currentTimeMillis();
        var map = AUDIBLE.get(speaker);
        if (map == null) return Set.of();
        return map.entrySet().stream()
                .filter(e -> now - e.getValue() <= withinMs)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public static void prune(long olderThanMs) {
        long now = System.currentTimeMillis();
        for (var entry : AUDIBLE.entrySet()) {
            entry.getValue().entrySet().removeIf(e -> now - e.getValue() > olderThanMs);
        }
    }
}
