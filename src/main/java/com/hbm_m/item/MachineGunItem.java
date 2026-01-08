package com.hbm_m.item;

import com.hbm_m.client.ModKeyBindings;
import com.hbm_m.entity.TurretBulletEntity;
import com.hbm_m.item.client.MachineGunRenderer;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.PacketReloadGun;
import com.hbm_m.network.PacketShoot;
import com.hbm_m.sound.ModSounds;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.AmmoTestItem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
        // ВАЖНО: Принудительно ставим макс. стак = 1
        super(properties.stacksTo(1));
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        if (oldStack.getItem() == newStack.getItem() && !slotChanged) {
            return false;
        }
        return super.shouldCauseReequipAnimation(oldStack, newStack, slotChanged);
    }

    public int getAmmo(ItemStack stack) {
        return stack.getOrCreateTag().getInt("Ammo");
    }

    public void setAmmo(ItemStack stack, int ammo) {
        stack.getOrCreateTag().putInt("Ammo", Math.max(0, Math.min(ammo, MAX_AMMO)));
    }

    public int getShootDelay(ItemStack stack) {
        return stack.getOrCreateTag().getInt("ShootDelay");
    }

    public void setShootDelay(ItemStack stack, int delay) {
        stack.getOrCreateTag().putInt("ShootDelay", delay);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide) {
            int delay = getShootDelay(stack);
            if (delay > 0) {
                setShootDelay(stack, delay - 1);
            }
        }
    }

    private void syncHand(Player player, ItemStack stack) {
        if (player instanceof ServerPlayer serverPlayer) {
            int slot = serverPlayer.getInventory().selected;
            serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(
                    -2, 0, slot, stack));
        }
    }

    public void reloadGun(Player player, ItemStack stack) {
        if (player.level().isClientSide) return;

        int currentAmmo = getAmmo(stack);
        if (currentAmmo >= MAX_AMMO) return;

        if (player.isCreative()) {
            setAmmo(stack, MAX_AMMO);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FLINTANDSTEEL_USE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            syncHand(player, stack);
            return;
        }

        int needed = MAX_AMMO - currentAmmo;
        int foundTotal = 0;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slotStack = player.getInventory().getItem(i);
            if (!slotStack.isEmpty() && slotStack.getItem() instanceof AmmoTestItem) {
                int inSlot = slotStack.getCount();
                int toTake = Math.min(inSlot, needed - foundTotal);

                if (toTake > 0) {
                    slotStack.shrink(toTake);
                    foundTotal += toTake;
                    if (slotStack.isEmpty()) {
                        player.getInventory().setItem(i, ItemStack.EMPTY);
                    }
                }
                if (foundTotal >= needed) break;
            }
        }

        if (foundTotal > 0) {
            setAmmo(stack, currentAmmo + foundTotal);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.IRON_DOOR_OPEN, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.5F);
            player.getInventory().setChanged();
            syncHand(player, stack);
        }
    }

    public void performShooting(Level level, Player player, ItemStack stack) {
        if (level.isClientSide) return;

        if (getShootDelay(stack) > 0) return;

        if (getAmmo(stack) <= 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 2.0F);
            return;
        }

        if (!player.isCreative()) {
            setAmmo(stack, getAmmo(stack) - 1);
            syncHand(player, stack);
        }

        setShootDelay(stack, SHOT_ANIM_TICKS);

        TurretBulletEntity bullet = new TurretBulletEntity(level, player);

        // Позиционирование
        Vec3 look = player.getLookAngle();
        Vec3 eyePos = player.getEyePosition();
        float yawRad = (float) Math.toRadians(-player.getYRot());
        Vec3 right = new Vec3(Math.cos(yawRad), 0, Math.sin(yawRad));

        // Смещение ствола
        double forwardOffset = 0.8;
        double rightOffset = 0.25;
        double downOffset = -0.25;

        Vec3 spawnPos = eyePos
                .add(look.scale(forwardOffset))
                .add(right.scale(rightOffset))
                .add(0, downOffset, 0);

        bullet.setPos(spawnPos.x, spawnPos.y, spawnPos.z);

        // 1. Задаем вектор скорости (включая разброс)
        bullet.shoot(look.x, look.y, look.z, BULLET_SPEED, BULLET_DIVERGENCE);

        // 2. СИНХРОНИЗАЦИЯ ПОВОРОТА (ПОСЛЕ shoot, ТАК КАК ОН МЕНЯЕТ ВЕКТОР)
        // Берем итоговый вектор скорости (с учетом разброса)
        Vec3 velocity = bullet.getDeltaMovement();
        double hDist = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

        // Математика поворота (как в турели)
        float yRot = (float)(Math.atan2(velocity.x, velocity.z) * (180D / Math.PI));
        float xRot = (float)(Math.atan2(velocity.y, hDist) * (180D / Math.PI));

        bullet.setYRot(yRot);
        bullet.setXRot(xRot);
        // Важно для интерполяции первого кадра
        bullet.yRotO = yRot;
        bullet.xRotO = xRot;

        level.addFreshEntity(bullet);

        float pitch = 0.9F + level.random.nextFloat() * 0.2F;
        if (ModSounds.TURRET_FIRE.isPresent()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TURRET_FIRE.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, pitch);
        } else {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 2.0F);
        }

        triggerAnim(player, GeoItem.getOrAssignId(stack, (ServerLevel)level), "controller", "shot");
    }

    // === GECKOLIB & CLIENT ===
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, event -> {
            return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }).triggerableAnim("shot", RawAnimation.begin().thenPlay("shot")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override
    public void initializeClient(Consumer consumer) {
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

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List tooltip, TooltipFlag flag) {
        int ammo = getAmmo(stack);
        tooltip.add(Component.literal("Ammo: " + ammo + " / " + MAX_AMMO).withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public InteractionResultHolder use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.NONE; }

    @Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientHandlers {
        private static int clientShootTimer = 0;

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) return;

            ItemStack stack = mc.player.getMainHandItem();
            if (!(stack.getItem() instanceof MachineGunItem)) {
                clientShootTimer = 0;
                return;
            }

            if (clientShootTimer > 0) clientShootTimer--;

            if (ModKeyBindings.RELOAD_KEY.consumeClick()) {
                ModPacketHandler.INSTANCE.sendToServer(new PacketReloadGun());
            }

            if (mc.options.keyAttack.isDown()) {
                if (clientShootTimer <= 0) {
                    ModPacketHandler.INSTANCE.sendToServer(new PacketShoot());
                    mc.player.attackAnim = 0;
                    mc.player.oAttackAnim = 0;
                    mc.player.swinging = false;
                    clientShootTimer = SHOT_ANIM_TICKS;
                }
                while (mc.options.keyAttack.consumeClick()) { }
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
    }
}
