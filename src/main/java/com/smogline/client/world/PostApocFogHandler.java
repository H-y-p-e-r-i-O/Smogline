package com.smogline.client.world;

import com.smogline.client.render.shader.ShaderCompatibilityDetector;
import com.smogline.config.BiomeFogEntry;
import com.smogline.config.BiomeFogRegistry;
import com.smogline.config.FogConfig;
import com.smogline.lib.RefStrings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = RefStrings.MODID, value = Dist.CLIENT)
public class PostApocFogHandler {

    private static final float BREATH_PERIOD = 280.0f;
    private static final float LERP_SPEED    = 0.04f;

    // Интерполируемые текущие значения
    private static float currentFogFar  = 160f;
    private static float currentR = 0.52f, currentG = 0.50f, currentB = 0.38f;

    // ─────────────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onFogDensity(ViewportEvent.RenderFog event) {
        FogConfig cfg = FogConfig.INSTANCE;
        if (!cfg.enabled.get()) return;

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null) return;

        ResourceLocation biomeId = level.getBiome(player.blockPosition())
                .unwrapKey().map(k -> k.location()).orElse(null);

        float targetFar;

        // 1. Проверяем конфиг биомов
        BiomeFogEntry biomeEntry = biomeId != null ? BiomeFogRegistry.get(biomeId) : null;
        if (biomeEntry != null) {
            targetFar = biomeEntry.fogFar;
        } else {
            // 2. Глобальные настройки по погоде/времени суток
            long gameTime = level.getDayTime();
            boolean isRain  = level.isRaining();
            boolean isNight = isNight(gameTime);

            if (isRain)       targetFar = (float) cfg.globalFogRain.get().floatValue();
            else if (isNight) targetFar = (float) cfg.globalFogNight.get().floatValue();
            else              targetFar = (float) cfg.globalFogFar.get().floatValue();

            // 3. Корректировка по высоте
            if (cfg.heightBasedFog.get()) {
                float playerY     = (float) player.getY();
                float lowH        = (float) cfg.heightFogLow.get().floatValue();
                float highH       = (float) cfg.heightFogHigh.get().floatValue();
                float heightFactor = Mth.clamp((playerY - lowH) / (highH - lowH), 0f, 1f);
                targetFar *= (1.0f + heightFactor * 0.4f);
            }
        }

        // 4. Анимация "дыхания"
        if (cfg.breathingFog.get()) {
            float amp = (float) cfg.breathingAmplitude.get().floatValue();
            float phase = (level.getDayTime() % (long) BREATH_PERIOD) / BREATH_PERIOD;
            targetFar += (float) Math.sin(phase * Math.PI * 2.0) * amp;
        }

        // 5. Плавная интерполяция
        currentFogFar = Mth.lerp(LERP_SPEED, currentFogFar, targetFar);

        event.setNearPlaneDistance(0.0f);
        event.setFarPlaneDistance(currentFogFar);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        FogConfig cfg = FogConfig.INSTANCE;
        if (!cfg.enabled.get()) return;
        if (ShaderCompatibilityDetector.isExternalShaderActive()) return;

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null) return;

        ResourceLocation biomeId = level.getBiome(player.blockPosition())
                .unwrapKey().map(k -> k.location()).orElse(null);

        float[] targetColor;

        // 1. Проверяем конфиг биомов
        BiomeFogEntry biomeEntry = biomeId != null ? BiomeFogRegistry.get(biomeId) : null;
        if (biomeEntry != null) {
            targetColor = new float[]{ biomeEntry.r, biomeEntry.g, biomeEntry.b };
        } else {
            // 2. Глобальные цвета
            long gameTime = level.getDayTime();
            boolean isRain  = level.isRaining();
            boolean isNight = isNight(gameTime);

            if (isRain) {
                targetColor = toFloatArray(cfg.fogColorRain.get());
            } else if (isNight) {
                float nightBlend = getNightBlend(gameTime);
                float[] day   = toFloatArray(cfg.fogColorDay.get());
                float[] night = toFloatArray(cfg.fogColorNight.get());
                targetColor = new float[]{
                        Mth.lerp(nightBlend, day[0], night[0]),
                        Mth.lerp(nightBlend, day[1], night[1]),
                        Mth.lerp(nightBlend, day[2], night[2])
                };
            } else {
                targetColor = toFloatArray(cfg.fogColorDay.get());
            }
        }

        // 3. Плавная интерполяция цвета
        currentR = Mth.lerp(LERP_SPEED, currentR, targetColor[0]);
        currentG = Mth.lerp(LERP_SPEED, currentG, targetColor[1]);
        currentB = Mth.lerp(LERP_SPEED, currentB, targetColor[2]);

        event.setRed(currentR);
        event.setGreen(currentG);
        event.setBlue(currentB);
    }

    // ── Вспомогательные ──────────────────────────────────────────────────

    private static boolean isNight(long t) {
        t %= 24000L;
        return t >= 13000L && t <= 23000L;
    }

    private static float getNightBlend(long gameTime) {
        long t = gameTime % 24000L;
        if (t < 12000L) return 0.0f;
        if (t < 13500L) return (t - 12000L) / 1500.0f;
        if (t < 22500L) return 1.0f;
        if (t < 24000L) return 1.0f - (t - 22500L) / 1500.0f;
        return 0.0f;
    }

    private static float[] toFloatArray(List<? extends Double> list) {
        if (list == null || list.size() < 3) return new float[]{ 0.5f, 0.5f, 0.5f };
        return new float[]{ list.get(0).floatValue(), list.get(1).floatValue(), list.get(2).floatValue() };
    }
}