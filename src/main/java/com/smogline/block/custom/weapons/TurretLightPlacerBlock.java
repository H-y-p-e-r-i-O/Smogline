package com.smogline.block.custom.weapons;

import com.smogline.block.entity.custom.TurretLightPlacerBlockEntity;
import com.smogline.entity.weapons.turrets.TurretLightEntity;
import com.smogline.entity.ModEntities; // Или где у тебя турель
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class TurretLightPlacerBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    // Форма блока (примерно полблока или как тебе надо, можно поменять на Shapes.block())
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public TurretLightPlacerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    // === БЛОК-СУЩНОСТЬ (BlockEntity) ===

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TurretLightPlacerBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // ENTITYBLOCK_ANIMATED скрывает стандартную модель JSON и позволяет GeckoLib рисовать всё самому
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    // === ЛОГИКА ===

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    // === СТАРАЯ ЛОГИКА (Спавн турели при клике) ===

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            // Спавним сущность турели
            TurretLightEntity turret = ModEntities.TURRET_LIGHT.get().create(level);
            if (turret != null) {
                turret.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                turret.setYRot(state.getValue(FACING).toYRot());
                level.addFreshEntity(turret);

                // Удаляем сам блок-размещатель, чтобы на его месте встала турель
                level.removeBlock(pos, false);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
