package dev.fancydrops;

import static dev.fancydrops.Ritual.*;

/**
 * Slayer, Stardrops: a black hole in the boss's colour eats the boss.
 *
 * The hole opens in a small patch of space (never the whole screen): stars behind it bend round it (gravitational lensing), an
 * accretion disk turns round it, brighter on the side coming toward you, its far side bent up over the top like a halo, a thin
 * photon ring hugging the dark. The boss (new pixel art, one for each slayer) is beamed in beside it and then torn apart pixel by
 * pixel: each pixel is stretched, spun into the disk and swallowed, nearest first, in three gulps (the sound beats). At 3.4 s
 * the boss is gone, and then:
 *   NO DROPS (5.5 s): the hole swallows everything left, stars, disk, glow, folding the scene into a point that winks out;
 *     "Nothing dropped" fades in where it was.
 *   A DROP (6 s): the hole convulses, two jets fire from its poles and it spits the drop out of its heart; then it shrinks
 *     away and the drop floats there, the words lingering 3 s after.
 *   RNG (7 s): the hole collapses on itself and goes supernova: a white flash, two shock fronts, a remnant nebula blooming in
 *     the boss's colour with glowing filaments, and at its heart a pulsar whose two beams sweep round behind the drop while
 *     shards of the boss circle it. The cinematic bars close in and the HUD slides away; both come back when it ends.
 */
final class SlayerHole {
    private SlayerHole() {}

    // the patch of deep space behind the scene, in canvas units: Ritual draws it as soft GPU glows before the pixel scene
    static float BD_X, BD_Y, BD_SPAN, BD_A; static int BD_COL;
    /** The backdrop on screen (u = gui pixels per canvas unit, ox/oy = the canvas origin on screen). */
    static void backdrop(net.minecraft.client.gui.GuiGraphicsExtractor g, float u, float ox, float oy) {
        if (BD_A < 0.02f) return;
        float x = ox + BD_X * u, y = oy + BD_Y * u, sp = BD_SPAN * u;
        NebulaTex.glow(g, x, y, sp * 1.6f, sp * 1.05f, Gfx.argb(Math.round(235 * BD_A), VOID));
        NebulaTex.glow(g, x, y, sp * 1.05f, sp * 0.72f, Gfx.argb(Math.round(150 * BD_A), VOID));
        NebulaTex.glow(g, x, y, sp * 0.95f, sp * 0.58f, Gfx.argb(Math.round(90 * BD_A), mixc(BD_COL, VOID, 0.7f)));
    }
    /** The heartbeat the soundtrack plays under the pull (1.2 s, gaps 0.62 s shrinking by 0.78 each, at least 0.17): 0..1, a quick swell on each beat. */
    static float beat(float t) {
        float tk = 1.2f, gap = 0.62f, b = 0f;
        while (tk < 3.35f) { float u = (t - tk) / 0.28f; if (u > 0 && u < 1) b = Math.max(b, (float) Math.sin(Math.PI * u) * (0.55f + 0.45f * (tk - 1.2f) / 2.2f)); tk += gap; gap = Math.max(0.17f, gap * 0.78f); }
        return b;
    }
    static final String[] NAMES = {"Revenant Horror", "Tarantula Broodfather", "Sven Packmaster", "Voidgloom Seraph", "Inferno Demonlord", "Riftstalker Bloodfiend"};
    private static final int WHITE_ = 0xFFFFFF, VOID = 0x04030C;

    // ------------------------------------------------------------------ the bosses (front or side view, Minecraft style)
    private static final String[][] SPR = {
        {"..hGGhGGGg..", "..GGGgGGhG..", "..gGhGGGGg..", "..GEeGGeEg..", "..GGGgGGGG..", "..gGMMMMGg..", "..GGMggMGG..", "..gGGGGGgg..", "lDDDDDDDDDDd", "DllDDDDDDDdd", "GgDlDDDDDDgg", "GgDDDdDDdDgg", "hgdDDDDDDdgG", "GgDDdDDdDDgg", "Gg.LLLLLL.gg", "...LlL.LLL..", "...LLL.LdL..", "...LdL.LLL..", "...LLL.LLL..", "...BBB.BBB.."},
        {"L................L", ".L..............L.", "..LL..ssssss..LL..", "LL..LsSSSSSSsL..LL", "..LLSSrSSSSrSSLL..", "LL..SRRSSSSRRS..LL", "..LLSSSrSSrSSSLL..", ".L..sSSSSSSSSs..L.", "L..L.sSSmmSSs.L..L", "..L...ssSSss...L..", ".L......ss......L."},
        {"..wW..............", ".WWWw.............", "WWWWWW............", "WWEWWWWwWWWWWWWw..", "NWWWWWWWWWWWWWWWwT", "kkWWWWwWWWWWWWWWTT", "..WWWWWWWwWWWWWw.T", "..rWwwwwwwwwwwwW..", "...WW.WW...WW.WW..", "...WW.WW...WW.WW..", "...ww.ww...ww.ww.."},
        {"XXXXXXXX", "XxXXXXxX", "XXXXxXXX", "XXxXXXXX", "PpPXXPpP", "XXXXXXXX", "XxXXXXXX", "XXXXXxXX", "X.YyYY.X", "X.YYYy.X", "X.yYYY.X", "X.YYyY.X", "X.YYYY.X", "x.YyYY.X", "X.YYYY.x", "X..YY..X", "X.Y..Y.X", "X.Y..y.X", "X.y..Y.X", "X.Y..Y.X", "..Y..Y..", "..y..Y..", "..Y..y..", "..Y..Y.."},
        {".O........O.", ".o..YYYY..o.", "...YyYYyYY..", "O.YYYYYYYY.O", "o.YDDYYDDY.o", "..YyYYYYyY..", "..YYDDDDYY..", "...yYYYYy...", ".O..yYYy..O.", ".o........o.", "....O..O....", "....o..o....", "...O....O..."},
        {"..HHHHHHHH..", "..HhHHHHhH..", "..HFFFFFFH..", "..FRRFFRRF..", "..FFFfFFFF..", "..FFvFFvFF..", "..fFFFFFFf..", "...FFFFFF...", "CccMMWWMMccC", "CcCMMWWMMCcC", "CcCMMrrMMCcC", "CccMMMMMMccC", "CcCMmMMmMCcC", "CccMMMMMMccC", "Ccc.MMMM.ccC", "CcC.MMMM.CcC", "Cc..MM.MM.cC", "C...MM.MM..C", "....MM.MM...", "....mM.Mm...", "....KK.KK..."}};
    private static int pal(int boss, char ch) {
        return switch (boss) {
            case 0 -> switch (ch) { case 'h' -> 0x8AC060; case 'G' -> 0x6A9A48; case 'g' -> 0x4A7A30; case 'E' -> 0xFF2A2A; case 'e' -> 0xFF9A8A; case 'M' -> 0x2A3E1E;
                                    case 'D' -> 0x6FE0DA; case 'l' -> 0xC4FFF8; case 'd' -> 0x3AA0AA; case 'L' -> 0x2E8C96; case 'B' -> 0x3A3A46; default -> 0x101010; };
            case 1 -> switch (ch) { case 'S' -> 0x3A2A30; case 's' -> 0x5E4A54; case 'r' -> 0xC81E1E; case 'R' -> 0xFF2A2A; case 'm' -> 0x6A2020; default -> 0x56444A; };
            case 2 -> switch (ch) { case 'W' -> 0xF2F2F2; case 'w' -> 0xC8CACE; case 'E' -> 0xFF2A2A; case 'N' -> 0x1E1E1E; case 'k' -> 0x5A1010; case 'r' -> 0xC02030; default -> 0xE4E4E8; };
            case 3 -> switch (ch) { case 'X' -> 0x141018; case 'x' -> 0x241A2E; case 'P' -> 0xD040FF; case 'p' -> 0xF4A8FF; case 'y' -> 0x2A1E36; default -> 0x1C1424; };
            case 4 -> switch (ch) { case 'Y' -> 0xFFD040; case 'y' -> 0xE8962A; case 'D' -> 0x5A2E0A; case 'o' -> 0xC86A10; default -> 0xFFB040; };
            default -> switch (ch) { case 'H' -> 0x1A0A12; case 'h' -> 0x3A1A2A; case 'F' -> 0xEADDE2; case 'f' -> 0xC8B4BE; case 'R' -> 0xFF2030; case 'v' -> 0xFFFFFF;
                                     case 'C' -> 0x8A0A1C; case 'c' -> 0x4A0610; case 'M' -> 0x241420; case 'm' -> 0x3A2236; case 'W' -> 0xF0F0F0; case 'r' -> 0xFF3040; default -> 0x0A0A0A; };
        };
    }
    private static boolean glows(int boss, char ch) { return switch (boss) { case 0 -> ch == 'E' || ch == 'e'; case 1 -> ch == 'r' || ch == 'R'; case 2 -> ch == 'E'; case 3 -> ch == 'P' || ch == 'p'; case 5 -> ch == 'R' || ch == 'r'; default -> false; }; }
    /** Each boss's pixels, shaded once like a Minecraft texture: a little grain in every pixel, light catching the top and left edges,
     *  shadow on the bottom and right edges and toward the feet (glowing eyes stay pure). */
    private static final int[][][] SHADE = new int[6][][];
    private static int shaded(int boss, int x, int y) {
        if (SHADE[boss] == null) {
            String[] gr = SPR[boss]; int h = gr.length, w = gr[0].length(); int[][] o = new int[h][w];
            for (int yy = 0; yy < h; yy++) for (int xx = 0; xx < w; xx++) {
                char ch = gr[yy].charAt(xx); if (ch == '.') continue;
                int c = pal(boss, ch);
                if (!glows(boss, ch)) {
                    int hs = (xx * 73856093) ^ (yy * 19349663) ^ (boss * 83492791); hs ^= hs >>> 13; hs *= 0x5bd1e995; hs ^= hs >>> 15;
                    float grain = ((hs & 255) / 255f - 0.5f) * 0.16f;
                    c = grain > 0 ? mixc(c, 0xFFFFFF, grain) : mixc(c, 0x000000, -grain);
                    boolean up = yy == 0 || gr[yy - 1].charAt(xx) == '.', lf = xx == 0 || gr[yy].charAt(xx - 1) == '.';
                    boolean dn = yy == h - 1 || gr[yy + 1].charAt(xx) == '.', rt = xx == w - 1 || gr[yy].charAt(xx + 1) == '.';
                    if (up || lf) c = mixc(c, 0xFFFFFF, 0.16f);
                    if (dn || rt) c = mixc(c, 0x000000, 0.3f);
                    c = mixc(c, 0x000000, 0.14f * yy / (float) h);
                }
                o[yy][xx] = c;
            }
            SHADE[boss] = o;
        }
        return SHADE[boss][y][x];
    }

    // ------------------------------------------------------------------ the scene
    static void paint(float t, float A, int boss, int energy, int rc, boolean rng, boolean nod, float icx, float icy, float sz, float isc,
                      float itemA, float[] offs, float cy0, float bg) {
        final float O = AnimationManager.SL_PIVOT; float p = t - O;
        int hot = mixc(energy, WHITE_, 0.55f);
        // the hole: opens, holds through the meal, then (by type) spits, collapses or swallows everything
        float open = eob((t - 0.1f) / 0.7f);
        float gulp = beat(t);                                                                       // the hole throbs on the soundtrack's heartbeat, faster and faster
        float rh0 = 21 * sz, rh = rh0 * open * (1f + 0.12f * gulp);
        float swallow = nod ? sm((p - 0.15f) / 0.95f) : 0f;                                      // no drops: the hole eats the scene
        float spit = !nod && !rng ? sm((p - 0.3f) / 1.4f) : 0f;                                    // a drop: the hole shrinks away after spitting
        float nova = rng ? sm(p / 0.22f) : 0f;                                                       // RNG: it collapses before the supernova
        float holeK = (1f - swallow) * (1f - spit) * (1f - nova);
        float stage = bg * sm(t / 0.35f) * (nod ? 1f - sm((p - 0.9f) / 0.4f) : 1f);
        // a patch of deep space round it all (it stays a patch: the rest of the screen is untouched)
        float span = 165 * sz * (1f - 0.85f * swallow);
        BD_X = icx; BD_Y = icy; BD_SPAN = span; BD_A = stage; BD_COL = energy;                         // the big soft patch is drawn on the GPU (backdrop), not pixel by pixel here
        lensedStars(t, icx, icy, rh * holeK + 0.01f, span, stage, swallow, energy);
        if (holeK > 0.01f) {
            float r = rh * holeK;
            if (gulp > 0) glowE(icx, icy, r * 2.6f, r * 1.5f, hot, 0.35f * gulp * stage);                 // each gulp flares the disk
            disk(t, icx, icy, r, energy, hot, stage * Math.min(1f, open * 1.4f), true);           // the far side of the disk, and its image bent over the top
            fillE(icx, icy, r * 1.02f, r * 1.02f, 0x000000, Math.min(1f, stage * 1.4f));                // the event horizon: nothing in it
            ringE(icx, icy, r * 1.06f, r * 1.06f, hot, 0.9f * stage, 0, 6.2832f, 0, 0);            // the photon ring
            ringE(icx, icy, r * 1.14f, r * 1.14f, energy, 0.45f * stage, 0, 6.2832f, 0, 0);
            disk(t, icx, icy, r, energy, hot, stage * Math.min(1f, open * 1.4f), false);          // the near side, in front
        }
        // the boss: beamed in beside the hole, then torn apart into it
        if (p < 0.2f) boss(t, boss, icx, icy, rh0, sz, energy, hot, stage);
        if (p < 0) return;

        // ---- the pivot
        if (nod) {                                                                                    // the hole folds the scene into a point, which winks out
            float wink = (p - 1.1f) / 0.35f;
            if (wink > 0 && wink < 1) { float f = (float) Math.sin(Math.PI * wink); glowE(icx, icy, 10 * sz * f, 10 * sz * f, hot, 0.9f * f); stroke(icx - 14 * sz * f, icy, icx + 14 * sz * f, icy, 0.5f, WHITE_, f); }
            return;
        }
        if (!rng) {                                                                                   // the spit: jets from the poles, a ring, sparks flung out with the drop
            float j = (float) Math.sin(Math.PI * cl(p / 0.9f));
            if (j > 0.01f) for (int d = -1; d <= 1; d += 2) {
                float len = 70 * sz * j; stroke(icx, icy, icx, icy + d * len, 2.6f * j, energy, 0.5f * j); stroke(icx, icy, icx, icy + d * len * 0.9f, 1f * j, WHITE_, 0.9f * j);
                for (int k = 0; k < 6; k++) { float u = ((p * 2.2f + k / 6f) % 1f); dot(icx + (rnd(k * 9) - 0.5f) * 3 * sz, icy + d * u * len, WHITE_, (1f - u) * j); }
            }
            float sw = p / 0.8f; if (sw < 1f) ringE(icx, icy, 10 * sz + 60 * sz * eo(sw), (10 * sz + 60 * sz * eo(sw)) * 0.5f, hot, (1f - sw) * 0.9f, 0, 6.2832f, 0, 0);
            for (int k = 0; k < 22; k++) { float an = rnd(k * 13 + 1) * 6.2832f, v = (40 + 80 * rnd(k * 7)) * sz, tau = Math.min(p, 1.3f), d = v * tau * (1f - 0.3f * tau);
                dot(icx + (float) Math.cos(an) * d, icy + (float) Math.sin(an) * d * 0.6f, k % 3 == 0 ? WHITE_ : energy, (1f - cl(p / 1.3f)) * 0.9f); }
            for (float off : offs) { float cx = icx + off; glowE(cx, cy0, 24 * sz * isc, 24 * sz * isc, energy, 0.4f * itemA); glowE(cx, cy0, 10 * sz * isc, 10 * sz * isc, WHITE_, 0.22f * itemA);
                for (int k = 0; k < 5; k++) { float an = t * 1.6f + k * 1.2566f; glint(cx + (float) Math.cos(an) * 22 * sz * isc, cy0 + (float) Math.sin(an) * 8 * sz * isc, (float) Math.sin(an) > 0 ? 2 : 1, k % 2 == 0 ? WHITE_ : energy, 0.85f * itemA); } }
            return;
        }
        supernova(t, p, icx, icy, sz, isc, energy, hot, rc, itemA, offs, cy0, boss);
    }

    /** Stars behind the hole, bent outward round it (a ring of light where they crowd the edge), dragged in when it swallows the scene. */
    private static void lensedStars(float t, float cx, float cy, float rh, float span, float a, float swallow, int energy) {
        if (a < 0.02f) return;
        for (int i = 0; i < 110; i++) {
            float an = rnd(i * 7 + 3) * 6.2832f, d = (6 + rnd(i * 11 + 5) * 1.25f * span) , dd = d + 2.2f * rh * rh / Math.max(1f, d);   // lensing: pushed out from the hole
            if (d < rh * 1.2f) continue;
            float sp = swallow * swallow; dd *= 1f - sp; float ang = an + sp * 4f;
            float x = cx + (float) Math.cos(ang) * dd * 1.45f, y = cy + (float) Math.sin(ang) * dd * 0.85f;
            float edge = 1f - cl((d - 0.8f * span) / (0.45f * span));                              // the patch fades out at its edge
            float tw = 0.5f + 0.5f * (float) Math.sin(t * (1.5f + rnd(i) * 3) + i), near = cl(1f - (dd - rh) / (rh * 3f));
            int c = i % 11 == 0 ? energy : i % 6 == 0 ? 0xC8D8FF : WHITE_;
            dot(x, y, c, a * edge * (0.35f + 0.55f * tw + 0.4f * near) * (1f - swallow * 0.3f));
            if (sp > 0.05f) stroke(x, y, x + (float) Math.cos(ang + 1.3f) * 6 * sp, y + (float) Math.sin(ang + 1.3f) * 4 * sp, 0.4f, c, a * edge * sp * 0.6f);
        }
    }
    /** The accretion disk: tilted, turning, hottest at the inner edge, brighter on the approaching (left) side. back: the far half
     *  and its lensed image arching over the top; else the near half, in front of the hole. */
    private static void disk(float t, float cx, float cy, float r, int energy, int hot, float a, boolean back) {
        if (a < 0.02f || r < 1f) return;
        if (back) {
            glowE(cx, cy, r * 4.4f, r * 1.1f, energy, 0.3f * a);                                      // the disk's light, behind everything
            for (int k = 0; k < 6; k++) {                                                              // its far half, and the image of it bent up over the top
                float rr = r * (1.55f + 0.42f * k), al = a * (0.75f - 0.1f * k);
                ringE(cx, cy, rr, rr * 0.22f, mixc(hot, energy, k / 6f), al * 0.32f, 3.1416f, 6.2832f, 0, 0);
                float lr = r * (1.12f + 0.16f * k);
                ringE(cx, cy, lr * 1.05f, lr * 0.98f, mixc(hot, energy, k / 6f), al * (0.45f - 0.06f * k), 3.1416f, 6.2832f, 0, 0);
            }
        } else for (int k = 0; k < 6; k++) {                                                          // its near half, in front of the hole
            float rr = r * (1.55f + 0.42f * k), al = a * (0.85f - 0.1f * k);
            ringE(cx, cy, rr, rr * 0.22f, mixc(mixc(hot, WHITE_, 0.35f), energy, k / 6f), al * 0.5f, 0, 3.1416f, 0, 0);
        }
        int n = 260;
        for (int i = 0; i < n; i++) {                                                                // clumps of gas racing round it
            float u = rnd(i * 5 + 1), rr = r * (1.5f + 2.6f * u * u), w = 1f / (float) Math.sqrt(rr / r);
            float an = rnd(i * 3 + 7) * 6.2832f + t * 2.4f * w; float sn = (float) Math.sin(an), cs = (float) Math.cos(an);
            if (back != (sn < 0)) continue;
            float x = cx + cs * rr, y = cy + sn * rr * 0.22f;
            float dop = 0.5f + 0.5f * -cs;                                                             // coming toward you (left): brighter, whiter
            int c = mixc(mixc(energy, hot, 1f - u), WHITE_, 0.45f * dop * (1f - u));
            dot(x, y, c, a * (0.3f + 0.7f * dop) * (1f - 0.45f * u));
            if (dop > 0.8f && u < 0.4f) dot(x + 1, y, c, a * 0.6f);
        }
    }
    /** The boss beside the hole: it shimmers in (a scan line), leans toward the hole, then is pulled apart pixel by pixel. */
    private static void boss(float t, int boss, float hx, float hy, float rh0, float sz, int energy, int hot, float a) {
        String[] g = SPR[boss]; int h = g.length, w = g[0].length();
        float px = (w > 14 ? 4.4f : 5.0f) * sz * (h >= 24 ? 0.8f : 1f), bw = w * px, bh = h * px;
        float mx = hx + rh0 * 3.4f + bw * 0.5f + 4 * sz, my = hy;                                      // its centre, to the right of the hole
        float beam = cl((t - 0.45f) / 0.6f);                                                         // beamed in, top to bottom
        float su = cl((t - 1.15f) / 2.25f), suck = su * su * (3 - 2 * su) * (0.7f + 0.3f * su);              // one long pull, speeding up
        float lean = sm((t - 1.1f) / 0.6f);
        float dmin = Float.MAX_VALUE, dmax = 0;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) { if (g[y].charAt(x) == '.') continue; float X = mx + (x - w / 2f + 0.5f) * px, Y = my + (y - h / 2f + 0.5f) * px; float d = (float) Math.hypot(X - hx, Y - hy); dmin = Math.min(dmin, d); dmax = Math.max(dmax, d); }
        float drift = (float) Math.sin(t * 2.2f) * 1.5f * sz;
        for (int y = 0; y < h; y++) {
            float rowIn = cl(beam * (h + 4) - y) ; if (rowIn <= 0) continue;
            for (int x = 0; x < w; x++) {
                char ch = g[y].charAt(x); if (ch == '.') continue;
                float X = mx + (x - w / 2f + 0.5f) * px, Y = my + (y - h / 2f + 0.5f) * px + drift;
                // leaning: everything is drawn a little toward the hole, the near side more (tidal stretch)
                float d0 = (float) Math.hypot(X - hx, Y - hy), ord = (d0 - dmin) / Math.max(1f, dmax - dmin);
                X -= lean * (1f - ord) * 6 * sz; Y += lean * (Y - hy) * -0.08f;
                float q = cl(suck * 1.15f - ord * 0.15f); q = q * q * (3 - 2 * q);
                int col = shaded(boss, x, y);
                float al = a * rowIn;
                if (q <= 0.001f) {
                    float sc = rowIn < 1f ? 0.6f : 1f;
                    cv.rect(Math.round(X - px / 2), Math.round(Y - px / 2), Math.round(X - px / 2 + px * sc), Math.round(Y - px / 2 + px), A(al, rowIn < 1f ? mixc(col, hot, 0.7f) : col));
                    if (glows(boss, ch)) glowE(X, Y, 3 * sz, 3 * sz, col, 0.45f * al);
                    continue;
                }
                // torn off: spiralling in, stretched along its path, heating up, swallowed at the horizon
                float ang0 = (float) Math.atan2(Y - hy, X - hx), r0 = (float) Math.hypot(X - hx, Y - hy);
                // it never quite crosses: it slows as it nears the horizon, smears round it, reddens and fades (time dilation)
                float rr = rh0 * 1.06f + (r0 - rh0 * 1.06f) * (float) Math.pow(1f - q, 1.5f), ang = ang0 + q * 3.2f * (1f - 0.45f * q);
                float red = cl((q - 0.72f) / 0.28f); al *= 1f - red; if (al < 0.02f) continue;
                float sx = hx + (float) Math.cos(ang) * rr, sy = hy + (float) Math.sin(ang) * rr * (1f - 0.7f * q);
                float pa = ang - 0.35f * q, pr = rr * (1f + 0.25f * q);
                float tx = hx + (float) Math.cos(pa) * pr, ty = hy + (float) Math.sin(pa) * pr * (1f - 0.7f * q);
                int c2 = mixc(mixc(col, hot, 0.85f * Math.min(q, 0.6f) / 0.6f), 0x5A0010, red);
                stroke(tx, ty, sx, sy, Math.max(0.4f, px * 0.45f * (1f - q * 0.5f)), c2, al * (0.6f + 0.4f * q));
                dot(sx, sy, mixc(c2, WHITE_, q * 0.5f), al);
            }
        }
        if (beam > 0 && beam < 1) { float by = my - bh / 2f + beam * (bh + 4 * px); stroke(mx - bw * 0.7f, by, mx + bw * 0.7f, by, 0.6f, hot, 0.9f * a); glowE(mx, by, bw * 0.8f, 3 * sz, hot, 0.4f * a); }
    }

    /** RNG: the supernova, and the pulsar left at its heart. */
    private static void supernova(float t, float p, float cx, float cy, float sz, float isc, int energy, int hot, int rc, float itemA, float[] offs, float cy0, int boss) {
        float R = 52 * sz * isc;
        float fl = 1f - cl((p - 0.15f) / 0.45f);
        if (p > 0.12f) glowE(cx, cy, R * (1.2f + 0.9f * (1f - fl)), R * (0.9f + 0.7f * (1f - fl)), WHITE_, 0.95f * fl);
        for (int k = 0; k < 2; k++) {                                                                  // two shock fronts
            float sw = (p - 0.15f - k * 0.18f) / 1.3f; if (sw < 0 || sw > 1) continue;
            float rr = R * (0.3f + 1.5f * eo(sw));
            ringE(cx, cy, rr, rr * 0.62f, k == 0 ? WHITE_ : hot, (1f - sw) * 0.9f, 0, 6.2832f, 0, 0);
            ringE(cx, cy, rr - 1.5f, rr * 0.62f - 1, energy, (1f - sw) * 0.55f, 0, 6.2832f, 0, 0);
        }
        float bloom = sm((p - 0.2f) / 1.2f), life = itemA;
        if (bloom > 0.01f) {                                                                         // the remnant nebula: clouds and filaments in the boss's colour
            for (int i = 0; i < 9; i++) { float an = i * 0.698f + rnd(i * 5 + boss) * 0.5f, d = R * (0.6f + 0.55f * rnd(i * 3)) * (0.6f + 0.6f * bloom);
                int c = i % 3 == 0 ? 0xFF7AD9 : i % 3 == 1 ? energy : 0x9B7BFF;
                glowE(cx + (float) Math.cos(an) * d, cy + (float) Math.sin(an) * d * 0.62f, R * 0.5f, R * 0.32f, c, 0.28f * bloom * life); }
            for (int i = 0; i < 26; i++) { float an = rnd(i * 13 + 2) * 6.2832f, d0 = R * (0.35f + 0.2f * rnd(i)), d1 = d0 + R * (0.4f + 0.6f * rnd(i * 7)) * bloom;
                float wob = (float) Math.sin(t * 1.3f + i) * 0.06f;
                stroke(cx + (float) Math.cos(an) * d0, cy + (float) Math.sin(an) * d0 * 0.62f, cx + (float) Math.cos(an + wob) * d1, cy + (float) Math.sin(an + wob) * d1 * 0.62f, 0.5f, i % 2 == 0 ? hot : 0xFF9AD8, 0.5f * bloom * life); }
        }
        for (float off : offs) {
            float x = cx + off;
            // the pulsar's two beams, sweeping round behind the drop like a lighthouse
            float ba = t * 2.6f;
            for (int k = 0; k < 2; k++) { float an = ba + k * 3.1416f, L = R * 1.6f, ex = x + (float) Math.cos(an) * L, ey = cy0 + (float) Math.sin(an) * L * 0.55f;
                stroke(x, cy0, ex, ey, 3.2f, energy, 0.22f * life); stroke(x, cy0, ex, ey, 1.1f, WHITE_, 0.55f * life); }
            float pulse = 0.75f + 0.25f * (float) Math.sin(t * 16f);
            glowE(x, cy0, 30 * sz * isc * pulse, 30 * sz * isc * pulse, hot, 0.5f * life);
            glowE(x, cy0, 12 * sz * isc, 12 * sz * isc, WHITE_, 0.3f * life);
            ringE(x, cy0, 27 * sz * isc, 27 * sz * isc * 0.92f, hot, 0.5f * life * pulse, 0, 6.2832f, 0, 0);          // the Einstein ring
            for (int k = 0; k < 12; k++) {                                                            // shards of the boss, circling in
                float ph = (t * 0.45f + k / 12f) % 1f, an = k * 2.1f + ph * 6f, rr = (46 - 26 * ph) * sz * isc;
                float sx = x + (float) Math.cos(an) * rr, sy = cy0 + (float) Math.sin(an) * rr * 0.6f;
                int c = pal(boss, SPR[boss][(k * 3) % SPR[boss].length].replace(".", "").isEmpty() ? 'X' : SPR[boss][(k * 3) % SPR[boss].length].replace(".", "").charAt(0));
                cv.rect(Math.round(sx - 1), Math.round(sy - 1), Math.round(sx + 1), Math.round(sy + 1), A((float) Math.sin(Math.PI * ph) * life, mixc(c, hot, ph)));
            }
            for (int k = 0; k < 8; k++) { float ph = (t * 0.7f + k / 8f) % 1f, an = k * 0.785f + t * 0.2f, d = 20 * sz * isc + ph * 40 * sz * isc;
                glint(x + (float) Math.cos(an) * d, cy0 + (float) Math.sin(an) * d * 0.62f, 2, k % 2 == 0 ? WHITE_ : 0xFF9AD8, (float) Math.sin(Math.PI * ph) * life); }
        }
    }
}
