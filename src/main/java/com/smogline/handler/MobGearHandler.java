package com.smogline.handler;

import com.smogline.item.ModItems;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "smogline")
public class MobGearHandler {

    @SubscribeEvent
    public static void onMobSpawn(MobSpawnEvent.FinalizeSpawn event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        if (entity instanceof Zombie zombie) {
            if (zombie.getRandom().nextFloat() < 0.07f) { // 15% шанс

                zombie.setItemSlot(EquipmentSlot.HEAD,  new ItemStack(ModItems.SECURITY_HELMET.get()));
                zombie.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.SECURITY_CHESTPLATE.get()));
                zombie.setItemSlot(EquipmentSlot.LEGS,  new ItemStack(ModItems.SECURITY_LEGGINGS.get()));
                zombie.setItemSlot(EquipmentSlot.FEET,  new ItemStack(ModItems.SECURITY_BOOTS.get()));



                // Оружие

                // zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.GUN_REX.get()));

                // Дроп шанса (как у ванильных)
                zombie.setDropChance(EquipmentSlot.HEAD, 0.05f);
                zombie.setDropChance(EquipmentSlot.CHEST, 0.05f);
                zombie.setDropChance(EquipmentSlot.LEGS, 0.05f);
                zombie.setDropChance(EquipmentSlot.FEET, 0.05f);
            }
        }

        if (entity instanceof Skeleton skeleton) {


            // Дроп шанса (как у ванильных)
            skeleton.setDropChance(EquipmentSlot.HEAD, 0.1f);
            skeleton.setDropChance(EquipmentSlot.CHEST, 0.1f);
            skeleton.setDropChance(EquipmentSlot.LEGS, 0.1f);
            skeleton.setDropChance(EquipmentSlot.FEET, 0.1f);

        }
    }
}