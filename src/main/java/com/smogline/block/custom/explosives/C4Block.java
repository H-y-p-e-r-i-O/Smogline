package com.smogline.block.custom.explosives;

import com.smogline.particle.explosions.basic.ExplosionParticleUtils;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.item.context.BlockPlaceContext; // Важный импорт
import net.minecraft.world.level.Level;
import net.minecraft.core.Direction; // Важный импорт
import net.minecraft.world.level.block.state.StateDefinition; // Важный импорт
import net.minecraft.world.level.block.state.properties.DirectionProperty; // Важный импорт

import com.smogline.particle.ModExplosionParticles;
import com.smogline.util.explosions.general.BlastExplosionGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class C4Block extends Block implements IDetonatable {
    // Добавляем свойство вращения (Горизонтальное: Север, Юг, Запад, Восток)
    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL);

    private static final float EXPLOSION_POWER = 25.0F;
    private static final double PARTICLE_VIEW_DISTANCE = 512.0;
    private static final int DETONATION_RADIUS = 6;
    private static final int CRATER_RADIUS = 15;
    private static final int CRATER_DEPTH = 25;

    public C4Block(Properties properties) {
        super(properties);
        // Регистрируем дефолтное состояние (обычно смотрят на Север)
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    // --- Логика вращения (Новая часть) ---

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Ставит блок "лицом" к игроку (противоположно взгляду игрока)
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    // -------------------------------------

    @Override
    public boolean onDetonate(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            ServerLevel serverLevel = (ServerLevel) level;
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5;

            level.removeBlock(pos, false);

            level.explode(null, x, y, z, EXPLOSION_POWER, Level.ExplosionInteraction.NONE);

            scheduleExplosionEffects(serverLevel, x, y, z);
            triggerNearbyDetonations(serverLevel, pos, player);

            serverLevel.getServer().tell(new TickTask(30, () -> {
                BlastExplosionGenerator.generateNaturalCrater(
                        serverLevel,
                        pos,
                        CRATER_RADIUS,
                        CRATER_DEPTH
                );
            }));

            return true;
        }
        return false;
    }

    private void scheduleExplosionEffects(ServerLevel level, double x, double y, double z) {
        level.sendParticles((SimpleParticleType) ModExplosionParticles.FLASH.get(),
                x, y, z, 1, 0, 0, 0, 0);
        ExplosionParticleUtils.spawnAirBombSparks(level, x, y, z);
        level.getServer().tell(new TickTask(3, () ->
                ExplosionParticleUtils.spawnAirBombShockwave(level, x, y, z)));
        level.getServer().tell(new TickTask(8, () ->
                ExplosionParticleUtils.spawnAirBombMushroomCloud(level, x, y, z)));
    }

    private void triggerNearbyDetonations(ServerLevel serverLevel, BlockPos pos, Player player) {
        for (int x = -DETONATION_RADIUS; x <= DETONATION_RADIUS; x++) {
            for (int y = -DETONATION_RADIUS; y <= DETONATION_RADIUS; y++) {
                for (int z = -DETONATION_RADIUS; z <= DETONATION_RADIUS; z++) {
                    double dist = Math.sqrt(x * x + y * y + z * z);
                    if (dist <= DETONATION_RADIUS && dist > 0) {
                        BlockPos checkPos = pos.offset(x, y, z);
                        BlockState checkState = serverLevel.getBlockState(checkPos);
                        Block block = checkState.getBlock();
                        if (block instanceof IDetonatable) {
                            IDetonatable detonatable = (IDetonatable) block;
                            int delay = (int)(dist * 2);
                            serverLevel.getServer().tell(new TickTask(delay, () -> {
                                detonatable.onDetonate(serverLevel, checkPos, checkState, player);
                            }));
                        }
                    }
                }
            }
        }
    }
}
