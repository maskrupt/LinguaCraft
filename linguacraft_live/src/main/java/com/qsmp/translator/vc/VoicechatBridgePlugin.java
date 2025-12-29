package com.qsmp.translator.vc;

import com.qsmp.translator.server.RecipientTracker;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.*;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import de.maxhenkel.voicechat.api.events.EventRegistration;

import java.lang.reflect.Method;
import java.util.UUID;

public class VoicechatBridgePlugin implements VoicechatPlugin {

    private VoicechatApi api;
    private OpusDecoder decoder;

    @Override
    public String getPluginId() {
        return "linguacraft_live";
    }

    @Override
    public void initialize(VoicechatApi api) {
        this.api = api;
        this.decoder = api.createDecoder();
        AudioSessionManager.init();
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        // mic packets arriving at server
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicPacket, 0);

        // Track who can actually hear whom (SVC decides this), using sender/receiver from PacketEvent
        registration.registerEvent(EntitySoundPacketEvent.class, this::onSoundPacket, 0);
        registration.registerEvent(LocationalSoundPacketEvent.class, this::onSoundPacket, 0);
        registration.registerEvent(StaticSoundPacketEvent.class, this::onSoundPacket, 0);
    }

    private void onSoundPacket(PacketEvent<?> event) {
        var sender = event.getSenderConnection();
        var receiver = event.getReceiverConnection();
        if (sender == null || receiver == null) return;

        UUID speaker = sender.getPlayerUuid();
        UUID viewer = receiver.getPlayerUuid();
        if (speaker == null || viewer == null) return;

        RecipientTracker.markAudible(speaker, viewer);
    }

    private void onMicPacket(MicrophonePacketEvent event) {
        if (decoder == null) return;

        var senderConn = event.getSenderConnection();
        if (senderConn == null) return;

        UUID speaker = senderConn.getPlayerUuid();
        if (speaker == null) return;

        MicrophonePacket packet = event.getPacket();
        byte[] opus = extractOpusBytes(packet);
        if (opus == null || opus.length == 0) return;

        short[] pcm48k;
        try {
            pcm48k = decoder.decode(opus);
        } catch (Throwable t) {
            return;
        }
        if (pcm48k == null || pcm48k.length == 0) return;

        AudioSessionManager.onPcm48k(speaker, pcm48k);
    }

    /**
     * SVC API minor versions rename getters. We support a few common ones via reflection.
     */
    private static byte[] extractOpusBytes(MicrophonePacket packet) {
        String[] candidates = new String[] {
                "getOpusEncodedData",
                "getOpusData",
                "getOpus",
                "getData"
        };
        for (String name : candidates) {
            try {
                Method m = packet.getClass().getMethod(name);
                Object o = m.invoke(packet);
                if (o instanceof byte[] b) return b;
            } catch (Exception ignored) {}
        }
        return null;
    }
}
