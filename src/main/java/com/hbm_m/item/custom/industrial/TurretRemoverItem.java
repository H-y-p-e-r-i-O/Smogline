package com.hbm_m.item.custom.industrial;

import com.hbm_m.block.ModBlocks; // ЗАМЕНИ НА СВОЙ КЛАСС БЛОКОВ
import com.hbm_m.entity.TurretLightEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class TurretRemoverItem extends Item {

    public TurretRemoverItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity interactionTarget, InteractionHand usedHand) {
        // Проверяем, что кликнули по Турели
        if (interactionTarget instanceof TurretLightEntity turret) {
            Level level = player.level();

            if (!level.isClientSide) {
                // Проверяем владельца
                // 1. У турели есть владелец?
                // 2. UUID владельца совпадает с UUID игрока?
                if (turret.getOwnerUUID() != null && turret.getOwnerUUID().equals(player.getUUID())) {

                    // Удаляем турель
                    turret.remove(TurretLightEntity.RemovalReason.DISCARDED);

                    // Выдаем игроку блок турели
                    // ВАЖНО: Замени ModBlocks.TURRET_LIGHT_BLOCK.get() на свой блок!
                    ItemStack drop = new ItemStack(ModBlocks.TURRET_LIGHT.get());

                    if (!player.getInventory().add(drop)) {
                        // Если инвентарь полон - дропаем под ноги
                        player.drop(drop, false);
                    }

                    // Отправляем сообщение
                    player.sendSystemMessage(Component.literal("§aТурель успешно демонтирована!"));

                    return InteractionResult.SUCCESS;
                } else {
                    // Если не владелец
                    player.sendSystemMessage(Component.literal("§cВы не владелец этой турели!"));
                    return InteractionResult.FAIL;
                }
            }
            return InteractionResult.SUCCESS;
        }

        return super.interactLivingEntity(stack, player, interactionTarget, usedHand);
    }
}
