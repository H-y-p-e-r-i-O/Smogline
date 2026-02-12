package com.smogline.block.entity.client;

import com.smogline.block.entity.custom.MotorElectroBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class MotorElectroRenderer extends GeoBlockRenderer<MotorElectroBlockEntity> {
    public MotorElectroRenderer(BlockEntityRendererProvider.Context context) {
        super(new MotorElectroModel());
    }
}