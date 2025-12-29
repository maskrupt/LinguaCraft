package com.qsmp.translator.net;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.UUID;

public class Net {

    public static final Identifier ID_BUBBLE = Identifier.of("linguacraft_live", "bubble");
    public static final Identifier ID_LANG_PREF = Identifier.of("linguacraft_live", "lang_pref");

    public record BubblePayload(UUID speaker, boolean isFinal, long tsMs, String text, String srcLang) implements CustomPayload {
        public static final CustomPayload.Id<BubblePayload> ID = new CustomPayload.Id<>(ID_BUBBLE);

        public static final PacketCodec<net.minecraft.network.PacketByteBuf, BubblePayload> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.UUID, BubblePayload::speaker,
                        PacketCodecs.BOOL, BubblePayload::isFinal,
                        PacketCodecs.VAR_LONG, BubblePayload::tsMs,
                        PacketCodecs.STRING, BubblePayload::text,
                        PacketCodecs.STRING, BubblePayload::srcLang,
                        BubblePayload::new
                );

        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record LangPrefPayload(String lang) implements CustomPayload {
        public static final CustomPayload.Id<LangPrefPayload> ID = new CustomPayload.Id<>(ID_LANG_PREF);

        public static final PacketCodec<net.minecraft.network.PacketByteBuf, LangPrefPayload> CODEC =
                PacketCodec.tuple(PacketCodecs.STRING, LangPrefPayload::lang, LangPrefPayload::new);

        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public static void registerPayloads() {
        PayloadTypeRegistry.playS2C().register(BubblePayload.ID, BubblePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(LangPrefPayload.ID, LangPrefPayload.CODEC);
    }
}
