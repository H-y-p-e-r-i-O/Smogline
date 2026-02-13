package com.smogline.block.custom.rotation;

import com.smogline.block.entity.ModBlockEntities;
import com.smogline.block.entity.custom.ShaftIronBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

public class ShaftIronBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public ShaftIronBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShaftIronBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos placePos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();

        BlockPos targetPos = placePos.relative(clickedFace.getOpposite());
        BlockState targetState = level.getBlockState(targetPos);
        Block targetBlock = targetState.getBlock();

        System.out.println("=== DEBUG ===");
        System.out.println("clickedFace: " + clickedFace);
        System.out.println("targetBlock: " + targetBlock.getClass().getSimpleName());

        boolean canPlace = false;
        Direction shaftFacing = clickedFace; // дефолт

        if (targetBlock instanceof MotorElectroBlock) {
            Direction motorFacing = targetState.getValue(MotorElectroBlock.FACING);
            System.out.println("motorFacing: " + motorFacing);
            System.out.println("check: clickedFace(" + clickedFace + ") == motorFacing(" + motorFacing + ")");

            if (clickedFace == motorFacing) {
                canPlace = true;
                shaftFacing = motorFacing;
                System.out.println("MOTOR: shaftFacing set to " + shaftFacing);
            }
        } else if (targetBlock instanceof ShaftIronBlock) {
            Direction existingFacing = targetState.getValue(ShaftIronBlock.FACING);
            System.out.println("existingFacing: " + existingFacing);

            if (clickedFace == existingFacing || clickedFace == existingFacing.getOpposite()) {
                canPlace = true;
                shaftFacing = clickedFace;
                System.out.println("SHAFT: shaftFacing set to " + shaftFacing);
            }
        }

        System.out.println("Result: canPlace=" + canPlace + ", shaftFacing=" + shaftFacing);

        if (!canPlace) {
            return null;
        }

        return this.defaultBlockState().setValue(FACING, shaftFacing);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.SHAFT_IRON_BE.get(), ShaftIronBlockEntity::tick);
    }
}