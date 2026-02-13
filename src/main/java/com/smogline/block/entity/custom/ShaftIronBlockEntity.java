package com.smogline.block.entity.custom;

import com.smogline.api.rotation.Rotational;
import com.smogline.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

public class ShaftIronBlockEntity extends BlockEntity implements GeoBlockEntity, Rotational {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private long speed = 0;
    private long torque = 0;
    private boolean hasPowerSource = false;

    // Анимация (скорость на клиенте)
    private float currentAnimationSpeed = 0f;
    private static final float ACCELERATION = 0.1f;

    // Единственная анимация вращения (вокруг локальной оси Z)
    private static final RawAnimation ROTATION = RawAnimation.begin().thenLoop("rotation");

    public ShaftIronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHAFT_IRON_BE.get(), pos, state);
    }

    // --- Синхронизация с клиентом ---
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        load(tag);
        // При получении обновления на клиенте сразу устанавливаем скорость анимации
        if (level != null && level.isClientSide) {
            currentAnimationSpeed = (speed > 0) ? Math.max(0.1f, speed / 100f) : 0f;
        }
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

    // --- Логика вращения (сервер) ---
    public static void tick(Level level, BlockPos pos, BlockState state, ShaftIronBlockEntity be) {
        if (level.isClientSide) {
            be.handleClientAnimation();
            return;
        }

        long bestSpeed = 0;
        long bestTorque = 0;
        boolean hasSource = false;

        // Проверяем моторы
        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor instanceof MotorElectroBlockEntity motor) {
                bestSpeed = motor.getSpeed();
                bestTorque = motor.getTorque();
                hasSource = true;
                break;
            }
        }

        // Если нет мотора — ищем вал с источником
        if (!hasSource) {
            for (Direction dir : Direction.values()) {
                BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
                if (neighbor instanceof ShaftIronBlockEntity shaft && shaft.hasPowerSource) {
                    bestSpeed = shaft.getSpeed();
                    bestTorque = shaft.getTorque();
                    hasSource = true;
                    break;
                }
            }
        }

        be.updateFromSource(bestSpeed, bestTorque, hasSource);
    }

    private void updateFromSource(long newSpeed, long newTorque, boolean hasSource) {
        boolean changed = false;
        if (this.speed != newSpeed) { this.speed = newSpeed; changed = true; }
        if (this.torque != newTorque) { this.torque = newTorque; changed = true; }
        if (this.hasPowerSource != hasSource) { this.hasPowerSource = hasSource; changed = true; }
        if (changed) {
            setChanged();
            sync();
        }
    }

    // --- Клиент: плавное изменение скорости анимации ---
    private void handleClientAnimation() {
        float targetSpeed = (speed > 0) ? Math.max(0.1f, speed / 100f) : 0f;

        // Если анимация стояла, а должна крутиться — стартуем мгновенно
        if (currentAnimationSpeed == 0 && targetSpeed > 0) {
            currentAnimationSpeed = targetSpeed;
        } else if (currentAnimationSpeed < targetSpeed) {
            currentAnimationSpeed = Math.min(currentAnimationSpeed + ACCELERATION, targetSpeed);
        } else if (currentAnimationSpeed > targetSpeed) {
            currentAnimationSpeed = Math.max(currentAnimationSpeed - ACCELERATION, targetSpeed);
        }

        // Отладка: раскомментировать при необходимости
        // System.out.println("Anim speed: " + currentAnimationSpeed + ", target: " + targetSpeed);
    }

    // --- Rotational ---
    @Override public long getSpeed() { return speed; }
    @Override public long getTorque() { return torque; }
    @Override public void setSpeed(long speed) { this.speed = speed; setChanged(); sync(); }
    @Override public void setTorque(long torque) { this.torque = torque; setChanged(); sync(); }
    @Override public long getMaxSpeed() { return 0; }
    @Override public long getMaxTorque() { return 0; }
    public boolean hasPowerSource() { return hasPowerSource; }

    // --- NBT ---
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("Speed", speed);
        tag.putLong("Torque", torque);
        tag.putBoolean("HasPowerSource", hasPowerSource);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        speed = tag.getLong("Speed");
        torque = tag.getLong("Torque");
        hasPowerSource = tag.getBoolean("HasPowerSource");

        // На клиенте сразу устанавливаем скорость анимации при загрузке из NBT
        if (level != null && level.isClientSide) {
            currentAnimationSpeed = (speed > 0) ? Math.max(0.1f, speed / 100f) : 0f;
        }
    }

    // --- GeckoLib ---
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "shaft_controller", 0, this::animationPredicate));
    }

    private <E extends GeoBlockEntity> PlayState animationPredicate(AnimationState<E> event) {
        if (currentAnimationSpeed <= 0.05f) {
            event.getController().forceAnimationReset();
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
}