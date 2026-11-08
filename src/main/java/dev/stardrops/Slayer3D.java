package dev.stardrops;

import static dev.stardrops.Ritual.*;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Slayer in 3D: the meteor scene drawn with the game's own models, in a soft-edged window of space over the screen (never the whole screen).
 *
 * A floating island of real blocks in the boss's own ground; the real mob (Revenant = a zombie in diamond armour, Tarantula = a spider,
 * Sven = a wolf, Voidgloom = an enderman, Inferno = a blaze, Riftstalker = a vindicator) arrives its own way and walks toward you; meteors
 * (a burning block with a long trail) strike it on SlayerScene.HITS. Behind it, deep space in layers: a starfield, nebula clouds, a ringed
 * planet, distant floating islands, haze between the layers, and dust drifting in front of the lens, so the slow camera shows real depth.
 *
 * Drawn as a picture-in-picture (Fabric's PictureInPictureRendererRegistry): our own perspective camera renders into the box's texture,
 * then the texture is blitted with the scene's fade. Everything else (the item, the words, the bars) stays Ritual's 2D layer on top.
 */
final class Slayer3D {
    private Slayer3D() {}

    static boolean registered, broken;
    /** The 3D scene can be used: registered, never failed, and a world to make the mobs in. */
    static boolean on() { return registered && !broken && Minecraft.getInstance().level != null; }

    private static RenderType ADD, TRANS, OPQ;
    static void register() {
        try {
            DepthStencilState noWrite = new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false);
            RenderPipeline add = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("stardrops", "pipeline/slayer_glow"))
                    .withVertexShader("core/rendertype_lightning").withFragmentShader("core/rendertype_lightning")
                    .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING)).withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
                    .withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withCull(false).withDepthStencilState(noWrite).build());
            RenderPipeline tr = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("stardrops", "pipeline/slayer_veil"))
                    .withVertexShader("core/rendertype_lightning").withFragmentShader("core/rendertype_lightning")
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)).withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
                    .withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withCull(false).withDepthStencilState(noWrite).build());
            RenderPipeline opq = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("stardrops", "pipeline/slayer_solid"))
                    .withVertexShader("core/rendertype_lightning").withFragmentShader("core/rendertype_lightning")
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                    .withCull(true).withDepthStencilState(DepthStencilState.DEFAULT).build());
            OPQ = RenderType.create("stardrops_slayer_solid", RenderSetup.builder(opq).createRenderSetup());
            ADD = RenderType.create("stardrops_slayer_glow", RenderSetup.builder(add).createRenderSetup());
            TRANS = RenderType.create("stardrops_slayer_veil", RenderSetup.builder(tr).createRenderSetup());
            net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry.register(ctx -> new R());
            registered = true;
        } catch (Throwable t) { System.err.println("[Stardrops] 3D slayer scene unavailable, using the pixel scene: " + t); }
    }

    /** One frame of the scene, as the GUI hands it to the renderer. */
    record State(float t, float animLen, int boss, int type, int energy, float alpha, float craterY, int x0, int y0, int x1, int y1,
                 float scale, ScreenRectangle scissorArea, ScreenRectangle bounds) implements PictureInPictureRenderState {}

    /** Queue the scene into the box (gui coordinates). craterY: where the crater sits, -1 (bottom) .. 1 (top) of the box. */
    static void submit(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, float t, float animLen, int boss, int type, int energy, float alpha, float craterY) {
        if (x1 - x0 < 4 || y1 - y0 < 4 || alpha < 0.01f) return;
        g.guiRenderState.addPicturesInPictureState(new State(t, animLen, boss, type, energy, alpha, craterY, x0, y0, x1, y1, 1f, null,
                PictureInPictureRenderState.getBounds(x0, y0, x1, y1, null)));
    }

    // ------------------------------------------------------------------ the renderer
    static final class R extends PictureInPictureRenderer<State> {
        private final ProjectionMatrixBuffer proj = new ProjectionMatrixBuffer("stardrops slayer");
        private static java.lang.reflect.Field viewF; private static boolean viewTried;
        @Override public Class<State> getRenderStateClass() { return State.class; }
        @Override protected String getTextureLabel() { return "stardrops slayer"; }
        @Override protected void renderToTexture(State s, PoseStack ignored, SubmitNodeCollector c) {
            try { render(s, c, proj); }
            catch (Throwable e) { broken = true; StringBuilder sb = new StringBuilder("[Stardrops] 3D slayer scene failed, using the pixel scene: " + e); StackTraceElement[] st = e.getStackTrace(); for (int i = 0; i < Math.min(6, st.length); i++) sb.append("\n    at ").append(st[i]); System.err.println(sb); }
        }
        /** The texture is drawn with the scene's fade (premultiplied: every channel scales). */
        @Override protected void blitTexture(State s, GuiRenderState gs) {
            try {
                if (!viewTried) { viewTried = true; viewF = PictureInPictureRenderer.class.getDeclaredField("textureView"); viewF.setAccessible(true); }
                GpuTextureView view = viewF == null ? null : (GpuTextureView) viewF.get(this);
                if (view == null) { super.blitTexture(s, gs); return; }
                int a = Math.round(cl(s.alpha()) * 255), col = a << 24 | a << 16 | a << 8 | a;
                gs.addBlitToCurrentLayer(new BlitRenderState(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                        TextureSetup.singleTexture(view, RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)), s.pose(),
                        s.x0(), s.y0(), s.x1(), s.y1(), 0f, 1f, 1f, 0f, col, s.scissorArea(), null));
            } catch (Throwable e) { super.blitTexture(s, gs); }
        }
        @Override public void close() { super.close(); proj.close(); }
    }

    /** One frame: our camera, then Vox3D's meshes, far to near. */
    static void render(State s, SubmitNodeCollector c, ProjectionMatrixBuffer pb) {
        float aspect = (s.x1() - s.x0()) / (float) (s.y1() - s.y0());
        Vox3D.Frame f = Vox3D.build(s.t(), s.animLen(), Math.floorMod(s.boss(), 6), s.type(), s.energy(), aspect, s.craterY(), RenderSystem.getDevice().getDeviceInfo().isZZeroToOne());
        RenderSystem.setProjectionMatrix(pb.getBuffer(f.proj), ProjectionType.PERSPECTIVE);
        RenderSystem.getModelViewStack().set(f.view);
        PoseStack ws = new PoseStack(), vs = new PoseStack(); vs.mulPose(new Matrix4f(f.view).invert());
        put(c, 0, ws, OPQ, f.opq); put(c, -3, vs, TRANS, f.back); put(c, -2, ws, ADD, f.sky); put(c, 1, ws, TRANS, f.veil); put(c, 2, ws, ADD, f.add);
    }
    static void put(SubmitNodeCollector c, int order, PoseStack ps, RenderType rt, Vox3D.Mesh m) {
        if (m.n == 0) return;
        final float[] p = m.p; final int[] col = m.c; final int n = m.n;
        c.order(order).submitCustomGeometry(ps, rt, (pose, b) -> { for (int i = 0; i < n; i++) b.addVertex(pose, p[i * 3], p[i * 3 + 1], p[i * 3 + 2]).setColor(col[i]); });
    }
}
