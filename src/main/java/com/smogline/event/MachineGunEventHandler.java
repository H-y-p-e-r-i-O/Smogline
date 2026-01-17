package com.smogline.event;

import com.smogline.item.MachineGunItem;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class MachineGunEventHandler {

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        // Если в руке пулемет и мы не в креативе - не ломаем блок
        if (event.getItemStack().getItem() instanceof MachineGunItem && !event.getEntity().isCreative()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        // Клиентский клик в воздух. Можно послать пакет на сервер здесь, если нужно.
    }
}
