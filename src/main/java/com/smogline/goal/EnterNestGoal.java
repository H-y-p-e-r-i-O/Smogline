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
        // Теперь проверяем только наличие цели и само гнездо
        if (this.worm.getTarget() != null || this.worm.nestPos == null) return false;

        double distSqr = this.worm.distanceToSqr(Vec3.atCenterOf(this.worm.nestPos));
        if (distSqr > 400.0D) return true; // Принудительный возврат если уполз слишком далеко

        if (worm.level().getBlockEntity(worm.nestPos) instanceof DepthWormNestBlockEntity nest) {
            // Если улей на кулдауне (isFull вернет true), червь просто не пойдет к нему
            return !nest.isFull() && distSqr > 1.0D;
        }

        return false;
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