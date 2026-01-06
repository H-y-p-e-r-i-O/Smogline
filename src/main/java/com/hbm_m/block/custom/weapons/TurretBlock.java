package com.hbm_m.block.custom.weapons;

import com.hbm_m.block.entity.custom.TurretBlockEntity;
import com.hbm_m.main.MainRegistry; // Замени на свой класс регистрации
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class TurretBlock extends BaseEntityBlock {

    public TurretBlock(Properties properties) {
        super(properties);
    }

    // ЭТО САМОЕ ГЛАВНОЕ ДЛЯ GECKOLIB БЛОКОВ!
    // Это говорит игре: "Не рисуй модель через JSON, я сам её нарисую через TileEntityRenderer"
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TurretBlockEntity(pos, state);
    }
}
