package com.smogline.item.client.weapons.turrets;

import com.smogline.item.custom.weapons.turrets.TurretLightPlacerBlockItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

// Обрати внимание: дженерик теперь TurretLightPlacerBlockItem
public class TurretLightPlacerItemModel extends GeoModel<TurretLightPlacerBlockItem> {

    @Override
    public ResourceLocation getModelResource(TurretLightPlacerBlockItem animatable) {
        return new ResourceLocation("smogline", "geo/turret_light.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TurretLightPlacerBlockItem animatable) {
        return new ResourceLocation("smogline", "textures/entity/turret_light.png");
    }

    @Override
    public ResourceLocation getAnimationResource(TurretLightPlacerBlockItem animatable) {
        return new ResourceLocation("smogline", "animations/turret_light_placer.animation.json");
    }
}
