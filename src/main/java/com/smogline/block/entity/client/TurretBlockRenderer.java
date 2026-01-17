package com.smogline.block.entity.client;

import com.smogline.block.entity.custom.TurretBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class TurretBlockRenderer extends GeoBlockRenderer<TurretBlockEntity> {
    public TurretBlockRenderer(BlockEntityRendererProvider.Context context) {
        super(new TurretBlockModel());
    }
}
