package com.hbm_m.item;

import com.hbm_m.client.ModKeyBindings;
import com.hbm_m.entity.TurretBulletEntity;
import com.hbm_m.item.client.MachineGunRenderer;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.PacketReloadGun;
import com.hbm_m.sound.ModSounds;
import com.hbm_m.item.ModItems;
// Убедитесь, что импорт вашего класса патрона правильный
import com.hbm_m.item.AmmoTestItem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class MachineGunItem extends Item implements GeoItem {

    private static final int SHOT_ANIM_TICKS = 14;
    private static final float BULLET_SPEED = 3.0F;
    private static final float BULLET_DIVERGENCE = 1.5F;
    private static final int MAX_AMMO = 24;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public MachineGunItem(Properties properties) {
        super(properties);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    // === ЛОГИКА ПАТРОНОВ ===

    public int getAmmo(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        return tag.getInt("Ammo");
    }

    public void setAmmo(ItemStack stack, int ammo) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt("Ammo", Math.max(0, Math.min(ammo, MAX_AMMO)));
    }

    // Метод перезарядки (вызывается на сервере)
    public void reloadGun(Player player, ItemStack stack) {
        if (player.level().isClientSide) return;

        int currentAmmo = getAmmo(stack);
        if (currentAmmo >= MAX_AMMO) return;

        // Креатив: полная перезарядка бесплатно
        if (player.isCreative()) {
            setAmmo(stack, MAX_AMMO);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FLINTANDSTEEL_USE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            return;
        }

        int needed = MAX_AMMO - currentAmmo;
        int found = 0;

        // Ищем патроны в инвентаре
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slotStack = player.getInventory().getItem(i);

            // Проверка на класс патрона
            if (slotStack.getItem() instanceof AmmoTestItem) {
                int count = slotStack.getCount();
                int take = Math.min(count, needed - found);

                slotStack.shrink(take);
                found += take;

                // Удаляем пустой стак (хотя shrink обычно сам справляется, но для надежности)
                if (slotStack.isEmpty()) {
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                }

                if (found >= needed) break;
            }
        }

        // Если нашли патроны - обновляем оружие
        if (found > 0) {
            setAmmo(stack, currentAmmo + found);

            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.IRON_DOOR_OPEN, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.5F);

            // СИНХРОНИЗАЦИЯ: сообщаем клиенту об изменениях в инвентаре
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
        }
    }

    // === ВАНИЛЬНЫЕ МЕТОДЫ ===

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int ammo = getAmmo(stack);
        tooltip.add(Component.literal("Ammo: " + ammo + " / " + MAX_AMMO).withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide && stack.hasTag()) {
            CompoundTag tag = stack.getTag();
            if (tag.contains("ShootDelay")) {
                int delay = tag.getInt("ShootDelay");
                if (delay > 0) {
                    tag.putInt("ShootDelay", delay - 1);
                }
            }
        }
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        if (!(entity instanceof Player player)) return false;

        // На клиенте ничего не делаем, только отменяем ванильный свинг
        if (entity.level().isClientSide) return true;

        // Проверка патронов перед выстрелом
        if (getAmmo(stack) <= 0) {
            if (!player.getCooldowns().isOnCooldown(this)) {
                entity.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 2.0F);
                player.getCooldowns().addCooldown(this, 10);
            }
            return true;
        }

        CompoundTag tag = stack.getOrCreateTag();
        int currentDelay = tag.getInt("ShootDelay");

        if (currentDelay <= 0) {
            performShooting(entity.level(), player, stack);

            // Ставим задержку
            tag.putInt("ShootDelay", SHOT_ANIM_TICKS);

            // Запускаем анимацию на всех клиентах
            triggerAnim(player, GeoItem.getOrAssignId(stack, (ServerLevel)entity.level()), "controller", "shot");
        }

        return true;
    }

    private void performShooting(Level level, Player player, ItemStack stack) {
        // Трата патронов (ТОЛЬКО СЕРВЕР)
        if (!level.isClientSide && !player.isCreative()) {
            setAmmo(stack, getAmmo(stack) - 1);
            // Синхронизация, чтобы тултип обновился
            player.getInventory().setChanged();
        }

        TurretBulletEntity bullet = new TurretBulletEntity(level, player);

        // Позиционирование пули (от правой руки)
        Vec3 look = player.getLookAngle();
        Vec3 basePos = player.getEyePosition().add(0, -0.3, 0);
        float yaw = (float) Math.toRadians(-player.getYRot());
        Vec3 rightDir = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        Vec3 spawnPos = basePos.add(rightDir.scale(0.3)).add(look.scale(0.5));

        bullet.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        bullet.shoot(look.x, look.y, look.z, BULLET_SPEED, BULLET_DIVERGENCE);
        level.addFreshEntity(bullet);

        float pitch = 0.9F + level.random.nextFloat() * 0.2F;
        if (ModSounds.TURRET_FIRE.isPresent()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.TURRET_FIRE.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, pitch);
        } else {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 2.0F);
        }
    }

    // === GECKOLIB ===

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, event -> {
            return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }).triggerableAnim("shot", RawAnimation.begin().thenPlay("shot")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // === CLIENT SETUP ===

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private MachineGunRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new MachineGunRenderer();
                return renderer;
            }

            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                return HumanoidModel.ArmPose.CROSSBOW_HOLD;
            }
        });
    }

    // === HANDLERS ===

    @Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientHandlers {
        private static int clientTickCounter = 0;

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) return;

            ItemStack stack = mc.player.getMainHandItem();
            if (!(stack.getItem() instanceof MachineGunItem)) {
                clientTickCounter = 0;
                return;
            }

            // Перезарядка по R
            if (ModKeyBindings.RELOAD_KEY.consumeClick()) {
                ModPacketHandler.INSTANCE.sendToServer(new PacketReloadGun());
            }

            // Стрельба по ЛКМ
            if (mc.options.keyAttack.isDown()) {
                if (clientTickCounter <= 0) {
                    // Отправляем пакет на сервер (выстрел)
                    mc.player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));

                    // ХАК: Глушим локальную анимацию руки, чтобы не дергалась
                    mc.player.attackAnim = 0;
                    mc.player.oAttackAnim = 0;
                    mc.player.swinging = false;

                    clientTickCounter = SHOT_ANIM_TICKS;
                } else {
                    clientTickCounter--;
                }

                // Блокируем ванильную обработку клика (ломание блоков)
                while (mc.options.keyAttack.consumeClick()) { }
            } else {
                clientTickCounter = 0;
            }
        }

        @SubscribeEvent
        public static void onInput(net.minecraftforge.client.event.InputEvent.InteractionKeyMappingTriggered event) {
            if (event.isAttack()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && mc.player.getMainHandItem().getItem() instanceof MachineGunItem) {
                    event.setCanceled(true);
                    event.setSwingHand(false);
                }
            }
        }
    }

    @Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class CommonHandlers {
        @SubscribeEvent
        public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
            if (event.getItemStack().getItem() instanceof MachineGunItem && !event.getEntity().isCreative()) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onAttackEntity(AttackEntityEvent event) {
            if (event.getEntity().getMainHandItem().getItem() instanceof MachineGunItem) {
                event.setCanceled(true);
            }
        }
    }
}
