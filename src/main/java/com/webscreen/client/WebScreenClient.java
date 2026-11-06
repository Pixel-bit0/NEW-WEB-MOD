package com.webscreen.client;

import com.webscreen.WebScreenMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public class WebScreenClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        if (!FabricLoader.getInstance().isModLoaded("mcef")) {
            System.err.println("[webscreen] MCEF is not installed - screens will not display anything.");
            return;
        }
        BlockEntityRendererFactories.register(WebScreenMod.SCREEN_BE, ScreenRenderer::new);
        ClientTickEvents.END_CLIENT_TICK.register(client -> BrowserManager.tick());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> BrowserManager.closeAll());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> BrowserManager.closeAll());
    }
}
