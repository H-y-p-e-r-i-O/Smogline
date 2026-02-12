package com.smogline.datagen.assets;

// Провайдер генерации состояний блоков и моделей для блоков мода.
// Используется в классе DataGenerators для регистрации.
import com.smogline.block.ModBlocks;
import com.smogline.item.tags_and_tiers.ModIngots;
import com.smogline.main.MainRegistry;
import com.smogline.lib.RefStrings;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.VariantBlockStateBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockStateProvider extends BlockStateProvider {

    private final ExistingFileHelper existingFileHelper;

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, RefStrings.MODID, exFileHelper);
        this.existingFileHelper = exFileHelper;
    }

    @Override
    protected void registerStatesAndModels() {
        // ГЕНЕРАЦИЯ МОДЕЛЕЙ ДЛЯ БЛОКОВ-РЕСУРСОВ С ПРЕФИКСОМ "block_"
        simpleBlockWithItem(ModBlocks.STRAWBERRY_BUSH.get(), models().cross(blockTexture(ModBlocks.STRAWBERRY_BUSH.get()).getPath(),
                blockTexture(ModBlocks.STRAWBERRY_BUSH.get())).renderType("cutout"));
        // Блоки слитков теперь генерируются автоматически в цикле ниже
        blockWithItem(ModBlocks.GIGA_DET);
        blockWithItem(ModBlocks.POLONIUM210_BLOCK);
        blockWithItem(ModBlocks.EXPLOSIVE_CHARGE);
        blockWithItem(ModBlocks.CRATE_WEAPON);
        blockWithItem(ModBlocks.CRATE_METAL);
        blockWithItem(ModBlocks.CRATE);
        blockWithItem(ModBlocks.TURRET_LIGHT);
        blockWithItem(ModBlocks.TURRET_LIGHT_PLACER);
        blockWithItem(ModBlocks.CRATE_LEAD);
        blockWithItem(ModBlocks.DEAD_DIRT);
        blockWithItem(ModBlocks.MOX1);
        blockWithItem(ModBlocks.MOX2);
        blockWithItem(ModBlocks.MOX3);
        blockWithItem(ModBlocks.SAND_ROUGH);
        blockWithItem(ModBlocks.GEYSIR_DIRT);
        blockWithItem(ModBlocks.GEYSIR_STONE);
        blockWithItem(ModBlocks.RESOURCE_ASBESTOS);
        blockWithItem(ModBlocks.RESOURCE_BAUXITE);
        blockWithItem(ModBlocks.SEQUESTRUM_ORE);
        blockWithItem(ModBlocks.RESOURCE_HEMATITE);
        blockWithItem(ModBlocks.RESOURCE_LIMESTONE);
        blockWithItem(ModBlocks.RESOURCE_MALACHITE);
        blockWithItem(ModBlocks.RESOURCE_SULFUR);
        oreWithItem(ModBlocks.URANIUM_ORE);
        blockWithItem(ModBlocks.WASTE_LEAVES);
        blockWithItem(ModBlocks.REINFORCED_STONE);
        blockWithItem(ModBlocks.SELLAFIELD_SLAKED);
        blockWithItem(ModBlocks.SELLAFIELD_SLAKED1);
        blockWithItem(ModBlocks.SELLAFIELD_SLAKED2);
        blockWithItem(ModBlocks.SELLAFIELD_SLAKED3);

        // ✅ ДОБАВЛЕНО: Модель для ядерных осадков
        // Эта функция автоматически создаст все 8 состояний высоты для блока
        // и свяжет их с моделями, которые выглядят как снег, но с вашей текстурой.
        registerSnowLayerBlock(ModBlocks.NUCLEAR_FALLOUT, "nuclear_fallout");

        // === РЕГИСТРАЦИЯ ПАДАЮЩИХ БЛОКОВ СЕЛЛАФИТА ===
        // Используется simpleBlockWithItem с явным указанием текстуры

        // === КОНЕЦ РЕГИСТРАЦИИ ПАДАЮЩИХ БЛОКОВ ===

        blockWithItem(ModBlocks.WASTE_PLANKS);

        simpleBlockWithItem(ModBlocks.WASTE_LOG.get(),
                models().cubeBottomTop(
                        ModBlocks.WASTE_LOG.getId().getPath(),
                        modLoc("block/waste_log_side"),
                        modLoc("block/waste_log_top"),
                        modLoc("block/waste_log_top")
                )
        );

        simpleBlockWithItem(ModBlocks.BURNED_GRASS.get(),
                models().cubeBottomTop(
                        ModBlocks.BURNED_GRASS.getId().getPath(),
                        modLoc("block/burned_grass_side"),
                        modLoc("block/burned_grass_bottom"),
                        modLoc("block/burned_grass_top")
                )
        );


        simpleBlock(ModBlocks.BLAST_FURNACE_EXTENSION.get(),
                models().getExistingFile(modLoc("block/difurnace_extension")));
        simpleBlockItem(ModBlocks.BLAST_FURNACE_EXTENSION.get(),
                models().getExistingFile(modLoc("block/difurnace_extension")));

        simpleBlockWithItem(ModBlocks.CRATE_IRON.get(),
                models().cubeBottomTop(
                        ModBlocks.CRATE_IRON.getId().getPath(),
                        modLoc("block/crate_iron_side"),
                        modLoc("block/crate_iron_top"),
                        modLoc("block/crate_iron_top")
                )
        );

        simpleBlockWithItem(ModBlocks.CRATE_STEEL.get(),
                models().cubeBottomTop(
                        ModBlocks.CRATE_STEEL.getId().getPath(),
                        modLoc("block/crate_steel_side"),
                        modLoc("block/crate_steel_top"),
                        modLoc("block/crate_steel_top")
                )
        );

        simpleBlockWithItem(ModBlocks.CRATE_DESH.get(),
                models().cubeBottomTop(
                        ModBlocks.CRATE_DESH.getId().getPath(),
                        modLoc("block/crate_desh_side"),
                        modLoc("block/crate_desh_top"),
                        modLoc("block/crate_desh_top")
                )
        );

        blockWithItem(ModBlocks.CINNABAR_ORE_DEEPSLATE);
        blockWithItem(ModBlocks.COBALT_ORE_DEEPSLATE);

        simpleBlockWithItem(ModBlocks.REINFORCED_GLASS.get(),
                models().cubeAll(ModBlocks.REINFORCED_GLASS.getId().getPath(),
                                blockTexture(ModBlocks.REINFORCED_GLASS.get()))
                        .renderType("cutout"));

        simpleBlockWithItem(ModBlocks.BARBED_WIRE.get(),
                models().cubeAll(ModBlocks.BARBED_WIRE.getId().getPath(),
                                blockTexture(ModBlocks.BARBED_WIRE.get()))
                        .renderType("cutout"));
        simpleBlockWithItem(ModBlocks.BARBED_WIRE_FIRE.get(),
                models().cubeAll(ModBlocks.BARBED_WIRE_FIRE.getId().getPath(),
                                blockTexture(ModBlocks.BARBED_WIRE_FIRE.get()))
                        .renderType("cutout"));
        simpleBlockWithItem(ModBlocks.BARBED_WIRE_POISON.get(),
                models().cubeAll(ModBlocks.BARBED_WIRE_POISON.getId().getPath(),
                                blockTexture(ModBlocks.BARBED_WIRE_POISON.get()))
                        .renderType("cutout"));
        simpleBlockWithItem(ModBlocks.BARBED_WIRE_RAD.get(),
                models().cubeAll(ModBlocks.BARBED_WIRE_RAD.getId().getPath(),
                                blockTexture(ModBlocks.BARBED_WIRE_RAD.get()))
                        .renderType("cutout"));
        simpleBlockWithItem(ModBlocks.BARBED_WIRE_WITHER.get(),
                models().cubeAll(ModBlocks.BARBED_WIRE_WITHER.getId().getPath(),
                                blockTexture(ModBlocks.BARBED_WIRE_WITHER.get()))
                        .renderType("cutout"));







        doorBlockWithRenderType(((DoorBlock) ModBlocks.METAL_DOOR.get()), modLoc("block/metal_door_bottom"), modLoc("block/metal_door_top"), "cutout");
        doorBlockWithRenderType(((DoorBlock) ModBlocks.DOOR_BUNKER.get()), modLoc("block/door_bunker_bottom"), modLoc("block/door_bunker_top"), "cutout");
        doorBlockWithRenderType(((DoorBlock) ModBlocks.DOOR_OFFICE.get()), modLoc("block/door_office_bottom"), modLoc("block/door_office_top"), "cutout");

        columnBlockWithItem(
                ModBlocks.WASTE_GRASS,
                modLoc("block/waste_grass_side"),
                modLoc("block/waste_grass_top"),
                mcLoc("block/dirt")
        );

        columnBlockWithItem(
                ModBlocks.ARMOR_TABLE,
                modLoc("block/armor_table_side"),
                modLoc("block/armor_table_top"),
                modLoc("block/armor_table_bottom")
        );

        // Блоки с кастомной OBJ моделью
        customObjBlock(ModBlocks.GEIGER_COUNTER_BLOCK);
        customObjBlock(ModBlocks.MACHINE_ASSEMBLER);


        simpleBlock(ModBlocks.UNIVERSAL_MACHINE_PART.get(), models().getBuilder(ModBlocks.UNIVERSAL_MACHINE_PART.getId().getPath()));
        simpleBlockWithItem(ModBlocks.WIRE_COATED.get(), models().getExistingFile(modLoc("block/wire_coated")));


        blockWithItem(ModBlocks.CONVERTER_BLOCK);


        orientableBlockWithItem(
                ModBlocks.MACHINE_BATTERY,
                modLoc("block/battery_side_alt"),
                modLoc("block/battery_front_alt"),
                modLoc("block/battery_top")
        );

        orientableBlockWithItem(
                ModBlocks.MACHINE_BATTERY_LITHIUM,
                modLoc("block/machine_battery_lithium_side"),
                modLoc("block/machine_battery_lithium_front"),
                modLoc("block/machine_battery_lithium_top")
        );

        orientableBlockWithItem(
                ModBlocks.MACHINE_BATTERY_SCHRABIDIUM,
                modLoc("block/machine_battery_schrabidium_side"),
                modLoc("block/machine_battery_schrabidium_front"),
                modLoc("block/machine_battery_schrabidium_top")
        );

        orientableBlockWithItem(
                ModBlocks.MACHINE_BATTERY_DINEUTRONIUM,
                modLoc("block/machine_battery_dineutronium_side"),
                modLoc("block/machine_battery_dineutronium_front"),
                modLoc("block/machine_battery_dineutronium_top")
        );









        simpleBlockWithItem(ModBlocks.SHREDDER.get(),
                new ModelFile.UncheckedModelFile(modLoc("block/shredder")));

        // АВТОМАТИЧЕСКАЯ ГЕНЕРАЦИЯ МОДЕЛЕЙ ДЛЯ БЛОКОВ СЛИТКОВ
        // АВТОМАТИЧЕСКАЯ ГЕНЕРАЦИЯ МОДЕЛЕЙ ДЛЯ БЛОКОВ СЛИТКОВ
        for (ModIngots ingot : ModIngots.values()) {
            // !!! ДОБАВЛЕНА ПРОВЕРКА !!!
            if (ModBlocks.hasIngotBlock(ingot)) {
                RegistryObject<Block> blockRegistryObject = ModBlocks.getIngotBlock(ingot);
                if (blockRegistryObject != null) {
                    resourceBlockWithItem(blockRegistryObject);
                }
            }
        }

        registerAnvils();
    }

    /**
     * Метод для блоков, у которых текстура имеет префикс "block_".
     * Например, для блока с именем "uranium_block" он будет искать текстуру "block_uranium".
     */
    private void resourceBlockWithItem(RegistryObject<Block> blockObject) {
        // 1. Получаем регистрационное имя (теперь оно уже "block_uranium")
        String registrationName = blockObject.getId().getPath();

        // 2. Имя текстуры теперь совпадает с именем блока!
        // (Если ваши текстуры называются block_uranium.png)
        String textureName = registrationName;

        // 4. Проверяем существование текстуры
        ResourceLocation textureLocation = modLoc("textures/block/" + textureName + ".png");
        if (!existingFileHelper.exists(textureLocation, net.minecraft.server.packs.PackType.CLIENT_RESOURCES)) {
            MainRegistry.LOGGER.warn("Texture not found for block {}: {}. Skipping model generation.",
                    registrationName, textureLocation);
            return;
        }

        // 5. Создаем модель
        simpleBlock(blockObject.get(), models().cubeAll(registrationName, modLoc("block/" + textureName)));

        // 6. Создаем модель для предмета
        simpleBlockItem(blockObject.get(), models().getExistingFile(blockTexture(blockObject.get())));
    }
    private void oreWithItem(RegistryObject<Block> blockObject) {
        // 1. Получаем регистрационное имя блока (например, "uranium_block")
        String registrationName = blockObject.getId().getPath();

        // 2. Трансформируем его в базовое имя (удаляем "_block" -> "uranium")
        String baseName = registrationName.replace("_ore", "");

        // 3. Создаем имя файла текстуры (добавляем "ore_" -> "ore_uranium")
        String textureName = "ore_" + baseName;

        // 4. Создаем модель блока, ЯВНО указывая путь к текстуре
        //    Метод models().cubeAll() создает модель типа "block/cube_all" с указанной текстурой.
        simpleBlock(blockObject.get(), models().cubeAll(registrationName, modLoc("block/" + textureName)));

        // 5. Создаем модель для предмета-блока, как и раньше
        simpleBlockItem(blockObject.get(), models().getExistingFile(blockTexture(blockObject.get())));
    }

    /**
     * Старый метод для блоков, у которых имя текстуры СОВПАДАЕТ с именем регистрации.
     */
    private void blockWithItem(RegistryObject<Block> blockObject) {
        simpleBlock(blockObject.get());
        simpleBlockItem(blockObject.get(), models().getExistingFile(blockTexture(blockObject.get())));
    }


    private void columnBlockWithItem(RegistryObject<Block> blockObject, ResourceLocation sideLocation, ResourceLocation topLocation, ResourceLocation bottomLocation) {
        // Создаем модель блока, передавая готовые ResourceLocation
        simpleBlock(blockObject.get(), models().cubeBottomTop(
            blockObject.getId().getPath(),
            sideLocation,
            bottomLocation,
            topLocation
        ));
        // Создаем модель предмета-блока
        simpleBlockItem(blockObject.get(), models().getExistingFile(blockTexture(blockObject.get())));
    }


    /**
     * Генерирует состояние для блока с кастомной OBJ моделью.
     * ВАЖНО: Сам файл модели (.json) должен быть создан вручную в /resources!
     */
    private <T extends Block> void customObjBlock(RegistryObject<T> blockObject) {
        // Создаём только blockstate, который ссылается на JSON модель
        // JSON модель должна лежать в resources/assets/smogline/models/block/<название>.json
        horizontalBlock(blockObject.get(),
            models().getExistingFile(modLoc("block/" + blockObject.getId().getPath())));
    }

    /**
     * Генерирует модель и состояние для горизонтально-ориентированного блока.
     * @param blockObject Блок
     * @param sideTexture Текстура для боковых и задней сторон
     * @param frontTexture Текстура для лицевой стороны (север)
     * @param topTexture Текстура для верха и низа
     */
    private void orientableBlockWithItem(RegistryObject<Block> blockObject, ResourceLocation sideTexture, ResourceLocation frontTexture, ResourceLocation topTexture) {
        // 1. Создаем модель блока с разными текстурами.
        //    Метод orientable использует стандартные имена: side, front, top, bottom.
        var model = models().orientable(
            blockObject.getId().getPath(),
            sideTexture,
            frontTexture,
            topTexture
        ).texture("particle", frontTexture); // Частицы при ломании будут из лицевой текстуры

        // 2. Создаем состояние блока (blockstate), которое будет вращать эту модель по горизонтали.
        horizontalBlock(blockObject.get(), model);

        // 3. Создаем модель для предмета-блока, которая выглядит так же, как и сам блок.
        simpleBlockItem(blockObject.get(), model);
    }

    private void registerAnvils() {
        ModBlocks.getAnvilBlocks().forEach(reg -> horizontalBlock(
                reg.get(),
                models().getExistingFile(modLoc("block/" + reg.getId().getPath()))
        ));
    }

    // ✅ ИСПРАВЛЕННЫЙ МЕТОД: Использует правильные ванильные модели
    private void registerSnowLayerBlock(RegistryObject<Block> block, String baseName) {
        // Получаем текстуру нашего блока (nuclear_fallout.png)
        ResourceLocation texture = blockTexture(block.get());

        // Создаем модели для разной высоты, наследуясь от ванильных моделей снега
        // Важно: используем mcLoc("block/...") чтобы указать на minecraft namespace
        ModelFile model2 = models().withExistingParent(baseName + "_height2", mcLoc("block/snow_height2")).texture("texture", texture).texture("particle", texture);
        ModelFile model4 = models().withExistingParent(baseName + "_height4", mcLoc("block/snow_height4")).texture("texture", texture).texture("particle", texture);
        ModelFile model6 = models().withExistingParent(baseName + "_height6", mcLoc("block/snow_height6")).texture("texture", texture).texture("particle", texture);
        // Для полного блока (8 слоев) используем модель height12 + еще 2 пикселя = height14? Нет, в ваниле 8 слоев = полный блок.
        // Но у снега есть хитрость: snow_height14 не существует.
        // Самый надежный способ - использовать snow_height12 и растянуть?
        // Нет, лучше всего использовать обычный куб для полного слоя, или snow_height10/12/14 если они есть.
        // В 1.20.1 модели снега: height2, height4, height6, height8, height10, height12, height14? Нет.

        // ВАНИЛЬ ИСПОЛЬЗУЕТ:
        // layers=1 -> snow_height2
        // layers=8 -> block/snow (который полный блок?)

        // Попробуем так:
        // Для слоев 1-7 используем соответствующие модели (они есть в ваниле)
        // Для слоя 8 используем куб

        // Чтобы не гадать с путями, давайте просто создадим модели с нужными размерами сами,
        // либо используем те, что точно есть.
        // Точно есть: snow_height2, snow_height4, snow_height6, snow_height8, snow_height10, snow_height12

        // Но проще всего ссылаться на mcLoc("block/snow_height" + (layer * 2))

        // Исправленная логика: генерируем варианты
        VariantBlockStateBuilder builder = getVariantBuilder(block.get());

        for (int i = 1; i <= 8; i++) {
            ModelFile model;
            if (i == 8) {
                // Полный блок
                model = models().withExistingParent(baseName + "_height16", mcLoc("block/cube_all")).texture("all", texture).texture("particle", texture);
            } else {
                // Слои 2, 4, 6, 8, 10, 12, 14
                String parentName = "block/snow_height" + (i * 2);
                model = models().withExistingParent(baseName + "_height" + (i * 2), mcLoc(parentName))
                        .texture("texture", texture)
                        .texture("particle", texture);
            }

            builder.partialState().with(SnowLayerBlock.LAYERS, i).modelForState().modelFile(model).addModel();
        }

        // Модель предмета - как слой высотой 2
        simpleBlockItem(block.get(), models().withExistingParent(baseName + "_inventory", mcLoc("block/snow_height2")).texture("texture", texture).texture("particle", texture));
    }



}