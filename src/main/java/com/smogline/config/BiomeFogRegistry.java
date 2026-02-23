package com.smogline.config;

import com.smogline.main.MainRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Кеш биомовых настроек тумана.
 * Перестраивается при изменении конфига (горячая перезагрузка).
 */
@Mod.EventBusSubscriber(modid = "smogline", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class BiomeFogRegistry {

    private static final Map<ResourceLocation, BiomeFogEntry> CACHE = new HashMap<>();
    private static boolean dirty = true;

    /** Получить настройки для биома, или null если не задано */
    @Nullable
    public static BiomeFogEntry get(ResourceLocation biomeId) {
        if (dirty) rebuild();
        return CACHE.get(biomeId);
    }

    /** Пометить кеш как устаревший (при изменении конфига) */
    public static void invalidate() {
        dirty = true;
    }

    @SubscribeEvent
    public static void onConfigReload(net.minecraftforge.fml.event.config.ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == FogConfig.SPEC) {
            invalidate();
            MainRegistry.LOGGER.info("[FogConfig] Конфиг тумана перезагружен, кеш биомов сброшен");
        }
    }

    private static void rebuild() {
        CACHE.clear();
        int loaded = 0, failed = 0;

        for (String raw : FogConfig.INSTANCE.biomeFogEntries.get()) {
            BiomeFogEntry entry = BiomeFogEntry.parse(raw);
            if (entry != null) {
                CACHE.put(entry.biomeId, entry);
                loaded++;
            } else {
                failed++;
            }
        }

        dirty = false;
        MainRegistry.LOGGER.info("[FogConfig] Загружено {} биомов тумана ({} ошибок)", loaded, failed);
    }
}
