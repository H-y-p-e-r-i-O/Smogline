package com.smogline.block.entity.custom.explosives;

import com.smogline.block.entity.ModBlockEntities;
import com.smogline.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;  // 🔥 ДОБАВЬ
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class MineBlockEntity extends BlockEntity implements GeoBlockEntity {  // 🔥 implements GeoBlockEntity

    private static final double DETECTION_RADIUS = 10.0;
    private static final int SOUND_COOLDOWN = 1800;
    private int soundCooldown = 0;
    private boolean hasPlayedWarning = false;

    // 🔥 GeckoLib instance cache
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public MineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MINE_BLOCK_ENTITY.get(), pos, state);
    }

    // 🔥 Обязательные методы GeoBlockEntity
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 🔥 ПУСТОЙ как у буфера!
    }



    // 🔥 Твой существующий код без изменений
    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T blockEntity) {
        if (!(blockEntity instanceof MineBlockEntity mine)) return;
        if (level == null || level.isClientSide) return;

        AABB searchArea = new AABB(blockPos).inflate(DETECTION_RADIUS);
        List<LivingEntity> entities = level.getEntitiesOfClass(
                LivingEntity.class,
                searchArea,
                e -> !(e instanceof Player player && player.isCreative())
        );

        if (!entities.isEmpty()) {
            if (mine.soundCooldown == 0) {
                mine.playWarningSound(level, blockPos);
                mine.soundCooldown = SOUND_COOLDOWN;
                mine.hasPlayedWarning = true;
            }
        } else {
            mine.hasPlayedWarning = false;
        }

        if (mine.soundCooldown > 0) mine.soundCooldown--;

        for (LivingEntity entityInRange : entities) {
            double distance = entityInRange.distanceToSqr(
                    blockPos.getX() + 0.5,
                    blockPos.getY() + 0.5,
                    blockPos.getZ() + 0.5
            );
            if (distance < 1.2) {
                level.explode(null, blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5, 3.5F, true, Level.ExplosionInteraction.NONE);
                level.removeBlock(blockPos, false);
                break;
            }
        }
    }

    private void playWarningSound(Level level, BlockPos blockPos) {
        if (level != null && ModSounds.GRENADE_TRIGGER.isPresent()) {
            level.playSound(
                    null,
                    blockPos.getX() + 0.5,
                    blockPos.getY() + 0.5,
                    blockPos.getZ() + 0.5,
                    ModSounds.GRENADE_TRIGGER.get(),
                    net.minecraft.sounds.SoundSource.BLOCKS,
                    3.0F,
                    1.0F
            );
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("SoundCooldown", this.soundCooldown);
        tag.putBoolean("HasPlayedWarning", this.hasPlayedWarning);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.soundCooldown = tag.getInt("SoundCooldown");
        this.hasPlayedWarning = tag.getBoolean("HasPlayedWarning");
    }

}
