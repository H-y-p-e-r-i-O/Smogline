package com.smogline.item.client;

import com.smogline.item.MachineGunItem;
import com.smogline.main.MainRegistry;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MachineGunModel extends GeoModel<MachineGunItem> {

    @Override
    public ResourceLocation getModelResource(MachineGunItem animatable) {
        return new ResourceLocation(MainRegistry.MOD_ID, "geo/machinegun.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MachineGunItem animatable) {
        return new ResourceLocation(MainRegistry.MOD_ID, "textures/item/machinegun.png");  // ✅ PNG, НЕ JPG!
    }

    @Override
    public ResourceLocation getAnimationResource(MachineGunItem animatable) {
        return new ResourceLocation(MainRegistry.MOD_ID, "animations/machinegun.animation.json");
    }
}
