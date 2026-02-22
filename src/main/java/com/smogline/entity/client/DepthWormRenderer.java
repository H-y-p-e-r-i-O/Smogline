package com.smogline.entity.client;

import com.smogline.entity.client.DepthWormModel;
import com.smogline.entity.custom.DepthWormEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DepthWormRenderer extends GeoEntityRenderer<DepthWormEntity> {
    public DepthWormRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DepthWormModel());
        this.shadowRadius = 0.3f;
    }
}
