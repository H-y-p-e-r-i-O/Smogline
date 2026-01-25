package com.smogline.client.overlay.turrets;

import com.smogline.block.entity.custom.TurretLightPlacerBlockEntity;
import com.smogline.menu.TurretLightMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class GUITurretAmmo extends AbstractContainerScreen<TurretLightMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("smogline", "textures/gui/weapon/turret_light.png");

    public GUITurretAmmo(TurretLightMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
        this.imageWidth = 201;
        this.imageHeight = 188; // Поправил размер
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // 1. Фон
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Данные из контейнера
        int energy = this.menu.getDataSlot(0);
        int maxEnergy = this.menu.getDataSlot(1);
        int status = this.menu.getDataSlot(2);

        // 2. Рендер энергии (полоска)
        if (maxEnergy > 0 && energy > 0) {
            int barHeight = 52;
            int filledHeight = (int) ((long) energy * barHeight / maxEnergy);

            int destX = x + 180;
            int destY = y + 27 + (barHeight - filledHeight);

            // Исправлено: явно указываем координаты текстуры
            guiGraphics.blit(TEXTURE, destX, destY, 204, 27 + (barHeight - filledHeight), 16, filledHeight);
        }

        // 3. Рендер ЭКРАНА (светящаяся версия)
        if (energy > 10000) {
            guiGraphics.blit(TEXTURE, x + 10, y + 32, 0, 196, 95, 16);
            drawStatusText(guiGraphics, x + 10, y + 32, 95, 16, status, energy, maxEnergy);
        }
    }


    private void drawStatusText(GuiGraphics guiGraphics, int screenX, int screenY, int w, int h, int status, int energy, int maxEnergy) {
        Component text;
        int color;

        // Подбор цветов под аналоговый фон (#788c91 / #5b7379)
        // Цвета должны быть светлыми, но не кислотными.

        if (status == 1) {
            // --- ONLINE ---
            text = Component.literal("SYSTEM ONLINE");
            color = 0xCCFFCC; // Бледно-зеленый (было 0x00FF00)
        }
        else if (status >= 200 && status <= 300) {
            // --- REPAIRING ---
            int hpPercent = status - 200;
            text = Component.literal("REPAIRING: " + hpPercent + "%");
            color = 0xFFFDD0; // Кремово-желтый (было 0xFFFF00)
        }
        else if (status >= 1000) {
            // --- RESPAWNING ---
            int ticksLeft = status - 1000;
            int seconds = ticksLeft / 20;
            text = Component.literal("RESPAWN: " + seconds + "s");
            color = 0xFFCCCC; // Бледно-красный (было 0xFF5555)
        }
        else {
            // --- OFFLINE ---
            if (energy < maxEnergy) {
                text = Component.literal("CHARGING...");
                color = 0xFFE4B5; // Moccasin (бледно-оранжевый)
            } else {
                text = Component.literal("STANDBY MODE");
                color = 0xE0E0E0; // Светло-серый
            }
        }

        // --- МАСШТАБИРОВАНИЕ И ПОЗИЦИЯ ---
        float scale = 0.7f; // Уменьшаем текст до 70%

        // Сохраняем текущую матрицу трансформаций
        guiGraphics.pose().pushPose();

        // Смещаем точку начала рисования в координаты экрана
        // +5 пикселей отступа слева, чтобы не прилипало к краю
        // +Центрирование по вертикали с учетом скейла
        float textX = (screenX + 5) / scale;
        float textY = (screenY + (h - 8 * scale) / 2) / scale;

        // Применяем масштаб
        guiGraphics.pose().scale(scale, scale, 1.0f);

        // Рисуем текст (false = без тени, для LCD эффекта)
        // Можно попробовать true, если на полосатом фоне плохо видно
        guiGraphics.drawString(this.font, text, (int)textX, (int)textY, color, false);

        // Восстанавливаем матрицу (чтобы остальной GUI не уменьшился)
        guiGraphics.pose().popPose();
    }



    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 13, 11, 4210752, false);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);

        // 3. Тултип при наведении на полоску энергии все работает как надо! теперь давай обновим логику буфера -
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        if (isHovering(180, 27, 16, 52, mouseX, mouseY)) {
            int energy = this.menu.getDataSlot(0);
            int maxEnergy = this.menu.getDataSlot(1);
            guiGraphics.renderTooltip(this.font,
                    Component.literal(String.format("%d / %d HE", energy, maxEnergy)),
                    mouseX, mouseY);
        }
    }

    // Хелпер для проверки наведения (координаты относительно GUI)
    protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;
        mouseX -= guiLeft;
        mouseY -= guiTop;
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 || keyCode == 69) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
