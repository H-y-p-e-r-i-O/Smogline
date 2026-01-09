package com.hbm_m.item.client;

import com.hbm_m.item.AmmoTurretItem;
import com.hbm_m.item.AmmoTurretPiercingItem;
import com.hbm_m.lib.RefStrings;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class AmmoTurretPiercingModel extends GeoModel<AmmoTurretPiercingItem> {
    @Override
    public ResourceLocation getModelResource(AmmoTurretPiercingItem object) {
        return new ResourceLocation(RefStrings.MODID, "geo/ammo_turret_piercing.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AmmoTurretPiercingItem object) {
        return new ResourceLocation(RefStrings.MODID, "textures/item/ammo_turret_piercing.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AmmoTurretPiercingItem animatable) {
        return new ResourceLocation(RefStrings.MODID, "animations/ammo_turret_piercing.animation.json");
    }
}
