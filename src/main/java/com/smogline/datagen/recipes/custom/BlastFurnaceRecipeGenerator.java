package com.smogline.datagen.recipes.custom;

import com.smogline.datagen.recipes.ModRecipeProvider;
import com.smogline.item.tags_and_tiers.ModIngots;
import com.smogline.item.ModItems;
import com.smogline.lib.RefStrings;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.Tags;

import java.util.function.Consumer;

/**
 * Handles Blast Furnace recipe generation to keep {@link ModRecipeProvider} focused on orchestration.
 */
public final class BlastFurnaceRecipeGenerator {

    private BlastFurnaceRecipeGenerator() {
    }

    public static void generate(Consumer<FinishedRecipe> writer) {
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(
                new ItemStack(ModItems.getIngot(ModIngots.STEEL).get()),
                Ingredient.of(Items.IRON_INGOT),
                Ingredient.of(ItemTags.COALS)
        ).save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "blast_furnace/steel_from_ingot"));

        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(
                new ItemStack(ModItems.getIngot(ModIngots.STEEL).get(), 2),
                Ingredient.of(Tags.Items.ORES_IRON),
                Ingredient.of(ItemTags.COALS)
        ).save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "blast_furnace/steel_from_ore"));




    }
}

