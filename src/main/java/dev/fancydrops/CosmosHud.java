package dev.fancydrops;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Visuals > Cosmos: the sky around you while you play, drawn lightly over the game (all of it fades away during drop animations).
 *
 *   Shooting stars: now and then a meteor streaks across the top of the screen (at night only, if you like).
 *   Celestial clock: a little dial in the top-left corner: the sun and the moon on their arc at the real Minecraft time, the moon's phase.
 *   Night sky: when it is dark out, a few faint stars twinkle in the top corners of the screen.
 *   Aurora: at night, a soft ribbon of violet and green light waves slowly across the top of the screen.
 *   Meteor on RNG: a big meteor burns across the screen the moment an RNG drop lands, before its animation.
 */
final class CosmosHud {
    private CosmosHud() {}

    private static long nextShoot, shootAt = -99999, meteorAt = -99999; private static float shootY, shootDir, away; private static long lastFrame;
    private static final java.util.Random R = new java.util.Random();

    /** An RNG drop just landed. */
    static void meteor() { if (FancyConfig.get().cosMeteor) meteorAt = System.currentTimeMillis(); }

    static void render(GuiGraphicsExtractor g) {
        FancyConfig c = FancyConfig.get(); Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (!(c.cosShooting || c.cosClock || c.cosNight || c.cosAurora || c.cosMeteor)) return;
        long now = System.currentTimeMillis(); float dt = Math.min(0.1f, (now - (lastFrame == 0 ? now : lastFrame)) / 1000f); lastFrame = now;
        away += ((AnimationManager.isPlaying() ? 1f : 0f) - away) * Math.min(1f, dt * 6f);
        int W = g.guiWidth(), H = g.guiHeight();
        long ticks = mc.level.getOverworldClockTime(); float day = (ticks % 24000L) / 24000f;
        boolean dark = mc.level.isDarkOutside(); float night = nightness(day);
        if (now - meteorAt < 1400) bigMeteor(g, W, H, (now - meteorAt) / 1400f);
        float vis = 1f - away; if (vis < 0.02f) return;
        if (c.cosAurora && night > 0.05f) aurora(g, W, H, now, night * vis);
        if (c.cosNight && night > 0.05f) corners(g, W, H, now, night * vis);
        if (c.cosShooting && (!c.cosShootNight || dark)) {
            if (nextShoot == 0) nextShoot = now + 8000 + R.nextInt(20000);
            if (now >= nextShoot) { shootAt = now; shootY = 0.05f + R.nextFloat() * 0.25f; shootDir = R.nextBoolean() ? 1f : -1f; nextShoot = now + 15000 + R.nextInt(40000); }
            if (now - shootAt < 900) shooting(g, W, H, (now - shootAt) / 900f, vis);
        }
        if (c.cosClock) clock(g, day, ticks, vis);
    }
    /** 0 by day, 1 deep in the night, easing through dusk and dawn (Minecraft: night is about 13000..23000 ticks). */
    private static float nightness(float day) {
        float t = day * 24000f;
        if (t < 12000) return 0f; if (t < 13500) return (t - 12000) / 1500f; if (t < 22500) return 1f; return Math.max(0f, 1f - (t - 22500) / 1500f);
    }
    private static void shooting(GuiGraphicsExtractor g, int W, int H, float u, float a) {
        float x0 = shootDir > 0 ? W * 0.1f : W * 0.9f, hx = x0 + shootDir * W * 0.55f * u, hy = H * shootY + H * 0.12f * u, fade = (float) Math.sin(Math.PI * u) * a;
        for (int i = 0; i < 18; i++) { float q = i / 18f; float tx = hx - shootDir * q * 50, ty = hy - q * 11; g.fill(Math.round(tx), Math.round(ty), Math.round(tx) + 1, Math.round(ty) + 1, Gfx.argb(Math.round(230 * fade * (1f - q)), q < 0.3f ? 0xFFFFFF : 0xC8B4FF)); }
        NebulaTex.glow(g, hx, hy, 4, 4, Gfx.argb(Math.round(200 * fade), 0xFFFFFF));
    }
    private static void bigMeteor(GuiGraphicsExtractor g, int W, int H, float u) {
        float hx = -40 + (W + 80) * u, hy = H * 0.08f + H * 0.25f * u, fade = Math.min(1f, (1f - u) * 3f);
        for (int i = 0; i < 40; i++) { float q = i / 40f, tx = hx - q * 140, ty = hy - q * 34, w = 3 * (1f - q);
            g.fill(Math.round(tx), Math.round(ty - w / 2), Math.round(tx) + 4, Math.round(ty + w / 2) + 1, Gfx.argb(Math.round(220 * fade * (1f - q)), Ritual.mixc(0xFFF0C8, 0xFF7AD9, q))); }
        NebulaTex.glow(g, hx, hy, 14, 14, Gfx.argb(Math.round(230 * fade), 0xFFE8B0));
        NebulaTex.glow(g, hx, hy, 40, 18, Gfx.argb(Math.round(120 * fade), 0xFF7AD9));
        for (int i = 0; i < 6; i++) { float q = ((u * 3 + i / 6f) % 1f); float sx = hx - 30 - q * 60 + i * 7, sy = hy + q * 20 + (i % 3) * 4; g.fill(Math.round(sx), Math.round(sy), Math.round(sx) + 1, Math.round(sy) + 1, Gfx.argb(Math.round(220 * fade * (1f - q)), 0xFFD8A0)); }   // sparks shed behind it
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
    /** The celestial clock: a half dial, the sun or moon on its arc, the moon drawn in its phase. Top left. */
    private static void clock(GuiGraphicsExtractor g, float day, long ticks, float a) {
        int cx = 26, cy = 24, r = 18; int A = Math.round(255 * a);
        NowPlayingHud.disc(g, cx, cy - 4, r + 3, Gfx.argb(Math.round(150 * a), 0x070816));
        g.fill(cx - r - 3, cy, cx + r + 4, cy + 1, Gfx.argb(Math.round(200 * a), 0x2C2A66));                       // the horizon
        for (int i = 0; i <= 18; i++) { double an = Math.PI + Math.PI * i / 18.0; int px = Math.round(cx + (float) Math.cos(an) * r), py = Math.round(cy + (float) Math.sin(an) * r); g.fill(px, py, px + 1, py + 1, Gfx.argb(Math.round(110 * a), 0x9B7BFF)); }   // the arc
        // Minecraft: sunrise at 0 (east, left), noon at 6000 (top), sunset at 12000 (right); the moon the other half
        boolean sunUp = day < 0.5f; float k = sunUp ? day * 2f : (day - 0.5f) * 2f; double an = Math.PI + Math.PI * k;
        float bx = cx + (float) Math.cos(an) * r, by = cy + (float) Math.sin(an) * r;
        if (sunUp) { NebulaTex.glow(g, bx, by, 7, 7, Gfx.argb(Math.round(170 * a), 0xFFC870)); NowPlayingHud.disc(g, bx, by, 3f, Gfx.argb(A, 0xFFE8A0)); }
        else {
            int phase = (int) ((ticks / 24000L) % 8);                                                       // 0 full .. 4 new .. back
            NebulaTex.glow(g, bx, by, 6, 6, Gfx.argb(Math.round(110 * a), 0xC8D4FF)); NowPlayingHud.disc(g, bx, by, 3f, Gfx.argb(A, 0xE8ECFF));
            float cover = phase <= 4 ? phase / 4f : (8 - phase) / 4f, off = (phase <= 4 ? 1 : -1) * (1f - cover) * 6f;
            if (cover > 0.02f) NowPlayingHud.disc(g, bx + (phase <= 4 ? 6f - 6f * cover : -6f + 6f * cover) * 0.5f, by, 3f * cover + 0.2f, Gfx.argb(A, 0x070816));
        }
        for (int i = 0; i < 5; i++) if (!sunUp) { float sx = cx - r + rnd(i * 5) * r * 2, sy = cy - 4 - rnd(i * 9) * r * 0.8f; g.fill(Math.round(sx), Math.round(sy), Math.round(sx) + 1, Math.round(sy) + 1, Gfx.argb(Math.round(170 * a), 0xFFFFFF)); }
    }
    private static float rnd(int i) { int h = i * 374761393 + 668265263; h = (h ^ (h >>> 13)) * 1274126177; return ((h ^ (h >>> 16)) & 0xFFFF) / 65535f; }
}
