package dev.stardrops;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Ambience (Visuals > Music > Ambience), while a song plays: the space around you, in the album cover's colours.
 *   NEBULA: soft clouds of nebula drift slowly along the edges of the screen, breathing with the music.
 *   STAR EQUALIZER: either side of the hotbar, columns of little stars rise and fall to the beat, the top star of each brightest.
 *   COMETS: small comets curve up from beside the hotbar, trailing light, and burst into sparks as they fade.
 *   LIFT-OFF: when a new song starts, the Now Playing rocket leaves its card, flies one loop beside it and lands back on its course.
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
        if (t != null && !t.id().equals(waveFor)) { waveFor = t.id(); waveAt = now; }
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
        if (away(sinceSong)) flight(g, W, H, now, sinceSong, acc);
    }

    // ---- LIFT-OFF: a new song. The Now Playing rocket leaves its card, flies one smooth loop beside it and lands back on its course.
    // Everything is measured from the card (where it is, how big it is), so it follows the HUD wherever it is moved. No card on screen: no flight.
    static final long T0 = 1100, T1 = 4100;
    private static final int PINK = 0xFF7AD9, VIO = 0x9B7BFF;
    /** True while the rocket is out of the Now Playing card (the card hides its own rocket meanwhile). since: ms since the song changed. */
    static boolean away(long since) {
        return StardropsConfig.get().ambWave && on > 0.9f && since > T0 && since < T1 && System.currentTimeMillis() - NowPlayingHud.drawnAt < 400 && NowPlayingHud.bx1 > NowPlayingHud.bx0;
    }
    private static float sm(float v) { v = Math.max(0f, Math.min(1f, v)); return v * v * (3 - 2 * v); }
    /** Where the rocket is at u (0..1 of the loop): {x, y}. The loop starts and ends where the card's own rocket is, heading the way it flies. */
    private static void loop(float u, float lx, float ly, float rx, float ry, float dir, float[] out) {
        double th = Math.PI * 2 * (0.55f * u + 0.45f * sm(u));
        out[0] = lx + rx * (float) Math.sin(th); out[1] = ly + dir * ry * (1f - (float) Math.cos(th));
    }
    private static void sq(GuiGraphicsExtractor g, float x, float y, float sz, int argb) {
        if ((argb >>> 24) < 3) return; int x0 = Math.round(x - sz / 2f), y0 = Math.round(y - sz / 2f), d = Math.max(1, Math.round(sz)); g.fill(x0, y0, x0 + d, y0 + d, argb);
    }
    private static final float[] P = new float[2], Q = new float[2];
    private static void flight(GuiGraphicsExtractor g, int W, int H, long now, long since, int acc) {
        float u = (since - T0) / (float) (T1 - T0);
        float ak = (NowPlayingHud.bx1 - NowPlayingHud.bx0) / (float) NowPlayingHud.width();
        float lx = NowPlayingHud.bx0 + NowPlayingHud.rkX * ak, ly = NowPlayingHud.by0 + NowPlayingHud.rkY * ak;      // the card's own rocket, on screen
        float below = H - ly, above = ly, dir = below >= above ? 1f : -1f;                                           // the loop goes to the side with more room
        float ry = Math.max(6f, Math.min(34f * ak, Math.max(below, above) / 2f - 8f)), rx = Math.max(6f, Math.min(30f * ak, Math.min(lx, W - lx) - 8f));
        float big = (float) Math.sin(Math.PI * u), size = ak * (1f + 0.7f * big * big);                               // nearest (biggest) at the far end of the loop
        if (u < 0.22f) {                                                                 // lift-off: a puff and a few sparks thrown back
            float v = u / 0.22f;
            NebulaTex.glow(g, lx - 3 * ak, ly, (4 + 8 * v) * ak, (3 + 4 * v) * ak, Gfx.argb(Math.round(110 * (1f - v)), 0xFFB060));
            for (int i = 0; i < 8; i++) {
                double an = Math.PI + (rnd(i * 3 + 2) - 0.5) * 1.6; float d = ease(v) * (8 + 14 * rnd(i * 7)) * ak;
                sq(g, lx + (float) Math.cos(an) * d, ly + (float) Math.sin(an) * d, 1.5f * ak, Gfx.argb(Math.round(220 * (1f - v)), i % 2 == 0 ? 0xFFE9A8 : PINK));
            }
        }
        for (int k = 20; k >= 1; k--) {                                                  // the trail: the same loop a moment ago, white to pink to violet to the album's colour
            float uu = u - k * 0.011f; if (uu <= 0f) continue;
            loop(uu, lx, ly, rx, ry, dir, Q); float f = k / 20f, bb = (float) Math.sin(Math.PI * uu);
            int col = f < 0.33f ? Ritual.mixc(0xFFFFFF, PINK, f * 3) : f < 0.66f ? Ritual.mixc(PINK, VIO, (f - 0.33f) * 3) : Ritual.mixc(VIO, acc, (f - 0.66f) * 3);
            sq(g, Q[0], Q[1], Math.max(1f, ak * (1f + 0.7f * bb * bb) * (1.7f - f)), Gfx.argb(Math.round(190 * (1f - f) * (1f - sm((u - 0.85f) / 0.15f))), col));
        }
        loop(u, lx, ly, rx, ry, dir, P); loop(Math.min(1f, u + 0.01f), lx, ly, rx, ry, dir, Q);
        float ang = u > 0.985f ? 0f : (float) Math.atan2(Q[1] - P[1], Q[0] - P[0]);
        if (u > 0.9f) { float v = (u - 0.9f) / 0.1f; NebulaTex.glow(g, lx, ly, 7 * ak * (1f - v) + 2, 7 * ak * (1f - v) + 2, Gfx.argb(Math.round(90 * (float) Math.sin(Math.PI * v)), 0xFFFFFF)); }   // back on course: a small twinkle
        Gfx.push(g); Gfx.translate(g, P[0], P[1]); Gfx.rotate(g, ang); Gfx.scale(g, size); NowPlayingHud.rocket(g, 0, 0, 255, now, true, 1.3f + 0.9f * big); Gfx.pop(g);
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
