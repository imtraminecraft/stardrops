package dev.stardrops;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Visuals > Cosmos > Night sky: a real sky box. At night the world's own sky fills with our stars, drawn in the level (not over the screen),
 * so hills, trees and your island stand in front of them and they turn with the night like the game's own stars.
 *
 *   about 1,500 pixel stars in the colours real stars have, the bright ones with a soft halo and a twinkle, thick along a Milky Way band of
 *   faint coloured haze; a ringed planet and a small red one, lit from one side; the eight constellations of your Achievements where they
 *   belong, every star you have lit shining in its colour and joined to the next lit one; shooting stars across it (Shooting stars) and
 *   curtains of aurora low in the north (Aurora).
 *
 * How: Fabric's LevelRenderEvents.COLLECT_SUBMITS hands us the level's collector; we submit one mesh of coloured quads on a huge sphere round
 * the camera (twice the render distance, so all terrain is nearer), with our own pipeline: additive, depth tested, never written, no fog.
 * If the pipeline cannot be made or drawing ever throws, the sky box switches itself off (CosmosHud then draws its old screen stars instead).
 */
final class SkyBox {
    private SkyBox() {}

    static boolean registered, broken;
    private static RenderType SKY;
    static boolean on() { return registered && !broken; }

    static void register() {
        try {
            RenderPipeline p = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("stardrops", "pipeline/cosmos_sky"))
                    .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                    .withCull(false).withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false)).build());
            SKY = RenderType.create("stardrops_cosmos_sky", RenderSetup.builder(p).createRenderSetup());
            LevelRenderEvents.COLLECT_SUBMITS.register(SkyBox::submit);
            registered = true;
        } catch (Throwable t) { System.err.println("[Stardrops] sky box unavailable, using screen stars: " + t); }
    }

    // ------------------------------------------------------------------ the sky's contents, made once
    private static float[] sDir, sU, sV, sSize, sPh; private static int[] sCol; private static int N;
    private static float[][] haze;                                              // x, y, z, radius (radians), colour, alpha
    private static final int[] STAR_COLS = {0xFFFFFF, 0xFFFFFF, 0xEAF0FF, 0xCFE0FF, 0xAECBFF, 0xFFF4D8, 0xFFE2B0, 0xFFC890, 0xFFA878};
    private static float rnd(int i) { int h = i * 374761393 + 668265263; h = (h ^ (h >>> 13)) * 1274126177; return ((h ^ (h >>> 16)) & 0xFFFF) / 65535f; }
    /** The Milky Way runs along the great circle perpendicular to this. */
    private static final Vector3f BAND_N = new Vector3f(0.35f, 0.55f, 0.76f).normalize();
    private static void basis(Vector3f d, Vector3f u, Vector3f v) {
        Vector3f a = Math.abs(d.y) < 0.9f ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0);
        d.cross(a, u).normalize(); d.cross(u, v).normalize();
    }
    static void make() {
        N = 1500; sDir = new float[N * 3]; sU = new float[N * 3]; sV = new float[N * 3]; sSize = new float[N]; sPh = new float[N]; sCol = new int[N];
        Vector3f bu = new Vector3f(), bv = new Vector3f(); basis(BAND_N, bu, bv);
        for (int i = 0; i < N; i++) {
            Vector3f d;
            if (i % 5 < 2) {                                                    // two in five crowd the Milky Way
                double lon = rnd(i * 7 + 1) * Math.PI * 2, lat = (rnd(i * 13 + 5) + rnd(i * 17 + 9) + rnd(i * 23 + 2) - 1.5) * 0.22;
                d = new Vector3f(bu).mul((float) (Math.cos(lon) * Math.cos(lat))).add(new Vector3f(bv).mul((float) (Math.sin(lon) * Math.cos(lat)))).add(new Vector3f(BAND_N).mul((float) Math.sin(lat)));
            } else {
                float z = rnd(i * 7 + 1) * 2f - 1f; double a = rnd(i * 13 + 5) * Math.PI * 2, r = Math.sqrt(1 - z * z); d = new Vector3f((float) (Math.cos(a) * r), z, (float) (Math.sin(a) * r));
            }
            d.normalize(); Vector3f u = new Vector3f(), v = new Vector3f(); basis(d, u, v);
            float m = rnd(i * 31 + 7); m = m * m * m;                            // few bright, many faint
            sDir[i * 3] = d.x; sDir[i * 3 + 1] = d.y; sDir[i * 3 + 2] = d.z; sU[i * 3] = u.x; sU[i * 3 + 1] = u.y; sU[i * 3 + 2] = u.z; sV[i * 3] = v.x; sV[i * 3 + 1] = v.y; sV[i * 3 + 2] = v.z;
            sSize[i] = 0.0016f + 0.0042f * m; sPh[i] = rnd(i * 3 + 11) * 6.28f; sCol[i] = STAR_COLS[(int) (rnd(i * 41 + 3) * STAR_COLS.length) % STAR_COLS.length];
        }
        int[] hc = {0x6A7AE8, 0x8A6AE0, 0x4A8AD8, 0xC86AB8, 0x5A6AC8, 0xE8A07A};
        haze = new float[64][];
        for (int i = 0; i < haze.length; i++) {
            double lon = i / (double) haze.length * Math.PI * 2 + rnd(i * 5) * 0.12, lat = (rnd(i * 9 + 4) - 0.5) * 0.2;
            Vector3f d = new Vector3f(bu).mul((float) (Math.cos(lon) * Math.cos(lat))).add(new Vector3f(bv).mul((float) (Math.sin(lon) * Math.cos(lat)))).add(new Vector3f(BAND_N).mul((float) Math.sin(lat))).normalize();
            boolean core = Math.abs(lon - 1.2) < 0.9;                             // the bright heart of the galaxy
            haze[i] = new float[]{d.x, d.y, d.z, 0.16f + 0.14f * rnd(i * 3 + 1) + (core ? 0.08f : 0f), hc[i % (core ? hc.length : 5)], (0.05f + 0.05f * rnd(i * 11)) * (core ? 1.7f : 1f)};
        }
    }

    // ------------------------------------------------------------------ drawing
    interface Q { void v(float x, float y, float z, int argb); }
    private static int argb(float a, int rgb) { return Math.round(Math.max(0f, Math.min(1f, a)) * 255) << 24 | (rgb & 0xFFFFFF); }
    private static long nextShoot; private static final float[][] shoots = new float[3][]; private static final java.util.Random R = new java.util.Random();

    static void submit(LevelRenderContext ctx) {
        if (broken || SKY == null) return;
        try {
            StardropsConfig c = StardropsConfig.get(); Minecraft mc = Minecraft.getInstance();
            if (!c.cosNight || mc.level == null || !Flicker.on(Flicker.SKY)) return;
            long ticks = mc.level.getOverworldClockTime(); float day = (ticks % 24000L) / 24000f;
            float night = (c.cosOnlyNight ? 1f : CosmosHud.nightness(day) * (1f - mc.level.getRainLevel(1f))) * (c.cosSkyBright / 100f);   // Only night: always there, whatever the world's (or another mod's) clock says
            if (night < 0.02f) return;
            if (sDir == null) make();
            final float Rr = Math.max(96f, mc.options.getEffectiveRenderDistance() * 16f * 2f);
            final long now = System.currentTimeMillis(); final float t = (now % 3_600_000L) / 1000f;
            final boolean cons = c.cosCons, shoot = c.cosShooting, aur = c.cosAurora; final float[][][] figs = cons ? Codex.skyFigures() : null;
            if (shoot) {
                if (nextShoot == 0) nextShoot = now + 3000 + R.nextInt(6000);
                if (now >= nextShoot) { for (int i = 0; i < shoots.length; i++) if (shoots[i] == null || t - shoots[i][0] > 1.2f) { shoots[i] = new float[]{t, R.nextFloat() * 6.28f, 0.35f + R.nextFloat() * 0.8f, R.nextFloat() * 6.28f, 0.25f + R.nextFloat() * 0.3f}; break; } nextShoot = now + 4000 + R.nextInt(14000); }
            }
            PoseStack ps = ctx.poseStack(); ps.pushPose();
            float turn = c.cosOnlyNight ? (now % 5_400_000L) / 5_400_000f : day;                    // Only night: it turns by the real clock (once in 90 minutes), so a mod that changes the time cannot make it jump
            ps.mulPose(new Quaternionf().rotateZ(0.35f).rotateX(turn * 6.2831853f));                // the whole sky turns, on a tilted axis
            final float nightF = night;
            ctx.submitNodeCollector().submitCustomGeometry(ps, SKY, (pose, buf) -> { Q q = (x, y, z, col) -> buf.addVertex(pose, x * Rr, y * Rr, z * Rr).setColor(col); draw(q, t, nightF, figs, shoot, aur); });
            ps.popPose();
        } catch (Throwable e) { broken = true; System.err.println("[Stardrops] sky box failed, using screen stars: " + e); }
    }
    private static void quad(Q q, float cx, float cy, float cz, float ux, float uy, float uz, float vx, float vy, float vz, float s, int col) {
        q.v(cx - (ux + vx) * s, cy - (uy + vy) * s, cz - (uz + vz) * s, col); q.v(cx + (ux - vx) * s, cy + (uy - vy) * s, cz + (uz - vz) * s, col);
        q.v(cx + (ux + vx) * s, cy + (uy + vy) * s, cz + (uz + vz) * s, col); q.v(cx - (ux - vx) * s, cy - (uy - vy) * s, cz - (uz - vz) * s, col);
    }
    /** A soft round glow: a fan of quads, bright in the middle, nothing at the rim. */
    private static void soft(Q q, Vector3f d, Vector3f u, Vector3f v, float r, int rgb, float a, int seg) {
        int ci = argb(a, rgb), co = argb(0, rgb);
        for (int k = 0; k < seg; k++) {
            double a0 = k * Math.PI * 2 / seg, a1 = (k + 1) * Math.PI * 2 / seg; float c0 = (float) Math.cos(a0) * r, s0 = (float) Math.sin(a0) * r, c1 = (float) Math.cos(a1) * r, s1 = (float) Math.sin(a1) * r;
            q.v(d.x, d.y, d.z, ci); q.v(d.x, d.y, d.z, ci); q.v(d.x + u.x * c0 + v.x * s0, d.y + u.y * c0 + v.y * s0, d.z + u.z * c0 + v.z * s0, co); q.v(d.x + u.x * c1 + v.x * s1, d.y + u.y * c1 + v.y * s1, d.z + u.z * c1 + v.z * s1, co);
        }
    }
    private static void line(Q q, Vector3f a, Vector3f b, float w, int ca, int cb) {
        Vector3f side = new Vector3f(b).sub(a).cross(a).normalize().mul(w);
        q.v(a.x + side.x, a.y + side.y, a.z + side.z, ca); q.v(b.x + side.x, b.y + side.y, b.z + side.z, cb); q.v(b.x - side.x, b.y - side.y, b.z - side.z, cb); q.v(a.x - side.x, a.y - side.y, a.z - side.z, ca);
    }
    private static Vector3f at(double az, double alt) { return new Vector3f((float) (Math.cos(alt) * Math.cos(az)), (float) Math.sin(alt), (float) (Math.cos(alt) * Math.sin(az))); }
    /** Where each constellation hangs (azimuth, altitude in degrees) and how big it is drawn (radians per sky unit). */
    private static final float[][] CON_AT = {{0, 62}, {70, 30}, {140, 38}, {205, 26}, {255, 46}, {310, 34}, {30, -20}, {180, -28}};
    private static final float CON_K = 1.9f;

    static void draw(Q q, float t, float night, float[][][] figs, boolean shoot, boolean aur) {
        Vector3f d = new Vector3f(), u = new Vector3f(), v = new Vector3f();
        for (float[] h : haze) { d.set(h[0], h[1], h[2]); basis(d, u, v); soft(q, d, u, v, h[3], (int) h[4], h[5] * night, 10); }          // the Milky Way's haze
        for (int i = 0; i < N; i++) {                                                                 // the stars
            float tw = sSize[i] > 0.0036f ? 0.72f + 0.28f * (float) Math.sin(t * (1.3f + (i % 7) * 0.35f) + sPh[i]) : 1f, s = sSize[i];
            float b = Math.min(1f, 0.35f + (s - 0.0016f) / 0.0042f * 1.1f) * night * tw;
            quad(q, sDir[i * 3], sDir[i * 3 + 1], sDir[i * 3 + 2], sU[i * 3], sU[i * 3 + 1], sU[i * 3 + 2], sV[i * 3], sV[i * 3 + 1], sV[i * 3 + 2], s, argb(b, sCol[i]));
            if (s > 0.0042f) {                                                                         // the bright ones: a halo and a four-point glint
                d.set(sDir[i * 3], sDir[i * 3 + 1], sDir[i * 3 + 2]); u.set(sU[i * 3], sU[i * 3 + 1], sU[i * 3 + 2]); v.set(sV[i * 3], sV[i * 3 + 1], sV[i * 3 + 2]);
                soft(q, d, u, v, s * 4.5f, sCol[i], 0.3f * night * tw, 8);
                int c1 = argb(0.75f * night * tw, sCol[i]), c0 = argb(0, sCol[i]); float L = s * 5.5f, w = s * 0.45f;
                for (int k = 0; k < 2; k++) { Vector3f a = k == 0 ? u : v, o = k == 0 ? v : u;
                    q.v(d.x - a.x * L, d.y - a.y * L, d.z - a.z * L, c0); q.v(d.x + o.x * w, d.y + o.y * w, d.z + o.z * w, c1); q.v(d.x + a.x * L, d.y + a.y * L, d.z + a.z * L, c0); q.v(d.x - o.x * w, d.y - o.y * w, d.z - o.z * w, c1); }
            }
        }
        planet(q, at(0.9, 0.62), 0.07f, 15, 0xE8B07A, 0x9A5A48, true, -0.4f, night);                   // a ringed giant and a small red world
        planet(q, at(3.9, 0.35), 0.028f, 9, 0xE87A5A, 0x8A3A2A, false, 0f, night);
        if (figs != null) for (int c = 0; c < figs.length && c < CON_AT.length; c++) {                 // your constellations
            float[][] f = figs[c]; if (f == null || f.length == 0) continue;
            Vector3f ctr = at(Math.toRadians(CON_AT[c][0]), Math.toRadians(CON_AT[c][1])); Vector3f cu = new Vector3f(), cv = new Vector3f(); basis(ctr, cu, cv);
            int col = (int) f[0][4]; Vector3f[] p = new Vector3f[f.length];
            for (int i = 0; i < f.length; i++) p[i] = new Vector3f(ctr).add(new Vector3f(cu).mul(-f[i][0] * CON_K)).add(new Vector3f(cv).mul(f[i][1] * CON_K)).normalize();
            for (int i = 0; i < f.length; i++) { int par = (int) f[i][2]; if (par < 0 || par >= f.length) continue; boolean lit = f[i][3] > 0 && f[par][3] > 0;
                float a = (lit ? 0.9f : 0.16f) * night; int lc = argb(a, lit ? Ritual.mixc(col, 0xFFFFFF, 0.25f) : 0x8A94B8); line(q, p[par], p[i], lit ? 0.0028f : 0.0014f, lc, lc);
                if (lit) { int gl = argb(0.22f * night, col); line(q, p[par], p[i], 0.009f, gl, gl); } }                                    // a lit line glows
            for (int i = 0; i < f.length; i++) { boolean lit = f[i][3] > 0; basis(p[i], u, v);
                if (lit) { float pu = 0.85f + 0.15f * (float) Math.sin(t * 2f + i + c); soft(q, p[i], u, v, 0.055f * pu, col, 0.6f * night, 12); soft(q, p[i], u, v, 0.02f, 0xFFFFFF, 0.8f * night, 8); quad(q, p[i].x, p[i].y, p[i].z, u.x, u.y, u.z, v.x, v.y, v.z, 0.0085f, argb(night, 0xFFFFFF));
                    int g1 = argb(0.9f * night * pu, Ritual.mixc(col, 0xFFFFFF, 0.5f)), g0 = argb(0, col); float L = 0.045f * pu, w = 0.003f;                                    // a four-point glint in its colour
                    for (int k = 0; k < 2; k++) { Vector3f aa = k == 0 ? u : v, o = k == 0 ? v : u; Vector3f pt = p[i];
                        q.v(pt.x - aa.x * L, pt.y - aa.y * L, pt.z - aa.z * L, g0); q.v(pt.x + o.x * w, pt.y + o.y * w, pt.z + o.z * w, g1); q.v(pt.x + aa.x * L, pt.y + aa.y * L, pt.z + aa.z * L, g0); q.v(pt.x - o.x * w, pt.y - o.y * w, pt.z - o.z * w, g1); } }
                else quad(q, p[i].x, p[i].y, p[i].z, u.x, u.y, u.z, v.x, v.y, v.z, 0.0038f, argb(0.6f * night, 0x9AA4C8)); }
        }
        if (shoot) for (float[] s : shoots) {                                                          // shooting stars
            if (s == null) continue; float k = (t - s[0]) / 0.8f; if (k < 0 || k > 1) continue;
            Vector3f a = at(s[1], s[2]); basis(a, u, v); Vector3f dir = new Vector3f(u).mul((float) Math.cos(s[3])).add(new Vector3f(v).mul((float) Math.sin(s[3])));
            Vector3f head = new Vector3f(a).add(new Vector3f(dir).mul(s[4] * k)).normalize(), tail = new Vector3f(a).add(new Vector3f(dir).mul(s[4] * Math.max(0f, k - 0.3f))).normalize();
            float f = (float) Math.sin(Math.PI * k) * night; line(q, tail, head, 0.0022f, argb(0, 0xC8B4FF), argb(f, 0xFFFFFF)); basis(head, u, v); soft(q, head, u, v, 0.012f, 0xFFFFFF, f, 8);
        }
        if (aur) for (int b = 0; b < 2; b++) for (int k = 0; k < 48; k++) {                             // aurora: curtains low in the north, swaying
            double az0 = Math.toRadians(-70 + k * 2.9), az1 = Math.toRadians(-70 + (k + 1) * 2.9);
            float lo0 = 0.2f + 0.1f * b + 0.05f * (float) Math.sin(k * 0.4f + t * 0.3f + b * 2), lo1 = 0.2f + 0.1f * b + 0.05f * (float) Math.sin((k + 1) * 0.4f + t * 0.3f + b * 2);
            float h0 = 0.34f + 0.12f * (float) Math.sin(k * 0.9f + t * 0.5f + b), h1 = 0.34f + 0.12f * (float) Math.sin((k + 1) * 0.9f + t * 0.5f + b);
            float e0 = (float) Math.sin(Math.PI * k / 48.0), e1 = (float) Math.sin(Math.PI * (k + 1) / 48.0);
            int col = Ritual.mixc(0x5AF0B0, 0x9B7BFF, 0.5f + 0.5f * (float) Math.sin(k * 0.3f + t * 0.2f + b * 1.5f));
            Vector3f p0 = at(az0, lo0), p1 = at(az1, lo1), q0 = at(az0, lo0 + h0), q1 = at(az1, lo1 + h1); float a0 = 0.22f * night * e0 * (0.7f + 0.3f * (float) Math.sin(k * 1.7f + t * 1.3f)), a1 = 0.22f * night * e1 * (0.7f + 0.3f * (float) Math.sin((k + 1) * 1.7f + t * 1.3f));
            q.v(p0.x, p0.y, p0.z, argb(a0, col)); q.v(p1.x, p1.y, p1.z, argb(a1, col)); q.v(q1.x, q1.y, q1.z, argb(0, col)); q.v(q0.x, q0.y, q0.z, argb(0, col));
        }
    }
    /** A pixel planet: lit from one side (its dark side adds nothing, so it shows a phase), with a ring if it has one. */
    private static void planet(Q q, Vector3f ctr, float rad, int n, int ca, int cb, boolean ring, float tilt, float night) {
        Vector3f u = new Vector3f(), v = new Vector3f(); basis(ctr, u, v); float px = rad * 2f / n, cs = (float) Math.cos(tilt), sn = (float) Math.sin(tilt);
        int W = ring ? Math.round(n * 1.0f) : n / 2 + 1, H = n / 2 + 1;
        soft(q, ctr, u, v, rad * 2.6f, ca, 0.12f * night, 12);
        for (int j = -H; j <= H; j++) for (int i = -W; i <= W; i++) {
            float x = i / (n / 2f), y = j / (n / 2f), d2 = x * x + y * y, rx = x * cs - y * sn, ry = (x * sn + y * cs) / 0.26f, rr = (float) Math.sqrt(rx * rx + ry * ry);
            boolean rg = ring && rr > 1.32f && rr < 1.85f && !(rr > 1.55f && rr < 1.62f), front = ry < 0f, in = d2 <= 1f; int col; float a;
            if (in && !(rg && front)) { float lit = Math.max(0f, -0.62f * x + 0.42f * y + 0.66f * (float) Math.sqrt(Math.max(0f, 1f - d2))), band = (float) Math.sin((y * 0.94f + x * 0.34f) * 7.5f + 0.6f) * 0.5f + 0.5f;
                col = Ritual.mixc(ca, cb, band * 0.75f); a = Math.min(1f, 0.05f + lit * 1.05f); }
            else if (rg) { col = Ritual.mixc(ca, 0xFFFFFF, rr > 1.6f ? 0.35f : 0.15f); a = 0.6f * (front ? 1f : 0.7f) * (!front && x > 0.1f ? 0.5f : 1f); }
            else continue;
            float ox = i * px, oy = j * px;
            quad(q, ctr.x + u.x * ox + v.x * oy, ctr.y + u.y * ox + v.y * oy, ctr.z + u.z * ox + v.z * oy, u.x, u.y, u.z, v.x, v.y, v.z, px * 0.5f, argb(a * night, col));
        }
    }
}
