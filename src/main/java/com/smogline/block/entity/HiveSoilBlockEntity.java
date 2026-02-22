package com.smogline.block.entity;

import com.smogline.api.hive.HiveNetworkMember;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class HiveSoilBlockEntity extends BlockEntity implements HiveNetworkMember {
    private UUID networkId;

    public HiveSoilBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HIVE_SOIL.get(), pos, state);
    }

    @Override
    public UUID getNetworkId() { return networkId; }

    @Override
    public void setNetworkId(UUID id) { this.networkId = id; setChanged(); }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (networkId != null) tag.putUUID("NetworkId", networkId);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("NetworkId")) networkId = tag.getUUID("NetworkId");
        else networkId = null;
    }
}