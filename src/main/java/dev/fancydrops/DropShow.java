package dev.fancydrops;

import dev.fancydrops.AnimationManager.Anim;
import dev.fancydrops.AnimationManager.Item;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * The drop animations (2.0): little pixel-art cutscenes, every shape whole cells on one grid ({@code U} GUI px), drawn fresh each
 * frame so they run at your full frame rate.
 *
 *   THE STRIKES: a rock of the group's colour sits on a strip of ground. Three strikes land on it (a pickaxe for the mining and
 *     treasure groups, a blade's slash for the rest): chips fly, the rock jolts, a crack runs in. Light leaks from the cracks and
 *     flickers through colours; at the third strike it settles on the true colour - the only tell, and an honest one.
 *   THE BURST: the rock breaks into its cells, which fly and fall; a flash of cells, a ring going out, a column of light.
 *   THE SHOWCASE: the item rises out of the light, pops to its size and floats, rays of cells turning behind it, sparkles and motes.
 *     Under it: the name typed in, the odds, and the price counting up.
 *   RNG is its own show: the HUD goes, bars and a glow of cells round the screen's edges pulse with every strike, a bigger rock with
 *     veins, the screen shakes, a pause where the light pours out before it bursts, a big showcase and a stamped banner.
 *   NO DROPS: three strikes, grey cracks, and the rock crumbles to dust.
 */
final class DropShow {
    private DropShow() {}

    private static float cl(float v) { return v < 0 ? 0 : v > 1 ? 1 : v; }
    private static float sm(float v) { v = cl(v); return v * v * (3 - 2 * v); }
    private static float eo(float v) { v = cl(v); return 1f - (1f - v) * (1f - v) * (1f - v); }
    private static float backOut(float v) { v = cl(v); float c1 = 1.9f, c3 = c1 + 1; return 1 + c3 * (v - 1) * (v - 1) * (v - 1) + c1 * (v - 1) * (v - 1); }
    private static float rnd(int n) { return Ritual.rnd(n); }
    private static int mix(int a, int b, float f) { return Ritual.mixc(a, b, cl(f)); }
    private static int A(float a, int rgb) { return Gfx.argb(Math.round(cl(a) * 255), rgb & 0xFFFFFF); }
    private static final int[] FLICKER = {0xFFFFFF, 0x55FFFF, 0xFFAA00, 0xFF55FF, 0x55FF55, 0x5555FF, 0xFF5555, 0xAA00AA};
    private static final float[] BEATS = {0.34f, 0.58f, 0.80f};

    /** The grid, in GUI px. */
    private static float U = 2f;

    static void draw(GuiGraphicsExtractor g, Font font, Anim a, float t, int W, int H) {
        FancyConfig c = FancyConfig.get();
        int type = a.type; boolean rng = type == AnimationManager.RNG, nod = type == AnimationManager.NO_DROPS;
        float A = AnimationManager.animLen(type), O = AnimationManager.opening(type), S = a.total(), p = t - O;
        Item it = a.lead();
        int gc = Categories.group(a.group).color() & 0xFFFFFF, rc = it == null ? 0x9AA3B5 : it.color() & 0xFFFFFF;
        int truth = nod ? 0x8A92A3 : rng ? rc : mix(gc, rc, 0.6f);
        float k = Math.max(0.75f, Math.min(1.5f, c.animScale));
        U = Math.max(1f, Math.round(H / 180f * k * 2f) / 2f);
        float ts = Math.max(1f, Math.min(1.35f, H * 0.7f / 150f)) * k;
        int seed = a.group.hashCode() * 31 + (it == null ? 7 : it.name.hashCode());
        boolean pick = switch (a.group) { case "mobs", "nucleus", "corpse", "fiesta", "treasure" -> true; default -> false; };

        float cx = W / 2f;
        float cy = rng ? H * (c.animPos == 0 ? 0.36f : 0.42f) : c.animPos == 0 ? H * 0.22f : c.animPos == 2 ? H - 64 - 52 * ts : H * 0.40f;
        float fxA = (1f - sm((t - (A - 0.3f)) / 0.3f)) * sm(t / 0.15f);
        float wordsA = 1f - sm((t - (S - 0.45f)) / 0.45f);
        float sceneA = Math.max(fxA, wordsA * (p > 0 ? 1f : 0f));

        // the light in the cracks: flickers while it builds, the truth from the third strike on
        float settle = BEATS[2] * O;
        int col = t >= settle || nod ? truth : mix(FLICKER[Math.floorMod((int) (t * (7f + 9f * t / settle)) * 5 + seed, FLICKER.length)], gc, 0.25f);
        int lite = mix(col, 0xFFFFFF, 0.5f);

        // which strike we are on, and how hard the last one landed
        int struck = 0; float hit = 0f;
        for (int b = 0; b < 3; b++) { float bt = t - BEATS[b] * O; if (bt >= 0) { struck = b + 1; hit = 1f - cl(bt / (rng ? 0.3f : 0.2f)); } }
        float wait = rng && p < 0 && t > settle ? sm((t - settle) / (O - settle)) : 0f;                  // RNG: the pause before the burst

        // ---- RNG: bars, and a glow of cells round the screen's edges that pulses with the strikes
        if (rng) {
            if (c.anim(a.key()).bars) {
                float b = Math.min(sm(t / 0.5f), 1f - sm((t - (S - 0.35f)) / 0.3f)); int bh = Math.round(H * 0.075f * eo(b));
                if (bh > 0) { g.fill(0, 0, W, bh, A(0.95f, 0x030308)); g.fill(0, H - bh, W, H, A(0.95f, 0x030308)); }
            }
            float pulse = fxA * (0.18f + 0.5f * hit + 0.3f * wait + (p >= 0 ? 0.55f * (1f - cl(p / 1.2f)) : 0f)) + (p >= 0 ? 0.12f * wordsA : 0f);
            edges(g, W, H, col, pulse);
            NebulaTex.glow(g, cx, cy, W * 0.42f, H * 0.42f, A(0.5f * sceneA, 0x020308));
        }

        // a shake: the strikes and the burst (RNG only)
        float sh = 0f;
        if (rng) { sh = hit * 1.4f + wait * 0.8f; if (p >= 0 && p < 0.4f) sh = Math.max(sh, (1f - p / 0.4f) * 3f); }
        Gfx.push(g); if (sh > 0.1f) Gfx.translate(g, Math.round((rnd((int) (t * 55) + seed) - 0.5f) * 2 * sh * U / 2), Math.round((rnd((int) (t * 55) + 7) - 0.5f) * 2 * sh * U / 2));

        // ---- the rock and its ground
        int rw = rng ? 18 : 13, rh = rng ? 13 : 10;                                                       // in cells
        float cell = rng ? U * 2f : U * 1.5f;
        float rockY = rng ? cy + 6 * U : cy;                                                               // the rock's centre
        float groundY = rockY + rh * cell / 2f;
        ground(g, cx, groundY, rng ? 34 : 24, cell, gc, fxA, seed);
        int rock = mix(gc, 0x3C3A48, 0.72f);
        if (p < 0f) {
            float jolt = hit * hit * cell * 0.8f, trem = (wait > 0 ? (rnd((int) (t * 70)) - 0.5f) * 2 * wait * cell * 0.6f : 0f);
            rock(g, cx + trem, rockY + jolt, rw, rh, cell, rock, col, lite, struck, t - BEATS[Math.max(0, struck - 1)] * O, fxA, seed, rng, wait, nod);
            if (rng && wait > 0) beam(g, cx, rockY - rh * cell * 0.3f, H * 0.5f * wait, cell, lite, fxA * wait * 0.8f, t);
            for (int b = 0; b < struck; b++) chips(g, cx + (b == 1 ? -1 : 1) * rw * cell * 0.25f, rockY - rh * cell * 0.35f, t - BEATS[b] * O, cell, rock, lite, fxA, seed + b * 97, rng);
            striker(g, cx, rockY, rw, rh, cell, t, O, pick, lite, fxA, rng);
        } else if (nod) {
            crumble(g, cx, rockY, rw, rh, cell, rock, p, fxA, seed);
        } else {
            shatter(g, cx, rockY, rw, rh, cell, rock, col, lite, p, fxA, seed, rng);
        }
        for (int b = 0; b < struck && p < 0.6f; b++) impact(g, cx + (b == 1 ? -1 : 1) * rw * cell * 0.22f, rockY - rh * cell * 0.4f, t - BEATS[b] * O, cell, lite, fxA, rng);

        // ---- the showcase
        float size = 0f, iy = cy;
        if (it != null && !nod && p >= 0f) {
            float rise = eo(p / (rng ? 0.7f : 0.5f));
            float pop = backOut(p / (rng ? 0.65f : 0.5f));
            size = (rng ? H * 0.16f : H * 0.085f) * k * (0.35f + 0.65f * pop);
            iy = rockY + (cy - (rng ? 2 * U : 8 * U) - rockY) * rise;
            float itemA = sm(p / 0.12f) * (1f - sm((t - (S - 0.55f)) / 0.5f));
            hype(g, cx, iy, H, col, lite, rc, p, t, itemA, rng, seed, size);
            List<Item> all = a.items(); int n = all.size();
            float gap = size * 1.05f, x0 = cx - gap * (n - 1) / 2f;
            for (int i = 0; i < n; i++) {
                Item cur = all.get(i); float pi = p - i * 0.08f; if (pi < 0) continue;
                float s = (rng ? H * 0.16f : H * 0.085f) * k * (n > 3 ? 0.75f : 1f) * (0.35f + 0.65f * backOut(pi / (rng ? 0.65f : 0.5f)));
                float bob = size * 0.05f * (float) Math.sin(t * 2.3f + i) * rise;
                ItemIcons.drawAlpha = cl(itemA); boolean still0 = Icons.STILL_ICONS; Icons.STILL_ICONS = false;
                try { ItemIcons.draw(g, cur.id, cur.icon, Math.round(x0 + gap * i), Math.round(iy + bob), Math.max(0.1f, s / 16f)); } catch (Throwable ignored) { } finally { ItemIcons.drawAlpha = 1f; Icons.STILL_ICONS = still0; }
            }
        }
        Gfx.pop(g);

        // ---- the words
        if (it != null && !nod && p >= 0f) words(g, font, a, it, p, cx, iy, size, ts, wordsA, rng, rc, lite);
    }

    // ================================================================== the scene

    /** A strip of ground under the rock: three rows of cells, thinner at the ends, in from the middle. */
    private static void ground(GuiGraphicsExtractor g, float cx, float y, int w, float cell, int gc, float a, int seed) {
        if (a < 0.02f) return;
        int dirt = mix(gc, 0x24222C, 0.8f), top = mix(gc, 0x4A4656, 0.65f);
        for (int i = -w / 2; i <= w / 2; i++) {
            float e = 1f - Math.abs(i) / (w / 2f + 0.5f); if (e <= 0) continue;
            float ea = a * sm(e * 2.2f);
            int rows = e > 0.55f ? 3 : e > 0.25f ? 2 : 1;
            for (int r = 0; r < rows; r++) cellAt(g, cx + i * cell, y + r * cell, cell, A(ea * (r == 0 ? 1f : 0.8f), r == 0 ? (rnd(seed + i * 3) > 0.7f ? mix(top, 0xFFFFFF, 0.12f) : top) : rnd(seed + i * 7 + r) > 0.6f ? mix(dirt, 0x000000, 0.25f) : dirt));
        }
    }

    /** Inside the rock's outline? (i, j) cells from its top-left. */
    private static boolean inRock(int i, int j, int w, int h, int seed) {
        float dx = (i + 0.5f - w / 2f) / (w / 2f), dy = (j + 0.5f - h * 0.55f) / (h * 0.55f);
        if (j >= h - 1) dy *= 0.7f;                                                                        // a flatter bottom, it sits on the ground
        return dx * dx + dy * dy < 1f - 0.22f * rnd(seed + i * 13 + j * 7);
    }

    /** The rock: shaded cells, its cracks (one per strike so far) glowing with the light inside, RNG veins. */
    private static void rock(GuiGraphicsExtractor g, float cx, float cy, int w, int h, float cell, int base, int col, int lite, int struck, float since, float a, int seed, boolean rng, float wait, boolean nod) {
        if (a < 0.02f) return;
        float x0 = cx - w * cell / 2f, y0 = cy - h * cell / 2f;
        int hiC = mix(base, 0xFFFFFF, 0.22f), loC = mix(base, 0x000000, 0.35f);
        float glowA = struck == 0 ? 0f : (0.55f + 0.45f * (1f - cl(since / 0.35f))) * (nod ? 0.5f : 1f) + wait * 0.5f;
        if (glowA > 0 && !nod) NebulaTex.glow(g, cx, cy, w * cell * 0.9f, h * cell * 0.9f, A(a * 0.22f * cl(glowA), col));
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) {
            if (!inRock(i, j, w, h, seed)) continue;
            boolean edgeTL = !inRock(i - 1, j, w, h, seed) || !inRock(i, j - 1, w, h, seed);
            boolean edgeBR = !inRock(i + 1, j, w, h, seed) || !inRock(i, j + 1, w, h, seed);
            int cc = edgeTL ? hiC : edgeBR ? loC : rnd(seed + i * 5 + j * 11) > 0.78f ? mix(base, 0x000000, 0.15f) : base;
            if (rng && !edgeTL && !edgeBR && Math.abs((i - w / 2f) * 0.6f + (j - h / 2f) - 2 * (float) Math.sin(i * 0.9f + seed)) < 0.6f) cc = mix(col, 0x000000, 0.25f);   // a vein
            cellAt(g, x0 + i * cell, y0 + j * cell, cell, A(a, cc));
        }
        // cracks: from the top edge in towards the middle, one per strike, growing as it lands
        for (int b = 0; b < struck; b++) {
            float grow = b < struck - 1 ? 1f : eo(since / 0.18f);
            int len = Math.round((rng ? 6 : 4) + b * 2);
            int ci = w / 2 + (b == 1 ? -2 : b == 2 ? 0 : 2), cj = 1;
            for (int s = 0; s < Math.round(len * grow); s++) {
                ci += rnd(seed + b * 41 + s) > 0.5f ? 1 : -1; cj += 1; if (cj >= h - 1) break;
                if (!inRock(ci, cj, w, h, seed)) continue;
                int cc = nod ? mix(0x2A2A30, 0x9AA3B5, glowA * 0.6f) : mix(lite, 0xFFFFFF, 0.3f * (1f - cl(since / 0.3f)));
                cellAt(g, x0 + ci * cell, y0 + cj * cell, cell, A(a * (nod ? 1f : 0.55f + 0.45f * cl(glowA)), cc));
            }
        }
    }

    /** The pickaxe or the blade: winds up, comes down on each beat, rests between. */
    private static void striker(GuiGraphicsExtractor g, float cx, float cy, int w, int h, float cell, float t, float O, boolean pick, int lite, float a, boolean rng) {
        if (a < 0.02f) return;
        float next = -1f, prev = -9f;
        for (float b : BEATS) { float bt = b * O; if (bt > t) { next = bt; break; } prev = bt; }
        if (pick) {
            // the swing: 0 rests raised, 1 is the blow
            float sw;
            if (next > 0 && next - t < 0.13f) sw = sm(1f - (next - t) / 0.13f);
            else if (t - prev < 0.12f) sw = 1f - eo((t - prev) / 0.12f) * 0.9f;
            else sw = 0.1f;
            float side = (rng ? 1f : 1f);
            float px = cx + w * cell * 0.62f * side, py = cy - h * cell * 0.15f;                         // the grip
            float ang = (float) Math.toRadians(-150 + 85 * sw);                                          // the head travels from up-right to the rock's top
            float len = (rng ? 12 : 9) * cell;
            float hx = px + (float) Math.cos(ang) * len, hy = py + (float) Math.sin(ang) * len;
            line(g, px, py, hx, hy, cell, A(a, 0x8A5A2B));
            float nx = -(float) Math.sin(ang), ny = (float) Math.cos(ang), hl = (rng ? 5 : 4) * cell;
            line(g, hx - nx * hl, hy - ny * hl, hx + nx * hl, hy + ny * hl, cell, A(a, 0xB8C2D6));
            cellAt(g, hx + nx * hl, hy + ny * hl, cell, A(a, 0xE8EEF8)); cellAt(g, hx - nx * hl, hy - ny * hl, cell, A(a, 0x6E7890));
        } else if (next > 0 && next - t < 0.1f || t - prev < 0.16f) {
            // the slash: a streak of cells across the rock, white hot, then its colour, fading
            float u = t - prev < 0.16f ? cl((t - prev) / 0.16f) : 0f; boolean before = !(t - prev < 0.16f);
            if (before) return;
            int b = 0; for (int i = 0; i < 3; i++) if (BEATS[i] * O <= t) b = i;
            float dir = b % 2 == 0 ? 1f : -1f, L = w * cell * 0.85f;
            float x1 = cx - dir * L, y1 = cy - h * cell * 0.8f, x2 = cx + dir * L * 0.9f, y2 = cy + h * cell * 0.25f;
            float head = eo(u * 2.2f), tail = eo(u * 1.4f - 0.1f);
            float ax = x1 + (x2 - x1) * tail, ay = y1 + (y2 - y1) * tail, bx = x1 + (x2 - x1) * head, by = y1 + (y2 - y1) * head;
            line(g, ax, ay, bx, by, cell, A(a * (1f - u), mix(0xFFFFFF, lite, u)));
            line(g, ax, ay + cell, bx, by + cell, cell, A(a * (1f - u) * 0.5f, lite));
        }
    }

    /** A strike's flash: a cross of cells, white, gone in a moment. */
    private static void impact(GuiGraphicsExtractor g, float x, float y, float since, float cell, int lite, float a, boolean rng) {
        if (since < 0 || since > 0.16f) return;
        float u = since / 0.16f; int arm = Math.round((rng ? 4 : 3) * (1f - u * 0.5f));
        for (int i = -arm; i <= arm; i++) { int cc = i == 0 ? 0xFFFFFF : lite; float al = a * (1f - u) * (1f - Math.abs(i) / (arm + 1f));
            cellAt(g, x + i * cell, y, cell, A(al, cc)); if (i != 0) cellAt(g, x, y + i * cell, cell, A(al, cc)); }
    }

    /** Chips knocked off by a strike: up and out, falling. */
    private static void chips(GuiGraphicsExtractor g, float x, float y, float since, float cell, int rock, int lite, float a, int seed, boolean rng) {
        float life = 0.6f; if (since < 0 || since > life) return;
        float u = since / life; int n = rng ? 9 : 6;
        for (int i = 0; i < n; i++) {
            float vx = (rnd(seed + i * 3) - 0.5f) * 26 * cell, vy = -(10 + 14 * rnd(seed + i * 5)) * cell;
            float px = x + vx * since, py = y + vy * since + 0.5f * 90 * cell * since * since;
            cellAt(g, px, py, cell, A(a * (1f - u), i % 3 == 0 ? lite : mix(rock, 0xFFFFFF, 0.15f)));
        }
    }

    /** RNG's pause: light pouring up out of the rock, a column of cells, three widths, rippling. */
    private static void beam(GuiGraphicsExtractor g, float x, float y, float h, float cell, int lite, float a, float t) {
        if (a < 0.02f) return;
        int rows = Math.round(h / cell);
        for (int r = 0; r < rows; r++) {
            float f = 1f - r / (float) rows, rip = 0.75f + 0.25f * (float) Math.sin(t * 14 - r * 0.6f);
            float al = a * f * rip;
            cellAt(g, x, y - r * cell, cell, A(al, 0xFFFFFF));
            cellAt(g, x - cell, y - r * cell, cell, A(al * 0.6f, lite)); cellAt(g, x + cell, y - r * cell, cell, A(al * 0.6f, lite));
            if (r % 2 == 0) { cellAt(g, x - 2 * cell, y - r * cell, cell, A(al * 0.25f, lite)); cellAt(g, x + 2 * cell, y - r * cell, cell, A(al * 0.25f, lite)); }
        }
    }

    /** The burst: the rock's cells fly and fall; a flash of cells; one ring (two for RNG); the column of light. */
    private static void shatter(GuiGraphicsExtractor g, float cx, float cy, int w, int h, float cell, int rock, int col, int lite, float p, float a, int seed, boolean rng) {
        float life = rng ? 1.3f : 0.9f;
        if (p < life) {
            float x0 = cx - w * cell / 2f, y0 = cy - h * cell / 2f;
            for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) {
                if (!inRock(i, j, w, h, seed) || (i + j) % 2 == 1) continue;
                float ox = x0 + i * cell - cx, oy = y0 + j * cell - cy, d = (float) Math.hypot(ox, oy) + 1f;
                float sp = (rng ? 70 : 45) * cell / 2f * (0.6f + 0.8f * rnd(seed + i * 17 + j));
                float px = cx + ox + ox / d * sp * p, py = cy + oy + (oy / d - 0.9f) * sp * p + 0.5f * 120 * cell / 2f * p * p;
                cellAt(g, px, py, cell, A(a * (1f - p / life), rnd(seed + i + j * 3) > 0.7f ? lite : mix(rock, 0xFFFFFF, 0.1f)));
            }
        }
        float fl = 1f - cl(p / (rng ? 0.35f : 0.25f));                                                // the flash: a diamond of cells
        if (fl > 0) {
            int r = Math.round((rng ? 9 : 6) * (1.2f - fl * 0.2f));
            for (int dy = -r; dy <= r; dy++) for (int dx = -r; dx <= r; dx++) {
                int m = Math.abs(dx) + Math.abs(dy); if (m > r) continue;
                cellAt(g, cx + dx * cell, cy + dy * cell, cell, A(a * fl * (1f - m / (r + 1f)), m < r / 2 ? 0xFFFFFF : lite));
            }
        }
        for (int wv = 0; wv < (rng ? 2 : 1); wv++) {                                                   // the ring going out
            float u = cl((p - wv * 0.12f) / (rng ? 0.8f : 0.55f)); if (u <= 0 || u >= 1) continue;
            float rx = (8 + eo(u) * (rng ? 70 : 36)) * U;
            ellipse(g, cx, cy, rx, rx * 0.55f, cell, A(a * (1f - u) * (1f - u), wv == 0 ? 0xFFFFFF : col));
        }
        if (p < 0.5f) beam(g, cx, cy, (rng ? 160 : 70) * U * eo(p / 0.15f), cell, lite, a * (1f - p / 0.5f), p);
    }

    /** No Drops: the rock crumbles into grey dust that falls and settles. */
    private static void crumble(GuiGraphicsExtractor g, float cx, float cy, int w, int h, float cell, int rock, float p, float a, int seed) {
        float x0 = cx - w * cell / 2f, y0 = cy - h * cell / 2f, life = 0.6f;
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) {
            if (!inRock(i, j, w, h, seed)) continue;
            float dl = rnd(seed + i * 9 + j * 5) * 0.2f + (h - j) * 0.012f, u = cl((p - dl) / life);
            float px = x0 + i * cell + (rnd(seed + i * 3 + j) - 0.5f) * 6 * cell * u, py = y0 + j * cell + u * u * (h - j) * cell;
            cellAt(g, px, py, cell, A(a * (1f - u * 0.85f), mix(0x6A6E7A, rock, 0.4f)));
        }
        for (int i = 0; i < 8; i++) {                                                                    // a puff of dust
            float u = cl(p / 0.5f); if (u >= 1) break;
            float px = cx + (i - 3.5f) * 2.5f * cell * eo(u), py = cy + h * cell * 0.4f - eo(u) * 3 * cell * (1f + rnd(seed + i));
            cellAt(g, px, py, cell, A(a * (1f - u) * 0.6f, 0xB8BEC8));
        }
    }

    /** The showcase's light: rays of cells turning, a ring as it lands, motes rising and four-point sparkles. */
    private static void hype(GuiGraphicsExtractor g, float x, float y, int H, int col, int lite, int rc, float p, float t, float a, boolean rng, int seed, float size) {
        if (a < 0.02f) return;
        float on = eo(p / 0.4f) * a;
        int n = 8; float reach = H * (rng ? 0.3f : 0.13f), turn = t * 0.35f;
        for (int r = 0; r < n; r++) {
            float an = r * (float) (Math.PI * 2 / n) + turn, ca = (float) Math.cos(an), sa = (float) Math.sin(an);
            float len = reach * (r % 2 == 0 ? 1f : 0.7f);
            for (float d = size * 0.45f; d < len; d += U * 2) {
                float f = 1f - d / len; int wdt = 1 + Math.round(d / len * (rng ? 2.5f : 1.5f));
                float al = on * f * (rng ? 0.55f : 0.45f);
                for (int q = 0; q < wdt; q++) { float off = (q - (wdt - 1) / 2f) * U; cellAt(g, x + ca * d - sa * off, y + sa * d + ca * off, U, A(al, q == wdt / 2 ? lite : col)); }
            }
        }
        if (rng && p < 0.9f) { float u = p / 0.9f; float rx = H * (0.08f + 0.35f * eo(u)); ellipse(g, x, y, rx, rx * 0.8f, U, A((1f - u) * (1f - u) * 0.6f * a, col)); }
        int motes = rng ? 18 : 8;
        for (int i = 0; i < motes; i++) {
            float life = 1.6f + 0.9f * rnd(seed + i * 71), ph = ((t + rnd(seed + i * 72) * life) % life) / life;
            float mx = x + (rnd(seed + i * 73) - 0.5f) * H * (rng ? 0.6f : 0.3f), my = y + H * (rng ? 0.2f : 0.1f) - ph * H * (rng ? 0.45f : 0.22f);
            cellAt(g, mx, my, U, A(on * 0.7f * (float) Math.sin(Math.PI * ph), i % 3 == 0 ? 0xFFFFFF : col));
        }
        int stars = rng ? 9 : 4;
        for (int i = 0; i < stars; i++) {
            double ang = rnd(seed + i * 61) * Math.PI * 2; float dist = H * ((rng ? 0.12f : 0.06f) + (rng ? 0.2f : 0.08f) * rnd(seed + i * 62));
            float period = 0.6f + 0.7f * rnd(seed + i * 63), ph = ((t + rnd(seed + i * 64) * period) % period) / period, al = on * (float) Math.sin(Math.PI * ph);
            if (al < 0.05f) continue;
            float sx = x + (float) Math.cos(ang) * dist, sy = y + (float) Math.sin(ang) * dist * 0.8f;
            cellAt(g, sx, sy, U, A(al, 0xFFFFFF));
            if (ph > 0.3f && ph < 0.7f) { int cc = A(al * 0.8f, i % 3 == 0 ? 0xFFFFFF : mix(rc, 0xFFFFFF, 0.3f));
                cellAt(g, sx - U, sy, U, cc); cellAt(g, sx + U, sy, U, cc); cellAt(g, sx, sy - U, U, cc); cellAt(g, sx, sy + U, U, cc); }
        }
    }

    /** RNG: the glow round the screen's edges as pixel art, four bands of cells, each a step weaker inwards. */
    private static void edges(GuiGraphicsExtractor g, int W, int H, int col, float a) {
        if (a < 0.02f) return;
        int u = Math.max(1, Math.round(U));
        for (int b = 0; b < 4; b++) {
            int cc = A(a * (0.55f - b * 0.13f), col), o = b * u * 2;
            g.fill(o, o, W - o, o + u * 2, cc); g.fill(o, H - o - u * 2, W - o, H - o, cc);
            g.fill(o, o + u * 2, o + u * 2, H - o - u * 2, cc); g.fill(W - o - u * 2, o + u * 2, W - o, H - o - u * 2, cc);
        }
    }

    // ================================================================== the words

    private static void words(GuiGraphicsExtractor g, Font f, Anim a, Item it, float p, float cx, float iy, float size, float ts, float wa, boolean rng, int rc, int lite) {
        if (wa < 0.02f) return;
        // RNG: the banner, stamped - in big, down to its size, a flash of its colour as it lands
        if (rng) {
            float k = cl((p - 0.15f) / 0.26f);
            if (k > 0) {
                String ban = (it.odds == null || it.odds.isEmpty() ? "RNG DROP" : it.odds.toUpperCase(java.util.Locale.ROOT)) + "!";
                float sc = ts * (1.3f + 1.2f * (1f - eo(k))), by = iy - size * 0.72f - 12 * ts;
                int bc = mix(rc, 0xFFFFFF, 0.15f);
                if (k < 1f) NebulaTex.glow(g, cx, by + 4 * sc, 80 * ts, 14 * ts, A(0.5f * (1f - k) * wa, rc));
                Component c = Component.literal(ban).withStyle(ChatFormatting.BOLD);
                outlinedC(g, f, c, cx - f.width(c) * sc / 2f, by, sc, Math.min(wa, k * 2.5f), bc);
            }
        }
        float below = iy + size * 0.62f + 4 * ts;
        // the name, a letter at a time
        List<Item> all = a.items(); int n = Math.min(4, all.size());
        float ns = (rng ? 1.6f : 1.25f) * ts * (n > 1 ? 0.8f : 1f), y = below;
        float nt = p - (rng ? 0.35f : 0.18f);
        for (int i = 0; i < n && nt > 0; i++) {
            String name = all.get(i).title(); int shown = Math.min(name.length(), (int) ((nt - i * 0.12f) * (rng ? 32 : 40)));
            if (shown > 0) {
                String part = name.substring(0, shown); float w = f.width(name) * ns;
                outlined(g, f, part, cx - w / 2f, y, ns, wa, all.get(i).color() & 0xFFFFFF);
            }
            y += 10 * ns;
        }
        // the odds, then the price counting up
        float la = sm((p - (rng ? 0.75f : 0.45f)) / 0.25f) * wa;
        if (la > 0.02f) {
            String line = (it.chance != null && !it.chance.isEmpty() ? it.chance : "") + (it.mf != null && !it.mf.isEmpty() ? (it.chance == null || it.chance.isEmpty() ? "" : "  ·  ") + it.mf + " MF" : "");
            if (!rng && it.odds != null && !it.odds.isEmpty()) line = it.odds + (line.isEmpty() ? "" : "  ·  " + line);
            if (!line.isEmpty()) { outlined(g, f, line, cx - f.width(line) * ts * 0.9f / 2f, y + 1, ts * 0.9f, la, 0xC8C8C8); y += 10 * ts; }
            if (it.price > 0) {
                float kc = cl((p - (rng ? 0.85f : 0.55f)) / (rng ? 1.1f : 0.7f));
                long total = Math.round(it.price * Math.max(1, it.amount)), shown = Math.round(total * (1f - (1f - kc) * (1f - kc) * (1f - kc)));
                String pr = String.format(java.util.Locale.ROOT, "%,d coins", shown); float ps = (rng ? 1.25f : 1f) * ts;
                outlined(g, f, pr, cx - f.width(String.format(java.util.Locale.ROOT, "%,d coins", total)) * ps / 2f, y + 1, ps, la, 0xFFD84A);
                y += 10 * ps;
                if (rng && it.priceSource != null && !it.priceSource.isEmpty()) outlined(g, f, it.priceSource, cx - f.width(it.priceSource) * ts * 0.75f / 2f, y + 1, ts * 0.75f, la * 0.7f, 0x9A9A9A);
            } else if (it.price == -2) outlined(g, f, "pricing...", cx - f.width("pricing...") * ts * 0.8f / 2f, y + 1, ts * 0.8f, la * 0.7f, 0x9A9A9A);
        }
    }

    // ================================================================== helpers

    /** Text with a black outline, like the game's own drop messages. */
    private static void outlined(GuiGraphicsExtractor g, Font f, String s, float x, float y, float sc, float a, int rgb) {
        if (a < 0.03f || s.isEmpty()) return;
        Gfx.push(g); Gfx.translate(g, x, y); Gfx.scale(g, sc);
        int o = A(a * 0.9f, 0x000000);
        Gfx.txt(g, f, s, -1, 0, o, false); Gfx.txt(g, f, s, 1, 0, o, false); Gfx.txt(g, f, s, 0, -1, o, false); Gfx.txt(g, f, s, 0, 1, o, false);
        Gfx.txt(g, f, s, 0, 0, A(a, rgb), false); Gfx.pop(g);
    }
    private static void outlinedC(GuiGraphicsExtractor g, Font f, Component s, float x, float y, float sc, float a, int rgb) {
        if (a < 0.03f) return;
        Gfx.push(g); Gfx.translate(g, x, y); Gfx.scale(g, sc);
        int o = A(a * 0.9f, 0x000000);
        Gfx.txt(g, f, s, -1, 0, o, false); Gfx.txt(g, f, s, 1, 0, o, false); Gfx.txt(g, f, s, 0, -1, o, false); Gfx.txt(g, f, s, 0, 1, o, false);
        Gfx.txt(g, f, s, 0, 0, A(a, rgb), false); Gfx.pop(g);
    }
    /** One cell, snapped to the grid. */
    private static void cellAt(GuiGraphicsExtractor g, float x, float y, float s, int argb) {
        if ((argb >>> 24) < 4) return;
        int is = Math.max(1, Math.round(s)), ix = Math.round(x / is) * is - is / 2, iy = Math.round(y / is) * is - is / 2;
        g.fill(ix, iy, ix + is, iy + is, argb);
    }
    /** A line of cells. */
    private static void line(GuiGraphicsExtractor g, float x0, float y0, float x1, float y1, float s, int argb) {
        if ((argb >>> 24) < 4) return;
        int n = Math.max(1, Math.round((float) Math.hypot(x1 - x0, y1 - y0) / s));
        for (int i = 0; i <= n; i++) cellAt(g, x0 + (x1 - x0) * i / n, y0 + (y1 - y0) * i / n, s, argb);
    }
    /** An ellipse of cells. */
    private static void ellipse(GuiGraphicsExtractor g, float cx, float cy, float rx, float ry, float s, int argb) {
        if ((argb >>> 24) < 4 || rx < s) return;
        int n = Math.max(12, Math.min(90, Math.round((float) (Math.PI * (rx + ry) / s))));
        for (int i = 0; i < n; i++) { double an = i * Math.PI * 2 / n; cellAt(g, cx + (float) Math.cos(an) * rx, cy + (float) Math.sin(an) * ry, s, argb); }
    }
}
