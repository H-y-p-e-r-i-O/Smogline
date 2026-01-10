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

            String ammoId = animatable.getAmmoId(); // Например: "hbm_m:ammo_turret_fire"

            // Логика формирования пути к текстуре свечения
            // 1. Убираем префикс мода
            String path = ammoId;
            if (path.contains(":")) {
                path = path.split(":")[1];
            }

            String itemPrefix = "ammo_turret";
            String bulletPrefix = "turret_bullet";
            String textureName = "turret_bullet_glow"; // Дефолтная текстура

            // Если это патрон из нашего семейства
            if (path.startsWith(itemPrefix)) {
                // Получаем суффикс (например "_fire")
                String suffix = path.replace(itemPrefix, "");

                // Если суффикс пустой (обычный патрон) -> turret_bullet_glow.png
                // Если суффикс есть (_fire) -> turret_bullet_fire_glow.png
                if (!suffix.isEmpty()) {
                    textureName = bulletPrefix + suffix + "_glow";
                }
            } else if (path.contains("piercing")) {
                // Фолбэк для старых ID или если логика префиксов не сработает
                textureName = "turret_bullet_piercing_glow";
            }

            ResourceLocation glowTexture = new ResourceLocation(RefStrings.MODID, "textures/entity/" + textureName + ".png");

            // Рендерим слой, который светится в темноте (Eyes = полный свет)
            RenderType glowRenderType = RenderType.eyes(glowTexture);

            getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable,
                    glowRenderType, bufferSource.getBuffer(glowRenderType),
                    partialTick, 15728880, packedOverlay,
                    1.0f, 1.0f, 1.0f, 1.0f);
        }
    }
}
