package com.smogline.block.entity;

import com.smogline.api.energy.ConverterBlockEntity;
import com.smogline.api.energy.SwitchBlockEntity;
import com.smogline.api.energy.WireBlockEntity;
import com.smogline.block.ModBlocks;
import com.smogline.block.entity.custom.*;
import com.smogline.block.entity.custom.crates.*;
import com.smogline.block.entity.custom.machines.*;
import com.smogline.block.entity.custom.explosives.MineBlockEntity;
import com.smogline.lib.RefStrings;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.smogline.block.ModBlocks.TURRET_BLOCK;


public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
		DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, RefStrings.MODID);

    public static final RegistryObject<BlockEntityType<GeigerCounterBlockEntity>> GEIGER_COUNTER_BE =
		BLOCK_ENTITIES.register("geiger_counter_be", () ->
			BlockEntityType.Builder.<GeigerCounterBlockEntity>of(GeigerCounterBlockEntity::new, ModBlocks.GEIGER_COUNTER_BLOCK.get())
				.build(null));

    public static final RegistryObject<BlockEntityType<TurretLightPlacerBlockEntity>> TURRET_LIGHT_PLACER_BE =
            BLOCK_ENTITIES.register("turret_light_placer",
                    () -> BlockEntityType.Builder.of(TurretLightPlacerBlockEntity::new, ModBlocks.TURRET_LIGHT_PLACER.get()).build(null));

    public static final RegistryObject<BlockEntityType<MotorElectroBlockEntity>> MOTOR_ELECTRO_BE =
            BLOCK_ENTITIES.register("motor_electro_be",
                    () -> BlockEntityType.Builder.of(MotorElectroBlockEntity::new, ModBlocks.MOTOR_ELECTRO.get()).build(null));

    public static final RegistryObject<BlockEntityType<DepthWormNestBlockEntity>> DEPTH_WORM_NEST =
            BLOCK_ENTITIES.register("depth_worm_nest",
                    () -> BlockEntityType.Builder.of(DepthWormNestBlockEntity::new, ModBlocks.DEPTH_WORM_NEST.get()).build(null));


    public static final RegistryObject<BlockEntityType<ShaftIronBlockEntity>> SHAFT_IRON_BE =
            BLOCK_ENTITIES.register("shaft_iron_be",
                    () -> BlockEntityType.Builder.of(ShaftIronBlockEntity::new, ModBlocks.SHAFT_IRON.get()).build(null));
    public static final RegistryObject<BlockEntityType<MachineAssemblerBlockEntity>> MACHINE_ASSEMBLER_BE =
		BLOCK_ENTITIES.register("machine_assembler_be", () ->
			BlockEntityType.Builder.<MachineAssemblerBlockEntity>of(MachineAssemblerBlockEntity::new, ModBlocks.MACHINE_ASSEMBLER.get())
				.build(null));
    public static final RegistryObject<BlockEntityType<TurretBlockEntity>> TURRET_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("turret_block_entity",
                    () -> BlockEntityType.Builder.of(TurretBlockEntity::new, TURRET_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<MachineAdvancedAssemblerBlockEntity>> ADVANCED_ASSEMBLY_MACHINE_BE =
		BLOCK_ENTITIES.register("advanced_assembly_machine_be", () ->
			BlockEntityType.Builder.<MachineAdvancedAssemblerBlockEntity>of(MachineAdvancedAssemblerBlockEntity::new, ModBlocks.ADVANCED_ASSEMBLY_MACHINE.get())
				.build(null));

    public static final RegistryObject<BlockEntityType<WindGenFlugerBlockEntity>> WIND_GEN_FLUGER_BE =
            BLOCK_ENTITIES.register("wind_gen_fluger",
                    () -> BlockEntityType.Builder.of(WindGenFlugerBlockEntity::new, ModBlocks.WIND_GEN_FLUGER.get()).build(null));

    public static final RegistryObject<BlockEntityType<TachometerBlockEntity>> TACHOMETER_BE =
            BLOCK_ENTITIES.register("tachometer",
                    () -> BlockEntityType.Builder.of(TachometerBlockEntity::new, ModBlocks.TACHOMETER.get()).build(null));

    public static final RegistryObject<BlockEntityType<RotationMeterBlockEntity>> ROTATION_METER_BE =
            BLOCK_ENTITIES.register("rotation_meter_be",
                    () -> BlockEntityType.Builder.of(RotationMeterBlockEntity::new, ModBlocks.ROTATION_METER.get()).build(null));

    public static final RegistryObject<BlockEntityType<AdderBlockEntity>> ADDER_BE =
            BLOCK_ENTITIES.register("adder",
                    () -> BlockEntityType.Builder.of(AdderBlockEntity::new, ModBlocks.ADDER.get()).build(null));

    public static final RegistryObject<BlockEntityType<StopperBlockEntity>> STOPPER_BE =
            BLOCK_ENTITIES.register("stopper",
                    () -> BlockEntityType.Builder.of(StopperBlockEntity::new, ModBlocks.STOPPER.get()).build(null));

    public static final RegistryObject<BlockEntityType<MachineBatteryBlockEntity>> MACHINE_BATTERY_BE =
            BLOCK_ENTITIES.register("machine_battery_be", () -> {
                // Превращаем список RegistryObject в массив Block[]
                Block[] validBlocks = ModBlocks.BATTERY_BLOCKS.stream()
                        .map(RegistryObject::get)
                        .toArray(Block[]::new);

                return BlockEntityType.Builder.<MachineBatteryBlockEntity>of(MachineBatteryBlockEntity::new, validBlocks)
                        .build(null);
            });

    public static final RegistryObject<BlockEntityType<GearPortBlockEntity>> GEAR_PORT_BE =
            BLOCK_ENTITIES.register("gear_port_be",
                    () -> BlockEntityType.Builder.of(GearPortBlockEntity::new, ModBlocks.GEAR_PORT.get()).build(null));

    public static final RegistryObject<BlockEntityType<AnvilBlockEntity>> ANVIL_BE =
        BLOCK_ENTITIES.register("anvil_be", () ->
            BlockEntityType.Builder.<AnvilBlockEntity>of(AnvilBlockEntity::new,
                    ModBlocks.ANVIL_IRON.get(),
                    ModBlocks.ANVIL_LEAD.get(),
                    ModBlocks.ANVIL_STEEL.get(),
                    ModBlocks.ANVIL_DESH.get(),
                    ModBlocks.ANVIL_FERROURANIUM.get(),
                    ModBlocks.ANVIL_SATURNITE.get(),
                    ModBlocks.ANVIL_BISMUTH_BRONZE.get(),
                    ModBlocks.ANVIL_ARSENIC_BRONZE.get(),
                    ModBlocks.ANVIL_SCHRABIDATE.get(),
                    ModBlocks.ANVIL_DNT.get(),
                    ModBlocks.ANVIL_OSMIRIDIUM.get(),
                    ModBlocks.ANVIL_MURKY.get())
                .build(null));

    public static final RegistryObject<BlockEntityType<MineBlockEntity>> MINE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("mine_block_entity", () ->
                    BlockEntityType.Builder.of(MineBlockEntity::new, ModBlocks.MINE_AP.get())
                            .build(null)
            );

    public static final RegistryObject<BlockEntityType<MineBlockEntity>> MINE_NUKE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("mine_nuke_block_entity", () ->
                    BlockEntityType.Builder.of(MineBlockEntity::new, ModBlocks.MINE_FAT.get())
                            .build(null)
            );
    public static final RegistryObject<BlockEntityType<MachineShredderBlockEntity>> SHREDDER =
            BLOCK_ENTITIES.register("shredder", () ->
                    BlockEntityType.Builder.of(MachineShredderBlockEntity::new,
                            ModBlocks.SHREDDER.get()).build(null));

    public static final RegistryObject<BlockEntityType<UniversalMachinePartBlockEntity>> UNIVERSAL_MACHINE_PART_BE =
        BLOCK_ENTITIES.register("universal_machine_part_be", () ->
			BlockEntityType.Builder.<UniversalMachinePartBlockEntity>of(UniversalMachinePartBlockEntity::new, ModBlocks.UNIVERSAL_MACHINE_PART.get())
				.build(null));

	public static final RegistryObject<BlockEntityType<WireBlockEntity>> WIRE_BE =
		BLOCK_ENTITIES.register("wire_be", () ->
			BlockEntityType.Builder.<WireBlockEntity>of(WireBlockEntity::new, ModBlocks.WIRE_COATED.get())
				.build(null));


    public static final RegistryObject<BlockEntityType<SwitchBlockEntity>> SWITCH_BE =
            BLOCK_ENTITIES.register("switch_be", () ->
                    BlockEntityType.Builder.of(SwitchBlockEntity::new, ModBlocks.SWITCH.get())
                            .build(null));

	public static final RegistryObject<BlockEntityType<BlastFurnaceBlockEntity>> BLAST_FURNACE_BE =
			BLOCK_ENTITIES.register("blast_furnace_be", () ->
					BlockEntityType.Builder.of(BlastFurnaceBlockEntity::new,
							ModBlocks.BLAST_FURNACE.get()).build(null));

	public static final RegistryObject<BlockEntityType<MachinePressBlockEntity>> PRESS_BE =
			BLOCK_ENTITIES.register("press_be", () ->
					BlockEntityType.Builder.of(MachinePressBlockEntity::new,
							ModBlocks.PRESS.get()).build(null));

	public static final RegistryObject<BlockEntityType<MachineWoodBurnerBlockEntity>> WOOD_BURNER_BE =
			BLOCK_ENTITIES.register("wood_burner_be", () ->
					BlockEntityType.Builder.of(MachineWoodBurnerBlockEntity::new,
							ModBlocks.WOOD_BURNER.get()).build(null));

    public static final RegistryObject<BlockEntityType<MachineFluidTankBlockEntity>> FLUID_TANK_BE =
            BLOCK_ENTITIES.register("fluid_tank_be", () ->
                    BlockEntityType.Builder.of(MachineFluidTankBlockEntity::new,
                            ModBlocks.FLUID_TANK.get()).build(null));


    public static final RegistryObject<BlockEntityType<IronCrateBlockEntity>> IRON_CRATE_BE =
            BLOCK_ENTITIES.register("iron_crate_be", () ->
                    BlockEntityType.Builder.<IronCrateBlockEntity>of(
                            IronCrateBlockEntity::new,
                            ModBlocks.CRATE_IRON.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<SteelCrateBlockEntity>> STEEL_CRATE_BE =
            BLOCK_ENTITIES.register("steel_crate_be", () ->
                    BlockEntityType.Builder.<SteelCrateBlockEntity>of(
                            SteelCrateBlockEntity::new,
                            ModBlocks.CRATE_STEEL.get()
                    ).build(null));
    public static final RegistryObject<BlockEntityType<DeshCrateBlockEntity>> DESH_CRATE_BE =
            BLOCK_ENTITIES.register("desh_crate_be", () ->
                    BlockEntityType.Builder.<DeshCrateBlockEntity>of(
                            DeshCrateBlockEntity::new,
                            ModBlocks.CRATE_DESH.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<ConverterBlockEntity>> CONVERTER_BE =
            BLOCK_ENTITIES.register("converter_be",
                    () -> BlockEntityType.Builder.of(ConverterBlockEntity::new, ModBlocks.CONVERTER_BLOCK.get()).build(null));
        

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
