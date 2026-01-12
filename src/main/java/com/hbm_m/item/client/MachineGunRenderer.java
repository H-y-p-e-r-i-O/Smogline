package com.hbm_m.item.client;

import com.hbm_m.item.MachineGunItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class MachineGunRenderer extends GeoItemRenderer<MachineGunItem> {

    public MachineGunRenderer() {
        super(new MachineGunModel());
    }

    @Override
    public void renderRecursively(
            com.mojang.blaze3d.vertex.PoseStack poseStack,
            MachineGunItem animatable,
            software.bernie.geckolib.cache.object.GeoBone bone,
            net.minecraft.client.renderer.RenderType renderType,
            net.minecraft.client.renderer.MultiBufferSource bufferSource,
            com.mojang.blaze3d.vertex.VertexConsumer buffer,
            boolean isReRender,
            float partialTick,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        // Получаем текущее количество патронов из предмета
        int ammo = animatable.getAmmo(this.currentItemStack);

        // --- ЛОГИКА СКРЫТИЯ КОСТЕЙ ---
        // Имя кости, которую сейчас рендерим
        String boneName = bone.getName();

        // Предположим, что в Blockbench у вас кости патронов называются:
        // "ammo3" - самый дальний (исчезает первым, когда ammo < 3)
        // "ammo2" - средний (исчезает, когда ammo < 2)
        // "ammo1" - последний (исчезает, когда ammo < 1)

        // ВАЖНО: Замените "ammoX" на реальные имена костей из вашей модели!

        // Если патронов <= 1 (последний уже в стволе), то для ленты это "пусто" (0 визуально)
        int visibleAmmoInBelt = Math.max(0, ammo - 1);

        if (boneName.equals("ammo3")) {
            if (visibleAmmoInBelt < 3) return;
        }
        if (boneName.equals("ammo2")) {
            if (visibleAmmoInBelt < 2) return;
        }
        if (boneName.equals("ammo1")) {
            if (visibleAmmoInBelt < 1) return;
        }

        // Вызываем стандартный рендер для остальных костей (или если условия выше не сработали)
        super.renderRecursively(
                poseStack, animatable, bone, renderType, bufferSource,
                buffer, isReRender, partialTick, packedLight, packedOverlay,
                red, green, blue, alpha
        );
    }
}
