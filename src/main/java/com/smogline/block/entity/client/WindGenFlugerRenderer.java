package com.smogline.block.entity.client;

import com.smogline.block.entity.custom.WindGenFlugerBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class WindGenFlugerRenderer extends GeoBlockRenderer<WindGenFlugerBlockEntity> {
    public WindGenFlugerRenderer(BlockEntityRendererProvider.Context context) {
        super(new WindGenFlugerModel());
    }
}