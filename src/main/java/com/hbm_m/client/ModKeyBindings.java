package com.hbm_m.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class ModKeyBindings {
    public static final KeyMapping RELOAD_KEY = new KeyMapping(
            "key.hbm_m.reload",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.hbm_m"
    );
    public static final KeyMapping UNLOAD_KEY = new KeyMapping(
            "key.hbm_m.unload", // Название в настройках
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G, // Кнопка G по умолчанию
            "key.categories.hbm_m"
    );


}
