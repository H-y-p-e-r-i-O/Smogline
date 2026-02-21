package com.smogline.mixin;

import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
@Mixin(WorldCreationUiState.class)
public abstract class WorldPresetsMixin {

    @Shadow
    private WorldCreationUiState.WorldTypeEntry worldType;

    @Shadow
    public abstract List<WorldCreationUiState.WorldTypeEntry> getNormalPresetList();

    @Shadow
    public abstract void setWorldType(WorldCreationUiState.WorldTypeEntry pWorldType);

    /**
     * После конструктора — если текущий worldType это minecraft:normal,
     * заменяем его на первый элемент из нашего списка (smogline:smogline)
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void smogline$overrideDefaultWorldType(CallbackInfo ci) {
        Holder<WorldPreset> currentPreset = this.worldType.preset();

        boolean isVanillaDefault = false;
        if (currentPreset != null) {
            Optional<ResourceKey<WorldPreset>> keyOpt = currentPreset.unwrapKey();
            isVanillaDefault = keyOpt.isPresent() && keyOpt.get().equals(WorldPresets.NORMAL);
        }

        if (isVanillaDefault || currentPreset == null) {
            List<WorldCreationUiState.WorldTypeEntry> presets = this.getNormalPresetList();
            if (!presets.isEmpty()) {
                System.out.println("[Smogline] Replacing default world type with: " +
                        presets.get(0).preset().unwrapKey().map(k -> k.location().toString()).orElse("unknown"));
                this.setWorldType(presets.get(0));
            }
        }
    }
}