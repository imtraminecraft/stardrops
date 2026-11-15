package dev.stardrops;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Ambience (Visuals > Music > Ambience), while a song plays: the space around you, in the album cover's colours.
 *   NEBULA: soft clouds of nebula drift slowly along the edges of the screen, breathing with the music.
 *   STAR EQUALIZER: either side of the hotbar, columns of little stars rise and fall to the beat, the top star of each brightest.
 *   COMETS: small comets curve up from beside the hotbar, trailing light, and burst into sparks as they fade.
 *   LIFT-OFF: when a new song starts, the Now Playing rocket leaves its card, flies to the song's world, circles it, and both settle back into the card.
 *   WORD SPARKLES: tiny stars burst from each word of the lyrics as it is sung (drawn by LyricsHud).
 * All of it fades away while a drop animation plays or the music is paused.
 */
final class AmbienceHud {
    private AmbienceHud() {}
    private static float on; private static long last, waveAt; private static String waveFor = "";

    static void render(GuiGraphicsExtractor g) {
        StardropsConfig c = StardropsConfig.get(); Minecraft mc = Minecraft.getInstance();
        long now = System.currentTimeMillis(); float dt = Math.min(0.1f, (now - (last == 0 ? now : last)) / 1000f); last = now;
        Spotify.Track t = Spotify.track;
        if (t != null && !t.id().equals(waveFor)) { waveFor = t.id(); waveAt = now; seed = t.id().hashCode(); }
        boolean any = c.ambGlow || c.ambNotes || c.ambBars || c.ambWave;
        boolean want = any && Spotify.playing && t != null && mc.level != null && !AnimationManager.isPlaying();
        on += ((want ? 1f : 0f) - on) * Math.min(1f, dt * 3f);
        if (on < 0.01f) return;
        draw(g, mc.font, g.guiWidth(), g.guiHeight(), now, on, c, now - waveAt, AlbumArt.accent());
    }

    static void draw(GuiGraphicsExtractor g, Font font, int W, int H, long now, float on, StardropsConfig c, long sinceSong, int acc) {
        int lite = Ritual.mixc(acc, 0xFFFFFF, 0.45f), neb1 = Ritual.mixc(acc, 0x9B7BFF, 0.45f), neb2 = Ritual.mixc(acc, 0xFF7AD9, 0.4f);
        float ts = (now % 3_600_000L) / 1000f;                                            // seconds, kept small (a float can't hold today's time in ms precisely)
        if (c.ambGlow) {                                                                 // nebula clouds drifting along the edges
            float str = on * c.ambGlowStrength / 100f;
            for (int i = 0; i < 7; i++) {
                float u = i / 7f, drift = ts * (0.012f + 0.006f * (i % 3)) + i * 0.37f;
                float along = (u + drift) % 1f, br = 0.65f + 0.35f * (float) Math.sin(ts * 0.5f + i * 1.7f);
                float x, y; int edge = i % 4;                                            // top, right, bottom, left: each cloud keeps to its edge
                if (edge == 0) { x = W * along; y = H * 0.02f; } else if (edge == 2) { x = W * (1f - along); y = H * 0.98f; }
                else if (edge == 1) { x = W * 0.99f; y = H * along; } else { x = W * 0.01f; y = H * (1f - along); }
                int col = i % 2 == 0 ? neb1 : neb2;
                NebulaTex.glow(g, x, y, W * 0.22f, H * 0.16f, Gfx.argb(Math.round(150 * br * str), col));
                NebulaTex.glow(g, x + W * 0.03f, y, W * 0.09f, H * 0.07f, Gfx.argb(Math.round(110 * br * str), lite));
            }
            for (int i = 0; i < 18; i++) {                                               // stars caught in the clouds
                float tw = 0.3f + 0.7f * Math.abs((float) Math.sin(ts * (1.1f + rnd(i) * 1.6f) + i)), u = rnd(i * 7 + 1);
                int edge = i % 4; float x = edge == 1 ? W - 2 - rnd(i * 3) * W * 0.06f : edge == 3 ? 2 + rnd(i * 3) * W * 0.06f : W * u;
                float y = edge == 0 ? 2 + rnd(i * 5) * H * 0.06f : edge == 2 ? H - 2 - rnd(i * 5) * H * 0.06f : H * u;
                g.fill(Math.round(x), Math.round(y), Math.round(x) + 1, Math.round(y) + 1, Gfx.argb(Math.round(190 * tw * str), 0xFFFFFF));
            }
        }
        if (c.ambBars) {                                                                 // the star equalizer, either side of the hotbar
            int n = 14, gap = 7, hot = 100;
            for (int side = -1; side <= 1; side += 2) for (int i = 0; i < n; i++) {
                float x = W / 2f + side * (hot + 3 + i * gap); if (x < 2 || x > W - 2) continue;
                float lvl = Math.abs((float) Math.sin(ts * (5.9f - i * 0.2f) + i * 0.7f + side) * (float) Math.sin(ts * (2.3f - i * 0.05f) + i * 1.9f)) * (1f - i / (float) n * 0.55f);
                int stars = 1 + Math.round(5 * lvl * on); int xi = Math.round(x);
                for (int k = 0; k < stars; k++) {
                    int y = H - 3 - k * 4; boolean top = k == stars - 1; int col = Ritual.mixc(acc, lite, k / 6f);
                    g.fill(xi, y, xi + 1, y + 1, Gfx.argb(Math.round((top ? 240 : 150) * on), top ? 0xFFFFFF : col));
                    if (top) { g.fill(xi - 1, y, xi + 2, y + 1, Gfx.argb(Math.round(110 * on), col)); g.fill(xi, y - 1, xi + 1, y + 2, Gfx.argb(Math.round(110 * on), col));
                               NebulaTex.glow(g, xi + 0.5f, y + 0.5f, 3.5f, 3.5f, Gfx.argb(Math.round(90 * on * lvl), lite)); }
                }
            }
        }
        if (c.ambNotes) {                                                                // comets curving up beside the hotbar
            for (int i = 0; i < 6; i++) {
                float per = 3.4f + (i % 3) * 0.7f, u = ((ts + i * 1.13f) % per) / per;
                float a = (float) Math.sin(Math.PI * Math.min(1f, u * 1.1f)) * on; if (a < 0.05f) continue;
                boolean left = i % 2 == 0; float dir = left ? -1 : 1, bx = W / 2f + dir * (104 + 16 * (i / 2 % 3)), by = H - 22;
                int col = Ritual.mixc(i % 3 == 0 ? neb2 : acc, 0xFFFFFF, 0.25f);
                for (int k = 14; k >= 0; k--) {                                          // the tail, then the head
                    float uu = Math.max(0f, u - k * 0.012f);
                    float x = bx + dir * (uu * 46 + (float) Math.sin(uu * 4 + i) * 6), y = by - uu * 90;
                    float fade = 1f - k / 15f;
                    if (k == 0) { NebulaTex.glow(g, x, y, 5, 5, Gfx.argb(Math.round(150 * a), col)); g.fill(Math.round(x), Math.round(y), Math.round(x) + 2, Math.round(y) + 2, Gfx.argb(Math.round(250 * a), 0xFFFFFF)); }
                    else g.fill(Math.round(x), Math.round(y), Math.round(x) + 1, Math.round(y) + 1, Gfx.argb(Math.round(200 * a * fade * fade), Ritual.mixc(0xFFFFFF, col, k / 14f)));
                }
                if (u > 0.86f) { float pu = (u - 0.86f) / 0.14f, x = bx + dir * (u * 46 + (float) Math.sin(u * 4 + i) * 6), y = by - u * 90;     // it bursts into sparks
                    for (int k = 0; k < 6; k++) { double an = k * Math.PI / 3 + i; float d = 8 * pu; g.fill(Math.round(x + (float) Math.cos(an) * d), Math.round(y + (float) Math.sin(an) * d), Math.round(x + (float) Math.cos(an) * d) + 1, Math.round(y + (float) Math.sin(an) * d) + 1, Gfx.argb(Math.round(220 * (1 - pu) * on), lite)); } }
            }
        }
        if (c.ambWave && sinceSong < FLIGHT) flight(g, W, H, now, on, sinceSong, acc);
    }

    // ---- LIFT-OFF: a new song. The Now Playing rocket leaves its card, flies across the screen to the song's world, circles it, and both settle back into the card.
    static final long FLIGHT = 8500;
    private static final int PINK = 0xFF7AD9, VIO = 0x9B7BFF;
    private static int seed = 7;
    /** True while the rocket is out of the Now Playing card (the card hides its own rocket meanwhile). since: ms since the song changed. */
    static boolean away(long since) { return StardropsConfig.get().ambWave && on > 0.5f && since > 1000 && since < FLIGHT - 250; }
    private static float sm(float v) { v = Math.max(0f, Math.min(1f, v)); return v * v * (3 - 2 * v); }
    private static float cub(float a, float b, float c, float d, float u) { float v = 1f - u; return v * v * v * a + 3 * v * v * u * b + 3 * v * u * u * c + u * u * u * d; }
    /** Where the rocket is at t seconds: {x, y, size, orbit angle or NaN}. Pure: the trail is this same path a little earlier. */
    private static float[] path(float t, float lx, float ly, float lk, float px, float py, float R, float s, float ax, float ay, float ak, float W, float H) {
        if (t < 4.4f) {                                                                  // the arc out of the card to the world
            float u = sm((t - 1f) / 3.4f), side = px > lx ? 1f : -1f, ex = px - side * R * 1.9f;
            return new float[]{cub(lx, lx - side * W * 0.16f, px - side * W * 0.34f, ex, u), cub(ly, ly + H * 0.34f, py - H * 0.3f, py, u), lk + (1.7f * s - lk) * sm(u * 2.2f), Float.NaN};
        }
        float home = sm((t - 6.6f) / 1.9f), side = px > lx ? 1f : -1f, ph = (side > 0 ? (float) Math.PI : 0f) + (t - 4.4f) * 2.7f * side;   // in orbit, then the whole thing shrinks home into the card
        float cx = px + (ax - px) * home, cy = py + (ay - py) * home, r = R + (6.5f * ak - R) * home;
        return new float[]{cx + (float) Math.cos(ph) * r * 1.9f, cy + (float) Math.sin(ph) * r * 0.55f * side, 1.7f * s + (ak - 1.7f * s) * home, ph};
    }
    private static void sq(GuiGraphicsExtractor g, float x, float y, float sz, int argb) {
        if ((argb >>> 24) < 3) return; int x0 = Math.round(x - sz / 2f), y0 = Math.round(y - sz / 2f), d = Math.max(1, Math.round(sz)); g.fill(x0, y0, x0 + d, y0 + d, argb);
    }
    private static void flight(GuiGraphicsExtractor g, int W, int H, long now, float on, long since, int acc) {
        float t = since / 1000f, s = H / 360f, ts = (now % 3_600_000L) / 1000f;
        boolean card = now - NowPlayingHud.drawnAt < 400 && NowPlayingHud.bx1 > NowPlayingHud.bx0;                  // the card on screen, or the top middle without it
        float ak = card ? (NowPlayingHud.bx1 - NowPlayingHud.bx0) / (float) NowPlayingHud.width() : 1f;
        float ax = card ? NowPlayingHud.bx0 + 197 * ak : W / 2f + 40, ay = card ? NowPlayingHud.by0 + 46 * ak : 30;  // the song's world in the card
        float lx = card ? NowPlayingHud.bx0 + 120 * ak : W / 2f - 40, ly = ay;                                       // where it lifts off
        float px = lx > W * 0.5f ? W * 0.26f : W * 0.74f, py = H * 0.36f, R = 26 * s;   // the world appears on the far side of the screen from the card
        if (t < 1.9f) {                                                                 // countdown, lift-off: smoke under the rocket, a small glow, never a white screen
            float up = sm(t / 0.9f), out = 1f - sm((t - 1.1f) / 0.8f);
            for (int i = 0; i < 7; i++) {
                float w = (ts * 0.9f + i * 0.37f) % 1f, sx = lx + (rnd(i * 5 + 1) - 0.5f) * 26 * ak * (0.3f + w), sy = ly + 4 * ak + w * 3 * ak;
                NebulaTex.glow(g, sx, sy, (3 + 6 * w) * ak, (2 + 3 * w) * ak, Gfx.argb(Math.round(120 * up * out * (1f - w) * on), 0xC8C4E0));
            }
            NebulaTex.glow(g, lx, ly, 9 * ak * up, 6 * ak * up, Gfx.argb(Math.round(150 * up * out * on), 0xFFB060));
        }
        if (t > 1f && t < 3.1f) {                                                       // three rings of little squares in the album's colours, and sparks on arcs
            for (int k = 0; k < 3; k++) {
                float u = (t - 1f - k * 0.16f) / 1.7f; if (u <= 0f || u >= 1f) continue;
                float r = ease(u) * Math.max(W, H) * (0.62f - k * 0.08f), a = (1f - u) * (1f - u) * on; int n = 36 + k * 8, col = k == 0 ? acc : k == 1 ? PINK : VIO;
                for (int i = 0; i < n; i++) {
                    double an = Math.PI * 2 * (i + k * 0.5) / n; float x = lx + (float) Math.cos(an) * r, y = ly + (float) Math.sin(an) * r * 0.8f;
                    if (x < -4 || x > W + 4 || y < -4 || y > H + 4) continue;
                    sq(g, x, y, (i % 3 == 0 ? 3 : 2) * s, Gfx.argb(Math.round(210 * a), i % 4 == 0 ? Ritual.mixc(col, 0xFFFFFF, 0.6f) : col));
                }
            }
            float u = (t - 1f) / 1.3f;
            if (u < 1f) for (int i = 0; i < 16; i++) {
                double an = Math.PI * (0.08 + 0.84 * rnd(i * 3 + 2)); float d = ease(u) * (40 + 70 * rnd(i * 7)) * s;
                sq(g, lx + (float) Math.cos(an) * d, ly + (float) Math.sin(an) * d * 0.7f + u * u * 26 * s, 2 * s, Gfx.argb(Math.round(230 * (1f - u) * on), i % 2 == 0 ? 0xFFE9A8 : PINK));
            }
        }
        if (t > 1.9f && t < 3.1f) {                                                     // warp: only stars stretch, into short streaks away from the middle
            float u = (t - 1.9f) / 1.2f, k = (float) Math.sin(Math.PI * u);
            for (int i = 0; i < 70; i++) {
                float x = rnd(i * 13 + 5) * W, y = rnd(i * 17 + 9) * H, dx = x - W / 2f, dy = y - H / 2f, d = (float) Math.sqrt(dx * dx + dy * dy) + 1f;
                float len = (3 + 15 * rnd(i * 5)) * s * k * Math.min(1f, d / (H * 0.35f)), mv = u * 14 * s;
                Gfx.push(g); Gfx.translate(g, x + dx / d * mv, y + dy / d * mv); Gfx.rotate(g, (float) Math.atan2(dy, dx));
                g.fill(0, 0, Math.max(1, Math.round(len)), 1, Gfx.argb(Math.round(150 * k * on), i % 4 == 0 ? Ritual.mixc(acc, 0xFFFFFF, 0.5f) : 0xE6ECFF));
                Gfx.pop(g);
            }
        }
        float[] p = path(Math.max(1f, t), lx, ly, ak, px, py, R, s, ax, ay, ak, W, H);
        float show = sm((t - 3f) / 1.2f), home = sm((t - 6.6f) / 1.9f), end = 1f - sm((t - 8.2f) / 0.3f);                // a new world comes into view; at the end the card takes over
        float rise = (1f - show) * 34 * s, wx = px + (ax - px) * home, wy = py + rise + (ay - py - rise) * home, wr = R * (0.4f + 0.6f * show) * (1f - home) + 6.5f * ak * home;
        boolean behind = !Float.isNaN(p[3]) && p[1] < wy;
        if (t > 1f) for (int k = 26; k >= 1; k--) {                                     // the trail: the same path a moment ago, white to pink to violet to the album's colour
            float tt = t - k * 0.032f; if (tt < 1f) continue;
            float[] q = path(tt, lx, ly, ak, px, py, R, s, ax, ay, ak, W, H); float f = k / 26f;
            if (!Float.isNaN(q[3]) && q[1] < wy && Math.abs(q[0] - wx) < wr) continue;                              // behind the world
            int col = f < 0.33f ? Ritual.mixc(0xFFFFFF, PINK, f * 3) : f < 0.66f ? Ritual.mixc(PINK, VIO, (f - 0.33f) * 3) : Ritual.mixc(VIO, acc, (f - 0.66f) * 3);
            sq(g, q[0], q[1], Math.max(1f, q[2] * (1.8f - f)), Gfx.argb(Math.round(200 * (1f - f) * (1f - home * 0.7f) * end * on), col));
        }
        float[] p2 = path(Math.max(1f, t) + 0.03f, lx, ly, ak, px, py, R, s, ax, ay, ak, W, H);
        float ang = (float) Math.atan2(p2[1] - p[1], p2[0] - p[0]); int ra = Math.round(255 * end * on);
        if (t > 1f && behind) { Gfx.push(g); Gfx.translate(g, p[0], p[1]); Gfx.rotate(g, ang); Gfx.scale(g, p[2]); NowPlayingHud.rocket(g, 0, 0, ra, now, true, 1.6f); Gfx.pop(g); }
        if (show > 0.01f) Worlds.draw(g, wx, wy, wr, Math.round(235 * show * end * on), seed, ts);
        if (t > 1f && !behind) { Gfx.push(g); Gfx.translate(g, p[0], p[1]); Gfx.rotate(g, ang); Gfx.scale(g, p[2]); NowPlayingHud.rocket(g, 0, 0, ra, now, true, t < 3f ? 2.2f : 1.4f); Gfx.pop(g); }
    }
    private static float rnd(int i) { int h = i * 374761393 + 668265263; h = (h ^ (h >>> 13)) * 1274126177; return ((h ^ (h >>> 16)) & 0xFFFF) / 65535f; }
    /** Stars that burst from a word as it lands (Word sparkles): little four-pointed twinkles flying up and out. u: 0..1 since it landed; k: size. */
    static void sparkle(GuiGraphicsExtractor g, float cx, float cy, float w, float u, float a, int seed, float k) {
        if (u >= 1f || a < 0.02f) return;
        int acc = LyricsHud.colour(), lite = Ritual.mixc(acc, 0xFFFFFF, 0.5f);
        for (int i = 0; i < 6; i++) {
            double an = -Math.PI / 2 + (Ritual.rnd(seed * 7 + i) - 0.5) * 2.8; float d = (8 + 14 * Ritual.rnd(seed * 3 + i)) * k * ease(u);
            float x = cx + (Ritual.rnd(seed + i * 11) - 0.5f) * w + (float) Math.cos(an) * d, y = cy + (float) Math.sin(an) * d + u * u * 6 * k, al = (1f - u) * a;
            float r = (1.2f + 1.6f * Ritual.rnd(seed * 5 + i)) * k * (1f - 0.5f * u); int col = i % 3 == 0 ? 0xDDE8FF : i % 3 == 1 ? acc : lite;
            NebulaTex.glow(g, x, y, r * 2.2f, r * 2.2f, Gfx.argb(Math.round(120 * al), col));
            NebulaTex.glow(g, x, y, r * 2.6f, r * 0.45f, Gfx.argb(Math.round(150 * al), 0xE8F2FF)); NebulaTex.glow(g, x, y, r * 0.45f, r * 2.6f, Gfx.argb(Math.round(150 * al), 0xE8F2FF));   // the star's points
        }
    }
    private static float ease(float v) { v = Math.max(0f, Math.min(1f, v)); return 1f - (1f - v) * (1f - v) * (1f - v); }
}
