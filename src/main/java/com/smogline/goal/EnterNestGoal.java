package com.smogline.goal;

import com.smogline.entity.custom.DepthWormEntity;
import com.smogline.block.entity.custom.DepthWormNestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

public class EnterNestGoal extends Goal {
    private final DepthWormEntity worm;

    public EnterNestGoal(DepthWormEntity worm) {
        this.worm = worm;
    }
    @Override
    public boolean canUse() {
        if (this.worm.nestPos == null) return false;

        double distSqr = this.worm.distanceToSqr(Vec3.atCenterOf(this.worm.nestPos));

        // Если червь слишком далеко (более 20 блоков), он ОБЯЗАН вернуться
        if (distSqr > 400.0D) return true;

        // Обычная логика захода домой (если нет врагов и таймер вышел)
        if (this.worm.getTarget() != null || this.worm.homeSickTimer > 0) return false;

        return distSqr > 1.0D;
    }



    @Override
    public void tick() {
        BlockPos pos = this.worm.nestPos;
        if (pos == null) return;

        this.worm.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0D);

        // Если подошли вплотную (меньше 1.5 блоков)
        if (this.worm.blockPosition().closerThan(pos, 1.5D)) {
            if (this.worm.level().getBlockEntity(pos) instanceof DepthWormNestBlockEntity nest) {
                if (!nest.isFull()) {
                    nest.addWorm(this.worm);
                } else {
                    this.worm.nestPos = null; // Ищем другое, это забилось
                }
            }
        }
    }

}