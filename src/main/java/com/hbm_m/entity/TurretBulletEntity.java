package com.hbm_m.entity;

import com.hbm_m.item.tags_and_tiers.AmmoRegistry;
import com.hbm_m.sound.ModSounds;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractGlassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class TurretBulletEntity extends AbstractArrow implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Только ID текстуры для клиента
    private static final EntityDataAccessor<String> AMMO_ID = SynchedEntityData.defineId(TurretBulletEntity.class, EntityDataSerializers.STRING);

    // Только базовый урон
    private float damage = 4.0f;

    public TurretBulletEntity(EntityType<? extends AbstractArrow> type, Level level) {
        super(type, level);
    }

    public TurretBulletEntity(Level level, LivingEntity shooter) {
        super(ModEntities.TURRET_BULLET.get(), shooter, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(AMMO_ID, "default");
    }

    // === НАСТРОЙКА ===
    public void setAmmoType(AmmoRegistry.AmmoType ammoType) {
        if (ammoType == null) return;
        this.entityData.set(AMMO_ID, ammoType.id);
        this.damage = ammoType.damage;

        // ВАЖНО: Принудительно отключаем ванильное пробитие, чтобы не было глитчей
        this.setPierceLevel((byte) 0);
    }

    public String getAmmoId() {
        return this.entityData.get(AMMO_ID);
    }

    // === ЛОГИКА ===
    @Override
    public void tick() {
        super.tick();

        // Простое удаление по времени или если коснулась земли
        if (this.tickCount > 200 || this.inGround) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        // Никакой логики типов патронов. Просто урон.
        Entity target = result.getEntity();
        float currentDamage = this.damage;

        Entity owner = this.getOwner();
        DamageSource source = this.damageSources().arrow(this, owner);

        if (target.hurt(source, currentDamage)) {
            if (!this.level().isClientSide) {
                playHitSound();
            }
            // Всегда удаляем после попадания.
            // Хочешь пробитие? Реализуй сам, не вызывая discard() здесь.
            this.discard();
        } else {
            // Рикошет от брони
            this.setDeltaMovement(this.getDeltaMovement().scale(-0.1));
            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide) {
            // Стекло бьем, в остальном - исчезаем
            BlockState state = this.level().getBlockState(result.getBlockPos());
            if (state.getBlock() instanceof AbstractGlassBlock) {
                this.level().destroyBlock(result.getBlockPos(), true);
            }
            this.discard();
        }
    }

    @Override
    protected ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }

    // === СОХРАНЕНИЕ ===
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("AmmoID", this.entityData.get(AMMO_ID));
        tag.putFloat("Damage", this.damage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("AmmoID")) this.entityData.set(AMMO_ID, tag.getString("AmmoID"));
        if (tag.contains("Damage")) this.damage = tag.getFloat("Damage");
    }

    // === ЗВУК ===
    private void playHitSound() {
        SoundEvent hitSound = ModSounds.BULLET_HIT1.isPresent() ? ModSounds.BULLET_HIT1.get() : SoundEvents.GENERIC_HURT;
        this.playSound(hitSound, 0.5F, 0.9F + this.random.nextFloat() * 0.2F);
    }

    // === GECKOLIB ===
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
