package com.smogline.main;

import com.smogline.item.tags_and_tiers.ModIngots;
import com.smogline.item.ModItems;
import com.smogline.block.ModBlocks;
import com.smogline.lib.RefStrings;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = 
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RefStrings.MODID);

    public static final RegistryObject<CreativeModeTab> NTM_MACHINES_TAB = CREATIVE_MODE_TABS.register("ntm_machines_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_machines_tab"))
                    .icon(() -> new ItemStack(ModBlocks.MACHINE_ASSEMBLER.get()))
                    .build());

    public static final RegistryObject<CreativeModeTab> SMOGLINE_WEAPONS_TAB = CREATIVE_MODE_TABS.register("smogline_weapons_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".smogline_weapons_tab"))
                    .icon(() -> new ItemStack(ModItems.GRENADE_NUC.get()))
                    .build());

    public static final RegistryObject<CreativeModeTab> SMOGLINE_TECH_TAB = CREATIVE_MODE_TABS.register("smogline_tech_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".smogline_tech_tab"))
                    .icon(() -> new ItemStack(ModBlocks.STOPPER.get()))
                    .build());


    // Метод для регистрации всех вкладок
    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}