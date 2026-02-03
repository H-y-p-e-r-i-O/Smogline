package com.smogline.datagen;

// Провайдер генерации локализаций (переводов) для мода.

import com.smogline.block.ModBlocks;
import com.smogline.item.tags_and_tiers.ModIngots;
import com.smogline.item.ModItems;
import com.smogline.item.tags_and_tiers.ModPowders;
import com.smogline.lib.RefStrings;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.registries.RegistryObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


public class ModLanguageProvider extends LanguageProvider {
    // 1. Создаем НАШЕ СОБСТВЕННОЕ поле для хранения языка
    private final String locale;

    public ModLanguageProvider(PackOutput output, String locale) {
        super(output, RefStrings.MODID, locale);
        // 2. Сохраняем язык в наше поле при создании объекта
        this.locale = locale;
    }

    private void addIngotPowderTranslations(Set<ResourceLocation> translatedPowders) {
        for (ModIngots ingot : ModIngots.values()) {
            if (ModItems.getPowder(ingot) != null) {
                var powder = ModItems.getPowder(ingot);
                if (!translatedPowders.contains(powder.getId())) {
                    add(powder.get(), buildPowderName(ingot, false));
                }
            }
            ModItems.getTinyPowder(ingot).ifPresent(tiny ->
                    add(tiny.get(), buildPowderName(ingot, true)));
        }

    }

    private String buildPowderName(ModIngots ingot, boolean tiny) {
        String base = ingot.getTranslation(this.locale);
        if (base == null || base.isBlank()) {
            base = formatName(ingot.getName());
        }

        String result = base;
        if ("ru_ru".equals(this.locale)) {
            String replaced = result.replace("Слиток", "Порошок").replace("слиток", "порошок");
            if (replaced.equals(result)) {
                replaced = "Порошок " + result;
            }
            result = replaced.trim();
            if (tiny) {
                result = "Малая кучка " + result;
            }
        } else {
            String replaced = result.replace("Ingot", "Powder").replace("ingot", "powder");
            if (replaced.equals(result)) {
                replaced = result + " Powder";
            }
            result = replaced.trim();
            if (tiny) {
                result = "Tiny " + result;
            }
        }

        return result;
    }

    private String formatName(String name) {
        return Arrays.stream(name.replace('.', '_').split("_"))
                .filter(part -> !part.isEmpty())
                .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1))
                .collect(Collectors.joining(" "));
    }


    // Метод формирования имени блока с переводом
    private String buildBlockName(ModIngots ingot) {
        String base = ingot.getTranslation(this.locale);
        if (base == null || base.isBlank()) {
            base = formatName(ingot.getName());
        }

        if ("ru_ru".equals(this.locale)) {
            // Для русского языка заменяем "слиток" на "блок", либо добавляем приставку "Блок"
            String replaced = base.replace("Слиток", "Блок").replace("слиток", "блок");
            if (replaced.equals(base)) {
                replaced = "Блок " + base;
            }
            return replaced.trim();
        } else {
            // Для английского - добавляем приставку "Block" или заменяем "Ingot" на "Block"
            String replaced = base.replace("Ingot", "Block").replace("ingot", "block");
            if (replaced.equals(base)) {
                replaced = base + " Block";
            }
            return replaced.trim();
        }
    }
    @Override
    protected void addTranslations() {
        // АВТОМАТИЧЕСКАЯ ЛОКАЛИЗАЦИЯ СЛИТКОВ
        for (ModIngots ingot : ModIngots.values()) {
            RegistryObject<Item> ingotItem = ModItems.getIngot(ingot);
            if (ingotItem != null && ingotItem.isPresent()) {
                String translation = ingot.getTranslation(this.locale);
                if (translation != null) {
                    add(ingotItem.get(), translation);
                }
            }
        }

        Set<ResourceLocation> translatedPowders = new HashSet<>();

        // АВТОМАТИЧЕСКАЯ ЛОКАЛИЗАЦИЯ ПОРОШКОВ
        for (ModPowders powders : ModPowders.values()) {
            RegistryObject<Item> powderItem = ModItems.getPowders(powders);
            if (powderItem != null && powderItem.isPresent()) {
                String translation = powders.getTranslation(this.locale);
                if (translation != null) {
                    add(powderItem.get(), translation);
                    translatedPowders.add(powderItem.getId());
                }
            }
        }

        // ДОБАВЛЕНИЕ ЛОКАЛИЗАЦИИ ДЛЯ ПОРОШКОВ ИЗ СЛИТКОВ
        for (ModIngots ingot : ModIngots.values()) {
            RegistryObject<Item> powder = ModItems.getPowder(ingot);
            if (powder != null && powder.isPresent() && !translatedPowders.contains(powder.getId())) {
                add(powder.get(), buildPowderName(ingot, false));
            }
            ModItems.getTinyPowder(ingot).ifPresent(tiny -> {
                if (tiny != null && tiny.isPresent()) {
                    add(tiny.get(), buildPowderName(ingot, true));
                }
            });
        }



    // ЯВНАЯ ЛОКАЛИЗАЦИЯ ДЛЯ ОСТАЛЬНЫХ КЛЮЧЕЙ
        switch (this.locale) {
            case "ru_ru":
                // КРЕАТИВНЫЕ ВКЛАДКИ
                add("itemGroup.smogline.ntm_resources_tab", "Слитки и ресурсы NTM");
                add("itemGroup.smogline.ntm_fuel_tab", "Топливо и элементы механизмов NTM");
                add("itemGroup.smogline.ntm_templates_tab", "Шаблоны и штампы NTM");
                add("itemGroup.smogline.ntm_ores_tab", "Руды и блоки NTM");
                add("itemGroup.smogline.ntm_building_tab", "Строительные блоки NTM");
                add("itemGroup.smogline.ntm_machines_tab", "Механизмы и станки NTM");
                add("itemGroup.smogline.ntm_instruments_tab", "Броня и инструменты NTM");
                add("itemGroup.smogline.ntm_spareparts_tab", "Запчасти NTM");
                add("itemGroup.smogline.ntm_bombs_tab", "Бомбы NTM");
                add("itemGroup.smogline.ntm_missiles_tab", "Ракеты и спутники NTM");
                add("itemGroup.smogline.ntm_weapons_tab", "Оружие и турели NTM");
                add("itemGroup.smogline.ntm_consumables_tab", "Расходники и снаряжение NTM");
                
                // СНАРЯГА
                add("item.smogline.alloy_sword", "Меч из продвинутого сплава");
                add("item.smogline.alloy_pickaxe", "Кирка из продвинутого сплава");
                add("item.smogline.alloy_axe", "Топор из продвинутого сплава");
                add("item.smogline.alloy_hoe", "Мотыга из продвинутого сплава");
                add("item.smogline.alloy_shovel", "Лопата из продвинутого сплава");

                add("item.smogline.steel_sword", "Стальной меч");
                add("item.smogline.steel_pickaxe", "Стальная кирка");
                add("item.smogline.steel_axe", "Стальной топор");
                add("item.smogline.steel_hoe", "Стальная мотыга");
                add("item.smogline.steel_shovel", "Стальная лопата");

                add("item.smogline.titanium_sword", "Титановый меч");
                add("item.smogline.titanium_pickaxe", "Титановая кирка");
                add("item.smogline.titanium_axe", "Титановый топор");
                add("item.smogline.titanium_hoe", "Титановая мотыга");
                add("item.smogline.titanium_shovel", "Титановая лопата");

                add("item.smogline.starmetal_sword", "Меч из звёздного металла");
                add("item.smogline.starmetal_pickaxe", "Кирка из звёздного металла");
                add("item.smogline.starmetal_axe", "Топор из звёздного металла");
                add("item.smogline.starmetal_hoe", "Мотыга из звёздного металла");
                add("item.smogline.starmetal_shovel", "Лопата из звёздного металла");

                add("gui.smogline.energy", "Энергия: %s/%s HE");
                add("gui.smogline.shredder.blade_warning.title", "Нет лезвий!");
                add("gui.smogline.shredder.blade_warning.desc", "Установите или отремонтируйте лезвия шреддера.");
                // БРОНЯ
                add("item.smogline.alloy_helmet", "Шлем из продвинутого сплава");
                add("item.smogline.alloy_chestplate", "Нагрудник из продвинутого сплава");
                add("item.smogline.alloy_leggings", "Поножи из продвинутого сплава");
                add("item.smogline.alloy_boots", "Ботинки из продвинутого сплава");

                add("item.smogline.cobalt_helmet", "Кобальтовый шлем");
                add("item.smogline.cobalt_chestplate", "Кобальтовый нагрудник");
                add("item.smogline.cobalt_leggings", "Кобальтовые поножи");
                add("item.smogline.cobalt_boots", "Кобальтовые ботинки");

                add("item.smogline.titanium_helmet", "Титановый шлем");
                add("item.smogline.titanium_chestplate", "Титановый нагрудник");
                add("item.smogline.titanium_leggings", "Титановые поножи");
                add("item.smogline.titanium_boots", "Титановые ботинки");

                add("item.smogline.security_helmet", "Шлем охранника");
                add("item.smogline.security_chestplate", "Нагрудник охранника");
                add("item.smogline.security_leggings", "Поножи охранника");
                add("item.smogline.security_boots", "Ботинки охранника");

                add("item.smogline.ajr_helmet", "Шлем Стальных Рейнджеров");
                add("item.smogline.ajr_chestplate", "Нагрудник Стальных Рейнджеров");
                add("item.smogline.ajr_leggings", "Поножи Стальных Рейнджеров");
                add("item.smogline.ajr_boots", "Ботинки Стальных Рейнджеров");

                add("item.smogline.steel_helmet", "Стальной шлем");
                add("item.smogline.steel_chestplate", "Стальной нагрудник");
                add("item.smogline.steel_leggings", "Стальные поножи");
                add("item.smogline.steel_boots", "Стальные ботинки");

                add("item.smogline.asbestos_helmet", "Огнезащитный шлем");
                add("item.smogline.asbestos_chestplate", "Огнезащитный нагрудник");
                add("item.smogline.asbestos_leggings", "Огнезащитные поножи");
                add("item.smogline.asbestos_boots", "Огнезащитные ботинки");

                add("item.smogline.hazmat_helmet", "Защитный шлем");
                add("item.smogline.hazmat_chestplate", "Защитный нагрудник");
                add("item.smogline.hazmat_leggings", "Защитные поножи");
                add("item.smogline.hazmat_boots", "Защитные ботинки");

                add("item.smogline.liquidator_helmet", "Шлем костюма Ликвидатора");
                add("item.smogline.liquidator_chestplate", "Нагрудник костюма Ликвидатора");
                add("item.smogline.liquidator_leggings", "Поножи костюма Ликвидатора");
                add("item.smogline.liquidator_boots", "Ботинки костюма Ликвидатора");

                add("item.smogline.paa_helmet", "Боевой защитный шлем PaA");
                add("item.smogline.paa_chestplate", "Защищающая нагрудная пластина из PaA");
                add("item.smogline.paa_leggings", "Укреплённые поножи из PaA");
                add("item.smogline.paa_boots", "''Старые добрые ботинки'' из PaA");

                add("item.smogline.starmetal_helmet", "Шлем из звёздного металла");
                add("item.smogline.starmetal_chestplate", "Нагрудник из звёздного металла");
                add("item.smogline.starmetal_leggings", "Поножи из звёздного металла");
                add("item.smogline.starmetal_boots", "Ботинки из звёздного металла");

                add("item.smogline.geiger_counter", "Счетчик Гейгера");
                add("item.smogline.dosimeter", "Дозиметр");
                add("item.smogline.battery_creative", "Бесконечная батарейка");
                add("tooltip.smogline.creative_battery_desc","Предоставляет бесконечное количество энергии");
                add("tooltip.smogline.creative_battery_flavor","Бесконечность — не предел!!");
                // ПРЕДМЕТЫ
                add(ModItems.BATTERY_POTATO.get(), "Картофельная батарейка");
                add(ModItems.BATTERY.get(), "Батарейка");
                add(ModItems.BATTERY_RED_CELL.get(), "Красная энергоячейка");
                add(ModItems.BATTERY_RED_CELL_6.get(), "Красная энергоячейка x6");
                add(ModItems.BATTERY_RED_CELL_24.get(), "Красная энергоячейка x24");
                add(ModItems.BATTERY_ADVANCED.get(), "Продвинутая батарея");
                add(ModItems.BATTERY_ADVANCED_CELL.get(), "Продвинутая энергоячейка");
                add(ModItems.BATTERY_ADVANCED_CELL_4.get(), "Продвинутая энергоячейка x4");
                add(ModItems.BATTERY_ADVANCED_CELL_12.get(), "Продвинутая энергоячейка x12");
                add(ModItems.BATTERY_LITHIUM.get(), "Литиевая батарея");
                add(ModItems.BATTERY_LITHIUM_CELL.get(), "Литиевая энергоячейка");
                add(ModItems.BATTERY_LITHIUM_CELL_3.get(), "Литиевая энергоячейка x3");
                add(ModItems.BATTERY_LITHIUM_CELL_6.get(), "Литиевая энергоячейка x6");
                add(ModItems.BATTERY_SCHRABIDIUM_CELL.get(), "Шрабидиевая энергоячейка");
                add(ModItems.BATTERY_SCHRABIDIUM_CELL_2.get(), "Шрабидиевая энергоячейка x2");
                add(ModItems.BATTERY_SCHRABIDIUM_CELL_4.get(), "Шрабидиевая энергоячейка x4");
                add(ModItems.BATTERY_SPARK.get(), "Спарк батарея");
                add(ModItems.BATTERY_TRIXITE.get(), "Нефритовый стержень спарк батарей оригинал");
                add(ModItems.BATTERY_SPARK_CELL_6.get(), "Спарк энергоячейка");
                add(ModItems.BATTERY_SPARK_CELL_25.get(), "Спарк магический аккумулятор");
                add(ModItems.BATTERY_SPARK_CELL_100.get(), "Спарк магический массив хранения энергии");
                add(ModItems.BATTERY_SPARK_CELL_1000.get(), "Спарк магическая масс-энергитическая пустота");
                add(ModItems.BATTERY_SPARK_CELL_2500.get(), "Спарк магическое море Дирака");
                add(ModItems.BATTERY_SPARK_CELL_10000.get(), "Устойчивый пространственно-временной спарк кристалл");
                add(ModItems.BATTERY_SPARK_CELL_POWER.get(), "Абсурдный физический спарк блок накопления энергии");

                add(ModItems.WIRE_RED_COPPER.get(), "Провод из красной меди");
                add(ModItems.WIRE_COPPER.get(), "Медный провод");
                add(ModItems.WIRE_ALUMINIUM.get(), "Алюминиевый провод");
                add(ModItems.WIRE_GOLD.get(), "Золотой провод");
                add(ModItems.WIRE_TUNGSTEN.get(), "Вольфрамовый провод");
                add(ModItems.WIRE_MAGNETIZED_TUNGSTEN.get(), "Провод из намагниченного вольфрама");
                add(ModItems.WIRE_FINE.get(), "Железный провод");
                add(ModItems.WIRE_CARBON.get(), "Провод из свинца");
                add(ModItems.WIRE_SCHRABIDIUM.get(), "Шрабидиевый провод");
                add(ModItems.WIRE_ADVANCED_ALLOY.get(), "Провод из продвинутого сплава");

                add(ModItems.BATTERY_SCHRABIDIUM.get(), "Шрабидиевая батарейка");



                add(ModItems.AMMO_TURRET_PIERCING.get(), "20мм турельный бронебойный снаряд");
                add(ModItems.AMMO_TURRET_FIRE.get(), "20мм турельный зажигательный снаряд");
                add(ModItems.AMMO_TURRET.get(), "20мм турельный снаряд");
                add(ModItems.AMMO_TURRET_RADIO.get(), "20мм турельный снаряд с радио-взрывателем");
                add(ModItems.MACHINEGUN.get(), "А.П.-17");
                add(ModBlocks.TURRET_LIGHT.get(), "Лёгкая десантная турель 'Нагваль'");


                add(ModItems.STAMP_STONE_FLAT.get(), "Плоский каменный штамп");
                add(ModItems.STAMP_STONE_PLATE.get(), "Каменный штамп пластины");
                add(ModItems.STAMP_STONE_WIRE.get(), "Каменный штамп провода");
                add(ModItems.STAMP_STONE_CIRCUIT.get(), "Каменный штамп чипа");
                add(ModItems.STAMP_IRON_FLAT.get(), "Плоский железный штамп");
                add(ModItems.STAMP_IRON_PLATE.get(), "Железный штамп пластины");
                add(ModItems.STAMP_IRON_WIRE.get(), "Железный штамп провода");
                add(ModItems.STAMP_IRON_CIRCUIT.get(), "Железный штамп чипа");
                add(ModItems.STAMP_STEEL_FLAT.get(), "Плоский стальной штамп");
                add(ModItems.STAMP_STEEL_PLATE.get(), "Стальной штамп пластины");
                add(ModItems.STAMP_STEEL_WIRE.get(), "Стальной штамп провода");
                add(ModItems.STAMP_STEEL_CIRCUIT.get(), "Стальной штамп чипа");
                add(ModItems.STAMP_TITANIUM_FLAT.get(), "Плоский титановый штамп");
                add(ModItems.STAMP_TITANIUM_PLATE.get(), "Титановый штамп пластины");
                add(ModItems.STAMP_TITANIUM_WIRE.get(), "Титановый штамп провода");
                add(ModItems.STAMP_TITANIUM_CIRCUIT.get(), "Титановый штамп чипа");
                add(ModItems.STAMP_OBSIDIAN_FLAT.get(), "Плоский обсидиановый штамп");
                add(ModItems.STAMP_OBSIDIAN_PLATE.get(), "Обсидиановый штамп пластины");
                add(ModItems.STAMP_OBSIDIAN_WIRE.get(), "Обсидиановый штамп провода");
                add(ModItems.STAMP_OBSIDIAN_CIRCUIT.get(), "Обсидиановый штамп чипа");
                add(ModItems.STAMP_DESH_FLAT.get(), "Плоский деш штамп");
                add(ModItems.STAMP_DESH_PLATE.get(), "Деш штамп пластины");
                add(ModItems.STAMP_DESH_WIRE.get(), "Деш штамп провода");
                add(ModItems.STAMP_DESH_CIRCUIT.get(), "Деш штамп чипа");
                add(ModItems.STAMP_DESH_9.get(), "Деш штамп 9мм");
                add(ModItems.STAMP_DESH_44.get(), "Деш штамп .44 Magnum");
                add(ModItems.STAMP_DESH_50.get(), "Деш штамп .50 BMG");
                add(ModItems.STAMP_DESH_357.get(), "Деш штамп .357 Magnum");
                add(ModItems.STAMP_IRON_357.get(), "Железный штамп .357 Magnum");
                add(ModItems.STAMP_IRON_44.get(), "Железный штамп .44 Magnum");
                add(ModItems.STAMP_IRON_50.get(), "Железный штамп .50 BMG");
                add(ModItems.STAMP_IRON_9.get(), "Железный штамп 9мм");




                add("item.smogline.heart_piece", "Частичка сердца");
                add(ModItems.HEART_CONTAINER.get(), "Контейнер для сердца");
                add(ModItems.HEART_BOOSTER.get(), "Усилитель сердца");
                add(ModItems.HEART_FAB.get(), "Фаб-сердце");
                add(ModItems.BLACK_DIAMOND.get(), "Черный алмаз");
                add(ModBlocks.SMOKE_BOMB.get(), "Семтекс");
                add(ModItems.TEMPLATE_FOLDER.get(), "Папка шаблонов машин");
                add(ModItems.ASSEMBLY_TEMPLATE.get(), "Шаблон сборочной машины: %s");
                add("tooltip.smogline.template_broken", "Шаблон сломан!");
                add("tooltip.smogline.created_with_template_folder", "Создано с помощью Папки шаблонов машин");
                add("tooltip.smogline.output", "Выход: ");
                add("tooltip.smogline.input", "Вход: ");
                add("tooltip.smogline.production_time", "Время производства: ");
                add("tooltip.smogline.seconds", "секунд");
                add("tooltip.smogline.energy_consumption", "Потребление энергии:");
                add("tooltip.smogline.tags", "Теги (OreDict):");
                add("item.smogline.template_folder.desc", "Шаблоны машин: Бумага + Краситель$Идентификатор: Железная пластина + Краситель$Штамп для пресса: Плоский штамп$Трек сирены: Изолятор + Стальная пластина");
                add("desc.gui.template", "Вставьте сборочный шаблон");
                add("desc.gui.assembler.warning", "Некорректный шаблон!");
                // === ИНСТРУМЕНТЫ И УСТРОЙСТВА ===
                add("tooltip.smogline.gigadet.line1", "Был создан по приколу");
                add("tooltip.smogline.nuclear_charge.line1", "Ядерное оружие высокой мощности!");
                add("tooltip.smogline.nuclear_charge.line2", "На данный момент, это самый");
                add("tooltip.smogline.nuclear_charge.line3", "разрушительный блок в нашем моде");
                add("tooltip.smogline.nuclear_charge.line4", "Если кратер загрузился некорректно");
                add("tooltip.smogline.nuclear_charge.line5", "или без биомов, то перезапустите мир");

                add("tooltip.smogline.detminer.line1", "Не наносит урон сущностям и игрокам");
                add("tooltip.smogline.detminer.line4", "Позволяет добывать глубинные руды и камень");

                add("tooltip.smogline.dudnuke.line1", "Ядерное оружие высокой мощности!");
                add("tooltip.smogline.dudnuke.line4", "Если кратер загрузился некорректно");
                add("tooltip.smogline.dudnuke.line5", "или без биомов, то перезапустите мир");
                add("tooltip.smogline.dudnuke.line6", "Может быть обезврежена");

                add("tooltip.smogline.dudsalted.line1", "Ядерное оружие высокой мощности!");
                add("tooltip.smogline.dudsalted.line4", "Если кратер загрузился некорректно");
                add("tooltip.smogline.dudsalted.line5", "или без биомов, то перезапустите мир");
                add("tooltip.smogline.dudsalted.line6", "Может быть обезврежена");

                add("tooltip.smogline.dudfugas.line1", "Фугасная бомба высокой мощности!");
                add("tooltip.smogline.dudfugas.line6", "Может быть обезврежена");

                add("tooltip.smogline.defuser.line1", "Устройство для обезвреживания мин и бомб");

                add("tooltip.smogline.crowbar.line1", "Инструмент для вскрытия контейнеров");
                add("tooltip.smogline.crowbar.line2", "Открывает ящики по нажатию ПКМ");

                add("tooltip.smogline.mine_nuke.line1", "Ядерное оружие!");
                add("tooltip.smogline.mine_nuke.line2", "Радиус поражения: 35 метров");
                add("tooltip.smogline.mine_nuke.line3", "Может быть обезврежена");

                add("tooltip.smogline.mine.line1", "Может быть обезврежена");

// ДЕТОНАТОР
                add("tooltip.smogline.detonator.target", "Цель: ");
                add("tooltip.smogline.detonator.no_target", "Нет цели");
                add("tooltip.smogline.detonator.right_click", "ПКМ - активировать");
                add("tooltip.smogline.detonator.shift_right_click", "Shift+ПКМ - установить");

// СКАНЕР КЛАСТЕРОВ
                add("tooltip.smogline.depth_ores_scanner.scans_chunks", "Сканирует чанки в поисках");
                add("tooltip.smogline.depth_ores_scanner.deep_clusters", "глубинных кластеров под игроком");
                add("tooltip.smogline.depth_ores_scanner.depth_warning", "Работает на глубине -30 и ниже!");
                // DEPTH ORES SCANNER (сообщения)
                add("message.smogline.depth_ores_scanner.invalid_height", "Сканер работает только на высоте -30 или ниже!");
                add("message.smogline.depth_ores_scanner.directly_below", "Глубинный кластер прямо под нами!");
                add("message.smogline.depth_ores_scanner.in_chunk", "В нашем чанке обнаружен глубинный кластер!");
                add("message.smogline.depth_ores_scanner.adjacent_chunk", "В соседнем чанке обнаружен глубинный кластер!");
                add("message.smogline.depth_ores_scanner.none_found", "Не обнаружено глубинных кластеров поблизости");

// MULTI DETONATOR TOOLTIPS
                add("tooltip.smogline.multi_detonator.active_point", "➤ %s:");
                add("tooltip.smogline.multi_detonator.point_set", "✅ %s:");
                add("tooltip.smogline.multi_detonator.coordinates", "   %d, %d, %d");
                add("tooltip.smogline.multi_detonator.point_empty", "○ Точка %d:");
                add("tooltip.smogline.multi_detonator.not_set", "   Не установлена");
                add("tooltip.smogline.multi_detonator.key_r", "R - открыть меню");
                add("tooltip.smogline.multi_detonator.shift_rmb", "Shift+ПКМ - сохранить в активную точку");
                add("tooltip.smogline.multi_detonator.rmb_activate", "ПКМ - активировать активную точку");

// MULTI DETONATOR MESSAGES
                add("message.smogline.multi_detonator.position_saved", "Позиция '%s' сохранена: %d, %d, %d");
                add("message.smogline.multi_detonator.no_coordinates", "Нет заданных координат!");
                add("message.smogline.multi_detonator.point_not_set", "Точка %d не установлена!");
                add("message.smogline.multi_detonator.chunk_not_loaded", "Позиция не загружена!");
                add("message.smogline.multi_detonator.activated", "%s активирован!");
                add("message.smogline.multi_detonator.activation_error", "Ошибка при активации!");
                add("message.smogline.multi_detonator.incompatible_block", "Блок несовместим!");





// ДЕТЕКТОР НЕФТИ (тултип)
                add("tooltip.smogline.oil_detector.scans_chunks", "Сканирует чанки в поисках");
                add("tooltip.smogline.oil_detector.oil_deposits", "нефтяных залеж под игроком");

// ДЕТЕКТОР НЕФТИ (сообщения использования)
                add("message.smogline.oil_detector.directly_below", "Залежи нефти прямо под нами!");
                add("message.smogline.oil_detector.in_chunk", "В нашем чанке обнаружена нефть!");
                add("message.smogline.oil_detector.adjacent_chunk", "В соседнем чанке обнаружены залежи нефти!");
                add("message.smogline.oil_detector.none_found", "Не обнаружено залежь нефти поблизости");

                // RANGE DETONATOR
                add("tooltip.smogline.range_detonator.desc", "Активирует совместимые блоки");
                add("tooltip.smogline.range_detonator.hint", "по лучу до 256 блоков.");
                add("message.smogline.range_detonator.pos_not_loaded", "Позиция несовместима или не прогружена");
                add("message.smogline.range_detonator.activated", "Успешно активировано");

                add("tooltip.smogline.grenade_nuc.line1", "Ядерное оружие!");
                add("tooltip.smogline.grenade_nuc.line2", "Зона поражения: 25 метров");
                add("tooltip.smogline.grenade_nuc.line3", "Задержка: 6с");

                add("tooltip.smogline.grenade.common.line1", "Ручная граната");

                add("tooltip.smogline.grenade.smart.line2", "Детонирует при прямом попадании в сущность");
                add("tooltip.smogline.grenade.fire.line2", "Оставляет огонь после детонации");
                add("tooltip.smogline.grenade.slime.line2", "Сильно отскакивает от поверхностей");
                add("tooltip.smogline.grenade.standard.line2", "Слабый осколочный взрыв");
                add("tooltip.smogline.grenade.he.line2", "Усиленный фугасный взрыв");
                add("tooltip.smogline.grenade.default.line2", "Кидайте и взрывайте!");

                add("tooltip.smogline.grenade_if.common.line1", "IF-Граната");

                add("tooltip.smogline.grenade_if.he.line2", "Мощный фугасный взрыв");
                add("tooltip.smogline.grenade_if.slime.line2", "Сильно отскакивает от поверхностей");
                add("tooltip.smogline.grenade_if.fire.line2", "Оставляет огонь после детонации");
                add("tooltip.smogline.grenade_if.standard.line2", "Стандартный взрыв с таймером");
                add("tooltip.smogline.grenade_if.default.line2", "Аллах одобряет!");

                // ru_ru case
                // ru_ru case

                add(ModBlocks.CRATE_CONSERVE.get(), "Ящик с консервами");
                add(ModBlocks.CAGE_LAMP.get(), "Лампа в клетке");
                add(ModBlocks.FLOOD_LAMP.get(), "Прожектор");
                add(ModBlocks.TAPE_RECORDER.get(), "Магнитофон");



                add(ModBlocks.MINE_FAT.get(), "Мина 'Толстяк'");
                add(ModBlocks.MINE_AP.get(), "Противопехотная мина");
                add(ModItems.GRENADE_NUC.get(), "Ядерная граната");
                add(ModItems.GRENADE_IF_HE.get(), "IF-Граната: фугасная");
                add(ModItems.GRENADE_IF_FIRE.get(), "IF-Граната: зажигательная");
                add(ModItems.GRENADE_IF_SLIME.get(), "IF-Граната: прыгучая");
                add(ModItems.MULTI_DETONATOR.get(), "Мульти-детонатор");
                add(ModItems.RANGE_DETONATOR.get(), "Детонатор дальнего действия");
                add(ModItems.DETONATOR.get(), "Детонатор");
                add(ModBlocks.BARBED_WIRE_POISON.get(), "Колючая проволока (яд)");
                add(ModBlocks.BARBED_WIRE_FIRE.get(), "Колючая проволока (огонь)");
                add(ModBlocks.BARBED_WIRE_RAD.get(), "Колючая проволока (радиация)");
                add(ModBlocks.BARBED_WIRE.get(), "Колючая проволока");
                add(ModBlocks.BARBED_WIRE_WITHER.get(), "Колючая проволока (иссушение)");
                add(ModBlocks.WASTE_CHARGE.get(), "Отходный заряд");
                add(ModBlocks.GIGA_DET.get(), "Чёртов заряд горняка");
                add(ModBlocks.NUCLEAR_CHARGE.get(), "Ядерный заряд");
                add(ModBlocks.C4.get(), "Заряд C4");
                add(ModItems.DEFUSER.get(), "Устройство для разминирования");
                add(ModItems.CROWBAR.get(), "Лом");
                add(ModItems.DEPTH_ORES_SCANNER.get(), "Сканер глубинных кластеров");
                add(ModItems.OIL_DETECTOR.get(), "Детектор нефти");

                add(ModItems.GHIORSIUM_CLADDING.get(), "Прокладка из гиорсия");
                add(ModItems.DESH_CLADDING.get(), "Обшивка из деш");
                add(ModItems.RUBBER_CLADDING.get(), "Резиновая обшивка");
                add(ModItems.LEAD_CLADDING.get(), "Свинцовая обшивка");
                add(ModItems.PAINT_CLADDING.get(), "Свинцовая краска");
                add(ModItems.GRENADESMART.get(), "УМная отскок граната");
                add(ModItems.GRENADESLIME.get(), "Отскок-отскок граната");
                add(ModItems.GRENADE.get(), "Отскок граната");
                add(ModItems.GRENADEHE.get(), "Мощная отскок граната");
                add(ModItems.GRENADEFIRE.get(), "Зажигательная отскок граната");

                add(ModItems.GRENADE_IF.get(), "IF граната");

                add("item.smogline.radaway", "Антирадин");
                add("item.smogline.wood_ash_powder", "Древесный пепел");
                add("effect.smogline.radaway", "Очищение от радиации");


// ru_ru case
                add(ModBlocks.CONVERTER_BLOCK.get(), "Конвертер энергии");
                add(ModBlocks.MACHINE_BATTERY_DINEUTRONIUM.get(), "Динейтрониевое энергохранилище");
                add(ModBlocks.MACHINE_BATTERY_SCHRABIDIUM.get(), "Шрабидиевое энергохранилище");
                add(ModBlocks.MACHINE_BATTERY_LITHIUM.get(), "Литиевое энергохранилище");
                add(ModBlocks.SEQUESTRUM_ORE.get(), "Селитровая руда");
                add(ModItems.SEQUESTRUM.get(), "Селитра");
                // русский:

                add(ModItems.AIRSTRIKE_TEST.get(), "Авиаудар");

                add(ModBlocks.BURNED_GRASS.get(), "Выжженная трава");
                add(ModBlocks.WASTE_PLANKS.get(), "Выжженные доски");
                add(ModBlocks.WASTE_LOG.get(), "Выжженное бревно");
                add(ModBlocks.SELLAFIELD_SLAKED.get(), "Погашенный селлафит");
                add(ModBlocks.SELLAFIELD_SLAKED1.get(), "Погашенный селлафит I");
                add(ModBlocks.SELLAFIELD_SLAKED2.get(), "Погашенный селлафит II");
                add(ModBlocks.SELLAFIELD_SLAKED3.get(), "Погашенный селлафит III");




                add("tooltip.smogline.depthstone.line1", "Может быть уничтожен только взрывом!");
                add("tooltip.smogline.depthstone.line4", "Используйте Шахтёрский заряд для безопасной добычи");

                add(ModItems.BLADE_TEST.get(), "Деш лезвия");
                add(ModItems.BLADE_STEEL.get(), "Стальные лезвия");
                add(ModItems.BLADE_TITANIUM.get(), "Титановые лезвия");
                add(ModItems.BLADE_ALLOY.get(), "Лезвия из продвинутого сплава");





                add(ModItems.STRAWBERRY.get(), "Клубника");



                add("item.smogline.firebrick", "Шамотный кирпич");
                add("item.smogline.uranium_raw", "Рудный уран");
                add("item.smogline.tungsten_raw", "Рудный вольфрам");
                add("item.smogline.titanium_raw", "Рудный титан");
                add("item.smogline.thorium_raw", "Рудный торий");
                add("item.smogline.lead_raw", "Рудный свинец");
                add("item.smogline.cobalt_raw", "Рудный кобальт");
                add("item.smogline.beryllium_raw", "Рудный бериллий");
                add("item.smogline.aluminum_raw", "Рудный алюминий");
                add("item.smogline.cinnabar", "Киноварь");
                add("item.smogline.sulfur", "Сера");
                add("item.smogline.rareground_ore_chunk", "Кусок редкоземельной руды");
                add("item.smogline.lignite", "Бурый уголь");
                add("item.smogline.fluorite", "Флюорит");
                add("item.smogline.fireclay_ball", "Комок огнеупорной глины");
            
                add("item.smogline.blueprint_folder", "Папка шаблонов");
                add("item.smogline.blueprint_folder.named", "Папка шаблонов машин");
                add("item.smogline.blueprint_folder.empty", "Пустая папка");
                add("item.smogline.blueprint_folder.obsolete", "Устаревший шаблон (группа удалена)");
                add("item.smogline.blueprint_folder.desc", "Вставьте в Сборочную машину для разблокировки рецептов");
                add("item.smogline.blueprint_folder.recipes", "Содержит рецепты:");
                add("gui.smogline.recipe_from_group", "Из группы:");
                
                add("sounds.smogline.radaway_use", "Использование антирадина");
                
                add("tooltip.smogline.mods", "Модификации:");
                add("tooltip.smogline.heart_piece.effect", "+5 Здоровья");
                
                add("tooltip.smogline.applies_to", "Применяется к:");

                add("tooltip.smogline.helmet", "Шлему");
                add("tooltip.smogline.chestplate", "Нагруднику");
                add("tooltip.smogline.leggings", "Поножам");
                add("tooltip.smogline.boots", "Ботинкам");
                add("tooltip.smogline.armor.all", "Любой броне");
                add("tooltip.smogline.rad_protection.value_short", "%s сопр. радиации.");

                add("gui.smogline.blast_furnace.accepts", "Принимает предметы со стороны: %s");
                add("direction.smogline.down", "Вниз");
                add("direction.smogline.up", "Вверх");
                add("direction.smogline.north", "Север");
                add("direction.smogline.south", "Юг");
                add("direction.smogline.west", "Запад");
                add("direction.smogline.east", "Восток");
                add("gui.smogline.anvil.inputs", "Входы:");
                add("gui.smogline.anvil.outputs", "Выходы:");
                add("gui.smogline.anvil.search", "Поиск");
                add("gui.smogline.anvil.search_hint", "Поиск...");
                add("gui.smogline.anvil.tier", "Требуемый уровень: %s");
                add("tier.smogline.anvil.iron", "Железо");
                add("tier.smogline.anvil.steel", "Сталь");
                add("tier.smogline.anvil.oil", "Нефтяной");
                add("tier.smogline.anvil.nuclear", "Ядерный");
                add("tier.smogline.anvil.rbmk", "РБМК");
                add("tier.smogline.anvil.fusion", "Термоядерный");
                add("tier.smogline.anvil.particle", "Частичный ускоритель");
                add("tier.smogline.anvil.gerald", "Джеральд");
                add("tier.smogline.anvil.murky", "Мрачный");

                // БЛОКИ
                add(ModBlocks.RESOURCE_ASBESTOS.get(), "Асбестовый кластер");
                add(ModBlocks.RESOURCE_BAUXITE.get(), "Боксит");
                add(ModBlocks.RESOURCE_HEMATITE.get(), "Гематит");
                add(ModBlocks.RESOURCE_LIMESTONE.get(), "Известняк");
                add(ModBlocks.RESOURCE_MALACHITE.get(), "Малахит");
                add(ModBlocks.RESOURCE_SULFUR.get(), "Серный кластер");
                add("block.smogline.anvil_block", "Индустриальная наковальня");
                add("block.smogline.anvil_iron", "Железная наковальня");
                add("block.smogline.anvil_lead", "Свинцовая наковальня");
                add("block.smogline.anvil_steel", "Стальная наковальня");
                add("block.smogline.anvil_desh", "Наковальня из деша");
                add("block.smogline.anvil_ferrouranium", "Наковальня из ферроурания");
                add("block.smogline.anvil_saturnite", "Сатурнитовая наковальня");
                add("block.smogline.anvil_bismuth_bronze", "Наковальня из висмутовой бронзы");
                add("block.smogline.anvil_arsenic_bronze", "Наковальня из мышьяковой бронзы");
                add("block.smogline.anvil_schrabidate", "Шрабидатовая наковальня");
                add("block.smogline.anvil_dnt", "Наковальня DNT");
                add("block.smogline.anvil_osmiridium", "Осмиридиевая наковальня");
                add("block.smogline.anvil_murky", "Мрачная наковальня");
                add("block.smogline.door_office", "Офисная дверь");
                add("block.smogline.door_bunker", "Бункерная дверь");
                add("block.smogline.metal_door", "Металлическая дверь");
                add("block.smogline.demon_lamp", "Милая лампа (WIP)");
                add("block.smogline.explosive_charge", "Заряд взрывчатки");
                add("block.smogline.det_miner", "Шахтёрский заряд");
                add("block.smogline.concrete_vent", "Вентиляция в бетоне");
                add("block.smogline.concrete_fan", "Вентилятор в бетоне");
                add("block.smogline.concrete_marked", "Помеченный бетон");
                add("block.smogline.concrete_cracked", "Потрескавшийся бетон");
                add("block.smogline.concrete_mossy", "Замшелый бетон");
                add("block.smogline.concrete", "Бетон");
                add("block.smogline.reinforced_glass", "Усиленное стекло");
                add("block.smogline.crate", "Ящик");
                add("block.smogline.crate_lead", "Свинцовый ящик");
                add("block.smogline.crate_metal", "Металлический ящик");
                add("block.smogline.crate_weapon", "Ящик с оружием");
                add("block.smogline.uranium_block", "Урановый блок");
                add("block.smogline.plutonium_block", "Плутониевый блок");
                add("block.smogline.plutonium_fuel_block", "Блок плутониевого топлива");
                add("block.smogline.polonium210_block", "Блок полония-210");
                add("block.smogline.armor_table", "Стол модификации брони");
                add("block.smogline.machine_assembler", "Сборочная машина (Старая)");
                add("block.smogline.advanced_assembly_machine", "Сборочная машина");
                add("block.smogline.machine_battery", "Энергохранилище");


                add("block.smogline.wire_coated", "Провод из красной меди");
                add("block.smogline.wood_burner", "Дровяной генератор");
                add("block.smogline.shredder", "Измельчитель");
                add("block.smogline.blast_furnace", "Доменная печь");
                add("block.smogline.blast_furnace_extension", "Расширение доменной печи");
                add("block.smogline.press", "Пресс");
                add("block.smogline.geiger_counter_block", "Стационарный счетчик Гейгера");
                add("block.smogline.freaky_alien_block", "Блок ебанутого инопланетянина");
                add("block.smogline.reinforced_stone", "Уплотнённый камень");
                add("block.smogline.reinforced_stone_slab", "Плита из уплотнённого камня");
                add("block.smogline.reinforced_stone_stairs", "Ступеньки из уплотнённого камня");
                add("block.smogline.concrete_hazard", "Бетон ''Выбор строителя'' - Полоса опасности");
                add("block.smogline.concrete_hazard_slab", "Бетонная плита ''Выбор строителя'' - Полоса опасности");
                add("block.smogline.concrete_hazard_stairs", "Бетонные ступеньки ''Выбор строителя'' - Полоса опасности");
                add("block.smogline.concrete_stairs", "Бетонные ступеньки");
                add("block.smogline.concrete_slab", "Бетонная плита");
                add("block.smogline.concrete_cracked_slab", "Плита из треснутого бетона");
                add("block.smogline.concrete_cracked_stairs", "Ступени из треснутого бетона");
                add("block.smogline.concrete_mossy_slab", "Плита из замшелого бетона");
                add("block.smogline.concrete_mossy_stairs", "Ступени из замшелого бетона");
                add("block.smogline.switch", "Рубильник");
                add("block.smogline.large_vehicle_door", "Дверь для крупногабаритного транспорта");
                add("block.smogline.round_airlock_door", "Круглая воздушная дверь");
                add("block.smogline.strawberry_bush", "Куст клубники");
                add("block.smogline.strawberry", "Клубника");
                add("block.smogline.brick_concrete", "Бетонные кирпичи");
                add("block.smogline.brick_concrete_slab", "Плита из бетонных кирпичей");
                add("block.smogline.brick_concrete_stairs", "Ступени из бетонных кирпичей");
                add("block.smogline.brick_concrete_broken", "Сломанные бетонные кирпичи");
                add("block.smogline.brick_concrete_broken_slab", "Плита из сломанных бетонных кирпичей");
                add("block.smogline.brick_concrete_broken_stairs", "Ступени из сломанных бетонных кирпичей");
                add("block.smogline.brick_concrete_cracked", "Треснутые Бетонные кирпичи");
                add("block.smogline.brick_concrete_cracked_slab", "Плита из треснутых бетонных кирпичей");
                add("block.smogline.brick_concrete_cracked_stairs", "Ступени из треснутых бетонных кирпичей");
                add("block.smogline.brick_concrete_mossy", "Замшелые бетонные кирпичи");
                add("block.smogline.brick_concrete_mossy_slab", "Плита из замшелых бетонных кирпичей");
                add("block.smogline.brick_concrete_mossy_stairs", "Ступени из замшелых бетонных кирпичей");
                add("block.smogline.brick_concrete_marked", "Помеченные бетонные кирпичи");


                // РУДЫ

                add("block.smogline.uranium_ore", "Урановая руда");
                add("block.smogline.aluminum_ore", "Алюминиевая руда");
                add("block.smogline.aluminum_ore_deepslate", "Глубинная алюминиевая руда");
                add("block.smogline.cinnabar_ore_deepslate", "Глубинная киноварная руда");
                add("block.smogline.cobalt_ore_deepslate", "Глубинная кобальтовая руда");
                add("block.smogline.titanium_ore", "Титановая руда");
                add("block.smogline.titanium_ore_deepslate", "Глубинная титановая руда");
                add("block.smogline.tungsten_ore", "Вольфрамовая руда");
                add("block.smogline.asbestos_ore", "Асбестовая руда");
                add("block.smogline.sulfur_ore", "Серная руда");
                add("block.smogline.cobalt_ore", "Кобальтовая руда");
                add("block.smogline.lignite_ore", "Руда бурого угля");
                add("block.smogline.uranium_ore_h", "Обогащённая урановая руда");
                add("block.smogline.uranium_ore_deepslate", "Глубинная урановая руда");
                add("block.smogline.thorium_ore", "Ториевая руда");
                add("block.smogline.thorium_ore_deepslate", "Глубинная ториевая руда");
                add("block.smogline.rareground_ore", "Руда редкоземельных металлов");
                add("block.smogline.rareground_ore_deepslate", "Глубинная руда редкоземельных металлов");
                add("block.smogline.beryllium_ore", "Бериллиевая руда");
                add("block.smogline.beryllium_ore_deepslate", "Глубинная бериллиевая руда");
                add("block.smogline.fluorite_ore", "Флюоритовая руда");
                add("block.smogline.lead_ore", "Свинцовая руда");
                add("block.smogline.lead_ore_deepslate", "Глубинная свинцовая руда");
                add("block.smogline.cinnabar_ore", "Киноварная руда");
                add("block.smogline.crate_iron", "Железный ящик");
                add("block.smogline.crate_steel", "Стальной ящик");
                add("block.smogline.crate_desh", "Деш ящик");

                add("block.smogline.waste_grass", "Мёртвая трава");
                add("block.smogline.waste_leaves", "Мёртвая листва");

                // MACHINE GUI
                
                add("tooltip.smogline.armor_table.main_slot", "Вставьте броню, чтобы ее модифицировать...");
                add("tooltip.smogline.slot", "Слот");
                add("tooltip.smogline.armor_table.helmet_slot", "Шлем");
                add("tooltip.smogline.armor_table.chestplate_slot", "Нагрудник");
                add("tooltip.smogline.armor_table.leggings_slot", "Поножи");
                add("tooltip.smogline.armor_table.boots_slot", "Ботинки");
                add("tooltip.smogline.armor_table.battery_slot", "Аккумулятор");
                add("tooltip.smogline.armor_table.special_slot", "Особое");
                add("tooltip.smogline.armor_table.plating_slot", "Пластина");
                add("tooltip.smogline.armor_table.casing_slot", "Обшивка");
                add("tooltip.smogline.armor_table.servos_slot", "Сервоприводы");

                add("tooltip.smogline.rad_protection.value", "Сопротивление радиации: %s");

                add("container.inventory", "Инвентарь");
                add("container.smogline.armor_table", "Стол модификации брони");
                add("container.smogline.machine_assembler", "Сборочная машина");
                add("container.smogline.advanced_assembly_machine", "Сборочная машина");
                add("container.smogline.wood_burner", "Дровяной генератор");
                add("container.smogline.machine_battery", "Энергохранилище");
                add("container.smogline.press", "Пресс");
                add("container.smogline.anvil_block", "Индустриальная наковальня");
                add("container.smogline.anvil", "Наковальня %s");
                add("container.smogline.crate_iron", "Железный ящик");
                add("container.smogline.crate_steel", "Стальной ящик");
                add("container.smogline.crate_desh", "Душ ящик");

                add("gui.smogline.battery.priority.0", "Приоритет: Низкий");
                add("gui.smogline.battery.priority.0.desc", "Низший приоритет. Опустошается в первую очередь, заполняется в последнюю");
                add("gui.smogline.battery.priority.1", "Приоритет: Нормальный");
                add("gui.smogline.battery.priority.1.desc", "Стандартный приоритет для передачи энергии.");
                add("gui.smogline.battery.priority.2", "Приоритет: Высокий");
                add("gui.smogline.battery.priority.2.desc", "Высший приоритет. Заполняется первым, опустошается последним.");
                add("gui.smogline.battery.priority.recommended", "(Рекомендуется)");

                add("gui.smogline.battery.condition.no_signal", "Когда НЕТ редстоун-сигнала:");
                add("gui.smogline.battery.condition.with_signal", "Когда ЕСТЬ редстоун-сигнал:");

                add("gui.smogline.battery.mode.both", "Режим: Приём и Передача");
                add("gui.smogline.battery.mode.both.desc", "Разрешены все операции с энергией.");
                add("gui.smogline.battery.mode.input", "Режим: Только Приём");
                add("gui.smogline.battery.mode.input.desc", "Разрешён только приём энергии.");
                add("gui.smogline.battery.mode.output", "Режим: Только Передача");
                add("gui.smogline.battery.mode.output.desc", "Разрешена только отдача энергии.");
                add("gui.smogline.battery.mode.locked", "Режим: Заблокировано");
                add("gui.smogline.battery.mode.locked.desc", "Все операции с энергией отключены.");

                add("gui.recipe.setRecipe", "Выбрать рецепт");

                add("tooltip.smogline.battery.stored", "Хранится энергии:");
                add("tooltip.smogline.battery.transfer_rate", "Скорость зарядки: %1$s HE/t");
                add("tooltip.smogline.battery.discharge_rate", "Скорость разрядки: %1$s HE/t");

                add("tooltip.smogline.machine_battery.capacity", "Ёмкость: %1$s HE");
                add("tooltip.smogline.machine_battery.charge_speed", "Скорость зарядки: %1$s HE/т");
                add("tooltip.smogline.machine_battery.discharge_speed", "Скорость разрядки: %1$s HE/т");
                add("tooltip.smogline.machine_battery.stored", "Заряжено: %1$s / %2$s HE");
                add("tooltip.smogline.requires", "Требуется");


                add("hazard.smogline.radiation", "[Радиоактивный]");
                add("hazard.smogline.radiation.format", "%s РАД/с");
                add("hazard.smogline.hydro_reactive", "[Гидрореактивный]");
                add("hazard.smogline.explosive_on_fire", "[Воспламеняющийся / Взрывоопасный]");
                add("hazard.smogline.pyrophoric", "[Пирофорный / Горячий]");
                add("hazard.smogline.explosion_strength.format", " Сила взрыва - %s");
                add("hazard.smogline.stack", "Стак: %s");

                add("item.smogline.meter.geiger_counter.name", "СЧЁТЧИК ГЕЙГЕРА");
                add("item.smogline.meter.dosimeter.name", "ДОЗИМЕТР");
                add("item.smogline.meter.title_format", "%s");
                add("smogline.render.shader_detected", "§e[HBM] §7Обнаружен активный шейдер. Переключение на совместимый рендер...");
                add("smogline.render.shader_disabled", "§a[HBM] §7Шейдер отключен. Возврат к оптимизированному VBO рендеру.");
                add("smogline.render.path_changed", "§e[HBM] §7Путь рендера установлен: %s");
                add("smogline.render.status", "§e[HBM] §7Текущий путь рендера: §f%s\n§7Внешний шейдер обнаружен: §f%s");

                add("tooltip.smogline.abilities", "Способности:");
                add("tooltip.smogline.vein_miner", "Жилковый майнер (%s)");
                add("tooltip.smogline.aoe", "Зона действия %s");
                add("tooltip.smogline.silk_touch", "Шёлковое касание");
                add("tooltip.smogline.fortune", "Удача (%s)");
                add("tooltip.smogline.right_click", "ПКМ - переключить способность");
                add("tooltip.smogline.shift_right_click", "Shift + ПКМ - выключить всё");

                add("message.smogline.vein_miner.enabled", "Жилковый майнер %s активирован!");
                add("message.smogline.vein_miner.disabled", "Жилковый майнер %s деактивирован!");
                add("message.smogline.aoe.enabled", "Зона действия %1$s x %1$s x %1$s активирована!");
                add("message.smogline.aoe.disabled", "Зона действия %s x %s x %s деактивирована!");
                add("message.smogline.silk_touch.enabled", "Шёлковое касание активировано!");
                add("message.smogline.silk_touch.disabled", "Шёлковое касание деактивировано!");
                add("message.smogline.fortune.enabled", "Удача %s активирована!");
                add("message.smogline.fortune.disabled", "Удача %s деактивирована!");
                add("message.smogline.disabled", "Все способности выключены!");

                add("item.smogline.meter.chunk_rads", "§eТекущий уровень радиации в чанке: %s\n");
                add("item.smogline.meter.env_rads", "§eОбщее радиационное заражение среды: %s");
                add("item.smogline.meter.player_rads", "§eУровень радиоактивного заражения игрока: %s\n");
                add("item.smogline.meter.protection", "§eЗащищённость игрока: %s (%s)");

                add("item.smogline.meter.rads_over_limit", ">%s RAD/s");
                add("tooltip.smogline.hold_shift_for_details", "<Зажмите SHIFT для деталей>");
                
                add("sounds.smogline.geiger_counter", "Щелчки счетчика Гейгера");
                add("sounds.smogline.tool.techboop", "Пик счетчика Гейгера");
                
                add("commands.smogline.rad.cleared", "Радиация очищена у %s игроков.");
                add("commands.smogline.rad.cleared.self", "Ваша радиация очищена.");
                add("commands.smogline.rad.added", "Добавлено %s радиации %s игрокам.");
                add("commands.smogline.rad.added.self", "Вам добавлено %s радиации.");
                add("commands.smogline.rad.removed", "Убрано %s радиации у %s игроков.");
                add("commands.smogline.rad.removed.self", "У вас убрано %s радиации.");

                add("death.attack.radiation", "Игрок %s умер от лучевой болезни");
                add("advancements.smogline.radiation_200.title", "Ура, Радиация!");
                add("advancements.smogline.radiation_200.description", "Достигнуть уровня радиации в 200 РАД");
                add("advancements.smogline.radiation_1000.title", "Ай, Радиация!");
                add("advancements.smogline.radiation_1000.description", "Умереть от лучевой болезни");

                add("chat.smogline.structure.obstructed", "Другие блоки мешают установке структуры!!");


                add("text.autoconfig.smogline.title", "Настройки радиации (HBM Modernized)");

                add("text.autoconfig.smogline.category.general", "Общие настройки");
                add("text.autoconfig.smogline.option.enableRadiation", "Включить радиацию");
                add("text.autoconfig.smogline.option.enableChunkRads", "Включить радиацию в чанках");
                add("text.autoconfig.smogline.option.usePrismSystem", "Использовать систему PRISM (иначе Simple, WIP)");

                add("text.autoconfig.smogline.category.world_effects", "Эффекты мира");
                add("text.autoconfig.smogline.option.worldRadEffects", "Эффекты радиации на мир (изменения блоков)");
                add("text.autoconfig.smogline.option.worldRadEffects.@Tooltip", "Включает/выключает эффекты разрушения мира от высокой радиации (замена блоков, гибель растительности и т.д.).");

                add("text.autoconfig.smogline.option.worldRadEffectsThreshold", "Порог радиации для разрушения");
                add("text.autoconfig.smogline.option.worldRadEffectsThreshold.@Tooltip", "Минимальный уровень фоновой радиации в чанке, при котором начинаются эффекты разрушения.");

                add("text.autoconfig.smogline.option.worldRadEffectsBlockChecks", "Проверок блоков в тик");
                add("text.autoconfig.smogline.option.worldRadEffectsBlockChecks.@Tooltip", "Количество случайных проверок блоков в затронутом чанке за один тик. Влияет на скорость разрушения. Большие значения могут повлиять на производительность.");

                add("text.autoconfig.smogline.option.worldRadEffectsMaxScaling", "Макс. множитель разрушения");
                add("text.autoconfig.smogline.option.worldRadEffectsMaxScaling.@Tooltip", "Максимальное ускорение разрушения мира при пиковой радиации. 1 = скорость не меняется, 4 = скорость может быть до 4 раз выше. Макс значение - 10х");

                add("text.autoconfig.smogline.option.worldRadEffectsMaxDepth", "Глубина разрушения");
                add("text.autoconfig.smogline.option.worldRadEffectsMaxDepth.@Tooltip", "Максимальная глубина (в блоках) от поверхности, на которую могут распространяться эффекты разрушения мира.");

                add("text.autoconfig.smogline.option.enableRadFogEffect", "Включить эффект радиоактивного тумана");
                add("text.autoconfig.smogline.option.enableRadFogEffect.@Tooltip", "Включает/выключает появление радиоактивного тумана в чанках с высоким уровнем радиации.");
                
                add("text.autoconfig.smogline.option.radFogThreshold", "Порог для появления тумана");
                add("text.autoconfig.smogline.option.radFogThreshold.@Tooltip", "Минимальный уровень фоновой радиации в чанке, при котором может появиться туман.");
                
                add("text.autoconfig.smogline.option.radFogChance", "Шанс появления тумана");
                add("text.autoconfig.smogline.option.radFogChance.@Tooltip", "Шанс появления частиц тумана в подходящем чанке за секунду. Рассчитывается как 1 к X. Чем меньше значение, тем чаще появляется туман.");

                add("text.autoconfig.smogline.category.player", "Игрок");
                add("text.autoconfig.smogline.option.maxPlayerRad", "Максимальный уровень радиации у игрока");
                add("text.autoconfig.smogline.option.radDecay", "Скорость распада радиации у игрока");
                add("text.autoconfig.smogline.option.radDamage", "Урон от радиации");
                add("text.autoconfig.smogline.option.radDamageThreshold", "Порог урона от радиации");
                add("text.autoconfig.smogline.option.radSickness", "Порог для тошноты");
                add("text.autoconfig.smogline.option.radWater", "Порог для негативного эффекта воды, WIP");
                add("text.autoconfig.smogline.option.radConfusion", "Порог для замешательства, WIP");
                add("text.autoconfig.smogline.option.radBlindness", "Порог для слепоты");

                add("text.autoconfig.smogline.category.overlay", "Экранные наложения");

                add("text.autoconfig.smogline.option.enableRadiationPixelEffect", "Экранный эффект радиационных помех");
                add("text.autoconfig.smogline.option.radiationPixelEffectThreshold", "Порог срабатывания эффекта");
                add("text.autoconfig.smogline.option.radiationPixelMaxIntensityRad", "Максимальная интенсивность эффекта");
                add("text.autoconfig.smogline.option.radiationPixelEffectMaxDots", "Макс. количество пикселей");
                add("text.autoconfig.smogline.option.radiationPixelEffectGreenChance", "Шанс зеленого пикселя");
                add("text.autoconfig.smogline.option.radiationPixelMinLifetime", "Мин. время жизни пикселя");
                add("text.autoconfig.smogline.option.radiationPixelMaxLifetime", "Макс. время жизни пикселя");
                add("text.autoconfig.smogline.option.enableObstructionHighlight", "Включить подсветку препятствий");
                add("text.autoconfig.smogline.option.enableObstructionHighlight.@Tooltip", "Если включено, блоки, мешающие размещению мультиблока, \nбудут подсвечиваться красной рамкой.");
                add("text.autoconfig.smogline.option.obstructionHighlightDuration", "Длительность подсветки (сек)");
                add("text.autoconfig.smogline.option.obstructionHighlightDuration.@Tooltip", "Время в секундах, в течение которого будет видна подсветка препятствий.");
                add("text.autoconfig.smogline.option.obstructionHighlightAlpha", "Непрозрачность подсветки препятствий");
                add("text.autoconfig.smogline.option.obstructionHighlightAlpha.@Tooltip", "Устанавливает непрозрачность заливки подсветки.\n0% = Невидимая, 100% = Непрозрачная.");

                add("text.autoconfig.smogline.category.chunk", "Чанк");
                
                add("text.autoconfig.smogline.option.maxRad", "Максимальная радиация в чанке");
                add("text.autoconfig.smogline.option.fogRad", "Порог радиации для появления тумана");
                add("text.autoconfig.smogline.option.fogCh", "Шанс появления тумана (1 из fogCh), WIP");
                add("text.autoconfig.smogline.option.radChunkDecay", "Скорость распада радиации в чанке");
                add("text.autoconfig.smogline.option.radChunkSpreadFactor", "Фактор распространения радиации между чанками");
                add("text.autoconfig.smogline.option.radSpreadThreshold", "Порог распространения радиации");
                add("text.autoconfig.smogline.option.minRadDecayAmount", "Мин. распад радиации за тик");
                add("text.autoconfig.smogline.option.radSourceInfluenceFactor", "Влияние источников радиации на чанк");
                add("text.autoconfig.smogline.option.radRandomizationFactor", "Фактор рандомизации радиации в чанке");

                add("text.autoconfig.smogline.category.rendering", "Рендеринг");

                add("text.autoconfig.smogline.option.modelUpdateDistance", "Дистанция для рендеринга динамических частей .obj моделей");
                add("text.autoconfig.smogline.option.enableOcclusionCulling", "Включить куллинг моделей");

                add("text.autoconfig.smogline.category.debug", "Отладка");

                add("text.autoconfig.smogline.option.enableDebugRender", "Включить отладочный рендер радиации");
                add("text.autoconfig.smogline.option.debugRenderTextSize", "Размер текста отладочного рендера");
                add("text.autoconfig.smogline.option.debugRenderDistance", "Дальность отладочного рендеринга (чанки)");
                add("text.autoconfig.smogline.option.debugRenderInSurvival", "Показывать отладочный рендер в режиме выживания");
                add("text.autoconfig.smogline.option.enableDebugLogging", "Включить отладочные логи");

                add("text.autoconfig.smogline.option.enableRadiation.@Tooltip", "Если выключено, вся радиация отключается (чанки, предметы)");
                add("text.autoconfig.smogline.option.enableChunkRads.@Tooltip", "Если выключено, радиация в чанках всегда 0");
                add("text.autoconfig.smogline.option.usePrismSystem.@Tooltip", "Использовать систему PRISM для радиации в чанках (WIP)");

                add("text.autoconfig.smogline.option.maxPlayerRad.@Tooltip", "Максимальная радиация, которую может накопить игрок");
                add("text.autoconfig.smogline.option.radDecay.@Tooltip", "Скорость распада радиации у игрока за тик");
                add("text.autoconfig.smogline.option.radDamage.@Tooltip", "Урон за тик при превышении порога");
                add("text.autoconfig.smogline.option.radDamageThreshold.@Tooltip", "Игрок начинает получать урон выше этого значения");
                add("text.autoconfig.smogline.option.radSickness.@Tooltip", "Порог для эффекта тошноты");
                add("text.autoconfig.smogline.option.radWater.@Tooltip", "Порог для негативного эффекта воды (WIP)");
                add("text.autoconfig.smogline.option.radConfusion.@Tooltip", "Порог для эффекта замешательства (WIP)");
                add("text.autoconfig.smogline.option.radBlindness.@Tooltip", "Порог для эффекта слепоты");

                add("text.autoconfig.smogline.option.enableRadiationPixelEffect.@Tooltip", "Включает/выключает эффект случайных мерцающих пикселей на экране, когда игрок подвергается радиационному облучению.");
                add("text.autoconfig.smogline.option.radiationPixelEffectThreshold.@Tooltip", "Минимальный уровень входящей радиации (в RAD/с), при котором начинает появляться эффект визуальных помех.");
                add("text.autoconfig.smogline.option.radiationPixelMaxIntensityRad.@Tooltip", "Уровень входящей радиации (в RAD/с), при котором эффект помех достигает своей максимальной силы (максимальное количество пикселей).");
                add("text.autoconfig.smogline.option.radiationPixelEffectMaxDots.@Tooltip", "Максимальное количество пикселей, которое может одновременно находиться на экране при пиковой интенсивности эффекта. Влияет на производительность на слабых системах.");
                add("text.autoconfig.smogline.option.radiationPixelEffectGreenChance.@Tooltip", "Вероятность (от 0.0 до 1.0), что новый появившийся пиксель будет зеленым, а не белым. Например, 0.1 = 10% шанс.");
                add("text.autoconfig.smogline.option.radiationPixelMinLifetime.@Tooltip", "Минимальное время (в тиках), которое один пиксель будет оставаться на экране. 20 тиков = 1 секунда.");
                add("text.autoconfig.smogline.option.radiationPixelMaxLifetime.@Tooltip", "Максимальное время (в тиках), которое один пиксель будет оставаться на экране. Для каждого пикселя выбирается случайное значение между минимальным и максимальным временем жизни.");

                add("text.autoconfig.smogline.option.maxRad.@Tooltip", "Максимальная радиация в чанке");
                add("text.autoconfig.smogline.option.fogRad.@Tooltip", "Порог радиации для появления тумана (WIP)");
                add("text.autoconfig.smogline.option.fogCh.@Tooltip", "Шанс появления тумана (WIP)");
                add("text.autoconfig.smogline.option.radChunkDecay.@Tooltip", "Скорость распада радиации в чанке");
                add("text.autoconfig.smogline.option.radChunkSpreadFactor.@Tooltip", "Сколько радиации распространяется на соседние чанки");
                add("text.autoconfig.smogline.option.radSpreadThreshold.@Tooltip", "Ниже этого значения радиация не распространяется");
                add("text.autoconfig.smogline.option.minRadDecayAmount.@Tooltip", "Минимальный распад радиации за тик в чанке");
                add("text.autoconfig.smogline.option.radSourceInfluenceFactor.@Tooltip", "Влияние источников радиации на чанк.");
                add("text.autoconfig.smogline.option.radRandomizationFactor.@Tooltip", "Фактор рандомизации радиации в чанке");

                add("text.autoconfig.smogline.option.modelUpdateDistance.@Tooltip", "Дистанция для рендеринга динамических частей .obj моделей (в чанках)");
                add("text.autoconfig.smogline.option.enableOcclusionCulling.@Tooltip", "Включить куллинг моделей (выключите, если ваши модели рендерятся некорректно)");

                add("text.autoconfig.smogline.option.enableDebugRender.@Tooltip", "Показывать отладочный оверлей радиации в чанках (F3)");
                add("text.autoconfig.smogline.option.debugRenderTextSize.@Tooltip", "Размер текста для отладочного оверлея");
                add("text.autoconfig.smogline.option.debugRenderDistance.@Tooltip", "Дальность отладочного рендеринга (чанки)");
                add("text.autoconfig.smogline.option.debugRenderInSurvival.@Tooltip", "Показывать отладочный рендер в режиме выживания");
                add("text.autoconfig.smogline.option.enableDebugLogging.@Tooltip", "Если выключено, будет активно глубокое логгирование игровых событий. Не стоит включать, если не испытываете проблем");
                break;
            
            case "en_us":

                // TABS
                add("itemGroup.smogline.ntm_resources_tab", "NTM Ingots and Resources");
                add("itemGroup.smogline.ntm_fuel_tab", "NTM Fuel and Machine Components");
                add("itemGroup.smogline.ntm_templates_tab", "NTM Templates");
                add("itemGroup.smogline.ntm_ores_tab", "NTM Ores and Blocks");
                add("itemGroup.smogline.ntm_machines_tab", "NTM Machines");
                add("itemGroup.smogline.ntm_bombs_tab", "NTM Bombs");
                add("itemGroup.smogline.ntm_missiles_tab", "NTM Missiles and Satellites");
                add("itemGroup.smogline.ntm_weapons_tab", "NTM Weapons and Turrets");
                add("itemGroup.smogline.ntm_consumables_tab", "NTM Consumables and Equipment");
                add("itemGroup.smogline.ntm_spareparts_tab", "NTM Spare Parts");
                add("itemGroup.smogline.ntm_instruments_tab", "NTM Instruments");
                add("itemGroup.smogline.ntm_building_tab", "NTM Building Blocks");


                // EQUIPMENT
                add("item.smogline.alloy_sword", "Alloy Sword");
                add("item.smogline.alloy_pickaxe", "Alloy Pickaxe");
                add("item.smogline.alloy_axe", "Alloy Axe");
                add("item.smogline.alloy_hoe", "Alloy Hoe");
                add("item.smogline.alloy_shovel", "Alloy Shovel");

                add("item.smogline.steel_sword", "Steel Sword");
                add("item.smogline.steel_pickaxe", "Steel Pickaxe");
                add("item.smogline.steel_axe", "Steel Axe");
                add("item.smogline.steel_hoe", "Steel Hoe");
                add("item.smogline.steel_shovel", "Steel Shovel");

                add("gui.smogline.energy", "Energy: %s/%s HE");
                add("gui.smogline.shredder.blade_warning.title", "Blades missing!");
                add("gui.smogline.shredder.blade_warning.desc", "Install or repair the shredder blades.");
                add("item.smogline.titanium_sword", "Titanium Sword");
                add("item.smogline.titanium_pickaxe", "Titanium Pickaxe");
                add("item.smogline.titanium_axe", "Titanium Axe");
                add("item.smogline.titanium_hoe", "Titanium Hoe");
                add("item.smogline.titanium_shovel", "Titanium Shovel");

                add("item.smogline.starmetal_sword", "Starmetal Sword");
                add("item.smogline.starmetal_pickaxe", "Starmetal Pickaxe");
                add("item.smogline.starmetal_axe", "Starmetal Axe");
                add("item.smogline.starmetal_hoe", "Starmetal Hoe");
                add("item.smogline.starmetal_shovel", "Starmetal Shovel");

                // ARMOR

                add("item.smogline.alloy_helmet", "Alloy Helmet");
                add("item.smogline.alloy_chestplate", "Alloy Chestplate");
                add("item.smogline.alloy_leggings", "Alloy Leggings");
                add("item.smogline.alloy_boots", "Alloy Boots");

                add("item.smogline.cobalt_helmet", "Cobalt Helmet");
                add("item.smogline.cobalt_chestplate", "Cobalt Chestplate");
                add("item.smogline.cobalt_leggings", "Cobalt Leggings");
                add("item.smogline.cobalt_boots", "Cobalt Boots");

                add("item.smogline.titanium_helmet", "Titanium Helmet");
                add("item.smogline.titanium_chestplate", "Titanium Chestplate");
                add("item.smogline.titanium_leggings", "Titanium Leggings");
                add("item.smogline.titanium_boots", "Titanium Boots");

                add("item.smogline.security_helmet", "Security Helmet");
                add("item.smogline.security_chestplate", "Security Chestplate");
                add("item.smogline.security_leggings", "Security Leggings");
                add("item.smogline.security_boots", "Security Boots");

                add("item.smogline.ajr_helmet", "Steel Ranger Helmet");
                add("item.smogline.ajr_chestplate", "Steel Ranger Chestplate");
                add("item.smogline.ajr_leggings", "Steel Ranger Leggings");
                add("item.smogline.ajr_boots", "Steel Ranger Boots");

                add("item.smogline.steel_helmet", "Steel Helmet");
                add("item.smogline.steel_chestplate", "Steel Chestplate");
                add("item.smogline.steel_leggings", "Steel Leggings");
                add("item.smogline.steel_boots", "Steel Boots");

                add("item.smogline.asbestos_helmet", "Fire Proximity Helmet");
                add("item.smogline.asbestos_chestplate", "Fire Proximity Chestplate");
                add("item.smogline.asbestos_leggings", "Fire Proximity Leggings");
                add("item.smogline.asbestos_boots", "Fire Proximity Boots");

                add("item.smogline.hazmat_helmet", "Hazmat Helmet");
                add("item.smogline.hazmat_chestplate", "Hazmat Chestplate");
                add("item.smogline.hazmat_leggings", "Hazmat Leggings");
                add("item.smogline.hazmat_boots", "Hazmat Boots");

                add("item.smogline.liquidator_helmet", "Liquidator Suit Helmet");
                add("item.smogline.liquidator_chestplate", "Liquidator Suit Chestplate");
                add("item.smogline.liquidator_leggings", "Liquidator Suit Leggings");
                add("item.smogline.liquidator_boots", "Liquidator Suit Boots");

                add("item.smogline.paa_helmet", "PaA Battle Hazmat Suit Helmet");
                add("item.smogline.paa_chestplate", "PaA Chest Protection Plate");
                add("item.smogline.paa_leggings", "PaA Leg Reinforcements");
                add("item.smogline.paa_boots", "PaA ''good ol` shoes''");

                add("item.smogline.starmetal_helmet", "Starmetal Helmet");
                add("item.smogline.starmetal_chestplate", "Starmetal Chestplate");
                add("item.smogline.starmetal_leggings", "Starmetal Leggings");
                add("item.smogline.starmetal_boots", "Starmetal Boots");

                // ITEMS


                add(ModItems.BATTERY_POTATO.get(), "Potato Battery");
                add(ModItems.BATTERY.get(), "Battery");
                add(ModItems.BATTERY_RED_CELL.get(), "Red Energy Cell");
                add(ModItems.BATTERY_RED_CELL_6.get(), "Red Energy Cell x6");
                add(ModItems.BATTERY_RED_CELL_24.get(), "Red Energy Cell x24");
                add(ModItems.BATTERY_ADVANCED.get(), "Advanced Battery");
                add(ModItems.BATTERY_ADVANCED_CELL.get(), "Advanced Energy Cell");
                add(ModItems.BATTERY_ADVANCED_CELL_4.get(), "Advanced Energy Cell x4");
                add(ModItems.BATTERY_ADVANCED_CELL_12.get(), "Advanced Energy Cell x12");
                add(ModItems.BATTERY_LITHIUM.get(), "Lithium Battery");
                add(ModItems.BATTERY_LITHIUM_CELL.get(), "Lithium Energy Cell");
                add(ModItems.BATTERY_LITHIUM_CELL_3.get(), "Lithium Energy Cell x3");
                add(ModItems.BATTERY_LITHIUM_CELL_6.get(), "Lithium Energy Cell x6");
                add(ModItems.BATTERY_SCHRABIDIUM.get(), "Schrabidium Battery");
                add(ModItems.BATTERY_SCHRABIDIUM_CELL.get(), "Schrabidium Energy Cell");
                add(ModItems.BATTERY_SCHRABIDIUM_CELL_2.get(), "Schrabidium Energy Cell x2");
                add(ModItems.BATTERY_SCHRABIDIUM_CELL_4.get(), "Schrabidium Energy Cell x4");
                add(ModItems.BATTERY_SPARK.get(), "Spark Battery");
                add(ModItems.BATTERY_TRIXITE.get(), "Trixite Battery");
                add(ModItems.BATTERY_SPARK_CELL_6.get(), "Spark Energy Cell x6");
                add(ModItems.BATTERY_SPARK_CELL_25.get(), "Spark Energy Cell x25");
                add(ModItems.BATTERY_SPARK_CELL_100.get(), "Spark Energy Cell x100");
                add(ModItems.BATTERY_SPARK_CELL_1000.get(), "Spark Energy Cell x1000");
                add(ModItems.BATTERY_SPARK_CELL_2500.get(), "Spark Energy Cell x2500");
                add(ModItems.BATTERY_SPARK_CELL_10000.get(), "Spark Energy Cell x10000");
                add(ModItems.BATTERY_SPARK_CELL_POWER.get(), "Spark Power Cell");

                add(ModItems.WIRE_RED_COPPER.get(), "Red Copper Wire");
                add(ModItems.WIRE_COPPER.get(), "Copper Wire");
                add(ModItems.WIRE_ALUMINIUM.get(), "Aluminium Wire");
                add(ModItems.WIRE_GOLD.get(), "Golden Wire");
                add(ModItems.WIRE_TUNGSTEN.get(), "Tungsten Wire");
                add(ModItems.WIRE_MAGNETIZED_TUNGSTEN.get(), "Magnetized Tungsten Wire");
                add(ModItems.WIRE_FINE.get(), "Fine Wire");
                add(ModItems.WIRE_CARBON.get(), "Lead Wire");
                add(ModItems.WIRE_SCHRABIDIUM.get(), "Shrabidium Wire");
                add(ModItems.WIRE_ADVANCED_ALLOY.get(), "Advanced Alloy Wire");

                add(ModItems.STAMP_STONE_FLAT.get(), "Stone Flat Stamp");
                add(ModItems.STAMP_STONE_PLATE.get(), "Stone Plate Stamp");
                add(ModItems.STAMP_STONE_WIRE.get(), "Stone Wire Stamp");
                add(ModItems.STAMP_STONE_CIRCUIT.get(), "Stone Circuit Stamp");
                add(ModItems.STAMP_IRON_FLAT.get(), "Iron Flat Stamp");
                add(ModItems.STAMP_IRON_PLATE.get(), "Iron Plate Stamp");
                add(ModItems.STAMP_IRON_WIRE.get(), "Iron Wire Stamp");
                add(ModItems.STAMP_IRON_CIRCUIT.get(), "Iron Circuit Stamp");
                add(ModItems.STAMP_STEEL_FLAT.get(), "Steel Flat Stamp");
                add(ModItems.STAMP_STEEL_PLATE.get(), "Steel Plate Stamp");
                add(ModItems.STAMP_STEEL_WIRE.get(), "Steel Wire Stamp");
                add(ModItems.STAMP_STEEL_CIRCUIT.get(), "Steel Circuit Stamp");
                add(ModItems.STAMP_TITANIUM_FLAT.get(), "Titanium Flat Stamp");
                add(ModItems.STAMP_TITANIUM_PLATE.get(), "Titanium Plate Stamp");
                add(ModItems.STAMP_TITANIUM_WIRE.get(), "Titanium Wire Stamp");
                add(ModItems.STAMP_TITANIUM_CIRCUIT.get(), "Titanium Circuit Stamp");
                add(ModItems.STAMP_OBSIDIAN_FLAT.get(), "Obsidian Flat Stamp");
                add(ModItems.STAMP_OBSIDIAN_PLATE.get(), "Obsidian Plate Stamp");
                add(ModItems.STAMP_OBSIDIAN_WIRE.get(), "Obsidian Wire Stamp");
                add(ModItems.STAMP_OBSIDIAN_CIRCUIT.get(), "Obsidian Circuit Stamp");
                add(ModItems.STAMP_DESH_FLAT.get(), "Desh Flat Stamp");
                add(ModItems.STAMP_DESH_PLATE.get(), "Desh Plate Stamp");
                add(ModItems.STAMP_DESH_WIRE.get(), "Desh Wire Stamp");
                add(ModItems.STAMP_DESH_CIRCUIT.get(), "Desh Circuit Stamp");
                add(ModItems.STAMP_DESH_9.get(), "Desh 9mm Stamp");
                add(ModItems.STAMP_DESH_44.get(), "Desh .44 Magnum Stamp");
                add(ModItems.STAMP_DESH_50.get(), "Desh .50 BMG Stamp");
                add(ModItems.STAMP_DESH_357.get(), "Desh .357 Magnum Stamp");
                add(ModItems.STAMP_IRON_357.get(), "Iron .357 Magnum Stamp");
                add(ModItems.STAMP_IRON_44.get(), "Iron .44 Magnum Stamp");
                add(ModItems.STAMP_IRON_50.get(), "Iron .50 BMG Stamp");
                add(ModItems.STAMP_IRON_9.get(), "Iron 9mm Stamp");


                add(ModItems.STRAWBERRY.get(), "Strawberry");



                

                add(ModItems.GRENADE.get(), "Bouncing Grenade");
                add(ModItems.GRENADEHE.get(), "Powerful Bouncing Grenade");
                add(ModItems.GRENADEFIRE.get(), "Fire Bouncing Grenade");
                add(ModItems.GRENADESLIME.get(), "Bouncy Bouncing Grenade");
                add(ModItems.GRENADESMART.get(), "Smart Bouncing Grenade");

                add(ModItems.GRENADE_IF.get(), "IF Grenade");

                add("item.smogline.geiger_counter", "Geiger Counter");
                add("item.smogline.dosimeter", "Dosimeter");
                add("item.smogline.battery_creative", "Creative Battery");
                add("tooltip.smogline.creative_battery_desc","Provides an infinite amount of power");
                add("tooltip.smogline.creative_battery_flavor","To infinity... and beyond!!");
                add("item.smogline.blueprint_folder", "Template Folder");
                add("item.smogline.blueprint_folder.named", "Machine Template Folder");
                add("item.smogline.blueprint_folder.empty", "Empty folder");
                add("item.smogline.blueprint_folder.obsolete", "Folder is Deprecated (Group was removed)");
                add("item.smogline.blueprint_folder.desc", "Insert into Assembly Machine to unlock recipes");
                add("item.smogline.blueprint_folder.recipes", "Contains recipes:");
                add("gui.smogline.recipe_from_group", "From Group:");

                add("item.smogline.heart_piece", "Heart Piece");
                add(ModItems.HEART_CONTAINER.get(), "Heart Container");
                add(ModItems.HEART_BOOSTER.get(), "Heart Booster");
                add(ModItems.HEART_FAB.get(), "Heart of Darkness");
                add(ModItems.BLACK_DIAMOND.get(), "Black Diamond");

                add(ModItems.GHIORSIUM_CLADDING.get(), "Ghiorsium Cladding");
                add(ModItems.DESH_CLADDING.get(), "Desh Cladding");
                add(ModItems.RUBBER_CLADDING.get(), "Rubber Cladding");
                add(ModItems.LEAD_CLADDING.get(), "Lead Cladding");
                add(ModItems.PAINT_CLADDING.get(), "Lead Paint");

                add("item.smogline.radaway", "Radaway");
                add("effect.smogline.radaway", "Radiation cleansing");
                add("sounds.smogline.radaway_use", "Use of radaway");

                add(ModItems.TEMPLATE_FOLDER.get(), "Template Folder");
                add(ModItems.ASSEMBLY_TEMPLATE.get(), "Assembly Template: %s");
                add("tooltip.smogline.template_broken", "Broken template");
                add("tooltip.smogline.created_with_template_folder", "Created via Template Folder");
                add("tooltip.smogline.output", "Output: ");
                add("tooltip.smogline.input", "Input: ");
                add("tooltip.smogline.production_time", "Production time:");
                add("tooltip.smogline.seconds", "seconds");
                add("tooltip.smogline.energy_consumption", "Energy Consumption:");
                add("tooltip.smogline.tags", "Тags (OreDict):");
                add("item.smogline.template_folder.desc", "Machine Templates: Paper + Dye$Fluid IDs: Iron Plate + Dye$Press Stamps: Flat Stamp$Siren Tracks: Insulator + Steel Plate");
                add("desc.gui.template", "Insert Assembly Template");
                add("desc.gui.assembler.warning", "No valid template!");

// === ИНСТРУМЕНТЫ И УСТРОЙСТВА ===
                add("tooltip.smogline.crowbar.line1", "Tool for prying open containers.");
                add("tooltip.smogline.crowbar.line2", "Opens crates on right-click");
                add("tooltip.smogline.defuser.line1", "Device for disarming mines and bombs");
                add("tooltip.smogline.defuser.line2", "RMB on a compatible device to disarm");
                add("tooltip.smogline.mine.line1", "Can be defused");
                add("tooltip.smogline.gigadet.line1", "Was made for fun");

                add("tooltip.smogline.nuclear_charge.line1", "High-yield nuclear weapon!");
                add("tooltip.smogline.nuclear_charge.line2", "At the moment, this is the");
                add("tooltip.smogline.nuclear_charge.line3", "most destructive block in our mod.");
                add("tooltip.smogline.nuclear_charge.line4", "If the crater loaded incorrectly");
                add("tooltip.smogline.nuclear_charge.line5", "or without biomes, restart the world.");

                add("tooltip.smogline.mine_nuke.line1", "Nuclear weapon!");
                add("tooltip.smogline.mine_nuke.line2", "Blast radius: 35 meters");
                add("tooltip.smogline.mine_nuke.line3", "Can be defused");

                add("tooltip.smogline.dudnuke.line1", "High-yield nuclear weapon!");
                add("tooltip.smogline.dudnuke.line4", "If the crater loaded incorrectly");
                add("tooltip.smogline.dudnuke.line5", "or without biomes, restart the world");
                add("tooltip.smogline.dudnuke.line6", "Can be defused");

                add("tooltip.smogline.dudsalted.line1", "High-yield nuclear weapon!");
                add("tooltip.smogline.dudsalted.line4", "If the crater loaded incorrectly");
                add("tooltip.smogline.dudsalted.line5", "or without biomes, restart the world");
                add("tooltip.smogline.dudsalted.line6", "Can be defused");

                add("tooltip.smogline.dudfugas.line1", "High-yield explosion!");
                add("tooltip.smogline.dudfugas.line6", "Can be defused");

// ДЕТОНАТОР
                add("tooltip.smogline.detonator.target", "Target: ");
                add("tooltip.smogline.detonator.no_target", "No target");
                add("tooltip.smogline.detonator.right_click", "RMB - Activate");
                add("tooltip.smogline.detonator.shift_right_click", "Shift+RMB - Set target");

// СКАНЕР КЛАСТЕРОВ
                add("tooltip.smogline.depth_ores_scanner.scans_chunks", "Scans chunks for");
                add("tooltip.smogline.depth_ores_scanner.deep_clusters", "depth clusters beneath the player");
                add("tooltip.smogline.depth_ores_scanner.depth_warning", "works at depth -30 and below!");
// DEPTH ORES SCANNER (сообщения)
                add("message.smogline.depth_ores_scanner.invalid_height", "Scanner works only at height -30 or below!");
                add("message.smogline.depth_ores_scanner.directly_below", "Depth cluster directly below us!");
                add("message.smogline.depth_ores_scanner.in_chunk", "Depth cluster found in our chunk!");
                add("message.smogline.depth_ores_scanner.adjacent_chunk", "Depth cluster found in adjacent chunk!");
                add("message.smogline.depth_ores_scanner.none_found", "No depth clusters found nearby");

// ДЕТЕКТОР НЕФТИ (тултип)
                add("tooltip.smogline.oil_detector.scans_chunks", "Scans chunks for");
                add("tooltip.smogline.oil_detector.oil_deposits", "oil deposits beneath the player");

// ДЕТЕКТОР НЕФТИ (сообщения использования)
                add("message.smogline.oil_detector.directly_below", "Oil deposits directly below us!");
                add("message.smogline.oil_detector.in_chunk", "Oil found in our chunk!");
                add("message.smogline.oil_detector.adjacent_chunk", "Oil deposits found in adjacent chunk!");
                add("message.smogline.oil_detector.none_found", "No oil deposits found nearby");

// MULTI DETONATOR TOOLTIPS
                add("tooltip.smogline.multi_detonator.active_point", "➤ %s:");
                add("tooltip.smogline.multi_detonator.point_set", "✅ %s:");
                add("tooltip.smogline.multi_detonator.coordinates", "   %d, %d, %d");
                add("tooltip.smogline.multi_detonator.point_empty", "○ Point %d:");
                add("tooltip.smogline.multi_detonator.not_set", "   Not set");
                add("tooltip.smogline.multi_detonator.key_r", "R - open menu");
                add("tooltip.smogline.multi_detonator.shift_rmb", "Shift+RMB - save to active point");
                add("tooltip.smogline.multi_detonator.rmb_activate", "RMB - activate active point");

// MULTI DETONATOR MESSAGES
                add("message.smogline.multi_detonator.position_saved", "Position '%s' saved: %d, %d, %d");
                add("message.smogline.multi_detonator.no_coordinates", "No coordinates set!");
                add("message.smogline.multi_detonator.point_not_set", "Point %d not set!");
                add("message.smogline.multi_detonator.chunk_not_loaded", "Position not loaded!");
                add("message.smogline.multi_detonator.activated", "%s activated!");
                add("message.smogline.multi_detonator.activation_error", "Activation error!");
                add("message.smogline.multi_detonator.incompatible_block", "Block incompatible!");
// RANGE DETONATOR
                add("tooltip.smogline.range_detonator.desc", "Activates compatible blocks");
                add("tooltip.smogline.range_detonator.hint", "along a ray up to 256 blocks.");
                add("message.smogline.range_detonator.pos_not_loaded", "Position incompatible or not loaded");
                add("message.smogline.range_detonator.activated", "Successfully activated");


                add("tooltip.smogline.grenade_nuc.line1", "Nuclear weapon!");
                add("tooltip.smogline.grenade_nuc.line2", "Blast radius: 25 meters");
                add("tooltip.smogline.grenade_nuc.line3", "Fuse time: 7s");
                add("tooltip.smogline.detminer.line1", "Does not damage entities or players");
                add("tooltip.smogline.detminer.line4", "Allows mining depth ores and stone");


                add("tooltip.smogline.grenade.common.line1", "Hand grenade");

                add("tooltip.smogline.grenade.smart.line2", "Detonates on direct hit with an entity");
                add("tooltip.smogline.grenade.fire.line2", "Leaves fire after detonation");
                add("tooltip.smogline.grenade.slime.line2", "Bounces strongly off surfaces");
                add("tooltip.smogline.grenade.standard.line2", "Weak fragmentation blast");
                add("tooltip.smogline.grenade.he.line2", "Enhanced high-explosive blast");
                add("tooltip.smogline.grenade.default.line2", "Throw it and watch it boom!");

                add("tooltip.smogline.grenade_if.common.line1", "IF-Grenade");

                add("tooltip.smogline.grenade_if.he.line2", "Powerful high-explosive blast");
                add("tooltip.smogline.grenade_if.slime.line2", "Bounces strongly off surfaces");
                add("tooltip.smogline.grenade_if.fire.line2", "Leaves fire after detonation");
                add("tooltip.smogline.grenade_if.standard.line2", "Standard timed explosion");
                add("tooltip.smogline.grenade_if.default.line2", "Throw it and wait for the boom");

                add("tooltip.smogline.depthstone.line1", "Can be mined or destroyed only by explosion!");
                add("tooltip.smogline.depthstone.line4", "Use Det Miner to safe-mine depth ores");

// en_us case
                // английский:

                add(ModBlocks.BURNED_GRASS.get(), "Burned Grass");
                add(ModBlocks.WASTE_PLANKS.get(), "Burned Planks");
                add(ModBlocks.WASTE_LOG.get(), "Burned Log");
                add(ModBlocks.SELLAFIELD_SLAKED.get(), "Slaked Sellafield");
                add(ModBlocks.SELLAFIELD_SLAKED1.get(), "Slaked Sellafield I");
                add(ModBlocks.SELLAFIELD_SLAKED2.get(), "Slaked Sellafield II");
                add(ModBlocks.SELLAFIELD_SLAKED3.get(), "Slaked Sellafield III");



                add(ModItems.AMMO_TURRET_PIERCING.get(), "20mm Turret AP Ammo");
                add(ModItems.AMMO_TURRET_FIRE.get(), "20mm Turret Fire Ammo");
                add(ModItems.AMMO_TURRET.get(), "20mm Turret Ammo");
                add(ModItems.AMMO_TURRET_RADIO.get(), "20mm Turret Ammo With Radio-Detonator");
                add(ModItems.MACHINEGUN.get(), "A.P.-17");
                add(ModBlocks.TURRET_LIGHT.get(), "Light Landing Turret 'Nagual'");




                add(ModBlocks.CONVERTER_BLOCK.get(), "Energy Converter");
                add(ModBlocks.MACHINE_BATTERY_DINEUTRONIUM.get(), "Spark Battery");
                add(ModBlocks.MACHINE_BATTERY_SCHRABIDIUM.get(), "Shrabidium Battery");
                add(ModBlocks.MACHINE_BATTERY_LITHIUM.get(), "Lithium Battery");
                // en_us case



                add(ModItems.BLADE_TEST.get(), "Desh Blades");
                add(ModItems.BLADE_STEEL.get(), "Steel Blades");
                add(ModItems.BLADE_TITANIUM.get(), "Titanium Blades");
                add(ModItems.BLADE_ALLOY.get(), "Advanced Alloy Blades");

                add(ModBlocks.CRATE_CONSERVE.get(), "Canned Goods Crate");
                add(ModBlocks.CAGE_LAMP.get(), "Cage Lamp");
                add(ModBlocks.FLOOD_LAMP.get(), "Flood Lamp");
                add(ModBlocks.TAPE_RECORDER.get(), "Tape Recorder");

                add(ModBlocks.MINE_FAT.get(), "FatMan Mine");
                add(ModBlocks.MINE_AP.get(), "Anti-Personnel Mine");
                add(ModItems.GRENADE_NUC.get(), "Nuclear Grenade");
                add(ModItems.GRENADE_IF_HE.get(), "IF Grenade: HE");
                add(ModItems.GRENADE_IF_FIRE.get(), "IF Grenade: Incendiary");
                add(ModItems.GRENADE_IF_SLIME.get(), "IF Grenade: Bouncy");
                add(ModItems.MULTI_DETONATOR.get(), "Multi Detonator");
                add(ModItems.RANGE_DETONATOR.get(), "Range Detonator");
                add(ModItems.DETONATOR.get(), "Detonator");
                add(ModBlocks.BARBED_WIRE_POISON.get(), "Poison Barbed Wire");
                add(ModBlocks.BARBED_WIRE_FIRE.get(), "Fire Barbed Wire");
                add(ModBlocks.BARBED_WIRE_RAD.get(), "Radiation Barbed Wire");
                add(ModBlocks.BARBED_WIRE.get(), "Barbed Wire");
                add(ModBlocks.BARBED_WIRE_WITHER.get(), "Wither Barbed Wire");
                add(ModBlocks.WASTE_CHARGE.get(), "Waste Charge");
                add(ModBlocks.GIGA_DET.get(), "Giga Det");
                add(ModBlocks.NUCLEAR_CHARGE.get(), "Nuclear Charge");
                add(ModBlocks.C4.get(), "C4 Charge");
                add(ModItems.DEFUSER.get(), "Defuser");
                add(ModItems.CROWBAR.get(), "Crowbar");
                add(ModItems.DEPTH_ORES_SCANNER.get(), "Depth Ore Scanner");
                add(ModItems.OIL_DETECTOR.get(), "Oil Detector");


                add(ModBlocks.SMOKE_BOMB.get(), "Semtex");


                add("item.smogline.firebrick", "Firebrick");
                add("item.smogline.uranium_raw", "Raw Uranium");
                add("item.smogline.tungsten_raw", "Raw Tungsten");
                add("item.smogline.titanium_raw", "Raw Titanium");
                add("item.smogline.thorium_raw", "Raw Thorium");
                add("item.smogline.lead_raw", "Raw Lead");
                add("item.smogline.cobalt_raw", "Raw Cobalt");
                add("item.smogline.beryllium_raw", "Raw Beryllium");
                add("item.smogline.aluminum_raw", "Raw Aluminum");
                add("item.smogline.cinnabar", "Cinnabar");
                add("item.smogline.sulfur", "Sulfur");
                add("item.smogline.rareground_ore_chunk", "Rareground Ore Chunk");
                add("item.smogline.lignite", "Lignite");
                add("item.smogline.fluorite", "Fluorite");
                add("item.smogline.fireclay_ball", "Fireclay Ball");
                add("item.smogline.wood_ash_powder", "Wood Ash Powder");

                
                add("tooltip.smogline.mods", "Modifications:");
                add("tooltip.smogline.heart_piece.effect", "+5 Max Health");
                
                add("tooltip.smogline.applies_to", "Applies to:");

                // ARMOR MODIFICATION TABLE TOOLTIPS

                add("tooltip.smogline.helmet", "Helmet");
                add("tooltip.smogline.chestplate", "Chestplate");
                add("tooltip.smogline.leggings", "Leggings");
                add("tooltip.smogline.boots", "Boots");
                add("tooltip.smogline.armor.all", "Any Armor");

                add("tooltip.smogline.armor_table.main_slot", "Insert armor to be modified...");
                add("tooltip.smogline.slot", "Slot");
                add("tooltip.smogline.armor_table.helmet_slot", "Helmet");
                add("tooltip.smogline.armor_table.chestplate_slot", "Chestplate");
                add("tooltip.smogline.armor_table.leggings_slot", "Leggings");
                add("tooltip.smogline.armor_table.boots_slot", "Boots");
                add("tooltip.smogline.armor_table.battery_slot", "Battery");
                add("tooltip.smogline.armor_table.special_slot", "Special");
                add("tooltip.smogline.armor_table.plating_slot", "Plating");
                add("tooltip.smogline.armor_table.casing_slot", "Casing");
                add("tooltip.smogline.armor_table.servos_slot", "Servos");

                add("gui.smogline.blast_furnace.accepts", "Accepts items from: %s");
                add("direction.smogline.down", "Down");
                add("direction.smogline.up", "Up");
                add("direction.smogline.north", "North");
                add("direction.smogline.south", "South");
                add("direction.smogline.west", "West");
                add("direction.smogline.east", "East");
                add("gui.smogline.anvil.inputs", "Inputs:");
                add("gui.smogline.anvil.outputs", "Outputs:");
                add("gui.smogline.anvil.search", "Search");
                add("gui.smogline.anvil.search_hint", "Search...");
                add("gui.smogline.anvil.tier", "Required Tier: %s");
                add("tier.smogline.anvil.iron", "Iron");
                add("tier.smogline.anvil.steel", "Steel");
                add("tier.smogline.anvil.oil", "Oil");
                add("tier.smogline.anvil.nuclear", "Nuclear");
                add("tier.smogline.anvil.rbmk", "RBMK");
                add("tier.smogline.anvil.fusion", "Fusion");
                add("tier.smogline.anvil.particle", "Particle");
                add("tier.smogline.anvil.gerald", "Gerald");
                add("tier.smogline.anvil.murky", "Murky");

                // BLOCKS
                add("block.smogline.anvil_block", "Industrial Anvil");
                add("block.smogline.anvil_iron", "Iron Anvil");
                add("block.smogline.anvil_lead", "Lead Anvil");
                add("block.smogline.anvil_steel", "Steel Anvil");
                add("block.smogline.anvil_desh", "Desh Anvil");
                add("block.smogline.anvil_ferrouranium", "Ferrouranium Anvil");
                add("block.smogline.anvil_saturnite", "Saturnite Anvil");
                add("block.smogline.anvil_bismuth_bronze", "Bismuth Bronze Anvil");
                add("block.smogline.anvil_arsenic_bronze", "Arsenic Bronze Anvil");
                add("block.smogline.anvil_schrabidate", "Schrabidate Anvil");
                add("block.smogline.anvil_dnt", "DNT Anvil");
                add("block.smogline.anvil_osmiridium", "Osmiridium Anvil");
                add("block.smogline.anvil_murky", "Murky Anvil");
                add("block.smogline.door_office", "Office Door");
                add("block.smogline.door_bunker", "Bunker Door");
                add("block.smogline.metal_door", "Metal Door");
                add("block.smogline.demon_lamp", "Demon Lamp (WIP)");
                add("block.smogline.explosive_charge", "Explosive Charge");
                add("block.smogline.reinforced_glass", "Reinforced Glass");
                add("block.smogline.crate", "Crate");
                add("block.smogline.crate_lead", "Lead Crate");
                add("block.smogline.crate_metal", "Metal Crate");
                add("block.smogline.crate_weapon", "Weapon Crate");
                add("block.smogline.uranium_block", "Uranium Block");
                add("block.smogline.plutonium_block", "Plutonium Block");
                add("block.smogline.plutonium_fuel_block", "Plutonium Fuel Block");
                add("block.smogline.polonium210_block", "Polonium-210 Block");
                add("block.smogline.armor_table", "Armor Modification Table");
                add("block.smogline.machine_assembler", "Assembly Machine (Legacy)");
                add("block.smogline.advanced_assembly_machine", "Assembly Machine");
                add("block.smogline.machine_battery", "Machine Battery");
                add("block.smogline.shredder", "Shredder");
                add("block.smogline.wood_burner", "Wood Burner Generator");
                add("block.smogline.blast_furnace", "Blast Furnace");
                add("block.smogline.blast_furnace_extension", "Blast Furnace Extension");
                add("block.smogline.press", "Press");
                add("block.smogline.crate_iron", "Iron Crate");
                add("block.smogline.crate_steel", "Steel Crate");
                add("block.smogline.crate_desh", "Desh Crate");

                add("block.smogline.det_miner", "Det Miner");
                add("block.smogline.concrete_vent", "Concrete Vent");
                add("block.smogline.concrete_fan", "Concrete Fan");
                add("block.smogline.concrete_marked", "Marked Concrete");
                add("block.smogline.concrete_cracked", "Cracked Concrete");
                add("block.smogline.concrete_mossy", "Mossy Concrete");
                add("block.smogline.concrete", "Concrete");
                add("block.smogline.concrete_cracked_stairs", "Concrete Cracked Stairs");
                add("block.smogline.concrete_cracked_slab", "Concrete Cracked Slab");
                add("block.smogline.concrete_mossy_stairs", "Concrete Mossy Stairs");
                add("block.smogline.concrete_mossy_slab", "Concrete Mossy Slab");
                add("block.smogline.brick_concrete", "Concrete Bricks");
                add("block.smogline.brick_concrete_slab", "Concrete Bricks Slab");
                add("block.smogline.brick_concrete_stairs", "Concrete Bricks Stairs");
                add("block.smogline.brick_concrete_broken", "Broken Concrete Bricks");
                add("block.smogline.brick_concrete_broken_slab", "Broken Concrete Bricks Slab");
                add("block.smogline.brick_concrete_broken_stairs", "Broken Concrete Bricks Stairs");
                add("block.smogline.brick_concrete_cracked", "Cracked Concrete Bricks");
                add("block.smogline.brick_concrete_cracked_slab", "Cracked Concrete Bricks Slab");
                add("block.smogline.brick_concrete_cracked_stairs", "Cracked Concrete Bricks Stairs");
                add("block.smogline.brick_concrete_mossy", "Mossy Concrete Bricks");
                add("block.smogline.brick_concrete_mossy_slab", "Mossy Concrete Bricks Slab");
                add("block.smogline.brick_concrete_mossy_stairs", "Mossy Concrete Bricks Stairs");
                add("block.smogline.brick_concrete_marked", "Marked Concrete Bricks");

                add("block.smogline.concrete_hazard", "Concrete Block with Hazard line");
                add("block.smogline.concrete_hazard_slab", "Concrete Slab with Hazard line");
                add("block.smogline.concrete_hazard_stairs", "Concrete Stairs with Hazard line");
                add("block.smogline.concrete_stairs", "Concrete Stairs");
                add("block.smogline.concrete_slab", "Concrete Slab");
                add("block.smogline.large_vehicle_door", "Large Vehicle Door");
                add("block.smogline.round_airlock_door", "Round Airlock Door");
                add("block.smogline.strawberry_bush", "Strawberry Bush");

                add("block.smogline.geiger_counter_block", "Geiger Counter Block");
                add("block.smogline.wire_coated", "Red Copper Wire");

                // ORES
                add(ModBlocks.SEQUESTRUM_ORE.get(), "Salpeter Ore");
                add(ModItems.SEQUESTRUM.get(), "Salpeter");
                add(ModItems.AIRSTRIKE_TEST.get(), "Airstrike");
                add(ModBlocks.RESOURCE_ASBESTOS.get(), "Asbestos Cluster");
                add(ModBlocks.RESOURCE_BAUXITE.get(), "Bauxite");
                add(ModBlocks.RESOURCE_HEMATITE.get(), "Hematite");
                add(ModBlocks.RESOURCE_LIMESTONE.get(), "Limestone");
                add(ModBlocks.RESOURCE_MALACHITE.get(), "Malachite");
                add(ModBlocks.RESOURCE_SULFUR.get(), "Sulfur Cluster");
                add("block.smogline.cinnabar_ore_deepslate", "Deepslate Cinnabar Ore");
                add("block.smogline.cobalt_ore_deepslate", "Deepslate Cobalt Ore");
                add("block.smogline.uranium_ore", "Uranium Ore");
                add("block.smogline.aluminum_ore", "Aluminum Ore");
                add("block.smogline.aluminum_ore_deepslate", "Deepslate Aluminum Ore");
                add("block.smogline.titanium_ore", "Titanium Ore");
                add("block.smogline.titanium_ore_deepslate", "Deepslate Titanium Ore");
                add("block.smogline.tungsten_ore", "Tungsten Ore");
                add("block.smogline.asbestos_ore", "Asbestos Ore");
                add("block.smogline.sulfur_ore", "Sulfur Ore");
                add("block.smogline.cobalt_ore", "Cobalt Ore");
                add("block.smogline.uranium_ore_h", "High-Yield Uranium Ore");
                add("block.smogline.uranium_ore_deepslate", "Deepslate Uranium Ore");
                add("block.smogline.thorium_ore", "Thorium Ore");
                add("block.smogline.thorium_ore_deepslate", "Deepslate Thorium Ore");
                add("block.smogline.rareground_ore", "Rare Earth Ore");
                add("block.smogline.rareground_ore_deepslate", "Deepslate Rare Earth Ore");
                add("block.smogline.lignite_ore", "Lignite Ore");
                add("block.smogline.beryllium_ore", "Beryllium Ore");
                add("block.smogline.beryllium_ore_deepslate", "Deepslate Beryllium Ore");
                add("block.smogline.fluorite_ore", "Fluorite Ore");
                add("block.smogline.lead_ore", "Lead Ore");
                add("block.smogline.lead_ore_deepslate", "Deepslate Lead Ore");
                add("block.smogline.cinnabar_ore", "Cinnabar Ore");
                add("block.smogline.waste_grass", "Waste Grass");
                add("block.smogline.waste_leaves", "Waste Leaves");
                add("block.smogline.freaky_alien_block", "Freaky Allien Block");
                add("block.smogline.reinforced_stone", "Reinforced Stone");
                add("block.smogline.reinforced_stone_slab", "Reinforced Stone Slab");
                add("block.smogline.reinforced_stone_stairs", "Reinforced Stone Stairs");
                add("block.smogline.switch", "Switch");
                add("tooltip.smogline.rad_protection.value", "Radiation Resistance: %s");
                add("tooltip.smogline.rad_protection.value_short", "%s rad-resistance");

                // MACHINE GUI

                add("container.inventory", "Inventory");
                add("container.smogline.armor_table", "Armor Modification Table");
                add("container.smogline.machine_assembler", "Assembly Machine");
                add("container.smogline.wood_burner", "Wood Burner Generator");
                add("container.smogline.advanced_assembly_machine", "Assembly Machine");
                add("container.smogline.machine_battery", "Machine Battery");
                add("container.smogline.press", "Press");
                add("container.smogline.anvil_block", "Industrial Anvil");
                add("container.smogline.anvil", "%s Anvil");
                add("container.smogline.crate_iron", "Iron Crate");
                add("container.smogline.crate_steel", "Steel Crate");
                add("container.smogline.crate_desh", "Desh Crate");
                add("gui.smogline.battery.priority.0", "Priority: Low");
                add("gui.smogline.battery.priority.0.desc", "Lowest priority. Will be drained first and filled last.");
                add("gui.smogline.battery.priority.1", "Priority: Normal");
                add("gui.smogline.battery.priority.1.desc", "Standard priority for energy transfer.");
                add("gui.smogline.battery.priority.2", "Priority: High");
                add("gui.smogline.battery.priority.2.desc", "Highest priority. Will be filled first and drained last.");
                add("gui.smogline.battery.priority.recommended", "(Recommended)");

                add("gui.smogline.battery.condition.no_signal", "When there is NO redstone signal:");
                add("gui.smogline.battery.condition.with_signal", "When there IS a redstone signal:");

                add("gui.smogline.battery.mode.both", "Mode: Input & Output");
                add("gui.smogline.battery.mode.both.desc", "All energy operations are allowed.");
                add("gui.smogline.battery.mode.input", "Mode: Input Only");
                add("gui.smogline.battery.mode.input.desc", "Only receiving energy is allowed.");
                add("gui.smogline.battery.mode.output", "Mode: Output Only");
                add("gui.smogline.battery.mode.output.desc", "Only sending energy is allowed.");
                add("gui.smogline.battery.mode.locked", "Mode: Locked");
                add("gui.smogline.battery.mode.locked.desc", "All energy operations are disabled.");

                add("gui.recipe.setRecipe", "Set Recipe");

                add("tooltip.smogline.machine_battery.capacity", "Capacity: %1$s HE");
                add("tooltip.smogline.machine_battery.charge_speed", "Charge Speed: %1$s HE/t");
                add("tooltip.smogline.machine_battery.discharge_speed", "Discharge Speed: %1$s HE/t");
                add("tooltip.smogline.machine_battery.stored", "Stored: %1$s / %2$s HE");

                // HAZARD TOOLTIPS

                add("hazard.smogline.radiation", "[Radioactive]");
                add("hazard.smogline.radiation.format", "%s RAD/s");
                add("hazard.smogline.hydro_reactive", "[Hydro-reactive]");
                add("hazard.smogline.explosive_on_fire", "[Flammable / Explosive]");
                add("hazard.smogline.pyrophoric", "[Pyrophoric / Hot]");
                add("hazard.smogline.explosion_strength.format", " Explosion Strength - %s");
                add("hazard.smogline.stack", "Stack: %s");

                add("tooltip.smogline.abilities", "Abilities:");
                add("tooltip.smogline.vein_miner", "Vein Miner (%s)");
                add("tooltip.smogline.aoe", "AOE (%s x %s x %s)");
                add("tooltip.smogline.silk_touch", "Silk Touch");
                add("tooltip.smogline.fortune", "Fortune (%s)");
                add("tooltip.smogline.right_click", "Right click - toggle ability");
                add("tooltip.smogline.shift_right_click", "Shift + Right click - disable all");

                add("message.smogline.vein_miner.enabled", "Vein Miner %s enabled!");
                add("message.smogline.vein_miner.disabled", "Vein Miner %s disabled!");
                add("message.smogline.aoe.enabled", "AOE %s x %s x %s enabled!");
                add("message.smogline.aoe.disabled", "AOE %s x %s x %s disabled!");
                add("message.smogline.silk_touch.enabled", "Silk Touch enabled!");
                add("message.smogline.silk_touch.disabled", "Silk Touch disabled!");
                add("message.smogline.fortune.enabled", "Fortune %s enabled!");
                add("message.smogline.fortune.disabled", "Fortune %s disabled!");
                add("message.smogline.disabled", "All abilities disabled!");

                add("item.smogline.meter.geiger_counter.name", "GEIGER COUNTER");
                add("item.smogline.meter.dosimeter.name", "DOSIMETER");
                add("item.smogline.meter.title_format", "%s");
                add("smogline.render.shader_detected", "§e[HBM] §7External shader detected. Switching to compatible renderer...");
                add("smogline.render.shader_disabled", "§a[HBM] §7Shader disabled. Returning to optimized VBO renderer.");
                add("smogline.render.path_changed", "§e[HBM] §7Render path set to: %s");
                add("smogline.render.status", "§e[HBM] §7Current render path: §f%s\n§7External shader detected: §f%s");

                add("item.smogline.meter.chunk_rads", "§eCurrent chunk radiation: %s\n");
                add("item.smogline.meter.env_rads", "§eTotal environment contamination: %s");
                add("item.smogline.meter.player_rads", "§ePlayer contamination: %s\n");
                add("item.smogline.meter.protection", "§ePlayer protection: %s (%s)");

                add("item.smogline.meter.rads_over_limit", ">%s RAD/s");
                add("gui.smogline.battery.energy.info", "%s / %s HE");
                add("gui.smogline.battery.energy.delta", "%s HE/t");
                add("tooltip.smogline.hold_shift_for_details", "<Hold SHIFT to display more info>");

                add("sounds.smogline.geiger_counter", "Geiger Counter clicking");
                add("sounds.smogline.tool.techboop", "Geiger counter beep");
                
                add("commands.smogline.rad.cleared", "Radiation cleared for %s players.");
                add("commands.smogline.rad.cleared.self", "Your radiation has been cleared.");
                add("commands.smogline.rad.added", "Added %s radiation to %s players.");
                add("commands.smogline.rad.added.self", "You have been given %s radiation.");
                add("commands.smogline.rad.removed", "Removed %s radiation from %s players.");
                add("commands.smogline.rad.removed.self", "%s radiation has been removed from you.");

                add("death.attack.radiation", "Player %s died from radiation sickness");
                add("advancements.smogline.radiation_200.title", "Hooray, Radiation!");
                add("advancements.smogline.radiation_200.description", "Reach a radiation level of 200 RAD");
                add("advancements.smogline.radiation_1000.title", "Ouch, Radiation!");
                add("advancements.smogline.radiation_1000.description", "Die from radiation sickness");

                add("chat.smogline.structure.obstructed", "Placement obstructed by other blocks!");
                
                add("text.autoconfig.smogline.title", "Radiation Settings (HBM Modernized)");

                // CONFIG

                add("text.autoconfig.smogline.category.general", "General Settings");
                add("text.autoconfig.smogline.option.enableRadiation", "Enable radiation");
                add("text.autoconfig.smogline.option.enableChunkRads", "Enable chunk radiation");
                add("text.autoconfig.smogline.option.usePrismSystem", "Use PRISM system (otherwise Simple), WIP");

                add("text.autoconfig.smogline.category.world_effects", "World Effects");

                add("text.autoconfig.smogline.option.worldRadEffects", "World Radiation Effects");
                add("text.autoconfig.smogline.option.worldRadEffects.@Tooltip", "Enables/disables world destruction effects from high radiation (block replacement, vegetation decay, etc.).");

                add("text.autoconfig.smogline.option.worldRadEffectsThreshold", "World Destruction Threshold");
                add("text.autoconfig.smogline.option.worldRadEffectsThreshold.@Tooltip", "The minimum ambient radiation level in a chunk at which world destruction effects begin.");

                add("text.autoconfig.smogline.option.worldRadEffectsBlockChecks", "Block Checks per Tick");
                add("text.autoconfig.smogline.option.worldRadEffectsBlockChecks.@Tooltip", "The number of random block checks in an affected chunk per tick. Affects the speed of destruction. Higher values may impact performance.");

                add("text.autoconfig.smogline.option.worldRadEffectsMaxScaling", "Max Destruction Scaler");
                add("text.autoconfig.smogline.option.worldRadEffectsMaxScaling.@Tooltip", "The maximum speed multiplier for world destruction at peak radiation. 1 = no scaling, 4 = can be up to 4 times faster. Max value - 10x");

                add("text.autoconfig.smogline.option.worldRadEffectsMaxDepth", "Destruction Depth");
                add("text.autoconfig.smogline.option.worldRadEffectsMaxDepth.@Tooltip", "The maximum depth (in blocks) from the surface that world destruction effects can reach.");

                add("text.autoconfig.smogline.option.enableRadFogEffect", "Enable Radiation Fog Effect");
                add("text.autoconfig.smogline.option.radFogThreshold", "Fog Threshold");
                add("text.autoconfig.smogline.option.radFogChance", "Fog Chance");

                add("text.autoconfig.smogline.category.player", "Player");

                add("text.autoconfig.smogline.option.maxPlayerRad", "Max player radiation level");
                add("text.autoconfig.smogline.option.radDecay", "Player radiation decay rate");
                add("text.autoconfig.smogline.option.radDamage", "Radiation damage");
                add("text.autoconfig.smogline.option.radDamageThreshold", "Radiation damage threshold");
                add("text.autoconfig.smogline.option.radSickness", "Nausea threshold");
                add("text.autoconfig.smogline.option.radWater", "Water negative effect threshold, WIP");
                add("text.autoconfig.smogline.option.radConfusion", "Confusion threshold, WIP");
                add("text.autoconfig.smogline.option.radBlindness", "Blindness threshold");

                add("text.autoconfig.smogline.category.overlay", "Screen Overlays");

                add("text.autoconfig.smogline.option.enableRadiationPixelEffect", "Enable Radiation Screen Pixel Effect");
                add("text.autoconfig.smogline.option.radiationPixelEffectThreshold", "Pixel Effect Threshold");
                add("text.autoconfig.smogline.option.radiationPixelMaxIntensityRad", "Pixel Effect Max Intensity");
                add("text.autoconfig.smogline.option.radiationPixelEffectMaxDots", "Max Pixel Count");
                add("text.autoconfig.smogline.option.radiationPixelEffectGreenChance", "Green Pixel Chance");
                add("text.autoconfig.smogline.option.radiationPixelMinLifetime", "Min Pixel Lifetime");
                add("text.autoconfig.smogline.option.radiationPixelMaxLifetime", "Max Pixel Lifetime");
                add("text.autoconfig.smogline.option.enableObstructionHighlight", "Enable Obstruction Highlight");
                add("text.autoconfig.smogline.option.enableObstructionHighlight.@Tooltip", "If enabled, blocks obstructing multiblock placement\nwill be highlighted with a red box.");
                add("text.autoconfig.smogline.option.obstructionHighlightAlpha", "Obstruction Highlight Opacity");
                add("text.autoconfig.smogline.option.obstructionHighlightAlpha.@Tooltip", "Sets the opacity of the highlight box's fill.\n0% = Invisible, 100% = Solid.");

                add("text.autoconfig.smogline.option.obstructionHighlightDuration", "Highlight Duration (sec)");
                add("text.autoconfig.smogline.option.obstructionHighlightDuration.@Tooltip", "The duration in seconds for how long the obstruction highlight will be visible.");

                add("text.autoconfig.smogline.category.chunk", "Chunk");

                add("text.autoconfig.smogline.option.maxRad", "Max chunk radiation");
                add("text.autoconfig.smogline.option.fogRad", "Fog radiation threshold");
                add("text.autoconfig.smogline.option.fogCh", "Fog chance (1 in fogCh), WIP");
                add("text.autoconfig.smogline.option.radChunkDecay", "Chunk radiation decay rate");
                add("text.autoconfig.smogline.option.radChunkSpreadFactor", "Chunk radiation spread factor");
                add("text.autoconfig.smogline.option.radSpreadThreshold", "Radiation spread threshold");
                add("text.autoconfig.smogline.option.minRadDecayAmount", "Min decay per tick");
                add("text.autoconfig.smogline.option.radSourceInfluenceFactor", "Source influence factor");
                add("text.autoconfig.smogline.option.radRandomizationFactor", "Chunk radiation randomization factor");

                add("text.autoconfig.smogline.category.rendering", "Rendering");

                add("text.autoconfig.smogline.option.modelUpdateDistance", "Distance for .obj model dynamic parts rendering");
                add("text.autoconfig.smogline.option.enableOcclusionCulling", "Enable model occlusion culling");

                add("text.autoconfig.smogline.category.debug", "Debug");

                add("text.autoconfig.smogline.option.enableDebugRender", "Enable radiation debug render");
                add("text.autoconfig.smogline.option.debugRenderTextSize", "Debug render text size");
                add("text.autoconfig.smogline.option.debugRenderDistance", "Debug render distance (chunks)");
                add("text.autoconfig.smogline.option.debugRenderInSurvival", "Show debug render in survival mode");
                add("text.autoconfig.smogline.option.enableDebugLogging", "Enable debug logging");

                add("key.smogline.open_config", "Open configuration menu");
                add("key.categories.smogline", "HBM Modernized");

                add("text.autoconfig.smogline.option.enableRadiation.@Tooltip", "If disabled, all radiation is turned off (chunks, items)");
                add("text.autoconfig.smogline.option.enableChunkRads.@Tooltip", "If disabled, chunk radiation is always 0");
                add("text.autoconfig.smogline.option.usePrismSystem.@Tooltip", "Use PRISM system for chunk radiation (WIP)");

                add("text.autoconfig.smogline.option.enableRadFogEffect.@Tooltip", "Enables/disables the radioactive fog particle effect in chunks with high radiation levels.");
                add("text.autoconfig.smogline.option.radFogThreshold.@Tooltip", "The minimum ambient radiation level in a chunk for the fog effect to appear.");
                add("text.autoconfig.smogline.option.radFogChance.@Tooltip", "The chance for fog particles to spawn in a suitable chunk per second. Calculated as 1 in X. A lower value means more frequent fog.");

                add("text.autoconfig.smogline.option.maxPlayerRad.@Tooltip", "Maximum radiation the player can accumulate");
                add("text.autoconfig.smogline.option.radDecay.@Tooltip", "How fast player radiation decays per tick");
                add("text.autoconfig.smogline.option.radDamage.@Tooltip", "Damage per tick when above threshold (Will be reworked)");
                add("text.autoconfig.smogline.option.radDamageThreshold.@Tooltip", "Player starts taking damage above this value");
                add("text.autoconfig.smogline.option.radSickness.@Tooltip", "Threshold for nausea effect");
                add("text.autoconfig.smogline.option.radWater.@Tooltip", "Threshold for water negative effect (WIP)");
                add("text.autoconfig.smogline.option.radConfusion.@Tooltip", "Threshold for confusion effect (WIP)");
                add("text.autoconfig.smogline.option.radBlindness.@Tooltip", "Threshold for blindness effect");

                add("text.autoconfig.smogline.option.enableRadiationPixelEffect.@Tooltip", "Shows random, flickering pixels on the screen when the player is exposed to incoming radiation.");
                add("text.autoconfig.smogline.option.radiationPixelEffectThreshold.@Tooltip", "The minimum incoming radiation (RAD/s) required for the visual interference effect to appear.");
                add("text.autoconfig.smogline.option.radiationPixelMaxIntensityRad.@Tooltip", "The level of incoming radiation (RAD/s) at which the pixel effect reaches its maximum strength (maximum number of pixels).");
                add("text.autoconfig.smogline.option.radiationPixelEffectMaxDots.@Tooltip", "The maximum number of pixels that can be on the screen at once when the effect is at its peak intensity. Affects performance on weak systems.");
                add("text.autoconfig.smogline.option.radiationPixelEffectGreenChance.@Tooltip", "The probability (from 0.0 to 1.0) that a newly appeared pixel will be green instead of white. E.g., 0.1 = 10% chance.");
                add("text.autoconfig.smogline.option.radiationPixelMinLifetime.@Tooltip", "The minimum time (in ticks) a single pixel will stay on the screen. 20 ticks = 1 second.");
                add("text.autoconfig.smogline.option.radiationPixelMaxLifetime.@Tooltip", "The maximum time (in ticks) a single pixel will stay on the screen. A random value between min and max lifetime is chosen for each pixel.");
                
                add("text.autoconfig.smogline.option.maxRad.@Tooltip", "Maximum chunk radiation");
                add("text.autoconfig.smogline.option.fogRad.@Tooltip", "Chunk radiation for fog to appear (WIP)");
                add("text.autoconfig.smogline.option.fogCh.@Tooltip", "Chance for fog to appear (WIP)");
                add("text.autoconfig.smogline.option.radChunkDecay.@Tooltip", "How fast chunk radiation decays");
                add("text.autoconfig.smogline.option.radChunkSpreadFactor.@Tooltip", "How much radiation spreads to neighboring chunks");
                add("text.autoconfig.smogline.option.radSpreadThreshold.@Tooltip", "Below this, radiation doesn't spread");
                add("text.autoconfig.smogline.option.minRadDecayAmount.@Tooltip", "Minimum decay per tick in chunk");
                add("text.autoconfig.smogline.option.radSourceInfluenceFactor.@Tooltip", "Influence of radioactive blocks in chunk");
                add("text.autoconfig.smogline.option.radRandomizationFactor.@Tooltip", "Randomization factor for chunk radiation");

                add("text.autoconfig.smogline.option.modelUpdateDistance.@Tooltip", "Distance for .obj model dynamic parts rendering (in chunks)");
                add("text.autoconfig.smogline.option.enableOcclusionCulling.@Tooltip", "Enable model occlusion culling (disable if your models are not rendering correctly)");

                add("text.autoconfig.smogline.option.enableDebugRender.@Tooltip", "Whether radiation debug render is enabled (F3)");
                add("text.autoconfig.smogline.option.debugRenderTextSize.@Tooltip", "Debug render text size");
                add("text.autoconfig.smogline.option.debugRenderDistance.@Tooltip", "Debug render distance (in chunks)");
                add("text.autoconfig.smogline.option.debugRenderInSurvival.@Tooltip", "Show debug renderer in survival mode");
                add("text.autoconfig.smogline.option.enableDebugLogging.@Tooltip", "If disabled, deep logging of game events will be active. Do not enable unless you experience problems");
                break;
        }
    }
}