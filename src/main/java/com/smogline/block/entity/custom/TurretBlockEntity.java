package com.smogline.block.entity.custom;

import com.smogline.block.ModBlocks; // ЗАМЕНИ НА СВОЙ КЛАСС БЛОКОВ
import com.smogline.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity; // Добавлен импорт
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob; // Добавлен импорт
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.UUID;

public class TurretBlockEntity extends BlockEntity implements GeoBlockEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int deployTimer = 80;
    private int shootTimer = 0;
    private int shootAnimTimer = 0;

    private boolean isDeployed = false;
    private boolean isShooting = false;
    private UUID ownerUUID = null;

    private LivingEntity currentTarget = null;
    private int targetSearchCooldown = 0;
    private boolean hasTriggeredDeploy = false;

    // Углы вращения для модели
    public float rotationYaw = 0.0F;
    public float rotationPitch = 0.0F;

    public TurretBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_BLOCK_ENTITY.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TurretBlockEntity entity) {
        if (level == null) return;

        if (level.isClientSide) {
            if (!entity.hasTriggeredDeploy && entity.deployTimer > 60) {
                entity.triggerAnim("deploy_ctrl", "deploy_trigger");
                entity.hasTriggeredDeploy = true;
            }
            return;
        }

        // Серверная часть
        if (entity.deployTimer > 0) {
            entity.deployTimer--;
            if (entity.deployTimer == 0) {
                entity.isDeployed = true;
            }
        }

        if (entity.isDeployed) {
            entity.targetSearchCooldown--;
            if (entity.targetSearchCooldown <= 0) {
                entity.findTarget(level);
                entity.targetSearchCooldown = 10;
            }

            if (entity.currentTarget != null && entity.currentTarget.isAlive()) {
                entity.aimAtTarget(entity.currentTarget);
                entity.tryShoot(level, entity.currentTarget);
            } else {
                entity.currentTarget = null;
            }
        }

        if (entity.isShooting) {
            entity.shootAnimTimer++;
            if (entity.shootAnimTimer > 15) {
                entity.isShooting = false;
                entity.shootAnimTimer = 0;
            }
        }

        // Важно для синхронизации с клиентом (чтобы модель вращалась)
        if (level instanceof ServerLevel) {
            entity.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    private void findTarget(Level level) {
        AABB searchArea = new AABB(getBlockPos()).inflate(35); // Радиус 35 блоков

        // 1. Игроки
        List<Player> players = level.getEntitiesOfClass(
                Player.class,
                searchArea,
                p -> !p.isSpectator() && !isAlly(p)
        );

        if (!players.isEmpty()) {
            currentTarget = players.get(0);
            return;
        }

        // 2. Мобы (Mob)
        List<Mob> mobs = level.getEntitiesOfClass(
                Mob.class,
                searchArea,
                m -> m.isAlive() && !isAlly(m)
        );

        if (!mobs.isEmpty()) {
            currentTarget = mobs.get(0);
        }
    }

    // Принимаем общий Entity, чтобы подходило и для Player, и для Mob
    private boolean isAlly(Entity entity) {
        if (ownerUUID != null && entity instanceof Player player) {
            return ownerUUID.equals(player.getUUID());
        }
        return false;
    }

    private void aimAtTarget(LivingEntity target) {
        Vec3 fromPos = Vec3.atCenterOf(getBlockPos());
        Vec3 toPos = target.getEyePosition();
        Vec3 dir = toPos.subtract(fromPos).normalize();

        this.rotationYaw = (float) Math.atan2(dir.z, dir.x) * 57.2958F - 90.0F;
        float distance = (float) fromPos.distanceTo(toPos);
        this.rotationPitch = -(float) Math.asin(dir.y) * 57.2958F;
    }

    private void tryShoot(Level level, LivingEntity target) {
        shootTimer--;
        if (shootTimer <= 0) {
            Arrow arrow = new Arrow(level, getBlockPos().getX() + 0.5, getBlockPos().getY() + 0.5, getBlockPos().getZ() + 0.5);

            Vec3 targetEye = target.getEyePosition();
            Vec3 fromPos = Vec3.atCenterOf(getBlockPos());
            Vec3 dir = targetEye.subtract(fromPos).normalize();

            arrow.shoot(dir.x, dir.y, dir.z, 1.6F, 1.0F);
            level.addFreshEntity(arrow);

            isShooting = true;
            shootAnimTimer = 0;
            shootTimer = 10;
        }
    }

    public void setOwner(Player player) {
        this.ownerUUID = player.getUUID();
        setChanged();
    }

    // --- NBT СОХРАНЕНИЕ / ЗАГРУЗКА ---
    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("DeployTimer", deployTimer);
        tag.putBoolean("IsDeployed", isDeployed);
        tag.putBoolean("IsShooting", isShooting);
        tag.putFloat("RotationYaw", rotationYaw);
        tag.putFloat("RotationPitch", rotationPitch);
        if (ownerUUID != null) tag.putUUID("Owner", ownerUUID);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        deployTimer = tag.getInt("DeployTimer");
        isDeployed = tag.getBoolean("IsDeployed");
        isShooting = tag.getBoolean("IsShooting");
        rotationYaw = tag.getFloat("RotationYaw");
        rotationPitch = tag.getFloat("RotationPitch");
        if (tag.hasUUID("Owner")) ownerUUID = tag.getUUID("Owner");
    }

    // --- СЕТЕВАЯ СИНХРОНИЗАЦИЯ (ОЧЕНЬ ВАЖНО ДЛЯ БЛОКОВ) ---
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    // --- GECKOLIB ---
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "deploy_ctrl", 0, event -> PlayState.STOP)
                .triggerableAnim("deploy_trigger", RawAnimation.begin().thenPlay("deploy")));

        controllers.add(new AnimationController<>(this, "shoot_ctrl", 0, event -> {
            if (this.isShooting) {
                if (event.getController().getAnimationState() == AnimationController.State.STOPPED) {
                    event.getController().forceAnimationReset();
                }
                return event.setAndContinue(RawAnimation.begin().thenPlay("shot"));
            }
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
