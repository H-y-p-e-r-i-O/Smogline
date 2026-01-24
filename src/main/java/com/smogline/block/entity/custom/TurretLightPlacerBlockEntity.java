package com.smogline.block.entity.custom;

import com.smogline.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class TurretLightPlacerBlockEntity extends BlockEntity implements GeoBlockEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final TurretAmmoContainer ammoContainer = new TurretAmmoContainer();

    public TurretLightPlacerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_LIGHT_PLACER_BE.get(), pos, state);
    }

    public TurretAmmoContainer getAmmoContainer() {
        return ammoContainer;
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("AmmoContainer", ammoContainer.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("AmmoContainer")) {
            ammoContainer.deserializeNBT(tag.getCompound("AmmoContainer"));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Анимация, если нужна
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
