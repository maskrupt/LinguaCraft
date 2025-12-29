package com.qsmp.translator.vc;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BiConsumer;

public class VadSegmenter {

    private final int sendPartialEveryMs;
    private final int vadSilenceEndMs;

    private boolean speaking = false;
    private int silenceMs = 0;
    private long lastPartialSentMs = 0;

    private final Deque<short[]> buffer = new ArrayDeque<>();

    // Heuristic constants
    private static final int SAMPLE_RATE = 16000;
    private static final double ENERGY_THRESHOLD = 800.0;

    public VadSegmenter(int sendPartialEveryMs, int vadSilenceEndMs) {
        this.sendPartialEveryMs = Math.max(150, sendPartialEveryMs);
        this.vadSilenceEndMs = Math.max(350, vadSilenceEndMs);
    }

    public void pushPcm16k(short[] pcm, BiConsumer<short[], Boolean> onChunk) {
        if (pcm == null || pcm.length == 0) return;

        double rms = rms(pcm);
        int frameMs = (int) Math.max(10, (pcm.length / (double) SAMPLE_RATE) * 1000.0);

        if (rms > ENERGY_THRESHOLD) {
            speaking = true;
            silenceMs = 0;
            buffer.add(pcm);

            long now = System.currentTimeMillis();
            if (now - lastPartialSentMs >= sendPartialEveryMs) {
                lastPartialSentMs = now;
                onChunk.accept(flatten(false), false);
            }
            return;
        }

        if (!speaking) return;

        // In speaking mode but low energy: count silence and keep buffer for final
        silenceMs += frameMs;
        buffer.add(pcm);

        if (silenceMs >= vadSilenceEndMs) {
            speaking = false;
            silenceMs = 0;
            lastPartialSentMs = 0;
            onChunk.accept(flatten(true), true);
            buffer.clear();
        }
    }

    private short[] flatten(boolean finalChunk) {
        int total = buffer.stream().mapToInt(a -> a.length).sum();
        short[] out = new short[total];
        int idx = 0;
        for (short[] a : buffer) {
            System.arraycopy(a, 0, out, idx, a.length);
            idx += a.length;
        }

        // For partial chunks, keep a tiny tail (continuity) instead of clearing fully
        if (!finalChunk && buffer.size() > 2) {
            short[] tail = buffer.peekLast();
            buffer.clear();
            if (tail != null) buffer.add(tail);
        }
        return out;
    }

    private static double rms(short[] pcm) {
        double sum = 0;
        for (short s : pcm) sum += (double) s * (double) s;
        return Math.sqrt(sum / Math.max(1, pcm.length));
    }
}
