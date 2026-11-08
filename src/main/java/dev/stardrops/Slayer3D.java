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

    private static RenderType ADD, TRANS;
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
            try { Scene.render(s, c, proj); }
            catch (Throwable e) { broken = true; System.err.println("[Stardrops] 3D slayer scene failed, using the pixel scene: " + e); }
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

    // ------------------------------------------------------------------ geometry we build ourselves (glows, rings, sky): position + colour triangles
    static final class Mesh {
        float[] p = new float[3 * 3072]; int[] c = new int[3072]; int n; final boolean fade;
        Mesh(boolean fade) { this.fade = fade; }
        void v(float x, float y, float z, int argb) {
            if (fade) { float e = Scene.edge(x, y, z); argb = Math.round((argb >>> 24) * e) << 24 | (argb & 0xFFFFFF); }
            if (n == c.length) { p = java.util.Arrays.copyOf(p, p.length * 2); c = java.util.Arrays.copyOf(c, c.length * 2); }
            p[n * 3] = x; p[n * 3 + 1] = y; p[n * 3 + 2] = z; c[n++] = argb;
        }
        void emit(PoseStack.Pose pose, VertexConsumer b) { for (int i = 0; i < n; i++) b.addVertex(pose, p[i * 3], p[i * 3 + 1], p[i * 3 + 2]).setColor(c[i]); }
    }
    static int argb(float a, int rgb) { return Math.round(cl(a) * 255) << 24 | (rgb & 0xFFFFFF); }

    // ------------------------------------------------------------------ the scene
    static final class Scene {
        static final int LIGHT = 15728880;
        static final float FOV = 40f;
        static final Vector3f FRONT = new Vector3f(0f, 0f, 1.0f), SPAWN = new Vector3f(1.3f, 0f, -1.6f);

        // per frame
        static Matrix4f view, projView; static Vector3f eye = new Vector3f(), right = new Vector3f(), up = new Vector3f(), fwd = new Vector3f();
        static final Map<Item, Object[]> ART = new HashMap<>();
        static Mesh sky, add, veil, back, haze, planetM;

        static void render(State s, SubmitNodeCollector c, ProjectionMatrixBuffer pb) {
            Minecraft mc = Minecraft.getInstance(); if (mc.level == null) return;
            float t = s.t(); int type = s.type(), boss = Math.floorMod(s.boss(), 6);
            float W = s.x1() - s.x0(), H = s.y1() - s.y0(), aspect = W / H;
            float[] hits = SlayerScene.HITS[type]; float last = hits[hits.length - 1], O = AnimationManager.SL_PIVOT, p = t - O;

            // ---- the camera: swoops in, dollies in as the boss walks, leans in before RNG's big one, shakes on every hit
            float sh = SlayerScene.shake(type, t);
            float dist = lerp(19f, 14f, eo(t / 1.1f)) - 2.4f * sm((t - SlayerScene.WALK) / (SlayerScene.WALK_END - SlayerScene.WALK));
            if (type == 2) dist -= 2.0f * sm((t - 2.95f) / 0.42f) * (1f - sm((t - last) / 0.3f));
            dist += 1.2f * sm((t - last) / 0.5f) * (type == 0 ? 0.3f : 1f);
            float yaw = (float) Math.toRadians(14 + 7 * Math.sin(t * 0.35) + 5 * eo(1f - t / 1.1f));
            float pitch = (float) Math.toRadians(19 + 3 * Math.sin(t * 0.27) + 6 * eo(1f - t / 1.1f));
            Vector3f target = new Vector3f(0.3f, 0.9f, -0.4f);
            eye.set(target.x + (float) (Math.sin(yaw) * Math.cos(pitch)) * dist, target.y + (float) Math.sin(pitch) * dist, target.z + (float) (Math.cos(yaw) * Math.cos(pitch)) * dist);
            float jx = (rnd((int) (t * 60)) - 0.5f) * 0.35f * sh, jy = (rnd((int) (t * 60) + 31) - 0.5f) * 0.35f * sh;
            view = new Matrix4f().lookAt(eye.x + jx, eye.y + jy, eye.z, target.x + jx * 0.4f, target.y + jy * 0.4f, target.z, 0, 1, 0);
            if (sh > 0.01f) view = new Matrix4f().rotateZ((rnd((int) (t * 30)) - 0.5f) * 0.05f * sh).mul(view);
            right.set(view.m00(), view.m10(), view.m20()); up.set(view.m01(), view.m11(), view.m21()); fwd.set(-view.m02(), -view.m12(), -view.m22());
            boolean z01 = RenderSystem.getDevice().getDeviceInfo().isZZeroToOne();
            Matrix4f persp = new Matrix4f().setPerspective((float) Math.toRadians(FOV), aspect, 1000f, 0.05f, z01);     // reversed depth, like the game's own
            // keep the crater where the item rises (centre x, craterY), whatever the camera does
            Vector4f cr = new Matrix4f(persp).mul(view).transform(new Vector4f(FRONT.x, FRONT.y, FRONT.z, 1f));
            float nx = cr.x / cr.w, ny = cr.y / cr.w;
            Matrix4f proj = new Matrix4f().translation(-nx + jx * 0.02f, s.craterY() - ny + jy * 0.02f, 0f).mul(persp);
            projView = new Matrix4f(proj).mul(view);
            RenderSystem.setProjectionMatrix(pb.getBuffer(proj), ProjectionType.PERSPECTIVE);
            RenderSystem.getModelViewStack().set(view);                                                       // world space in, like the level
            mc.gameRenderer.lighting().setupFor(Lighting.Entry.LEVEL);

            sky = new Mesh(false); add = new Mesh(true); veil = new Mesh(true); back = new Mesh(false); haze = new Mesh(false); planetM = new Mesh(false); ART.clear();
            PoseStack ps = new PoseStack();
            float dim = type == 2 ? sm((t - 2.95f) / 0.3f) * (1f - sm((t - last) / 0.25f)) : 0f;            // RNG: the sky holds its breath

            // ---- deep space, far to near
            backdrop(aspect, s.energy(), dim);
            stars(t, dim); nebula(t, s.energy(), boss); planet(t); shootingStars(t);
            hazeDisc(aspect, 22f, 0.26f); hazeDisc(aspect, 38f, 0.22f);
            farIslands(c, ps, t, boss);

            // ---- the island and the spawn
            island(c, ps, boss);
            prop(c, ps, t, boss);

            // ---- the boss
            boss(c, ps, mc, t, type, boss, hits, last, s.energy());

            // ---- meteors and impacts
            for (int i = 0; i < hits.length; i++) meteor(c, ps, t, type, boss, i, hits);

            // ---- after the pivot: the drop rises
            if (p > 0 && type > 0) reveal(t, p, type, s.animLen(), s.energy());

            motes(t);

            // ---- hand our own geometry over: far to near
            final Matrix4f inv = new Matrix4f(view).invert();
            PoseStack vs = new PoseStack(); vs.mulPose(inv);                                                     // view-space meshes
            submit(c, -3, vs, TRANS, back); submit(c, -2, ps, ADD, sky); submit(c, -1, ps, TRANS, planetM);
            submit(c, 0, vs, TRANS, haze); submit(c, 1, ps, TRANS, veil); submit(c, 2, ps, ADD, add);
        }
        static void submit(SubmitNodeCollector c, int order, PoseStack ps, RenderType rt, Mesh m) {
            if (m.n == 0) return;
            c.order(order).submitCustomGeometry(ps, rt, m::emit);
        }

        /** How far a world point is toward the edge of the window: 1 in the middle, 0 at the soft edge (or behind us). */
        static float edge(float x, float y, float z) {
            Vector4f v = projView.transform(new Vector4f(x, y, z, 1f)); if (v.w <= 0.01f) return 0f;
            float nx = v.x / v.w, ny = v.y / v.w, r = (float) Math.sqrt(nx * nx + ny * ny);
            return 1f - sm((r - 0.55f) / 0.42f);
        }

        // ---------------- builders
        /** A soft round glow facing the camera. */
        static void glow(Mesh m, float x, float y, float z, float r, int rgb, float a) {
            if (a < 0.005f || r <= 0) return; int seg = r > 1.2f ? 18 : 10; int ci = argb(a, rgb), co = argb(0, rgb);
            float px = 0, py = 0, pz = 0;
            for (int k = 0; k <= seg; k++) {
                double an = k * Math.PI * 2 / seg; float cx = (float) Math.cos(an) * r, cy = (float) Math.sin(an) * r;
                float qx = x + right.x * cx + up.x * cy, qy = y + right.y * cx + up.y * cy, qz = z + right.z * cx + up.z * cy;
                if (k > 0) { m.v(x, y, z, ci); m.v(px, py, pz, co); m.v(qx, qy, qz, co); }
                px = qx; py = qy; pz = qz;
            }
        }
        /** A four-point star glint facing the camera. */
        static void glint(Mesh m, float x, float y, float z, float r, int rgb, float a) {
            if (a < 0.01f) return; int ci = argb(a, rgb), co = argb(0, rgb); float w = r * 0.16f;
            for (int k = 0; k < 2; k++) {
                float ax = k == 0 ? 1 : 0, ay = k == 0 ? 0 : 1;
                float lx = (right.x * ax + up.x * ay) * r, ly = (right.y * ax + up.y * ay) * r, lz = (right.z * ax + up.z * ay) * r;
                float wx = (right.x * ay + up.x * ax) * w, wy = (right.y * ay + up.y * ax) * w, wz = (right.z * ay + up.z * ax) * w;
                m.v(x - lx, y - ly, z - lz, co); m.v(x + wx, y + wy, z + wz, ci); m.v(x + lx, y + ly, z + lz, co);
                m.v(x - lx, y - ly, z - lz, co); m.v(x - wx, y - wy, z - wz, ci); m.v(x + lx, y + ly, z + lz, co);
            }
            glow(m, x, y, z, r * 0.35f, rgb, a * 0.8f);
        }
        /** A flat ring on the ground (y), soft on both sides. */
        static void ring(Mesh m, float x, float y, float z, float r, float w, int rgb, float a) {
            if (a < 0.01f || r <= 0) return; int seg = 40, cm = argb(a, rgb), c0 = argb(0, rgb); float ri = Math.max(0, r - w), ro = r + w;
            for (int k = 0; k < seg; k++) {
                double a0 = k * Math.PI * 2 / seg, a1 = (k + 1) * Math.PI * 2 / seg;
                float c0x = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1x = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
                quad(m, x + c0x * ri, y, z + s0 * ri, c0, x + c1x * ri, y, z + s1 * ri, c0, x + c1x * r, y, z + s1 * r, cm, x + c0x * r, y, z + s0 * r, cm);
                quad(m, x + c0x * r, y, z + s0 * r, cm, x + c1x * r, y, z + s1 * r, cm, x + c1x * ro, y, z + s1 * ro, c0, x + c0x * ro, y, z + s0 * ro, c0);
            }
        }
        /** A flat disc on the ground. */
        static void disc(Mesh m, float x, float y, float z, float r, int rgb, float a, float soft) {
            if (a < 0.01f) return; int seg = 28, ci = argb(a, rgb), cm = argb(a, rgb), co = argb(0, rgb); float rm = r * (1 - soft);
            for (int k = 0; k < seg; k++) {
                double a0 = k * Math.PI * 2 / seg, a1 = (k + 1) * Math.PI * 2 / seg;
                float c0x = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1x = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
                m.v(x, y, z, ci); m.v(x + c0x * rm, y, z + s0 * rm, cm); m.v(x + c1x * rm, y, z + s1 * rm, cm);
                quad(m, x + c0x * rm, y, z + s0 * rm, cm, x + c1x * rm, y, z + s1 * rm, cm, x + c1x * r, y, z + s1 * r, co, x + c0x * r, y, z + s0 * r, co);
            }
        }
        static void quad(Mesh m, float ax, float ay, float az, int ca, float bx, float by, float bz, int cb, float cx, float cy, float cz, int cc, float dx, float dy, float dz, int cd) {
            m.v(ax, ay, az, ca); m.v(bx, by, bz, cb); m.v(cx, cy, cz, cc); m.v(ax, ay, az, ca); m.v(cx, cy, cz, cc); m.v(dx, dy, dz, cd);
        }
        /** A vertical column of light from (x, y0, z) up to y1, turned to face the camera. */
        static void column(Mesh m, float x, float y0, float z, float y1, float w, int rgb, float a) {
            if (a < 0.01f) return;
            float hx = right.x, hz = right.z, l = (float) Math.sqrt(hx * hx + hz * hz); if (l < 1e-4f) return; hx /= l; hz /= l;
            int cb = argb(a, rgb), ct = argb(0, rgb), ce = argb(0, rgb);
            // a soft middle and soft sides: two halves, each fading out sideways
            for (int sd = -1; sd <= 1; sd += 2) {
                quad(m, x, y0, z, cb, x + hx * w * sd, y0, z + hz * w * sd, ce, x + hx * w * sd, y1, z + hz * w * sd, ce, x, y1, z, ct);
            }
            glow(m, x, y0 + 0.05f, z, w * 2.2f, rgb, a * 0.6f);
        }
        /** A disc in view space at depth d, filling the window with a soft elliptical edge. */
        static void viewDisc(Mesh m, float aspect, float d, int cIn, int cOut, float aIn) {
            float hh = d * (float) Math.tan(Math.toRadians(FOV / 2)), hw = hh * aspect; int seg = 48, rings = 10;
            for (int r = 0; r < rings; r++) {
                float r0 = r / (float) rings, r1 = (r + 1) / (float) rings;
                float a0 = aIn * (1f - sm((r0 - 0.55f) / 0.42f)), a1 = aIn * (1f - sm((r1 - 0.55f) / 0.42f));
                int k0 = argb(a0, mixc(cIn, cOut, r0)), k1 = argb(a1, mixc(cIn, cOut, r1));
                for (int k = 0; k < seg; k++) {
                    double t0 = k * Math.PI * 2 / seg, t1 = (k + 1) * Math.PI * 2 / seg;
                    float x00 = (float) Math.cos(t0) * hw * r0, y00 = (float) Math.sin(t0) * hh * r0, x01 = (float) Math.cos(t1) * hw * r0, y01 = (float) Math.sin(t1) * hh * r0;
                    float x10 = (float) Math.cos(t0) * hw * r1, y10 = (float) Math.sin(t0) * hh * r1, x11 = (float) Math.cos(t1) * hw * r1, y11 = (float) Math.sin(t1) * hh * r1;
                    quad(m, x00, y00, -d, k0, x01, y01, -d, k0, x11, y11, -d, k1, x10, y10, -d, k1);
                }
            }
        }

        // ---------------- the sky
        static void backdrop(float aspect, int energy, float dim) {
            viewDisc(back, aspect, 600f, mixc(0x0C1238, energy, 0.10f), 0x04061A, 0.94f);
            if (dim > 0.01f) viewDisc(back, aspect, 590f, 0x000000, 0x000000, 0.55f * dim);
        }
        static void hazeDisc(float aspect, float d, float a) { viewDisc(haze, aspect, d, 0x0A1030, 0x080C24, a); }
        static void stars(float t, float dim) {
            for (int i = 0; i < 460; i++) {
                float x = (rnd(i * 7 + 1) - 0.5f) * 620f, y = -140f + rnd(i * 13 + 2) * 380f, z = -300f - rnd(i * 5 + 3) * 140f;
                float e = edge(x, y, z); if (e < 0.02f) continue;
                float tw = 0.55f + 0.45f * (float) Math.sin(t * (1.2f + rnd(i) * 2.5f) + i);
                float big = rnd(i * 31 + 9); float r = 0.5f + big * big * 1.4f;
                int col = i % 11 == 0 ? 0xFFD8A8 : i % 7 == 0 ? 0xA8C4FF : i % 13 == 0 ? 0xFFB0E0 : 0xFFFFFF;
                float a = e * tw * (0.45f + 0.55f * big) * (1f - 0.75f * dim);
                glow(sky, x, y, z, r * 1.6f, col, a * 0.8f);
                if (big > 0.9f) glint(sky, x, y, z, r * 5f, col, a * 0.7f);
            }
        }
        static final int[] NEB = {0x6A4AE8, 0x2A9FD8, 0xD84AA8, 0x3A5AE8, 0x8A3AD8};
        static void nebula(float t, int energy, int boss) {
            for (int i = 0; i < 34; i++) {
                float x = (rnd(i * 17 + 5) - 0.5f) * 420f, y = -60f + rnd(i * 23 + 1) * 220f, z = -230f - rnd(i * 3 + 7) * 120f;
                x += (float) Math.sin(t * 0.05f + i) * 4f;
                float e = edge(x, y, z); if (e < 0.02f) continue;
                int col = i % 3 == 0 ? energy : NEB[(i + boss) % NEB.length];
                float r = 40f + rnd(i * 41) * 70f;
                glow(sky, x, y, z, r, col, e * (0.05f + rnd(i * 9 + 2) * 0.07f));
            }
        }
        /** A ringed planet, shaded like a sphere (lit from the upper left), with a thin atmosphere. */
        static void planet(float t) {
            float px = -70f, py = 34f, pz = -230f, R = 24f; float e = edge(px, py, pz); if (e < 0.02f) return;
            int rings = 10, seg = 40; Vector3f L = new Vector3f(-0.6f, 0.55f, 0.58f).normalize();
            int dark = 0x120A2A, lit = 0x8A6AE8, hot = 0xD8C8FF;
            for (int r = 0; r < rings; r++) for (int k = 0; k < seg; k++) {
                float r0 = r / (float) rings, r1 = (r + 1) / (float) rings; double a0 = k * Math.PI * 2 / seg, a1 = (k + 1) * Math.PI * 2 / seg;
                float[][] pts = {{r0, (float) a0}, {r0, (float) a1}, {r1, (float) a1}, {r1, (float) a0}}; float[] xs = new float[12]; int[] cs = new int[4];
                for (int q = 0; q < 4; q++) {
                    float rr = pts[q][0], cx = (float) Math.cos(pts[q][1]) * rr, cy = (float) Math.sin(pts[q][1]) * rr, cz = (float) Math.sqrt(Math.max(0, 1 - rr * rr));
                    float sh = Math.max(0f, cx * L.x + cy * L.y + cz * L.z), band = 0.08f * (float) Math.sin(cy * 9f + 1.3f);
                    int col = sh < 0.5f ? mixc(dark, lit, sh * 2f + band) : mixc(lit, hot, (sh - 0.5f) * 1.2f);
                    cs[q] = argb(e, col);
                    xs[q * 3] = px + (right.x * cx + up.x * cy) * R; xs[q * 3 + 1] = py + (right.y * cx + up.y * cy) * R; xs[q * 3 + 2] = pz + (right.z * cx + up.z * cy) * R;
                }
                quad(planetM, xs[0], xs[1], xs[2], cs[0], xs[3], xs[4], xs[5], cs[1], xs[6], xs[7], xs[8], cs[2], xs[9], xs[10], xs[11], cs[3]);
            }
            glow(sky, px, py, pz, R * 1.35f, 0x8A6AE8, 0.22f * e);
            // its ring: a tilted band
            int seg2 = 64; float ri = R * 1.45f, ro = R * 2.1f;
            for (int k = 0; k < seg2; k++) {
                double a0 = k * Math.PI * 2 / seg2, a1 = (k + 1) * Math.PI * 2 / seg2; float[] q = new float[12];
                double[] as = {a0, a1, a1, a0}; float[] rs = {ri, ri, ro, ro};
                for (int j = 0; j < 4; j++) {
                    float cx = (float) Math.cos(as[j]) * rs[j], cy = (float) Math.sin(as[j]) * rs[j] * 0.28f; float rx = cx * 0.96f - cy * 0.28f, ry = cx * 0.28f + cy * 0.96f;
                    q[j * 3] = px + right.x * rx + up.x * ry; q[j * 3 + 1] = py + right.y * rx + up.y * ry; q[j * 3 + 2] = pz + right.z * rx + up.z * ry + (float) Math.sin(as[j]) * 2f;
                }
                int ca = argb(0.35f * e, 0xC8B8F8);
                quad(sky, q[0], q[1], q[2], ca, q[3], q[4], q[5], ca, q[6], q[7], q[8], argb(0.08f * e, 0xC8B8F8), q[9], q[10], q[11], argb(0.08f * e, 0xC8B8F8));
            }
        }
        static void shootingStars(float t) {
            float per = 2.3f; int k = (int) (t / per); float u = (t - k * per) / 0.7f; if (u > 1f) return;
            float x0 = -200f + rnd(k * 7) * 300f, y0 = 120f + rnd(k * 11) * 60f, z = -280f;
            for (int i = 0; i < 14; i++) {
                float uu = u - i * 0.025f; if (uu < 0) break;
                float x = x0 + uu * 160f, y = y0 - uu * 70f;
                glow(sky, x, y, z, 1.6f - i * 0.08f, 0xDDE6FF, (1f - i / 14f) * (1f - u) * 0.9f * edge(x, y, z));
            }
        }
        /** Dust and soft bokeh drifting between us and the island. */
        static void motes(float t) {
            for (int i = 0; i < 34; i++) {
                float x = (rnd(i * 5 + 1) - 0.5f) * 16f + (float) Math.sin(t * 0.3f + i) * 0.6f, y = -1f + rnd(i * 7 + 2) * 7f + ((t * 0.15f + rnd(i)) % 1f) * 1.2f, z = 3.5f + rnd(i * 3 + 3) * 4f;
                float e = edge(x, y, z); if (e < 0.02f) continue;
                boolean bokeh = i % 6 == 0;
                glow(add, x, y, z, bokeh ? 0.45f : 0.06f, bokeh ? 0x9AB8FF : 0xDDE6FF, e * (bokeh ? 0.06f : 0.5f) * (0.6f + 0.4f * (float) Math.sin(t * 2 + i)));
            }
        }

        // ---------------- blocks
        /** A block (any block item), its model centred: {state, cx, cy, cz, size}. */
        static Object[] art(Item it) {
            return ART.computeIfAbsent(it, k -> {
                Minecraft mc = Minecraft.getInstance(); ItemStackRenderState st = new ItemStackRenderState();
                mc.getItemModelResolver().updateForTopItem(st, new ItemStack(k), ItemDisplayContext.NONE, mc.level, null, 0);
                float cx = 0.5f, cy = 0.5f, cz = 0.5f, sz = 1f;
                try { AABB bb = st.getModelBoundingBox(); if (bb != null && bb.getXsize() > 0.01) { cx = (float) bb.getCenter().x; cy = (float) bb.getCenter().y; cz = (float) bb.getCenter().z; sz = (float) Math.max(bb.getXsize(), Math.max(bb.getYsize(), bb.getZsize())); } }
                catch (Throwable ignored) { }
                return new Object[]{st, cx, cy, cz, sz};
            });
        }
        static void block(SubmitNodeCollector c, PoseStack ps, Item it, float x, float y, float z, float sx, float sy, float sz, Quaternionf rot) {
            Object[] a = art(it); ItemStackRenderState st = (ItemStackRenderState) a[0]; if (st.isEmpty()) return; float n = (float) a[4];
            if (Math.max(sx, Math.max(sy, sz)) < 0.9f) { float e = sm(edge(x, y, z) * 1.6f); if (e < 0.05f) return; sx *= e; sy *= e; sz *= e; }   // debris, shards, meteors: gone before the soft edge
            ps.pushPose(); ps.translate(x, y, z); if (rot != null) ps.mulPose(rot); ps.scale(sx / n, sy / n, sz / n);
            ps.translate(-(float) a[1], -(float) a[2], -(float) a[3]);
            st.submit(ps, c, LIGHT, OverlayTexture.NO_OVERLAY, 0); ps.popPose();
        }
        static void block(SubmitNodeCollector c, PoseStack ps, Item it, float x, float y, float z, float s) { block(c, ps, it, x, y, z, s, s, s, null); }

        static Item[] soil(int boss) {
            return switch (boss) {
                case 0 -> new Item[]{Items.GRASS_BLOCK, Items.DIRT, Items.STONE, Items.COARSE_DIRT, Items.PODZOL};
                case 1 -> new Item[]{Items.COARSE_DIRT, Items.DIRT, Items.DEEPSLATE, Items.MOSSY_COBBLESTONE, Items.ROOTED_DIRT};
                case 2 -> new Item[]{Items.GRASS_BLOCK, Items.DIRT, Items.STONE, Items.MOSS_BLOCK, Items.GRASS_BLOCK};
                case 3 -> new Item[]{Items.END_STONE, Items.END_STONE, Items.OBSIDIAN, Items.END_STONE, Items.PURPUR_BLOCK};
                case 4 -> new Item[]{Items.NETHERRACK, Items.NETHERRACK, Items.BASALT, Items.BLACKSTONE, Items.MAGMA_BLOCK};
                default -> new Item[]{Items.CRIMSON_NYLIUM, Items.NETHERRACK, Items.BLACKSTONE, Items.NETHERRACK, Items.CRYING_OBSIDIAN};
            };
        }
        static final float IR = 3.6f;
        /** Depth of the island's column (0 = not there). */
        static int depth(int i, int j, float rad, int seed) {
            float d = (float) Math.sqrt(i * i + j * j); if (d > rad + (rnd(i * 31 + j * 17 + seed) - 0.5f) * 0.9f) return 0;
            return 1 + Math.round((rad - d) * 0.65f + rnd(i * 13 + j * 7 + seed) * 1.2f);
        }
        static void island(SubmitNodeCollector c, PoseStack ps, int boss) {
            Item[] s = soil(boss); int R = (int) Math.ceil(IR + 1);
            for (int i = -R; i <= R; i++) for (int j = -R; j <= R; j++) {
                int dp = depth(i, j, IR, boss * 101); if (dp == 0) continue;
                int dl = depth(i - 1, j, IR, boss * 101), dr = depth(i + 1, j, IR, boss * 101), df = depth(i, j + 1, IR, boss * 101), db = depth(i, j - 1, IR, boss * 101);
                for (int k = 0; k < dp; k++) {
                    boolean show = k == 0 || k == dp - 1 || dl <= k || dr <= k || df <= k || db <= k;
                    if (!show) continue;
                    float n = rnd(i * 71 + j * 37 + k * 13 + boss);
                    Item it = k == 0 ? (n < 0.18f ? s[3] : n < 0.26f ? s[4] : s[0]) : k <= 1 ? s[1] : (n < 0.25f ? s[1] : s[2]);
                    block(c, ps, it, i, -0.5f - k, j, 1f);
                }
            }
        }
        static final float[][] FAR = {{-15f, -3f, -14f, 2.2f}, {14f, 1.5f, -20f, 2.6f}, {-7f, 6.5f, -32f, 1.8f}, {21f, -5f, -30f, 2.0f}, {-26f, 3f, -42f, 2.4f}, {6f, 10f, -48f, 1.6f}};
        static void farIslands(SubmitNodeCollector c, PoseStack ps, float t, int boss) {
            Item[] s = soil(boss);
            for (int f = 0; f < FAR.length; f++) {
                float cx = FAR[f][0], cy = FAR[f][1] + (float) Math.sin(t * 0.6f + f * 1.7f) * 0.35f, cz = FAR[f][2], rad = FAR[f][3];
                if (edge(cx, cy, cz) < 0.35f) continue;
                int R = (int) Math.ceil(rad + 1);
                for (int i = -R; i <= R; i++) for (int j = -R; j <= R; j++) {
                    int dp = depth(i, j, rad, f * 977 + 5); if (dp == 0) continue;
                    for (int k = 0; k < Math.min(dp, 4); k++) block(c, ps, k == 0 ? s[0] : k == 1 ? s[1] : s[2], cx + i, cy - 0.5f - k, cz + j, 1f);
                }
                glow(add, cx, cy - 1f, cz, rad * 1.6f, 0x3A5AE8, 0.05f);
            }
        }

        /** The boss's spawn: the grave, the web, the den, the portal, the lava, the rift. */
        static void prop(SubmitNodeCollector c, PoseStack ps, float t, int boss) {
            float x = SPAWN.x, z = SPAWN.z;
            switch (boss) {
                case 0 -> {                                                                   // a headstone with a cross, and the mound it claws out of
                    block(c, ps, Items.MOSSY_STONE_BRICKS, x, 0.75f, z - 1.05f, 0.95f, 1.5f, 0.32f, null);
                    block(c, ps, Items.STONE_BRICKS, x, 1.62f, z - 1.05f, 0.7f, 0.25f, 0.3f, null);
                    block(c, ps, Items.COBBLESTONE, x, 1.05f, z - 0.86f, 0.12f, 0.6f, 0.06f, null); block(c, ps, Items.COBBLESTONE, x, 1.18f, z - 0.86f, 0.42f, 0.12f, 0.06f, null);
                    block(c, ps, Items.COARSE_DIRT, x, 0.08f, z + 0.1f, 1.1f, 0.18f, 1.7f, null);
                    if (t > SlayerScene.RISE && t < SlayerScene.WALK + 0.3f) for (int i = 0; i < 8; i++) {   // dirt thrown up as it climbs out
                        float u = ((t * 1.6f + i / 8f) % 1f); float a = (rnd(i * 5) - 0.5f) * 1.6f, b = (rnd(i * 7) - 0.5f) * 1.6f;
                        block(c, ps, Items.DIRT, x + a * u, 0.2f + u * 1.4f - u * u * 1.2f, z + b * u, 0.14f, 0.14f, 0.14f, new Quaternionf().rotateXYZ(u * 6, u * 4, i));
                    }
                }
                case 1 -> {                                                                   // cobwebs at the edge and a thread from the dark
                    block(c, ps, Items.COBWEB, x - 2.4f, 0.5f, z - 0.8f, 1f); block(c, ps, Items.COBWEB, x + 1.2f, 0.5f, z - 1.4f, 1f);
                    block(c, ps, Items.MOSSY_COBBLESTONE, x + 1.0f, 0.5f, z - 1.2f, 1f);
                }
                case 2 -> {                                                                   // a rocky den
                    for (int[] b : new int[][]{{-1, 0, -1}, {1, 0, -1}, {-1, 1, -1}, {1, 1, -1}, {0, 1, -1}, {-1, 0, -2}, {0, 0, -2}, {1, 0, -2}, {0, 2, -1}})
                        block(c, ps, (b[0] + b[1] + b[2]) % 2 == 0 ? Items.MOSSY_COBBLESTONE : Items.STONE, x + b[0], 0.5f + b[1], z + b[2], 1f);
                    block(c, ps, Items.COAL_BLOCK, x, 0.5f, z - 1.45f, 0.98f, 0.98f, 0.2f, null);
                }
                case 3 -> {                                                                   // a ring of end portal frames
                    for (int k = 0; k < 8; k++) { double an = k * Math.PI / 4; block(c, ps, Items.END_PORTAL_FRAME, x + (float) Math.cos(an) * 1.4f, 0.4f, z + (float) Math.sin(an) * 1.4f, 0.9f); }
                    for (int i = 0; i < 12; i++) { float u = ((t * 0.7f + i / 12f) % 1f), an = i * 0.9f + t * 2f; glow(add, x + (float) Math.cos(an) * (1f - u), 0.3f + u * 3f, z + (float) Math.sin(an) * (1f - u), 0.12f, 0xD060FF, (1f - u) * 0.9f); }
                    disc(add, x, 0.02f, z, 1.2f, 0x6A20A8, 0.5f, 0.6f);
                }
                case 4 -> {                                                                   // a lava pool
                    float pulse = 0.5f + 0.5f * (float) Math.sin(t * 4f);
                    for (int[] b : new int[][]{{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, -1}}) block(c, ps, Items.MAGMA_BLOCK, x + b[0], -0.48f, z + b[1], 1f);
                    disc(add, x + 0.2f, 0.03f, z, 1.7f, mixc(0xFF6A10, 0xFFB030, pulse), 0.7f, 0.5f);
                    glow(add, x, 0.6f, z, 2.2f, 0xFF7A20, 0.25f);
                    for (int i = 0; i < 6; i++) { float u = ((t * 0.9f + i / 6f) % 1f); glow(add, x + (rnd(i * 3) - 0.5f) * 2f, 0.1f + u * 1.5f, z + (rnd(i * 9) - 0.5f) * 2f, 0.07f, 0xFFE07A, 1f - u); }
                }
                default -> {                                                                  // crying obsidian round a red rift
                    block(c, ps, Items.CRYING_OBSIDIAN, x - 1f, 0.5f, z - 1f, 1f); block(c, ps, Items.CRYING_OBSIDIAN, x + 1f, 0.5f, z - 1f, 1f); block(c, ps, Items.BLACKSTONE, x + 1f, 1.5f, z - 1f, 1f);
                    float op = sm((t - 0.2f) / 0.5f) * (1f - 0.6f * sm((t - SlayerScene.WALK - 0.6f) / 0.5f));
                    if (op > 0.01f) {
                        for (int k = 0; k < 10; k++) { float u = k / 9f; glow(add, x + (rnd(k * 9) - 0.5f) * 0.3f, 0.1f + u * 2.4f, z - 0.4f, (0.35f - Math.abs(u - 0.5f) * 0.4f) * op, 0xFF2050, 0.7f * op); }
                        column(add, x, 0f, z - 0.4f, 2.6f, 0.12f * op, 0xFF8AB0, 0.9f * op);
                    }
                }
            }
        }

        // ---------------- the boss
        static net.minecraft.world.level.Level mobLevel; static final LivingEntity[] MOBS = new LivingEntity[6];
        static LivingEntity mob(int boss) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != mobLevel) { mobLevel = mc.level; java.util.Arrays.fill(MOBS, null); }
            if (MOBS[boss] == null) {
                LivingEntity e = switch (boss) {
                    case 0 -> EntityTypes.ZOMBIE.create(mc.level, EntitySpawnReason.LOAD);
                    case 1 -> EntityTypes.SPIDER.create(mc.level, EntitySpawnReason.LOAD);
                    case 2 -> EntityTypes.WOLF.create(mc.level, EntitySpawnReason.LOAD);
                    case 3 -> EntityTypes.ENDERMAN.create(mc.level, EntitySpawnReason.LOAD);
                    case 4 -> EntityTypes.BLAZE.create(mc.level, EntitySpawnReason.LOAD);
                    default -> EntityTypes.VINDICATOR.create(mc.level, EntitySpawnReason.LOAD);
                };
                if (e != null && boss == 0) { e.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE)); e.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS)); }
                if (e != null && boss == 5) e.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
                MOBS[boss] = e;
            }
            return MOBS[boss];
        }
        static final float[] MOB_SCALE = {1.15f, 1.5f, 1.7f, 1.0f, 1.2f, 1.15f};
        /** Where the boss stands at time t (before any knockback). */
        static Vector3f bossAt(float t) {
            float w = sm((t - SlayerScene.WALK) / (SlayerScene.WALK_END - SlayerScene.WALK));
            return new Vector3f(lerp(SPAWN.x, FRONT.x, w), 0f, lerp(SPAWN.z, FRONT.z, w));
        }
        static void boss(SubmitNodeCollector c, PoseStack ps, Minecraft mc, float t, int type, int boss, float[] hits, float last, int energy) {
            if (t < SlayerScene.RISE - (boss == 3 || boss == 5 ? 0.1f : 0f)) return;
            Vector3f at = bossAt(t); float x = at.x, y = 0f, z = at.z, k = MOB_SCALE[boss];
            float ar = (t - SlayerScene.RISE) / 0.95f;
            switch (boss) {
                case 0 -> y = lerp(-2.3f, 0f, eo(ar));
                case 1 -> { y = lerp(7f, 0f, eo(ar)); if (ar < 1.2f) column(add, x, y + 0.6f, z, 12f, 0.03f, 0xE8ECF8, 0.7f * (1f - sm((ar - 0.9f) / 0.3f))); }
                case 2 -> z -= 1.6f * (1f - eo(ar / 0.7f));
                case 3 -> { if (ar < 0.6f && ((int) (t * 30)) % 3 == 0) return; if (ar < 0.8f) for (int i = 0; i < 10; i++) glow(add, x + (rnd(i * 3 + (int) (t * 20)) - 0.5f) * 1.4f, rnd(i * 7 + (int) (t * 20)) * 3f, z + (rnd(i * 5) - 0.5f) * 1.4f, 0.14f, 0xC060FF, 1f - ar); }
                case 4 -> y = lerp(-2.6f, 0.35f, eo(ar)) + 0.18f * (float) Math.sin(t * 3f);
                default -> { if (ar < 0.25f) return; x += 0.5f * (1f - eo((ar - 0.25f) / 0.6f)); }
            }
            // after the last hit: crumble (no drops) or shatter (a drop, RNG)
            float since = t - last;
            if (since >= 0 && type > 0) { shatter(c, ps, since, boss, x, z, energy); return; }
            float sink = type == 0 && since > 0 ? sm((since - 0.7f) / 0.6f) : 0f;
            if (sink >= 1f) { ash(since, x, z); return; }
            // knockback and the red flash on every hit
            boolean red = false; float kb = 0f;
            for (int i = 0; i < hits.length; i++) { float d = t - hits[i]; if (d >= 0 && d < 0.16f) red = true; if (d >= 0 && d < 0.35f) kb += (1f - d / 0.35f) * 0.18f; }
            LivingEntity e = mob(boss); if (e == null) return;
            net.minecraft.client.renderer.entity.EntityRenderer<? super LivingEntity, ?> rr = mc.getEntityRenderDispatcher().getRenderer(e); EntityRenderState st = rr.createRenderState(e, 1f);   // like the inventory's player
            st.shadowPieces.clear(); st.outlineColor = 0; st.nameTag = null; st.lightCoords = LIGHT; st.ageInTicks = t * 20f;
            float walking = t > SlayerScene.WALK && t < SlayerScene.WALK_END ? 1f : boss == 2 && ar < 0.7f ? 1f : 0f;
            float dx = FRONT.x - SPAWN.x, dz = FRONT.z - SPAWN.z;
            float face = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float toCam = (float) Math.toDegrees(Math.atan2(-(eye.x - x), eye.z - z));
            float yawNow = t < SlayerScene.WALK ? toCam : t < SlayerScene.WALK_END ? face : lerp(face, toCam, sm((t - SlayerScene.WALK_END) / 0.4f));
            if (st instanceof LivingEntityRenderState L) {
                L.bodyRot = yawNow; L.yRot = 0f; L.xRot = -8f; L.walkAnimationPos = t * 7f; L.walkAnimationSpeed = walking * 0.9f;
                L.hasRedOverlay = red || (type == 0 && since > 0); L.scale = k;
                if (type == 0 && since > 0) L.deathTime = Math.min(20f, since * 24f);
            }
            z -= kb; y -= sink * 1.4f;
            mc.getEntityRenderDispatcher().submit(st, cam(), x, y, z, ps, c);
            if (red) glow(add, x, y + 1.1f * k, z, 1.6f * k, 0xFFFFFF, 0.25f);
            glow(add, x, 0.05f, z, 1.3f * k, energy, 0.16f);                                         // its aura on the ground
            if (type == 0 && since > 0.3f) ash(since, x, z);
        }
        static CameraRenderState cam() {
            CameraRenderState cs = new CameraRenderState(); cs.pos = new Vec3(eye.x, eye.y, eye.z); cs.initialized = true;
            cs.orientation = new Quaternionf().setFromUnnormalized(new Matrix4f(view).invert()); return cs;
        }
        /** Grey ash blowing off the fallen boss. */
        static void ash(float since, float x, float z) {
            for (int i = 0; i < 26; i++) {
                float u = (since - 0.3f) * 0.7f - rnd(i * 3) * 0.4f; if (u <= 0 || u > 1f) continue;
                glow(veil, x + (rnd(i * 7) - 0.5f) * 1.2f + u * 1.5f, 0.3f + rnd(i * 11) * 1.4f + u * 2.2f, z + (rnd(i * 5) - 0.5f) * 0.8f, 0.25f + u * 0.5f, 0x8A8A96, (1f - u) * 0.35f);
            }
        }
        static Item[] shards(int boss) {
            return switch (boss) {
                case 0 -> new Item[]{Items.CONCRETE.green(), Items.DIAMOND_BLOCK, Items.CONCRETE.cyan(), Items.DYED_TERRACOTTA.green()};
                case 1 -> new Item[]{Items.COAL_BLOCK, Items.CONCRETE.black(), Items.CONCRETE.red(), Items.CONCRETE.gray()};
                case 2 -> new Item[]{Items.CONCRETE.white(), Items.CONCRETE.lightGray(), Items.SNOW_BLOCK, Items.CONCRETE.gray()};
                case 3 -> new Item[]{Items.CONCRETE.black(), Items.OBSIDIAN, Items.CONCRETE.purple(), Items.CONCRETE.magenta()};
                case 4 -> new Item[]{Items.CONCRETE.yellow(), Items.GOLD_BLOCK, Items.CONCRETE.orange(), Items.BLACKSTONE};
                default -> new Item[]{Items.CONCRETE.red(), Items.BLACKSTONE, Items.CONCRETE.gray(), Items.CONCRETE.black()};
            };
        }
        /** The boss bursts into cubes of its own colours. */
        static void shatter(SubmitNodeCollector c, PoseStack ps, float d, int boss, float x, float z, int energy) {
            if (d > 1.6f) return; Item[] sh = shards(boss); float g = 9f;
            for (int i = 0; i < 44; i++) {
                float an = rnd(i * 13 + 1) * 6.2832f, sp = 2.5f + rnd(i * 7 + 2) * 4.5f, vy = 2f + rnd(i * 11 + 3) * 5.5f, y0 = 0.2f + rnd(i * 5) * 1.8f * MOB_SCALE[boss];
                float px = x + (float) Math.cos(an) * sp * d, pz = z + (float) Math.sin(an) * sp * d * 0.8f, py = y0 + vy * d - g * d * d * 0.5f;
                if (py < 0.08f) py = 0.08f;
                float s = (0.16f + rnd(i * 17) * 0.16f) * (1f - sm((d - 1.0f) / 0.6f));
                if (s > 0.01f) block(c, ps, sh[i % sh.length], px, py, pz, s, s, s, new Quaternionf().rotateXYZ(d * (3 + i % 5), d * (2 + i % 3), i));
            }
            glow(add, x, 1f, z, 3.5f * (1f + d), energy, 0.6f * (1f - d / 0.6f));
        }

        // ---------------- meteors
        static Vector3f aim(int type, int i, int n) {
            boolean big = type == 0 || i == n - 1;
            Vector3f b = bossAt(SlayerScene.HITS[type][i]);
            if (big) return b.add(0, 0f, 0);
            return b.add((rnd(i * 31 + 3) - 0.5f) * 3.2f, 0f, (rnd(i * 17 + 5) - 0.5f) * 2.4f);
        }
        static void meteor(SubmitNodeCollector c, PoseStack ps, float t, int type, int boss, int i, float[] hits) {
            int n = hits.length; boolean big = type == 0 || i == n - 1; float h = hits[i], fl = SlayerScene.flight(type, i), t0 = h - fl;
            Vector3f a = aim(type, i, n); float k = big ? (type == 2 ? 1.6f : 1.3f) : 1f;
            Vector3f s0 = new Vector3f(a.x + (6f + 3f * rnd(i * 7 + 1)) * k, a.y + (15f + 4f * rnd(i * 5)) * k, a.z - (5f + 3f * rnd(i * 3)) * k);
            int col = type == 2 ? (big ? 0xFFD24A : SlayerScene.TIER[Math.min(i, SlayerScene.TIER.length - 1)]) : big ? 0xFFB040 : 0xFF8A30;
            if (t >= t0 && t < h) {
                float u = (t - t0) / fl;
                for (int q = 22; q >= 0; q--) {                                                        // the trail, tail first
                    float uu = u - q * 0.03f; if (uu < 0) continue; float e = uu * uu * (0.55f + 0.45f * uu);
                    float px = lerp(s0.x, a.x, e), py = lerp(s0.y, a.y, e), pz = lerp(s0.z, a.z, e), f = 1f - q / 23f;
                    int tc = type == 2 && big ? rainbow(q / 23f + u) : mixc(col, 0xFF3A10, 1f - f);
                    glow(add, px, py, pz, (0.25f + 0.7f * f) * k, tc, 0.55f * f);
                }
                float e = u * u * (0.55f + 0.45f * u), px = lerp(s0.x, a.x, e), py = lerp(s0.y, a.y, e), pz = lerp(s0.z, a.z, e);
                glow(add, px, py, pz, 2.6f * k, col, 0.35f); glow(add, px, py, pz, 1.0f * k, mixc(col, 0xFFFFFF, 0.5f), 0.9f);
                block(c, ps, type == 2 && big ? Items.GOLD_BLOCK : Items.MAGMA_BLOCK, px, py, pz, 0.55f * k, 0.55f * k, 0.55f * k, new Quaternionf().rotateXYZ(t * 7f + i, t * 5f, i));
                for (int q = 0; q < 8; q++) { float v = ((u * 3f + q / 8f) % 1f); glow(add, px + (rnd(i * 11 + q) - 0.5f) * 0.8f + v * 1.2f * k, py + v * 2.5f * k, pz - v * 0.8f, 0.09f, q % 2 == 0 ? 0xFFE07A : col, 1f - v); }
                glow(add, a.x, 0.05f, a.z, 1.4f * k * u, col, 0.35f * u);                              // its light on the ground as it comes
            }
            float d = t - h; if (d < 0 || d > 2.2f) return;
            impact(c, ps, d, a, i, type, big, col, boss);
        }
        static int rainbow(float u) { int[] r = {0xFF5A5A, 0xFFB04A, 0xFFE04A, 0x6AE87A, 0x6AB8FF, 0xB56BFF}; float v = ((u % 1f) + 1f) % 1f * r.length; int i = (int) v; return mixc(r[i % r.length], r[(i + 1) % r.length], v - i); }
        static void impact(SubmitNodeCollector c, PoseStack ps, float d, Vector3f a, int i, int type, boolean big, int col, int boss) {
            float s = big ? (type == 2 ? 2.2f : 1.6f) : 0.9f;
            float fl = Math.max(0f, 1f - d / 0.28f);
            if (fl > 0) { glow(add, a.x, 0.8f * s, a.z, 3.4f * s, 0xFFFFFF, 0.9f * fl); glow(add, a.x, 0.4f, a.z, 6f * s, col, 0.5f * fl); }
            float rw = d / 0.7f; if (rw < 1f) ring(add, a.x, 0.04f, a.z, (0.4f + 4.6f * eo(rw)) * s, 0.35f * s, mixc(col, 0xFFFFFF, 0.4f), 0.9f * (1f - rw));
            if (big) { float r2 = (d - 0.08f) / 1.0f; if (r2 > 0 && r2 < 1f) ring(add, a.x, 0.05f, a.z, (0.5f + 7f * eo(r2)) * s, 0.22f * s, col, 0.6f * (1f - r2)); }
            Item[] so = soil(boss); float g = 9.8f;
            int nd = big ? 22 : 9;
            for (int q = 0; q < nd; q++) {                                                            // ground thrown up
                float an = rnd(i * 37 + q) * 6.2832f, v = (1.5f + 3f * rnd(i * 13 + q)) * Math.min(1.4f, s), vy = 3f + 4f * rnd(i * 19 + q);
                float px = a.x + (float) Math.cos(an) * v * d, pz = a.z + (float) Math.sin(an) * v * d, py = 0.2f + vy * d - g * d * d * 0.5f;
                if (py < -0.3f || d > 1.6f) continue;
                float sc = (0.18f + 0.14f * rnd(q * 3 + i)) * (1f - sm((d - 1.1f) / 0.5f));
                block(c, ps, so[q % 3 == 0 ? 1 : q % 3 == 1 ? 0 : 2], px, py, pz, sc, sc, sc, new Quaternionf().rotateXYZ(d * 6 + q, d * 4, q));
            }
            for (int q = 0; q < (big ? 16 : 6); q++) {                                               // embers
                float an = rnd(i * 41 + q) * 6.2832f, v = 2f + 3f * rnd(i * 23 + q), vy = 4f + 3f * rnd(i * 29 + q), tt = Math.min(d, 1.2f);
                float py = 0.3f + vy * tt - g * tt * tt * 0.5f; if (py < 0) continue;
                glow(add, a.x + (float) Math.cos(an) * v * tt, py, a.z + (float) Math.sin(an) * v * tt, 0.1f, q % 2 == 0 ? 0xFFE07A : col, 1f - tt / 1.2f);
            }
            float cool = Math.max(0f, 1f - d / 1.8f);                                                  // the crater: dark, glowing at the rim as it cools
            disc(veil, a.x, 0.02f, a.z, 0.7f * s, 0x120A08, 0.8f * Math.min(1f, d * 8), 0.4f);
            ring(add, a.x, 0.03f, a.z, 0.55f * s, 0.18f * s, col, 0.8f * cool);
            for (int q = 0; q < 5; q++) { float u = ((d * 0.6f + q / 5f) % 1f); glow(veil, a.x + (rnd(i * 5 + q) - 0.5f) * 0.8f * s, 0.3f + u * 2.5f, a.z, 0.35f + u * 0.6f, 0x6A6A76, (1f - u) * 0.3f * cool); }
        }

        // ---------------- the drop
        static void reveal(float t, float p, int type, float A, int energy) {
            float x = FRONT.x, z = FRONT.z; float beam = sm(p / 0.25f) * (1f - sm((t - (A - 0.8f)) / 0.6f));
            if (beam < 0.01f) return;
            if (type == 2) {
                float pil = beam * (0.75f + 0.25f * (float) Math.sin(t * 9f));
                column(add, x, 0f, z, 22f, 1.6f * pil, 0xFFD24A, 0.35f * pil); column(add, x, 0f, z, 22f, 0.55f * pil, 0xFFF4C8, 0.85f * pil);
                for (int k = 0; k < 3; k++) { float u = ((p * 0.8f + k / 3f) % 1f); ring(add, x, 0.06f + u * 0.4f, z, 0.6f + 5.5f * u, 0.25f, 0xFFD24A, (1f - u) * 0.8f * beam); }
                for (int k = 0; k < 10; k++) { float an = t * 0.9f + k * 0.628f, rr = 1.6f + 0.3f * (float) Math.sin(t * 3 + k); glint(add, x + (float) Math.cos(an) * rr, 2.2f + (float) Math.sin(an * 2) * 0.4f, z + (float) Math.sin(an) * rr, k % 3 == 0 ? 0.6f : 0.35f, k % 2 == 0 ? 0xFFF4C8 : 0xFFD24A, beam); }
                for (int k = 0; k < 70; k++) {                                                     // confetti
                    float u = ((p * 0.35f + rnd(k * 7)) % 1f), cx = x + (rnd(k * 13) - 0.5f) * 9f, cz = z + (rnd(k * 19) - 0.5f) * 5f, cy = 8f - u * 9f;
                    int cc = k % 4 == 0 ? 0xFF7AD9 : k % 4 == 1 ? 0x6AB8FF : k % 4 == 2 ? 0xFFE04A : 0x7AE88A;
                    glint(add, cx + (float) Math.sin(t * 3 + k) * 0.3f, cy, cz, 0.16f, cc, (1f - u) * beam);
                }
                for (int k = 0; k < 12; k++) {                                                     // god rays round the drop
                    float an = k * 0.5236f + t * 0.25f, len = 6f + 2f * (float) Math.sin(t * 2 + k); float ax = (float) Math.cos(an), ay = (float) Math.sin(an);
                    float cx = x, cy = 2.3f, cz = z, w = 0.25f;
                    float ex = cx + (right.x * ax + up.x * ay) * len, ey = cy + (right.y * ax + up.y * ay) * len, ez = cz + (right.z * ax + up.z * ay) * len;
                    float px2 = (right.x * -ay + up.x * ax) * w, py2 = (right.y * -ay + up.y * ax) * w, pz2 = (right.z * -ay + up.z * ax) * w;
                    int c0 = argb(0.22f * beam, 0xFFE9A0), c1 = argb(0, 0xFFE9A0);
                    quad(add, cx, cy, cz, c0, cx, cy, cz, c0, ex + px2, ey + py2, ez + pz2, c1, ex - px2, ey - py2, ez - pz2, c1);
                }
            } else {
                column(add, x, 0f, z, 14f, 0.8f * beam, energy, 0.3f * beam); column(add, x, 0f, z, 14f, 0.26f * beam, mixc(energy, 0xFFFFFF, 0.55f), 0.7f * beam);
                ring(add, x, 0.06f, z, 1.1f + 0.2f * (float) Math.sin(t * 4), 0.25f, energy, 0.6f * beam);
                for (int k = 0; k < 16; k++) { float u = ((p * 0.6f + rnd(k * 7)) % 1f); glow(add, x + (rnd(k * 5) - 0.5f) * 1.2f, u * 5f, z + (rnd(k * 9) - 0.5f) * 1.2f, 0.08f, k % 2 == 0 ? 0xFFFFFF : energy, (1f - u) * beam); }
            }
        }
    }

}
