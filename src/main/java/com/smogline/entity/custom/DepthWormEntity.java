package com.smogline.entity.custom;

import com.smogline.entity.ModEntities;
import com.smogline.goal.DepthWormJumpGoal;
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

    public void setAttacking(boolean attacking) { this.entityData.set(IS_ATTACKING, attacking); }
    public boolean isAttacking() { return this.entityData.get(IS_ATTACKING); }

    @Override
    protected void registerGoals() {
        // Добавление целей: прыжок, ближний бой, движение, ИИ. Враждебен ко всем.
        this.goalSelector.addGoal(0, new DepthWormJumpGoal(this, 1.5D, 5.0F, 10.0F));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(0, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, LivingEntity.class, true, (e) -> e instanceof Monster));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private PlayState predicate(software.bernie.geckolib.core.animation.AnimationState<DepthWormEntity> state) {
        if (this.isAttacking()) {
            // Проигрываем анимацию подготовки
            state.getController().setAnimation(RawAnimation.begin().thenPlay("prepare"));
        } else if (state.isMoving()) {
            // Проигрываем анимацию скольжения при движении
            state.getController().setAnimation(RawAnimation.begin().thenLoop("slide"));
        } else {
            // Если стоит, можно либо оставить slide с низкой скоростью, либо остановить
            state.getController().setAnimation(RawAnimation.begin().thenLoop("slide"));
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }
}