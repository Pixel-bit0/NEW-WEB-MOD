package com.webscreen.client;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;
import com.mojang.blaze3d.systems.RenderSystem;
import com.webscreen.ScreenBlockEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Util;
import net.minecraft.world.World;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** One Chromium browser per screen block that is currently being looked at. */
public final class BrowserManager {
    public static final int PX_PER_BLOCK = 256;
    private static final int MAX_BROWSERS = 6;
    private static final long IDLE_MS = 10_000;

    public static final class Entry {
        public MCEFBrowser browser;
        public String requestedUrl;
        public int pxW, pxH;
        long lastUsed;
    }

    private static final Map<String, Entry> MAP = new HashMap<>();

    private BrowserManager() {}

    private static String key(ScreenBlockEntity be) {
        World w = be.getWorld();
        return (w == null ? "" : w.getRegistryKey().getValue().toString()) + "@" + be.getPos().asLong();
    }

    public static Entry get(ScreenBlockEntity be) {
        if (!MCEF.isInitialized()) return null;
        String k = key(be);
        int pw = be.getWidth() * PX_PER_BLOCK;
        int ph = be.getHeight() * PX_PER_BLOCK;
        Entry e = MAP.get(k);
        if (e == null) {
            if (MAP.size() >= MAX_BROWSERS) return null;
            e = new Entry();
            e.browser = MCEF.createBrowser(be.getUrl(), false);
            e.requestedUrl = be.getUrl();
            e.browser.resize(pw, ph);
            e.pxW = pw;
            e.pxH = ph;
            MAP.put(k, e);
        } else {
            if (pw != e.pxW || ph != e.pxH) {
                e.browser.resize(pw, ph);
                e.pxW = pw;
                e.pxH = ph;
            }
            if (!be.getUrl().equals(e.requestedUrl)) {
                e.requestedUrl = be.getUrl();
                if (!be.getUrl().equals(e.browser.getURL())) e.browser.loadURL(be.getUrl());
            }
        }
        e.lastUsed = Util.getMeasuringTimeMs();
        return e;
    }

    /** Closes browsers for screens nobody has looked at for a while. */
    public static void tick() {
        if (MAP.isEmpty()) return;
        long now = Util.getMeasuringTimeMs();
        Iterator<Map.Entry<String, Entry>> it = MAP.entrySet().iterator();
        while (it.hasNext()) {
            Entry e = it.next().getValue();
            if (now - e.lastUsed > IDLE_MS) {
                try { e.browser.close(); } catch (Exception ignored) {}
                it.remove();
            }
        }
    }

    public static void closeAll() {
        for (Entry e : MAP.values()) {
            try { e.browser.close(); } catch (Exception ignored) {}
        }
        MAP.clear();
    }

    /** Draws the browser texture as a flat 2D quad in a GUI. */
    public static void drawGui(DrawContext ctx, Entry e, int x, int y, int w, int h) {
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, e.browser.getRenderer().getTextureID());
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        BufferBuilder b = Tessellator.getInstance().getBuffer();
        b.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        b.vertex(m, x,     y + h, 0).texture(0f, 1f).color(255, 255, 255, 255).next();
        b.vertex(m, x + w, y + h, 0).texture(1f, 1f).color(255, 255, 255, 255).next();
        b.vertex(m, x + w, y,     0).texture(1f, 0f).color(255, 255, 255, 255).next();
        b.vertex(m, x,     y,     0).texture(0f, 0f).color(255, 255, 255, 255).next();
        Tessellator.getInstance().draw();
    }
}
