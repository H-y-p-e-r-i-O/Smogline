package com.smogline.block.custom.weapons;

import com.smogline.block.entity.custom.TurretLightPlacerBlockEntity;
import com.smogline.entity.ModEntities;
import com.smogline.entity.weapons.turrets.TurretLightLinkedEntity;
import net.minecraft.core.BlockPos;
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
import org.jetbrains.annotations.Nullable;

public class TurretLightPlacerBlock extends BaseEntityBlock {

    public TurretLightPlacerBlock(Properties properties) {
        super(properties);
    }

    // ✅ БЛОК-СУЩНОСТЬ (если нужна анимация GeckoLib)
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TurretLightPlacerBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED; // GeckoLib рендер
    }

    // ✅ ГЛАВНАЯ ЛОГИКА: Клик правой кнопкой → спавнить турель
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        try {
            // 1. Проверяем, нет ли уже турели (чтобы не ставить друг на друга)
            net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(pos).inflate(1.5);
            var existing = level.getEntitiesOfClass(TurretLightLinkedEntity.class, box,
                    t -> pos.equals(t.getParentBlock()));

            if (!existing.isEmpty()) {
                return InteractionResult.CONSUME; // Турель уже есть
            }

            // 2. БЕЗОПАСНОЕ СОЗДАНИЕ СУЩНОСТИ
            // Используем .create(), это стандарт Forge, он сам подтянет нужный EntityType
            TurretLightLinkedEntity turret = ModEntities.TURRET_LIGHT_LINKED.get().create(level);
            turret.setParentBlock(pos); // <--- ЭТА СТРОКА ТОЧНО ЕСТЬ?
            if (turret == null) {
                System.out.println("ERROR: Turret Entity failed to create (null)!");
                return InteractionResult.FAIL;
            }

            // 3. Настройка позиции и данных
            turret.setPersistenceRequired();
            // Центр буфера + 1 блок вверх
            double x = pos.getX() + 0.5D;
            double y = pos.getY() + 1.0D;
            double z = pos.getZ() + 0.5D;

            // Поворот
            float yRot = player.getYRot();
            turret.moveTo(x, y, z, yRot, 0.0F);
            turret.setYRot(yRot);
            turret.yBodyRot = yRot;
            turret.yHeadRot = yRot;

            // 4. Владелец
            turret.setOwner(player);

            // 5. Финализация спавна (кастим Level в ServerLevel безопасно)
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

            return InteractionResult.SUCCESS;

        } catch (Exception e) {
            // 🔥 ЭТО ПОКАЖЕТ НАСТОЯЩУЮ ОШИБКУ В КОНСОЛИ
            System.out.println("CRASH IN TURRET PLACER BLOCK:");
            e.printStackTrace();
            return InteractionResult.FAIL;
        }
    }


    // ✅ ЕСЛИ БЛОК СЛОМАН → удалить турель
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
