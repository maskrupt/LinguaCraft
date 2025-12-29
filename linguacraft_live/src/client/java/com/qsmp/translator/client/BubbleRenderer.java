package com.qsmp.translator.client;

import com.qsmp.translator.config.Config;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.ArrayList;
import java.util.List;

public class BubbleRenderer {

    public static void render(MatrixStack matrices) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        HitResult hr = mc.crosshairTarget;
        if (Config.DATA.bubble.showOnlyWhenLooking) {
            if (!(hr instanceof EntityHitResult ehr)) return;
            Entity target = ehr.getEntity();
            if (!(target instanceof PlayerEntity)) return;
            drawForTarget(mc, matrices, (PlayerEntity) target);
        } else {
            // Optional: render for all nearby speakers (not requested)
        }
    }

    private static void drawForTarget(MinecraftClient mc, MatrixStack matrices, PlayerEntity target) {
        var bubble = BubbleState.bubbles.get(target.getUuid());
        if (bubble == null) return;

        long age = System.currentTimeMillis() - bubble.tsMs();
        long ttl = bubble.isFinal() ? Config.DATA.bubble.ttlMsFinal : Config.DATA.bubble.ttlMsPartial;
        if (age > ttl) return;

        String text = bubble.text();
        if (!bubble.isFinal()) text = text + " …";

        drawBillboardWrapped(mc, matrices, target, text, Config.DATA.bubble.maxWidthPx);
    }

    private static void drawBillboardWrapped(MinecraftClient mc, MatrixStack matrices, Entity target, String text, int maxWidthPx) {
        double x = target.getX();
        double y = target.getY() + target.getHeight() + 0.45;
        double z = target.getZ();

        var cam = mc.gameRenderer.getCamera();
        double dx = x - cam.getPos().x;
        double dy = y - cam.getPos().y;
        double dz = z - cam.getPos().z;

        matrices.push();
        matrices.translate(dx, dy, dz);
        matrices.multiply(mc.getEntityRenderDispatcher().getRotation());
        matrices.scale(-0.025f, -0.025f, 0.025f);

        TextRenderer tr = mc.textRenderer;

        List<String> lines = wrap(tr, text, maxWidthPx);
        int lineHeight = tr.fontHeight + 2;
        int totalH = lines.size() * lineHeight;

        int bg = 0x55000000;
        int fg = 0xFFFFFFFF;

        VertexConsumerProvider.Immediate vcp = mc.getBufferBuilders().getEntityVertexConsumers();

        int yOff = -totalH;
        for (String line : lines) {
            int w = tr.getWidth(line);
            float xOff = -w / 2f;
            tr.draw(
                    Text.of(line),
                    xOff,
                    yOff,
                    fg,
                    false,
                    matrices.peek().getPositionMatrix(),
                    vcp,
                    TextRenderer.TextLayerType.SEE_THROUGH,
                    bg,
                    0x00F000F0
            );
            yOff += lineHeight;
        }
        vcp.draw();
        matrices.pop();
    }

    private static List<String> wrap(TextRenderer tr, String text, int maxWidthPx) {
        List<String> out = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();

        for (String w : words) {
            String candidate = line.isEmpty() ? w : line + " " + w;
            if (tr.getWidth(candidate) <= maxWidthPx) {
                line.setLength(0);
                line.append(candidate);
            } else {
                if (!line.isEmpty()) out.add(line.toString());
                line.setLength(0);
                line.append(w);
            }
        }
        if (!line.isEmpty()) out.add(line.toString());
        return out;
    }
}
