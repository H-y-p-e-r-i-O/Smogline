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

        // 1. ПРОВЕРКА СУЩЕСТВОВАНИЯ ТУРЕЛИ
        if (entity.turretUUID != null) {
            ServerLevel serverLevel = (ServerLevel) level;
            Entity existing = serverLevel.getEntity(entity.turretUUID);

            // Если энтити найдена в памяти сервера -> Проверяем, жива ли она
            if (existing != null) {
                if (!existing.isAlive()) {
                    // Турель умерла -> Сбрасываем UUID, разрешаем новый спавн
                    entity.turretUUID = null;
                    entity.setChanged();
                }
            }
            // Если existing == null, это может значить две вещи:
            // а) Турель уничтожена и исчезла.
            // б) Турель в выгруженном чанке.
            // МЫ НЕ ДОЛЖНЫ сбрасывать UUID просто так, иначе при перезаходе будет дубликат!
            // Поэтому, если null, мы считаем "пусть пока считается живой",
            // и проверяем более сложным способом (поиск по UUID в мире, если очень надо),
            // но для простоты лучше НЕ сбрасывать UUID, если мы не уверены.

            // ЕДИНСТВЕННОЕ ИСКЛЮЧЕНИЕ: Если мы точно знаем, что турель должна быть ЗДЕСЬ, но её нет.
            // Можно проверить AABB вокруг блока.
            else {
                // Ищем турель физически в мире рядом с блоком (радиус 2 блока)
                // Это защитит от ситуации "перезашел в мир -> переменная existing сбросилась -> спавн дубликата"
                boolean foundPhysical = false;
                var nearby = level.getEntitiesOfClass(TurretLightLinkedEntity.class,
                        new net.minecraft.world.phys.AABB(pos).inflate(2.0));

                for (var t : nearby) {
                    if (t.getUUID().equals(entity.turretUUID)) {
                        foundPhysical = true;
                        break;
                    }
                }

                // Если в радиусе 2 блоков турели с таким UUID нет -> считаем её погибшей
                if (!foundPhysical) {
                    entity.turretUUID = null;
                    entity.setChanged();
                }
            }
        }

        // 2. ЛОГИКА СПАВНА
        // Спавним ТОЛЬКО если турели нет (UUID == null)
        if (entity.turretUUID == null) {
            // И только если энергии хватает
            if (entity.energyStored >= 100000) {
                spawnTurret(level, pos, entity);

                // Списываем энергию
                entity.energyStored -= 100000;
                entity.setChanged();
            }
        }

        // В любых других случаях (турель есть) энергия просто копится до максимума (MAX_ENERGY)
        // благодаря методу receiveEnergy.
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
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("AmmoContainer")) {ammoContainer.deserializeNBT(tag.getCompound("AmmoContainer"));}
        if (tag.hasUUID("OwnerUUID")) ownerUUID = tag.getUUID("OwnerUUID"); // <---
        if (tag.contains("Energy")) {energyStored = tag.getLong("Energy");}
        if (tag.hasUUID("TurretUUID")) {turretUUID = tag.getUUID("TurretUUID");}
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
