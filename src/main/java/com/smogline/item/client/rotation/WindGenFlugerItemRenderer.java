package com.smogline.item.client.rotation;

import com.smogline.item.custom.rotation.MotorElectroBlockItem;
import com.smogline.item.custom.rotation.WindGenFlugerBlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class WindGenFlugerItemRenderer extends GeoItemRenderer<WindGenFlugerBlockItem> {
    public WindGenFlugerItemRenderer() {
        super(new WindGenFlugerItemModel());
    }
}