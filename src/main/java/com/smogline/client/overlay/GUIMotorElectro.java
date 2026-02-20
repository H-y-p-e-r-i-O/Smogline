package com.smogline.client.overlay;

import com.smogline.menu.MotorElectroMenu;
import com.smogline.network.packet.PacketToggleMotor;
import com.smogline.network.packet.PacketToggleMotorMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GUIMotorElectro extends AbstractContainerScreen<MotorElectroMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("smogline", "textures/gui/machine/motor_electro_gui.png");

    public GUIMotorElectro(MotorElectroMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 180;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // 1. Базовый фон (с выключенными кнопками на 0,0)
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // 2. Энергобар (Вертикальный)
        int energy = menu.getEnergy();
        int maxEnergy = menu.getMaxEnergy();
        if (maxEnergy > 0) {
            int barHeight = 52;
            int filledHeight = (int) ((long) energy * barHeight / maxEnergy);
            // Рисуем заполнение снизу вверх
            guiGraphics.blit(TEXTURE,
                    x + 123, y + 5 + (barHeight - filledHeight),
                    187, 30 + (barHeight - filledHeight),
                    16, filledHeight);
        }

        // 3. Кнопка питания (Красная) — Рисуем ТОЛЬКО при включении (ON)
        if (menu.isSwitchedOn()) {
            // Позиция: x47, y35 | Текстура ON: x187, y133 | Размер: 10x32
            guiGraphics.blit(TEXTURE, x + 47, y + 35, 187, 133, 10, 32);
        }

        // 4. Кнопка режима (Желтая) — Рисуем ТОЛЬКО при активном генераторе
        if (menu.isGeneratorMode()) {
            // Позиция: x47, y69 | Текстура ON: x187, y83 | Размер: 10x17
            guiGraphics.blit(TEXTURE, x + 47, y + 69, 187, 83, 10, 17);
        }

        // 5. Прогресс-бар вращения (Горизонтальный, СЛЕВА НАПРАВО)
        int rotVal = menu.getRotationValue();
        if (rotVal > 0) {
            int barWidth = 52;
            // Лимит 100 000 (Speed * Torque) для полной полоски
            int filledWidth = (int) Math.min(barWidth, (rotVal * (long) barWidth) / 100000);

            // Место: x59, y35 | Текстура: x204, y49 | Размер: filledWidth x 16
            // Рисуем кусок текстуры шириной filledWidth
            guiGraphics.blit(TEXTURE, x + 59, y + 35, 204, 49, filledWidth, 16);
        }
    }


    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);

        // Тултип для энергии
        if (isHovering(123, 5, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, Component.literal(menu.getEnergy() + " / " + menu.getMaxEnergy() + " HE"), mouseX, mouseY);
        }

        // Тултип для вращения
        if (isHovering(59, 35, 52, 16, mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, Component.literal("Rotation Power: " + menu.getRotationValue()), mouseX, mouseY);
        }

        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 8, 6, 4210752, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int x = (width - imageWidth) / 2;
            int y = (height - imageHeight) / 2;
            double relX = mouseX - x;
            double relY = mouseY - y;

            // Клик по кнопке ПИТАНИЯ (x47, y35)
            if (relX >= 47 && relX < 57 && relY >= 35 && relY < 67) {
                playClickSound();
                PacketToggleMotor packet = new PacketToggleMotor(menu.getPos());
                com.smogline.network.ModPacketHandler.INSTANCE.sendToServer(packet);
                return true;
            }

            // Клик по кнопке РЕЖИМА (x47, y69)
            if (relX >= 47 && relX < 57 && relY >= 69 && relY < 86) {
                playClickSound();
                com.smogline.network.ModPacketHandler.INSTANCE.sendToServer(new PacketToggleMotorMode(menu.getPos()));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void playClickSound() {
        net.minecraft.client.Minecraft.getInstance().getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}