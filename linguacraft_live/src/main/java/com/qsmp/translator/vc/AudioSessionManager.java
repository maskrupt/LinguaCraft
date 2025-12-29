package com.qsmp.translator.vc;

import com.qsmp.translator.backend.BackendClient;
import com.qsmp.translator.config.Config;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AudioSessionManager {

    private static final Map<UUID, VadSegmenter> sessions = new ConcurrentHashMap<>();

    public static void init() {}

    public static void onPcm48k(UUID speaker, short[] pcm48k) {
        // Downsample 48k -> 16k for STT bandwidth (simple decimation)
        short[] pcm16k = downsample48to16(pcm48k);
        VadSegmenter seg = sessions.computeIfAbsent(speaker, k -> new VadSegmenter(
                Config.DATA.performance.sendPartialEveryMs,
                Config.DATA.performance.vadSilenceEndMs
        ));

        seg.pushPcm16k(pcm16k, (chunk, isFinal) -> BackendClient.sendAudioChunk(speaker, chunk, 16000, isFinal));
    }

    private static short[] downsample48to16(short[] pcm48k) {
        int n = pcm48k.length / 3;
        if (n <= 0) return new short[0];
        short[] out = new short[n];
        for (int i = 0, j = 0; j < n; j++, i += 3) out[j] = pcm48k[i];
        return out;
    }
}
