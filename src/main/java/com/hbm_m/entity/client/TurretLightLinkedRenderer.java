package com.hbm_m.entity.client;

import com.hbm_m.entity.TurretLightLinkedEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class TurretLightLinkedRenderer extends GeoEntityRenderer<TurretLightLinkedEntity> {
    public TurretLightLinkedRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new TurretLightLinkedModel());
        this.shadowRadius = 0.7f;
    }
}
