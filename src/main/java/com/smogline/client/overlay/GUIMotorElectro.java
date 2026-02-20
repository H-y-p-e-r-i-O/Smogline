package com.smogline.client.overlay;

import com.smogline.menu.MotorElectroMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import com.smogline.network.packet.PacketToggleMotor;
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

        // Фон (предположим, что он начинается с (0,0) в текстуре)
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Энергобар
        int energy = menu.getEnergy();
        int maxEnergy = menu.getMaxEnergy();
        if (maxEnergy > 0) {
            int barHeight = 52;
            int filledHeight = (int) ((long) energy * barHeight / maxEnergy);
            guiGraphics.blit(TEXTURE,
                    x + 123, y + 5 + (barHeight - filledHeight),
                    187, 30 + (barHeight - filledHeight),
                    16, filledHeight);
        }

        // Кнопка питания
        boolean isOn = menu.isSwitchedOn();
        int buttonV = isOn ? 101 : 101 + 32; // предполагается, что выключенное состояние на 32 пикселя ниже
        guiGraphics.blit(TEXTURE,
                x + 47, y + 35,
                187, buttonV,
                10, 32);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        if (isHovering(123, 5, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, Component.literal(menu.getEnergy() + " / " + menu.getMaxEnergy() + " HE"), mouseX, mouseY);
        }
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // можно добавить название
        guiGraphics.drawString(this.font, this.title, 8, 6, 4210752, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        double relX = mouseX - x;
        double relY = mouseY - y;

        if (button == 0) {
            // Область кнопки питания (x=47..57, y=35..67 на экране)
            if (relX >= 47 && relX < 57 && relY >= 35 && relY < 67) {
                playClickSound(); // <-- добавить
                PacketToggleMotor packet = new PacketToggleMotor(menu.getPos());
                com.smogline.network.ModPacketHandler.INSTANCE.sendToServer(packet);
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