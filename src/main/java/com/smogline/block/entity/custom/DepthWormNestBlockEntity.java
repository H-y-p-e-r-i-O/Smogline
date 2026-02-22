package com.smogline.block.entity.custom;

import com.smogline.block.entity.ModBlockEntities;
import com.smogline.entity.custom.DepthWormEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public class DepthWormNestBlockEntity extends BlockEntity {
    private final List<CompoundTag> storedWorms = new ArrayList<>();

    private int releaseCooldown = 0; // Таймер блокировки входа

    public boolean isFull() {
        return storedWorms.size() >= 3 || releaseCooldown > 0;
    }

    public DepthWormNestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DEPTH_WORM_NEST.get(), pos, state);
    }


    public void addWorm(DepthWormEntity worm) {
        if (!isFull()) {
            CompoundTag tag = new CompoundTag();
            worm.save(tag);
            storedWorms.add(tag);
            worm.discard(); // Удаляем сущность из мира
            setChanged();
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DepthWormNestBlockEntity blockEntity) {
        if (level.isClientSide) return;

        if (blockEntity.releaseCooldown > 0) blockEntity.releaseCooldown--;

        if (level.getGameTime() % 20 == 0 && !blockEntity.storedWorms.isEmpty()) {
            // Ищем только реальные угрозы (игнорируем креатив и наблюдателя)
            boolean hasEnemy = !level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(pos).inflate(20),
                    e -> e.isAlive() && // Цель должна быть ЖИВОЙ (не в анимации смерти)
                            e.deathTime <= 0 && // Дополнительная проверка на смерть
                            !(e instanceof DepthWormEntity) &&
                            !(e instanceof Player p && (p.isCreative() || p.isSpectator()))
            ).isEmpty();


            if (hasEnemy && blockEntity.releaseCooldown <= 0) {
                blockEntity.releaseWorms();
                blockEntity.releaseCooldown = 400; // 20 секунд покоя после выхода
            }
        }
    }


    public void releaseWorms() {
        for (CompoundTag tag : storedWorms) {
            Entity entity = EntityType.loadEntityRecursive(tag, level, (e) -> {
                BlockPos spawnPos = findSpawnPos(worldPosition);
                e.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, level.random.nextFloat() * 360F, 0);
                return e;
            });

            if (entity != null) {
                level.addFreshEntity(entity);
                entity.invulnerableTime = 20;

                if (entity instanceof DepthWormEntity worm) {
                    // homeSickTimer удален, оставляем только привязку к позиции
                    worm.nestPos = this.worldPosition;
                }
            }
        }
        storedWorms.clear();
        setChanged();
    }


    private BlockPos findSpawnPos(BlockPos nestPos) {
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
            BlockPos relative = nestPos.relative(direction);
            if (level.getBlockState(relative).isAir()) {
                return relative;
            }
        }
        return nestPos.above(); // Если всё забито, спавним сверху
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag list = new ListTag();
        list.addAll(storedWorms);
        tag.put("StoredWorms", list);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        storedWorms.clear();
        ListTag list = tag.getList("StoredWorms", 10);
        for (int i = 0; i < list.size(); i++) {
            storedWorms.add(list.getCompound(i));
        }
    }
}
