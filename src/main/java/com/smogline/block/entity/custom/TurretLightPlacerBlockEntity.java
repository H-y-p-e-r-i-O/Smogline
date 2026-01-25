package com.smogline.block.entity.custom;

import com.smogline.api.energy.IEnergyReceiver;
import com.smogline.api.energy.IEnergyConnector; // Возможно он у вас есть, если нет - убери
import com.smogline.api.energy.LongEnergyWrapper; // Для совместимости с Forge
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

    // Инвентарь
    private final TurretAmmoContainer ammoContainer = new TurretAmmoContainer();
    private final LazyOptional<ItemStackHandler> itemHandlerOptional = LazyOptional.of(() -> ammoContainer);

    // Энергия
    private long energyStored = 0;
    private final long MAX_ENERGY = 100000;
    private final long MAX_RECEIVE = 10000;
    // --- Новые поля для логики ---
    private int respawnTimer = 0;
    private static final int RESPAWN_DELAY_TICKS = 1200; // 60 секунд

    // Потребление (HE/tick)
    private static final long DRAIN_TRACKING = 13; // ~250 HE/s
    private static final long DRAIN_HEALING = 25;  // x2 от базы (примерно)
    private static final float HEAL_PER_TICK = 0.05F; // 1 HP в секунду

    // HBM Capabilities
    private final LazyOptional<IEnergyReceiver> hbmReceiverOptional = LazyOptional.of(() -> this);

    // Forge Compatibility Capability (через ваш враппер)
    private final LazyOptional<IEnergyStorage> forgeEnergyOptional = LazyOptional.of(
            () -> new LongEnergyWrapper(this, LongEnergyWrapper.BitMode.LOW)
    );

    private UUID turretUUID;

    public TurretLightPlacerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_LIGHT_PLACER_BE.get(), pos, state);
        this.ammoContainer.setOnContentsChanged(this::setChanged);
    }

    public TurretAmmoContainer getAmmoContainer() {
        return ammoContainer;
    }

    private UUID ownerUUID; // <--- ДОБАВИТЬ ЭТО

    public void setOwner(UUID owner) {
        this.ownerUUID = owner;
        setChanged();
    }

    // ================== HBM ENERGY API ==================

    @Override
    public long receiveEnergy(long amount, boolean simulate) {
        if (!canReceive()) return 0;
        long energyReceived = Math.min(MAX_ENERGY - energyStored, Math.min(MAX_RECEIVE, amount));
        if (!simulate && energyReceived > 0) {
            energyStored += energyReceived;
            setChanged();
        }
        return energyReceived;
    }

    @Override
    public long getEnergyStored() {
        return energyStored;
    }

    @Override
    public void setEnergyStored(long energy) {
        this.energyStored = Math.max(0, Math.min(energy, MAX_ENERGY));
        setChanged();
    }

    @Override
    public long getMaxEnergyStored() {
        return MAX_ENERGY;
    }

    @Override
    public boolean canReceive() {
        return energyStored < MAX_ENERGY;
    }

    @Override
    public long getReceiveSpeed() {
        return MAX_RECEIVE;
    }

    @Override
    public Priority getPriority() {
        return Priority.NORMAL;
    }

    @Override
    public boolean canConnectEnergy(Direction side) {
        return side != Direction.UP; // Подключаемся везде, кроме верха
    }

    // Вспомогательные методы для GUI (если там int)
    public int getEnergyStoredInt() { return (int) Math.min(energyStored, Integer.MAX_VALUE); }
    public int getMaxEnergyStoredInt() { return (int) Math.min(MAX_ENERGY, Integer.MAX_VALUE); }

    // ================== LOGIC ==================

    public static void tick(Level level, BlockPos pos, BlockState state, TurretLightPlacerBlockEntity entity) {
        if (level.isClientSide) return;

        // --- 1. ПРОВЕРКА СОСТОЯНИЯ ТУРЕЛИ ---
        TurretLightLinkedEntity existingTurret = null;
        if (entity.turretUUID != null) {
            ServerLevel serverLevel = (ServerLevel) level;
            Entity e = serverLevel.getEntity(entity.turretUUID);

            if (e instanceof TurretLightLinkedEntity t) {
                if (t.isAlive()) {
                    existingTurret = t;
                } else {
                    // ТУРЕЛЬ ПОГИБЛА В ЭТОМ ТИКЕ
                    handleTurretDeath(entity);
                }
            } else {
                // Если энтити null, проверяем физически (вдруг выгружена)
                var nearby = level.getEntitiesOfClass(TurretLightLinkedEntity.class,
                        new net.minecraft.world.phys.AABB(pos).inflate(2.0));
                boolean found = false;
                for (var t : nearby) {
                    if (t.getUUID().equals(entity.turretUUID)) {
                        if (t.isAlive()) {
                            existingTurret = t;
                            found = true;
                        } else {
                            handleTurretDeath(entity);
                        }
                        break;
                    }
                }
                // Если не нашли и не убили -> считаем, что она просто далеко/выгружена (ничего не делаем)
            }
        }

        // --- 2. ЛОГИКА ЖИВОЙ ТУРЕЛИ (ПОТРЕБЛЕНИЕ) ---
        if (existingTurret != null) {
            long totalDrain = 0;
            boolean needsHeal = existingTurret.needsHealing();
            boolean isTracking = existingTurret.isTrackingTarget();

            // Если лечимся -> Потребление x2 (25)
            if (needsHeal) {
                totalDrain = DRAIN_HEALING;
            }
            // Если просто следим -> Потребление (13)
            else if (isTracking) {
                totalDrain = DRAIN_TRACKING;
            }

            // Пытаемся списать энергию
            if (entity.energyStored >= totalDrain) {
                entity.energyStored -= totalDrain;
                existingTurret.setPowered(true);

                // Лечим
                if (needsHeal) {
                    existingTurret.healFromPower(HEAL_PER_TICK);
                }

                if (totalDrain > 0) entity.setChanged();
            } else {
                // Энергии не хватило даже на слежение -> Выключаем ИИ
                existingTurret.setPowered(false);
            }

            // Если турель жива, таймер респауна не нужен
            entity.respawnTimer = 0;
        }

        // --- 3. ЛОГИКА ВОЗРОЖДЕНИЯ (ТУРЕЛИ НЕТ) ---
        else if (entity.turretUUID == null) {
            // Энергия копится сама через receiveEnergy.

            // Если накопили максимум (100к)
            if (entity.energyStored >= entity.MAX_ENERGY) {

                // Если таймер > 0, значит мы ждем восстановления (минута)
                if (entity.respawnTimer > 0) {

                    // Потребление x2 во время восстановления (как в ТЗ)
                    if (entity.energyStored >= DRAIN_HEALING) {
                        entity.energyStored -= DRAIN_HEALING;
                        entity.respawnTimer--; // Тикаем таймер только если есть энергия на "процесс"
                        entity.setChanged();
                    }

                    // Таймер вышел -> СПАВН
                    if (entity.respawnTimer <= 0) {
                        spawnTurret(level, pos, entity);
                    }
                }
                // Если таймер == 0 и энергия полная -> Значит это первая установка блока (не смерть)
                else {
                    spawnTurret(level, pos, entity);
                }
            }
        }
    }

    // Вызывается при смерти турели
    private static void handleTurretDeath(TurretLightPlacerBlockEntity entity) {
        // ТЗ: "буфер её блока обнуляется"
        entity.energyStored = 0;
        entity.turretUUID = null;

        // ТЗ: "после того как он снова заполнится, через минуту турель снова восстановится"
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

            // ВАЖНО: Восстанавливаем владельца!
            if (entity.ownerUUID != null) {
                Player owner = level.getPlayerByUUID(entity.ownerUUID);
                if (owner != null) {
                    turret.setOwner(owner); // Если игрок онлайн
                } else {
                    // Если оффлайн, ставим UUID напрямую (надо добавить метод в турель, см. ниже)
                    turret.setOwnerUUIDDirect(entity.ownerUUID);
                }
            }

            level.addFreshEntity(turret);
            entity.turretUUID = turret.getUUID();
             entity.setChanged();
        }
    }

    // ================== CAPABILITIES ==================

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        // Игнорируем верхнюю сторону (там турель)
        if (side == Direction.UP) {
            return super.getCapability(cap, side);
        }

        // 1. HBM API (для вашей сети)
        if (cap == ModCapabilities.HBM_ENERGY_RECEIVER) {
            return hbmReceiverOptional.cast();
        }
        // Если сеть требует IEnergyConnector для проверки соединений
        if (cap == ModCapabilities.HBM_ENERGY_CONNECTOR) {
            return hbmReceiverOptional.cast();
        }

        // 2. Forge Energy (для совместимости с трубами других модов)
        if (cap == ForgeCapabilities.ENERGY) {
            return forgeEnergyOptional.cast();
        }

        // 3. Инвентарь
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandlerOptional.cast();
        }

        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        hbmReceiverOptional.invalidate();
        forgeEnergyOptional.invalidate();
        itemHandlerOptional.invalidate();
    }

    // ================== SAVE / LOAD ==================

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("AmmoContainer", ammoContainer.serializeNBT());
        tag.putLong("Energy", energyStored);
        if (ownerUUID != null) tag.putUUID("OwnerUUID", ownerUUID); // <--- СОХРАНЕНИЕ
        if (turretUUID != null) {tag.putUUID("TurretUUID", turretUUID);}
        tag.putInt("RespawnTimer", respawnTimer);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("AmmoContainer")) {ammoContainer.deserializeNBT(tag.getCompound("AmmoContainer"));}
        if (tag.hasUUID("OwnerUUID")) ownerUUID = tag.getUUID("OwnerUUID"); // <---
        if (tag.contains("Energy")) {energyStored = tag.getLong("Energy");}
        if (tag.hasUUID("TurretUUID")) {turretUUID = tag.getUUID("TurretUUID");}
        if (tag.contains("RespawnTimer")) respawnTimer = tag.getInt("RespawnTimer");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // Синхронизация данных (Энергия) с клиентом
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
        public void set(int index, int value) {
            if (index == 0) {
                TurretLightPlacerBlockEntity.this.energyStored = value;
            }
        }

        @Override
        public int getCount() {
            return 2; // Два значения: текущая и макс. энергия
        }
    };

    public net.minecraft.world.inventory.ContainerData getDataAccess() {
        return dataAccess;
    }

}
