package com.smogline.item.client.weapons.turrets;


import com.smogline.item.client.weapons.turrets.TurretLightPlacerItemModel;
import com.smogline.item.custom.weapons.turrets.TurretLightPlacerBlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class TurretLightPlacerItemRenderer extends GeoItemRenderer<TurretLightPlacerBlockItem> {
    public TurretLightPlacerItemRenderer() {
        super(new TurretLightPlacerItemModel()); // Используем правильную модель
    }
}
