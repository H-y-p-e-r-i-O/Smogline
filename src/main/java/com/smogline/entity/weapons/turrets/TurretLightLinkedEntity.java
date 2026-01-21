package com.smogline.entity.weapons.turrets;

import com.smogline.block.custom.weapons.TurretLightPlacerBlock;
import com.smogline.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public class TurretLightLinkedEntity extends TurretLightEntity {

    private static final EntityDataAccessor<Optional<BlockPos>> PARENT_BLOCK_POS =
            SynchedEntityData.defineId(TurretLightLinkedEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);

    private static final EntityDataAccessor<Integer> LAST_DAMAGE_TICK =
            SynchedEntityData.defineId(TurretLightLinkedEntity.class, EntityDataSerializers.INT);

    private static final int HEAL_DELAY_TICKS = 200;     // 10 секунд
    private static final int HEAL_INTERVAL_TICKS = 20;   // лечим раз в 1 секунду
    private static final float HEAL_AMOUNT = 1.0F;       // 1 HP в секунду

    public TurretLightLinkedEntity(EntityType<? extends TurretLightEntity> entityType, Level level) {
        super(entityType, level);
    }

    // Конструктор для ModEntities
    public TurretLightLinkedEntity(Level level) {
        this(ModEntities.TURRET_LIGHT_LINKED.get(), level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(PARENT_BLOCK_POS, Optional.empty());
        this.entityData.define(LAST_DAMAGE_TICK, 0);
    }

    public void setParentBlock(BlockPos pos) {
        this.entityData.set(PARENT_BLOCK_POS, Optional.ofNullable(pos));
    }

    public BlockPos getParentBlock() {
        return this.entityData.get(PARENT_BLOCK_POS).orElse(null);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) return;

        BlockPos parent = getParentBlock();
        if (parent == null) {
            this.discard();
            return;
        }

        BlockState state = this.level().getBlockState(parent);
        if (!(state.getBlock() instanceof TurretLightPlacerBlock)) {
            this.discard();
            return;
        }

        // Держим турель на буфере
        double x = parent.getX() + 0.5;
        double y = parent.getY() + 1.0;
        double z = parent.getZ() + 0.5;
        this.moveTo(x, y, z, this.getYRot(), this.getXRot());

        // ✅ УБРАЛ FACING — буфер не имеет направления
        // Турель будет стоять так, как её развернули при спавне

        // Хил после 10 секунд без урона
        int last = this.entityData.get(LAST_DAMAGE_TICK);
        if (this.tickCount - last >= HEAL_DELAY_TICKS) {
            if (this.getHealth() < this.getMaxHealth() && (this.tickCount % HEAL_INTERVAL_TICKS == 0)) {
                this.heal(HEAL_AMOUNT);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean ok = super.hurt(source, amount);
        if (ok) {
            this.entityData.set(LAST_DAMAGE_TICK, this.tickCount);
        }
        return ok;
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
        tag.putInt("LastDamageTick", this.entityData.get(LAST_DAMAGE_TICK));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.contains("ParentX")) {
            setParentBlock(new BlockPos(tag.getInt("ParentX"), tag.getInt("ParentY"), tag.getInt("ParentZ")));
        }
        if (tag.contains("LastDamageTick")) {
            this.entityData.set(LAST_DAMAGE_TICK, tag.getInt("LastDamageTick"));
        }
    }
}
