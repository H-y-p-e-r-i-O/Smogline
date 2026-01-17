package com.smogline.entity.weapons.turrets;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class TurretLightLinkedEntity extends TurretLightEntity {

    private static final EntityDataAccessor<Optional<BlockPos>> PARENT_BLOCK_POS =
            SynchedEntityData.defineId(TurretLightLinkedEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);

    public TurretLightLinkedEntity(EntityType<? extends TurretLightEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(PARENT_BLOCK_POS, Optional.empty());
    }

    public void setParentBlock(BlockPos pos) {
        this.entityData.set(PARENT_BLOCK_POS, Optional.of(pos));
    }

    public BlockPos getParentBlock() {
        return this.entityData.get(PARENT_BLOCK_POS).orElse(null);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        BlockPos parent = getParentBlock();
        if (parent != null) {
            tag.putInt("ParentX", parent.getX());
            tag.putInt("ParentY", parent.getY());
            tag.putInt("ParentZ", parent.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("ParentX")) {
            setParentBlock(new BlockPos(
                    tag.getInt("ParentX"),
                    tag.getInt("ParentY"),
                    tag.getInt("ParentZ")
            ));
        }
    }
}
