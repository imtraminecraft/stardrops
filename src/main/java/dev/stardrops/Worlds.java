package dev.stardrops;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Little pixel planets, each picked by a seed (Now Playing gives every song its own world): a ringed violet planet, a cratered
 * grey moon, a banded gas giant, an ice world, a lava world, an ocean world with drifting islands. Lit from the top left, a dark
 * night side on the right, a soft halo in its own colour. Drawn row by row, so they stay crisp at any size.
 */
final class Worlds {
    private Worlds() {}
    static final int KINDS = 6;
    private static final int[] LIT = {0x8A6AE8, 0xC8C8D8, 0xE8A868, 0xBFE8FF, 0xFF7A3A, 0x4AB0E8};
    private static final int[] DARK = {0x2E2470, 0x4A4A60, 0x6A3420, 0x2E5A90, 0x4A1418, 0x163A70};
    private static final int[] HALO = {0x9B7BFF, 0xD8D8F0, 0xFFB070, 0x9AE8FF, 0xFF5A2A, 0x6AC8FF};

    static int kind(int seed) { return Math.floorMod(seed * 0x9E3779B1 >>> 7, KINDS); }
    static int halo(int seed) { return HALO[kind(seed)]; }

    /** A whole world at (x, y), radius r, alpha a (0..255). spin: how far its surface has turned (seconds). */
    static void draw(GuiGraphicsExtractor g, float x, float y, float r, int a, int seed, float spin) {
        if (a < 4 || r < 1) return;
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y);                                   // drawn on whole pixels, then slid by the fraction: it glides instead of stepping
        Gfx.push(g); Gfx.translate(g, x - xi, y - yi);
        try { draw0(g, xi, yi, r, a, seed, spin); } finally { Gfx.pop(g); }
    }
    private static void draw0(GuiGraphicsExtractor g, float x, float y, float r, int a, int seed, float spin) {
        int k = kind(seed);
        NebulaTex.glow(g, x, y, r * 2.1f, r * 2.1f, Gfx.argb(Math.round(a * 0.35f), HALO[k]));
        if (k == 0) ringHalf(g, x, y, r, a, true);
        int ri = Math.round(r);
        for (int dy = -ri; dy <= ri; dy++) {
            float hw = (float) Math.sqrt(Math.max(0f, r * r - (dy + 0.5f) * (dy + 0.5f))); if (hw < 0.5f) continue;
            int lit = LIT[k];
            if (k == 2) { int band = Math.floorMod(dy + ri + (seed & 3), 4); lit = band == 0 ? 0xF0D0A0 : band == 2 ? 0xC87848 : 0xE8A868; }
            float shade = 0.15f + 0.35f * (dy + r) / (2 * r);                                   // a little darker toward the bottom
            int y0 = Math.round(y + dy), xl = Math.round(x - hw), xr = Math.round(x + hw), term = Math.round(x + hw * 0.35f - dy * 0.15f);
            g.fill(xl, y0, xr, y0 + 1, Gfx.argb(a, DARK[k]));                                   // night side
            g.fill(xl, y0, Math.min(xr, term), y0 + 1, Gfx.argb(a, Ritual.mixc(lit, DARK[k], shade)));   // day side
        }
        // surface detail, turning slowly with the planet
        if (k == 1 || k == 5 || k == 4) {
            int n = k == 4 ? 4 : 3;
            for (int i = 0; i < n; i++) {
                float u = ((spin * 0.08f + i / (float) n + (seed & 7) * 0.13f) % 1f) * 2f - 1f, dx = u * r * 0.8f, dyy = (rnd(seed + i * 7) - 0.5f) * r;
                if (dx * dx + dyy * dyy > (r - 1.5f) * (r - 1.5f)) continue;
                float sz = Math.max(1f, r * (0.18f + 0.15f * rnd(seed + i * 3))) * (float) Math.sqrt(Math.max(0.2f, 1f - u * u));
                int c = k == 1 ? 0x8A8AA0 : k == 5 ? 0x5AC878 : 0xFFD060;
                if (k == 4) g.fill(Math.round(x + dx - sz), Math.round(y + dyy), Math.round(x + dx + sz), Math.round(y + dyy) + 1, Gfx.argb(a, c));   // lava cracks
                else NowPlayingHud.disc(g, x + dx, y + dyy, sz, Gfx.argb(Math.round(a * 0.9f), c));
            }
        }
        if (k == 3) g.fill(Math.round(x - r * 0.5f), Math.round(y - r) + 1, Math.round(x + r * 0.3f), Math.round(y - r) + 2, Gfx.argb(a, 0xFFFFFF));   // an ice cap
        NowPlayingHud.disc(g, x - r * 0.38f, y - r * 0.38f, Math.max(0.6f, r * 0.22f), Gfx.argb(Math.round(a * 0.55f), 0xFFFFFF));
        if (k == 0) ringHalf(g, x, y, r, a, false);
    }
    /** The ring of the violet world, tilted: the back half behind the planet, the front half over it. */
    private static void ringHalf(GuiGraphicsExtractor g, float x, float y, float r, int a, boolean back) {
        float rx = r * 1.75f, ry = r * 0.45f; int n = Math.max(16, Math.round(r * 6));
        for (int i = 0; i < n; i++) {
            double an = Math.PI * 2 * i / n; float sy = (float) Math.sin(an);
            if (back != (sy < 0)) continue;
            float px = x + (float) Math.cos(an) * rx, py = y + sy * ry - (float) Math.cos(an) * r * 0.2f;
            g.fill(Math.round(px), Math.round(py), Math.round(px) + 1, Math.round(py) + 1, Gfx.argb(Math.round(a * (back ? 0.55f : 0.9f)), 0xD8C8FF));
        }
    }
    private static float rnd(int i) { int h = i * 374761393 + 668265263; h = (h ^ (h >>> 13)) * 1274126177; return ((h ^ (h >>> 16)) & 0xFFFF) / 65535f; }
}
