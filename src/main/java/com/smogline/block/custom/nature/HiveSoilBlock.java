package com.smogline.block.custom.nature;

import com.smogline.api.hive.HiveNetworkManager;
import com.smogline.block.entity.HiveSoilBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class HiveSoilBlock extends Block implements EntityBlock {
    public HiveSoilBlock(Properties properties) { super(properties); }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HiveSoilBlockEntity(pos, state);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            HiveNetworkManager manager = HiveNetworkManager.get(level);
            if (manager != null) manager.onBlockAdded(level, pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide) {
            HiveNetworkManager manager = HiveNetworkManager.get(level);
            if (manager != null) manager.onBlockRemoved(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}