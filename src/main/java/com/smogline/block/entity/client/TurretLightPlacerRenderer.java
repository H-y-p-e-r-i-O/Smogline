package com.smogline.block.entity.client;

import com.smogline.block.entity.custom.TurretLightPlacerBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class TurretLightPlacerRenderer extends GeoBlockRenderer<TurretLightPlacerBlockEntity> {
    public TurretLightPlacerRenderer(BlockEntityRendererProvider.Context context) {
        super(new TurretLightPlacerModel());
    }
}
