package com.smogline.block.custom.rotation;

import com.smogline.block.entity.ModBlockEntities;
import com.smogline.block.entity.custom.GearPortBlockEntity;
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

    @Nullable
    @Override
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
                shaftFacing = motorFacing;
            }
        } else if (targetBlock instanceof ShaftIronBlock) {
            Direction existingFacing = targetState.getValue(ShaftIronBlock.FACING);
            if (clickedFace == existingFacing || clickedFace == existingFacing.getOpposite()) {
                canPlace = true;
                shaftFacing = existingFacing;
            }
        } else if (targetBlock instanceof GearPortBlock) {
            BlockEntity be = level.getBlockEntity(targetPos);
            if (be instanceof GearPortBlockEntity gear) {
                if (gear.hasPortOnSide(clickedFace)) {
                    canPlace = true;
                    shaftFacing = clickedFace;
                }
            }
        } else if (targetBlock instanceof RotationMeterBlock) {
            Direction meterFacing = targetState.getValue(RotationMeterBlock.FACING);
            Direction left, right;
            switch (meterFacing) {
                case NORTH:
                    left = Direction.WEST;
                    right = Direction.EAST;
                    break;
                case SOUTH:
                    left = Direction.EAST;
                    right = Direction.WEST;
                    break;
                case EAST:
                    left = Direction.NORTH;
                    right = Direction.SOUTH;
                    break;
                case WEST:
                    left = Direction.SOUTH;
                    right = Direction.NORTH;
                    break;
                default:
                    left = right = null;
            }
            if (clickedFace == left || clickedFace == right) {
                canPlace = true;
                shaftFacing = clickedFace;
            }
        } else if (targetBlock instanceof StopperBlock) {
            Direction stopperFacing = targetState.getValue(StopperBlock.FACING);
            Direction left, right;
            switch (stopperFacing) {
                case NORTH: left = Direction.WEST; right = Direction.EAST; break;
                case SOUTH: left = Direction.EAST; right = Direction.WEST; break;
                case EAST:  left = Direction.NORTH; right = Direction.SOUTH; break;
                case WEST:  left = Direction.SOUTH; right = Direction.NORTH; break;
                case UP:    left = Direction.NORTH; right = Direction.SOUTH; break;
                case DOWN:  left = Direction.SOUTH; right = Direction.NORTH; break;
                default: left = right = null;
            }
            if (clickedFace == left || clickedFace == right) {
                canPlace = true;
                shaftFacing = clickedFace;
            }
        } else if (targetBlock instanceof AdderBlock) {
            Direction adderFacing = targetState.getValue(AdderBlock.FACING);
            Direction left, right;
            switch (adderFacing) {
                case NORTH: left = Direction.WEST; right = Direction.EAST; break;
                case SOUTH: left = Direction.EAST; right = Direction.WEST; break;
                case EAST:  left = Direction.NORTH; right = Direction.SOUTH; break;
                case WEST:  left = Direction.SOUTH; right = Direction.NORTH; break;
                default: left = right = null;
            }
            // Вал можно ставить на заднюю сторону (выход) и на боковые стороны (входы)
            Direction outputSide = adderFacing.getOpposite();
            if (clickedFace == outputSide || clickedFace == left || clickedFace == right) {
                canPlace = true;
                shaftFacing = clickedFace;
            }
        } else if (targetBlock instanceof TachometerBlock) {
            Direction tachoFacing = targetState.getValue(TachometerBlock.FACING);
            Direction left, right;
            switch (tachoFacing) {
                case NORTH: left = Direction.WEST; right = Direction.EAST; break;
                case SOUTH: left = Direction.EAST; right = Direction.WEST; break;
                case EAST:  left = Direction.NORTH; right = Direction.SOUTH; break;
                case WEST:  left = Direction.SOUTH; right = Direction.NORTH; break;
                case UP:    left = Direction.NORTH; right = Direction.SOUTH; break;
                case DOWN:  left = Direction.SOUTH; right = Direction.NORTH; break;
                default: left = right = null;
            }
            if (clickedFace == left || clickedFace == right) {
                canPlace = true;
                shaftFacing = clickedFace;
            }
        } else if (targetBlock instanceof WindGenFlugerBlock) {
            // Вал можно ставить только снизу
            if (clickedFace == Direction.DOWN) {
                canPlace = true;
                shaftFacing = Direction.DOWN;
            }
        }

        if (!canPlace) {
            return null;
        }

        return this.defaultBlockState().setValue(FACING, shaftFacing);
    }

    private static final VoxelShape SHAPE_NORTH_SOUTH = Block.box(6.75, 6.75, 0, 9.25, 9.25, 16); // X/Z:2.5px→6.75-9.25, Y:2.5px
    private static final VoxelShape SHAPE_EAST_WEST = Block.box(0, 6.75, 6.75, 16, 9.25, 9.25);   // X:16px, Y/Z:2.5px
    private static final VoxelShape SHAPE_UP_DOWN = Block.box(6.75, 0, 6.75, 9.25, 16, 9.25);     // Y:16px, X/Z:2.5px
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return switch (facing) {
            case NORTH, SOUTH -> SHAPE_NORTH_SOUTH;
            case EAST, WEST -> SHAPE_EAST_WEST;
            case UP, DOWN -> SHAPE_UP_DOWN;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return getShape(state, level, pos, null);
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