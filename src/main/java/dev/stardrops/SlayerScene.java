package dev.stardrops;

import static dev.stardrops.Ritual.*;

/**
 * Slayer, Stardrops: the boss comes, and the sky answers.
 *
 * A floating chunk of the boss's ground sits in a patch of night sky (never the whole screen). The boss arrives its own way,
 * the Revenant clawing up out of the grave, the Tarantula dropping down its thread, Sven bounding out of its den, Voidgloom
 * teleporting in, Inferno rising from the lava, Riftstalker stepping out of a rift, and walks toward you. Then:
 *   NO DROPS (5.5 s): one meteor streaks down and hits it; the boss crumbles to ash and "Nothing dropped" fades in.
 *   A DROP (6 s): a barrage of meteors, then a big one; the boss shatters and the drop rises out of the crater in a beam.
 *   RNG (7 s): a shower that climbs through the rarities (white, blue, purple, gold), a breath of silence while the sky dims,
 *     then a giant golden meteor; a pillar of light, rings and stars, the cinematic bars close in. Pull-the-lever energy.
 */
final class SlayerScene {
    private SlayerScene() {}

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
    private static final int WHITE_ = 0xFFFFFF, VOID = 0x060A1C;

    // ------------------------------------------------------------------ the bosses (front or side view, Minecraft style)
    private static final String[][] SPR = {      // Minecraft model proportions, one sprite pixel per model pixel
        {".......gggggggg.", ".......ggGGGgGh.", ".......gggggGGG.", ".......eEgGMgGG.", "......GgGGgggGg.", "......ghGghGGGG.", ".......MMMGGGGG.", ".......gMhGgGGG.", "..aaaaaaaCCCc...", "..aaaaaaaCccc...", "..MMMMMMMCCCc...", "hhGGGGGGCCCCc...", "hGGGGGGhCCCCc...", "hGGGGGGGCCCCc...", "hgggggggcccGc...", ".........CCCc...", ".........CCCc...", ".........Cgcc...", ".........CCCc...", "..........C.c...", ".........PPPp...", ".........PPPp...", ".........PPPp...", "........PPPppp..", "........PPPppp..", "........PPPppp..", ".......PPP..ppp.", ".......PPP..ppp.", ".......PPP..ppp.", "......PPP....ppp", ".....BBBB....bbb", ".....BBBB....bbb"},
        {"............bssssssssb............", "....l.....l.bbbbbbsbbb.l.....l....", ".lL.LLLlL.LLbbbbsbbbssLL.LlLLL.Ll.", ".L.LL..LLLL.LSsSsSSsSL.LLLL..LL.L.", ".L.L.LLL.LLLLsSssSSsSLLLL.LLL.L.L.", "L..L..L.LL.LLSrSrrsrSLL.LL.L..L..L", "L..L..L..LLL.sRRSsRRS.LLL..L..L..L", "L..L..L..L..LSssSSSSSL..L..L..L..L", "L..L..L..L...SSSSSSSS...L..L..L..L", "L..L..L..L...SSmsSmsS...L..L..L..L", "..L..L..L....sSmkkmSS....L..L..L..", "..L..L..L................L..L..L..", "..L..k..L................L..k..L..", "..k.....k................k.....k.."},
        {"..WW.WW..................w", "..Wp.Wp.................TT", ".WWWWWWW...............TT.", ".WWWwwWrWwWwW..........TT.", ".WWWEEWrWWWWWWWwWwWWW.TT..", "NNWWWWWrwWWwWWWWWWWWWTT...", "WWWWWWWrwWWWWWWWWWWwWT....", "kttWWWWrWWWWWWWWWwWWW.....", ".......rWwWWWWWWWWWWW.....", ".......ywwwwwwwwwwwww.....", ".......rWWWWW...WW.WW.....", "........WW.WW...WW.WW.....", "........WW.WW...WW.WW.....", "........WW.WW...WW.WW.....", "........ww.ww...ww.ww....."},
        {"..XXxXxXXx..", "..XXxXXXxx..", "..XxxXXxXX..", "..xXXXxxXx..", "..PpPXXPpP..", "..xxXxXxXX..", "..XxxXxxXX..", "..XXXxXxXX..", "XX.YYYyYY.XX", "Xx.YyYYyY.XX", ".X.YyYYYY.X.", ".X.YYYYyY.X.", ".X.YYYyyy.x.", ".X.YYYYYY.X.", ".X.YyYYyy.X.", ".X.YyYYYY.x.", ".X.YyYYYy.X.", ".X.YyyYYY.x.", ".X..X..X..X.", ".X..X..X..X.", ".X..X..X..X.", ".x..X..X..X.", ".X..X..X..X.", ".X..X..X..X.", ".X..X..X..X.", ".X..X..X..X.", ".X..X..X..X.", ".X..X..X..X.", ".X..X..X..X.", ".X..X..X..X.", ".X..X..X..X.", ".X..X..X..X.", ".X..X..X..X.", ".X..X..X..X.", "....X..X....", "....X..X...."},
        {"..................", ".oO............oO.", ".OO..ffffffff..OO.", ".OO..YYYYyYyY..OO.", ".OO..yYyYYYfy..OO.", ".Oo..yDDyYDDY..Oo.", ".....yYYyYyyY.....", "oO...YDYYYyDY...oO", "OO...YYDDDDyy...OO", "OO...YYYYyYYY...OO", "OO..............OO", "Oo..............Oo", "...oO........oO...", "...OO.oO..oO.OO...", "...OO.OO..OO.OO...", "...OO.OO..OO.OO...", "...Oo.OO..OO.Oo...", "..oOO.Oo..Oo.OoO..", "..OO..OO.oOO..OO..", "..OO...oOOO...OO..", "..OO...OOOO...OO..", "..Oo...OOOO...Oo..", "..OO...OOOo...OO..", ".......OoOO......."},
        {"......HHHHHHHH......", "......HHHHHHHH......", "......HFFHHFFH......", "......HFfFFFFH......", ".CCC..FRRfFRRf..CCC.", ".CcC..FFFFFFFf..CcC.", ".CcC..FfkkkkFf..CcC.", "CCcCCCFFvFFvFfCCCcCC", "CCMMMMMmMWWmMMmMMMCC", "CcMMMMMMMrrMmMMMMMcC", "CcMMMMMMMrrmMMMMMMcC", "CcMMmMMmmrrMMMMMMMcC", "CcMmMMMMMWWmmMMMMMcC", "CcMMMMMmMWWmmmmMMMcC", "CcMMMMMMMmMmMMMMmMcC", "CcMMmmMMMMMMMmMMMMcC", "CcMMMMMMMmMMMMmMMMcC", "CcmMMMMMmMMmMmMMMMcC", "CcMMMmMmmMMMMMMMMMcC", "CcFFFFMMmmmMmMFFFFcC", "CcccccMMMMMMMMcccccC", "CcccccMMM..MMMcccccC", "CcccccMMM..MMMcccccC", "CcccccMMM..MMMcccccC", "CcccccMMM..MMMcccccC", "CcccccMMM..MMMcccccC", "CcccccMMM..MMMcccccC", "CcccccMMM..MMMcccccC", "CcccccMMM..MMMcccccC", "CCCCCCMMM..MMMCCCCCC", "......KKK..KKK......", "......KKK..KKK......"}};
    private static int pal(int boss, char ch) {
        return switch (boss) {
            case 0 -> switch (ch) { case 'h' -> 0x7AB858; case 'G' -> 0x5E9A3E; case 'g' -> 0x46782C; case 'E' -> 0xFF2A2A; case 'e' -> 0xFFA090; case 'M' -> 0x2A3E1E; case 'a' -> 0x3A6424; case 'C' -> 0x3AAFB0; case 'c' -> 0x2A8A8C; case 'P' -> 0x46468A; case 'p' -> 0x32326A; case 'B' -> 0x3A3A44; case 'b' -> 0x2A2A32; default -> 0x101010; };
            case 1 -> switch (ch) { case 'S' -> 0x3A2A30; case 's' -> 0x5E4A54; case 'b' -> 0x2A1E24; case 'r' -> 0xC81E1E; case 'R' -> 0xFF2A2A; case 'm' -> 0x6A2020; case 'k' -> 0x120808; case 'L' -> 0x4A3A40; case 'l' -> 0x6A5660; default -> 0x101010; };
            case 2 -> switch (ch) { case 'W' -> 0xF2F2F2; case 'w' -> 0xC8CACE; case 'E' -> 0xFF2A2A; case 'N' -> 0x1E1E1E; case 'k' -> 0x5A1010; case 't' -> 0xFFFFFF; case 'r' -> 0xC02030; case 'y' -> 0xFFD040; case 'p' -> 0xD89A9A; case 'T' -> 0xE4E4E8; default -> 0x101010; };
            case 3 -> switch (ch) { case 'X' -> 0x141018; case 'x' -> 0x241A2E; case 'P' -> 0xD040FF; case 'p' -> 0xF4A8FF; case 'Y' -> 0x1C1424; case 'y' -> 0x2A1E36; default -> 0x101010; };
            case 4 -> switch (ch) { case 'Y' -> 0xFFD040; case 'y' -> 0xE8962A; case 'f' -> 0xFFF0A0; case 'D' -> 0x5A2E0A; case 'O' -> 0xFFB040; case 'o' -> 0xC86A10; default -> 0x101010; };
            default -> switch (ch) { case 'H' -> 0x1A0A12; case 'F' -> 0xEADDE2; case 'f' -> 0xC8B4BE; case 'R' -> 0xFF2030; case 'v' -> 0xFFFFFF; case 'k' -> 0x3A0A10; case 'C' -> 0x8A0A1C; case 'c' -> 0x4A0610; case 'M' -> 0x241420; case 'm' -> 0x3A2236; case 'W' -> 0xF0F0F0; case 'r' -> 0xC01030; case 'K' -> 0x0A0A0A; default -> 0x101010; };
        };
    }
    private static boolean glows(int boss, char ch) { return switch (boss) { case 0 -> ch == 'E' || ch == 'e'; case 1 -> ch == 'r' || ch == 'R'; case 2 -> ch == 'E'; case 3 -> ch == 'P' || ch == 'p'; case 4 -> false; default -> ch == 'R'; }; }
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

    // ------------------------------------------------------------------ the timeline (shared with the camera and the sounds)
    /** Meteor impacts (seconds) by type: 0 no drops, 1 a drop, 2 RNG. The last one of a drop / RNG is the big one. */
    static final float[][] HITS = {{3.0f}, {2.45f, 2.62f, 2.78f, 2.92f, 3.04f, 3.14f, 3.36f}, {2.25f, 2.42f, 2.57f, 2.70f, 2.81f, 2.90f, 3.38f}};
    static final float RISE = 0.45f, WALK = 1.45f, WALK_END = 2.5f;
    /** How long a meteor flies before it lands. */
    private static float flight(int type, int i) { int n = HITS[type].length; return type == 0 ? 0.45f : i == n - 1 ? (type == 2 ? 0.55f : 0.42f) : 0.32f; }
    /** The camera's kick from the impacts at time t: 0..~1. */
    static float shake(int type, float t) {
        float s = 0f; float[] h = HITS[type];
        for (int i = 0; i < h.length; i++) { float d = t - h[i]; if (d < 0) continue; float big = (i == h.length - 1 && type > 0) || type == 0 ? 1f : 0.45f; s += big * (float) Math.exp(-d * 9f); }
        return Math.min(1.2f, s);
    }
    /** The rarity each meteor of an RNG shower burns with (white, blue, purple, gold). */
    private static final int[] TIER = {0xEAF2FF, 0x6AB8FF, 0x6AB8FF, 0xB56BFF, 0xB56BFF, 0xFF7AD9, 0xFFD24A};

    // ------------------------------------------------------------------ the scene
    static void paint(float t, float A, int boss, int energy, int rc, boolean rng, boolean nod, float icx, float icy, float sz, float isc,
                      float itemA, float[] offs, float cy0, float bg) {
        final float O = AnimationManager.SL_PIVOT; float p = t - O;
        int type = nod ? 0 : rng ? 2 : 1; float[] hits = HITS[type]; float last = hits[hits.length - 1];
        int hot = mixc(energy, WHITE_, 0.55f);
        float stage = bg * sm(t / 0.4f) * (1f - sm((t - (A - 0.6f)) / 0.6f));
        // the patch of night sky (GPU glows, drawn by Ritual under the pixels), stars and a moon in it
        float gy = icy + 30 * sz, rx = 92 * sz, ry = 22 * sz, span = 150 * sz;
        float dim = rng ? sm((t - 2.95f) / 0.3f) * (1f - sm((t - last) / 0.25f)) : 0f;            // RNG: the sky holds its breath before the big one
        BD_X = icx; BD_Y = icy + 6 * sz; BD_SPAN = span; BD_A = stage * (1f + 0.3f * dim); BD_COL = mixc(energy, 0x1A2A6A, 0.7f);
        sky(t, icx, icy, span, stage * (1f - 0.6f * dim), boss);
        ground(t, boss, icx, gy, rx, ry, sz, stage, energy);
        // where the boss is: its spawn at the back, then walking toward you; scale grows as it comes
        float spawnX = icx + 34 * sz, spawnY = gy - 9 * sz, frontX = icx - 4 * sz, frontY = gy + 9 * sz;
        float w = sm((t - WALK) / (WALK_END - WALK)), bx = lerp(spawnX, frontX, w), by = lerp(spawnY, frontY, w), bs = lerp(0.85f, 1.55f, w) * sz;
        prop(t, boss, spawnX, spawnY, sz, stage, energy, true);                                     // the grave, the den, the lava... behind the boss
        // meteors in flight and their impacts
        float hitFlash = 0f;
        for (int i = 0; i < hits.length; i++) {
            float tl = hits[i] - flight(type, i), big = (type == 0 || i == hits.length - 1) ? 1f : 0f;
            float aimX = frontX + (big > 0 ? 0 : (rnd(i * 31 + 3) - 0.5f) * 30 * sz), aimY = frontY - (big > 0 ? 8 * sz : rnd(i * 17) * 6 * sz);
            if (t < hits[i] && t >= tl) meteor((t - tl) / (hits[i] - tl), aimX, aimY, i, type, big, sz, stage, energy);
            float d = t - hits[i];
            if (d >= 0 && d < 1.4f) impact(d, aimX, aimY, i, type, big, sz, stage, energy, hot);
            if (d >= 0 && d < 0.1f && big > 0) hitFlash = Math.max(hitFlash, 1f - d / 0.1f);
            else if (d >= 0 && d < 0.06f) hitFlash = Math.max(hitFlash, 0.5f * (1f - d / 0.06f));
        }
        // the boss
        float end = type == 0 ? last : last;                                                        // the hit that ends it
        float gone = sm((t - end) / (type == 0 ? 0.9f : 0.6f));
        if (gone < 1f) boss(t, boss, bx, by, bs, stage, energy, hot, w, hitFlash, t >= end ? (t - end) : -1f, type);
        prop(t, boss, spawnX, spawnY, sz, stage, energy, false);                                    // anything in front of it (the lava's lip, the web's thread)
        if (p < 0 || type == 0) return;
        // the drop rises out of the crater in a beam; RNG: a golden pillar, rings and stars
        float beam = sm(p / 0.25f) * (1f - sm((t - (A - 0.8f)) / 0.6f));
        for (float off : offs) {
            float cx = icx + off;
            if (rng) {
                float pil = beam * (0.7f + 0.3f * (float) Math.sin(t * 9f));
                stroke(cx, frontY, cx, icy - span * 0.75f, 9 * sz * pil, 0xFFD24A, 0.18f * pil); stroke(cx, frontY, cx, icy - span * 0.75f, 3.2f * sz * pil, 0xFFF4C8, 0.55f * pil);
                for (int k = 0; k < 3; k++) { float u = ((p * 0.9f + k / 3f) % 1f); ringE(cx, frontY - u * 20 * sz, (18 + 40 * u) * sz, (5 + 11 * u) * sz, 0xFFD24A, (1f - u) * 0.8f * beam, 0, 6.2832f, 0, 0); }
                for (int k = 0; k < 10; k++) { float an = t * 0.8f + k * 0.628f, rr = (34 + 6 * (float) Math.sin(t * 3 + k)) * sz * isc; glint(cx + (float) Math.cos(an) * rr, cy0 + (float) Math.sin(an) * rr * 0.45f, k % 3 == 0 ? 2 : 1, k % 2 == 0 ? 0xFFF4C8 : 0xFFD24A, beam); }
                for (int k = 0; k < 26; k++) { float u = ((p * 0.45f + rnd(k * 7) ) % 1f); dot(cx + (rnd(k * 13) - 0.5f) * 70 * sz, frontY - u * 110 * sz, k % 4 == 0 ? 0xFF7AD9 : k % 3 == 0 ? 0x6AB8FF : 0xFFE9A0, (1f - u) * beam); }
            } else {
                stroke(cx, frontY, cx, cy0, 5 * sz * beam, energy, 0.25f * beam); stroke(cx, frontY, cx, cy0, 1.6f * sz * beam, hot, 0.6f * beam);
                for (int k = 0; k < 6; k++) { float an = t * 1.6f + k * 1.047f; glint(cx + (float) Math.cos(an) * 22 * sz * isc, cy0 + (float) Math.sin(an) * 8 * sz * isc, (float) Math.sin(an) > 0 ? 2 : 1, k % 2 == 0 ? WHITE_ : hot, itemA); }
            }
            glowE(cx, cy0, 24 * sz * isc, 24 * sz * isc, rng ? 0xFFD24A : energy, 0.4f * itemA); glowE(cx, cy0, 10 * sz * isc, 10 * sz * isc, WHITE_, 0.22f * itemA);
        }
    }

    /** Stars and a moon in the patch of sky. */
    private static void sky(float t, float icx, float icy, float span, float a, int boss) {
        if (a < 0.02f) return;
        for (int i = 0; i < 70; i++) {
            float an = rnd(i * 7 + 1) * 6.2832f, d = (float) Math.sqrt(rnd(i * 13 + 2)) * span * 0.95f;
            float x = icx + (float) Math.cos(an) * d * 1.4f, y = icy - 10 * (span / 150f) + (float) Math.sin(an) * d * 0.62f;
            float tw = 0.5f + 0.5f * (float) Math.sin(t * (1.5f + rnd(i) * 2) + i), edge = 1f - cl((d / (span * 0.95f) - 0.7f) / 0.3f);
            dot(x, y, i % 9 == 0 ? 0xFFE0B0 : i % 5 == 0 ? 0xB8D0FF : WHITE_, a * edge * (0.35f + 0.65f * tw));
        }
        float mx = icx - span * 0.62f, my = icy - span * 0.32f, mr = span * 0.06f;               // the moon
        glowE(mx, my, mr * 3f, mr * 3f, 0xDDE6FF, 0.25f * a); fillE(mx, my, mr, mr, 0xE8ECF8, a); fillE(mx + mr * 0.35f, my - mr * 0.15f, mr * 0.28f, mr * 0.28f, 0xC8CEE0, a); fillE(mx - mr * 0.3f, my + mr * 0.35f, mr * 0.2f, mr * 0.2f, 0xC8CEE0, a);
    }

    private static final int[][] SOIL = {{0x3E5A2A, 0x4A6A32, 0x5A3E28}, {0x3A3A3E, 0x4A4A50, 0x2E2A2E}, {0x4A7A30, 0x5E8E3E, 0x6A4A30},
        {0xDCD8A0, 0xC8C48C, 0x9A9670}, {0x7A2A22, 0x8E3A2E, 0x5A1A16}, {0x3A1E3A, 0x4A2A48, 0x24122A}};
    /** The floating chunk of ground: a block-textured top and a cliff edge under it, in the boss's own ground. */
    private static void ground(float t, int boss, float cx, float gy, float rx, float ry, float sz, float a, int energy) {
        if (a < 0.02f) return;
        int[] s = SOIL[boss]; int bk = Math.max(2, Math.round(4 * sz));
        // the cliff: the front half of the ellipse, dropping in blocky columns
        for (int x = Math.round(cx - rx); x <= Math.round(cx + rx); x += bk) {
            float u = (x - cx) / rx; if (Math.abs(u) > 1) continue; float top = gy + ry * (float) Math.sqrt(1 - u * u);
            float depth = (14 + 10 * rnd(x * 7)) * sz * (float) Math.sqrt(1 - u * u) + 4 * sz;
            for (float y = top - bk; y < top + depth; y += bk) {
                int col = y < top ? s[2] : (rnd(x * 31 + Math.round(y) * 7) < 0.3f ? 0x4A4A52 : mixc(s[2], 0x000000, 0.35f + 0.4f * (y - top) / depth));
                cv.rect(x, Math.round(y), x + bk, Math.round(y) + bk, A(a, col));
            }
        }
        // the top: a block grid with grain, lighter at the back
        for (int y = Math.round(gy - ry); y <= Math.round(gy + ry); y += bk) {
            float v = (y - gy) / ry; if (Math.abs(v) > 1) continue; float hw = rx * (float) Math.sqrt(1 - v * v);
            for (int x = Math.round(cx - hw); x < Math.round(cx + hw); x += bk) {
                float n = rnd(x * 13 + y * 101); int col = n < 0.33f ? s[0] : n < 0.8f ? s[1] : mixc(s[1], 0xFFFFFF, 0.12f);
                cv.rect(x, y, x + bk, y + bk, A(a, mixc(col, 0x000000, 0.25f * (1f - v) * 0.5f + 0.1f)));
            }
        }
        glowE(cx, gy, rx * 1.1f, ry * 1.6f, energy, 0.08f * a);
    }

    /** The boss's spawn: back = drawn behind it, front = anything over it. */
    private static void prop(float t, int boss, float x, float y, float sz, float a, int energy, boolean back) {
        if (a < 0.02f) return;
        float k = sz;
        switch (boss) {
            case 0 -> {                                                                              // a gravestone with a cross, a mound of dirt the Revenant claws out of
                if (!back) { float dug = sm((t - RISE) / 0.5f); for (int i = 0; i < 9; i++) { float u = ((t * 1.3f + i / 9f) % 1f); if (t > RISE && t < WALK + 0.2f) dot(x + (rnd(i * 5) - 0.5f) * 16 * k, y - u * 10 * k * dug, 0x5A3E28, (1f - u) * a); } return; }
                int sx0 = Math.round(x - 7 * k), sx1 = Math.round(x + 7 * k), sy0 = Math.round(y - 26 * k), sy1 = Math.round(y - 4 * k);
                cv.rect(sx0, sy0 + 2, sx1, sy1, A(a, 0x8A8E96)); cv.rect(sx0 + 2, sy0, sx1 - 2, sy0 + 2, A(a, 0x8A8E96)); cv.rect(sx1 - 3, sy0 + 2, sx1, sy1, A(a, 0x6A6E78)); cv.rect(sx0, sy0 + 2, sx0 + 2, sy1, A(a, 0xA8ACB4));
                cv.rect(Math.round(x - 0.8f * k), Math.round(y - 22 * k), Math.round(x + 0.8f * k), Math.round(y - 12 * k), A(a, 0x4A4E58)); cv.rect(Math.round(x - 4 * k), Math.round(y - 19 * k), Math.round(x + 4 * k), Math.round(y - 17.4f * k), A(a, 0x4A4E58));
                fillE(x, y, 13 * k, 4 * k, 0x5A3E28, a); fillE(x - 2 * k, y - 1 * k, 9 * k, 2.5f * k, 0x6E4E32, a);
            }
            case 1 -> {                                                                              // a web strung across the top, a thread the Tarantula drops down
                if (!back) return;
                float cy = y - 62 * k;
                for (int i = 0; i < 8; i++) { float an = i * 0.785f; stroke(x, cy, x + (float) Math.cos(an) * 26 * k, cy + (float) Math.sin(an) * 16 * k, 0.5f, 0xD8DCE8, 0.55f * a); }
                for (int r = 1; r <= 3; r++) ringE(x, cy, r * 8 * k, r * 5 * k, 0xD8DCE8, 0.4f * a, 0, 6.2832f, 0, 3);
            }
            case 2 -> {                                                                              // a rocky den the wolf bounds out of
                if (!back) return;
                fillE(x + 4 * k, y - 8 * k, 20 * k, 13 * k, 0x5A5E66, a); fillE(x + 4 * k, y - 10 * k, 17 * k, 11 * k, 0x6E727C, a); fillE(x + 2 * k, y - 4 * k, 9 * k, 8 * k, 0x101014, a);
                for (int i = 0; i < 5; i++) dot(x - 14 * k + i * 7 * k, y - 18 * k + rnd(i) * 4 * k, 0x3A5A2A, a);
            }
            case 3 -> {                                                                              // an end portal frame ring, ender particles spiralling up
                if (!back) { for (int i = 0; i < 14; i++) { float u = ((t * 0.8f + i / 14f) % 1f), an = i * 0.9f + t * 2f; dot(x + (float) Math.cos(an) * 10 * k * (1 - u), y - 4 * k - u * 34 * k, 0xD060FF, (1f - u) * a * (t < WALK + 0.4f ? 1f : 0.3f)); } return; }
                fillE(x, y, 15 * k, 4.5f * k, 0x2A5A4A, a); fillE(x, y, 11 * k, 3 * k, 0x0A1414, a);
                for (int i = 0; i < 8; i++) { float an = i * 0.785f; dot(x + (float) Math.cos(an) * 13 * k, y + (float) Math.sin(an) * 3.8f * k, 0x7AE8C8, a); }
            }
            case 4 -> {                                                                              // a lava pool the blaze rises out of
                float pulse = 0.5f + 0.5f * (float) Math.sin(t * 4f);
                if (back) { fillE(x, y, 17 * k, 5 * k, 0x7A2A10, a); fillE(x, y, 15 * k, 4 * k, mixc(0xFF6A10, 0xFFB030, pulse), a); glowE(x, y - 6 * k, 22 * k, 12 * k, 0xFF7A20, 0.35f * a);
                    for (int i = 0; i < 4; i++) { float u = ((t * 0.9f + i * 0.25f) % 1f); dot(x + (rnd(i * 3) - 0.5f) * 22 * k, y - u * 4 * k, 0xFFE07A, (1 - u) * a); } }
                else fillE(x, y + 2 * k, 17 * k, 2.2f * k, 0x7A2A10, a);
            }
            default -> {                                                                             // a rift: a glowing vertical tear the vampire steps out of
                if (!back) return;
                float op = sm((t - 0.2f) / 0.5f) * (1f - sm((t - WALK - 0.6f) / 0.5f));
                if (op <= 0.01f) return;
                glowE(x, y - 16 * k, 10 * k * op, 24 * k, 0xFF2050, 0.5f * a * op);
                for (int i = 0; i < 12; i++) { float u = i / 11f; float jx = (rnd(i * 9) - 0.5f) * 5 * k * op; stroke(x + jx, y - u * 34 * k, x + (rnd(i * 9 + 9) - 0.5f) * 5 * k * op, y - (u + 0.09f) * 34 * k, 1.6f * k * op, 0x200010, a); }
                stroke(x, y, x, y - 34 * k, 0.6f, 0xFF8AB0, a * op);
            }
        }
    }

    /** One meteor on its way: a burning head, a long trail, sparks shed behind. u: 0..1 of its flight. */
    private static void meteor(float u, float tx, float ty, int i, int type, float big, float sz, float a, int energy) {
        float dx = 80 + 60 * rnd(i * 7 + 1), dy = -140;
        if (big > 0) { dx = 110; dy = -175; }
        float sx = tx + dx * sz, sy = ty + dy * sz, e = u * u * (0.6f + 0.4f * u);                // it speeds up as it falls
        float x = lerp(sx, tx, e), y = lerp(sy, ty, e);
        int col = type == 2 ? TIER[Math.min(i, TIER.length - 1)] : big > 0 ? 0xFFB040 : mixc(0xFF8A30, energy, 0.25f);
        if (type == 2 && big > 0) col = 0xFFD24A;
        float r = (big > 0 ? (type == 2 ? 11f : 8.5f) : 4.8f) * sz;
        // the trail
        int seg = 22; float px = x, py = y;
        for (int k = 1; k <= seg; k++) {
            float uu = Math.max(0f, u - k * 0.035f), ee = uu * uu * (0.6f + 0.4f * uu), qx = lerp(sx, tx, ee), qy = lerp(sy, ty, ee), f = 1f - k / (float) seg;
            int tc = type == 2 && big > 0 ? rainbow(k / (float) seg + u) : mixc(col, 0xFF4A20, 1f - f);
            stroke(px, py, qx, qy, r * 2.4f * f + 0.6f, tc, 0.22f * f * a); stroke(px, py, qx, qy, r * 1.4f * f + 0.4f, tc, 0.8f * f * a); stroke(px, py, qx, qy, r * 0.5f * f + 0.3f, mixc(tc, WHITE_, 0.6f), 0.8f * f * a); px = qx; py = qy;
        }
        glowE(x, y, r * 4.5f, r * 4.5f, col, 0.45f * a); glowE(x, y, r * 2f, r * 2f, mixc(col, WHITE_, 0.4f), 0.5f * a); fillE(x, y, r, r, mixc(col, WHITE_, 0.3f), a); fillE(x - r * 0.25f, y - r * 0.25f, r * 0.5f, r * 0.5f, WHITE_, a);
        for (int k = 0; k < 12; k++) { float v = ((u * 3f + k / 6f) % 1f); dot(x + (dx * 0.25f * v + (rnd(i * 11 + k) - 0.5f) * 6) * sz, y + (dy * 0.25f * v) * sz, k % 2 == 0 ? 0xFFE07A : col, (1f - v) * a); }
    }
    private static int rainbow(float u) { int[] r = {0xFF5A5A, 0xFFB04A, 0xFFE04A, 0x6AE87A, 0x6AB8FF, 0xB56BFF}; float v = ((u % 1f) + 1f) % 1f * r.length; int i = (int) v; return mixc(r[i % r.length], r[(i + 1) % r.length], v - i); }

    /** What a meteor does when it lands: a flash, a shock ring on the ground, rock flung up, a glowing crater. d: seconds since. */
    private static void impact(float d, float x, float y, int i, int type, float big, float sz, float a, int energy, int hot) {
        int col = type == 2 ? (big > 0 ? 0xFFD24A : TIER[Math.min(i, TIER.length - 1)]) : 0xFF9A3A;
        float s = big > 0 ? (type == 2 ? 2.3f : 1.7f) : 0.95f;
        float fl = Math.max(0f, 1f - d / 0.22f);
        if (fl > 0) { glowE(x, y - 4 * sz, 30 * sz * s, 22 * sz * s, WHITE_, 0.8f * fl * a); glowE(x, y, 50 * sz * s, 26 * sz * s, col, 0.5f * fl * a); }
        float rw = d / 0.6f; if (rw < 1f) { ringE(x, y, (6 + 46 * eo(rw)) * sz * s, (2 + 12 * eo(rw)) * sz * s, mixc(col, WHITE_, 0.4f), (1f - rw) * a, 0, 6.2832f, 0, 0); }
        for (int k = 0; k < (big > 0 ? 18 : 8); k++) {                                              // rock and embers thrown up and falling back
            float an = (rnd(i * 37 + k) * 0.8f + 0.1f) * 3.1416f, v = (30 + 50 * rnd(i * 13 + k)) * sz * s, tt = Math.min(d, 1.2f);
            float qx = x + (float) Math.cos(an) * v * tt * 0.9f, qy = y - (float) Math.sin(an) * v * tt + 90 * sz * tt * tt;
            if (qy > y + 6 * sz) continue;
            int pc = k % 3 == 0 ? 0xFFE07A : k % 3 == 1 ? col : 0x5A4A40;
            cv.rect(Math.round(qx), Math.round(qy), Math.round(qx) + (k % 2 == 0 ? 2 : 1), Math.round(qy) + (k % 2 == 0 ? 2 : 1), A(a * (1f - tt / 1.2f), pc));
        }
        float cr = Math.max(0f, 1f - d / 1.4f);                                                     // the crater glows and cools
        fillE(x, y + 1 * sz, 9 * sz * s, 2.6f * sz * s, 0x1A1010, 0.8f * a * Math.min(1f, d * 8));
        glowE(x, y, 10 * sz * s, 3.5f * sz * s, col, 0.5f * cr * a);
        for (int k = 0; k < 4; k++) { float u = ((d * 0.8f + k / 4f) % 1f); dot(x + (rnd(i * 5 + k) - 0.5f) * 10 * sz * s, y - u * 18 * sz, 0x9A9AA6, (1f - u) * 0.6f * cr * a); }   // smoke
    }

    /**
     * The boss at feet (fx, fy), k canvas units per sprite pixel. Arrives its own way, walks with a bob and a rock, flashes white on
     * each hit, and after the last hit (since >= 0) crumbles to ash (no drops) or shatters (a drop / RNG).
     */
    private static void boss(float t, int boss, float fx, float fy, float k, float a, int energy, int hot, float walk, float flash, float since, int type) {
        String[] g = SPR[boss]; int h = g.length, w = g[0].length();
        float bw = w * k, bh = h * k;
        float arrive = sm((t - RISE) / 0.85f);
        float ox = 0f, oy = 0f, alpha = a, clipY = Float.MAX_VALUE;                                   // clipY: pixels below this line are still in the ground
        switch (boss) {
            case 0 -> { oy = (1f - arrive) * bh * 1.02f; clipY = fy; ox = (float) Math.sin(t * 30f) * (arrive < 1 && arrive > 0 ? 0.8f * k : 0); }      // clawing up out of the grave
            case 1 -> { oy = -(1f - arrive) * 64 * k; }                                             // down the thread
            case 2 -> { ox = (1f - arrive) * 22 * k; alpha *= Math.min(1f, arrive * 3f); }           // out of the den
            case 3 -> { float fl = arrive < 1f ? (float) (0.5 + 0.5 * Math.sin(t * 60)) : 1f; alpha *= arrive * fl; }   // teleporting in
            case 4 -> { oy = (1f - arrive) * bh * 0.9f; clipY = fy; }                                 // rising from the lava
            default -> { ox = (1f - arrive) * 10 * k; alpha *= arrive; }                              // stepping out of the rift
        }
        if (arrive <= 0f || alpha < 0.02f) return;
        if (boss == 1 && arrive > 0) stroke(fx + ox, fy - bh + oy - 70 * k, fx + ox, fy - bh + oy, 0.5f, 0xD8DCE8, 0.7f * a * (1f - walk));
        float bob = walk > 0 && walk < 1 ? Math.abs((float) Math.sin(t * 11f)) * 1.6f * k : 0f;      // walking: bob and rock
        float rock = walk > 0 && walk < 1 ? (float) Math.sin(t * 11f) * 0.05f : 0f;
        float x0 = fx + ox - bw / 2f, y0 = fy + oy - bh - bob;
        boolean dying = since >= 0f;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            char ch = g[y].charAt(x); if (ch == '.') continue;
            float lx = x0 + x * k + (h - y) * k * rock, ly = y0 + y * k, al = alpha;
            if (ly + k > clipY + 0.5f) continue;
            int col = shaded(boss, x, y);
            if (flash > 0) col = mixc(col, WHITE_, flash * 0.85f);
            if (dying) {
                float r = rnd(boss * 997 + y * 61 + x * 7);
                if (type == 0) {                                                                    // crumbling to ash: drifting up, greying, going out row by row from the top
                    float dly = (y / (float) h) * 0.4f, q = cl((since - dly * (1f - r * 0.3f)) / 0.6f);
                    if (q > 0) { col = mixc(col, 0x5A5A62, cl(q * 2)); lx += (r - 0.5f) * 12 * k * q + (float) Math.sin(since * 5 + r * 6) * 2 * k * q; ly -= q * (10 + 18 * r) * k; al *= 1f - q; }
                } else {                                                                            // shattering: flung out from the middle
                    float q = cl(since / 0.55f), an = r * 6.2832f, sp = (20 + 40 * rnd(x * 13 + y)) * k;
                    lx += ((x - w / 2f) * 0.8f * k + (float) Math.cos(an) * 4 * k) * q * 3 + (float) Math.cos(an) * sp * q; ly += (float) Math.sin(an) * sp * q * 0.7f - 10 * k * q; al *= 1f - q; col = mixc(col, hot, q);
                }
                if (al < 0.02f) continue;
            }
            int X0 = Math.round(lx), Y0 = Math.round(ly), X1 = Math.round(lx + k), Y1 = Math.round(ly + k);
            if (X1 <= X0) X1 = X0 + 1; if (Y1 <= Y0) Y1 = Y0 + 1;
            cv.rect(X0, Y0, X1, Y1, A(al, col));
            if (glows(boss, ch) && !dying) glowE(lx + k / 2, ly + k / 2, 2.5f * k, 2.5f * k, col, 0.4f * al);
        }
        if (!dying) { fillE(fx, fy + 1, bw * 0.45f, 1.6f * k, 0x000000, 0.35f * alpha * arrive); }     // its shadow
    }
}
