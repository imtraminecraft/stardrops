package dev.fancydrops;

import dev.fancydrops.AnimationManager.Anim;
import dev.fancydrops.AnimationManager.Item;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

/**
 * The drop animations (1.1): small, quick pop-ups drawn fresh every frame in crisp pixel squares, so they move smoothly at your full
 * frame rate and never cover the screen. A Normal pop-up fits in about 120 x 60 GUI pixels, RNG in about 180 x 110.
 *
 *   BUILD-UP: pixel sparks fly in and a small diamond of light gathers, beating three times (a ring of squares on each beat). Its colour
 *     flickers while it builds and settles on the true colour at the last beat: the only tell, and an honest one.
 *   THE POP: a white cross flash and a ring of squares going out, a few sparks flung up that fall under their own weight; the item
 *     springs up into place and twinkles. RNG adds thin cinematic bars, a second ring, more sparks, slow pixel rays and a tiny shake.
 *   THE WORDS: the name in its rarity colour with a black outline (like the game's own drop messages), typed in left to right, a line
 *     drawing open under it, then the odds and the price. RNG adds a small "RNG DROP" tag and a shine across the name.
 *   NO DROPS: the diamond cracks into shards that drop away. Quick: 1.6 s.
 *
 * Timings come from AnimationManager (No Drops 1.6 s, Normal 2.4 s + 1.4 s words, RNG 3.6 s + 1.8 s words).
 * The editors in the menu (Item / Text / Bars) still use the classic scene for their previews.
 */
final class DropShow {
    private DropShow() {}

    private static float cl(float v) { return v < 0 ? 0 : v > 1 ? 1 : v; }
    private static float sm(float v) { v = cl(v); return v * v * (3 - 2 * v); }
    private static float eo(float v) { v = cl(v); return 1f - (1f - v) * (1f - v) * (1f - v); }
    private static float spring(float v) { v = cl(v); return 1f - (float) (Math.exp(-7 * v) * Math.cos(11 * v)); }
    private static float rnd(int n) { return Ritual.rnd(n); }
    private static int mix(int a, int b, float f) { return Ritual.mixc(a, b, cl(f)); }
    private static int A(float a, int rgb) { return Gfx.argb(Math.round(cl(a) * 255), rgb & 0xFFFFFF); }
    private static final int[] FLICKER = {0xFFFFFF, 0x55FFFF, 0xFFAA00, 0xFF55FF, 0x55FF55, 0x5555FF, 0xFF5555};

    static void draw(GuiGraphicsExtractor g, Font font, Anim a, float t, int W, int H) {
        FancyConfig c = FancyConfig.get();
        int type = a.type; boolean rng = type == AnimationManager.RNG, nod = type == AnimationManager.NO_DROPS;
        float A = AnimationManager.animLen(type), O = AnimationManager.opening(type), S = a.total(), p = t - O;
        Item it = a.lead();
        int gc = Categories.group(a.group).color() & 0xFFFFFF, rc = it == null ? 0x9AA3B5 : it.color() & 0xFFFFFF;
        int truth = nod ? 0x8A92A3 : rng ? rc : mix(gc, rc, 0.5f);
        float k = Math.max(0.6f, Math.min(1.4f, c.animScale));
        int q = Math.max(1, Math.round(k * (rng ? 2f : 1.5f)));                                   // the size of one art square
        float cx = W / 2f, cy = c.animPos == 0 ? 34 * k + (rng ? H * 0.07f : 0) : c.animPos == 2 ? H - 104 * k : H * 0.40f;
        float fxA = (1f - sm((t - (A - 0.35f)) / 0.35f)) * sm(t / 0.12f);                         // the effects end with the animation; the words stay on
        float wordsA = 1f - sm((t - (S - 0.4f)) / 0.4f);

        // the colour of the core: flickers while it builds, settles on the truth at the last beat
        float settle = 0.80f * O;
        int col;
        if (t >= settle || nod) col = truth;
        else { int step = (int) (t * (6f + 10f * t / settle)); col = mix(FLICKER[Math.floorMod(step * 3 + a.group.length(), FLICKER.length)], gc, 0.35f); }
        int lite = mix(col, 0xFFFFFF, 0.55f);

        // a tiny shake on an RNG drop
        float sh = rng && p > 0 && p < 0.3f ? (1f - p / 0.3f) * 1.5f * k : 0f;
        Gfx.push(g); if (sh > 0) Gfx.translate(g, Math.round((rnd((int) (t * 60)) - 0.5f) * 2 * sh), Math.round((rnd((int) (t * 60) + 7) - 0.5f) * 2 * sh));

        // ---- RNG: thin cinematic bars and a soft pool of shade behind the pop-up only
        if (rng) {
            if (c.anim(a.key()).bars) {
                float b = Math.min(sm((t - (O - 0.6f)) / 0.45f), 1f - sm((t - (S - 0.35f)) / 0.3f)); int bh = Math.round(H * 0.055f * eo(b));
                if (bh > 0) { g.fill(0, 0, W, bh, A(0.92f, 0x04050A)); g.fill(0, H - bh, W, H, A(0.92f, 0x04050A));
                    g.fill(0, bh, W, bh + 1, A(0.45f * b, lite)); g.fill(0, H - bh - 1, W, H - bh, A(0.45f * b, lite)); }
            }
            NebulaTex.glow(g, cx, cy + 14 * k, 110 * k, 62 * k, A(0.42f * Math.max(fxA, wordsA * 0.6f) * sm(t / 0.4f), 0x03040A));
        }

        // ---- build-up: sparks fly in, the diamond gathers and beats
        float core = (rng ? 5f : 3.5f) * k * (0.35f + 0.65f * sm(t / O));
        int sparks = rng ? 16 : 9;
        if (p < 0.05f) for (int i = 0; i < sparks; i++) {
            float life = 0.55f + 0.25f * rnd(i * 5), st = rnd(i * 3) * life;
            float u = ((t + st) % life) / life; if (t < st * 0.5f) continue;
            float an = rnd(i * 7) * 6.2832f + u * 1.6f, r = (1f - eo(u)) * (rng ? 58 : 34) * k + core;
            float x = cx + (float) Math.cos(an) * r, y = cy + (float) Math.sin(an) * r * 0.7f, al = (float) Math.sin(Math.PI * u) * fxA;
            sq(g, x, y, q, A(al, i % 3 == 0 ? 0xFFFFFF : lite));
            sq(g, x - (float) Math.cos(an) * q * 1.5f, y - (float) Math.sin(an) * q * 1.05f, q, A(al * 0.4f, lite));          // a one-square trail
        }
        float beat = 0f;
        for (int b = 0; b < 3; b++) {
            float bt = t - new float[]{0.34f, 0.58f, 0.80f}[b] * O; if (bt < 0) continue;
            beat = Math.max(beat, 1f - cl(bt / 0.22f));
            if (bt < 0.45f) { float u = eo(bt / 0.45f); squareRing(g, cx, cy, core + u * (rng ? 26 + 6 * b : 14 + 4 * b) * k, 0.72f, q, A((1f - u) * 0.85f * fxA, b == 2 ? 0xFFFFFF : lite), rng ? 20 : 14); }
        }
        if (p < 0f) {
            float cr = core * (1f + 0.4f * beat);
            NebulaTex.glow(g, cx, cy, cr * 3.2f, cr * 3.2f, A(0.3f * fxA, col));
            diamond(g, cx, cy, cr, q, A(fxA, lite), A(fxA, 0xFFFFFF));
            if (rng) { float fl = (0.5f + 0.5f * sm(t / O)) * fxA; float len = (18 + 30 * sm(t / O)) * k * (1f + 0.3f * beat);         // a thin flare line through the core
                g.fill(Math.round(cx - len), Math.round(cy), Math.round(cx + len), Math.round(cy) + 1, A(0.5f * fl, lite)); }
        }

        // ---- the pop (or, for No Drops, the crack)
        if (p >= 0f) {
            if (nod) {
                float u = cl(p / (A - O));
                if (u < 0.15f) diamond(g, cx, cy, core * (1f + u * 2f), q, A(fxA, 0x9AA3B5), A(fxA, 0xD8DEE8));
                for (int i = 0; i < 6; i++) {                                                         // shards falling away
                    float dx = (rnd(i * 11) - 0.5f) * 22 * k, vy = -(6 + 8 * rnd(i * 5)) * k;
                    float x = cx + dx * eo(u), y = cy + vy * u + 60 * k * u * u;
                    sq(g, x, y, q + (i % 2), A((1f - u) * fxA, i % 2 == 0 ? 0x9AA3B5 : 0x5E6575));
                }
                float pu = cl(p / 0.35f); if (pu < 1f) squareRing(g, cx, cy, core + eo(pu) * 10 * k, 0.7f, q, A((1f - pu) * 0.6f * fxA, 0xB8C0D0), 10);
            } else {
                float fl = 1f - cl(p / 0.22f);
                if (fl > 0f) {                                                                        // a white cross flash
                    float hl = (rng ? 70 : 34) * k * (0.6f + 0.4f * fl), vl = hl * 0.45f;
                    g.fill(Math.round(cx - hl), Math.round(cy), Math.round(cx + hl), Math.round(cy) + q, A(fl, 0xFFFFFF));
                    g.fill(Math.round(cx), Math.round(cy - vl), Math.round(cx) + q, Math.round(cy + vl), A(fl, 0xFFFFFF));
                    NebulaTex.glow(g, cx, cy, (rng ? 40 : 22) * k, (rng ? 40 : 22) * k, A(fl * 0.7f, 0xFFFFFF));
                }
                for (int w = 0; w < (rng ? 2 : 1); w++) {
                    float u = cl((p - w * 0.1f) / (rng ? 0.65f : 0.5f)); if (u <= 0f || u >= 1f) continue;
                    squareRing(g, cx, cy, 6 * k + eo(u) * (rng ? 62 : 32) * k, 0.68f, q, A((1f - u) * 0.95f * fxA, w == 0 ? 0xFFFFFF : lite), rng ? 28 : 18);
                }
                int ns = rng ? 26 : 10; float life = rng ? 1.0f : 0.75f;
                if (p < life) for (int i = 0; i < ns; i++) {                                         // sparks flung up, falling
                    float an = (float) (-Math.PI / 2 + (rnd(i * 13) - 0.5f) * Math.PI * 1.5f), sp = (22 + 40 * rnd(i * 3)) * k * (rng ? 1.3f : 1f), u = cl(p / life);
                    float x = cx + (float) Math.cos(an) * sp * eo(u), y = cy + (float) Math.sin(an) * sp * eo(u) + u * u * 34 * k;
                    sq(g, x, y, q, A((1f - u) * fxA, i % 3 == 0 ? 0xFFFFFF : i % 3 == 1 ? lite : col));
                }
            }
        }

        // ---- the item(s), springing up into place
        float itemA = 0f, under = cy + 10 * k, over = cy - 10 * k;
        if (it != null && !nod && p >= 0f) {
            List<Item> all = a.items(); int n = all.size();
            float base = (rng ? 1.75f : 1.4f) * k * (n > 3 ? 0.8f : 1f), gap = 16 * base + 6 * k, x0 = cx - gap * (n - 1) / 2f;
            itemA = sm(p / 0.12f) * (1f - sm((t - (S - 0.5f)) / 0.45f));
            if (rng) {                                                                                // slow pixel rays turning behind it
                float ra = itemA * 0.55f;
                for (int r = 0; r < 8; r++) {
                    float an = r * 0.7854f + t * 0.35f, len = (r % 2 == 0 ? 30 : 20) * k;
                    for (float d = 12 * k; d < len; d += q * 2) sq(g, cx + (float) Math.cos(an) * d, cy + (float) Math.sin(an) * d, q, A(ra * (1f - d / len), lite));
                }
            }
            for (int i = 0; i < n; i++) {
                Item cur = all.get(i); float pi = p - i * 0.07f; if (pi < 0) continue;
                float sp = spring(pi / 0.55f), s = base * (0.4f + 0.6f * sp), bob = (float) Math.sin(t * 2.4f + i) * k;
                float x = x0 + gap * i, y = cy + (1f - sp) * 8 * k + (pi > 0.55f ? bob : 0);
                NebulaTex.glow(g, x, y, 13 * base, 13 * base, A(0.32f * itemA, col));
                ItemIcons.drawAlpha = cl(itemA); boolean still0 = Icons.STILL_ICONS; Icons.STILL_ICONS = false;
                try { ItemIcons.draw(g, cur.id, cur.icon, Math.round(x), Math.round(y), s); } catch (Throwable ignored) { } finally { ItemIcons.drawAlpha = 1f; Icons.STILL_ICONS = still0; }
                for (int z = 0; z < (rng ? 4 : 3); z++) {                                           // four-point twinkles round it
                    float cyc = t * 1.3f + z * 0.29f + i * 0.5f, ph = cyc - (float) Math.floor(cyc); int slot = (int) Math.floor(cyc) * 17 + z * 5 + i * 3;
                    float an = rnd(slot) * 6.2832f, rad = (9 + 5 * rnd(slot + 1)) * base, tw = (float) Math.sin(Math.PI * ph) * itemA;
                    twinkle(g, x + (float) Math.cos(an) * rad, y + (float) Math.sin(an) * rad * 0.8f, q, tw, z == 0 ? 0xFFFFFF : mix(rc, 0xFFFFFF, 0.4f));
                }
            }
            under = cy + 10 * base + 4 * k; over = cy - 10 * base - 4 * k;
        }
        Gfx.pop(g);

        // ---- the words
        if (it != null && !nod) words(g, font, a, it, p, t, cx, c.textAbove() ? over : under, c.textAbove(), k, wordsA, rng, col, lite, rc);
    }

    private static void words(GuiGraphicsExtractor g, Font f, Anim a, Item it, float p, float t, float cx, float anchor, boolean above, float k, float wa, boolean rng, int col, int lite, int rc) {
        float tt = p - 0.12f; if (tt < 0f || wa < 0.02f) return;
        List<Item> all = a.items(); int n = Math.min(4, all.size());
        float ns = (n > 1 ? 0.9f : 1f) * k * (rng ? 1.15f : 1f), ds = 0.75f * k;
        String chance = it.chance != null && !it.chance.isEmpty() ? it.chance : it.odds == null ? "" : it.odds;
        String odds = chance + (it.mf != null && !it.mf.isEmpty() ? (chance.isEmpty() ? "" : "  ·  ") + it.mf + " MF" : "");
        String price = it.price > 0 ? PriceService.fmt(it.price * Math.max(1, it.amount)) + " coins" : it.price == -2 ? "pricing..." : "";
        float h = (rng ? 9 * k : 0) + n * 10 * ns + 4 * k + (odds.isEmpty() ? 0 : 8 * k) + (price.isEmpty() ? 0 : 8 * k);
        float y = above ? anchor - h : anchor;

        if (rng) {                                                                                   // a small "RNG DROP" tag
            String ttl = "RNG DROP"; float ta = sm(tt / 0.2f) * wa, ts = 0.7f * k, tw = f.width(ttl) * ts;
            g.fill(Math.round(cx - tw / 2f - 3 * k), Math.round(y - 1 * k), Math.round(cx + tw / 2f + 3 * k), Math.round(y + 7 * k), A(0.55f * ta, mix(rc, 0x000000, 0.6f)));
            txt(g, f, ttl, cx - tw / 2f, y, ts, A(ta, mix(rc, 0xFFFFFF, 0.3f)), false);
            y += 9 * k;
        }
        float maxW = 0;
        for (int i = 0; i < n; i++) {                                                                // the name(s), typed in left to right
            String name = all.get(i).title(); float w = f.width(name) * ns, x = cx - w / 2f, lt = tt - i * 0.1f; maxW = Math.max(maxW, w);
            int ncol = all.get(i).color() & 0xFFFFFF;
            float shine = rng ? ((t * 0.8f) % 1.8f) - 0.4f : -9f;
            for (int ch = 0; ch < name.length(); ch++) {
                String s = String.valueOf(name.charAt(ch)); float cw = f.width(s) * ns, u = sm((lt - ch * 0.016f) / 0.12f);
                if (u <= 0f) break;
                float rel = (x - (cx - w / 2f)) / Math.max(1f, w); int cc = Math.abs(rel - shine) < 0.1f ? mix(ncol, 0xFFFFFF, 0.7f) : ncol;
                outlined(g, f, s, x, y + (1f - u) * 2 * k, ns, u * wa, cc);
                x += cw;
            }
            y += 10 * ns;
        }
        float lu = eo((tt - 0.1f) / 0.3f), lw = (maxW / 2f + 6 * k) * lu;                           // a line drawing itself open
        if (lu > 0f) { g.fill(Math.round(cx - lw), Math.round(y + 1), Math.round(cx + lw), Math.round(y + 2), A(0.75f * wa, lite)); g.fill(Math.round(cx - lw), Math.round(y + 2), Math.round(cx + lw), Math.round(y + 3), A(0.3f * wa, 0x000000)); }
        y += 4 * k;
        int li = 0;
        for (String line : new String[]{odds, price}) {                                              // the chance, then the price: short lines
            if (line.isEmpty()) continue;
            float u = sm((tt - 0.25f - li * 0.1f) / 0.25f);
            outlined(g, f, line, cx - f.width(line) * ds / 2f, y + (1f - u) * 2 * k, ds, u * wa * 0.95f, line == price ? 0xFFD98A : 0xC8CEDC);
            y += 8 * k; li++;
        }
    }

    // ---- helpers
    private static void txt(GuiGraphicsExtractor g, Font f, String s, float x, float y, float sc, int argb, boolean shadow) {
        Gfx.push(g); Gfx.translate(g, x, y); Gfx.scale(g, sc); Gfx.txt(g, f, s, 0, 0, argb, shadow); Gfx.pop(g);
    }
    /** Text with a black outline, like the game's own drop messages. */
    private static void outlined(GuiGraphicsExtractor g, Font f, String s, float x, float y, float sc, float a, int rgb) {
        if (a < 0.03f) return;
        Gfx.push(g); Gfx.translate(g, x, y); Gfx.scale(g, sc);
        int o = A(a * 0.85f, 0x000000);
        Gfx.txt(g, f, s, -1, 0, o, false); Gfx.txt(g, f, s, 1, 0, o, false); Gfx.txt(g, f, s, 0, -1, o, false); Gfx.txt(g, f, s, 0, 1, o, false);
        Gfx.txt(g, f, s, 0, 0, A(a, rgb), false); Gfx.pop(g);
    }
    /** One art square, snapped to the pixel grid. */
    private static void sq(GuiGraphicsExtractor g, float x, float y, int s, int argb) {
        if ((argb >>> 24) < 4) return;
        int ix = Math.round(x - s / 2f), iy = Math.round(y - s / 2f); g.fill(ix, iy, ix + s, iy + s, argb);
    }
    /** A ring of squares; flat squashes it into an ellipse. */
    private static void squareRing(GuiGraphicsExtractor g, float cx, float cy, float r, float flat, int s, int argb, int n) {
        if ((argb >>> 24) < 4 || r < 1) return;
        for (int i = 0; i < n; i++) { double an = i * 2 * Math.PI / n; sq(g, cx + (float) Math.cos(an) * r, cy + (float) Math.sin(an) * r * flat, s, argb); }
    }
    /** A pixel diamond with a bright middle. */
    private static void diamond(GuiGraphicsExtractor g, float cx, float cy, float r, int s, int rim, int mid) {
        int n = Math.max(1, Math.round(r / s));
        for (int dy = -n; dy <= n; dy++) { int w = n - Math.abs(dy); int x0 = Math.round(cx - (w + 0.5f) * s), y0 = Math.round(cy + (dy - 0.5f) * s);
            g.fill(x0, y0, x0 + (2 * w + 1) * s, y0 + s, Math.abs(dy) <= n / 2 ? mid : rim); }
    }
    /** A four-point star: a square with arms that grow and shrink. */
    private static void twinkle(GuiGraphicsExtractor g, float x, float y, int s, float a, int rgb) {
        if (a < 0.05f) return;
        int arm = Math.round(a * 2) * s, ix = Math.round(x), iy = Math.round(y);
        g.fill(ix - arm, iy, ix + s + arm, iy + s, A(a * 0.8f, rgb)); g.fill(ix, iy - arm, ix + s, iy + s + arm, A(a * 0.8f, rgb)); g.fill(ix, iy, ix + s, iy + s, A(a, 0xFFFFFF));
    }
}
