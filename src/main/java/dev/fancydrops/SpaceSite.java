package dev.fancydrops;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The WIP page, Stardrops style: an orbital construction yard. Three star layers drift at different speeds, a ringed planet hangs
 * behind, a comet now and then. A half-built truss with a lit habitat module and a solar wing unfolding; a crane arm swings the next
 * girder in. Astronauts in white suits float in zero-g on jetpacks: one welds on the truss (sparks), one tumbles past with a wrench on
 * a tether, a far one waves. Bubbles float up and pop, loose bolts and a wrench spin by. The page's name is a cyan hologram projected
 * by a little drone. Hazard tape stays along the edges (FancyScreen draws it). All plain fills, a few rotated quads and soft glows.
 */
final class SpaceSite {
    private SpaceSite() {}
    private static final int SUIT = 0xFFEDEFF6, SUIT_S = 0xFFB8BED0, SUIT_D = 0xFF6E7590, STEEL = 0xFF8A93AE, STEEL_D = 0xFF4A526C,
            VISOR = 0xFFE3A23A, VISOR_D = 0xFF8A4E12, HOLO = 0x5FE8FF, HOLO_D = 0x1C8FD0, HAZ = 0xFFFFC830;

    static void draw(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h, float T, String title, String sub) {
        g.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);
        for (int yy = y + 1; yy < y + h - 1; yy += 2) {                                   // deep space: a dark gradient, bluer at the bottom
            float v = (yy - y) / (float) h; g.fill(x + 1, yy, x + w - 1, yy + 2, 0xFF000000 | Canvas.mix(0xFF05061A, 0xFF0A1236, v * v) & 0xFFFFFF);
        }
        NebulaTex.glow(g, x + w * 0.22f, y + h * 0.3f, w * 0.32f, h * 0.55f, Gfx.argb(46, 0x7B4BFF));
        NebulaTex.glow(g, x + w * 0.78f, y + h * 0.75f, w * 0.34f, h * 0.5f, Gfx.argb(36, 0x2F8BFF));
        NebulaTex.glow(g, x + w * 0.55f, y + h * 0.15f, w * 0.2f, h * 0.3f, Gfx.argb(22, 0xFF5FB0));
        stars(g, x, y, w, h, T);
        float pr = Math.min(22, h * 0.17f); planet(g, x + w - pr * 2.4f - 4, y + h - pr * 0.9f, pr, T);          // a ringed planet rising at the bottom right
        comet(g, x, y, w, h, T);
        float k = Math.max(0.6f, Math.min(1.2f, h / 150f));                                // everything in the yard scales with the panel's height
        float far = (float) Math.sin(T * 0.4f);                                            // a far astronaut, small and slow, waving
        astronaut(g, x + w * 0.5f + far * 4, y + h * 0.82f + (float) Math.sin(T * 0.7f) * 3, 0.5f * k, -0.3f + far * 0.15f, T + 3f, 2);
        float cy = y + h * 0.56f;
        station(g, x, y, w, h, T, k, cy);
        float ax = x + w * 0.25f + (float) Math.sin(T * 0.33f) * w * 0.03f, ay = y + h * 0.24f + (float) Math.sin(T * 0.51f) * 4 * k;    // the tumbler, tethered to the truss
        tether(g, x + w * 0.22f, cy - 4 * k, ax - 4 * k, ay + 4 * k, T);
        astronaut(g, ax, ay, 0.95f * k, T * 0.35f, T, 1);
        float wx = x + w * 0.39f, wy = cy - 15 * k + (float) Math.sin(T * 1.1f) * 1.5f;                            // the welder at the unfinished end of the truss
        astronaut(g, wx, wy, 0.85f * k, 0.35f, T + 1.3f, 0);
        sparks(g, wx + 10 * k, wy + 9 * k, T);
        debris(g, x, y, w, h, T);
        bubbles(g, x, y, w, h, T);
        hologram(g, font, x + w * 0.71f, y + h * 0.4f, w, h, T, title, sub);
        g.disableScissor();
    }

    // ---- the sky

    private static void stars(GuiGraphicsExtractor g, int x, int y, int w, int h, float T) {
        for (int L = 0; L < 3; L++) {
            int n = L == 0 ? 60 : L == 1 ? 30 : 12; float sp = L == 0 ? 1.2f : L == 1 ? 3.5f : 8f;
            for (int i = 0; i < n; i++) {
                int k = L * 1000 + i;
                float sx = rnd(k * 7 + 1) * (w - 4) - T * sp; sx = ((sx % (w - 4)) + (w - 4)) % (w - 4);
                int px = x + 2 + Math.round(sx), py = y + 3 + Math.round(rnd(k * 13 + 2) * (h - 6));
                float tw = 0.55f + 0.45f * (float) Math.sin(T * (0.7f + rnd(k) * 1.6f) + k);
                int col = k % 9 == 0 ? 0xFFE0B0 : k % 5 == 0 ? 0xB8D0FF : 0xFFFFFF;
                int a = Math.round((L == 0 ? 110 : L == 1 ? 170 : 230) * tw);
                if (L == 2) { g.fill(px - 1, py, px + 2, py + 1, Gfx.argb(a / 3, col)); g.fill(px, py - 1, px + 1, py + 2, Gfx.argb(a / 3, col)); }
                g.fill(px, py, px + 1, py + 1, Gfx.argb(a, col));
            }
        }
    }

    private static void planet(GuiGraphicsExtractor g, float cx, float cy, float r, float T) {
        float tilt = -0.32f, rx = r * 2.1f, ry = r * 0.55f;
        ringHalf(g, cx, cy, rx, ry, tilt, false);
        NebulaTex.glow(g, cx, cy, r * 1.9f, r * 1.9f, Gfx.argb(44, 0xFF9A5A));
        int R = Math.round(r);
        for (int dy = -R; dy <= R; dy++) {
            float hw = (float) Math.sqrt(Math.max(0, r * r - dy * dy)); if (hw < 0.5f) continue;
            int x0 = Math.round(cx - hw), x1 = Math.round(cx + hw), yy = Math.round(cy) + dy;
            float band = 0.5f + 0.5f * (float) Math.sin(dy * 0.55f + 1.3f) * (float) Math.cos(dy * 0.17f);
            int base = Canvas.mix(0xFFC2603A, 0xFFF0B070, band);
            for (int xx = x0; xx < x1; xx += 2) {                                             // lit from the upper left: darker to the lower right
                float lx = (xx - cx) / r, ly = dy / r, lit = Math.max(0f, Math.min(1f, 0.85f - (lx * 0.6f + ly * 0.5f + 0.25f)));
                g.fill(xx, yy, Math.min(x1, xx + 2), yy + 1, Canvas.mix(0xFF2A1020, base, 0.25f + 0.75f * lit));
            }
        }
        ringHalf(g, cx, cy, rx, ry, tilt, true);
        g.fill(Math.round(cx - r * 0.45f), Math.round(cy - r * 0.55f), Math.round(cx - r * 0.45f) + 2, Math.round(cy - r * 0.55f) + 1, 0x70FFFFFF);
    }
    /** One half of the planet's rings (the back half behind the ball, the front half over it). */
    private static void ringHalf(GuiGraphicsExtractor g, float cx, float cy, float rx, float ry, float tilt, boolean front) {
        float c = (float) Math.cos(tilt), s = (float) Math.sin(tilt);
        for (int band = 0; band < 3; band++) {
            float k = 0.78f + band * 0.11f; int col = band == 1 ? 0xD8C49A : 0xA88A66; int a = band == 1 ? 200 : 130;
            for (int i = 0; i < 120; i++) {
                float an = i * 6.2832f / 120; boolean fr = Math.sin(an) > 0; if (fr != front) continue;
                float ex = (float) Math.cos(an) * rx * k, ey = (float) Math.sin(an) * ry * k;
                int px = Math.round(cx + ex * c - ey * s), py = Math.round(cy + ex * s + ey * c);
                g.fill(px, py, px + 2, py + 1, Gfx.argb(a, col));
            }
        }
    }

    private static void comet(GuiGraphicsExtractor g, int x, int y, int w, int h, float T) {
        float per = 8.5f, p = (T % per) / 1.9f; if (p >= 1f) return;
        int seed = (int) (T / per);
        float hx = x + w * (1.05f - p * 1.2f), hy = y + h * (0.06f + rnd(seed * 3) * 0.22f) + p * h * 0.22f;
        for (int i = 18; i >= 0; i--) {
            float tx = hx + i * 3.2f, ty = hy - i * 0.6f; int a = Math.round(200 * (1f - i / 19f));
            g.fill(Math.round(tx), Math.round(ty), Math.round(tx) + 2, Math.round(ty) + 1, Gfx.argb(a, i < 4 ? 0xFFFFFF : 0x8AD8FF));
        }
        NebulaTex.glow(g, hx, hy, 7, 5, Gfx.argb(120, 0xA8E8FF));
        g.fill(Math.round(hx) - 1, Math.round(hy) - 1, Math.round(hx) + 2, Math.round(hy) + 1, 0xFFFFFFFF);
    }

    // ---- the yard

    /** The station: a spinning habitat ring round a hub, a truss running out of it (chase lights along the top, radiator fins under it,
     *  a pair of solar wings with light gliding over the cells), the last bays only a bare frame being welded, a crane swinging the next
     *  girder in, and a maintenance drone circling the ring. */
    private static void station(GuiGraphicsExtractor g, int x, int y, int w, int h, float T, float k, float cy) {
        float hx = x + w * 0.09f, R = h * 0.36f, rx = R * 0.42f, bx1 = x + w * 0.43f;
        NebulaTex.glow(g, hx, cy, R * 1.6f, R * 1.4f, Gfx.argb(40, 0x5FA8FF));
        float spin = T * 0.55f; int n = 22;
        ring(g, hx, cy, rx, R, spin, n, k, false);                                                        // the far half of the ring
        for (int sp = 0; sp < 4; sp++) { float an = spin + sp * 1.5708f; if (Math.cos(an) < 0) line(g, hx, cy, hx + (float) Math.cos(an) * rx, cy + (float) Math.sin(an) * R, 1, STEEL_D); }
        // the truss
        float tx0 = hx + 6 * k; int seg = 8; float sl = (bx1 - tx0) / seg; int top = Math.round(cy - 5 * k), bot = Math.round(cy + 5 * k);
        for (int i = 0; i < seg; i++) {
            float sx = tx0 + i * sl; boolean done = i < 6; int a = done ? 255 : 140;
            g.fill(Math.round(sx), top, Math.round(sx + sl), top + 2, Gfx.argb(a, done ? STEEL : STEEL_D));
            g.fill(Math.round(sx), bot - 2, Math.round(sx + sl), bot, Gfx.argb(a, done ? STEEL_D : 0x3A4058));
            g.fill(Math.round(sx), top, Math.round(sx) + 1, bot, Gfx.argb(a, STEEL));
            if (done) { line(g, sx, top + 1, sx + sl, bot - 1, 1, Gfx.argb(190, STEEL_D)); line(g, sx, bot - 1, sx + sl, top + 1, 1, Gfx.argb(190, STEEL_D)); }
            if (i >= 1 && i <= 3) for (int f = 0; f < 3; f++) { int fx = Math.round(sx + 2 + f * sl / 3f); g.fill(fx, bot, fx + 2, bot + Math.round(9 * k), 0xFFE8ECF6); g.fill(fx + 1, bot, fx + 2, bot + Math.round(9 * k), 0xFFB0B8CC); }   // radiators
        }
        float chase = (T * 1.6f) % 1f;                                                                       // a light running out along the top
        for (int i = 0; i < 6; i++) { float u = i / 6f; float d = Math.abs(u - chase); boolean lit = d < 0.08f; int lx = Math.round(tx0 + u * sl * 6 + 2); g.fill(lx, top - 1, lx + 2, top, lit ? 0xFF5FE8FF : 0xFF1E4A5A); if (lit) NebulaTex.glow(g, lx + 1, top - 1, 4, 3, 0xA05FE8FF); }
        boolean strobe = (T % 1.3f) < 0.12f; g.fill(Math.round(bx1 - sl * 2) - 1, top - 3, Math.round(bx1 - sl * 2) + 1, top - 1, strobe ? 0xFFFF4A5E : 0xFF5A1A24); if (strobe) NebulaTex.glow(g, bx1 - sl * 2, top - 2, 7, 7, 0xC0FF4A5E);
        // solar wings on a mast, light gliding over the cells
        float mx = tx0 + sl * 4.5f; int mast = Math.round(22 * k);
        g.fill(Math.round(mx), top - mast, Math.round(mx) + 2, bot + mast, STEEL_D);
        float glide = (T * 0.35f) % 1.6f;
        for (int side = -1; side <= 1; side += 2) for (int r = 0; r < 3; r++) for (int c = 0; c < 2; c++) {
            int cw = Math.round(7 * k), ch = Math.round(5 * k), cx0 = Math.round(mx) - cw - 1 + c * (cw + 3), cy0 = side < 0 ? top - mast + r * (ch + 1) : bot + mast - (3 - r) * (ch + 1);
            g.fill(cx0 - 1, cy0 - 1, cx0 + cw + 1, cy0 + ch + 1, 0xFFB08A30);
            float gl = Math.max(0f, 1f - Math.abs(glide - (r * 0.2f + c * 0.1f + (side + 1) * 0.15f)) / 0.18f);
            g.fill(cx0, cy0, cx0 + cw, cy0 + ch, Canvas.mix(0xFF1D3F8A, 0xFF7AC8FF, gl * 0.8f)); g.fill(cx0, cy0, cx0 + cw, cy0 + 1, Canvas.mix(0xFF3D6FCA, 0xFFFFFFFF, gl));
        }
        // the hub: a short cylinder, a docking port blinking green
        int hw = Math.round(7 * k), hh = Math.round(9 * k);
        g.fill(Math.round(hx) - hw, Math.round(cy) - hh, Math.round(hx) + hw, Math.round(cy) + hh, STEEL); g.fill(Math.round(hx) - hw, Math.round(cy) + hh - 3, Math.round(hx) + hw, Math.round(cy) + hh, STEEL_D);
        g.fill(Math.round(hx) - hw, Math.round(cy) - hh, Math.round(hx) + hw, Math.round(cy) - hh + 1, 0xFFD0D8EC);
        for (int wn = -1; wn <= 1; wn++) { boolean lit = ((int) (T * 0.7f) + wn) % 4 != 0; g.fill(Math.round(hx) + wn * 4 - 1, Math.round(cy) - 3, Math.round(hx) + wn * 4 + 1, Math.round(cy) + 1, lit ? 0xFFFFE08A : 0xFF3A4060); }
        boolean dock = ((int) (T * 2f)) % 2 == 0; g.fill(Math.round(hx) - hw - 2, Math.round(cy) - 2, Math.round(hx) - hw, Math.round(cy) + 2, dock ? 0xFF4AFF7A : 0xFF1A5A2A);
        if (dock) NebulaTex.glow(g, hx - hw - 1, cy, 6, 6, 0x904AFF7A);
        ring(g, hx, cy, rx, R, spin, n, k, true);                                                         // the near half, over the hub
        for (int sp = 0; sp < 4; sp++) { float an = spin + sp * 1.5708f; if (Math.cos(an) >= 0) line(g, hx, cy, hx + (float) Math.cos(an) * rx, cy + (float) Math.sin(an) * R, 1, STEEL); }
        // a maintenance drone circling the ring, its light blinking
        float da = T * 0.9f, dxp = hx + (float) Math.cos(da) * (rx + 12 * k), dyp = cy + (float) Math.sin(da) * (R + 6 * k);
        g.fill(Math.round(dxp) - 2, Math.round(dyp) - 1, Math.round(dxp) + 3, Math.round(dyp) + 2, 0xFFB8C0D8); g.fill(Math.round(dxp), Math.round(dyp) - 2, Math.round(dxp) + 1, Math.round(dyp) - 1, ((int) (T * 4)) % 2 == 0 ? 0xFFFFC830 : 0xFF5A4A10);
        // the crane at the open end, swinging the next girder in
        float cbx = bx1 - sl * 0.8f, cby = top, ang = -1.95f + 0.25f * (float) Math.sin(T * 0.45f), L = 34 * k;
        float ex = cbx + (float) Math.cos(ang) * L, ey = cby + (float) Math.sin(ang) * L;
        line(g, cbx, cby, ex, ey, 2, HAZ); line(g, cbx, cby - 2, ex, ey - 2, 1, 0xFF8A6A10);
        g.fill(Math.round(cbx) - 2, Math.round(cby) - 2, Math.round(cbx) + 3, Math.round(cby) + 3, STEEL_D);
        float sw = (float) Math.sin(T * 0.9f) * 3f, gx = ex + sw, gy = ey + 10 * k + (float) Math.sin(T * 0.6f) * 2;
        line(g, ex, ey, gx, gy, 1, 0xFFB0B6C8);
        Gfx.push(g); Gfx.translate(g, gx, gy); Gfx.rotate(g, (float) Math.sin(T * 0.5f) * 0.25f); Gfx.scale(g, k);
        g.fill(-11, 0, 11, 2, STEEL); g.fill(-11, 4, 11, 6, STEEL_D); for (int i = -11; i <= 9; i += 5) g.fill(i, 0, i + 1, 6, STEEL);
        g.fill(-11, -1, -7, 0, HAZ); g.fill(7, -1, 11, 0, HAZ);
        Gfx.pop(g);
    }
    /** Half of the spinning habitat ring (seen nearly edge on): a solid band, six pods with lit windows sliding round it. */
    private static void ring(GuiGraphicsExtractor g, float cx, float cy, float rx, float ry, float spin, int n, float k, boolean front) {
        int th = Math.max(2, Math.round(3 * k));
        for (int i = 0; i < 140; i++) {
            float an = i * 6.2832f / 140; float c = (float) Math.cos(an); if ((c >= 0) != front) continue;
            int px = Math.round(cx + c * rx), py = Math.round(cy + (float) Math.sin(an) * ry);
            int col = front ? Canvas.mix(0xFF6A7290, 0xFFE0E6F4, 0.3f + 0.7f * c) : Canvas.mix(0xFF1E2238, 0xFF4A526C, 1f + c);
            g.fill(px - th / 2, py - 1, px - th / 2 + th, py + 1, col);
        }
        int pw = Math.max(4, Math.round(8 * k)), ph = Math.max(3, Math.round(6 * k));
        for (int i = 0; i < 6; i++) {
            float an = spin + i * 6.2832f / 6; float c = (float) Math.cos(an); if ((c >= 0) != front) continue;
            int px = Math.round(cx + c * rx), py = Math.round(cy + (float) Math.sin(an) * ry);
            int w2 = Math.max(3, Math.round(pw * (0.55f + 0.45f * Math.abs(c))));
            int col = front ? Canvas.mix(0xFF8A93AE, 0xFFF0F4FF, 0.3f + 0.7f * c) : 0xFF3A4058;
            g.fill(px - w2 / 2, py - ph / 2, px - w2 / 2 + w2, py - ph / 2 + ph, col);
            if (front) { g.fill(px - w2 / 2 + 1, py - 1, px - w2 / 2 + w2 - 1, py + 1, i % 3 == 0 ? 0xFF5FE8FF : 0xFFFFE08A); g.fill(px - w2 / 2, py - ph / 2, px - w2 / 2 + w2, py - ph / 2 + 1, 0xFFFFFFFF); }
        }
    }

    private static void tether(GuiGraphicsExtractor g, float x0, float y0, float x1, float y1, float T) {
        int n = 26; float px = x0, py = y0;
        for (int i = 1; i <= n; i++) {
            float u = i / (float) n, wav = (float) Math.sin(u * 3.1416f) * (6f + 3f * (float) Math.sin(T * 0.8f)) * (float) Math.sin(u * 6.28f + T * 1.2f);
            float qx = x0 + (x1 - x0) * u, qy = y0 + (y1 - y0) * u + wav;
            line(g, px, py, qx, qy, 1, i % 4 < 2 ? 0xFFFFC830 : 0xFFD0D4E0); px = qx; py = qy;
        }
    }

    /** An astronaut in a white suit with a jetpack, drawn round its chest. pose 0 welds (arm out with a torch), 1 tumbles with a wrench, 2 waves. */
    private static void astronaut(GuiGraphicsExtractor g, float cx, float cy, float s, float ang, float T, int pose) {
        Gfx.push(g); Gfx.translate(g, cx, cy); Gfx.rotate(g, ang); Gfx.scale(g, s);
        boolean burn = ((T * 1.7f) % 3f) < (pose == 1 ? 1.4f : 0.7f);
        // jetpack and its thrust
        g.fill(-9, -6, -4, 5, 0xFF9AA2B8); g.fill(-9, -6, -8, 5, 0xFF6A7088); g.fill(-8, -5, -5, -3, 0xFFFF4A5E);
        g.fill(-9, 5, -7, 7, STEEL_D); g.fill(-6, 5, -4, 7, STEEL_D);
        if (burn) for (int i = 0; i < 2; i++) {
            int fl = 3 + Math.round(3 * Math.abs((float) Math.sin(T * 31f + i * 2f)));
            g.fill(-9 + i * 3, 7, -7 + i * 3, 7 + fl, 0xFFFFB040); g.fill(-9 + i * 3, 7, -7 + i * 3, 8 + fl / 2, 0xFFFFF0C0);
            g.fill(-10 + i * 3, 7 + fl, -6 + i * 3, 9 + fl, 0x55FF8A30);
        }
        // legs, dangling in zero-g
        float lw = (float) Math.sin(T * 1.3f) * 0.25f;
        limb(g, -2, 5, 1.75f + lw, 8, 4, SUIT, SUIT_S, 0xFF8A93AE);
        limb(g, 2, 5, 1.35f - lw, 8, 4, SUIT_S, SUIT_S, 0xFF6E7590);
        // the back arm
        float ab = pose == 2 ? 2.4f : 2.0f + (float) Math.sin(T * 0.9f) * 0.2f;
        limb(g, -3, -3, ab, 7, 3, SUIT_S, SUIT_D, 0xFF6E7590);
        // torso, chest panel with blinking lights, mission patch
        g.fill(-5, -6, 5, 6, SUIT); g.fill(3, -6, 5, 6, SUIT_S); g.fill(-5, 4, 5, 6, SUIT_S);
        g.fill(-2, -2, 3, 2, 0xFF3A4060); g.fill(-1, -1, 0, 0, ((int) (T * 3)) % 2 == 0 ? 0xFF4AFF7A : 0xFF1A5A2A); g.fill(1, -1, 2, 0, ((int) (T * 2.3f)) % 3 == 0 ? 0xFFFF4A5E : 0xFF5A1A24);
        g.fill(-4, -5, -2, -3, 0xFFFF8A30);
        // the front arm, with its tool
        float af = pose == 0 ? 0.25f + (float) Math.sin(T * 6f) * 0.05f : pose == 1 ? -0.4f + (float) Math.sin(T * 1.1f) * 0.5f : -1.9f + (float) Math.sin(T * 5f) * 0.45f;
        limb(g, 3, -4, af, 7, 3, SUIT, SUIT_S, 0xFF8A93AE);
        float hx = 3 + (float) Math.cos(af) * 8, hy = -4 + (float) Math.sin(af) * 8;
        if (pose == 0) { Gfx.push(g); Gfx.translate(g, hx, hy); Gfx.rotate(g, af); g.fill(0, -1, 5, 1, 0xFF5A6078); g.fill(5, -1, 6, 1, 0xFFFFE8A0); Gfx.pop(g); }
        if (pose == 1) { Gfx.push(g); Gfx.translate(g, hx, hy); Gfx.rotate(g, af + 1.2f); g.fill(-1, 0, 1, 8, 0xFFB0B8CC); g.fill(-2, 7, 2, 10, 0xFFB0B8CC); g.fill(-1, 8, 1, 10, 0xFF06071A); Gfx.pop(g); }
        // helmet: white shell, a gold visor with the station's light in it, an antenna light
        disc(g, 0, -11, 6.2f, SUIT); disc(g, 0.6f, -10.6f, 6.2f, SUIT_S); disc(g, -0.4f, -11.4f, 5.6f, SUIT);
        disc(g, 1.2f, -11, 4.2f, VISOR_D); disc(g, 1.4f, -11.3f, 3.6f, VISOR);
        g.fill(0, -14, 2, -13, 0xCCFFFFFF); g.fill(-1, -13, 0, -11, 0x99FFFFFF); g.fill(3, -9, 5, -8, 0x705FE8FF);
        g.fill(-3, -19, -2, -16, SUIT_D); g.fill(-4, -21, -1, -19, ((int) (T * 2.5f)) % 2 == 0 ? 0xFFFF4A5E : 0xFF6A1A24);
        Gfx.pop(g);
    }
    /** An arm or a leg: a suited bar from (x,y) at an angle, with a grey glove or boot at the end. */
    private static void limb(GuiGraphicsExtractor g, float x, float y, float ang, int len, int th, int col, int shade, int end) {
        Gfx.push(g); Gfx.translate(g, x, y); Gfx.rotate(g, ang);
        g.fill(0, -th / 2, len, th - th / 2, col); g.fill(0, th - th / 2 - 1, len, th - th / 2, shade); g.fill(len, -th / 2, len + 3, th - th / 2, end);
        Gfx.pop(g);
    }

    private static void sparks(GuiGraphicsExtractor g, float x0, float y0, float T) {
        boolean on = (T % 2.6f) < 1.9f; if (!on) return;
        NebulaTex.glow(g, x0, y0, 10, 10, Gfx.argb(150 + Math.round(60 * (float) Math.sin(T * 40f)), 0xFFE8A0));
        g.fill(Math.round(x0) - 1, Math.round(y0) - 1, Math.round(x0) + 2, Math.round(y0) + 2, 0xFFFFFFFF);
        for (int i = 0; i < 14; i++) {
            float life = ((T * 2.2f + rnd(i) * 7f) % 1f), an = (rnd(i * 5 + (int) (T * 2.2f + rnd(i) * 7f)) - 0.3f) * 3.4f, d = life * (10 + rnd(i * 3) * 14);
            int px = Math.round(x0 + (float) Math.cos(an) * d), py = Math.round(y0 + (float) Math.sin(an) * d);
            g.fill(px, py, px + 1, py + 1, Gfx.argb(Math.round(255 * (1 - life)), life < 0.4f ? 0xFFFFE0 : 0xFFA040));
        }
    }

    private static void debris(GuiGraphicsExtractor g, int x, int y, int w, int h, float T) {
        for (int i = 0; i < 5; i++) {
            float sp = 5f + rnd(i * 11) * 6f, fx = rnd(i * 17 + 3) * (w + 40) + T * sp; fx = fx % (w + 40) - 20;
            float fy = y + h * (0.2f + rnd(i * 23) * 0.65f) + (float) Math.sin(T * 0.7f + i) * 5;
            Gfx.push(g); Gfx.translate(g, x + fx, fy); Gfx.rotate(g, T * (0.8f + rnd(i) * 1.5f) * (i % 2 == 0 ? 1 : -1));
            if (i == 0) { g.fill(-1, -5, 1, 4, 0xFFB0B8CC); g.fill(-3, -7, 3, -4, 0xFFB0B8CC); g.fill(-1, -7, 1, -5, 0xFF06071A); }                     // a lost wrench
            else if (i == 1) { g.fill(-3, -2, 3, 2, 0xFFFFC830); g.fill(-3, -2, 3, -1, 0xFFFFE890); g.fill(-1, -1, 1, 1, 0xFF8A6A10); }           // a hex nut
            else if (i == 2) { g.fill(-4, -3, 4, 3, 0xFF5A6078); g.fill(-4, -3, 4, -2, 0xFF8A93AE); g.fill(-1, -1, 1, 1, 0xFF5FE8FF); }           // a little crate
            else { g.fill(-1, -1, 2, 2, 0xFF9AA2B8); g.fill(0, 2, 1, 4, 0xFF6E7590); }                                                   // bolts
            Gfx.pop(g);
        }
    }

    /** Bubbles in zero-g: they wobble up through the yard and pop near the top. */
    private static void bubbles(GuiGraphicsExtractor g, int x, int y, int w, int h, float T) {
        for (int i = 0; i < 11; i++) {
            float r = 2.5f + rnd(i * 7 + 5) * 5f, per = 9f + rnd(i * 3) * 7f, ph = ((T + rnd(i * 13) * per) % per) / per;
            float bx = x + 10 + rnd(i * 29 + 1) * (w - 20) + (float) Math.sin(T * (0.9f + rnd(i) * 0.8f) + i * 2f) * 6f;
            float by = y + h + r - ph * (h * 0.95f);
            if (ph > 0.9f) {                                                                         // pop: a ring that grows and fades, a few droplets
                float q = (ph - 0.9f) / 0.1f; ring(g, bx, by, r * (1f + q * 1.2f), Gfx.argb(Math.round(170 * (1 - q)), 0xC8F4FF));
                for (int k = 0; k < 4; k++) { float an = k * 1.57f + i; int dx = Math.round(bx + (float) Math.cos(an) * r * (1.4f + q * 2f)), dy = Math.round(by + (float) Math.sin(an) * r * (1.4f + q * 2f)); g.fill(dx, dy, dx + 1, dy + 1, Gfx.argb(Math.round(200 * (1 - q)), 0xE8FCFF)); }
                continue;
            }
            float sq = 1f + 0.08f * (float) Math.sin(T * 4f + i);                                   // they jiggle as they go
            fillE(g, bx, by, r * sq, r / sq, Gfx.argb(22, 0x8ADFFF));
            ring(g, bx, by, r, Gfx.argb(140, 0xB8EEFF));
            int hx = Math.round(bx - r * 0.45f), hy = Math.round(by - r * 0.5f); g.fill(hx, hy, hx + 2, hy + 1, 0xC8FFFFFF); if (r > 4) g.fill(hx, hy + 1, hx + 1, hy + 2, 0x90FFFFFF);
            int rx = Math.round(bx + r * 0.4f), ry = Math.round(by + r * 0.35f); g.fill(rx, ry, rx + 1, ry + 1, 0x60FF8AF0);       // a little rainbow sheen
        }
    }

    /** The page's name as a hologram: a drone projects a cyan panel with scanlines, corner brackets and the odd glitch. */
    private static void hologram(GuiGraphicsExtractor g, Font font, float cx, float cy, int w, int h, float T, String title, String sub) {
        String head = "UNDER CONSTRUCTION", t = title == null ? "" : title;
        float hs = 0.6f; int hw = Math.round(Gfx.w(font, head) * hs), tw = Gfx.w(font, t);
        String sb = sub == null ? "" : sub; float ss = 0.6f; int sw = Math.round(Gfx.w(font, sb) * ss);
        int maxW = Math.round(w * 0.5f); if (sw > maxW - 12) { while (sb.length() > 4 && Math.round(Gfx.w(font, sb + "...") * ss) > maxW - 12) sb = sb.substring(0, sb.length() - 1); sb = sb.trim() + "..."; sw = Math.round(Gfx.w(font, sb) * ss); }
        int pw = Math.min(maxW, Math.max(Math.max(hw, tw), sw) + 26), ph = sb.isEmpty() ? 38 : 46;
        float bob = (float) Math.sin(T * 1.4f) * 1.5f;
        int x0 = Math.round(cx - pw / 2f), y0 = Math.round(cy - ph / 2f + bob), x1 = x0 + pw, y1 = y0 + ph;
        boolean glitch = (T % 4.7f) < 0.12f; float fl = glitch ? 0.55f : 0.92f + 0.08f * (float) Math.sin(T * 23f);
        // the drone underneath and its light cone up to the panel
        float dx = cx + (float) Math.sin(T * 0.8f) * 2, dy = y1 + 18 + (float) Math.sin(T * 1.9f) * 1.5f;
        for (int yy = y1; yy < Math.round(dy) - 2; yy++) {
            float u = (yy - y1) / Math.max(1f, dy - 2 - y1), hw2 = (pw / 2f - 4) * (1 - u) + 2 * u;
            g.fill(Math.round(dx - hw2), yy, Math.round(dx + hw2), yy + 1, Gfx.argb(Math.round((10 + 26 * u) * fl), HOLO));
        }
        g.fill(Math.round(dx) - 6, Math.round(dy) - 1, Math.round(dx) + 7, Math.round(dy) + 3, STEEL_D); g.fill(Math.round(dx) - 4, Math.round(dy) - 3, Math.round(dx) + 5, Math.round(dy) - 1, STEEL);
        g.fill(Math.round(dx) - 1, Math.round(dy) - 3, Math.round(dx) + 2, Math.round(dy) - 2, 0xFF5FE8FF);
        for (int k : new int[]{-8, 7}) { int px = Math.round(dx) + k; int sp = ((int) (T * 30)) % 2 == 0 ? 3 : 1; g.fill(px - sp, Math.round(dy) - 2, px + sp + 1, Math.round(dy) - 1, 0xAAD0D8E8); g.fill(px, Math.round(dy) - 1, px + 1, Math.round(dy) + 1, STEEL_D); }
        NebulaTex.glow(g, dx, dy - 3, 9, 5, Gfx.argb(110, HOLO));
        // the panel
        NebulaTex.glow(g, cx, y0 + ph / 2f, pw * 0.7f, ph * 0.9f, Gfx.argb(Math.round(40 * fl), HOLO_D));
        g.fill(x0, y0, x1, y1, Gfx.argb(Math.round(70 * fl), 0x0A3A6A));
        for (int yy = y0 + 1; yy < y1; yy += 2) g.fill(x0, yy, x1, yy + 1, Gfx.argb(Math.round(18 * fl), HOLO));
        int scan = y0 + Math.round(((T * 22f) % (ph + 10)) - 5); if (scan > y0 && scan < y1) g.fill(x0, scan, x1, Math.min(y1, scan + 2), Gfx.argb(Math.round(60 * fl), HOLO));
        int bc = Gfx.argb(Math.round(230 * fl), HOLO);
        g.fill(x0, y0, x1, y0 + 1, Gfx.argb(Math.round(90 * fl), HOLO)); g.fill(x0, y1 - 1, x1, y1, Gfx.argb(Math.round(90 * fl), HOLO));
        for (int[] c : new int[][]{{x0, y0, 1, 1}, {x1 - 1, y0, -1, 1}, {x0, y1 - 1, 1, -1}, {x1 - 1, y1 - 1, -1, -1}}) {
            g.fill(Math.min(c[0], c[0] + c[2] * 6), c[1], Math.max(c[0], c[0] + c[2] * 6) + 1, c[1] + 1, bc); g.fill(c[0], Math.min(c[1], c[1] + c[3] * 6), c[0] + 1, Math.max(c[1], c[1] + c[3] * 6) + 1, bc);
        }
        int gx = glitch ? Math.round(rnd((int) (T * 50)) * 6 - 3) : 0;
        // the header with blinking hazard marks, the name, and its line
        int hx = Math.round(cx - hw / 2f) + gx, hy = y0 + 5; boolean blink = ((int) (T * 2)) % 2 == 0;
        Gfx.push(g); Gfx.translate(g, hx, hy); Gfx.scale(g, hs); Gfx.text(g, font, head, 0, 0, Gfx.argb(Math.round(255 * fl), 0xFFC830)); Gfx.pop(g);
        if (blink) for (int sx : new int[]{hx - 8, hx + hw + 3}) { g.fill(sx + 2, hy, sx + 3, hy + 1, HAZ); g.fill(sx + 1, hy + 1, sx + 4, hy + 3, HAZ); g.fill(sx, hy + 3, sx + 5, hy + 5, HAZ); g.fill(sx + 2, hy + 2, sx + 3, hy + 3, 0xFF06071A); }
        int ty = y0 + 15;
        for (int[] o : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) Gfx.text(g, font, t, Math.round(cx - tw / 2f) + o[0] - gx, ty + o[1], Gfx.argb(Math.round(50 * fl), HOLO));
        Gfx.text(g, font, t, Math.round(cx - tw / 2f) - gx, ty, Gfx.argb(Math.round(255 * fl), 0xE8FCFF));
        if (glitch) g.fill(x0 + 2, ty + 3, x1 - 2, ty + 5, Gfx.argb(90, 0xFF5FB0));
        if (!sb.isEmpty()) { Gfx.push(g); Gfx.translate(g, Math.round(cx - sw / 2f), y0 + 30); Gfx.scale(g, ss); Gfx.text(g, font, sb, 0, 0, Gfx.argb(Math.round(220 * fl), 0x9FDFFF)); Gfx.pop(g); }
        int bar = x0 + 8, bw = pw - 16, prog = Math.round(bw * (0.35f + 0.25f * (float) Math.sin(T * 0.3f)));         // a build bar that never quite finishes
        int byy = y1 - 6; g.fill(bar, byy, bar + bw, byy + 2, Gfx.argb(Math.round(60 * fl), HOLO)); g.fill(bar, byy, bar + prog, byy + 2, Gfx.argb(Math.round(220 * fl), HOLO));
        int gl = bar + Math.round(((T * 40f) % Math.max(1, prog))); g.fill(gl, byy, Math.min(bar + prog, gl + 4), byy + 2, 0xFFFFFFFF);
    }

    // ---- drawing helpers

    private static void line(GuiGraphicsExtractor g, float x0, float y0, float x1, float y1, int th, int col) {
        float L = (float) Math.hypot(x1 - x0, y1 - y0); if (L < 0.5f) return;
        Gfx.push(g); Gfx.translate(g, x0, y0); Gfx.rotate(g, (float) Math.atan2(y1 - y0, x1 - x0)); g.pose().scale(L, 1f); g.fill(0, -th / 2, 1, th - th / 2, col); Gfx.pop(g);
    }
    private static void disc(GuiGraphicsExtractor g, float cx, float cy, float r, int col) {
        int R = (int) Math.ceil(r);
        for (int dy = -R; dy <= R; dy++) { float hw = (float) Math.sqrt(Math.max(0, r * r - (dy + 0.5f) * (dy + 0.5f))); if (hw < 0.3f) continue; g.fill(Math.round(cx - hw), Math.round(cy) + dy, Math.round(cx + hw), Math.round(cy) + dy + 1, col); }
    }
    private static void fillE(GuiGraphicsExtractor g, float cx, float cy, float rx, float ry, int col) {
        int R = (int) Math.ceil(ry);
        for (int dy = -R; dy <= R; dy++) { float u = (dy + 0.5f) / ry, hw = rx * (float) Math.sqrt(Math.max(0, 1 - u * u)); if (hw < 0.3f) continue; g.fill(Math.round(cx - hw), Math.round(cy) + dy, Math.round(cx + hw), Math.round(cy) + dy + 1, col); }
    }
    private static void ring(GuiGraphicsExtractor g, float cx, float cy, float r, int col) {
        int n = Math.max(12, Math.round(r * 6f)); int lx = Integer.MIN_VALUE, ly = 0;
        for (int i = 0; i < n; i++) { float an = i * 6.2832f / n; int px = Math.round(cx + (float) Math.cos(an) * r), py = Math.round(cy + (float) Math.sin(an) * r); if (px == lx && py == ly) continue; g.fill(px, py, px + 1, py + 1, col); lx = px; ly = py; }
    }
    private static float rnd(int i) { int h = i * 374761393 + 668265263; h = (h ^ (h >>> 13)) * 1274126177; return ((h ^ (h >>> 16)) & 0xFFFF) / 65535f; }
}
