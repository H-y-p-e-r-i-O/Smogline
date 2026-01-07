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
    private float damage = 4.0f;

    // ПАРАМЕТРЫ НАКЛОНА (как в AirBomb)
    private static final float ROTATION_SPEED = 5.0F; // Градусов в секунду
    private float currentPitchOffset = 0.0F;

    public TurretBulletEntity(EntityType<? extends ThrowableProjectile> type, Level level) {
        super(type, level);
    }

    public TurretBulletEntity(Level level, LivingEntity shooter) {
        super(ModEntities.TURRET_BULLET.get(), shooter, level);
    }

    @Override
    public void tick() {
        super.tick();

        // --- УЛУЧШЕННАЯ ФИЗИКА ВРАЩЕНИЯ (как в авиаударе) ---
        if (!this.level().isClientSide) {
            Vec3 motion = this.getDeltaMovement();

            // Если пуля движется (а она движется)
            if (motion.lengthSqr() > 0.001D) {
                // 1. Поворот по горизонтали (YAW) - всегда по вектору движения
                float targetYaw = (float)(Mth.atan2(motion.x, motion.z) * (double)(180F / (float)Math.PI));
                this.setYRot(targetYaw);

                // 2. Поворот по вертикали (PITCH) - ПЛАВНЫЙ НАКЛОН ВНИЗ
                // Вместо того чтобы пуля смотрела строго по вектору (который быстро падает вниз),
                // мы делаем плавный наклон носа, как у реального снаряда.

                // Увеличиваем наклон каждый тик (20 тиков = 1 сек)
                // За 1 секунду (20 тиков) наклон увеличится на ROTATION_SPEED (5 градусов)
                currentPitchOffset += (ROTATION_SPEED / 20.0F);

                // Базовый угол от вектора скорости (чтобы старт был правильным)
                double horizontalDist = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
                float vectorPitch = (float)(Mth.atan2(motion.y, horizontalDist) * (double)(180F / (float)Math.PI));

                // Применяем наш плавный наклон
                this.setXRot(vectorPitch + currentPitchOffset);
            }
        }

        // Синхронизация для рендера
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();

        if (this.tickCount > 200) {
            this.discard();
        }
    }

    @Override
    protected void defineSynchedData() {}

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
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}