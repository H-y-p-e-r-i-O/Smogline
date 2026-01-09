package com.hbm_m.entity.client;

import com.hbm_m.entity.TurretBulletEntity;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
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
    public void preRender(PoseStack poseStack, TurretBulletEntity animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);

        // 1. Поворот на 180 (который мы уже сделали)
        poseStack.mulPose(Axis.ZP.rotationDegrees(180f));

        // 2. Увеличение в 2 раза по всем осям (X, Y, Z)
        poseStack.scale(1.20f, 1.20f, 1.20f);
    }

    @Override
    public void render(TurretBulletEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        // 1. Интерполяция: (текущее - старое) * частичный_тик + старое
        // Это делает движение идеально плавным
        float lerpedYaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        float lerpedPitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());

        // 2. Вращение PoseStack
        // Порядок важен: сначала Y (рысканье), потом X (тангаж)
        // +180 к Yaw часто нужно для projectile-моделей, если они смотрят "на игрока" в Blockbench
        poseStack.mulPose(Axis.YP.rotationDegrees(lerpedYaw - 180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(lerpedPitch));

        // 3. Рендер модели GeckoLib
        // Передаем 0 вместо entityYaw, так как мы уже повернули стэк вручную!
        super.render(entity, 0.0F, partialTick, poseStack, bufferSource, packedLight);

        poseStack.popPose();
    }
}