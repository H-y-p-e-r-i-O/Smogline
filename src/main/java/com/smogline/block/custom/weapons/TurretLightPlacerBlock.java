package com.smogline.block.custom.weapons;

import com.smogline.entity.ModEntities;
import com.smogline.entity.weapons.turrets.TurretLightEntity;
import com.smogline.entity.weapons.turrets.TurretLightLinkedEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TurretLightPlacerBlock extends Block {

    public TurretLightPlacerBlock(Properties properties) {
        super(properties);
    }

    // --- Спавн турели при установке ---
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (!level.isClientSide) {
            TurretLightLinkedEntity turret = ModEntities.TURRET_LIGHT_LINKED.get().create(level);

            if (turret != null) {
                float yRot = placer != null ? placer.getYRot() : 0.0F;

                // Ставим турель НАД блоком (Y + 1.0)
                turret.moveTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, yRot, 0.0F);
                turret.setYRot(yRot);
                turret.yBodyRot = yRot;
                turret.yHeadRot = yRot;

                // Записываем координаты этого блока в турель (для связи)
                turret.setParentBlock(pos);

                if (placer instanceof Player player) {
                    turret.setOwner(player);
                }

                turret.finalizeSpawn(level.getServer().getLevel(level.dimension()), level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
                level.addFreshEntity(turret);
            }
        }
    }

    // --- Уничтожение турели при поломке блока ---
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        // Если блок заменяется на ДРУГОЙ блок (то есть удаляется/ломается)
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide) {
                // Ищем все Linked-турели в радиусе 1.5 блока вокруг
                AABB searchBox = new AABB(pos).inflate(1.5);

                List<TurretLightLinkedEntity> turrets = level.getEntitiesOfClass(
                        TurretLightLinkedEntity.class,
                        searchBox,
                        // Доп. проверка: координаты родителя в турели совпадают с координатами этого блока
                        t -> pos.equals(t.getParentBlock())
                );

                // Убиваем всех найденных "детей"
                for (TurretLightLinkedEntity turret : turrets) {
                    turret.remove(TurretLightEntity.RemovalReason.DISCARDED);
                }
            }
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
