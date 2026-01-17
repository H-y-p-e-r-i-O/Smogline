package com.smogline.entity.client.renderers.turrets;

import com.smogline.entity.client.models.turrets.TurretLightLinkedModel;
import com.smogline.entity.weapons.turrets.TurretLightLinkedEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class TurretLightLinkedRenderer extends GeoEntityRenderer<TurretLightLinkedEntity> {
    public TurretLightLinkedRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new TurretLightLinkedModel());
        this.shadowRadius = 0.7f;
    }
}
