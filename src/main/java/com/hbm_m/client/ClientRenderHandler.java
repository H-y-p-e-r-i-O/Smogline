package com.hbm_m.client;

// Этот класс отвечает за подсветку блоков, если те мешают установке многоблочной структуры
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.item.AmmoTurretItem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import software.bernie.geckolib.animatable.GeoItem;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientRenderHandler {

    private static final Map<BlockPos, Long> highlightedBlocks = new HashMap<>();

    private static class CustomRenderTypes extends RenderType {
        private CustomRenderTypes(String s, VertexFormat v, VertexFormat.Mode m, int i, boolean b, boolean b2, Runnable r, Runnable r2) { super(s, v, m, i, b, b2, r, r2); }

        public static final RenderType HIGHLIGHT_BOX_FILL = create("highlight_box_fill",
                DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(POSITION_COLOR_SHADER)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(NO_DEPTH_TEST)
                        .setCullState(NO_CULL)
                        .setLightmapState(NO_LIGHTMAP)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false));
    }

    public static void highlightBlocks(List<BlockPos> positions) {
        long duration = ModClothConfig.get().obstructionHighlightDuration * 1000L;
        long expiryTime = System.currentTimeMillis() + duration;
        highlightedBlocks.clear(); // Очищаем старые, чтобы не было дубликатов
        for (BlockPos pos : positions) {
            highlightedBlocks.put(pos, expiryTime);
        }
    }

    // === Вставь в ClientRenderHandler.java ===

// Добавь/проверь импорты:
// import net.minecraftforge.client.event.InputEvent;
// import org.lwjgl.glfw.GLFW; // Обязательно этот импорт для констант кнопок!

    @SubscribeEvent
    public static void onMouseClick(InputEvent.MouseButton.Pre event) {
        if (event.getAction() == GLFW.GLFW_PRESS && event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {

            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.screen == null) {

                ItemStack stack = mc.player.getMainHandItem();

                // ✅ Добавлена проверка на второй предмет (AmmoPiercingItem)
                if (stack.getItem() instanceof AmmoTurretItem) {

                    if (!mc.player.getCooldowns().isOnCooldown(stack.getItem())) {
                        CompoundTag tag = stack.getOrCreateTag();
                        long instanceId;
                        if (tag.contains("GeckoLibID")) {
                            instanceId = tag.getLong("GeckoLibID");
                        } else {
                            instanceId = java.util.UUID.randomUUID().getMostSignificantBits();
                            tag.putLong("GeckoLibID", instanceId);
                        }

                        // ✅ Кастим к интерфейсу GeoItem, так как он есть у обоих предметов
                        if (stack.getItem() instanceof GeoItem geoItem) {
                            geoItem.triggerAnim(mc.player, instanceId, "controller", "flip");
                        }

                        mc.player.getCooldowns().addCooldown(stack.getItem(), 60);

                        mc.player.attackAnim = 0;
                        mc.player.swinging = false;
                    }

                    event.setCanceled(true);
                }
            }
        }
    }



    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() == VanillaGuiOverlay.CROSSHAIR.type()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.getMainHandItem().getItem() instanceof com.hbm_m.item.MachineGunItem) {
                event.setCanceled(true);

                GuiGraphics graphics = event.getGuiGraphics();
                int width = event.getWindow().getGuiScaledWidth();
                int height = event.getWindow().getGuiScaledHeight();
                int x = width / 2;
                int y = height / 2;

                // Было: 2x2 пикселя, непрозрачный белый
                // graphics.fill(x - 1, y - 1, x + 1, y + 1, 0xFFFFFFFF);

                // Стало: 1x1 пиксель, 70% непрозрачности (полупрозрачный)
                // 0xB2FFFFFF: B2 = 178 (из 255) альфа, FFFFFF = белый
                graphics.fill(x, y, x + 1, y + 1, 0x80FFFFFF);
            }
        }
    }
    @SubscribeEvent
    public static void onRenderWorldLast(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || highlightedBlocks.isEmpty()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Minecraft mc = Minecraft.getInstance();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        long currentTime = System.currentTimeMillis();
        VertexConsumer fillConsumer = mc.renderBuffers().bufferSource().getBuffer(CustomRenderTypes.HIGHLIGHT_BOX_FILL);
        Color color = Color.RED;
        float alpha = ModClothConfig.get().obstructionHighlightAlpha / 100.0f;

        if (alpha <= 0) {
            highlightedBlocks.clear();
            return;
        }

        poseStack.pushPose();
        // Переводим в camera-relative координаты один раз
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        highlightedBlocks.entrySet().removeIf(entry -> {
            BlockPos pos = entry.getKey();
            if (currentTime > entry.getValue()) {
                return true;
            }

            // Проверяем соседей
            boolean drawDown = !highlightedBlocks.containsKey(pos.below());
            boolean drawUp = !highlightedBlocks.containsKey(pos.above());
            boolean drawNorth = !highlightedBlocks.containsKey(pos.north());
            boolean drawSouth = !highlightedBlocks.containsKey(pos.south());
            boolean drawWest = !highlightedBlocks.containsKey(pos.west());
            boolean drawEast = !highlightedBlocks.containsKey(pos.east());

            // Раздуваем в camera-relative пространстве
            AABB boundingBox = new AABB(pos).inflate(0.002D);
            
            renderFilledBox(poseStack, fillConsumer, boundingBox, color, alpha, 
                        drawDown, drawUp, drawNorth, drawSouth, drawWest, drawEast);
            return false;
        });

        poseStack.popPose();
        mc.renderBuffers().bufferSource().endBatch(CustomRenderTypes.HIGHLIGHT_BOX_FILL);
    }

    // Рендерим только те грани куба, которые не примыкают к другим подсвеченным блокам.
    private static void renderFilledBox(PoseStack poseStack, VertexConsumer consumer, AABB box, Color color, float alpha,
                                        boolean drawDown, boolean drawUp, boolean drawNorth, boolean drawSouth, boolean drawWest, boolean drawEast) {
        Matrix4f matrix = poseStack.last().pose();
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        
        float minX = (float)box.minX; float minY = (float)box.minY; float minZ = (float)box.minZ;
        float maxX = (float)box.maxX; float maxY = (float)box.maxY; float maxZ = (float)box.maxZ;
        
        // Низ (Y-)
        if (drawDown) {
            consumer.vertex(matrix, minX, minY, minZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, maxX, minY, minZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, minX, minY, maxZ).color(r, g, b, alpha).endVertex();
        }
        // Верх (Y+)
        if (drawUp) {
            consumer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, minX, maxY, minZ).color(r, g, b, alpha).endVertex();
        }
        // Север (Z-)
        if (drawNorth) {
            consumer.vertex(matrix, minX, minY, minZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, minX, maxY, minZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, maxX, minY, minZ).color(r, g, b, alpha).endVertex();
        }
        // Юг (Z+)
        if (drawSouth) {
            consumer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, minX, minY, maxZ).color(r, g, b, alpha).endVertex();
        }
        // Запад (X-)
        if (drawWest) {
            consumer.vertex(matrix, minX, minY, maxZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, minX, maxY, minZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, minX, minY, minZ).color(r, g, b, alpha).endVertex();
        }
        // Восток (X+)
        if (drawEast) {
            consumer.vertex(matrix, maxX, minY, minZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, alpha).endVertex();
        }
    }
}