package com.smogline.block.entity.custom;

import com.smogline.api.rotation.Rotational;
import com.smogline.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Random;

public class WindGenFlugerBlockEntity extends BlockEntity implements GeoBlockEntity, Rotational {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private long speed = 0;
    private long torque = 0;
    private static final long MAX_SPEED = 100;
    private static final long MAX_TORQUE = 25;

    private int tickCounter = 0;
    private static final int CHANGE_INTERVAL = 100;
    private final Random random = new Random();

    private float currentAnimationSpeed = 0f;
    private static final float ACCELERATION = 0.05f;
    private static final float DECELERATION = 0.02f;
    private static final int STOP_DELAY_TICKS = 10;
    private static final float MIN_ANIM_SPEED = 0.005f;
    private int ticksWithoutPower = 0;

    // Направление ветра (поворот корпуса)
    private float currentWindYaw = 0f;
    private float targetWindYaw = 0f;
    private boolean shouldPlayTurnAnimation = false;

    private static final float MAX_TURN_PER_TICK = 0.5f;

    private static final RawAnimation ROTATION = RawAnimation.begin().thenLoop("rotation");
    private static final RawAnimation FLUGER = RawAnimation.begin().thenLoop("fluger");
    private static final RawAnimation FLUGER_FAST = RawAnimation.begin().thenPlay("fluger_fast");

    public float getCurrentWindYaw() {
        return currentWindYaw;
    }

    public WindGenFlugerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIND_GEN_FLUGER_BE.get(), pos, state);
    }

    // Rotational
    @Override public long getSpeed() { return speed; }
    @Override public long getTorque() { return torque; }
    @Override public void setSpeed(long speed) { this.speed = Math.min(speed, MAX_SPEED); setChanged(); sync(); }
    @Override public void setTorque(long torque) { this.torque = Math.min(torque, MAX_TORQUE); setChanged(); sync(); }
    @Override public long getMaxSpeed() { return MAX_SPEED; }
    @Override public long getMaxTorque() { return MAX_TORQUE; }

    public static void tick(Level level, BlockPos pos, BlockState state, WindGenFlugerBlockEntity be) {
        if (level.isClientSide) {
            be.handleClientAnimation();
            be.handleWindYawAnimation();
        } else {
            be.tickServer();
        }
    }

    private void tickServer() {
        tickCounter++;
        if (tickCounter >= CHANGE_INTERVAL) {
            tickCounter = 0;
            long newSpeed = (long) (random.nextDouble() * MAX_SPEED);
            long newTorque = (long) (random.nextDouble() * MAX_TORQUE);
            setSpeed(newSpeed);
            setTorque(newTorque);

            float newYaw = (random.nextFloat() * 80) - 40;
            setTargetWindYaw(newYaw);
        }
    }

    private void handleClientAnimation() {
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

    private void handleWindYawAnimation() {
        if (Math.abs(targetWindYaw - currentWindYaw) > 0.01f) {
            float delta = targetWindYaw - currentWindYaw;
            float step = Math.copySign(Math.min(Math.abs(delta), MAX_TURN_PER_TICK), delta);
            currentWindYaw += step;
            if (!shouldPlayTurnAnimation && Math.abs(step) > 0.01f) {
                shouldPlayTurnAnimation = true;
                triggerAnim("fluger_controller", "fluger_fast");
            }
        } else {
            shouldPlayTurnAnimation = false;
        }
    }

    public void setTargetWindYaw(float yaw) {
        this.targetWindYaw = yaw;
        setChanged();
        sync();
    }

    // GeckoLib
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "rotation_controller", 0, this::rotationPredicate));
        controllers.add(new AnimationController<>(this, "fluger_controller", 0, this::flugerPredicate));
    }

    private <E extends GeoBlockEntity> PlayState rotationPredicate(AnimationState<E> event) {
        if (currentAnimationSpeed < MIN_ANIM_SPEED) {
            return PlayState.STOP;
        }
        event.getController().setAnimation(ROTATION);
        event.getController().setAnimationSpeed(currentAnimationSpeed);
        return PlayState.CONTINUE;
    }

    private <E extends GeoBlockEntity> PlayState flugerPredicate(AnimationState<E> event) {
        event.getController().setAnimation(FLUGER);
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getBoneResetTime() { return 0; }

    // NBT
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("Speed", speed);
        tag.putLong("Torque", torque);
        tag.putFloat("TargetWindYaw", targetWindYaw);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        speed = tag.getLong("Speed");
        torque = tag.getLong("Torque");
        targetWindYaw = tag.getFloat("TargetWindYaw");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putLong("Speed", speed);
        tag.putLong("Torque", torque);
        tag.putFloat("TargetWindYaw", targetWindYaw);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        speed = tag.getLong("Speed");
        torque = tag.getLong("Torque");
        targetWindYaw = tag.getFloat("TargetWindYaw");
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}