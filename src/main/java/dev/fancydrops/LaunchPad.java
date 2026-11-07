package dev.fancydrops;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The Active control above the menu: a little launch site. Off, the rocket stands docked on its pad. On, it lifts off (flame, smoke)
 * and climbs into orbit round a small planet, circling it, passing behind it. Switching plays the launch or the landing.
 */
final class LaunchPad {
    private LaunchPad() {}
    static boolean clip = true;                                             // (the bench draws without clipping)

    /** fly: 0 on the pad .. 1 in orbit; mv: how lit the control is; hv: hover; pulse: the border's slow breath. */
    static void draw(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h, boolean on, float fly, float mv, float hv, float pulse, long now, boolean still) {
        int bg = Canvas.mix(Canvas.mix(0xFF070816, 0xFF100E30, mv), 0xFF1A1748, hv * 0.8f);
        g.fill(x + 2, y, x + w - 2, y + h, bg); g.fill(x + 1, y + 1, x + w - 1, y + h - 1, bg); g.fill(x, y + 2, x + w, y + h - 2, bg);
        int bc = Gfx.argb(Math.round(80 + 60 * hv + 70 * pulse * mv), mv > 0.5f ? 0x9B7BFF : 0x4A4878);
        g.fill(x + 2, y, x + w - 2, y + 1, bc); g.fill(x + 2, y + h - 1, x + w - 2, y + h, bc); g.fill(x, y + 2, x + 1, y + h - 2, bc); g.fill(x + w - 1, y + 2, x + w, y + h - 2, bc);
        g.fill(x + 1, y + 1, x + 2, y + 2, bc); g.fill(x + w - 2, y + 1, x + w - 1, y + 2, bc); g.fill(x + 1, y + h - 2, x + 2, y + h - 1, bc); g.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, bc);
        if (clip) g.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);
        for (int i = 0; i < 12; i++) {                                                                   // the sky over the launch site
            int sx = x + 50 + (i * 23) % (w - 52), sy = y + 2 + (i * 7) % (h - 5);
            float tw = still ? 0.6f : 0.4f + 0.6f * Math.abs((float) Math.sin(now / (600.0 + i * 70) + i));
            g.fill(sx, sy, sx + 1, sy + 1, Gfx.argb(Math.round(150 * tw * (0.35f + 0.65f * mv)), 0xFFFFFF));
        }
        Gfx.text(g, font, "Active", x + 7, y + 3, Canvas.mix(0xFF8A88B0, Ui.TEXT, mv));
        String st = on ? (fly < 0.99f ? "LAUNCHING" : "IN ORBIT") : (fly > 0.01f ? "LANDING" : "DOCKED");
        Gfx.push(g); Gfx.translate(g, x + 7, y + 13); Gfx.scale(g, 0.6f); Gfx.text(g, font, st, 0, 0, on ? 0xFFC8B4FF : 0xFF7672AE); Gfx.pop(g);

        float pcx = x + w - 32, pcy = y + h / 2f, padX = x + w - 10, padY = y + h - 5;
        float oa = still ? 0.6f : now / 700f, ox = pcx + (float) Math.cos(oa) * 15, oy = pcy + (float) Math.sin(oa) * 5.5f;
        float sx0 = padX, sy0 = padY - 4;
        float rx = sx0 + (ox - sx0) * fly, ry = sy0 + (oy - sy0) * fly - (float) Math.sin(Math.PI * fly) * 7;
        float orbitAng = (float) Math.atan2(Math.cos(oa) * 5.5f, -Math.sin(oa) * 15), up = -(float) Math.PI / 2;
        float d = orbitAng - up; while (d > Math.PI) d -= 2 * Math.PI; while (d < -Math.PI) d += 2 * Math.PI;
        float ang = up + d * ease(Math.max(0f, (fly - 0.4f) / 0.6f));
        boolean behind = fly > 0.85f && Math.sin(oa) < 0;

        // the launch pad: a deck on two legs, a little tower with a blinking light
        g.fill(Math.round(padX - 5), Math.round(padY), Math.round(padX + 5), Math.round(padY) + 1, 0xFF9A9EC8);
        g.fill(Math.round(padX - 5), Math.round(padY) + 1, Math.round(padX + 5), Math.round(padY) + 2, 0xFF5A5E88);
        g.fill(Math.round(padX - 4), Math.round(padY) + 2, Math.round(padX - 3), y + h - 1, 0xFF3A3E68); g.fill(Math.round(padX + 2), Math.round(padY) + 2, Math.round(padX + 3), y + h - 1, 0xFF3A3E68);
        g.fill(Math.round(padX + 4), Math.round(padY) - 9, Math.round(padX + 5), Math.round(padY), 0xFF4A4E78);
        boolean blink = still || (now / 500) % 2 == 0;
        g.fill(Math.round(padX + 4), Math.round(padY) - 10, Math.round(padX + 5), Math.round(padY) - 9, blink ? (on ? 0xFF7AF0B0 : 0xFFFF5A6A) : 0xFF3A3E68);

        if (behind) rocket(g, rx, ry, ang, fly, now);
        NebulaTex.glow(g, pcx, pcy, 10, 10, Gfx.argb(Math.round(40 + 70 * mv), 0x9B7BFF));             // the planet, its ring
        g.fill(Math.round(pcx - 8), Math.round(pcy), Math.round(pcx + 9), Math.round(pcy) + 1, Gfx.argb(Math.round(90 + 60 * mv), 0xC8B4FF));
        NowPlayingHud.disc(g, pcx, pcy, 4.6f, 0xFF3E3090); NowPlayingHud.disc(g, pcx - 0.8f, pcy - 0.8f, 3.6f, Canvas.mix(0xFF5A4AA0, 0xFF8A6AE8, mv)); NowPlayingHud.disc(g, pcx - 1.7f, pcy - 1.7f, 1.2f, 0xAAFFFFFF);
        g.fill(Math.round(pcx - 8), Math.round(pcy), Math.round(pcx - 4), Math.round(pcy) + 1, Gfx.argb(Math.round(90 + 60 * mv), 0xC8B4FF));
        g.fill(Math.round(pcx + 5), Math.round(pcy), Math.round(pcx + 9), Math.round(pcy) + 1, Gfx.argb(Math.round(90 + 60 * mv), 0xC8B4FF));
        if (fly > 0.02f && fly < 0.9f) for (int i = 0; i < 4; i++) {                                    // smoke rolling off the pad
            float q = ((now / 450f) + i / 4f) % 1f, a = (1f - q) * Math.min(1f, (0.9f - fly) * 4f);
            NebulaTex.glow(g, padX + (i - 1.5f) * 3.5f * (0.6f + q), padY - q * 3, 2 + q * 3, 1.5f + q * 2, Gfx.argb(Math.round(120 * a), 0xC8C4E0));
        }
        if (!behind) rocket(g, rx, ry, ang, fly, now);
        if (clip) g.disableScissor();
    }
    /** The rocket, nose along +x before rotating: white body, pink nose and fins, a blue window, a flame while it flies. */
    private static void rocket(GuiGraphicsExtractor g, float rx, float ry, float ang, float fly, long now) {
        Gfx.push(g); Gfx.translate(g, rx, ry); Gfx.rotate(g, ang);
        if (fly > 0.02f) {
            float fl = 0.6f + 0.4f * (float) Math.sin(now / 45.0); int len = Math.round(2 + 3 * fl * (fly < 0.9f ? 1.7f : 0.8f));
            NebulaTex.glow(g, -4 - len / 2f, 0, len + 2, 2.5f, 0x66FF8A3A);
            g.fill(-3 - len, -1, -3, 1, 0xFFFF8A3A); g.fill(-3 - len / 2, -1, -3, 1, 0xFFFFE07A);
        }
        g.fill(-3, -1, 2, 1, 0xFFE8ECFF); g.fill(-2, -2, 1, -1, 0xFFC8CCE8); g.fill(-2, 1, 1, 2, 0xFFC8CCE8);
        g.fill(2, -1, 4, 1, 0xFFFF7AD9); g.fill(0, -1, 1, 0, 0xFF7AD8FF);
        g.fill(-4, -3, -2, -2, 0xFFFF7AD9); g.fill(-4, 2, -2, 3, 0xFFFF7AD9);
        Gfx.pop(g);
    }
    private static float ease(float t) { t = Math.max(0f, Math.min(1f, t)); return 1f - (1f - t) * (1f - t) * (1f - t); }
}
