package com.smogline.network;

import com.smogline.lib.RefStrings;
import com.smogline.network.sounds.GeigerSoundPacket;
import com.smogline.network.ToggleWoodBurnerPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModPacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(RefStrings.MODID, "main_channel"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;
        // ... ваши существующие пакеты ...
        INSTANCE.registerMessage(id++, GeigerSoundPacket.class, GeigerSoundPacket::encode, GeigerSoundPacket::decode, GeigerSoundPacket::handle);
        INSTANCE.registerMessage(id++, RadiationDataPacket.class, RadiationDataPacket::encode, RadiationDataPacket::decode, RadiationDataPacket::handle);
        INSTANCE.registerMessage(id++, ChunkRadiationDebugBatchPacket.class, ChunkRadiationDebugBatchPacket::encode, ChunkRadiationDebugBatchPacket::decode, ChunkRadiationDebugBatchPacket::handle);
        INSTANCE.registerMessage(id++, GiveTemplateC2SPacket.class, GiveTemplateC2SPacket::encode, GiveTemplateC2SPacket::decode, GiveTemplateC2SPacket::handle);
        INSTANCE.registerMessage(id++, UpdateBatteryC2SPacket.class, UpdateBatteryC2SPacket::toBytes, UpdateBatteryC2SPacket::new, (msg, ctx) -> msg.handle(ctx));
        INSTANCE.registerMessage(id++, HighlightBlocksPacket.class, HighlightBlocksPacket::toBytes, HighlightBlocksPacket::fromBytes, HighlightBlocksPacket::handle);
        INSTANCE.registerMessage(id++, SetAssemblerRecipeC2SPacket.class, SetAssemblerRecipeC2SPacket::encode, SetAssemblerRecipeC2SPacket::decode, SetAssemblerRecipeC2SPacket::handle);
        INSTANCE.registerMessage(id++, ToggleWoodBurnerPacket.class, ToggleWoodBurnerPacket::encode, ToggleWoodBurnerPacket::decode, ToggleWoodBurnerPacket::handle);
        INSTANCE.registerMessage(id++, DetonateAllPacket.class, DetonateAllPacket::encode, DetonateAllPacket::decode, DetonateAllPacket::handle);
        INSTANCE.registerMessage(id++, AnvilCraftC2SPacket.class, AnvilCraftC2SPacket::encode, AnvilCraftC2SPacket::decode, AnvilCraftC2SPacket::handle);
        INSTANCE.registerMessage(id++, AnvilSelectRecipeC2SPacket.class, AnvilSelectRecipeC2SPacket::encode, AnvilSelectRecipeC2SPacket::decode, AnvilSelectRecipeC2SPacket::handle);

        INSTANCE.registerMessage(id++,
                com.smogline.network.packet.PacketSyncEnergy.class,
                com.smogline.network.packet.PacketSyncEnergy::encode,
                com.smogline.network.packet.PacketSyncEnergy::decode,
                com.smogline.network.packet.PacketSyncEnergy::handle
        );

        // Пакет перезарядки (ОБНОВИТЬ: использовать типизированный Supplier)
        INSTANCE.registerMessage(id++,
                PacketReloadGun.class,
                PacketReloadGun::toBytes,
                PacketReloadGun::new,
                PacketReloadGun::handle
        );

        // === ДОБАВЬТЕ ЭТОТ ПАКЕТ ДЛЯ СТРЕЛЬБЫ ===
        INSTANCE.registerMessage(id++,
                PacketShoot.class,
                PacketShoot::toBytes,
                PacketShoot::new,
                PacketShoot::handle
        );

        INSTANCE.registerMessage(id++,
                PacketUnloadGun.class,
                PacketUnloadGun::toBytes,
                PacketUnloadGun::new,
                PacketUnloadGun::handle
        );
    }
}
