package com.smogline.block.entity.custom;

import com.smogline.api.rotation.Rotational;
import com.smogline.api.rotation.RotationalNode;
import com.smogline.api.rotation.RotationSource;
import com.smogline.block.custom.rotation.MotorElectroBlock;
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

public class MotorElectroBlockEntity extends BlockEntity implements GeoBlockEntity, Rotational, RotationalNode {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private long speed = 100;
    private long torque = 50;
    private static final long MAX_SPEED = 100;
    private static final long MAX_TORQUE = 50;

    // Поля для RotationalNode (кеш источника – мотор сам источник, но интерфейс требует)
    private RotationSource cachedSource;
    private long cacheTimestamp;
    private static final long CACHE_LIFETIME = 20;

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
    @Override public void setSpeed(long speed) {
        this.speed = Math.min(speed, MAX_SPEED);
        setChanged();
        sync();
        // При изменении скорости инвалидируем кеш у соседей (валов)
        invalidateNeighborCaches();
    }
    @Override public void setTorque(long torque) {
        this.torque = Math.min(torque, MAX_TORQUE);
        setChanged();
        sync();
        invalidateNeighborCaches();
    }
    @Override public long getMaxSpeed() { return MAX_SPEED; }
    @Override public long getMaxTorque() { return MAX_TORQUE; }

    // --- RotationalNode ---
    @Override
    @Nullable
    public RotationSource getCachedSource() {
        return cachedSource;
    }

    @Override
    public void setCachedSource(@Nullable RotationSource source, long gameTime) {
        this.cachedSource = source;
        this.cacheTimestamp = gameTime;
    }

    @Override
    public boolean isCacheValid(long currentTime) {
        return cachedSource != null && (currentTime - cacheTimestamp) <= CACHE_LIFETIME;
    }

    @Override
    public void invalidateCache() {
        this.cachedSource = null;
    }

    /**
     * Мотор не передаёт поиск дальше – он сам источник.
     */
    @Override
    public Direction[] getPropagationDirections(@Nullable Direction fromDir) {
        return new Direction[0];
    }

    /**
     * Мотор может предоставить источник только при запросе с выходной стороны (противоположной лицевой).
     */
    @Override
    public boolean canProvideSource(@Nullable Direction fromDir) {
        if (fromDir == null) return false;
        Direction facing = getBlockState().getValue(MotorElectroBlock.FACING);
        // Выходная сторона – противоположная лицевой
        return fromDir == facing.getOpposite();
    }

    /**
     * Инвалидирует кеш у соседних блоков (валов) на выходной стороне.
     */
    private void invalidateNeighborCaches() {
        if (level == null || level.isClientSide) return;
        Direction facing = getBlockState().getValue(MotorElectroBlock.FACING);
        BlockPos outputPos = worldPosition.relative(facing.getOpposite());
        BlockEntity neighbor = level.getBlockEntity(outputPos);
        if (neighbor instanceof RotationalNode node) {
            node.invalidateCache();
        }
    }

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
        cachedSource = null; // сбрасываем кеш при загрузке
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