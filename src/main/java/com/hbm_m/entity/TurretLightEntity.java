package com.hbm_m.entity;

import com.hbm_m.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
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
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TurretLightEntity extends Monster implements GeoEntity, RangedAttackMob {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Boolean> SHOOTING = SynchedEntityData.defineId(TurretLightEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DEPLOYED = SynchedEntityData.defineId(TurretLightEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(TurretLightEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DEPLOY_TIMER = SynchedEntityData.defineId(TurretLightEntity.class, EntityDataSerializers.INT);

    private static final int DEPLOY_DURATION = 80;
    private static final int SHOT_ANIMATION_LENGTH = 14;
    private static final double TARGET_LOCK_DISTANCE = 5.0D;
    private static final double CLOSE_COMBAT_RANGE = 5.0D;
    private static final float BULLET_SPEED = 3.0F;
    private static final float BULLET_GRAVITY = 0.01F;

    private int shootAnimTimer = 0;
    private int shotCooldown = 0;
    private int lockSoundCooldown = 0;
    private LivingEntity currentTargetCache = null;
    private int currentTargetPriority = 999;

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
    }

    @Override
    public int getMaxHeadYRot() { return 360; }

    @Override
    public int getMaxHeadXRot() { return 90; }

    @Override
    public boolean isAlliedTo(Entity entity) {
        if (super.isAlliedTo(entity)) return true;
        if (entity instanceof TurretLightEntity otherTurret) {
            UUID myOwner = this.getOwnerUUID();
            UUID theirOwner = otherTurret.getOwnerUUID();
            if (myOwner != null && myOwner.equals(theirOwner)) return true;
        }
        UUID ownerUUID = this.getOwnerUUID();
        if (ownerUUID != null && entity.getUUID().equals(ownerUUID)) return true;
        return false;
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

    @Override
    public void tick() {
        super.tick();
        this.yBodyRot = this.getYRot();
        this.yBodyRotO = this.getYRot();

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

            if (target != null) {
                currentTargetPriority = calculateTargetPriority(target);
            } else {
                currentTargetPriority = 999;
            }

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
                Vec3 aimPoint = calculateInterceptPosition(target);
                if (aimPoint != null) {
                    this.getLookControl().setLookAt(aimPoint.x, aimPoint.y, aimPoint.z, 30.0F, 30.0F);
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
                    double distToCurrentTarget = this.distanceTo(target);
                    if (distToCurrentTarget < TARGET_LOCK_DISTANCE) {
                        canSwitchToFarTarget = false;
                    }
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
        }
    }

    private Vec3 calculateInterceptPosition(LivingEntity target) {
        Vec3 targetPos = getViableTargetPos(target);
        if (targetPos == null) return null;

        double dist = this.getEyePosition().distanceTo(targetPos);
        double timeToImpact = dist / BULLET_SPEED;
        Vec3 targetVelocity = target.getDeltaMovement();

        double predictedX = targetPos.x + (targetVelocity.x * timeToImpact * 15.0);
        double predictedZ = targetPos.z + (targetVelocity.z * timeToImpact * 15.0);
        double predictedY = targetPos.y + (targetVelocity.y * timeToImpact);

        if (predictedY < target.getY()) predictedY = target.getY();

        double bulletDrop = 0.5 * BULLET_GRAVITY * timeToImpact * timeToImpact;
        return new Vec3(predictedX, predictedY + bulletDrop, predictedZ);
    }

    private boolean isLineOfFireSafe(LivingEntity target) {
        Vec3 start = this.getEyePosition();
        Vec3 end = getViableTargetPos(target);
        if (end == null) return false;

        Vec3 vec3 = end.subtract(start);
        double distance = vec3.length();
        vec3 = vec3.normalize();

        for (double d = 0; d < distance; d += 0.5) {
            Vec3 checkPos = start.add(vec3.scale(d));
            AABB checkBox = new AABB(checkPos.subtract(0.5, 0.5, 0.5), checkPos.add(0.5, 0.5, 0.5));
            List<Entity> list = this.level().getEntities(this, checkBox);

            for (Entity e : list) {
                if (e != this && e != target && e instanceof LivingEntity living) {
                    if (this.isAlliedTo(living)) {
                        return false;
                    }
                }
            }
        }
        return true;
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

    private Vec3 getViableTargetPos(LivingEntity target) {
        float height = target.getBbHeight();
        float width = target.getBbWidth();
        Vec3 pos = target.position();

        if (width > height) {
            Vec3 center = pos.add(0, height * 0.5, 0);
            if (canSeePoint(this.getEyePosition(), center)) return center;
        }

        if (height < 0.9F) {
            Vec3 center = pos.add(0, height * 0.5, 0);
            if (canSeePoint(this.getEyePosition(), center)) return center;
            return null;
        }

        Vec3 chest = pos.add(0, height * 0.65, 0);
        if (canSeePoint(this.getEyePosition(), chest)) return chest;

        Vec3 head = target.getEyePosition();
        if (canSeePoint(this.getEyePosition(), head)) return head;

        Vec3 feet = pos.add(0, 0.1, 0);
        if (canSeePoint(this.getEyePosition(), feet)) return feet;

        return null;
    }

    private boolean canSeePoint(Vec3 start, Vec3 end) {
        BlockHitResult blockHit = this.level().clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this
        ));
        return blockHit.getType() == HitResult.Type.MISS ||
                start.distanceToSqr(blockHit.getLocation()) > start.distanceToSqr(end);
    }

    private boolean canShootSafe(LivingEntity target) {
        return getViableTargetPos(target) != null && isLineOfFireSafe(target);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new RangedAttackGoal(this, 0.0D, SHOT_ANIMATION_LENGTH, 20.0F) {
            @Override
            public boolean canUse() { return TurretLightEntity.this.isDeployed() && super.canUse(); }
            @Override
            public boolean canContinueToUse() { return TurretLightEntity.this.isDeployed() && super.canContinueToUse(); }
        });

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

        this.targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers(TurretLightEntity.class));

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

        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false, (entity) -> {
            if (this.isAlliedTo(entity)) return false;
            return TurretLightEntity.this.isDeployed();
        }));

        this.targetSelector.addGoal(6, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, (entity) -> {
            if (this.isAlliedTo(entity)) return false;
            return TurretLightEntity.this.isDeployed();
        }));
    }

    @Override
    public void performRangedAttack(LivingEntity target, float pullProgress) {
        if (!this.isDeployed()) return;
        if (this.shotCooldown > 0) return;
        if (!canShootSafe(target)) return;

        this.shotCooldown = SHOT_ANIMATION_LENGTH;
        this.setShooting(true);
        this.shootAnimTimer = 0;

        TurretBulletEntity bullet = new TurretBulletEntity(this.level(), this);

        double offsetY = 10.49 / 16.0;
        double offsetZ = 14.86 / 16.0;

        float yRotRad = this.yHeadRot * ((float)Math.PI / 180F);
        float xRotRad = -this.getXRot() * ((float)Math.PI / 180F);

        double yShift = Math.sin(xRotRad) * offsetZ;
        double forwardShift = Math.cos(xRotRad) * offsetZ;

        double spawnX = this.getX() - Math.sin(yRotRad) * forwardShift;
        double spawnY = this.getY() + offsetY + yShift;
        double spawnZ = this.getZ() + Math.cos(yRotRad) * forwardShift;

        bullet.setPos(spawnX, spawnY, spawnZ);

        bullet.setYRot(this.yHeadRot);
        bullet.setXRot(this.getXRot());

        Vec3 lookVec = Vec3.directionFromRotation(this.getXRot(), this.yHeadRot);
        bullet.shoot(lookVec.x, lookVec.y, lookVec.z, BULLET_SPEED, 0.1F);

        if (ModSounds.TURRET_FIRE.isPresent()) {
            this.playSound(ModSounds.TURRET_FIRE.get(), 1.0F, 1.0F);
        } else {
            this.playSound(SoundEvents.GENERIC_EXPLODE, 0.5F, 2.0F);
        }

        this.level().addFreshEntity(bullet);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "deploy_ctrl", 0, event -> {
            if (!this.isDeployed()) return event.setAndContinue(RawAnimation.begin().thenPlay("deploy"));
            return PlayState.STOP;
        }));

        controllers.add(new AnimationController<>(this, "shoot_ctrl", 0, event -> {
            if (this.isShooting()) {
                if (event.getController().getAnimationState() == AnimationController.State.STOPPED)
                    event.getController().forceAnimationReset();
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
    public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }

    public void setOwner(Player player) { this.entityData.set(OWNER_UUID, Optional.of(player.getUUID())); }
    public UUID getOwnerUUID() { return this.entityData.get(OWNER_UUID).orElse(null); }
    public void setShooting(boolean shooting) { this.entityData.set(SHOOTING, shooting); }
    public boolean isShooting() { return this.entityData.get(SHOOTING); }
    public boolean isDeployed() { return this.entityData.get(DEPLOYED); }

    @Override
    public boolean isPushable() { return false; }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockIn) {}
}