package com.smogline.client;

import com.smogline.entity.weapons.turrets.TurretLightEntity;
import com.smogline.entity.client.debug.TurretDebugRenderer_Enhanced;
import com.smogline.entity.weapons.turrets.TurretLightLinkedEntity;
import com.smogline.main.MainRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class TurretDebugRenderEvent {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // Проверяем, включен ли дебаг
        if (!TurretDebugKeyHandler.debugVisualizationEnabled) {
            return;
        }

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;

        // Получаем камеру рендера (это точка ГЛАЗ, а не ног)
        if (level == null || mc.gameRenderer.getMainCamera() == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        // >>> ФИКС: Используем getPosition() камеры, а не camera.getEntity().getX()
        // getX()/getY() у entity возвращает НОГИ. Рендер смещен на высоту глаз (~1.62 блока).
        // getPosition() возвращает точную позицию КАМЕРЫ с учетом интерполяции.
        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
        double camX = camPos.x;
        double camY = camPos.y;
        double camZ = camPos.z;

        // Ищем турели вокруг камеры
        Vec3 renderCenter = new Vec3(camX, camY, camZ);
        for (Entity entity : level.getEntities(null, new net.minecraft.world.phys.AABB(
                renderCenter.x - 64, renderCenter.y - 64, renderCenter.z - 64,
                renderCenter.x + 64, renderCenter.y + 64, renderCenter.z + 64))) {


                if (entity instanceof TurretLightEntity turret && !turret.isRemoved()) {
                    TurretDebugRenderer_Enhanced.renderTurretDebug(poseStack, bufferSource, turret, camX, camY, camZ);
                } else if (entity instanceof TurretLightLinkedEntity linked && !linked.isRemoved()) {
                    TurretDebugRenderer_Enhanced.renderTurretDebug(poseStack, bufferSource, linked, camX, camY, camZ);
                }


        }

        // Принудительно отрисовываем буфер линий, чтобы они были видны сквозь блоки (если нужно)
        // или просто завершаем батч
        bufferSource.endBatch(net.minecraft.client.renderer.RenderType.lines());
    }
}
