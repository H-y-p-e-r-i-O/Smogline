package com.hbm_m.item.client;

import com.hbm_m.item.AmmoTurretItem;
import com.hbm_m.item.AmmoTurretPiercingItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class AmmoTurretPiercingRenderer extends GeoItemRenderer<AmmoTurretPiercingItem> {
    public AmmoTurretPiercingRenderer() {
        super(new AmmoTurretPiercingModel());
    }
}
