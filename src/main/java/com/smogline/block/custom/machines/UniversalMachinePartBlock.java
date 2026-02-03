package com.smogline.block.custom.machines;

import com.smogline.block.entity.custom.machines.UniversalMachinePartBlockEntity;
import com.smogline.multiblock.IMultiblockController;
import com.smogline.multiblock.IMultiblockPart;
import com.smogline.multiblock.MultiblockStructureHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class UniversalMachinePartBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    // Дефолтный маленький шейп, если что-то пошло не так
    private static final VoxelShape SMALL_INTERACT_SHAPE = Shapes.box(0.45, 0.45, 0.45, 0.55, 0.55, 0.55);

    public UniversalMachinePartBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new UniversalMachinePartBlockEntity(pPos, pState);
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        // 1. Получаем BlockEntity части
        if (!(pLevel.getBlockEntity(pPos) instanceof IMultiblockPart part)) {
            return SMALL_INTERACT_SHAPE;
        }

        // 2. Ищем позицию контроллера
        BlockPos controllerPos = part.getControllerPos();
        if (controllerPos == null) {
            return SMALL_INTERACT_SHAPE;
        }

        // 3. Получаем состояние контроллера
        BlockState controllerState = pLevel.getBlockState(controllerPos);
        if (!(controllerState.getBlock() instanceof IMultiblockController controller)) {
            return SMALL_INTERACT_SHAPE;
        }

        // 4. Генерируем форму на основе логики контроллера
        VoxelShape masterShape = controller.getCustomMasterVoxelShape(controllerState);
        if (masterShape == null) {
            // Если кастомной формы нет, берем общую из структуры
            masterShape = controller.getStructureHelper().generateShapeFromParts(controllerState.getValue(FACING));
        }

        if (masterShape.isEmpty()) {
            return SMALL_INTERACT_SHAPE;
        }

        // 5. Сдвигаем форму так, чтобы она корректно отображалась для ЭТОЙ части структуры
        BlockPos offset = pPos.subtract(controllerPos);
        return masterShape.move(-offset.getX(), -offset.getY(), -offset.getZ());
    }

    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        // Логика коллизии идентична логике формы (getShape), но возвращает Shapes.empty() при ошибках,
        // чтобы игрок не застревал в невидимых блоках.

        if (!(pLevel.getBlockEntity(pPos) instanceof IMultiblockPart part)) {
            return Shapes.empty();
        }

        BlockPos controllerPos = part.getControllerPos();
        if (controllerPos == null) {
            return Shapes.empty();
        }

        BlockState controllerState = pLevel.getBlockState(controllerPos);
        if (!(controllerState.getBlock() instanceof IMultiblockController controller)) {
            return Shapes.empty();
        }

        // Используем ту же логику получения формы от контроллера
        VoxelShape masterShape = controller.getCustomMasterVoxelShape(controllerState);
        if (masterShape == null) {
            masterShape = controller.getStructureHelper().generateShapeFromParts(controllerState.getValue(FACING));
        }

        if (masterShape.isEmpty()) {
            return Shapes.empty();
        }

        BlockPos offset = pPos.subtract(controllerPos);
        return masterShape.move(-offset.getX(), -offset.getY(), -offset.getZ());
    }

    @Override
    public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
        super.neighborChanged(pState, pLevel, pPos, pBlock, pFromPos, pIsMoving);
        MultiblockStructureHelper.onNeighborChangedForPart(pLevel, pPos, pFromPos);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pLevel.getBlockEntity(pPos) instanceof IMultiblockPart part) {
            BlockPos controllerPos = part.getControllerPos();

            if (controllerPos == null) {
                if (!pLevel.isClientSide()) {
                    pLevel.setBlock(pPos, Blocks.AIR.defaultBlockState(), 3);
                }
                return InteractionResult.sidedSuccess(pLevel.isClientSide());
            }

            BlockState controllerState = pLevel.getBlockState(controllerPos);
            if (controllerState.getBlock() instanceof IMultiblockController) {
                // Перенаправляем клик на контроллер
                return controllerState.use(pLevel, pPlayer, pHand, pHit.withPosition(controllerPos));
            } else {
                // Если контроллер невалиден — удаляем фантом
                if (!pLevel.isClientSide()) {
                    pLevel.setBlock(pPos, Blocks.AIR.defaultBlockState(), 3);
                }
                return InteractionResult.sidedSuccess(pLevel.isClientSide());
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (!pState.is(pNewState.getBlock())) {
            if (pLevel.getBlockEntity(pPos) instanceof IMultiblockPart partBe) {
                BlockPos controllerPos = partBe.getControllerPos();
                if (controllerPos != null) {
                    BlockState controllerState = pLevel.getBlockState(controllerPos);
                    if (controllerState.getBlock() instanceof IMultiblockController) {
                        pLevel.destroyBlock(controllerPos, true);
                    }
                }
            }
        }
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof IMultiblockPart part) {
            BlockPos controllerPos = part.getControllerPos();
            if (controllerPos != null) {
                BlockState controllerState = level.getBlockState(controllerPos);
                return controllerState.getBlock().getCloneItemStack(level, controllerPos, controllerState);
            }
        }
        return ItemStack.EMPTY;
    }
}
