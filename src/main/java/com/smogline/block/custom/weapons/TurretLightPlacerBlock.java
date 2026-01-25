package com.smogline.block.custom.weapons;

import com.smogline.api.energy.EnergyNetworkManager; // Импорт сети друга
import com.smogline.block.entity.ModBlockEntities;
import com.smogline.block.entity.custom.TurretLightPlacerBlockEntity;
import com.smogline.entity.weapons.turrets.TurretLightLinkedEntity;
import com.smogline.menu.TurretLightMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class TurretLightPlacerBlock extends BaseEntityBlock {

    public TurretLightPlacerBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TurretLightPlacerBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    // --- ЭНЕРГОСЕТЬ (КАК В ASSEMBLER) ---

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            // Добавляем узел в сеть, чтобы кабели видели этот блок
            EnergyNetworkManager.get((ServerLevel) level).addNode(pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide) {
                // Удаляем узел из сети
                EnergyNetworkManager.get((ServerLevel) level).removeNode(pos);

                // Удаляем саму турель, если она была
                AABB box = new AABB(pos).inflate(2.0);
                var turrets = level.getEntitiesOfClass(TurretLightLinkedEntity.class, box,
                        t -> pos.equals(t.getParentBlock()));
                turrets.forEach(t -> t.discard());
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    // ------------------------------------

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;


        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TurretLightPlacerBlockEntity turretBE) {

            // --- DEBUG: ВЫВОД ЭНЕРГИИ В ЧАТ ---
            long energy = turretBE.getEnergyStored();
            long max = turretBE.getMaxEnergyStored();
            String msg = String.format("§eEnergy: §b%d §7/ §b%d §7HE", energy, max);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(msg), true); // true = над хотбаром
            // ----------------------------------

            if (player instanceof ServerPlayer serverPlayer) {
                // В TurretLightPlacerBlock.java -> use()

                NetworkHooks.openScreen(serverPlayer,
                        new net.minecraft.world.SimpleMenuProvider(
                                (windowId, playerInventory, playerEntity) ->
                                        // ПЕРЕДАЕМ turretBE.getDataAccess()
                                        new TurretLightMenu(windowId, playerInventory, turretBE.getAmmoContainer(), turretBE.getDataAccess()),
                                net.minecraft.network.chat.Component.literal("Turret Buffer")
                        ),
                        pos
                );

            }
        }

        return InteractionResult.SUCCESS;
    }


    // ВАЖНО: Добавляем тикер, чтобы BlockEntity мог проверять энергию каждый тик
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.TURRET_LIGHT_PLACER_BE.get(), TurretLightPlacerBlockEntity::tick);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof Player player) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TurretLightPlacerBlockEntity turretBE) {
                turretBE.setOwner(player.getUUID());
            }
        }
    }


}
