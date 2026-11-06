package com.webscreen.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class ClientHooks {
    private ClientHooks() {}

    public static void openControl(BlockPos pos) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!FabricLoader.getInstance().isModLoaded("mcef")) {
            if (mc.player != null) mc.player.sendMessage(Text.literal("Web Screen needs the MCEF mod installed."), true);
            return;
        }
        mc.setScreen(new ScreenControlScreen(pos));
    }
}
