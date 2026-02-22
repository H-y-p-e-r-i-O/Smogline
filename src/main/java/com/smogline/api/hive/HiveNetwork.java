package com.smogline.api.hive;

import com.smogline.block.entity.custom.DepthWormNestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;

import java.util.*;

public class HiveNetwork {
    public final UUID id;
    public final Set<BlockPos> members = new HashSet<>();
    public final Map<BlockPos, Integer> wormCounts = new HashMap<>(); // только для гнёзд

    public HiveNetwork(UUID id) { this.id = id; }

    public void addMember(BlockPos pos, boolean isNest) {
        members.add(pos);
        if (isNest) wormCounts.put(pos, 0);
    }

    public void removeMember(BlockPos pos) {
        members.remove(pos);
        wormCounts.remove(pos);
    }

    public int getTotalWorms() {
        return wormCounts.values().stream().mapToInt(Integer::intValue).sum();
    }

    public void addWorm(Level level, CompoundTag wormTag, BlockPos sourcePos) {
        if (wormCounts.isEmpty()) return;
        BlockPos target = Collections.min(wormCounts.entrySet(), Map.Entry.comparingByValue()).getKey();
        if (level.getBlockEntity(target) instanceof DepthWormNestBlockEntity nest) {
            nest.addWormTag(wormTag);
            wormCounts.merge(target, 1, Integer::sum);
        }
    }

    public void updateWormCount(BlockPos nestPos, int delta) {
        wormCounts.merge(nestPos, delta, Integer::sum);
    }

    // для сериализации
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        ListTag membersList = new ListTag();
        for (BlockPos p : members) {
            CompoundTag pTag = NbtUtils.writeBlockPos(p);
            pTag.putBoolean("IsNest", wormCounts.containsKey(p));
            if (wormCounts.containsKey(p)) {
                pTag.putInt("WormCount", wormCounts.get(p));
            }
            membersList.add(pTag);
        }
        tag.put("Members", membersList);
        return tag;
    }

    public static HiveNetwork fromNBT(CompoundTag tag) {
        UUID id = tag.getUUID("Id");
        HiveNetwork net = new HiveNetwork(id);
        ListTag membersList = tag.getList("Members", 10);
        for (int i = 0; i < membersList.size(); i++) {
            CompoundTag pTag = membersList.getCompound(i);
            BlockPos pos = NbtUtils.readBlockPos(pTag);
            boolean isNest = pTag.getBoolean("IsNest");
            net.members.add(pos);
            if (isNest) {
                net.wormCounts.put(pos, pTag.getInt("WormCount"));
            }
        }
        return net;
    }
}