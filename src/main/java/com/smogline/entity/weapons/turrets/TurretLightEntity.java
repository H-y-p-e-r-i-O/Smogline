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

    // Ballistics
    private static final float BULLET_SPEED = 3.0F;
    private static final float BULLET_GRAVITY = 0.01F;
    private static final float AIR_RESISTANCE = 0.99F; // на всякий (если будешь усложнять solver)

    // Adaptive lead (по размеру цели)
    private static final double MIN_ENTITY_SIZE = 0.5D;
    private static final double MAX_ENTITY_SIZE = 5.0D;
    private static final double MIN_PREDICTION_LEAD = 0.35D;
    private static final double MAX_PREDICTION_LEAD = 1.65D;
    private double adaptiveLeadTime = 1.0D;

    // Anti “Y impulse” filter (главный фикс под твой кейс)
    // Сколько миллисекунд после нашего выстрела игнорировать Y-скорость цели
    private static final long Y_IMPULSE_FILTER_MS = 220L;
    private long lastShotTimeMs = 0L;

    // Aim tuning
    private static final double AIM_Y_BIAS = 0.05D; // если “в землю” — увеличь; если “в воздух” — уменьши

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
    public double getAdaptiveLeadTime() { return adaptiveLeadTime; }

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

    // -------------------- Key fix: filter target velocity --------------------

    /**
     * Возвращает скорость цели для упреждения.
     * Главная идея: когда цель получила импульс по Y от попадания (подброс/падение),
     * это НЕ “намеренное уклонение”, поэтому Y игнорируем краткое время после нашего выстрела.
     */
    private Vec3 getFilteredTargetVelocity(LivingEntity target) {
        Vec3 v = target.getDeltaMovement();

        // Ванильные прыжки/ступеньки тоже часто ломают упреждение — на земле Y лучше игнорировать всегда.
        if (target.onGround()) {
            return new Vec3(v.x, 0.0D, v.z);
        }

        long now = System.currentTimeMillis();
        if (now - lastShotTimeMs < Y_IMPULSE_FILTER_MS) {
            return new Vec3(v.x, 0.0D, v.z);
        }

        // Летающие/прыгающие мобы (без “нашего” импульса) — можно учитывать Y.
        return v;
    }

    private void updateAdaptiveLeadTime(LivingEntity target) {
        // Если сейчас “окно импульса” — не трогаем leadTime (иначе турель начнет “дергаться”)
        long now = System.currentTimeMillis();
        if (now - lastShotTimeMs < Y_IMPULSE_FILTER_MS) return;

        AABB bb = target.getBoundingBox();
        double w = bb.maxX - bb.minX;
        double h = bb.maxY - bb.minY;
        double size = Math.max(w, h);

        double t = (size - MIN_ENTITY_SIZE) / (MAX_ENTITY_SIZE - MIN_ENTITY_SIZE);
        t = Math.max(0.0D, Math.min(1.0D, t));

        this.adaptiveLeadTime = MIN_PREDICTION_LEAD + t * (MAX_PREDICTION_LEAD - MIN_PREDICTION_LEAD);
    }

    // -------------------- Allies --------------------

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

    // -------------------- Tick --------------------

    @Override
    public void tick() {
        super.tick();

        this.yBodyRot = this.getYRot();
        this.yBodyRotO = this.getYRot();

        // Server: sync target id
        if (!this.level().isClientSide) {
            LivingEntity currentTarget = this.getTarget();
            int targetId = currentTarget != null ? currentTarget.getId() : -1;
            if (this.entityData.get(TARGET_ID) != targetId) {
                this.entityData.set(TARGET_ID, targetId);
            }
        } else {
            // Client debug
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

        // Server-only logic
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

            // Lock sound
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

            // Look control
            if (target != null && this.isDeployed()) {
                updateAdaptiveLeadTime(target);

                Vec3 lead = predictLeadPoint(target, getMuzzlePos());
                if (lead != null) {
                    this.getLookControl().setLookAt(lead.x, lead.y, lead.z, 30.0F, 30.0F);
                } else {
                    this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                }
            }

            // Re-targeting each 10 ticks
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

    // -------------------- Ballistics + lead --------------------

    private Vec3 calculateBallisticVelocity(LivingEntity target, Vec3 muzzlePos, float speed, float gravity) {
        Vec3 targetPos = getSmartTargetPos(target);
        if (targetPos == null) return null;

        Vec3 targetVel = getFilteredTargetVelocity(target);

        double timeToTarget = targetPos.distanceTo(muzzlePos) / speed;
        Vec3 impactPos = targetPos.add(targetVel.scale(timeToTarget * this.adaptiveLeadTime));

        double dirX = impactPos.x - muzzlePos.x;
        double dirZ = impactPos.z - muzzlePos.z;
        double dirY = impactPos.y - muzzlePos.y;

        double x = Math.sqrt(dirX * dirX + dirZ * dirZ);
        double y = dirY;

        double v = speed;
        double v2 = v * v;
        double v4 = v2 * v2;

        // Упрощенная “эффективная гравитация” (можно тюнить под твою пулю/drag)
        double effectiveGravity = gravity;

        double discriminant = v4 - effectiveGravity * (effectiveGravity * x * x + 2 * y * v2);
        if (discriminant < 0) return null;

        double sqrtDisc = Math.sqrt(discriminant);
        double tanTheta = (v2 - sqrtDisc) / (effectiveGravity * x);
        double theta = Math.atan(tanTheta);

        double vx = v * Math.cos(theta);
        double vy = v * Math.sin(theta);

        double yaw = Math.atan2(dirZ, dirX);

        double finalX = vx * Math.cos(yaw);
        double finalZ = vx * Math.sin(yaw);
        double finalY = vy;

        return new Vec3(finalX, finalY, finalZ);
    }

    private Vec3 predictLeadPoint(LivingEntity target, Vec3 muzzlePos) {
        Vec3 targetPos = getSmartTargetPos(target);
        if (targetPos == null) return null;

        Vec3 targetVel = getFilteredTargetVelocity(target);
        double timeToTarget = targetPos.distanceTo(muzzlePos) / BULLET_SPEED;

        return targetPos.add(targetVel.scale(timeToTarget * this.adaptiveLeadTime));
    }

    // -------------------- Smart hitbox scan --------------------

    private Vec3 getSmartTargetPos(LivingEntity target) {
        Vec3 start = this.getMuzzlePos();
        AABB aabb = target.getBoundingBox();
        List<Vec3> visiblePoints = new ArrayList<>();

        if (this.level().isClientSide) this.debugScanPoints.clear();

        // фикс “точки на границах”: используем центры ячеек (x+0.5)/steps
        int stepsX = 4;
        int stepsY = 6;
        int stepsZ = 4;

        for (int x = 0; x < stepsX; x++) {
            for (int y = 0; y < stepsY; y++) {
                for (int z = 0; z < stepsZ; z++) {
                    double lx = (x + 0.5D) / stepsX;
                    double ly = (y + 0.5D) / stepsY;
                    double lz = (z + 0.5D) / stepsZ;

                    double px = aabb.minX + (aabb.maxX - aabb.minX) * lx;
                    double py = aabb.minY + (aabb.maxY - aabb.minY) * ly;
                    double pz = aabb.minZ + (aabb.maxZ - aabb.minZ) * lz;

                    Vec3 point = new Vec3(px, py, pz);
                    boolean visible = canSeePoint(start, point);

                    if (visible) visiblePoints.add(point);
                    if (this.level().isClientSide) this.debugScanPoints.add(Pair.of(point, visible));
                }
            }
        }

        if (visiblePoints.isEmpty()) return null;

        // Стабильный вариант: “верхняя видимая” (часто лучше против укрытий/ступеней)
        Vec3 bestPoint = null;
        double bestY = -1e9;
        for (Vec3 p : visiblePoints) {
            if (bestPoint == null || p.y > bestY) {
                bestPoint = p;
                bestY = p.y;
            }
        }

        // маленький bias вверх (чтобы не “в землю” при краях AABB)
        return bestPoint.add(0.0D, AIM_Y_BIAS, 0.0D);
    }

    private boolean canSeePoint(Vec3 start, Vec3 end) {
        BlockHitResult blockHit = this.level().clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this
        ));

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

    // -------------------- Attack --------------------

    @Override
    public void performRangedAttack(LivingEntity target, float pullProgress) {
        if (!this.isDeployed()) return;
        if (this.shotCooldown > 0) return;
        if (!canShootSafe(target)) return;

        // фикс “рывков”: отмечаем момент выстрела, чтобы ближайшие N мс игнорировать Y цели
        this.lastShotTimeMs = System.currentTimeMillis();

        updateAdaptiveLeadTime(target);

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

            // ✅ синхра поворота пули
            bullet.alignToVelocity();

            serverLevel.addFreshEntity(bullet);

            if (ModSounds.TURRET_FIRE.isPresent()) {
                this.playSound(ModSounds.TURRET_FIRE.get(), 1.0F, 1.0F);
            }
        }
    }

    // -------------------- GeckoLib --------------------

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

    // -------------------- NBT --------------------

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

    // -------------------- Goals --------------------

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

    // -------------------- Misc --------------------

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }

    public void setOwner(Player player) { this.entityData.set(OWNER_UUID, Optional.of(player.getUUID())); }

    public UUID getOwnerUUID() { return this.entityData.get(OWNER_UUID).orElse(null); }

    public void setShooting(boolean shooting) { this.entityData.set(SHOOTING, shooting); }

    public boolean isShooting() { return this.entityData.get(SHOOTING); }

    public boolean isDeployed() { return this.entityData.get(DEPLOYED); }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public double getBoneResetTime() { return 0; }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockIn) {}
}
