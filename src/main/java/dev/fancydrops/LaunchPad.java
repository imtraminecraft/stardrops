package dev.fancydrops;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The Active control above the menu: a window onto a launch. Off, the rocket stands on its pad on the ground under a dusk sky,
 * "DOCKED". Switching on launches it: the flame lights, smoke rolls off the pad, it climbs and the view follows it up: the ground
 * drops away, the sky darkens into space, the stars streak past and then settle, a planet rises into view and the rocket swings
 * into orbit round it, "IN ORBIT". Switching off plays it all backwards: down out of orbit, through the sky, onto the pad.
 */
final class LaunchPad {
    private LaunchPad() {}
    static boolean clip = true;                                             // (the bench draws without clipping)

    /** fly: 0 on the pad .. 1 in orbit; mv: how lit the control is; hv: hover; pulse: the border's slow breath. */
    static void draw(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h, boolean on, float fly, float mv, float hv, float pulse, long now, boolean still) {
        int bg = Canvas.mix(Canvas.mix(0xFF070816, 0xFF100E30, mv), 0xFF1A1748, hv * 0.8f);
        g.fill(x + 2, y, x + w - 2, y + h, bg); g.fill(x + 1, y + 1, x + w - 1, y + h - 1, bg); g.fill(x, y + 2, x + w, y + h - 2, bg);
        Gfx.text(g, font, "Active", x + 7, y + 3, Canvas.mix(0xFF8A88B0, Ui.TEXT, mv));
        String st = on ? (fly < 0.6f ? "LAUNCHING" : fly < 0.99f ? "ARRIVING" : "IN ORBIT") : (fly > 0.01f ? "LANDING" : "DOCKED");
        Gfx.push(g); Gfx.translate(g, x + 7, y + 13); Gfx.scale(g, 0.55f); Gfx.text(g, font, st, 0, 0, on ? 0xFFC8B4FF : 0xFF7672AE); Gfx.pop(g);

        // the window: the right half of the control
        int wx0 = x + 49, wx1 = x + w - 2, wy0 = y + 2, wy1 = y + h - 2, ww = wx1 - wx0, wh = wy1 - wy0;
        if (clip) g.enableScissor(wx0, wy0, wx1, wy1);
        float rise = smooth(Math.min(1f, fly / 0.6f)), climb = smooth(Math.max(0f, (rise - 0.2f) / 0.8f)), arrive = ease(Math.max(0f, (fly - 0.55f) / 0.45f));   // up off the pad first, then the camera follows
        // the sky: dusk violet near the ground, darkening to space as it climbs
        for (int row = 0; row < wh; row += 2) {
            float v = row / (float) wh;
            int dusk = Ritual.mixc(0x1A1240, 0x5A3A8A, v * v), space = 0x05061A;
            g.fill(wx0, wy0 + row, wx1, wy0 + row + 2, 0xFF000000 | Ritual.mixc(dusk, space, climb));
        }
        // stars: faint over the dusk, bright in space; streaking down while it climbs
        float speed = (float) Math.sin(Math.PI * Math.min(1f, fly / 0.75f));
        for (int i = 0; i < 16; i++) {
            float sx = wx0 + 1 + rnd(i * 7) * (ww - 2), base = rnd(i * 13) * wh;
            float sy = still ? base : (base + (now / 1000f) * 40f * speed * (0.6f + rnd(i))) % wh;
            float tw = still ? 0.7f : 0.4f + 0.6f * Math.abs((float) Math.sin(now / (600.0 + i * 70) + i));
            int al = Math.round(200 * tw * (0.25f + 0.75f * climb) * (0.5f + 0.5f * mv)); int len = Math.max(1, Math.round(speed * 5));
            g.fill(Math.round(sx), wy0 + Math.round(sy) - len + 1, Math.round(sx) + 1, wy0 + Math.round(sy) + 1, Gfx.argb(al, 0xFFFFFF));
        }
        // the ground: hills and the pad, dropping out of view as the camera follows the rocket up
        float drop = climb * (wh + 6);
        int gy = Math.round(wy1 - 4 + drop);
        float padX = wx0 + 14;
        if (gy < wy1 + 2) {
            for (int i = 0; i < ww; i++) { int hh = 2 + Math.round(2.5f * (float) Math.sin(i * 0.31f) + 1.5f * (float) Math.sin(i * 0.77f + 1)); g.fill(wx0 + i, gy - Math.max(0, hh - 1), wx0 + i + 1, wy1 + 8, 0xFF141030); }
            g.fill(wx0, gy + 1, wx1, wy1 + 8, 0xFF0C0A22);
            g.fill(Math.round(padX - 5), gy - 1, Math.round(padX + 6), gy, 0xFF9A9EC8);                                   // the pad
            g.fill(Math.round(padX + 5), gy - 10, Math.round(padX + 6), gy - 1, 0xFF4A4E78);                              // the tower
            boolean blink = still || (now / 500) % 2 == 0;
            g.fill(Math.round(padX + 5), gy - 11, Math.round(padX + 6), gy - 10, blink ? (on ? 0xFF7AF0B0 : 0xFFFF5A6A) : 0xFF3A3E68);
        }
        // the planet rising into view
        float pcx = wx1 - 15, pcy = wy0 - 14 + (wh / 2f + 14) * arrive;
        float oa = still ? 0.6f : now / 650f;
        float ox = pcx + (float) Math.cos(oa) * 12, oy = pcy + (float) Math.sin(oa) * 4f;
        // the rocket: up off the pad, the camera holding it mid-window, then across into orbit
        float rx = padX, ry = wy1 - 9 - wh * 0.3f * Math.min(1f, rise * 4f);
        float ang = -(float) Math.PI / 2;
        boolean behind = false;
        if (arrive > 0f) {
            float oang = (float) Math.atan2(Math.cos(oa) * 4f, -Math.sin(oa) * 12);
            rx += (ox - rx) * arrive; ry += (oy - ry) * arrive; ang = lerpAng(ang, oang, arrive); behind = arrive > 0.7f && Math.sin(oa) < 0;
        }
        if (fly > 0.01f && climb < 1f && gy < wy1 + 2) for (int i = 0; i < 4; i++) {                       // smoke rolling off the pad
            float q = ((now / 450f) + i / 4f) % 1f;
            NebulaTex.glow(g, padX + (i - 1.5f) * 3.5f * (0.6f + q), gy - 1 - q * 3, 2 + q * 3, 1.5f + q * 2, Gfx.argb(Math.round(120 * (1f - q) * (1f - climb)), 0xC8C4E0));
        }
        boolean firing = fly > 0.01f && fly < 0.97f;
        if (behind) rocket(g, rx, ry, ang, firing, now, 1f - arrive * 0.5f);
        if (arrive > 0f) Worlds.draw(g, pcx, pcy, 5.5f, Math.round(255 * Math.min(1f, arrive * 2f) * (0.6f + 0.4f * mv)), 16, still ? 0f : now / 1000f);
        if (!behind) rocket(g, rx, ry, ang, firing, now, arrive > 0.5f ? 0.7f : 1.4f);
        if (clip) g.disableScissor();
        // the frame over everything
        int bc = Gfx.argb(Math.round(80 + 60 * hv + 70 * pulse * mv), mv > 0.5f ? 0x9B7BFF : 0x4A4878);
        g.fill(wx0 - 1, wy0, wx0, wy1, Gfx.argb(Math.round(60 + 40 * mv), 0x2C2A66));
        g.fill(x + 2, y, x + w - 2, y + 1, bc); g.fill(x + 2, y + h - 1, x + w - 2, y + h, bc); g.fill(x, y + 2, x + 1, y + h - 2, bc); g.fill(x + w - 1, y + 2, x + w, y + h - 2, bc);
        g.fill(x + 1, y + 1, x + 2, y + 2, bc); g.fill(x + w - 2, y + 1, x + w - 1, y + 2, bc); g.fill(x + 1, y + h - 2, x + 2, y + h - 1, bc); g.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, bc);
    }
    /** The rocket, nose along +x before rotating: white body, pink nose and fins, a blue window, a flame while it flies. */
    private static void rocket(GuiGraphicsExtractor g, float rx, float ry, float ang, boolean firing, long now, float boost) {
        Gfx.push(g); Gfx.translate(g, rx, ry); Gfx.rotate(g, ang);
        if (firing) {
            float fl = 0.6f + 0.4f * (float) Math.sin(now / 45.0); int len = Math.round((2 + 3 * fl) * boost);
            NebulaTex.glow(g, -4 - len / 2f, 0, len + 2, 2.5f, 0x66FF8A3A);
            g.fill(-3 - len, -1, -3, 1, 0xFFFF8A3A); g.fill(-3 - len / 2, -1, -3, 1, 0xFFFFE07A);
        }
        g.fill(-3, -1, 2, 1, 0xFFE8ECFF); g.fill(-2, -2, 1, -1, 0xFFC8CCE8); g.fill(-2, 1, 1, 2, 0xFFC8CCE8);
        g.fill(2, -1, 4, 1, 0xFFFF7AD9); g.fill(0, -1, 1, 0, 0xFF7AD8FF);
        g.fill(-4, -3, -2, -2, 0xFFFF7AD9); g.fill(-4, 2, -2, 3, 0xFFFF7AD9);
        Gfx.pop(g);
    }
    private static float lerpAng(float a0, float a1, float u) { float d = a1 - a0; while (d > Math.PI) d -= (float) (2 * Math.PI); while (d < -Math.PI) d += (float) (2 * Math.PI); return a0 + d * u; }
    private static float smooth(float t) { t = Math.max(0f, Math.min(1f, t)); return t * t * (3 - 2 * t); }
    private static float ease(float t) { t = Math.max(0f, Math.min(1f, t)); return 1f - (1f - t) * (1f - t) * (1f - t); }
    private static float rnd(int i) { int h = i * 374761393 + 668265263; h = (h ^ (h >>> 13)) * 1274126177; return ((h ^ (h >>> 16)) & 0xFFFF) / 65535f; }
}
