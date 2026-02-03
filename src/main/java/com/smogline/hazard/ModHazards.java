package com.smogline.hazard;

// Система опасностей для предметов и блоков.
// Позволяет регистрировать опасности для Item, Block и тегов.

import com.smogline.block.ModBlocks;
import com.smogline.item.tags_and_tiers.ModIngots;
import com.smogline.item.ModItems;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class ModHazards {

    public static final TagKey<Item> URANIUM_INGOTS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("forge", "ingots/uranium"));

    public static final TagKey<Item> ALKALI_METALS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("forge", "ingots/sodium"));

     public static void registerHazards() {
        // АВТОМАТИЧЕСКАЯ РЕГИСТРАЦИЯ ОПАСНОСТЕЙ ДЛЯ СЛИТКОВ 
        for (ModIngots ingot : ModIngots.values()) {
            // Используем switch для явного назначения опасностей
            // Это гораздо чище и гибче, чем хранить данные в enum
            switch (ingot) {



                // Для STEEL и TECHNELLOY мы ничего не делаем, поэтому они проваливаются в default
                default:
                    // Нет опасностей, ничего не делаем
                    break;
            }
        }
        // БЛОКИ 

         HazardSystem.register(ModBlocks.NUCLEAR_FALLOUT.get(), new HazardData(
                 new HazardEntry(HazardType.RADIATION, 0.5f)
         ));


        // РЕГИСТРАЦИЯ ЗАЩИТЫ ДЛЯ ВАНИЛЬНОЙ БРОНИ 
        HazardSystem.registerArmorProtection(Items.IRON_HELMET, 0.004f);
        HazardSystem.registerArmorProtection(Items.IRON_CHESTPLATE, 0.009f);
        HazardSystem.registerArmorProtection(Items.IRON_LEGGINGS, 0.006f);
        HazardSystem.registerArmorProtection(Items.IRON_BOOTS, 0.002f);

        HazardSystem.registerArmorProtection(Items.GOLDEN_HELMET, 0.004f);
        HazardSystem.registerArmorProtection(Items.GOLDEN_CHESTPLATE, 0.009f);
        HazardSystem.registerArmorProtection(Items.GOLDEN_LEGGINGS, 0.006f);
        HazardSystem.registerArmorProtection(Items.GOLDEN_BOOTS, 0.002f);

        HazardSystem.registerArmorProtection(Items.DIAMOND_HELMET, 0.05f);
        HazardSystem.registerArmorProtection(Items.DIAMOND_CHESTPLATE, 0.25f);
        HazardSystem.registerArmorProtection(Items.DIAMOND_LEGGINGS, 0.1f);
        HazardSystem.registerArmorProtection(Items.DIAMOND_BOOTS, 0.025f);

        HazardSystem.registerArmorProtection(Items.NETHERITE_HELMET, 0.1f);
        HazardSystem.registerArmorProtection(Items.NETHERITE_CHESTPLATE, 0.45f);
        HazardSystem.registerArmorProtection(Items.NETHERITE_LEGGINGS, 0.2f);
        HazardSystem.registerArmorProtection(Items.NETHERITE_BOOTS, 0.05f);

        // РЕГИСТРАЦИЯ ЗАЩИТЫ ДЛЯ МОДОВОЙ БРОНИ 



        HazardSystem.registerArmorProtection(ModItems.SECURITY_HELMET.get(), 0.165f);
        HazardSystem.registerArmorProtection(ModItems.SECURITY_CHESTPLATE.get(), 0.33f);
        HazardSystem.registerArmorProtection(ModItems.SECURITY_LEGGINGS.get(), 0.247f);
        HazardSystem.registerArmorProtection(ModItems.SECURITY_BOOTS.get(), 0.082f);


    }
}