package com.smogline.entity.weapons.turrets.logic;

import com.mojang.datafixers.util.Pair;
import com.smogline.entity.weapons.bullets.TurretBulletEntity;
import com.smogline.entity.weapons.turrets.TurretLightEntity;
import com.smogline.entity.weapons.turrets.TurretLightLinkedEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Бортовой компьютер для лёгких турелей.
 * Отвечает за поиск целей, приоритеты, баллистику, упреждение и проверку линии огня.
 */
public class TurretLightComputer {

    // === КОНФИГУРАЦИЯ ТУРЕЛИ ===
    public static class Config {
        public final float bulletSpeed;
        public final float bulletGravity;
        public final double drag;
        public final double maxRangeSqr;
        public final double closeCombatRangeSqr;

        public Config(float bulletSpeed, float bulletGravity, double drag, double maxRange) {
            this.bulletSpeed = bulletSpeed;
            this.bulletGravity = bulletGravity;
            this.drag = drag;
            this.maxRangeSqr = maxRange * maxRange;
            this.closeCombatRangeSqr = 5.0 * 5.0; // 5 блоков
        }

        // Стандартный пресет для 20мм пушки
        public static final Config STANDARD_20MM = new Config(3.0F, 0.01F, 0.99, 35.0);
    }

    private final Mob turret; // Владелец компьютера (сама турель)
    private final Config config;
    private final Level level;

    // === WAR THUNDER TRACKING DATA ===
    private Vec3 lastTargetPos = Vec3.ZERO;
    private Vec3 avgTargetVelocity = Vec3.ZERO;
    private Vec3 targetAcceleration = Vec3.ZERO;
    private int trackingTicks = 0;
    private LivingEntity currentTargetCache = null;

    // === RECOIL FILTER ===
    private long lastShotTimeMs = 0L;
    private long expectedImpactTimeMs = 0L;
    private static final long Y_IMPULSE_FILTER_MS = 350L;

    // === OPTIMIZATION ===
    private int raycastSkipTimer = 0;
    private Vec3 cachedSmartTargetPos = null;
    private static final int RAYCAST_INTERVAL = 4;

    // === DEBUG ===
    public Vec3 debugTargetPoint = null;
    public Vec3 debugBallisticVelocity = null;
    public final List<Pair<Vec3, Boolean>> debugScanPoints = new ArrayList<>();

    public TurretLightComputer(Mob turret, Config config) {
        this.turret = turret;
        this.level = turret.level();
        this.config = config;
    }

    // ========================================================================
    // 🎯 ПОИСК И ПРИОРИТЕТЫ ЦЕЛЕЙ
    // ========================================================================

    public int calculateTargetPriority(LivingEntity entity, UUID ownerUUID) {
        if (entity == null || !entity.isAlive() || isAllied(entity, ownerUUID)) return 999;

        // 🔥 ДОБАВЛЕНО: Не атаковать другие турели того же владельца
        if (entity instanceof TurretLightLinkedEntity || entity instanceof TurretLightEntity) {
            // isAllied уже проверяет владельца, но на всякий случай явно:
            if (isAllied(entity, ownerUUID)) return 999;

            // Если хочешь чтобы турели ВООБЩЕ не воевали друг с другом (даже чужие):
            // return 999;
        }

        double distanceSqr = turret.distanceToSqr(entity);
        if (distanceSqr < config.closeCombatRangeSqr) return 0; // В упор - высший приоритет

        Player owner = ownerUUID != null ? level.getPlayerByUUID(ownerUUID) : null;

        if (owner != null) {
            if (owner.getLastHurtByMob() == entity) return 1; // Кто бьет хозяина
            if (entity instanceof Mob mob && mob.getTarget() == owner) return 1; // Кто целится в хозяина
        }

        if (turret.getLastHurtByMob() == entity) return 3; // Кто бьет меня
        if (owner != null && owner.getLastHurtMob() == entity) return 4; // Кого бьет хозяин
        if (entity instanceof Monster) return 5; // Монстры
        if (entity instanceof Player) return 6; // Игроки (чужие)

        return 999;
    }

    public boolean isAllied(Entity entity, UUID ownerUUID) {
        // 1. Проверка владельца
        if (ownerUUID != null && entity.getUUID().equals(ownerUUID)) return true;

        // 2. Проверка команд (ванильная логика без вызова isAlliedTo турели)
        if (turret.getTeam() != null && entity.getTeam() != null) {
            if (turret.getTeam().isAlliedTo(entity.getTeam())) {
                return true;
            }
        }

        // 3. Союзные турели того же владельца (Linked)
        if (entity instanceof TurretLightLinkedEntity linked) {
            return ownerUUID != null && ownerUUID.equals(linked.getOwnerUUID());
        }

        // 4. Союзные турели того же владельца (Обычные)
        if (entity instanceof TurretLightEntity light) {
            return ownerUUID != null && ownerUUID.equals(light.getOwnerUUID());
        }

        return false;
    }



    public LivingEntity findClosestThreat(UUID ownerUUID) {
        LivingEntity closest = null;
        double closestDist = config.closeCombatRangeSqr;

        // Оптимизация: ищем только в радиусе ближнего боя
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class,
                turret.getBoundingBox().inflate(Math.sqrt(config.closeCombatRangeSqr)));

        for (LivingEntity entity : entities) {
            if (!isAllied(entity, ownerUUID) && entity.isAlive()) {
                double dist = turret.distanceToSqr(entity);
                if (dist < closestDist) {
                    closest = entity;
                    closestDist = dist;
                }
            }
        }
        return closest;
    }

    // ========================================================================
    // 🚀 БАЛЛИСТИКА И УПРЕЖДЕНИЕ (WAR THUNDER STYLE)
    // ========================================================================

    public void updateTracking(LivingEntity target) {
        if (target != currentTargetCache) {
            // Новая цель - сброс трекера
            currentTargetCache = target;
            trackingTicks = 0;
            lastTargetPos = target != null ? target.position() : Vec3.ZERO;
            avgTargetVelocity = Vec3.ZERO;
            targetAcceleration = Vec3.ZERO;
            cachedSmartTargetPos = null;
        }

        if (target != null && target.isAlive()) {
            Vec3 currentPos = target.position();
            if (trackingTicks > 0) {
                Vec3 instantaneousVel = currentPos.subtract(lastTargetPos);
                // Сглаживание скорости (lerp 0.15)
                this.avgTargetVelocity = this.avgTargetVelocity.lerp(instantaneousVel, 0.15);

                Vec3 newAccel = instantaneousVel.subtract(this.avgTargetVelocity);
                this.targetAcceleration = this.targetAcceleration.lerp(newAccel, 0.05);
            } else {
                this.avgTargetVelocity = target.getDeltaMovement();
                this.targetAcceleration = Vec3.ZERO;
            }
            this.lastTargetPos = currentPos;
            this.trackingTicks++;
        } else {
            trackingTicks = 0;
        }
    }

    public Vec3 calculateBallisticVelocity(LivingEntity target, Vec3 muzzlePos) {
        Vec3 visibleBasePos = getSmartTargetPos(target, muzzlePos);
        if (visibleBasePos == null) return null;

        double maxVisibleY = visibleBasePos.y + 0.5;
        Vec3 targetVel;
        Vec3 targetAcc;

        if (trackingTicks > 5) {
            targetVel = this.avgTargetVelocity;

            // Фильтр отдачи (чтобы не стрелять в небо когда моб подпрыгивает от удара)
            long now = System.currentTimeMillis();
            boolean postImpact = now < expectedImpactTimeMs;
            boolean insideFilterWindow = now < expectedImpactTimeMs + Y_IMPULSE_FILTER_MS;
            boolean isRecoilState = postImpact || insideFilterWindow || target.hurtTime > 0;

            if (target.onGround() || isRecoilState) {
                targetVel = new Vec3(targetVel.x, 0, targetVel.z);
                targetAcc = new Vec3(this.targetAcceleration.x, 0, this.targetAcceleration.z);
            } else {
                targetAcc = this.targetAcceleration;
            }
        } else {
            targetVel = target.getDeltaMovement();
            if (target.onGround()) {
                targetVel = new Vec3(targetVel.x, 0, targetVel.z);
            }
            targetAcc = Vec3.ZERO;
        }

        // Итеративное предсказание
        double dist = visibleBasePos.distanceTo(muzzlePos);
        double t = calculateFlightTime(dist);
        Vec3 predictedPos = visibleBasePos;

        for (int i = 0; i < 4; i++) {
            Vec3 velocityPart = targetVel.scale(t);
            Vec3 accelPart = targetAcc.scale(0.5 * t * t);
            predictedPos = visibleBasePos.add(velocityPart).add(accelPart);

            if (predictedPos.y > maxVisibleY) {
                predictedPos = new Vec3(predictedPos.x, maxVisibleY, predictedPos.z);
            }

            if (!canSeePoint(muzzlePos, predictedPos)) {
                predictedPos = visibleBasePos; // Откат если точка ушла в стену
            }

            double newDist = predictedPos.distanceTo(muzzlePos);
            double newT = calculateFlightTime(newDist);
            if (Math.abs(newT - t) < 0.05) {
                t = newT;
                break;
            }
            t = newT;
        }

        this.debugTargetPoint = predictedPos;
        return solveBallisticArc(muzzlePos, predictedPos, t);
    }

    private Vec3 solveBallisticArc(Vec3 muzzle, Vec3 target, double t) {
        double dragFactor = getDragCompensationFactor(t);
        double dirX = target.x - muzzle.x;
        double dirZ = target.z - muzzle.z;
        double dirY = target.y - muzzle.y;
        double horizontalDist = Math.sqrt(dirX * dirX + dirZ * dirZ) * dragFactor;

        double v = config.bulletSpeed;
        double v2 = v * v;
        double v4 = v2 * v2;
        double g = config.bulletGravity * dragFactor;

        double discriminant = v4 - g * (g * horizontalDist * horizontalDist + 2 * dirY * v2);
        if (discriminant < 0) return null; // Недолет

        double sqrtDisc = Math.sqrt(discriminant);
        double tanTheta = (v2 - sqrtDisc) / (g * horizontalDist); // Нижняя дуга
        double pitch = Math.atan(tanTheta);
        double yaw = Math.atan2(dirZ, dirX);

        double groundSpeed = v * Math.cos(pitch);
        double vy = v * Math.sin(pitch);

        return new Vec3(groundSpeed * Math.cos(yaw), vy, groundSpeed * Math.sin(yaw));
    }

    // ========================================================================
    // 👁️ УМНЫЙ ХИТБОКС (Smart Hitbox)
    // ========================================================================

    private Vec3 getSmartTargetPos(LivingEntity target, Vec3 start) {
        if (raycastSkipTimer > 0 && cachedSmartTargetPos != null) {
            raycastSkipTimer--;
            if (target.distanceToSqr(cachedSmartTargetPos) < 4.0) return cachedSmartTargetPos;
        }

        Vec3 eyePos = target.getEyePosition();
        if (canSeePoint(start, eyePos)) {
            updateSmartCache(eyePos);
            return eyePos;
        }

        // Сканирование сетки (как было у тебя)
        AABB aabb = target.getBoundingBox();
        List<Vec3> visiblePoints = new ArrayList<>();
        int stepsY = 3;

        for (int y = stepsY; y >= 0; y--) {
            double ly = (double)y / stepsY;
            // Центр по Y
            Vec3 point = new Vec3(aabb.getCenter().x, aabb.minY + (aabb.maxY - aabb.minY) * ly, aabb.getCenter().z);
            if (canSeePoint(start, point)) {
                visiblePoints.add(point);
                if (ly > 0.7) { // Голова видна - берем
                    updateSmartCache(point);
                    return point;
                }
            }
        }

        if (visiblePoints.isEmpty()) {
            raycastSkipTimer = 0;
            return null;
        }

        Vec3 best = visiblePoints.get(0);
        updateSmartCache(best);
        return best;
    }

    private void updateSmartCache(Vec3 pos) {
        this.cachedSmartTargetPos = pos;
        this.raycastSkipTimer = RAYCAST_INTERVAL;
    }

    private boolean canSeePoint(Vec3 start, Vec3 end) {
        BlockHitResult blockHit = level.clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, turret));
        return blockHit.getType() == HitResult.Type.MISS ||
                start.distanceToSqr(blockHit.getLocation()) >= start.distanceToSqr(end) - 0.5;
    }

    public boolean canShootSafe(LivingEntity target, Vec3 muzzlePos, UUID ownerUUID) {
        Vec3 targetPos = getSmartTargetPos(target, muzzlePos); // или просто target.position() для начала
        if (targetPos == null) return false;

        Vec3 fireVec = targetPos.subtract(muzzlePos);
        double dist = fireVec.length();
        fireVec = fireVec.normalize();

        // Проверяем линию огня шагами по 1 блоку
        for (double d = 1.0; d < dist; d += 1.0) {
            Vec3 checkPos = muzzlePos.add(fireVec.scale(d));
            // Ищем сущности в радиусе 0.5 блока от траектории
            AABB safetyBox = new AABB(checkPos.subtract(0.5, 0.5, 0.5), checkPos.add(0.5, 0.5, 0.5));

            List<LivingEntity> entitiesInWay = level.getEntitiesOfClass(LivingEntity.class, safetyBox);
            for (LivingEntity ally : entitiesInWay) {
                if (ally == turret || ally == target) continue; // Игнорируем себя и цель

                // Если на линии огня союзник - НЕ СТРЛЯЕМ
                if (isAllied(ally, ownerUUID)) {
                    return false;
                }
            }
        }
        return true;
    }


    // ========================================================================
    // ⚙️ ВСПОМОГАТЕЛЬНЫЕ РАСЧЕТЫ
    // ========================================================================

    public void onShotFired(LivingEntity target, Vec3 muzzlePos) {
        this.lastShotTimeMs = System.currentTimeMillis();
        double dist = muzzlePos.distanceTo(target.position());
        long flightTimeMs = (long) ((dist / config.bulletSpeed) * 50.0);
        this.expectedImpactTimeMs = this.lastShotTimeMs + flightTimeMs;
    }

    private double calculateFlightTime(double dist) {
        double term = 1.0 - (dist * (1.0 - config.drag)) / config.bulletSpeed;
        if (term <= 0.05) return 60.0;
        return Math.log(term) / Math.log(config.drag);
    }

    private double getDragCompensationFactor(double t) {
        if (t < 0.001) return 1.0;
        double numerator = t * (1.0 - config.drag);
        double denominator = 1.0 - Math.pow(config.drag, t);
        if (denominator < 0.001) return 1.0;
        return numerator / denominator;
    }
}
