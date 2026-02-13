package com.smogline.block.entity.client;

import com.smogline.block.entity.custom.ShaftIronBlockEntity;
import com.smogline.lib.RefStrings;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ShaftIronModel extends GeoModel<ShaftIronBlockEntity> {
    @Override
    public ResourceLocation getModelResource(ShaftIronBlockEntity animatable) {
        return new ResourceLocation(RefStrings.MODID, "geo/shaft_iron.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ShaftIronBlockEntity animatable) {
        return new ResourceLocation(RefStrings.MODID, "textures/block/shaft_iron.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ShaftIronBlockEntity animatable) {
        return new ResourceLocation(RefStrings.MODID, "animations/shaft_iron.animation.json");
    }
}