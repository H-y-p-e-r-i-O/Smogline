package com.smogline.datagen.assets;

import java.util.LinkedHashMap;
import com.smogline.block.ModBlocks;
import com.smogline.item.tags_and_tiers.ModIngots;
import com.smogline.item.ModItems;
import com.smogline.item.tags_and_tiers.ModPowders;
import com.smogline.lib.RefStrings;
import com.smogline.main.MainRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItemModelProvider extends ItemModelProvider {

    private static LinkedHashMap<ResourceKey<TrimMaterial>, Float> trimMaterials = new LinkedHashMap<>();
    static {
        trimMaterials.put(TrimMaterials.QUARTZ, 0.1F);
        trimMaterials.put(TrimMaterials.IRON, 0.2F);
        trimMaterials.put(TrimMaterials.NETHERITE, 0.3F);
        trimMaterials.put(TrimMaterials.REDSTONE, 0.4F);
        trimMaterials.put(TrimMaterials.COPPER, 0.5F);
        trimMaterials.put(TrimMaterials.GOLD, 0.6F);
        trimMaterials.put(TrimMaterials.EMERALD, 0.7F);
        trimMaterials.put(TrimMaterials.DIAMOND, 0.8F);
        trimMaterials.put(TrimMaterials.LAPIS, 0.9F);
        trimMaterials.put(TrimMaterials.AMETHYST, 1.0F);
    }

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, RefStrings.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // ✅ ИСПРАВЛЕННЫЙ ЦИКЛ ДЛЯ СЛИТКОВ
        for (ModIngots ingot : ModIngots.values()) {
            RegistryObject<Item> ingotObject = ModItems.getIngot(ingot);
            if (ingotObject != null && ingotObject.isPresent()) {
                ingotItem(ingotObject);
            }
        }

        // ✅ ИСПРАВЛЕННЫЙ ЦИКЛ ДЛЯ ModPowders (ОСНОВНАЯ ОШИБКА!)
        for (ModPowders powder : ModPowders.values()) {
            RegistryObject<Item> powderObject = ModItems.getPowders(powder);
            if (powderObject != null && powderObject.isPresent()) {
                powdersItem(powderObject);
            }
        }

        // ✅ ИСПРАВЛЕННЫЙ ЦИКЛ ДЛЯ ПОРОШКОВ ИЗ СЛИТКОВ
        for (ModIngots ingot : ModIngots.values()) {
            RegistryObject<Item> powder = ModItems.getPowder(ingot);
            if (powder != null && powder.isPresent() && powderTextureExists(ingot.getName())) {
                powdersItem(powder);
            }
            ModItems.getTinyPowder(ingot).ifPresent(tiny -> {
                if (tiny != null && tiny.isPresent() && powderTinyTextureExists(ingot.getName())) {
                    tinyPowderItem(tiny);
                }
            });
        }


        // Door items (flat icons like vanilla doors)
        withExistingParent(ModBlocks.METAL_DOOR.getId().getPath(), "item/generated")
            .texture("layer0", modLoc("item/" + ModBlocks.METAL_DOOR.getId().getPath()));
        withExistingParent(ModBlocks.DOOR_BUNKER.getId().getPath(), "item/generated")
            .texture("layer0", modLoc("item/" + ModBlocks.DOOR_BUNKER.getId().getPath()));
        withExistingParent(ModBlocks.DOOR_OFFICE.getId().getPath(), "item/generated")
            .texture("layer0", modLoc("item/" + ModBlocks.DOOR_OFFICE.getId().getPath()));

        // РЕГИСТРАЦИЯ МОДЕЛЕЙ ДЛЯ УНИКАЛЬНЫХ ПРЕДМЕТОВ 
        // Для предметов, зарегистрированных вручную, мы также можем генерировать модели.
        simpleItem(ModItems.DEFUSER);
        simpleItem(ModItems.TURRET_REMOVER);
        simpleItem(ModItems.AIRSTRIKE_AGENT);
        simpleItem(ModItems.SCREWDRIVER);
        simpleItem(ModItems.CROWBAR);
        simpleItem(ModItems.OIL_DETECTOR);
        simpleItem(ModItems.MULTI_DETONATOR);
        simpleItem(ModItems.AIRSTRIKE_TEST);
        simpleItem(ModItems.AIRSTRIKE_HEAVY);
        simpleItem(ModItems.DETONATOR);
        simpleItem(ModItems.SEQUESTRUM);
        simpleItem(ModItems.TURRET_CHIP);
        simpleItem(ModItems.BLADE_STEEL);
        simpleItem(ModItems.BLADE_TITANIUM);
        simpleItem(ModItems.BLADE_ALLOY);
        simpleItem(ModItems.BLADE_TEST);
        simpleItem(ModItems.GEIGER_COUNTER);
        simpleItem(ModItems.DOSIMETER);
        simpleItem(ModItems.HEART_PIECE);
        simpleItem(ModItems.HEART_CONTAINER);
        simpleItem(ModItems.HEART_BOOSTER);
        simpleItem(ModItems.HEART_FAB);
        simpleItem(ModItems.BLACK_DIAMOND);
        simpleItem(ModItems.GHIORSIUM_CLADDING);
        simpleItem(ModItems.DESH_CLADDING);
        simpleItem(ModItems.LEAD_CLADDING);
        simpleItem(ModItems.RUBBER_CLADDING);
        simpleItem(ModItems.PAINT_CLADDING);
        simpleItem(ModItems.RADAWAY);
        simpleItem(ModItems.CREATIVE_BATTERY);
        simpleItem(ModItems.TEMPLATE_FOLDER);
        simpleItem(ModItems.STRAWBERRY);
        simpleItem(ModItems.INFINITE_WATER_500);
        simpleItem(ModItems.INFINITE_WATER_5000);
        simpleItem(ModItems.LIMESTONE);
        simpleItem(ModItems.MALACHITE_CHUNK);
        simpleItem(ModItems.GRENADE_NUC);
        simpleItem(ModItems.GRENADE_IF_HE);
        simpleItem(ModItems.GRENADE_IF_FIRE);
        simpleItem(ModItems.GRENADE_IF_SLIME);
        simpleItem(ModItems.GRENADE_IF);
        simpleItem(ModItems.BATTERY_SCHRABIDIUM);
        simpleItem(ModItems.BATTERY_POTATO);
        simpleItem(ModItems.BATTERY);
        simpleItem(ModItems.AIRSTRIKE_NUKE);
        simpleItem(ModItems.BATTERY_RED_CELL);
        simpleItem(ModItems.BATTERY_RED_CELL_6);
        simpleItem(ModItems.BATTERY_RED_CELL_24);
        simpleItem(ModItems.BATTERY_ADVANCED);
        simpleItem(ModItems.BATTERY_ADVANCED_CELL);
        simpleItem(ModItems.BATTERY_ADVANCED_CELL_4);
        simpleItem(ModItems.BATTERY_ADVANCED_CELL_12);
        simpleItem(ModItems.BATTERY_LITHIUM);
        simpleItem(ModItems.BATTERY_LITHIUM_CELL);
        simpleItem(ModItems.BATTERY_LITHIUM_CELL_3);
        simpleItem(ModItems.BATTERY_LITHIUM_CELL_6);
        simpleItem(ModItems.BATTERY_SCHRABIDIUM_CELL);
        simpleItem(ModItems.BATTERY_SCHRABIDIUM_CELL_2);
        simpleItem(ModItems.BATTERY_SCHRABIDIUM_CELL_4);
        simpleItem(ModItems.BATTERY_SPARK);
        simpleItem(ModItems.BATTERY_TRIXITE);
        simpleItem(ModItems.BATTERY_SPARK_CELL_6);
        simpleItem(ModItems.BATTERY_SPARK_CELL_25);
        simpleItem(ModItems.BATTERY_SPARK_CELL_100);
        simpleItem(ModItems.BATTERY_SPARK_CELL_1000);
        simpleItem(ModItems.BATTERY_SPARK_CELL_2500);
        simpleItem(ModItems.BATTERY_SPARK_CELL_10000);
        simpleItem(ModItems.BATTERY_SPARK_CELL_POWER);
        simpleItem(ModItems.DEPTH_ORES_SCANNER);
        simpleItem(ModItems.STAMP_STONE_FLAT);
        simpleItem(ModItems.STAMP_STONE_PLATE);
        simpleItem(ModItems.STAMP_STONE_WIRE);
        simpleItem(ModItems.STAMP_STONE_CIRCUIT);
        simpleItem(ModItems.STAMP_IRON_FLAT);
        simpleItem(ModItems.STAMP_IRON_PLATE);
        simpleItem(ModItems.STAMP_IRON_WIRE);
        simpleItem(ModItems.STAMP_IRON_CIRCUIT);
        simpleItem(ModItems.STAMP_IRON_9);
        simpleItem(ModItems.STAMP_IRON_44);
        simpleItem(ModItems.STAMP_IRON_50);
        simpleItem(ModItems.STAMP_IRON_357);
        simpleItem(ModItems.STAMP_STEEL_FLAT);
        simpleItem(ModItems.STAMP_STEEL_PLATE);
        simpleItem(ModItems.STAMP_STEEL_WIRE);
        simpleItem(ModItems.STAMP_STEEL_CIRCUIT);
        simpleItem(ModItems.STAMP_TITANIUM_FLAT);
        simpleItem(ModItems.STAMP_TITANIUM_PLATE);
        simpleItem(ModItems.STAMP_TITANIUM_WIRE);
        simpleItem(ModItems.STAMP_TITANIUM_FLAT);
        simpleItem(ModItems.STAMP_TITANIUM_PLATE);
        simpleItem(ModItems.STAMP_TITANIUM_WIRE);
        simpleItem(ModItems.STAMP_TITANIUM_CIRCUIT);
        simpleItem(ModItems.STAMP_OBSIDIAN_FLAT);
        simpleItem(ModItems.STAMP_OBSIDIAN_PLATE);
        simpleItem(ModItems.STAMP_OBSIDIAN_WIRE);
        simpleItem(ModItems.STAMP_OBSIDIAN_CIRCUIT);
        simpleItem(ModItems.STAMP_DESH_FLAT);
        simpleItem(ModItems.STAMP_DESH_PLATE);
        simpleItem(ModItems.STAMP_DESH_WIRE);
        simpleItem(ModItems.STAMP_DESH_CIRCUIT);
        simpleItem(ModItems.STAMP_DESH_9);
        simpleItem(ModItems.STAMP_DESH_44);
        simpleItem(ModItems.STAMP_DESH_50);
        simpleItem(ModItems.STAMP_DESH_357);
        simpleItem(ModItems.WIRE_RED_COPPER);
        simpleItem(ModItems.WIRE_COPPER);
        simpleItem(ModItems.WIRE_TUNGSTEN);
        simpleItem(ModItems.WIRE_ALUMINIUM);
        simpleItem(ModItems.WIRE_FINE);
        simpleItem(ModItems.WIRE_SCHRABIDIUM);
        simpleItem(ModItems.WIRE_ADVANCED_ALLOY);
        simpleItem(ModItems.WIRE_GOLD);
        simpleItem(ModItems.WIRE_MAGNETIZED_TUNGSTEN);
        simpleItem(ModItems.WIRE_CARBON);


        trimmedArmorItem(ModItems.SECURITY_HELMET);
        trimmedArmorItem(ModItems.SECURITY_CHESTPLATE);
        trimmedArmorItem(ModItems.SECURITY_LEGGINGS);
        trimmedArmorItem(ModItems.SECURITY_BOOTS);


        simpleBlockItem(ModBlocks.DOOR_BUNKER);
        simpleBlockItem(ModBlocks.DOOR_OFFICE);
        simpleBlockItem(ModBlocks.METAL_DOOR);
        simpleItem(ModItems.GRENADEHE);
        simpleItem(ModItems.GRENADEFIRE);

        ModBlocks.getAnvilBlocks().forEach(this::blockItemFromBlockModel);
    };

    /**
     * Вспомогательный метод для генерации простой модели предмета.
     * Он предполагает, что модель имеет родителя "item/generated" и одну текстуру "layer0".
     * Это стандарт для большинства 2D предметов в Minecraft.
     * @param itemObject RegistryObject предмета, для которого генерируется модель.
     */

    private void simpleItem(RegistryObject<Item> itemObject) {
        // Получаем имя предмета из его ID (например, "uranium_ingot")
        String name = itemObject.getId().getPath();
        
        // Генерируем .json файл модели.
        // Он будет искать текстуру по пути 'assets/smogline/textures/item/ИМЯ_ПРЕДМЕТА.png'
        withExistingParent(name, "item/generated")
                .texture("layer0", modLoc("item/" + name));

    }
    private ItemModelBuilder simpleBlockItem(RegistryObject<Block> item) {
        return withExistingParent(item.getId().getPath(),
                ResourceLocation.parse("item/generated")).texture("layer0",
                ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID,"item/" + item.getId().getPath()));
    }

    private ItemModelBuilder blockItemFromBlockModel(RegistryObject<Block> block) {
        return withExistingParent(block.getId().getPath(), modLoc("block/" + block.getId().getPath()));
    }
    
    private void ingotItem(RegistryObject<Item> itemObject) {
        // 1. Получаем регистрационное имя (например, "uranium_ingot")
        String registrationName = itemObject.getId().getPath();
        
        // 2. Получаем базовое имя (например, "uranium")
        String baseName = registrationName.replace("_ingot", "");

        // 3. Формируем ИМЯ ФАЙЛА ТЕКСТУРЫ (например, "ingot_uranium")
        String textureFileName = "ingot_" + baseName;
        
        // Генерируем .json файл модели
        // Имя файла модели совпадает с регистрационным именем
        withExistingParent(registrationName, "item/generated")
                // Путь к текстуре теперь использует правильное имя файла и подпапку
                .texture("layer0", modLoc("item/ingot/" + textureFileName));
    }

    // ✅ ОСНОВНОЕ ИСПРАВЛЕНИЕ - powdersItem с проверкой!
    private void powdersItem(RegistryObject<Item> itemObject) {
        String registrationName = itemObject.getId().getPath();
        String baseName = registrationName.replace("_powder", "");
        String textureFileName = "powder_" + baseName;
        withExistingParent(registrationName, "item/generated")
                .texture("layer0", modLoc("item/powders/" + textureFileName));
    }

    private void tinyPowderItem(RegistryObject<Item> itemObject) {
        String registrationName = itemObject.getId().getPath();
        String baseName = registrationName.replace("_powder_tiny", "");
        String textureFileName = "powder_" + baseName + "_tiny";
        withExistingParent(registrationName, "item/generated")
                .texture("layer0", modLoc("item/powders/tiny/" + textureFileName));
    }

    private void powderTexture(RegistryObject<Item> itemObject, String texturePath) {
        withExistingParent(itemObject.getId().getPath(), "item/generated")
                .texture("layer0", modLoc("item/" + texturePath));
    }

    private boolean powderTextureExists(String baseName) {
        ResourceLocation texture = modLoc("textures/item/powders/powder_" + baseName + ".png");
        return existingFileHelper.exists(texture, PackType.CLIENT_RESOURCES);
    }

    private boolean powderTinyTextureExists(String baseName) {
        ResourceLocation texture = modLoc("textures/item/powders/tiny/powder_" + baseName + "_tiny.png");
        return existingFileHelper.exists(texture, PackType.CLIENT_RESOURCES);
    }

    private void trimmedArmorItem(RegistryObject<Item> itemRegistryObject) {
        final String MOD_ID = MainRegistry.MOD_ID; // Change this to your mod id

        if(itemRegistryObject.get() instanceof ArmorItem armorItem) {
            trimMaterials.entrySet().forEach(entry -> {

                ResourceKey<TrimMaterial> trimMaterial = entry.getKey();
                float trimValue = entry.getValue();

                String armorType = switch (armorItem.getEquipmentSlot()) {
                    case HEAD -> "helmet";
                    case CHEST -> "chestplate";
                    case LEGS -> "leggings";
                    case FEET -> "boots";
                    default -> "";
                };

                String armorItemPath = "item/" + armorItem;
                String trimPath = "trims/items/" + armorType + "_trim_" + trimMaterial.location().getPath();
                String currentTrimName = armorItemPath + "_" + trimMaterial.location().getPath() + "_trim";
                ResourceLocation armorItemResLoc = ResourceLocation.fromNamespaceAndPath(MOD_ID, armorItemPath);
                ResourceLocation trimResLoc = ResourceLocation.parse(trimPath); // minecraft namespace
                ResourceLocation trimNameResLoc = ResourceLocation.fromNamespaceAndPath(MOD_ID, currentTrimName);

                existingFileHelper.trackGenerated(trimResLoc, PackType.CLIENT_RESOURCES, ".png", "textures");

                getBuilder(currentTrimName)
                        .parent(new ModelFile.UncheckedModelFile("item/generated"))
                        .texture("layer0", armorItemResLoc)
                        .texture("layer1", trimResLoc);

                this.withExistingParent(itemRegistryObject.getId().getPath(),
                                mcLoc("item/generated"))
                        .override()
                        .model(new ModelFile.UncheckedModelFile(trimNameResLoc))
                        .predicate(mcLoc("trim_type"), trimValue).end()
                        .texture("layer0",
                                ResourceLocation.fromNamespaceAndPath(MOD_ID,
                                        "item/" + itemRegistryObject.getId().getPath()));
            });
        }
    }

    public void evenSimplerBlockItem(RegistryObject<Block> block) {
        this.withExistingParent(MainRegistry.MOD_ID + ":" + ForgeRegistries.BLOCKS.getKey(block.get()).getPath(),
                modLoc("block/" + ForgeRegistries.BLOCKS.getKey(block.get()).getPath()));
    }

}
