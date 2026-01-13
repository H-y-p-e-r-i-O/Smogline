package com.hbm_m.entity;

import com.hbm_m.item.tags_and_tiers.AmmoRegistry;
import com.hbm_m.sound.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractGlassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Optional;

public class TurretBulletEntity extends AbstractArrow implements GeoEntity, IEntityAdditionalSpawnData {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<String> AMMO_ID = SynchedEntityData.defineId(TurretBulletEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> AMMO_TYPE = SynchedEntityData.defineId(TurretBulletEntity.class, EntityDataSerializers.STRING);

    public static final float BULLET_GRAVITY = 0.01F;
    public static final float AIR_RESISTANCE = 0.99F;
    public static final float MAX_FLIGHT_DISTANCE = 256.0F;

    private float baseDamage = 4.0f;
    private float baseSpeed = 3.0f;
    private AmmoType ammoType = AmmoType.NORMAL;
    private float initialSpeed = 0.0f;
    private Vec3 initialPosition = null;

    // Вращение для визуального эффекта
    public float spin = 0;

    public enum AmmoType {
        NORMAL("normal"), PIERCING("piercing"), HOLLOW("hollow"), INCENDIARY("incendiary");
        public final String id;
        AmmoType(String id) { this.id = id; }
        public static AmmoType fromString(String str) {
            for (AmmoType type : AmmoType.values()) if (type.id.equals(str)) return type;
            return NORMAL;
        }
    }

    public TurretBulletEntity(EntityType<? extends AbstractArrow> type, Level level) {
        super(type, level);
    }

    public TurretBulletEntity(Level level, LivingEntity shooter) {
        super(ModEntities.TURRET_BULLET.get(), shooter, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    // --- СИНХРОНИЗАЦИЯ СПАВНА ---

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        Vec3 motion = this.getDeltaMovement();
        buffer.writeDouble(motion.x);
        buffer.writeDouble(motion.y);
        buffer.writeDouble(motion.z);
        buffer.writeDouble(this.getX());
        buffer.writeDouble(this.getY());
        buffer.writeDouble(this.getZ());
        buffer.writeFloat(this.getYRot());
        buffer.writeFloat(this.getXRot());
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        double vx = additionalData.readDouble();
        double vy = additionalData.readDouble();
        double vz = additionalData.readDouble();
        double x = additionalData.readDouble();
        double y = additionalData.readDouble();
        double z = additionalData.readDouble();
        float rotY = additionalData.readFloat();
        float rotX = additionalData.readFloat();

        this.setPos(x, y, z);
        this.setDeltaMovement(vx, vy, vz);

        // Принудительно устанавливаем поворот
        this.setYRot(rotY);
        this.setXRot(rotX);

        // КЛЮЧЕВОЙ МОМЕНТ:
        // Сбрасываем "предыдущие" значения поворота и позиции в текущие.
        // Это отключает интерполяцию для первого кадра отрисовки.
        this.yRotO = rotY;
        this.xRotO = rotX;
        this.xo = x;
        this.yo = y;
        this.zo = z;

        // Дополнительный пересчет на случай рассинхрона
        this.alignToVelocity();
    }
    // ----------------------------

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(AMMO_ID, "default");
        this.entityData.define(AMMO_TYPE, "normal");
    }

    public void setAmmoType(AmmoRegistry.AmmoType ammoType) {
        if (ammoType == null) return;
        this.baseDamage = ammoType.damage;
        this.baseSpeed = ammoType.speed;
        this.entityData.set(AMMO_ID, ammoType.id);

        if (ammoType.id.contains("piercing")) {
            this.ammoType = AmmoType.PIERCING;
            this.entityData.set(AMMO_TYPE, "piercing");
        } else if (ammoType.id.contains("hollow")) {
            this.ammoType = AmmoType.HOLLOW;
            this.entityData.set(AMMO_TYPE, "hollow");
        } else if (ammoType.id.contains("fire") || ammoType.id.contains("incendiary")) {
            this.ammoType = AmmoType.INCENDIARY;
            this.entityData.set(AMMO_TYPE, "incendiary");
        } else {
            this.ammoType = AmmoType.NORMAL;
            this.entityData.set(AMMO_TYPE, "normal");
        }

        this.setPierceLevel((byte) 0);
        this.setBaseDamage(baseDamage);
    }

    public String getAmmoId() { return this.entityData.get(AMMO_ID); }
    public AmmoType getAmmoType() { return AmmoType.fromString(this.entityData.get(AMMO_TYPE)); }

    public void setBallisticTrajectory(Vec3 startPos, Vec3 velocity) {
        this.setPos(startPos.x, startPos.y, startPos.z);
        this.setDeltaMovement(velocity);
        this.initialSpeed = (float) velocity.length();
        this.initialPosition = startPos;
        this.alignToVelocity();
    }

    public void shootBallisticFromRotation(LivingEntity shooter, float pitch, float yaw, float rollOffset, float speed, float divergence) {
        Vec3 lookDir = getLookDirFromRotation(pitch, yaw);
        if (divergence > 0) lookDir = addDispersion(lookDir, divergence);
        Vec3 velocity = lookDir.scale(speed);

        double startX = shooter.getX();
        double startY = shooter.getEyeY() - 0.1;
        double startZ = shooter.getZ();

        Vec3 offset = lookDir.normalize().scale(0.5);
        Vec3 startPos = new Vec3(startX, startY, startZ).add(offset);

        setBallisticTrajectory(startPos, velocity);
    }

    private static Vec3 getLookDirFromRotation(float pitch, float yaw) {
        float pitchRad = pitch * ((float) Math.PI / 180.0F);
        float yawRad = yaw * ((float) Math.PI / 180.0F);
        return new Vec3(-Math.sin(yawRad) * Math.cos(pitchRad), -Math.sin(pitchRad), Math.cos(yawRad) * Math.cos(pitchRad));
    }

    private Vec3 addDispersion(Vec3 baseDir, float divergence) {
        Vec3 normalized = baseDir.normalize();
        double dx = normalized.x + (this.random.nextGaussian() * divergence * 0.1);
        double dy = normalized.y + (this.random.nextGaussian() * divergence * 0.1);
        double dz = normalized.z + (this.random.nextGaussian() * divergence * 0.1);
        return new Vec3(dx, dy, dz).normalize().scale(baseDir.length());
    }

    @Override
    public void tick() {
        if (this.isRemoved() || this.inGround) {
            this.discard();
            return;
        }

        this.spin += 20.0F;

        if (initialPosition != null && this.position().distanceTo(initialPosition) > MAX_FLIGHT_DISTANCE) {
            this.discard();
            return;
        }

        if (this.tickCount > 200) {
            this.discard();
            return;
        }

        Vec3 start = this.position();
        Vec3 vel = this.getDeltaMovement();
        Vec3 end = start.add(vel);

        HitResult hit = traceHit(start, end);

        if (hit.getType() != HitResult.Type.MISS) {
            this.setPos(hit.getLocation().x, hit.getLocation().y, hit.getLocation().z);
            handleHitResult(hit);
            return;
        }

        this.setPos(end.x, end.y, end.z);

        vel = vel.scale(AIR_RESISTANCE).add(0.0, -BULLET_GRAVITY, 0.0);
        this.setDeltaMovement(vel);

        this.alignToVelocity();
    }

    // Исправленная математика выравнивания
    public void alignToVelocity() {
        Vec3 velocity = this.getDeltaMovement();
        double horizontalDist = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

        // В Minecraft Yaw считается от оси Z (South), а atan2 возвращает от оси X.
        // Поэтому atan2(x, z) - правильно для Minecraft.
        this.setYRot((float) (Math.atan2(velocity.x, velocity.z) * (180D / Math.PI)));

        // Pitch (XRot) считается от горизонтали.
        this.setXRot((float) (Math.atan2(velocity.y, horizontalDist) * (180D / Math.PI)));

        // Принудительно обновляем prevRotation, чтобы не было дергания при тике
        if (this.tickCount == 0) {
            this.yRotO = this.getYRot();
            this.xRotO = this.getXRot();
        }
    }

    @Override
    public void lerpMotion(double x, double y, double z) {
        super.lerpMotion(x, y, z);
        // Обновляем поворот при обновлении вектора движения сервером
        this.alignToVelocity();
    }

    private void handleHitResult(HitResult hit) {
        if (hit.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) hit;
            handleEntityHit(entityHit.getEntity());
        } else if (hit.getType() == HitResult.Type.BLOCK) {
            this.onHitBlock((BlockHitResult) hit);
        }
    }

    private void handleEntityHit(Entity target) {
        if (!(target instanceof LivingEntity livingTarget)) return;

        AmmoType currentType = getAmmoType();
        float finalDamage = calculateDamage(livingTarget, currentType);
        Entity owner = this.getOwner();

        DamageSource source = this.damageSources().mobProjectile(this, (LivingEntity) owner);

        if (target.hurt(source, finalDamage)) {
            applySpecialEffect(livingTarget, currentType);
            playHitSound();
            this.discard();
        }
    }

    private HitResult traceHit(Vec3 start, Vec3 end) {
        HitResult blockHit = this.level().clip(new ClipContext(
                start, end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        ));

        Vec3 endForEntities = end;
        if (blockHit.getType() != HitResult.Type.MISS) {
            endForEntities = blockHit.getLocation();
        }

        AABB sweep = this.getBoundingBox().expandTowards(end.subtract(start)).inflate(0.5);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                this.level(), this,
                start, endForEntities,
                sweep,
                e -> e.isAlive() && e != this.getOwner() && e.isPickable()
        );

        if (entityHit != null) return entityHit;
        return blockHit;
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!this.level().isClientSide) {
            BlockState state = this.level().getBlockState(result.getBlockPos());
            if (state.getBlock() instanceof AbstractGlassBlock) {
                this.level().destroyBlock(result.getBlockPos(), true);
            }
            playHitSound();
            this.discard();
        }
    }

    private float calculateDamage(LivingEntity target, AmmoType type) {
        float armor = (float) target.getArmorValue();
        switch (type) {
            case PIERCING: return calculatePiercingDamage(armor);
            case HOLLOW: return calculateHollowDamage(armor);
            case INCENDIARY: return calculateIncendiaryDamage(armor);
            default: return Math.max(baseDamage * (1.0f - armor * 0.02f), baseDamage * 0.4f);
        }
    }

    private float calculatePiercingDamage(float armor) {
        float armorPenetration = Math.min(35f, (baseDamage * 1.5f) + (baseSpeed * 3f));
        float armorEffectiveness = 1f - (armor / (armor + 40f));
        float damage = baseDamage * (1f + armorPenetration / 100f);
        return Math.max(damage * armorEffectiveness, baseDamage * 0.6f);
    }

    private float calculateHollowDamage(float armor) {
        float maxArmorEstimate = 20f;
        float armorMultiplier = Math.max(0.75f, 2f - (armor / maxArmorEstimate) * 1.0f);
        return baseDamage * armorMultiplier;
    }

    private float calculateIncendiaryDamage(float armor) {
        return Math.max(baseDamage * (1.0f - armor * 0.015f), baseDamage * 0.5f);
    }

    private void applySpecialEffect(LivingEntity target, AmmoType type) {
        if (type == AmmoType.INCENDIARY) target.setSecondsOnFire(5);
    }

    private void playHitSound() {
        if (ModSounds.BULLET_HIT1.isPresent()) {
            this.playSound(ModSounds.BULLET_HIT1.get(), 0.6F, 0.9F + this.random.nextFloat() * 0.2F);
        } else {
            this.playSound(SoundEvents.GENERIC_HURT, 0.5F, 1.0F);
        }
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return ModSounds.BULLET_HIT1.isPresent() ? ModSounds.BULLET_HIT1.get() : SoundEvents.ARROW_HIT;
    }

    @Override
    protected ItemStack getPickupItem() { return ItemStack.EMPTY; }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.initialPosition != null) {
            tag.putDouble("InitialX", this.initialPosition.x);
            tag.putDouble("InitialY", this.initialPosition.y);
            tag.putDouble("InitialZ", this.initialPosition.z);
        }
        tag.putFloat("InitialSpeed", this.initialSpeed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("InitialX")) {
            this.initialPosition = new Vec3(
                    tag.getDouble("InitialX"),
                    tag.getDouble("InitialY"),
                    tag.getDouble("InitialZ")
            );
        }
        this.initialSpeed = tag.getFloat("InitialSpeed");
    }
}
