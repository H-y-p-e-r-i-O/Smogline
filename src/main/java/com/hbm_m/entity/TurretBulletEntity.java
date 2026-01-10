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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class TurretBulletEntity extends ThrowableProjectile implements GeoEntity, IEntityAdditionalSpawnData {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Синхронизируемые данные
    private static final EntityDataAccessor<String> DATA_AMMO_ID =
            SynchedEntityData.defineId(TurretBulletEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> DATA_DAMAGE =
            SynchedEntityData.defineId(TurretBulletEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_IS_PIERCING =
            SynchedEntityData.defineId(TurretBulletEntity.class, EntityDataSerializers.BOOLEAN);

    public TurretBulletEntity(EntityType<? extends TurretBulletEntity> type, Level level) {
        super(type, level);
    }

    public TurretBulletEntity(Level level, LivingEntity shooter) {
        super(ModEntities.TURRET_BULLET.get(), shooter, level);
    }

    // === ПОВОРОТ И ДВИЖЕНИЕ ===

    @Override
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        super.shoot(x, y, z, velocity, inaccuracy);

        // СРАЗУ считаем поворот
        Vec3 motion = this.getDeltaMovement();
        double horizontalDist = Math.sqrt(motion.x * motion.x + motion.z * motion.z);

        // YAW: atan2(x, z)
        float yaw = (float)(Mth.atan2(motion.x, motion.z) * (180.0D / Math.PI));

        // PITCH: atan2(y, horizontalDist)
        // Обычно для снарядов Pitch положительный, если летит вверх? Нет, в MC Pitch вниз - положительный (90), вверх - отрицательный (-90).
        // motion.y > 0 (летит вверх) -> atan2 > 0.
        // Нам нужно setXRot = -atan2(...) если модель стандартная.
        // НО! Метод setXRot сам нормализует угол.
        // Стандартная формула Projectile:
        float pitch = (float)(Mth.atan2(motion.y, horizontalDist) * (180.0D / Math.PI));

        this.setYRot(yaw);
        this.setXRot(pitch);
        this.yRotO = yaw;
        this.xRotO = pitch;
    }

    @Override
    public void tick() {
        super.tick();
        // Обновляем поворот каждый тик (особенно важно на клиенте из-за гравитации)
        updateRotationByMotion();
    }

    /**
     * Рассчитывает Yaw и Pitch на основе текущего вектора движения.
     * Работает для всех 3-х осей.
     */
    public void updateRotationByMotion() {
        Vec3 motion = this.getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-7D) return;

        double horizontalDist = Math.sqrt(motion.x * motion.x + motion.z * motion.z);

        float yaw = (float)(Mth.atan2(motion.x, motion.z) * (180.0D / Math.PI));
        float pitch = (float)(Mth.atan2(motion.y, horizontalDist) * (180.0D / Math.PI));

        this.setYRot(yaw);
        this.setXRot(pitch);
    }

    // === СЕТЕВАЯ СИНХРОНИЗАЦИЯ (IEntityAdditionalSpawnData) ===

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        // Данные патрона
        buffer.writeUtf(getAmmoId());
        buffer.writeFloat(getDamage());
        buffer.writeBoolean(isPiercing());

        // Вектор скорости
        Vec3 motion = this.getDeltaMovement();
        buffer.writeDouble(motion.x);
        buffer.writeDouble(motion.y);
        buffer.writeDouble(motion.z);

        // Текущий поворот (чтобы клиент знал его сразу при спавне)
        buffer.writeFloat(this.getYRot());
        buffer.writeFloat(this.getXRot());
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        // Читаем данные патрона
        this.entityData.set(DATA_AMMO_ID, buffer.readUtf());
        this.entityData.set(DATA_DAMAGE, buffer.readFloat());
        this.entityData.set(DATA_IS_PIERCING, buffer.readBoolean());

        // Читаем и ставим скорость
        double vx = buffer.readDouble();
        double vy = buffer.readDouble();
        double vz = buffer.readDouble();
        this.setDeltaMovement(vx, vy, vz);

        // Читаем и ПРИМЕНЯЕМ поворот
        float yaw = buffer.readFloat();
        float pitch = buffer.readFloat();

        this.setYRot(yaw);
        this.setXRot(pitch);
        this.yRotO = yaw;
        this.xRotO = pitch;

        // Дополнительно обновляем повороты тела/головы (важно для некоторых моделей)
        this.setYBodyRot(yaw);
        this.setYHeadRot(yaw);
    }

    // === LITE ACCESSORS ===

    public void setAmmoType(AmmoRegistry.AmmoType ammoType) {
        if (ammoType == null) return;
        this.entityData.set(DATA_AMMO_ID, ammoType.id);
        this.entityData.set(DATA_DAMAGE, ammoType.damage);
        this.entityData.set(DATA_IS_PIERCING, ammoType.isPiercing);
    }

    public String getAmmoId() { return this.entityData.get(DATA_AMMO_ID); }
    public float getDamage() { return this.entityData.get(DATA_DAMAGE); }
    public boolean isPiercing() { return this.entityData.get(DATA_IS_PIERCING); }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_AMMO_ID, "");
        this.entityData.define(DATA_DAMAGE, 6.0f);
        this.entityData.define(DATA_IS_PIERCING, false);
    }

    @Override
    protected float getGravity() { return 0.01F; }

    // === ЛОГИКА ПОПАДАНИЯ ===

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        target.hurt(this.damageSources().thrown(this, this.getOwner()), getDamage());

        if (!this.level().isClientSide) {
            SoundEvent hitSound = null;
            if (this.random.nextBoolean()) {
                if (ModSounds.BULLET_HIT1.isPresent()) hitSound = ModSounds.BULLET_HIT1.get();
            } else {
                if (ModSounds.BULLET_HIT2.isPresent()) hitSound = ModSounds.BULLET_HIT2.get();
            }
            if (hitSound != null) {
                float pitch = 0.9F + this.random.nextFloat() * 0.2F;
                this.playSound(hitSound, 0.5F, pitch);
            }
        }

        // Если пуля не пробивная — исчезаем после первого попадания
        if (!isPiercing()) {
            this.discard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        // При ударе о блок всегда исчезаем
        if (!this.level().isClientSide) this.discard();
    }

    // === СИСТЕМНЫЕ МЕТОДЫ ===

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString("ammoId", getAmmoId());
        compound.putFloat("damage", getDamage());
        compound.putBoolean("isPiercing", isPiercing());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("ammoId")) {
            this.entityData.set(DATA_AMMO_ID, compound.getString("ammoId"));
            this.entityData.set(DATA_DAMAGE, compound.getFloat("damage"));
            this.entityData.set(DATA_IS_PIERCING, compound.getBoolean("isPiercing"));
        }
    }
}
