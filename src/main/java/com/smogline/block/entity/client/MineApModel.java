package com.smogline.block.entity.client;

import com.smogline.block.entity.custom.TurretLightPlacerBlockEntity;
import com.smogline.block.entity.custom.explosives.MineBlockEntity;
import com.smogline.lib.RefStrings;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MineApModel extends GeoModel<MineBlockEntity> {
    @Override
    public ResourceLocation getModelResource(MineBlockEntity animatable) {
        // Имя файла: buffer_small.geo.json
        return new ResourceLocation(RefStrings.MODID, "geo/mine_ap.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MineBlockEntity animatable) {
        // Имя файла: buffer_light.jpg (лучше переименовать в .png, Minecraft любит png)
        return new ResourceLocation(RefStrings.MODID, "textures/block/mine_ap.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MineBlockEntity animatable) {
        return new ResourceLocation(RefStrings.MODID, "animations/mine_ap.animation.json");  // 🔥 RefStrings.MODID!
    }


}
