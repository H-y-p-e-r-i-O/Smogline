package com.smogline.goal;

import com.smogline.api.hive.HiveNetworkManager;
import com.smogline.api.hive.HiveNetworkMember;
import com.smogline.block.custom.nature.HiveSoilBlock;
import com.smogline.entity.custom.DepthWormEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.UUID;

public class EnterHiveSoilGoal extends Goal {
    private final DepthWormEntity worm;
    private BlockPos targetSoil;

    public EnterHiveSoilGoal(DepthWormEntity worm) {
        this.worm = worm;
    }

    @Override
    public boolean canUse() {
        if (worm.getTarget() != null) return false;
        targetSoil = findNearestSoil();
        return targetSoil != null;
    }

    private BlockPos findNearestSoil() {
        BlockPos pos = worm.blockPosition();
        int radius = 16;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos p = pos.offset(dx, dy, dz);
                    if (worm.level().getBlockState(p).getBlock() instanceof HiveSoilBlock) {
                        BlockEntity be = worm.level().getBlockEntity(p);
                        if (be instanceof HiveNetworkMember member && member.getNetworkId() != null) {
                            return p;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void tick() {
        if (targetSoil == null) return;
        worm.getNavigation().moveTo(targetSoil.getX() + 0.5, targetSoil.getY(), targetSoil.getZ() + 0.5, 1.0);
        if (worm.blockPosition().closerThan(targetSoil, 1.5)) {
            CompoundTag tag = new CompoundTag();
            worm.save(tag);
            BlockEntity be = worm.level().getBlockEntity(targetSoil);
            if (be instanceof HiveNetworkMember member) {
                UUID netId = member.getNetworkId();
                if (netId != null) {
                    HiveNetworkManager manager = HiveNetworkManager.get(worm.level());
                    if (manager != null) {
                        manager.addWormToNetwork(netId, tag, targetSoil, worm.level());
                    }
                }
            }
            worm.discard();
        }
    }
}