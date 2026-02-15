package com.smogline.block.entity.custom;

import com.smogline.api.rotation.Rotational;
import com.smogline.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class MotorElectroBlockEntity extends BlockEntity implements GeoBlockEntity, Rotational {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private long speed = 100;
    private long torque = 50;
    private static final long MAX_SPEED = 100;
    private static final long MAX_TORQUE = 50;

    // Анимация
    private float currentAnimationSpeed = 0f;
    private static final float ACCELERATION = 0.1f;
    private static final float DECELERATION = 0.05f;
    private static final int STOP_DELAY_TICKS = 5;
    private static final float MIN_ANIM_SPEED = 0.01f;
    private int ticksWithoutPower = 0;

    private static final RawAnimation ROTATION = RawAnimation.begin().thenLoop("rotation");
    private static final RawAnimation RUMBLE = RawAnimation.begin().thenLoop("rumble");

    public MotorElectroBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MOTOR_ELECTRO_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MotorElectroBlockEntity be) {
        if (level.isClientSide) {
            be.handleClientAnimation();
        }
        // Здесь может быть логика работы мотора (потребление энергии и т.п.)
    }

    // --- Клиент: управление скоростью анимации ---
    private void handleClientAnimation() {
        // Мотор всегда работает на полной скорости (пока нет системы энергии)
        float targetSpeed = (speed > 0) ? Math.max(0.1f, speed / 100f) : 0f;

        if (targetSpeed > 0) {
            ticksWithoutPower = 0;
            if (currentAnimationSpeed < targetSpeed) {
                currentAnimationSpeed = Math.min(currentAnimationSpeed + ACCELERATION, targetSpeed);
            } else if (currentAnimationSpeed > targetSpeed) {
                currentAnimationSpeed = Math.max(currentAnimationSpeed - ACCELERATION, targetSpeed);
            }
        } else {
            if (currentAnimationSpeed > 0) {
                ticksWithoutPower++;
                if (ticksWithoutPower > STOP_DELAY_TICKS) {
                    currentAnimationSpeed = Math.max(currentAnimationSpeed - DECELERATION, 0f);
                }
            } else {
                ticksWithoutPower = 0;
            }
        }
    }

    // --- Rotational ---
    @Override public long getSpeed() { return speed; }
    @Override public long getTorque() { return torque; }
    @Override public void setSpeed(long speed) { this.speed = Math.min(speed, MAX_SPEED); setChanged(); }
    @Override public void setTorque(long torque) { this.torque = Math.min(torque, MAX_TORQUE); setChanged(); }
    @Override public long getMaxSpeed() { return MAX_SPEED; }
    @Override public long getMaxTorque() { return MAX_TORQUE; }

    // --- NBT ---
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("Speed", speed);
        tag.putLong("Torque", torque);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        speed = tag.getLong("Speed");
        torque = tag.getLong("Torque");
    }

    // --- GeckoLib ---
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Контроллер для rotation (ротор)
        controllers.add(new AnimationController<>(this, "rotation_controller", 0, this::rotationPredicate));
        // Контроллер для rumble (вибрация корпуса)
        controllers.add(new AnimationController<>(this, "rumble_controller", 0, this::rumblePredicate));
    }

    private <E extends GeoBlockEntity> PlayState rotationPredicate(AnimationState<E> event) {
        if (currentAnimationSpeed < MIN_ANIM_SPEED) {
            return PlayState.STOP;
        }
        event.getController().setAnimation(ROTATION);
        event.getController().setAnimationSpeed(currentAnimationSpeed);
        return PlayState.CONTINUE;
    }

    private <E extends GeoBlockEntity> PlayState rumblePredicate(AnimationState<E> event) {
        if (currentAnimationSpeed < MIN_ANIM_SPEED) {
            return PlayState.STOP;
        }
        event.getController().setAnimation(RUMBLE);
        event.getController().setAnimationSpeed(currentAnimationSpeed);
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getBoneResetTime() {
        return 0;
    }
}