package com.smogline.entity.weapons.turrets;

import com.mojang.datafixers.util.Pair;
import com.smogline.block.custom.weapons.TurretLightPlacerBlock;
import com.smogline.entity.ModEntities;
import com.smogline.entity.weapons.bullets.TurretBulletEntity;
import com.smogline.entity.weapons.turrets.logic.TurretLightComputer;
import com.smogline.item.tags_and_tiers.AmmoRegistry;
import com.smogline.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TurretLightLinkedEntity extends Monster implements GeoEntity {

    private final TurretLightComputer computer;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Optional<BlockPos>> PARENT_BLOCK_POS =
            SynchedEntityData.defineId(TurretLightLinkedEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Integer> LAST_DAMAGE_TICK =
            SynchedEntityData.defineId(TurretLightLinkedEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SHOOTING =
            SynchedEntityData.defineId(TurretLightLinkedEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DEPLOYED =
            SynchedEntityData.defineId(TurretLightLinkedEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(TurretLightLinkedEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DEPLOY_TIMER =
            SynchedEntityData.defineId(TurretLightLinkedEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_ID =
            SynchedEntityData.defineId(TurretLightLinkedEntity.class, EntityDataSerializers.INT);

    private static final int DEPLOY_DURATION = 80;
    private static final int SHOT_ANIMATION_LENGTH = 14;
    private static final int HEAL_DELAY_TICKS = 200;
    private static final int HEAL_INTERVAL_TICKS = 20;
    private static final float HEAL_AMOUNT = 1.0F;
    private static final double TARGET_LOCK_DISTANCE = 5.0;

    private int shootAnimTimer = 0;
    private int shotCooldown = 0;
    private int lockSoundCooldown = 0;
    private LivingEntity currentTargetCache = null;
    private int currentTargetPriority = 999;

    public TurretLightLinkedEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 0;
        this.computer = new TurretLightComputer(this, TurretLightComputer.Config.STANDARD_20MM);

    }

    public TurretLightLinkedEntity(Level level) {
        this(ModEntities.TURRET_LIGHT_LINKED.get(), level);
    }

    // 🔥 ЗАПРЕТ ДЕСПАВНА
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false; // Никогда не удалять из-за дистанции
    }

    @Override
    public void checkDespawn() {
        // Пустой метод блокирует стандартную проверку деспавна мобов
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
        // Должно быть так:
        this.entityData.define(PARENT_BLOCK_POS, Optional.empty());
        this.entityData.define(LAST_DAMAGE_TICK, 0);
        this.entityData.define(SHOOTING, false);
        this.entityData.define(DEPLOYED, false);
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(DEPLOY_TIMER, DEPLOY_DURATION);
        this.entityData.define(TARGET_ID, -1);
    }

    public void setParentBlock(BlockPos pos) {
        this.entityData.set(PARENT_BLOCK_POS, Optional.ofNullable(pos));
    }

    public BlockPos getParentBlock() {
        return this.entityData.get(PARENT_BLOCK_POS).orElse(null);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            // 🔥 ФОРА: Не проверяем буфер первые 20 тиков (1 секунда)
            if (this.tickCount < 20) {
                // Но позицию обновлять надо, если parent уже есть
                BlockPos p = getParentBlock();
                if (p != null) {
                    this.moveTo(p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 0.5, this.getYRot(), this.getXRot());
                }
                return; // Просто выходим, не удаляя турель
            }

            BlockPos parent = getParentBlock();
            if (parent == null) {
                // Если спустя 20 тиков всё ещё null - тогда удаляем
                System.out.println("TURRET DESPAWN: Parent Pos is NULL after grace period");
                this.discard();
                return;
            }


            double x = parent.getX() + 0.5;
            double y = parent.getY() + 1.0;
            double z = parent.getZ() + 0.5;
            this.moveTo(x, y, z, this.getYRot(), this.getXRot());

            int last = this.entityData.get(LAST_DAMAGE_TICK);
            if (this.tickCount - last >= HEAL_DELAY_TICKS) {
                if (this.getHealth() < this.getMaxHealth() && (this.tickCount % HEAL_INTERVAL_TICKS == 0)) {
                    this.heal(HEAL_AMOUNT);
                }
            }

            int currentTimer = this.entityData.get(DEPLOY_TIMER);
            if (currentTimer > 0) {
                this.entityData.set(DEPLOY_TIMER, currentTimer - 1);
            }
            if (currentTimer - 1 <= 0 && !this.isDeployed()) {
                this.entityData.set(DEPLOYED, true);
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
            currentTargetPriority = target != null ? computer.calculateTargetPriority(target, getOwnerUUID()) : 999;

            if (target != null && target != currentTargetCache && this.isDeployed()) {
                if (this.lockSoundCooldown <= 0) {
                    if (ModSounds.TURRET_LOCK.isPresent()) {
                        this.playSound(ModSounds.TURRET_LOCK.get(), 1.0F, 1.0F);
                    }
                    this.lockSoundCooldown = 40;
                }
                currentTargetCache = target;
            } else if (target == null) {
                currentTargetCache = null;
            }

            computer.updateTracking(target);

            int targetId = target != null ? target.getId() : -1;
            if (this.entityData.get(TARGET_ID) != targetId) {
                this.entityData.set(TARGET_ID, targetId);
            }

            if (target != null && this.isDeployed()) {
                Vec3 aimPos = getAimTargetPosition(target);
                if (aimPos != null) {
                    this.getLookControl().setLookAt(aimPos.x, aimPos.y, aimPos.z, 30.0F, 30.0F);
                }
            }

            if (this.tickCount % 10 == 0) {
                LivingEntity closeThreat = computer.findClosestThreat(getOwnerUUID());
                if (closeThreat != null && closeThreat != this.getTarget()) {
                    int newPriority = computer.calculateTargetPriority(closeThreat, getOwnerUUID());
                    if (newPriority < currentTargetPriority) {
                        this.setTarget(closeThreat);
                        currentTargetPriority = newPriority;
                    }
                }

                boolean canSwitch = true;
                if (target != null && target.isAlive()) {
                    if (this.distanceToSqr(target) < TARGET_LOCK_DISTANCE * TARGET_LOCK_DISTANCE) {
                        canSwitch = false;
                    }
                }

                if (canSwitch) {
                    UUID ownerUUID = this.getOwnerUUID();
                    if (ownerUUID != null) {
                        Player owner = this.level().getPlayerByUUID(ownerUUID);
                        if (owner != null) {
                            LivingEntity ownerAttacker = owner.getLastHurtByMob();
                            if (ownerAttacker != null && ownerAttacker != this.getTarget() && ownerAttacker.isAlive()
                                    && !computer.isAllied(ownerAttacker, ownerUUID)) {
                                int newPriority = computer.calculateTargetPriority(ownerAttacker, ownerUUID);
                                if (newPriority < currentTargetPriority) {
                                    this.setTarget(ownerAttacker);
                                    currentTargetPriority = newPriority;
                                }
                            }
                        }
                    }

                }
            }
        } else {
            int targetId = this.entityData.get(TARGET_ID);
            if (targetId != -1) {
                Entity targetEntity = this.level().getEntity(targetId);
                if (targetEntity instanceof LivingEntity livingTarget && livingTarget.isAlive()) {
                    Vec3 muzzle = getMuzzlePos();
                    computer.calculateBallisticVelocity(livingTarget, muzzle);
                }
            }
        }
    }

    private Vec3 getAimTargetPosition(LivingEntity target) {
        Vec3 muzzle = getMuzzlePos();
        Vec3 velocity = computer.calculateBallisticVelocity(target, muzzle);
        if (velocity != null) {
            return muzzle.add(velocity.normalize().scale(10.0));
        }
        return null;
    }

    public void performRangedAttack(LivingEntity target, float pullProgress) {
        if (!this.isDeployed()) return;
        if (this.shotCooldown > 0) return;
        if (!computer.canShootSafe(target, getMuzzlePos(), getOwnerUUID())) return;

        Vec3 muzzlePos = getMuzzlePos();
        Vec3 ballisticVelocity = computer.calculateBallisticVelocity(target, muzzlePos);
        if (ballisticVelocity == null) return;

        double horizontalDist = Math.sqrt(ballisticVelocity.x * ballisticVelocity.x + ballisticVelocity.z * ballisticVelocity.z);
        float targetYaw = (float) (Math.atan2(ballisticVelocity.z, ballisticVelocity.x) * (180D / Math.PI)) - 90.0F;
        float targetPitch = (float) (Math.atan2(ballisticVelocity.y, horizontalDist) * (180D / Math.PI));
        float currentYaw = this.yHeadRot;
        float currentPitch = -this.getXRot();
        float yawDiff = Math.abs(wrapDegrees(targetYaw - currentYaw));
        float pitchDiff = Math.abs(wrapDegrees(targetPitch - currentPitch));

        if (yawDiff > 10.0F || pitchDiff > 10.0F) return;

        computer.onShotFired(target, muzzlePos);

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

            // 🔥 ПРАВИЛЬНАЯ ИНИЦИАЛИЗАЦИЯ ПОВОРОТА ПЕРЕД СПАВНОМ
            bullet.setYRot(targetYaw);
            bullet.setXRot(targetPitch);
            bullet.yRotO = targetYaw;
            bullet.xRotO = targetPitch;

            serverLevel.addFreshEntity(bullet);

            if (ModSounds.TURRET_FIRE.isPresent()) {
                this.playSound(ModSounds.TURRET_FIRE.get(), 1.0F, 1.0F);
            }
        }
    }

    private float wrapDegrees(float degrees) {
        float f = degrees % 360.0F;
        if (f >= 180.0F) f -= 360.0F;
        if (f < -180.0F) f += 360.0F;
        return f;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean ok = super.hurt(source, amount);
        if (ok) {
            this.entityData.set(LAST_DAMAGE_TICK, this.tickCount);
        }
        return ok;
    }

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
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public double getBoneResetTime() {
        return 0;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.Goal() {
            private final TurretLightLinkedEntity turret = TurretLightLinkedEntity.this;

            @Override
            public boolean canUse() {
                LivingEntity target = this.turret.getTarget();
                return this.turret.isDeployed() && target != null && target.isAlive() && this.turret.distanceToSqr(target) < 1225.0D;
            }

            @Override
            public void start() {
                this.turret.getNavigation().stop();
            }

            @Override
            public void stop() {
                this.turret.setShooting(false);
            }

            @Override
            public boolean requiresUpdateEveryTick() {
                return true;
            }

            @Override
            public void tick() {
                LivingEntity target = this.turret.getTarget();
                if (target == null) return;
                this.turret.getSensing().tick();
                this.turret.performRangedAttack(target, 1.0F);
            }
        });

        // ПРАВИЛЬНАЯ ЛЯМБДА: запрашиваем getOwnerUUID() внутри проверки, а не при инициализации
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                entity -> !computer.isAllied(entity, this.getOwnerUUID()))); // <--- this.getOwnerUUID()

        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

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

    @Override
    public int getMaxHeadYRot() {
        return 360;
    }

    @Override
    public int getMaxHeadXRot() {
        return 80;
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        return computer.isAllied(entity, getOwnerUUID());
    }

    public void setOwner(Player player) {
        this.entityData.set(OWNER_UUID, Optional.of(player.getUUID()));
    }

    public UUID getOwnerUUID() {
        return this.entityData.get(OWNER_UUID).orElse(null);
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

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        BlockPos parent = getParentBlock();
        if (parent != null) {
            tag.putInt("ParentX", parent.getX());
            tag.putInt("ParentY", parent.getY());
            tag.putInt("ParentZ", parent.getZ());
        }
        tag.putInt("LastDamageTick", this.entityData.get(LAST_DAMAGE_TICK));

        if (this.getOwnerUUID() != null) tag.putUUID("Owner", this.getOwnerUUID());
        tag.putBoolean("Deployed", this.isDeployed());
        tag.putInt("DeployTimer", this.entityData.get(DEPLOY_TIMER));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.contains("ParentX")) {
            setParentBlock(new BlockPos(tag.getInt("ParentX"), tag.getInt("ParentY"), tag.getInt("ParentZ")));
        }
        if (tag.contains("LastDamageTick")) {
            this.entityData.set(LAST_DAMAGE_TICK, tag.getInt("LastDamageTick"));
        }

        if (tag.hasUUID("Owner")) this.entityData.set(OWNER_UUID, Optional.of(tag.getUUID("Owner")));
        if (tag.contains("Deployed")) this.entityData.set(DEPLOYED, tag.getBoolean("Deployed"));
        if (tag.contains("DeployTimer")) this.entityData.set(DEPLOY_TIMER, tag.getInt("DeployTimer"));
    }

    public Vec3 getDebugTargetPoint() {
        return computer.debugTargetPoint;
    }

    public Vec3 getDebugBallisticVelocity() {
        return computer.debugBallisticVelocity;
    }

    public List<Pair<Vec3, Boolean>> getDebugScanPoints() {
        return computer.debugScanPoints;
    }
}
