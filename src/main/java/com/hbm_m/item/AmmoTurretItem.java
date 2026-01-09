package com.hbm_m.item;

import com.hbm_m.item.client.AmmoTurretRenderer;
import com.hbm_m.lib.RefStrings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class AmmoTurretItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public AmmoTurretItem(Properties properties) {
        super(properties);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    // === GECKOLIB SETUP ===
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> PlayState.CONTINUE)
                .triggerableAnim("flip", RawAnimation.begin().thenPlay("flip")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private AmmoTurretRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new AmmoTurretRenderer();
                return renderer;
            }
        });
    }

    @SubscribeEvent
    public static void onAmmoTurretClick(InputEvent.InteractionKeyMappingTriggered event) {
        // Проверяем ЛКМ (Attack)
        if (event.isAttack()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.getMainHandItem().getItem() instanceof AmmoTurretItem) {

                ItemStack stack = mc.player.getMainHandItem();

                // Проверяем кулдаун
                if (!mc.player.getCooldowns().isOnCooldown(stack.getItem())) {

                    // Генерируем ID для анимации
                    CompoundTag tag = stack.getOrCreateTag();
                    long instanceId;
                    if (tag.contains("GeckoLibID")) {
                        instanceId = tag.getLong("GeckoLibID");
                    } else {
                        instanceId = java.util.UUID.randomUUID().getMostSignificantBits();
                        tag.putLong("GeckoLibID", instanceId);
                    }

                    // Запускаем анимацию "flip"
                    ((AmmoTurretItem)stack.getItem()).triggerAnim(
                            mc.player, instanceId, "controller", "flip");

                    // Ставим кулдаун (3 сек = 60 тиков)
                    mc.player.getCooldowns().addCooldown(stack.getItem(), 60);
                }

                // ВАЖНО: Отменяем удар рукой
                event.setCanceled(true);
                event.setSwingHand(false);
            }
        }
    }
}

