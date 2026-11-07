package dev.fancydrops;

import dev.fancydrops.AnimationManager.Anim;
import dev.fancydrops.AnimationManager.Item;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

/**
 * The drop animations (1.0): drawn straight on the screen every frame (soft lights, rings, rays and sparks on the GPU), so they move
 * like a video at your full frame rate and cost almost nothing. Every group shares one clean structure in its own colour:
 *
 *   OPENING (until the drop appears): the screen dims softly round the middle, motes of light spiral in, a core of light gathers and
 *     beats three times (a ring rolls out on each beat). RNG also brings in the cinematic bars, slow light rays and a deeper dim.
 *   THE DROP: a flash and a shockwave; the item springs in on a halo (RNG: a second ring, a burst of sparks and a turning flare).
 *   THE WORDS: the name rises in letter by letter under a line that draws itself open, then the odds and the price; RNG adds a small
 *     "RNG DROP" title and a shine that runs across the name. They stay 2 s after the animation ends, then fade.
 *   NO DROPS: the core gathers and beats, then folds in on itself and puffs away.
 *
 * The editors in the menu (Item / Text / Bars) still use the classic scene for their previews.
 */
final class DropShow {
    private DropShow() {}

    private static float cl(float v) { return v < 0 ? 0 : v > 1 ? 1 : v; }
    private static float sm(float v) { v = cl(v); return v * v * (3 - 2 * v); }
    private static float eo(float v) { v = cl(v); return 1f - (1f - v) * (1f - v) * (1f - v); }
    private static float spring(float v) { v = cl(v); return 1f - (float) (Math.exp(-6.5 * v) * Math.cos(10 * v)); }
    private static float rnd(int n) { return Ritual.rnd(n); }
    private static int mix(int a, int b, float f) { return Ritual.mixc(a, b, cl(f)); }
    private static int A(float a, int rgb) { return Gfx.argb(Math.round(cl(a) * 255), rgb & 0xFFFFFF); }

    static void draw(GuiGraphicsExtractor g, Font font, Anim a, float t, int W, int H) {
        FancyConfig c = FancyConfig.get();
        int type = a.type; boolean rng = type == AnimationManager.RNG, nod = type == AnimationManager.NO_DROPS;
        float A = AnimationManager.animLen(type), O = AnimationManager.opening(type), S = a.total(), p = t - O;
        Item it = a.lead();
        int gc = Categories.group(a.group).color() & 0xFFFFFF, rc = it == null ? 0x9AA3B5 : it.color() & 0xFFFFFF;
        int col = nod ? 0x9AA3B5 : rng ? mix(gc, rc, 0.5f) : gc, lite = mix(col, 0xFFFFFF, 0.55f);
        float k = Math.min(W, H) / 270f * Math.max(0.6f, Math.min(1.4f, c.animScale)) * (rng ? 1.1f : 1f);
        float cx = W / 2f, cy = H * (c.animPos == 0 ? 0.3f : c.animPos == 2 ? 0.58f : 0.42f);
        float sceneA = (1f - sm((t - (A - 0.5f)) / 0.7f)) * sm(t / 0.25f);                  // the lights end with the animation; the words stay on
        float wordsA = 1f - sm((t - (S - 0.55f)) / 0.5f);

        // a small shake on an RNG drop
        float sh = rng && p > 0 && p < 0.45f ? (1f - p / 0.45f) * 2.2f * k : 0f;
        Gfx.push(g); if (sh > 0) Gfx.translate(g, (rnd((int) (t * 90)) - 0.5f) * 2 * sh, (rnd((int) (t * 90) + 7) - 0.5f) * 2 * sh);

        // ---- the dim round the middle (one soft picture), deeper for RNG
        float dim = sceneA * (rng ? 0.62f : 0.38f) * sm(t / 0.5f);
        NebulaTex.glow(g, cx, cy, W * 0.55f, H * 0.5f, A(dim, 0x03040A));

        // ---- RNG: cinematic bars and slow light rays
        if (rng && c.anim(a.key()).bars) {
            float b = Math.min(sm((t - (O - 1.0f)) / 0.7f), 1f - sm((t - (S - 0.45f)) / 0.4f)), bh = H * 0.085f * eo(b);
            if (bh > 0.5f) { g.fill(0, 0, W, Math.round(bh), A(0.94f, 0x04050A)); g.fill(0, H - Math.round(bh), W, H, A(0.94f, 0x04050A));
                g.fill(0, Math.round(bh), W, Math.round(bh) + 1, A(0.5f * b, lite)); g.fill(0, H - Math.round(bh) - 1, W, H - Math.round(bh), A(0.5f * b, lite)); }
        }
        if (rng) {
            float ra = sceneA * sm((t - O * 0.4f) / (O * 0.6f)) * (p > 0 ? 1f : 0.55f);
            for (int i = 0; i < 10; i++) {
                float an = i * 0.6283f + t * 0.18f * (i % 2 == 0 ? 1 : -1), len = (90 + 50 * rnd(i)) * k * (1f + 0.25f * eo(p / 0.6f));
                ray(g, cx, cy, an, len, (2.2f + 1.6f * rnd(i + 3)) * k, A(ra * (0.16f + 0.08f * (float) Math.sin(t * 2 + i)), lite));
            }
        }

        // ---- the opening: motes spiral in, the core gathers and beats three times
        float gather = sm(t / O), core = (nod ? 6 : 8) * k * (0.3f + 0.7f * gather);
        int motes = rng ? 46 : 26;
        if (p < 0.15f) for (int i = 0; i < motes; i++) {
            float life = 1.15f + 0.6f * rnd(i * 5), u = ((t + rnd(i * 3) * life) % life) / life;
            if (t + 0.05f < rnd(i * 3) * life * 0.3f) continue;
            float an = rnd(i * 7) * 6.2832f + u * 2.4f, r = (1f - eo(u)) * (70 + 60 * rnd(i)) * k + core;
            float x = cx + (float) Math.cos(an) * r, y = cy + (float) Math.sin(an) * r * 0.75f, al = (float) Math.sin(Math.PI * u) * sceneA * (1f - cl(p / 0.15f));
            NebulaTex.glow(g, x, y, 3.2f * k, 3.2f * k, A(0.55f * al, i % 3 == 0 ? 0xFFFFFF : lite));
        }
        float beatPulse = 0f;
        for (int b = 0; b < 3; b++) {
            float bt = t - new float[]{0.34f, 0.58f, 0.80f}[b] * O; if (bt < 0) continue;
            beatPulse = Math.max(beatPulse, 1f - cl(bt / 0.35f));
            if (bt < 0.8f) { float u = eo(bt / 0.8f); ring(g, cx, cy, core + u * (40 + 14 * b) * k, 0.75f, A((1f - u) * 0.8f * sceneA, lite), 1.2f * k); }
        }
        if (p < 0f) {
            float cr = core * (1f + 0.35f * beatPulse);
            NebulaTex.glow(g, cx, cy, cr * 4.5f, cr * 4.5f, A(0.35f * sceneA, col));
            NebulaTex.glow(g, cx, cy, cr * 1.8f, cr * 1.8f, A(0.85f * sceneA, lite));
            NebulaTex.glow(g, cx, cy, cr * 0.8f, cr * 0.8f, A(sceneA, 0xFFFFFF));
        }

        // ---- the drop: a flash, a shockwave (or, for No Drops, the core folding away)
        if (p >= 0f) {
            if (nod) {
                float u = cl(p / 0.9f), cr = core * (1f - eo(u));
                NebulaTex.glow(g, cx, cy, cr * 2f + 1, cr * 2f + 1, A((1f - u) * sceneA, lite));
                for (int i = 0; i < 10; i++) { float an = i * 0.628f, d = eo(u) * 26 * k; NebulaTex.glow(g, cx + (float) Math.cos(an) * d, cy + (float) Math.sin(an) * d * 0.7f - u * 8 * k, 5 * k * (1f - u * 0.5f), 5 * k * (1f - u * 0.5f), A(0.35f * (1f - u) * sceneA, 0xB8C0D0)); }
            } else {
                float fl = 1f - cl(p / (rng ? 0.55f : 0.4f));
                if (fl > 0f) { NebulaTex.glow(g, cx, cy, (rng ? 120 : 70) * k * (1.2f - fl * 0.2f), (rng ? 120 : 70) * k, A(fl * fl * (rng ? 0.95f : 0.7f), 0xFFFFFF)); }
                for (int w = 0; w < (rng ? 2 : 1); w++) {
                    float u = cl((p - w * 0.12f) / (rng ? 1.0f : 0.8f)); if (u <= 0f || u >= 1f) continue;
                    ring(g, cx, cy, 14 * k + eo(u) * (rng ? 150 : 95) * k, 0.72f, A((1f - u) * 0.9f * sceneA, w == 0 ? 0xFFFFFF : lite), (2.2f - 1.4f * u) * k);
                }
                if (rng && p < 1.3f) for (int i = 0; i < 56; i++) {                              // a burst of sparks, falling a little
                    float an = rnd(i * 13) * 6.2832f, sp = (60 + 110 * rnd(i * 3)) * k, u = cl(p / 1.3f), d = eo(u) * sp;
                    float x = cx + (float) Math.cos(an) * d, y = cy + (float) Math.sin(an) * d * 0.8f + u * u * 30 * k;
                    float al = (1f - u) * sceneA; int sc = i % 3 == 0 ? 0xFFFFFF : i % 3 == 1 ? lite : col;
                    g.fill(Math.round(x), Math.round(y), Math.round(x) + Math.max(1, Math.round(k)), Math.round(y) + Math.max(1, Math.round(k)), A(al, sc));
                    if (i % 4 == 0) NebulaTex.glow(g, x, y, 3 * k, 3 * k, A(0.5f * al, sc));
                }
            }
        }

        // ---- the item(s) on a halo
        float itemA = 0f, iy = cy;
        if (it != null && !nod && p >= 0f) {
            List<Item> all = a.items(); int n = all.size();
            float gap = 34 * k * Math.min(1f, 3f / Math.max(1, n)), x0 = cx - gap * (n - 1) / 2f;
            itemA = sm(p / 0.18f) * (1f - sm((t - (S - 0.9f)) / 0.8f));
            for (int i = 0; i < n; i++) {
                Item cur = all.get(i); float pi = p - i * 0.08f; if (pi < 0) continue;
                float s = (rng ? 2.3f : 1.9f) * k * (n > 3 ? 0.75f : 1f) * spring(pi / 0.75f), bob = (float) Math.sin(t * 2.2f + i) * 1.6f * k;
                float x = x0 + gap * i, y = cy + bob;
                float halo = (0.85f + 0.15f * (float) Math.sin(t * 3 + i)) * itemA;
                NebulaTex.glow(g, x, y, 30 * k, 30 * k, A(0.35f * halo, col));
                NebulaTex.glow(g, x, y, 13 * k, 13 * k, A(0.45f * halo, lite));
                if (rng) for (int r = 0; r < 4; r++) ray(g, x, y, r * 0.785f + t * 0.5f, 26 * k, 0.9f * k, A(0.35f * halo, 0xFFFFFF));
                ItemIcons.drawAlpha = cl(itemA); boolean still0 = Icons.STILL_ICONS; Icons.STILL_ICONS = false;
                try { ItemIcons.draw(g, cur.id, cur.icon, Math.round(x), Math.round(y), s); } catch (Throwable ignored) { } finally { ItemIcons.drawAlpha = 1f; Icons.STILL_ICONS = still0; }
                for (int q = 0; q < 5; q++) {                                               // little glints twinkling round it
                    float cyc = t * 1.4f + q * 0.21f + i, ph = cyc - (float) Math.floor(cyc); int slot = (int) Math.floor(cyc) * 17 + q * 5 + i * 3;
                    float an = rnd(slot) * 6.2832f, rad = (12 + 9 * rnd(slot + 1)) * k, tw = (float) Math.sin(Math.PI * ph) * itemA;
                    float gx = x + (float) Math.cos(an) * rad, gy = y + (float) Math.sin(an) * rad * 0.8f, arm = (1.5f + 1.5f * rnd(slot + 2)) * k * tw;
                    g.fill(Math.round(gx - arm), Math.round(gy), Math.round(gx + arm) + 1, Math.round(gy) + 1, A(tw, 0xFFFFFF)); g.fill(Math.round(gx), Math.round(gy - arm), Math.round(gx) + 1, Math.round(gy + arm) + 1, A(tw, 0xFFFFFF));
                }
            }
            iy = cy + (n > 3 ? 14 : 20) * k;
        }
        Gfx.pop(g);

        // ---- the words
        if (it != null && !nod) words(g, font, a, it, p, t, cx, iy + 8 * k, k, wordsA, rng, col, lite, rc);
    }

    private static void words(GuiGraphicsExtractor g, Font f, Anim a, Item it, float p, float t, float cx, float top, float k, float wa, boolean rng, int col, int lite, int rc) {
        float tt = p - 0.25f; if (tt < 0f || wa < 0.02f) return;
        float y = top;
        if (rng) {                                                                            // a small tracked title
            String ttl = "R N G   D R O P"; float ta = sm(tt / 0.3f) * wa, ts = 0.7f * k;
            txt(g, f, ttl, cx - f.width(ttl) * ts / 2f, y - 2 * k * (1f - ta), ts, A(ta, rc), false);
            y += 10 * k;
        }
        List<Item> all = a.items(); int n = Math.min(4, all.size());
        float ns = (n > 1 ? 1.15f : 1.45f) * k;
        for (int i = 0; i < n; i++) {                                                         // the name(s), letter by letter, rising into place
            String name = all.get(i).title(); float w = f.width(name) * ns, x = cx - w / 2f, lt = tt - i * 0.12f;
            int ncol = all.get(i).color() & 0xFFFFFF;
            float shine = rng ? ((t * 0.7f) % 1.6f) - 0.3f : -9f;                           // RNG: a shine running across the name
            for (int ch = 0; ch < name.length(); ch++) {
                String s = String.valueOf(name.charAt(ch)); float cw = f.width(s) * ns, u = sm((lt - ch * 0.022f) / 0.28f);
                if (u <= 0f) { x += cw; continue; }
                float rel = (x - (cx - w / 2f)) / Math.max(1f, w); int cc = Math.abs(rel - shine) < 0.12f ? mix(ncol, 0xFFFFFF, 0.75f) : ncol;
                txt(g, f, s, x, y + (1f - u) * 6 * k, ns, A(u * wa, cc), true);
                x += cw;
            }
            y += 11 * ns / k * k;
        }
        float lu = eo((tt - 0.15f) / 0.45f), lw = 70 * k * lu;                                // a line drawing itself open
        if (lu > 0f) { g.fill(Math.round(cx - lw), Math.round(y + 1), Math.round(cx + lw), Math.round(y + 2), A(0.7f * wa, lite)); NebulaTex.glow(g, cx, y + 1.5f, lw, 3 * k, A(0.25f * wa, col)); }
        y += 6 * k;
        String odds = (it.odds == null || it.odds.isEmpty() ? "" : it.odds) + (it.chance != null && !it.chance.isEmpty() && !it.chance.equals(it.odds) ? (it.odds == null || it.odds.isEmpty() ? "" : "  ·  ") + it.chance : "") + (it.mf != null && !it.mf.isEmpty() ? "  ·  " + it.mf + " MF" : "");
        String price = it.price > 0 ? PriceService.fmt(it.price * Math.max(1, it.amount)) + " coins" + (it.priceSource == null || it.priceSource.isEmpty() ? "" : "  ·  " + it.priceSource) : it.price == -2 ? "pricing..." : "";
        float ds = 0.85f * k;
        for (String line : new String[]{odds, price}) {
            if (line.isEmpty()) continue;
            float u = sm((tt - 0.35f) / 0.35f);
            txt(g, f, line, cx - f.width(line) * ds / 2f, y + (1f - u) * 4 * k, ds, A(u * wa * 0.9f, line == price ? 0xFFD98A : 0xC8CEDC), true);
            y += 10 * k; tt -= 0.12f;
        }
    }

    // ---- helpers
    private static void txt(GuiGraphicsExtractor g, Font f, String s, float x, float y, float sc, int argb, boolean shadow) {
        Gfx.push(g); Gfx.translate(g, x, y); Gfx.scale(g, sc); Gfx.txt(g, f, s, 0, 0, argb, shadow); Gfx.pop(g);
    }
    /** A ring of short bars; flat squashes it into an ellipse. */
    private static void ring(GuiGraphicsExtractor g, float cx, float cy, float r, float flat, int argb, float w) {
        if ((argb >>> 24) < 4 || r < 1) return;
        int n = Math.max(24, Math.min(72, Math.round(r * 0.6f)));
        for (int i = 0; i < n; i++) {
            double a0 = i * 2 * Math.PI / n, a1 = (i + 1) * 2 * Math.PI / n;
            float x0 = cx + (float) Math.cos(a0) * r, y0 = cy + (float) Math.sin(a0) * r * flat, x1 = cx + (float) Math.cos(a1) * r, y1 = cy + (float) Math.sin(a1) * r * flat;
            float len = (float) Math.hypot(x1 - x0, y1 - y0);
            Gfx.push(g); Gfx.translate(g, x0, y0); Gfx.rotate(g, (float) Math.atan2(y1 - y0, x1 - x0)); Gfx.scale(g, 1f, Math.max(1f, w));
            g.fill(0, 0, Math.max(1, Math.round(len + 0.6f)), 1, argb); Gfx.pop(g);
        }
    }
    /** A soft ray from the centre outwards: one stretched glow. */
    private static void ray(GuiGraphicsExtractor g, float cx, float cy, float an, float len, float w, int argb) {
        if ((argb >>> 24) < 3) return;
        Gfx.push(g); Gfx.translate(g, cx, cy); Gfx.rotate(g, an);
        NebulaTex.glow(g, len / 2f, 0, len / 2f, w, argb); Gfx.pop(g);
    }
}
