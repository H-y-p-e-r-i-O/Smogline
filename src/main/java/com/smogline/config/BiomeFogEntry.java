package com.smogline.config;

import com.smogline.main.MainRegistry;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * Распарсенная запись тумана для одного биома.
 * Формат исходной строки: "namespace:biome_id, дальность, R, G, B"
 */
public class BiomeFogEntry {

    public final ResourceLocation biomeId;
    public final float fogFar;
    public final float r, g, b;

    private BiomeFogEntry(ResourceLocation biomeId, float fogFar, float r, float g, float b) {
        this.biomeId = biomeId;
        this.fogFar  = fogFar;
        this.r       = r;
        this.g       = g;
        this.b       = b;
    }

    /**
     * Парсит строку вида "smogline:inner_crater, 18.0, 0.08, 0.08, 0.10"
     * Возвращает null если строка некорректна (с предупреждением в лог).
     */
    @Nullable
    public static BiomeFogEntry parse(String raw) {
        try {
            String[] parts = raw.split(",");
            if (parts.length < 5) {
                MainRegistry.LOGGER.warn("[FogConfig] Некорректная запись биома (нужно 5 частей): '{}'", raw);
                return null;
            }

            ResourceLocation biomeId = new ResourceLocation(parts[0].trim());
            float fogFar = Float.parseFloat(parts[1].trim());
            float r      = Float.parseFloat(parts[2].trim());
            float g      = Float.parseFloat(parts[3].trim());
            float b      = Float.parseFloat(parts[4].trim());

            // Базовая валидация
            if (fogFar <= 0) {
                MainRegistry.LOGGER.warn("[FogConfig] Дальность тумана должна быть > 0 для биома '{}'", biomeId);
                return null;
            }

            return new BiomeFogEntry(biomeId, fogFar,
                    Math.min(1f, Math.max(0f, r)),
                    Math.min(1f, Math.max(0f, g)),
                    Math.min(1f, Math.max(0f, b)));

        } catch (Exception e) {
            MainRegistry.LOGGER.warn("[FogConfig] Ошибка парсинга записи биома '{}': {}", raw, e.getMessage());
            return null;
        }
    }

    @Override
    public String toString() {
        return biomeId + " (far=" + fogFar + ", rgb=[" + r + "," + g + "," + b + "])";
    }
}