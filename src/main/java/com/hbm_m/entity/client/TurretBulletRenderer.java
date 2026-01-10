package com.hbm_m.entity.client;

import com.hbm_m.entity.TurretBulletEntity;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class TurretBulletRenderer extends GeoEntityRenderer<TurretBulletEntity> {

    public TurretBulletRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new TurretBulletModel());

        // Регистрируем слой свечения
        addRenderLayer(new TurretBulletGlowLayer(this));
    }

    // Внутренний класс для слоя свечения
    public static class TurretBulletGlowLayer extends GeoRenderLayer<TurretBulletEntity> {

        public TurretBulletGlowLayer(GeoEntityRenderer<TurretBulletEntity> entityRendererIn) {
            super(entityRendererIn);
        }

        @Override
        public void render(PoseStack poseStack, TurretBulletEntity animatable, BakedGeoModel bakedModel,
                           RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                           float partialTick, int packedLight, int packedOverlay) {

            String ammoId = animatable.getAmmoId();
            ResourceLocation glowTexture;

            // Выбираем текстуру свечения так же, как и основную
            if (ammoId.contains("piercing")) {
                glowTexture = new ResourceLocation(RefStrings.MODID, "textures/entity/turret_bullet_piercing_glow.png");
            } else {
                // Если для обычной пули нет свечения, используем дефолтную
                glowTexture = new ResourceLocation(RefStrings.MODID, "textures/entity/turret_bullet_glow.png");
            }

            // Рендерим слой, который светится в темноте (Eyes = полный свет, игнорирует освещение мира)
            RenderType glowRenderType = RenderType.eyes(glowTexture);

            // Используем bakedModel, который передан в аргументы метода
            getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable,
                    glowRenderType, bufferSource.getBuffer(glowRenderType),
                    partialTick, 15728880, packedOverlay,
                    1.0f, 1.0f, 1.0f, 1.0f);
        }
    }
}
