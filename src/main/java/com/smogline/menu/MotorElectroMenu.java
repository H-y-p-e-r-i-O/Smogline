package com.smogline.menu;

import com.smogline.block.entity.custom.MotorElectroBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MotorElectroMenu extends AbstractContainerMenu {
    private final MotorElectroBlockEntity blockEntity;
    private final ContainerData data;
    private final BlockPos pos;

    // Индексы данных
    public static final int DATA_ENERGY = 0;
    public static final int DATA_MAX_ENERGY = 1;
    public static final int DATA_SWITCHED = 2;
    public static final int DATA_BOOT_TIMER = 3;
    private static final int DATA_COUNT = 4;

    public MotorElectroMenu(int containerId, Inventory playerInv, MotorElectroBlockEntity be, ContainerData data, BlockPos pos) {
        super(ModMenuTypes.MOTOR_ELECTRO_MENU.get(), containerId);
        this.blockEntity = be;
        this.data = data;
        this.pos = pos;

        checkContainerDataCount(data, DATA_COUNT);
        addDataSlots(data);

        // Инвентарь игрока (3 строки по 9 слотов)
        int playerStartX = 8;
        int playerStartY = 98; // координаты на текстуре, где начинается инвентарь
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, playerStartX + col * 18, playerStartY + row * 18));
            }
        }
// Хотбар (9 слотов)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInv, col, playerStartX + col * 18, playerStartY + 58));
        }
    }

    public MotorElectroMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(containerId, playerInv, null, new SimpleContainerData(DATA_COUNT), extraData.readBlockPos());
    }
    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        return ItemStack.EMPTY; // нет слотов
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }



    public BlockPos getPos() { return pos; }
    public int getEnergy() { return data.get(DATA_ENERGY); }
    public int getMaxEnergy() { return data.get(DATA_MAX_ENERGY); }
    public boolean isSwitchedOn() { return data.get(DATA_SWITCHED) == 1; }
    public int getBootTimer() { return data.get(DATA_BOOT_TIMER); }
}