package com.smogline.block.entity.client;

import com.smogline.block.entity.custom.TurretLightPlacerBlockEntity;
import com.smogline.block.entity.custom.explosives.MineBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class MineApRenderer extends GeoBlockRenderer<MineBlockEntity> {
    public MineApRenderer(BlockEntityRendererProvider.Context context) {
        super(new MineApModel());
    }
}
