package com.smogline.client.overlay.turrets;

import com.smogline.block.entity.custom.TurretLightPlacerBlockEntity;
import com.smogline.item.custom.weapons.turrets.TurretChipItem;
import com.smogline.menu.TurretLightMenu;
import com.smogline.network.packet.PacketModifyTurretChip;
import com.smogline.network.packet.PacketToggleTurret;
import com.smogline.network.packet.PacketUpdateTurretSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW; // Для кодов клавиш

import java.util.ArrayList;
import java.util.List;

public class GUITurretAmmo extends AbstractContainerScreen<TurretLightMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("smogline", "textures/gui/weapon/turret_light.png");

    // --- СОСТОЯНИЯ GUI ---
    private static final int STATE_NORMAL = 0;

    // Главное меню выбора
    private static final int STATE_MAIN_MENU = 1;

    // Подменю Чипа
    private static final int STATE_CHIP_LIST = 2;
    private static final int STATE_ADD_INPUT = 3;
    private static final int STATE_RESULT_MSG = 4;

    // Подменю Атаки
    private static final int STATE_ATTACK_MODE = 5;

    private int uiState = STATE_NORMAL;

    // Навигация
    private int selectedIndex = 0; // Общий индекс для списков

    // Ввод текста
    private String inputString = "";
    private int cursorTimer = 0;

    // Сообщения
    private String resultMessage = "";
    private int resultColor = 0xFFFFFF;
    private int resultDuration = 0;

    // Таймеры кнопок
    private int timerPlus = 0, timerMinus = 0, timerCheck = 0, timerLeft = 0, timerRight = 0, timerMenu = 0;
    private static final int PRESS_DURATION = 10;

    public GUITurretAmmo(TurretLightMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
        this.imageWidth = 201;
        this.imageHeight = 188;
    }

    public void handleFeedback(boolean success) {
        this.uiState = STATE_RESULT_MSG;
        this.resultDuration = 40;
        if (success) {
            this.resultMessage = "SUCCESS";
            this.resultColor = 0x55FF55;
        } else {
            this.resultMessage = "ERROR 404";
            this.resultColor = 0xFF5555;
        }
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Данные
        int energy = this.menu.getDataSlot(TurretLightMenu.DATA_ENERGY);
        int maxEnergy = this.menu.getDataSlot(TurretLightMenu.DATA_MAX_ENERGY);
        int status = this.menu.getDataSlot(TurretLightMenu.DATA_STATUS);
        boolean isSwitchedOn = this.menu.getDataSlot(TurretLightMenu.DATA_SWITCH) == 1;
        int bootTimer = this.menu.getDataSlot(TurretLightMenu.DATA_BOOT_TIMER);

        // Анимация кнопок
        if (isSwitchedOn) guiGraphics.blit(TEXTURE, x + 10, y + 62, 204, 103, 10, 32);
        if (timerPlus > 0) { timerPlus--; guiGraphics.blit(TEXTURE, x + 39, y + 62, 221, 171, 15, 15); }
        if (timerMinus > 0) { timerMinus--; guiGraphics.blit(TEXTURE, x + 56, y + 62, 204, 137, 15, 15); }
        if (timerCheck > 0) { timerCheck--; guiGraphics.blit(TEXTURE, x + 22, y + 62, 204, 171, 15, 15); }
        if (timerLeft > 0) { timerLeft--; guiGraphics.blit(TEXTURE, x + 73, y + 62, 221, 137, 15, 15); }
        if (timerRight > 0) { timerRight--; guiGraphics.blit(TEXTURE, x + 90, y + 62, 221, 120, 15, 15); }
        if (timerMenu > 0) { timerMenu--; guiGraphics.blit(TEXTURE, x + 73, y + 79, 221, 154, 15, 15); }

        // Энергия
        if (maxEnergy > 0 && energy > 0) {
            int barHeight = 52;
            int filledHeight = (int) ((long) energy * barHeight / maxEnergy);
            guiGraphics.blit(TEXTURE, x + 180, y + 27 + (barHeight - filledHeight), 204, 27 + (barHeight - filledHeight), 16, filledHeight);
        }

        // --- ЭКРАН ---
        if (energy > 10000 && isSwitchedOn) {
            guiGraphics.blit(TEXTURE, x + 10, y + 32, 0, 196, 95, 16);

            if (bootTimer > 0) {
                drawBootingText(guiGraphics, x + 10, y + 32, 95, 16);
                if (uiState != STATE_NORMAL) uiState = STATE_NORMAL;
            } else {

                switch (uiState) {
                    case STATE_MAIN_MENU:
                        drawMainMenu(guiGraphics, x + 10, y + 32, 95, 16);
                        break;

                    case STATE_ATTACK_MODE:
                        drawAttackMode(guiGraphics, x + 10, y + 32, 95, 16);
                        break;

                    case STATE_CHIP_LIST:
                        if (!hasChip()) { uiState = STATE_MAIN_MENU; break; }
                        drawChipUserList(guiGraphics, x + 10, y + 32, 95, 16);
                        break;

                    case STATE_ADD_INPUT:
                        cursorTimer++;
                        String display = inputString + ((cursorTimer / 10 % 2 == 0) ? "_" : "");
                        if (display.length() > 14) display = display.substring(display.length() - 14);
                        drawCenteredText(guiGraphics, display, 0xFFFF00, x + 10, y + 32, 95, 16);
                        break;

                    case STATE_RESULT_MSG:
                        if (resultDuration > 0) {
                            resultDuration--;
                            drawCenteredText(guiGraphics, resultMessage, resultColor, x + 10, y + 32, 95, 16);
                        } else {
                            if (resultMessage.equals("SUCCESS")) uiState = STATE_CHIP_LIST;
                            else uiState = STATE_ADD_INPUT;
                        }
                        break;

                    default: // STATE_NORMAL
                        drawStatusText(guiGraphics, x + 10, y + 32, 95, 16, status, energy, maxEnergy);
                        break;
                }
            }
        } else {
            uiState = STATE_NORMAL;
        }
    }

    // --- ОТРИСОВКА НОВЫХ МЕНЮ ---

    private void drawMainMenu(GuiGraphics guiGraphics, int x, int y, int w, int h) {
        // Опции: 0 = CHIP CONTROL, 1 = ATTACK MODE
        // Ограничиваем индекс
        if (selectedIndex < 0) selectedIndex = 1;
        if (selectedIndex > 1) selectedIndex = 0;

        String text = "";
        int color = 0xFFFFFF;

        if (selectedIndex == 0) {
            text = "CHIP CONTROL";
            // Если чипа нет - тусклый
            if (!hasChip()) color = 0x555555;
        } else {
            text = "ATTACK MODE";
        }

        // Рисуем стрелочки выбора вокруг текста
        text = "< " + text + " >";
        drawCenteredText(guiGraphics, text, color, x, y, w, h);
    }

    private void drawAttackMode(GuiGraphics guiGraphics, int x, int y, int w, int h) {
        // Индексы: 0=Hostile, 1=Neutral, 2=Players
        if (selectedIndex < 0) selectedIndex = 2;
        if (selectedIndex > 2) selectedIndex = 0;

        String name = "";
        boolean isEnabled = false;

        // Получаем текущие данные из меню
        int valHostile = this.menu.getDataSlot(TurretLightMenu.DATA_TARGET_HOSTILE);
        int valNeutral = this.menu.getDataSlot(TurretLightMenu.DATA_TARGET_NEUTRAL);
        int valPlayer = this.menu.getDataSlot(TurretLightMenu.DATA_TARGET_PLAYERS);

        switch (selectedIndex) {
            case 0: name = "HOSTILES"; isEnabled = valHostile == 1; break;
            case 1: name = "NEUTRALS"; isEnabled = valNeutral == 1; break;
            case 2: name = "PLAYERS"; isEnabled = valPlayer == 1; break;
        }

        String symbol = isEnabled ? "[V]" : "[X]";
        int color = isEnabled ? 0x55FF55 : 0xFF5555; // Зеленый / Красный

        drawCenteredText(guiGraphics, name + " " + symbol, color, x, y, w, h);
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        double relX = mouseX - x;
        double relY = mouseY - y;

        if (button == 0) { // ЛКМ

            // --- 1. ОПРЕДЕЛЕНИЕ ОБЛАСТЕЙ КНОПОК ---
            boolean hitPower = (relX >= 10 && relX < 20 && relY >= 62 && relY < 94);
            boolean hitMenu  = (relX >= 73 && relX < 88 && relY >= 79 && relY < 94); // Квадрат с плюсом
            boolean hitCheck = (relX >= 22 && relX < 37 && relY >= 62 && relY < 77); // Галочка
            boolean hitPlus  = (relX >= 39 && relX < 54 && relY >= 62 && relY < 77); // Плюс
            boolean hitMinus = (relX >= 56 && relX < 71 && relY >= 62 && relY < 77); // Минус
            boolean hitLeft  = (relX >= 73 && relX < 88 && relY >= 62 && relY < 77); // Влево
            boolean hitRight = (relX >= 90 && relX < 105 && relY >= 62 && relY < 77); // Вправо

            // --- 2. ВИЗУАЛ И ЗВУК (Срабатывает ВСЕГДА) ---
            // Если мы нажали на любую из кнопок, она должна мигнуть и щелкнуть
            if (hitPower || hitMenu || hitCheck || hitPlus || hitMinus || hitLeft || hitRight) {
                playClickSound();
            }

            if (hitMenu)  timerMenu  = PRESS_DURATION;
            if (hitCheck) timerCheck = PRESS_DURATION;
            if (hitPlus)  timerPlus  = PRESS_DURATION;
            if (hitMinus) timerMinus = PRESS_DURATION;
            if (hitLeft)  timerLeft  = PRESS_DURATION;
            if (hitRight) timerRight = PRESS_DURATION;

            // --- 3. ЛОГИКА (Срабатывает только в нужных состояниях) ---

            // POWER (Работает всегда)
            if (hitPower) {
                com.smogline.network.ModPacketHandler.INSTANCE.send(
                        net.minecraftforge.network.PacketDistributor.SERVER.noArg(),
                        new PacketToggleTurret(this.menu.getPos()));
                return true;
            }

            // MENU TOGGLE (Работает всегда, если есть чип, или просто открывает/закрывает)
            if (hitMenu) {
                if (uiState == STATE_NORMAL) {
                    uiState = STATE_MAIN_MENU;
                    selectedIndex = hasChip() ? 0 : 1;
                } else {
                    uiState = STATE_NORMAL;
                }
                return true;
            }

            // ОСТАЛЬНЫЕ КНОПКИ (Работают только внутри меню)
            if (uiState != STATE_NORMAL && uiState != STATE_RESULT_MSG) {

                if (hitCheck) {
                    if (uiState == STATE_MAIN_MENU) {
                        if (selectedIndex == 0) {
                            if (hasChip()) { uiState = STATE_CHIP_LIST; selectedIndex = 0; }
                        } else if (selectedIndex == 1) {
                            uiState = STATE_ATTACK_MODE; selectedIndex = 0;
                        }
                    } else if (uiState == STATE_ADD_INPUT) {
                        if (!inputString.isEmpty()) {
                            com.smogline.network.ModPacketHandler.INSTANCE.send(
                                    net.minecraftforge.network.PacketDistributor.SERVER.noArg(),
                                    new PacketModifyTurretChip(1, inputString));
                        }
                    }
                    return true;
                }

                if (hitLeft || hitRight) {
                    if (uiState == STATE_MAIN_MENU) {
                        selectedIndex = (selectedIndex == 0) ? 1 : 0;
                    } else if (uiState == STATE_CHIP_LIST || uiState == STATE_ATTACK_MODE) {
                        if (hitLeft) selectedIndex--; else selectedIndex++;
                    }
                    return true;
                }

                if (hitPlus || hitMinus) {
                    if (uiState == STATE_CHIP_LIST) {
                        if (hitPlus) { uiState = STATE_ADD_INPUT; inputString = ""; }
                        else {
                            com.smogline.network.ModPacketHandler.INSTANCE.send(
                                    net.minecraftforge.network.PacketDistributor.SERVER.noArg(),
                                    new PacketModifyTurretChip(0, String.valueOf(selectedIndex)));
                            if (selectedIndex > 0) selectedIndex--;
                        }
                    } else if (uiState == STATE_ATTACK_MODE) {
                        boolean newValue = hitPlus; // true если плюс, false если минус
                        com.smogline.network.ModPacketHandler.INSTANCE.send(
                                net.minecraftforge.network.PacketDistributor.SERVER.noArg(),
                                new PacketUpdateTurretSettings(this.menu.getPos(), selectedIndex, newValue));
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }


    // --- ВВОД С КЛАВИАТУРЫ ---
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (uiState == STATE_ADD_INPUT) {
            // Разрешенные символы (буквы, цифры, _)
            if (Character.isLetterOrDigit(codePoint) || codePoint == '_') {
                if (inputString.length() < 16) { // Макс длина ника
                    inputString += codePoint;
                    return true;
                }
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (uiState == STATE_ADD_INPUT) {
            // Backspace (259)
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!inputString.isEmpty()) {
                    inputString = inputString.substring(0, inputString.length() - 1);
                }
                return true;
            }
            // Enter (257) -> аналог нажатия галочки
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                if (!inputString.isEmpty()) {
                    playClickSound();
                    timerCheck = PRESS_DURATION;
                    com.smogline.network.ModPacketHandler.INSTANCE.send(
                            net.minecraftforge.network.PacketDistributor.SERVER.noArg(),
                            new com.smogline.network.packet.PacketModifyTurretChip(1, inputString)
                    );
                }
                return true;
            }
            // Escape -> выход
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                uiState = STATE_CHIP_LIST;
                return true;
            }
            // Чтобы не закрывалось окно на E, пока печатаем
            if (this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // Хелперы
    private boolean hasChip() {
        ItemStack stack = this.menu.getAmmoContainer().getStackInSlot(9);
        return !stack.isEmpty() && stack.getItem() instanceof TurretChipItem;
    }

    private void drawChipUserList(GuiGraphics guiGraphics, int screenX, int screenY, int w, int h) {
        ItemStack stack = this.menu.getAmmoContainer().getStackInSlot(9);
        List<String> names = new ArrayList<>();
        if (stack.hasTag() && stack.getTag().contains("TurretOwners")) {
            ListTag list = stack.getTag().getList("TurretOwners", Tag.TAG_STRING);
            for (Tag t : list) {
                String s = t.getAsString();
                names.add(s.contains("|") ? s.split("\\|")[1] : s);
            }
        }

        if (selectedIndex < 0) selectedIndex = names.size() - 1;
        if (selectedIndex >= names.size()) selectedIndex = 0;

        String textToShow;
        if (names.isEmpty()) textToShow = "EMPTY LIST";
        else textToShow = (selectedIndex + 1) + "/" + names.size() + " " + names.get(selectedIndex);

        drawCenteredText(guiGraphics, textToShow, 0x00FFFF, screenX, screenY, w, h);
    }


    private void drawCenteredText(GuiGraphics guiGraphics, String textStr, int color, int screenX, int screenY, int w, int h) {
        Component text = Component.literal(textStr);
        float scale = 0.7f;
        guiGraphics.pose().pushPose();
        float textX = (screenX + 5) / scale;
        float textY = (screenY + (h - 8 * scale) / 2) / scale;
        guiGraphics.pose().scale(scale, scale, 1.0f);
        guiGraphics.drawString(this.font, text, (int)textX, (int)textY, color, false);
        guiGraphics.pose().popPose();
    }

    private void drawBootingText(GuiGraphics guiGraphics, int x, int y, int w, int h) {
        long time = System.currentTimeMillis() / 500;
        String dots = ".".repeat((int) (time % 4));
        drawCenteredText(guiGraphics, "SYSTEM BOOT" + dots, 0xFFFFFF, x, y, w, h);
    }

    private void drawStatusText(GuiGraphics guiGraphics, int x, int y, int w, int h, int status, int energy, int maxEnergy) {
        String msg;
        int color;
        if (status == 1) { msg = "SYSTEM ONLINE"; color = 0xCCFFCC; }
        else if (status >= 200 && status <= 300) { msg = "REPAIRING: " + (status - 200) + "%"; color = 0xFFFDD0; }
        else if (status >= 1000) { msg = "RESPAWN: " + ((status - 1000) / 20) + "s"; color = 0xFFCCCC; }
        else {
            if (energy < maxEnergy) { msg = "CHARGING..."; color = 0xFFE4B5; }
            else { msg = "STANDBY MODE"; color = 0xE0E0E0; }
        }
        drawCenteredText(guiGraphics, msg, color, x, y, w, h);
    }

    private void playClickSound() {
        net.minecraft.client.Minecraft.getInstance().getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
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
        if (isHovering(180, 27, 16, 52, mouseX, mouseY)) {
            int energy = this.menu.getDataSlot(0);
            int maxEnergy = this.menu.getDataSlot(1);
            guiGraphics.renderTooltip(this.font, Component.literal(String.format("%d / %d HE", energy, maxEnergy)), mouseX, mouseY);
        }
    }


}
