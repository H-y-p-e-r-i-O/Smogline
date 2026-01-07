package com.hbm_m.entity.client;

import com.hbm_m.entity.TurretBulletEntity;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class TurretBulletRenderer extends GeoEntityRenderer<TurretBulletEntity> {

    // Если модель в Blockbench/Gecko смотрит не вдоль +Z, а вдоль +X, нужен оффсет 90/-90.
    // Сейчас ставим 0, чтобы не было постоянного "боком"; при необходимости просто меняй на 90F или -90F.
    private static final float MODEL_YAW_OFFSET = 0.0F;

    public TurretBulletRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new TurretBulletModel());

        addRenderLayer(new AutoGlowingGeoLayer<>(this) {
            @Override
            protected RenderType getRenderType(TurretBulletEntity animatable) {
                return RenderType.eyes(new ResourceLocation(MainRegistry.MOD_ID, "textures/entity/turret_bullet_glow.png"));
            }
        });
    }

    @Override
    public void render(TurretBulletEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        // Рендерим по ротации сущности (Yaw+Pitch), а не по фиксированному 90°.
        float yaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) + MODEL_YAW_OFFSET;
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());

        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        // Важно: передаем 0, чтобы GeoEntityRenderer не добавил свой yaw второй раз.
        super.render(entity, 0.0F, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}