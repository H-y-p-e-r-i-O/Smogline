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
import net.minecraft.world.entity.MoverType;
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
    private static final EntityDataAccessor AMMO_ID = SynchedEntityData.defineId(TurretBulletEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor AMMO_TYPE = SynchedEntityData.defineId(TurretBulletEntity.class, EntityDataSerializers.STRING);

    // === БАЛЛИСТИКА ===
    // Гравитация в блоках/тик² (примерно 9.8 м/с² переведено в Майнкрафт масштаб)
    public static final float BULLET_GRAVITY = 0.01F;

    // Сопротивление воздуха (множитель скорости каждый тик, от 0 до 1)
    public static final float AIR_RESISTANCE = 0.99F;

    // Максимальное расстояние полёта (в блоках)
    public static final float MAX_FLIGHT_DISTANCE = 256.0F;
    // Константы из расчётов турели
    // Из турели
    public static final float BULLET_DRAG = 0.02F;         // Сопротивление воздуха из турели
    // Локальные поля
    private float baseDamage = 4.0f;
    private float baseSpeed = 3.0f;
    private AmmoType ammoType = AmmoType.NORMAL;
    private float initialSpeed = 0.0f; // Для расчёта расстояния
    private Vec3 initialPosition = null; // Стартовая позиция для отслеживания дальности

    // === ENUM ДЛЯ ТИПОВ БОЕПРИПАСОВ ===
    public enum AmmoType {
        NORMAL("normal"),
        PIERCING("piercing"),
        HOLLOW("hollow"),
        INCENDIARY("incendiary");

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
        this.noPhysics = true; // ✅ Нужен для баллистики 0.01
    }


    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(AMMO_ID, "default");
        this.entityData.define(AMMO_TYPE, "normal");
    }

    // === НАСТРОЙКА БОЕПРИПАСА ===
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
        this.setPierceLevel((byte) 0); // ✅ Нет протыкания
        this.setBaseDamage(baseDamage); // ✅ Базовый урон стрелы включён
    }

    public String getAmmoId() {
        return (String) this.entityData.get(AMMO_ID);
    }

    public AmmoType getAmmoType() {
        return AmmoType.fromString((String) this.entityData.get(AMMO_TYPE));
    }

    // === БАЛЛИСТИКА И ФИЗИКА ===

    /**
     * Инициализирует пулю с начальной позицией и вектором скорости.
     * Используется вместо shootFromRotation() для полного контроля баллистики.
     *
     * @param startPos  Начальная позиция пули
     * @param velocity  Вектор скорости (x, y, z)
     */
    public void setBallisticTrajectory(Vec3 startPos, Vec3 velocity) {
        this.setPos(startPos.x, startPos.y, startPos.z);
        this.setDeltaMovement(velocity);

        // Сохраняем начальную скорость для расчёта дальности
        this.initialSpeed = (float) velocity.length();
        this.initialPosition = startPos;

        // Синхронизируем позицию на клиенте
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
    }

    /**
     * Альтернатива: инициализирует пулю от оружия с углами поворота.
     * Более удобно использовать, если в MachineGunItem уже есть углы.
     *
     * @param shooter    Стрелок (для получения позиции)
     * @param pitch      Угол наклона (ось X, вверх-вниз)
     * @param yaw        Угол поворота (ось Y, влево-вправо)
     * @param rollOffset Угол крена (обычно 0)
     * @param speed      Начальная скорость
     * @param divergence Разброс в радианах (обычно 0.05-0.1)
     */
    public void shootBallisticFromRotation(LivingEntity shooter, float pitch, float yaw, float rollOffset, float speed, float divergence) {
        Vec3 lookDir = getLookDirFromRotation(pitch, yaw);

        // Разброс как в турели (очень маленький)
        if (divergence > 0) {
            lookDir = addDispersion(lookDir, divergence);
        }

        Vec3 velocity = lookDir.scale(speed);

        // Точка вылета как у турели (из центра оружия)
        double startX = shooter.getX() + 0.3;  // Справа от центра
        double startY = shooter.getY() + 1.2;  // Грудь
        double startZ = shooter.getZ();

        Vec3 lookNorm = lookDir.normalize();
        startX += lookNorm.x * 0.4;
        startY += lookNorm.y * 0.4;
        startZ += lookNorm.z * 0.4;

        Vec3 startPos = new Vec3(startX, startY, startZ);

        setBallisticTrajectory(startPos, velocity);
    }


    /**
     * Вычисляет вектор направления из углов поворота.
     */
    private static Vec3 getLookDirFromRotation(float pitch, float yaw) {
        float pitchRad = pitch * ((float) Math.PI / 180.0F);
        float yawRad = yaw * ((float) Math.PI / 180.0F);

        float cosP = (float) Math.cos(pitchRad);
        float sinP = (float) Math.sin(pitchRad);
        float cosY = (float) Math.cos(yawRad);
        float sinY = (float) Math.sin(yawRad);

        return new Vec3(-sinY * cosP, -sinP, cosY * cosP);
    }

    /**
     * Добавляет случайный разброс к вектору направления.
     */
    private Vec3 addDispersion(Vec3 baseDir, float divergence) {
        // Нормализуем и создаём два перпендикулярных вектора
        Vec3 normalized = baseDir.normalize();

        // Случайные углы в конусе с половинным углом divergence
        float angle1 = (this.random.nextFloat() - 0.5f) * divergence;
        float angle2 = (this.random.nextFloat() - 0.5f) * divergence;

        // Простой способ: добавляем случайные компоненты
        double dx = normalized.x + (this.random.nextGaussian() * 0.01);
        double dy = normalized.y + (this.random.nextGaussian() * 0.01);
        double dz = normalized.z + (this.random.nextGaussian() * 0.01);

        return new Vec3(dx, dy, dz).normalize().scale(baseDir.length());
    }

    @Override
    public void tick() {
        super.tick();

        if (this.inGround || this.isRemoved()) {
            this.discard();
            return;
        }

        // === РУЧНАЯ ПРОВЕРКА СТОЛКНОВЕНИЙ (noPhysics её отключает) ===
        this.checkInsideBlocks();

        // ✅ Raycast мобов в радиусе 0.5 блока
        this.level().getEntities(this,
                        this.getBoundingBox().inflate(0.5),
                        Entity::isAlive)
                .stream()
                .filter(e -> e != this.getOwner())
                .forEach(this::handleEntityHit);

        // === БАЛЛИСТИКА ТУРЕЛИ ===
        Vec3 motion = this.getDeltaMovement();
        motion = new Vec3(motion.x, motion.y - BULLET_GRAVITY, motion.z);
        motion = motion.scale(1.0F - BULLET_DRAG);

        this.setDeltaMovement(motion);
        this.move(MoverType.SELF, motion);

        // Проверки
        if (initialPosition != null && this.position().distanceTo(initialPosition) > MAX_FLIGHT_DISTANCE) {
            this.discard();
        }
        if (this.tickCount > 200) {
            this.discard();
        }
    }

    /**
     * Обрабатывает попадание по мобу (замена onHitEntity)
     */
    private void handleEntityHit(Entity target) {
        if (!(target instanceof LivingEntity livingTarget)) return;

        AmmoType currentType = getAmmoType();
        float finalDamage = calculateDamage(livingTarget, currentType);

        Entity owner = this.getOwner();
        DamageSource source = this.damageSources().arrow(this, owner);

        if (target.hurt(source, finalDamage)) {
            applySpecialEffect(livingTarget, currentType);
            playHitSound();
        }

        this.discard(); // ✅ Удаляем пулю после попадания
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        // ✅ ВЫЗЫВАЕМ РОДИТЕЛЬСКИЙ МЕТОД ПЕРВЫМ!
        super.onHitEntity(result);

        Entity target = result.getEntity();
        if (!(target instanceof LivingEntity livingTarget)) {
            return;
        }

        // ТВОЯ ДОПОЛНИТЕЛЬНАЯ ЛОГИКА (спецэффекты)
        AmmoType currentType = getAmmoType();
        float finalDamage = calculateDamage(livingTarget, currentType);

        // ✅ ПРОВЕРЯЕМ, жив ли target после стандартного урона
        if (target.isAlive()) {
            Entity owner = this.getOwner();
            DamageSource source = this.damageSources().arrow(this, owner);

            if (target.hurt(source, finalDamage)) {
                applySpecialEffect(livingTarget, currentType);
                playHitSound();
            }
        }

        // ✅ НЕ discard здесь — let super.onHitEntity() обработать
    }


    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!this.level().isClientSide) {
            BlockState state = this.level().getBlockState(result.getBlockPos());

            // Стекло ломаем
            if (state.getBlock() instanceof AbstractGlassBlock) {
                this.level().destroyBlock(result.getBlockPos(), true);
            }

            // Звук удара
            if (ModSounds.BULLET_HIT1.isPresent()) {
                SoundEvent hit = ModSounds.BULLET_HIT1.get();
                this.level().playSound(
                        null,
                        this.getX(), this.getY(), this.getZ(),
                        hit,
                        SoundSource.PLAYERS,
                        0.6F,
                        0.9F + this.random.nextFloat() * 0.2F
                );
            }
        }

        this.discard();
    }

    // === РАСЧЁТ УРОНА ===

    private float calculateDamage(LivingEntity target, AmmoType type) {
        float armor = (float) target.getArmorValue();
        switch (type) {
            case PIERCING:
                return calculatePiercingDamage(armor);
            case HOLLOW:
                return calculateHollowDamage(armor);
            case INCENDIARY:
                return calculateIncendiaryDamage(armor);
            default:
                return Math.max(baseDamage * (1.0f - armor * 0.02f), baseDamage * 0.4f);
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
        switch (type) {
            case INCENDIARY:
                target.setSecondsOnFire(5);
                break;
            case PIERCING:
            case HOLLOW:
            default:
                break;
        }
    }

    private void playHitSound() {
        if (ModSounds.BULLET_HIT1.isPresent()) {
            this.playSound(
                    ModSounds.BULLET_HIT1.get(),
                    0.6F,
                    0.9F + this.random.nextFloat() * 0.2F
            );
        } else {
            this.playSound(SoundEvents.GENERIC_HURT, 0.5F, 1.0F);
        }
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return ModSounds.BULLET_HIT1.isPresent() ? ModSounds.BULLET_HIT1.get() : SoundEvents.EMPTY;
    }

    @Override
    protected ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }

    // === СОХРАНЕНИЕ ===
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("AmmoID", (String) this.entityData.get(AMMO_ID));
        tag.putString("AmmoType", (String) this.entityData.get(AMMO_TYPE));
        tag.putFloat("BaseDamage", this.baseDamage);
        tag.putFloat("BaseSpeed", this.baseSpeed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("AmmoID")) {
            this.entityData.set(AMMO_ID, tag.getString("AmmoID"));
        }
        if (tag.contains("AmmoType")) {
            this.entityData.set(AMMO_TYPE, tag.getString("AmmoType"));
        }
        if (tag.contains("BaseDamage")) {
            this.baseDamage = tag.getFloat("BaseDamage");
        }
        if (tag.contains("BaseSpeed")) {
            this.baseSpeed = tag.getFloat("BaseSpeed");
        }
    }

    // === GECKOLIB ===
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
