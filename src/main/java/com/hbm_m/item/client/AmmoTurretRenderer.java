package com.hbm_m.item.client;

import com.hbm_m.item.AmmoTurretItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class AmmoTurretRenderer extends GeoItemRenderer<AmmoTurretItem> {
    public AmmoTurretRenderer() {
        super(new AmmoTurretModel());
    }
}
