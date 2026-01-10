package com.hbm_m.entity.client; // Проверь свой пакет!

import com.hbm_m.entity.TurretBulletEntity;
import com.hbm_m.lib.RefStrings;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TurretBulletModel extends GeoModel<TurretBulletEntity> {

    @Override
    public ResourceLocation getModelResource(TurretBulletEntity object) {
        // Модель одна для всех (если форма пули одинаковая)
        return new ResourceLocation(RefStrings.MODID, "geo/turret_bullet.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TurretBulletEntity object) {
        // 1. Получаем ID патрона из сущности
        String ammoId = object.getAmmoId();

        // 2. Выбираем текстуру в зависимости от ID
        if (ammoId.contains("piercing")) {
            return new ResourceLocation(RefStrings.MODID, "textures/entity/turret_bullet_piercing.png");
        }

        // Можно добавить еще условия для других типов
        // if (ammoId.contains("explosive")) { ... }

        // 3. Дефолтная текстура (обычная пуля)
        return new ResourceLocation(RefStrings.MODID, "textures/entity/turret_bullet.png");
    }

    @Override
    public ResourceLocation getAnimationResource(TurretBulletEntity animatable) {
        return new ResourceLocation(RefStrings.MODID, "animations/turret_bullet.animation.json");
    }
}
