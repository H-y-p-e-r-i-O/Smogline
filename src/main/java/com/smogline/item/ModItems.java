package com.smogline.item;

// Класс для регистрации всех предметов мода.
// Использует DeferredRegister для отложенной регистрации. Здесь так же регистрируются моды для брони.
// Слитки регистрируются автоматически на основе перечисления ModIngots.

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;


import com.smogline.api.fluids.ModFluids;

import com.smogline.item.custom.rotation.MotorElectroBlockItem;
import com.smogline.item.custom.rotation.ShaftIronBlockItem;
import com.smogline.item.custom.rotation.WindGenFlugerBlockItem;
import com.smogline.item.custom.weapons.ammo.AmmoTurretItem;
import com.smogline.item.custom.weapons.grenades_and_activators.*;
import com.smogline.item.custom.weapons.guns.MachineGunItem;
import com.smogline.item.custom.industrial.TurretRemoverItem;
import com.smogline.item.custom.fekal_electric.ItemCreativeBattery;
import com.smogline.item.custom.fekal_electric.ModBatteryItem;
import com.smogline.item.custom.crates.IronCrateItem;
import com.smogline.item.custom.crates.SteelCrateItem;
import com.smogline.item.custom.industrial.*;
import com.smogline.item.custom.liquids.InfiniteWaterItem;
import com.smogline.item.custom.liquids.ItemFluidIdentifier;
import com.smogline.item.custom.radiation_meter.ItemDosimeter;
import com.smogline.item.custom.radiation_meter.ItemGeigerCounter;
import com.smogline.item.custom.food.ItemConserve;
import com.smogline.item.custom.food.ItemEnergyDrink;
import com.smogline.item.custom.food.ModFoods;
import com.smogline.item.custom.tools_and_armor.*;
import com.smogline.item.custom.weapons.mines.MineApBlockItem;
import com.smogline.item.custom.weapons.turrets.TurretChipItem;
import com.smogline.item.custom.weapons.turrets.TurretLightPlacerBlockItem;
import com.smogline.item.tags_and_tiers.*;
import com.smogline.item.custom.scanners.DepthOresScannerItem;
import com.smogline.item.custom.scanners.OilDetectorItem;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.List;

import com.smogline.block.custom.machines.armormod.item.ItemModHealth;
import com.smogline.block.custom.machines.armormod.item.ItemModRadProtection;
import com.smogline.block.ModBlocks;
import com.smogline.effect.ModEffects;
import com.smogline.entity.ModEntities;
import com.smogline.entity.weapons.grenades.GrenadeIfType;
import com.smogline.entity.weapons.grenades.GrenadeType;
import com.smogline.multiblock.MultiblockBlockItem;
import com.smogline.sound.ModSounds;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.smogline.lib.RefStrings.MODID;


public class ModItems {
    // Создаем отложенный регистратор для предметов.
    // Это стандартный способ регистрации объектов в Forge.
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final Map<ModIngots, RegistryObject<Item>> INGOTS = new EnumMap<>(ModIngots.class);
    public static final Map<ModPowders, RegistryObject<Item>> POWDERS = new EnumMap<>(ModPowders.class);
    public static final Map<ModIngots, RegistryObject<Item>> INGOT_POWDERS = new EnumMap<>(ModIngots.class);
    public static final Map<ModIngots, RegistryObject<Item>> INGOT_POWDERS_TINY = new EnumMap<>(ModIngots.class);

    private static final Set<String> POWDER_TINY_NAMES = Set.of(
            "actinium", "boron", "cerium", "cobalt", "cs137", "i131",
            "lanthanium", "lithium", "meteorite", "neodymium", "niobium",
            "sr90", "steel", "xe135");
    private static final Map<String, RegistryObject<Item>> POWDER_ITEMS_BY_ID = new HashMap<>();



    static {
        // 1. СЛИТКИ (ВСЕГДА) ✅ OK
        for (ModIngots ingot : ModIngots.values()) {
            RegistryObject<Item> registeredItem;
            if (ingot == ModIngots.URANIUM) {
                registeredItem = ITEMS.register(ingot.getName() + "_ingot", () -> new RadioactiveItem(new Item.Properties()));
            } else {
                registeredItem = ITEMS.register(ingot.getName() + "_ingot", () -> new Item(new Item.Properties()));
            }
            INGOTS.put(ingot, registeredItem);
        }

    }

    // УДОБНЫЙ МЕТОД ДЛЯ ПОЛУЧЕНИЯ СЛИТКА
    public static RegistryObject<Item> getIngot(ModIngots ingot) {
        return INGOTS.get(ingot);
    }

    public static RegistryObject<Item> getPowders(ModPowders powders) {return POWDERS.get(powders);}
    public static RegistryObject<Item> getPowder(ModIngots ingot) { return INGOT_POWDERS.get(ingot); }
    public static Optional<RegistryObject<Item>> getTinyPowder(ModIngots ingot) {
        return Optional.ofNullable(INGOT_POWDERS_TINY.get(ingot));
    }



    public static final int SLOT_HELMET = 0;
    public static final int SLOT_CHEST = 1;
    public static final int SLOT_LEGS = 2;
    public static final int SLOT_BOOTS = 3;
    public static final int SLOT_BATTERY = 4;
    public static final int SLOT_SPECIAL = 5;
    public static final int SLOT_INSERT = 6;
    public static final int SLOT_CLADDING = 7;
    public static final int SLOT_SERVOS = 8;

    public static final int BATTERY_CAPACITY = 1_000_000;

// ХАВЧИК:
    public static final RegistryObject<Item> STRAWBERRY = ITEMS.register("strawberry",
            () -> new Item(new Item.Properties().food(ModFoods.STRAWBERRY)));


    // ИНСТРУМЕНТЫ ГОРНЯКА:
    public static final RegistryObject<Item> STARMETAL_SWORD = ITEMS.register("starmetal_sword",
            () -> new SwordItem(ModToolTiers.STARMETAL, 7, -2, new Item.Properties()));
    public static final RegistryObject<Item> STARMETAL_AXE = ITEMS.register("starmetal_axe",
            () -> new ModAxeItem(ModToolTiers.STARMETAL, 15, 1, new Item.Properties()));
    public static final RegistryObject<Item> STARMETAL_PICKAXE = ITEMS.register("starmetal_pickaxe",
            () -> new ModPickaxeItem(ModToolTiers.STARMETAL, 3, 1, new Item.Properties(), 6, 3, 1, 5));
    public static final RegistryObject<Item> STARMETAL_SHOVEL = ITEMS.register("starmetal_shovel",
            () -> new ShovelItem(ModToolTiers.STARMETAL, 0, 0, new Item.Properties()));
    public static final RegistryObject<Item> STARMETAL_HOE = ITEMS.register("starmetal_hoe",
            () -> new HoeItem(ModToolTiers.STARMETAL, 0, 0f, new Item.Properties()));

    public static final RegistryObject<Item> GRENADE = ITEMS.register("grenade",
        () -> new GrenadeItem(new Item.Properties(), GrenadeType.STANDARD, ModEntities.GRENADE_PROJECTILE));

    public static final RegistryObject<Item> GRENADEHE = ITEMS.register("grenadehe",
        () -> new GrenadeItem(new Item.Properties(), GrenadeType.HE, ModEntities.GRENADEHE_PROJECTILE));

    public static final RegistryObject<Item> GRENADEFIRE = ITEMS.register("grenadefire",
        () -> new GrenadeItem(new Item.Properties(), GrenadeType.FIRE, ModEntities.GRENADEFIRE_PROJECTILE));

    public static final RegistryObject<Item> GRENADESLIME = ITEMS.register("grenadeslime",
        () -> new GrenadeItem(new Item.Properties(), GrenadeType.SLIME, ModEntities.GRENADESLIME_PROJECTILE));

    public static final RegistryObject<Item> GRENADESMART = ITEMS.register("grenadesmart",
        () -> new GrenadeItem(new Item.Properties(), GrenadeType.SMART, ModEntities.GRENADESMART_PROJECTILE));

    public static final RegistryObject<Item> GRENADE_IF = ITEMS.register("grenade_if",
            () -> new GrenadeIfItem(new Item.Properties(), GrenadeIfType.GRENADE_IF, ModEntities.GRENADE_IF_PROJECTILE));

    public static final RegistryObject<Item> GRENADE_IF_HE = ITEMS.register("grenade_if_he",
            () -> new GrenadeIfItem(new Item.Properties(), GrenadeIfType.GRENADE_IF_HE, ModEntities.GRENADE_IF_HE_PROJECTILE));

    public static final RegistryObject<Item> GRENADE_IF_SLIME = ITEMS.register("grenade_if_slime",
            () -> new GrenadeIfItem(new Item.Properties(), GrenadeIfType.GRENADE_IF_SLIME, ModEntities.GRENADE_IF_SLIME_PROJECTILE));

    public static final RegistryObject<Item> GRENADE_IF_FIRE = ITEMS.register("grenade_if_fire",
            () -> new GrenadeIfItem(new Item.Properties(), GrenadeIfType.GRENADE_IF_FIRE, ModEntities.GRENADE_IF_FIRE_PROJECTILE));

    public static final RegistryObject<Item> GRENADE_NUC = ITEMS.register("grenade_nuc",
            () -> new GrenadeNucItem(new Item.Properties(), ModEntities.GRENADE_NUC_PROJECTILE));

    public static final RegistryObject<Item> AIRBOMB_A = ITEMS.register("airbomb_a",
            () -> new AirBombItem(new Item.Properties(), ModEntities.AIRBOMB_PROJECTILE));
    public static final RegistryObject<Item> AIRNUKEBOMB_A = ITEMS.register("airnukebomb_a",
            () -> new AirNukeBombItem(new Item.Properties(), ModEntities.AIRNUKEBOMB_PROJECTILE));

    // БРОНЯ ГОРНЯКА:


    public static final RegistryObject<Item> SECURITY_HELMET = ITEMS.register("security_helmet",
            () -> new ArmorItem(ModArmorMaterials.SECURITY, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> SECURITY_CHESTPLATE = ITEMS.register("security_chestplate",
            () -> new ArmorItem(ModArmorMaterials.SECURITY, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> SECURITY_LEGGINGS = ITEMS.register("security_leggings",
            () -> new ArmorItem(ModArmorMaterials.SECURITY, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> SECURITY_BOOTS = ITEMS.register("security_boots",
            () -> new ArmorItem(ModArmorMaterials.SECURITY, ArmorItem.Type.BOOTS, new Item.Properties()));



    // Инструменты
    public static final RegistryObject<Item> GEIGER_COUNTER = ITEMS.register("geiger_counter",
            () -> new ItemGeigerCounter(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> DOSIMETER = ITEMS.register("dosimeter",
            () -> new ItemDosimeter(new Item.Properties().stacksTo(1)));




    public static final RegistryObject<Item> CRATE_IRON = ITEMS.register("crate_iron",
            () -> new IronCrateItem(ModBlocks.CRATE_IRON.get(), new Item.Properties()));
    public static final RegistryObject<Item> CRATE_STEEL = ITEMS.register("crate_steel",
            () -> new SteelCrateItem(ModBlocks.CRATE_STEEL.get(), new Item.Properties()));


    // ---------- МОТОР ЭЛЕКТРО (с GeoItem) ----------
    public static final RegistryObject<Item> MOTOR_ELECTRO_ITEM = ITEMS.register("motor_electro",
            () -> new MotorElectroBlockItem(ModBlocks.MOTOR_ELECTRO.get(),
                    new Item.Properties()));

    public static final RegistryObject<Item> WIND_GEN_FLUGER = ITEMS.register("wind_gen_fluger",
            () -> new WindGenFlugerBlockItem(ModBlocks.WIND_GEN_FLUGER.get(),
                    new Item.Properties()));


    // ---------- ВАЛ ЖЕЛЕЗНЫЙ (с GeoItem) ----------
    public static final RegistryObject<Item> SHAFT_IRON_ITEM = ITEMS.register("shaft_iron",
            () -> new ShaftIronBlockItem(ModBlocks.SHAFT_IRON.get(),
                    new Item.Properties()));

// Сначала убедись, что сам БЛОК уже зарегистрирован в ModBlocks
// ModBlocks.TURRET_LIGHT_PLACER

    public static final RegistryObject<Item> TURRET_LIGHT_PLACER_ITEM = ITEMS.register("turret_light_placer",
            () -> new TurretLightPlacerBlockItem(
                    ModBlocks.TURRET_LIGHT_PLACER.get(), // Ссылка на блок
                    new Item.Properties() // Свойства предмета
            ));

    public static final RegistryObject<Item> MINE_AP = ITEMS.register("mine_ap",
            () -> new MineApBlockItem(
                    ModBlocks.MINE_AP.get(), // Ссылка на блок
                    new Item.Properties() // Свойства предмета
            ));

    // Модификаторы брони
    public static final RegistryObject<Item> HEART_PIECE = ITEMS.register("heart_piece",
            () -> new ItemModHealth(
                    new Item.Properties(),
                    SLOT_SPECIAL,
                    5.0
            )
    );
    public static final RegistryObject<Item> HEART_CONTAINER = ITEMS.register("heart_container",
            () -> new ItemModHealth(
                    new Item.Properties(),
                    SLOT_SPECIAL,
                    20.0
            )
    );
    public static final RegistryObject<Item> HEART_BOOSTER = ITEMS.register("heart_booster",
            () -> new ItemModHealth(
                    new Item.Properties(),
                    SLOT_SPECIAL,
                    40.0
            )
    );
    public static final RegistryObject<Item> HEART_FAB = ITEMS.register("heart_fab",
            () -> new ItemModHealth(
                    new Item.Properties(),
                    SLOT_SPECIAL,
                    60.0
            )
    );
    public static final RegistryObject<Item> BLACK_DIAMOND = ITEMS.register("black_diamond",
            () -> new ItemModHealth(
                    new Item.Properties(),
                    SLOT_SPECIAL,
                    40.0
            )
    );

    public static final RegistryObject<Item> GHIORSIUM_CLADDING = ITEMS.register("cladding_ghiorsium",
            () -> new ItemModRadProtection(
                    new Item.Properties(),
                    SLOT_CLADDING,
                    0.5f
            )
    );
    public static final RegistryObject<Item> DESH_CLADDING = ITEMS.register("cladding_desh",
            () -> new ItemModRadProtection(
                    new Item.Properties(),
                    SLOT_CLADDING,
                    0.2f
            )
    );
    public static final RegistryObject<Item> LEAD_CLADDING = ITEMS.register("cladding_lead",
            () -> new ItemModRadProtection(
                    new Item.Properties(),
                    SLOT_CLADDING,
                    0.1f
            )
    );
    public static final RegistryObject<Item> RUBBER_CLADDING = ITEMS.register("cladding_rubber",
            () -> new ItemModRadProtection(
                    new Item.Properties(),
                    SLOT_CLADDING,
                    0.005f
            )
    );
    public static final RegistryObject<Item> PAINT_CLADDING = ITEMS.register("cladding_paint",
            () -> new ItemModRadProtection(
                    new Item.Properties(),
                    SLOT_CLADDING,
                    0.025f
            )
    );
    public static final RegistryObject<Item> CREATIVE_BATTERY = ITEMS.register("battery_creative",
            () -> new ItemCreativeBattery(
                    new Item.Properties()
            )
    );
    public static final RegistryObject<Item> ASSEMBLY_TEMPLATE = ITEMS.register("assembly_template",
            () -> new ItemAssemblyTemplate(
                    new Item.Properties().stacksTo(1)
            )
    );
    public static final RegistryObject<Item> TEMPLATE_FOLDER = ITEMS.register("template_folder",
            () -> new ItemTemplateFolder(
                    new Item.Properties().stacksTo(1)
            )
    );
    public static final RegistryObject<Item> BLUEPRINT_FOLDER = ITEMS.register("blueprint_folder",
        () -> new ItemBlueprintFolder(
                new Item.Properties().stacksTo(1)
        )
    );


    public static final RegistryObject<Item> RADAWAY = ITEMS.register("radaway",
            () -> new ItemSimpleConsumable(new Item.Properties(), (player, stack) -> {
                // Это лямбда-выражение определяет, что произойдет при использовании предмета.
                
                // Действуем только на сервере
                if (!player.level().isClientSide()) {
                    // 1. Накладываем эффект Антирадина.
                    //    Длительность: 200 тиков (10 секунд)
                    //    Уровень: I (amplifier = 0)
                    player.addEffect(new MobEffectInstance(ModEffects.RADAWAY.get(), 120, 0));

                    // 2. Проигрываем звук
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.RADAWAY_USE.get(), player.getSoundSource(), 1.0F, 1.0F);

                    // 3. Уменьшаем количество предметов в стаке
                    if (!player.getAbilities().instabuild) { // не уменьшать в креативе
                        stack.shrink(1);
                    }
                }
            })
    );
    public static final RegistryObject<Item> OIL_DETECTOR = ITEMS.register("oil_detector",
            () -> new OilDetectorItem(new Item.Properties()));

    public static final RegistryObject<Item> DEPTH_ORES_SCANNER = ITEMS.register("depth_ores_scanner",
            () -> new DepthOresScannerItem(new Item.Properties()));

    public static final RegistryObject<Item> RANGE_DETONATOR = ITEMS.register("range_detonator",
            () -> new RangeDetonatorItem(new Item.Properties()));

    public static final RegistryObject<Item> MULTI_DETONATOR = ITEMS.register("multi_detonator",
            () -> new MultiDetonatorItem(new Item.Properties()));

    public static final RegistryObject<Item> DETONATOR = ITEMS.register("detonator",
            () -> new DetonatorItem(new Item.Properties()));


    public static final RegistryObject<Item> CROWBAR = ITEMS.register("crowbar",
            () -> new Item(new Item.Properties()) {
                @Override
                public void appendHoverText(ItemStack stack, @Nullable Level level,
                                            @Nullable List<Component> tooltip, TooltipFlag flag) {
                    if (tooltip == null) return;

                    tooltip.add(Component.translatable("tooltip.smogline.crowbar.line1")
                            .withStyle(ChatFormatting.GRAY));
                    tooltip.add(Component.translatable("tooltip.smogline.crowbar.line2")
                            .withStyle(ChatFormatting.GRAY));
                }
            });


    public static final RegistryObject<Item> MALACHITE_CHUNK = ITEMS.register("malachite_chunk",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> LIMESTONE = ITEMS.register("limestone",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CAN_KEY = ITEMS.register("can_key",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DEFUSER = ITEMS.register("defuser",
            () -> new Item(new Item.Properties()) {
                @Override
                public void appendHoverText(ItemStack stack, @Nullable Level level,
                                            @Nullable List<Component> tooltip, TooltipFlag flag) {
                    if (tooltip == null) return;

                    tooltip.add(Component.translatable("tooltip.smogline.defuser.line1")
                            .withStyle(ChatFormatting.GRAY));
                }
            });


    public static final RegistryObject<Item> TURRET_REMOVER = ITEMS.register("turret_remover",
            () -> new TurretRemoverItem(new Item.Properties()));

    public static final RegistryObject<Item> MACHINEGUN = ITEMS.register("machinegun",
            () -> new MachineGunItem(new Item.Properties()));

    public static final RegistryObject<Item> TURRET_CHIP = ITEMS.register("turret_chip",
            () -> new TurretChipItem(new Item.Properties()));

    // Обычный
    public static final RegistryObject<Item> AMMO_TURRET = ITEMS.register("ammo_turret",
            () -> new AmmoTurretItem(new Item.Properties(), 4.0f, 3.0f, false));

    // Пробивной
    public static final RegistryObject<Item> AMMO_TURRET_PIERCING = ITEMS.register("ammo_turret_piercing",
            () -> new AmmoTurretItem(new Item.Properties(), 5.0f, 3.0f, true));

    // Пробивной
    public static final RegistryObject<Item> AMMO_TURRET_HOLLOW = ITEMS.register("ammo_turret_hollow",
            () -> new AmmoTurretItem(new Item.Properties(), 4.0f, 3.0f, false));

    // ОГНЕННЫЙ (убедись, что параметры правильные!)
    public static final RegistryObject<Item> AMMO_TURRET_FIRE = ITEMS.register("ammo_turret_fire",
            () -> new AmmoTurretItem(new Item.Properties(), 3.0f, 3.0f, false)); // Урон 3.0, не пробивает

    public static final RegistryObject<Item> AMMO_TURRET_RADIO = ITEMS.register("ammo_turret_radio",
            () -> new AmmoTurretItem(new Item.Properties(), 4.0f, 3.0f, false));



    // RAW METALS

    public static final RegistryObject<Item> URANIUM_RAW = ITEMS.register("uranium_raw",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> LEAD_RAW = ITEMS.register("lead_raw",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> BERYLLIUM_RAW = ITEMS.register("beryllium_raw",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ALUMINUM_RAW = ITEMS.register("aluminum_raw",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> TITANIUM_RAW = ITEMS.register("titanium_raw",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> THORIUM_RAW = ITEMS.register("thorium_raw",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> COBALT_RAW = ITEMS.register("cobalt_raw",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> TUNGSTEN_RAW = ITEMS.register("tungsten_raw",
            () -> new Item(new Item.Properties()));




    // Материалы
    public static final RegistryObject<Item> SULFUR = ITEMS.register("sulfur",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> SEQUESTRUM = ITEMS.register("sequestrum",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> FLUORITE = ITEMS.register("fluorite",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> RAREGROUND_ORE_CHUNK = ITEMS.register("rareground_ore_chunk",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> FIRECLAY_BALL = ITEMS.register("fireclay_ball",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> WOOD_ASH_POWDER = ITEMS.register("wood_ash_powder",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> FIREBRICK = ITEMS.register("firebrick",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> LIGNITE = ITEMS.register("lignite",
            () -> new FuelItem(new Item.Properties(), 1000));

    public static final RegistryObject<Item> CINNABAR = ITEMS.register("cinnabar",
            () -> new Item(new Item.Properties()));




    // Здесь мы регистрируем мультиблочные структуры для того, чтобы MultiblockBlockItem при установке мог обрабатывать их на наличие препятствующих блоков.

    public static final RegistryObject<Item> MACHINE_ASSEMBLER = ITEMS.register("machine_assembler",
        () -> new MultiblockBlockItem(ModBlocks.MACHINE_ASSEMBLER.get(), new Item.Properties()));
            
    public static final RegistryObject<Item> ADVANCED_ASSEMBLY_MACHINE = ITEMS.register("advanced_assembly_machine",
        () -> new MultiblockBlockItem(ModBlocks.ADVANCED_ASSEMBLY_MACHINE.get(), new Item.Properties()));

    public static final RegistryObject<Item> PRESS = ITEMS.register("press",
        () -> new MultiblockBlockItem(ModBlocks.PRESS.get(), new Item.Properties()));

    public static final RegistryObject<Item> WOOD_BURNER = ITEMS.register("wood_burner",
        () -> new MultiblockBlockItem(ModBlocks.WOOD_BURNER.get(), new Item.Properties()));



    public static final RegistryObject<Item> STAMP_STONE_FLAT = ITEMS.register("stamp_stone_flat",
            () -> new ItemStamp(new Item.Properties(), 32));
    public static final RegistryObject<Item> STAMP_STONE_PLATE = ITEMS.register("stamp_stone_plate",
            () -> new ItemStamp(new Item.Properties(), 32));
    public static final RegistryObject<Item> STAMP_STONE_WIRE = ITEMS.register("stamp_stone_wire",
            () -> new ItemStamp(new Item.Properties(), 32));
    public static final RegistryObject<Item> STAMP_STONE_CIRCUIT = ITEMS.register("stamp_stone_circuit",
            () -> new ItemStamp(new Item.Properties(), 32));


    public static final RegistryObject<Item> BLADE_TEST = ITEMS.register("blade_test",
            () -> new ItemBlades(new Item.Properties()));

    public static final RegistryObject<Item> BLADE_STEEL = ITEMS.register("blade_steel",
            () -> new ItemBlades(new Item.Properties(), 200));

    public static final RegistryObject<Item> BLADE_TITANIUM = ITEMS.register("blade_titanium",
            () -> new ItemBlades(new Item.Properties(), 350));

    public static final RegistryObject<Item> BLADE_ALLOY = ITEMS.register("blade_alloy",
            () -> new ItemBlades(new Item.Properties(), 700));

    // Железные штампы (48 использований)
    public static final RegistryObject<Item> STAMP_IRON_FLAT = ITEMS.register("stamp_iron_flat",
            () -> new ItemStamp(new Item.Properties(), 48));
    public static final RegistryObject<Item> STAMP_IRON_PLATE = ITEMS.register("stamp_iron_plate",
            () -> new ItemStamp(new Item.Properties(), 48));
    public static final RegistryObject<Item> STAMP_IRON_WIRE = ITEMS.register("stamp_iron_wire",
            () -> new ItemStamp(new Item.Properties(), 48));
    public static final RegistryObject<Item> STAMP_IRON_CIRCUIT = ITEMS.register("stamp_iron_circuit",
            () -> new ItemStamp(new Item.Properties(), 48));
    public static final RegistryObject<Item> STAMP_IRON_9 = ITEMS.register("stamp_iron_9",
            () -> new ItemStamp(new Item.Properties(), 48));
    public static final RegistryObject<Item> STAMP_IRON_44 = ITEMS.register("stamp_iron_44",
            () -> new ItemStamp(new Item.Properties(), 48));
    public static final RegistryObject<Item> STAMP_IRON_50 = ITEMS.register("stamp_iron_50",
            () -> new ItemStamp(new Item.Properties(), 48));
    public static final RegistryObject<Item> STAMP_IRON_357 = ITEMS.register("stamp_iron_357",
            () -> new ItemStamp(new Item.Properties(), 48));

    // Стальные штампы (64 использования)
    public static final RegistryObject<Item> STAMP_STEEL_FLAT = ITEMS.register("stamp_steel_flat",
            () -> new ItemStamp(new Item.Properties(), 64));
    public static final RegistryObject<Item> STAMP_STEEL_PLATE = ITEMS.register("stamp_steel_plate",
            () -> new ItemStamp(new Item.Properties(), 64));
    public static final RegistryObject<Item> STAMP_STEEL_WIRE = ITEMS.register("stamp_steel_wire",
            () -> new ItemStamp(new Item.Properties(), 64));
    public static final RegistryObject<Item> STAMP_STEEL_CIRCUIT = ITEMS.register("stamp_steel_circuit",
            () -> new ItemStamp(new Item.Properties(), 64));

    // Титановые штампы (80 использований)
    public static final RegistryObject<Item> STAMP_TITANIUM_FLAT = ITEMS.register("stamp_titanium_flat",
            () -> new ItemStamp(new Item.Properties(), 80));
    public static final RegistryObject<Item> STAMP_TITANIUM_PLATE = ITEMS.register("stamp_titanium_plate",
            () -> new ItemStamp(new Item.Properties(), 80));
    public static final RegistryObject<Item> STAMP_TITANIUM_WIRE = ITEMS.register("stamp_titanium_wire",
            () -> new ItemStamp(new Item.Properties(), 80));
    public static final RegistryObject<Item> STAMP_TITANIUM_CIRCUIT = ITEMS.register("stamp_titanium_circuit",
            () -> new ItemStamp(new Item.Properties(), 80));

    // Обсидиановые штампы (96 использований)
    public static final RegistryObject<Item> STAMP_OBSIDIAN_FLAT = ITEMS.register("stamp_obsidian_flat",
            () -> new ItemStamp(new Item.Properties(), 96));
    public static final RegistryObject<Item> STAMP_OBSIDIAN_PLATE = ITEMS.register("stamp_obsidian_plate",
            () -> new ItemStamp(new Item.Properties(), 96));
    public static final RegistryObject<Item> STAMP_OBSIDIAN_WIRE = ITEMS.register("stamp_obsidian_wire",
            () -> new ItemStamp(new Item.Properties(), 96));
    public static final RegistryObject<Item> STAMP_OBSIDIAN_CIRCUIT = ITEMS.register("stamp_obsidian_circuit",
            () -> new ItemStamp(new Item.Properties(), 96));

    // Desh штампы (бесконечная прочность)
    public static final RegistryObject<Item> STAMP_DESH_FLAT = ITEMS.register("stamp_desh_flat",
            () -> new ItemStamp(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_DESH_PLATE = ITEMS.register("stamp_desh_plate",
            () -> new ItemStamp(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_DESH_WIRE = ITEMS.register("stamp_desh_wire",
            () -> new ItemStamp(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_DESH_CIRCUIT = ITEMS.register("stamp_desh_circuit",
            () -> new ItemStamp(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_DESH_9 = ITEMS.register("stamp_desh_9",
            () -> new ItemStamp(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_DESH_44 = ITEMS.register("stamp_desh_44",
            () -> new ItemStamp(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_DESH_50 = ITEMS.register("stamp_desh_50",
            () -> new ItemStamp(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_DESH_357 = ITEMS.register("stamp_desh_357",
            () -> new ItemStamp(new Item.Properties()));


    //батарейки

    public static final RegistryObject<Item> BATTERY_SCHRABIDIUM = ITEMS.register("battery_schrabidium",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    1000000,
                    5000,
                    5000
            ));

    // ========== КАРТОФЕЛЬНАЯ И БАЗОВЫЕ ==========
    public static final RegistryObject<Item> BATTERY_POTATO = ITEMS.register("battery_potato",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    1_000,
                    100,
                    100
            ));

    public static final RegistryObject<Item> BATTERY = ITEMS.register("battery",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    5000,
                    100,
                    100
            ));

    // ========== КРАСНЫЕ БАТАРЕЙКИ (RED CELL) ==========
    public static final RegistryObject<Item> BATTERY_RED_CELL = ITEMS.register("battery_red_cell",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    15000,
                    100,
                    100
            ));

    public static final RegistryObject<Item> BATTERY_RED_CELL_6 = ITEMS.register("battery_red_cell_6",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    90000,
                    100,
                    100
            ));

    public static final RegistryObject<Item> BATTERY_RED_CELL_24 = ITEMS.register("battery_red_cell_24",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    240000,
                    100,
                    100
            ));

    // ========== ПРОДВИНУТЫЕ БАТАРЕЙКИ (ADVANCED) ==========
    public static final RegistryObject<Item> BATTERY_ADVANCED = ITEMS.register("battery_advanced",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    20000,
                    500,
                    500
            ));

    public static final RegistryObject<Item> BATTERY_ADVANCED_CELL = ITEMS.register("battery_advanced_cell",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    60000,
                    500,
                    500
            ));

    public static final RegistryObject<Item> BATTERY_ADVANCED_CELL_4 = ITEMS.register("battery_advanced_cell_4",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    240000,
                    500,
                    500
            ));

    public static final RegistryObject<Item> BATTERY_ADVANCED_CELL_12 = ITEMS.register("battery_advanced_cell_12",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    720000,
                    500,
                    500
            ));

    // ========== ЛИТИЕВЫЕ БАТАРЕЙКИ (LITHIUM) ==========
    public static final RegistryObject<Item> BATTERY_LITHIUM = ITEMS.register("battery_lithium",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    250000,
                    1000,
                    1000
            ));

    public static final RegistryObject<Item> BATTERY_LITHIUM_CELL = ITEMS.register("battery_lithium_cell",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    750000,
                    1000,
                    1000
            ));

    public static final RegistryObject<Item> BATTERY_LITHIUM_CELL_3 = ITEMS.register("battery_lithium_cell_3",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    2250000,
                    1000,
                    1000
            ));

    public static final RegistryObject<Item> BATTERY_LITHIUM_CELL_6 = ITEMS.register("battery_lithium_cell_6",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    4500000,
                    1000,
                    1000
            ));

// ========== ШРАБИДИЕВЫЕ БАТАРЕЙКИ (SCHRABIDIUM) - уже есть ==========

    public static final RegistryObject<Item> BATTERY_SCHRABIDIUM_CELL = ITEMS.register("battery_schrabidium_cell",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    3000000,
                    5000,
                    5000
            ));

    public static final RegistryObject<Item> BATTERY_SCHRABIDIUM_CELL_2 = ITEMS.register("battery_schrabidium_cell_2",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    6000000,
                    5000,
                    5000
            ));

    public static final RegistryObject<Item> BATTERY_SCHRABIDIUM_CELL_4 = ITEMS.register("battery_schrabidium_cell_4",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    12000000,
                    5000,
                    5000
            ));

    // ========== ИСКРОВЫЕ БАТАРЕЙКИ (SPARK) - ЭКСТРЕМАЛЬНЫЕ ==========
    public static final RegistryObject<Item> BATTERY_SPARK = ITEMS.register("battery_spark",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    100000000,
                    2000000,
                    2000000
            ));

    public static final RegistryObject<Item> BATTERY_TRIXITE = ITEMS.register("battery_trixite",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    5000000,
                    40000,
                    200000
            ));

    public static final RegistryObject<Item> BATTERY_SPARK_CELL_6 = ITEMS.register("battery_spark_cell_6",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    600_000_000L,
                    2000000,
                    2000000
            ));

    public static final RegistryObject<Item> BATTERY_SPARK_CELL_25 = ITEMS.register("battery_spark_cell_25",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    2_500_000_000L,
                    2000000,
                    2000000
            ));

    public static final RegistryObject<Item> BATTERY_SPARK_CELL_100 = ITEMS.register("battery_spark_cell_100",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    10_000_000_000L,
                    20000000,
                    2000000
            ));

    public static final RegistryObject<Item> BATTERY_SPARK_CELL_1000 = ITEMS.register("battery_spark_cell_1000",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    100_000_000_000L,
                    20000000,
                    20000000
            ));

    public static final RegistryObject<Item> BATTERY_SPARK_CELL_2500 = ITEMS.register("battery_spark_cell_2500",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    250_000_000_000L,
                    20000000,
                    20000000
            ));

    public static final RegistryObject<Item> BATTERY_SPARK_CELL_10000 = ITEMS.register("battery_spark_cell_10000",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    1_000_000_000_000L,
                    200000000,
                    200000000
            ));

    public static final RegistryObject<Item> BATTERY_SPARK_CELL_POWER = ITEMS.register("battery_spark_cell_power",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    100_000_000_000_000L,
                    200000000,
                    200000000
            ));


    public static final RegistryObject<Item> AIRSTRIKE_TEST = ITEMS.register("airstrike_test",
            () -> new AirstrikeItem(new Item.Properties()));
    public static final RegistryObject<Item> AIRSTRIKE_AGENT= ITEMS.register("airstrike_agent",
            () -> new AirstrikeAgentItem(new Item.Properties()));
    public static final RegistryObject<Item> AIRSTRIKE_HEAVY = ITEMS.register("airstrike_heavy",
            () -> new AirstrikeHeavyItem(new Item.Properties()));
    public static final RegistryObject<Item> AIRSTRIKE_NUKE = ITEMS.register("airstrike_nuke",
            () -> new AirstrikeNukeItem(new Item.Properties()));
    public static final RegistryObject<Item> WIRE_RED_COPPER = ITEMS.register("wire_red_copper",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WIRE_ADVANCED_ALLOY = ITEMS.register("wire_advanced_alloy",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WIRE_ALUMINIUM = ITEMS.register("wire_aluminium",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WIRE_COPPER = ITEMS.register("wire_copper",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WIRE_CARBON = ITEMS.register("wire_carbon",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WIRE_FINE = ITEMS.register("wire_fine",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WIRE_GOLD = ITEMS.register("wire_gold",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WIRE_MAGNETIZED_TUNGSTEN = ITEMS.register("wire_magnetized_tungsten",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WIRE_SCHRABIDIUM = ITEMS.register("wire_schrabidium",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WIRE_TUNGSTEN = ITEMS.register("wire_tungsten",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> SCREWDRIVER = ITEMS.register("screwdriver",
            () -> new ScrewdriverItem(new Item.Properties()
                    .stacksTo(1)
                    .durability(256))); // Прочность как у железных инструментов


        // Медленный источник (500 mB/t)
        public static final RegistryObject<Item> INFINITE_WATER_500 = ITEMS.register("inf_water",
                () -> new InfiniteWaterItem(new Item.Properties().stacksTo(1), 500));

        // Быстрый источник (5000 mB/t)
        public static final RegistryObject<Item> INFINITE_WATER_5000 = ITEMS.register("inf_water_mk2",
                () -> new InfiniteWaterItem(new Item.Properties().stacksTo(1), 5000));



    //=============================== ВЁДРА ДЛЯ ЖИДКОСТЕЙ ===============================//

    public static final RegistryObject<Item> CRUDE_OIL_BUCKET = ITEMS.register("bucket_crude_oil",
            () -> new BucketItem(ModFluids.CRUDE_OIL_SOURCE,
                    new Item.Properties()
                            .craftRemainder(Items.BUCKET) // Возвращает пустое ведро при крафте
                            .stacksTo(1))); // Ведра не стакаются


    public static final RegistryObject<Item> FLUID_IDENTIFIER = ITEMS.register("fluid_identifier",
            () -> new ItemFluidIdentifier(new Item.Properties()));






    // Метод для регистрации всех предметов, вызывается в основном классе мода.
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
