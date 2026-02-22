package com.smogline.goal;

import com.smogline.entity.custom.DepthWormEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;

public class DepthWormJumpGoal extends Goal {
    private final DepthWormEntity worm;
    private LivingEntity target;
    private final double speedModifier;
    private final float jumpRangeMin, jumpRangeMax;
    private int jumpTimer;
    private final int PREPARE_TIME = 30; // 1.5 сек

    public DepthWormJumpGoal(DepthWormEntity worm, double speedModifier, float jumpRangeMin, float jumpRangeMax) {
        this.worm = worm;
        this.speedModifier = speedModifier;
        this.jumpRangeMin = jumpRangeMin;
        this.jumpRangeMax = jumpRangeMax;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        this.target = this.worm.getTarget();
        if (this.target == null || !this.target.isAlive()) return false;
        double dist = this.worm.distanceTo(this.target);
        return dist >= this.jumpRangeMin && dist <= this.jumpRangeMax;
    }

    @Override
    public boolean canContinueToUse() { return this.canUse() || this.jumpTimer > 0; }

    @Override
    public void start() {
        this.jumpTimer = PREPARE_TIME;
        this.worm.getNavigation().stop();
        this.worm.setAttacking(true);
    }

    @Override
    public void stop() {
        this.target = null;
        this.worm.setAttacking(false);
    }

    @Override
    public void tick() {
        this.worm.getLookControl().setLookAt(this.target.getX(), this.target.getEyeY(), this.target.getZ());
        if (--this.jumpTimer == 0) {
            doJump();
            this.worm.setAttacking(false);
        }
    }

    private void doJump() {
        // Получаем вектор направления к глазам цели
        Vec3 targetVec = this.target.getEyePosition().subtract(this.worm.position());

        // Вычисляем горизонтальное расстояние
        double horizontalDist = Math.sqrt(targetVec.x * targetVec.x + targetVec.z * targetVec.z);

        // Нормализуем вектор и задаем силу прыжка
        // speedModifier (1.5) определит общую скорость полета
        Vec3 velocity = targetVec.normalize().scale(1.2);

        // Добавляем небольшой подброс вверх в зависимости от дистанции
        double yInertia = 0.2 + (horizontalDist * 0.05);

        this.worm.setDeltaMovement(new Vec3(velocity.x, yInertia, velocity.z));
    }

}