package com.smogline.block.entity.custom;

import com.smogline.api.rotation.Rotational;
import com.smogline.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class MotorElectroBlockEntity extends BlockEntity implements GeoBlockEntity, Rotational {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private long speed = 100;
    private long torque = 50;
    private static final long MAX_SPEED = 100;
    private static final long MAX_TORQUE = 50;

    public MotorElectroBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MOTOR_ELECTRO_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MotorElectroBlockEntity be) {
        // здесь может быть логика работы мотора (потребление энергии и т.п.)
    }

    // --- Rotational ---
    @Override public long getSpeed() { return speed; }
    @Override public long getTorque() { return torque; }
    @Override public void setSpeed(long speed) { this.speed = Math.min(speed, MAX_SPEED); setChanged(); }
    @Override public void setTorque(long torque) { this.torque = Math.min(torque, MAX_TORQUE); setChanged(); }
    @Override public long getMaxSpeed() { return MAX_SPEED; }
    @Override public long getMaxTorque() { return MAX_TORQUE; }

    // --- NBT ---
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("Speed", speed);
        tag.putLong("Torque", torque);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        speed = tag.getLong("Speed");
        torque = tag.getLong("Torque");
    }

    // --- GeckoLib ---
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}