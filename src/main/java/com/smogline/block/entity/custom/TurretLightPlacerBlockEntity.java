package com.smogline.block.entity.custom;

import com.smogline.api.energy.EnergyNetworkManager;
import com.smogline.api.energy.IEnergyReceiver;
import com.smogline.api.energy.IEnergyConnector;
import com.smogline.api.energy.LongEnergyWrapper;
import com.smogline.block.entity.ModBlockEntities;
import com.smogline.capability.ModCapabilities;
import com.smogline.entity.ModEntities;
import com.smogline.entity.weapons.turrets.TurretLightLinkedEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.ItemStackHandler;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.UUID;

public class TurretLightPlacerBlockEntity extends BlockEntity implements GeoBlockEntity, IEnergyReceiver, IEnergyConnector {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final TurretAmmoContainer ammoContainer = new TurretAmmoContainer();
    private final LazyOptional<ItemStackHandler> itemHandlerOptional = LazyOptional.of(() -> ammoContainer);

    private long energyStored = 0;
    private final long MAX_ENERGY = 100000;
    private final long MAX_RECEIVE = 10000;

    private int respawnTimer = 0;
    private static final int RESPAWN_DELAY_TICKS = 1200;
    private static final long DRAIN_TRACKING = 13;
    private static final long DRAIN_HEALING = 25;
    private static final float HEAL_PER_TICK = 0.05F;

    private final LazyOptional<IEnergyReceiver> hbmReceiverOptional = LazyOptional.of(() -> this);
    private final LazyOptional<IEnergyStorage> forgeEnergyOptional = LazyOptional.of(
            () -> new LongEnergyWrapper(this, LongEnergyWrapper.BitMode.LOW)
    );

    private UUID turretUUID;
    private UUID ownerUUID;

    public TurretLightPlacerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_LIGHT_PLACER_BE.get(), pos, state);
        this.ammoContainer.setOnContentsChanged(this::setChanged);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TurretLightPlacerBlockEntity entity) {
        if (level.isClientSide) return;

        // Фикс сети
        if (level instanceof ServerLevel serverLevel) {
            EnergyNetworkManager manager = EnergyNetworkManager.get(serverLevel);
            if (!manager.hasNode(pos)) manager.addNode(pos);
        }

        // --- 1. ПРОВЕРКА ТУРЕЛИ ---
        TurretLightLinkedEntity existingTurret = null;
        if (entity.turretUUID != null) {
            ServerLevel serverLevel = (ServerLevel) level;
            Entity e = serverLevel.getEntity(entity.turretUUID);

            if (e instanceof TurretLightLinkedEntity t && t.isAlive()) {
                existingTurret = t;
            } else {
                var nearby = level.getEntitiesOfClass(TurretLightLinkedEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(2.0));
                for (var t : nearby) {
                    if (t.getUUID().equals(entity.turretUUID)) {
                        if (t.isAlive()) existingTurret = t;
                        else handleTurretDeath(entity);
                        break;
                    }
                }
                if (existingTurret == null && e != null && !e.isAlive()) {
                    handleTurretDeath(entity);
                }
            }
        }

        // --- 2. ПОТРЕБЛЕНИЕ ---
        if (existingTurret != null) {
            long totalDrain = 0;
            boolean needsHeal = existingTurret.needsHealing();
            boolean isTracking = existingTurret.isTrackingTarget();

            if (needsHeal) totalDrain = DRAIN_HEALING;
            else if (isTracking) totalDrain = DRAIN_TRACKING;

            if (entity.energyStored >= totalDrain) {
                entity.energyStored -= totalDrain;
                existingTurret.setPowered(true);
                if (needsHeal) existingTurret.healFromPower(HEAL_PER_TICK);
                if (totalDrain > 0) entity.setChanged();
            } else {
                existingTurret.setPowered(false);
            }
            entity.respawnTimer = 0;
        }

        // --- 3. ВОЗРОЖДЕНИЕ И СПАВН ---
        else if (entity.turretUUID == null) {

            boolean readyToSpawn = false;

            // А) Режим таймера (после смерти)
            if (entity.respawnTimer > 0) {
                // Тикаем таймер, только если буфер ПОЧТИ полон.
                // (MAX - DRAIN) нужно, чтобы таймер не останавливался на 99975 энергии.
                if (entity.energyStored >= entity.MAX_ENERGY - DRAIN_HEALING) {
                    entity.energyStored -= DRAIN_HEALING;
                    entity.respawnTimer--;
                    entity.setChanged();
                }

                if (entity.respawnTimer <= 0) {
                    readyToSpawn = true;
                }
            }
            // Б) Режим первого запуска (таймер 0)
            else {
                // Ждем полного заряда
                if (entity.energyStored >= entity.MAX_ENERGY) {
                    readyToSpawn = true;
                }
            }

            // В) Сам спавн
            if (readyToSpawn) {
                spawnTurret(level, pos, entity);

                // 🔥 ГЛАВНОЕ ИЗМЕНЕНИЕ: Списываем половину буфера при спавне
                if (entity.turretUUID != null) {
                    entity.energyStored -= (entity.MAX_ENERGY / 2); // -50,000 HE
                    entity.setChanged();
                }
            }
        }
    }

    // ... Остальной код (handleTurretDeath, spawnTurret, Capabilities и т.д.) без изменений ...

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level != null && !this.level.isClientSide) {
            EnergyNetworkManager.get((ServerLevel) this.level).removeNode(this.getBlockPos());
        }
        hbmReceiverOptional.invalidate();
        forgeEnergyOptional.invalidate();
        itemHandlerOptional.invalidate();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (this.level != null && !this.level.isClientSide) {
            EnergyNetworkManager.get((ServerLevel) this.level).removeNode(this.getBlockPos());
        }
    }

    private static void handleTurretDeath(TurretLightPlacerBlockEntity entity) {
        entity.energyStored = 0;
        entity.turretUUID = null;
        entity.respawnTimer = RESPAWN_DELAY_TICKS;
        entity.setChanged();
    }

    private static void spawnTurret(Level level, BlockPos pos, TurretLightPlacerBlockEntity entity) {
        TurretLightLinkedEntity turret = ModEntities.TURRET_LIGHT_LINKED.get().create(level);
        if (turret != null) {
            turret.setParentBlock(pos);
            turret.moveTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0, 0);
            turret.setPersistenceRequired();
            turret.yBodyRot = 0;
            turret.yHeadRot = 0;
            turret.setAmmoContainer(entity.getAmmoContainer());

            if (entity.ownerUUID != null) {
                Player owner = level.getPlayerByUUID(entity.ownerUUID);
                if (owner != null) turret.setOwner(owner);
                else turret.setOwnerUUIDDirect(entity.ownerUUID);
            }

            level.addFreshEntity(turret);
            entity.turretUUID = turret.getUUID();
            entity.setChanged();
        }
    }

    // ... Остальные геттеры/сеттеры/capability ...
    public TurretAmmoContainer getAmmoContainer() { return ammoContainer; }
    public void setOwner(UUID owner) { this.ownerUUID = owner; setChanged(); }

    @Override public long receiveEnergy(long amount, boolean simulate) {
        if (!canReceive()) return 0;
        long energyReceived = Math.min(MAX_ENERGY - energyStored, Math.min(MAX_RECEIVE, amount));
        if (!simulate && energyReceived > 0) { energyStored += energyReceived; setChanged(); }
        return energyReceived;
    }
    @Override public long getEnergyStored() { return energyStored; }
    @Override public void setEnergyStored(long energy) { this.energyStored = Math.max(0, Math.min(energy, MAX_ENERGY)); setChanged(); }
    @Override public long getMaxEnergyStored() { return MAX_ENERGY; }
    @Override public boolean canReceive() { return energyStored < MAX_ENERGY; }
    @Override public long getReceiveSpeed() { return MAX_RECEIVE; }
    @Override public Priority getPriority() { return Priority.NORMAL; }
    @Override public boolean canConnectEnergy(Direction side) { return side != Direction.UP; }

    public int getEnergyStoredInt() { return (int) Math.min(energyStored, Integer.MAX_VALUE); }
    public int getMaxEnergyStoredInt() { return (int) Math.min(MAX_ENERGY, Integer.MAX_VALUE); }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (side == Direction.UP) return super.getCapability(cap, side);
        if (cap == ModCapabilities.HBM_ENERGY_RECEIVER || cap == ModCapabilities.HBM_ENERGY_CONNECTOR) return hbmReceiverOptional.cast();
        if (cap == ForgeCapabilities.ENERGY) return forgeEnergyOptional.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemHandlerOptional.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        hbmReceiverOptional.invalidate();
        forgeEnergyOptional.invalidate();
        itemHandlerOptional.invalidate();
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("AmmoContainer", ammoContainer.serializeNBT());
        tag.putLong("Energy", energyStored);
        if (ownerUUID != null) tag.putUUID("OwnerUUID", ownerUUID);
        if (turretUUID != null) tag.putUUID("TurretUUID", turretUUID);
        tag.putInt("RespawnTimer", respawnTimer);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("AmmoContainer")) ammoContainer.deserializeNBT(tag.getCompound("AmmoContainer"));
        if (tag.hasUUID("OwnerUUID")) ownerUUID = tag.getUUID("OwnerUUID");
        if (tag.contains("Energy")) energyStored = tag.getLong("Energy");
        if (tag.hasUUID("TurretUUID")) turretUUID = tag.getUUID("TurretUUID");
        if (tag.contains("RespawnTimer")) respawnTimer = tag.getInt("RespawnTimer");
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    protected final net.minecraft.world.inventory.ContainerData dataAccess = new net.minecraft.world.inventory.ContainerData() {
        @Override
        public int get(int index) {
            switch (index) {
                case 0: return TurretLightPlacerBlockEntity.this.getEnergyStoredInt();
                case 1: return TurretLightPlacerBlockEntity.this.getMaxEnergyStoredInt();
                default: return 0;
            }
        }
        @Override
        public void set(int index, int value) { if (index == 0) TurretLightPlacerBlockEntity.this.energyStored = value; }
        @Override
        public int getCount() { return 2; }
    };
    public net.minecraft.world.inventory.ContainerData getDataAccess() { return dataAccess; }
}
