package dev.fancydrops;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

/**
 * Now Playing (Visuals > Now Playing), placed wherever you drag it (Visuals > Move HUD): one clean glass card. The cover on the left (it
 * flips in when the song changes and dims under a pause sign when you pause), the title and artists, and a thin time bar along the bottom with the time on either side. A small equalizer sits top right. It takes its accent from the
 * album (in soft, cool colours), slides away when nothing is playing, waits under boss bars and hides during RNG animations.
 */
final class NowPlayingHud {
    private NowPlayingHud() {}

    private static float show; private static long last = System.currentTimeMillis();
    private static String lastId = ""; private static long changedAt;
    private static boolean lastPlayingSeen; private static long pausedSince;
    /** The demo shown while you move the HUD with nothing playing. */
    static boolean preview;
    /** Where it was last drawn (screen pixels), for the HUD editor. */
    static int bx0, by0, bx1, by1;

    /** From the HUD (not while the HUD editor draws its own). */
    static void hud(GuiGraphicsExtractor g) { if (!preview) render(g); }

    static void render(GuiGraphicsExtractor g) {
        FancyConfig c = FancyConfig.get(); Minecraft mc = Minecraft.getInstance();
        long now = System.currentTimeMillis(); float dt = Math.min(0.1f, (now - last) / 1000f); last = now;
        Spotify.Track t = preview && Spotify.track == null ? new Spotify.Track("demo", "Song title", "Artist", "", 200_000, "") : Spotify.track;
        boolean playing = preview || Spotify.playing;
        if (!Spotify.playing && lastPlayingSeen) pausedSince = now; lastPlayingSeen = Spotify.playing;
        long hideAfter = c.npHidePaused == 0 ? Long.MAX_VALUE : c.npHidePaused == 1 ? 8000 : 0;
        boolean want = preview || (c.npOn && Spotify.linked() && t != null && (Spotify.playing || now - pausedSince < hideAfter) && !AnimationManager.hidesHud() && mc.level != null);
        show += ((want ? 1f : 0f) - show) * Math.min(1f, dt * 8f);
        if (show < 0.01f || t == null) return;
        if (!t.id().equals(lastId)) { lastId = t.id(); changedAt = now; }
        int W = g.guiWidth(); float k = c.npScale / 100f, e = show * show * (3 - 2 * show);
        float cx = W * c.npX, h = height(false) * k;
        float top = below(mc, W, cx, width() * k, c.npY) - (1f - e) * (h + 14);
        long pos = preview && Spotify.track == null ? (now / 10) % 200_000 : Spotify.position();
        draw(g, mc.font, cx, top, k, e, t, now, now - changedAt, pos, playing, dt, "", AlbumArt.ready && !(preview && Spotify.track == null), AlbumArt.accent());
    }
    static int width() { return 226; }
    static int height(boolean lyric) { return lyric ? 64 : 56; }

    /** Draws the card. cx/top: the middle of its top edge; e: 0..1 how far it has slid in; since: ms since the song changed. */
    static void draw(GuiGraphicsExtractor g, Font font, float cx, float top, float k, float e, Spotify.Track t, long now, long since,
                     long pos, boolean playing, float dt, String lyric, boolean art, int acc) {
        int w = width(), h = height(!lyric.isEmpty());
        bx0 = Math.round(cx - w / 2f * k); by0 = Math.round(top); bx1 = Math.round(cx + w / 2f * k); by1 = Math.round(top + h * k);
        Gfx.push(g); Gfx.translate(g, cx - w / 2f * k, top); Gfx.scale(g, k);
        LyricsHud.dock(g, font, w, h, e, top > 26 * k, acc);                       // the sung line, sliding out from behind the card
        glass(g, font, w, h, e, t, now, since, pos, playing, lyric, art, acc);
        Gfx.pop(g);
    }

    /**
     * The card: dark glass washed with the album's colour from the left, a big cover with a record sliding out from behind it and
     * turning while the song plays, the title in white over the artist, a small equalizer, and a rounded progress bar with a glowing
     * knob and the times under its ends.
     */
    private static void glass(GuiGraphicsExtractor g, Font font, int w, int h, float e, Spotify.Track t, long now, long since, long pos, boolean playing, String lyric, boolean art, int acc) {
        int a = Math.round(255 * e), panel = 0x0D0E13;
        NebulaTex.glow(g, w / 2f, h / 2f + 6, w * 0.58f, h * 0.9f, Gfx.argb(Math.round(110 * e), 0x000000));               // a soft shadow under it
        round(g, 0, 0, w, h, Gfx.argb(Math.round(150 * e), 0x2A2C38));                                                  // a fine light edge
        round(g, 1, 1, w - 2, h - 2, Gfx.argb(Math.round(242 * e), panel));                                              // plain dark glass: no colour wash
        g.fill(4, 1, w - 4, 2, Gfx.argb(Math.round(22 * e), 0xFFFFFF));                                                  // a sheen along the top
        // the record, sliding out from behind the cover while it plays: fine grooves, the album's art printed on its spinning label,
        // two highlights that stay put while it turns (the way light sits on a real record), a thin bright rim
        int as = 42, ax = 7, ay = 7;
        rec += ((playing ? 1f : 0.3f) - rec) * 0.08f;
        float rx = ax + as / 2f + rec * 25, ry = ay + as / 2f, rr = as / 2f - 1, spin = (now % 3_600_000L) / 1000f * 3.5f * rec;
        disc(g, rx, ry, rr, Gfx.argb(a, 0x0A0A0D));
        circle(g, rx, ry, rr - 0.5f, Gfx.argb(Math.round(a * 0.5f), 0x3A3C46));
        for (int ring = 0; ring < 6; ring++) circle(g, rx, ry, rr * (0.46f + ring * 0.085f), Gfx.argb(Math.round(a * (ring % 2 == 0 ? 0.22f : 0.12f)), 0x8A8E9C));
        for (int side = 0; side < 2; side++) {                                                   // the highlights, fixed: light, not paint
            double an = -Math.PI / 4 + side * Math.PI; float hx = rx + (float) Math.cos(an) * rr * 0.68f, hy = ry + (float) Math.sin(an) * rr * 0.68f;
            Gfx.push(g); Gfx.translate(g, hx, hy); Gfx.rotate(g, (float) an + (float) Math.PI / 2); NebulaTex.glow(g, 0, 0, rr * 0.42f, rr * 0.08f, Gfx.argb(Math.round(a * 0.28f), 0xFFFFFF)); Gfx.pop(g);
        }
        float lr = rr * 0.38f;
        disc(g, rx, ry, lr, Gfx.argb(a, Ritual.mixc(acc, 0x1A1A20, 0.4f)));
        Gfx.push(g); Gfx.translate(g, rx, ry); Gfx.rotate(g, spin); float ls = lr * 1.3f / as; Gfx.scale(g, ls, ls); Gfx.translate(g, -as / 2f, -as / 2f);
        cover(g, font, 0, 0, as, a, art, acc);                                                   // the label: the album's art, turning
        Gfx.pop(g);
        circle(g, rx, ry, lr, Gfx.argb(Math.round(a * 0.6f), 0x0A0A0D));
        disc(g, rx, ry, 1.2f, Gfx.argb(a, 0xD8DCE6)); disc(g, rx, ry, 0.6f, Gfx.argb(a, 0x0A0A0D));                  // the spindle
        NebulaTex.glow(g, ax + as + 1, ay + as / 2f, 4, as * 0.5f, Gfx.argb(Math.round(a * 0.55f), 0x000000));      // the cover's shadow falling on it
        // the cover: a shadow under it, it flips in when the song changes, dims under a pause sign when paused
        NebulaTex.glow(g, ax + as / 2f + 1, ay + as / 2f + 3, as * 0.62f, as * 0.6f, Gfx.argb(Math.round(e * 150), 0x000000));
        float flip = Math.min(1f, since / 400f), sx = Math.abs((float) Math.cos(Math.PI * 0.5 * (1f - flip)));
        Gfx.push(g); Gfx.translate(g, ax + as / 2f, ay); Gfx.scale(g, Math.max(0.02f, sx), 1f); Gfx.translate(g, -as / 2f, 0);
        cover(g, font, 0, 0, as, a, art, acc);
        for (int[] q : new int[][]{{0, 0}, {as - 1, 0}, {0, as - 1}, {as - 1, as - 1}}) g.fill(q[0], q[1], q[0] + 1, q[1] + 1, Gfx.argb(Math.round(242 * e), panel));   // rounded corners
        g.fill(1, 0, as - 1, 1, Gfx.argb(Math.round(a * 0.18f), 0xFFFFFF));
        if (!playing) { g.fill(0, 0, as, as, Gfx.argb(Math.round(130 * e), 0x000000)); g.fill(as / 2 - 5, as / 2 - 6, as / 2 - 2, as / 2 + 6, Gfx.argb(a, 0xFFFFFF)); g.fill(as / 2 + 2, as / 2 - 6, as / 2 + 5, as / 2 + 6, Gfx.argb(a, 0xFFFFFF)); }
        Gfx.pop(g);
        // the words
        float in = ease(Math.min(1f, since / 380f)), sl = (1f - in) * 8; int tx = 84, eqW = 16, tw = w - tx - eqW - 10;
        small(g, font, playing ? "NOW PLAYING" : "PAUSED", tx + sl * 0.4f, 8, 0.55f, Gfx.argb(Math.round(a * 0.75f * in), 0x8A8E9C));
        Gfx.txt(g, font, marquee(font, t.name(), tw, since), Math.round(tx + sl), 15, Gfx.argb(Math.round(a * in), 0xFFFFFF), true);
        small(g, font, cut(font, t.artists(), tw / 0.8f), tx + sl * 0.6f, 26, 0.8f, Gfx.argb(Math.round(a * 0.7f * in), 0xA8ACB8));
        // a small equalizer, top right, in white
        int ex = w - eqW - 7, ey = 15;
        for (int i = 0; i < 5; i++) {
            float amp = playing ? 0.2f + 0.8f * Math.abs((float) Math.sin(now / (150.0 + i * 37) + i * 1.7) * (float) Math.sin(now / (380.0 + i * 53) + i * 0.6)) : 0.08f;
            int bh = Math.max(1, Math.round(8 * amp)), bx = ex + i * 3;
            g.fill(bx, ey - bh, bx + 2, ey, Gfx.argb(Math.round(a * 0.7f), 0xFFFFFF));
        }
        // time: a thin white bar with a small knob, the times under its ends
        long dur = Math.max(1, t.durationMs()); float f = Math.min(1f, pos / (float) dur);
        int by = h - 13, b0 = tx, b1 = w - 9, kx = Math.round(b0 + (b1 - b0) * f);
        g.fill(b0, by, b1, by + 2, Gfx.argb(Math.round(a * 0.12f), 0xFFFFFF));
        g.fill(b0, by, kx, by + 2, Gfx.argb(Math.round(a * 0.9f), 0xFFFFFF));
        NebulaTex.glow(g, kx, by + 1, 4, 3, Gfx.argb(Math.round(a * 0.35f), 0xFFFFFF));
        g.fill(kx - 1, by - 1, kx + 2, by + 3, Gfx.argb(a, 0xFFFFFF));
        String l = mmss(pos), r = "-" + mmss(Math.max(0, dur - pos)); float rw = font.width(r) * 0.55f;
        small(g, font, l, b0, by + 4, 0.55f, Gfx.argb(Math.round(a * 0.5f), 0xFFFFFF));
        small(g, font, r, b1 - rw, by + 4, 0.55f, Gfx.argb(Math.round(a * 0.5f), 0xFFFFFF));
    }
    private static float rec = 0f;
    static void disc(GuiGraphicsExtractor g, float cx, float cy, float r, int c) {
        for (int y = Math.round(-r); y <= Math.round(r); y++) { float hw = (float) Math.sqrt(Math.max(0, r * r - y * y)); g.fill(Math.round(cx - hw), Math.round(cy + y), Math.round(cx + hw), Math.round(cy + y) + 1, c); }
    }
    static void circle(GuiGraphicsExtractor g, float cx, float cy, float r, int c) {
        int n = Math.max(12, Math.round(r * 4)); for (int i = 0; i < n; i++) { double an = i * Math.PI * 2 / n; int x = Math.round(cx + (float) Math.cos(an) * r), y = Math.round(cy + (float) Math.sin(an) * r); g.fill(x, y, x + 1, y + 1, c); }
    }

    // ------------------------------------------------------------------ bits
    private static void cover(GuiGraphicsExtractor g, Font font, int x, int y, int s, int a, boolean art, int acc) {
        if (art) g.blit(RenderPipelines.GUI_TEXTURED, AlbumArt.ID, x, y, 0f, 0f, s, s, 64, 64, 64, 64, Gfx.argb(a, 0xFFFFFF));
        else { g.fill(x, y, x + s, y + s, Gfx.argb(a, Ritual.mixc(acc, 0x101018, 0.55f))); small(g, font, "♫", x + s / 2f - 3 * s / 20f, y + s / 2f - 4 * s / 20f, s / 20f, Gfx.argb(a, 0xFFFFFF)); }
    }
    private static java.lang.reflect.Field bossEvents; private static boolean bossTried; private static float bossPush;
    /** Boss bars come first: when the card sits where they are drawn (top, middle) it slides down below the last one. */
    private static float below(Minecraft mc, int W, float cx, float w, float y) {
        int n = 0;
        try {
            Object ov = mc.gui.hud.getBossOverlay();
            if (!bossTried) { bossTried = true; for (var f : ov.getClass().getDeclaredFields()) if (java.util.Map.class.isAssignableFrom(f.getType())) { f.setAccessible(true); bossEvents = f; } }
            if (bossEvents != null) n = ((java.util.Map<?, ?>) bossEvents.get(ov)).size();
        } catch (Throwable ignored) { }
        float want = 0;
        if (n > 0 && Math.abs(cx - W / 2f) < 91 + w / 2f) want = Math.max(0, Math.min(19 * n + 3, mc.getWindow().getGuiScaledHeight() / 3f + 8) - y);
        bossPush += (want - bossPush) * 0.15f;
        return y + bossPush;
    }
    private static void small(GuiGraphicsExtractor g, Font f, String s, float x, float y, float k, int c) { Gfx.push(g); Gfx.translate(g, x, y); Gfx.scale(g, k); Gfx.txt(g, f, s, 0, 0, c, false); Gfx.pop(g); }
    private static String cut(Font f, String s, float maxW) { return f.width(s) <= maxW ? s : f.plainSubstrByWidth(s, Math.round(maxW) - f.width("…")) + "…"; }
    /** A long title scrolls a letter at a time, pausing at both ends. */
    private static String marquee(Font font, String title, int maxW, long since) {
        if (font.width(title) <= maxW) return title;
        int extra = 0; while (extra < title.length() && font.width(title.substring(extra)) > maxW) extra++;
        int per = extra + 12, u = (int) ((since / 220L) % (per * 2));
        int off = u < 6 ? 0 : u < per ? Math.min(extra, u - 6) : u < per + 6 ? extra : Math.max(0, extra - (u - per - 6));
        return font.plainSubstrByWidth(title.substring(off), maxW);
    }
    private static float ease(float v) { return 1f - (1f - v) * (1f - v) * (1f - v); }
    private static String mmss(long ms) { long s = ms / 1000; return s / 60 + ":" + (s % 60 < 10 ? "0" : "") + s % 60; }
    static void round(GuiGraphicsExtractor g, int x, int y, int w, int h, int c) {
        g.fill(x + 2, y, x + w - 2, y + 1, c); g.fill(x + 1, y + 1, x + w - 1, y + 2, c); g.fill(x, y + 2, x + w, y + h - 2, c); g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, c); g.fill(x + 2, y + h - 1, x + w - 2, y + h, c);
    }
}
