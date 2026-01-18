package com.smogline.entity.weapons.turrets;

import com.mojang.datafixers.util.Pair;
import com.smogline.entity.weapons.bullets.TurretBulletEntity;
import com.smogline.item.tags_and_tiers.AmmoRegistry;
import com.smogline.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TurretLightEntity extends Monster implements GeoEntity, RangedAttackMob {

    // GeckoLib
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Synched data
    private static final EntityDataAccessor<Boolean> SHOOTING =
            SynchedEntityData.defineId(TurretLightEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DEPLOYED =
            SynchedEntityData.defineId(TurretLightEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(TurretLightEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DEPLOY_TIMER =
            SynchedEntityData.defineId(TurretLightEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_ID =
            SynchedEntityData.defineId(TurretLightEntity.class, EntityDataSerializers.INT);

    // Balance
    private static final int DEPLOY_DURATION = 80;
    private static final int SHOT_ANIMATION_LENGTH = 14;
    private static final double TARGET_LOCK_DISTANCE = 5.0D;
    private static final double CLOSE_COMBAT_RANGE = 5.0D;

    // Ballistics Constants
    private static final float BULLET_SPEED = 3.0F;
    private static final float BULLET_GRAVITY = 0.01F;
    private static final double DRAG = 0.99; // Сопротивление воздуха (Standard MC Drag)

    // === WAR THUNDER STYLE TRACKING ===
    private Vec3 lastTargetPos = Vec3.ZERO;       // Позиция цели в прошлом тике
    private Vec3 avgTargetVelocity = Vec3.ZERO;   // Сглаженная скорость (Smoothed Velocity)
    private Vec3 targetAcceleration = Vec3.ZERO;  // Ускорение цели
    private int trackingTicks = 0;                // Время удержания цели

    // Anti-Recoil Filter (сколько мс игнорировать Y после выстрела)
    private static final long Y_IMPULSE_FILTER_MS = 80L;
    private long lastShotTimeMs = 0L;

    // Aim tuning
    private static final double AIM_Y_BIAS = 0.05D;

    // Local state
    private int shootAnimTimer = 0;
    private int shotCooldown = 0;
    private int lockSoundCooldown = 0;
    private LivingEntity currentTargetCache = null;
    private int currentTargetPriority = 999;

    // Debug
    private Vec3 debugTargetPoint = null;
    private Vec3 debugBallisticVelocity = null;
    private final List<Pair<Vec3, Boolean>> debugScanPoints = new ArrayList<>();

    public Vec3 getDebugTargetPoint() { return debugTargetPoint; }
    public Vec3 getDebugBallisticVelocity() { return debugBallisticVelocity; }
    public List<Pair<Vec3, Boolean>> getDebugScanPoints() { return debugScanPoints; }

    public TurretLightEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 35.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SHOOTING, false);
        this.entityData.define(DEPLOYED, false);
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(DEPLOY_TIMER, DEPLOY_DURATION);
        this.entityData.define(TARGET_ID, -1);
    }

    @Override
    public int getMaxHeadYRot() { return 360; }
    @Override
    public int getMaxHeadXRot() { return 90; }

    public Vec3 getMuzzlePos() {
        double offsetY = 10.49 / 16.0;
        double offsetZ = 14.86 / 16.0;

        float yRotRad = this.yHeadRot * ((float) Math.PI / 180F);
        float xRotRad = -this.getXRot() * ((float) Math.PI / 180F);

        double yShift = Math.sin(xRotRad) * offsetZ;
        double forwardShift = Math.cos(xRotRad) * offsetZ;

        double x = this.getX() - Math.sin(yRotRad) * forwardShift;
        double y = this.getY() + offsetY + yShift;
        double z = this.getZ() + Math.cos(yRotRad) * forwardShift;

        return new Vec3(x, y, z);
    }

    // -------------------- Allies & Priority --------------------

    @Override
    public boolean isAlliedTo(Entity entity) {
        if (super.isAlliedTo(entity)) return true;
        if (entity instanceof TurretLightEntity otherTurret) {
            UUID myOwner = this.getOwnerUUID();
            UUID theirOwner = otherTurret.getOwnerUUID();
            if (myOwner != null && myOwner.equals(theirOwner)) return true;
        }
        UUID ownerUUID = this.getOwnerUUID();
        return ownerUUID != null && entity.getUUID().equals(ownerUUID);
    }

    private int calculateTargetPriority(LivingEntity entity) {
        if (entity == null || !entity.isAlive() || this.isAlliedTo(entity)) return 999;

        double distance = this.distanceTo(entity);
        if (distance < CLOSE_COMBAT_RANGE) return 0;

        UUID ownerUUID = this.getOwnerUUID();
        Player owner = ownerUUID != null ? this.level().getPlayerByUUID(ownerUUID) : null;

        if (owner != null) {
            if (owner.getLastHurtByMob() == entity) return 1;
            if (entity instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() == owner) return 1;
        }

        for (Entity e : this.level().getEntities(this, this.getBoundingBox().inflate(16.0D))) {
            if (e instanceof TurretLightEntity ally && ally != this && this.isAlliedTo(ally)) {
                if (ally.getLastHurtByMob() == entity) return 2;
                if (entity instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() == ally) return 2;
            }
        }

        if (this.getLastHurtByMob() == entity) return 3;
        if (owner != null && owner.getLastHurtMob() == entity) return 4;
        if (entity instanceof Monster) return 5;
        if (entity instanceof Player) return 6;

        return 999;
    }

    // -------------------- TICK (Tracking Logic) --------------------

    @Override
    public void tick() {
        super.tick();

        this.yBodyRot = this.getYRot();
        this.yBodyRotO = this.getYRot();

        if (!this.level().isClientSide) {
            LivingEntity target = this.getTarget();

            // === WAR THUNDER TRACKER UPDATE ===
            if (target != null && target.isAlive()) {
                Vec3 currentPos = target.position();

                if (trackingTicks > 0) {
                    // Мгновенная скорость за тик
                    Vec3 instantaneousVel = currentPos.subtract(lastTargetPos);

                    // Сглаживание скорости (80% старой, 20% новой)
                    // Это убирает "дёрганье" прицела
                    this.avgTargetVelocity = this.avgTargetVelocity.lerp(instantaneousVel, 0.2);

                    // Расчет ускорения (изменение скорости)
                    Vec3 newAccel = instantaneousVel.subtract(this.avgTargetVelocity);
                    // Ускорение сглаживаем сильнее (90% старого, 10% нового), чтобы не реагировать на микро-рывки
                    this.targetAcceleration = this.targetAcceleration.lerp(newAccel, 0.1);
                } else {
                    // Первый тик захвата
                    this.avgTargetVelocity = target.getDeltaMovement();
                    this.targetAcceleration = Vec3.ZERO;
                }

                this.lastTargetPos = currentPos;
                this.trackingTicks++;
            } else {
                // Цели нет - сбрасываем трекер
                this.trackingTicks = 0;
                this.avgTargetVelocity = Vec3.ZERO;
                this.targetAcceleration = Vec3.ZERO;
                this.lastTargetPos = Vec3.ZERO;
            }
            // ==================================

            int targetId = target != null ? target.getId() : -1;
            if (this.entityData.get(TARGET_ID) != targetId) {
                this.entityData.set(TARGET_ID, targetId);
            }
        } else {
            // Client debug logic
            int targetId = this.entityData.get(TARGET_ID);
            if (targetId != -1) {
                Entity targetEntity = this.level().getEntity(targetId);
                if (targetEntity instanceof LivingEntity livingTarget && livingTarget.isAlive()) {
                    Vec3 muzzle = getMuzzlePos();
                    this.debugBallisticVelocity = calculateBallisticVelocity(livingTarget, muzzle, BULLET_SPEED, BULLET_GRAVITY);
                    this.debugTargetPoint = predictLeadPoint(livingTarget, muzzle);
                } else {
                    this.debugTargetPoint = null;
                    this.debugBallisticVelocity = null;
                }
            } else {
                this.debugTargetPoint = null;
                this.debugBallisticVelocity = null;
            }
        }

        if (!this.level().isClientSide) {
            int currentTimer = this.entityData.get(DEPLOY_TIMER);
            if (currentTimer > 0) {
                this.entityData.set(DEPLOY_TIMER, currentTimer - 1);
                if (currentTimer - 1 == 0) this.entityData.set(DEPLOYED, true);
            }

            if (this.shotCooldown > 0) this.shotCooldown--;
            if (this.lockSoundCooldown > 0) this.lockSoundCooldown--;

            if (this.isShooting()) {
                shootAnimTimer++;
                if (shootAnimTimer >= SHOT_ANIMATION_LENGTH) {
                    this.setShooting(false);
                    shootAnimTimer = 0;
                }
            }

            LivingEntity target = this.getTarget();
            currentTargetPriority = (target != null) ? calculateTargetPriority(target) : 999;

            if (target != null && target != currentTargetCache && this.isDeployed()) {
                if (this.lockSoundCooldown == 0) {
                    if (ModSounds.TURRET_LOCK.isPresent()) {
                        this.playSound(ModSounds.TURRET_LOCK.get(), 1.0F, 1.0F);
                    }
                    this.lockSoundCooldown = 40;
                }
                currentTargetCache = target;
            } else if (target == null) {
                currentTargetCache = null;
            }

            if (target != null && this.isDeployed()) {
                Vec3 lead = predictLeadPoint(target, getMuzzlePos());
                if (lead != null) {
                    this.getLookControl().setLookAt(lead.x, lead.y, lead.z, 30.0F, 30.0F);
                } else {
                    this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                }
            }

            if (this.tickCount % 10 == 0) {
                LivingEntity closeThreat = findClosestThreat();
                if (closeThreat != null && closeThreat != this.getTarget()) {
                    int newPriority = calculateTargetPriority(closeThreat);
                    if (newPriority < currentTargetPriority) {
                        this.setTarget(closeThreat);
                        currentTargetPriority = newPriority;
                    }
                }

                boolean canSwitchToFarTarget = true;
                if (target != null && target.isAlive()) {
                    if (this.distanceTo(target) < TARGET_LOCK_DISTANCE) canSwitchToFarTarget = false;
                }

                if (canSwitchToFarTarget) {
                    UUID ownerUUID = this.getOwnerUUID();
                    if (ownerUUID != null) {
                        Player owner = this.level().getPlayerByUUID(ownerUUID);
                        if (owner != null) {
                            LivingEntity ownerAttacker = owner.getLastHurtByMob();
                            if (ownerAttacker != null && ownerAttacker != this.getTarget()
                                    && ownerAttacker.isAlive() && !isAlliedTo(ownerAttacker)) {
                                int newPriority = calculateTargetPriority(ownerAttacker);
                                if (newPriority < currentTargetPriority) {
                                    this.setTarget(ownerAttacker);
                                    currentTargetPriority = newPriority;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // -------------------- BALLISTICS (WT Style) --------------------

    /**
     * WAR THUNDER STYLE CALCULATOR
     * Использует сглаженную скорость и ускорение + итеративный подбор времени полета.
     * + Raycast Clamp (не стреляет сквозь стены при упреждении)
     */
    private Vec3 calculateBallisticVelocity(LivingEntity target, Vec3 muzzlePos, float speed, float gravity) {
        Vec3 currentVisiblePos = getSmartTargetPos(target);
        if (currentVisiblePos == null) return null;

        // --- 1. Сбор данных о движении (как и было) ---
        Vec3 targetVel;
        Vec3 targetAcc;
        if (trackingTicks > 5) {
            targetVel = this.avgTargetVelocity;
            long now = System.currentTimeMillis();
            boolean isRecoilState = (now - lastShotTimeMs < Y_IMPULSE_FILTER_MS) || (target.hurtTime > 0);
            if (target.onGround() || isRecoilState) {
                targetVel = new Vec3(targetVel.x, 0, targetVel.z);
                targetAcc = new Vec3(this.targetAcceleration.x, 0, this.targetAcceleration.z);
            } else {
                targetAcc = this.targetAcceleration;
            }
        } else {
            targetVel = target.getDeltaMovement();
            if (target.onGround()) targetVel = new Vec3(targetVel.x, 0, targetVel.z);
            targetAcc = Vec3.ZERO;
        }

        double dist = currentVisiblePos.distanceTo(muzzlePos);
        double t = calculateFlightTime(dist, speed);

        // --- 2. Предсказание позиции (как и было) ---
        Vec3 predictedPos = currentVisiblePos;
        for (int i = 0; i < 5; i++) {
            Vec3 velocityPart = targetVel.scale(t);
            Vec3 accelPart = targetAcc.scale(0.5 * t * t);
            predictedPos = currentVisiblePos.add(velocityPart).add(accelPart);

            double newDist = predictedPos.distanceTo(muzzlePos);
            double newT = calculateFlightTime(newDist, speed);
            if (Math.abs(newT - t) < 0.01) { t = newT; break; }
            t = newT;
        }

        // --- 3. [FIX] CLAMP TO VISIBILITY (Моя добавка) ---
        // Если упреждение ушло за стену, возвращаем прицел на край препятствия
        if (!canSeePoint(muzzlePos, predictedPos)) {
            predictedPos = currentVisiblePos;
            // Пересчитываем время для статической цели, чтобы баллистика не перекинула
            double staticDist = predictedPos.distanceTo(muzzlePos);
            t = calculateFlightTime(staticDist, speed);
        }

        this.debugTargetPoint = predictedPos;

        // --- 4. [RESTORED] Твоя оригинальная математика ---
        // Расчет баллистики через дискриминант

        // Убедись, что метод getDragCompensationFactor у тебя остался в классе!
        double dragFactor = getDragCompensationFactor(t);

        double dirX = predictedPos.x - muzzlePos.x;
        double dirZ = predictedPos.z - muzzlePos.z;
        double dirY = predictedPos.y - muzzlePos.y;

        // Учитываем драг-фактор, как было у тебя
        double horizontalDist = Math.sqrt(dirX * dirX + dirZ * dirZ) * dragFactor;

        double v = speed;
        double v2 = v * v;
        double v4 = v2 * v2;
        double g = gravity * dragFactor;

        // Классическое решение уравнения траектории
        double discriminant = v4 - g * (g * horizontalDist * horizontalDist + 2 * dirY * v2);
        if (discriminant < 0) return null; // Недолет

        double sqrtDisc = Math.sqrt(discriminant);
        // Выбираем нижнюю дугу (минус sqrtDisc)
        double tanTheta = (v2 - sqrtDisc) / (g * horizontalDist);
        double pitch = Math.atan(tanTheta);

        double yaw = Math.atan2(dirZ, dirX);

        double groundSpeed = v * Math.cos(pitch);
        double vy = v * Math.sin(pitch);

        return new Vec3(groundSpeed * Math.cos(yaw), vy, groundSpeed * Math.sin(yaw));
    }

    /**
     * Помощник для предсказания поворота башни.
     * Использует ту же логику V*t, чтобы башня смотрела в точку упреждения.
     */
    private Vec3 predictLeadPoint(LivingEntity target, Vec3 muzzlePos) {
        Vec3 targetPos = getSmartTargetPos(target);
        if (targetPos == null) return null;

        Vec3 targetVel = (trackingTicks > 5) ? this.avgTargetVelocity : target.getDeltaMovement();
        if (target.onGround()) targetVel = new Vec3(targetVel.x, 0, targetVel.z);

        double dist = targetPos.distanceTo(muzzlePos);
        double t = calculateFlightTime(dist, BULLET_SPEED);

        return targetPos.add(targetVel.scale(t));
    }

    /**
     * Физика полета с сопротивлением воздуха.
     */
    private double calculateFlightTime(double dist, double speed) {
        // t = log(1 - dist * (1-drag)/speed) / log(drag)
        double term = 1.0 - (dist * (1.0 - DRAG) / speed);
        if (term <= 0.05) return 60.0;
        return Math.log(term) / Math.log(DRAG);
    }

    private double getDragCompensationFactor(double t) {
        if (t <= 0.001) return 1.0;
        double numerator = t * (1.0 - DRAG);
        double denominator = 1.0 - Math.pow(DRAG, t);
        if (denominator < 0.001) return 1.0;
        return numerator / denominator;
    }

    // -------------------- Smart Hitbox --------------------

    private Vec3 getSmartTargetPos(LivingEntity target) {
        Vec3 start = this.getMuzzlePos();
        AABB aabb = target.getBoundingBox();

        if (this.level().isClientSide) this.debugScanPoints.clear();

        List<Vec3> visiblePoints = new ArrayList<>();

        // 1. Оптимизированная сетка (Внешний контур)
        int stepsX = 2;
        int stepsY = 4;
        int stepsZ = 2;

        for (int y = stepsY; y >= 0; y--) {
            for (int x = 0; x <= stepsX; x++) {
                for (int z = 0; z <= stepsZ; z++) {
                    // Проверяем только внешний контур
                    boolean isOuterX = (x == 0 || x == stepsX);
                    boolean isOuterY = (y == 0 || y == stepsY);
                    boolean isOuterZ = (z == 0 || z == stepsZ);

                    if (!isOuterX && !isOuterZ && !isOuterY) continue;

                    double lx = (double)x / stepsX;
                    double ly = (double)y / stepsY;
                    double lz = (double)z / stepsZ;

                    double px = aabb.minX + (aabb.maxX - aabb.minX) * lx;
                    double py = aabb.minY + (aabb.maxY - aabb.minY) * ly;
                    double pz = aabb.minZ + (aabb.maxZ - aabb.minZ) * lz;

                    Vec3 point = new Vec3(px, py, pz);

                    if (canSeePoint(start, point)) {
                        visiblePoints.add(point);
                        if (this.level().isClientSide) this.debugScanPoints.add(Pair.of(point, true));
                    } else {
                        if (this.level().isClientSide) this.debugScanPoints.add(Pair.of(point, false));
                    }
                }
            }
        }

        // 2. [НОВОЕ] Резервный скан центральной оси (Центральный "позвоночник")
        // Если внешние углы закрыты (цель в узкой щели), проверяем центр.
        if (visiblePoints.isEmpty()) {
            double cx = aabb.minX + (aabb.maxX - aabb.minX) * 0.5;
            double cz = aabb.minZ + (aabb.maxZ - aabb.minZ) * 0.5;
            int centerSteps = 5;

            for (int i = 0; i <= centerSteps; i++) {
                double ly = (double)i / (double)centerSteps;
                double py = aabb.minY + (aabb.maxY - aabb.minY) * ly;
                Vec3 point = new Vec3(cx, py, cz);

                if (canSeePoint(start, point)) {
                    visiblePoints.add(point);
                    if (this.level().isClientSide) this.debugScanPoints.add(Pair.of(point, true));
                } else {
                    if (this.level().isClientSide) this.debugScanPoints.add(Pair.of(point, false));
                }
            }
        }

        if (visiblePoints.isEmpty()) return null;

        // ЭВРИСТИКА: Сортируем (сначала высокие точки, чтобы стрелять в голову/тело, а не ноги)
        visiblePoints.sort((p1, p2) -> Double.compare(p2.y, p1.y));

        Vec3 bestPoint = visiblePoints.get(0);

        // [НОВОЕ] Безопасное смещение (Safe Bias)
        // Пытаемся поднять прицел чуть выше (AIM_Y_BIAS), но ТОЛЬКО если эта точка тоже видна.
        // Иначе стреляем в "честную" видимую точку (например, в пятку).
        Vec3 biasedPoint = bestPoint.add(0.0D, AIM_Y_BIAS, 0.0D);
        if (canSeePoint(start, biasedPoint)) {
            return biasedPoint;
        }

        return bestPoint;
    }

    private boolean canSeePoint(Vec3 start, Vec3 end) {
        // Проверяем коллизию блоков между start и end
        BlockHitResult blockHit = this.level().clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this
        ));

        // MISS означает чисто.
        // Либо дистанция до удара больше или равна дистанции до цели (минус погрешность).
        return blockHit.getType() == HitResult.Type.MISS ||
                start.distanceToSqr(blockHit.getLocation()) >= start.distanceToSqr(end) - 0.05;
    }


    private boolean isLineOfFireSafe(LivingEntity target) {
        Vec3 start = this.getMuzzlePos();
        Vec3 end = getSmartTargetPos(target);
        if (end == null) return false;

        Vec3 vec3 = end.subtract(start);
        double distance = vec3.length();
        vec3 = vec3.normalize();

        for (double d = 0; d < distance; d += 0.5) {
            Vec3 checkPos = start.add(vec3.scale(d));
            AABB checkBox = new AABB(
                    checkPos.subtract(0.5, 0.5, 0.5),
                    checkPos.add(0.5, 0.5, 0.5)
            );

            List<Entity> list = this.level().getEntities(this, checkBox);
            for (Entity e : list) {
                if (e != this && e != target && e instanceof LivingEntity living) {
                    if (this.isAlliedTo(living)) return false;
                }
            }
        }
        return true;
    }

    private boolean canShootSafe(LivingEntity target) {
        return getSmartTargetPos(target) != null && isLineOfFireSafe(target);
    }

    private LivingEntity findClosestThreat() {
        LivingEntity closest = null;
        double closestDist = CLOSE_COMBAT_RANGE;

        for (Entity entity : this.level().getEntities(this, this.getBoundingBox().inflate(CLOSE_COMBAT_RANGE))) {
            if (entity instanceof LivingEntity living && !this.isAlliedTo(living) && living.isAlive()) {
                double dist = this.distanceTo(living);
                if (dist < closestDist) {
                    closest = living;
                    closestDist = dist;
                }
            }
        }
        return closest;
    }

    // -------------------- Fire! --------------------

    @Override
    public void performRangedAttack(LivingEntity target, float pullProgress) {
        if (!this.isDeployed()) return;
        if (this.shotCooldown > 0) return;
        if (!canShootSafe(target)) return;

        this.lastShotTimeMs = System.currentTimeMillis();

        Vec3 muzzlePos = getMuzzlePos();
        Vec3 ballisticVelocity = calculateBallisticVelocity(target, muzzlePos, BULLET_SPEED, BULLET_GRAVITY);
        if (ballisticVelocity == null) return;

        this.shotCooldown = SHOT_ANIMATION_LENGTH;
        this.setShooting(true);
        this.shootAnimTimer = 0;

        if (!this.level().isClientSide) {
            ServerLevel serverLevel = (ServerLevel) this.level();
            TurretBulletEntity bullet = new TurretBulletEntity(serverLevel, this);

            AmmoRegistry.AmmoType randomAmmo = AmmoRegistry.getRandomAmmoForCaliber("20mm_turret", this.level().random);
            if (randomAmmo != null) {
                bullet.setAmmoType(randomAmmo);
            } else {
                bullet.setAmmoType(new AmmoRegistry.AmmoType("default", "20mm_turret", 6.0f, 3.0f, false));
            }

            bullet.setPos(muzzlePos.x, muzzlePos.y, muzzlePos.z);
            bullet.setDeltaMovement(ballisticVelocity);

            // SYNC ROTATION
            double horizontalDist = Math.sqrt(ballisticVelocity.x * ballisticVelocity.x + ballisticVelocity.z * ballisticVelocity.z);
            float yaw = (float) (Math.atan2(ballisticVelocity.z, ballisticVelocity.x) * (180D / Math.PI)) - 90.0F;
            float pitch = (float) (Math.atan2(ballisticVelocity.y, horizontalDist) * (180D / Math.PI));

            bullet.setYRot(yaw);
            bullet.setXRot(pitch);
            bullet.yRotO = yaw;
            bullet.xRotO = pitch;

            serverLevel.addFreshEntity(bullet);

            if (ModSounds.TURRET_FIRE.isPresent()) {
                this.playSound(ModSounds.TURRET_FIRE.get(), 1.0F, 1.0F);
            }
        }
    }

    // -------------------- GeckoLib & NBT --------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "deploy_ctrl", 0, event -> {
            if (!this.isDeployed()) return event.setAndContinue(RawAnimation.begin().thenPlay("deploy"));
            return PlayState.STOP;
        }));

        controllers.add(new AnimationController<>(this, "shoot_ctrl", 0, event -> {
            if (this.isShooting()) {
                if (event.getController().getAnimationState() == AnimationController.State.STOPPED) {
                    event.getController().forceAnimationReset();
                }
                return event.setAndContinue(RawAnimation.begin().thenPlay("shot"));
            }
            return PlayState.STOP;
        }));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.getOwnerUUID() != null) tag.putUUID("Owner", this.getOwnerUUID());
        tag.putBoolean("Deployed", this.isDeployed());
        tag.putInt("DeployTimer", this.entityData.get(DEPLOY_TIMER));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) this.entityData.set(OWNER_UUID, Optional.of(tag.getUUID("Owner")));
        if (tag.contains("Deployed")) this.entityData.set(DEPLOYED, tag.getBoolean("Deployed"));
        if (tag.contains("DeployTimer")) this.entityData.set(DEPLOY_TIMER, tag.getInt("DeployTimer"));
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new RangedAttackGoal(this, 0.0D, SHOT_ANIMATION_LENGTH, 20.0F) {
            @Override
            public boolean canUse() { return TurretLightEntity.this.isDeployed() && super.canUse(); }

            @Override
            public boolean canContinueToUse() { return TurretLightEntity.this.isDeployed() && super.canContinueToUse(); }
        });

        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, entity -> {
            if (this.isAlliedTo(entity)) return false;
            UUID ownerUUID = this.getOwnerUUID();
            if (ownerUUID != null) {
                Player owner = this.level().getPlayerByUUID(ownerUUID);
                if (owner != null) {
                    if (owner.getLastHurtByMob() == entity) return true;
                    if (entity instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() == owner) return true;
                }
            }
            return false;
        }));

        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, entity -> {
            if (this.isAlliedTo(entity)) return false;
            for (Entity e : this.level().getEntities(this, this.getBoundingBox().inflate(16.0D))) {
                if (e instanceof TurretLightEntity ally && ally != this && this.isAlliedTo(ally)) {
                    if (ally.getLastHurtByMob() == entity) return true;
                    if (entity instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() == ally) return true;
                }
            }
            return false;
        }));

        this.targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers(TurretLightEntity.class));

        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, entity -> {
            if (this.isAlliedTo(entity)) return false;
            UUID ownerUUID = this.getOwnerUUID();
            if (ownerUUID != null) {
                Player owner = this.level().getPlayerByUUID(ownerUUID);
                if (owner != null) return owner.getLastHurtMob() == entity;
            }
            return false;
        }));

        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                entity -> !this.isAlliedTo(entity) && TurretLightEntity.this.isDeployed()));

        this.targetSelector.addGoal(6, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                entity -> !this.isAlliedTo(entity) && TurretLightEntity.this.isDeployed()));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }

    public void setOwner(Player player) { this.entityData.set(OWNER_UUID, Optional.of(player.getUUID())); }
    public UUID getOwnerUUID() { return this.entityData.get(OWNER_UUID).orElse(null); }
    public void setShooting(boolean shooting) { this.entityData.set(SHOOTING, shooting); }
    public boolean isShooting() { return this.entityData.get(SHOOTING); }
    public boolean isDeployed() { return this.entityData.get(DEPLOYED); }
    @Override public boolean isPushable() { return false; }
    @Override public double getBoneResetTime() { return 0; }
    @Override protected void playStepSound(BlockPos pos, BlockState blockIn) {}
}
