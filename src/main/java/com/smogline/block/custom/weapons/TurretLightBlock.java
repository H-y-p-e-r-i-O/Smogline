package com.smogline.block.custom.weapons;

import com.smogline.entity.ModEntities;
import com.smogline.entity.weapons.turrets.TurretLightEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class TurretLightBlock extends Block {

    public TurretLightBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide) {
            // Удаляем блок
            level.removeBlock(pos, false);

            // Создаем турель
            TurretLightEntity turret = ModEntities.TURRET_LIGHT.get().create(level);
            if (turret != null) {
                // ПОВОРОТ: Берем угол игрока и добавляем 180 (разворот)
                // Если игрок смотрит на СЕВЕР (180), турель будет смотреть на ЮГ (0) -> друг на друга
                float yRot = placer != null ? placer.getYRot() : 0.0F;

                // Ставим в центр блока с нужным поворотом
                turret.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yRot, 0.0F);

                // Фиксируем поворот корпуса и головы сразу
                turret.setYRot(yRot);
                turret.yBodyRot = yRot;
                turret.yHeadRot = yRot;
                turret.yBodyRotO = yRot;
                turret.yHeadRotO = yRot;

                // Записываем владельца
                if (placer instanceof Player player) {
                    turret.setOwner(player);
                }

                // Инициализация
                turret.finalizeSpawn(level.getServer().getLevel(level.dimension()), level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);

                // Спавн
                level.addFreshEntity(turret);
            }
        }
    }
}
