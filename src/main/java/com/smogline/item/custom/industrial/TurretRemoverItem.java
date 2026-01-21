package com.smogline.item.custom.industrial;

import com.smogline.block.ModBlocks;
import com.smogline.entity.weapons.turrets.TurretLightEntity;
import com.smogline.entity.weapons.turrets.TurretLightLinkedEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class TurretRemoverItem extends Item {

    public TurretRemoverItem(Properties properties) {
        super(properties);
    }

    // --- Демонтаж при клике по ТУРЕЛИ (entity) ---
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        Level level = player.level();

        // 1. Обработка СВЯЗАННОЙ турели
        if (target instanceof TurretLightLinkedEntity linkedTurret) {
            if (!level.isClientSide) {
                if (isOwner(player, linkedTurret)) {
                    // Удаляем блок
                    BlockPos parentPos = linkedTurret.getParentBlock();
                    if (parentPos != null) {
                        level.removeBlock(parentPos, false);
                    }

                    // Удаляем турель
                    linkedTurret.remove(Entity.RemovalReason.DISCARDED); // Исправлено

                    // Даем блок обратно
                    giveItem(player, new ItemStack(ModBlocks.TURRET_LIGHT_PLACER.get()));
                    player.sendSystemMessage(Component.literal("§aСвязанная турель демонтирована!"));
                    return InteractionResult.SUCCESS;
                } else {
                    player.sendSystemMessage(Component.literal("§cВы не владелец этой турели!"));
                    return InteractionResult.FAIL;
                }
            }
            return InteractionResult.SUCCESS;
        }

        // 2. Обработка ОБЫЧНОЙ турели
        if (target instanceof TurretLightEntity turret) {
            if (!level.isClientSide) {
                if (isOwner(player, turret)) {
                    turret.remove(Entity.RemovalReason.DISCARDED); // Исправлено
                    giveItem(player, new ItemStack(ModBlocks.TURRET_LIGHT.get()));
                    player.sendSystemMessage(Component.literal("§aТурель демонтирована!"));
                    return InteractionResult.SUCCESS;
                } else {
                    player.sendSystemMessage(Component.literal("§cВы не владелец этой турели!"));
                    return InteractionResult.FAIL;
                }
            }
            return InteractionResult.SUCCESS;
        }

        return super.interactLivingEntity(stack, player, target, hand);
    }

    // --- Демонтаж при клике по БЛОКУ ---
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();

        if (player == null) return InteractionResult.PASS;

        // Если кликнули по блоку турели-плейсера
        if (state.getBlock() == ModBlocks.TURRET_LIGHT_PLACER.get()) {
            if (!level.isClientSide) {
                // Ищем турель над блоком (в радиусе 2 блока)
                AABB searchBox = new AABB(pos).inflate(2.0);
                List<TurretLightLinkedEntity> turrets = level.getEntitiesOfClass(
                        TurretLightLinkedEntity.class,
                        searchBox,
                        turret -> pos.equals(turret.getParentBlock())
                );

                if (!turrets.isEmpty()) {
                    TurretLightLinkedEntity turret = turrets.get(0);
                    if (isOwner(player, turret)) {
                        // Удаляем блок
                        level.removeBlock(pos, false);
                        // Удаляем турель
                        turret.remove(Entity.RemovalReason.DISCARDED); // Исправлено
                        // Выдаем блок
                        giveItem(player, new ItemStack(ModBlocks.TURRET_LIGHT_PLACER.get()));
                        player.sendSystemMessage(Component.literal("§aТурель (блок) демонтирована!"));
                        return InteractionResult.SUCCESS;
                    } else {
                        player.sendSystemMessage(Component.literal("§cВы не владелец!"));
                        return InteractionResult.FAIL;
                    }
                }
            }
            return InteractionResult.SUCCESS;
        }

        return super.useOn(context);
    }

    // Вспомогательные методы (перегрузка для разных классов)

    private boolean isOwner(Player player, TurretLightEntity turret) {
        return turret.getOwnerUUID() != null && turret.getOwnerUUID().equals(player.getUUID());
    }

    private boolean isOwner(Player player, TurretLightLinkedEntity turret) {
        return turret.getOwnerUUID() != null && turret.getOwnerUUID().equals(player.getUUID());
    }

    private void giveItem(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
