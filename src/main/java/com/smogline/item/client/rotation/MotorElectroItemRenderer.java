package com.smogline.item.client.rotation;

import com.smogline.item.custom.rotation.MotorElectroBlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class MotorElectroItemRenderer extends GeoItemRenderer<MotorElectroBlockItem> {
    public MotorElectroItemRenderer() {
        super(new MotorElectroItemModel());
    }
}