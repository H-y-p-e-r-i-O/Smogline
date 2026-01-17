package com.smogline.item.client.weapons.ammo.renderers;

import com.smogline.item.custom.weapons.ammo.AmmoTurretItem;
import com.smogline.item.client.weapons.ammo.models.AmmoTurretModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class AmmoTurretRenderer extends GeoItemRenderer<AmmoTurretItem> {
    public AmmoTurretRenderer() {
        super(new AmmoTurretModel());
    }
}
