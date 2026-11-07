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
    static int width() { return 214; }
    static int height(boolean lyric) { return lyric ? 64 : 56; }

    /** Draws the card. cx/top: the middle of its top edge; e: 0..1 how far it has slid in; since: ms since the song changed. */
    static void draw(GuiGraphicsExtractor g, Font font, float cx, float top, float k, float e, Spotify.Track t, long now, long since,
                     long pos, boolean playing, float dt, String lyric, boolean art, int acc) {
        int w = width(), h = height(!lyric.isEmpty());
        bx0 = Math.round(cx - w / 2f * k); by0 = Math.round(top); bx1 = Math.round(cx + w / 2f * k); by1 = Math.round(top + h * k);
        Gfx.push(g); Gfx.translate(g, cx - w / 2f * k, top); Gfx.scale(g, k);
        glass(g, font, w, h, e, t, now, since, pos, playing, lyric, art, acc);
        Gfx.pop(g);
    }

    /**
     * The card: dark glass washed with the album's colour from the left, a big cover with a record sliding out from behind it and
     * turning while the song plays, the title in white over the artist, a small equalizer, and a rounded progress bar with a glowing
     * knob and the times under its ends.
     */
    private static void glass(GuiGraphicsExtractor g, Font font, int w, int h, float e, Spotify.Track t, long now, long since, long pos, boolean playing, String lyric, boolean art, int acc) {
        int a = Math.round(255 * e), panel = 0x0B0D14;
        NebulaTex.glow(g, w / 2f, h / 2f + 5, w * 0.58f, h * 0.95f, Gfx.argb(Math.round(90 * e), 0x000000));                // the shadow under it
        round(g, 0, 0, w, h, Gfx.argb(Math.round(232 * e), panel));
        for (int i = 0; i < 12; i++) {                                                                                 // the album's colour washing in from the left
            int x0 = 2 + i * (w - 4) / 12, x1 = 2 + (i + 1) * (w - 4) / 12; float u = i / 11f;
            g.fill(x0, 1, x1, h - 1, Gfx.argb(Math.round(e * 70 * (1f - u) * (1f - u)), acc));
        }
        round(g, 0, 0, w, h / 2, Gfx.argb(Math.round(12 * e), 0xFFFFFF));
        g.fill(3, 0, w - 3, 1, Gfx.argb(Math.round(45 * e), 0xFFFFFF));                                               // a lit top edge
        g.fill(3, h - 1, w - 3, h, Gfx.argb(Math.round(60 * e), Ritual.mixc(acc, 0x000000, 0.4f)));
        // the record, sliding out from behind the cover while it plays
        int as = 42, ax = 7, ay = 7;
        float slide = playing ? Math.min(1f, (since + 300) / 700f) : 0.35f; rec += ((playing ? 1f : 0.35f) - rec) * 0.08f;
        float rx = ax + as / 2f + rec * 12, ry = ay + as / 2f, rr = as / 2f - 2, spin = (now % 3_600_000L) / 1000f * (playing ? 2.4f : 0f);
        disc(g, rx, ry, rr, Gfx.argb(a, 0x101014));
        for (int ring = 0; ring < 3; ring++) circle(g, rx, ry, rr * (0.55f + ring * 0.15f), Gfx.argb(Math.round(a * 0.25f), 0x5A5E70));
        disc(g, rx, ry, rr * 0.3f, Gfx.argb(a, acc)); disc(g, rx, ry, 1.2f, Gfx.argb(a, 0x101014));
        float gx = rx + (float) Math.cos(spin) * rr * 0.8f, gy = ry + (float) Math.sin(spin) * rr * 0.8f;                   // the shine going round
        NebulaTex.glow(g, gx, gy, 4, 2.5f, Gfx.argb(Math.round(a * 0.35f), 0xFFFFFF));
        // the cover (it flips in when the song changes, dims under a pause sign when paused)
        float flip = Math.min(1f, since / 400f), sx = Math.abs((float) Math.cos(Math.PI * 0.5 * (1f - flip)));
        NebulaTex.glow(g, ax + as / 2f, ay + as / 2f + 3, as * 0.65f, as * 0.6f, Gfx.argb(Math.round(e * 110), acc));
        Gfx.push(g); Gfx.translate(g, ax + as / 2f, ay); Gfx.scale(g, Math.max(0.02f, sx), 1f); Gfx.translate(g, -as / 2f, 0);
        cover(g, font, 0, 0, as, a, art, acc);
        g.fill(0, 0, as, 1, Gfx.argb(Math.round(a * 0.35f), 0xFFFFFF)); g.fill(0, 0, 1, as, Gfx.argb(Math.round(a * 0.2f), 0xFFFFFF));
        for (int[] p : new int[][]{{0, 0}, {as - 1, 0}, {0, as - 1}, {as - 1, as - 1}}) g.fill(p[0], p[1], p[0] + 1, p[1] + 1, Gfx.argb(Math.round(232 * e), panel));
        if (!playing) { g.fill(0, 0, as, as, Gfx.argb(Math.round(130 * e), 0x000000)); g.fill(as / 2 - 5, as / 2 - 6, as / 2 - 2, as / 2 + 6, Gfx.argb(a, 0xFFFFFF)); g.fill(as / 2 + 2, as / 2 - 6, as / 2 + 5, as / 2 + 6, Gfx.argb(a, 0xFFFFFF)); }
        Gfx.pop(g);
        // the words
        float in = ease(Math.min(1f, since / 380f)), sl = (1f - in) * 8; int tx = 70, eqW = 16, tw = w - tx - eqW - 10;
        small(g, font, playing ? "NOW PLAYING" : "PAUSED", tx + sl * 0.4f, 8, 0.55f, Gfx.argb(Math.round(a * 0.8f * in), Ritual.mixc(acc, 0xFFFFFF, 0.35f)));
        Gfx.txt(g, font, marquee(font, t.name(), tw, since), Math.round(tx + sl), 15, Gfx.argb(Math.round(a * in), 0xFFFFFF), true);
        small(g, font, cut(font, t.artists(), tw / 0.8f), tx + sl * 0.6f, 26, 0.8f, Gfx.argb(Math.round(a * 0.62f * in), 0xC6CAD6));
        if (!lyric.isEmpty()) small(g, font, cut(font, lyric, (w - tx - 8) / 0.75f), tx, 36, 0.75f, Gfx.argb(Math.round(a * 0.9f), Ritual.mixc(acc, 0xFFFFFF, 0.4f)));
        // a small equalizer, top right
        int ex = w - eqW - 7, ey = 15;
        for (int i = 0; i < 5; i++) {
            float amp = playing ? 0.2f + 0.8f * Math.abs((float) Math.sin(now / (150.0 + i * 37) + i * 1.7) * (float) Math.sin(now / (380.0 + i * 53) + i * 0.6)) : 0.08f;
            int bh = Math.max(1, Math.round(8 * amp)), bx = ex + i * 3;
            g.fill(bx, ey - bh, bx + 2, ey, Gfx.argb(Math.round(a * 0.9f), Ritual.mixc(acc, 0xFFFFFF, 0.2f * i / 4f)));
        }
        // time: a rounded bar with a glowing knob, the times under its ends
        long dur = Math.max(1, t.durationMs()); float f = Math.min(1f, pos / (float) dur);
        int by = h - 13 - (lyric.isEmpty() ? 0 : 0), b0 = tx, b1 = w - 9, kx = Math.round(b0 + (b1 - b0) * f);
        g.fill(b0, by, b1, by + 2, Gfx.argb(Math.round(a * 0.16f), 0xFFFFFF));
        g.fill(b0, by, kx, by + 2, Gfx.argb(a, Ritual.mixc(acc, 0xFFFFFF, 0.15f)));
        NebulaTex.glow(g, kx, by + 1, 5, 4, Gfx.argb(Math.round(a * 0.6f), acc));
        g.fill(kx - 1, by - 1, kx + 2, by + 3, Gfx.argb(a, 0xFFFFFF));
        String l = mmss(pos), r = "-" + mmss(Math.max(0, dur - pos)); float rw = font.width(r) * 0.55f;
        small(g, font, l, b0, by + 4, 0.55f, Gfx.argb(Math.round(a * 0.55f), 0xFFFFFF));
        small(g, font, r, b1 - rw, by + 4, 0.55f, Gfx.argb(Math.round(a * 0.55f), 0xFFFFFF));
    }
    private static float rec = 0f;
    private static void disc(GuiGraphicsExtractor g, float cx, float cy, float r, int c) {
        for (int y = Math.round(-r); y <= Math.round(r); y++) { float hw = (float) Math.sqrt(Math.max(0, r * r - y * y)); g.fill(Math.round(cx - hw), Math.round(cy + y), Math.round(cx + hw), Math.round(cy + y) + 1, c); }
    }
    private static void circle(GuiGraphicsExtractor g, float cx, float cy, float r, int c) {
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
