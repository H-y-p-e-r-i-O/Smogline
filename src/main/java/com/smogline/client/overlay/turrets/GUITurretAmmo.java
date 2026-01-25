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

        // 1. Рисуем основу GUI (фон)
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // 2. Рендер энергии
        // Получаем данные через Menu (мы добавили dataAccess в BlockEntity ранее)
        // Индексы: 0 = текущая, 1 = макс. См. TurretLightPlacerBlockEntity.dataAccess
        int energy = this.menu.getDataSlot(0); // Текущая
        int maxEnergy = this.menu.getDataSlot(1); // Максимум (100000)

        if (maxEnergy > 0 && energy > 0) {
            int barHeight = 52; // Полная высота полоски

            // Считаем высоту заполнения (снизу вверх)
            // Формула: height * current / max
            int filledHeight = (int) ((long) energy * barHeight / maxEnergy);

            // Координаты на экране (куда рисовать)
            // x + 180 (смещение по X)
            // y + 27 + (52 - filledHeight) (смещение по Y, чтобы рисовать снизу)
            int destX = x + 180;
            int destY = y + 27 + (barHeight - filledHeight);

            // Координаты в текстуре (откуда брать)
            // u = 204 (начало полоски в файле)
            // v = 27 + (52 - filledHeight) (берем нижнюю часть картинки)
            int srcU = 204;
            int srcV = 27 + (barHeight - filledHeight);

            guiGraphics.blit(TEXTURE, destX, destY, srcU, srcV, 16, filledHeight);
        }
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
