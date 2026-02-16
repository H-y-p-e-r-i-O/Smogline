package com.smogline.block.entity.client;

import com.smogline.block.entity.custom.WindGenFlugerBlockEntity;
import com.smogline.lib.RefStrings;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class WindGenFlugerModel extends GeoModel<WindGenFlugerBlockEntity> {
    @Override
    public ResourceLocation getModelResource(WindGenFlugerBlockEntity animatable) {
        return new ResourceLocation(RefStrings.MODID, "geo/wind_gen_fluger.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(WindGenFlugerBlockEntity animatable) {
        return new ResourceLocation(RefStrings.MODID, "textures/block/wind_gen_fluger.png");
    }

    @Override
    public ResourceLocation getAnimationResource(WindGenFlugerBlockEntity animatable) {
        return new ResourceLocation(RefStrings.MODID, "animations/wind_gen_fluger.animation.json");
    }
}