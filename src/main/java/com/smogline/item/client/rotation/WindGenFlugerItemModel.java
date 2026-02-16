package com.smogline.item.client.rotation;

import com.smogline.item.custom.rotation.MotorElectroBlockItem;
import com.smogline.item.custom.rotation.WindGenFlugerBlockItem;
import com.smogline.lib.RefStrings;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class WindGenFlugerItemModel extends GeoModel<WindGenFlugerBlockItem> {
    @Override
    public ResourceLocation getModelResource(WindGenFlugerBlockItem animatable) {
        return new ResourceLocation(RefStrings.MODID, "geo/motor_electro.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(WindGenFlugerBlockItem animatable) {
        return new ResourceLocation(RefStrings.MODID, "textures/block/motor_electro.png");
    }

    @Override
    public ResourceLocation getAnimationResource(WindGenFlugerBlockItem animatable) {
        return new ResourceLocation(RefStrings.MODID, "animations/motor_electro.animation.json");
    }
}