package dev.stardrops;

import java.util.List;

import dev.stardrops.AnimationManager.Anim;
import dev.stardrops.AnimationManager.Item;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The Mythological animations (Mythological Mobs and Treasure), all three types. One timeline, S seconds long (5 to 7, set per animation):
 *
 *   0 .. S-2.5    the OPENING, identical for No Drops, Normal and RNG.
 *                 Mobs: a ritual circle draws itself on the ground, three lightning strikes crack the ground, then the power is drawn in.
 *                 Treasure: a spade digs three times, a light grows out of the hole, then the light is drawn in.
 *   S-2.5 .. S    the PIVOT, the part that tells the three apart:
 *                 No Drops  the charge fizzles, the circle goes out, smoke and embers (a bone for treasure), grey words.
 *                 Normal    ascension rings and a burst lift the item out of the ground, it glows and sparkles, no backdrop.
 *                 RNG       white flash, shockwaves, god rays, lightning round the item, runes, a fountain of light, bars and blur.
 *
 * For all three: the item appears first, then the background fades while the words show; at the end the item fades slowly and
 * 0.5 s later the words fade. Everything is drawn into a small pixel canvas (one textured quad), then the item and the words on top.
 */
final class Ritual {
    private Ritual() {}

    static final int CW = 640, CH = 360;
    /** Canvas units per old canvas unit: the scene canvas has twice the detail of the first 320 x 180 one, every size is written in the old units. */
    static final float K = CW / 320f;
    private static final Canvas main = new Canvas(CW, CH), sil = new Canvas(CW, CH);
    static Canvas cv = main;                                                    // what the drawing helpers paint on

    private static final int CYAN = 0x7FE8FF, GOLD = 0xFFD25A, EMBER = 0xFF8A3A, WHITE = 0xFFFFFF, GREY = 0x6A7388;
    private static final float[] POS = {0.30f, 0.46f, 0.66f};

    // ------------------------------------------------------------------ small maths
    static float cl(float v) { return v < 0 ? 0 : v > 1 ? 1 : v; }
    static float sm(float v) { v = cl(v); return v * v * (3 - 2 * v); }
    static float eo(float v) { v = cl(v); float q = 1 - v; return 1 - q * q * q; }
    static float eob(float v) { v = cl(v); float c1 = 1.9f, c3 = c1 + 1, q = v - 1; return 1 + c3 * q * q * q + c1 * q * q; }
    static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    static float rnd(int n) { n ^= n >>> 16; n *= 0x7feb352d; n ^= n >>> 15; n *= 0x846ca68b; n ^= n >>> 16; return (n & 0xFFFFFF) / 16777216f; }
    static int A(float a, int rgb) { return Canvas.rgb(Math.round(cl(a) * 255), rgb); }
    static int mixc(int a, int b, float f) { return Canvas.mix(0xFF000000 | a, 0xFF000000 | b, f) & 0xFFFFFF; }

    // ------------------------------------------------------------------ pixel drawing
    static void dot(float x, float y, int rgb, float a) { if (a > 0.01f) cv.set(Math.round(x), Math.round(y), A(a, rgb)); }
    static void line(float x0, float y0, float x1, float y1, int rgb, float a) {
        int n = (int) Math.max(1, Math.ceil(Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0))));
        for (int i = 0; i <= n; i++) { float f = i / (float) n; dot(x0 + (x1 - x0) * f, y0 + (y1 - y0) * f, rgb, a); }
    }
    /** A smooth, tapered, anti-aliased cut from (x0,y0) to (x1,y1): w is the half-width in the middle, the ends come to a point (one straight clean line, never a zig-zag). */
    static void stroke(float x0, float y0, float x1, float y1, float w, int rgb, float a) {
        if (a <= 0.01f) return;
        float dx = x1 - x0, dy = y1 - y0, len2 = Math.max(0.0001f, dx * dx + dy * dy), reach = w + 1f;
        int ax = (int) Math.floor(Math.min(x0, x1) - reach), bx = (int) Math.ceil(Math.max(x0, x1) + reach), ay = (int) Math.floor(Math.min(y0, y1) - reach), by = (int) Math.ceil(Math.max(y0, y1) + reach);
        for (int y = ay; y <= by; y++) for (int x = ax; x <= bx; x++) {
            float px = x + 0.5f - x0, py = y + 0.5f - y0, t = cl((px * dx + py * dy) / len2);
            float qx = px - dx * t, qy = py - dy * t, d = (float) Math.sqrt(qx * qx + qy * qy);
            float half = w * (float) Math.sqrt(Math.sin(Math.PI * t));                              // thickest in the middle, pointed at both ends
            float cov = cl(half + 0.5f - d); if (cov <= 0.02f) continue;
            cv.set(x, y, A(a * cov, rgb));
        }
    }
    /** An arc of an ellipse. from..to in radians (a full circle is 0..2pi), rot turns it, dash > 0 breaks it into dashes. */
    static void ringE(float cx, float cy, float rx, float ry, int rgb, float a, float from, float to, float rot, int dash) {
        int steps = Math.max(24, (int) (6.3f * Math.max(rx, ry) * 1.3f));
        for (int i = 0; i <= steps; i++) {
            float ang = lerp(from, to, i / (float) steps);
            if (dash > 0 && (i / dash) % 2 == 1) continue;
            float cs = (float) Math.cos(ang + rot), sn = (float) Math.sin(ang + rot);
            dot(cx + cs * rx, cy + sn * ry, rgb, a);
        }
    }
    static void fillE(float cx, float cy, float rx, float ry, int rgb, float a) {
        if (rx < 0.5f || ry < 0.5f) return;
        for (int y = (int) Math.floor(cy - ry); y <= Math.ceil(cy + ry); y++) {
            float v = (y + 0.5f - cy) / ry; if (v * v > 1f) continue;
            float hw = rx * (float) Math.sqrt(1 - v * v);
            for (int x = (int) Math.floor(cx - hw); x <= Math.ceil(cx + hw); x++) cv.set(x, y, A(a, rgb));
        }
    }
    /** A soft elliptical glow: strongest in the middle, nothing at the edge. */
    static void glowE(float cx, float cy, float rx, float ry, int rgb, float strength) {
        if (strength <= 0.01f || rx < 1 || ry < 1) return;
        for (int y = (int) Math.floor(cy - ry); y <= Math.ceil(cy + ry); y++) for (int x = (int) Math.floor(cx - rx); x <= Math.ceil(cx + rx); x++) {
            float dx = (x + 0.5f - cx) / rx, dy = (y + 0.5f - cy) / ry, d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d >= 1f) continue;
            float f = (1f - d) * (1f - d);
            cv.set(x, y, A(strength * f, rgb));
        }
    }
    static void glint(float x, float y, int r, int rgb, float a) { if (a > 0.03f) cv.glint(Math.round(x), Math.round(y), r, rgb, cl(a)); }

    /** A forked bolt of lightning from (x0,y0) to (x1,y1): a wide soft glow, a coloured body and a white core. */
    static void bolt(float x0, float y0, float x1, float y1, int seed, int rgb, float a, int depth, boolean branches) {
        if (depth == 0) {
            line(x0 - 2, y0, x1 - 2, y1, rgb, a * 0.13f); line(x0 + 3, y0, x1 + 3, y1, rgb, a * 0.13f);
            line(x0 - 1, y0, x1 - 1, y1, rgb, a * 0.28f); line(x0 + 2, y0, x1 + 2, y1, rgb, a * 0.28f);
            line(x0 + 1, y0, x1 + 1, y1, rgb, a * 0.8f);
            line(x0, y0, x1, y1, mixc(rgb, WHITE, 0.7f), a);
            return;
        }
        float dx = x1 - x0, dy = y1 - y0, len = (float) Math.max(1e-3, Math.sqrt(dx * dx + dy * dy));
        float off = (rnd(seed * 31 + depth * 7) - 0.5f) * len * 0.42f;
        float mx = (x0 + x1) / 2 - dy / len * off, my = (y0 + y1) / 2 + dx / len * off;
        bolt(x0, y0, mx, my, seed * 2 + 1, rgb, a, depth - 1, branches);
        bolt(mx, my, x1, y1, seed * 2 + 2, rgb, a, depth - 1, branches);
        if (branches && depth == 4 && rnd(seed + 99) > 0.35f) {
            float ang = (float) Math.atan2(dy, dx) + (rnd(seed + 5) > 0.5f ? 0.7f : -0.7f);
            bolt(mx, my, mx + (float) Math.cos(ang) * len * 0.45f, my + (float) Math.sin(ang) * len * 0.45f, seed * 5 + 3, rgb, a * 0.7f, 2, false);
        }
    }

    /** A column of motes turning round and rising (the ritual's breath). */
    static void motes(float t, float cx, float cy, float rx, float ry, float height, int rgb, float a, int n) {
        for (int i = 0; i < n; i++) {
            float ph = rnd(i * 7 + 1), u = (t * (0.22f + 0.18f * rnd(i)) + ph) % 1f;
            float an = ph * 6.28f + t * (1.1f + rnd(i * 3) * 0.8f) * (i % 2 == 0 ? 1 : -1), rr = (0.85f - 0.55f * u) * (0.6f + 0.4f * rnd(i * 5));
            float x = cx + (float) Math.cos(an) * rx * rr, y = cy + (float) Math.sin(an) * ry * rr - u * height;
            float al = (float) Math.sin(u * Math.PI) * a;
            dot(x, y, mixc(rgb, WHITE, 0.4f), al);
            if (i % 4 == 0) glint(x, y, 2, rgb, al * 0.8f);
        }
    }

    /** Which creature the scene summons: the Mythological Mobs of the Animations page rotate through all twelve; a real drop calls its own. */
    static final String[] KINDS = {"minotaur", "minos_hunter", "siamese_lynxes", "stranded_nymph", "cretan_bull", "harpy", "gaia_construct", "minos_champion", "sphinx", "minos_inquisitor", "manticore", "king_minos"};
    private static final float[] SHAPE_TOP = {100, 90, 58, 76, 60, 90, 98, 100, 74, 104, 86, 108, 70, 52, 66, 82, 90, 84};
    private static String upKey = ""; private static long upMs;
    static int kind = 0;
    static int kindOf(String sourceId) { for (int i = 0; i < KINDS.length; i++) if (KINDS[i].equals(sourceId)) return i; return 0; }
    private static final int DK = 0x070A14;
    private static void fe(float cx, float gy, float k, float x, float y, float rx, float ry) { fillE(cx + x * k, gy + y * k, rx * k, ry * k, DK, 1f); }
    private static void ln2(float cx, float gy, float k, float x0, float y0, float x1, float y1, int w) { for (int i = -(w / 2); i <= w / 2; i++) { line(cx + x0 * k + i, gy + y0 * k, cx + x1 * k + i, gy + y1 * k, DK, 1f); line(cx + x0 * k, gy + y0 * k + i, cx + x1 * k, gy + y1 * k + i, DK, 1f); } }

    /** The canvas of the scene at time t: the opening, then the pivot of the type. Everything except the item and the words. */
    static void paint(float t, boolean tre, int type, float S, float sz, float isc, int rc, float icy, int blade, boolean itemBob, float bobAmp) {
        boolean rng = type == AnimationManager.RNG, nod = type == AnimationManager.NO_DROPS;
        String grp = slayerMode ? "slayer" : ""; float O = AnimationManager.opening(grp, type), p = t - O, icx = CW / 2f, gy = icy + 22 * sz, rx = 50 * sz, ry = 13 * sz;
        int energy = tre ? GOLD : slayerMode ? SL_ENERGY[Math.floorMod(kind - 12, SL_ENERGY.length)] : CYAN;
        float bg = 1f - sm((p - 0.5f) / 0.9f);                                             // the background fades while the words show
        float fxEnd = AnimationManager.animLen(grp, type) + hold;                                     // the finish ends with the animation; the words stay on after it
        float itemA = TextFx.itemOnly ? 1f : sm(p / 0.22f) * (1f - sm((t - (fxEnd - 0.6f)) / 0.6f));
        float tb = TextFx.itemOnly ? (System.currentTimeMillis() % 100000L) / 1000f : t;                // the item keeps bobbing while the Item editor holds the scene
        cv.clear();
        if (!slayerMode) return;                                                                // Stardrops: only Slayer has its new animation; the rest are being remade
        float cyS = itemY(t, p, gy, icy, sz, itemBob, bobAmp);
        SlayerScene.paint(t, AnimationManager.animLen("slayer", type), Math.floorMod(kind - 12, SL_ENERGY.length), energy, rc, rng, nod, icx, icy, sz, isc, itemA, itemOffs, cyS, bg);
    }

    static boolean threeD;                                                              // this frame's slayer scene is drawn in 3D (Slayer3D); the canvas only keeps the item's glow
    static boolean slayerMode;                                                          // set by the caller: paint() draws a slayer scene
    static float[] itemOffs = {0f};                                                     // canvas units from the middle to each drop (one entry per drop)

    /** Sets the scene flags for an animation; end() clears them again. */
    static float hold;                                                                  // seconds the finish of the animation being drawn is held (several drops: time to read them)
    static void begin(Anim a, float sz, float isc) {
        hold = a.hold();
        slayerMode = isSlayer(a); if (slayerMode) { kind = 12 + slTheme(a); }
        itemOffs = offsetsOf(a, sz, isc);
    }
    /** Canvas units from the middle to each drop: one drop sits in the middle, more are spaced evenly, closer together the more there are, never wider than about 60% of the screen. */
    static float[] offsetsOf(Anim a, float sz, float isc) {
        int n = Math.max(1, a.count());
        if (n == 1) return new float[]{0f};
        float gap = (n == 2 ? 40f : n == 3 ? 36f : 30f) * sz * isc * (a.type == AnimationManager.RNG ? 1.1f : 1f), span = CW * 0.6f;
        if (gap * (n - 1) > span) gap = span / (n - 1);
        float[] o = new float[n]; for (int i = 0; i < n; i++) o[i] = (i - (n - 1) / 2f) * gap;
        return o;
    }
    /** How much higher drop i of n floats (canvas units): three or more sit on a gentle arc, the middle ones highest. */
    static float liftOf(int i, int n, float sz) {
        if (n < 3) return 0f;
        float x = (i - (n - 1) / 2f) / ((n - 1) / 2f);
        return 6f * sz * (1f - x * x);
    }
    /** Each drop's size when several share the scene. */
    static float sizeOf(int n) { return n <= 1 ? 1f : n == 2 ? 0.85f : n == 3 ? 0.76f : Math.max(0.5f, 2.6f / n); }
    static void end() { hold = 0f; slayerMode = false; itemOffs = new float[]{0f}; }
    /** The colour of each boss: zombie, spider, wolf, enderman, blaze, vampire. */
    static final int[] SL_ENERGY = {0x7CD060, 0xE03030, 0xC8D4E8, 0xD060FF, 0xFFB030, 0xD01030, 0xFF6A2A, 0xC77DFF, 0x7FE8D8, 0xFF5A20, 0xFFB040, 0xFF7A20, 0xB070FF, 0x8AD8FF, 0x8AC8E8, 0xFF6A2A, 0x40E8A0};   // the six slayers, then the Crimson Isle, the End, the Mines
    static boolean isSlayer(Anim a) { return AnimationManager.isSlayer(a.group); }
    static int slTheme(Anim a) { int fixed = AnimationManager.starBoss(a.group); if (fixed >= 0) { int cr = -1; if (a.item != null && a.item.source != null) for (DropData.Drop d : a.item.source.drops) if (d.id.equals(a.item.id)) { cr = AnimationManager.creature(d.mob); break; } return cr >= 0 ? cr : fixed; }      // the drop's own creature (its "mob" in the data), else the group's
        return Math.floorMod(a.kind >= 0 ? a.kind : a.item != null && a.item.source != null ? SlayerData.theme(a.item.source.id) : 0, 6); }


    /** Where the animation sits when it is at the Top Center: its highest point (the boss, or the item) just touches the top of the screen, or the bottom of the top bar. */
    private static float topIcy(float sz, float hi, float topM) {
        float icy = topM + hi + 1;
        for (int n = 0; n < 160; n++, icy += 1f) {
            float gy = icy + 22 * sz, kk = Math.min(sz, (gy - 4) / 98f);
            if (gy - 2 - 86 * kk >= topM && icy - hi >= topM) break;
        }
        return icy;
    }
    /** Where the item is (canvas units, y): it springs out of the ground, overshoots, then bobs gently. */
    /** A flat round disc, one fill per row (the soft glow is a few of these stacked). */
    private static void discFill(GuiGraphicsExtractor g, int cx, int cy, int r, int col) {
        for (int y = -r; y <= r; y++) { int hw = (int) Math.round(Math.sqrt(Math.max(0, (double) r * r - (double) y * y))); g.fill(cx - hw, cy + y, cx + hw, cy + y + 1, col); }
    }
    /** The Normal flourish of the drop being drawn (0 rings, 1 starburst, 2 spiral); RNG and No Drops always use 0. */
    static int variant;
    private static float itemY(float t, float p, float gy, float icy, float sz, boolean bob, float amp) {
        float wob = bob ? (float) Math.sin(t * 2.2f) * 1.8f * sz * amp * sm((p - 0.8f) / 0.6f) : 0f;
        if (variant == 1) {                                                                // falls in from above and lands with two small bounces
            float u = cl(p / 0.75f), b = u < 0.55f ? (u / 0.55f) * (u / 0.55f) : u < 0.8f ? 1f - 0.18f * (float) Math.sin((u - 0.55f) / 0.25f * Math.PI) : 1f - 0.05f * (float) Math.sin((u - 0.8f) / 0.2f * Math.PI);
            return lerp(icy - 40 * sz, icy, b) + wob;
        }
        if (variant == 2) {                                                                // spirals up out of the ground
            float u = eo(p / 0.9f);
            return lerp(gy - 4, icy, u) + (float) Math.sin(u * 9f) * 3f * sz * (1f - u) + wob;
        }
        float rise = eob(p / 0.85f);
        return lerp(gy - 4, icy, rise) + wob;
    }

    /** Seconds from the pivot (the moment the drop appears) to the end. */
    /** The scene is drawn 25% smaller than before: that is the new 100% of the Size slider. */
    static final float SMALLER = 0.6f;
    /** The scene (everything round the item) is drawn this much smaller than the item's own scale, so it never takes over the screen. */
    static final float SCENE_K = 0.72f;
    /** Keeps the painted scene inside a small stage round the item: a soft ellipse, wider only when several drops sit side by side. */
    static void stage(Anim a, float icy, float ssz, float sz, float isc) {
        float[] o = offsetsOf(a, sz, isc); float span = 0f; for (float v : o) span = Math.max(span, Math.abs(v));
        float k = ssz / (SMALLER * SCENE_K);
        float rx = Math.max(76f * k, span + 36f * k), ry = 66f * k, cy = icy + 4f * ssz;
        cv.mask(CW / 2f, cy, rx, ry, 0.7f);
    }

    /** The scene canvas alone (320x180 ARGB) at time t: what the bench renders. */
    static int[] snapshot(Anim a, float t) {
        StardropsConfig c = StardropsConfig.get(); StardropsConfig.AnimCfg cfg = a.cfg(); Item it = a.lead();
        boolean tre = a.group.equals("treasure"); kind = Math.max(0, a.kind); int type = a.type;
        boolean rng = type == AnimationManager.RNG, nod = type == AnimationManager.NO_DROPS;
        float S = a.secs() + a.hold(), csz = Math.max(0.5f, Math.min(1.5f, c.animScale)), sz = SMALLER * csz * K, isc = Math.max(0.4f, c.itemScale);
        int rc = it != null ? (it.color() & 0xFFFFFF) : GREY;
        if (!tre && !rng && !nod) rc = mixc(rc, WHITE, 0.15f);
        float barH = CH * Math.max(0.031f, Math.min(0.094f, c.barPx / 160f)), hi = 16 * sz * isc * (rng ? 1.2f : 1f), icy = CH * POS[Math.max(0, Math.min(2, c.animPos))];
        if (c.animPos == 0) icy = topIcy(sz, hi, rng && cfg.bars ? barH + 1 : 0f);
        if (rng && cfg.bars) icy = Math.max(c.animPos == 0 ? 0f : barH + hi + 4, Math.min(CH - barH - hi - 14 * sz, icy));
        begin(a, sz, isc); float ssz = sz * SCENE_K;
        try { paint(t, tre, type, S, ssz, isc, rc, icy, TreasureData.COLS[Math.max(0, Math.min(2, c.mythSpade))] & 0xFFFFFF, c.itemBob, c.itemBobAmp); stage(a, icy, ssz, sz, isc); } finally { end(); }
        return cv.px.clone();
    }

    private static float textH(Anim a, Item it, float sc, StardropsConfig c) { return a.count() >= 2 ? TextFx.blockHeightN(a.items(), sc, c) : TextFx.blockHeight(it, sc, c); }

    /** The slayer camera at time t: {zoom, roll (radians), shake x, shake y (canvas units)}. A slow dolly in while the boss walks toward you,
     *  a shake on every meteor (SlayerScene.shake), and for RNG a held zoom before the golden one lands, then a kick back as it hits. */
    static float[] slayerCam(float t, float p, int type) {
        int ty = type == AnimationManager.NO_DROPS ? 0 : type == AnimationManager.RNG ? 2 : 1;
        float settle = 1f - sm(p / 0.6f), walk = sm((t - SlayerScene.WALK) / (SlayerScene.WALK_END - SlayerScene.WALK)), sh = SlayerScene.shake(ty, t);
        float z = 1f + 0.10f * walk * settle + 0.06f * sh;                                          // a slow push in as the boss walks up, a kick on every hit
        if (ty == 2) z += 0.14f * sm((t - 2.95f) / 0.4f) * (1f - sm(p / 0.35f)) - 0.06f * (p > 0 ? sm(p / 0.1f) * (1f - sm((p - 0.1f) / 0.8f)) : 0f);   // RNG: lean in for the big one, blown back by it
        float r = 0.018f * (float) Math.sin(t * 1.3f) * walk * settle + (rnd((int) (t * 30)) - 0.5f) * 0.04f * sh;
        float jx = (rnd((int) (t * 60)) - 0.5f) * 7f * sh, jy = (rnd((int) (t * 60) + 31) - 0.5f) * 7f * sh;
        return new float[]{z, r, jx, jy};
    }

    /** Draws one animation at time t (seconds) in a W x H screen: the scene, the bars, the item and the words. */
    static void draw(GuiGraphicsExtractor g, Font font, Anim a, float t, int W, int H) {
        StardropsConfig c = StardropsConfig.get();
        StardropsConfig.AnimCfg cfg = a.cfg();
        Item it = a.lead();
        boolean tre = a.group.equals("treasure"), sl = isSlayer(a);
        kind = a.kind >= 0 ? a.kind : a.item != null && a.item.source != null ? kindOf(a.item.source.id) : 0;
        variant = a.type == AnimationManager.NORMAL ? a.variant : 0;
        if (sl) kind = 12 + slTheme(a);
        int type = a.type;
        boolean rng = type == AnimationManager.RNG, nod = type == AnimationManager.NO_DROPS;
        float hd = a.hold(), S = a.secs() + hd, O = AnimationManager.opening(a.group, type), p = t - O;
        float csz = Math.max(0.5f, Math.min(1.5f, c.animScale)) , sz = SMALLER * csz, szc = sz * K, ssz = szc * SCENE_K, isc = Math.max(0.4f, c.itemScale);          // Normal and No Drops can play smaller, so they stay out of the way
        int rc = it != null ? (it.color() & 0xFFFFFF) : GREY;
        if (!tre && !rng && !nod) rc = mixc(rc, WHITE, 0.15f);
        boolean barsOn = (rng && cfg.bars) || TextFx.barsOnly, blurOn = rng && cfg.blur && !TextFx.barsOnly && !TextFx.itemOnly;

        // where everything is (canvas units)
        float barH = CH * Math.max(0.031f, Math.min(0.094f, c.barPx / 160f));
        float hi = 16 * szc * isc * (rng ? 1.2f : 1f);
        float icy = CH * POS[Math.max(0, Math.min(2, c.animPos))];
        if (c.animPos == 0) icy = topIcy(szc, hi, barsOn ? barH + 1 : 0f);
        if (barsOn) icy = Math.max(c.animPos == 0 ? 0f : barH + hi + 4, Math.min(CH - barH - hi - 14 * szc, icy));
        float u = (float) H / CH;                                                          // gui units per canvas unit
        float itemA = TextFx.itemOnly ? 1f : sm(p / 0.22f) * (1f - sm((t - (S - 1.0f)) / 0.9f));
        float tb = TextFx.itemOnly ? (System.currentTimeMillis() % 100000L) / 1000f : t;                // the item keeps bobbing while the Item editor holds the scene
        float textT = t - (O + 0.45f), textOut = sm((t - (S - 0.6f)) / 0.5f);               // the words leave after the item
        float barEnd = sl ? AnimationManager.animLen(a.group, type) + hd : S;                                // slayer: the bars open again when the animation ends
        float barSlide = TextFx.barsOnly ? 1f : Math.min(sm((t - (O - (sl ? 0.5f : 0.8f))) / 0.6f), 1f - sm((t - (barEnd - 0.5f)) / 0.45f));
        float shake = rng && p > 0 && p < 0.5f ? (1f - p / 0.5f) * 3f : 0f;
        int sx = Math.round((rnd((int) (t * 60)) - 0.5f) * 2f * shake * u), sy = Math.round((rnd((int) (t * 60) + 77) - 0.5f) * 2f * shake * u);

        boolean sceneOff = TextFx.textOnly || TextFx.itemOnly || TextFx.barsOnly;            // the Changes previews show only the part being edited
        threeD = sl && !sceneOff && Slayer3D.on();
        if (!sceneOff) {
            // ---- the blur: the world dims and softens at the edges (RNG, switchable)
            if (blurOn && barSlide > 0.01f) {                                             // a soft shade only behind the stage, so the item stands out (the rest of the screen stays as it is)
                int rr = Math.round(78 * csz * K * u), ccy = Math.round(icy * u);
                NebulaTex.glow(g, W / 2f, ccy, rr * 1.15f, rr * 1.15f, Gfx.argb(Math.round(150 * barSlide), 0x04050E));          // one soft picture instead of hundreds of fills
            }
            if (threeD) {                                                                      // slayer in 3D: a soft-edged window of space round the stage, the crater under the item
                float top = Math.max(0f, icy - 70 * ssz), bot = Math.min(CH, icy + 110 * ssz), hw = Math.min(CW / 2f, 130 * ssz), A = AnimationManager.animLen(a.group, type);
                float craterY = 1f - 2f * ((icy + 14 * ssz) - top) / Math.max(1f, bot - top);
                float al = sm(t / 0.35f) * (1f - sm((t - (A + hd - 0.7f)) / 0.7f)) * (1f - 0.35f * sm((p - 0.8f) / 0.8f));
                Slayer3D.submit(g, Math.round((CW / 2f - hw) * u) + sx, Math.round(top * u) + sy, Math.round((CW / 2f + hw) * u) + sx, Math.round(bot * u) + sy,
                        t, A + hd, Math.floorMod(kind - 12, SL_ENERGY.length), type, SL_ENERGY[Math.floorMod(kind - 12, SL_ENERGY.length)], al, craterY);
            }
            {
                long nowMs = System.currentTimeMillis();
                String id = a.group + a.type + "/" + a.kind + "/" + (it == null ? 0 : it.id.hashCode()) + "/" + a.count() + "/" + TextFx.itemOnly;
                boolean reuse = id.equals(upKey) && nowMs >= upMs && nowMs - upMs < (AnimationManager.previewing ? 16 : 11);       // the pixel scene repaints every frame up to ~140 a second (60 in menu previews): painting it costs under 1 ms
                if (!reuse) {
                    begin(a, szc, isc);
                    try { paint(t, tre, type, S, ssz, isc, rc, icy, TreasureData.COLS[Math.max(0, Math.min(2, c.mythSpade))] & 0xFFFFFF, c.itemBob, c.itemBobAmp); stage(a, icy, ssz, szc, isc); } finally { end(); }
                    SceneTexture.upload(cv); upKey = id; upMs = nowMs;
                }
            }
            float[] cam = sl && !threeD ? slayerCam(t, p, type) : null;                                 // slayer: a camera that moves like a film (dolly in, beats, punch, roll)
            if (cam != null) { float pvx = W / 2f + sx + cam[2] * u, pvy = icy * u + sy + cam[3] * u; Gfx.push(g); Gfx.translate(g, pvx, pvy); Gfx.rotate(g, cam[1]); Gfx.scale(g, cam[0]); Gfx.translate(g, -pvx + cam[2] * u, -pvy + cam[3] * u); }
            if (sl && !threeD) SlayerScene.backdrop(g, (float) W / CW, sx, sy);                          // the patch of night sky: soft GPU glows under the pixel scene
            if (!SceneTexture.draw(g, cv, sx, sy, W, H, 1f)) { int cell = Math.max(1, Math.round((float) W / CW)); cv.flush((x0, y0, x1, y1, col) -> g.fill(x0 + sx, y0 + sy, x1 + sx, y1 + sy, col), 0, 0, cell, 1f); }
            if (rng && p > 0 && p < 0.45f) { float fl = (1f - p / 0.45f) * (1f - p / 0.45f); int rr = Math.round(46 * csz * K * u * (1f + 0.4f * p)); NebulaTex.glow(g, W / 2f + sx, icy * u + sy, rr * 1.1f, rr * 1.1f, Gfx.argb(Math.round(230 * fl), 0xFFFFFF)); }                  // the flash: a burst of light on the item, not the whole screen
            if (cam != null) Gfx.pop(g);

        }

        // ---- the bars
        if (barsOn && barSlide > 0.01f && !TextFx.textOnly && !TextFx.itemOnly) {
            int bh = Math.round(barH * u * barSlide), oa = Math.round(255 * c.barOpacity / 100f);
            int bc = c.barCustom ? (c.barColor & 0xFFFFFF) : 0x05060C;          // never tinted by the drop colour
            g.fill(0, 0, W, bh, Gfx.argb(oa, bc)); g.fill(0, H - bh, W, H, Gfx.argb(oa, bc));
             if (c.barPillars) { int pw = bh;   g.fill(0, bh, pw, H - bh, Gfx.argb(oa, bc)); g.fill(W - pw, bh, W, H - bh, Gfx.argb(oa, bc)); if (c.barLine) { int lc = Gfx.argb(Math.round(210 * barSlide), mixc(rc, WHITE, 0.3f)); g.fill(pw, bh, pw + 1, H - bh, lc); g.fill(W - pw - 1, bh, W - pw, H - bh, lc); } }
            if (c.barCurve > 0 && bh > 2) {                                                              // curved corners: the four inner corners of the picture are rounded off in the bar colour
                int pwc = c.barPillars ? bh : 0, wx0 = pwc, wx1 = W - pwc, wy0 = bh, wy1 = H - bh;
                int rr = Math.round(c.barCurve / 100f * Math.min(wx1 - wx0, wy1 - wy0) / 5f);
                int col = Gfx.argb(oa, bc);
                for (int i = 0; i < rr; i++) {
                    double dv = rr - i - 0.5; int cut = (int) Math.round(rr - Math.sqrt(Math.max(0.0, (double) rr * rr - dv * dv)));
                    if (cut <= 0) continue;
                    g.fill(wx0, wy0 + i, wx0 + cut, wy0 + i + 1, col); g.fill(wx1 - cut, wy0 + i, wx1, wy0 + i + 1, col);
                    g.fill(wx0, wy1 - 1 - i, wx0 + cut, wy1 - i, col); g.fill(wx1 - cut, wy1 - 1 - i, wx1, wy1 - i, col);
                }
            }
            // (the pillars above are exactly as thick as the bars)
            if (c.barSoft) { int nr = Math.max(8, Math.round(16 * u)); for (int i = 0; i < nr; i++) { float f = 1f - (i + 0.5f) / nr; int al = Math.round(oa * f * f * 0.9f * barSlide); if (al <= 0) continue; g.fill(0, bh + i, W, bh + i + 1, Gfx.argb(al, bc)); g.fill(0, H - bh - i - 1, W, H - bh - i, Gfx.argb(al, bc)); } }          // a smooth fade along the inner edge, one thin row per step (no visible lines)
            if (c.barLine) { g.fill(0, bh, W, bh + 1, Gfx.argb(Math.round(210 * barSlide), mixc(rc, WHITE, 0.3f))); g.fill(0, H - bh - 1, W, H - bh, Gfx.argb(Math.round(210 * barSlide), mixc(rc, WHITE, 0.3f))); }
        }

        // ---- the item (flat icons stay flat, skulls are 3D and turn)
        if (it != null && p >= 0f && !nod && !TextFx.textOnly && !TextFx.barsOnly) {
            List<Item> all = a.items(); int n = all.size(); float[] offs = offsetsOf(a, szc, isc);
            for (int ii = 0; ii < n; ii++) {
            Item cur = all.get(ii);
            float pi = p - (n > 1 ? ii * 0.07f : 0f);                                          // several drops pop in one after another, left to right
            if (pi < 0f) continue;
            float grow = 0.25f + 0.75f * eob(pi / 0.7f);
            float s = 3f * (H / 270f) * sz * isc * grow * (rng ? 1.2f : 1f) * (TextFx.itemOnly ? 1.9f : 1f) * sizeOf(n);
            float gcx = CW / 2f * W / CW + sx + offs[Math.min(ii, offs.length - 1)] * u, gcy = -liftOf(ii, n, szc) * u + itemY(tb, p, icy + 22 * ssz, icy, ssz, c.itemBob, c.itemBobAmp) * u + sy;
            if (c.itemGlow > 0.02f) {                                                          // the glow behind the item (Item changes > Glow), in the drop's colour
                int gr = Math.round(s * 11f), gcol = mixc(rc, WHITE, 0.25f); float ga = Math.max(0f, Math.min(1f, itemA)) * Math.min(1f, c.itemGlow);
                int al = Math.round(Math.min(46f, 26f * ga * (0.6f + c.itemGlow)) / 1.6f);                   // two soft pictures (it used to be hundreds of fills)
                NebulaTex.glow(g, gcx, gcy, gr * 1.1f, gr * 1.1f, Gfx.argb(al * 3, gcol)); NebulaTex.glow(g, gcx, gcy, gr * 0.55f, gr * 0.55f, Gfx.argb(al * 2, gcol));
            }
            ItemIcons.spinSpeed = c.itemSpin; ItemIcons.spinRev = c.itemSpinRev;
            ItemIcons.drawAlpha = Math.max(0f, Math.min(1f, itemA)); boolean still0 = Icons.STILL_ICONS; Icons.STILL_ICONS = false;                 // the dropped item always turns, whatever the menu says
            try { ItemIcons.draw(g, cur.id, cur.icon, Math.round(gcx), Math.round(gcy), s); } catch (Throwable ignored) { } finally { ItemIcons.drawAlpha = 1f; Icons.STILL_ICONS = still0; ItemIcons.spinSpeed = 1f; ItemIcons.spinRev = false; }
            if (c.itemGlints && itemA > 0.05f) for (int i = 0; i < 9; i++) {                      // Item changes > Glints: little four-point sparks twinkle in and out round the item
                float cyc = tb * 1.7f + i * 0.113f, ph = cyc - (float) Math.floor(cyc); int slot = (int) Math.floor(cyc) * 31 + i * 7;
                float an = rnd(slot) * 6.2832f, rad = s * (9f + 8f * rnd(slot + 1)), tw = (float) Math.sin(Math.PI * ph);
                int gx = Math.round(gcx + (float) Math.cos(an) * rad), gy2 = Math.round(gcy + (float) Math.sin(an) * rad * 0.85f), arm = Math.max(1, Math.round((1.5f + s * 0.6f * rnd(slot + 2)) * tw));
                int al = Math.round(255 * tw * Math.min(1f, itemA)), col = Gfx.argb(al, i % 2 == 0 ? 0xFFFFFF : mixc(rc, WHITE, 0.6f));
                g.fill(gx - arm, gy2, gx + arm + 1, gy2 + 1, col); g.fill(gx, gy2 - arm, gx + 1, gy2 + arm + 1, col);
                g.fill(gx - 1, gy2 - 1, gx + 2, gy2 + 2, Gfx.argb(al / 2, rc));
            }
            }
        }

        // ---- slayer, nothing dropped: the words over the crater, once the boss has crumbled
        if (sl && nod && !sceneOff) {
            float na = sm((t - (O + 0.55f)) / 0.4f) * (1f - sm((t - (S - 0.6f)) / 0.5f));
            if (na > 0.01f) {
                int boss = slTheme(a), en = SL_ENERGY[boss];
                float sc = Math.max(1f, Math.min(W, H) / 270f) * 1.1f;
                String l1 = "Nothing dropped", l2 = SlayerScene.NAMES[boss];
                int y0 = Math.round(icy * u - 6 * sc);
                Gfx.push(g); Gfx.translate(g, W / 2f - font.width(l1) * sc / 2f, y0 + (1f - na) * 4); Gfx.scale(g, sc); Gfx.txt(g, font, l1, 0, 0, Gfx.argb(Math.round(240 * na), 0xFFFFFF), true); Gfx.pop(g);
                float s2 = sc * 0.65f;
                Gfx.push(g); Gfx.translate(g, W / 2f - font.width(l2) * s2 / 2f, y0 + 12 * sc); Gfx.scale(g, s2); Gfx.txt(g, font, l2, 0, 0, Gfx.argb(Math.round(200 * na), mixc(en, WHITE, 0.35f)), true); Gfx.pop(g);
            }
        }

        // ---- the words: below the animation (top and center), above it (bottom); never over the bars
        if (!TextFx.itemOnly && !TextFx.barsOnly && !(sl && nod && !TextFx.textOnly) && (textT > 0f || TextFx.textOnly)) {   // (a slayer with nothing has its own words)
            float sc = c.textK() * Math.max(0.55f, Math.min(W, H) / 540f) * (0.8f + 0.2f * csz); if (TextFx.textOnly) sc = c.textK() * Math.max(1.3f, Math.min(2.6f, W / 150f));          // the Text editor shows the words big enough to read
            float th = textH(a, it, sc, c);
            float top = c.textAbove() ? (icy - hi - 14 * szc) * u - th : (icy + hi + 26 * szc) * u;
            if (TextFx.textOnly) { while (th > H * 0.92f && sc > 0.8f) { sc *= 0.9f; th = textH(a, it, sc, c); } top = Math.max(2f, (H - th) / 2f); }       // the Text editor centres the words and fills the box
            if (barsOn) { float bhg = barH * u; top = Math.max(bhg + 6, Math.min(H - bhg - th - 6, top)); }
            float tl = TextFx.textOnly ? TextFx.loopClock(4.6f) : 0f;                                    // the Text editor replays the words on their own clock
            TextFx.revealT = TextFx.textOnly ? tl : textT; TextFx.waveT = TextFx.textOnly ? tl : t; TextFx.waveOut = TextFx.textOnly ? sm((tl - 2.9f) / 1.1f) : textOut; TextFx.lineDelay = 0f;
            if (a.count() >= 2) TextFx.blockN(g, font, W / 2, Math.round(top), sc, a.items(), c, 255); else TextFx.block(g, font, W / 2, Math.round(top), sc, it, c, 255);
        }
    }
}
