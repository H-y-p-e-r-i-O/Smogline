package com.smogline.hazard;

import net.minecraft.ChatFormatting;

public enum HazardType {

    RADIATION("hazard.smogline.radiation", "hazard.smogline.radiation.format", ChatFormatting.GREEN, true),
    
    HYDRO_REACTIVE("hazard.smogline.hydro_reactive", "hazard.smogline.explosion_strength.format", ChatFormatting.RED, true),
    
    EXPLOSIVE_ON_FIRE("hazard.smogline.explosive_on_fire", "hazard.smogline.explosion_strength.format", ChatFormatting.RED, true),
    
    PYROPHORIC("hazard.smogline.pyrophoric", "", ChatFormatting.GOLD, false);

    private final String translationKey;
    private final String formatTranslationKey;
    private final ChatFormatting color;
    private final boolean showValueInTooltip;

    HazardType(String translationKey, String formatTranslationKey, ChatFormatting color, boolean showValueInTooltip) {
        this.translationKey = translationKey;
        this.formatTranslationKey = formatTranslationKey;
        this.color = color;
        this.showValueInTooltip = showValueInTooltip;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public String getFormatTranslationKey() {
        return formatTranslationKey;
    }

    public ChatFormatting getColor() {
        return color;
    }

    public boolean shouldShowValueInTooltip() {
        return showValueInTooltip;
    }
}
