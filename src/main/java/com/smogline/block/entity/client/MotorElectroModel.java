package com.smogline.block.entity.client;

import com.smogline.block.entity.custom.MotorElectroBlockEntity;
import com.smogline.lib.RefStrings;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MotorElectroModel extends GeoModel<MotorElectroBlockEntity> {
    @Override
    public ResourceLocation getModelResource(MotorElectroBlockEntity animatable) {
        return new ResourceLocation(RefStrings.MODID, "geo/motor_electro.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MotorElectroBlockEntity animatable) {
        return new ResourceLocation(RefStrings.MODID, "textures/block/motor_electro.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MotorElectroBlockEntity animatable) {
        return new ResourceLocation(RefStrings.MODID, "animations/motor_electro.animation.json"); // если есть
    }
}