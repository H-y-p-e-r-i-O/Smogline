package com.smogline.item.tags_and_tiers;

import com.smogline.item.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.HashMap;
import java.util.Map;

public class AmmoRegistry {

    private static final Map<String, AmmoType> AMMO_BY_ID = new HashMap<>();
    private static final Map<String, AmmoType> AMMO_BY_CALIBER = new HashMap<>();

    public static class AmmoType {
        public final String id;
        public final String caliber;
        public final float damage;
        public final float speed;
        public final boolean isPiercing;

        public AmmoType(String id, String caliber, float damage, float speed, boolean isPiercing) {
            this.id = id;
            this.caliber = caliber;
            this.damage = damage;
            this.speed = speed;
            this.isPiercing = isPiercing;
        }
    }

    public static void register(Item item, String caliber, float damage, float speed, boolean isPiercing) {
        String itemId = ForgeRegistries.ITEMS.getKey(item).toString();
        AmmoType type = new AmmoType(itemId, caliber, damage, speed, isPiercing);
        AMMO_BY_ID.put(itemId, type);
        AMMO_BY_CALIBER.putIfAbsent(caliber, type);
    }


    public static AmmoType getAmmoTypeById(String itemId) {
        // 1. Ищем в карте (для старых регистраций)
        AmmoType cached = AMMO_BY_ID.get(itemId);
        if (cached != null) return cached;

        // 2. Если нет, пробуем создать динамически через IAmmoItem
        Item item = ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(itemId));
        if (item != null) {
            return getAmmoTypeFromItem(item);
        }

        return null;
    }


    /**
     * Создает AmmoType на основе предмета, реализующего IAmmoItem.
     */
    public static AmmoType getAmmoTypeFromItem(Item item) {
        if (item instanceof IAmmoItem iAmmo) {
            return new AmmoType(
                    ForgeRegistries.ITEMS.getKey(item).toString(), // ID
                    iAmmo.getCaliber(),                            // Калибр
                    iAmmo.getDamage(),                             // Урон
                    iAmmo.getSpeed(),                              // Скорость
                    iAmmo.isPiercing()                             // Пробивание
            );
        }
        return null;
    }


    public static AmmoType getAmmoTypeByCaliber(String caliber) {
        return AMMO_BY_CALIBER.get(caliber);
    }

    public static boolean isValidAmmo(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();

        // ✅ Проверяем интерфейс ИЛИ карту
        if (item instanceof IAmmoItem) return true;

        String itemId = ForgeRegistries.ITEMS.getKey(item).toString();
        return AMMO_BY_ID.containsKey(itemId);
    }

    public static String getCaliber(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Item item = stack.getItem();

        if (item instanceof IAmmoItem iAmmo) {
            return iAmmo.getCaliber();
        }

        String itemId = ForgeRegistries.ITEMS.getKey(item).toString();
        AmmoType type = AMMO_BY_ID.get(itemId);
        return type != null ? type.caliber : null;
    }

}
