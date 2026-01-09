package com.hbm_m.item.client;

import com.hbm_m.item.AmmoTurretItem;
import com.hbm_m.lib.RefStrings;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class AmmoTurretModel extends GeoModel<AmmoTurretItem> {
    @Override
    public ResourceLocation getModelResource(AmmoTurretItem object) {
        return new ResourceLocation(RefStrings.MODID, "geo/ammo_turret.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AmmoTurretItem object) {
        return new ResourceLocation(RefStrings.MODID, "textures/item/ammo_turret.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AmmoTurretItem animatable) {
        return new ResourceLocation(RefStrings.MODID, "animations/ammo_turret.animation.json");
    }
}
