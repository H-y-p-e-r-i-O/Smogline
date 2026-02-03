package com.smogline.item.tags_and_tiers;

// Перечисление всех слитков в моде с поддержкой многоязычности.
// Для каждого слитка можно задать переводы на разные языки.

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum ModIngots {
    // Конструктор принимает название и пары "язык, перевод"
    URANIUM("uranium",
            "ru_ru", "Слиток урана",
            "en_us", "Uranium Ingot"),
    THORIUM232("th",
            "ru_ru", "Слиток урана",
            "en_us", "Thorium Ingot"),
    STEEL("steel",
            "ru_ru", "Стальной слиток",
            "en_us", "Steel Ingot"),
    ALUMINUM("aluminum",
            "ru_ru", "Слиток алюминия",
            "en_us", "Aluminum Ingot"),
    LEAD("lead",
            "ru_ru", "Свинцовый слиток",
            "en_us", "Lead Ingot"),
    ASBESTOS("asbestos",
            "ru_ru", "Асбестовый лист",
            "en_us", "Asbestos"),
    TITANIUM("titanium",
            "ru_ru", "Титановый слиток",
            "en_us", "Titanium Ingot"),
    COBALT("cobalt",
            "ru_ru", "Кобальтовый слиток",
            "en_us", "Cobalt Ingot"),
    TUNGSTEN("tungsten",
            "ru_ru", "Вольфрамовый слиток",
            "en_us", "Tungsten Ingot"),
    BERYLLIUM("beryllium",
            "ru_ru", "Бериллиевый слиток",
            "en_us", "Beryllium Ingot");

            
    // Чтобы добавить новый слиток, просто добавьте новую запись с его переводами

    private final String name;
    private final Map<String, String> translations;
    private static final Map<String, ModIngots> BY_NAME = new HashMap<>();

    /**
     * Конструктор для слитков с поддержкой нескольких языков.
     * @param name Системное имя (например, "uranium")
     * @param translationPairs Пары строк: "код_языка", "перевод". Например: "ru_ru", "Слиток", "en_us", "Ingot"
     */
    ModIngots(String name, String... translationPairs) {
        this.name = name;
        
        // Проверка, что количество элементов четное (пары ключ-значение)
        if (translationPairs.length % 2 != 0) {
            throw new IllegalArgumentException("Translation pairs must be even for ingot: " + name);
        }

        Map<String, String> translationMap = new HashMap<>();
        for (int i = 0; i < translationPairs.length; i += 2) {
            String locale = translationPairs[i];
            String translation = translationPairs[i + 1];
            translationMap.put(locale, translation);
        }
        // Делаем карту неизменяемой после создания
        this.translations = Collections.unmodifiableMap(translationMap);
    }

    static {
        for (ModIngots ingot : values()) {
            BY_NAME.put(ingot.name, ingot);
        }
    }

    public String getName() {
        return name;
    }

    /**
     * Получает перевод для указанного языка.
     * @param locale Код языка (например, "ru_ru").
     * @return Перевод или null, если он не найден.
     */
    public String getTranslation(String locale) {
        return translations.get(locale);
    }

    public static Optional<ModIngots> byName(String name) {
        return Optional.ofNullable(BY_NAME.get(name));
    }
}