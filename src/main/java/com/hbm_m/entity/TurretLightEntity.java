package com.hbm_m.entity;

import com.hbm_m.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
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
import com.mojang.datafixers.util.Pair;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Основной класс легкой турели.
 * Реализует логику монстра (Monster), анимации (GeoEntity) и дальнего боя (RangedAttackMob).
 */
public class TurretLightEntity extends Monster implements GeoEntity, RangedAttackMob {

    // Кэш анимаций GeckoLib
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // --- СИНХРОНИЗИРУЕМЫЕ ДАННЫЕ (доступны и на сервере, и на клиенте) ---
    // Стреляет ли турель прямо сейчас (для анимации отдачи)
    private static final EntityDataAccessor<Boolean> SHOOTING = SynchedEntityData.defineId(TurretLightEntity.class, EntityDataSerializers.BOOLEAN);
    // Развернута ли турель (готова к стрельбе)
    private static final EntityDataAccessor<Boolean> DEPLOYED = SynchedEntityData.defineId(TurretLightEntity.class, EntityDataSerializers.BOOLEAN);
    // UUID владельца (игрока, поставившего турель)
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(TurretLightEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    // Таймер процесса развертывания
    private static final EntityDataAccessor<Integer> DEPLOY_TIMER = SynchedEntityData.defineId(TurretLightEntity.class, EntityDataSerializers.INT);
    // ID текущей цели (нужен клиенту для отрисовки дебаг-линий к цели)
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(TurretLightEntity.class, EntityDataSerializers.INT);

    // --- КОНСТАНТЫ БАЛАНСА ---
    private static final int DEPLOY_DURATION = 80;        // Время развертывания (в тиках)
    private static final int SHOT_ANIMATION_LENGTH = 14;  // Длительность анимации выстрела/перезарядки
    private static final double TARGET_LOCK_DISTANCE = 5.0D; // Дистанция "захвата" цели (гистерезис)
    private static final double CLOSE_COMBAT_RANGE = 5.0D;   // Дистанция ближней угрозы
    private static final float BULLET_SPEED = 3.0F;       // Скорость полета пули
    private static final float BULLET_GRAVITY = 0.01F;    // Гравитация пули (для расчетов)

    // --- ЛОКАЛЬНЫЕ ПЕРЕМЕННЫЕ ---
    private int shootAnimTimer = 0;       // Таймер проигрывания анимации выстрела
    private int shotCooldown = 0;         // Кулдаун между выстрелами
    private int lockSoundCooldown = 0;    // Чтобы звук захвата цели не спамил каждый тик
    private LivingEntity currentTargetCache = null; // Запоминаем прошлую цель
    private int currentTargetPriority = 999;        // Приоритет текущей цели (чем меньше число, тем важнее)

    // --- ПЕРЕМЕННЫЕ ДЛЯ ОТЛАДКИ (DEBUG) ---
    // Точка, куда мы целимся (с упреждением)
    private Vec3 debugTargetPoint = null;
    // Рассчитанный вектор скорости пули (для отрисовки дуги)
    private Vec3 debugBallisticVelocity = null;
    // Список точек сканирования (зеленые/красные точки на хитбоксе врага)
    private final List<Pair<Vec3, Boolean>> debugScanPoints = new ArrayList<>();

    // Геттеры для рендерера
    public Vec3 getDebugTargetPoint() { return debugTargetPoint; }
    public Vec3 getDebugBallisticVelocity() { return debugBallisticVelocity; }
    public List<Pair<Vec3, Boolean>> getDebugScanPoints() { return debugScanPoints; }

    public TurretLightEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 0; // Турель не дает опыт при убийстве
    }

    // Настройка базовых атрибутов (здоровье, скорость, дальность обзора)
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D) // Турель не ходит
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D) // Иммунитет к отбрасыванию
                .add(Attributes.FOLLOW_RANGE, 35.0D); // Дальность обнаружения врагов
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
    public int getMaxHeadYRot() { return 360; } // Голова может крутиться на 360

    @Override
    public int getMaxHeadXRot() { return 90; } // Вверх/вниз на 90 градусов

    /**
     * Вычисляет точные мировые координаты дула турели.
     * Учитывает текущий поворот головы (yaw) и наклон ствола (pitch).
     * Важно для спавна пули не внутри модели, а на конце ствола.
     */
    public Vec3 getMuzzlePos() {
        double offsetY = 10.49 / 16.0; // Высота оси вращения
        double offsetZ = 14.86 / 16.0; // Смещение ствола вперед
        float yRotRad = this.yHeadRot * ((float)Math.PI / 180F);
        float xRotRad = -this.getXRot() * ((float)Math.PI / 180F);

        // Математика поворота точки в 3D
        double yShift = Math.sin(xRotRad) * offsetZ;
        double forwardShift = Math.cos(xRotRad) * offsetZ;
        double x = this.getX() - Math.sin(yRotRad) * forwardShift;
        double y = this.getY() + offsetY + yShift;
        double z = this.getZ() + Math.cos(yRotRad) * forwardShift;
        return new Vec3(x, y, z);
    }

    /**
     * Проверяет, является ли сущность союзником.
     * 1. Другие турели того же владельца.
     * 2. Сам владелец.
     */
    @Override
    public boolean isAlliedTo(Entity entity) {
        if (super.isAlliedTo(entity)) return true;

        // Проверка: другая турель того же хозяина
        if (entity instanceof TurretLightEntity otherTurret) {
            UUID myOwner = this.getOwnerUUID();
            UUID theirOwner = otherTurret.getOwnerUUID();
            if (myOwner != null && myOwner.equals(theirOwner)) return true;
        }

        // Проверка: сам хозяин
        UUID ownerUUID = this.getOwnerUUID();
        if (ownerUUID != null && entity.getUUID().equals(ownerUUID)) return true;

        return false;
    }

    /**
     * Система приоритетов целей.
     * Возвращает число (меньше = важнее).
     * 0: Враг в упор (ближний бой).
     * 1: Тот, кто бьет хозяина.
     * 2: Тот, кто бьет другую турель.
     * 3: Тот, кто бьет эту турель.
     * ...
     */
    private int calculateTargetPriority(LivingEntity entity) {
        if (entity == null || !entity.isAlive() || this.isAlliedTo(entity)) return 999;

        double distance = this.distanceTo(entity);
        if (distance < CLOSE_COMBAT_RANGE) return 0; // Наивысший приоритет

        UUID ownerUUID = this.getOwnerUUID();
        Player owner = ownerUUID != null ? this.level().getPlayerByUUID(ownerUUID) : null;
        if (owner != null) {
            if (owner.getLastHurtByMob() == entity) return 1;
            if (entity instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() == owner) return 1;
        }

        // Защита других турелей рядом
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

    /**
     * Главный цикл обновления (вызывается 20 раз в секунду).
     */
    @Override
    public void tick() {
        super.tick();
        // Синхронизация вращения тела и головы (турель не имеет тела как такового)
        this.yBodyRot = this.getYRot();
        this.yBodyRotO = this.getYRot();

        // === ЛОГИКА СЕРВЕРА ===
        if (!this.level().isClientSide) {
            // Синхронизируем ID цели для клиента (чтобы рисовать дебаг)
            LivingEntity currentTarget = this.getTarget();
            int targetId = (currentTarget != null) ? currentTarget.getId() : -1;
            if (this.entityData.get(TARGET_ID) != targetId) {
                this.entityData.set(TARGET_ID, targetId);
            }
        }
        // === ЛОГИКА КЛИЕНТА (DEBUG) ===
        else {
            int targetId = this.entityData.get(TARGET_ID);
            if (targetId != -1) {
                Entity targetEntity = this.level().getEntity(targetId);
                if (targetEntity instanceof LivingEntity livingTarget && livingTarget.isAlive()) {
                    Vec3 muzzle = getMuzzlePos();
                    // Рассчитываем данные для отрисовки траектории и упреждения в реальном времени
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

        // Дальнейшая логика только на сервере
        if (!this.level().isClientSide) {
            // Процесс развертывания
            int currentTimer = this.entityData.get(DEPLOY_TIMER);
            if (currentTimer > 0) {
                this.entityData.set(DEPLOY_TIMER, currentTimer - 1);
                if (currentTimer - 1 == 0) this.entityData.set(DEPLOYED, true);
            }

            // Уменьшение кулдаунов
            if (this.shotCooldown > 0) this.shotCooldown--;
            if (this.lockSoundCooldown > 0) this.lockSoundCooldown--;

            // Сброс анимации стрельбы
            if (this.isShooting()) {
                shootAnimTimer++;
                if (shootAnimTimer >= SHOT_ANIMATION_LENGTH) {
                    this.setShooting(false);
                    shootAnimTimer = 0;
                }
            }

            // Управление приоритетами целей
            LivingEntity target = this.getTarget();
            if (target != null) {
                currentTargetPriority = calculateTargetPriority(target);
            } else {
                currentTargetPriority = 999;
            }

            // Звук захвата цели
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

            // ПОВОРОТ ГОЛОВЫ К ЦЕЛИ
            if (target != null && this.isDeployed()) {
                // Пытаемся смотреть на упреждение, а не просто на цель
                Vec3 lead = predictLeadPoint(target, getMuzzlePos());
                if (lead != null) {
                    this.getLookControl().setLookAt(lead.x, lead.y, lead.z, 30.0F, 30.0F);
                } else {
                    this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                }
            }

            // Периодический поиск более важных целей (каждые 10 тиков)
            if (this.tickCount % 10 == 0) {
                LivingEntity closeThreat = findClosestThreat();
                if (closeThreat != null && closeThreat != this.getTarget()) {
                    int newPriority = calculateTargetPriority(closeThreat);
                    if (newPriority < currentTargetPriority) {
                        this.setTarget(closeThreat);
                        currentTargetPriority = newPriority;
                    }
                }
            }

            // Удерживание цели ("прилипание")
            boolean canSwitchToFarTarget = true;
            if (target != null && target.isAlive()) {
                double distToCurrentTarget = this.distanceTo(target);
                if (distToCurrentTarget < TARGET_LOCK_DISTANCE) {
                    canSwitchToFarTarget = false; // Не меняем цель, если она слишком близко
                }
            }

            // Переключение на обидчика хозяина
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

            // Защита союзных турелей
            if (this.getTarget() == null || this.tickCount % 20 == 0) {
                LivingEntity allyThreat = findAllyThreat();
                if (allyThreat != null && allyThreat != this.getTarget()) {
                    int newPriority = calculateTargetPriority(allyThreat);
                    if (newPriority < currentTargetPriority) {
                        this.setTarget(allyThreat);
                        currentTargetPriority = newPriority;
                    }
                }
            }
        }
    }

    /**
     * РАСЧЕТ БАЛЛИСТИКИ (Solver).
     * Вычисляет вектор начальной скорости (vX, vY, vZ), чтобы попасть в цель
     * с учетом навесной траектории, гравитации и движения цели.
     *
     * @return Вектор скорости или null, если цель вне досягаемости.
     */
    private Vec3 calculateBallisticVelocity(LivingEntity target, Vec3 muzzlePos, float speed, float gravity) {
        // 1. Находим видимую точку на теле врага
        Vec3 targetPos = getSmartTargetPos(target);
        if (targetPos == null) return null;

        // 2. Учет движения цели (Упреждение)
        Vec3 targetVel = target.getDeltaMovement();
        // Игнорируем вертикальную скорость, если цель на земле (избегаем ошибок предсказания при прыжках)
        if (target.onGround()) targetVel = new Vec3(targetVel.x, 0, targetVel.z);

        double timeToTarget = targetPos.distanceTo(muzzlePos) / speed;
        Vec3 impactPos = targetPos.add(targetVel.scale(timeToTarget)); // Где будет цель через время полета

        double dirX = impactPos.x - muzzlePos.x;
        double dirZ = impactPos.z - muzzlePos.z;
        double dirY = impactPos.y - muzzlePos.y;

        double x = Math.sqrt(dirX * dirX + dirZ * dirZ); // Горизонтальная дистанция
        double y = dirY;                                 // Разница высот
        double v = speed;

        // Эффективная гравитация. Специально подобрана для компенсации сопротивления воздуха (drag).
        // В Minecraft пули тормозят об воздух (x0.99 каждый тик), поэтому обычная парабола тут не работает.
        double effectiveGravity = 0.01;

        double v2 = v * v;
        double v4 = v2 * v2;

        // Квадратное уравнение баллистики
        double discriminant = v4 - effectiveGravity * (effectiveGravity * x * x + 2 * y * v2);

        if (discriminant < 0) return null; // Цель слишком далеко или высоко

        // Вычисляем угол броска (берем низкую траекторию)
        double sqrtDisc = Math.sqrt(discriminant);
        double tanTheta = (v2 - sqrtDisc) / (effectiveGravity * x);
        double theta = Math.atan(tanTheta);

        // Раскладываем скорость на компоненты
        double vx = v * Math.cos(theta);
        double vy = v * Math.sin(theta);

        double yaw = Math.atan2(dirZ, dirX);

        double finalX = vx * Math.cos(yaw);
        double finalZ = vx * Math.sin(yaw);
        double finalY = vy;

        return new Vec3(finalX, finalY, finalZ);
    }

    /**
     * Предсказывает точку встречи (для LookAt и дебага), но без учета навеса.
     * Просто экстраполяция: Позиция + Скорость * Время.
     */
    private Vec3 predictLeadPoint(LivingEntity target, Vec3 muzzlePos) {
        Vec3 targetPos = getSmartTargetPos(target);
        if (targetPos == null) return null;

        Vec3 targetVel = target.getDeltaMovement();
        if (target.onGround()) targetVel = new Vec3(targetVel.x, 0, targetVel.z);

        double timeToTarget = targetPos.distanceTo(muzzlePos) / BULLET_SPEED;
        return targetPos.add(targetVel.scale(timeToTarget));
    }

    /**
     * УМНОЕ СКАНИРОВАНИЕ ХИТБОКСА.
     * Вместо того чтобы стрелять в центр (который может быть за стеной),
     * турель сканирует точки от ног до головы.
     */
    private Vec3 getSmartTargetPos(LivingEntity target) {
        Vec3 start = this.getMuzzlePos();
        AABB aabb = target.getBoundingBox();

        List<Vec3> visiblePoints = new ArrayList<>();
        // Очищаем дебаг точки (только на клиенте)
        if (this.level().isClientSide) this.debugScanPoints.clear();

        // Количество точек проверки по осям
        int stepsX = 3;
        int stepsY = 4; // Важно: сканируем 4 уровня высоты
        int stepsZ = 3;

        for (int x = 0; x <= stepsX; x++) {
            for (int y = 0; y <= stepsY; y++) {
                for (int z = 0; z <= stepsZ; z++) {
                    // Интерполяция координат внутри хитбокса
                    double lerpX = aabb.minX + (aabb.maxX - aabb.minX) * (x / (double)stepsX);
                    double lerpY = aabb.minY + (aabb.maxY - aabb.minY) * (y / (double)stepsY);
                    double lerpZ = aabb.minZ + (aabb.maxZ - aabb.minZ) * (z / (double)stepsZ);

                    Vec3 point = new Vec3(lerpX, lerpY, lerpZ);
                    // Проверяем видимость рейкастом
                    boolean visible = canSeePoint(start, point);

                    if (visible) visiblePoints.add(point);
                    if (this.level().isClientSide) this.debugScanPoints.add(Pair.of(point, visible));
                }
            }
        }

        if (visiblePoints.isEmpty()) return null;

        // Выбираем самую верхнюю видимую точку (стреляем в голову/грудь, а не в пятки)
        Vec3 bestPoint = null;
        double maxY = -99999;
        for (Vec3 p : visiblePoints) {
            if (bestPoint == null || p.y > maxY) {
                maxY = p.y;
                bestPoint = p;
            }
        }
        return bestPoint;
    }

    /**
     * Проверяет, чистая ли линия огня (нет ли союзников на пути).
     */
    private boolean isLineOfFireSafe(LivingEntity target) {
        Vec3 start = this.getMuzzlePos();
        Vec3 end = getSmartTargetPos(target);
        if (end == null) return false;

        Vec3 vec3 = end.subtract(start);
        double distance = vec3.length();
        vec3 = vec3.normalize();

        // Проверяем каждые 0.5 блока вдоль траектории
        for (double d = 0; d < distance; d += 0.5) {
            Vec3 checkPos = start.add(vec3.scale(d));
            AABB checkBox = new AABB(checkPos.subtract(0.5, 0.5, 0.5), checkPos.add(0.5, 0.5, 0.5));
            List<Entity> list = this.level().getEntities(this, checkBox);
            for (Entity e : list) {
                if (e != this && e != target && e instanceof LivingEntity living) {
                    if (this.isAlliedTo(living)) return false; // Нашли союзника на линии огня!
                }
            }
        }
        return true;
    }

    // Поиск ближайшей угрозы (для смены приоритета)
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

    // Поиск того, кто атакует союзные турели
    private LivingEntity findAllyThreat() {
        for (Entity entity : this.level().getEntities(this, this.getBoundingBox().inflate(16.0D))) {
            if (entity instanceof TurretLightEntity ally && ally != this && this.isAlliedTo(ally)) {
                LivingEntity allyAttacker = ally.getLastHurtByMob();
                if (allyAttacker != null && allyAttacker.isAlive() && !this.isAlliedTo(allyAttacker)) {
                    return allyAttacker;
                }
            }
        }
        return null;
    }

    // Вспомогательный метод для рейкаста (проверка видимости)
    private boolean canSeePoint(Vec3 start, Vec3 end) {
        BlockHitResult blockHit = this.level().clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this
        ));

        // Видим, если луч не врезался ни во что, или врезался очень близко к цели
        return blockHit.getType() == HitResult.Type.MISS ||
                start.distanceToSqr(blockHit.getLocation()) >= start.distanceToSqr(end) - 0.05;
    }

    // Можно ли безопасно стрелять? (Видим цель + линия огня чиста)
    private boolean canShootSafe(LivingEntity target) {
        return getSmartTargetPos(target) != null && isLineOfFireSafe(target);
    }

    @Override
    protected void registerGoals() {
        // Гол стрельбы
        this.goalSelector.addGoal(1, new RangedAttackGoal(this, 0.0D, SHOT_ANIMATION_LENGTH, 20.0F) {
            @Override
            public boolean canUse() { return TurretLightEntity.this.isDeployed() && super.canUse(); }

            @Override
            public boolean canContinueToUse() { return TurretLightEntity.this.isDeployed() && super.canContinueToUse(); }
        });

        // Голы выбора целей (TargetSelectors)

        // 1. Атакуем того, кто ударил хозяина
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, (entity) -> {
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

        // 2. Атакуем того, кто ударил другую турель
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, (entity) -> {
            if (this.isAlliedTo(entity)) return false;
            for (Entity e : this.level().getEntities(this, this.getBoundingBox().inflate(16.0D))) {
                if (e instanceof TurretLightEntity ally && ally != this && this.isAlliedTo(ally)) {
                    if (ally.getLastHurtByMob() == entity) return true;
                    if (entity instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() == ally) return true;
                }
            }
            return false;
        }));

        // 3. Месть (кто ударил меня)
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers(TurretLightEntity.class));

        // 4. Добивание (кого бьет хозяин)
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, (entity) -> {
            if (this.isAlliedTo(entity)) return false;
            UUID ownerUUID = this.getOwnerUUID();
            if (ownerUUID != null) {
                Player owner = this.level().getPlayerByUUID(ownerUUID);
                if (owner != null) {
                    return owner.getLastHurtMob() == entity;
                }
            }
            return false;
        }));

        // 5. Обычная агрессия на монстров
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false, (entity) -> {
            if (this.isAlliedTo(entity)) return false;
            return TurretLightEntity.this.isDeployed();
        }));

        // 6. Агрессия на игроков (не владельца)
        this.targetSelector.addGoal(6, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, (entity) -> {
            if (this.isAlliedTo(entity)) return false;
            return TurretLightEntity.this.isDeployed();
        }));
    }

    /**
     * ВЫСТРЕЛ
     * Создает пулю и задает ей рассчитанную скорость.
     */
    @Override
    public void performRangedAttack(LivingEntity target, float pullProgress) {
        if (!this.isDeployed()) return;
        if (this.shotCooldown > 0) return;

        // 1. Проверка видимости
        if (!canShootSafe(target)) return;

        // 2. Позиция дула
        Vec3 muzzlePos = getMuzzlePos();

        // 3. Расчет баллистики
        Vec3 ballisticVelocity = calculateBallisticVelocity(target, muzzlePos, BULLET_SPEED, BULLET_GRAVITY);

        if (ballisticVelocity == null) return; // Цель недосягаема

        // 4. Дебаг для клиента
        if (this.level().isClientSide) {
            this.debugBallisticVelocity = ballisticVelocity;
        }

        // 5. Подготовка к выстрелу
        this.shotCooldown = SHOT_ANIMATION_LENGTH;
        this.setShooting(true);
        this.shootAnimTimer = 0;

        TurretBulletEntity bullet = new TurretBulletEntity(this.level(), this);
        bullet.setPos(muzzlePos.x, muzzlePos.y, muzzlePos.z);

        // 6. Установка скорости
        bullet.setDeltaMovement(ballisticVelocity);

        // 7. СИНХРОНИЗАЦИЯ ПОВОРОТА
        // Считаем угол поворота на основе вектора скорости
        double hDist = Math.sqrt(ballisticVelocity.x * ballisticVelocity.x + ballisticVelocity.z * ballisticVelocity.z);

        // Mth.atan2 возвращает радианы, переводим в градусы
        float yRot = (float)(Mth.atan2(ballisticVelocity.x, ballisticVelocity.z) * (180D / Math.PI));
        float xRot = (float)(Mth.atan2(ballisticVelocity.y, hDist) * (180D / Math.PI));

        // Устанавливаем текущий поворот
        bullet.setYRot(yRot);
        bullet.setXRot(xRot);

        // КРИТИЧЕСКИ ВАЖНО: Устанавливаем "предыдущий" поворот равным текущему
        // Это гарантирует, что Lerp(tick, old, new) вернет правильный угол даже при tick=0
        bullet.yRotO = yRot;
        bullet.xRotO = xRot;

        // 8. Звук и спавн
        if (ModSounds.TURRET_FIRE.isPresent()) {
            this.playSound(ModSounds.TURRET_FIRE.get(), 1.0F, 1.0F);
        } else {
            this.playSound(SoundEvents.GENERIC_EXPLODE, 0.5F, 2.0F);
        }

        this.level().addFreshEntity(bullet);
    }

    /**
     * Контроллеры анимаций GeckoLib.
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Анимация развертывания (deploy)
        controllers.add(new AnimationController<>(this, "deploy_ctrl", 0, event -> {
            if (!this.isDeployed()) return event.setAndContinue(RawAnimation.begin().thenPlay("deploy"));
            return PlayState.STOP;
        }));

        // Анимация стрельбы (shot)
        controllers.add(new AnimationController<>(this, "shoot_ctrl", 0, event -> {
            if (this.isShooting()) {
                // Принудительный сброс, чтобы анимация проигрывалась с начала при быстром огне
                if (event.getController().getAnimationState() == AnimationController.State.STOPPED)
                    event.getController().forceAnimationReset();
                return event.setAndContinue(RawAnimation.begin().thenPlay("shot"));
            }
            return PlayState.STOP;
        }));
    }

    // Сохранение данных в NBT (при выходе из мира)
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.getOwnerUUID() != null) tag.putUUID("Owner", this.getOwnerUUID());
        tag.putBoolean("Deployed", this.isDeployed());
        tag.putInt("DeployTimer", this.entityData.get(DEPLOY_TIMER));
    }

    // Загрузка данных из NBT
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) this.entityData.set(OWNER_UUID, Optional.of(tag.getUUID("Owner")));
        if (tag.contains("Deployed")) this.entityData.set(DEPLOYED, tag.getBoolean("Deployed"));
        if (tag.contains("DeployTimer")) this.entityData.set(DEPLOY_TIMER, tag.getInt("DeployTimer"));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }

    public void setOwner(Player player) { this.entityData.set(OWNER_UUID, Optional.of(player.getUUID())); }

    public UUID getOwnerUUID() { return this.entityData.get(OWNER_UUID).orElse(null); }

    public void setShooting(boolean shooting) { this.entityData.set(SHOOTING, shooting); }

    public boolean isShooting() { return this.entityData.get(SHOOTING); }

    public boolean isDeployed() { return this.entityData.get(DEPLOYED); }

    @Override
    public boolean isPushable() { return false; } // Турель нельзя толкать
    @Override
    public double getBoneResetTime() {
        return 0; // Отключает плавный возврат к дефолтной позе (в тиках)
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockIn) {} // У турели нет звука шагов
}
