package com.hbm_m.entity.client;

import com.hbm_m.entity.TurretBulletEntity;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class TurretBulletRenderer extends GeoEntityRenderer<TurretBulletEntity> {
    public TurretBulletRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new TurretBulletModel());

        // ЯВНОЕ указание текстуры свечения
        addRenderLayer(new AutoGlowingGeoLayer<>(this) {
            @Override
            protected RenderType getRenderType(TurretBulletEntity animatable) {
                // Указываем путь к glow-маске вручную
                return RenderType.eyes(new ResourceLocation(MainRegistry.MOD_ID, "textures/entity/turret_bullet_glow.png"));
            }
        });
    }

    @Override
    public void render(TurretBulletEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        // Коррекция модели: если пуля летит "боком", этот поворот исправит её
        // Если летит задом наперед - поменяй на 180, если левым боком - на -90

        poseStack.mulPose(Axis.YP.rotationDegrees(90f));
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
