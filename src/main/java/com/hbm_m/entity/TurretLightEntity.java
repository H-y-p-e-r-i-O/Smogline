package com.hbm_m.entity;

import com.hbm_m.main.MainRegistry;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

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

    private int shootAnimTimer = 0;
    private int shotCooldown = 0;
    private int lockSoundCooldown = 0;

    private LivingEntity currentTargetCache = null;

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
    public void tick() {
        super.tick();

        this.yBodyRot = this.getYRot();
        this.yBodyRotO = this.getYRot();

        if (!this.level().isClientSide) {
            // Logic
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

            // --- ЗВУК ЗАХВАТА ---
            // Теперь проверяем this.isDeployed()
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

            // --- БАЛЛИСТИЧЕСКИЙ ВЫЧИСЛИТЕЛЬ ---
            if (target != null && this.isDeployed()) {
                Vec3 aimPos = getViableTargetPos(target);

                if (aimPos != null) {
                    double bulletSpeed = 3.0D;
                    double gravity = 0.03D;

                    double distance = Math.sqrt(this.distanceToSqr(aimPos));
                    double timeToImpact = distance / bulletSpeed;

                    Vec3 targetVelocity = target.getDeltaMovement();

                    double predictedX = aimPos.x + (targetVelocity.x * timeToImpact * 15.0);
                    double predictedZ = aimPos.z + (targetVelocity.z * timeToImpact * 15.0);
                    double predictedY = aimPos.y + (targetVelocity.y * timeToImpact);

                    double drop = 0.5 * gravity * (timeToImpact * timeToImpact);
                    predictedY += drop;

                    if (predictedY < target.getY()) predictedY = target.getY() + 0.5;

                    this.getLookControl().setLookAt(predictedX, predictedY, predictedZ, 30.0F, 30.0F);
                } else {
                    this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                }
            }
        }
        // --- ПРОВЕРКА ПРИОРИТЕТОВ (Force Target Switch) ---
        if (!this.level().isClientSide && this.tickCount % 10 == 0) { // Каждые полсекунды
            UUID ownerUUID = this.getOwnerUUID();
            if (ownerUUID != null) {
                Player owner = this.level().getPlayerByUUID(ownerUUID);
                if (owner != null) {
                    LivingEntity ownerAttacker = owner.getLastHurtByMob();
                    // Если владельца кто-то бьет, и это НЕ наша текущая цель -> ПЕРЕКЛЮЧАЕМСЯ!
                    if (ownerAttacker != null && ownerAttacker != this.getTarget() && ownerAttacker.isAlive() && !isAlliedTo(ownerAttacker)) {
                        this.setTarget(ownerAttacker);
                    }
                }
            }
        }

    }

    // --- УЛУЧШЕННЫЙ ВЫБОР ТОЧКИ ---
    private Vec3 getViableTargetPos(LivingEntity target) {
        Vec3 start = this.getEyePosition();
        float height = target.getBbHeight();
        float width = target.getBbWidth();

        // 1. Для ПАУКОВ (широкие и низкие)
        // Целимся ВЫШЕ центра (в спину/голову), чтобы не стрелять в землю
        if (width > height) {
            Vec3 highCenter = target.position().add(0, height * 0.75, 0);
            return canSeePoint(start, highCenter) ? highCenter : null;
        }

        // 2. Для МЕЛОЧИ (слаймы)
        if (height < 0.8F) {
            Vec3 center = target.position().add(0, height / 2, 0);
            return canSeePoint(start, center) ? center : null;
        }

        // 3. Для ОСТАЛЬНЫХ (Глаза -> Центр -> Ноги)
        Vec3[] checkPoints = new Vec3[] {
                target.getEyePosition(),
                target.position().add(0, height / 2, 0),
                target.position()
        };

        for (Vec3 end : checkPoints) {
            if (canSeePoint(start, end)) return end;
        }
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
        return getViableTargetPos(target) != null;
    }

    @Override
    protected void registerGoals() {
        // --- 1. ЦЕЛИ АТАКИ (Execution) ---
        // Стреляем в того, кого выбрали в targetSelector
        this.goalSelector.addGoal(1, new RangedAttackGoal(this, 0.0D, SHOT_ANIMATION_LENGTH, 20.0F) {
            @Override
            public boolean canUse() { return TurretLightEntity.this.isDeployed() && super.canUse(); }
            @Override
            public boolean canContinueToUse() { return TurretLightEntity.this.isDeployed() && super.canContinueToUse(); }
        });

        // --- 2. ВЫБОР ЦЕЛИ (Target Selection) ---

        // ПРИОРИТЕТ 1: ЗАЩИТА ВЛАДЕЛЬЦА (Враги, атакующие хозяина)
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, (entity) -> {
            if (this.isAlliedTo(entity)) return false;
            UUID ownerUUID = this.getOwnerUUID();
            if (ownerUUID != null) {
                Player owner = this.level().getPlayerByUUID(ownerUUID);
                if (owner != null) {
                    // Агрится на тех, кто бьет владельца
                    if (owner.getLastHurtByMob() == entity) return true;
                    if (entity instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() == owner) return true;
                }
            }
            return false;
        }));

        // ПРИОРИТЕТ 2: САМООБОРОНА и ОБОРОНА СОЮЗНИКОВ (Кто ударил меня или другую турель)
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this).setAlertOthers(TurretLightEntity.class));

        // ПРИОРИТЕТ 3: АССИСТ ВЛАДЕЛЬЦУ (Кого бьет хозяин)
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, (entity) -> {
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

        // ПРИОРИТЕТ 4: ЗАЧИСТКА ТЕРРИТОРИИ (Монстры)
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false, (entity) -> {
            // Игнорируем питомцев и союзников
            if (this.isAlliedTo(entity)) return false;
            return TurretLightEntity.this.isDeployed();
        }));

        // ПРИОРИТЕТ 5: ВРАЖДЕБНЫЕ ИГРОКИ
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, (entity) -> {
            if (this.isAlliedTo(entity)) return false; // Союзников/Владельца не трогаем
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

        // Manual Rotation Init
        bullet.setYRot(this.yHeadRot);
        bullet.setXRot(this.getXRot());
        bullet.yRotO = this.yHeadRot;
        bullet.xRotO = this.getXRot();

        Vec3 lookVec = Vec3.directionFromRotation(this.getXRot(), this.yHeadRot);
        bullet.shoot(lookVec.x, lookVec.y, lookVec.z, 3.0F, 0.1F);

        if (ModSounds.TURRET_FIRE.isPresent()) {
            this.playSound(ModSounds.TURRET_FIRE.get(), 1.0F, 1.0F);
        } else {
            this.playSound(SoundEvents.GENERIC_EXPLODE, 0.5F, 2.0F);
        }

        this.level().addFreshEntity(bullet);
    }

    // --- Boilerplate ---
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "deploy_ctrl", 0, event -> {
            if (!this.isDeployed()) return event.setAndContinue(RawAnimation.begin().thenPlay("deploy"));
            return PlayState.STOP;
        }));
        controllers.add(new AnimationController<>(this, "shoot_ctrl", 0, event -> {
            if (this.isShooting()) {
                if (event.getController().getAnimationState() == AnimationController.State.STOPPED) event.getController().forceAnimationReset();
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
