package com.smogline.block.entity.custom; // Укажи свой пакет

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import com.smogline.block.entity.ModBlockEntities; // Твой реестр BlockEntity

public class TurretLightPlacerBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public TurretLightPlacerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_LIGHT_PLACER_BE.get(), pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Пока без анимаций, просто статичная модель.
        // Если захочешь добавить анимацию раскладывания - добавим контроллер здесь.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
