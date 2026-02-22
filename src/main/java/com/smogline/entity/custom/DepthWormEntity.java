package com.smogline.entity.custom;

import com.smogline.block.entity.custom.DepthWormNestBlockEntity;
import com.smogline.entity.ModEntities;
import com.smogline.goal.DepthWormJumpGoal;
import com.smogline.goal.EnterNestGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

// Класс сущности с Geckolib для анимаций, смены текстур и ИИ
public class DepthWormEntity extends Monster implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final EntityDataAccessor<Boolean> IS_ATTACKING = SynchedEntityData.defineId(DepthWormEntity.class, EntityDataSerializers.BOOLEAN);
    public int ignoreFallDamageTicks = 0;
    public int homeSickTimer = 0; // Таймер, запрещающий вход
    public boolean isFlying = false;
    public DepthWormEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 15.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IS_ATTACKING, false);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        // Если мы в полете и коснулись цели
        if (this.isFlying && !this.onGround()) {
            LivingEntity target = this.getTarget();
            if (target != null && this.getBoundingBox().inflate(0.2).intersects(target.getBoundingBox())) {
                target.hurt(this.damageSources().mobAttack(this), 10.0F); // Урон 5 сердец
                this.isFlying = false; // Выключаем режим полета после удара

                // Небольшой отскок назад после удара
                this.setDeltaMovement(this.getDeltaMovement().multiply(-0.2, 0.1, -0.2));
            }
        }

        if (!level().isClientSide) {
            // 1. Проверяем валидность текущего гнезда
            if (this.nestPos != null) {
                if (!(level().getBlockEntity(this.nestPos) instanceof DepthWormNestBlockEntity nest) || nest.isFull()) {
                    this.nestPos = null; // Гнездо сломано или переполнено, ищем новое
                }
            }

            // 2. Поиск ближайшего гнезда (раз в 5 секунд)
            if (this.nestPos == null && level().getGameTime() % 100 == 0) {
                // Используем встроенный поиск по области
                Iterable<BlockPos> ps = BlockPos.betweenClosed(
                        blockPosition().offset(-16, -8, -16),
                        blockPosition().offset(16, 8, 16)
                );
                for (BlockPos p : ps) {
                    if (level().getBlockEntity(p) instanceof DepthWormNestBlockEntity nest && !nest.isFull()) {
                        this.nestPos = p.immutable();
                        break;
                    }
                }
            }
        }
        if (homeSickTimer > 0) homeSickTimer--;
    }

    // Защита от урона при падении
    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, net.minecraft.world.damagesource.DamageSource source) {
        if (this.ignoreFallDamageTicks > 0) return false;
        return super.causeFallDamage(distance, damageMultiplier, source);
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        // Игнорируем урон от удушья в стене
        if (source.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    public void setAttacking(boolean attacking) { this.entityData.set(IS_ATTACKING, attacking); }
    public boolean isAttacking() { return this.entityData.get(IS_ATTACKING); }

    @Override
    protected void registerGoals() {
        // Приоритет 0 - самое важное
        this.goalSelector.addGoal(0, new DepthWormJumpGoal(this, 1.5D, 5.0F, 10.0F));

        // Приоритет 1 - если нет врага, бегом домой!
        this.goalSelector.addGoal(1, new EnterNestGoal(this));

        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8D));



    // Цель 1: Игроки (приоритет)
        this.targetSelector.addGoal(0, new NearestAttackableTargetGoal<>(this, Player.class, true));

        // Цель 2: Другие мобы, НО НЕ черви
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, LivingEntity.class, true, (target) -> {
            // Условие: цель жива И это НЕ такой же червь
            return target.isAlive() && !(target instanceof DepthWormEntity);

        }));

        // Добавь это в registerGoals после NearestAttackableTargetGoal
        this.targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this).setAlertOthers());
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        // Запрещаем самому себе выбирать целью другого червя даже при получении урона
        if (target instanceof DepthWormEntity) {
            return false;
        }
        return super.canAttack(target);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private PlayState predicate(software.bernie.geckolib.core.animation.AnimationState<DepthWormEntity> state) {
        // Если идет подготовка к прыжку (1.5 сек)
        if (this.isAttacking()) {
            return state.setAndContinue(RawAnimation.begin().thenPlay("prepare"));
        }

        // Если червь просто ползет
        if (state.isMoving()) {
            state.getController().setAnimation(RawAnimation.begin().thenLoop("slide"));
            return PlayState.CONTINUE;
        }

        return PlayState.STOP;
    }


    // УРОН ПРИ СТОЛКНОВЕНИИ (Прыжок в голову)
    @Override
    public void push(net.minecraft.world.entity.Entity entity) {
        super.push(entity);
        // Если червь в воздухе, атакует и столкнулся с живой целью
        if (this.isAttacking() && !this.onGround() && entity instanceof LivingEntity livingTarget) {
            if (livingTarget != this.getTarget()) return; // Бьем только свою цель

            float damage = 8.0F; // Большой урон при попадании
            livingTarget.hurt(this.damageSources().mobAttack(this), damage);

            // Отбрасываем цель немного назад или вниз
            double d0 = entity.getX() - this.getX();
            double d1 = entity.getZ() - this.getZ();
            livingTarget.knockback(0.5D, d0, d1);

            // После успешного попадания выключаем режим атаки, чтобы не спамить уроном
            this.setAttacking(false);
        }
    }
    public BlockPos nestPos;


    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }
}