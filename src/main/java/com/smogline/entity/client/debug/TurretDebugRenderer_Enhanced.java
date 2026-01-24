package com.smogline.entity.client.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import com.smogline.entity.weapons.turrets.TurretLightEntity;
import com.smogline.entity.weapons.turrets.TurretLightLinkedEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import java.util.List;


public class TurretDebugRenderer_Enhanced {

    // Публичные методы для вызова из ивента
    public static void renderTurretDebug(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                         TurretLightEntity turret, double camX, double camY, double camZ) {
        renderDebugInfo(poseStack, turret.getDebugTargetPoint(),
                turret.getDebugBallisticVelocity(),
                turret.getDebugScanPoints(),
                turret.getMuzzlePos(), // Используем дуло как начало
                turret.getPosition(0));
    }

    public static void renderTurretDebug(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                         TurretLightLinkedEntity turret, double camX, double camY, double camZ) {
        renderDebugInfo(poseStack, turret.getDebugTargetPoint(),
                turret.getDebugBallisticVelocity(),
                turret.getDebugScanPoints(),
                turret.getMuzzlePos(),
                turret.getPosition(0));
    }

    private static void renderDebugInfo(PoseStack poseStack, Vec3 targetPoint, Vec3 velocity,
                                        List<Pair<Vec3, Boolean>> scanPoints, Vec3 muzzlePos, Vec3 turretPos) {
        VertexConsumer lineBuilder = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(RenderType.lines());

        poseStack.pushPose();

        // Смещение камеры
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f matrix = poseStack.last().pose();

        // 1. СЕТКА (Grid Scan) - Красные/Зеленые точки
        if (scanPoints != null && !scanPoints.isEmpty()) {
            for (Pair<Vec3, Boolean> point : scanPoints) {
                Vec3 p = point.getFirst();
                boolean isHit = point.getSecond();
                float r = isHit ? 0.0F : 1.0F;
                float g = isHit ? 1.0F : 0.0F;
                LevelRenderer.renderLineBox(poseStack, lineBuilder,
                        p.x - 0.05, p.y - 0.05, p.z - 0.05,
                        p.x + 0.05, p.y + 0.05, p.z + 0.05,
                        r, g, 0.0F, 0.8F);
            }
        }

        if (targetPoint != null) {
            // 2. ЦЕНТРОИД (Фиолетовый бокс цели)
            float boxSize = 0.25F;
            LevelRenderer.renderLineBox(poseStack, lineBuilder,
                    targetPoint.x - boxSize, targetPoint.y - boxSize, targetPoint.z - boxSize,
                    targetPoint.x + boxSize, targetPoint.y + boxSize, targetPoint.z + boxSize,
                    0.6F, 0.0F, 1.0F, 1.0F); // Фиолетовый

            // 3. ЛИНИЯ НАВОДКИ (Фиолетовая прямая)
            lineBuilder.vertex(matrix, (float)muzzlePos.x, (float)muzzlePos.y, (float)muzzlePos.z)
                    .color(0.6F, 0.0F, 1.0F, 0.8F)
                    .normal(0, 1, 0)
                    .endVertex();
            lineBuilder.vertex(matrix, (float)targetPoint.x, (float)targetPoint.y, (float)targetPoint.z)
                    .color(0.6F, 0.0F, 1.0F, 0.8F)
                    .normal(0, 1, 0)
                    .endVertex();

            // 4. ТРАЕКТОРИЯ (Бирюзовая парабола)
            if (velocity != null) {
                Vec3 currentPos = muzzlePos;
                Vec3 currentVel = velocity;
                double gravity = 0.05; // Гравитация (подстрой под свои пули)
                double drag = 0.99;    // Сопротивление воздуха

                for (int i = 0; i < 50; i++) { // Рисуем 50 шагов вперед
                    Vec3 nextPos = currentPos.add(currentVel);

                    lineBuilder.vertex(matrix, (float)currentPos.x, (float)currentPos.y, (float)currentPos.z)
                            .color(0.0F, 1.0F, 1.0F, 1.0F) // Бирюзовый
                            .normal(0, 1, 0)
                            .endVertex();
                    lineBuilder.vertex(matrix, (float)nextPos.x, (float)nextPos.y, (float)nextPos.z)
                            .color(0.0F, 1.0F, 1.0F, 1.0F)
                            .normal(0, 1, 0)
                            .endVertex();

                    currentPos = nextPos;
                    currentVel = currentVel.scale(drag).subtract(0, gravity, 0);

                    // Если ушли под землю - стоп (опционально)
                    if (currentPos.y < targetPoint.y - 5) break;
                }
            }
        }

        poseStack.popPose();
    }
}
