package com.hbm_m.client.model;

import com.hbm_m.block.entity.custom.machines.MachineFluidTankBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FluidTankModelWrapper extends BakedModelWrapper<BakedModel> {

    private final TextureAtlasSprite targetSprite;

    public FluidTankModelWrapper(BakedModel original) {
        super(original);
        // Мы ищем текстуру "tank_none", которую указали в .mtl файле, чтобы заменить её
        // Убедись, что путь совпадает с тем, что в .mtl: map_Kd hbm_m:block/tank/tank_none
        this.targetSprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(new ResourceLocation("hbm_m", "block/tank/tank_none"));
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData data, @Nullable net.minecraft.client.renderer.RenderType renderType) {
        // Получаем исходные квады (полигоны) модели
        List<BakedQuad> originalQuads = super.getQuads(state, side, rand, data, renderType);

        // 1. Проверяем, есть ли данные о жидкости в ModelData
        if (!data.has(MachineFluidTankBlockEntity.FLUID_RENDER_PROP)) {
            return originalQuads;
        }

        Fluid fluid = data.get(MachineFluidTankBlockEntity.FLUID_RENDER_PROP);

        // Если жидкости нет или она пустая - возвращаем стандартную модель (белую)
        if (fluid == null) {
            return originalQuads;
        }

        // 2. Получаем текстуру самой жидкости
        IClientFluidTypeExtensions fluidInfo = IClientFluidTypeExtensions.of(fluid);
        ResourceLocation fluidTexturePath = fluidInfo.getStillTexture();
        TextureAtlasSprite fluidSprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(fluidTexturePath);

        // 3. Создаем новый список квадов с подменой
        List<BakedQuad> newQuads = new ArrayList<>();

        for (BakedQuad quad : originalQuads) {
            // Проверяем: если этот полигон использует текстуру "tank_none"...
            if (quad.getSprite().contents().name().equals(targetSprite.contents().name())) {
                // ... создаем копию полигона, но с текстурой жидкости
                BakedQuad newQuad = new BakedQuad(
                        quad.getVertices(),
                        quad.getTintIndex(),
                        quad.getDirection(),
                        fluidSprite, // <--- ПОДМЕНА ЗДЕСЬ
                        quad.isShade()
                );
                newQuads.add(newQuad);
            } else {
                // Остальные части (раму) оставляем как есть
                newQuads.add(quad);
            }
        }

        return newQuads;
    }
}