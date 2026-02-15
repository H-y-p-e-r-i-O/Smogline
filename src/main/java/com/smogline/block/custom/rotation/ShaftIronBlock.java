package com.smogline.block.custom.rotation;

import com.smogline.block.entity.ModBlockEntities;
import com.smogline.block.entity.custom.ShaftIronBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
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

        boolean canPlace = false;
        Direction shaftFacing = clickedFace;

        if (targetBlock instanceof MotorElectroBlock) {
            Direction motorFacing = targetState.getValue(MotorElectroBlock.FACING);

            if (clickedFace == motorFacing) {
                canPlace = true;
                // Вал смотрит в ту же сторону что и мотор
                shaftFacing = motorFacing;
            }
        } else if (targetBlock instanceof ShaftIronBlock) {
            Direction existingFacing = targetState.getValue(ShaftIronBlock.FACING);

            // Клик должен быть по торцу вала (вдоль оси)
            if (clickedFace == existingFacing || clickedFace == existingFacing.getOpposite()) {
                canPlace = true;
                // Вал смотрит в ту же сторону что и существующий для синхронизации анимации
                shaftFacing = existingFacing;
            }
        }

        if (!canPlace) {
            return null;
        }

        return this.defaultBlockState().setValue(FACING, shaftFacing);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    @Override
    public VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
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