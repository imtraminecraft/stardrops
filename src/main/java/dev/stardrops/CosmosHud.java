package dev.stardrops;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Visuals > Cosmos: the sky around you while you play.
 *
 *   Night sky: a real sky box in the world (SkyBox): stars, the Milky Way, planets, your constellations. If the sky box cannot be drawn,
 *              a few faint stars twinkle in the top corners of the screen instead.
 *   Shooting stars and Aurora: in the sky box; over the top of the screen if the sky box is off or unavailable.
 *   Moments: the moment a drop lands, one meteor burns across the screen; the moment an RNG drop lands, a whole meteor shower does.
 * The instruments (the celestial clock and the others) live in InstrumentsHud.
 */
final class CosmosHud {
    private CosmosHud() {}

    private static long nextShoot, shootAt = -99999, meteorAt = -99999; private static float shootY, shootDir, away; private static long lastFrame; private static boolean showerNow; private static int meteorSeed;
    private static final java.util.Random R = new java.util.Random();

    /** A drop just landed: one meteor for a drop, a shower for an RNG drop. */
    static void meteor(boolean rng) { if (StardropsConfig.get().cosMeteor) { long now = System.currentTimeMillis(); if (!rng && showerNow && now - meteorAt < SHOWER_MS) return; meteorAt = now; showerNow = rng; meteorSeed = R.nextInt(1000); } }
    private static final int ONE_MS = 1500, SHOWER_MS = 3400;

    static void render(GuiGraphicsExtractor g) {
        StardropsConfig c = StardropsConfig.get(); Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = System.currentTimeMillis(); float dt = Math.min(0.1f, (now - (lastFrame == 0 ? now : lastFrame)) / 1000f); lastFrame = now;
        away += ((AnimationManager.isPlaying() ? 1f : 0f) - away) * Math.min(1f, dt * 6f);
        int W = g.guiWidth(), H = g.guiHeight();
        if (now - meteorAt < (showerNow ? SHOWER_MS : ONE_MS)) { if (showerNow) shower(g, W, H, (now - meteorAt) / 1000f); else oneMeteor(g, W, H, (now - meteorAt) / (float) ONE_MS); }
        if (SkyBox.on() || !(c.cosShooting || c.cosNight || c.cosAurora)) return;                 // the sky box draws the sky in the world; what follows is only its stand-in
        long ticks = mc.level.getOverworldClockTime(); float day = (ticks % 24000L) / 24000f;
        boolean dark = mc.level.isDarkOutside(); float night = nightness(day);
        float vis = 1f - away; if (vis < 0.02f) return;
        if (c.cosAurora && night > 0.05f) aurora(g, W, H, now, night * vis);
        if (c.cosNight && night > 0.05f) corners(g, W, H, now, night * vis);
        if (c.cosShooting && (!c.cosShootNight || dark)) {
            if (nextShoot == 0) nextShoot = now + 8000 + R.nextInt(20000);
            if (now >= nextShoot) { shootAt = now; shootY = 0.05f + R.nextFloat() * 0.25f; shootDir = R.nextBoolean() ? 1f : -1f; nextShoot = now + 15000 + R.nextInt(40000); }
            if (now - shootAt < 900) shooting(g, W, H, (now - shootAt) / 900f, vis);
        }
    }
    // ------------------------------------------------------------------ moments
    private static final int[][] ROCK = {{0, 1, 1, 1, 0}, {1, 2, 1, 3, 1}, {1, 1, 3, 1, 1}, {1, 3, 1, 2, 1}, {0, 1, 1, 1, 0}};
    /** One burning meteorite at u (0 entering .. 1 gone) flying from (x0, y0) to (x1, y1): rock, white-hot core, flame, smoke and sparks. */
    private static void burn(GuiGraphicsExtractor g, float x0, float y0, float x1, float y1, float u, float k, int hot, int flame, int seed, float fade) {
        if (u <= 0 || u >= 1 || fade < 0.01f) return;
        float dx = x1 - x0, dy = y1 - y0, len = (float) Math.sqrt(dx * dx + dy * dy), nx = dx / len, ny = dy / len, hx = x0 + dx * u, hy = y0 + dy * u, t = (System.currentTimeMillis() % 100000L) / 1000f;
        float a = Math.min(1f, u * 8f) * Math.min(1f, (1f - u) * 5f) * fade, tail = Math.min(len * u, 170f * k);
        for (int i = 26; i >= 1; i--) {                                                                // smoke, then flame: far to near
            float q = i / 26f, px = hx - nx * tail * q, py = hy - ny * tail * q, wob = (float) Math.sin(t * 22 + i * 1.3f + seed) * 2.2f * k * q;
            if (i % 2 == 0) NebulaTex.glow(g, px - ny * wob, py + nx * wob - q * 5f * k, (4f + 9f * q) * k, (4f + 9f * q) * k, Gfx.argb(Math.round(70 * a * (1f - q)), 0x1A1418));
        }
        for (int i = 22; i >= 0; i--) {
            float q = i / 22f, px = hx - nx * tail * q * 0.85f, py = hy - ny * tail * q * 0.85f, wob = (float) Math.sin(t * 31 + i * 1.7f + seed) * 1.6f * k * q, r = (4f + 13f * (1f - q) * (1f - q)) * k;
            int col = q < 0.18f ? Ritual.mixc(0xFFFFFF, hot, q / 0.18f) : Ritual.mixc(hot, flame, Math.min(1f, (q - 0.18f) / 0.5f));
            NebulaTex.glow(g, px - ny * wob, py + nx * wob, r * 1.5f, r * 1.5f, Gfx.argb(Math.round(200 * a * (1f - q * 0.85f)), col)); if (i < 12) NebulaTex.glow(g, px, py, r * 0.7f, r * 0.7f, Gfx.argb(Math.round(220 * a * (1f - q)), 0xFFFFFF));
        }
        for (int i = 0; i < 46; i++) { float q = i / 46f, px = hx - nx * tail * q * 0.7f, py = hy - ny * tail * q * 0.7f; int w = Math.max(1, Math.round(3.6f * k * (1f - q)));      // the white-hot line down the middle
            g.fill(Math.round(px) - w / 2, Math.round(py) - w / 2, Math.round(px) - w / 2 + w, Math.round(py) - w / 2 + w, Gfx.argb(Math.round(235 * a * (1f - q)), q < 0.35f ? 0xFFFFFF : hot)); }
        for (int i = 0; i < 14; i++) {                                                                // sparks shed behind it, falling
            float life = ((u * 5f + rnd(seed * 7 + i * 13)) % 1f), back = (0.1f + 0.9f * rnd(seed + i * 5)) * tail * 0.8f, sp = (rnd(seed * 3 + i * 11) - 0.5f) * 26f * k * life;
            float px = hx - nx * back - ny * sp, py = hy - ny * back + nx * sp + life * life * 16f * k; int w = life < 0.5f && i % 3 == 0 ? 2 : 1;
            g.fill(Math.round(px), Math.round(py), Math.round(px) + w, Math.round(py) + w, Gfx.argb(Math.round(240 * a * (1f - life)), i % 2 == 0 ? 0xFFFFFF : hot));
        }
        NebulaTex.glow(g, hx, hy, 60f * k, 60f * k, Gfx.argb(Math.round(120 * a), flame)); NebulaTex.glow(g, hx, hy, 30f * k, 30f * k, Gfx.argb(Math.round(240 * a), hot)); NebulaTex.glow(g, hx, hy, 14f * k, 14f * k, Gfx.argb(Math.round(255 * a), 0xFFFFFF));
        int ps = Math.max(1, Math.round(1.6f * k)), ox = Math.round(hx) - ps * 5 / 2, oy = Math.round(hy) - ps * 5 / 2;                 // the rock itself, in big pixels
        for (int j = 0; j < 5; j++) for (int i = 0; i < 5; i++) { int m = ROCK[(j + seed) % 5][i]; if (m == 0) continue; g.fill(ox + i * ps, oy + j * ps, ox + (i + 1) * ps, oy + (j + 1) * ps, Gfx.argb(Math.round(255 * a), m == 1 ? 0x6A4630 : m == 2 ? 0xFFB060 : 0xFFFFFF)); }
        for (int i = 0; i < 4; i++) { float gl = 0.5f + 0.5f * (float) Math.sin(t * 12 + i * 2); float L = (10f + 6f * gl) * k; int c0 = Gfx.argb(Math.round(200 * a * gl), 0xFFFFFF);      // a glint on the head
            if (i < 2) g.fill(Math.round(hx - L), Math.round(hy), Math.round(hx + L), Math.round(hy) + 1, c0); else g.fill(Math.round(hx), Math.round(hy - L), Math.round(hx) + 1, Math.round(hy + L), c0); }
    }
    /** A drop: one big meteorite burns across the top of the screen, lighting the edge it comes from. */
    private static void oneMeteor(GuiGraphicsExtractor g, int W, int H, float u) {
        boolean left = meteorSeed % 2 == 0; float x0 = left ? -60 : W + 60, x1 = left ? W + 80 : -80, y0 = H * (0.04f + 0.08f * rnd(meteorSeed)), y1 = H * (0.34f + 0.1f * rnd(meteorSeed + 3));
        float in = Math.max(0f, 1f - u / 0.25f); if (in > 0) NebulaTex.glow(g, left ? 0 : W, y0 + 10, W * 0.3f, H * 0.3f, Gfx.argb(Math.round(70 * in), 0xFFC878));
        burn(g, x0, y0, x1, y1, eio(u), 1.9f, 0xFFE2A0, 0xFF7AD9, meteorSeed, 1f);
    }
    private static float eio(float u) { return u * u * (3 - 2 * u) * 0.35f + u * 0.65f; }
    /** An RNG drop: the sky opens. A gold flash, then a shower of meteorites burning down across the whole screen, a few of them huge. */
    private static void shower(GuiGraphicsExtractor g, int W, int H, float t) {
        float fl = Math.max(0f, 1f - t / 0.5f); if (fl > 0) { NebulaTex.glow(g, W * 0.5f, 0, W * 0.8f, H * 0.6f, Gfx.argb(Math.round(95 * fl), 0xFFD24A)); g.fill(0, 0, W, H, Gfx.argb(Math.round(26 * fl * fl), 0xFFF4D0)); }
        float glow = (float) Math.sin(Math.PI * Math.min(1f, t / (SHOWER_MS / 1000f))); NebulaTex.glow(g, W * 0.5f, -H * 0.05f, W * 0.75f, H * 0.28f, Gfx.argb(Math.round(40 * glow), 0xFF9AD0));
        boolean left = meteorSeed % 2 == 0; int n = 16;
        for (int i = 0; i < n; i++) {
            int sd = meteorSeed + i * 37; boolean big = i % 5 == 0; float start = i == 0 ? 0.05f : 0.1f + rnd(sd) * 2.1f, dur = big ? 1.15f : 0.75f + 0.3f * rnd(sd + 1), u = (t - start) / dur;
            if (u <= 0 || u >= 1) continue;
            float lane = rnd(sd + 2), sx = (left ? -0.25f + 1.1f * lane : 1.25f - 1.1f * lane) * W, sy = -30 - 40 * rnd(sd + 4), run = (0.75f + 0.5f * rnd(sd + 5)) * H;
            float ex = sx + (left ? 1 : -1) * run * 0.95f, ey = sy + run * 0.8f;
            int[] hots = {0xFFE2A0, 0xFFFFFF, 0xBFE0FF, 0xFFC8F0}, flames = {0xFF7AD9, 0x6AB8FF, 0xB56BFF, 0xFFD24A};
            burn(g, sx, sy, ex, ey, u, big ? 2.1f : 0.8f + 0.6f * rnd(sd + 6), big ? 0xFFE89A : hots[i % 4], big ? 0xFFB02A : flames[i % 4], sd, 1f);
        }
    }
    /** 0 by day, 1 deep in the night, easing through dusk and dawn (Minecraft: night is about 13000..23000 ticks). */
    static float nightness(float day) {
        float t = day * 24000f;
        if (t < 12000) return 0f; if (t < 13500) return (t - 12000) / 1500f; if (t < 22500) return 1f; return Math.max(0f, 1f - (t - 22500) / 1500f);
    }
    private static void shooting(GuiGraphicsExtractor g, int W, int H, float u, float a) {
        float x0 = shootDir > 0 ? W * 0.1f : W * 0.9f, hx = x0 + shootDir * W * 0.55f * u, hy = H * shootY + H * 0.12f * u, fade = (float) Math.sin(Math.PI * u) * a;
        for (int i = 0; i < 18; i++) { float q = i / 18f; float tx = hx - shootDir * q * 50, ty = hy - q * 11; g.fill(Math.round(tx), Math.round(ty), Math.round(tx) + 1, Math.round(ty) + 1, Gfx.argb(Math.round(230 * fade * (1f - q)), q < 0.3f ? 0xFFFFFF : 0xC8B4FF)); }
        NebulaTex.glow(g, hx, hy, 4, 4, Gfx.argb(Math.round(200 * fade), 0xFFFFFF));
    }
    /** Faint stars in the top corners, twinkling, only where there is no hotbar or chat. */
    private static void corners(GuiGraphicsExtractor g, int W, int H, long now, float a) {
        for (int i = 0; i < 40; i++) {
            float side = i % 2 == 0 ? 0f : 1f, u = rnd(i * 7 + 1) * 0.22f, v = rnd(i * 13 + 5) * rnd(i * 3 + 2) * 0.35f;
            float x = side == 0 ? W * u : W * (1f - u), y = H * v, tw = 0.3f + 0.7f * Math.abs((float) Math.sin(now / (700.0 + rnd(i) * 900) + i));
            g.fill(Math.round(x), Math.round(y), Math.round(x) + 1, Math.round(y) + 1, Gfx.argb(Math.round(170 * a * tw * (1f - (u / 0.22f) * 0.6f)), i % 5 == 0 ? 0xD8C8FF : 0xFFFFFF));
        }
    }
    /** A soft ribbon of light across the top of the screen, swaying slowly, violet into green. */
    private static void aurora(GuiGraphicsExtractor g, int W, int H, long now, float a) {
        float t = now / 1000f; int n = 14;
        for (int i = 0; i < n; i++) {
            float u = i / (n - 1f), x = W * u, y = H * (0.06f + 0.05f * (float) Math.sin(u * 5 + t * 0.4f) + 0.02f * (float) Math.sin(u * 13 - t * 0.7f));
            int col = Ritual.mixc(0x9B7BFF, 0x5AF0B0, 0.5f + 0.5f * (float) Math.sin(u * 3 + t * 0.2f));
            NebulaTex.glow(g, x, y, W / (float) n * 1.4f, H * 0.05f, Gfx.argb(Math.round(40 * a), col));
        }
    }
    private static float rnd(int i) { int h = i * 374761393 + 668265263; h = (h ^ (h >>> 13)) * 1274126177; return ((h ^ (h >>> 16)) & 0xFFFF) / 65535f; }
}
