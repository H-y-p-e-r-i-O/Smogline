package com.hbm_m.item;

import com.hbm_m.client.ModKeyBindings;
import com.hbm_m.entity.TurretBulletEntity;
import com.hbm_m.item.client.MachineGunRenderer;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.PacketReloadGun;
import com.hbm_m.network.PacketShoot;
import com.hbm_m.sound.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
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
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class MachineGunItem extends Item implements GeoItem {

    private static final int SHOT_ANIM_TICKS = 14;
    private static final float BULLET_SPEED = 3.0F;
    private static final float BULLET_DIVERGENCE = 1.5F;
    private static final int MAG_CAPACITY = 24;
    private static final int MAX_TOTAL_AMMO = MAG_CAPACITY + 1;

    private static final int RELOAD_ANIM_TICKS = 100;  // 5 сек
    private static final int FLIP_ANIM_TICKS = 80;     // 4 сек
    private static final int RELOAD_AMMO_ADD_TICK = 50; // 2.5 сек

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public MachineGunItem(Properties properties) {
        super(properties.stacksTo(1));
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        if (oldStack.getItem() == newStack.getItem() && !slotChanged) return false;
        return super.shouldCauseReequipAnimation(oldStack, newStack, slotChanged);
    }

    // === NBT МЕТОДЫ ===
    public int getAmmo(ItemStack stack) { return stack.getOrCreateTag().getInt("Ammo"); }
    public void setAmmo(ItemStack stack, int ammo) { stack.getOrCreateTag().putInt("Ammo", Math.max(0, Math.min(ammo, MAX_TOTAL_AMMO))); }

    public int getShootDelay(ItemStack stack) { return stack.getOrCreateTag().getInt("ShootDelay"); }
    public void setShootDelay(ItemStack stack, int delay) { stack.getOrCreateTag().putInt("ShootDelay", delay); }

    public int getReloadTimer(ItemStack stack) { return stack.getOrCreateTag().getInt("ReloadTimer"); }
    public void setReloadTimer(ItemStack stack, int timer) { stack.getOrCreateTag().putInt("ReloadTimer", timer); }

    public int getPendingAmmo(ItemStack stack) { return stack.getOrCreateTag().getInt("PendingAmmo"); }
    public void setPendingAmmo(ItemStack stack, int ammo) { stack.getOrCreateTag().putInt("PendingAmmo", ammo); }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (!level.isClientSide && entity instanceof Player player) {
            // Таймер стрельбы
            int delay = getShootDelay(stack);
            if (delay > 0) setShootDelay(stack, delay - 1);

            // Таймер перезарядки/flip
            int reloadTimer = getReloadTimer(stack);
            if (reloadTimer > 0) {
                setReloadTimer(stack, reloadTimer - 1);

                // На 2.5 сек анимации добавляем патроны в магазин
                if (reloadTimer == (RELOAD_ANIM_TICKS - RELOAD_AMMO_ADD_TICK) ||
                        reloadTimer == (FLIP_ANIM_TICKS - RELOAD_AMMO_ADD_TICK)) {

                    int pending = getPendingAmmo(stack);
                    if (pending > 0) {
                        setAmmo(stack, getAmmo(stack) + pending);
                        setPendingAmmo(stack, 0);
                        syncHand(player, stack);
                    }
                }
            }
        }
    }

    private void syncHand(Player player, ItemStack stack) {
        if (player instanceof ServerPlayer serverPlayer) {
            int slot = serverPlayer.getInventory().selected;
            serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(-2, 0, slot, stack));
        }
    }

    // === ПЕРЕЗАРЯДКА ===
    public void reloadGun(Player player, ItemStack stack) {
        if (player.level().isClientSide) return;

        // Если уже идёт перезарядка/flip - игнорируем
        if (getReloadTimer(stack) > 0) return;

        long instanceId = GeoItem.getOrAssignId(stack, (ServerLevel) player.level());
        int currentAmmo = getAmmo(stack);

        // 1️⃣ ПОЛНЫЙ МАГАЗИН → FLIP (просто выбрасываем пустой магазин)
        if (currentAmmo >= MAX_TOTAL_AMMO) {
            triggerAnim(player, instanceId, "controller", "flip");
            setReloadTimer(stack, FLIP_ANIM_TICKS);
            playSound(player, 1.0F);
            return;
        }

        // 2️⃣ CREATIVE MODE → RELOAD
        if (player.isCreative()) {
            int toAdd = MAX_TOTAL_AMMO - currentAmmo;
            setPendingAmmo(stack, toAdd);
            triggerAnim(player, instanceId, "controller", "reload");
            setReloadTimer(stack, RELOAD_ANIM_TICKS);
            playSound(player, 1.0F);
            return;
        }

        // 3️⃣ ПОИСК ПАТРОНОВ
        int needed = MAX_TOTAL_AMMO - currentAmmo;
        int foundTotal = 0;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slotStack = player.getInventory().getItem(i);
            if (!slotStack.isEmpty() && slotStack.getItem() instanceof AmmoTurretItem) {
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

        // РЕЗУЛЬТАТ:
        if (foundTotal > 0) {
            // Нашли хотя бы один патрон → RELOAD
            setPendingAmmo(stack, foundTotal);
            player.getInventory().setChanged();
            triggerAnim(player, instanceId, "controller", "reload");
            setReloadTimer(stack, RELOAD_ANIM_TICKS);
            playSound(player, 1.0F);
        } else {
            // Нет патронов → FLIP
            triggerAnim(player, instanceId, "controller", "flip");
            setReloadTimer(stack, FLIP_ANIM_TICKS);
            playSound(player, 1.5F);
        }
    }

    private void playSound(Player player, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.IRON_DOOR_OPEN, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, pitch);
    }

    // === СТРЕЛЬБА ===
    public void performShooting(Level level, Player player, ItemStack stack) {
        if (level.isClientSide) return;

        // 🔒 Блокировка при перезарядке/flip
        if (getReloadTimer(stack) > 0) return;
        if (getShootDelay(stack) > 0) return;

        int ammo = getAmmo(stack);
        if (ammo <= 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 2.0F);
            return;
        }

        if (!player.isCreative()) {
            setAmmo(stack, ammo - 1);
            syncHand(player, stack);
        }

        setShootDelay(stack, SHOT_ANIM_TICKS);

        TurretBulletEntity bullet = new TurretBulletEntity(level, player);
        Vec3 look = player.getLookAngle();
        Vec3 upGlobal = new Vec3(0, 1, 0);
        Vec3 right = look.cross(upGlobal);

        if (right.lengthSqr() < 1.0E-5) right = new Vec3(1, 0, 0);
        right = right.normalize();

        Vec3 upLocal = right.cross(look).normalize();

        double forwardOffset = 1.9;
        double rightOffset = 0.25;
        double downOffset = 0.13;

        Vec3 spawnPos = player.getEyePosition()
                .add(look.scale(forwardOffset))
                .add(right.scale(rightOffset))
                .add(upLocal.scale(-downOffset));

        bullet.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        bullet.shoot(look.x, look.y, look.z, BULLET_SPEED, BULLET_DIVERGENCE);
        bullet.yRotO = bullet.getYRot();
        bullet.xRotO = bullet.getXRot();
        level.addFreshEntity(bullet);

        float pitch = 0.9F + level.random.nextFloat() * 0.2F;
        if (ModSounds.TURRET_FIRE.isPresent()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.TURRET_FIRE.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, pitch);
        } else {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 2.0F);
        }

        triggerAnim(player, GeoItem.getOrAssignId(stack, (ServerLevel)level), "controller", "shot");
    }

    // === GECKOLIB КОНТРОЛЛЕР ===
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, event -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return PlayState.CONTINUE;

            ItemStack mainHandStack = mc.player.getMainHandItem();
            if (mainHandStack.getItem() != this) return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));

            // 🔒 ЗАЩИТА: reload и flip НЕ прерываются
            if (event.getController().getAnimationState() == AnimationController.State.RUNNING) {
                String currentAnim = event.getController().getCurrentAnimation().animation().name();
                if ("reload".equals(currentAnim) || "flip".equals(currentAnim)) {
                    return PlayState.CONTINUE;
                }
                if ("shot".equals(currentAnim)) {
                    return PlayState.CONTINUE;
                }
            }

            boolean isKeyDown = mc.options.keyAttack.isDown();
            boolean hasAmmo = getAmmo(mainHandStack) > 0;
            boolean isReloading = getReloadTimer(mainHandStack) > 0;

            if (isKeyDown && hasAmmo && !isReloading) {
                event.getController().forceAnimationReset();
                return event.setAndContinue(RawAnimation.begin().thenPlay("shot"));
            }

            return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        })
                // 🎯 РЕГИСТРИРУЕМ ТРИГГЕРНЫЕ АНИМАЦИИ
                .triggerableAnim("reload", RawAnimation.begin().thenPlay("reload"))
                .triggerableAnim("flip", RawAnimation.begin().thenPlay("flip"))
                .triggerableAnim("shot", RawAnimation.begin().thenPlay("shot")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

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

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int totalAmmo = getAmmo(stack);
        String displayString = (totalAmmo > MAG_CAPACITY)
                ? MAG_CAPACITY + " + " + (totalAmmo - MAG_CAPACITY)
                : String.valueOf(totalAmmo);
        tooltip.add(Component.literal("Ammo: " + displayString + " / " + MAG_CAPACITY).withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.NONE; }

    @Override
    public double getBoneResetTime() { return 0; }

    // === КЛИЕНТ ===
    @Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientHandlers {
        private static int clientShootTimer = 0;

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) return;

            ItemStack stack = mc.player.getMainHandItem();
            if (!(stack.getItem() instanceof MachineGunItem item)) {
                clientShootTimer = 0;
                return;
            }

            if (clientShootTimer > 0) clientShootTimer--;

            // Перезарядка
            if (ModKeyBindings.RELOAD_KEY.consumeClick()) {
                ModPacketHandler.INSTANCE.sendToServer(new PacketReloadGun());
                return;
            }

            // 🔒 Блокировка стрельбы при перезарядке
            if (item.getReloadTimer(stack) > 0) return;

            // Стрельба
            if (mc.options.keyAttack.isDown()) {
                if (item.getAmmo(stack) <= 0) return;

                if (clientShootTimer <= 0) {
                    ModPacketHandler.INSTANCE.sendToServer(new PacketShoot());
                    mc.player.attackAnim = 0;
                    mc.player.oAttackAnim = 0;
                    mc.player.swinging = false;
                    clientShootTimer = SHOT_ANIM_TICKS;
                }
            } else {
                if (clientShootTimer < SHOT_ANIM_TICKS - 2) clientShootTimer = 0;
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
