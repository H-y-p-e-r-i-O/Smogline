package com.smogline.entity.client.debug;

import com.smogline.entity.weapons.turrets.TurretLightEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class TurretDebugRenderer_Enhanced {

    public static void renderTurretDebug(PoseStack poseStack, MultiBufferSource bufferSource,
                                         TurretLightEntity turret, double camX, double camY, double camZ) {
        if (turret == null || !turret.isDeployed()) return;

        poseStack.pushPose();
        poseStack.translate(-camX, -camY, -camZ);

        Vec3 muzzlePos = turret.getMuzzlePos();

        // 1) Сетка сканирования
        if (turret.getDebugScanPoints() != null) {
            for (Pair<Vec3, Boolean> pointData : turret.getDebugScanPoints()) {
                Vec3 pos = pointData.getFirst();
                boolean visible = pointData.getSecond();
                int r = visible ? 0 : 255;
                int g = visible ? 255 : 0;
                renderMiniDot(poseStack, bufferSource, pos, r, g, 0);
            }
        }

        // 2) Хитбокс цели
        if (turret.getTarget() != null) {
            renderHitbox(poseStack, bufferSource, turret.getTarget().getBoundingBox(), 255, 255, 255, 255);
        }

        // 3) Точка упреждения (Lead Point)
        Vec3 leadPoint = turret.getDebugTargetPoint();
        if (leadPoint != null) {
            // Рисуем точку (Magenta / Фиолетовый)
            renderHitPoint(poseStack, bufferSource, leadPoint, 255, 0, 255);

            // Рисуем линию (Magenta)
            drawLine(poseStack, bufferSource, muzzlePos, leadPoint, 255, 0, 255, 1.0F);
        }

        // 4) Баллистическая траектория
        Vec3 v0 = turret.getDebugBallisticVelocity();
        if (v0 != null) {
            renderSimulatedTrajectory(poseStack, bufferSource, muzzlePos, v0,
                    0.01F, 0.99F, 80);
        } else {
            Vec3 forward = Vec3.directionFromRotation(turret.getXRot(), turret.getYHeadRot()).scale(3.0);
            drawLine(poseStack, bufferSource, muzzlePos, muzzlePos.add(forward), 255, 0, 255, 0.3F);
        }

        poseStack.popPose();
    }

    private static void renderSimulatedTrajectory(PoseStack ps, MultiBufferSource buf,
                                                  Vec3 startPos, Vec3 startVel,
                                                  float gravityPerTick, float drag, int maxTicks) {
        Vec3 pos = startPos;
        Vec3 vel = startVel;
        for (int i = 0; i < maxTicks; i++) {
            Vec3 nextPos = pos.add(vel);
            drawLine(ps, buf, pos, nextPos, 0, 255, 255, 1.0F);
            pos = nextPos;
            vel = vel.scale(drag).add(0.0, -gravityPerTick, 0.0);
        }
    }

    // --- Helpers (Старая версия с pushPose в каждом вызове) ---

    private static void renderMiniDot(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 pos, int r, int g, int b) {
        VertexConsumer vc = bufferSource.getBuffer(RenderType.lines());
        double s = 0.05;
        drawBoxWireframe(vc, poseStack, pos.x - s, pos.y - s, pos.z - s, pos.x + s, pos.y + s, pos.z + s, r, g, b, 255);
    }

    private static void renderHitPoint(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 pos, int r, int g, int b) {
        VertexConsumer vc = bufferSource.getBuffer(RenderType.lines());
        double s = 0.15;
        drawBoxWireframe(vc, poseStack, pos.x - s, pos.y - s, pos.z - s, pos.x + s, pos.y + s, pos.z + s, r, g, b, 255);
        drawLine(poseStack, bufferSource, pos.subtract(s, s, s), pos.add(s, s, s), r, g, b, 1.0F);
        drawLine(poseStack, bufferSource, pos.subtract(s, -s, s), pos.add(s, -s, s), r, g, b, 1.0F);
    }

    private static void renderHitbox(PoseStack poseStack, MultiBufferSource bufferSource, AABB box, int r, int g, int b, int a) {
        VertexConsumer vc = bufferSource.getBuffer(RenderType.lines());
        drawBoxWireframe(vc, poseStack, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, r, g, b, a);
    }

    private static void drawLine(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 start, Vec3 end,
                                 int r, int g, int b, float alpha) {
        VertexConsumer vc = bufferSource.getBuffer(RenderType.lines());
        poseStack.pushPose(); // ВОТ ОНО - то, что, вероятно, фиксило координаты

        float dx = (float) (end.x - start.x);
        float dy = (float) (end.y - start.y);
        float dz = (float) (end.z - start.z);
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len > 0) { dx /= len; dy /= len; dz /= len; } else { dx = 0; dy = 1; dz = 0; }

        vc.vertex(poseStack.last().pose(), (float) start.x, (float) start.y, (float) start.z)
                .color(r, g, b, (int) (alpha * 255)).normal(poseStack.last().normal(), dx, dy, dz).endVertex();
        vc.vertex(poseStack.last().pose(), (float) end.x, (float) end.y, (float) end.z)
                .color(r, g, b, (int) (alpha * 255)).normal(poseStack.last().normal(), dx, dy, dz).endVertex();

        poseStack.popPose();
    }

    private static void drawBoxWireframe(VertexConsumer vc, PoseStack ps,
                                         double x1, double y1, double z1, double x2, double y2, double z2,
                                         int r, int g, int b, int a) {
        drawLineV(vc, ps, x1, y1, z1, x2, y1, z1, r, g, b, a);
        drawLineV(vc, ps, x2, y1, z1, x2, y1, z2, r, g, b, a);
        drawLineV(vc, ps, x2, y1, z2, x1, y1, z2, r, g, b, a);
        drawLineV(vc, ps, x1, y1, z2, x1, y1, z1, r, g, b, a);
        drawLineV(vc, ps, x1, y2, z1, x2, y2, z1, r, g, b, a);
        drawLineV(vc, ps, x2, y2, z1, x2, y2, z2, r, g, b, a);
        drawLineV(vc, ps, x2, y2, z2, x1, y2, z2, r, g, b, a);
        drawLineV(vc, ps, x1, y2, z2, x1, y2, z1, r, g, b, a);
        drawLineV(vc, ps, x1, y1, z1, x1, y2, z1, r, g, b, a);
        drawLineV(vc, ps, x2, y1, z1, x2, y2, z1, r, g, b, a);
        drawLineV(vc, ps, x2, y1, z2, x2, y2, z2, r, g, b, a);
        drawLineV(vc, ps, x1, y1, z2, x1, y2, z2, r, g, b, a);
    }

    private static void drawLineV(VertexConsumer vc, PoseStack ps,
                                  double x1, double y1, double z1, double x2, double y2, double z2,
                                  int r, int g, int b, int a) {
        float dx = (float) (x2 - x1);
        float dy = (float) (y2 - y1);
        float dz = (float) (z2 - z1);
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len > 0) { dx /= len; dy /= len; dz /= len; }

        // Тут тоже использовался ps.last().pose(), это ок, так как pushPose вызывается выше в стеке
        vc.vertex(ps.last().pose(), (float) x1, (float) y1, (float) z1)
                .color(r, g, b, a).normal(ps.last().normal(), dx, dy, dz).endVertex();
        vc.vertex(ps.last().pose(), (float) x2, (float) y2, (float) z2)
                .color(r, g, b, a).normal(ps.last().normal(), dx, dy, dz).endVertex();
    }
}
