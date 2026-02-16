package com.smogline.block.entity.custom;

import com.smogline.api.rotation.Rotational;
import com.smogline.block.custom.rotation.MotorElectroBlock;
import com.smogline.block.custom.rotation.ShaftIronBlock;
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

import java.util.HashSet;
import java.util.Set;

public class ShaftIronBlockEntity extends BlockEntity implements GeoBlockEntity, Rotational {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private long speed = 0;
    private long torque = 0;

    private static final float ACCELERATION = 0.1f;
    private static final float DECELERATION = 0.03f; // Медленное замедление
    private static final int STOP_DELAY_TICKS = 5;
    private static final float MIN_ANIM_SPEED = 0.005f; // Минимальная скорость перед остановкой

    private float currentAnimationSpeed = 0f;
    private int ticksWithoutPower = 0;

    private static final RawAnimation ROTATION = RawAnimation.begin().thenLoop("rotation");
    private static final int MAX_SEARCH_DEPTH = 32;

    public ShaftIronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHAFT_IRON_BE.get(), pos, state);
    }

    // ========== NBT and sync ==========
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
        this.speed = tag.getLong("Speed");
        this.torque = tag.getLong("Torque");
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

    // ========== Rotational ==========
    @Override public long getSpeed() { return speed; }
    @Override public long getTorque() { return torque; }
    @Override public void setSpeed(long speed) { this.speed = speed; setChanged(); sync(); }
    @Override public void setTorque(long torque) { this.torque = torque; setChanged(); sync(); }
    @Override public long getMaxSpeed() { return 0; }
    @Override public long getMaxTorque() { return 0; }

    // ========== SourceInfo (public static) ==========
    public static class SourceInfo {
        public final long speed;
        public final long torque;
        public SourceInfo(long speed, long torque) {
            this.speed = speed;
            this.torque = torque;
        }
    }

    // ========== Поиск источника ==========
    @Nullable
    public SourceInfo findSource(Set<BlockPos> visited, @Nullable Direction fromDir) {
        return findSource(visited, fromDir, 0);
    }

    @Nullable
    public SourceInfo findSource(Set<BlockPos> visited, @Nullable Direction fromDir, int depth) {
        if (depth > MAX_SEARCH_DEPTH || visited.contains(worldPosition)) {
            return null;
        }
        visited.add(worldPosition);

        Direction myFacing = getBlockState().getValue(ShaftIronBlock.FACING);

        Direction[] searchDirs;
        if (fromDir != null) {
            // Мы пришли с определённого направления – проверяем, что оно совпадает с осью вала
            if (fromDir == myFacing || fromDir == myFacing.getOpposite()) {
                // Идём в противоположную сторону
                searchDirs = new Direction[]{fromDir.getOpposite()};
            } else {
                // Пришли не по оси – тупик
                return null;
            }
        } else {
            // Начало поиска – проверяем обе стороны вдоль оси
            searchDirs = new Direction[]{myFacing, myFacing.getOpposite()};
        }

        for (Direction dir : searchDirs) {
            BlockPos neighborPos = worldPosition.relative(dir);
            BlockEntity neighbor = level.getBlockEntity(neighborPos);

            if (neighbor instanceof MotorElectroBlockEntity motor) {
                Direction motorFacing = motor.getBlockState().getValue(MotorElectroBlock.FACING);
                if (motorFacing == dir.getOpposite()) {
                    return new SourceInfo(motor.getSpeed(), motor.getTorque());
                }
            }else if (neighbor instanceof RotationMeterBlockEntity meter) {
                SourceInfo found = meter.findSource(visited, dir.getOpposite(), depth + 1);
                if (found != null) return found;
            } else if (neighbor instanceof ShaftIronBlockEntity shaft) {
                Direction neighborFacing = shaft.getBlockState().getValue(ShaftIronBlock.FACING);
                if (neighborFacing == dir || neighborFacing == dir.getOpposite()) {
                    SourceInfo found = shaft.findSource(visited, dir.getOpposite(), depth + 1);
                    if (found != null) {
                        return found;
                    }
                }
            } else if (neighbor instanceof GearPortBlockEntity gear) {
                // Добавляем поддержку GearPort
                SourceInfo found = gear.findSource(visited, dir.getOpposite()); // предполагаем, что у GearPort есть такой метод
                if (found != null) {
                    return found;
                }
            } else if (neighbor instanceof StopperBlockEntity stopper) {
                SourceInfo found = stopper.findSource(visited, dir.getOpposite(), depth + 1);
                if (found != null) return found;
            } else if (neighbor instanceof AdderBlockEntity adder) {
                SourceInfo found = adder.findSource(visited, dir.getOpposite(), depth + 1);
                if (found != null) return found;
            }
        }
        return null;
    }

    // ========== Тик ==========
    public static void tick(Level level, BlockPos pos, BlockState state, ShaftIronBlockEntity be) {
        if (level.isClientSide) {
            be.handleClientAnimation();
            return;
        }

        SourceInfo source = be.findSource(new HashSet<>(), null);

        long newSpeed = (source != null) ? source.speed : 0;
        long newTorque = (source != null) ? source.torque : 0;

        boolean changed = false;
        if (be.speed != newSpeed) { be.speed = newSpeed; changed = true; }
        if (be.torque != newTorque) { be.torque = newTorque; changed = true; }

        if (changed) {
            be.setChanged();
            be.sync();
        }
    }

    // ========== Анимация (клиент) ==========
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

    // ========== GeckoLib ==========
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "shaft_controller", 0, this::animationPredicate));
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
}