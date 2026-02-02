package com.smogline.item.client.weapons.mines;


import com.smogline.item.client.weapons.turrets.TurretLightPlacerItemModel;
import com.smogline.item.custom.weapons.mines.MineApBlockItem;
import com.smogline.item.custom.weapons.turrets.TurretLightPlacerBlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class MineApItemRenderer extends GeoItemRenderer<MineApBlockItem> {
    public MineApItemRenderer() {
        super(new MineApItemModel()); // Используем правильную модель
    }
}
