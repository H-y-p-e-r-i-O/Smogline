package com.hbm_m.entity.client.renderer;

import com.hbm_m.entity.TurretLightEntity;
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

        // Переходим в систему координат камеры
        poseStack.translate(-camX, -camY, -camZ);

        Vec3 muzzlePos = turret.getMuzzlePos();

        if (turret.getDebugScanPoints() != null) {
            for (Pair<Vec3, Boolean> pointData : turret.getDebugScanPoints()) {
                Vec3 pos = pointData.getFirst();
                boolean visible = pointData.getSecond();
                int r = visible ? 0 : 255;
                int g = visible ? 255 : 0;
                renderMiniDot(poseStack, bufferSource, pos, r, g, 0);
            }
        }

        if (turret.getTarget() != null) {
            renderHitbox(poseStack, bufferSource, turret.getTarget().getBoundingBox(), 255, 255, 255, 255);
        }

        Vec3 targetPoint = turret.getDebugTargetPoint();
        if (targetPoint != null) {
            renderHitPoint(poseStack, bufferSource, targetPoint, 255, 0, 255);
            renderBallisticArc(poseStack, bufferSource, muzzlePos, targetPoint, 3.0F, 0.05F);
        } else {
            Vec3 forward = Vec3.directionFromRotation(turret.getXRot(), turret.getYHeadRot()).scale(3.0);
            drawLine(poseStack, bufferSource, muzzlePos, muzzlePos.add(forward), 255, 0, 255, 0.3F);
        }

        poseStack.popPose();
    }

    private static void renderBallisticArc(PoseStack ps, MultiBufferSource buf, Vec3 start, Vec3 end, float speed, float gravity) {
        int segments = 20;
        Vec3 prev = start;
        Vec3 totalDelta = end.subtract(start);
        double dist = totalDelta.length();
        double humpHeight = dist * 0.12;

        for (int i = 1; i <= segments; i++) {
            float t = (float)i / segments;
            Vec3 current = start.add(totalDelta.scale(t));
            current = current.add(0, humpHeight * 4 * t * (1-t), 0);
            drawLine(ps, buf, prev, current, 0, 255, 255, 1.0F);
            prev = current;
        }
    }

    private static void renderMiniDot(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 pos, int r, int g, int b) {
        VertexConsumer vc = bufferSource.getBuffer(RenderType.lines());
        double s = 0.05;
        drawBoxWireframe(vc, poseStack, pos.x-s, pos.y-s, pos.z-s, pos.x+s, pos.y+s, pos.z+s, r, g, b, 255);
    }

    private static void renderHitPoint(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 pos, int r, int g, int b) {
        VertexConsumer vc = bufferSource.getBuffer(RenderType.lines());
        double s = 0.15;
        drawBoxWireframe(vc, poseStack, pos.x-s, pos.y-s, pos.z-s, pos.x+s, pos.y+s, pos.z+s, r, g, b, 255);
        drawLine(poseStack, bufferSource, pos.subtract(s,s,s), pos.add(s,s,s), r, g, b, 1.0F);
        drawLine(poseStack, bufferSource, pos.subtract(s,-s,s), pos.add(s,-s,s), r, g, b, 1.0F);
    }

    private static void renderHitbox(PoseStack poseStack, MultiBufferSource bufferSource, AABB box, int r, int g, int b, int a) {
        VertexConsumer vc = bufferSource.getBuffer(RenderType.lines());
        drawBoxWireframe(vc, poseStack, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, r, g, b, a);
    }

    private static void drawLine(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 start, Vec3 end, int r, int g, int b, float alpha) {
        VertexConsumer vc = bufferSource.getBuffer(RenderType.lines());
        poseStack.pushPose();
        float dx = (float)(end.x - start.x);
        float dy = (float)(end.y - start.y);
        float dz = (float)(end.z - start.z);
        float len = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len > 0) { dx /= len; dy /= len; dz /= len; } else { dx=0; dy=1; dz=0; }
        vc.vertex(poseStack.last().pose(), (float)start.x, (float)start.y, (float)start.z)
                .color(r, g, b, (int)(alpha*255)).normal(poseStack.last().normal(), dx, dy, dz).endVertex();
        vc.vertex(poseStack.last().pose(), (float)end.x, (float)end.y, (float)end.z)
                .color(r, g, b, (int)(alpha*255)).normal(poseStack.last().normal(), dx, dy, dz).endVertex();
        poseStack.popPose();
    }

    private static void drawBoxWireframe(VertexConsumer vc, PoseStack ps, double x1, double y1, double z1, double x2, double y2, double z2, int r, int g, int b, int a) {
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

    private static void drawLineV(VertexConsumer vc, PoseStack ps, double x1, double y1, double z1, double x2, double y2, double z2, int r, int g, int b, int a) {
        float dx = (float)(x2 - x1);
        float dy = (float)(y2 - y1);
        float dz = (float)(z2 - z1);
        float len = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len > 0) { dx /= len; dy /= len; dz /= len; }
        vc.vertex(ps.last().pose(), (float)x1, (float)y1, (float)z1)
                .color(r, g, b, a).normal(ps.last().normal(), dx, dy, dz).endVertex();
        vc.vertex(ps.last().pose(), (float)x2, (float)y2, (float)z2)
                .color(r, g, b, a).normal(ps.last().normal(), dx, dy, dz).endVertex();
    }
}
