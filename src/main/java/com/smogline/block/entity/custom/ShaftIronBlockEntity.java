package com.smogline.block.entity.custom;

import com.smogline.api.rotation.Rotational;
import com.smogline.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ShaftIronBlockEntity extends BlockEntity implements GeoBlockEntity, Rotational {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private long speed = 0;
    private long torque = 0;

    public ShaftIronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHAFT_IRON_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ShaftIronBlockEntity be) {
        if (level.isClientSide) return;
        long bestSpeed = 0;
        long bestTorque = 0;
        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor instanceof Rotational rot && !(neighbor instanceof ShaftIronBlockEntity && neighbor == be)) {
                long s = rot.getSpeed();
                if (s > 0) {
                    bestSpeed = s;
                    bestTorque = rot.getTorque();
                    break;
                }
            }
        }
        if (bestSpeed == 0) {
            if (be.speed != 0 || be.torque != 0) {
                be.speed = 0;
                be.torque = 0;
                be.setChanged();
            }
        } else {
            if (be.speed != bestSpeed || be.torque != bestTorque) {
                be.speed = bestSpeed;
                be.torque = bestTorque;
                be.setChanged();
            }
        }
    }

    // --- Rotational ---
    @Override public long getSpeed() { return speed; }
    @Override public long getTorque() { return torque; }
    @Override public void setSpeed(long speed) { this.speed = speed; setChanged(); }
    @Override public void setTorque(long torque) { this.torque = torque; setChanged(); }
    @Override public long getMaxSpeed() { return 0; }
    @Override public long getMaxTorque() { return 0; }

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