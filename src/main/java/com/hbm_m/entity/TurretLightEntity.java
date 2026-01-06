package com.hbm_m.entity;

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
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
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

    // Длительность деплоя (в тиках)
    private static final int DEPLOY_DURATION = 80;

    // Таймер выстрела (быстрый)
    private int shootAnimTimer = 0;

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
    public int getMaxHeadYRot() { return 360; } // Голова на 360
    @Override
    public int getMaxHeadXRot() { return 90; }  // Вверх/вниз полностью

    @Override
    public void tick() {
        super.tick();
        // Фиксируем корпус на месте
        this.yBodyRot = this.getYRot();
        this.yBodyRotO = this.getYRot();

        if (!this.level().isClientSide) {
            int currentTimer = this.entityData.get(DEPLOY_TIMER);
            if (currentTimer > 0) {
                this.entityData.set(DEPLOY_TIMER, currentTimer - 1);
                if (currentTimer - 1 == 0) {
                    this.entityData.set(DEPLOYED, true);
                }
            }

            if (this.isShooting()) {
                shootAnimTimer++;
                // Быстрая анимация (15 тиков = 0.75 сек)
                if (shootAnimTimer > 15) {
                    this.setShooting(false);
                    shootAnimTimer = 0;
                }
            }
        }
    }

    public void setOwner(Player player) { this.entityData.set(OWNER_UUID, Optional.of(player.getUUID())); }
    public UUID getOwnerUUID() { return this.entityData.get(OWNER_UUID).orElse(null); }
    public int getDeployTimer() { return this.entityData.get(DEPLOY_TIMER); }

    @Override
    public boolean isAlliedTo(Entity entity) {
        if (entity == this) return true;
        if (this.getOwnerUUID() != null) {
            if (entity instanceof Player player && this.getOwnerUUID().equals(player.getUUID())) return true;
            if (entity instanceof TurretLightEntity turret && this.getOwnerUUID().equals(turret.getOwnerUUID())) return true;
        }
        return super.isAlliedTo(entity);
    }

    private boolean canShootSafe(LivingEntity target) {
        Vec3 start = this.getEyePosition();
        Vec3 end = target.getEyePosition();
        Vec3 viewVector = end.subtract(start);
        EntityHitResult hitResult = ProjectileUtil.getEntityHitResult(
                this.level(), this, start, end,
                this.getBoundingBox().expandTowards(viewVector).inflate(1.0D),
                (entity) -> !entity.isSpectator() && entity.isPickable() && entity != target
        );
        if (hitResult != null) {
            Entity hitEntity = hitResult.getEntity();
            if (start.distanceToSqr(hitEntity.position()) < start.distanceToSqr(target.position())) {
                if (isAlliedTo(hitEntity)) return false;
            }
        }
        return true;
    }

    @Override
    protected void registerGoals() {
        // СКОРОСТРЕЛЬНОСТЬ: 10 тиков (было 30)
        this.goalSelector.addGoal(1, new RangedAttackGoal(this, 0.0D, 10, 20.0F) {
            @Override
            public boolean canUse() { return TurretLightEntity.this.isDeployed() && super.canUse(); }
            @Override
            public boolean canContinueToUse() { return TurretLightEntity.this.isDeployed() && super.canContinueToUse(); }
        });

        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F) {
            @Override
            public boolean canUse() { return TurretLightEntity.this.isDeployed() && super.canUse(); }
        });

        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, (entity) -> {
            return TurretLightEntity.this.isDeployed() && !TurretLightEntity.this.isAlliedTo(entity);
        }));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false, (entity) -> {
            return TurretLightEntity.this.isDeployed() && !TurretLightEntity.this.isAlliedTo(entity);
        }));
    }

    @Override
    public void performRangedAttack(LivingEntity target, float pullProgress) {
        if (!this.isDeployed()) return;
        if (!canShootSafe(target)) return;

        Arrow arrow = new Arrow(this.level(), this);
        double d0 = target.getX() - this.getX();
        double d1 = target.getY(0.3333333333333333D) - arrow.getY();
        double d2 = target.getZ() - this.getZ();
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);

        arrow.shoot(d0, d1 + d3 * 0.2D, d2, 1.6F, 1.0F);
        this.playSound(SoundEvents.GENERIC_EXPLODE, 0.5F, 2.0F);
        this.level().addFreshEntity(arrow);

        this.setShooting(true);
        this.shootAnimTimer = 0;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Контроллер Раскладки
        controllers.add(new AnimationController<>(this, "deploy_ctrl", 0, event -> {

            // ✅ ГЛАВНОЕ ИСПРАВЛЕНИЕ:
            // Если турель УЖЕ разложена (DEPLOYED = true), мы принудительно СТОПАЕМ анимацию.
            // Теперь установка новой турели не будет влиять на старые.
            if (this.isDeployed()) {
                return PlayState.STOP;
            }

            // Иначе (если ещё не разложена) - играем анимацию
            if (this.getDeployTimer() > 0) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("deploy"));
            }

            return PlayState.STOP;
        }));

        // Контроллер Стрельбы (без изменений)
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
    public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }
    public void setShooting(boolean shooting) { this.entityData.set(SHOOTING, shooting); }
    public boolean isShooting() { return this.entityData.get(SHOOTING); }
    public boolean isDeployed() { return this.entityData.get(DEPLOYED); }
    @Override
    public boolean isPushable() { return false; }
    @Override
    protected void playStepSound(BlockPos pos, BlockState blockIn) {}
}
