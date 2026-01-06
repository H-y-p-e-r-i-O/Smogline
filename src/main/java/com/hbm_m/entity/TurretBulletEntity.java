package com.hbm_m.entity;

import com.hbm_m.main.MainRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class TurretBulletEntity extends ThrowableProjectile implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private float damage = 4.0f; // Урон от пули

    public TurretBulletEntity(EntityType<? extends ThrowableProjectile> type, Level level) {
        super(type, level);
    }

    public TurretBulletEntity(Level level, LivingEntity shooter) {
        super(ModEntities.TURRET_BULLET.get(), shooter, level);
    }

    @Override
    protected void defineSynchedData() {
        // Если понадобятся данные для синхронизации, добавляй сюда
    }

    @Override
    public void tick() {
        super.tick();

        // Если пуля живет слишком долго - убиваем (защита от лагов)
        if (this.tickCount > 200) {
            this.discard();
        }

        // Частицы дыма или огня для красоты (опционально)
        if (this.level().isClientSide) {
            // Можно добавить trail, если захочешь
            // this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }
    }

    // Настройка гравитации
    @Override
    protected float getGravity() {
        return 0.01F; // Очень слабая гравитация для настильной стрельбы
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();

        // Наносим урон
        target.hurt(this.damageSources().thrown(this, this.getOwner()), damage);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            // Звук попадания или эффекты можно добавить здесь
            this.discard(); // Удаляем пулю при попадании
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    // --- GeckoLib ---
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Пуле не нужны сложные контроллеры анимации, она просто летит
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
