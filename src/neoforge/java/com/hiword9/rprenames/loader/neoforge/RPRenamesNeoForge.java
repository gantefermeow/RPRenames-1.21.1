package com.hiword9.rprenames.loader.neoforge;

import com.hiword9.rprenames.mod.config.ModConfigScreenFactory;
import com.hiword9.rprenames.mod.Settings;
import com.hiword9.rprenames.mod.RPRenames;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.resource.PathPackResources;

import java.util.Optional;

import static com.hiword9.rprenames.util.Util.*;
import static com.hiword9.rprenames.mod.RPRenames.*;

@Mod(value = MOD_ID, dist = Dist.CLIENT)
public class RPRenamesNeoForge {
    public RPRenamesNeoForge(IEventBus modEventBus) {
        onInit(modEventBus);
    }

    public void onInit(IEventBus modEventBus) {
        Settings.setConfigDir(FMLPaths.CONFIGDIR.get());

        LOGGER.info("Initializing RPRenames NeoForge");

        modEventBus.addListener(this::onAddPackFinders);
        modEventBus.addListener(this::onRegisterClientReloadListeners);
        modEventBus.addListener(this::onRegisterEvent);
        NeoForge.EVENT_BUS.addListener(this::onRegisterClientCommands);

        RPRenames.onInit();

        if (FMLLoader.getLoadingModList().getModFileById("yet_another_config_lib_v3") != null) {
            ModLoadingContext.get().registerExtensionPoint(
                    IConfigScreenFactory.class,
                    () -> (container, parent) -> ModConfigScreenFactory.create(parent)
            );
        }
    }

    @SubscribeEvent
    public void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        registerCommand(event.getDispatcher(), event.getBuildContext());
    }

    @SubscribeEvent
    public void onAddPackFinders(AddPackFindersEvent event) {
        if (!config().loadModBuiltinResources) return;

        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;

        for (String id : new String[]{"vanillish", "default_dark_mode", "high_contrasted"}) {
            var packLocationInfo = new PackLocationInfo(
                    id,
                    Component.translatable("rprenames.builtinResourcePack." + id),
                    PackSource.BUILT_IN,
                    Optional.empty()
            );

            var resourcePath = ModList.get().getModFileById(MOD_ID).getFile()
                    .findResource("resourcepacks/" + id);

            var pack = Pack.readMetaAndCreate(
                    packLocationInfo,
                    new PathPackResources.PathResourcesSupplier(resourcePath),
                    PackType.CLIENT_RESOURCES,
                    new PackSelectionConfig(
                            false,
                            Pack.Position.TOP,
                            false
                    )
            );

            if (pack != null)
                event.addRepositorySource((infoConsumer) -> infoConsumer.accept(pack));
            else
                LOGGER.warn("Could not register built-in pack {}", id);
        }
    }

    @SubscribeEvent
    public void onRegisterEvent(RegisterEvent event) {
        event.register(
                Registries.CREATIVE_MODE_TAB,
                register -> registerItemGroup(register::register)
        );
    }

    @SubscribeEvent
    public void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(RENAMES_RELOADER_ID, updatableRenamesManager);
    }
}