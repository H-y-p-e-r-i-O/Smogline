package com.smogline.datagen.recipes.custom;

import com.smogline.datagen.assets.ModItemTagProvider;
import com.smogline.item.tags_and_tiers.ModIngots;
import com.smogline.item.ModItems;
import com.smogline.lib.RefStrings;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

/**
 * Generates all press recipes (plates, wires, circuits).
 */
public final class PressRecipeGenerator {

    private PressRecipeGenerator() {
    }

    public static void generate(Consumer<FinishedRecipe> writer) {
        generatePlates(writer);
        generateWires(writer);
        generateCircuits(writer);
    }

    private static void generatePlates(Consumer<FinishedRecipe> writer) {
    }

    private static void generateWires(Consumer<FinishedRecipe> writer) {
        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.WIRE_COPPER.get(), 8))
                .stamp(ModItemTagProvider.STAMPS_WIRE)
                .material(Items.COPPER_INGOT)
                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "wire_copper"));
        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.WIRE_GOLD.get(), 8))
                .stamp(ModItemTagProvider.STAMPS_WIRE)
                .material(Items.GOLD_INGOT)
                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "wire_gold"));
        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.WIRE_ALUMINIUM.get(), 8))
                .stamp(ModItemTagProvider.STAMPS_WIRE)
                .material(ModItems.getIngot(ModIngots.ALUMINUM).get())
                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "wire_aluminium"));
        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.WIRE_CARBON.get(), 8))
                .stamp(ModItemTagProvider.STAMPS_WIRE)
                .material(ModItems.getIngot(ModIngots.LEAD).get())
                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "wire_carbon"));
        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.WIRE_FINE.get(), 8))
                .stamp(ModItemTagProvider.STAMPS_WIRE)
                .material(Items.IRON_INGOT)
                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "wire_fine"));
        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.WIRE_TUNGSTEN.get(), 8))
                .stamp(ModItemTagProvider.STAMPS_WIRE)
                .material(ModItems.getIngot(ModIngots.TUNGSTEN).get())
                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "wire_tungsten"));
    }


    private static void generateCircuits(Consumer<FinishedRecipe> writer) {

    }
}

