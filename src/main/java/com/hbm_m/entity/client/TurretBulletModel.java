package com.hbm_m.entity.client;

import com.hbm_m.entity.TurretBulletEntity;
import com.hbm_m.main.MainRegistry;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TurretBulletModel extends GeoModel<TurretBulletEntity> {
    @Override
    public ResourceLocation getModelResource(TurretBulletEntity object) {
        return new ResourceLocation(MainRegistry.MOD_ID, "geo/turret_bullet.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TurretBulletEntity object) {
        return new ResourceLocation(MainRegistry.MOD_ID, "textures/entity/turret_bullet.png");
    }

    @Override
    public ResourceLocation getAnimationResource(TurretBulletEntity object) {
        // У пули нет файла анимации, возвращаем null или заглушку
        return null;
    }
}
