package com.smogline.block;
// Этот класс содержит пользовательские теги блоков для мода HBM-Modernized.
// Теги позволяют группировать блоки по определенным характеристикам. TODO На данный момент частицы не излучаются радиоактивными блоками
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import com.smogline.lib.RefStrings;

import net.minecraft.resources.ResourceLocation;

public class HBMBlockTags {
    public static final TagKey<Block> EMIT_DARK_PARTICLES = BlockTags.create(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "emit_dark_particles"));
}
