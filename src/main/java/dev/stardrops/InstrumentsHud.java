package dev.stardrops;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Visuals > Instruments: small flight instruments for your HUD, each one switched on by itself, dragged anywhere and sized (HudEditScreen).
 * They share one look: dark glass, a fine violet edge with corner brackets, starlight text.
 *
 *   0 Celestial clock   the sun and the moon on their arc at the world's time, the moon in its phase
 *   1 Star compass      a tape of headings sliding past a gold marker, your bearing in degrees
 *   2 Altimeter         a height tape with a little rocket at your level, your Y
 *   3 Thruster gauge    your speed on an arc of lights with a needle, in blocks a second
 *   4 Nav computer      X, Y, Z and the way you face
 *   5 Deep-space link   your ping as signal bars round a dish, and your FPS
 *   6 Orbit clock       the real time: the hour is a planet, the minute its moon, the second a comet on the rim
 *   7 Star log          this session: how long, how many drops, how many of them RNG
 *
 * All of them fade away while a drop animation plays, like the rest of our HUD.
 */
final class InstrumentsHud {
    private InstrumentsHud() {}

    static final int N = 8;
    static final String[] NAMES = {"Day & night clock", "Compass", "Y level", "Speed", "Coordinates", "Ping & FPS", "Real time clock", "Session drops"};      // named for what they show (author: "rename the HUD features according to what they do")
    static final String[] SUBS = {"The sun and moon on their arc at the world's time, the moon's phase", "A tape of headings sliding past a gold marker, your bearing",
        "A height tape with a little rocket at your level", "Your speed on an arc of lights, in blocks a second", "X, Y, Z and the way you face",
        "Your ping as signal bars round a dish, and your FPS", "The real time: the hour a planet, the minute its moon", "This session: time played, drops, RNG drops"};
    static final String[] ICONS = {":cos_clock:", ":inst_compass:", ":inst_alt:", ":inst_speed:", ":inst_nav:", ":inst_link:", ":inst_orbit:", ":inst_log:"};
    private static final int[][] SIZE = {{52, 28}, {72, 30}, {30, 72}, {60, 38}, {76, 36}, {62, 30}, {46, 54}, {80, 36}};
    private static final float[][] HOME = {{0.008f, 0.02f}, {0.008f, 0.15f}, {0.008f, 0.29f}, {0.085f, 0.02f}, {0.085f, 0.15f}, {0.90f, 0.02f}, {0.90f, 0.14f}, {0.875f, 0.36f}};
    static final int BG = 0x070816, EDGE = 0x2C2A66, VIOLET = 0x9B7BFF, PINK = 0xFF7AD9, GOLD = 0xFFD27A, TEXT = 0xEEF2FF, DIM = 0x8A93B0;

    static boolean preview;                                                   // the HUD editor: keep them fully visible
    private static final int[][] BOX = new int[N][4];
    static int[] box(int i) { return BOX[i]; }
    static float homeX(int i) { return HOME[i][0]; } static float homeY(int i) { return HOME[i][1]; }
    static boolean any() { StardropsConfig c = StardropsConfig.get(); for (int i = 0; i < N; i++) if (c.instOn[i]) return true; return false; }
    static int count() { StardropsConfig c = StardropsConfig.get(); int n = 0; for (int i = 0; i < N; i++) if (c.instOn[i]) n++; return n; }

    // ---- this session, for the star log
    static int drops, rngDrops; static final long T0 = System.currentTimeMillis();
    static void dropLanded(int n, boolean rng) { drops += n; if (rng) rngDrops += n; }

    private static float gaugeMax = 10f;
    private static float away; private static long lastFrame; private static double lx, lz; private static long lt; private static float speed;

    static void render(GuiGraphicsExtractor g) {
        StardropsConfig c = StardropsConfig.get(); Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !any()) { for (int[] b : BOX) b[2] = b[0]; return; }
        long now = System.currentTimeMillis(); float dt = Math.min(0.1f, (now - (lastFrame == 0 ? now : lastFrame)) / 1000f); lastFrame = now;
        away += ((AnimationManager.isPlaying() && !preview ? 1f : 0f) - away) * Math.min(1f, dt * 6f);
        float a = 1f - away; int W = g.guiWidth(), H = g.guiHeight();
        long nt = System.nanoTime(); double px = mc.player.getX(), pz = mc.player.getZ();
        // speed: how far the last game tick moved you, times 20. (Measuring between frames jitters: the position only changes 20 times a second.)
        double ddx = px - mc.player.xo, ddz = pz - mc.player.zo; float v = (float) Math.sqrt(ddx * ddx + ddz * ddz) * 20f;
        if (v < 200f) speed += (v - speed) * Math.min(1f, dt * 5f); if (speed < 0.05f) speed = 0f; lt = nt; lx = px; lz = pz;
        for (int i = 0; i < N; i++) {
            if (!c.instOn[i] || (c.instHide && !preview)) { BOX[i][2] = BOX[i][0]; continue; }
            float k = c.instSize[i] / 100f; int w = Math.round(SIZE[i][0] * k), h = Math.round(SIZE[i][1] * k);
            int x = Math.max(0, Math.min(W - w, Math.round(c.instX[i] * W))), y = Math.max(0, Math.min(H - h, Math.round(c.instY[i] * H)));
            BOX[i][0] = x; BOX[i][1] = y; BOX[i][2] = x + w; BOX[i][3] = y + h;
            if (a < 0.02f) continue;
            Gfx.push(g); Gfx.translate(g, x, y); Gfx.scale(g, k);
            try { draw(g, mc, i, a, now); } catch (Throwable ignored) { }
            Gfx.pop(g);
        }
    }

    private static void draw(GuiGraphicsExtractor g, Minecraft mc, int i, float a, long now) {
        int w = SIZE[i][0], h = SIZE[i][1]; Font f = mc.font;
        switch (i) {
            case 0 -> { long ticks = mc.level.getOverworldClockTime(); clock(g, (ticks % 24000L) / 24000f, ticks, a); }
            case 1 -> compass(g, f, w, h, a, mc.player.getYRot());
            case 2 -> altimeter(g, f, w, h, a, (float) mc.player.getY(), now);
            case 3 -> thruster(g, f, w, h, a, now);
            case 4 -> nav(g, f, w, h, a, mc);
            case 5 -> link(g, f, w, h, a, mc, now);
            case 6 -> orbit(g, f, w, h, a, now);
            default -> log(g, f, w, h, a, now);
        }
    }

    // ------------------------------------------------------------------ shared bits
    private static int al(float a, int v) { return Math.round(v * a); }
    /** The glass every instrument sits on: dark, a fine edge, bright brackets at the corners. */
    private static void plate(GuiGraphicsExtractor g, int w, int h, float a) {
        g.fill(1, 0, w - 1, h, Gfx.argb(al(a, 150), BG)); g.fill(0, 1, 1, h - 1, Gfx.argb(al(a, 150), BG)); g.fill(w - 1, 1, w, h - 1, Gfx.argb(al(a, 150), BG));
        int e = Gfx.argb(al(a, 150), EDGE); g.fill(1, 0, w - 1, 1, e); g.fill(1, h - 1, w - 1, h, e); g.fill(0, 1, 1, h - 1, e); g.fill(w - 1, 1, w, h - 1, e);
        int b = Gfx.argb(al(a, 230), VIOLET);
        g.fill(0, 0, 4, 1, b); g.fill(0, 0, 1, 4, b); g.fill(w - 4, 0, w, 1, b); g.fill(w - 1, 0, w, 4, b); g.fill(0, h - 1, 4, h, b); g.fill(0, h - 4, 1, h, b); g.fill(w - 4, h - 1, w, h, b); g.fill(w - 1, h - 4, w, h, b);
    }
    private static void small(GuiGraphicsExtractor g, Font f, String s, float x, float y, float sc, int col, boolean centre) {
        if ((col >>> 24) < 6) return; Gfx.push(g); Gfx.translate(g, x, y); Gfx.scale(g, sc); g.text(f, s, centre ? -f.width(s) / 2 : 0, 0, col, false); Gfx.pop(g);
    }
    private static float rnd(int i) { int h = i * 374761393 + 668265263; h = (h ^ (h >>> 13)) * 1274126177; return ((h ^ (h >>> 16)) & 0xFFFF) / 65535f; }
    private static void star(GuiGraphicsExtractor g, int x, int y, int col) { g.fill(x, y - 1, x + 1, y + 2, col); g.fill(x - 1, y, x + 2, y + 1, col); }

    // ------------------------------------------------------------------ 0: the celestial clock
    /** A half dial, the sun or moon on its arc, the moon drawn in its phase. */
    private static void clock(GuiGraphicsExtractor g, float day, long ticks, float a) {
        int cx = 26, cy = 24, r = 18; int A = Math.round(255 * a);
        NowPlayingHud.disc(g, cx, cy - 4, r + 3, Gfx.argb(Math.round(150 * a), BG));
        g.fill(cx - r - 3, cy, cx + r + 4, cy + 1, Gfx.argb(Math.round(200 * a), EDGE));                       // the horizon
        for (int i = 0; i <= 18; i++) { double an = Math.PI + Math.PI * i / 18.0; int px = Math.round(cx + (float) Math.cos(an) * r), py = Math.round(cy + (float) Math.sin(an) * r); g.fill(px, py, px + 1, py + 1, Gfx.argb(Math.round(110 * a), VIOLET)); }   // the arc
        // Minecraft: sunrise at 0 (east, left), noon at 6000 (top), sunset at 12000 (right); the moon the other half
        boolean sunUp = day < 0.5f; float k = sunUp ? day * 2f : (day - 0.5f) * 2f; double an = Math.PI + Math.PI * k;
        float bx = cx + (float) Math.cos(an) * r, by = cy + (float) Math.sin(an) * r;
        if (sunUp) { NebulaTex.glow(g, bx, by, 7, 7, Gfx.argb(Math.round(170 * a), 0xFFC870)); NowPlayingHud.disc(g, bx, by, 3f, Gfx.argb(A, 0xFFE8A0)); }
        else {
            int phase = (int) ((ticks / 24000L) % 8);                                                       // 0 full .. 4 new .. back
            NebulaTex.glow(g, bx, by, 6, 6, Gfx.argb(Math.round(110 * a), 0xC8D4FF)); NowPlayingHud.disc(g, bx, by, 3f, Gfx.argb(A, 0xE8ECFF));
            float cover = phase <= 4 ? phase / 4f : (8 - phase) / 4f;
            if (cover > 0.02f) NowPlayingHud.disc(g, bx + (phase <= 4 ? 6f - 6f * cover : -6f + 6f * cover) * 0.5f, by, 3f * cover + 0.2f, Gfx.argb(A, BG));
        }
        for (int i = 0; i < 5; i++) if (!sunUp) { float sx = cx - r + rnd(i * 5) * r * 2, sy = cy - 4 - rnd(i * 9) * r * 0.8f; g.fill(Math.round(sx), Math.round(sy), Math.round(sx) + 1, Math.round(sy) + 1, Gfx.argb(Math.round(170 * a), 0xFFFFFF)); }
    }

    // ------------------------------------------------------------------ 1: the star compass
    private static final String[] WINDS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
    private static void compass(GuiGraphicsExtractor g, Font f, int w, int h, float a, float yaw) {
        plate(g, w, h, a);
        float bearing = ((yaw + 180f) % 360f + 360f) % 360f, cx = w / 2f, ppd = 0.62f;                       // Minecraft's yaw 0 looks south: north is bearing 0
        for (int d = 0; d < 360; d += 15) {
            float off = ((d - bearing + 540f) % 360f) - 180f, x = cx + off * ppd; if (x < 4 || x > w - 4) continue;
            float e = Math.min(1f, Math.min(x - 4, w - 4 - x) / 9f) * a; boolean wind = d % 45 == 0;
            Gfx.push(g); Gfx.translate(g, x - 0.5f, 0); g.fill(0, wind ? 11 : 13, 1, 16, Gfx.argb(al(e, wind ? 230 : 120), wind ? TEXT : DIM)); Gfx.pop(g);
            if (wind) { String s = WINDS[d / 45]; boolean north = d == 0; small(g, f, s, x, 3, s.length() > 1 ? 0.6f : 0.8f, Gfx.argb(al(e, 255), north ? PINK : d % 90 == 0 ? TEXT : DIM), true); }
        }
        g.fill(2, 16, w - 2, 17, Gfx.argb(al(a, 110), VIOLET));
        int m = Math.round(cx); int gc = Gfx.argb(al(a, 255), GOLD);                                           // the marker: a gold chevron pointing down at the tape
        g.fill(m - 2, 0, m + 3, 1, gc); g.fill(m - 1, 1, m + 2, 2, gc); g.fill(m, 2, m + 1, 3, gc); g.fill(m, 17, m + 1, 19, gc);
        small(g, f, Math.round(bearing) % 360 + "°", cx, 20, 0.85f, Gfx.argb(al(a, 255), TEXT), true);
        star(g, 7, 23, Gfx.argb(al(a, 200), GOLD)); star(g, w - 8, 23, Gfx.argb(al(a, 120), VIOLET));
    }

    // ------------------------------------------------------------------ 2: the altimeter
    private static void altimeter(GuiGraphicsExtractor g, Font f, int w, int h, float a, float y, long now) {
        plate(g, w, h, a);
        float mid = (h - 12) / 2f + 1, ppb = 0.5f;                                                             // two blocks a pixel
        g.fill(8, 3, 9, h - 12, Gfx.argb(al(a, 120), VIOLET));
        for (int d = (int) Math.floor((y - 60) / 8f) * 8; d <= y + 60; d += 8) {
            float py = mid - (d - y) * ppb; if (py < 4 || py > h - 13) continue; float e = Math.min(1f, Math.min(py - 4, h - 13 - py) / 7f) * a; boolean big = Math.floorMod(d, 32) == 0;
            Gfx.push(g); Gfx.translate(g, 0, py - 0.5f); g.fill(big ? 4 : 6, 0, 9, 1, Gfx.argb(al(e, big ? 230 : 110), big ? TEXT : DIM)); Gfx.pop(g);
            if (big) small(g, f, String.valueOf(d), 18, py - 2.5f, 0.55f, Gfx.argb(al(e, 220), d == 0 ? PINK : DIM), false);
        }
        int my = Math.round(mid); float fl = 0.6f + 0.4f * (float) Math.sin((now % 100000L) / 90.0);          // the rocket at your level, flame flickering
        int rc = Gfx.argb(al(a, 255), TEXT); g.fill(13, my - 3, 14, my - 2, rc); g.fill(12, my - 2, 15, my + 2, rc); g.fill(13, my - 1, 14, my, Gfx.argb(al(a, 255), 0x4FC3FF));
        g.fill(11, my + 1, 12, my + 3, Gfx.argb(al(a, 255), PINK)); g.fill(15, my + 1, 16, my + 3, Gfx.argb(al(a, 255), PINK)); g.fill(13, my + 2, 14, my + 3 + Math.round(2 * fl), Gfx.argb(al(a * fl, 255), GOLD));
        g.fill(9, my, 11, my + 1, Gfx.argb(al(a, 255), GOLD));
        g.fill(2, h - 11, w - 2, h - 10, Gfx.argb(al(a, 110), VIOLET));
        small(g, f, "Y " + Math.round(y), w / 2f, h - 8.5f, 0.8f, Gfx.argb(al(a, 255), TEXT), true);
    }

    // ------------------------------------------------------------------ 3: the thruster gauge
    private static void thruster(GuiGraphicsExtractor g, Font f, int w, int h, float a, long now) {
        plate(g, w, h, a);
        float want = speed < 9 ? 10 : speed < 18 ? 20 : speed < 36 ? 40 : 80; if (want > gaugeMax || speed < gaugeMax * 0.38f) gaugeMax = want;      // the scale steps up at once but only steps down well below it (no flapping at the edge)
        float max = gaugeMax, u = Math.min(1f, speed / max), cx = w / 2f, cy = 25, r = 19;
        for (int i = 0; i <= 24; i++) {
            float q = i / 24f; double an = Math.PI + Math.PI * q; float px = cx + (float) Math.cos(an) * r, py = cy + (float) Math.sin(an) * r; boolean on = q <= u + 0.001f;
            int col = q < 0.5f ? Ritual.mixc(VIOLET, PINK, q * 2f) : Ritual.mixc(PINK, GOLD, (q - 0.5f) * 2f);
            NowPlayingHud.dot(g, px, py, Gfx.argb(al(a, on ? 255 : 70), on ? col : DIM)); if (on && i % 2 == 0) NowPlayingHud.dot(g, px, py - 1f, Gfx.argb(al(a, 120), col));
            if (i % 6 == 0) { float tx = cx + (float) Math.cos(an) * (r - 3), ty = cy + (float) Math.sin(an) * (r - 3); NowPlayingHud.dot(g, tx, ty, Gfx.argb(al(a, 160), TEXT)); }
        }
        double an = Math.PI + Math.PI * u;                                                                     // the needle
        for (int s = 3; s <= 15; s++) NowPlayingHud.dot(g, cx + (float) Math.cos(an) * s, cy + (float) Math.sin(an) * s, Gfx.argb(al(a, 255), s > 12 ? GOLD : TEXT));
        NowPlayingHud.disc(g, cx, cy, 2f, Gfx.argb(al(a, 255), VIOLET));
        if (u > 0.6f) NebulaTex.glow(g, cx + (float) Math.cos(an) * r, cy + (float) Math.sin(an) * r, 6, 6, Gfx.argb(al(a * (u - 0.6f) / 0.4f, 150), GOLD));
        small(g, f, String.format(java.util.Locale.ROOT, "%.1f", speed), cx, 28.5f, 0.85f, Gfx.argb(al(a, 255), TEXT), true);
        small(g, f, "b/s", w - 12, 30, 0.55f, Gfx.argb(al(a, 200), DIM), false); small(g, f, String.valueOf(Math.round(max)), w - 10, 19, 0.5f, Gfx.argb(al(a, 160), DIM), false);
    }

    // ------------------------------------------------------------------ 4: the nav computer
    private static void nav(GuiGraphicsExtractor g, Font f, int w, int h, float a, Minecraft mc) {
        plate(g, w, h, a);
        int[] v = {(int) Math.floor(mc.player.getX()), (int) Math.floor(mc.player.getY()), (int) Math.floor(mc.player.getZ())}; int[] col = {0xFF8A8A, 0x8AF0A0, 0x8AB8FF}; String[] ax = {"X", "Y", "Z"};
        for (int i = 0; i < 3; i++) { small(g, f, ax[i], 5, 4 + i * 10, 0.8f, Gfx.argb(al(a, 255), col[i]), false); small(g, f, String.valueOf(v[i]), 14, 4 + i * 10, 0.8f, Gfx.argb(al(a, 255), TEXT), false); }
        float yaw = mc.player.getYRot(), bearing = ((yaw + 180f) % 360f + 360f) % 360f; float cx = w - 15, cy = h / 2f - 2;      // a little dial with an arrow the way you face
        NowPlayingHud.circle(g, cx, cy, 9, Gfx.argb(al(a, 130), VIOLET));
        double an = Math.toRadians(bearing - 90);
        for (int s = -4; s <= 7; s++) NowPlayingHud.dot(g, cx + (float) Math.cos(an) * s, cy + (float) Math.sin(an) * s, Gfx.argb(al(a, 255), s > 3 ? GOLD : TEXT));
        NowPlayingHud.dot(g, cx, cy - 9, Gfx.argb(al(a, 255), PINK));
        small(g, f, WINDS[Math.round(bearing / 45f) % 8], cx, h - 9, 0.7f, Gfx.argb(al(a, 255), GOLD), true);
    }

    // ------------------------------------------------------------------ 5: the deep-space link
    private static void link(GuiGraphicsExtractor g, Font f, int w, int h, float a, Minecraft mc, long now) {
        plate(g, w, h, a);
        int ping = -1; try { var info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(mc.player.getUUID()); if (info != null) ping = info.getLatency(); } catch (Throwable ignored) { }
        int bars = ping < 0 ? 0 : ping < 60 ? 5 : ping < 110 ? 4 : ping < 180 ? 3 : ping < 300 ? 2 : 1, good = bars >= 4 ? 0x8AF0A0 : bars == 3 ? GOLD : 0xFF8A8A;
        int dc = Gfx.argb(al(a, 255), TEXT);                                                                   // the dish
        g.fill(5, 12, 6, 16, dc); g.fill(6, 10, 7, 18, dc); g.fill(7, 9, 8, 19, dc); g.fill(8, 13, 11, 14, dc); g.fill(11, 12, 12, 15, Gfx.argb(al(a, 255), GOLD)); g.fill(6, 19, 9, 22, Gfx.argb(al(a, 200), DIM)); g.fill(4, 22, 11, 23, Gfx.argb(al(a, 200), DIM));
        float ph = (now % 1500L) / 1500f;                                                                      // its waves going out
        for (int k = 0; k < 3; k++) { float q = (ph + k / 3f) % 1f, r = 3 + q * 9; for (int s = -3; s <= 3; s++) { double an = s * 0.22; NowPlayingHud.dot(g, 12 + (float) Math.cos(an) * r, 13.5f + (float) Math.sin(an) * r, Gfx.argb(al(a * (1f - q), 200), VIOLET)); } }
        for (int b = 0; b < 5; b++) { int bh = 3 + b * 3, x = 26 + b * 4; g.fill(x, 20 - bh, x + 3, 20, Gfx.argb(al(a, b < bars ? 255 : 60), b < bars ? good : DIM)); }
        small(g, f, ping < 0 ? "-- ms" : ping + " ms", 48, 5, 0.6f, Gfx.argb(al(a, 255), TEXT), false);
        small(g, f, mc.getFps() + " fps", 26, 22, 0.65f, Gfx.argb(al(a, 230), DIM), false);
    }

    // ------------------------------------------------------------------ 6: the orbit clock
    private static void orbit(GuiGraphicsExtractor g, Font f, int w, int h, float a, long now) {
        plate(g, w, h, a);
        java.time.LocalTime tm = java.time.LocalTime.now(); float sec = tm.getSecond() + tm.getNano() / 1e9f, min = tm.getMinute() + sec / 60f, hr = (tm.getHour() % 12) + min / 60f, cx = w / 2f, cy = 22;
        for (int i = 0; i < 12; i++) { double an = i * Math.PI / 6 - Math.PI / 2; NowPlayingHud.dot(g, cx + (float) Math.cos(an) * 19, cy + (float) Math.sin(an) * 19, Gfx.argb(al(a, i % 3 == 0 ? 230 : 100), i % 3 == 0 ? TEXT : DIM)); }
        NowPlayingHud.circle(g, cx, cy, 13, Gfx.argb(al(a, 60), VIOLET));
        NebulaTex.glow(g, cx, cy, 6, 6, Gfx.argb(al(a, 150), 0xFFC870)); NowPlayingHud.disc(g, cx, cy, 2f, Gfx.argb(al(a, 255), 0xFFE8A0));        // the sun in the middle
        double ha = hr / 12.0 * Math.PI * 2 - Math.PI / 2, ma = min / 60.0 * Math.PI * 2 - Math.PI / 2, sa = sec / 60.0 * Math.PI * 2 - Math.PI / 2;
        float hx = cx + (float) Math.cos(ha) * 13, hy = cy + (float) Math.sin(ha) * 13;                         // the hour: a planet on its orbit
        NowPlayingHud.disc(g, hx, hy, 2.6f, Gfx.argb(al(a, 255), 0x8AB8FF)); NowPlayingHud.dot(g, hx - 1, hy - 1, Gfx.argb(al(a, 255), 0xD8E8FF));
        NowPlayingHud.circle(g, hx, hy, 6, Gfx.argb(al(a, 50), TEXT));
        float mx = hx + (float) Math.cos(ma) * 6, my = hy + (float) Math.sin(ma) * 6;                           // the minute: its moon
        NowPlayingHud.disc(g, mx, my, 1.2f, Gfx.argb(al(a, 255), TEXT));
        for (int k = 0; k < 6; k++) { double an = sa - k * 0.09; NowPlayingHud.dot(g, cx + (float) Math.cos(an) * 19, cy + (float) Math.sin(an) * 19, Gfx.argb(al(a * (1f - k / 6f), 255), k == 0 ? 0xFFFFFF : PINK)); }      // the second: a comet on the rim
        g.fill(2, 43, w - 2, 44, Gfx.argb(al(a, 110), VIOLET));
        small(g, f, String.format("%02d:%02d", tm.getHour(), tm.getMinute()), cx, 46, 0.8f, Gfx.argb(al(a, 255), TEXT), true);
    }

    // ------------------------------------------------------------------ 7: the star log
    private static void log(GuiGraphicsExtractor g, Font f, int w, int h, float a, long now) {
        plate(g, w, h, a);
        long s = (now - T0) / 1000L; String time = s >= 3600 ? (s / 3600) + "h " + (s % 3600) / 60 + "m" : (s / 60) + "m " + (s % 60) + "s";
        small(g, f, "SESSION", 5, 4, 0.55f, Gfx.argb(al(a, 220), VIOLET), false); small(g, f, time, w - 5 - f.width(time) * 0.7f, 3.5f, 0.7f, Gfx.argb(al(a, 255), TEXT), false);
        g.fill(4, 11, w - 4, 12, Gfx.argb(al(a, 90), VIOLET));
        star(g, 7, 17, Gfx.argb(al(a, 255), TEXT)); small(g, f, drops + (drops == 1 ? " drop" : " drops"), 13, 14, 0.8f, Gfx.argb(al(a, 255), TEXT), false);
        float tw = 0.7f + 0.3f * (float) Math.sin((now % 100000L) / 300.0); star(g, 7, 27, Gfx.argb(al(a * tw, 255), GOLD)); NebulaTex.glow(g, 7.5f, 27.5f, 5, 5, Gfx.argb(al(a * tw, rngDrops > 0 ? 110 : 0), GOLD));
        small(g, f, rngDrops + " RNG", 13, 24, 0.8f, Gfx.argb(al(a, 255), GOLD), false);
    }
}
