package com.smogline.block.entity.custom;

import com.smogline.api.rotation.RotationNetworkHelper;
import com.smogline.api.rotation.RotationSource;
import com.smogline.api.rotation.Rotational;
import com.smogline.api.rotation.RotationalNode;
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

import java.util.HashSet;
import java.util.Set;

public class GearPortBlockEntity extends BlockEntity implements RotationalNode {

    private long speed = 0;
    private long torque = 0;

    private Direction firstPort = null;
    private Direction secondPort = null;

    // Кеш собственного значения (не используется как источник, но для совместимости)
    private RotationSource cachedSource;
    private long cacheTimestamp;
    private static final long CACHE_LIFETIME = 10; // тиков (1 секунда)

    public GearPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GEAR_PORT_BE.get(), pos, state);
    }

    // ========== Логика портов ==========
    public String handleScrewdriverClick(Direction face, boolean shift) {
        System.out.println("GearPort handleClick: face=" + face + ", shift=" + shift);

        if (shift) {
            // Установка второго порта
            if (face.equals(firstPort)) {
                return "Нельзя назначить второй порт на сторону первого порта!";
            }
            if (face.equals(secondPort)) {
                secondPort = null;
                setChanged();
                sync();
                return "Второй порт сброшен.";
            } else {
                secondPort = face;
                setChanged();
                sync();
                return "Второй порт назначен на сторону " + face.getName() + ".";
            }
        } else {
            // Установка первого порта
            if (face.equals(secondPort)) {
                return "Нельзя назначить первый порт на сторону второго порта!";
            }
            if (face.equals(firstPort)) {
                firstPort = null;
                setChanged();
                sync();
                return "Первый порт сброшен.";
            } else {
                firstPort = face;
                setChanged();
                sync();
                return "Первый порт назначен на сторону " + face.getName() + ".";
            }
        }
    }

    public Direction getFirstPort() { return firstPort; }
    public Direction getSecondPort() { return secondPort; }

    // ========== Rotational ==========
    @Override public long getSpeed() { return speed; }
    @Override public long getTorque() { return torque; }
    @Override public void setSpeed(long speed) { this.speed = speed; setChanged(); sync(); }
    @Override public void setTorque(long torque) { this.torque = torque; setChanged(); sync(); }
    @Override public long getMaxSpeed() { return 0; }
    @Override public long getMaxTorque() { return 0; }

    // ========== RotationalNode ==========
    @Override
    @Nullable
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
        if (this.cachedSource != null) {
            this.cachedSource = null;
            if (level != null && !level.isClientSide) {
                if (firstPort != null) {
                    BlockPos neighborPos = worldPosition.relative(firstPort);
                    if (level.getBlockEntity(neighborPos) instanceof RotationalNode node) {
                        node.invalidateCache();
                    }
                }
                if (secondPort != null) {
                    BlockPos neighborPos = worldPosition.relative(secondPort);
                    if (level.getBlockEntity(neighborPos) instanceof RotationalNode node) {
                        node.invalidateCache();
                    }
                }
            }
        }
    }

    /**
     * GearPort не может быть источником сам по себе.
     */
    @Override
    public boolean canProvideSource(@Nullable Direction fromDir) {
        return false;
    }

    /**
     * Определяет направления для продолжения поиска.
     */
    @Override
    public Direction[] getPropagationDirections(@Nullable Direction fromDir) {
        if (fromDir == null) {
            // Начало поиска – оба порта (если они есть)
            java.util.ArrayList<Direction> list = new java.util.ArrayList<>();
            if (firstPort != null) list.add(firstPort);
            if (secondPort != null) list.add(secondPort);
            return list.toArray(new Direction[0]);
        } else {
            // Если пришли с одного порта, идём в другой (если есть)
            if (fromDir.equals(firstPort) && secondPort != null) {
                return new Direction[]{secondPort};
            } else if (fromDir.equals(secondPort) && firstPort != null) {
                return new Direction[]{firstPort};
            } else {
                return new Direction[0];
            }
        }
    }

    // ========== Тик (сервер) ==========
    public static void tick(Level level, BlockPos pos, BlockState state, GearPortBlockEntity be) {
        if (level.isClientSide) return;

        if (be.firstPort == null || be.secondPort == null) {
            be.setSpeed(0);
            be.setTorque(0);
            return;
        }

        RotationSource sourceFromFirst = findSourceOnPort(be, be.firstPort);
        RotationSource sourceFromSecond = findSourceOnPort(be, be.secondPort);

        long newSpeed = 0;
        long newTorque = 0;

        if (sourceFromFirst != null && sourceFromSecond != null) {
            // Конфликт: два источника
            newSpeed = 0;
            newTorque = 0;
        } else if (sourceFromFirst != null) {
            newSpeed = sourceFromFirst.speed();
            newTorque = sourceFromFirst.torque();
        } else if (sourceFromSecond != null) {
            newSpeed = sourceFromSecond.speed();
            newTorque = sourceFromSecond.torque();
        }

        boolean changed = false;
        if (be.speed != newSpeed) {
            be.speed = newSpeed;
            changed = true;
        }
        if (be.torque != newTorque) {
            be.torque = newTorque;
            changed = true;
        }

        if (changed) {
            be.setChanged();
            be.sync();
        }
    }

    @Nullable
    private static RotationSource findSourceOnPort(GearPortBlockEntity be, Direction port) {
        if (be.level == null) return null;
        BlockPos neighborPos = be.worldPosition.relative(port);
        BlockEntity neighbor = be.level.getBlockEntity(neighborPos);
        if (neighbor == null) return null;
        // Начинаем поиск с соседа, направление от которого мы пришли (противоположное порту)
        return RotationNetworkHelper.findSource(neighbor, port.getOpposite(), new HashSet<>(), 0);
    }

    // ========== NBT и синхронизация ==========
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("Speed", speed);
        tag.putLong("Torque", torque);
        if (firstPort != null) tag.putInt("FirstPort", firstPort.ordinal());
        if (secondPort != null) tag.putInt("SecondPort", secondPort.ordinal());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        speed = tag.getLong("Speed");
        torque = tag.getLong("Torque");
        firstPort = tag.contains("FirstPort") ? Direction.from3DDataValue(tag.getInt("FirstPort")) : null;
        secondPort = tag.contains("SecondPort") ? Direction.from3DDataValue(tag.getInt("SecondPort")) : null;
        cachedSource = null; // сброс кеша
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putLong("Speed", speed);
        tag.putLong("Torque", torque);
        if (firstPort != null) tag.putInt("FirstPort", firstPort.ordinal());
        if (secondPort != null) tag.putInt("SecondPort", secondPort.ordinal());
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        speed = tag.getLong("Speed");
        torque = tag.getLong("Torque");
        firstPort = tag.contains("FirstPort") ? Direction.from3DDataValue(tag.getInt("FirstPort")) : null;
        secondPort = tag.contains("SecondPort") ? Direction.from3DDataValue(tag.getInt("SecondPort")) : null;
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

    public boolean hasPortOnSide(Direction side) {
        return side.equals(firstPort) || side.equals(secondPort);
    }
}