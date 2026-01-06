package com.hbm_m.entity;

import com.hbm_m.main.MainRegistry;
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

    // Длительность анимации выстрела (0.6667 сек ≈ 14 тиков)
    // Используем это число и для таймера анимации, и для кулдауна стрельбы
    private static final int SHOT_ANIMATION_LENGTH = 14;

    private int shootAnimTimer = 0;
    private int shotCooldown = 0; // Серверный кулдаун, синхронизированный с анимацией

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
    public int getMaxHeadYRot() {
        return 360;
    }

    @Override
    public int getMaxHeadXRot() {
        return 90;
    }

    @Override
    public void tick() {
        super.tick();

        // Фиксируем корпус (чтобы не вращался сам по себе)
        this.yBodyRot = this.getYRot();
        this.yBodyRotO = this.getYRot();

        if (!this.level().isClientSide) {
            // Логика развертывания (Deploy)
            int currentTimer = this.entityData.get(DEPLOY_TIMER);
            if (currentTimer > 0) {
                this.entityData.set(DEPLOY_TIMER, currentTimer - 1);
                if (currentTimer - 1 == 0) {
                    this.entityData.set(DEPLOYED, true);
                }
            }

            // Логика кулдауна выстрела
            if (this.shotCooldown > 0) {
                this.shotCooldown--;
            }

            // Логика сброса флага анимации стрельбы
            if (this.isShooting()) {
                shootAnimTimer++;
                // Сбрасываем флаг чуть раньше конца анимации, чтобы она успела доиграть корректно
                if (shootAnimTimer >= SHOT_ANIMATION_LENGTH) {
                    this.setShooting(false);
                    shootAnimTimer = 0;
                }
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Контроллер деплоя (зависит от состояния isDeployed)
        controllers.add(new AnimationController<>(this, "deploy_ctrl", 0, event -> {
            if (!this.isDeployed()) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("deploy"));
            }
            return PlayState.STOP;
        }));

        // Контроллер стрельбы
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

    public void setOwner(Player player) {
        this.entityData.set(OWNER_UUID, Optional.of(player.getUUID()));
    }

    public UUID getOwnerUUID() {
        return this.entityData.get(OWNER_UUID).orElse(null);
    }

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
        // Интервал атаки = длине анимации (SHOT_ANIMATION_LENGTH), чтобы AI не спамил
        this.goalSelector.addGoal(1, new RangedAttackGoal(this, 0.0D, SHOT_ANIMATION_LENGTH, 20.0F) {
            @Override
            public boolean canUse() {
                return TurretLightEntity.this.isDeployed() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return TurretLightEntity.this.isDeployed() && super.canContinueToUse();
            }
        });

        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F) {
            @Override
            public boolean canUse() {
                return TurretLightEntity.this.isDeployed() && super.canUse();
            }
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
        // 1. Проверки: деплой, безопасность стрельбы, кулдаун анимации
        if (!this.isDeployed()) return;
        if (this.shotCooldown > 0) return; // Ждем окончания предыдущей анимации
        if (!canShootSafe(target)) return;

        // 2. Активируем анимацию и кулдаун
        this.shotCooldown = SHOT_ANIMATION_LENGTH;
        this.setShooting(true);
        this.shootAnimTimer = 0;

        // 3. Создаем пулю (а не стрелу!)
        TurretBulletEntity bullet = new TurretBulletEntity(this.level(), this);

        // --- Расчет позиции дула ---
        // Y = 10.49 px
        // Z = 11.86 + 3.0 = 14.86 px (смещение вперед)
        double offsetY = 10.49 / 16.0;
        double offsetZ = 14.86 / 16.0;

        // Перевод углов в радианы
        // Используем поворот головы (куда смотрит турель)
        float yRotRad = this.yHeadRot * ((float)Math.PI / 180F);
        // Инвертируем pitch для корректной математики (в MC вверх - это минус)
        float xRotRad = -this.getXRot() * ((float)Math.PI / 180F);

        // Смещение дула с учетом наклона пушки (вверх-вниз)
        double yShift = Math.sin(xRotRad) * offsetZ;
        double forwardShift = Math.cos(xRotRad) * offsetZ;

        // Финальные координаты спавна пули с учетом поворота турели
        double spawnX = this.getX() - Math.sin(yRotRad) * forwardShift;
        double spawnY = this.getY() + offsetY + yShift;
        double spawnZ = this.getZ() + Math.cos(yRotRad) * forwardShift;

        bullet.setPos(spawnX, spawnY, spawnZ);

        // ✅ СИНХРОНИЗАЦИЯ ПОВОРОТА
        // Копируем поворот головы турели в пулю ПЕРЕД выстрелом
        // Важно: yHeadRot - это "куда смотрит голова" (Yaw)
        // getXRot() - это "наклон пушки" (Pitch)
        bullet.setYRot(this.yHeadRot);
        bullet.setXRot(this.getXRot());
        // Для надежности обновляем "предыдущие" значения, чтобы не было дерганья при интерполяции
        bullet.yRotO = this.yHeadRot;
        bullet.xRotO = this.getXRot();


        // --- Расчет вектора полета ---
        double d0 = target.getX() - spawnX;
        double d1 = target.getY(0.3333333333333333D) - spawnY;
        double d2 = target.getZ() - spawnZ;
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);

        // shoot(x, y, z, velocity, inaccuracy)
        // velocity = 3.0F (быстрая пуля)
        // inaccuracy = 0.5F (небольшой разброс)
        bullet.shoot(d0, d1 + d3 * 0.05D, d2, 3.0F, 0.5F);

        this.playSound(SoundEvents.GENERIC_EXPLODE, 0.5F, 2.0F);
        this.level().addFreshEntity(bullet);
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
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public void setShooting(boolean shooting) {
        this.entityData.set(SHOOTING, shooting);
    }

    public boolean isShooting() {
        return this.entityData.get(SHOOTING);
    }

    public boolean isDeployed() {
        return this.entityData.get(DEPLOYED);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockIn) {
    }
}
