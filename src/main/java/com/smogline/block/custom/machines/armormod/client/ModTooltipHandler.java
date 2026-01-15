package com.smogline.block.custom.machines.armormod.client;

import com.smogline.block.custom.machines.armormod.item.ItemArmorMod;
import com.smogline.datagen.assets.ModItemTagProvider;
import com.smogline.lib.RefStrings;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Этот класс отвечает за добавление общих подсказок ко всем предметам-модификациям брони.
 * Он слушает событие ItemTooltipEvent на стороне клиента.
 */
@Mod.EventBusSubscriber(modid = RefStrings.MODID, value = Dist.CLIENT)
public class ModTooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().getItem() instanceof ItemArmorMod mod) {
        List<Component> tooltip = event.getToolTip();
        
        // Добавляем отступ, чтобы отделить наши строки от строк эффектов
        tooltip.add(Component.empty());
        
        // Секция "Применяется к:"
        tooltip.add(Component.translatable("tooltip.smogline.applies_to").withStyle(ChatFormatting.DARK_PURPLE));
            
            boolean requiresHelmet = event.getItemStack().is(ModItemTagProvider.REQUIRES_HELMET);
            boolean requiresChestplate = event.getItemStack().is(ModItemTagProvider.REQUIRES_CHESTPLATE);
            boolean requiresLeggings = event.getItemStack().is(ModItemTagProvider.REQUIRES_LEGGINGS);
            boolean requiresBoots = event.getItemStack().is(ModItemTagProvider.REQUIRES_BOOTS);

            // Если мод подходит для ВСЕХ частей брони
            if (requiresHelmet && requiresChestplate && requiresLeggings && requiresBoots) {
                tooltip.add(Component.literal("  ").append(Component.translatable("tooltip.smogline.armor.all")).withStyle(ChatFormatting.GRAY));
            } else {
                // Если мод не универсальный, выводим список как раньше
                if (requiresHelmet) {
                    tooltip.add(Component.literal("  ").append(Component.translatable("tooltip.smogline.helmet")).withStyle(ChatFormatting.GRAY));
                }
                if (requiresChestplate) {
                    tooltip.add(Component.literal("  ").append(Component.translatable("tooltip.smogline.chestplate")).withStyle(ChatFormatting.GRAY));
                }
                if (requiresLeggings) {
                    tooltip.add(Component.literal("  ").append(Component.translatable("tooltip.smogline.leggings")).withStyle(ChatFormatting.GRAY));
                }
                if (requiresBoots) {
                    tooltip.add(Component.literal("  ").append(Component.translatable("tooltip.smogline.boots")).withStyle(ChatFormatting.GRAY));
                }
            }
            
            // Секция "Slot:"
            tooltip.add(Component.translatable("tooltip.smogline.slot").withStyle(ChatFormatting.DARK_PURPLE));
            
            // Определяем название слота по его типу (индексу)
            Component slotName = switch (mod.type) {
                case 0 -> Component.translatable("tooltip.smogline.helmet");
                case 1 -> Component.translatable("tooltip.smogline.chestplate");
                case 2 -> Component.translatable("tooltip.smogline.leggings");
                case 3 -> Component.translatable("tooltip.smogline.boots");
                case 4 -> Component.translatable("tooltip.smogline.armor_table.battery_slot");
                case 5 -> Component.translatable("tooltip.smogline.armor_table.special_slot");
                case 6 -> Component.translatable("tooltip.smogline.armor_table.plating_slot");
                case 7 -> Component.translatable("tooltip.smogline.armor_table.casing_slot");
                case 8 -> Component.translatable("tooltip.smogline.armor_table.servos_slot");
                default -> Component.literal("Unknown");
            };
            
            tooltip.add(Component.literal("  ").append(slotName).withStyle(ChatFormatting.GRAY));
        }
    }
}