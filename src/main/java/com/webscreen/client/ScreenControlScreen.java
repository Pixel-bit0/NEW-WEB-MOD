package com.webscreen.client;

import com.webscreen.ScreenBlockEntity;
import com.webscreen.UrlUtil;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

/** Opens when you right-click a Web Screen block: browse the page and change size / position. */
public class ScreenControlScreen extends Screen {
    private final BlockPos pos;
    private TextFieldWidget urlField;
    private boolean loaded, contentPressed;
    private int w, h;
    private float ox, oy;
    private String sentUrl = "";
    private int cx, cy, cw, ch;
    private BrowserManager.Entry entry;

    public ScreenControlScreen(BlockPos pos) {
        super(Text.literal("Web Screen"));
        this.pos = pos;
    }

    private ScreenBlockEntity be() {
        World world = MinecraftClient.getInstance().world;
        if (world == null) return null;
        BlockEntity b = world.getBlockEntity(pos);
        return b instanceof ScreenBlockEntity s ? s : null;
    }

    private ButtonWidget btn(String label, int x, int y, Runnable action) {
        return ButtonWidget.builder(Text.literal(label), b -> action.run()).dimensions(x, y, 18, 18).build();
    }

    private void push() {
        ScreenNet.send(pos, sentUrl, w, h, ox, oy);
    }

    private void move(float dx, float dy) {
        float lim = ScreenBlockEntity.MAX_OFF;
        ox = Math.max(-lim, Math.min(lim, Math.round((ox + dx) * 4f) / 4f));
        oy = Math.max(-lim, Math.min(lim, Math.round((oy + dy) * 4f) / 4f));
        push();
    }

    @Override
    protected void init() {
        ScreenBlockEntity be = be();
        if (be == null) { close(); return; }
        if (!loaded) {
            w = be.getWidth();
            h = be.getHeight();
            ox = be.getOffX();
            oy = be.getOffY();
            sentUrl = be.getUrl();
            loaded = true;
        }

        addDrawableChild(btn("<", 10, 6, () -> { if (entry != null) entry.browser.goBack(); }));
        addDrawableChild(btn(">", 30, 6, () -> { if (entry != null) entry.browser.goForward(); }));
        addDrawableChild(btn("R", 50, 6, () -> { if (entry != null) entry.browser.reload(); }));

        urlField = new TextFieldWidget(textRenderer, 74, 8, width - 84, 14, Text.empty());
        urlField.setMaxLength(2048);
        urlField.setText(sentUrl);
        addDrawableChild(urlField);

        int by = height - 22;
        addDrawableChild(btn("-", 78, by, () -> { w = Math.max(1, w - 1); push(); }));
        addDrawableChild(btn("+", 98, by, () -> { w = Math.min(ScreenBlockEntity.MAX_W, w + 1); push(); }));
        addDrawableChild(btn("-", 190, by, () -> { h = Math.max(1, h - 1); push(); }));
        addDrawableChild(btn("+", 210, by, () -> { h = Math.min(ScreenBlockEntity.MAX_H, h + 1); push(); }));
        addDrawableChild(btn("<", 290, by, () -> move(-0.25f, 0)));
        addDrawableChild(btn(">", 310, by, () -> move(0.25f, 0)));
        addDrawableChild(btn("^", 330, by, () -> move(0, 0.25f)));
        addDrawableChild(btn("v", 350, by, () -> move(0, -0.25f)));
    }

    @Override
    public boolean shouldPause() { return false; }

    private boolean inContent(double mx, double my) {
        return mx >= cx && mx < cx + cw && my >= cy && my < cy + ch;
    }

    private int bx(double mx) { return (int) ((mx - cx) * entry.pxW / (double) Math.max(1, cw)); }
    private int by(double my) { return (int) ((my - cy) * entry.pxH / (double) Math.max(1, ch)); }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ScreenBlockEntity be = be();
        if (be == null) { close(); return; }
        renderBackground(ctx);
        entry = BrowserManager.get(be);

        int ax = 10, ay = 30, aw = width - 20, ah = height - 30 - 28;
        float aspect = (float) w / (float) h;
        cw = aw;
        ch = (int) (cw / aspect);
        if (ch > ah) { ch = ah; cw = (int) (ch * aspect); }
        cx = ax + (aw - cw) / 2;
        cy = ay + (ah - ch) / 2;

        ctx.fill(cx - 1, cy - 1, cx + cw + 1, cy + ch + 1, 0xFF000000);
        if (entry != null) {
            BrowserManager.drawGui(ctx, entry, cx, cy, cw, ch);
            if (!urlField.isFocused()) {
                String u = entry.browser.getURL();
                if (u != null && !u.isEmpty() && !u.equals(urlField.getText())) urlField.setText(u);
            }
        } else {
            ctx.drawText(textRenderer, "Chromium is still loading (first launch downloads files)...", cx + 8, cy + 8, 0xAAAAAA, false);
        }

        int ty = height - 17;
        ctx.drawText(textRenderer, "Width: " + w, 10, ty, 0xFFFFFF, false);
        ctx.drawText(textRenderer, "Height: " + h, 122, ty, 0xFFFFFF, false);
        ctx.drawText(textRenderer, "Move:", 250, ty, 0xFFFFFF, false);
        ctx.drawText(textRenderer, "Esc = close", width - 70, ty, 0x888888, false);

        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) {
            if (urlField.isFocused() && entry != null) entry.browser.setFocus(false);
            return true;
        }
        setFocused(null);
        if (entry != null && inContent(mx, my)) {
            entry.browser.setFocus(true);
            entry.browser.sendMousePress(bx(mx), by(my), button);
            contentPressed = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (contentPressed && entry != null) {
            entry.browser.sendMouseMove(bx(mx), by(my));
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (contentPressed && entry != null) {
            entry.browser.sendMouseRelease(bx(mx), by(my), button);
            contentPressed = false;
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public void mouseMoved(double mx, double my) {
        if (entry != null && inContent(mx, my)) entry.browser.sendMouseMove(bx(mx), by(my));
        super.mouseMoved(mx, my);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        if (entry != null && inContent(mx, my)) {
            entry.browser.sendMouseWheel(bx(mx), by(my), amount, 0);
            return true;
        }
        return super.mouseScrolled(mx, my, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        if (urlField.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                String u = UrlUtil.normalize(urlField.getText());
                if (UrlUtil.isSafe(u)) {
                    sentUrl = u;
                    if (entry != null) {
                        entry.requestedUrl = u;
                        entry.browser.loadURL(u);
                    }
                    push();
                }
                setFocused(null);
                if (entry != null) entry.browser.setFocus(true);
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (entry != null) {
            entry.browser.sendKeyPress(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (!urlField.isFocused() && entry != null) {
            entry.browser.sendKeyRelease(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (urlField.isFocused()) return super.charTyped(chr, modifiers);
        if (entry != null) {
            entry.browser.sendKeyTyped(chr, modifiers);
            return true;
        }
        return false;
    }

    @Override
    public void removed() {
        if (entry == null) return;
        entry.browser.setFocus(false);
        // share the page you ended up on with everyone who sees this screen
        String cur = entry.browser.getURL();
        if (cur != null && UrlUtil.isSafe(cur) && !cur.equals(sentUrl)) {
            sentUrl = cur;
            entry.requestedUrl = cur;
            push();
        }
    }
}
