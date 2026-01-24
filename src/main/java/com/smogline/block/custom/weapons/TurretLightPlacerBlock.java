package com.smogline.block.custom.weapons;

import com.smogline.block.entity.custom.TurretLightPlacerBlockEntity;
import com.smogline.entity.ModEntities;
import com.smogline.entity.weapons.turrets.TurretLightLinkedEntity;
import com.smogline.menu.TurretLightMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
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

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        // ЕСЛИ ИГРОК НА ШИФТЕ -> Открываем GUI
        if (player.isShiftKeyDown()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TurretLightPlacerBlockEntity turretBE) {
                if (player instanceof ServerPlayer serverPlayer) {
                    NetworkHooks.openScreen(serverPlayer,
                            new net.minecraft.world.SimpleMenuProvider(
                                    (windowId, playerInventory, playerEntity) ->
                                            new TurretLightMenu(windowId, playerInventory, turretBE.getAmmoContainer()),
                                    net.minecraft.network.chat.Component.literal("Turret Ammo")
                            )
                    );
                    return InteractionResult.SUCCESS;
                }
            }
        }

        // ИНАЧЕ -> Стандартная логика (спавн турели)
        try {
            // 1. Проверяем, нет ли уже турели
            AABB box = new AABB(pos).inflate(1.5);
            var existing = level.getEntitiesOfClass(TurretLightLinkedEntity.class, box,
                    t -> pos.equals(t.getParentBlock()));

            if (!existing.isEmpty()) {
                return InteractionResult.CONSUME;
            }

            // 2. Создаем турель
            TurretLightLinkedEntity turret = ModEntities.TURRET_LIGHT_LINKED.get().create(level);

            if (turret == null) {
                System.out.println("ERROR: Turret Entity failed to create (null)!");
                return InteractionResult.FAIL;
            }

            turret.setParentBlock(pos);

            // 3. Настройка позиции и данных
            turret.setPersistenceRequired();
            double x = pos.getX() + 0.5D;
            double y = pos.getY() + 1.0D;
            double z = pos.getZ() + 0.5D;
            float yRot = player.getYRot();
            turret.moveTo(x, y, z, yRot, 0.0F);
            turret.setYRot(yRot);
            turret.yBodyRot = yRot;
            turret.yHeadRot = yRot;

            // 4. Владелец
            turret.setOwner(player);

            // 5. Финализация спавна
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                turret.finalizeSpawn(
                        serverLevel,
                        level.getCurrentDifficultyAt(pos),
                        MobSpawnType.EVENT,
                        null,
                        null
                );
            }

            // 6. Добавляем в мир
            level.addFreshEntity(turret);

            // ✅ ОТКРЫВАЕМ GUI
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TurretLightPlacerBlockEntity turretBE) {
                if (player instanceof ServerPlayer serverPlayer) {
                    NetworkHooks.openScreen(serverPlayer,
                            new net.minecraft.world.SimpleMenuProvider(
                                    (windowId, playerInventory, playerEntity) ->
                                            new TurretLightMenu(windowId, playerInventory, turretBE.getAmmoContainer()),
                                    net.minecraft.network.chat.Component.literal("Turret Ammo")
                            )
                    );
                }
            }

            return InteractionResult.SUCCESS;

        } catch (Exception e) {
            System.out.println("CRASH IN TURRET PLACER BLOCK:");
            e.printStackTrace();
            return InteractionResult.FAIL;
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && state.getBlock() != newState.getBlock()) {
            AABB box = new AABB(pos).inflate(2.0);
            var turrets = level.getEntitiesOfClass(TurretLightLinkedEntity.class, box,
                    t -> pos.equals(t.getParentBlock()));
            turrets.forEach(t -> t.discard());
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
