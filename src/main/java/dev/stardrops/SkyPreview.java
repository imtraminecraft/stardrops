package dev.stardrops;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * The menu's picture of the night sky: the real sky box (the same stars, Milky Way, planets, your constellations, aurora and shooting
 * stars, at the same turn and brightness as in your world, with your Cosmos switches), drawn by the computer into a small picture about
 * eight times a second and shown in the Visuals door and the Cosmos preview. It always looks at the same patch of sky (the Milky Way, the
 * ringed planet and the aurora), slowly panning, so every switch you flip shows in it.
 */
final class SkyPreview {
    private SkyPreview() {}
    static final int TW = 320, TH = 112; static float YAW = -55f, PITCH = 38f, GAIN = 1.8f;
    private static final Identifier[] ID = {Identifier.fromNamespaceAndPath("stardrops", "sky_preview_0"), Identifier.fromNamespaceAndPath("stardrops", "sky_preview_1")};
    private static final DynamicTexture[] TEX = new DynamicTexture[2]; private static final long[] AT = new long[2]; private static final float[] ASP = new float[2];
    private static boolean failed; private static double cost = -1; private static long gap = 200;
    private static final float[] acc = new float[TW * TH * 3];
    private static float[] vp = new float[4096 * 3]; private static int[] vc = new int[4096]; private static int vn;

    /** Draws the sky into the box (gui coordinates). slot: 0 the Visuals door, 1 the Cosmos preview (each keeps its own picture). */
    static void draw(GuiGraphicsExtractor g, int slot, int x, int y, int w, int h) {
        if (failed || w < 4 || h < 4) return;
        try {
            long now = System.currentTimeMillis(); float asp = w / (float) h;
            if (TEX[slot] == null) { TEX[slot] = new DynamicTexture(() -> "stardrops sky preview", TW, TH, true); Minecraft.getInstance().getTextureManager().register(ID[slot], TEX[slot]); AT[slot] = 0; }
            if (now - AT[slot] > gap || Math.abs(ASP[slot] - asp) > 0.05f) { AT[slot] = now; ASP[slot] = asp; long t0 = System.nanoTime(); paint(TEX[slot], now, asp);
                double ms = (System.nanoTime() - t0) / 1e6; cost = cost < 0 ? ms : cost * 0.8 + ms * 0.2; gap = (long) Math.max(200, Math.min(1000, cost * 40)); }   // drawn by the computer: at most 5 times a second, less on a slow one (never more than 2.5% of the time)
            Gfx.push(g); Gfx.translate(g, x, y); g.pose().scale(w / (float) TW, h / (float) TH);
            g.blit(RenderPipelines.GUI_TEXTURED, ID[slot], 0, 0, 0f, 0f, TW, TH, TW, TH, TW, TH, 0xFFFFFFFF);
            Gfx.pop(g);
        } catch (Throwable e) { failed = true; System.err.println("[Stardrops] sky preview off: " + e); }
    }

    private static final int[] PX = new int[TW * TH];
    private static void paint(DynamicTexture tex, long now, float asp) {
        render(PX, now, asp);
        NativeImage img = tex.getPixels(); if (img == null) return;
        for (int y = 0; y < TH; y++) for (int x = 0; x < TW; x++) img.setPixelABGR(x, y, PX[y * TW + x]);
        tex.upload();
    }
    /** The picture as ABGR pixels, TW x TH (also used by the test bench). */
    static void render(int[] out, long now, float asp) {
        StardropsConfig c = StardropsConfig.get();
        SkyBox.make0();
        float t = (now % 3_600_000L) / 1000f, yaw = YAW + 14f * (float) Math.sin(t * 0.05f);
        Matrix4f pv = new Matrix4f().perspective((float) Math.toRadians(62), asp, 0.05f, 10f).rotateX((float) Math.toRadians(-PITCH)).rotateY((float) Math.toRadians(yaw))
                .rotate(SkyBox.rot(0.6f));             // a fixed turn, looking where the Milky Way, the ringed planet and the aurora meet
        float keepS = SkyBox.minS, kt = SkyBox.drawTurn; boolean km = SkyBox.milky, kp = SkyBox.planets, kd = SkyBox.deep, kc = SkyBox.comet;
        SkyBox.minS = 0.3f * (float) Math.toRadians(62) / TH; SkyBox.milky = c.cosMilky; SkyBox.planets = c.cosPlanets; SkyBox.deep = c.cosDeep; SkyBox.comet = c.cosComet; SkyBox.drawTurn = 0.6f;
        vn = 0;
        try { SkyBox.draw(SkyBox.guard(SkyPreview::v), t, Math.max(0.3f, c.cosSkyBright / 100f), c.cosCons ? Codex.skyFigures() : null, c.cosShooting, c.cosAurora); }
        finally { SkyBox.minS = keepS; SkyBox.milky = km; SkyBox.planets = kp; SkyBox.deep = kd; SkyBox.comet = kc; SkyBox.drawTurn = kt; }
        java.util.Arrays.fill(acc, 0f);
        float[][] p = new float[4][3];
        for (int q = 0; q + 3 < vn; q += 4) {
            boolean ok = true;
            for (int k = 0; k < 4; k++) { int i = q + k; Vector4f cl = pv.transform(new Vector4f(vp[i * 3], vp[i * 3 + 1], vp[i * 3 + 2], 1f)); if (cl.w <= 0.01f) { ok = false; break; }
                p[k][0] = (cl.x / cl.w * 0.5f + 0.5f) * TW; p[k][1] = (0.5f - cl.y / cl.w * 0.5f) * TH; p[k][2] = i; }
            if (!ok) continue;
            tri(p[0], p[1], p[2]); tri(p[0], p[2], p[3]);
        }
        for (int y = 0; y < TH; y++) { float f = y / (float) TH; int br = Math.round(4 + 6 * f), bg = Math.round(5 + 4 * f), bb = Math.round(22 + 6 * f);      // a deep night, a little lighter toward the horizon
            for (int x = 0; x < TW; x++) { int o = (y * TW + x) * 3;
                int r = Math.min(255, br + Math.round(acc[o] * 255 * GAIN)), gg = Math.min(255, bg + Math.round(acc[o + 1] * 255 * GAIN)), b = Math.min(255, bb + Math.round(acc[o + 2] * 255 * GAIN));      // brighter than the real sky: it is small
                out[y * TW + x] = 0xFF000000 | b << 16 | gg << 8 | r; } }
    }
    private static void v(float x, float y, float z, int argb) {
        if (vn == vc.length) { vc = java.util.Arrays.copyOf(vc, vn * 2); vp = java.util.Arrays.copyOf(vp, vn * 6); }
        vp[vn * 3] = x; vp[vn * 3 + 1] = y; vp[vn * 3 + 2] = z; vc[vn++] = argb;
    }
    /** Adds a triangle's light (colours blended across it, additive like the real sky). */
    private static void tri(float[] a, float[] b, float[] c) {
        int x0 = (int) Math.max(0, Math.floor(Math.min(a[0], Math.min(b[0], c[0])))), x1 = (int) Math.min(TW - 1, Math.ceil(Math.max(a[0], Math.max(b[0], c[0]))));
        int y0 = (int) Math.max(0, Math.floor(Math.min(a[1], Math.min(b[1], c[1])))), y1 = (int) Math.min(TH - 1, Math.ceil(Math.max(a[1], Math.max(b[1], c[1]))));
        if (x1 < x0 || y1 < y0) return;
        float den = (b[1] - c[1]) * (a[0] - c[0]) + (c[0] - b[0]) * (a[1] - c[1]); if (Math.abs(den) < 1e-6f) return;
        int ca = vc[(int) a[2]], cb = vc[(int) b[2]], cc = vc[(int) c[2]];
        float tiny = (x1 - x0 + 1) * (y1 - y0 + 1) <= 2 ? 1f : 0f;                                 // a star smaller than a pixel still lights its pixel
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) {
            float px = x + 0.5f, py = y + 0.5f, w0 = ((b[1] - c[1]) * (px - c[0]) + (c[0] - b[0]) * (py - c[1])) / den, w1 = ((c[1] - a[1]) * (px - c[0]) + (a[0] - c[0]) * (py - c[1])) / den, w2 = 1f - w0 - w1;
            if (tiny == 0f && (w0 < 0 || w1 < 0 || w2 < 0)) continue;
            if (tiny == 1f) { w0 = w1 = w2 = 1f / 6f; }                                             // (each of its two triangles gives half)
            int o = (y * TW + x) * 3;
            float aa = (ca >>> 24) / 255f * w0, ab = (cb >>> 24) / 255f * w1, ac = (cc >>> 24) / 255f * w2;
            acc[o] += ((ca >> 16 & 255) * aa + (cb >> 16 & 255) * ab + (cc >> 16 & 255) * ac) / 255f;
            acc[o + 1] += ((ca >> 8 & 255) * aa + (cb >> 8 & 255) * ab + (cc >> 8 & 255) * ac) / 255f;
            acc[o + 2] += ((ca & 255) * aa + (cb & 255) * ab + (cc & 255) * ac) / 255f;
        }
    }
}
