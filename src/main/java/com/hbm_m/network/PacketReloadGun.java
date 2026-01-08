package com.hbm_m.network;

import com.hbm_m.item.MachineGunItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketReloadGun {
    public PacketReloadGun() {}
    public PacketReloadGun(FriendlyByteBuf buf) {}
    public void toBytes(FriendlyByteBuf buf) {}

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ItemStack stack = player.getMainHandItem();
                if (stack.getItem() instanceof MachineGunItem gun) {
                    // Вызываем логику перезарядки
                    gun.reloadGun(player, stack);

                    // ВАЖНО: Синхронизируем инвентарь, если были изменения
                    player.inventoryMenu.broadcastChanges();
                }
            }
        });
        return true;
    }
}
