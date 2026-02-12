package com.smogline.item.client.rotation;

import com.smogline.item.custom.rotation.ShaftIronBlockItem;
import com.smogline.lib.RefStrings;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ShaftIronItemModel extends GeoModel<ShaftIronBlockItem> {
    @Override
    public ResourceLocation getModelResource(ShaftIronBlockItem animatable) {
        return new ResourceLocation(RefStrings.MODID, "geo/shaft_iron.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ShaftIronBlockItem animatable) {
        return new ResourceLocation(RefStrings.MODID, "textures/block/shaft_iron.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ShaftIronBlockItem animatable) {
        return new ResourceLocation(RefStrings.MODID, "animations/shaft_iron.animation.json");
    }
}