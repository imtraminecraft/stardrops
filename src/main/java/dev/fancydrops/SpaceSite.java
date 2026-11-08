package dev.fancydrops;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The WIP page, Stardrops style: a space station under construction. Deep space and a slow planet below; a half-built ring
 * station turning, its finished modules lit, the empty ones only scaffolding; a crane arm swinging the next module into place;
 * an astronaut on a tether welding (sparks fly); a cargo shuttle drifting in to dock with its lights blinking. Hazard tape stays
 * along the edges (FancyScreen draws it). Everything is plain fills and a few soft glows: cheap to draw every frame.
 */
final class SpaceSite {
    private SpaceSite() {}
    private static final int STEEL = 0xFF8A90A8, STEEL_D = 0xFF4A5068, HAZ = 0xFFFFC830, GLASS = 0xFF7AD8FF;

    static void draw(GuiGraphicsExtractor g, int x, int y, int w, int h, float T) {
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF06071A);
        // stars, twinkling
        for (int i = 0; i < 70; i++) {
            int sx = x + 4 + (int) (rnd(i * 7 + 1) * (w - 8)), sy = y + 10 + (int) (rnd(i * 13 + 2) * (h - 20));
            float tw = 0.4f + 0.6f * Math.abs((float) Math.sin(T * (0.8f + rnd(i) * 1.5f) + i));
            g.fill(sx, sy, sx + 1, sy + 1, Gfx.argb(Math.round(200 * tw), i % 9 == 0 ? 0xFFE0B0 : i % 5 == 0 ? 0xC8D8FF : 0xFFFFFF));
        }
        NebulaTex.glow(g, x + w * 0.75f, y + h * 0.35f, w * 0.3f, h * 0.5f, Gfx.argb(40, 0x9B7BFF));
        // a planet below, its curve filling the bottom, slow bands drifting
        float pcx = x + w * 0.5f, pcy = y + h + w * 0.55f, pr = w * 0.62f;
        NebulaTex.glow(g, pcx, pcy - pr, w * 0.7f, 18, Gfx.argb(90, 0x6AA8FF));
        for (int yy = (int) (pcy - pr); yy < y + h - 1; yy++) {
            float dy = yy - pcy; float hw = (float) Math.sqrt(Math.max(0, pr * pr - dy * dy)); if (hw < 1) continue;
            int x0 = (int) Math.max(x + 1, pcx - hw), x1 = (int) Math.min(x + w - 1, pcx + hw);
            float band = (float) Math.sin((yy - y) * 0.45f + T * 0.3f) * 0.5f + 0.5f;
            g.fill(x0, yy, x1, yy + 1, 0xFF000000 | Canvas.mix(0xFF1A3A6A, 0xFF2A5A8A, band * 0.6f + (yy == (int) (pcy - pr) ? 0.4f : 0f)) & 0xFFFFFF | 0xFF000000);
        }
        // the station: a ring, half built, turning slowly; its hub in the middle
        float scx = x + w * 0.4f, scy = y + h * 0.46f, R = Math.min(w * 0.24f, h * 0.42f);
        int seg = 16; float rot = T * 0.12f;
        for (int i = 0; i < seg; i++) {
            float an = rot + i * 6.2832f / seg, ax = scx + (float) Math.cos(an) * R, ay = scy + (float) Math.sin(an) * R * 0.38f;
            boolean built = i < 10, front = Math.sin(an) > 0;
            int col = built ? (front ? STEEL : STEEL_D) : 0;
            if (built) { g.fill(Math.round(ax - 3), Math.round(ay - 2), Math.round(ax + 3), Math.round(ay + 2), col); if (i % 2 == 0 && front) g.fill(Math.round(ax - 1), Math.round(ay - 1), Math.round(ax + 1), Math.round(ay), (((int) (T * 2) + i) % 3 == 0) ? 0xFFFFE08A : GLASS); }
            else { int c = Gfx.argb(front ? 200 : 110, 0xFFC830); g.fill(Math.round(ax - 3), Math.round(ay - 2), Math.round(ax + 3), Math.round(ay - 1), c); g.fill(Math.round(ax - 3), Math.round(ay + 1), Math.round(ax + 3), Math.round(ay + 2), c);
                   g.fill(Math.round(ax - 3), Math.round(ay - 2), Math.round(ax - 2), Math.round(ay + 2), c); g.fill(Math.round(ax + 2), Math.round(ay - 2), Math.round(ax + 3), Math.round(ay + 2), c); }   // only scaffolding yet
        }
        for (int k = 0; k < 4; k++) { float an = rot + k * 1.5708f; line(g, scx, scy, scx + (float) Math.cos(an) * R, scy + (float) Math.sin(an) * R * 0.38f, 0xFF5A6080); }   // spokes
        g.fill(Math.round(scx - 5), Math.round(scy - 4), Math.round(scx + 5), Math.round(scy + 4), STEEL); g.fill(Math.round(scx - 3), Math.round(scy - 2), Math.round(scx + 3), Math.round(scy), GLASS);
        g.fill(Math.round(scx - 14), Math.round(scy - 1), Math.round(scx - 5), Math.round(scy + 1), STEEL_D); g.fill(Math.round(scx - 22), Math.round(scy - 4), Math.round(scx - 14), Math.round(scy + 4), 0xFF3A6AC8);   // a solar panel
        g.fill(Math.round(scx + 5), Math.round(scy - 1), Math.round(scx + 14), Math.round(scy + 1), STEEL_D); g.fill(Math.round(scx + 14), Math.round(scy - 4), Math.round(scx + 22), Math.round(scy + 4), 0xFF3A6AC8);
        if (((int) (T * 1.5f)) % 2 == 0) { g.fill(Math.round(scx), Math.round(scy - 8), Math.round(scx + 1), Math.round(scy - 4), STEEL_D); g.fill(Math.round(scx), Math.round(scy - 9), Math.round(scx + 1), Math.round(scy - 8), 0xFFFF5A6A); }
        // the crane: an arm from the hub swinging a module out to the next empty slot and back
        float sw = 0.5f - 0.5f * (float) Math.cos(T * 0.5f), slotAn = rot + 10 * 6.2832f / seg;
        float tx = scx + (float) Math.cos(slotAn) * R, ty = scy + (float) Math.sin(slotAn) * R * 0.38f - 10;
        float mx = scx + (tx - scx) * sw, my = scy - 16 + (ty - (scy - 16)) * sw;
        line(g, scx, scy - 4, scx, scy - 16, STEEL); line(g, scx, scy - 16, mx, my, HAZ); line(g, mx, my, mx, my + 5, 0xFF9A9EC8);
        g.fill(Math.round(mx - 3), Math.round(my + 5), Math.round(mx + 3), Math.round(my + 9), STEEL);
        // the astronaut on a tether, welding at the last built module; sparks fly when the torch is on
        float wa = rot + 9 * 6.2832f / seg, wx = scx + (float) Math.cos(wa) * R, wy = scy + (float) Math.sin(wa) * R * 0.38f;
        float ax = wx + 9 + (float) Math.sin(T * 0.9f) * 2, ay = wy + 7 + (float) Math.cos(T * 0.7f) * 1.5f;
        for (int i = 0; i <= 8; i++) { float u = i / 8f; float lx = scx + (ax - scx) * u, ly = scy + (ay - scy) * u + (float) Math.sin(u * Math.PI) * 6; g.fill(Math.round(lx), Math.round(ly), Math.round(lx) + 1, Math.round(ly) + 1, 0xFF6A6E88); }   // the tether, sagging
        g.fill(Math.round(ax - 2), Math.round(ay - 3), Math.round(ax + 2), Math.round(ay + 1), 0xFFE8ECFF); g.fill(Math.round(ax - 1), Math.round(ay - 2), Math.round(ax + 2), Math.round(ay), 0xFFFFB04A);   // helmet, gold visor
        g.fill(Math.round(ax - 2), Math.round(ay + 1), Math.round(ax + 2), Math.round(ay + 5), 0xFFE8ECFF); g.fill(Math.round(ax - 3), Math.round(ay + 1), Math.round(ax - 2), Math.round(ay + 4), 0xFFC8CCE8);
        g.fill(Math.round(ax - 2), Math.round(ay + 5), Math.round(ax), Math.round(ay + 7), 0xFFC8CCE8); g.fill(Math.round(ax + 1), Math.round(ay + 5), Math.round(ax + 2), Math.round(ay + 7), 0xFFC8CCE8);
        boolean torch = ((int) (T * 3)) % 4 != 0;
        float tx2 = wx + 3, ty2 = wy + 1; line(g, ax - 2, ay + 2, tx2, ty2, 0xFF9A9EC8);
        if (torch) {
            NebulaTex.glow(g, tx2, ty2, 6, 6, Gfx.argb(200, 0xBFE8FF)); g.fill(Math.round(tx2), Math.round(ty2), Math.round(tx2) + 1, Math.round(ty2) + 1, 0xFFFFFFFF);
            for (int s = 0; s < 6; s++) { float ph = (T * 2.5f + s / 6f) % 1f, an = rnd(s * 9 + (int) (T * 2.5f)) * 6.2832f, d = ph * 9;
                g.fill(Math.round(tx2 + (float) Math.cos(an) * d), Math.round(ty2 + (float) Math.sin(an) * d + ph * ph * 6), Math.round(tx2 + (float) Math.cos(an) * d) + 1, Math.round(ty2 + (float) Math.sin(an) * d + ph * ph * 6) + 1, Gfx.argb(Math.round(255 * (1 - ph)), s % 2 == 0 ? 0xFFE07A : 0xFFFFFF)); }
        }
        // the cargo shuttle: drifting in from the right, docking, waiting, leaving (a 14 s loop)
        float cyc = (T % 14f) / 14f, sx;
        if (cyc < 0.35f) sx = x + w + 10 - (x + w + 10 - (scx + R + 26)) * ease(cyc / 0.35f);
        else if (cyc < 0.7f) sx = scx + R + 26;
        else sx = scx + R + 26 + (x + w + 20 - (scx + R + 26)) * ease((cyc - 0.7f) / 0.3f) * ease((cyc - 0.7f) / 0.3f);
        float sy = scy + 2 + (float) Math.sin(T * 1.3f);
        if (sx < x + w - 2) {
            g.fill(Math.round(sx - 6), Math.round(sy - 3), Math.round(sx + 8), Math.round(sy + 3), 0xFFE8ECFF); g.fill(Math.round(sx - 9), Math.round(sy - 1), Math.round(sx - 6), Math.round(sy + 2), 0xFFC8CCE8);
            g.fill(Math.round(sx - 1), Math.round(sy - 2), Math.round(sx + 6), Math.round(sy + 2), HAZ); g.fill(Math.round(sx + 8), Math.round(sy - 2), Math.round(sx + 11), Math.round(sy + 2), GLASS);
            boolean blink = ((int) (T * 2)) % 2 == 0; g.fill(Math.round(sx - 6), Math.round(sy - 4), Math.round(sx - 5), Math.round(sy - 3), blink ? 0xFFFF5A6A : 0xFF5A2A2A);
            if (cyc < 0.35f || cyc > 0.7f) NebulaTex.glow(g, sx + 13, sy, 4, 2, Gfx.argb(180, 0x7AD8FF));     // thrusters
        }
    }
    private static void line(GuiGraphicsExtractor g, float x0, float y0, float x1, float y1, int c) {
        int n = (int) Math.max(1, Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)));
        for (int i = 0; i <= n; i++) { float u = i / (float) n; int px = Math.round(x0 + (x1 - x0) * u), py = Math.round(y0 + (y1 - y0) * u); g.fill(px, py, px + 1, py + 1, c); }
    }
    private static float ease(float t) { t = Math.max(0, Math.min(1, t)); return t * t * (3 - 2 * t); }
    private static float rnd(int i) { int h = i * 374761393 + 668265263; h = (h ^ (h >>> 13)) * 1274126177; return ((h ^ (h >>> 16)) & 0xFFFF) / 65535f; }
}
