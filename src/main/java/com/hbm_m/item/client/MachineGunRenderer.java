package com.hbm_m.item.client;

import com.hbm_m.item.MachineGunItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class MachineGunRenderer extends GeoItemRenderer<MachineGunItem> {

    public MachineGunRenderer() {
        super(new MachineGunModel());
    }
}
