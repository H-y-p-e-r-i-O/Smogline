package com.smogline.item.custom.liquids;

import com.smogline.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemFluidIdentifier extends Item {

    public ItemFluidIdentifier(Properties properties) {
        super(properties);
    }

    // Статический хелпер для получения жидкости
    public static Fluid getFluid(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("FluidID")) {
            ResourceLocation loc = new ResourceLocation(stack.getTag().getString("FluidID"));
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(loc);
            return fluid != null ? fluid : Fluids.EMPTY;
        }
        return Fluids.EMPTY;
    }

    // Статический хелпер для создания предмета с жидкостью
    public static ItemStack createStackFor(Fluid fluid) {
        // ВАЖНО: Используй здесь ссылку на свой зарегистрированный предмет
        ItemStack stack = new ItemStack(ModItems.FLUID_IDENTIFIER.get());
        CompoundTag tag = new CompoundTag();
        tag.putString("FluidID", ForgeRegistries.FLUIDS.getKey(fluid).toString());
        stack.setTag(tag);
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        Fluid fluid = getFluid(stack);
        if (fluid != Fluids.EMPTY) {
            // Логика: "НазваниеПредмета" + ": " + "НазваниеЖидкости"
            return Component.translatable(this.getDescriptionId())
                    .append(": ")
                    .append(fluid.getFluidType().getDescription());
        }
        // Если жидкость не задана (пустой идентификатор)
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Fluid fluid = getFluid(stack);
        if (fluid != Fluids.EMPTY) {
            tooltip.add(Component.literal("Type: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(ForgeRegistries.FLUIDS.getKey(fluid).toString()).withStyle(ChatFormatting.GOLD)));
        } else {
            tooltip.add(Component.literal("Empty / Universal").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
