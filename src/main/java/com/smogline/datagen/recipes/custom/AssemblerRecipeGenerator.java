package com.smogline.datagen.recipes.custom;

import com.smogline.block.ModBlocks;
import com.smogline.datagen.recipes.ModRecipeProvider;
import com.smogline.item.tags_and_tiers.ModIngots;
import com.smogline.item.ModItems;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

/**
 * Groups all assembler recipes so they can be maintained separately from {@link ModRecipeProvider}.
 */
public final class AssemblerRecipeGenerator {

    private AssemblerRecipeGenerator() {
    }

    public static void generate(Consumer<FinishedRecipe> writer) {
        registerMainRecipes(writer);
        registerElectronics(writer);
        registerPlateRecipes(writer);
    }


    private static void registerMainRecipes(Consumer<FinishedRecipe> writer) {


    }

    private static void registerElectronics(Consumer<FinishedRecipe> writer) {

    }

    private static void registerPlateRecipes(Consumer<FinishedRecipe> writer) {



    }
}