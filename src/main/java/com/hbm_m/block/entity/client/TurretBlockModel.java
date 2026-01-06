package com.hbm_m.block.entity.client;

import com.hbm_m.block.entity.custom.TurretBlockEntity;
import com.hbm_m.main.MainRegistry;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TurretBlockModel extends GeoModel<TurretBlockEntity> {
    @Override
    public ResourceLocation getModelResource(TurretBlockEntity object) {
        // Тот же файл, что и у сущности!
        return new ResourceLocation(MainRegistry.MOD_ID, "geo/turret_light.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TurretBlockEntity object) {
        return new ResourceLocation(MainRegistry.MOD_ID, "textures/entity/turret_light.png");
    }

    @Override
    public ResourceLocation getAnimationResource(TurretBlockEntity object) {
        return new ResourceLocation(MainRegistry.MOD_ID, "animations/turret_light.animation.json");
    }
}
