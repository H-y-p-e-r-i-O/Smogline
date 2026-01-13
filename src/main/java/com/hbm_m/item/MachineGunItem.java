package com.hbm_m.item;

import com.hbm_m.client.ModKeyBindings;
import com.hbm_m.entity.TurretBulletEntity;
import com.hbm_m.item.client.MachineGunRenderer;
import com.hbm_m.item.tags_and_tiers.AmmoRegistry;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.PacketReloadGun;
import com.hbm_m.network.PacketShoot;
import com.hbm_m.sound.ModSounds;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraftforge.registries.ForgeRegistries;

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
    private static final int MAG_CAPACITY = 24;
    private static final int MAX_TOTAL_AMMO = MAG_CAPACITY + 1;
    private static final int RELOAD_ANIM_TICKS = 100;
    private static final int FLIP_ANIM_TICKS = 80;
    private static final int RELOAD_AMMO_ADD_TICK = 50;
    private static final String LOADED_AMMO_ID_TAG = "LoadedAmmoID";

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
    public String getLoadedAmmoID(ItemStack stack) { return stack.getOrCreateTag().getString(LOADED_AMMO_ID_TAG); }
    public void setLoadedAmmoID(ItemStack stack, String ammoID) { stack.getOrCreateTag().putString(LOADED_AMMO_ID_TAG, ammoID); }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!level.isClientSide && entity instanceof Player player) {
            int delay = getShootDelay(stack);
            if (delay > 0) setShootDelay(stack, delay - 1);

            int reloadTimer = getReloadTimer(stack);
            if (reloadTimer > 0) {
                setReloadTimer(stack, reloadTimer - 1);
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
        if (getReloadTimer(stack) > 0) return;

        long instanceId = GeoItem.getOrAssignId(stack, (ServerLevel) player.level());
        int currentAmmo = getAmmo(stack);

        // 1) Полный магазин -> FLIP (разрядить/проверить)
        if (currentAmmo >= MAX_TOTAL_AMMO) {
            triggerAnim(player, instanceId, "controller", "flip");
            setReloadTimer(stack, FLIP_ANIM_TICKS);
            playSound(player, 1.0F);
            return;
        }

        String currentLoadedID = getLoadedAmmoID(stack);

        // Ищем патрон, который МОЖНО зарядить.
        // Если в магазине что-то есть (currentAmmo > 0) -> ищем СТРОГО такой же ID.
        // Если магазин пуст -> ищем любой подходящий калибра "20mm_turret".
        String targetAmmoId = findAmmoIdForReload(player, (currentAmmo > 0 && currentLoadedID != null && !currentLoadedID.isEmpty()) ? currentLoadedID : null);

        // 2) Если подходящих патронов в инвентаре НЕТ -> FLIP (даже в креативе!)
        if (targetAmmoId == null) {
            triggerAnim(player, instanceId, "controller", "flip");
            setReloadTimer(stack, FLIP_ANIM_TICKS);
            playSound(player, 1.5F); // Звук "пусто" или "затвор"
            return;
        }

        // 3) Патроны ЕСТЬ (мы нашли targetAmmoId). Начинаем перезарядку.

        // КРЕАТИВ:
        if (player.isCreative()) {
            int toAdd = MAX_TOTAL_AMMO - currentAmmo;
            setPendingAmmo(stack, toAdd);

            // Если магазин был пуст — ставим тип найденного патрона
            if (currentAmmo == 0) {
                setLoadedAmmoID(stack, targetAmmoId);
            }

            triggerAnim(player, instanceId, "controller", "reload");
            setReloadTimer(stack, RELOAD_ANIM_TICKS);
            playSound(player, 1.0F);
            return;
        }

        // ВЫЖИВАНИЕ:
        int needed = MAX_TOTAL_AMMO - currentAmmo;
        int taken = consumeAmmoById(player, targetAmmoId, needed);

        if (taken > 0) {
            if (currentAmmo == 0) {
                setLoadedAmmoID(stack, targetAmmoId);
            }
            setPendingAmmo(stack, taken);
            player.getInventory().setChanged();
            triggerAnim(player, instanceId, "controller", "reload");
            setReloadTimer(stack, RELOAD_ANIM_TICKS);
            playSound(player, 1.0F);
        } else {
            // На всякий случай (хотя проверка выше должна была отловить) -> FLIP
            triggerAnim(player, instanceId, "controller", "flip");
            setReloadTimer(stack, FLIP_ANIM_TICKS);
            playSound(player, 1.5F);
        }
    }

    /** Ищет первый подходящий ID патрона в инвентаре. Если requiredId != null, ищет строго его. */
    @Nullable
    private String findAmmoIdForReload(Player player, @Nullable String requiredId) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (slot.isEmpty()) continue;
            if (!AmmoRegistry.isValidAmmo(slot)) continue;

            String caliber = AmmoRegistry.getCaliber(slot);
            if (!"20mm_turret".equals(caliber)) continue;

            String id = ForgeRegistries.ITEMS.getKey(slot.getItem()).toString();

            // Если нам нужен конкретный ID (дозарядка), пропускаем все остальные
            if (requiredId != null && !requiredId.equals(id)) continue;

            return id; // Нашли подходящий!
        }
        return null;
    }

    /** Забирает патроны конкретного ID из инвентаря. */
    private int consumeAmmoById(Player player, String ammoId, int needed) {
        int taken = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (taken >= needed) break;

            ItemStack slot = player.getInventory().getItem(i);
            if (slot.isEmpty()) continue;
            if (!AmmoRegistry.isValidAmmo(slot)) continue;

            String id = ForgeRegistries.ITEMS.getKey(slot.getItem()).toString();
            if (!ammoId.equals(id)) continue;

            int toTake = Math.min(slot.getCount(), needed - taken);
            slot.shrink(toTake);
            taken += toTake;
            if (slot.isEmpty()) player.getInventory().setItem(i, ItemStack.EMPTY);
        }
        return taken;
    }


    // Вспомогательный метод поиска ID патрона
    private String findAmmoIDInInventory(Player player, ItemStack gunStack) {
        String currentLoadedID = getLoadedAmmoID(gunStack);
        int currentAmmo = getAmmo(gunStack);

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slotStack = player.getInventory().getItem(i);
            if (!slotStack.isEmpty() && AmmoRegistry.isValidAmmo(slotStack)) {

                // Проверка калибра
                String caliber = AmmoRegistry.getCaliber(slotStack);
                if (!"20mm_turret".equals(caliber)) continue;

                String slotItemID = ForgeRegistries.ITEMS.getKey(slotStack.getItem()).toString();

                // Если в оружии уже есть патроны, ищем только такие же
                if (currentAmmo > 0 && currentLoadedID != null && !currentLoadedID.isEmpty()) {
                    if (currentLoadedID.equals(slotItemID)) return slotItemID;
                } else {
                    // Если оружие пустое, возвращаем первый подходящий
                    return slotItemID;
                }
            }
        }
        return null;
    }

    // Вспомогательный метод изъятия патронов
    private int consumeAmmo(Player player, String targetID, int countNeeded) {
        int gathered = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (gathered >= countNeeded) break;

            ItemStack slotStack = player.getInventory().getItem(i);
            if (!slotStack.isEmpty() && AmmoRegistry.isValidAmmo(slotStack)) {
                String slotItemID = ForgeRegistries.ITEMS.getKey(slotStack.getItem()).toString();
                if (targetID.equals(slotItemID)) {
                    int take = Math.min(slotStack.getCount(), countNeeded - gathered);
                    slotStack.shrink(take);
                    gathered += take;
                    if (slotStack.isEmpty()) {
                        player.getInventory().setItem(i, ItemStack.EMPTY);
                    }
                }
            }
        }
        return gathered;
    }

    private void playSound(Player player, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.IRON_DOOR_OPEN, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, pitch);
    }

    // === СТРЕЛЬБА ===
    public void performShooting(Level level, Player player, ItemStack stack) {
        // ✅ ТОЛЬКО НА СЕРВЕРЕ!
        if (level.isClientSide) return;

        if (getReloadTimer(stack) > 0 || getShootDelay(stack) > 0) return;

        int ammo = getAmmo(stack);
        if (ammo <= 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 2.0F);
            return;
        }

        if (!player.isCreative()) {
            setAmmo(stack, ammo - 1);
            if (ammo - 1 <= 0) setLoadedAmmoID(stack, "");
        }

        syncHand(player, stack);
        setShootDelay(stack, SHOT_ANIM_TICKS);

        // ✅ СПАВН ПУЛИ НА СЕРВЕРЕ
        if (!(level instanceof ServerLevel serverLevel)) return;

        // 1. Создаем пулю
        TurretBulletEntity bullet = new TurretBulletEntity(serverLevel, player);

        // 2. Боеприпас - ✅ ИСПРАВЛЕНО
        String loadedID = getLoadedAmmoID(stack);
        AmmoRegistry.AmmoType ammoInfo = null;

        if (loadedID != null && !loadedID.isEmpty()) {
            // ✅ ПРАВИЛЬНО: ищем через ForgeRegistries
            net.minecraft.world.item.Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(loadedID));
            if (item != null) {
                ammoInfo = AmmoRegistry.getAmmoTypeFromItem(item);
            }
        }

        if (ammoInfo == null) {
            ammoInfo = new AmmoRegistry.AmmoType("default", "20mm_turret", 6.0f, 3.0f, false);
        }

        bullet.setAmmoType(ammoInfo);

        // 3. Параметры выстрела
        Vec3 lookDir = player.getLookAngle();
        Vec3 velocity = lookDir.normalize().add(
                level.random.nextGaussian() * 0.0075 * 1.0F,
                level.random.nextGaussian() * 0.0075 * 1.0F,
                level.random.nextGaussian() * 0.0075 * 1.0F
        ).scale(ammoInfo.speed);

        // 4. Смещение вправо
        Vec3 right = lookDir.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 spawnPos = player.position().add(right.scale(0.2)).add(0, player.getEyeY() - player.getY() - 0.1, 0);

        // 5. Устанавливаем
        bullet.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        bullet.setDeltaMovement(velocity);
        bullet.alignToVelocity();

        // ✅ ДОБАВЛЯЕМ В МИР
        serverLevel.addFreshEntity(bullet);

        // Звук
        float pitch = 0.9F + level.random.nextFloat() * 0.2F;
        SoundEvent shotSound = ModSounds.TURRET_FIRE.isPresent() ? ModSounds.TURRET_FIRE.get() : SoundEvents.GENERIC_EXPLODE;
        level.playSound(null, player.getX(), player.getY(), player.getZ(), shotSound, SoundSource.PLAYERS, 1.0F, pitch);

        // Анимация
        triggerAnim(player, GeoItem.getOrAssignId(stack, serverLevel), "controller", "shot");
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
        super.appendHoverText(stack, level, tooltip, flag);

        String ammoId = getLoadedAmmoID(stack);
        if (ammoId == null || ammoId.isEmpty()) {
            tooltip.add(Component.literal("Патроны: нет").withStyle(ChatFormatting.RED));
            return;
        }

        AmmoRegistry.AmmoType ammoType = AmmoRegistry.getAmmoTypeById(ammoId); // твой реестр боеприпасов [file:3]
        if (ammoType == null) {
            tooltip.add(Component.literal("Патроны: неизвестно").withStyle(ChatFormatting.GRAY));
            return;
        }

        // Базовые характеристики
        float dmg = ammoType.damage;
        float spd = ammoType.speed;
        boolean piercing = ammoType.isPiercing;

        // Определяем тип (для текста) по id
        String typeText = "обычная";
        if (ammoId.contains("piercing")) typeText = "пробивная";
        else if (ammoId.contains("hollow")) typeText = "экспансивная";
        else if (ammoId.contains("fire") || ammoId.contains("incendiary")) typeText = "зажигательная";

        // Кратко: урон, скорость, тип
        tooltip.add(Component.literal("Патрон: " + typeText).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(String.format("Урон: %.1f", dmg)).withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal(String.format("Скорость: %.1f", spd)).withStyle(ChatFormatting.DARK_AQUA));

        // Особенности
        if ("пробивная".equals(typeText)) {
            tooltip.add(Component.literal("Частично игнорирует броню").withStyle(ChatFormatting.BLUE));
        } else if ("экспансивная".equals(typeText)) {
            tooltip.add(Component.literal("Х2 по без брони, слабее по тяжёлой броне").withStyle(ChatFormatting.BLUE));
        } else if ("зажигательная".equals(typeText)) {
            tooltip.add(Component.literal("Поджигает цель на 5 секунд").withStyle(ChatFormatting.BLUE));
        }

        if (piercing && !"пробивная".equals(typeText)) {
            tooltip.add(Component.literal("Пробивная способность").withStyle(ChatFormatting.GRAY));
        }
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

            if (ModKeyBindings.RELOAD_KEY.consumeClick()) {
                ModPacketHandler.INSTANCE.sendToServer(new PacketReloadGun());
                return;
            }

            if (item.getReloadTimer(stack) > 0) return;

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
