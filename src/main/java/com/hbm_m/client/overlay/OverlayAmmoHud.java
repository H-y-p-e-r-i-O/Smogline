package com.hbm_m.client.overlay;

import com.hbm_m.item.MachineGunItem;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.registries.ForgeRegistries;

public class OverlayAmmoHud {

    // Текстура заднего фона для плашки (опционально, если нарисуешь)
    // private static final ResourceLocation BG_TEXTURE = new ResourceLocation(RefStrings.MODID, "textures/gui/ammo_hud_bg.png");

    public static final IGuiOverlay HUD_AMMO = (ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) -> {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        // Проверяем, что в руках пулемет
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof MachineGunItem machineGun)) {
            return;
        }

        // Получаем данные
        int currentAmmo = machineGun.getAmmo(stack);
        String loadedId = machineGun.getLoadedAmmoID(stack);

        // Настройки позиционирования (Правый нижний угол)
        int x = screenWidth - 16; // Отступ справа
        int y = screenHeight - 16; // Отступ снизу

        // --- РЕНДЕР ИКОНКИ ПАТРОНА ---
        if (loadedId != null && !loadedId.isEmpty()) {
            Item ammoItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(loadedId));
            if (ammoItem != null) {
                // Рисуем иконку предмета (самого патрона)
                // Сдвигаем влево от текста
                guiGraphics.renderItem(new ItemStack(ammoItem), x - 60, y - 10);
            }
        }

        // --- РЕНДЕР ТЕКСТА ---
        // Формат: "24 + 1/24" (где 24 - в магазине, +1 в стволе (опционально), /24 макс)
        // Твой запрос: "24 + 1/24"
        // Но обычно в играх пишут: "Текущее / Всего" или "Магазин / Запас"
        // У тебя в MachineGunItem: MAG_CAPACITY = 24.

        // Сделаем формат: "AMMO / MAX"
        String text = currentAmmo + " / " + 25; // 24 + 1

        // Если патронов мало - текст красный, иначе белый
        int color = (currentAmmo < 5) ? 0xFF5555 : 0xFFFFFF;

        // Рисуем текст (с тенью)
        // Сдвигаем влево, чтобы текст кончался у края экрана
        int textWidth = mc.font.width(text);
        guiGraphics.drawString(mc.font, text, x - textWidth, y - 6, color, true);
    };
}
