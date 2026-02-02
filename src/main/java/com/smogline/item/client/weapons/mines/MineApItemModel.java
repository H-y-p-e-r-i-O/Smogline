package com.smogline.item.client.weapons.mines;

import com.smogline.item.custom.weapons.mines.MineApBlockItem;
import com.smogline.item.custom.weapons.turrets.TurretLightPlacerBlockItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

// Обрати внимание: дженерик теперь TurretLightPlacerBlockItem
public class MineApItemModel extends GeoModel<MineApBlockItem> {

    @Override
    public ResourceLocation getModelResource(MineApBlockItem animatable) {
        return new ResourceLocation("smogline", "geo/mine_ap.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MineApBlockItem animatable) {
        return new ResourceLocation("smogline", "textures/block/mine_ap.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MineApBlockItem animatable) {
        return new ResourceLocation("smogline", "animations/mine_ap.animation.json");
    }
}
