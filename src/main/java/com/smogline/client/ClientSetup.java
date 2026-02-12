package com.smogline.client;

// Основной класс клиентской настройки мода. Здесь регистрируются все клиентские обработчики событий,
// GUI, рендереры, модели и т.д.
import com.smogline.block.entity.client.MineApRenderer;
import com.smogline.block.entity.client.MotorElectroRenderer;
import com.smogline.block.entity.client.ShaftIronRenderer;
import com.smogline.block.entity.client.TurretLightPlacerRenderer;
import com.smogline.client.model.FluidTankModelWrapper;
import com.smogline.client.overlay.*;
import com.smogline.client.loader.*;
import com.smogline.client.overlay.crates.GUIDeshCrate;
import com.smogline.client.overlay.crates.GUIIronCrate;
import com.smogline.client.overlay.crates.GUISteelCrate;
import com.smogline.client.overlay.turrets.GUITurretAmmo;
import com.smogline.client.render.*;
import com.smogline.client.render.shader.*;
import com.smogline.config.*;
import com.smogline.client.tooltip.*;
import com.smogline.entity.ModEntities;
import com.smogline.entity.client.bullets.TurretBulletRenderer;
import com.smogline.entity.client.turrets.TurretLightLinkedRenderer;
import com.smogline.entity.client.turrets.TurretLightRenderer;
import com.smogline.item.custom.industrial.ItemAssemblyTemplate;
import com.smogline.item.custom.industrial.ItemBlueprintFolder;
import com.smogline.item.ModItems;
import com.smogline.item.tags_and_tiers.ModTags;
import com.smogline.lib.RefStrings;
import com.smogline.main.MainRegistry;
import com.smogline.menu.ModMenuTypes;
import com.smogline.multiblock.DoorPartAABBRegistry;
import com.smogline.particle.ModParticleTypes;
import com.smogline.recipe.AssemblerRecipe;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.google.common.collect.ImmutableMap;
import com.smogline.particle.custom.*;
import com.smogline.block.ModBlocks;
import com.smogline.block.entity.ModBlockEntities;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraft.client.GraphicsStatus;
import net.minecraftforge.client.ChunkRenderTypeSet;

import javax.annotation.Nonnull;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        MainRegistry.LOGGER.info("FMLClientSetupEvent fired. Registering client-side FORGE event handlers.");
        // Обработчики, которые должны работать каждый тик/каждое событие в игре,
        // регистрируются на шине MinecraftForge.EVENT_BUS.
        MinecraftForge.EVENT_BUS.register(ModConfigKeybindHandler.class);
        MinecraftForge.EVENT_BUS.register(DarkParticleHandler.class);
        MinecraftForge.EVENT_BUS.register(ChunkRadiationDebugRenderer.class);
        MinecraftForge.EVENT_BUS.register(ClientRenderHandler.class);
        // MinecraftForge.EVENT_BUS.register(DoorDebugRenderer.class);
        // MinecraftForge.EVENT_BUS.register(ClientSetup.class);

        // ✅ ДОБАВЬТЕ ЭТО:
        ModItems.MACHINEGUN.ifPresent(item -> {
            ItemProperties.register(item,
                    new ResourceLocation(RefStrings.MODID, "pull"),
                    (pStack, pLevel, pEntity, pSeed) -> {
                        if (pEntity != null && pEntity.isUsingItem() && pEntity.getUseItem() == pStack) {
                            return 1.0f;
                        }
                        return 0.0f;
                    });
        });

// Register Entity Renders


        // Register Entity Renders
        ModEntities.GRENADE_NUC_PROJECTILE.ifPresent(entityType ->
                EntityRenderers.register(entityType, ThrownItemRenderer::new)
        );
        ModEntities.GRENADE_IF_FIRE_PROJECTILE.ifPresent(entityType ->
                EntityRenderers.register(entityType, ThrownItemRenderer::new)
        );
        ModEntities.GRENADE_IF_SLIME_PROJECTILE.ifPresent(entityType ->
                EntityRenderers.register(entityType, ThrownItemRenderer::new)
        );
        ModEntities.GRENADE_IF_HE_PROJECTILE.ifPresent(entityType ->
                EntityRenderers.register(entityType, ThrownItemRenderer::new)
        );
        ModEntities.GRENADE_PROJECTILE.ifPresent(entityType ->
                EntityRenderers.register(entityType, ThrownItemRenderer::new)
        );
        ModEntities.GRENADEHE_PROJECTILE.ifPresent(entityType ->
                EntityRenderers.register(entityType, ThrownItemRenderer::new)
        );
        ModEntities.GRENADEFIRE_PROJECTILE.ifPresent(entityType ->
                EntityRenderers.register(entityType, ThrownItemRenderer::new)
        );
        ModEntities.GRENADESMART_PROJECTILE.ifPresent(entityType ->
                EntityRenderers.register(entityType, ThrownItemRenderer::new)
        );
        ModEntities.GRENADESLIME_PROJECTILE.ifPresent(entityType ->
                EntityRenderers.register(entityType, ThrownItemRenderer::new)
        );
        ModEntities.GRENADE_IF_PROJECTILE.ifPresent(entityType ->
                EntityRenderers.register(entityType, ThrownItemRenderer::new)
        );

        MinecraftForge.EVENT_BUS.register(new com.smogline.event.MachineGunEventHandler());


        // MinecraftForge.EVENT_BUS.register(new ClientTickHandler());

        event.enqueueWork(() -> {
            // Здесь мы связываем наш тип меню с классом экрана
            MenuScreens.register(ModMenuTypes.TURRET_AMMO_MENU.get(), GUITurretAmmo::new);
            MenuScreens.register(ModMenuTypes.ARMOR_TABLE_MENU.get(), GUIArmorTable::new);
            MenuScreens.register(ModMenuTypes.MACHINE_ASSEMBLER_MENU.get(), GUIMachineAssembler::new);
            MenuScreens.register(ModMenuTypes.ADVANCED_ASSEMBLY_MACHINE_MENU.get(), GUIMachineAdvancedAssembler::new);
            MenuScreens.register(ModMenuTypes.MACHINE_BATTERY_MENU.get(), GUIMachineBattery::new);
            MenuScreens.register(ModMenuTypes.BLAST_FURNACE_MENU.get(), GUIBlastFurnace::new);
            MenuScreens.register(ModMenuTypes.PRESS_MENU.get(), GUIMachinePress::new);
            MenuScreens.register(ModMenuTypes.SHREDDER_MENU.get(), GUIMachineShredder::new);
            MenuScreens.register(ModMenuTypes.WOOD_BURNER_MENU.get(), GUIMachineWoodBurner::new);
            MenuScreens.register(ModMenuTypes.ANVIL_MENU.get(), GUIAnvil::new);
            MenuScreens.register(ModMenuTypes.IRON_CRATE_MENU.get(), GUIIronCrate::new);
            MenuScreens.register(ModMenuTypes.STEEL_CRATE_MENU.get(), GUISteelCrate::new);
            MenuScreens.register(ModMenuTypes.DESH_CRATE_MENU.get(), GUIDeshCrate::new);
            MenuScreens.register(ModMenuTypes.FLUID_TANK_MENU.get(), GUIMachineFluidTank::new);

            // Register BlockEntity renderers
            BlockEntityRenderers.register(ModBlockEntities.ADVANCED_ASSEMBLY_MACHINE_BE.get(), MachineAdvancedAssemblerRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.PRESS_BE.get(), MachinePressRenderer::new);
            // СТАЛО (ПРАВИЛЬНО: Берем из ModBlockEntities)
            BlockEntityRenderers.register(ModBlockEntities.TURRET_LIGHT_PLACER_BE.get(), TurretLightPlacerRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.MOTOR_ELECTRO_BE.get(), MotorElectroRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.SHAFT_IRON_BE.get(), ShaftIronRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.MINE_BLOCK_ENTITY.get(), MineApRenderer::new);


            OcclusionCullingHelper.setTransparentBlocksTag(ModTags.Blocks.NON_OCCLUDING);
            try {
                RenderPathManager.updateRenderPath();
                MainRegistry.LOGGER.info("VBO render system initialized successfully");
            } catch (Exception e) {
                MainRegistry.LOGGER.error("Failed to initialize VBO render system", e);
            }
            
            // ДОБАВИТЬ: Регистрация обработчика отключения от сервера
            MinecraftForge.EVENT_BUS.addListener(ClientSetup::onClientDisconnect);
            MainRegistry.LOGGER.info("Initial render path check completed");
        });
    }



    @SubscribeEvent
    public static void onModelBake(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, BakedModel> modelRegistry = event.getModels();
        
        // Получаем ResourceLocation для нашего блока листвы
        ResourceLocation leavesLocation = new ModelResourceLocation(ModBlocks.WASTE_LEAVES.getId(), "");

        // Находим оригинальную, "запеченную" модель в регистре
        BakedModel originalModel = modelRegistry.get(leavesLocation);
        
        // Если модель найдена, заменяем ее на нашу обертку
        if (originalModel != null) {
            LeavesModelWrapper wrappedModel = new LeavesModelWrapper(originalModel);
            event.getModels().put(leavesLocation, wrappedModel);
            if (ModClothConfig.get().enableDebugLogging) {
                MainRegistry.LOGGER.debug("Successfully wrapped waste_leaves model for dynamic render types.");
            }
        } else {
            if (ModClothConfig.get().enableDebugLogging) {
                MainRegistry.LOGGER.warn("Could not find model for waste_leaves to wrap.");
            }
        }

        for (BlockState state : ModBlocks.FLUID_TANK.get().getStateDefinition().getPossibleStates()) {

            // Получаем ResourceLocation модели для этого стейта
            ResourceLocation modelLoc = BlockModelShaper.stateToModelLocation(state);

            // Если модель существует в реестре загруженных
            if (event.getModels().containsKey(modelLoc)) {
                BakedModel existingModel = event.getModels().get(modelLoc);

                // Оборачиваем её
                FluidTankModelWrapper wrapper = new FluidTankModelWrapper(existingModel);

                // Кладем обратно в реестр
                event.getModels().put(modelLoc, wrapper);
            }
        }
    }

    @SubscribeEvent
    public static void onModelRegister(ModelEvent.RegisterGeometryLoaders event) {


        event.register("procedural_wire", new ProceduralWireLoader());
        event.register("advanced_assembly_machine_loader", new MachineAdvancedAssemblerModelLoader());
        event.register("template_loader", new TemplateModelLoader());
        event.register("press_loader", new PressModelLoader());

        MainRegistry.LOGGER.info("Registered geometry loaders: procedural_wire, advanced_assembly_machine_loader, template_loader, door, press_loader");
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ModKeyBindings.RELOAD_KEY);
        event.register(ModKeyBindings.UNLOAD_KEY);
        ModConfigKeybindHandler.onRegisterKeyMappings(event);
        MainRegistry.LOGGER.info("Registered key mappings.");
    }
    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.TURRET_LIGHT.get(), TurretLightRenderer::new);
        event.registerEntityRenderer(ModEntities.AIRNUKEBOMB_PROJECTILE.get(),
                AirNukeBombProjectileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.AIRBOMB_PROJECTILE.get(),
                AirBombProjectileEntityRenderer::new);
        EntityRenderers.register(ModEntities.TURRET_LIGHT_LINKED.get(), TurretLightLinkedRenderer::new);





        event.registerEntityRenderer(ModEntities.AIRSTRIKE_NUKE_ENTITY.get(), AirstrikeNukeEntityRenderer::new);
        EntityRenderers.register(ModEntities.TURRET_BULLET.get(), TurretBulletRenderer::new);

        event.registerEntityRenderer(ModEntities.AIRSTRIKE_ENTITY.get(), AirstrikeEntityRenderer::new);
    }
    @SubscribeEvent
    public static void onResourceReload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new ShaderReloadListener());
        event.registerReloadListener((preparationBarrier, resourceManager,
                preparationsProfiler, reloadProfiler,
                backgroundExecutor, gameExecutor) -> {
            return preparationBarrier.wait(null).thenRunAsync(() -> {
                // Очищаем глобальный кэш VBO
                MachineAdvancedAssemblerVboRenderer.clearGlobalCache();
                ImmediateFallbackRenderer.clearGlobalCache();
                MachinePressRenderer.clearCaches();
                
                // ИСПРАВЛЕНО: НЕ вызываем reset(), вместо этого очищаем только кеши
                GlobalMeshCache.clearAll();
                DoorPartAABBRegistry.clear();
                
                // Переинициализируем immediate рендер после очистки
                ImmediateFallbackRenderer.onShaderReload();
                
                // ИСПРАВЛЕНО: Вместо reset() просто обновляем путь на основе текущего состояния
                // Это сохранит ручное переключение, если оно было активно
                RenderPathManager.updateRenderPath();
                
                MainRegistry.LOGGER.info("VBO cache cleanup completed, render path preserved");
            }, gameExecutor);
        });
    }

    public static void onClientDisconnect(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        MainRegistry.LOGGER.info("Client disconnecting, clearing VBO caches...");
        MachineAdvancedAssemblerVboRenderer.clearGlobalCache();
        MachinePressRenderer.clearCaches();
        com.smogline.client.render.shader.ImmediateFallbackRenderer.forceReset();
    }

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        // Связываем наш ТИП частицы с ее ФАБРИКОЙ.
        event.registerSpriteSet(ModParticleTypes.DARK_PARTICLE.get(), DarkParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.RAD_FOG_PARTICLE.get(), RadFogParticle.Provider::new);
        MainRegistry.LOGGER.info("Registered custom particle providers.");
    }

    @SubscribeEvent
    public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
        MainRegistry.LOGGER.info("Registering GUI overlays...");
        
        // Регистрируем оверлей.
        // Мы говорим: "Нарисуй оверлей с ID 'geiger_counter_hud' НАД хотбаром,
        // используя логику из объекта GeigerOverlay.GEIGER_HUD_OVERLAY".
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "geiger_counter_hud", OverlayGeiger.GEIGER_HUD_OVERLAY);

        event.registerAbove(VanillaGuiOverlay.PORTAL.id(), "radiation_pixels", OverlayRadiationVisuals.RADIATION_PIXELS_OVERLAY);
        // ✅ ДОБАВЛЯЕМ ВОТ ЭТО:
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "ammo_hud", OverlayAmmoHud.HUD_AMMO);
        MainRegistry.LOGGER.info("GUI overlays registered.");
    }
    
    @SubscribeEvent
    public static void registerTooltipFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(ItemTooltipComponent.class, ItemTooltipComponentRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
        MainRegistry.LOGGER.info("Registering custom shaders...");
        VertexFormat blockLitFormat = new VertexFormat(
            ImmutableMap.<String, VertexFormatElement>builder()
                .put("Position", DefaultVertexFormat.ELEMENT_POSITION)
                .put("Normal",   DefaultVertexFormat.ELEMENT_NORMAL)
                .put("UV0",      DefaultVertexFormat.ELEMENT_UV0)
                .put("InstMatRow0", DefaultVertexFormat.ELEMENT_NORMAL) // vec4
                .put("InstMatRow1", DefaultVertexFormat.ELEMENT_NORMAL) // vec4
                .put("InstMatRow2", DefaultVertexFormat.ELEMENT_NORMAL) // vec4
                .put("InstMatRow3", DefaultVertexFormat.ELEMENT_NORMAL) // vec4
                .put("InstLight",   DefaultVertexFormat.ELEMENT_UV2)    // vec2
                .build()
        );
        event.registerShader(
            new ShaderInstance(
                event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "block_lit"),
                blockLitFormat
            ),
            ModShaders::setBlockLitShader
        );
        MainRegistry.LOGGER.info("Successfully registered block_lit shader");
    }

    private static class LeavesModelWrapper extends BakedModelWrapper<BakedModel> {

        public LeavesModelWrapper(BakedModel originalModel) {
            super(originalModel);
        }

        @Override
        public ChunkRenderTypeSet getRenderTypes(@Nonnull BlockState state, @Nonnull RandomSource rand, @Nonnull ModelData data) {
            
            GraphicsStatus graphics = Minecraft.getInstance().options.graphicsMode().get();
            
            if (graphics == GraphicsStatus.FANCY || graphics == GraphicsStatus.FABULOUS) {
                return ChunkRenderTypeSet.of(RenderType.cutoutMipped());
            }
            
            return ChunkRenderTypeSet.of(RenderType.solid());
        }
    }
    public static void addTemplatesClient(BuildCreativeModeTabContentsEvent event) {
        if (Minecraft.getInstance().level != null) {
            RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();
            List<AssemblerRecipe> recipes = recipeManager.getAllRecipesFor(AssemblerRecipe.Type.INSTANCE);
            
            // Собираем уникальные blueprintPool из всех рецептов
            Set<String> blueprintPools = new HashSet<>();
            for (AssemblerRecipe recipe : recipes) {
                String pool = recipe.getBlueprintPool();
                if (pool != null && !pool.isEmpty()) {
                    blueprintPools.add(pool);
                }
            }
            
            // Создаём папку для каждого уникального пула
            for (String pool : blueprintPools) {
                ItemStack folderStack = new ItemStack(ModItems.BLUEPRINT_FOLDER.get());
                ItemBlueprintFolder.writeBlueprintPool(folderStack, pool);
                event.accept(folderStack);
            }
            
            if (ModClothConfig.get().enableDebugLogging) {
                MainRegistry.LOGGER.info("Added {} blueprint folders to NTM Templates tab", blueprintPools.size());
            }
            
            // Добавляем шаблоны
            for (AssemblerRecipe recipe : recipes) {
                ItemStack templateStack = new ItemStack(ModItems.ASSEMBLY_TEMPLATE.get());
                ItemAssemblyTemplate.writeRecipeOutput(templateStack, recipe.getResultItem(null));
                event.accept(templateStack);
            }
            
            if (ModClothConfig.get().enableDebugLogging) {
                MainRegistry.LOGGER.info("Added {} templates to NTM Templates tab", recipes.size());
            }
        } else {
            if (ModClothConfig.get().enableDebugLogging) {
                MainRegistry.LOGGER.warn("Could not populate templates tab: Minecraft level is null.");
            }
        }
    }
}