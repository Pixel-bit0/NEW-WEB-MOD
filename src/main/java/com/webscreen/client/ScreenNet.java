package com.webscreen.client;

import com.webscreen.WebScreenMod;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

public final class ScreenNet {
    private ScreenNet() {}

    public static void send(BlockPos pos, String url, int w, int h, float ox, float oy) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeString(url, 2048);
        buf.writeInt(w);
        buf.writeInt(h);
        buf.writeFloat(ox);
        buf.writeFloat(oy);
        ClientPlayNetworking.send(WebScreenMod.UPDATE, buf);
    }
}
