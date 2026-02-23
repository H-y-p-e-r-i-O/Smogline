package com.smogline.api.hive;

import com.smogline.block.entity.custom.DepthWormNestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

import java.util.*;

public class HiveNetworkManager {
    private final Map<UUID, HiveNetwork> networks = new HashMap<>();
    private final Map<BlockPos, UUID> posToNetwork = new HashMap<>();
    public boolean hasNests(UUID netId) {
        HiveNetwork net = networks.get(netId);
        return net != null && !net.wormCounts.isEmpty();

    }
    public boolean hasFreeNest(UUID netId, Level level) {
        HiveNetwork net = networks.get(netId);
        if (net == null || net.wormCounts.isEmpty()) return false;

        for (Map.Entry<BlockPos, Integer> entry : net.wormCounts.entrySet()) {
            BlockEntity be = level.getBlockEntity(entry.getKey());
            if (be instanceof DepthWormNestBlockEntity nest) {
                if (!nest.isFull()) return true; // Найдено свободное место!
            }
        }
        return false;
    }


    public void onBlockAdded(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof HiveNetworkMember member)) return;

        Set<UUID> neighborNetworks = new HashSet<>();
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockEntity neighborBe = level.getBlockEntity(neighborPos);
            if (neighborBe instanceof HiveNetworkMember neighbor && neighbor.getNetworkId() != null) {
                neighborNetworks.add(neighbor.getNetworkId());
            }
        }

        if (neighborNetworks.isEmpty()) {
            UUID newId = UUID.randomUUID();
            HiveNetwork net = new HiveNetwork(newId);
            networks.put(newId, net);
            posToNetwork.put(pos, newId);
            member.setNetworkId(newId);
            net.addMember(pos, be instanceof DepthWormNestBlockEntity);
        } else if (neighborNetworks.size() == 1) {
            UUID netId = neighborNetworks.iterator().next();
            posToNetwork.put(pos, netId);
            member.setNetworkId(netId);
            networks.get(netId).addMember(pos, be instanceof DepthWormNestBlockEntity);
        } else {
            // объединение нескольких сетей
            Iterator<UUID> it = neighborNetworks.iterator();
            UUID primary = it.next();
            HiveNetwork primaryNet = networks.get(primary);
            posToNetwork.put(pos, primary);
            member.setNetworkId(primary);
            primaryNet.addMember(pos, be instanceof DepthWormNestBlockEntity);

            while (it.hasNext()) {
                UUID otherId = it.next();
                HiveNetwork otherNet = networks.remove(otherId);
                if (otherNet != null) {
                    for (BlockPos m : otherNet.members) {
                        posToNetwork.put(m, primary);
                        BlockEntity mBe = level.getBlockEntity(m);
                        if (mBe instanceof HiveNetworkMember mMember) {
                            mMember.setNetworkId(primary);
                        }

                        // 1. Добавляем в общий список участников
                        primaryNet.members.add(m);

                        // 2. КРИТИЧЕСКОЕ ИСПРАВЛЕНИЕ: Переносим данные о гнездах!
                        if (otherNet.wormCounts.containsKey(m)) {
                            primaryNet.wormCounts.put(m, otherNet.wormCounts.get(m));
                        }
                    }
                }
            }

        }
    }

    public void onBlockRemoved(Level level, BlockPos pos) {
        UUID netId = posToNetwork.remove(pos);
        if (netId == null) return;
        HiveNetwork net = networks.get(netId);
        if (net == null) return;

        net.removeMember(pos);
        if (net.members.isEmpty()) {
            networks.remove(netId);
            return;
        }

        // проверка на разделение сети
        Set<BlockPos> remaining = new HashSet<>(net.members);
        Map<BlockPos, UUID> componentMap = new HashMap<>();
        while (!remaining.isEmpty()) {
            BlockPos start = remaining.iterator().next();
            Set<BlockPos> component = new HashSet<>();
            Queue<BlockPos> queue = new LinkedList<>();
            queue.add(start);
            component.add(start);
            remaining.remove(start);

            while (!queue.isEmpty()) {
                BlockPos current = queue.poll();
                for (Direction dir : Direction.values()) {
                    BlockPos neighbor = current.relative(dir);
                    if (net.members.contains(neighbor) && !component.contains(neighbor)) {
                        component.add(neighbor);
                        remaining.remove(neighbor);
                        queue.add(neighbor);
                    }
                }
            }

            if (component.size() == net.members.size()) {
                break; // всё ещё одна компонента
            } else {
                UUID newId = UUID.randomUUID();
                HiveNetwork newNet = new HiveNetwork(newId);
                networks.put(newId, newNet);
                for (BlockPos p : component) {
                    newNet.members.add(p);
                    posToNetwork.put(p, newId);
                    if (net.wormCounts.containsKey(p)) {
                        newNet.wormCounts.put(p, net.wormCounts.get(p));
                    }
                    if (level.getBlockEntity(p) instanceof HiveNetworkMember m) {
                        m.setNetworkId(newId);
                    }
                }
                net.members.removeAll(component);
                for (BlockPos p : component) {
                    net.wormCounts.remove(p);
                }
            }
        }
    }

    public boolean addWormToNetwork(UUID netId, CompoundTag wormTag, BlockPos sourcePos, Level level) {
        HiveNetwork net = networks.get(netId);
        if (net != null) {
            return net.addWorm(level, wormTag, sourcePos);
        }
        return false;
    }

    public void updateWormCount(UUID netId, BlockPos nestPos, int delta) {
        HiveNetwork net = networks.get(netId);
        if (net != null) net.updateWormCount(nestPos, delta);
    }

    // Сериализация
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag networksList = new ListTag();
        for (HiveNetwork net : networks.values()) {
            networksList.add(net.toNBT());
        }
        tag.put("Networks", networksList);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        networks.clear();
        posToNetwork.clear();
        ListTag networksList = tag.getList("Networks", 10);
        for (int i = 0; i < networksList.size(); i++) {
            HiveNetwork net = HiveNetwork.fromNBT(networksList.getCompound(i));
            networks.put(net.id, net);
            for (BlockPos p : net.members) {
                posToNetwork.put(p, net.id);
            }
        }
    }

    // Capability
    public static final Capability<HiveNetworkManager> HIVE_NETWORK_MANAGER = CapabilityManager.get(new CapabilityToken<>(){});
    public static HiveNetworkManager get(Level level) {
        return level.getCapability(HIVE_NETWORK_MANAGER).orElse(null);
    }
}