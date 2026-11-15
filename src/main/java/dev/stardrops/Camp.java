package dev.stardrops;

import java.util.ArrayList;
import java.util.List;

/**
 * The camp under the Achievements sky. The world is realistic (a painted night landscape: two mountain ranges with snow, a lake that
 * holds the moon, a far shore of trees, mist, pines, a tent, a grassy hill), baked once into two pictures. The characters are
 * Minecraft: Steve sitting on an oak log with a copper spyglass in his hand, a ginger cat, a campfire, a lantern on a fence post.
 * They are box models drawn texel by texel and lit by the scene's own lights (the moon, the fire as it flickers, the lantern), so the
 * firelight really moves over Steve.
 *
 * Steve holds the spyglass in his hand. At rest it lies across his lap; when you point at a constellation he lifts it and holds it a
 * little in front of his eye (never in it), and his head follows what you point at.
 *
 * Everything is laid out in a 640 x 360 picture and stretched to the page. Nothing here touches the game: it draws through a Pen.
 */
final class Camp {
    private Camp() {}

    interface Pen {
        void fill(float x0, float y0, float x1, float y1, int argb);
        void glow(float cx, float cy, float rx, float ry, int argb);
        void layer(String name, Canvas c, float x, float y, float w, float h, int argb);
        /** The cells that follow are in this face's own grid: its corner, one cell along, one cell down (screen pixels). */
        void face(float ox, float oy, float ux, float uy, float vx, float vy);
        void cell(int i0, int j0, int i1, int j1, int argb);
        void endFace();
    }

    // ------------------------------------------------------------------ small maths
    private static float h(int i) { int n = i * 374761393 + 668265263; n = (n ^ (n >>> 13)) * 1274126177; return ((n ^ (n >>> 16)) & 0xFFFFFF) / 16777216f; }
    private static float cl(float v) { return v < 0 ? 0 : v > 1 ? 1 : v; }
    private static float sm(float v) { v = cl(v); return v * v * (3 - 2 * v); }
    private static float mixf(float a, float b, float k) { return a + (b - a) * k; }
    private static float vn(float x, int s) { int i = (int) Math.floor(x); float f = x - i, u = f * f * (3 - 2 * f); return h(i + s * 977) * (1 - u) + h(i + 1 + s * 977) * u; }
    private static float rdg(float x, int s) { return (float) Math.pow(1 - Math.abs(2 * vn(x, s) - 1), 1.5); }
    private static int mix(int a, int b, float k) { return Canvas.mix(0xFF000000 | a, 0xFF000000 | b, cl(k)); }
    private static int argb(float a, int rgb) { return (Math.round(cl(a) * 255) << 24) | (rgb & 0xFFFFFF); }

    // ------------------------------------------------------------------ the land, in the 640 x 360 picture
    static final float OX = 214, FX = 31, FZ = 8, SC = 3.3f, PITCH = 0.2f;
    /** The far range (snow on its peaks), the nearer range, and the hill you sit on: the top of each at x. */
    static float m1(float x) { return 206 - 66 * (rdg(x / 105 + 0.35f, 3) * 0.72f + rdg(x / 37, 4) * 0.2f + vn(x / 11, 5) * 0.08f); }
    static float m2(float x) { return 220 - 34 * (rdg(x / 66 + 2, 9) * 0.6f + vn(x / 24, 10) * 0.4f); }
    static float hill(float x) { return 318 - 78 * sm((96 - x) / 110) + 52 * sm((x - 430) / 200) + (vn(x / 9, 40) - 0.5f) * 1.6f; }
    static float groundY() { return hill(OX) + 3; }

    private static Canvas farC, landC; private static int bw, bh;

    private static void poly(Canvas c, float[] p, int top, int bot, float yTop, float yBot, float alpha) {
        float lo = 1e9f, hi = -1e9f; int n = p.length / 2; for (int i = 0; i < n; i++) { lo = Math.min(lo, p[i * 2 + 1]); hi = Math.max(hi, p[i * 2 + 1]); }
        float[] xs = new float[n];
        for (int y = Math.max(0, (int) Math.floor(lo)); y <= Math.min(c.h - 1, (int) Math.ceil(hi)); y++) {
            float yc = y + 0.5f; int k = 0;
            for (int i = 0; i < n; i++) { int j = (i + 1) % n; float yi = p[i * 2 + 1], yj = p[j * 2 + 1]; if ((yi <= yc) != (yj <= yc)) xs[k++] = p[i * 2] + (yc - yi) / (yj - yi) * (p[j * 2] - p[i * 2]); }
            java.util.Arrays.sort(xs, 0, k);
            int col = (Math.round(255 * alpha) << 24) | (mix(top, bot, (yc - yTop) / Math.max(1f, yBot - yTop)) & 0xFFFFFF);
            for (int q = 0; q + 1 < k; q += 2) for (int x = Math.max(0, Math.round(xs[q])); x < Math.min(c.w, Math.round(xs[q + 1])); x++) c.set(x, y, col);
        }
    }
    /** A pine: a trunk and tiers of ragged boughs, the moon on the left edge of each tier. */
    private static void pine(Canvas c, float kx, float ky, float x, float by, float ht, int col) {
        c.rect(Math.round((x - ht * 0.012f - 1) * kx), Math.round((by - ht * 0.2f) * ky), Math.round((x + ht * 0.012f + 1) * kx), Math.round((by + ht * 0.02f) * ky), 0xFF0A0D14);
        int n = Math.max(5, Math.round(ht / 11));
        for (int k = 0; k < n; k++) {
            float u = k / (float) (n - 1), y = by - ht + u * ht * 0.84f, w = 2 + (float) Math.pow(u, 0.85) * ht * 0.23f, dh = ht / n * 1.9f, j = (h(Math.round(x * 3) + k) - 0.5f) * w * 0.3f;
            poly(c, new float[]{(x + j * 0.3f) * kx, (y - dh * 0.55f) * ky, (x - w + j) * kx, (y + dh * 0.5f) * ky, (x - w * 0.45f) * kx, (y + dh * 0.38f) * ky, x * kx, (y + dh * 0.62f) * ky, (x + w * 0.5f) * kx, (y + dh * 0.4f) * ky, (x + w + j) * kx, (y + dh * 0.5f) * ky}, col, col, 0, 1, 1f);
            poly(c, new float[]{(x + j * 0.3f) * kx, (y - dh * 0.55f) * ky, (x - w + j) * kx, (y + dh * 0.5f) * ky, (x - w * 0.6f) * kx, (y + dh * 0.4f) * ky}, 0x9AB0E0, 0x9AB0E0, 0, 1, 0.16f);
        }
    }

    /** Paints the two pictures for a page w x h (only when the size changed). */
    static void bake(int w, int hgt) {
        if (farC != null && bw == w && bh == hgt || w < 8 || hgt < 8) return;
        bw = w; bh = hgt; int CW = w * 2, CH = hgt * 2; float kx = CW / 640f, ky = CH / 360f;
        Canvas f = new Canvas(CW, CH);
        float[] r1 = new float[CW * 3]; for (int i = 0; i < r1.length; i++) r1[i] = m1((i - CW) / kx);     // the far range's skyline, looked up (its ridges run down and to the right, away from the moon)
        for (int x = 0; x < CW; x++) {
            float mx = x / kx, a1 = r1[x + CW], a2 = m2(mx);
            for (int y = (int) (100 * ky); y < CH; y++) {
                float my = y / ky; int col;
                float dd = Math.max(0f, my - a1); int xs = Math.max(4, Math.min(r1.length - 5, x + CW - Math.round(dd * 0.55f * kx))), e = Math.max(1, Math.round(1.5f * kx));
                float sl = (r1[xs + e] - r1[xs - e]) / 3f * 0.9f * (1f - cl(dd / 80f)), slope = Math.max(-1f, Math.min(1f, sl)), sx = (xs - CW) / kx;
                float snow = sm((170 - r1[xs]) / 26f) * (5 + 20 * vn(sx / 16, 70) + 6 * vn(sx / 5, 71));
                if (my < a1 && my < a2 && my < 224) {                                              // the sky pales toward the horizon
                    float t = sm((my - 105) / 120f); int al = Math.round(135 * t * t); if (al >= 2) f.px[y * CW + x] = (al << 24) | 0x3A3F7C; continue;
                }
                if (my >= 224) {                                                                 // the lake: it holds the ranges, upside down and dim
                    float t = (my - 224) / 106f; col = t < 0.25f ? mix(0x313A7C, 0x1E2456, t / 0.25f) : mix(0x1E2456, 0x070A1E, (t - 0.25f) / 0.75f);
                    if (my < 224 + (224 - a1) * 0.7f) col = mix(col & 0xFFFFFF, 0x0A0D28, 0.1f);
                    if (my < 224 + (224 - a2) * 0.7f) col = mix(col & 0xFFFFFF, 0x0A0D28, 0.2f);
                    if (h((y / 3) * 131 + x / 22) < 0.07f) col = mix(col & 0xFFFFFF, 0x8F9AE0, 0.09f);
                } else if (my >= a2) { col = mix(0x232A5C, 0x1A1F4A, (my - 180) / 60f);         // the nearer range
                } else {                                                                         // the far range: pale with distance, snow on the peaks, the moon on its left faces
                    float t = cl((my - 120) / 120f); col = t < 0.5f ? mix(0x566299, 0x353D78, t * 2) : mix(0x353D78, 0x2C3068, (t - 0.5f) * 2);
                    float nz = 0.55f + 0.45f * vn(mx / 6 + my / 9, 33);
                    if (slope > 0) col = mix(col & 0xFFFFFF, 0x10143A, 0.42f * slope * nz); else col = mix(col & 0xFFFFFF, 0xA8B4E8, 0.24f * -slope * nz);
                    if (my - a1 < snow) col = mix(col & 0xFFFFFF, mix(0xF4F7FF, 0x9AA8DC, (my - 120) / 65f) & 0xFFFFFF, slope > 0 ? 0.45f : 0.66f);
                }
                float mist = 1f - Math.abs(my - 222) / 22f; if (mist > 0) col = mix(col & 0xFFFFFF, 0x7882C8, 0.16f * mist);
                f.px[y * CW + x] = 0xFF000000 | col;
            }
        }
        for (int cx = 0; cx < 640; cx += 3) {                                                    // trees along the far shore
            if (vn(cx / 40f, 51) < 0.3f) continue; float th = 3 + vn(cx / 14f, 50) * 9 * (0.4f + h(cx));
            poly(f, new float[]{(cx - 2.2f) * kx, 225.5f * ky, cx * kx, (225 - th) * ky, (cx + 2.2f) * kx, 225.5f * ky}, 0x12163C, 0x12163C, 0, 1, 1f);
        }
        f.rect(0, Math.round(224.5f * ky), CW, Math.round(226 * ky), 0xFF12163C);
        farC = f;

        Canvas l = new Canvas(CW, CH);
        pine(l, kx, ky, 604, 356, 150, 0x070B16); pine(l, kx, ky, 566, 362, 100, 0x080D1A); pine(l, kx, ky, 632, 350, 110, 0x060912);
        {   float tx = 398, ty = hill(tx) + 3;                                                   // the tent, lamplight through its open flap
            poly(l, new float[]{(tx - 30) * kx, (ty + 4) * ky, tx * kx, (ty - 36) * ky, (tx + 34) * kx, (ty + 8) * ky}, 0x2C3466, 0x1A1F44, (ty - 36) * ky, (ty + 8) * ky, 1f);
            poly(l, new float[]{(tx - 30) * kx, (ty + 4) * ky, tx * kx, (ty - 36) * ky, (tx - 5) * kx, (ty + 5) * ky}, 0x4A548E, 0x2A3060, (ty - 36) * ky, (ty + 8) * ky, 1f);
            poly(l, new float[]{(tx - 9) * kx, (ty + 5) * ky, (tx + 1) * kx, (ty - 22) * ky, (tx + 13) * kx, (ty + 7) * ky}, 0xFFB85A, 0xFF7A2A, (ty - 22) * ky, (ty + 6) * ky, 1f);
            for (int i = 0; i <= 80; i++) { float q = i / 80f; l.set(Math.round((tx + 44 * q) * kx), Math.round((ty - 36 + 38 * q) * ky), 0xCC0A0D1C); l.set(Math.round((tx - 42 * q) * kx), Math.round((ty - 36 + 36 * q) * ky), 0xCC0A0D1C); }
        }
        float fireX = OX + FX * SC * ky / kx, fireY = groundY();
        for (int x = 0; x < CW; x++) {                                                           // the hill, warm where the fire reaches
            float mx = x / kx, gy = hill(mx);
            for (int y = Math.max(0, (int) (gy * ky)); y < CH; y++) {
                float my = y / ky, t = (my - 240) / 120f; int col = t < 0.35f ? mix(0x1B2A3C, 0x122030, t / 0.35f) : mix(0x122030, 0x070B14, (t - 0.35f) / 0.65f);
                float d = (float) Math.hypot(mx - fireX, (my - fireY) * 2), k = cl(1 - d / 150f);
                col = mix(col & 0xFFFFFF, 0x8A4A1E, 0.5f * k * k);
                if (my - gy < 1f) col = mix(col & 0xFFFFFF, 0x7F96C8, 0.28f);
                l.px[y * CW + x] = 0xFF000000 | col;
            }
        }
        for (float gx = 0; gx <= 640; gx += 0.75f) {                                              // grass: fine blades, each leaning its own way, warm near the fire
            int sd = Math.round(gx * 8); float y = hill(gx), bl = 2 + h(sd * 7) * 4.5f, lean = (h(sd * 3) - 0.5f) * 3;
            float d = (float) Math.hypot(gx - fireX, (y - fireY) * 2), k = cl(1 - d / 150f); int col = 0xE6000000 | (mix(0x24384A, 0xD8904A, k * 0.72f) & 0xFFFFFF);
            int n = Math.round(bl * ky) + 2; for (int i = 0; i < n; i++) { float q = i / (float) n; l.set(Math.round((gx + lean * q * q) * kx), Math.round((y + 1 - (bl + 1) * q) * ky), col); }
        }
        pine(l, kx, ky, 22, hill(22) + 6, 190, 0x060A14); pine(l, kx, ky, 66, hill(66) + 5, 128, 0x08101C); pine(l, kx, ky, -6, hill(0) + 8, 120, 0x05080F);
        landC = l;
    }

    // ------------------------------------------------------------------ the far part: the moon, the mountains and the lake
    static void far(Pen p, float x0, float y0, float w, float hgt, float a, float t) {
        if (a < 0.01f || farC == null) return;
        float kx = w / 640f, ky = hgt / 360f, mx = x0 + 460 * kx, my = y0 + 128 * ky, r = 9 * ky;
        p.glow(mx, my, 62 * ky, 62 * ky, argb(0.2f * a, 0xCFE0FF));
        for (float dy = -r; dy < r; dy += 1f) {                                                 // the moon, most of it in shadow
            float hw = (float) Math.sqrt(Math.max(0f, r * r - (dy + 0.5f) * (dy + 0.5f))); p.fill(mx - hw, my + dy, mx + hw, my + dy + 1, argb(a, 0xF2F5FF));
            float sr = r * 0.92f, sy = dy + 0.5f + 2.5f * ky * 0.6f, shw = sr * sr - sy * sy; if (shw <= 0) continue; shw = (float) Math.sqrt(shw);
            float sx0 = Math.max(mx - hw, mx + 4.2f * ky * 0.6f - shw), sx1 = Math.min(mx + hw, mx + 4.2f * ky * 0.6f + shw); if (sx1 > sx0) p.fill(sx0, my + dy, sx1, my + dy + 1, argb(0.6f * a, 0x0A0E34));
        }
        p.layer("far", farC, x0, y0, w, hgt, argb(a, 0xFFFFFF));
        for (int i = 0; i < 44; i++) {                                                          // the moon on the water: glints coming and going
            float y = 228 + h(i * 3) * 80, x = 460 + (h(i * 5) - 0.5f) * (26 + (y - 228) * 1.5f), wd = 3 + h(i * 9) * 9;
            float al = (0.12f + 0.3f * Math.max(0f, (float) Math.sin(t * 1.7f + i * 2.1f))) * (1 - (y - 228) / 110f); x += (float) Math.sin(t * 1.3f + i) * 2;
            p.fill(x0 + x * kx, y0 + y * ky, x0 + (x + wd) * kx, y0 + (y + 0.8f) * ky, argb(al * a, 0xDFE6FF));
        }
        for (int i = 0; i < 26; i++) {
            float y = 228 + h(i * 4 + 1) * 70, x = h(i * 6 + 2) * 640;
            p.fill(x0 + x * kx, y0 + y * ky, x0 + (x + 6 + h(i) * 10) * kx, y0 + (y + 0.7f) * ky, argb((0.06f + 0.08f * Math.max(0f, (float) Math.sin(t + i))) * a, 0x8F9AE0));
        }
    }

    // ------------------------------------------------------------------ the box models
    private static final int EM = 0x1000000;                                                     // a texel that glows by itself
    private static final int T_LEG = 0, T_BODY = 1, T_ARM = 2, T_HEAD = 3, T_SPY = 4, T_CAT = 5, T_CATHEAD = 6, T_CATD = 7, T_TAIL = 8, T_PAW = 9, T_BARK = 10, T_LOG = 11, T_POST = 12, T_PANE = 13, T_CAP = 14;
    private static final int SK = 0xC69C7C, SK2 = 0xA67C5E, HR = 0x3B2616, SH = 0x1FB8B8, SH2 = 0x179A9A, PT = 0x4A3FA8, PT2 = 0x3D3390, BT = 0x70707A, CATO = 0xE08A34, CATDK = 0xB8651E;
    /** One model: where it stands, which way it faces, its boxes {min x, y, z, size x, y, z, pivot x, y, z, turn x, y, z, texture}. */
    private static final class Model { final float[] pos; final float yaw; final List<float[]> boxes = new ArrayList<>(); Model(float x, float y, float z, float yaw) { pos = new float[]{x, y, z}; this.yaw = yaw; }
        Model box(float ax, float ay, float az, float sx, float sy, float sz, float px, float py, float pz, float rx, float ry, float rz, int tex) { boxes.add(new float[]{ax, ay, az, sx, sy, sz, px, py, pz, rx, ry, rz, tex}); return this; }
        Model box(float ax, float ay, float az, float sx, float sy, float sz, int tex) { return box(ax, ay, az, sx, sy, sz, 0, 0, 0, 0, 0, 0, tex); } }

    private static int tex(int id, char f, int u, int v, int nu, int nv, float t) {
        switch (id) {
            case T_HEAD: {
                if (f == 't') return HR; if (f == 'u') return SK2; if (f == 'b') return v < 5 ? HR : SK;
                if (f != 'f') return v < 3 || (v < 5 && (f == 'r' ? u > 5 : u < 2)) ? HR : SK;
                if (v < 2 || (v == 2 && (u == 0 || u == 7))) return HR;
                if (v == 4) { if (u == 1 || u == 6) return 0xFFFFFF; if (u == 2 || u == 5) return 0x4A3F9C; }
                if (v == 5 && (u == 3 || u == 4)) return SK2;
                if (v == 6) { if (u == 2 || u == 5) return 0x6A4030; if (u == 3 || u == 4) return 0x7A4A3A; }
                if (v == 7 && u >= 2 && u <= 5) return 0x6A4030;
                return SK; }
            case T_BODY: return f == 't' ? SH : f == 'u' ? PT : (f == 'f' && v == 0 && (u == 3 || u == 4)) ? SK : v >= nv - 1 ? PT2 : (f == 'f' || f == 'b') ? SH : SH2;
            case T_ARM: return f == 't' ? SH : f == 'u' ? SK2 : v < 4 ? (v == 3 ? SH2 : SH) : SK;
            case T_LEG: return f == 'u' ? BT : f == 't' ? PT : v >= nv - 2 ? BT : (f == 'f' || f == 'b') ? PT : PT2;
            case T_SPY: { if (f == 'f') return EM | 0xC9B0FF; if (f == 'b') return 0x5A341C; int i = f == 'l' || f == 'r' ? u : v, n = f == 'l' || f == 'r' ? nu : nv; return i < 2 || i > n - 3 ? 0xE8B07A : i == n / 2 ? 0xD89A5A : 0xB86A3A; }
            case T_CAT: return (f == 't' || f == 'l' || f == 'r') && (f == 't' ? v : u) % 3 == 1 ? CATDK : CATO;
            case T_CATHEAD: return f != 'f' ? (f == 't' && v % 2 == 1 ? CATDK : CATO) : v == 1 && (u == 1 || u == 3) ? (EM | 0x8AF05A) : v >= 3 ? (u == 2 && v == 3 ? 0xE89AA0 : 0xF4EAD8) : CATO;
            case T_CATD: return CATDK;
            case T_TAIL: { int i = f == 't' ? v : u; return i > 5 ? 0xF4EAD8 : i % 2 == 1 ? CATDK : CATO; }
            case T_PAW: return 0xF4EAD8;
            case T_BARK: return (f == 'f' || f == 'b') && nu < 8 ? ((u > 0 && u < nu - 1 && v > 0 && v < nv - 1) ? 0xB8905A : 0x5A4028) : (u * 3 + v * 5) % 9 < 2 ? 0x503A24 : v % 3 == 1 ? 0x5C4228 : 0x664A2C;
            case T_LOG: {                                                                        // the fire's logs: embers breathing on top, in the middle
                if (f == 't' && Math.abs(u - nu / 2f + 0.5f) < 3 && Math.abs(v - nv / 2f + 0.5f) < 3) return EM | (mix(0xC8421A, 0xFF8A2A, 0.5f + 0.5f * (float) Math.sin(t * 2.6f + u * 2.3f + v * 1.7f)) & 0xFFFFFF);
                return (u * 2 + v * 3) % 7 < 2 ? 0x3A281A : 0x54391F; }
            case T_POST: return v % 4 == 3 ? 0x6E5430 : 0x8A6A3C;
            case T_PANE: return f == 't' || f == 'u' ? 0x3A3D4A : (u == 0 || u == nu - 1 || v == 0 || v == nv - 1) ? 0x3A3D4A : (EM | (v < 3 ? 0xFFF0B0 : 0xFFC85A));
            default: return 0x4A4E5E;
        }
    }

    private static float[] rX(float[] p, float a) { float c = (float) Math.cos(a), s = (float) Math.sin(a); return new float[]{p[0], p[1] * c - p[2] * s, p[1] * s + p[2] * c}; }
    private static float[] rY(float[] p, float a) { float c = (float) Math.cos(a), s = (float) Math.sin(a); return new float[]{p[0] * c + p[2] * s, p[1], -p[0] * s + p[2] * c}; }
    private static float[] rZ(float[] p, float a) { float c = (float) Math.cos(a), s = (float) Math.sin(a); return new float[]{p[0] * c - p[1] * s, p[0] * s + p[1] * c, p[2]}; }
    private static float[] lin(float[] p, float[] b, float yaw) { return rX(rY(rY(rX(rZ(p, b[11]), -b[9]), b[10]), yaw), PITCH); }

    /** One visible face, ready to draw. */
    private record Face(float z, char name, float[] o, float[] tu, float[] tv, int nu, int nv, int tex, float lr, float lg, float lb) {}
    private static final char[] NAMES = {'f', 'b', 'r', 'l', 't', 'u'};
    private static final List<Face> FACES = new ArrayList<>();

    /** lights: {kind (0 = from a direction, 1 = a lamp), x, y, z, red, green, blue, reach, strength} */
    private static void render(Pen p, List<Model> models, float ox, float oy, float sc, float[][] lights, float[] amb, float a, float t) {
        FACES.clear();
        for (Model m : models) for (float[] b : m.boxes) {
            float x0 = b[0], y0 = b[1], z0 = b[2], w = b[3], hh = b[4], d = b[5], x1 = x0 + w, y1 = y0 + hh, z1 = z0 + d;
            float[] pv = rX(rY(new float[]{b[6], b[7], b[8]}, m.yaw), PITCH), mp = rX(m.pos, PITCH);
            float[][][] F = {{{x0, y1, z1}, {w, 0, 0}, {0, -hh, 0}, {w, hh}, {0, 0, 1}}, {{x1, y1, z0}, {-w, 0, 0}, {0, -hh, 0}, {w, hh}, {0, 0, -1}}, {{x1, y1, z1}, {0, 0, -d}, {0, -hh, 0}, {d, hh}, {1, 0, 0}},
                {{x0, y1, z0}, {0, 0, d}, {0, -hh, 0}, {d, hh}, {-1, 0, 0}}, {{x0, y1, z0}, {w, 0, 0}, {0, 0, d}, {w, d}, {0, 1, 0}}, {{x0, y0, z1}, {w, 0, 0}, {0, 0, -d}, {w, d}, {0, -1, 0}}};
            for (int fi = 0; fi < 6; fi++) {
                float[] n = lin(F[fi][4], b, m.yaw); if (n[2] <= 0.03f) continue;
                int nu = Math.max(1, Math.round(F[fi][3][0])), nv = Math.max(1, Math.round(F[fi][3][1]));
                float[] q = lin(new float[]{F[fi][0][0] - b[6], F[fi][0][1] - b[7], F[fi][0][2] - b[8]}, b, m.yaw), o = {q[0] + pv[0] + mp[0], q[1] + pv[1] + mp[1], q[2] + pv[2] + mp[2]};
                float[] tu = lin(F[fi][1], b, m.yaw), tv = lin(F[fi][2], b, m.yaw);
                for (int k = 0; k < 3; k++) { tu[k] /= nu; tv[k] /= nv; }
                float cx = o[0] + tu[0] * nu / 2 + tv[0] * nv / 2, cy = o[1] + tu[1] * nu / 2 + tv[1] * nv / 2, cz = o[2] + tu[2] * nu / 2 + tv[2] * nv / 2;
                float lr = amb[0], lg = amb[1], lb = amb[2];
                for (float[] L : lights) {
                    float dx, dy, dz, att = 1;
                    if (L[0] == 0) { dx = L[1]; dy = L[2]; dz = L[3]; }
                    else { dx = L[1] - cx; dy = L[2] - cy; dz = L[3] - cz; float dd = (float) Math.sqrt(dx * dx + dy * dy + dz * dz); if (dd < 1e-4f) dd = 1; dx /= dd; dy /= dd; dz /= dd; att = 1f / (1 + (dd / L[7]) * (dd / L[7])); }
                    float k = Math.max(0f, n[0] * dx + n[1] * dy + n[2] * dz) * att * L[8]; lr += L[4] * k; lg += L[5] * k; lb += L[6] * k;
                }
                FACES.add(new Face(cz, NAMES[fi], o, tu, tv, nu, nv, (int) b[12], lr, lg, lb));
            }
        }
        FACES.sort((u, v) -> Float.compare(u.z, v.z));
        int al = Math.round(cl(a) * 255) << 24;
        for (Face f : FACES) {
            float e = 0.012f, ux = f.tu[0] * sc, uy = -f.tu[1] * sc, vx = f.tv[0] * sc, vy = -f.tv[1] * sc;      // a hair bigger than it is: neighbouring faces overlap instead of leaving a seam
            float sx = ox + f.o[0] * sc - (ux * f.nu + vx * f.nv) * e, sy = oy - f.o[1] * sc - (uy * f.nu + vy * f.nv) * e;
            p.face(sx, sy, ux * (1 + 2 * e), uy * (1 + 2 * e), vx * (1 + 2 * e), vy * (1 + 2 * e));
            for (int j = 0; j < f.nv; j++) {
                int run = 0, prev = 0;
                for (int i = 0; i <= f.nu; i++) {
                    int col = 0;
                    if (i < f.nu) { int c = tex(f.tex, f.name, i, j, f.nu, f.nv, t);
                        if ((c & EM) != 0) col = al | (c & 0xFFFFFF);
                        else col = al | (Math.min(255, Math.round(((c >> 16) & 255) * f.lr)) << 16) | (Math.min(255, Math.round(((c >> 8) & 255) * f.lg)) << 8) | Math.min(255, Math.round((c & 255) * f.lb)); }
                    if (i == f.nu || (run > 0 && col != prev)) { p.cell(i - run, j, i, j + 1, prev); run = 0; }
                    prev = col; run++;
                }
            }
            p.endFace();
        }
    }

    // ------------------------------------------------------------------ the camp: what lives and moves
    private static float lookP = 16, lookY = 0, raise;
    /** Over Steve, the cat, the fire or the lantern (so pointing there is not pointing at a constellation). x, y: in the 640 x 360 picture. */
    static boolean over(float x, float y) { float g = groundY(); return x > OX - 190 && x < OX + 150 && y > g - 120 && y < g + 30 && y > hill(x) - 110; }

    /**
     * The hill, the trees and the tent, then the characters and the fire. t: seconds; dt: since the last frame; tx, ty: what you point
     * at, in the 640 x 360 picture (NaN: nothing).
     */
    static void near(Pen p, float x0, float y0, float w, float hgt, float a, float t, float dt, float tx, float ty) {
        if (a < 0.01f || landC == null) return;
        float kx = w / 640f, ky = hgt / 360f, sc = SC * ky, ox = x0 + OX * kx, oy = y0 + groundY() * ky;
        float fl = 0.82f + 0.1f * (float) Math.sin(t * 9) + 0.08f * (float) Math.sin(t * 23.7f);
        p.layer("land", landC, x0, y0, w, hgt, argb(a, 0xFFFFFF));
        boolean look = !Float.isNaN(tx);
        float k1 = 1f - (float) Math.exp(-Math.min(0.1f, dt) * 4.5f);
        float wp = look ? Math.max(16, Math.min(46, 52 - (ty - 40) * 0.22f)) : 16 + 5 * (float) Math.sin(t * 0.4f), wy = look ? Math.max(-42, Math.min(34, (tx - 250) * 0.16f)) : 10 * (float) Math.sin(t * 0.23f);
        lookP += (wp - lookP) * k1; lookY += (wy - lookY) * k1; raise += ((look ? 1f : 0f) - raise) * k1;
        float pet = look ? 0f : sm(((float) Math.sin(t * 0.5f) - 0.86f) * 9);

        float fx = ox + FX * sc + 4 * ky, fy = oy - 20 * ky;
        p.glow(ox + FX * sc, oy - 6 * ky, 120 * ky, 80 * ky, argb(0.26f * fl * a, 0xFF8A2A));       // the firelight on everything round it
        p.glow(ox + 4 * ky, oy + 3 * ky, 50 * ky, 7 * ky, argb(0.55f * a, 0x04060C)); p.glow(ox - 70 * ky, oy + 3 * ky, 26 * ky, 4 * ky, argb(0.55f * a, 0x04060C));   // shadows under them

        float D = (float) Math.PI / 180f, hp = lookP * D, hy = lookY * D, k = sm(raise);
        // the spyglass: lying across the lap, or held a little in front of the eye. The arm reaches for wherever it is.
        float[] e0 = rY(rX(new float[]{2, 3.5f, 5.8f}, -hp), hy);
        float ex = mixf(4.6f, e0[0], k), ey = mixf(15.6f, e0[1] + 24, k), ez = mixf(4.5f, e0[2], k), sp = mixf(-0.16f, hp, k), sy = mixf(-0.35f, hy, k);
        float gx = ex + (float) (Math.cos(sp) * Math.sin(sy)) * 4.5f - 6, gy = ey + (float) Math.sin(sp) * 4.5f - 22, gz = ez + (float) (Math.cos(sp) * Math.cos(sy)) * 4.5f, gl = (float) Math.sqrt(gx * gx + gy * gy + gz * gz);
        float armX = (float) Math.atan2(gz, -gy), armZ = (float) Math.asin(Math.max(-1f, Math.min(1f, gx / Math.max(0.001f, gl))));
        float leg = 76 * D, lean = -8 * D;
        List<Model> ms = new ArrayList<>();
        ms.add(new Model(-44, 0, -6, 0.5f).box(-1.5f, 0, -1.5f, 3, 15, 3, T_POST).box(-3, 15, -3, 6, 7, 6, T_PANE).box(-2, 22, -2, 4, 2, 4, T_CAP));          // the lantern on its post
        ms.add(new Model(0, 0, -1, 0.75f).box(-12, 0, -4, 24, 6, 6, T_BARK));                                                                                  // the log he sits on
        ms.add(new Model(0, -6, 0, 0.75f)
            .box(-4, 0, -2, 4, 12, 4, -2, 12, 0, leg, -0.06f, 0, T_LEG).box(0, 0, -2, 4, 12, 4, 2, 12, 0, leg, 0.06f, 0, T_LEG)
            .box(-4, 12, -2, 8, 12, 4, 0, 12, 0, lean, 0, 0, T_BODY)
            .box(-8, 12, -2, 4, 12, 4, -6, 22, 0, mixf(-24, 26, pet) * D, 0, -mixf(8, 30, pet) * D, T_ARM)
            .box(4, 12, -2, 4, 12, 4, 6, 22, 0, armX, 0, armZ, T_ARM)
            .box(-4, 24, -4, 8, 8, 8, 0, 24, 0, hp, hy, 0, T_HEAD)
            .box(ex - 1, ey - 1, ez, 2, 2, 11, ex, ey, ez, sp, sy, 0, T_SPY));
        float tail = (float) Math.sin(t * 2.1f) * 0.5f + pet * 0.4f, up = pet * 0.8f;
        ms.add(new Model(-22, 0, 10, 1.35f).box(-2.5f, 0, -5, 5, 4, 10, T_CAT).box(-2.5f, 2 + up, 4, 5, 5, 5, T_CATHEAD).box(-2.5f, 7 + up, 6, 1.2f, 1.6f, 1.2f, T_CATD).box(1.3f, 7 + up, 6, 1.2f, 1.6f, 1.2f, T_CATD)
            .box(-0.6f, 2, -13, 1.2f, 1.2f, 8, 0, 2.6f, -5, 0.25f, tail, 0, T_TAIL).box(-2.2f, 0, 4.5f, 1.6f, 1.2f, 3, T_PAW).box(0.6f, 0, 4.5f, 1.6f, 1.2f, 3, T_PAW));
        ms.add(new Model(FX, 0, FZ, 0.4f).box(-7, 0, -7, 4, 3, 14, T_LOG).box(3, 0, -7, 4, 3, 14, T_LOG).box(-7, 3, -6, 14, 3, 4, T_LOG).box(-7, 3, 2, 14, 3, 4, T_LOG));
        float[] fp = rX(new float[]{FX, 9, FZ}, PITCH), lp = rX(new float[]{-44, 19, -6}, PITCH);
        float[][] lights = {{0, -0.55f, 0.62f, 0.55f, 0.5f, 0.56f, 0.8f, 0, 1}, {1, fp[0], fp[1], fp[2], 1.5f, 0.78f, 0.3f, 46, fl}, {1, lp[0], lp[1], lp[2], 1.0f, 0.72f, 0.36f, 16, 1}};
        render(p, ms, ox, oy, sc, lights, new float[]{0.3f, 0.3f, 0.42f}, a, t);

        float[][] tongues = {{-7, 6, 20, 0xE8501A, 0}, {6, 6, 18, 0xE8501A, 2}, {0, 8, 30, 0xFF8A2A, 1}, {-3, 5, 19, 0xFFC04A, 3}, {3, 4.5f, 21, 0xFFC04A, 4}, {0, 3, 13, 0xFFF2B0, 5}};
        for (float[] e : tongues) {                                                              // the flames: tongues that lean and lick
            float bx = fx + e[0] * ky, by = fy + 8 * ky, tw = e[1] * ky, th = e[2] * ky * (0.85f + 0.15f * (float) Math.sin(t * 11 + e[4] * 2)); int n = 14;
            for (int i = 0; i < n; i++) { float v = (i + 0.5f) / n, ww = tw * (float) Math.sin(Math.pow(v, 0.6) * Math.PI) * (1 - v * 0.35f), sway = (float) Math.sin(t * 9 + e[4] + v * 4) * tw * 0.35f * v;
                p.fill(bx - ww + sway, by - (i + 1f) / n * th, bx + ww + sway, by - (float) i / n * th + 0.3f, argb(a, (int) e[3])); }
        }
        p.glow(fx, fy - 4 * ky, 26 * ky, 26 * ky, argb(0.6f * fl * a, 0xFFC86A));
        p.glow(ox - 44 * sc, oy - 65 * ky, 34 * ky, 34 * ky, argb(0.42f * a, 0xFFC85A));                                                     // the lantern
        for (int i = 0; i < 8; i++) { float u = (t * 0.5f + i / 8f) % 1f, x = fx + (float) Math.sin(u * 7 + i * 2) * (4 + u * 10) * ky, y = fy - (10 + u * 70) * ky; p.fill(x - 0.8f * ky, y - 0.8f * ky, x + 0.8f * ky, y + 0.8f * ky, argb((1 - u) * 0.9f * a, 0xFFB04A)); }   // sparks
        for (int i = 0; i < 5; i++) { float u = (t * 0.16f + i / 5f) % 1f, r = (8 + u * 16) * ky; p.glow(fx + ((float) Math.sin(u * 4 + i) * 8 + u * 18) * ky, fy - (24 + u * 90) * ky, r, r, argb((1 - u) * 0.1f * a, 0x9AA4C8)); }                          // smoke
        for (int i = 0; i < 9; i++) {                                                            // fireflies over the grass
            float mx = 90 + h(i * 9) * 300 + (float) Math.sin(t * 0.6f + i) * 8, my = hill(mx) - 8 - h(i * 5) * 26 + (float) Math.cos(t * 0.8f + i * 2) * 4, al = Math.max(0f, (float) Math.sin(t * 1.5f + i * 1.9f));
            float x = x0 + mx * kx, y = y0 + my * ky; p.glow(x, y, 5 * ky, 5 * ky, argb(al * 0.4f * a, 0xE8FF80)); p.fill(x - 0.8f * ky, y - 0.8f * ky, x + 0.8f * ky, y + 0.8f * ky, argb(al * a, 0xE8FF80));
        }
    }
}
