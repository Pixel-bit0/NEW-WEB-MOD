package com.webscreen;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;

public class ScreenBlockEntity extends BlockEntity {
    public static final String DEFAULT_URL = "https://www.google.com";
    public static final int MAX_W = 8, MAX_H = 6;
    public static final float MAX_OFF = 16f;

    private String url = DEFAULT_URL;
    private int width = 3, height = 2;
    private float offX = 0f, offY = 0f;

    public ScreenBlockEntity(BlockPos pos, BlockState state) {
        super(WebScreenMod.SCREEN_BE, pos, state);
    }

    public String getUrl() { return url; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public float getOffX() { return offX; }
    public float getOffY() { return offY; }

    /** Server side: validate, store, and sync to every player watching this block. */
    public void applySettings(String newUrl, int w, int h, float ox, float oy) {
        if (UrlUtil.isSafe(newUrl)) url = newUrl;
        width = Math.max(1, Math.min(MAX_W, w));
        height = Math.max(1, Math.min(MAX_H, h));
        offX = clampOff(ox);
        offY = clampOff(oy);
        markDirty();
        if (world != null && !world.isClient) {
            BlockState s = getCachedState();
            world.updateListeners(pos, s, s, Block.NOTIFY_ALL);
        }
    }

    private static float clampOff(float v) {
        if (Float.isNaN(v)) return 0f;
        return Math.max(-MAX_OFF, Math.min(MAX_OFF, v));
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putString("url", url);
        nbt.putInt("w", width);
        nbt.putInt("h", height);
        nbt.putFloat("ox", offX);
        nbt.putFloat("oy", offY);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        if (nbt.contains("url")) {
            String u = nbt.getString("url");
            if (UrlUtil.isSafe(u)) url = u;
        }
        if (nbt.contains("w")) width = Math.max(1, Math.min(MAX_W, nbt.getInt("w")));
        if (nbt.contains("h")) height = Math.max(1, Math.min(MAX_H, nbt.getInt("h")));
        if (nbt.contains("ox")) offX = clampOff(nbt.getFloat("ox"));
        if (nbt.contains("oy")) offY = clampOff(nbt.getFloat("oy"));
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }
}
