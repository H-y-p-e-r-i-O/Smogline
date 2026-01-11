package com.hbm_m.entity;

import com.hbm_m.item.tags_and_tiers.AmmoRegistry;
import com.hbm_m.sound.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

    // Синхронизируемые данные
    private static final EntityDataAccessor<String> AMMO_ID = SynchedEntityData.defineId(TurretBulletEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> AMMO_TYPE = SynchedEntityData.defineId(TurretBulletEntity.class, EntityDataSerializers.STRING);

    // Локальные поля для расчетов
    public static final float BULLET_GRAVITY = 0.01F; // как в TurretLightEntity
    private float baseDamage = 4.0f;
    private float baseSpeed = 3.0f;
    private AmmoType ammoType = AmmoType.NORMAL;

    // === ENUM ДЛЯ ТИПОВ БОЕПРИПАСОВ ===
    public enum AmmoType {
        NORMAL("normal"),           // Обычная пуля
        PIERCING("piercing"),       // Пробивная - игнорирует часть брони
        HOLLOW("hollow"),           // Экспансивная (hollow point) - чем больше брони, тем меньше урона
        INCENDIARY("incendiary");   // Зажигательная - поджигает врага

        public final String id;

        AmmoType(String id) {
            this.id = id;
        }

        public static AmmoType fromString(String str) {
            for (AmmoType type : AmmoType.values()) {
                if (type.id.equals(str)) return type;
            }
            return NORMAL;
        }
    }

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
        this.entityData.define(AMMO_TYPE, "normal");
    }

    // === НАСТРОЙКА ===
    public void setAmmoType(AmmoRegistry.AmmoType ammoType) {
        if (ammoType == null) return;

        this.baseDamage = ammoType.damage;
        this.baseSpeed = ammoType.speed;
        this.entityData.set(AMMO_ID, ammoType.id);

        // Определяем тип боеприпаса по его свойствам
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
    }

    public String getAmmoId() {
        return this.entityData.get(AMMO_ID);
    }

    public AmmoType getAmmoType() {
        return AmmoType.fromString(this.entityData.get(AMMO_TYPE));
    }

    // === ЛОГИКА ===
    // НЕ override, просто своя обёртка
    protected float getBulletGravity() {
        return BULLET_GRAVITY;
    }

    @Override
    public void tick() {
        super.tick();

        // своя гравитация поверх ванильной
        if (!this.isNoGravity()) {
            Vec3 motion = this.getDeltaMovement();
            this.setDeltaMovement(motion.x, motion.y - getBulletGravity(), motion.z);
        }

        if (this.tickCount > 200 || this.inGround) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        if (!(target instanceof LivingEntity livingTarget)) {
            this.discard();
            return;
        }

        // Получаем актуальный тип боеприпаса
        AmmoType currentType = getAmmoType();

        // Рассчитываем финальный урон в зависимости от типа
        float finalDamage = calculateDamage(livingTarget, currentType);

        Entity owner = this.getOwner();
        DamageSource source = this.damageSources().arrow(this, owner);

        if (target.hurt(source, finalDamage)) {
            if (!this.level().isClientSide) {
                // Применяем специальные эффекты в зависимости от типа
                applySpecialEffect(livingTarget, currentType);
                playHitSound();
            }
        } else {
            // Рикошет от брони
            this.setDeltaMovement(this.getDeltaMovement().scale(-0.1));
        }

        this.discard();
    }

    /**
     * Рассчитывает финальный урон в зависимости от типа боеприпаса и брони цели
     */
    // Внутри TurretBulletEntity

    private float calculateDamage(LivingEntity target, AmmoType type) {
        float armor = (float) target.getArmorValue(); // ванильный метод брони [web:14]

        switch (type) {
            case PIERCING:
                return calculatePiercingDamage(armor);
            case HOLLOW:
                return calculateHollowDamage(armor);
            case INCENDIARY:
                return calculateIncendiaryDamage(armor);
            default:
                // NORMAL: -2% урона за 1 броню, минимум 40% базового
                return Math.max(baseDamage * (1.0f - armor * 0.02f), baseDamage * 0.4f);
        }
    }

    /**
     * ПРОБИВНАЯ: мягче броня и пробитие
     */
    private float calculatePiercingDamage(float armor) {
        // Макс +35% урона вместо 50%
        float armorPenetration = Math.min(35f, (baseDamage * 1.5f) + (baseSpeed * 3f));

        // Эффективность брони делаем слабее: armor/(armor+40) вместо 25
        float armorEffectiveness = 1f - (armor / (armor + 40f));

        float damage = baseDamage * (1f + armorPenetration / 100f);
        // Минимум 60% базового
        return Math.max(damage * armorEffectiveness, baseDamage * 0.6f);
    }

    /**
     * ЭКСПАНСИВНАЯ (hollow): х2 по голому, но падение мягче
     */
    private float calculateHollowDamage(float armor) {
        float maxArmorEstimate = 20f;
        // Было: 2f - (armor/max)*1.5  → делаем 1.0
        float armorMultiplier = Math.max(0.75f, 2f - (armor / maxArmorEstimate) * 1.0f);
        return baseDamage * armorMultiplier;
    }

    /**
     * ЗАЖИГАТЕЛЬНАЯ: ещё мягче штраф брони
     */
    private float calculateIncendiaryDamage(float armor) {
        // -1.5% за единицу брони, минимум 50% базового
        return Math.max(baseDamage * (1.0f - armor * 0.015f), baseDamage * 0.5f);
    }


    /**
     * Применяет специальные эффекты в зависимости от типа боеприпаса
     */
    private void applySpecialEffect(LivingEntity target, AmmoType type) {
        switch (type) {
            case INCENDIARY:
                // Поджигаем врага на 5 секунд (100 тиков)
                target.setSecondsOnFire(5);
                break;

            case PIERCING:
                // Опционально: можем добавить звуковой эффект пробитого
                break;

            case HOLLOW:
                // Опционально: можем добавить частицы кровотечения или отбросить врага
                break;

            default:
                break;
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!this.level().isClientSide) {
            BlockState state = this.level().getBlockState(result.getBlockPos());

            // Стекло ломаем
            if (state.getBlock() instanceof AbstractGlassBlock) {
                this.level().destroyBlock(result.getBlockPos(), true);
            }

            // Наш звук удара по блоку — только если SoundEvent реально есть
            if (ModSounds.BULLET_HIT1.isPresent()) {
                SoundEvent hit = ModSounds.BULLET_HIT1.get();
                this.level().playSound(
                        null,                               // null = слышно всем игрокам
                        this.getX(), this.getY(), this.getZ(),
                        hit,
                        SoundSource.PLAYERS,               // или SoundSource.BLOCKS/NEUTRAL по вкусу
                        0.6F,
                        0.9F + this.random.nextFloat() * 0.2F
                );
            }
        }

        this.discard();
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
        tag.putString("AmmoType", this.entityData.get(AMMO_TYPE));
        tag.putFloat("BaseDamage", this.baseDamage);
        tag.putFloat("BaseSpeed", this.baseSpeed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("AmmoID")) this.entityData.set(AMMO_ID, tag.getString("AmmoID"));
        if (tag.contains("AmmoType")) this.entityData.set(AMMO_TYPE, tag.getString("AmmoType"));
        if (tag.contains("BaseDamage")) this.baseDamage = tag.getFloat("BaseDamage");
        if (tag.contains("BaseSpeed")) this.baseSpeed = tag.getFloat("BaseSpeed");
    }

    // Звук при попадании по существу
    private void playHitSound() {
        if (ModSounds.BULLET_HIT1.isPresent()) {
            this.playSound(
                    ModSounds.BULLET_HIT1.get(),
                    0.6F,
                    0.9F + this.random.nextFloat() * 0.2F
            );
        } else {
            // Фоллбек, если по какой‑то причине звук не зарегистрировался
            this.playSound(SoundEvents.GENERIC_HURT, 0.5F, 1.0F);
        }
    }

    // Выключаем дефолтный «звук стрелы о землю»
    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        // Возвращаем НЕ null, а, например, тот же наш звук — или любой другой гарантированно живой
        // Если хочешь именно тишину — можно вернуть SoundEvents.EMPTY, но не null.
        return ModSounds.BULLET_HIT1.isPresent()
                ? ModSounds.BULLET_HIT1.get()
                : SoundEvents.EMPTY;
    }

    // === GECKOLIB ===
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
