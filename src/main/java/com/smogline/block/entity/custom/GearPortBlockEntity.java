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

import java.util.HashSet;
import java.util.Set;

public class GearPortBlockEntity extends BlockEntity implements Rotational {

    private long speed = 0;
    private long torque = 0;

    private Direction firstPort = null;
    private Direction secondPort = null;

    private static final int MAX_SEARCH_DEPTH = 32;

    public GearPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GEAR_PORT_BE.get(), pos, state);
    }

    // ========== Логика портов ==========
    public String handleScrewdriverClick(Direction face, boolean shift) {
        // Отладка: выводим в консоль (можно заменить на логгер)
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

    // ========== NBT ==========
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

    // ========== Поиск источника вращения ==========
    @Nullable
    public ShaftIronBlockEntity.SourceInfo findSource(Set<BlockPos> visited, Direction fromDir) {
        if (visited.contains(worldPosition)) return null;
        if (visited.size() > MAX_SEARCH_DEPTH) return null;
        visited.add(worldPosition);

        Direction[] portsToCheck;
        if (fromDir == null) {
            portsToCheck = new Direction[]{firstPort, secondPort};
        } else {
            if (fromDir.equals(firstPort) && secondPort != null) {
                portsToCheck = new Direction[]{secondPort};
            } else if (fromDir.equals(secondPort) && firstPort != null) {
                portsToCheck = new Direction[]{firstPort};
            } else {
                portsToCheck = new Direction[0];
            }
        }

        for (Direction dir : portsToCheck) {
            if (dir == null) continue;

            BlockPos neighborPos = worldPosition.relative(dir);
            BlockEntity neighbor = level.getBlockEntity(neighborPos);

            if (neighbor instanceof MotorElectroBlockEntity motor) {
                Direction motorFacing = motor.getBlockState().getValue(MotorElectroBlock.FACING);
                if (motorFacing == dir.getOpposite()) {
                    return new ShaftIronBlockEntity.SourceInfo(motor.getSpeed(), motor.getTorque());
                }
            } else if (neighbor instanceof ShaftIronBlockEntity shaft) {
                Direction shaftFacing = shaft.getBlockState().getValue(ShaftIronBlock.FACING);
                if (shaftFacing == dir || shaftFacing == dir.getOpposite()) {
                    ShaftIronBlockEntity.SourceInfo found = shaft.findSource(visited, dir.getOpposite());
                    if (found != null) return found;
                }
            } else if (neighbor instanceof GearPortBlockEntity gear) {
                ShaftIronBlockEntity.SourceInfo found = gear.findSource(visited, dir.getOpposite());
                if (found != null) return found;
            } else if (neighbor instanceof StopperBlockEntity stopper) {
            ShaftIronBlockEntity.SourceInfo found = stopper.findSource(visited, dir.getOpposite(), MAX_SEARCH_DEPTH + 1);
            if (found != null) return found;
            } else if (neighbor instanceof AdderBlockEntity adder) {
                ShaftIronBlockEntity.SourceInfo found = adder.findSource(visited, dir.getOpposite(), MAX_SEARCH_DEPTH + 1);
                if (found != null) return found;
            } else if (neighbor instanceof TachometerBlockEntity tacho) {
                ShaftIronBlockEntity.SourceInfo found = tacho.findSource(visited, dir.getOpposite(), MAX_SEARCH_DEPTH + 1);
                if (found != null) return found;
            } else if (neighbor instanceof WindGenFlugerBlockEntity windGen) {
                // Генератор сам является источником
                return new ShaftIronBlockEntity.SourceInfo(windGen.getSpeed(), windGen.getTorque());
            }
        }
        return null;
    }

    // Перегрузка для внешнего вызова (без fromDir)
    @Nullable
    public ShaftIronBlockEntity.SourceInfo findSource(Set<BlockPos> visited) {
        return findSource(visited, null);
    }

    // ========== Тик (сервер) ==========
    public static void tick(Level level, BlockPos pos, BlockState state, GearPortBlockEntity be) {
        if (level.isClientSide) {
            return; // на клиенте ничего не делаем
        }

        if (be.firstPort == null || be.secondPort == null) {
            be.setSpeed(0);
            be.setTorque(0);
            return;
        }

        Set<BlockPos> visited = new HashSet<>();
        ShaftIronBlockEntity.SourceInfo sourceFromFirst = be.findSource(visited, be.firstPort);

        visited.clear();
        ShaftIronBlockEntity.SourceInfo sourceFromSecond = be.findSource(visited, be.secondPort);

        long newSpeed = 0;
        long newTorque = 0;

        if (sourceFromFirst != null && sourceFromSecond != null) {
            // Конфликт: два источника
            newSpeed = 0;
            newTorque = 0;
        } else if (sourceFromFirst != null) {
            newSpeed = sourceFromFirst.speed;
            newTorque = sourceFromFirst.torque;
        } else if (sourceFromSecond != null) {
            newSpeed = sourceFromSecond.speed;
            newTorque = sourceFromSecond.torque;
        }

        boolean changed = false;
        if (be.speed != newSpeed) { be.speed = newSpeed; changed = true; }
        if (be.torque != newTorque) { be.torque = newTorque; changed = true; }

        if (changed) {
            be.setChanged();
            be.sync();
        }
    }

    public boolean hasPortOnSide(Direction side) {
        return side.equals(firstPort) || side.equals(secondPort);
    }

}