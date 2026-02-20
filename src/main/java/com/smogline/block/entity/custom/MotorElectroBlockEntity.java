package com.smogline.block.entity.custom;

import com.smogline.api.energy.IEnergyConnector;
import com.smogline.api.energy.IEnergyReceiver;
import com.smogline.api.energy.LongEnergyWrapper;
import com.smogline.api.rotation.Rotational;
import com.smogline.api.rotation.RotationalNode;
import com.smogline.api.rotation.RotationSource;
import com.smogline.block.custom.rotation.MotorElectroBlock;
import com.smogline.block.entity.ModBlockEntities;
import com.smogline.capability.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class MotorElectroBlockEntity extends BlockEntity implements GeoBlockEntity, Rotational, RotationalNode, IEnergyReceiver, IEnergyConnector {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private long speed = 100;
    private long torque = 50;
    private static final long MAX_SPEED = 100;
    private static final long MAX_TORQUE = 50;

    // Поля для RotationalNode
    private RotationSource cachedSource;
    private long cacheTimestamp;
    private static final long CACHE_LIFETIME = 10;

    // Анимация
    private float currentAnimationSpeed = 0f;
    private static final float ACCELERATION = 0.1f;
    private static final float DECELERATION = 0.05f;
    private static final int STOP_DELAY_TICKS = 5;
    private static final float MIN_ANIM_SPEED = 0.01f;
    private int ticksWithoutPower = 0;

    // Энергия
    private long energyStored = 0;
    private final long MAX_ENERGY = 50000;
    private final long MAX_RECEIVE = 1000;
    private final long ENERGY_PER_TICK = 500 / 20;

    private boolean isSwitchedOn = false;
    private int bootTimer = 0;

    private final LazyOptional<IEnergyReceiver> hbmReceiverOptional = LazyOptional.of(() -> this);
    private final LazyOptional<IEnergyConnector> hbmConnectorOptional = LazyOptional.of(() -> this);
    private final LazyOptional<IEnergyStorage> forgeEnergyOptional = LazyOptional.of(() -> new LongEnergyWrapper(this, LongEnergyWrapper.BitMode.LOW));

    public MotorElectroBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MOTOR_ELECTRO_BE.get(), pos, state);
    }

    // ========== Управление питанием ==========
    public void togglePower() {
        this.isSwitchedOn = !this.isSwitchedOn;
        if (this.isSwitchedOn) {
            this.bootTimer = 60;
        } else {
            this.bootTimer = 0;
            this.speed = 0;
            this.torque = 0;
        }
        setChanged();
        sync();
        // ВАЖНО: Будим соседей сразу при нажатии кнопки, даже если скорость еще 0.
        // Они проверят нас, увидят 0, но будут знать, что связь есть.
        invalidateNeighborCaches();
    }

    // ========== Инвалидация кеша соседей ==========
    private void invalidateNeighborCaches() {
        if (level == null || level.isClientSide) return;
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(dir);
            BlockEntity neighbor = level.getBlockEntity(neighborPos);
            if (neighbor instanceof RotationalNode node) {
                node.invalidateCache();
            }
        }
    }

    // ========== Энергия ==========
    @Override public long getEnergyStored() { return energyStored; }
    @Override public long getMaxEnergyStored() { return MAX_ENERGY; }
    @Override public void setEnergyStored(long energy) { this.energyStored = Math.max(0, Math.min(energy, MAX_ENERGY)); setChanged(); }
    @Override public long getReceiveSpeed() { return MAX_RECEIVE; }
    @Override public IEnergyReceiver.Priority getPriority() { return IEnergyReceiver.Priority.NORMAL; }
    @Override public boolean canReceive() { return energyStored < MAX_ENERGY; }
    @Override public boolean canConnectEnergy(Direction side) { return side == getBlockState().getValue(MotorElectroBlock.FACING).getOpposite(); }

    @Override
    public long receiveEnergy(long maxReceive, boolean simulate) {
        if (!canReceive()) return 0;
        long energyReceived = Math.min(MAX_ENERGY - energyStored, Math.min(MAX_RECEIVE, maxReceive));
        if (!simulate && energyReceived > 0) {
            energyStored += energyReceived;
            setChanged();
        }
        return energyReceived;
    }

    // ========== Тик ==========
    public static void tick(Level level, BlockPos pos, BlockState state, MotorElectroBlockEntity be) {
        if (level.isClientSide) {
            be.handleClientAnimation();
            return;
        }

        // Если мотор выключен
        if (!be.isSwitchedOn) {
            if (be.speed != 0 || be.torque != 0) {
                be.speed = 0;
                be.torque = 0;
                be.setChanged();
                be.sync();
                be.invalidateNeighborCaches();
            }
            return;
        }

        // Загрузка
        if (be.bootTimer > 0) {
            be.bootTimer--;
            if (be.speed != 0 || be.torque != 0) {
                be.speed = 0;
                be.torque = 0;
                be.setChanged();
                be.sync();
                be.invalidateNeighborCaches();
            }
            return;
        }

        // Потребление энергии
        if (be.energyStored >= be.ENERGY_PER_TICK) {
            be.energyStored -= be.ENERGY_PER_TICK;
            long newSpeed = 100;
            long newTorque = 50;
            if (be.speed != newSpeed || be.torque != newTorque) {
                be.speed = newSpeed;
                be.torque = newTorque;
                be.setChanged();
                be.sync();
                be.invalidateNeighborCaches();
            }
        } else {
            // Не хватает энергии – выключаем
            be.isSwitchedOn = false;
            if (be.speed != 0 || be.torque != 0) {
                be.speed = 0;
                be.torque = 0;
                be.setChanged();
                be.sync();
                be.invalidateNeighborCaches();
            }
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

    // ========== Rotational ==========
    @Override public long getSpeed() { return speed; }
    @Override public long getTorque() { return torque; }
    @Override public void setSpeed(long speed) {
        this.speed = Math.min(speed, MAX_SPEED);
        setChanged();
        sync();
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

    // ========== RotationalNode ==========
    @Override @Nullable
    public RotationSource getCachedSource() { return cachedSource; }
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
        // Пусто. Мотор — источник, ему не нужно реагировать на сброс кеша соседей.
    }
    @Override
    public Direction[] getPropagationDirections(@Nullable Direction fromDir) {
        return new Direction[0];
    }
    @Override
    public boolean canProvideSource(@Nullable Direction fromDir) {
        if (fromDir == null) return false;
        Direction facing = getBlockState().getValue(MotorElectroBlock.FACING);
        return fromDir == facing.getOpposite();
    }

    // ========== Capabilities ==========
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.HBM_ENERGY_RECEIVER) {
            return hbmReceiverOptional.cast();
        }
        if (cap == ModCapabilities.HBM_ENERGY_CONNECTOR) {
            return hbmConnectorOptional.cast();
        }
        if (cap == ForgeCapabilities.ENERGY) {
            return forgeEnergyOptional.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        hbmReceiverOptional.invalidate();
        hbmConnectorOptional.invalidate();
        forgeEnergyOptional.invalidate();
    }

    // ========== NBT и синхронизация ==========
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("Speed", speed);
        tag.putLong("Torque", torque);
        tag.putLong("Energy", energyStored);
        tag.putBoolean("SwitchedOn", isSwitchedOn);
        tag.putInt("BootTimer", bootTimer);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        speed = tag.getLong("Speed");
        torque = tag.getLong("Torque");
        energyStored = tag.getLong("Energy");
        isSwitchedOn = tag.getBoolean("SwitchedOn");
        bootTimer = tag.getInt("BootTimer");
        cachedSource = null;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putLong("Speed", speed);
        tag.putLong("Torque", torque);
        tag.putBoolean("SwitchedOn", isSwitchedOn);
        tag.putInt("BootTimer", bootTimer);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        speed = tag.getLong("Speed");
        torque = tag.getLong("Torque");
        isSwitchedOn = tag.getBoolean("SwitchedOn");
        bootTimer = tag.getInt("BootTimer");
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

    // ========== GeckoLib ==========
    private static final RawAnimation ROTATION = RawAnimation.begin().thenLoop("rotation");
    private static final RawAnimation RUMBLE = RawAnimation.begin().thenLoop("rumble");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "rotation_controller", 0, this::rotationPredicate));
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

    // ========== ContainerData ==========
    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) Math.min(energyStored, Integer.MAX_VALUE);
                case 1 -> (int) Math.min(MAX_ENERGY, Integer.MAX_VALUE);
                case 2 -> isSwitchedOn ? 1 : 0;
                case 3 -> bootTimer;
                default -> 0;
            };
        }
        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> energyStored = value;
                case 2 -> isSwitchedOn = value == 1;
                case 3 -> bootTimer = value;
            }
        }
        @Override
        public int getCount() { return 4; }
    };

    public ContainerData getDataAccess() { return data; }
}