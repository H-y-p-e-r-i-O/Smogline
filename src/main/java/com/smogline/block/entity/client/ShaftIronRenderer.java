package com.smogline.block.entity.client;

import com.smogline.block.entity.custom.ShaftIronBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class ShaftIronRenderer extends GeoBlockRenderer<ShaftIronBlockEntity> {
    public ShaftIronRenderer(BlockEntityRendererProvider.Context context) {
        super(new ShaftIronModel());
    }
}