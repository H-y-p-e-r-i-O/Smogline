package com.smogline.goal;

import com.smogline.api.hive.HiveNetworkManager;
import com.smogline.api.hive.HiveNetworkMember;
import com.smogline.entity.custom.DepthWormEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.EnumSet;
import java.util.UUID;

public class ReturnToHiveGoal extends Goal {
    private final DepthWormEntity worm;
    private BlockPos targetPos;
    private int nextSearchTick;

    public ReturnToHiveGoal(DepthWormEntity worm) {
        this.worm = worm;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (worm.getTarget() != null || worm.tickCount < nextSearchTick) return false;

        this.targetPos = findNearestEntry();
        return this.targetPos != null;
    }

    private BlockPos findNearestEntry() {
        BlockPos wormPos = worm.blockPosition();
        HiveNetworkManager manager = HiveNetworkManager.get(worm.level());

        // Радиус 16 блоков для комфортного поиска
        for (BlockPos p : BlockPos.betweenClosed(wormPos.offset(-16, -5, -16), wormPos.offset(16, 5, 16))) {
            BlockEntity be = worm.level().getBlockEntity(p);
            if (be instanceof HiveNetworkMember member && member.getNetworkId() != null) {
                // Если у блока есть ID сети — этого достаточно, чтобы червь ЗАХОТЕЛ туда пойти
                return p.immutable();
            }
        }
        return null;
    }

    private void sendDebug(String msg) {
        if (!worm.level().isClientSide) {
            worm.level().players().forEach(p -> p.sendSystemMessage(net.minecraft.network.chat.Component.literal(msg)));
        }
    }

    private void spawnDebugParticles(BlockPos p) {
        if (!worm.level().isClientSide) {
            ((net.minecraft.server.level.ServerLevel)worm.level()).sendParticles(
                    net.minecraft.core.particles.ParticleTypes.FLAME,
                    p.getX() + 0.5, p.getY() + 1.2, p.getZ() + 0.5, 3, 0.1, 0.1, 0.1, 0.01
            );
        }
    }

    @Override
    public void start() {
        // Сообщаем в консоль/лог, что червь нашел дом
        // System.out.println("Червь нашел вход на " + targetPos);
    }

    @Override
    public void tick() {
        if (targetPos == null) return;

        worm.getNavigation().moveTo(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, 1.0D);

        if (worm.distanceToSqr(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5) < 2.5D) {
            HiveNetworkManager manager = HiveNetworkManager.get(worm.level());
            BlockEntity be = worm.level().getBlockEntity(targetPos);

            if (be instanceof HiveNetworkMember member && member.getNetworkId() != null) {
                // ТОЛЬКО ПРИ КАСАНИИ проверяем, есть ли куда телепортировать червя
                if (manager.hasFreeNest(member.getNetworkId(), worm.level())) {
                    CompoundTag tag = new CompoundTag();
                    worm.saveWithoutId(tag);
                    if (manager.addWormToNetwork(member.getNetworkId(), tag, targetPos, worm.level())) {
                        worm.discard(); // Успех!
                    }
                } else {
                    // Если мест нет, червь "понимает" это только уткнувшись носом
                    this.nextSearchTick = worm.tickCount + 200;
                    this.targetPos = null;
                }
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        return targetPos != null && worm.getTarget() == null;
    }
}