package com.smogline.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;

public class FogConfig {

    public static final ForgeConfigSpec SPEC;
    public static final FogConfig INSTANCE;

    static {
        Pair<FogConfig, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder()
                .configure(FogConfig::new);
        INSTANCE = pair.getLeft();
        SPEC = pair.getRight();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(
                ModConfig.Type.CLIENT,
                SPEC,
                "smogline-fog.toml"
        );
    }

    // ── Глобальные настройки ─────────────────────────────────────────────

    public final ForgeConfigSpec.BooleanValue enabled;
    public final ForgeConfigSpec.DoubleValue globalFogFar;
    public final ForgeConfigSpec.DoubleValue globalFogNight;
    public final ForgeConfigSpec.DoubleValue globalFogRain;
    public final ForgeConfigSpec.BooleanValue heightBasedFog;
    public final ForgeConfigSpec.DoubleValue heightFogLow;
    public final ForgeConfigSpec.DoubleValue heightFogHigh;
    public final ForgeConfigSpec.BooleanValue breathingFog;
    public final ForgeConfigSpec.DoubleValue breathingAmplitude;

    // ── Цвет тумана ──────────────────────────────────────────────────────

    public final ForgeConfigSpec.ConfigValue<List<? extends Double>> fogColorDay;
    public final ForgeConfigSpec.ConfigValue<List<? extends Double>> fogColorNight;
    public final ForgeConfigSpec.ConfigValue<List<? extends Double>> fogColorRain;

    // ── Настройки биомов ─────────────────────────────────────────────────
    // Формат списка: "namespace:biome_name,far_distance,r,g,b"
    // Пример: "minecraft:swamp,80,0.25,0.28,0.20"

    public final ForgeConfigSpec.ConfigValue<List<? extends String>> biomeFogEntries;

    // ���────────────────────────────────────────────────────────────────────

    private FogConfig(ForgeConfigSpec.Builder builder) {

        builder.comment("=== Smogline Fog Configuration ===",
                        "Настройки постапокалиптического тумана.",
                        "Биомы задаются в списке biomeFogEntries в формате:",
                        "  \"namespace:biome_id, дальность_тумана, R, G, B\"",
                        "Пример: \"minecraft:plains, 120.0, 0.50, 0.48, 0.36\"")
                .push("global");

        enabled = builder
                .comment("Включить постапокалиптический туман")
                .define("enabled", true);

        globalFogFar = builder
                .comment("Дальность тумана в ясную погоду (блоков). Стандарт: 160")
                .defineInRange("fogFarDefault", 160.0, 20.0, 512.0);

        globalFogNight = builder
                .comment("Дальность тумана ночью (блоков). Стандарт: 90")
                .defineInRange("fogFarNight", 90.0, 10.0, 512.0);

        globalFogRain = builder
                .comment("Дальность тумана в дождь (блоков). Стандарт: 60")
                .defineInRange("fogFarRain", 60.0, 10.0, 512.0);

        builder.pop().push("height");

        heightBasedFog = builder
                .comment("Включить зависимость густоты тумана от высоты (гуще у земли)")
                .define("enabled", true);

        heightFogLow = builder
                .comment("Ниже этой высоты туман максимально густой")
                .defineInRange("lowHeight", 48.0, -64.0, 320.0);

        heightFogHigh = builder
                .comment("Выше этой высоты туман минимальный (видимость на 40% лучше)")
                .defineInRange("highHeight", 128.0, -64.0, 320.0);

        builder.pop().push("breathing");

        breathingFog = builder
                .comment("Включить анимацию 'дыхания' тумана (плавная пульсация)")
                .define("enabled", true);

        breathingAmplitude = builder
                .comment("Амплитуда пульсации в блоках. 0 = отключить")
                .defineInRange("amplitude", 12.0, 0.0, 64.0);

        builder.pop().push("colors");

        fogColorDay = builder
                .comment("Цвет тумана днём [R, G, B]. Диапазон 0.0 - 1.0",
                        "Стандарт: серо-жёлтый смог [0.52, 0.50, 0.38]")
                .defineList("colorDay",
                        Arrays.asList(0.52, 0.50, 0.38),
                        e -> e instanceof Double d && d >= 0.0 && d <= 1.0);

        fogColorNight = builder
                .comment("Цвет тумана ночью [R, G, B].",
                        "Стандарт: тёмно-серый с синевой [0.12, 0.12, 0.16]")
                .defineList("colorNight",
                        Arrays.asList(0.12, 0.12, 0.16),
                        e -> e instanceof Double d && d >= 0.0 && d <= 1.0);

        fogColorRain = builder
                .comment("Цвет тумана в дождь [R, G, B].",
                        "Стандарт: грязно-серый [0.30, 0.30, 0.28]")
                .defineList("colorRain",
                        Arrays.asList(0.30, 0.30, 0.28),
                        e -> e instanceof Double d && d >= 0.0 && d <= 1.0);

        builder.pop().push("biomes");

        biomeFogEntries = builder
                .comment("Настройки тумана для конкретных биомов.",
                        "Формат каждой строки: \"namespace:biome_id, дальность, R, G, B\"",
                        "Дальность — в блоках. R/G/B — от 0.0 до 1.0.",
                        "Биомы из этого списка ПЕРЕОПРЕДЕЛЯЮТ глобальные настройки.",
                        "Примеры:")
                .defineList("biomeFogEntries",
                        Arrays.asList(
                                "smogline:inner_crater, 18.0, 0.08, 0.08, 0.10",
                                "smogline:outer_crater, 80.0, 0.22, 0.22, 0.20",
                                "minecraft:swamp,       100.0, 0.25, 0.28, 0.20",
                                "minecraft:dark_forest, 110.0, 0.20, 0.22, 0.18"
                        ),
                        e -> e instanceof String s && s.contains(",")
                );

        builder.pop();
    }
}
