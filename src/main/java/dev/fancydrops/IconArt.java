package dev.fancydrops;

/**
 * The pictures of every ":name:" icon, drawn in code as 16 x 16 pixel art with depth: a light from the top left (bright top and left
 * edges, darker bottom and right), a deep-coloured outline, and a few frames of animation. Pure code, no Minecraft classes, so the
 * whole set can be rendered anywhere. render() returns 256 ARGB pixels, or null when the name has no picture.
 */
final class IconArt {
    private IconArt() {}

    static final int FRAMES = 16;
    private static Canvas c;
    private static final float TAU = 6.2832f;

    // ------------------------------------------------------------------ the entry point
    /** The picture of one icon at one frame (0..FRAMES-1). tint (RGB, or 0) colours the icons that take a colour. */
    static synchronized int[] render(String name, int tint, int frame) {                   // synchronized: the icon export runs on a background thread and the canvas is shared
        c = new Canvas(16, 16);
        float f = (frame % FRAMES) / (float) FRAMES;
        if (!paint(name, tint, f)) return null;
        return c.px.clone();
    }

    static boolean has(String name) { return render(name, 0, 0) != null; }

    // ------------------------------------------------------------------ drawing helpers
    private static int mix(int a, int b, float f) { return Canvas.mix(0xFF000000 | a, 0xFF000000 | b, f) & 0xFFFFFF; }
    private static int lite(int rgb, float f) { return mix(rgb, 0xFFFFFF, f); }
    private static int dark(int rgb, float f) { return mix(rgb, 0x000000, f); }
    private static void px(int x, int y, int rgb) { c.set(x, y, 0xFF000000 | rgb); }
    private static void pxa(int x, int y, int rgb, float a) { if (a > 0.02f) c.set(x, y, Canvas.rgb(Math.round(Math.min(1f, a) * 255), rgb)); }
    private static void rect(int x, int y, int w, int h, int rgb) { for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) px(x + i, y + j, rgb); }
    /** A block with a lit top and left edge and a shaded bottom and right edge. */
    private static void bev(int x, int y, int w, int h, int rgb) {
        rect(x, y, w, h, rgb);
        for (int i = 0; i < w; i++) { px(x + i, y, lite(rgb, 0.42f)); px(x + i, y + h - 1, dark(rgb, 0.38f)); }
        for (int j = 1; j < h - 1; j++) { px(x, y + j, lite(rgb, 0.22f)); px(x + w - 1, y + j, dark(rgb, 0.28f)); }
    }
    private static void ln(float x0, float y0, float x1, float y1, int rgb) {
        int n = (int) Math.max(1, Math.ceil(Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0))));
        for (int i = 0; i <= n; i++) px(Math.round(x0 + (x1 - x0) * i / n), Math.round(y0 + (y1 - y0) * i / n), rgb);
    }
    /** A ball lit from the top left: bright on that side, dark on the other, with a white glint. */
    private static void ball(float cx, float cy, float r, int rgb) {
        for (int y = (int) Math.floor(cy - r); y <= Math.ceil(cy + r); y++) for (int x = (int) Math.floor(cx - r); x <= Math.ceil(cx + r); x++) {
            float dx = (x + 0.5f - cx) / r, dy = (y + 0.5f - cy) / r, d2 = dx * dx + dy * dy;
            if (d2 > 1f) continue;
            float nz = (float) Math.sqrt(1f - d2), l = Math.max(0f, Math.min(1f, 0.55f + 0.55f * (-dx * 0.55f - dy * 0.7f + nz * 0.45f) - 0.25f));
            px(x, y, l > 0.55f ? lite(rgb, (l - 0.55f) * 1.1f) : dark(rgb, (0.55f - l) * 0.9f));
        }
        px(Math.round(cx - r * 0.38f), Math.round(cy - r * 0.42f), 0xFFFFFF);
    }
    private static boolean inside(float[] xs, float[] ys, float x, float y) {
        boolean in = false;
        for (int i = 0, j = xs.length - 1; i < xs.length; j = i++) if ((ys[i] > y) != (ys[j] > y) && x < (xs[j] - xs[i]) * (y - ys[i]) / (ys[j] - ys[i]) + xs[i]) in = !in;
        return in;
    }
    /** A filled shape, lit from the top left (the colour runs from light to dark across it). */
    private static void poly(float[] xs, float[] ys, int rgb) {
        float x0 = 99, x1 = -99, y0 = 99, y1 = -99;
        for (int i = 0; i < xs.length; i++) { x0 = Math.min(x0, xs[i]); x1 = Math.max(x1, xs[i]); y0 = Math.min(y0, ys[i]); y1 = Math.max(y1, ys[i]); }
        for (int y = Math.max(0, (int) y0); y <= Math.min(15, (int) Math.ceil(y1)); y++) for (int x = Math.max(0, (int) x0); x <= Math.min(15, (int) Math.ceil(x1)); x++) {
            if (!inside(xs, ys, x + 0.5f, y + 0.5f)) continue;
            float t = ((x + 0.5f - x0) / Math.max(1f, x1 - x0) * 0.45f + (y + 0.5f - y0) / Math.max(1f, y1 - y0) * 0.55f);
            px(x, y, t < 0.3f ? lite(rgb, 0.4f * (1f - t / 0.3f)) : dark(rgb, (t - 0.3f) / 0.7f * 0.5f));
        }
    }
    private static void tri(float ax, float ay, float bx, float by, float cx, float cy, int rgb) { poly(new float[]{ax, bx, cx}, new float[]{ay, by, cy}, rgb); }
    private static void quad(float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy, int rgb) { poly(new float[]{ax, bx, cx, dx}, new float[]{ay, by, cy, dy}, rgb); }
    private static void outline() { c.softOutline(0.34f); }
    /** Water along the bottom, waves rolling. */
    private static void waves(float f, int rgb) { for (int y = 11; y < 16; y++) for (int x = 0; x < 16; x++) { boolean crest = y == 11 && ((x + (int) (f * 16)) % 6 < 2); px(x, y, crest ? lite(rgb, 0.5f) : mix(rgb, dark(rgb, 0.4f), (y - 11) / 5f)); } }
    /** Lava along the bottom, bubbling. */
    private static void lava(float f) { for (int y = 10; y < 16; y++) for (int x = 0; x < 16; x++) { float n = (float) Math.sin(x * 1.3f + f * TAU) * 0.5f + 0.5f; px(x, y, mix(0xC82A0A, 0xFFB030, n * (1f - (y - 10) / 6f))); } int bx = (int) (f * 5) * 3 % 14 + 1; px(bx, 9, 0xFFE07A); }
    private static void glint(int x, int y, int r, int rgb, float a) { c.glint(x, y, r, rgb, Math.max(0f, Math.min(1f, a))); }
    private static void glow(float x, float y, float r, int rgb, float s) { c.glow(x, y, r, rgb, s); }
    private static float tri(float f) { return 1f - Math.abs(2f * f - 1f); }                  // 0 .. 1 .. 0 over the loop
    private static float sn(float f) { return (float) Math.sin(f * TAU); }
    private static int hue(float h, float s, float v) { return java.awt.Color.HSBtoRGB(((h % 1f) + 1f) % 1f, s, v) & 0xFFFFFF; }

    /** 3 x 5 letters for the text-effect tiles and a few labels. */
    private static final String[] GLYPH_A = {".#.", "#.#", "###", "#.#", "#.#"};
    private static void letter(String[] g, int x, int y, int rgb, boolean shade) {
        for (int j = 0; j < g.length; j++) for (int i = 0; i < g[j].length(); i++) if (g[j].charAt(i) == '#') px(x + i, y + j, shade ? (j < 2 ? lite(rgb, 0.3f) : j > 3 ? dark(rgb, 0.3f) : rgb) : rgb);
    }

    // ------------------------------------------------------------------ the icons
    private static boolean paint(String n, int tint, float f) {
        float s = sn(f), t = tri(f);
        if (n.startsWith("fx") && n.length() <= 4 && Character.isDigit(n.charAt(n.length() - 1))) return fx(Integer.parseInt(n.substring(2)), f);
        switch (n) {
            // ---- the tab icons (0.8.0, "vault" set): one family, gold trim, a dark outline, light from the top left, one small motion each
            case "home" -> {                                                                                 // Home: a little observatory dome on a moon, its slit open, a telescope peeking out, stars over it
                ball(8, 16, 9, 0x2A2858); ball(8, 15.5f, 8, 0x3A3870);                                // the moon's curve
                ball(8, 9.5f, 5, 0xC8C4E8); rect(3, 9, 11, 3, 0xC8C4E8); ball(7, 8.5f, 3.5f, 0xE8E4FF);   // the dome
                rect(7, 5, 2, 6, 0x1A1848); ln(8, 7, 12, 3 + Math.round(s), 0x9B7BFF);              // the slit, the telescope
                rect(3, 12, 11, 1, 0x7672AE);
                glint(13, 2, 1, 0xFFFFFF, 0.5f + 0.5f * t); px(2, 3, 0xFFE38A); px(4, 1, 0xFFFFFF);
                outline();
            }
            case "dropped" -> {                                                                                 // the Drop Library: a meteor falling into a glowing crater, a gem glinting where it lands
                ball(8, 14, 7, 0x2A2858); ball(8, 13.6f, 5, 0x1A1848);                                // the crater
                glow(8, 13, 5, 0xFF7AD9, 0.35f + 0.2f * t);
                float u = f % 1f; int hx = Math.round(3 + u * 5), hy = Math.round(1 + u * 9);
                for (int i = 1; i < 5; i++) pxa(hx - i, hy - i, i < 2 ? 0xFFFFFF : 0x9B7BFF, 1f - i / 5f);
                ball(hx, hy, 1.4f, 0xFFF4D8);
                tri(8, 9.5f, 6, 12, 10, 12, 0xB48CFF); px(8, 11, 0xFFFFFF);                         // the gem
                outline();
            }
            case "animations" -> {                                                                                 // Animations: a planet with a moon swinging round it, a motion trail behind the moon
                ball(8, 8, 4.5f, 0x5A4AA8); ball(7, 7, 3.6f, 0x9B7BFF); px(6, 6, 0xE8E0FF);
                for (int i = 0; i < 16; i++) { float an = i / 16f * TAU; pxa(Math.round(8 + (float) Math.cos(an) * 7), Math.round(8 + (float) Math.sin(an) * 3), 0x7672AE, 0.5f); }
                float an = f * TAU; for (int k = 3; k >= 0; k--) { float a2 = an - k * 0.25f; pxa(Math.round(8 + (float) Math.cos(a2) * 7), Math.round(8 + (float) Math.sin(a2) * 3), k == 0 ? 0xFFFFFF : 0xFF7AD9, 1f - k * 0.25f); }
                ball(8 + (float) Math.cos(an) * 7, 8 + (float) Math.sin(an) * 3, 1.3f, 0xFFE9F8);
                outline();
            }
            case "sounds" -> {                                                                                 // Sounds: a radio dish on its stand, signal arcs travelling out into space
                rect(4, 12, 6, 2, 0x5A5A88); rect(6, 9, 2, 3, 0x7672AE);
                quad(1, 6, 7, 2, 10, 7, 4, 11, 0xC8C4E8); ln(2, 7, 8, 3, 0xFFFFFF);                   // the dish
                ln(5, 6, 8, 4, 0x7672AE); px(9, 3, 0xFF7AD9);                                        // its feed
                for (int i = 0; i < 3; i++) { float u = (f + i / 3f) % 1f; int r = 3 + Math.round(u * 4);
                    for (int k = -2; k <= 2; k++) { float a2 = -0.8f + k * 0.25f; pxa(Math.round(9 + (float) Math.cos(a2) * r), Math.round(3 + (float) Math.sin(a2) * r), 0x9B7BFF, 1f - u); } }
                outline();
            }
            case "chat" -> {                                                                                 // Chat: a speech bubble shaped from a little nebula, three stars in it lighting up in turn
                ball(5, 7, 4, 0x6A4AD8); ball(10, 7, 4.5f, 0x8A6AE8); rect(4, 6, 8, 5, 0x7A5AE0); tri(3, 10, 6, 10, 2, 14, 0x6A4AD8);
                ball(9, 5.5f, 2, 0xA88AFF);
                for (int i = 0; i < 3; i++) { float on = Math.max(0f, (float) Math.sin(f * TAU - i * 1.2f)); int x = 5 + i * 3; px(x, 8, mix(0xC8B4FF, 0xFFFFFF, on)); if (on > 0.6f) { pxa(x - 1, 8, 0xFFFFFF, on - 0.4f); pxa(x + 1, 8, 0xFFFFFF, on - 0.4f); pxa(x, 7, 0xFFFFFF, on - 0.4f); pxa(x, 9, 0xFFFFFF, on - 0.4f); } }
                outline();
            }
            case "settings" -> {                                                                                 // Settings: a clean eight-tooth gear, turning slowly, a little star in its hub
                float rot = 0f;                                                                      // held still: a turning 16px gear only jitters
                for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
                    int hit = 0; float lit = 0;
                    for (int sy = 0; sy < 4; sy++) for (int sx = 0; sx < 4; sx++) {
                        float dx = x + (sx + 0.5f) / 4f - 8f, dy = y + (sy + 0.5f) / 4f - 8f, r = (float) Math.sqrt(dx * dx + dy * dy);
                        float an = (float) Math.atan2(dy, dx) - rot, ph = ((an / TAU * 8f) % 1f + 1f) % 1f;
                        float tooth = Math.abs(ph - 0.5f) < 0.2f ? 7.3f : 5.4f;
                        if (r < tooth && r > 2.3f) { hit++; lit += (-dx - dy) / 11f; }
                    }
                    if (hit == 0) continue;
                    float l = lit / hit; int col = mix(0x8A90B8, l > 0 ? 0xE6E8FF : 0x4A4E7A, Math.min(1f, Math.abs(l) * 1.4f));
                    pxa(x, y, col, hit / 16f);
                }
                glint(8, 8, 1, 0xFFE38A, 0.55f + 0.45f * t);
                outline();
            }
            case "collection" -> {                                                                                 // the Collection: a star atlas, the cover lifting, light and a constellation spilling out
                bev(2, 6, 12, 9, 0x2C2A66); rect(3, 7, 10, 7, 0x1A1848);
                int lift = Math.round(2 * t); quad(2, 6 - lift, 14, 6 - lift, 13, 4 - lift, 3, 4 - lift, 0x5A4AA8);
                glow(8, 6, 5, 0xC8B4FF, 0.3f * t);
                px(5, 9, 0xFFFFFF); px(8, 11, 0xFFE38A); px(11, 9, 0xFFFFFF); ln(5, 9, 8, 11, 0x7672AE); ln(8, 11, 11, 9, 0x7672AE);
                outline();
            }
            case "trophy" -> {                                                                                 // Achievements: a cup made of starlight on a dark base, a star glinting on it
                rect(5, 13, 6, 2, 0x2C2A66); rect(7, 10, 2, 3, 0x7672AE);
                quad(3, 3, 13, 3, 11, 9, 5, 9, 0xC8B4FF); quad(4, 3, 8, 3, 7, 8, 5, 8, 0xE8E0FF);
                ln(2, 4, 3, 7, 0x9B7BFF); ln(14, 4, 13, 7, 0x9B7BFF);
                glint(10, 5, 1, 0xFFFFFF, 0.5f + 0.5f * t); px(8, 5, 0xFF7AD9);
                outline();
            }
            case "stats" -> {                                                                                 // Stats: a little star chart, points joined by a climbing line, the newest twinkling
                bev(1, 2, 14, 12, 0x14163E); rect(2, 3, 12, 10, 0x0E1030);
                int[] xs = {3, 6, 9, 12}, ys = {11, 9, 10, 5};
                for (int i = 0; i < 3; i++) ln(xs[i], ys[i], xs[i + 1], ys[i + 1], 0x9B7BFF);
                for (int i = 0; i < 4; i++) px(xs[i], ys[i], i == 3 ? mix(0xFF7AD9, 0xFFFFFF, t) : 0xE8E0FF);
                glint(12, 5, 1, 0xFFFFFF, t); px(4, 4, 0x5A5A88); px(10, 7, 0x5A5A88);
                outline();
            }
            case "mytho" -> {                                                                // the minotaur: a horned head with glaring eyes
                tri(2, 7, 1, 2, 5.5f, 5.2f, 0xF4EBD2); tri(14, 7, 15, 2, 10.5f, 5.2f, 0xF4EBD2);
                quad(4, 5, 12, 5, 11.5f, 12, 4.5f, 12, 0x7A4A2B);
                quad(5.5f, 9, 10.5f, 9, 10, 14, 6, 14, 0xA8714A);
                rect(6, 12, 1, 1, 0x2A1A10); rect(9, 12, 1, 1, 0x2A1A10);
                outline();
                float e = 0.5f + 0.5f * t; int ec = mix(0xFF7A3A, 0xFFE9A0, e);
                rect(5, 8, 2, 1, ec); rect(9, 8, 2, 1, ec);
                glow(6, 8, 3, 0xFF5A2A, 0.35f * e); glow(10, 8, 3, 0xFF5A2A, 0.35f * e);
                if (f > 0.6f && f < 0.85f) { px(8, 1, 0xFFFFFF); px(7, 2, 0x9FEFFF); px(8, 3, 0xFFFFFF); }
            }
            case "treasure" -> {                                                             // a spade stuck in the ground beside a heap of gold it just dug up, a glint running over the coins
                fillMound();
                ln(4, 1, 6.5f, 9, 0x8A5A2C); ln(5, 1, 7.5f, 9, 0x6B4423); rect(3, 1, 3, 1, 0x3A2410);       // the spade, leaning, blade in the earth
                poly(new float[]{5.4f, 9.4f, 8.2f, 7.8f}, new float[]{9, 9.6f, 12.4f, 14f}, 0xA8B2C8); quad(5.6f, 9.3f, 6.6f, 9.4f, 6.8f, 12.4f, 6.2f, 12f, 0xE8EEF8);
                rect(10, 11, 5, 3, 0xE0A820); rect(11, 9, 3, 2, 0xF2C230); rect(12, 8, 1, 1, 0xFFE27A);      // the gold heap: stacked ingots and coins
                rect(9, 13, 6, 1, 0xB07A12); px(10, 11, 0xFFF3B0); px(13, 10, 0xFFF3B0); px(11, 12, 0xB07A12); px(14, 12, 0xFFE27A);
                outline();
                float u = (f * 2f) % 1f; int gx = 10 + Math.round(u * 4f); px(gx, 9 + (gx % 2), 0xFFFFFF);
                glint(12, 9, 1, 0xFFF3B0, 0.4f + 0.6f * t);
                glow(12, 10, 5, 0xFFD25A, 0.14f + 0.12f * t);
                float up = (f * 1.5f) % 1f; pxa(11, 7 - Math.round(up * 5), 0xFFE27A, 1f - up);             // a nugget glimmer floating up
            }
            case "anvil_hit" -> {                                                            // an anvil being struck: a hammer comes down, sparks fly off the hit
                rect(2, 12, 12, 2, 0x4A4F5A); rect(4, 10, 8, 2, 0x5A606C); rect(6, 8, 4, 2, 0x6A7080);          // the foot, the waist
                rect(1, 5, 14, 3, 0x7A8090); rect(1, 5, 14, 1, 0xB8C0D0); px(0, 6, 0x7A8090); px(15, 6, 0x5A606C);   // the face, with a horn at the left
                rect(2, 8, 12, 1, 0x4A4F5A);
                float h = f < 0.45f ? f / 0.45f : f < 0.55f ? 1f : 1f - (f - 0.55f) / 0.45f;                     // 0 raised .. 1 down
                float hy = 5f - (1f - h) * 6f, hx = 8.5f + (1f - h) * 2f;
                ln(hx + 4, hy - 4, hx + 1, hy - 1, 0x8A5A2C); ln(hx + 5, hy - 4, hx + 2, hy - 1, 0x6B4423);       // the handle
                rect(Math.round(hx) - 2, Math.round(hy) - 3, 5, 3, 0xA8B2C8); rect(Math.round(hx) - 2, Math.round(hy) - 3, 5, 1, 0xE8EEF8);
                outline();
                if (f > 0.45f) { float u = (f - 0.45f) / 0.55f; for (int i = 0; i < 6; i++) { float an = -0.3f - i * 0.5f; pxa(Math.round(8 + (float) Math.cos(an) * u * (4 + i % 3 * 2) * (i % 2 == 0 ? 1 : -1)), Math.round(5 - (float) Math.sin(-an) * u * (5 + i % 2 * 3) + u * u * 4), i % 2 == 0 ? 0xFFE27A : 0xFFA030, 1f - u); } glint(8, 4, 1, 0xFFFFFF, 1f - (f - 0.45f) / 0.2f); }
            }
            case "slayers" -> {                                                              // Slayers: a bone-white skull with a sword behind it, its eyes burning red
                ln(2, 14, 13, 3, 0xC9D2E4); ln(3, 14, 14, 3, 0x9AA6BE); ln(2, 13, 13, 2, 0xE8EEF8);                          // the blade
                rect(1, 13, 3, 1, 0x8A5A2B); rect(2, 12, 1, 3, 0x8A5A2B); px(1, 15, 0xE8B84A);                               // guard and grip
                ball(8, 7, 4.6f, 0xEDE6D6);
                rect(5, 10, 6, 3, 0xEDE6D6); rect(6, 12, 1, 1, 0x8A8296); rect(8, 12, 1, 1, 0x8A8296); rect(10, 12, 1, 1, 0x8A8296);
                outline();
                float e = 0.5f + 0.5f * t; int ec = mix(0xB01818, 0xFF6A4A, e);
                rect(5, 7, 2, 2, 0x1A1018); rect(9, 7, 2, 2, 0x1A1018); px(6, 8, ec); px(10, 8, ec); px(5, 7, mix(0x1A1018, ec, 0.5f)); px(9, 7, mix(0x1A1018, ec, 0.5f));
                px(8, 10, 0x3A2A30);
                glow(6, 8, 3, 0xFF3A2A, 0.25f * e); glow(10, 8, 3, 0xFF3A2A, 0.25f * e);
            }
            case "grp_slayers" -> {                                                          // Slayers: a slayer's blade driven through a boss's horned skull, embers rising, the eyes burning
                glow(8, 9, 8, 0xFF3A3A, 0.16f + 0.12f * t);
                tri(3, 6, 1, 1, 6, 4, 0x5A4A40); tri(13, 6, 15, 1, 10, 4, 0x5A4A40);                      // horns
                ball(8, 9, 5.2f, 0xD8D0BC); rect(5, 12, 6, 3, 0xC8C0A8); rect(6, 15, 4, 1, 0xA8A090);       // skull and jaw
                rect(5, 8, 2, 2, 0x140A0C); rect(9, 8, 2, 2, 0x140A0C); int ey = mix(0xC81A1A, 0xFF7A4A, t); px(6, 9, ey); px(9, 9, ey); px(5, 8, dark(ey, 0.3f));
                px(8, 11, 0x3A2A28); px(6, 13, 0x5A5048); px(8, 13, 0x5A5048); px(10, 13, 0x5A5048);
                ln(14, 1, 9, 6, 0xE8EEF8); ln(15, 1, 10, 6, 0x9AA4C0); rect(8, 5, 3, 1, 0xD8A02A); rect(9, 4, 1, 3, 0xD8A02A);   // the blade going in
                px(9, 7, 0xB01818); px(9, 8, 0x8A1010);
                outline();
                for (int i = 0; i < 3; i++) { float u = (f + i / 3f) % 1f; pxa(3 + i * 5, Math.round(14 - u * 13), i == 1 ? 0xFFC060 : 0xFF6A3A, sn(u * 0.5f)); }
                glint(14, 1, 1, 0xFFFFFF, t);
            }
            case "grp_end" -> {                                                              // The End: an Ender Dragon's head in profile, a violet eye glowing, its breath curling out
                glow(8, 8, 8, 0xC77DFF, 0.14f + 0.1f * t);
                rect(3, 5, 8, 6, 0x1E1A26); rect(10, 7, 4, 4, 0x1E1A26); rect(12, 10, 3, 1, 0x2A2434);         // skull and snout
                rect(3, 5, 8, 1, 0x3A3448); rect(10, 7, 4, 1, 0x3A3448);
                px(2, 3, 0x2A2434); px(3, 4, 0x2A2434); px(5, 3, 0x2A2434); px(6, 4, 0x2A2434);                 // horns
                rect(4, 11, 7, 2, 0x16121C); rect(11, 11, 3, 1, 0x16121C);                                    // the jaw, opening a little
                int eye = mix(0x9A4AE0, 0xF0C8FF, t); rect(8, 7, 2, 1, eye); px(9, 7, 0xFFFFFF);
                for (int i = 0; i < 4; i++) { float u = (f + i * 0.25f) % 1f; pxa(14 + Math.round(u * 1.5f), 11 - Math.round(u * 6) + Math.round(sn(u + i * 0.3f)), mix(0xC77DFF, 0xFFFFFF, u), 1f - u); }
                outline();
            }
            case "grp_crimson" -> {                                                          // the Crimson Isle: a volcano of netherrack, lava pouring down its side, an ember plume
                poly(new float[]{1, 6, 10, 15}, new float[]{15, 5, 5, 15}, 0x8A2A28);
                rect(6, 5, 4, 1, 0x3A0A0A); rect(7, 4, 2, 1, 0xFF8A2A);
                for (int y = 6; y < 15; y++) { int x = 8 + (y % 3 == 0 ? 1 : 0) - (y > 10 ? 1 : 0); px(x, y, mix(0xFF5A1A, 0xFFC040, ((y + f * 9) % 3) / 3f)); }   // the lava running down
                rect(2, 14, 12, 1, 0x5A1414); px(4, 12, 0xC83A2A); px(12, 11, 0xC83A2A);
                outline();
                for (int i = 0; i < 4; i++) { float u = (f + i / 4f) % 1f; pxa(7 + (i % 2) + Math.round(sn(u + i) * 1.5f), Math.round(3 - u * 3.5f), i % 2 == 0 ? 0xFFB040 : 0xFF5A2A, 1f - u); }
                glow(8, 4, 5, 0xFF6A2A, 0.2f + 0.15f * t);
            }
            case "grp_water" -> {                                                            // Water: a blue wave curls over, a fish leaps out of it, spray at the crest
                for (int x = 0; x < 16; x++) { float h = 9 + 2.2f * sn(f + x * 0.35f); for (int y = Math.round(h); y < 15; y++) px(x, y, mix(0x3FA9FF, 0x0A3A78, (y - h) / 7f)); px(x, Math.round(h), 0xBDEBFF); }
                float jy = 9 - 7f * 4f * f * (1f - f), jx = 3 + 10 * f, ang = (f - 0.5f) * 1.6f;
                float ca = (float) Math.cos(ang), sa = (float) Math.sin(ang);
                for (int i = -3; i <= 3; i++) { float bx = jx + i * ca, by = jy + i * sa; px(Math.round(bx), Math.round(by), i > 1 ? 0x2E8FD8 : 0x7FD4FF); px(Math.round(bx), Math.round(by) + 1, 0x4FA8E0); }
                px(Math.round(jx + 3 * ca), Math.round(jy + 3 * sa) - 1, 0x10202E);
                px(Math.round(jx - 4 * ca), Math.round(jy - 4 * sa) - 1, 0x2E8FD8); px(Math.round(jx - 4 * ca), Math.round(jy - 4 * sa) + 1, 0x2E8FD8);
                outline();
                for (int i = 0; i < 4; i++) { float u = (f * 2 + i * 0.25f) % 1f; pxa(Math.round(jx - 1 + (i - 1.5f) * 2 * u), Math.round(10 - u * 3 + u * u * 4), 0xFFFFFF, (1 - u) * 0.9f); }
            }
            case "grp_lava" -> {                                                             // Lava: a bubbling magma pool in a basalt rim, a fire eel arching out of it
                rect(0, 10, 16, 6, 0x2A2026); rect(1, 10, 14, 1, 0x4A3A40);
                for (int x = 2; x < 14; x++) for (int y = 11; y < 15; y++) px(x, y, mix(0xFFD060, 0xC8300A, (y - 11) / 4f * 0.7f + 0.3f * (0.5f + 0.5f * sn(f * 2 + x * 0.6f + y))));
                float u = (f * 1.0f) % 1f; int ne = 9;
                for (int i = 0; i < ne; i++) { float v = i / (float) (ne - 1), a2 = 3.1416f * (v * 0.9f + 0.05f), x = 4 + 8 * v, y = 11 - (float) Math.sin(a2) * 8 * (float) Math.sin(u * 3.1416f);
                    px(Math.round(x), Math.round(y), i == ne - 1 ? 0xFFE080 : mix(0xFF7A1A, 0xFFB040, v)); px(Math.round(x), Math.round(y) + 1, 0xA02A08); }
                for (int i = 0; i < 3; i++) { float b = (f * 1.7f + i * 0.33f) % 1f; pxa(3 + i * 4, Math.round(10 - b * 6), i % 2 == 0 ? 0xFFB030 : 0xFF5A20, 1 - b); }
                outline();
                glow(8, 12, 8, 0xFF6A1A, 0.18f + 0.1f * t);
            }
            case "grp_spooky" -> {                                                           // Spooky: a carved pumpkin with a flickering face, a bat flitting over it
                ball(8, 10, 5f, 0xE8801A); rect(4, 7, 8, 1, 0xF0A040); px(5, 10, 0xC86010); px(11, 10, 0xC86010);
                rect(7, 4, 2, 2, 0x4A7A2A);
                int fc = mix(0xFFD040, 0xFFF4A0, t); rect(5, 9, 2, 1, fc); rect(9, 9, 2, 1, fc); rect(6, 12, 4, 1, fc); px(5, 11, fc); px(10, 11, fc);
                float bx = 8 + (float) Math.cos(f * 6.2832f) * 5, byy = 2 + (float) Math.sin(f * 12.566f) * 1; int w = (int) (f * 8) % 2;
                px(Math.round(bx), Math.round(byy), 0x7A5AA8); px(Math.round(bx) - 1, Math.round(byy) - w, 0x7A5AA8); px(Math.round(bx) + 1, Math.round(byy) - w, 0x7A5AA8);
                outline();
                glow(8, 10, 6, 0xFF9A2E, 0.14f + 0.12f * t);
            }
            case "grp_winter" -> {                                                           // Winter: a hole in the ice, a fishing line in it, snow falling
                for (int y = 9; y < 15; y++) { int hw = Math.round(7f * (float) Math.sqrt(Math.max(0, 1 - ((y - 11.5f) / 3.5f) * ((y - 11.5f) / 3.5f)))); for (int x = 8 - hw; x < 8 + hw; x++) px(x, y, y == 9 ? 0xFFFFFF : 0xCFEFFF); }
                rect(5, 11, 6, 2, 0x0A3A5A); rect(6, 10, 4, 1, 0x1A5A7A); px(5, 11, 0x5AA8D0); px(10, 12, 0x5AA8D0);
                ln(12, 0, 8, 11, 0xD8E0EA);
                ln(2, 12, 4, 13, 0x8ACCE8); ln(13, 11, 11, 13, 0x8ACCE8);
                for (int i = 0; i < 6; i++) { float u = (f * 0.7f + i / 6f) % 1f; pxa(1 + (i * 5) % 14, Math.round(u * 9), 0xFFFFFF, (float) Math.sin(u * Math.PI)); }
                outline();
                glint(8, 11, 1, 0x9FE4FF, t);
            }
            case "grp_crops" -> {                                                            // Rare Crops: a carrot, a potato and a wheat ear, the rare golden carrot glowing in the middle
                tri(1, 6, 4, 6, 3, 14, 0xFF9A2A); ln(2, 3, 2, 6, 0x4AA83A); ln(3, 3, 4, 6, 0x5AC84A);                       // a carrot
                ball(13, 11, 2.4f, 0xC8A060); px(12, 10, 0x8A6A3A);                                                        // a potato
                ln(13, 2, 13, 8, 0xC8A040); px(12, 3, 0xE8C860); px(14, 4, 0xE8C860); px(12, 5, 0xE8C860); px(14, 6, 0xE8C860);   // wheat
                int gold = mix(0xF2B81A, 0xFFE87A, t);
                tri(6, 5, 10, 5, 8, 15, gold); ln(7, 2, 7, 5, 0x7AD84A); ln(8, 1, 8, 5, 0x8AE85A); ln(9, 2, 9, 5, 0x7AD84A);  // the golden carrot
                px(7, 7, 0xFFF6C0); px(8, 10, 0xC88A10);
                outline();
                glow(8, 9, 6, 0xFFD84A, 0.15f + 0.15f * t);
                glint(7, 7, 1, 0xFFFFFF, t);
            }
            case "grp_feast" -> {                                                            // Harvest Feast: a fat pumpkin on a plate, steam curling up
                glow(8, 9, 8, 0xFFA63A, 0.10f + 0.08f * t);
                rect(1, 13, 14, 1, 0xE8E4D8); rect(2, 14, 12, 1, 0xB8B4A8); rect(3, 12, 10, 1, 0xC8C4B8);
                for (int y = 5; y < 13; y++) {
                    float dy = (y - 8.5f) / 4f; int hw = Math.round((float) Math.sqrt(Math.max(0, 1 - dy * dy)) * 6.2f);
                    for (int x = 8 - hw; x <= 7 + hw; x++) px(x, y, x < 6 ? 0xE8782A : x < 10 ? 0xFF9A3A : 0xD0621E);
                }
                for (int y = 5; y < 13; y++) { px(5, y, 0xC85A1A); px(10, y, 0xC85A1A); }
                px(8, 5, 0xB04A14); px(8, 12, 0xB04A14); rect(7, 3, 2, 2, 0x5E9A36); px(7, 3, 0x7CCB4F); px(9, 4, 0x3E6A24);
                px(6, 6, 0xFFC878); px(7, 6, 0xFFC878);
                float d = f;                                                                       // two wisps of steam rise and fade
                for (int k = 0; k < 2; k++) { float e = (d + k * 0.5f) % 1f; pxa(11 + Math.round(sn(e * 2 + k)), 2 - Math.round(e * 2) + 2, 0xFFFFFF, 0.6f * (1f - e)); pxa(3 + Math.round(sn(e * 2 + k)), 3 - Math.round(e * 2) + 1, 0xFFFFFF, 0.5f * (1f - e)); }
                outline();
            }
            case "grp_pests" -> {                                                            // Pest Drops: a red shelled beetle seen from above, legs scuttling, feelers twitching
                glow(8, 9, 7, 0xFF5A3A, 0.06f + 0.06f * t);
                int lg = Math.round(sn(f * 2) * 0.9f);
                for (int k = 0; k < 3; k++) { int y = 6 + k * 3, o = (k % 2 == 0) ? lg : -lg;
                    ln(4.5f, y, 1, y + 1 + o, 0x2A1810); ln(11.5f, y, 15, y + 1 - o, 0x2A1810); px(1, y + 1 + o, 0x5A3A2A); px(14, y + 1 - o, 0x5A3A2A); }
                for (int y = 5; y < 15; y++) {
                    float dy = (y - 10f) / 5.2f; int hw = Math.round((float) Math.sqrt(Math.max(0, 1 - dy * dy)) * 4.6f);
                    for (int x = 8 - hw; x <= 7 + hw; x++) px(x, y, x < 8 ? (y < 8 ? 0xE0503A : 0xC03A2A) : (y < 8 ? 0xC03A2A : 0x9A2A20));
                }
                rect(7, 5, 2, 10, 0x2A1410); px(5, 8, 0x2A1410); px(6, 8, 0x2A1410); px(10, 11, 0x2A1410); px(9, 11, 0x2A1410); px(5, 12, 0x2A1410); px(10, 7, 0x2A1410);
                px(5, 7, 0xFF9A7A); px(6, 6, 0xFF9A7A);                                                                                 // a shine on the shell
                rect(6, 2, 4, 3, 0x2A1410); px(6, 3, 0xFFE27A); px(9, 3, 0xFFE27A);
                int an = Math.round(sn(f * 3)); ln(6, 2, 4 + an, 0, 0x2A1810); ln(9, 2, 11 - an, 0, 0x2A1810);
                outline();
                glint(5, 5, 1, 0xFFFFFF, 0.2f + 0.8f * t);
            }
            case "grp_exp" -> {                                                              // the Experimentation Table: the enchanting table's book, a page flipping, runes rising
                float u = f;                                                                           // the flip: a page swings from right to left
                float fl = (float) Math.cos(u * 3.1416f);                                              // 1 .. -1
                bev(1, 10, 14, 4, 0x8A1F2E);                                                            // cover
                quad(2, 6, 8, 7, 8, 11, 2, 10, 0xF2E8C8);                                               // left page
                quad(8, 7, 14, 6, 14, 10, 8, 11, 0xFFF6DC);                                             // right page
                ln(3, 8, 7, 8.8f, 0xB8A878); ln(9, 8.8f, 13, 8, 0xB8A878);
                float px0 = 8 + fl * 6;                                                                 // the turning page's edge
                quad(8, 7, px0, 6 - Math.abs(fl) * 2.2f, px0, 10 - Math.abs(fl) * 2.2f, 8, 11, fl > 0 ? 0xFFFFF0 : 0xE8DCB4);
                rect(7, 6, 2, 6, 0x5A1018);                                                             // spine
                outline();
                for (int i = 0; i < 3; i++) { float v = (f + i / 3f) % 1f; int rx = 4 + i * 4 + Math.round((float) Math.sin(v * 6 + i) * 1.5f), ry = Math.round(5 - v * 5);
                    pxa(rx, ry, 0xC9A0FF, 1f - v * 0.6f); pxa(rx + 1, ry, 0xFFFFFF, (1f - v) * 0.7f); }
            }
            case "shovel" -> {                                                               // a digging shovel (the blade takes the spade's colour): it dips into the ground and flicks up a lump of dirt
                int bl = tint != 0 ? tint : 0xC9D1E0;
                float dip = t * 2f;
                for (int x = 1; x < 15; x++) px(x, 14, 0x5A3E22);
                for (int x = 2; x < 14; x += 3) px(x, 13, 0x7A5A38);
                ln(12, 1 + dip, 6.5f, 9 + dip, 0x6B4423); ln(13, 1 + dip, 7.5f, 9 + dip, 0x9A6A38);
                rect(11, 1 + (int) dip, 4, 1, 0x3A2410); px(11, 2 + (int) dip, 0x3A2410); px(14, 2 + (int) dip, 0x3A2410);                       // the D grip
                quad(6, 9 + dip, 8.5f, 9.8f + dip, 6.8f, 14, 3.2f, 13, bl);
                ln(6, 9 + dip, 3.3f, 13, lite(bl, 0.5f)); px((int) 6, (int) (14), dark(bl, 0.4f));
                ln(5.5f, 9.5f + dip, 8, 10 + dip, dark(bl, 0.35f));
                float u = f < 0.5f ? 0f : (f - 0.5f) * 2f;
                if (u > 0f) { px(Math.round(8 + u * 4), Math.round(12 - 5 * (float) Math.sin(u * 3.14f)), 0x9A7A52); px(Math.round(9 + u * 5), Math.round(12 - 4 * (float) Math.sin(u * 3.14f)), 0x6B4A2B); }
                outline();
            }
            case "farm" -> {                                                                 // the farm: a red barn and a silo over a green field of wheat, the weathervane turning
                for (int y = 11; y < 16; y++) rect(0, y, 16, 1, y == 11 ? 0x7CCB4F : y < 14 ? 0x4E9A32 : 0x3A7A26);                   // the field, lit at the far edge
                for (int i = 0; i < 4; i++) { int sw = Math.round(sn(f + i * 0.25f) * 0.7f); px(1 + i * 4 + sw, 12, 0xE8C64A); px(1 + i * 4, 13, 0xC8A438); px(1 + i * 4, 14, 0x6A9A32); px(2 + i * 4 + sw, 12, 0xF4DA70); }
                rect(2, 5, 8, 6, 0xC83A32); rect(2, 5, 8, 1, 0xE86A5A); rect(2, 10, 8, 1, 0x8A2420);
                tri(1, 6, 6, 1, 11, 6, 0x8A2420); tri(2, 6, 6, 2, 10, 6, 0xB02E2A);                                                   // the gambrel roof
                rect(4, 7, 4, 4, 0xF4E8D8); ln(4, 7, 7.5f, 10.5f, 0xC83A32); ln(7.5f, 7, 4, 10.5f, 0xC83A32);                          // the doors with their white X
                rect(11, 4, 3, 7, 0xB8C0CC); rect(11, 4, 1, 7, 0xE0E6EE); tri(10.5f, 4, 12.5f, 2, 14.5f, 4, 0x8A93A8);                // the silo
                float an = f * TAU; px(6, 0, 0x3A2A20); px(Math.round(6 + (float) Math.cos(an) * 1.4f), 0, 0xFFD25A); px(6, 1, 0x3A2A20);
                outline();
                glint(13 - Math.round(f * 3), 2, 1, 0xFFFFFF, 0.25f + 0.75f * t);
            }
            case "grp_bounty" -> {                                                           // Harvest Bounty: a green tractor mowing a field, a mown strip behind it, grass flying
                for (int y = 9; y < 16; y++) rect(0, y, 16, 1, y == 9 ? 0x9AE05A : y < 13 ? 0x5EAE3A : 0x468A2C);
                int off = Math.round(f);                                                                                               // it creeps along, the stripes slide under it
                for (int x = 0; x < 16; x++) rect(x, 13, 1, 2, (x + Math.round(f * 4)) % 4 < 2 ? 0xB8E064 : 0x8CCB48);                 // the mown stripes
                rect(4 + off, 5, 7, 4, 0x2E8A3A); rect(4 + off, 5, 7, 1, 0x6ACB5A); rect(9 + off, 6, 5, 3, 0x2E8A3A); rect(9 + off, 6, 5, 1, 0x6ACB5A);   // body and bonnet
                rect(3 + off, 1, 5, 4, 0x3AA848); rect(4 + off, 2, 3, 3, 0xBFE8FF); rect(3 + off, 0, 6, 1, 0x1E6A2A);                  // the cab
                rect(13 + off, 3, 1, 3, 0x3A3A44); pxa(13 + off, 2 - Math.round(f * 2), 0xDADADA, 1f - f);
                for (int w = 0; w < 2; w++) { int wr = w == 0 ? 3 : 2, wx = (w == 0 ? 6 : 12) + off, wy = 13 - wr + 1;
                    for (int y = -wr; y <= wr; y++) for (int x = -wr; x <= wr; x++) if (x * x + y * y <= wr * wr + 1) px(wx + x, wy + y, x * x + y * y <= (wr - 1) * (wr - 1) ? 0x4A4A56 : 0x1A1A20);
                    px(wx, wy, 0xE8B83A); int a2 = Math.round(f * 8) % 4; px(wx + (a2 == 0 ? 1 : a2 == 2 ? -1 : 0), wy + (a2 == 1 ? 1 : a2 == 3 ? -1 : 0), 0x8A8A96); }
                rect(off, 12, 3, 2, 0xB8B8C8); px(off, 12, 0xE8E8F4);                                                                   // the mower deck
                for (int i = 0; i < 3; i++) { float u = (f + i / 3f) % 1f; pxa(Math.round(off + 1 - u * 3), Math.round(11 - u * 4 + i), 0x9AE05A, 1f - u); }
                outline();
            }
            case "raredrops" -> {                                                            // Rare Drops: an open enchanted book, a violet gem rising out of it on a beam of light
                int hov = Math.round(1.2f * s);
                quad(0.5f, 9, 8, 11, 8, 15.5f, 0.5f, 13.5f, 0x5A3A9E); quad(8, 11, 15.5f, 9, 15.5f, 13.5f, 8, 15.5f, 0x4A2E86);   // the covers
                quad(1.5f, 8, 8, 10, 8, 14, 1.5f, 12, 0xF3EAD8); quad(8, 10, 14.5f, 8, 14.5f, 12, 8, 14, 0xE0D4B8);            // the pages
                ln(3, 10, 7, 11.2f, 0xB8A888); ln(3, 11.3f, 7, 12.5f, 0xB8A888); ln(9, 11.2f, 13, 10, 0xB8A888); ln(9, 12.5f, 13, 11.3f, 0xB8A888);
                for (int y = 2; y < 10; y++) { pxa(7, y, 0xFFE9A8, 0.15f + 0.2f * t); pxa(8, y, 0xFFE9A8, 0.3f + 0.3f * t); }
                float[] gx = {8, 11.5f, 8, 4.5f}, gy = {0.5f + hov, 4f + hov, 8.5f + hov, 4f + hov};
                poly(gx, gy, 0xB56BFF);
                outline();
                glint(7, 2 + hov, 1, 0xFFFFFF, 0.5f + 0.5f * t);
                glow(8, 4, 6, 0xB56BFF, 0.15f + 0.1f * t);
            }
            case "grp_mines" -> {                                                            // Mines: a ghost of the Dwarven Mines drifting over a mithril block, its eyes glowing
                bev(3, 11, 10, 5, 0x4A4C60); rect(5, 12, 2, 1, 0x4FCFBE); rect(9, 13, 3, 1, 0x4FCFBE); px(7, 14, 0x9AF4E8);
                int b = Math.round(sn(f) * 1.2f);
                for (int y = 2; y < 11; y++) for (int x = 4; x < 12; x++) {                                 // the ghost: a rounded hood, a ragged hem
                    float dx = (x + 0.5f - 8) / 4f, dy = (y + 0.5f - 6) / 4.5f; boolean in = y < 6 ? dx * dx + dy * dy < 1f : (y < 9 || (x + y + (int) (f * 4)) % 2 == 0);
                    if (in) pxa(x, y + b, y < 4 ? 0xF2F6FF : mix(0xD8E4FF, 0x8AA4D8, (y - 2) / 9f), 0.85f);
                }
                rect(6, 5 + b, 1, 2, 0x10142A); rect(9, 5 + b, 1, 2, 0x10142A); px(6, 5 + b, mix(0x4FCFBE, 0xB8FFF4, t)); px(9, 5 + b, mix(0x4FCFBE, 0xB8FFF4, t));
                rect(7, 8 + b, 2, 1, 0x2A3050);
                outline();
                glow(8, 6 + b, 6, 0x9AC8FF, 0.1f + 0.1f * t);
            }
            case "grp_nucleus" -> {                                                          // the Crystal Nucleus: five gem crystals circling a glowing core
                int[] gems = {0x5ADC8A, 0xFFB030, 0xB060FF, 0x3A8AFF, 0xFFE040};
                glow(8, 8, 7, 0x9AE6FF, 0.16f + 0.12f * t);
                ball(8, 8, 2.6f, mix(0x6A5AFF, 0xE8F4FF, t));
                for (int i = 0; i < 5; i++) {
                    float an = (f + i / 5f) * TAU; int x = Math.round(8 + (float) Math.cos(an) * 5.6f), y = Math.round(8 + (float) Math.sin(an) * 5.6f);
                    px(x, y - 1, lite(gems[i], 0.4f)); px(x, y, gems[i]); px(x, y + 1, dark(gems[i], 0.3f));
                }
                outline();
                glint(7, 7, 1, 0xFFFFFF, t);
            }
            case "grp_corpse" -> {                                                           // a Frozen Corpse: a dwarf frozen in ice, and a golden key turning in its lock
                bev(2, 1, 12, 14, 0x6AB8E0); rect(3, 2, 10, 12, mix(0x8ACCEC, 0xA8E0F8, t * 0.5f));
                rect(5, 3, 6, 2, 0x6A6E7A); rect(5, 5, 6, 3, 0xD8C0A0); px(6, 6, 0x2A2A34); px(9, 6, 0x2A2A34);   // helmet and face
                rect(5, 8, 6, 3, 0xC8C0B0); rect(6, 11, 4, 2, 0x5A6A7A);                                    // the beard and body
                ln(3, 3, 5, 1, 0xF2FCFF); px(12, 13, 0xF2FCFF); px(11, 3, 0xF2FCFF);
                outline();
                float kt = Math.min(1f, f * 2f); int kx = 10, ky = 11;                                       // the key: slides in, then turns
                rect(kx - 2 + Math.round((1 - kt) * 3), ky, 4, 1, 0xFFC857); rect(kx + 2 + Math.round((1 - kt) * 3), ky - 1, 3, 3, 0xFFC857); px(kx + 3 + Math.round((1 - kt) * 3), ky, 0x7A5A10);
                if (f > 0.5f) px(kx - 1, ky + 1, 0xFFC857);
                glint(13, 11, 1, 0xFFFFFF, f > 0.5f ? tri((f - 0.5f) * 2) : 0f);
                glow(8, 8, 8, 0x8FD8FF, 0.1f + 0.08f * t);
            }
            case "grp_fiesta" -> {                                                           // the Mining Fiesta: a gold nugget bouncing on an ore block, confetti falling
                bev(3, 9, 10, 6, 0x5E5A68); rect(5, 11, 2, 1, 0xFFC857); rect(9, 12, 2, 1, 0x55D8FF);
                int b = Math.round(-2.5f * 4f * f * (1f - f));
                ball(8, 6 + b, 2.6f, mix(0xE8A020, 0xFFE070, t)); px(7, 5 + b, 0xFFF6C0);
                int[] cf = {0xFF5A8A, 0x55D8FF, 0x7BE05A, 0xFFC857};
                for (int i = 0; i < 4; i++) { float u = (f + i / 4f) % 1f; px(2 + i * 4, Math.round(u * 8), cf[i]); }
                outline();
                glint(9, 4 + b, 1, 0xFFFFFF, t);
            }
            case "cat_table" -> {                                                            // Table & Dyes: the Experimentation Table's open book, a single drop of dye falling onto the page and spreading
                bev(1, 11, 14, 4, 0x6A2A3A); rect(2, 15, 2, 1, 0x3A1420); rect(12, 15, 2, 1, 0x3A1420);
                quad(1, 8, 8, 9, 8, 11, 1, 10, 0xF2EAD8); quad(8, 9, 15, 8, 15, 10, 8, 11, 0xE8DCC0); ln(8, 9, 8, 11, 0x8A5A3A);
                ln(3, 9, 6, 9, 0xB8A890); ln(10, 9, 13, 9, 0xB8A890);
                float u = f; int dy = Math.round(1 + u * 6);
                if (u < 0.75f) { ball(11.5f, dy, 1.3f, 0xFF4FA8); }                          // the drop falls...
                else { float k = (u - 0.75f) / 0.25f; for (int q = -1; q <= 1; q++) pxa(11 + q * Math.round(1 + k), 9, 0xFF4FA8, 1f - k * 0.5f); px(11, 9, 0xFF4FA8); }   // ...and spreads on the page
                for (int q = 0; q < 2; q++) { float v = (f + q * 0.5f) % 1f; pxa(3 + q * 2, Math.round(7 - v * 6), 0xC77DFF, 1f - v); }   // a rune rising off the left page
                outline();
            }
            case "boss_voidgloom", "voidgloom" -> {                                          // Voidgloom Seraph: an enderman's head, its purple eyes glowing, ender particles drifting round it
                bev(2, 2, 12, 12, 0x161020);
                int[] spot = {3, 5, 9, 4, 6, 10, 11, 8, 4, 11, 8, 3};                                 // the faint mottling of the enderman's skin
                for (int q = 0; q < spot.length; q += 2) px(spot[q], spot[q + 1], 0x241A30);
                int eye = mix(0xC040FF, 0xF0A0FF, t);
                rect(3, 7, 4, 2, 0xE070FF); rect(9, 7, 4, 2, 0xE070FF); px(4, 7, eye); px(5, 7, eye); px(10, 7, eye); px(11, 7, eye);
                px(4, 8, 0xB040E0); px(11, 8, 0xB040E0);
                for (int q = 0; q < 4; q++) { float v = (f + q / 4f) % 1f; pxa(q % 2 == 0 ? 1 : 14, 13 - Math.round(v * 11), 0xC86AFF, 1f - v); }   // ender particles rising at the sides
                outline();
                glow(4.5f, 7.5f, 2.5f, 0xE070FF, 0.2f + 0.15f * t); glow(10.5f, 7.5f, 2.5f, 0xE070FF, 0.2f + 0.15f * t);
            }
            case "move" -> {                                                                                 // Position: a little astronaut drifting, a crosshair round them
                for (int i = 2; i < 14; i++) { if (i > 5 && i < 10) continue; pxa(i, 8, 0x7672AE, 0.8f); pxa(8, i, 0x7672AE, 0.8f); }
                int dx = Math.round(s), dy = Math.round((float) Math.cos(f * TAU));
                ball(8 + dx, 7 + dy, 2.6f, 0xE8ECFF); rect(7 + dx, 6 + dy, 3, 2, 0x5A8AE8); px(7 + dx, 6 + dy, 0xBFE8FF);   // helmet, visor
                rect(7 + dx, 9 + dy, 3, 3, 0xE8ECFF); px(6 + dx, 10 + dy, 0xC8CCE8); px(10 + dx, 10 + dy, 0xC8CCE8);
                outline();
            }
            case "lyr_burst" -> {                                                            // Star bursts: a word on a line, little stars bursting off it
                rect(2, 10, 12, 2, 0xFFC8A0); rect(2, 12, 12, 1, dark(0xFFC8A0, 0.4f));
                for (int q = 0; q < 5; q++) { float v = (f + q / 5f) % 1f; double an = -Math.PI / 2 + (q - 2) * 0.55; int x = 8 + (int) Math.round(Math.cos(an) * v * 7), y = 9 + (int) Math.round(Math.sin(an) * v * 8);
                    int sc = q % 2 == 0 ? 0xFFE38A : 0xFF9AD8; pxa(x, y, sc, 1f - v); if (v < 0.5f) { pxa(x - 1, y, sc, 0.5f - v); pxa(x + 1, y, sc, 0.5f - v); pxa(x, y - 1, sc, 0.5f - v); } }
                outline();
            }
            case "lyr_intro" -> {                                                            // Song intro: a title card sliding up under a spotlight
                tri(8, 0, 2, 15, 14, 15, 0x2A2440); for (int y = 1; y < 15; y++) pxa(8, y, 0xFFF4D8, 0.15f);
                int up = Math.round((1f - Math.min(1f, f * 2f)) * 4);
                bev(2, 7 + up, 12, 5, 0x3A2A5A); rect(4, 9 + up, 8, 1, 0xFFE9B8); rect(5, 11 + up, 6, 1, 0xB8A8D8);
                outline();
                glint(12, 7 + up, 1, 0xFFFFFF, t);
            }
            case "lyr_dock" -> {                                                             // Sung line: a little Now Playing card with a lyric strip sliding out under it
                int dn = Math.round(Math.min(1f, f * 2f) * 3);
                bev(3, 8 + dn, 10, 4, 0x1E5A3A); rect(5, 9 + dn, 4, 1, 0x7CFFB0); rect(9, 9 + dn, 2, 1, 0x8A9AA8);
                bev(1, 2, 14, 7, 0x2A2E44); bev(2, 3, 5, 5, 0x1ED760); rect(3, 4, 3, 3, 0x0C3A20); rect(8, 4, 6, 1, 0xFFFFFF); rect(8, 6, 4, 1, 0x9AA4C4);
                outline();
                glint(13, 9 + dn, 1, 0xFFFFFF, t);
            }
            case "visuals" -> {                                                              // Visuals: an eye-piece view: a ringed planet in a round lens, a star glinting
                ball(8, 8, 7f, 0x1A1848); ball(8, 8, 6f, 0x0B0C26);
                ball(8, 8, 3f, 0x9B7BFF); ball(7, 7, 1.5f, 0xD8C8FF);
                ln(2, 10, 14, 6, 0xFF7AD9);
                float tw = sn(f * 2) * 0.5f + 0.5f; pxa(12, 3, 0xFFFFFF, tw); pxa(4, 4, 0xFFFFFF, 1 - tw); pxa(11, 12, 0xC8B4FF, tw);
                outline();
                glint(5, 5, 1, 0xFFFFFF, t);
            }
            case "cosmos" -> {                                                               // Cosmos: a crescent moon, stars, a shooting star
                ball(6, 9, 4.5f, 0xE8ECFF); ball(8, 8, 4f, 0x00000000);
                float u = ((f % 1f) + 1f) % 1f; int sx = Math.round(4 + u * 10), sy = Math.round(2 + u * 3);
                ln(sx - 3, sy - 1, sx, sy, 0xC8B4FF); px(sx, sy, 0xFFFFFF);
                pxa(13, 11, 0xFFFFFF, sn(f * 3) * 0.5f + 0.5f); pxa(11, 14, 0xFF7AD9, 0.8f); pxa(2, 3, 0xFFFFFF, 0.7f);
                outline();
            }
            case "cos_shoot" -> {                                                            // Shooting stars: a meteor with its tail
                float u = ((f % 1f) + 1f) % 1f; int hx = Math.round(5 + u * 7), hy = Math.round(5 + u * 5);
                for (int i = 1; i < 6; i++) pxa(hx - i, hy - i, i < 3 ? 0xFFFFFF : 0xC8B4FF, 1f - i / 6f);
                rect(hx, hy, 2, 2, 0xFFFFFF); pxa(3, 12, 0xFFFFFF, 0.7f); pxa(13, 3, 0xFFFFFF, 0.6f);
            }
            case "cos_night" -> {                                                            // Starry corners: a corner of the screen with stars
                rect(1, 1, 14, 1, 0x4A4890); rect(1, 1, 1, 14, 0x4A4890);
                for (int i = 0; i < 5; i++) { float tw = sn(f * 2 + i) * 0.5f + 0.5f; pxa(3 + i * 2, 3 + (i * 3) % 5, 0xFFFFFF, tw); }
                pxa(4, 9, 0xD8C8FF, 0.8f); pxa(8, 8, 0xFFFFFF, 0.6f);
            }
            case "cos_aurora" -> {                                                           // Aurora: waving ribbons of light
                for (int x = 1; x < 15; x++) { int y = 6 + Math.round(sn(x * 0.5f + f * 2) * 2); rect(x, y, 1, 3, x < 8 ? 0x9B7BFF : 0x5AF0B0); pxa(x, y - 1, 0xFFFFFF, 0.3f); }
                pxa(3, 13, 0xFFFFFF, 0.7f); pxa(12, 14, 0xFFFFFF, 0.6f);
            }
            case "cos_meteor" -> {                                                           // Meteor on RNG: a big burning meteor
                ball(10, 10, 3f, 0xFFE8A0); ball(10, 10, 1.6f, 0xFFFFFF);
                ln(1, 3, 8, 8, 0xFF9A4A); ln(2, 2, 9, 8, 0xFF7AD9); ln(3, 1, 9, 7, 0xFFC870);
                outline();
            }
            case "cos_clock" -> {                                                            // Celestial clock: a half dial, the sun on its arc
                rect(1, 11, 14, 1, 0x6A68B0);
                for (int i = 0; i <= 8; i++) { double an = Math.PI + Math.PI * i / 8.0; px(Math.round(8 + (float) Math.cos(an) * 6), Math.round(11 + (float) Math.sin(an) * 6), 0x9B7BFF); }
                float u = ((f % 1f) + 1f) % 1f; double an = Math.PI + Math.PI * u; ball(8 + (float) Math.cos(an) * 6, 11 + (float) Math.sin(an) * 6, 1.6f, 0xFFE07A);
            }
            case "sty_pop" -> {                                                              // Starlight: a star flaring, opening into a word
                float fl = sn(f) * 0.5f + 0.5f;
                rect(3, 11, 10, 1, 0xEEF2FF); rect(4, 13, 7, 1, 0x8A94C0);
                int r = 1 + Math.round(fl * 2);
                rect(8 - r * 2, 5, r * 4 + 1, 1, 0xC8D4FF); rect(8, 5 - r * 2, 1, r * 4 + 1, 0xC8D4FF);
                rect(7, 4, 3, 3, 0xFFFFFF); pxa(5, 2, 0xFFFFFF, 1 - fl); pxa(12, 8, 0xFFFFFF, fl);
                glint(8, 5, 1, 0xFFFFFF, t);
            }
            case "sty_kara" -> {                                                             // Constellation: stars over words, joined by lines
                int[][] st = {{2, 6}, {6, 3}, {10, 5}, {13, 2}};
                for (int i = 1; i < st.length; i++) ln(st[i - 1][0], st[i - 1][1], st[i][0], st[i][1], 0x7A6AC8);
                for (int i = 0; i < st.length; i++) { float tw = sn(f * 2 + i) * 0.5f + 0.5f; rect(st[i][0], st[i][1], 1, 1, 0xFFFFFF); pxa(st[i][0] - 1, st[i][1], 0xB4A0FF, tw); pxa(st[i][0] + 1, st[i][1], 0xB4A0FF, tw); }
                rect(1, 10, 3, 1, 0xD8E2FF); rect(5, 10, 3, 1, 0xD8E2FF); rect(9, 10, 3, 1, 0xD8E2FF); rect(13, 10, 2, 1, 0xD8E2FF);
                rect(2, 12, 11, 1, 0x4A4A7A);
            }
            case "sty_scroll" -> {                                                           // Drift: lines rising through the dark, a star rising with them
                int off = Math.round((((f % 1f) + 1f) % 1f) * 3);
                for (int i = 0; i < 5; i++) { int y = 3 + i * 3 - off; if (y < 1 || y > 14) continue; boolean mid = i == 2; rect(mid ? 3 : 4, y, mid ? 10 : 6 + (i % 2) * 2, 1, mid ? 0xEEF2FF : 0x5A6A9A); }
                pxa(13, 12 - off * 3, 0xFFFFFF, 0.9f); pxa(2, 9 - off * 2, 0x8AB4FF, 0.8f);
            }
            case "sty_sung" -> {                                                             // Sung line: a Now Playing card with one lyric strip docked under it
                int dn = Math.round((sn(f) * 0.5f + 0.5f) * 1);
                bev(1, 2, 14, 7, 0x1E2234); bev(2, 3, 5, 5, 0x4FC3FF); rect(3, 4, 3, 3, 0x0A2A3A); rect(8, 4, 6, 1, 0xFFFFFF); rect(8, 6, 4, 1, 0x8A94B0);
                bev(2, 10 + dn, 12, 4, 0x14304A); rect(3, 11 + dn, 1, 2, 0xA8E4FF); int w = Math.round((((f % 1f) + 1f) % 1f) * 8); rect(5, 11 + dn, Math.max(1, w), 1, 0xA8E4FF); rect(5 + w, 11 + dn, 8 - w, 1, 0x4A5068);
                outline();
                glint(13, 3, 1, 0xFFFFFF, t);
            }
            case "lyr_prev" -> {                                                             // Previous line: a faint line above, a bright one below, an arrow pointing back up
                rect(3, 4, 9, 1, 0x6A7088); rect(3, 6, 6, 1, 0x4A5068);
                rect(3, 10, 10, 1, 0xFFFFFF); rect(3, 12, 7, 1, 0xC8CCD8);
                ln(13, 8, 13, 3, 0xFFC8A0); px(12, 4, 0xFFC8A0); px(14, 4, 0xFFC8A0);
                outline();
            }
            case "lyr_timing" -> {                                                           // Timing: a metronome, its arm ticking side to side
                quad(4, 15, 12, 15, 10, 2, 6, 2, 0x8A5A3A); quad(5, 14, 11, 14, 9.5f, 4, 6.5f, 4, 0x6A3A1E);
                float an = sn(f) * 0.55f; float ax = 8 + (float) Math.sin(an) * 9, ay = 13 - (float) Math.cos(an) * 9;
                ln(8, 13, ax, ay, 0xE8D8B8); rect(Math.round(8 + (float) Math.sin(an) * 5) - 1, Math.round(13 - (float) Math.cos(an) * 5), 2, 2, 0xFFC94A);
                bev(5, 12, 6, 3, 0x4A2A14);
                outline();
            }
            case "amb" -> {                                                                  // Ambience: a soft glow ring round a disc, light washing out
                glow(8, 8, 8, 0xB48CFF, 0.25f + 0.15f * t); ball(8, 8, 5.5f, 0x2A2238); ball(8, 8, 2, 0xB48CFF); px(8, 8, 0x1A1424);
                for (int q = 0; q < 8; q++) { double an = q * Math.PI / 4 + f * 0.8; pxa(8 + (int) Math.round(Math.cos(an) * 7), 8 + (int) Math.round(Math.sin(an) * 7), 0xE8D8FF, 0.4f + 0.4f * tri((f + q / 8f) % 1f)); }
                outline();
            }
            case "amb_glow" -> {                                                             // Nebula: a drifting cloud with stars in it
                glow(6 + sn(f) * 1.5f, 7, 6, 0x9B7BFF, 0.45f); glow(10 - sn(f) * 1.5f, 9, 5, 0xFF7AD9, 0.4f);
                pxa(4, 4, 0xFFFFFF, 0.6f + 0.4f * t); pxa(11, 5, 0xFFFFFF, 0.9f); pxa(8, 11, 0xFFFFFF, 1f - 0.4f * t); pxa(13, 12, 0xFFFFFF, 0.6f);
                rect(7, 7, 2, 1, 0xFFFFFF); rect(7, 6, 1, 3, 0xFFFFFF);
            }
            case "amb_strength" -> {                                                         // Glow strength: a sun whose rays grow and shrink
                int r = 2 + Math.round(t * 2);
                for (int q = 0; q < 8; q++) { double an = q * Math.PI / 4; ln(8 + (float) Math.cos(an) * 4, 8 + (float) Math.sin(an) * 4, 8 + (float) Math.cos(an) * (4 + r), 8 + (float) Math.sin(an) * (4 + r), 0xFFC94A); }
                ball(8, 8, 3.5f, 0xFFE38A);
                outline();
            }
            case "amb_viz" -> {                                                              // Star equalizer: columns of stars, the top one brightest
                for (int q = 0; q < 5; q++) { int cnt = 1 + Math.round(3 * (0.5f + 0.5f * (float) Math.sin(f * 6.2832f * (q % 2 == 0 ? 2 : 3) + q * 1.3f)));
                    for (int k = 0; k < cnt; k++) { boolean top = k == cnt - 1; int x = 2 + q * 3, y = 14 - k * 3; if (top) { px(x, y, 0xFFFFFF); pxa(x - 1, y, 0xC8B4FF, 0.7f); pxa(x + 1, y, 0xC8B4FF, 0.7f); pxa(x, y - 1, 0xC8B4FF, 0.7f); } else px(x, y, hue(0.75f - q * 0.05f, 0.45f, 1f)); } }
            }
            case "amb_notes" -> {                                                            // Comets: two comets curving up, trailing light
                for (int q = 0; q < 2; q++) { float v = (f + q * 0.5f) % 1f; int hx = 3 + q * 6 + Math.round(v * 4), hy = 13 - Math.round(v * 9);
                    for (int k = 1; k < 5; k++) pxa(hx - k / 2, hy + k, q == 0 ? 0xB48CFF : 0xFF7AD9, 1f - k / 5f);
                    rect(hx, hy, 2, 2, 0xFFFFFF); }
            }
            case "amb_wave" -> {                                                             // Supernova: a bright core and a ring bursting outward
                float u = f % 1f; float r = 1.5f + u * 6.5f;
                for (int q = 0; q < 20; q++) { double an = q * Math.PI / 10; pxa(Math.round(8 + (float) Math.cos(an) * r), Math.round(8 + (float) Math.sin(an) * r), q % 2 == 0 ? 0xFF7AD9 : 0xC8B4FF, 1f - u * 0.7f); }
                glow(8, 8, 3, 0xFFFFFF, 0.5f * (1f - u)); ball(8, 8, 1.5f, 0xFFF4D8);
            }
            case "spot" -> {                                                                 // Spotify page: a pair of headphones, a beat pulsing between them
                for (int x = 3; x < 13; x++) { double a = (x - 8) / 5.0; px(x, (int) Math.round(6 - Math.sqrt(Math.max(0, 1 - a * a)) * 4), 0xD8E8DA); }
                bev(1, 7, 4, 6, 0x1ED760); bev(11, 7, 4, 6, 0x1ED760);
                int b = Math.round(t * 2); for (int q = 0; q < 3; q++) rect(6 + q * 2, 11 - b - (q == 1 ? 1 : 0), 1, 2 + b + (q == 1 ? 1 : 0), 0xEAFFEA);
                outline();
            }
            case "step_web" -> {                                                             // Step: a browser window, its page loading
                bev(1, 2, 14, 12, 0xD8E0EA); rect(1, 2, 14, 3, 0x3A6AD8); px(2, 3, 0xFF6A6A); px(4, 3, 0xFFC94A); px(6, 3, 0x6AE86A);
                int w = Math.round(f * 10); rect(3, 7, w, 1, 0x1ED760); rect(3, 9, 8, 1, 0x9AA4B8); rect(3, 11, 6, 1, 0x9AA4B8);
                outline();
            }
            case "step_app" -> {                                                             // Step: a new app tile with a plus
                bev(2, 2, 12, 12, 0x1ED760); rect(7, 4, 2, 8, 0xFFFFFF); rect(4, 7, 8, 2, 0xFFFFFF);
                outline(); glint(12, 4, 1, 0xFFFFFF, t);
            }
            case "step_link" -> {                                                            // Step: two chain links joining
                int o = Math.round((1f - t) * 1.5f);
                for (int k = 0; k < 2; k++) { int x0 = k == 0 ? 1 + o : 7 - o; rect(x0, 6, 8, 1, 0xB8E8FF); rect(x0, 10, 8, 1, 0xB8E8FF); rect(x0, 6, 1, 5, 0xB8E8FF); rect(x0 + 7, 6, 1, 5, 0xB8E8FF); }
                outline();
            }
            case "step_check" -> {                                                           // Step: a box getting ticked
                bev(2, 2, 12, 12, 0x2A3448); int n2 = Math.round(Math.min(1f, f * 1.6f) * 8);
                int[][] ck = {{4, 8}, {5, 9}, {6, 10}, {7, 9}, {8, 8}, {9, 7}, {10, 6}, {11, 5}};
                for (int q = 0; q < n2; q++) { px(ck[q][0], ck[q][1], 0x1ED760); px(ck[q][0], ck[q][1] + 1, dark(0x1ED760, 0.3f)); }
                outline();
            }
            case "step_key" -> {                                                             // Step: a key card with an ID line copying across
                bev(1, 4, 14, 9, 0xEAF2FF); rect(1, 6, 14, 2, 0x2A3448); rect(3, 10, Math.round(2 + 8 * t), 1, 0x1ED760);
                outline();
            }
            case "step_login" -> {                                                           // Step: a door opening onto green light
                bev(3, 1, 10, 14, 0x4A3A2E); rect(4, 2, 8, 12, 0x1ED760); int w = 8 - Math.round(t * 5); bev(4, 2, w, 12, 0x8A6A4A); px(4 + w - 2, 8, 0xFFE38A);
                outline();
            }
            case "mus_play" -> {                                                             // Play / Pause: a round green button that turns from play to pause and back (one symbol at a time)
                ball(8, 8, 6.6f, 0x0E5A2E); ball(8, 8, 5.8f, 0x1ED760); ball(7, 6, 2.5f, 0x5AF090);
                boolean play = (f % 1f + 1f) % 1f < 0.5f;
                if (play) tri(6, 5, 6, 11, 11, 8, 0xFFFFFF); else { rect(5, 5, 2, 6, 0xFFFFFF); rect(9, 5, 2, 6, 0xFFFFFF); }
                outline();
                glint(5, 4, 1, 0xFFFFFF, t);
            }
            case "mus_next" -> { int o = Math.round(t * 2); bev(1, 1, 14, 14, 0x1A2A20); tri(3 + o, 4, 3 + o, 12, 8 + o, 8, 0x1ED760); tri(8 + o, 4, 8 + o, 12, 13 + o, 8, 0x1ED760); outline(); }
            case "mus_prev" -> { int o = Math.round(t * 2); bev(1, 1, 14, 14, 0x1A2A20); tri(13 - o, 4, 13 - o, 12, 8 - o, 8, 0x1ED760); tri(8 - o, 4, 8 - o, 12, 3 - o, 8, 0x1ED760); outline(); }
            case "src_open_water" -> { waves(f, 0x2A7AD8); ln(3, 1, 12, 9, 0x8A5A2B); ln(12, 9, 12, 10, 0xD8D8D8); int b = Math.round(sn(f) * 0.8f); px(12, 11 + b, 0xFF3A3A); px(12, 10 + b, 0xFFFFFF); outline(); }   // a rod's line and bobber on open water
            case "src_backwater_bayou" -> {                                                  // the Bayou: a lily pad on murky water, a frog's eyes peeking
                waves(f, 0x3A6A4A); ball(8, 9, 4.5f, 0x4AA83A); tri(8, 9, 12, 7, 12, 10, 0x3A6A4A);
                int b = (f * 4 % 1f) < 0.8f ? 0 : 1; ball(5, 6 + b, 1.4f, 0x7AC84A); ball(9, 6 + b, 1.4f, 0x7AC84A); px(5, 6 + b, 0x101010); px(9, 6 + b, 0x101010);
                outline();
            }
            case "src_lotus_atoll" -> {                                                      // the Lotus Atoll: a pink lotus opening on the water
                waves(f, 0x2A9AC8); float o = 0.6f + 0.4f * t;
                tri(8, 3, 6, 10, 10, 10, 0xFF9AC8); tri(3 + (1 - o) * 2, 6, 6, 10, 8, 10, 0xFF7AB8); tri(13 - (1 - o) * 2, 6, 10, 10, 8, 10, 0xFF7AB8);
                rect(6, 10, 5, 1, 0x4AA83A); px(8, 8, 0xFFE87A);
                outline(); glint(8, 4, 1, 0xFFFFFF, t);
            }
            case "src_moonglade_marsh" -> {                                                  // Moonglade Marsh: a crescent moon over the reeds, mist drifting
                ball(10, 4, 3f, 0xF2F0D8); ball(11.5f, 3, 2.6f, 0x1A1A2A);
                waves(f, 0x2A4A5A);
                for (int i = 0; i < 4; i++) { int x = 2 + i * 3; ln(x, 13, x + (i % 2), 6 + (i % 3), 0x5A8A3A); px(x + (i % 2), 6 + (i % 3), 0x8A5A2A); }
                outline();
                for (int i = 0; i < 6; i++) pxa(Math.round((i * 3 + f * 16) % 16), 10, 0xD8E8F0, 0.35f);
            }
            case "src_torrhus_canyon" -> {                                                   // Torrhus Canyon: two red cliffs and a river running between them
                poly(new float[]{0, 6, 5, 0}, new float[]{2, 5, 16, 16}, 0xB8583A); poly(new float[]{16, 10, 11, 16}, new float[]{3, 6, 16, 16}, 0x9A4A32);
                for (int y = 6; y < 16; y++) { int c = mix(0x3A9AE8, 0x8ADAFF, ((y - f * 10) % 4 + 4) % 4 / 4f); rect(6, y, 4, 1, c); }
                outline();
            }
            case "src_fishing_festival" -> {                                                 // the Fishing Festival: a shark's fin cutting through the waves
                waves(f, 0x2A6AC8); float x = 2 + f * 10;
                tri(x, 10, x + 4, 3, x + 6, 10, 0x6A7A8A); ln(x + 4, 3, x + 5, 9, 0x9AAABA);
                outline();
                pxa(Math.round(x - 1), 10, 0xFFFFFF, 0.8f); pxa(Math.round(x - 2), 11, 0xFFFFFF, 0.5f);
            }
            case "src_water_hotspots" -> {                                                   // water hotspots: a glowing ring of bubbles on the water
                waves(f, 0x2A7AD8); glow(8, 10, 6, 0xFF8A3A, 0.3f + 0.2f * t);
                for (int i = 0; i < 8; i++) { double a = i * TAU / 8 + f * TAU; px(Math.round(8 + (float) Math.cos(a) * 5), Math.round(10 + (float) Math.sin(a) * 2), 0xFFC07A); }
                for (int i = 0; i < 3; i++) { float u = (f + i / 3f) % 1f; pxa(6 + i * 2, Math.round(9 - u * 7), 0xBFEFFF, 1f - u); }
                outline();
            }
            case "src_special_catches" -> {                                                  // special catches: a golden fish leaping, sparkling
                waves(f, 0x2A7AD8); int y = Math.round(7 - sn(f * 0.5f) * 3);
                poly(new float[]{3, 9, 12, 9}, new float[]{y + 2, y - 1, y + 2, y + 5}, 0xF2B81A); tri(12, y + 2, 15, y, 15, y + 4, 0xD89A10); px(5, y + 1, 0x101010);
                outline(); glint(7, y, 1, 0xFFFFFF, t);
            }
            case "src_crimson_isle" -> {                                                     // Crimson Isle lava: a hook dipped into bubbling lava
                lava(f); ln(9, 0, 9, 9, 0xD8D8D8); ln(9, 9, 7, 11, 0xB8C2D6); px(6, 10, 0xB8C2D6); outline(); glow(9, 11, 4, 0xFFAA3A, 0.3f * t);
            }
            case "src_lava_hotspots" -> {                                                    // lava hotspots: a ring of flame on the lava
                lava(f);
                for (int i = 0; i < 8; i++) { double a = i * TAU / 8 + f * TAU; int fx = Math.round(8 + (float) Math.cos(a) * 5), fy = Math.round(9 + (float) Math.sin(a) * 2); px(fx, fy, 0xFFE07A); px(fx, fy - 1, 0xFF8A2A); }
                outline(); glow(8, 9, 6, 0xFF6A2A, 0.25f + 0.2f * t);
            }
            case "src_magma_fields" -> {                                                     // the Magma Fields: magma blocks cracking, flames licking up
                for (int i = 0; i < 2; i++) { bev(1 + i * 7, 8, 7, 7, 0x6A2A1A); ln(2 + i * 7, 10, 6 + i * 7, 13, mix(0xFF6A1A, 0xFFC040, t)); ln(3 + i * 7, 13, 5 + i * 7, 9, mix(0xFF6A1A, 0xFFC040, 1 - t)); }
                for (int i = 0; i < 4; i++) { float u = (f + i / 4f) % 1f; int x = 3 + i * 3; pxa(x, Math.round(7 - u * 5), 0xFFB040, 1f - u); pxa(x, Math.round(8 - u * 5), 0xFF5A1A, 1f - u); }
                outline();
            }
            case "src_plhlegblast_pool" -> {                                                 // the Plhlegblast Pool: a dark pool, a purple tentacle curling out of it
                for (int y = 10; y < 16; y++) rect(1, y, 14, 1, mix(0x2A1A3A, 0x4A2A5A, ((y + f * 6) % 3) / 3f));
                float w = sn(f) * 1.5f; for (int y = 2; y < 11; y++) { int x = Math.round(8 + (float) Math.sin(y * 0.6f + f * TAU) * 2 * (11 - y) / 9f + w * (11 - y) / 9f); rect(x, y, 2, 1, mix(0x8A3AC8, 0xC87AFF, (11 - y) / 9f)); }
                outline(); glow(8, 12, 6, 0xC87AFF, 0.12f + 0.1f * t);
            }
            case "src_spooky_festival" -> {                                                  // the Spooky Festival: a bat flapping over moonlit water
                waves(f, 0x3A2A5A); ball(12, 3, 2.2f, 0xFFE8A0);
                int fl = (int) (f * 4) % 2, y = 6;
                rect(7, y, 2, 2, 0x1A1018); tri(7, y, 2, y - 2 + fl * 3, 4, y + 2, 0x2A1A28); tri(9, y, 14, y - 2 + fl * 3, 12, y + 2, 0x2A1A28); px(7, y, 0xFF3A3A); px(8, y, 0xFF3A3A);
                outline();
            }
            case "src_jerrys_workshop" -> {                                                  // Jerry's Workshop: icy water and a present bobbing in it, snow falling
                for (int y = 11; y < 16; y++) rect(0, y, 16, 1, mix(0x8ACCEC, 0xD8F4FF, ((y + f * 5) % 3) / 3f));
                int b = Math.round(sn(f)); bev(5, 6 + b, 6, 6, 0xD83A3A); rect(7, 6 + b, 2, 6, 0x3AA85A); rect(5, 8 + b, 6, 1, 0x3AA85A); px(6, 5 + b, 0x3AA85A); px(9, 5 + b, 0x3AA85A);
                for (int i = 0; i < 4; i++) px(i * 4 + 1, Math.round((f * 12 + i * 5) % 11), 0xFFFFFF);
                outline();
            }
            case "src_fishing_treasure", "grp_fish_treasure" -> {                            // fishing treasure: a chest hooked up out of the water, gold spilling
                waves(f, 0x2A7AD8); int y = Math.round(5 - tri(f) * 2);
                ln(8, 0, 8, y, 0xD8D8D8); bev(3, y + 1, 10, 6, 0x8A5A2B); rect(3, y + 3, 10, 1, 0x5A3A1A); rect(7, y + 3, 2, 2, 0xFFC857);
                for (int i = 0; i < 3; i++) { float u = (f + i / 3f) % 1f; pxa(5 + i * 3, y + 7 + Math.round(u * 6), 0xFFD84A, 1f - u); }
                outline(); glint(7, y + 3, 1, 0xFFFFFF, t);
            }
            case "mining" -> {                                                               // Mining: a crystal cluster on rock, a pickaxe across it, the crystals catching the light in turn
                bev(1, 12, 14, 4, 0x4A4C60);
                int[] gc = {0x55D8FF, 0xC77DFF, 0x7BE05A};
                tri(4, 12, 6, 4, 8, 12, lite(gc[0], 0.1f)); tri(7, 12, 9, 2, 11, 12, gc[1]); tri(10, 12, 12, 6, 14, 12, gc[2]);
                ln(6, 5, 6, 11, lite(gc[0], 0.5f)); ln(9, 3, 9, 11, lite(gc[1], 0.5f)); ln(12, 7, 12, 11, lite(gc[2], 0.5f));
                ln(1, 13, 11, 3, 0x7A4E22); ln(2, 13, 12, 3, 0xA8743A);                                      // the handle
                ln(8, 1, 14, 6, 0xB8C2D6); ln(8, 2, 13, 6, 0x6E7890); px(8, 1, 0xFFFFFF);                    // the head
                outline();
                int k = (int) (f * 3); glint(k == 0 ? 6 : k == 1 ? 9 : 12, k == 0 ? 5 : k == 1 ? 3 : 7, 1, 0xFFFFFF, tri(f * 3 % 1f));
            }
            case "fishing" -> {                                                              // a fish leaping
                float jy = -3.5f * 4f * f * (1f - f) * 1f;
                float yy = 8 + jy;
                tri(11, yy, 15, yy - 3 + s, 15, yy + 3 - s, 0x2E8FD8);
                poly(new float[]{1.5f, 5, 9, 12, 9, 5}, new float[]{yy, yy - 3.4f, yy - 3, yy, yy + 3, yy + 3.4f}, 0x4FC3FF);
                tri(6, yy - 3.2f, 8, yy - 5, 9, yy - 3, 0x2E8FD8);
                rect(3, Math.round(yy) - 1, 1, 1, 0x10202E); rect(4, Math.round(yy) + 1, 4, 1, 0xBDEBFF);
                outline();
                for (int i = 0; i < 3; i++) { float u = (f + i / 3f) % 1f; pxa(2 + i * 4, 14 - Math.round(u * 3), 0xBDEBFF, (1f - u) * 0.9f * (f < 0.2f || f > 0.8f ? 1f : 0.3f)); }
            }
            case "dye" -> {                                                                  // a glass vial of one dye (pink), the liquid sloshing gently, a glint on the glass
                int col = 0xE8489A, li = 0xFF8AC8, dk = 0x9A2466;
                rect(6, 1, 4, 2, 0x8A5A34); rect(6, 1, 4, 1, 0xB07A48);                              // the cork
                rect(6, 3, 4, 2, 0xCFE6F0); px(6, 3, 0xFFFFFF);                                       // the neck
                for (int y = 5; y < 15; y++) { int hw = y < 7 ? 3 + (y - 5) : y > 12 ? 5 - (y - 12) : 5; rect(8 - hw, y, hw * 2, 1, 0xBFDCE8); }      // the glass body
                float slosh = sn(f) * 0.9f;
                for (int y = 7; y < 14; y++) { int hw = y > 12 ? 4 - (y - 13) : 4; for (int x = 8 - hw; x < 8 + hw; x++) { float surf = 8.2f + slosh * (x - 7.5f) / 4f; if (y + 0.5f >= surf) px(x, y, y == Math.round(surf) ? li : y >= 12 ? dk : col); } }
                rect(5, 8, 1, 4, 0xFFFFFF); pxa(5, 7, 0xFFFFFF, 0.6f);                                 // the shine on the glass
                outline();
                for (int i = 0; i < 2; i++) { float u = (f * 1.3f + i * 0.5f) % 1f; pxa(9 - i * 2, Math.round(12 - u * 4), li, 1f - u); }   // a bubble rising
                glint(11, 6, 1, 0xFFFFFF, t);
            }
            case "nodrops" -> {                                                              // an empty box, a puff of dust
                bev(2, 8, 12, 6, 0x6A7388);
                rect(3, 9, 10, 2, 0x1B2030);
                quad(2, 8, 4, 5, 12, 5, 14, 8, 0x8A93A8);
                outline();
                for (int i = 0; i < 3; i++) { float u = (f + i / 3f) % 1f; pxa(5 + i * 3, Math.round(7 - u * 5), 0xB8C0D4, (1f - u) * 0.9f); }
            }
            case "normal" -> {                                                               // a loot bag, tied at the neck, a coin peeking out
                poly(new float[]{4.5f, 11.5f, 14, 14.5f, 1.5f, 2}, new float[]{5, 5, 11, 14, 14, 11}, 0xB5773A);
                poly(new float[]{4.5f, 8, 3, 1.8f}, new float[]{5, 5, 12.5f, 11}, lite(0xB5773A, 0.35f));
                quad(5, 2.2f, 11, 2.2f, 11.5f, 5.5f, 4.5f, 5.5f, 0xC98A48);                              // the gathered neck
                rect(4, 5, 8, 1, 0xE8D08A); px(3, 6, 0xE8D08A); px(12, 6, 0xE8D08A); px(2, 7, 0xB89A4A); px(13, 7, 0xB89A4A);   // the rope
                float cy2 = 1 - 0.8f * t;
                ball(8, 2.5f + cy2 * 0.4f, 2.2f, 0xFFD25A); px(8, Math.round(2 + cy2 * 0.4f), 0xFFF3B0);
                outline();
                glint(11 - Math.round(f * 4), 8 + Math.round((float) Math.sin(f * 6.28f) * 1.5f), 1, 0xFFE9A0, 0.3f + 0.7f * t);
            }
            case "dice" -> dice(f);
            case "emblem", "emblem_special" -> {                                             // a set badge: a heraldic shield (13 wide, centred on column 8) with a star, in the set's colour
                boolean sp = n.equals("emblem_special");
                int col = tint != 0 ? tint : sp ? 0xB48CFF : 0x55D8FF;
                int[][] span = {{2, 14}, {2, 14}, {2, 14}, {2, 14}, {2, 14}, {2, 14}, {2, 14}, {3, 13}, {3, 13}, {4, 12}, {5, 11}, {6, 10}, {7, 9}, {8, 8}};       // columns of each row, y = 1 .. 14
                for (int r = 0; r < span.length; r++) for (int x = span[r][0]; x <= span[r][1]; x++) {
                    boolean edge = r == 0 || x == span[r][0] || x == span[r][1] || r == span.length - 1 || (r + 1 < span.length && (x < span[r + 1][0] || x > span[r + 1][1]));
                    boolean hi = !edge && (r == 1 || x == span[r][0] + 1) && r < 10;
                    px(x, 1 + r, edge ? dark(col, 0.55f) : hi ? lite(col, 0.45f) : mix(dark(col, 0.45f), col, r < 7 ? 0.25f : 0.1f));
                }
                for (int x = 3; x <= 13; x++) px(x, 2, lite(col, 0.55f));
                if (!sp) {
                    String[] star = {"...#...", "...#...", "#######", ".#####.", "..###..", ".##.##.", ".#...#."};       // a 5 point star, 7 x 7, its middle on column 8
                    float k = 0.55f + 0.45f * t;
                    for (int y = 0; y < 7; y++) for (int x = 0; x < 7; x++) if (star[y].charAt(x) == '#') px(5 + x, 4 + y, mix(0xFFE9A0, 0xFFFFFF, k));
                    glint(5, 4, 1, 0xFFFFFF, 0.3f + 0.7f * t);
                } else {                                                                      // Special: a four-point sparkle with two little twinkles
                    float k = 0.5f + 0.5f * t;
                    for (int d = -4; d <= 4; d++) { int w = 4 - Math.abs(d); px(8 + d, 7, mix(0xFFF3C8, 0xFFFFFF, k)); px(8, 7 + d, mix(0xFFF3C8, 0xFFFFFF, k)); }
                    for (int d = -1; d <= 1; d++) { px(8 + d, 6, 0xFFFFFF); px(8 + d, 8, 0xFFFFFF); px(7, 7 + d, 0xFFFFFF); px(9, 7 + d, 0xFFFFFF); }
                    px(8, 7, 0xFFFFFF);
                    int tw = (int) (f * 8) % 4; px(5, 4, tw == 0 ? 0xFFFFFF : lite(col, 0.4f)); px(11, 10, tw == 2 ? 0xFFFFFF : lite(col, 0.4f)); px(11, 4, tw == 1 ? 0xFFFFFF : lite(col, 0.3f)); px(5, 10, tw == 3 ? 0xFFFFFF : lite(col, 0.3f));
                    glow(8, 7, 7, 0xB48CFF, 0.18f * t);
                }
            }
            case "snd_beat" -> {                                                             // Beat: a drum seen from the front, a stick strikes it and a ring of sound bursts out
                float hit = f < 0.12f ? 1f - f / 0.12f : 0f;
                ball(8, 9, 5.6f, 0xB83A3A);
                ball(8, 9, 4.2f, 0xF3EAD8);
                px(8, 9, 0xD9CCB0); rect(7, 8, 3, 1, 0xEDE3CC);
                int sy = f < 0.12f ? 6 : 2 + Math.round(2 * (float) Math.sin(Math.min(1f, (f - 0.12f) / 0.5f) * Math.PI));
                ln(13, sy - 3, 9, sy + 1, 0xC8902A); px(9, sy + 1, 0xFFE3A0);
                outline();
                float u = f; float r = 5.5f + u * 3.5f;
                for (int k = 0; k < 20; k++) { float an = k * TAU / 20f; pxa(Math.round(8 + (float) Math.cos(an) * r), Math.round(9 + (float) Math.sin(an) * r), 0xFFE3A0, (1f - u) * 0.8f); }
                if (hit > 0f) glow(8, 9, 5, 0xFFFFFF, 0.35f * hit);
            }
            case "snd_burst" -> {                                                            // Burst: a starburst
                glow(8, 8, 8, 0xFF9A3A, 0.25f + 0.15f * t);
                float r = 3f + 3.5f * t;
                for (int i = 0; i < 8; i++) { float a = i * 0.7854f + f * 0.4f; float r2 = i % 2 == 0 ? r + 2 : r; ln(8, 8, 8 + (float) Math.cos(a) * r2, 8 + (float) Math.sin(a) * r2, i % 2 == 0 ? 0xFFE27A : 0xFFB03A); }
                ball(8, 8, 2.6f, 0xFFF3B0);
                outline();
            }
            case "snd_reveal" -> {                                                           // Reveal: a curtain opening on a bright stage
                rect(1, 2, 14, 12, 0xFFF3B0); glow(8, 8, 6, 0xFFFFFF, 0.4f);
                float o = 2f + 3.5f * t;
                quad(1, 2, 1 + (8 - o), 2, 1 + (8 - o) - 1, 14, 1, 14, 0xC8263A); quad(15, 2, 15 - (8 - o), 2, 15 - (8 - o) + 1, 14, 15, 14, 0xC8263A);
                rect(1, 1, 14, 2, 0x8A1F2E); rect(2, 2, 12, 1, 0xE0484E);
                ball(8, 9, 1.8f, 0xFFD25A);
                outline();
            }
            case "snd_linger" -> {                                                           // Linger: a gold note, its echoes drifting off to the right and fading
                for (int e = 2; e >= 0; e--) {
                    float u = (f + e / 3f) % 1f; int ox = e == 0 ? 0 : Math.round(u * 6) + e; float al = e == 0 ? 1f : (1f - u) * 0.6f;
                    int col = e == 0 ? 0xE8B84A : 0xB9A8F0;
                    if (e == 0) { rect(8, 3, 1, 9, 0xC8902A); quad(8, 3, 12, 5, 12, 7, 8, 5, 0xE8B84A); ball(6.5f, 12, 2.2f, col); }
                    else { for (int y = 3; y < 12; y++) pxa(8 + ox, y, col, al * 0.8f); for (int y = 11; y <= 13; y++) for (int x = 5; x <= 8; x++) pxa(x + ox, y, col, al * 0.7f); }
                }
                outline();
                glint(5, 11, 1, 0xFFFFFF, 0.4f + 0.6f * t);
            }
            case "snd_end" -> {                                                              // End: a bell struck once, a last ring
                float sw = s * 1.2f;
                poly(new float[]{8 + sw * 0.3f, 12 + sw * 0.5f, 13 + sw, 3 + sw, 4 + sw * 0.5f}, new float[]{2, 6, 12, 12, 6}, 0xFFD25A);
                bev(2 + Math.round(sw), 12, 12, 2, 0xE8A62A);
                px(Math.round(8 + sw), 14, 0x8A5A12);
                outline();
                if (t > 0.3f) { pxa(1, 7, 0xFFE9A0, t); pxa(0, 9, 0xFFE9A0, t * 0.6f); pxa(14, 7, 0xFFE9A0, t); pxa(15, 9, 0xFFE9A0, t * 0.6f); }
            }
            case "bell" -> {                                                                 // a bell swinging
                float sw = s * 1.6f;
                poly(new float[]{8 + sw * 0.3f, 12 + sw * 0.6f, 13.5f + sw, 2.5f + sw, 4 + sw * 0.6f}, new float[]{2, 6, 12, 12, 6}, 0xFFD25A);
                bev(1 + Math.round(sw), 12, 14, 2, 0xE8A62A);
                px(Math.round(8 + sw * 1.2f), 14, 0x8A5A12); px(Math.round(8 + sw * 1.2f), 15, 0x8A5A12);
                rect(7, 1, 2, 1, 0xB8861E);
                outline();
                glint(5 + Math.round(sw * 0.6f), 5, 1, 0xFFFFFF, 0.9f);
            }
            case "coin" -> {                                                                 // a coin spinning
                float w = Math.abs((float) Math.cos(f * TAU / 2f)) * 6.2f + 0.8f;
                ball(8, 8, 6.5f, 0xFFC93A);
                for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) { float dx = (x + 0.5f - 8); if (Math.hypot(dx, y + 0.5f - 8) <= 6.5f && Math.abs(dx) > w) px(x, y, 0xFFC93A); }
                for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) { float dx = x + 0.5f - 8, dy = y + 0.5f - 8; if (Math.abs(dx) <= w && Math.hypot(dx / (w / 6.5f), dy) <= 6.5f) { float l = (-dx * 0.4f - dy * 0.7f) / 6f; px(x, y, l > 0.1f ? lite(0xFFC93A, l * 0.9f) : dark(0xE8A62A, -l * 0.5f)); } }
                if (w > 3.5f) { rect(7, 5, 2, 6, 0xB8861E); rect(6, 6, 4, 1, 0xB8861E); rect(6, 9, 4, 1, 0xB8861E); }
                outline();
            }
            case "size" -> {                                                                                 // Size: a planet growing and shrinking inside its frame
                for (int i = 1; i < 15; i++) { if (i % 3 == 0) continue; px(i, 1, 0x7672AE); px(i, 14, 0x7672AE); px(1, i, 0x7672AE); px(14, i, 0x7672AE); }
                float r = 3.2f + 2.2f * t; ball(8, 8, r, 0x5A4AA8); ball(7.3f, 7.3f, r * 0.8f, 0x9B7BFF); px(Math.round(7 - r * 0.4f), Math.round(7 - r * 0.4f), 0xFFFFFF);
                outline();
            }
            case "text" -> {                                                                 // a letter in a tile, a blinking cursor
                bev(1, 2, 14, 12, 0x5A74D8);
                letter(GLYPH_A, 4, 4, 0xFFFFFF, true);
                if (f < 0.5f) rect(9, 4, 1, 5, 0xFFE08A);
                rect(10, 11, 3, 1, 0xBFD0FF);
                outline();
            }
            case "bars" -> {                                                                 // cinematic bars sliding over a picture
                bev(1, 2, 14, 12, 0x6FC8FF);
                rect(2, 9, 12, 4, 0x4FA84A); ball(11, 5, 1.8f, 0xFFE08A);
                int bh = Math.round(1 + 2.5f * t);
                rect(1, 2, 14, bh, 0x0B0E1A); rect(1, 14 - bh, 14, bh, 0x0B0E1A);
                outline();
            }
            case "format" -> {                                                               // { } with a cursor
                ln(5, 3, 3, 5, 0xFFC857); ln(3, 5, 3, 7, 0xFFC857); ln(3, 7, 2, 8, 0xFFC857); ln(2, 8, 3, 9, 0xFFC857); ln(3, 9, 3, 11, 0xFFC857); ln(3, 11, 5, 13, 0xFFC857);
                ln(10, 3, 12, 5, 0xFFC857); ln(12, 5, 12, 7, 0xFFC857); ln(12, 7, 13, 8, 0xFFC857); ln(13, 8, 12, 9, 0xFFC857); ln(12, 9, 12, 11, 0xFFC857); ln(12, 11, 10, 13, 0xFFC857);
                outline();
                if (f < 0.5f) rect(7, 4, 2, 8, 0xFFFFFF);
            }
            case "note" -> {                                                                 // a music note bobbing
                int by = Math.round(s * 1f);
                ball(5.5f, 11.5f + by, 2.6f, 0xA8B8FF);
                rect(7, 2 + by, 2, 10, 0xA8B8FF); rect(7, 2 + by, 2, 1, 0xFFFFFF);
                quad(9, 2 + by, 13, 4 + by, 13, 7 + by, 9, 5 + by, 0x7F92F0);
                outline();
            }
            case "glass" -> {                                                                // a magnifying glass with a gleam
                ball(6.5f, 6.5f, 4.8f, 0x9AD8FF);
                for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) { float r = (float) Math.hypot(x + 0.5f - 6.5f, y + 0.5f - 6.5f); if (r > 3.3f && r <= 4.8f) px(x, y, 0xD8A52A); else if (r <= 3.3f) { px(x, y, mix(0xBFE8FF, 0x6FB8E8, (x + y) / 18f)); } }
                ln(10, 10, 14, 14, 0x8A5A2C); ln(11, 10, 14, 13, 0xB5733A);
                outline();
                int gx = 4 + Math.round(f * 3); px(gx, 5, 0xFFFFFF); px(gx + 1, 4, 0xFFFFFF);
            }
            case "cosmetics" -> {                                                            // a wizard hat with a star
                tri(8, 1, 3, 12, 13, 12, 0x7A5AE8);
                tri(8, 1, 8, 12, 13, 12, 0x5A3AC8);
                bev(1, 11, 14, 3, 0x4A2AA8);
                rect(4, 11, 8, 1, 0xFFD25A);
                outline();
                glint(8, 5, 1, 0xFFE9A8, 0.4f + 0.6f * t); glint(12 - Math.round(f * 2), 3, 1, 0xFFFFFF, 0.5f + 0.5f * (1f - t));
            }
            case "cos_aura" -> {                                                             // a glowing ring pulsing round a figure
                glow(8, 8, 8, 0xB56BFF, 0.35f + 0.25f * t);
                for (int a = 0; a < 40; a++) { float ang = a / 40f * TAU; px(Math.round(8 + (float) Math.cos(ang) * (5.5f + t)), Math.round(8 + (float) Math.sin(ang) * (5.5f + t)), mix(0xFF9AF0, 0x9A7AFF, a / 40f)); }
                ball(8, 7, 2.2f, 0xFFE0C0); quad(5.5f, 9.5f, 10.5f, 9.5f, 11, 14, 5, 14, 0x8A6AE8);
                outline();
            }
            case "cos_name" -> {                                                             // a name tag
                bev(1, 4, 14, 8, 0xFFE9A8);
                rect(2, 5, 12, 1, 0xFF5A5A);
                rect(3, 8, 7, 1, 0x6A5A3A); rect(3, 10, 5, 1, 0x6A5A3A);
                outline();
                glint(12, 9, 1, 0xFFFFFF, 0.3f + 0.7f * t);
            }
            case "cos_suffix" -> {                                                           // a tag with a plus, glittering
                quad(1, 5, 11, 5, 15, 8, 11, 11, 0xB48CFF); poly(new float[]{1, 11, 15, 11, 1}, new float[]{5, 5, 8, 11, 11}, 0xB48CFF);
                rect(3, 7, 4, 1, 0xFFFFFF); rect(4, 6, 1, 3, 0xFFFFFF);
                px(11, 8, 0x3A2A6A);
                outline();
                glint(13, 4, 1, 0xFFFFFF, 0.3f + 0.7f * t);
            }
            case "chan_party" -> {                                                           // three friends
                for (int i = 0; i < 3; i++) { int bx = 3 + i * 5, by = i == 1 ? 3 : 5 + Math.round(Math.max(0f, (float) Math.sin(f * TAU + i * 2)) * -1f); ball(bx, by + 1, 2.1f, i == 1 ? 0xFFD0A0 : 0xF0C090); quad(bx - 2.5f, by + 3.5f, bx + 2.5f, by + 3.5f, bx + 3, by + 10 - (i == 1 ? 0 : 1), bx - 3, by + 10 - (i == 1 ? 0 : 1), i == 0 ? 0x5AA8FF : i == 1 ? 0x62E68A : 0xFF8AB0); }
                outline();
            }
            case "chan_coop" -> {                                                            // a little house shared by two
                tri(1, 7.5f, 8, 1.5f, 15, 7.5f, 0x4FB07A);
                bev(3, 7, 10, 7, 0xD8F0E0);
                ball(6, 10, 1.6f, 0xF0C090); ball(10, 10, 1.6f, 0xF0C090); rect(5, 12, 2, 2, 0x5AA8FF); rect(9, 12, 2, 2, 0xFF8AB0);
                outline();
                px(8, 5, 0xFFFFFF);
            }
            case "chan_public" -> {                                                          // a globe turning
                ball(8, 8, 6.6f, 0x3F8FE0);
                for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
                    float dx = x + 0.5f - 8, dy = y + 0.5f - 8; if (Math.hypot(dx, dy) > 6.5f) continue;
                    float lon = (float) Math.asin(dx / 6.6f) + f * TAU;
                    float land = (float) (Math.sin(lon * 2.0 + dy * 0.55) + Math.sin(lon * 3.0 - dy * 0.4) * 0.6);
                    if (land > 0.55f) { float l = (-dx * 0.5f - dy * 0.7f) / 7f; px(x, y, l > 0.1f ? 0x7BE05A : dark(0x4FA84A, Math.max(0f, -l) * 0.7f)); }
                }
                outline();
                px(5, 4, 0xFFFFFF); px(6, 3, 0xDDEEFF);
            }
            case "chan_off" -> {                                                             // a bubble crossed out
                bev(2, 3, 12, 8, 0x6A7388); tri(4, 11, 4, 14, 8, 11, 0x6A7388);
                outline();
                ln(2, 2, 14, 13, 0xFF5A5A); ln(3, 2, 14, 12, 0xFF8A8A);
            }
            case "bazaar" -> {                                                               // a market stall with a striped awning
                for (int i = 0; i < 6; i++) quad(1 + i * 2.3f, 3, 3.3f + i * 2.3f, 3, 3.3f + i * 2.3f + (i % 2 == 0 ? 0 : 0), 7, 1 + i * 2.3f, 7, i % 2 == 0 ? 0xE8503A : 0xF4F4F8);
                rect(1, 7, 14, 1, 0x8F2F22);
                bev(2, 8, 12, 6, 0xB5733A);
                ball(5.5f, 10.5f, 1.7f, 0xFF7A5A); ball(9, 10.5f, 1.7f, 0xFFC93A); ball(12, 11, 1.4f, 0x7BE05A);
                outline();
                glint(9, 9, 1, 0xFFFFFF, t);
            }
            case "hammer" -> {                                                               // an auction gavel
                float sw = f < 0.5f ? t : 1f - (f - 0.5f) * 2f;
                float a = -0.9f + 0.9f * (f < 0.5f ? f * 2f : 1f - (f - 0.5f) * 2f * 0.0f);
                float hx = 10 - 2 * sw, hy = 4 + 3 * sw;
                ln(3, 13, hx - 1, hy + 2, 0x8A5A2C); ln(4, 13, hx, hy + 2, 0x6B4423);
                quad(hx - 3, hy - 1, hx + 3, hy - 3, hx + 5, hy + 1, hx - 1, hy + 3, 0xB5733A);
                quad(hx - 3, hy - 1, hx + 3, hy - 3, hx + 3.6f, hy - 2, hx - 2.4f, hy, 0xE8A06A);
                rect(2, 13, 6, 2, 0x6A7388);
                outline();
                if (sw > 0.85f) { glint(2, 12, 1, 0xFFFFFF, 1f); px(1, 11, 0xFFE27A); px(8, 12, 0xFFE27A); }
            }
            case "back" -> {                                                                                 // Back: a little rocket turning for home, nudging left
                int o = -Math.round(t);
                rect(5 + o, 7, 7, 3, 0xE8ECFF); tri(5 + o, 6.5f, 5 + o, 10.5f, 2 + o, 8.5f, 0xFF7AD9);   // body, nose (pointing left)
                px(7 + o, 8, 0x7AD8FF); rect(11 + o, 5, 2, 2, 0xFF7AD9); rect(11 + o, 10, 2, 2, 0xFF7AD9);
                pxa(13 + o, 8, 0xFFB050, 0.9f); pxa(14 + o, 8, 0xFFE07A, 0.6f + 0.4f * t);
                outline();
            }
            case "updates", "refresh" -> {                                                                                 // Updates: a satellite turning, a signal blinking
                float an = f * TAU * 0.25f;
                rect(6, 6, 4, 4, 0xC8CCE8); px(7, 7, 0xFFFFFF);
                rect(1, 7, 4, 2, 0x5A8AE8); rect(11, 7, 4, 2, 0x5A8AE8); px(1, 7, 0x8AB8FF); px(11, 7, 0x8AB8FF);   // solar panels
                ln(8, 6, 8 + Math.round((float) Math.cos(an) * 2), 2, 0x9A9EC8); px(8 + Math.round((float) Math.cos(an) * 2), 2, t > 0.5f ? 0x7AF0B0 : 0x2A6A4A);
                outline();
            }
            case "switch" -> {                                                                                 // a toggle: a track with a star for a knob, sliding across
                bev(1, 5, 14, 6, 0x2C2A66); rect(2, 6, 12, 4, 0x1A1848);
                int kx = 4 + Math.round(t * 7); rect(2, 6, kx - 1, 4, 0x5A4AA8);
                ball(kx, 8, 2.5f, 0xE8E0FF); px(kx, 6, 0xFFFFFF); px(kx - 1, 8, 0xFFFFFF); px(kx + 1, 8, 0xFFFFFF); px(kx, 10, 0xFFFFFF); px(kx, 8, 0xFF7AD9);
                outline();
            }
            case "speed" -> {                                                                                 // Speed: a rocket streaking right, speed lines behind it
                for (int i = 0; i < 3; i++) { float u = (f + i / 3f) % 1f; int y = 5 + i * 3, x0 = Math.round(1 + u * 3); for (int k = 0; k < 4; k++) pxa(x0 + k, y, 0x9B7BFF, (1f - u) * (1f - k / 4f)); }
                rect(6, 7, 6, 3, 0xE8ECFF); tri(12, 6.5f, 12, 10.5f, 15, 8.5f, 0xFF7AD9); px(9, 8, 0x7AD8FF);
                rect(6, 5, 2, 2, 0xFF7AD9); rect(6, 10, 2, 2, 0xFF7AD9); pxa(5, 8, 0xFFB050, 0.9f); pxa(4, 8, 0xFFE07A, 0.5f + 0.5f * t);
                outline();
            }
            case "clock" -> {                                                                                 // Time: a moon going through its phases
                ball(8, 8, 6, 0xE8E4FF); ball(7, 7, 1.2f, 0xC8C4E8); ball(10, 10, 0.9f, 0xC8C4E8);
                float ph = f * 2f - 1f; ball(8 + ph * 8, 8, 6, 0x14163E);                             // the shadow sweeping across
                px(2, 2, 0xFFFFFF); px(14, 13, 0xFFE38A);
                outline();
            }
            case "send" -> {                                                                 // a paper plane flying off
                int ox = Math.round(t * -1.5f), oy = Math.round(t * 1.5f);
                tri(1 + ox, 7 + oy, 15 + ox, 2 + oy, 6 + ox, 13 + oy, 0xF4F8FF);
                tri(6 + ox, 13 + oy, 15 + ox, 2 + oy, 8 + ox, 9 + oy, 0x9AB0E8);
                outline();
                pxa(1, 12, 0xBFD0FF, 1f - t); pxa(2, 14, 0xBFD0FF, 0.6f);
            }
            case "play" -> {                                                                 // a play button, pulsing
                float sc = 0.9f + 0.1f * t;
                tri(4, 8 - 6 * sc, 4, 8 + 6 * sc, 14, 8, 0x62E68A);
                outline();
                px(6, 5, 0xFFFFFF); px(6, 6, 0xDFFFE8);
            }
            case "menu" -> {                                                                 // three bars sliding
                for (int i = 0; i < 3; i++) { int sh = Math.round((float) Math.sin(f * TAU + i * 0.9f) * 1f); bev(2 + sh, 3 + i * 4, 12, 2, 0xB8C4F0); }
                outline();
            }
            case "disc" -> {                                                                 // a record spinning
                ball(8, 8, 7f, 0x2A2F44);
                for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) { float r = (float) Math.hypot(x + 0.5f - 8, y + 0.5f - 8); if (r < 7 && r > 2.4f && ((int) (r * 1.6f) % 2 == 0)) px(x, y, 0x3C4260); else if (r <= 2.4f) px(x, y, 0xE0508A); }
                float a = f * TAU; px(Math.round(8 + (float) Math.cos(a) * 4.5f), Math.round(8 + (float) Math.sin(a) * 4.5f), 0xFFFFFF); px(8, 8, 0x14161F);
                outline();
            }
            case "griffin" -> {                                                              // a feather in the colour you give it
                int col = tint != 0 ? tint : 0xFFD25A;
                poly(new float[]{13, 8, 2, 4.5f, 9, 11}, new float[]{1.5f, 2, 13, 11.5f, 6, 7}, col);
                poly(new float[]{13, 9, 4, 12}, new float[]{1.5f, 6, 11.5f, 5}, lite(col, 0.4f));
                ln(2, 14, 12.5f, 2, lite(col, 0.75f));
                outline();
                glint(11, 4 + Math.round(t * 2), 1, 0xFFFFFF, 0.4f + 0.6f * t);
            }
            case "dock_top", "dock_bottom", "dock_off" -> {                                  // a screen with the preview docked
                bev(1, 2, 14, 12, 0x2B3350);
                if (n.equals("dock_top")) bev(3 + Math.round(t), 3, 10, 4, 0x5A74D8);
                else if (n.equals("dock_bottom")) bev(3 + Math.round(t), 9, 10, 4, 0x5A74D8);
                else { ln(3, 4, 12, 11, 0xFF5A5A); ln(3, 11, 12, 4, 0xFF5A5A); }
                outline();
            }
            case "anim_menu" -> {                                                            // a window bouncing open
                int h = Math.round(5 + 3 * t);
                bev(2, 8 - h / 2, 12, h, 0x7A92E0); rect(3, 9 - h / 2, 10, 1, 0xBFD0FF);
                outline();
            }
            case "anim_icons" -> {                                                           // a small icon in motion
                int bx = 5 + Math.round(s * 1.5f);
                ball(bx + 2, 8, 3.4f, 0xFFC857);
                for (int i = 1; i <= 3; i++) pxa(bx - i * 2, 8, 0xFFC857, 0.7f / i);
                outline();
                glint(bx + 4, 5, 1, 0xFFFFFF, 1f);
            }
            case "fancydrops" -> {                                                           // the logo: a gem over a chest
                bev(2, 10, 12, 5, 0x9A5E2C); bev(7, 10, 2, 3, 0xFFD25A);
                int gy = Math.round(-1f * t);
                poly(new float[]{5, 11, 13, 8, 3}, new float[]{3 + gy, 3 + gy, 6 + gy, 11 + gy, 6 + gy}, 0x55D8FF);
                poly(new float[]{5, 8, 11, 8}, new float[]{3 + gy, 6 + gy, 3 + gy, 3 + gy}, 0xBFF1FF);
                outline();
                glint(3, 3, 2, 0xFFFFFF, t); glint(13, 2 + Math.round(t), 1, 0xFF72C6, 1f - t);
            }
            case "discord" -> {                                                              // community: two bubbles chatting, bobbing against each other, the dots typing in turn
                int o1 = Math.round((float) Math.sin(f * Math.PI * 2) * 1.2f), o2 = -o1, lit = (int) (f * 3f) % 3, lit2 = (int) (f * 3f + 1f) % 3;
                bev(1, 2 + o1, 9, 7, 0x7A8CF0); tri(3, 9 + o1, 3, 12 + o1, 6, 9 + o1, 0x7A8CF0);
                bev(6, 7 + o2, 9, 6, 0xB8C2FF); tri(12, 13 + o2, 13, 15.5f + o2, 10, 13 + o2, 0xB8C2FF);
                for (int d = 0; d < 3; d++) rect(3 + d * 2, 4 + o1 - (d == lit ? 1 : 0), 1, 1, d == lit ? 0xFFFFFF : 0xC9D2FF);
                outline();
                for (int d = 0; d < 3; d++) rect(8 + d * 2, 9 + o2 - (d == lit2 ? 1 : 0), 1, 1, d == lit2 ? 0x2A36B8 : 0x4A5AD8);
            }
            case "patreon" -> {                                                              // support: a heart that beats, with a spark
                float k = 1f + 0.17f * t, cxh = 8f, cyh = 8.5f;
                float[] hx = {8, 14, 15, 8, 1, 2}, hy = {3.2f, 1.5f, 6, 15, 6, 1.5f};
                for (int q = 0; q < 6; q++) { hx[q] = cxh + (hx[q] - cxh) * k; hy[q] = cyh + (hy[q] - cyh) * k; }
                poly(hx, hy, 0xFF6A5A);
                ball(cxh + (4.6f - cxh) * k, cyh + (5 - cyh) * k, 3.4f * k, 0xFF6A5A); ball(cxh + (11.4f - cxh) * k, cyh + (5 - cyh) * k, 3.4f * k, 0xFF6A5A);
                tri(cxh + (1.4f - cxh) * k, cyh + (6.2f - cyh) * k, cxh + (14.6f - cxh) * k, cyh + (6.2f - cyh) * k, 8, cyh + (14.5f - cyh) * k, 0xE04A4A);
                outline();
                px(4, 3, 0xFFFFFF); glint(13, 2, 1, 0xFFE08A, Math.max(0f, t));
            }
            case "github" -> {                                                               // source: a branching tree, nodes breathing in turn, a commit travelling up
                ln(5, 3, 5, 13, 0xC8D0E4); ln(6, 3, 6, 13, 0x9AA4C0);
                ln(5, 8, 11, 4, 0xC8D0E4); ln(6, 8, 12, 4, 0x9AA4C0);
                float w = f * (float) Math.PI * 2;
                ball(5.5f, 3, 2.2f + 0.7f * (float) Math.sin(w), 0x62E6A8); ball(5.5f, 13, 2.2f + 0.7f * (float) Math.sin(w + 2.1f), 0x62E6A8); ball(11.5f, 4, 2.2f + 0.7f * (float) Math.sin(w + 4.2f), 0xFFC857);
                outline();
                float u = f; int cx2, cy2;
                if (u < 0.5f) { cx2 = 5; cy2 = Math.round(13 - 10 * u); } else { float v = (u - 0.5f) * 2f; cx2 = Math.round(5.5f + 6 * v); cy2 = Math.round(8 - 4 * v); }
                px(cx2, cy2, 0xFFFFFF); pxa(cx2, cy2 + 1, 0xFFFFFF, 0.5f);
            }
            case "cx_bubbleaura" -> {                                                        // Bubbles: bubbles wobbling up
                for (int i = 0; i < 4; i++) { float u = (f + i * 0.25f) % 1f; int x = Math.round(4 + i * 2.6f + (float) Math.sin(u * 9 + i) * 1.2f), y = Math.round(14 - u * 13); int r = i % 2 == 0 ? 2 : 1;
                    for (int dy = -r; dy <= r; dy++) for (int dx = -r; dx <= r; dx++) { int d2 = dx * dx + dy * dy; if (d2 <= r * r && d2 >= (r - 1) * (r - 1) + (r > 1 ? 1 : 0)) pxa(x + dx, y + dy, 0x8FE0FF, 1f - u * 0.6f); }
                    pxa(x - r + 1, y - r + 1, 0xFFFFFF, 1f - u); }
            }
            case "cx_fireflyaura" -> {                                                       // Fireflies: soft lights that blink in turn
                int[][] pos = {{4, 5}, {11, 4}, {7, 10}, {12, 12}, {3, 12}};
                for (int i = 0; i < pos.length; i++) { float b = Math.max(0f, (float) Math.sin((f + i * 0.2f) * TAU)); glow(pos[i][0], pos[i][1], 3, 0xE8FF70, 0.35f * b); px(pos[i][0], pos[i][1], b > 0.3f ? 0xF4FFB0 : 0x6A8A30); }
            }
            case "cx_coinaura" -> {                                                          // Coin Shower: coins tumbling down
                for (int i = 0; i < 3; i++) { float u = (f + i / 3f) % 1f; int x = 3 + i * 5, y = Math.round(u * 13); float w = Math.abs((float) Math.cos((u * 3 + i) * TAU / 2)); int hw = Math.max(0, Math.round(w * 2));
                    rect(x - hw, y, hw * 2 + 1, 3, 0xF2B01E); rect(x - hw, y, hw * 2 + 1, 1, 0xFFE27A); if (hw > 0) px(x, y + 1, 0xFFF4C8); }
                outline();
            }
            case "cx_runeaura" -> {                                                          // Runes: glyphs on a turning ring, one lit
                for (int i = 0; i < 6; i++) { float ang = f * TAU + i * TAU / 6; int x = Math.round(8 + (float) Math.cos(ang) * 6), y = Math.round(8 + (float) Math.sin(ang) * 3); boolean on = i == (int) (f * 6) % 6;
                    int c = on ? 0xF4E8FF : 0xB56BFF; px(x, y, c); px(x, y - 1, c); if (i % 2 == 0) px(x + 1, y - 1, c); else px(x - 1, y, c); if (on) glow(x, y, 3, 0xC9A0FF, 0.4f); }
            }
            case "cx_sparkleaura" -> {                                                       // Twinkle: gold and pink stars twinkling
                for (int i = 0; i < 4; i++) { float b = Math.max(0f, (float) Math.sin((f + i * 0.25f) * TAU)); int x = new int[]{4, 11, 7, 12}[i], y = new int[]{4, 6, 11, 12}[i]; int c = i % 2 == 0 ? 0xFFE680 : 0xFFB3E6; int r = Math.round(b * 2);
                    px(x, y, 0xFFFFFF); for (int k = 1; k <= r; k++) { px(x + k, y, c); px(x - k, y, c); px(x, y + k, c); px(x, y - k, c); } }
            }
            case "cx_glitchaura" -> {                                                        // Alarm: red sparks round a rising amber ring
                int ry = Math.round(14 - f * 12); for (int x = 3; x < 13; x++) pxa(x, ry, 0xFFC03D, 0.8f);
                for (int i = 0; i < 5; i++) { float ang = f * TAU + i * TAU / 5; int x = Math.round(8 + (float) Math.cos(ang) * 6), y = 3 + i * 2; px(x, y, Math.abs(y - ry) < 2 ? 0xFFFFFF : i % 2 == 0 ? 0xFF4A2E : 0xFFB02E); }
            }
            case "cx_stardust" -> {                                                          // Stardust: two strands winding up, a halo above
                for (int i = 0; i < 12; i++) { float u = i / 12f, ang = u * 9 + f * TAU; int y = Math.round(14 - u * 11); px(Math.round(8 + (float) Math.cos(ang) * 4), y, 0x4DD6FF); px(Math.round(8 - (float) Math.cos(ang) * 4), y, 0x8FA8FF); }
                for (int x = 5; x < 12; x++) px(x, 1, 0xE8FFFF);
            }
            case "cx_patronaura" -> {                                                        // Embers: gold embers drifting up
                for (int i = 0; i < 6; i++) { float u = (f + i / 6f) % 1f; int x = Math.round(3 + i * 2 + (float) Math.sin(u * 6 + i)), y = Math.round(14 - u * 13); pxa(x, y, u < 0.5f ? 0xFFD24A : 0xFF7A1E, 1f - u * 0.7f); if (i % 3 == 0) pxa(x, y + 1, 0xFF7A1E, 0.5f * (1 - u)); }
            }
            default -> {
                if (n.startsWith("cx_")) return generic(n, f);
                if (n.startsWith("mob_")) { mob(n.substring(4), f); outline(); return true; }
                if (n.startsWith("emblem")) return paint("emblem", tint, f);
                return false;
            }
        }
        return true;
    }

    /** The mound of earth under the spade. */
    private static void fillMound() { for (int y = 11; y < 15; y++) for (int x = 1; x < 15; x++) { float dx = (x + 0.5f - 8) / 7.2f, dy = (y + 0.5f - 14.5f) / 3.8f; if (dx * dx + dy * dy <= 1f) px(x, y, y < 12 ? 0x9A7A52 : y < 14 ? 0x6B4A2B : 0x4A3220); } }

    /** Cosmetic effects and auras without their own picture: a glowing orb in a colour taken from the name. */
    private static boolean generic(String n, float f) {
        float h = (n.hashCode() & 0xFFFF) / 65535f; int col = hue(h, 0.55f, 1f);
        glow(8, 8, 8, col, 0.3f + 0.2f * tri(f));
        ball(8, 8, 4.6f, col);
        for (int a = 0; a < 3; a++) { float ang = f * TAU + a * 2.09f; px(Math.round(8 + (float) Math.cos(ang) * 7), Math.round(8 + (float) Math.sin(ang) * 5.5f), 0xFFFFFF); }
        outline();
        return true;
    }

    // ------------------------------------------------------------------ the 25 text effects: the letter A, doing what the effect does
    private static final String[] BIG_A = {".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"};
    // ---- the twelve Mythological creatures (Drop Library sidebar and tiles)
    private static void mob(String id, float f) {
        float t = tri(f), s = sn(f);
        switch (id) {
            case "minos_hunter" -> {                                                         // a hooded hunter drawing a bow
                tri(3, 14, 8, 2, 13, 14, 0x2F6B3A); tri(5, 14, 8, 4, 11, 14, 0x3F8A4C);
                ball(8, 7, 2.6f, 0x1A2A1C); px(7, 7, 0xFFE6A0); px(9, 7, 0xFFE6A0);
                rect(5, 12, 6, 3, 0x6B4A2B);
                float pull = 1.5f * t;
                ln(13, 3, 14 + 0.5f, 8, 0xC8A060); ln(14.5f, 8, 13, 13, 0xC8A060);
                ln(13 - pull, 8, 13, 3, 0xE8E8E8); ln(13 - pull, 8, 13, 13, 0xE8E8E8);
                ln(6 + pull, 8, 14, 8, 0xD8D0B8); px(15, 8, 0xFFFFFF);
            }
            case "siamese_lynxes" -> {                                                       // two cats side by side
                for (int k = 0; k < 2; k++) {
                    int ox = k * 7, base = k == 0 ? 0xD8B27A : 0xB8946A;
                    tri(1 + ox, 7, 1 + ox, 2, 4 + ox, 5, base); tri(8 + ox - 1, 7, 7 + ox + 1, 2, 5 + ox, 5, base);
                    px(2 + ox, 4, 0xF0A0A0);
                    ball(4.5f + ox, 9, 3.4f, base); px(1 + ox, 11, lite(base, 0.5f)); px(8 + ox, 11, lite(base, 0.5f));
                    boolean blink = (k == 0 ? f > 0.55f && f < 0.65f : f > 0.2f && f < 0.3f);
                    if (blink) { px(3 + ox, 9, 0x1A1208); px(6 + ox, 9, 0x1A1208); } else { px(3 + ox, 8, 0x7CFF9A); px(3 + ox, 9, 0x101810); px(6 + ox, 8, 0x7CFF9A); px(6 + ox, 9, 0x101810); }
                    px(4 + ox, 10, 0xF08A8A); px(5 + ox, 10, 0xF08A8A);
                    px(4 + ox, 2 + (k == 0 ? 0 : 0), 0x3A2A18);
                }
                rect(1, 13, 14, 2, 0x8A6A44);
            }
            case "stranded_nymph" -> {                                                       // a sea nymph with flowing aqua hair
                float sw = s * 1.2f;
                for (int y = 3; y < 15; y++) { float k = (y - 3) / 12f; int x0 = Math.round(3 - sw * k), x1 = Math.round(12 + sw * k); px(x0, y, 0x2FB8B0); px(x0 + 1, y, 0x58E0D0); px(x1, y, 0x2FB8B0); px(x1 - 1, y, 0x58E0D0); }
                ball(8, 6, 3.2f, 0x2FB8B0); ball(8, 7, 2.6f, 0xF6D2B4);
                px(7, 7, 0x203040); px(9, 7, 0x203040); px(8, 9, 0xE08A8A);
                rect(4, 3, 8, 2, 0x58E0D0); px(8, 2, 0xFFFFFF);
                tri(5, 15, 8, 11, 11, 15, 0x3F8AD8);
                float d = (f * 12) % 12; pxa(14, 2 + Math.round(d), 0x9AE8FF, 1f - d / 12f);
            }
            case "cretan_bull" -> {                                                          // a bull's head with a ring in its nose
                ln(1, 6, 3, 3, 0xF0E8D0); ln(1, 6, 2, 8, 0xF0E8D0); ln(15, 6, 13, 3, 0xF0E8D0); ln(15, 6, 14, 8, 0xF0E8D0);
                px(2, 3, 0xFFFFFF); px(13, 3, 0xFFFFFF);
                rect(3, 5, 10, 6, 0x7A4A2A); rect(4, 4, 8, 1, 0x7A4A2A);
                rect(5, 10, 6, 4, 0xC89870); rect(6, 13, 4, 1, 0xA87850);
                px(5, 7, t > 0.5f ? 0x101010 : 0xFF6A3A); px(10, 7, t > 0.5f ? 0x101010 : 0xFF6A3A);
                px(6, 11, 0x301810); px(9, 11, 0x301810);
                ln(7, 13, 8, 15, 0xE8D060); px(9, 13, 0xE8D060);
                if (t > 0.5f) { pxa(5, 15, 0xD8E8F0, 0.7f); pxa(10, 15, 0xD8E8F0, 0.7f); }
            }
            case "harpy" -> {                                                                // a flapping harpy
                float fl = s * 3f;
                poly(new float[]{7, 0, 1, 7}, new float[]{8, 5 - fl, 9 - fl, 11}, 0x9A5AB8); poly(new float[]{9, 16, 15, 9}, new float[]{8, 5 - fl, 9 - fl, 11}, 0x9A5AB8);
                ln(1, 9 - fl, 7, 9, 0xC890E0); ln(15, 9 - fl, 9, 9, 0xC890E0);
                rect(6, 8, 4, 5, 0xB87AD0); tri(6, 13, 8, 15, 10, 13, 0x7A3A98);
                ball(8, 6, 2.4f, 0xF2CBA8); rect(5, 3, 6, 2, 0x5A2A78); px(5, 5, 0x5A2A78); px(10, 5, 0x5A2A78);
                px(7, 6, 0x301020); px(9, 6, 0x301020);
                px(6, 14, 0xE8D060); px(10, 14, 0xE8D060);
            }
            case "gaia_construct" -> {                                                       // a stone golem with green eyes
                bev(3, 3, 10, 10, 0x8A8F9A); rect(4, 4, 8, 2, 0xA8ADB8);
                rect(1, 6, 2, 6, 0x6E7380); rect(13, 6, 2, 6, 0x6E7380);
                rect(4, 7, 3, 2, 0x161A18); rect(9, 7, 3, 2, 0x161A18);
                int ey = mix(0x3FD16A, 0xB8FFC8, t); px(5, 7, ey); px(6, 7, ey); px(10, 7, ey); px(11, 7, ey);
                rect(5, 11, 6, 1, 0x4A4F5A); for (int i = 5; i < 11; i += 2) px(i, 12, 0x4A4F5A);
                px(4, 4, 0x4F9A4A); px(5, 3, 0x4F9A4A); px(11, 12, 0x4F9A4A); px(12, 11, 0x4F9A4A);
                glow(8, 8, 6, 0x3FD16A, 0.12f * t);
            }
            case "minotaur" -> {                                                             // the horned dark head, eyes burning
                ln(2, 6, 2, 3, 0xE8E0C8); ln(2, 3, 4, 2, 0xE8E0C8); ln(13, 6, 13, 3, 0xE8E0C8); ln(13, 3, 11, 2, 0xE8E0C8);
                rect(4, 3, 8, 7, 0x4A2C2C); rect(3, 5, 10, 4, 0x4A2C2C); rect(5, 9, 6, 5, 0x3A2222); rect(6, 13, 4, 1, 0x2A1616);
                int ey = mix(0xFF4A2A, 0xFFE0A0, t); px(5, 6, ey); px(6, 6, ey); px(9, 6, ey); px(10, 6, ey);
                px(7, 11, 0x1A0E0E); px(8, 11, 0x1A0E0E); px(7, 12, 0xA8A0A0); px(8, 12, 0xA8A0A0);
                glow(8, 6, 7, 0xFF4A2A, 0.12f * t);
            }
            case "minos_champion" -> {                                                       // a gold helm with a red plume
                for (int i = 0; i < 6; i++) { float w = (float) Math.sin((f - i * 0.08f) * TAU) * 1.2f; px(Math.round(8 + w), 1 + i, i % 2 == 0 ? 0xD8402A : 0xA82A1A); px(Math.round(9 + w), 1 + i, 0xA82A1A); }
                rect(4, 6, 8, 4, 0xE0B83A); rect(3, 8, 10, 6, 0xC89A28); rect(4, 6, 8, 1, 0xFFE27A);
                rect(5, 9, 6, 1, 0x1A1208); rect(7, 9, 2, 5, 0x1A1208);
                rect(3, 13, 10, 1, 0x8A6A18); px(5, 9, mix(0x1A1208, 0xFFE27A, t * 0.7f)); px(10, 9, mix(0x1A1208, 0xFFE27A, t * 0.7f));
                px(4, 7, 0xFFF3B0);
            }
            case "sphinx" -> {                                                               // the headdress of a sphinx, gold and blue stripes
                poly(new float[]{3, 8, 13, 14, 2}, new float[]{3, 1, 3, 14, 14}, 0x2F5FB0);
                for (int y = 3; y < 14; y += 2) { rect(3, y, 2, 1, 0xE8C040); rect(11, y, 2, 1, 0xE8C040); }
                rect(5, 3, 6, 9, 0xE6B87A); rect(5, 3, 6, 1, 0xE8C040);
                px(6, 6, 0x101828); px(9, 6, 0x101828); px(6, 5, 0xE8C040); px(9, 5, 0xE8C040);
                rect(7, 8, 2, 2, 0xC8986A); rect(6, 11, 4, 1, 0xB87A5A);
                px(8, 1, 0xE8C040); if (t > 0.6f) px(8, 2, 0xFFF3B0);
            }
            case "minos_inquisitor" -> {                                                     // a white mask in a dark purple hood
                tri(2, 15, 8, 0, 14, 15, 0x4A2A6A); tri(4, 15, 8, 3, 12, 15, 0x2A1A3A);
                rect(5, 4, 6, 8, 0xEAEAF0); rect(6, 12, 4, 1, 0xEAEAF0); rect(7, 13, 2, 1, 0xC8C8D8);
                int ey = mix(0xB46AFF, 0xF0D8FF, t); rect(5, 7, 2, 1, ey); rect(9, 7, 2, 1, ey);
                ln(8, 8, 8, 10, 0xB8B8C8); px(8, 5, 0xB46AFF);
                glow(8, 7, 6, 0xB46AFF, 0.12f * t);
            }
            case "manticore" -> {                                                            // a lion's face with a mane and a stinging tail
                for (int i = 0; i < 12; i++) { float a = i * 0.5236f; px(Math.round(8 + (float) Math.cos(a) * 6), Math.round(9 + (float) Math.sin(a) * 5.5f), 0xC8601A); px(Math.round(8 + (float) Math.cos(a) * 5), Math.round(9 + (float) Math.sin(a) * 4.5f), 0xE08A2A); }
                ball(8, 9, 4.4f, 0xE8B060); ball(8, 9, 3.4f, 0xF0C47A);
                px(6, 8, 0x201008); px(10, 8, 0x201008); px(8, 10, 0x6A2A18); px(7, 11, 0x6A2A18); px(9, 11, 0x6A2A18);
                px(5, 5, 0xE8B060); px(11, 5, 0xE8B060);
                float sw = s * 1.5f;
                ln(13, 5, 14 + sw * 0.4f, 2, 0x7A3A8A); ln(14 + sw * 0.4f, 2, 12 + sw, 0, 0x7A3A8A); px(12 + Math.round(sw), 0, 0xD0F060);
            }
            case "king_minos" -> {                                                           // a crowned, bearded king in purple
                rect(2, 12, 12, 3, 0x6A2A9A); rect(3, 12, 10, 1, 0xA860D8);
                rect(5, 5, 6, 6, 0xF2CBA8); rect(4, 9, 8, 3, 0xE8E0D0); rect(5, 12, 6, 1, 0xC8C0B0);
                px(6, 7, 0x203040); px(9, 7, 0x203040); px(7, 9, 0xB89888); px(8, 9, 0xB89888);
                rect(4, 3, 8, 2, 0xE8C040); px(4, 2, 0xE8C040); px(7, 1, 0xE8C040); px(8, 1, 0xE8C040); px(11, 2, 0xE8C040);
                px(7, 4, 0xE84A4A); px(5, 3, mix(0x4A9AFF, 0xFFFFFF, t)); px(10, 3, mix(0x4AD86A, 0xFFFFFF, 1f - t)); px(8, 2, 0xFFF3B0);
                glint(8, 1, 1, 0xFFFFFF, t);
            }
            default -> ball(8, 8, 4, 0x8890A0);
        }
    }

    private static void bigA(int x, int y, int rgb) { letter(BIG_A, x, y, rgb, true); }
    /** The big A with a colour per pixel: col(x, y) gets the pixel's place in the letter. */
    private interface Tint { int at(int x, int y); }
    private static void bigA(int x, int y, Tint t) { for (int j = 0; j < 7; j++) for (int i = 0; i < 5; i++) if (BIG_A[j].charAt(i) == '#') px(x + i, y + j, t.at(i, j)); }

    /** A white die with black pips (a red 1) that hops, tumbles in the air and settles on a 6, with its shadow below. A real cube is turned and drawn face by face. */
    private static void dice(float f) {
        float fl = f < 0.62f ? f / 0.62f : 1f, e = 1f - (1f - fl) * (1f - fl) * (1f - fl);
        float ang = (1f - e) * TAU * 1.75f;
        float hh = f < 0.38f ? (float) Math.sin(f / 0.38f * Math.PI) * 3.2f : f < 0.62f ? (float) Math.sin((f - 0.38f) / 0.24f * Math.PI) * 1.4f : 0f;
        float cy = 7.9f - hh, cx = 8f, sc = 3.45f;
        // shadow on the ground: it shrinks and fades as the die rises
        float sh = 1f - hh / 6f;
        for (int y = 12; y <= 15; y++) for (int x = 0; x < 16; x++) { float dx = (x + 0.5f - 8) / (6.4f * sh), dy = (y + 0.5f - 13.8f) / (1.7f * sh); if (dx * dx + dy * dy <= 1f) pxa(x, y, 0x8C93AE, 0.42f * sh); }
        // rotation: a turn about a slanted axis (shrinking to nothing as it lands), then the fixed view from above
        double[] ax = {0.62, 0.70, 0.35}; double n = Math.sqrt(ax[0] * ax[0] + ax[1] * ax[1] + ax[2] * ax[2]); ax[0] /= n; ax[1] /= n; ax[2] /= n;
        double ca = Math.cos(ang), sa = Math.sin(ang), ic = 1 - ca;
        double[][] r1 = {{ca + ax[0] * ax[0] * ic, ax[0] * ax[1] * ic - ax[2] * sa, ax[0] * ax[2] * ic + ax[1] * sa}, {ax[1] * ax[0] * ic + ax[2] * sa, ca + ax[1] * ax[1] * ic, ax[1] * ax[2] * ic - ax[0] * sa}, {ax[2] * ax[0] * ic - ax[1] * sa, ax[2] * ax[1] * ic + ax[0] * sa, ca + ax[2] * ax[2] * ic}};
        double ty = 0.7854, tx = 0.6155;                                                      // yaw, then tilt
        double[][] ry = {{Math.cos(ty), 0, Math.sin(ty)}, {0, 1, 0}, {-Math.sin(ty), 0, Math.cos(ty)}}, rx = {{1, 0, 0}, {0, Math.cos(tx), -Math.sin(tx)}, {0, Math.sin(tx), Math.cos(tx)}};
        double[][] m = mul(rx, mul(ry, r1));
        int[][] pips = {{}, {0, 0}, {-1, -1, 1, 1}, {-1, -1, 0, 0, 1, 1}, {-1, -1, 1, -1, -1, 1, 1, 1}, {-1, -1, 1, -1, 0, 0, -1, 1, 1, 1}, {-1, -1, -1, 0, -1, 1, 1, -1, 1, 0, 1, 1}};
        int[][] cnt = {{3, 4}, {6, 1}, {5, 2}};                                          // faces +X/-X, +Y/-Y, +Z/-Z
        boolean[][] mask = new boolean[16][16];
        double[] L = {-0.45, 0.80, 0.40};
        // draw the visible faces
        for (int a = 0; a < 3; a++) for (int sg = 1; sg >= -1; sg -= 2) {
            double[] nrm = new double[3]; nrm[a] = sg; double[] nt = apply(m, nrm);
            if (nt[2] <= 0.02) continue;
            int b = (a + 1) % 3, c2 = (a + 2) % 3;
            float[] xs = new float[4], ys = new float[4]; int[][] cs = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
            for (int q = 0; q < 4; q++) { double[] v = new double[3]; v[a] = sg; v[b] = cs[q][0]; v[c2] = cs[q][1]; double[] w = apply(m, v); xs[q] = (float) (cx + w[0] * sc); ys[q] = (float) (cy - w[1] * sc); }
            double lit = Math.max(0, nt[0] * L[0] + nt[1] * L[1] + nt[2] * L[2]);
            int face = lit > 0.55 ? 0xFFFFFF : lit > 0.2 ? 0xDADEEE : 0xB0B6CE;                  // three flat tones, so the cube reads at this size
            for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) if (inside(xs, ys, x + 0.5f, y + 0.5f)) { px(x, y, face); mask[y][x] = true; }
            double area = 0; for (int q = 0; q < 4; q++) area += xs[q] * ys[(q + 1) % 4] - xs[(q + 1) % 4] * ys[q]; area = Math.abs(area) / 2;
            if (area < 9) continue;                                                           // a face seen edge-on shows no pips
            int cn = cnt[a][sg > 0 ? 0 : 1], pc = cn == 1 ? 0xD02030 : 0x16171F;
            int[] pl = pips[cn];
            for (int pi = 0; pi + 1 < pl.length; pi += 2) {
                double[] v = new double[3]; v[a] = sg; v[b] = pl[pi] * 0.5; v[c2] = pl[pi + 1] * 0.5; double[] w = apply(m, v);
                int px0 = (int) Math.floor(cx + w[0] * sc), py0 = (int) Math.floor(cy - w[1] * sc);
                px(px0, py0, pc);
                if (cn == 1 && area > 18) { px(px0 + 1, py0, pc); px(px0, py0 + 1, pc); }
            }
        }
        // the grey-blue outline
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) if (!mask[y][x]) {
            boolean nb = (x > 0 && mask[y][x - 1]) || (x < 15 && mask[y][x + 1]) || (y > 0 && mask[y - 1][x]) || (y < 15 && mask[y + 1][x]);
            if (nb) px(x, y, 0x7B83A3);
        }
        if (f >= 0.62f && f < 0.9f) glint(11, 3, 2, 0xFFFFFF, 1f - (f - 0.62f) / 0.28f);       // a little shine as it settles
    }
    private static double[][] mul(double[][] a, double[][] b) { double[][] r = new double[3][3]; for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) for (int k = 0; k < 3; k++) r[i][j] += a[i][k] * b[k][j]; return r; }
    private static double[] apply(double[][] m, double[] v) { return new double[]{m[0][0] * v[0] + m[0][1] * v[1] + m[0][2] * v[2], m[1][0] * v[0] + m[1][1] * v[1] + m[1][2] * v[2], m[2][0] * v[0] + m[2][1] * v[1] + m[2][2] * v[2]}; }

    private static boolean fx(int i, float f) {
        float s = sn(f), t = tri(f);
        bev(0, 0, 16, 16, 0x1B2236);
        switch (i) {
            case 0 -> { bigA(5, 4, 0xFFD25A); for (int a = 0; a < 8; a++) { float ang = a * 0.785f; pxa(Math.round(7.5f + (float) Math.cos(ang) * (5 + 2.5f * t)), Math.round(7.5f + (float) Math.sin(ang) * (5 + 2.5f * t)), 0xFFE9A8, 1f - t); } }
            case 1 -> { bigA(5, 4, 0xB8C4E8); int sx = Math.round(f * 18) - 3; for (int y = 3; y < 12; y++) pxa(sx + (11 - y) / 2, y, 0xFFFFFF, 0.85f); }
            case 2 -> { for (int k = 0; k < 3; k++) letter(new String[]{".#.", "#.#", "###", "#.#"}, 1 + k * 5, 6 + Math.round((float) Math.sin(f * TAU + k * 1.3f) * 2f), 0x7FE0D0, true); }
            case 3 -> bigA(5, 4, (x, y) -> hue(f + y * 0.1f + x * 0.06f, 0.7f, 1f));
            case 4 -> { int dy = Math.round(t * 5); bigA(5, 1 + dy, 0x8FD0FF); for (int k = 0; k < 3; k++) pxa(6 + k * 2, 0 + dy / 2, 0x8FD0FF, 0.5f - k * 0.1f); }
            case 5 -> { bigA(5, 4, 0xFF9AC8); int r = 6 + Math.round(t * 2); for (int a = 0; a < 28; a++) { float ang = a * TAU / 28; pxa(Math.round(7.5f + (float) Math.cos(ang) * r), Math.round(7.5f + (float) Math.sin(ang) * r), 0xFF9AC8, 1f - t); } }
            case 6 -> { letter(BIG_A, 4, 4, 0x55D8FF, false); letter(BIG_A, 6, 4, 0xFF72C6, false); letter(BIG_A, 5, 4, 0xFFFFFF, false); if (f > 0.5f) rect(2, 8, 12, 1, 0x1B2236); }
            case 7 -> { glow(8, 8, 8, 0xFFE9A8, 0.35f + 0.3f * t); bigA(5, 4, 0xFFF3C8); }
            case 8 -> { int dy = Math.round(Math.abs((float) Math.cos(f * 3.14f)) * -6f) + 6; bigA(5, 1 + dy, 0xFF9A5A); rect(4, 14, 8, 1, 0x4A5470); }
            case 9 -> { bigA(3, 4, 0x9AF0A8); if (f < 0.5f) rect(10, 4, 2, 7, 0xFFFFFF); }
            case 10 -> { int dy = Math.round(s * 2f); bigA(5, 4 + dy, 0xBFD0FF); px(2, 11 - dy, 0x7F92F0); px(13, 12 + dy, 0x7F92F0); px(13, 3 + dy, 0x7F92F0); }
            case 11 -> { int dx = Math.round(sn(f * 4f) * 1.4f); bigA(5 + dx, 4, 0xFF8A8A); px(2, 6, 0xFF8A8A); px(13, 9, 0xFF8A8A); px(2, 11, 0xFF8A8A); }
            case 12 -> { bigA(5, 4, (x, y) -> mix(0xFFD84A, 0xFF4A1C, y / 6f)); for (int k = 0; k < 4; k++) { float u = (f + k / 4f) % 1f; pxa(5 + k * 1, Math.round(4 - u * 4), mix(0xFFD84A, 0xFF4A1C, u), 1f - u); } }
            case 13 -> { bigA(5, 4, 0x9FE8FF); px(2, 3, 0xFFFFFF); px(13, 12, 0xFFFFFF); if (f > 0.5f) { px(1, 3, 0xBFF4FF); px(3, 3, 0xBFF4FF); px(2, 2, 0xBFF4FF); px(2, 4, 0xBFF4FF); } }
            case 14 -> { bigA(5, 4, 0xFFE9A8); glint(3, 3, 2, 0xFFFFFF, t); glint(13, 12, 1, 0xFFFFFF, 1f - t); }
            case 15 -> bigA(5, 4, (x, y) -> mix(0xE8EEFF, 0x2A3350, ((x + y) % 3) / 2f * t));
            case 16 -> { int dy = Math.round(t * -4f); bigA(5, 6 + dy, 0xB8FFD0); for (int k = 0; k < 3; k++) pxa(6 + k, 13 + 0, 0xB8FFD0, 0.45f - k * 0.1f); }
            case 17 -> { if (t < 0.5f) bigA(5, 4, 0xFFB8D8); else letter(new String[]{".#.", "#.#", "###", "#.#"}, 6, 8, 0xFFB8D8, true); rect(3, 14, 10, 1, 0x4A5470); }
            case 18 -> { float k = t * 3.2f; for (int y = 0; y < 7; y++) for (int x = 0; x < 5; x++) if (BIG_A[y].charAt(x) == '#') px(Math.round(5 + x + (x - 2) * k), Math.round(4 + y + (y - 3) * k * 0.6f), 0xFFC857); }
            case 19 -> { int dx = Math.round((1f - t) * -6f); bigA(5 + dx, 4, 0x9AD8FF); for (int k = 1; k < 4; k++) pxa(5 + dx - k * 2, 7, 0x9AD8FF, 0.5f / k); }
            case 20 -> { float w = Math.max(0.2f, Math.abs((float) Math.cos(f * 3.14f))); for (int y = 0; y < 7; y++) for (int x = 0; x < 5; x++) if (BIG_A[y].charAt(x) == '#') px(Math.round(7.5f + (x - 2) * w), 4 + y, 0xE0B8FF); }
            case 21 -> { int sh = Math.round(s * 2f); for (int y = 0; y < 7; y++) for (int x = 0; x < 5; x++) if (BIG_A[y].charAt(x) == '#') px(5 + x + Math.round(sh * (y - 3) / 3f), 4 + y, 0xFFD0A0); ln(3, 2, 12, 2, 0x7F6A4A); }
            case 22 -> bigA(5, 4, (x, y) -> mix(0xE0A21B, 0xFFF0A0, 0.5f + 0.5f * (float) Math.sin(f * TAU - (x + y) * 0.8f)));
            case 23 -> { int dx = Math.round(t * 6f); bigA(5 + dx - 3, 4, 0xFFA8A8); for (int k = 1; k < 4; k++) pxa(2 + dx - k * 2 + 3, 7, 0xFFA8A8, 0.5f / k); }
            case 24 -> { letter(BIG_A, 5, 2, 0xA8F0A8, true); for (int k = 0; k < 5; k += 2) { int len = 2 + Math.round(t * (2 + k)); for (int j = 0; j < len; j++) pxa(5 + k, 9 + j, 0xA8F0A8, 1f - j * 0.15f); } }
            default -> { return false; }
        }
        outline();
        return true;
    }
}
