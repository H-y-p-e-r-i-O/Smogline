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
    private static final long MAX_SPEED = 100; // максимальная скорость
    private static final long MAX_TORQUE = 25; // максимальный момент

    // Для случайной генерации
    private int tickCounter = 0;
    private static final int CHANGE_INTERVAL = 100; // менять каждые 5 секунд (20 тиков/сек)
    private final Random random = new Random();

    // Анимация
    private float currentAnimationSpeed = 0f;
    private static final float ACCELERATION = 0.05f;
    private static final float DECELERATION = 0.02f;
    private static final int STOP_DELAY_TICKS = 10;
    private static final float MIN_ANIM_SPEED = 0.005f;
    private int ticksWithoutPower = 0;

    private static final RawAnimation ROTATION = RawAnimation.begin().thenLoop("rotation");

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

    // Тик
    public static void tick(Level level, BlockPos pos, BlockState state, WindGenFlugerBlockEntity be) {
        if (level.isClientSide) {
            be.handleClientAnimation();
            return;
        }

        // Генерация случайных значений с течением времени
        be.tickCounter++;
        if (be.tickCounter >= CHANGE_INTERVAL) {
            be.tickCounter = 0;
            // Генерируем случайные значения в заданных диапазонах
            // Можно добавить зависимость от погоды, времени суток и т.п.
            long newSpeed = (long) (be.random.nextDouble() * MAX_SPEED);
            long newTorque = (long) (be.random.nextDouble() * MAX_TORQUE);
            be.setSpeed(newSpeed);
            be.setTorque(newTorque);
        }
    }

    // Анимация на клиенте
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

    // GeckoLib
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "rotation_controller", 0, this::animationPredicate));
    }

    private <E extends GeoBlockEntity> PlayState animationPredicate(AnimationState<E> event) {
        if (currentAnimationSpeed < MIN_ANIM_SPEED) {
            return PlayState.STOP;
        }
        event.getController().setAnimation(ROTATION);
        event.getController().setAnimationSpeed(currentAnimationSpeed);
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
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        speed = tag.getLong("Speed");
        torque = tag.getLong("Torque");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putLong("Speed", speed);
        tag.putLong("Torque", torque);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        speed = tag.getLong("Speed");
        torque = tag.getLong("Torque");
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