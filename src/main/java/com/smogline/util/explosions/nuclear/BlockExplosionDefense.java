package com.smogline.util.explosions.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.smogline.block.ModBlocks;

/**
 * 💥 СИСТЕМА ЗАЩИТЫ БЛОКОВ С КОЭФФИЦИЕНТОМ ПРОБИТИЯ v3.0
 *
 * ✅ Логичные коэффициенты по материалам:
 * ✅ Бетон: 250
 * ✅ Бетонные кирпичи: 350
 * ✅ Метеорит: 500
 * ✅ Кафель, мозаика: 180-220
 * ✅ Специальный бетон (усиленный): 400-600
 * ✅ Тултип с золотым цветом взрывоустойчивости (ИНТЕГРИРОВАН)
 */

@Mod.EventBusSubscriber(modid = "smogline", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BlockExplosionDefense {

    /**
     * ✅ ГЛАВНЫЙ МЕТОД: Получить коэффициент защиты блока
     * Основано на взрывоустойчивости и типе материала
     */
    public static float getBlockDefenseValue(ServerLevel level, BlockPos pos, BlockState state) {
        if (state == null) return 0.0F;

        // Бедрок - абсолютная защита
        if (state.is(Blocks.BEDROCK)) {
            return 10_000.0F;
        }

        if (level != null && pos != null && state.getDestroySpeed(level, pos) < 0) {
            return 10_000.0F;
        }

        Block block = state.getBlock();

        // === ЯВНО СУПЕР-ПРОЧНЫЕ БЛОКИ ===
        if (block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN) {
            return 250.0F;
        }

        if (block == Blocks.ANCIENT_DEBRIS) {
            return 400.0F;
        }

        if (block == Blocks.NETHERITE_BLOCK) {
            return 300.0F;
        }

        // ========== ПОЛЬЗОВАТЕЛЬСКИЕ БЛОКИ ==========

        // === БЕТОН (базовый) - 250 ===
        if (isConcreteBlock(block)) {
            return 250.0F;
        }

        // === МЕТЕОРИТ - 500 ===
        if (isMeteorBlock(block)) {
            return 500.0F;
        }

        // === БЕТОННЫЕ КИРПИЧИ - 350 ===
        if (isBrickBlock(block)) {
            return 350.0F;
        }

        // === КАФЕЛЬ И МОЗАИКА - 200 ===
        if (isTileBlock(block)) {
            return 200.0F;
        }

        // === СПЕЦИАЛЬНЫЕ МАТЕРИАЛЫ ===
        if (isDepthBlock(block)) {
            return 280.0F;
        }

        if (isGneissBlock(block)) {
            return 260.0F;
        }

        if (isBasaltBlock(block)) {
            return 240.0F;
        }

        // === ЛЕСТНИЦЫ (половина защиты от базового блока) ===
        if (isStairsBlock(block)) {
            return 150.0F;
        }

        // === СТАНДАРТНАЯ КОНВЕРСИЯ ВЗРЫВОУСТОЙЧИВОСТИ ===
        float blastRes = getBlastResistance(state);

        // Диапазон 0-50: защита 5-10 (линейно)
        if (blastRes <= 50.0F) {
            float t = blastRes / 50.0F;
            return 5.0F + t * 5.0F;
        }

        // Диапазон 50-250: защита 25
        if (blastRes <= 250.0F) {
            return 25.0F;
        }

        // Диапазон 250-1000: защита 50
        if (blastRes <= 1000.0F) {
            return 50.0F;
        }

        // 1000+: защита 100
        return 100.0F;
    }

    // ========== ОБРАБОТЧИК ТУЛТИПОВ (EventHandler встроен в класс) ==========

    /**
     * ✅ ОБРАБОТЧИК СОБЫТИЙ ТУЛТИПОВ
     * Автоматически добавляет информацию о взрывоустойчивости к блокам
     */
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        // Проверяем, это ли BlockItem (блок в виде предмета)
        if (!(stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem)) {
            return;
        }

        // Получаем блок из предмета
        var block = blockItem.getBlock();

        // Проверяем, это ли один из наших модульных блоков
        if (isModularBlock(block)) {
            // Определяем коэффициент защиты по типу блока
            float defenseValue = getDefenseValueForBlock(block);

            // Добавляем строку в тултип золотым цветом
            if (defenseValue >= 10_000.0F) {
                event.getToolTip().add(Component.literal("§6Взрывоустойчивость: §cНЕДОЕМИЕ§r"));
            } else if (defenseValue > 0) {
                event.getToolTip().add(Component.literal(
                        String.format("§6Взрывоустойчивость: §e%.0f§r", defenseValue)
                ));
            }
        }
    }

    /**
     * ✅ Проверка: это ли один из наших модульных блоков
     */
    private static boolean isModularBlock(Block block) {
        return isConcreteBlock(block) ||
                isMeteorBlock(block) ||
                isBrickBlock(block) ||
                isTileBlock(block) ||
                isDepthBlock(block) ||
                isGneissBlock(block) ||
                isBasaltBlock(block) ||
                isStairsBlock(block);
    }

    /**
     * ✅ Получить защиту по типу блока
     */
    private static float getDefenseValueForBlock(Block block) {
        if (isConcreteBlock(block)) return 250.0F;
        if (isMeteorBlock(block)) return 500.0F;
        if (isBrickBlock(block)) return 350.0F;
        if (isTileBlock(block)) return 200.0F;
        if (isDepthBlock(block)) return 280.0F;
        if (isGneissBlock(block)) return 260.0F;
        if (isBasaltBlock(block)) return 240.0F;
        if (isStairsBlock(block)) return 150.0F;

        return 0.0F;
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ КЛАССИФИКАЦИИ ==========

    /**
     * ✅ Базовые БЕТОННЫЕ блоки - 250
     */
    private static boolean isConcreteBlock(Block block) {
        return block == Blocks.SANDSTONE;
    }

    /**
     * ✅ УСИЛЕННЫЙ БЕТОН - 400
     */
    private static boolean isSpecialConcreteBlock(Block block) {
        return block == Blocks.SKELETON_SKULL;
    }

    /**
     * ✅ МЕТЕОРИТ - 500
     */
    private static boolean isMeteorBlock(Block block) {
        return block == Blocks.JUKEBOX;
    }

    /**
     * ✅ БЕТОННЫЕ КИРПИЧИ - 350
     */
    private static boolean isBrickBlock(Block block) {
        return block == Blocks.STRIPPED_BAMBOO_BLOCK;
    }

    /**
     * ✅ КАФЕЛЬ И МОЗАИКА - 200
     */
    private static boolean isTileBlock(Block block) {
        return block == Blocks.SLIME_BLOCK;
    }

    /**
     * ✅ DEPTH МАТЕРИАЛЫ - 280
     */
    private static boolean isDepthBlock(Block block) {
        return block == Blocks.ACACIA_FENCE_GATE;
    }

    /**
     * ✅ ГНЕЙСС - 260
     */
    private static boolean isGneissBlock(Block block) {
        return block == Blocks.AMETHYST_BLOCK;
    }

    /**
     * ✅ БАЗАЛЬТ - 240
     */
    private static boolean isBasaltBlock(Block block) {
        return block == Blocks.BROWN_MUSHROOM_BLOCK;
    }

    /**
     * ✅ ЛЕСТНИЦЫ (STAIRS) - 150 (половина от базового)
     */
    private static boolean isStairsBlock(Block block) {
        return block == Blocks.BAMBOO_BLOCK;

    }

    /**
     * ✅ Получить взрывоустойчивость блока
     */
    public static float getBlastResistance(BlockState state) {
        if (state == null) return 0.0F;
        return state.getBlock().getExplosionResistance();
    }

    /**
     * ✅ Получить уровень защиты по диапазонам (для обратной совместимости)
     */
    public static int getDefenseLevelFromResistance(float blastRes) {
        if (blastRes < 0) return 15;
        if (blastRes < 1.0F) return 0;
        if (blastRes < 2.0F) return 1;
        if (blastRes < 5.0F) return 2;
        if (blastRes < 10.0F) return 3;
        if (blastRes < 20.0F) return 4;
        if (blastRes < 30.0F) return 5;
        if (blastRes < 50.0F) return 6;
        if (blastRes < 75.0F) return 7;
        if (blastRes < 100.0F) return 8;
        if (blastRes < 150.0F) return 9;
        if (blastRes < 250.0F) return 10;
        if (blastRes < 500.0F) return 11;
        if (blastRes < 1000.0F) return 12;
        if (blastRes < 5000.0F) return 13;
        return 14;
    }
}
