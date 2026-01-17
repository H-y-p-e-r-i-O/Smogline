package com.smogline.client;

import com.smogline.item.custom.weapons.guns.MachineGunItem;
import com.smogline.lib.RefStrings;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RefStrings.MODID, value = Dist.CLIENT)
public class ClientZoomHandler {

    private static boolean isZoomed = false; // Состояние зума
    private static boolean wasKeyDown = false; // Для отслеживания клика (не удержания)

    @SubscribeEvent
    public static void onFovModifier(ComputeFovModifierEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack stack = mc.player.getMainHandItem();
        if (!(stack.getItem() instanceof MachineGunItem)) {
            // Если оружие убрали из рук, сбрасываем зум
            isZoomed = false;
            return;
        }

        // Если зум активен, применяем модификатор FOV
        if (isZoomed) {
            event.setNewFovModifier(event.getNewFovModifier() * 0.5F); // 2x зум
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        ItemStack stack = mc.player.getMainHandItem();
        if (!(stack.getItem() instanceof MachineGunItem)) {
            isZoomed = false;
            wasKeyDown = false;
            return;
        }

        // Отслеживаем клик ПКМ (toggle)
        boolean isKeyDown = mc.options.keyUse.isDown();

        if (isKeyDown && !wasKeyDown) {
            // Клик произошел (переход из "не нажата" в "нажата")
            isZoomed = !isZoomed; // Переключаем состояние
        }

        wasKeyDown = isKeyDown;

        // Замедление при зуме
        if (isZoomed && mc.player.input != null) {
            // Уменьшаем скорость движения в 2 раза
            mc.player.input.leftImpulse *= 0.5F;
            mc.player.input.forwardImpulse *= 0.5F;
        }
    }

    // Дополнительный обработчик: сброс зума при смене предмета (опционально)
    @SubscribeEvent
    public static void onInput(InputEvent.InteractionKeyMappingTriggered event) {
        // Если игрок нажал ПКМ НЕ на пулемете, сбрасываем зум
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack stack = mc.player.getMainHandItem();
        if (!(stack.getItem() instanceof MachineGunItem) && event.isUseItem()) {
            isZoomed = false;
        }
    }
}
