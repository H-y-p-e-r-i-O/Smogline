package com.smogline.datagen.recipes.custom;

import com.smogline.block.custom.machines.anvils.AnvilTier;
import com.smogline.block.ModBlocks;
import com.smogline.item.tags_and_tiers.ModIngots;
import com.smogline.item.ModItems;
import com.smogline.lib.RefStrings;
import com.smogline.recipe.AnvilRecipe;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Consumer;

public final class AnvilRecipeGenerator {
    private AnvilRecipeGenerator() { }

    public static void generate(Consumer<FinishedRecipe> writer) {
        registerTieredRecipes(writer);
    }

    private static void registerTieredRecipes(Consumer<FinishedRecipe> writer) {
        registerCombineRecipes(writer);
        registerCraftRecipes(writer);
        registerDisassemblyRecipes(writer);
    }

    private static void registerCombineRecipes(Consumer<FinishedRecipe> writer) {


        registerCombineRecipe(writer, "iron", "anvil_steel",
                stack(ModBlocks.ANVIL_IRON.get(), 1),
                stack(ModItems.getIngot(ModIngots.STEEL).get(), 10),
                stack(ModBlocks.ANVIL_STEEL.get(), 1),
                AnvilTier.IRON);




    }

    private static void registerCraftRecipes(Consumer<FinishedRecipe> writer) {



        registerInventoryRecipe(writer, "iron", "7blast_furnace",
                AnvilTier.IRON,
                stack(ModBlocks.BLAST_FURNACE),
                stack(Items.STONE_BRICKS, 4 ),
                stack(ModItems.FIREBRICK, 4 ));



    }

    private static void registerDisassemblyRecipes(Consumer<FinishedRecipe> writer) {






    }

    /**
     * Регистрирует рецепт объединения (Combine Recipe).
     *
     * @param writer      Потребитель рецептов
     * @param tierFolder  Папка тира (iron, steel...)
     * @param name        Имя рецепта
     * @param inputA      Первый входной слот
     * @param inputB      Второй входной слот
     * @param output      Результат
     * @param tier        Минимальный тир наковальни
     */
    private static void registerCombineRecipe(Consumer<FinishedRecipe> writer, String tierFolder, String name,
                                                ItemStack inputA, ItemStack inputB, ItemStack output,
                                                AnvilTier tier) {
        // Вызываем перегруженный метод без дополнительных настроек
        registerCombineRecipe(writer, tierFolder, name, inputA, inputB, output, tier, builder -> {});
    }

    /**
     * Регистрирует рецепт объединения с дополнительными настройками (например, сохранение предметов).
     */
    private static void registerCombineRecipe(Consumer<FinishedRecipe> writer, String tierFolder, String name,
                                                ItemStack inputA, ItemStack inputB, ItemStack output,
                                                AnvilTier tier, Consumer<AnvilRecipeBuilder> settings) {
        AnvilRecipeBuilder builder = AnvilRecipeBuilder.anvilRecipe(inputA, inputB, output, tier)
                .withOverlay(AnvilRecipe.OverlayType.SMITHING);
        
        // Применяем пользовательские настройки (здесь можно вызвать .keepInputA() и т.д.)
        settings.accept(builder);

        builder.save(writer, anvilId(tierFolder, "combine", name));
    }

    private static void registerInventoryRecipe(Consumer<FinishedRecipe> writer, String tierFolder, String name,
                                                AnvilTier tier, ItemStack output, ItemStack... requirements) {
        AnvilRecipeBuilder builder = AnvilRecipeBuilder.anvilRecipe(ItemStack.EMPTY, ItemStack.EMPTY, output, tier)
                .withOverlay(AnvilRecipe.OverlayType.CONSTRUCTION);
        for (ItemStack stack : requirements) {
            builder.addInventoryRequirement(stack);
        }
        builder.save(writer, anvilId(tierFolder, "craft", name));
    }

    private static void registerDisassemblyRecipe(Consumer<FinishedRecipe> writer, String tierFolder, String name,
                                                  AnvilTier tier, ItemStack dismantled, ItemStack primaryOutput,
                                                  Consumer<AnvilRecipeBuilder> outputs) {
        AnvilRecipeBuilder builder = AnvilRecipeBuilder.anvilRecipe(ItemStack.EMPTY, ItemStack.EMPTY, primaryOutput, tier)
                .withOverlay(AnvilRecipe.OverlayType.RECYCLING)
                .addInventoryRequirement(dismantled)
                .clearOutputs()
                .addOutput(primaryOutput, 1.0F);
        outputs.accept(builder);
        builder.save(writer, anvilId(tierFolder, "disassemble", name));
    }

    private static ResourceLocation anvilId(String tierFolder, String category, String name) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID,
                "anvil/" + tierFolder + "/" + category + "_" + name);
    }

    private static ItemStack stack(Object obj, int count) {
        if (obj instanceof RegistryObject<?>) {
            Object val = ((RegistryObject<?>) obj).get();
            if (val instanceof Item) {
                return new ItemStack((Item) val, count);
            } else if (val instanceof Block) {
                return new ItemStack(((Block) val).asItem(), count);
            }
        } else if (obj instanceof Item) {
            return new ItemStack((Item) obj, count);
        } else if (obj instanceof Block) {
            return new ItemStack(((Block) obj).asItem(), count);
        }
        throw new IllegalArgumentException("Unsupported object for stack: " + obj);
    }

    private static ItemStack stack(Object obj) {
        return stack(obj, 1);
    }


}

