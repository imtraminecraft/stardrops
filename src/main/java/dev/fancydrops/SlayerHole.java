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
    static final String[] NAMES = {"Revenant Horror", "Tarantula Broodfather", "Sven Packmaster", "Voidgloom Seraph", "Inferno Demonlord", "Riftstalker Bloodfiend"};
    private static final int WHITE_ = 0xFFFFFF, VOID = 0x04030C;

    // ------------------------------------------------------------------ the bosses (front or side view, Minecraft style)
    private static final String[][] SPR = {
        {"..gGGGGGGg..", "..GGGGGGGG..", "..GRRGGRRG..", "..GGGggGGG..", "..GgGGGGgG..", "..GgEggEgG..", "..GGGGGGGG..", "..gGGGGGGg..",
         "GGAAAAAAAAGG", "GGAaAAAAaAGG", "GgAAAAAAAAgG", "GGAAaAAaAAGG", "GgAAAAAAAAgG", "GGAAAAAAAAGG", "gGaaaaaaaagG",
         "..BBBBBBBB..", "..BBB..BBB..", "..BBB..BBB..", "..BBB..BBB..", "..BBB..BBB..", "..KKK..KKK..", "..KKK..KKK.."},
        {"L..............L", ".L....SSSS....L.", "..L..SSSSSS..L..", "LLLLSRRSSRRSLLLL", "...LSSSSSSSSL...", "LLLLSSrSSrSSLLLL", "..L.SSSSSSSS.L..", ".L...SSSSSS...L.", "L.....SSSS.....L"},
        {"..WW..............", ".WWWW.............", "WWEWWWWWWWWWWWW..T", "NWWWWWWWWWWWWWWWTT", ".WWWWWWWWWWWWWWW.T", "...WwwwwwwwwwwW...", "...WW.WW..WW.WW...", "...WW.WW..WW.WW...", "...GG.GG..GG.GG..."},
        {"XXXXXXXX", "XXXXXXXX", "XXXXXXXX", "XXXXXXXX", "XPPXXPPX", "XXXXXXXX", "XXXXXXXX", "XXXXXXXX",
         "X.YYYY.X", "X.YYYY.X", "X.YYYY.X", "X.YYYY.X", "X.YYYY.X", "X.YYYY.X", "X.YYYY.X", "X..YY..X",
         "X.Y..Y.X", "X.Y..Y.X", "X.Y..Y.X", "X.Y..Y.X", "..Y..Y..", "..Y..Y..", "..Y..Y..", "..Y..Y..", "..Y..Y.."},
        {".O........O.", ".O..YYYY..O.", "...YYYYYY...", "O.YYYYYYYY.O", "O.YDDYYDDY.O", "..YYYYYYYY..", "..YYDDDDYY..", "...YYYYYY...",
         ".O..YYYY..O.", ".O........O.", "....O..O....", "....O..O....", "...O....O..."},
        {"..HHHHHHHH..", "..HHHHHHHH..", "..FFFFFFFF..", "..FRRFFRRF..", "..FFFFFFFF..", "..FFvFFvFF..", "..FFFFFFFF..", "...FFFFFF...",
         "CCCMMMMMMCCC", "CCCMMMMMMCCC", "CCCMMrrMMCCC", "CCCMMMMMMCCC", "CCCMMMMMMCCC", "CCCMMMMMMCCC", "CCC.MMMM.CCC", "CCC.MMMM.CCC",
         "CC..MM.MM.CC", "C...MM.MM..C", "....MM.MM...", "....MM.MM...", "....KK.KK..."}};
    private static int pal(int boss, char ch) {
        return switch (boss) {
            case 0 -> switch (ch) { case 'G' -> 0x5A9A3A; case 'g' -> 0x3E7A2A; case 'R' -> 0xFF3030; case 'A' -> 0x7AD8E0; case 'a' -> 0x3A8A9A; case 'B' -> 0x3A3A8A; case 'K' -> 0x2A2A30; default -> 0x101010; };
            case 1 -> switch (ch) { case 'S' -> 0x2E1A20; case 'r' -> 0xC02020; case 'R' -> 0xFF2A2A; default -> 0x4A3238; };
            case 2 -> switch (ch) { case 'W' -> 0xE8ECF4; case 'w' -> 0xB8BCC8; case 'E' -> 0xFF3030; case 'N' -> 0x202020; case 'T' -> 0xD8DCE8; default -> 0x9A9EA8; };
            case 3 -> switch (ch) { case 'X' -> 0x160C20; case 'Y' -> 0x24143A; default -> 0xE070FF; };
            case 4 -> switch (ch) { case 'Y' -> 0xFFC830; case 'D' -> 0x5A2A0A; default -> 0xFF8A20; };
            default -> switch (ch) { case 'H' -> 0x1A0A10; case 'F' -> 0xE8DCE0; case 'R' -> 0xFF2030; case 'v' -> 0xFFFFFF; case 'C' -> 0x7A0A18; case 'M' -> 0x241420; case 'r' -> 0xFF3040; default -> 0x0A0A0A; };
        };
    }
    private static boolean glows(int boss, char ch) { return ch == 'R' || ch == 'E' && boss == 2 || ch == 'P' || ch == 'r' && boss == 5; }

    // ------------------------------------------------------------------ the scene
    static void paint(float t, float A, int boss, int energy, int rc, boolean rng, boolean nod, float icx, float icy, float sz, float isc,
                      float itemA, float[] offs, float cy0, float bg) {
        final float O = AnimationManager.SL_PIVOT; float p = t - O;
        int hot = mixc(energy, WHITE_, 0.55f);
        // the hole: opens, holds through the meal, then (by type) spits, collapses or swallows everything
        float open = eob((t - 0.1f) / 0.7f);
        float gulp = 0f; for (float g0 : AnimationManager.SL_GULPS) { float u = (t - g0) / 0.35f; if (u > 0 && u < 1) gulp = Math.max(gulp, (float) Math.sin(Math.PI * u)); }
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
        float px = (w > 14 ? 4.4f : 5.0f) * sz * (h > 24 ? 0.8f : 1f), bw = w * px, bh = h * px;
        float mx = hx + rh0 * 3.4f + bw * 0.5f + 4 * sz, my = hy;                                      // its centre, to the right of the hole
        float beam = cl((t - 0.45f) / 0.6f);                                                         // beamed in, top to bottom
        float suck = 0f; for (int k = 0; k < 3; k++) suck += sm((t - AnimationManager.SL_GULPS[k] + 0.25f) / 0.55f) / 3f;   // three gulps
        suck = cl(suck * 1.05f + sm((t - 1.3f) / 2.1f) * 0.15f);
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
                float q = cl((suck * 1.7f - ord * 0.7f) / 1.0f); q = q * q * (3 - 2 * q);
                int col = pal(boss, ch);
                float al = a * rowIn;
                if (q <= 0.001f) {
                    float sc = rowIn < 1f ? 0.6f : 1f;
                    cv.rect(Math.round(X - px / 2), Math.round(Y - px / 2), Math.round(X - px / 2 + px * sc), Math.round(Y - px / 2 + px), A(al, rowIn < 1f ? mixc(col, hot, 0.7f) : col));
                    if (glows(boss, ch)) glowE(X, Y, 3 * sz, 3 * sz, col, 0.45f * al);
                    continue;
                }
                // torn off: spiralling in, stretched along its path, heating up, swallowed at the horizon
                float ang0 = (float) Math.atan2(Y - hy, X - hx), r0 = (float) Math.hypot(X - hx, Y - hy);
                float rr = rh0 * 0.9f + (r0 - rh0 * 0.9f) * (float) Math.pow(1f - q, 1.4f), ang = ang0 + q * q * 3.6f;
                if (rr < rh0 * 1.0f && q > 0.85f) continue;
                float sx = hx + (float) Math.cos(ang) * rr, sy = hy + (float) Math.sin(ang) * rr * (1f - 0.7f * q);
                float pa = ang - 0.35f * q, pr = rr * (1f + 0.25f * q);
                float tx = hx + (float) Math.cos(pa) * pr, ty = hy + (float) Math.sin(pa) * pr * (1f - 0.7f * q);
                int c2 = mixc(col, hot, 0.85f * q);
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
