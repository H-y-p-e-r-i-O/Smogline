package com.smogline.block.entity.custom;

import com.smogline.api.hive.HiveNetworkMember;
import com.smogline.block.entity.ModBlockEntities;
import com.smogline.main.MainRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.UUID;

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
    public void setNetworkId(UUID id) {
        this.networkId = id;
        this.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (this.networkId != null) tag.putUUID("NetworkId", this.networkId);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("NetworkId")) this.networkId = tag.getUUID("NetworkId");
    }
}
