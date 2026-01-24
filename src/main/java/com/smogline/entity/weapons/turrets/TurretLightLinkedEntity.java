package com.smogline.entity.weapons.turrets;

import com.mojang.datafixers.util.Pair;
import com.smogline.block.custom.weapons.TurretLightPlacerBlock;
import com.smogline.block.entity.custom.TurretAmmoContainer;
import com.smogline.block.entity.custom.TurretLightPlacerBlockEntity;
import com.smogline.entity.ModEntities;
import com.smogline.entity.weapons.bullets.TurretBulletEntity;
import com.smogline.entity.weapons.turrets.logic.TurretLightComputer;
import com.smogline.item.tags_and_tiers.AmmoRegistry;
import com.smogline.item.tags_and_tiers.IAmmoItem;
import com.smogline.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
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

public class TurretLightLinkedEntity extends Monster implements GeoEntity, RangedAttackMob {

    // В начало класса добавь:
    private TurretAmmoContainer linkedAmmoContainer = null;

    public void setAmmoContainer(TurretAmmoContainer container) {
        this.linkedAmmoContainer = container;
    }

    public TurretAmmoContainer getAmmoContainer() {
        return linkedAmmoContainer;
    }
    private final TurretLightComputer computer;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Synched data
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

    // Balance
    private static final int DEPLOY_DURATION = 80;
    private static final int SHOT_ANIMATION_LENGTH = 14;
    private static final int HEAL_DELAY_TICKS = 200;
    private static final int HEAL_INTERVAL_TICKS = 20;
    private static final float HEAL_AMOUNT = 1.0F;

    // Local state
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

    // -------------------- Attributes --------------------

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 35.0D);
    }

    // -------------------- Despawn --------------------

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void checkDespawn() {
        // no-op (anti despawn)
    }

    // -------------------- Synched data --------------------

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(PARENT_BLOCK_POS, Optional.empty());
        this.entityData.define(LAST_DAMAGE_TICK, 0);
        this.entityData.define(SHOOTING, false);
        this.entityData.define(DEPLOYED, false);
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(DEPLOY_TIMER, DEPLOY_DURATION);
        this.entityData.define(TARGET_ID, -1);
    }

    // -------------------- Tick --------------------

    @Override
    public void tick() {
        super.tick();

        // Body follows base yaw (как у обычной)
        this.yBodyRot = this.getYRot();
        this.yBodyRotO = this.getYRot();

        // Base “fix” to head when it turns too far
        float diff = Mth.wrapDegrees(this.yHeadRot - this.getYRot());
        if (Math.abs(diff) > 60.0F) {
            float step = 8.0F;
            this.setYRot(this.getYRot() + Mth.clamp(diff, -step, step));
        }

        if (this.level().isClientSide) {
            // client debug (если нужно)
            int tid = this.entityData.get(TARGET_ID);
            if (tid != -1) {
                Entity e = this.level().getEntity(tid);
                if (e instanceof LivingEntity living && living.isAlive()) {
                    Vec3 muzzle = getMuzzlePos();
                    computer.calculateBallisticVelocity(living, muzzle);
                }
            }
            return;
        }

        // ---- Grace period: дождаться, пока block entity/placer всё проставит ----
        if (this.tickCount < 20) {
            BlockPos p = getParentBlock();
            if (p != null) {
                this.moveTo(p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 0.5, this.getYRot(), this.getXRot());
            }
            return;
        }

        // ---- Validate parent block ----
        BlockPos parent = getParentBlock();
        if (parent == null) {
            this.discard();
            return;
        }

        BlockState state = this.level().getBlockState(parent);
        if (!(state.getBlock() instanceof TurretLightPlacerBlock)) {
            this.discard();
            return;
        }

        // ---- Position sync ----
        this.moveTo(parent.getX() + 0.5, parent.getY() + 1.0, parent.getZ() + 0.5, this.getYRot(), this.getXRot());

        // ---- Auto heal ----
        int last = this.entityData.get(LAST_DAMAGE_TICK);
        if (this.tickCount - last >= HEAL_DELAY_TICKS) {
            if (this.getHealth() < this.getMaxHealth() && (this.tickCount % HEAL_INTERVAL_TICKS == 0)) {
                this.heal(HEAL_AMOUNT);
            }
        }

        // ---- Deploy timer ----
        int t = this.entityData.get(DEPLOY_TIMER);
        if (t > 0) {
            this.entityData.set(DEPLOY_TIMER, t - 1);
            if (t - 1 <= 0 && !this.isDeployed()) {
                this.entityData.set(DEPLOYED, true);
            }
        }

        // ---- HARD GATE: до deploy вообще не работаем по целям ----
        if (!this.isDeployed()) {
            this.setTarget(null);
            this.currentTargetCache = null;
            computer.updateTracking(null);

            // можно ещё сбрасывать анимацию стрельбы на всякий:
            this.setShooting(false);

            // target id sync
            if (this.entityData.get(TARGET_ID) != -1) this.entityData.set(TARGET_ID, -1);
            return;
        }

        // ---- Timers ----
        if (this.shotCooldown > 0) this.shotCooldown--;
        if (this.lockSoundCooldown > 0) this.lockSoundCooldown--;

        if (this.isShooting()) {
            shootAnimTimer++;
            if (shootAnimTimer >= SHOT_ANIMATION_LENGTH) {
                this.setShooting(false);
                shootAnimTimer = 0;
            }
        }

        // ---- Target management ----
        LivingEntity target = this.getTarget();
        currentTargetPriority = target != null ? computer.calculateTargetPriority(target, getOwnerUUID()) : 999;

        if (target != null && target != currentTargetCache) {
            if (this.lockSoundCooldown <= 0 && ModSounds.TURRET_LOCK.isPresent()) {
                this.playSound(ModSounds.TURRET_LOCK.get(), 1.0F, 1.0F);
                this.lockSoundCooldown = 40;
            }
            currentTargetCache = target;
        } else if (target == null) {
            currentTargetCache = null;
        }

        // tracking update
        computer.updateTracking(target);

        // target id sync (debug)
        int targetId = target != null ? target.getId() : -1;
        if (this.entityData.get(TARGET_ID) != targetId) {
            this.entityData.set(TARGET_ID, targetId);
        }

        // ---- Aim ----
        if (target != null && target.isAlive()) {
            Vec3 aimPos = getAimTargetPosition(target);
            if (aimPos != null) {
                this.getLookControl().setLookAt(aimPos.x, aimPos.y, aimPos.z, 30.0F, 30.0F);
            }
        }

        // ---- Priority switching (every 10 ticks) ----
        if (this.tickCount % 10 == 0) {
            LivingEntity closeThreat = computer.findClosestThreat(getOwnerUUID());
            if (closeThreat != null && closeThreat != this.getTarget()) {
                int newPriority = computer.calculateTargetPriority(closeThreat, getOwnerUUID());
                if (newPriority < currentTargetPriority) {
                    this.setTarget(closeThreat);
                    currentTargetPriority = newPriority;
                }
            }
        }

        if (!this.level().isClientSide) {
            BlockEntity be = this.level().getBlockEntity(getParentBlock());
            if (be instanceof TurretLightPlacerBlockEntity turretBE && linkedAmmoContainer == null) {
                setAmmoContainer(turretBE.getAmmoContainer());
            }
        }
    }

    private Vec3 getAimTargetPosition(LivingEntity target) {
        Vec3 muzzle = getMuzzlePos();
        Vec3 vel = computer.calculateBallisticVelocity(target, muzzle);
        if (vel != null) return muzzle.add(vel.normalize().scale(10.0));
        return null;
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

    // -------------------- Ranged attack --------------------

    @Override
    public void performRangedAttack(LivingEntity target, float pullProgress) {
        // 1. Базовые проверки
        if (!this.isDeployed()) return;
        if (this.shotCooldown > 0) return;
        if (!computer.canShootSafe(target, getMuzzlePos(), getOwnerUUID())) return;

        // 2. Забираем патрон
        IAmmoItem ammoUsed = null;
        if (linkedAmmoContainer != null) {
            ammoUsed = linkedAmmoContainer.takeAmmoAndGet("20mm_turret");
            if (ammoUsed == null) return;
        } else {
            return;
        }

        // 3. Расчет баллистики
        Vec3 muzzlePos = getMuzzlePos();
        if (!computer.canShootSafe(target, muzzlePos, getOwnerUUID())) return;

        // Баллистика
        Vec3 ballisticVelocity = computer.calculateBallisticVelocity(target, muzzlePos);
        if (ballisticVelocity == null) return;

        // 4. Проверка наведения
        double horizontalDist = Math.sqrt(ballisticVelocity.x * ballisticVelocity.x + ballisticVelocity.z * ballisticVelocity.z);
        float targetYaw = (float) (Math.atan2(ballisticVelocity.z, ballisticVelocity.x) * (180D / Math.PI)) - 90.0F;
        float targetPitch = (float) (Math.atan2(ballisticVelocity.y, horizontalDist) * (180D / Math.PI));

        float currentYaw = this.yHeadRot;
        float currentPitch = -this.getXRot();

        if (Math.abs(wrapDegrees(targetYaw - currentYaw)) > 10.0F ||
                Math.abs(wrapDegrees(targetPitch - currentPitch)) > 10.0F) return;

        // 5. Выстрел
        computer.onShotFired(target, muzzlePos);
        this.shotCooldown = SHOT_ANIMATION_LENGTH;
        this.setShooting(true);
        this.shootAnimTimer = 0;

        if (!this.level().isClientSide) {
            ServerLevel serverLevel = (ServerLevel) this.level();
            TurretBulletEntity bullet = new TurretBulletEntity(serverLevel, this);

            // ✅ ПРАВИЛЬНОЕ ПОЛУЧЕНИЕ ТИПА ПАТРОНА (как в пулемете)
            if (ammoUsed instanceof Item item) {
                ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
                if (itemId != null) {
                    AmmoRegistry.AmmoType type = AmmoRegistry.getAmmoTypeById(itemId.toString());
                    if (type != null) {
                        bullet.setAmmoType(type);
                    } else {
                        // Fallback: создаем тип вручную, если в реестре нет
                        bullet.setAmmoType(new AmmoRegistry.AmmoType(
                                itemId.toString(),
                                ammoUsed.getCaliber(),
                                ammoUsed.getDamage(),
                                ammoUsed.getSpeed(),
                                ammoUsed.isPiercing()
                        ));
                    }
                }
            } else {
                // Fallback для неизвестных предметов
                bullet.setAmmoType(new AmmoRegistry.AmmoType("default", "20mm_turret", 6.0f, 3.0f, false));
            }

            bullet.setPos(muzzlePos.x, muzzlePos.y, muzzlePos.z);
            bullet.setDeltaMovement(ballisticVelocity); // Вектор уже рассчитан компьютером

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

    // -------------------- Damage --------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean ok = super.hurt(source, amount);
        if (ok) this.entityData.set(LAST_DAMAGE_TICK, this.tickCount);
        return ok;
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

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public double getBoneResetTime() {
        return 0;
    }

    // -------------------- Goals --------------------

    @Override
    protected void registerGoals() {
        // Attack goal (как у обычной турели)
        this.goalSelector.addGoal(1, new Goal() {
            private final TurretLightLinkedEntity turret = TurretLightLinkedEntity.this;

            @Override
            public boolean canUse() {
                LivingEntity target = turret.getTarget();
                return turret.isDeployed()
                        && target != null
                        && target.isAlive()
                        && turret.distanceToSqr(target) < 1225.0D; // 35^2
            }

            @Override
            public void start() {
                turret.getNavigation().stop();
            }

            @Override
            public void stop() {
                turret.setShooting(false);
            }

            @Override
            public boolean requiresUpdateEveryTick() {
                return true;
            }

            @Override
            public void tick() {
                LivingEntity target = turret.getTarget();
                if (target == null) return;
                turret.getSensing().tick();
                turret.performRangedAttack(target, 1.0F);
            }
        });

        // 1) Кто бьёт хозяина / целится в хозяина (только после deploy)
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, entity -> {
            if (!this.isDeployed()) return false;
            if (computer.isAllied(entity, this.getOwnerUUID())) return false;

            UUID ownerUUID = this.getOwnerUUID();
            if (ownerUUID == null) return false;

            Player owner = this.level().getPlayerByUUID(ownerUUID);
            if (owner == null) return false;

            if (owner.getLastHurtByMob() == entity) return true;
            return entity instanceof Mob mob && mob.getTarget() == owner;
        }));

        // 2) Кто бьёт союзную linked-турель / целится в неё (только после deploy)
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, entity -> {
            if (!this.isDeployed()) return false;
            if (computer.isAllied(entity, this.getOwnerUUID())) return false;

            List<Entity> allies = this.level().getEntities(this, this.getBoundingBox().inflate(16.0D));
            for (Entity e : allies) {
                if (e instanceof TurretLightLinkedEntity ally && ally != this && this.isAlliedTo(ally)) {
                    if (ally.getLastHurtByMob() == entity) return true;
                    if (entity instanceof Mob mob && mob.getTarget() == ally) return true;
                }
            }
            return false;
        }));

        this.targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers(TurretLightLinkedEntity.class));

        // 4) Кого бьёт хозяин (только после deploy)
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, entity -> {
            if (!this.isDeployed()) return false;
            if (computer.isAllied(entity, this.getOwnerUUID())) return false;

            UUID ownerUUID = this.getOwnerUUID();
            if (ownerUUID == null) return false;

            Player owner = this.level().getPlayerByUUID(ownerUUID);
            return owner != null && owner.getLastHurtMob() == entity;
        }));

        // 5–6) Пассивная агрессия: только монстры и игроки (как у обычной) [file:11]
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                entity -> this.isDeployed() && !this.isAlliedTo(entity)));

        this.targetSelector.addGoal(6, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                entity -> this.isDeployed() && !this.isAlliedTo(entity)));
    }

    // -------------------- Entity props --------------------

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

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockIn) {
        // no-op
    }

    // -------------------- Data access --------------------

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

    public void setParentBlock(BlockPos pos) {
        this.entityData.set(PARENT_BLOCK_POS, Optional.ofNullable(pos));
    }

    public BlockPos getParentBlock() {
        return this.entityData.get(PARENT_BLOCK_POS).orElse(null);
    }

    // -------------------- NBT --------------------

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

        UUID owner = this.getOwnerUUID();
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }

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

        if (tag.hasUUID("Owner")) {
            this.entityData.set(OWNER_UUID, Optional.of(tag.getUUID("Owner")));
        }

        if (tag.contains("Deployed")) {
            this.entityData.set(DEPLOYED, tag.getBoolean("Deployed"));
        }

        if (tag.contains("DeployTimer")) {
            this.entityData.set(DEPLOY_TIMER, tag.getInt("DeployTimer"));
        }
    }

    // -------------------- Debug access --------------------

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
