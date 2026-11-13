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
        if (!Float.isFinite(d.x + d.y + d.z) || d.lengthSquared() < 1e-12f) { u.set(1, 0, 0); v.set(0, 0, 1); return; }
        Vector3f a = Math.abs(d.y) < 0.9f ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0);
        d.cross(a, u).normalize(); d.cross(u, v).normalize();
    }
    static void make() {
        N = 1100; sDir = new float[N * 3]; sU = new float[N * 3]; sV = new float[N * 3]; sSize = new float[N]; sPh = new float[N]; sCol = new int[N];
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
        double[][] nc = {{70, 40}, {250, 25}, {160, 70}}; int[][] ncol = {{0xFF5AA8, 0xB06AFF, 0xFF9A6A}, {0x3AD8C8, 0x5A8AFF, 0x9AE8FF}, {0xFFB04A, 0xFF5A7A, 0xC86AFF}};
        nebula = new float[nc.length * 14][];
        for (int n = 0; n < nc.length; n++) { Vector3f ctr = at(Math.toRadians(nc[n][0]), Math.toRadians(nc[n][1])); Vector3f u = new Vector3f(), v = new Vector3f(); basis(ctr, u, v);
            for (int i = 0; i < 14; i++) { int sd = n * 97 + i * 13; float ox = (rnd(sd) - 0.5f) * 0.16f, oy = (rnd(sd + 1) - 0.5f) * 0.1f;
                Vector3f d = new Vector3f(ctr).add(new Vector3f(u).mul(ox)).add(new Vector3f(v).mul(oy)).normalize();
                nebula[n * 14 + i] = new float[]{d.x, d.y, d.z, 0.035f + 0.07f * rnd(sd + 2), ncol[n][i % 3], 0.09f + 0.1f * rnd(sd + 3)}; } }
    }

    // ------------------------------------------------------------------ drawing
    interface Q { void v(float x, float y, float z, int argb); }
    private static int argb(float a, int rgb) { return Math.round(Math.max(0f, Math.min(1f, a)) * 255) << 24 | (rgb & 0xFFFFFF); }
    private static double turnD = -1; private static long lastTurnAt;
    /** How far the sky has turned right now (0..1), for the menu's picture of it. */
    static float turnNow() { return turnD < 0 ? 0.6f : (float) turnD; }
    /** Builds the stars if they are not built yet (the menu's picture may need them before the world draws the sky). */
    static void make0() { if (sDir == null) make(); }
    private static long nextShoot; private static final float[][] shoots = new float[3][]; private static final java.util.Random R = new java.util.Random();

    static void submit(LevelRenderContext ctx) {
        if (broken || SKY == null) return;
        try {
            StardropsConfig c = StardropsConfig.get(); Minecraft mc = Minecraft.getInstance();
            if (!c.cosNight || mc.level == null || !Flicker.on(Flicker.SKY)) return;
            long ticks = mc.level.getOverworldClockTime(); float day = (ticks % 24000L) / 24000f;
            long nowMs = System.currentTimeMillis();
            day = steadyDay(day, nowMs);                                                         // a clock that jumps back and forth is not believed until it stays
            float rawNight = (c.cosOnlyNight ? 1f : CosmosHud.nightness(day) * (1f - mc.level.getRainLevel(1f))) * (c.cosSkyBright / 100f);   // Only night: always there, whatever the world's (or another mod's) clock says
            double dtN = lastNightAt == 0 ? 1 : Math.min(0.25, (nowMs - lastNightAt) / 1000.0); lastNightAt = nowMs;
            nightE = lastNightAt == 0 || nightE < 0 ? rawNight : nightE + (float) ((rawNight - nightE) * Math.min(1.0, dtN / 1.5));   // and the stars fade in and out over a second and a half, never in one frame
            float night = nightE;
            if (night < 0.02f) return;
            if (sDir == null) make();
            milky = c.cosMilky; planets = c.cosPlanets; deep = c.cosDeep; comet = c.cosComet;
            minS = 0.75f * (float) Math.toRadians(mc.options.fov().get()) / Math.max(240, mc.getWindow().getHeight());   // one and a half pixels across
            final float Rr = Math.max(96f, mc.options.getEffectiveRenderDistance() * 16f * 2f);
            final long now = System.currentTimeMillis(); final float t = (now % 3_600_000L) / 1000f;
            final boolean cons = c.cosCons, shoot = c.cosShooting, aur = c.cosAurora; final float[][][] figs = cons ? Codex.skyFigures() : null;
            if (shoot) {
                if (nextShoot == 0) nextShoot = now + 3000 + R.nextInt(6000);
                if (now >= nextShoot) { for (int i = 0; i < shoots.length; i++) if (shoots[i] == null || t - shoots[i][0] > 1.2f) { shoots[i] = new float[]{t, R.nextFloat() * 6.28f, 0.35f + R.nextFloat() * 0.8f, R.nextFloat() * 6.28f, 0.25f + R.nextFloat() * 0.3f}; break; } nextShoot = now + 4000 + R.nextInt(14000); }
            }
            PoseStack ps = ctx.poseStack(); ps.pushPose();
            float want = c.cosOnlyNight ? (now % 5_400_000L) / 5_400_000f : day;                    // Only night: it turns by the real clock (once in 90 minutes), so a mod that changes the time cannot make it jump
            // The sky never follows the game's clock directly. On Hypixel the clock is often stopped and set by the server about once a
            // second, so it moves in jumps (and sometimes back): following it made the sky step and twitch. Instead the sky turns at the
            // steady speed of a Minecraft day (20 minutes) on its own and is only nudged, slowly, toward the game's time; it never turns
            // backwards, and only a big jump (/time set, a new world) is taken at once.
            double dtS = lastTurnAt == 0 ? 0 : Math.min(0.25, (now - lastTurnAt) / 1000.0); lastTurnAt = now;
            double diff = want - turnD; diff -= Math.round(diff);
            if (turnD < 0 || Math.abs(diff) > 0.02) turnD = want;
            else { double rate = c.cosOnlyNight ? 1.0 / 5400 : 1.0 / 1200; turnD += Math.max(0, Math.min(3 * rate, rate + diff / 8.0)) * dtS; }
            turnD -= Math.floor(turnD); float turn = (float) turnD; drawTurn = turn;
            ps.mulPose(rot(turn));                                                                      // the whole sky turns round the zenith (a hair tilted): what hangs above stays above all night
            final float nightF = night;
            ctx.submitNodeCollector().submitCustomGeometry(ps, SKY, (pose, buf) -> { Q q = guard((x, y, z, col) -> buf.addVertex(pose, x * Rr, y * Rr, z * Rr).setColor(col)); draw(q, t, nightF, figs, shoot, aur); });
            ps.popPose();
        } catch (Throwable e) { broken = true; System.err.println("[Stardrops] sky box failed, using screen stars: " + e); }
    }
    /** The smallest half-size, in sky units, that still covers about one and a half screen pixels (0 on the bench). */
    /** The sky's turn: round the point straight overhead, tilted a hair. Turning on a sideways axis carried the constellations and the
     *  planets under the horizon for half of every night (you had to look into the void to see them); turning round the zenith keeps
     *  everything at its height, so whatever hangs above the horizon stays up there. */
    static final float TILT = 0.1f;
    static Quaternionf rot(float turn) { return new Quaternionf().rotateZ(TILT).rotateY(turn * 6.2831853f); }
    /**
     * The day as the sky believes it. Some servers and mods fight over the client's clock (the author's video, 13 Nov: day and night
     * swapped on every other frame), so a jump of the clock is only taken once it has held for 1.5 s; until then the last steady
     * time goes on. The stars must never follow the clock frame by frame (standing rule: no light flickering).
     */
    private static float steadyDay(float day, long now) {
        if (dayHeld < 0) { dayHeld = day; return day; }
        float d = day - dayHeld; d -= Math.round(d);
        if (Math.abs(d) < 0.02f) { dayHeld = day; dayPendAt = 0; return day; }
        float p = day - dayPend; p -= Math.round(p);
        if (dayPendAt == 0 || Math.abs(p) > 0.02f) { dayPend = day; dayPendAt = now; return dayHeld; }
        if (now - dayPendAt >= 1500) { dayHeld = day; dayPendAt = 0; return day; }
        return dayHeld;
    }
    private static float dayHeld = -1, dayPend, nightE = -1; private static long dayPendAt, lastNightAt;
    static float minS; static boolean milky = true, planets = true, deep = true, comet = true;
    /** The turn the sky is drawn at (the aurora is held still against it). */
    static float drawTurn;
    private static float[][] nebula;
    /** A star or line smaller than that is drawn at that size and dimmed to the same total light, so it never blinks in and out. */
    private static int fit(float s, int col) { if (s >= minS) return col; float k = s / minS; return Math.round((col >>> 24) * k * k) << 24 | (col & 0xFFFFFF); }
    private static void quad(Q q, float cx, float cy, float cz, float ux, float uy, float uz, float vx, float vy, float vz, float s, int col) {
        if (s < minS) { col = fit(s, col); s = minS; }
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
    /**
     * Passes on whole quads only, and only when all four corners are real numbers. A corner that is not (a line between two stars in
     * the same spot gives a direction of length 0, which normalises to NaN) makes the graphics card draw a huge triangle across the
     * screen for a frame: the sky flashes white. Such a quad is dropped.
     */
    static Q guard(Q out) {
        float[] b = new float[12]; int[] c = new int[4]; int[] n = {0};
        return (x, y, z, col) -> { int i = n[0]; b[i * 3] = x; b[i * 3 + 1] = y; b[i * 3 + 2] = z; c[i] = col; n[0] = i + 1;
            if (i < 3) return; n[0] = 0;
            for (float v : b) if (!Float.isFinite(v) || Math.abs(v) > 4f) return;                   // every sky point is on the unit sphere (a little off for glints)
            for (int k = 0; k < 4; k++) out.v(b[k * 3], b[k * 3 + 1], b[k * 3 + 2], c[k]); };
    }
    private static void line(Q q, Vector3f a, Vector3f b, float w, int ca, int cb) {
        if (a.distanceSquared(b) < 1e-10f) return;                                                    // two stars in the same spot: no line
        if (w < minS * 0.8f) { float m = minS * 0.8f, k = w / m; ca = Math.round((ca >>> 24) * k) << 24 | (ca & 0xFFFFFF); cb = Math.round((cb >>> 24) * k) << 24 | (cb & 0xFFFFFF); w = m; }
        Vector3f side = new Vector3f(b).sub(a).cross(a); if (side.lengthSquared() < 1e-14f) return; side.normalize().mul(w);
        q.v(a.x + side.x, a.y + side.y, a.z + side.z, ca); q.v(b.x + side.x, b.y + side.y, b.z + side.z, cb); q.v(b.x - side.x, b.y - side.y, b.z - side.z, cb); q.v(a.x - side.x, a.y - side.y, a.z - side.z, ca);
    }
    private static Vector3f at(double az, double alt) { return new Vector3f((float) (Math.cos(alt) * Math.cos(az)), (float) Math.sin(alt), (float) (Math.cos(alt) * Math.sin(az))); }
    /** Where each constellation hangs (azimuth, altitude in degrees) and how big it is drawn (radians per sky unit). */
    private static final float[][] CON_AT = {{0, 58}, {45, 34}, {95, 46}, {150, 36}, {200, 52}, {250, 34}, {300, 44}, {340, 74}};
    private static final float CON_K = 1.9f;

    static void draw(Q q, float t, float night, float[][][] figs, boolean shoot, boolean aur) {
        Vector3f d = new Vector3f(), u = new Vector3f(), v = new Vector3f();
        if (milky) for (float[] h : haze) { d.set(h[0], h[1], h[2]); basis(d, u, v); soft(q, d, u, v, h[3], (int) h[4], h[5] * night, 8); }          // the Milky Way's haze
        for (int i = 0; i < N; i++) {                                                                 // the stars
            float tw = sSize[i] > 0.0036f ? 0.72f + 0.28f * (float) Math.sin(t * (1.3f + (i % 7) * 0.35f) + sPh[i]) : 1f, s = sSize[i];
            float b = Math.min(1f, 0.35f + (s - 0.0016f) / 0.0042f * 1.1f) * night * tw;
            quad(q, sDir[i * 3], sDir[i * 3 + 1], sDir[i * 3 + 2], sU[i * 3], sU[i * 3 + 1], sU[i * 3 + 2], sV[i * 3], sV[i * 3 + 1], sV[i * 3 + 2], s, argb(b, sCol[i]));
            if (s > 0.0042f) {                                                                         // the bright ones: a halo and a four-point glint
                d.set(sDir[i * 3], sDir[i * 3 + 1], sDir[i * 3 + 2]); u.set(sU[i * 3], sU[i * 3 + 1], sU[i * 3 + 2]); v.set(sV[i * 3], sV[i * 3 + 1], sV[i * 3 + 2]);
                soft(q, d, u, v, s * 4.5f, sCol[i], 0.3f * night * tw, 6);
                int c1 = argb(0.75f * night * tw, sCol[i]), c0 = argb(0, sCol[i]); float L = s * 5.5f, w = s * 0.45f;
                for (int k = 0; k < 2; k++) { Vector3f a = k == 0 ? u : v, o = k == 0 ? v : u;
                    q.v(d.x - a.x * L, d.y - a.y * L, d.z - a.z * L, c0); q.v(d.x + o.x * w, d.y + o.y * w, d.z + o.z * w, c1); q.v(d.x + a.x * L, d.y + a.y * L, d.z + a.z * L, c0); q.v(d.x - o.x * w, d.y - o.y * w, d.z - o.z * w, c1); }
            }
        }
        if (planets) { planet(q, at(0.9, 0.78), 0.07f, 15, 0xE8B07A, 0x9A5A48, true, -0.4f, night);    // a ringed giant and a small red world
        planet(q, at(3.9, 0.6), 0.028f, 9, 0xE87A5A, 0x8A3A2A, false, 0f, night); }
        if (figs != null) for (int c = 0; c < figs.length && c < CON_AT.length; c++) {                 // your constellations
            float[][] f = figs[c]; if (f == null || f.length == 0) continue;
            Vector3f ctr = at(Math.toRadians(CON_AT[c][0]), Math.toRadians(CON_AT[c][1])); Vector3f cu = new Vector3f(), cv = new Vector3f(); basis(ctr, cu, cv);
            int col = (int) f[0][4]; Vector3f[] p = new Vector3f[f.length];
            for (int i = 0; i < f.length; i++) p[i] = new Vector3f(ctr).add(new Vector3f(cu).mul(-f[i][0] * CON_K)).add(new Vector3f(cv).mul(f[i][1] * CON_K)).normalize();
            for (int i = 0; i < f.length; i++) { int par = (int) f[i][2]; if (par < 0 || par >= f.length) continue; boolean lit = f[i][3] > 0 && f[par][3] > 0;
                float a = (lit ? 0.9f : 0.32f) * night; int lc = argb(a, lit ? Ritual.mixc(col, 0xFFFFFF, 0.25f) : 0xA8B4E0); line(q, p[par], p[i], lit ? 0.0028f : 0.0018f, lc, lc);
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
        if (deep) {                                                                                     // deep sky: nebulae, and a spiral galaxy far away
            for (float[] nb : nebula) { d.set(nb[0], nb[1], nb[2]); basis(d, u, v); soft(q, d, u, v, nb[3], (int) nb[4], nb[5] * night, 10); }
            Vector3f gc = at(Math.toRadians(205), Math.toRadians(58)); basis(gc, u, v); float tl = 0.5f, cs = (float) Math.cos(tl), sn = (float) Math.sin(tl);
            soft(q, gc, u, v, 0.06f, 0x8AA0FF, 0.16f * night, 12); soft(q, gc, u, v, 0.018f, 0xFFF0C8, 0.55f * night, 10);
            for (int arm = 0; arm < 2; arm++) for (int i = 0; i < 44; i++) {
                float an = i * 0.2f + arm * 3.1416f + t * 0.01f, r = 0.005f + i * 0.00095f, x = r * (float) Math.cos(an), y = r * 0.42f * (float) Math.sin(an), rx = x * cs - y * sn, ry = x * sn + y * cs;
                Vector3f pt = new Vector3f(gc).add(new Vector3f(u).mul(rx)).add(new Vector3f(v).mul(ry));
                quad(q, pt.x, pt.y, pt.z, u.x, u.y, u.z, v.x, v.y, v.z, 0.0016f + 0.0012f * (1f - i / 44f), argb(night * (0.75f - 0.5f * i / 44f), i % 4 == 0 ? 0xFFC8F0 : 0xC8D8FF));
            }
        }
        if (comet) {                                                                                    // a comet: every eight minutes it crosses part of the sky, slowly, in about 80 seconds
            long ms = System.currentTimeMillis() % 480_000L; float k = ms / 80_000f;
            if (k < 1f) {
                Vector3f a0 = at(Math.toRadians(40), Math.toRadians(30)), a1 = at(Math.toRadians(140), Math.toRadians(62));
                Vector3f head = new Vector3f(a0).lerp(a1, k).normalize(), back = new Vector3f(a0).sub(a1).normalize(); float f = (float) Math.sin(Math.PI * k) * night;
                basis(head, u, v); soft(q, head, u, v, 0.03f, 0x9AE8FF, 0.35f * f, 10); soft(q, head, u, v, 0.009f, 0xFFFFFF, 0.95f * f, 8);
                for (int i = 1; i <= 14; i++) { float s = i / 14f;                                           // the dust tail curves a little, the ion tail runs straight and blue
                    Vector3f pd = new Vector3f(head).add(new Vector3f(back).mul(0.3f * s)).add(new Vector3f(u).mul(0.05f * s * s)).normalize(); basis(pd, u, v);
                    soft(q, pd, u, v, 0.012f + 0.03f * s, 0xFFE8B8, 0.22f * f * (1f - s), 8); basis(head, u, v); }
                line(q, head, new Vector3f(head).add(new Vector3f(back).mul(0.42f)).normalize(), 0.004f, argb(0.6f * f, 0x8AC8FF), argb(0, 0x6A9AFF));
            }
        }
        if (aur) {                                                                                      // aurora: curtains of light low over the northern horizon, held still while the stars turn
            org.joml.Matrix3f fix = new org.joml.Matrix3f().rotateY((float) (-drawTurn * Math.PI * 2)).rotateZ(-TILT);
            Vector3f p0 = new Vector3f(), p1 = new Vector3f(), m0 = new Vector3f(), m1 = new Vector3f(), q0 = new Vector3f(), q1 = new Vector3f();
            for (int b = 0; b < 2; b++) for (int k = 0; k < 64; k++) {
                double az0 = Math.toRadians(-60 + k * 1.9 + b * 6), az1 = Math.toRadians(-60 + (k + 1) * 1.9 + b * 6);
                float w0 = 0.04f * (float) Math.sin(k * 0.35f + t * 0.25f + b * 2), w1 = 0.04f * (float) Math.sin((k + 1) * 0.35f + t * 0.25f + b * 2);
                float lo0 = 0.06f + 0.05f * b + w0, lo1 = 0.06f + 0.05f * b + w1;
                float h0 = 0.2f + 0.09f * (float) Math.sin(k * 0.7f + t * 0.4f + b), h1 = 0.2f + 0.09f * (float) Math.sin((k + 1) * 0.7f + t * 0.4f + b);
                float e0 = (float) Math.sin(Math.PI * k / 64.0), e1 = (float) Math.sin(Math.PI * (k + 1) / 64.0);
                float r0 = 0.45f + 0.55f * sq((float) Math.sin(k * 1.9f + t * 0.6f + b * 3)), r1 = 0.45f + 0.55f * sq((float) Math.sin((k + 1) * 1.9f + t * 0.6f + b * 3));   // light rays
                float a0 = 0.3f * night * e0 * r0 * (b == 0 ? 1f : 0.6f), a1 = 0.3f * night * e1 * r1 * (b == 0 ? 1f : 0.6f);
                fix.transform(p0.set(at(az0, lo0))); fix.transform(p1.set(at(az1, lo1))); fix.transform(m0.set(at(az0, lo0 + h0 * 0.35f))); fix.transform(m1.set(at(az1, lo1 + h1 * 0.35f)));
                fix.transform(q0.set(at(az0, lo0 + h0))); fix.transform(q1.set(at(az1, lo1 + h1)));
                int gr = 0x4AF0A0, mid = 0x5AD8C8, top = 0xA06AFF;
                q.v(p0.x, p0.y, p0.z, argb(a0 * 0.6f, gr)); q.v(p1.x, p1.y, p1.z, argb(a1 * 0.6f, gr)); q.v(m1.x, m1.y, m1.z, argb(a1, mid)); q.v(m0.x, m0.y, m0.z, argb(a0, mid));
                q.v(m0.x, m0.y, m0.z, argb(a0, mid)); q.v(m1.x, m1.y, m1.z, argb(a1, mid)); q.v(q1.x, q1.y, q1.z, argb(0, top)); q.v(q0.x, q0.y, q0.z, argb(0, top));
            }
        }
    }
    private static float sq(float x) { return x * x; }
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
