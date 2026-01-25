package com.smogline.menu;

import com.smogline.block.entity.custom.TurretAmmoContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class TurretLightMenu extends AbstractContainerMenu {

    private final TurretAmmoContainer ammoContainer;
    private final ContainerData data;
    private final BlockPos pos; // <--- ДОБАВЛЕНО ПОЛЕ

    // Константы размеров
    public static final int AMMO_SLOT_COUNT = 9;
    public static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    public static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int HOTBAR_SLOT_COUNT = 9;

    // --- СЕРВЕРНЫЙ КОНСТРУКТОР ---
    // Добавлен аргумент BlockPos pos
    public TurretLightMenu(int containerId, Inventory playerInventory, TurretAmmoContainer ammoContainer, ContainerData data, BlockPos pos) {
        super(ModMenuTypes.TURRET_AMMO_MENU.get(), containerId);
        this.ammoContainer = ammoContainer;
        this.data = data;
        this.pos = pos; // <--- СОХРАНЯЕМ ПОЗИЦИЮ

        this.addDataSlots(data);

        // Слоты турели (3x3)
        int ammoStartX = 115;
        int ammoStartY = 44;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = row * 3 + col;
                this.addSlot(new SlotItemHandler(ammoContainer, index,
                        ammoStartX + col * 18,
                        ammoStartY + row * 18) {
                    @Override
                    public void setChanged() {
                        super.setChanged();
                        ammoContainer.onContentsChanged(this.getSlotIndex());
                    }
                });
            }
        }

        // Инвентарь игрока
        int playerStartX = 8;
        int playerStartY = 106;
        for (int row = 0; row < PLAYER_INVENTORY_ROW_COUNT; row++) {
            for (int column = 0; column < PLAYER_INVENTORY_COLUMN_COUNT; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9,
                        playerStartX + column * 18, playerStartY + row * 18));
            }
        }

        // Хотбар
        int hotbarY = playerStartY + 58;
        for (int column = 0; column < HOTBAR_SLOT_COUNT; column++) {
            this.addSlot(new Slot(playerInventory, column, playerStartX + column * 18, hotbarY));
        }
    }

    // --- КЛИЕНТСКИЙ КОНСТРУКТОР ---
    // Читаем BlockPos из буфера
    public TurretLightMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, new TurretAmmoContainer(), new SimpleContainerData(5), extraData.readBlockPos());
    }

    // --- ГЕТТЕР ДЛЯ GUI ---
    public BlockPos getPos() {
        return pos;
    }

    public int getDataSlot(int index) {
        return this.data.get(index);
    }

    public TurretAmmoContainer getAmmoContainer() {
        return ammoContainer;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < AMMO_SLOT_COUNT) {
                if (!this.moveItemStackTo(itemstack1, AMMO_SLOT_COUNT, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, AMMO_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
