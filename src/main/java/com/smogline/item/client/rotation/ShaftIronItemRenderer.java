package com.smogline.item.client.rotation;

import com.smogline.item.custom.rotation.ShaftIronBlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class ShaftIronItemRenderer extends GeoItemRenderer<ShaftIronBlockItem> {
    public ShaftIronItemRenderer() {
        super(new ShaftIronItemModel());
    }
}