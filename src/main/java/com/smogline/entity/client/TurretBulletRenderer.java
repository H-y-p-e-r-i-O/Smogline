package com.smogline.entity.client;

import com.smogline.entity.TurretBulletEntity;
import com.smogline.lib.RefStrings;
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
        addRenderLayer(new TurretBulletGlowLayer(this));
    }

    // AbstractArrow сам крутит модель. Нам не нужен preRender с математикой.

    // Слой свечения (тот же самый)
    public static class TurretBulletGlowLayer extends GeoRenderLayer<TurretBulletEntity> {
        public TurretBulletGlowLayer(GeoEntityRenderer<TurretBulletEntity> entityRenderer) {
            super(entityRenderer);
        }

        @Override
        public void render(PoseStack poseStack, TurretBulletEntity entity, BakedGeoModel bakedModel,
                           RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                           float partialTick, int packedLight, int packedOverlay) {

            String ammoId = entity.getAmmoId(); // Берется из SynchedEntityData

            String path = ammoId;
            if (path.contains(":")) path = path.split(":")[1];
            String textureName = "turret_bullet_glow";

            if (path.startsWith("ammo_turret")) {
                String suffix = path.replace("ammo_turret", "");
                if (!suffix.isEmpty()) textureName = "turret_bullet" + suffix + "_glow";
            } else if (path.contains("piercing")) {
                textureName = "turret_bullet_piercing_glow";
            }

            ResourceLocation glowTexture = new ResourceLocation(RefStrings.MODID, "textures/entity/" + textureName + ".png");
            RenderType glowRenderType = RenderType.eyes(glowTexture);

            this.getRenderer().reRender(bakedModel, poseStack, bufferSource, entity,
                    glowRenderType, bufferSource.getBuffer(glowRenderType),
                    partialTick, 15728880, packedOverlay, 1.0f, 1.0f, 1.0f, 1.0f);
        }
    }
}
