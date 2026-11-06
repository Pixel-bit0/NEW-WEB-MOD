package com.webscreen.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.webscreen.ScreenBlock;
import com.webscreen.ScreenBlockEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

public class ScreenRenderer implements BlockEntityRenderer<ScreenBlockEntity> {
    public ScreenRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(ScreenBlockEntity be, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        BrowserManager.Entry e = BrowserManager.get(be);
        if (e == null) return;

        Direction facing = be.getCachedState().get(ScreenBlock.FACING);
        matrices.push();
        matrices.translate(0.5, 0.5, 0.5);
        // local frame: the front of the screen faces -Z
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - facing.asRotation()));

        float z = 0.375f - 0.002f;                  // just in front of the panel
        float xL = 0.5f - be.getOffX();             // viewer's left edge
        float xR = xL - be.getWidth();              // extends to the viewer's right
        float yB = -0.5f + be.getOffY();
        float yT = yB + be.getHeight();             // extends upward

        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, e.browser.getRenderer().getTextureID());
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableCull();
        Matrix4f m = matrices.peek().getPositionMatrix();
        BufferBuilder b = Tessellator.getInstance().getBuffer();
        b.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        b.vertex(m, xL, yT, z).texture(0f, 0f).color(255, 255, 255, 255).next();
        b.vertex(m, xR, yT, z).texture(1f, 0f).color(255, 255, 255, 255).next();
        b.vertex(m, xR, yB, z).texture(1f, 1f).color(255, 255, 255, 255).next();
        b.vertex(m, xL, yB, z).texture(0f, 1f).color(255, 255, 255, 255).next();
        Tessellator.getInstance().draw();
        RenderSystem.enableCull();

        matrices.pop();
    }

    @Override
    public boolean rendersOutsideBoundingBox(ScreenBlockEntity be) {
        return true;
    }

    @Override
    public int getRenderDistance() {
        return 48;
    }
}
