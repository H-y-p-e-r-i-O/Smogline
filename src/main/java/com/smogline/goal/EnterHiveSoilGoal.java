package com.smogline.goal;

import com.smogline.api.hive.HiveNetworkManager;
import com.smogline.api.hive.HiveNetworkMember;
import com.smogline.block.custom.nature.HiveSoilBlock;
import com.smogline.block.entity.custom.DepthWormNestBlockEntity;
import com.smogline.entity.custom.DepthWormEntity;
import com.smogline.main.MainRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class EnterHiveSoilGoal extends Goal {
    private final DepthWormEntity worm;
    private BlockPos targetSoil;
    private int nextSearchTick;

    public EnterHiveSoilGoal(DepthWormEntity worm) {
        this.worm = worm;
    }

    @Override
    public boolean canUse() {
        // Если червь видит цель или у него нет привязки к гнезду — не идем
        if (this.worm.getTarget() != null || this.worm.nestPos == null) return false;

        double distSqr = this.worm.distanceToSqr(Vec3.atCenterOf(this.worm.nestPos));

        // ДОБАВИТЬ ПРОВЕРКУ: если рядом есть почва, НЕ бежим к гнезду напрямую
        // Это позволит EnterHiveSoilGoal (priority 1) сработать первым
        if (distSqr > 25.0D) { // Если до гнезда больше 5 блоков
            return false; // Даем шанс сработать входу через почву
        }

        if (worm.level().getBlockEntity(worm.nestPos) instanceof DepthWormNestBlockEntity nest) {
            return !nest.isFull() && distSqr > 1.0D;
        }
        return false;
    }


    private BlockPos findNearestSoil() {
        for (BlockPos pos : BlockPos.betweenClosed(worm.blockPosition().offset(-10, -3, -10), worm.blockPosition().offset(10, 3, 10))) {
            if (worm.level().getBlockState(pos).getBlock() instanceof HiveSoilBlock) {
                BlockEntity be = worm.level().getBlockEntity(pos);
                if (be instanceof HiveNetworkMember member && member.getNetworkId() != null) {
                    // Проверяем, есть ли в этой сети вообще место
                    HiveNetworkManager manager = HiveNetworkManager.get(worm.level());
                    if (manager != null && manager.hasNests(member.getNetworkId())) { // Проверяем, есть ли ВООБЩЕ гнезда в сети
                        // hasFreeNest() вызывается позже, в tick(), когда червь подойдет к почве и попытается войти.
                        return pos.immutable();
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void tick() {
        if (targetSoil == null) return;
        worm.getNavigation().moveTo(targetSoil.getX() + 0.5, targetSoil.getY(), targetSoil.getZ() + 0.5, 1.2D);

        // Дистанция всасывания (3 блока)
        if (worm.blockPosition().closerThan(targetSoil, 3.0D)) {
            HiveNetworkManager manager = HiveNetworkManager.get(worm.level());
            UUID netId = ((HiveNetworkMember)worm.level().getBlockEntity(targetSoil)).getNetworkId();
            // Добавить проверку:
            if (!HiveNetworkManager.get(worm.level()).hasFreeNest(netId, worm.level())) {
                this.nextSearchTick = worm.tickCount + 100; // Устанавливаем кулдаун, чтобы не спамить поиском каждый тик
                this.targetSoil = null;
                return; // Выходим, так как сеть забита
            }
            CompoundTag wormData = new CompoundTag();
            worm.saveWithoutId(wormData);
            if (manager.addWormToNetwork(netId, wormData, targetSoil, worm.level())) {
                worm.discard();
            } else {
                this.nextSearchTick = worm.tickCount + 100;
                this.targetSoil = null;
            }
        }
    }
}