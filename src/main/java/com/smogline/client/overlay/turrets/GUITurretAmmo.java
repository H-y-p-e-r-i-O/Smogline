package com.smogline.client.overlay.turrets;

import com.smogline.block.entity.custom.TurretLightPlacerBlockEntity;
import com.smogline.item.custom.weapons.turrets.TurretChipItem;
import com.smogline.menu.TurretLightMenu;
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

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("smogline", "textures/gui/weapon/turret_light.png");

    // Состояния
    private static final int STATE_NORMAL = 0;
    private static final int STATE_CHIP_MENU = 1;
    private static final int STATE_CHIP_LIST = 2;
    private static final int STATE_ADD_INPUT = 3;  // Ввод ника
    private static final int STATE_RESULT_MSG = 4; // Сообщение (404 / Success)

    private int uiState = STATE_NORMAL;
    private int selectedUserIndex = 0;

    // Ввод текста
    private String inputString = "";
    private int cursorTimer = 0;

    // Результат
    private String resultMessage = "";
    private int resultColor = 0xFFFFFF;
    private int resultDuration = 0; // Таймер показа сообщения

    // Таймеры кнопок
    private int timerPlus = 0, timerMinus = 0, timerCheck = 0, timerLeft = 0, timerRight = 0;
    private static final int PRESS_DURATION = 10;

    public GUITurretAmmo(TurretLightMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
        this.imageWidth = 201;
        this.imageHeight = 188;
    }

    // --- ОБРАБОТКА ОТВЕТА ОТ СЕРВЕРА ---
    public void handleFeedback(boolean success) {
        this.uiState = STATE_RESULT_MSG;
        this.resultDuration = 40; // 2 секунды (20 тиков * 2)
        if (success) {
            this.resultMessage = "SUCCESS";
            this.resultColor = 0x55FF55; // Зеленый
            // Если успех, сервер уже добавил в NBT, так что при выходе мы увидим нового юзера
        } else {
            this.resultMessage = "ERROR 404";
            this.resultColor = 0xFF5555; // Красный
        }
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Данные
        int energy = this.menu.getDataSlot(0);
        int maxEnergy = this.menu.getDataSlot(1);
        int status = this.menu.getDataSlot(2);
        boolean isSwitchedOn = this.menu.getDataSlot(3) == 1;
        int bootTimer = this.menu.getDataSlot(4);

        // --- АНИМАЦИЯ КНОПОК ---
        if (isSwitchedOn) guiGraphics.blit(TEXTURE, x + 10, y + 62, 204, 103, 10, 32); // Power
        if (timerPlus > 0) { timerPlus--; guiGraphics.blit(TEXTURE, x + 39, y + 62, 221, 171, 15, 15); } // Plus
        if (timerMinus > 0) { timerMinus--; guiGraphics.blit(TEXTURE, x + 56, y + 62, 204, 137, 15, 15); } // Minus
        if (timerCheck > 0) { timerCheck--; guiGraphics.blit(TEXTURE, x + 22, y + 62, 204, 171, 15, 15); } // Check
        if (timerLeft > 0) { timerLeft--; guiGraphics.blit(TEXTURE, x + 73, y + 62, 221, 137, 15, 15); } // Left
        if (timerRight > 0) { timerRight--; guiGraphics.blit(TEXTURE, x + 90, y + 62, 221, 120, 15, 15); } // Right

        // Кнопка MENU (+) переключатель
        if (this.uiState != STATE_NORMAL) { // Визуально она "нажата" если мы в меню? Или нет, оставим просто анимацию клика
            // Если нужно, чтобы она горела пока мы в меню, добавь условие
        }
        // Кнопка "открытия меню" (квадрат с плюсом)
        // В твоем описании это 73,79. У меня таймер для нее timerMenuToggle (в прошлом коде было timerPlus, но сейчас Plus это добавление)
        // Давай добавим отдельный таймер для кнопки открытия меню, если она нужна.
        // Но судя по прошлому промпту, ты используешь "квадрат с плюсом" (73, 79) для входа.
        // А новые "плюс" и "минус" это 39,62 и 56,62.

        // Кнопка MENU (73, 79) - я использую отдельный таймер для визуализации
        if (menuToggleTimer > 0) {
            menuToggleTimer--;
            guiGraphics.blit(TEXTURE, x + 73, y + 79, 221, 154, 15, 15);
        }

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
                if (uiState != STATE_NORMAL && !hasChip()) uiState = STATE_NORMAL;

                // ЛОГИКА ОТРИСОВКИ СОСТОЯНИЙ
                switch (uiState) {
                    case STATE_CHIP_MENU:
                        drawCenteredText(guiGraphics, "CHIP CONTROL", 0xFFFFFF, x + 10, y + 32, 95, 16);
                        break;

                    case STATE_CHIP_LIST:
                        drawChipUserList(guiGraphics, x + 10, y + 32, 95, 16);
                        break;

                    case STATE_ADD_INPUT:
                        // Мигающий курсор
                        cursorTimer++;
                        String cursor = (cursorTimer / 10 % 2 == 0) ? "_" : "";
                        String display = inputString + cursor;
                        // Ограничим длину отображения
                        if (display.length() > 14) display = display.substring(display.length() - 14);
                        drawCenteredText(guiGraphics, display, 0xFFFF00, x + 10, y + 32, 95, 16);
                        break;

                    case STATE_RESULT_MSG:
                        if (resultDuration > 0) {
                            resultDuration--;
                            drawCenteredText(guiGraphics, resultMessage, resultColor, x + 10, y + 32, 95, 16);
                        } else {
                            // Таймер кончился
                            if (resultMessage.equals("SUCCESS")) {
                                uiState = STATE_CHIP_LIST; // Возвращаемся в список
                                // Нужно бы поставить индекс на последнего, но 0 тоже норм
                            } else {
                                uiState = STATE_ADD_INPUT; // Возвращаем вводить имя заново
                            }
                        }
                        break;

                    default:
                        drawStatusText(guiGraphics, x + 10, y + 32, 95, 16, status, energy, maxEnergy);
                        break;
                }
            }
        } else {
            uiState = STATE_NORMAL;
        }
    }

    private int menuToggleTimer = 0; // Для кнопки 73, 79

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        double relX = mouseX - x;
        double relY = mouseY - y;

        if (button == 0) {
            // Power (10, 62)
            if (relX >= 10 && relX < 20 && relY >= 62 && relY < 94) {
                com.smogline.network.ModPacketHandler.INSTANCE.send(
                        net.minecraftforge.network.PacketDistributor.SERVER.noArg(),
                        new com.smogline.network.packet.PacketToggleTurret(this.menu.getPos()));
                playClickSound();
                return true;
            }

            if (hasChip()) {
                // КНОПКА "КВАДРАТ С ПЛЮСОМ" (Вход в меню) (73, 79)
                if (relX >= 73 && relX < 88 && relY >= 79 && relY < 94) {
                    menuToggleTimer = PRESS_DURATION;
                    playClickSound();
                    if (uiState == STATE_NORMAL) uiState = STATE_CHIP_MENU;
                    else uiState = STATE_NORMAL;
                    return true;
                }

                // Кнопки управления (только если не в обычном режиме)
                if (uiState != STATE_NORMAL && uiState != STATE_RESULT_MSG) {

                    // ГАЛОЧКА (22, 62)
                    if (relX >= 22 && relX < 37 && relY >= 62 && relY < 77) {
                        timerCheck = PRESS_DURATION;
                        playClickSound();

                        if (uiState == STATE_CHIP_MENU) {
                            uiState = STATE_CHIP_LIST; // Входим
                            selectedUserIndex = 0;
                        } else if (uiState == STATE_ADD_INPUT) {
                            // ОТПРАВКА НА СЕРВЕР (Добавить)
                            if (!inputString.isEmpty()) {
                                com.smogline.network.ModPacketHandler.INSTANCE.send(
                                        net.minecraftforge.network.PacketDistributor.SERVER.noArg(),
                                        new com.smogline.network.packet.PacketModifyTurretChip(1, inputString)
                                );
                                // Не меняем стейт тут, ждем пакета ответа
                            }
                        }
                        return true;
                    }

                    // ПЛЮС (39, 62) -> РЕЖИМ ДОБАВЛЕНИЯ
                    if (relX >= 39 && relX < 54 && relY >= 62 && relY < 77) {
                        timerPlus = PRESS_DURATION;
                        playClickSound();
                        // Можно нажать и из меню, и из списка
                        uiState = STATE_ADD_INPUT;
                        inputString = "";
                        return true;
                    }

                    // МИНУС (56, 62) -> УДАЛЕНИЕ
                    if (relX >= 56 && relX < 71 && relY >= 62 && relY < 77) {
                        timerMinus = PRESS_DURATION;
                        playClickSound();

                        if (uiState == STATE_CHIP_LIST) {
                            // Удаляем текущего выбранного
                            com.smogline.network.ModPacketHandler.INSTANCE.send(
                                    net.minecraftforge.network.PacketDistributor.SERVER.noArg(),
                                    new com.smogline.network.packet.PacketModifyTurretChip(0, String.valueOf(selectedUserIndex))
                            );
                            // Индекс можно уменьшить, если он был последним
                            if (selectedUserIndex > 0) selectedUserIndex--;
                        }
                        return true;
                    }

                    // СТРЕЛКИ (Навигация)
                    if (uiState == STATE_CHIP_LIST) {
                        if (relX >= 73 && relX < 88 && relY >= 62 && relY < 77) { // Left
                            timerLeft = PRESS_DURATION; playClickSound(); selectedUserIndex--; return true;
                        }
                        if (relX >= 90 && relX < 105 && relY >= 62 && relY < 77) { // Right
                            timerRight = PRESS_DURATION; playClickSound(); selectedUserIndex++; return true;
                        }
                    }
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

        String textToShow;
        int color = 0x00FFFF;

        if (names.isEmpty()) {
            textToShow = "EMPTY LIST";
            color = 0xAAAAAA;
        } else {
            // Коррекция индекса
            if (selectedUserIndex < 0) selectedUserIndex = names.size() - 1;
            if (selectedUserIndex >= names.size()) selectedUserIndex = 0;
            textToShow = (selectedUserIndex + 1) + "/" + names.size() + " " + names.get(selectedUserIndex);
        }
        drawCenteredText(guiGraphics, textToShow, color, screenX, screenY, w, h);
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
        renderTooltip(guiGraphics, mouseX, mouseY); // Тултипы предметов

        // --- ВОЗВРАЩАЕМ ТУЛТИП ЭНЕРГИИ ---
        // Проверяем наведение на координаты полоски (180, 27, ширина 16, высота 52)
        if (isHovering(180, 27, 16, 52, mouseX, mouseY)) {
            int energy = this.menu.getDataSlot(0);
            int maxEnergy = this.menu.getDataSlot(1);
            guiGraphics.renderTooltip(this.font,
                    Component.literal(String.format("%d / %d HE", energy, maxEnergy)),
                    mouseX, mouseY);
        }
    }


}
