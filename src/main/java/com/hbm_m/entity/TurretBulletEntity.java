package com.hbm_m.entity;

import com.hbm_m.sound.ModSounds;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
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
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class TurretBulletEntity extends ThrowableProjectile implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private float damage = 6.0f;

    public TurretBulletEntity(EntityType<? extends ThrowableProjectile> type, Level level) {
        super(type, level);
    }

    public TurretBulletEntity(Level level, LivingEntity shooter) {
        super(ModEntities.TURRET_BULLET.get(), shooter, level);
    }

    @Override
    public void tick() {
        // 1. ВАЖНО: Сохраняем предыдущие значения ДО любых изменений
        // Это обеспечивает плавную интерполяцию (lerp) в рендерере
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();

        super.tick();

        // 2. Обновляем вращение по вектору движения
        updateRotationByMotion();

        // Удаляем старый код, где xRotO присваивался в конце метода!
    }

    /**
     * Метод для синхронизации поворота модели с вектором движения.
     * Вызывайте его в tick(), а также СРАЗУ ПОСЛЕ создания пули и задания ей скорости.
     */
    public void updateRotationByMotion() {
        Vec3 motion = this.getDeltaMovement();

        // Если пуля почти стоит, не крутим её, чтобы не сбить ориентацию
        if (motion.lengthSqr() > 1.0E-7D) {
            double horizontalDist = Math.sqrt(motion.x * motion.x + motion.z * motion.z);

            // Minecraft использует градусы, atan2 возвращает радианы
            float yaw = (float) (Mth.atan2(motion.x, motion.z) * (180.0D / Math.PI));
            float pitch = (float) (Mth.atan2(motion.y, horizontalDist) * (180.0D / Math.PI));

            // setRot автоматически обновляет YRot и XRot
            this.setRot(yaw, pitch);
        }
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected float getGravity() {
        return 0.01F;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        target.hurt(this.damageSources().thrown(this, this.getOwner()), damage);

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
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) this.discard();
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}