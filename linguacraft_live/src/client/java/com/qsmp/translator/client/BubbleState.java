package com.qsmp.translator.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BubbleState {

    public static final Map<UUID, Bubble> bubbles = new ConcurrentHashMap<>();

    public static void put(UUID speaker, String text, boolean isFinal, long tsMs) {
        bubbles.put(speaker, new Bubble(text, isFinal, tsMs));
    }

    public record Bubble(String text, boolean isFinal, long tsMs) {}
}
