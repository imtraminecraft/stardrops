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
        LyricsHud.dock(g, font, w, h, e, top > 26 * k, acc);                       // the sung line, sliding out from behind the card
        glass(g, font, w, h, e, t, now, since, pos, playing, lyric, art, acc);
        Gfx.pop(g);
    }

    /**
     * The card: dark glass washed with the album's colour from the left, a big cover with a record sliding out from behind it and
     * turning while the song plays, the title in white over the artist, a small equalizer, and a rounded progress bar with a glowing
     * knob and the times under its ends.
     */
    private static final int V = 0x9B7BFF, PINK = 0xFF7AD9, STAR = 0xEEF0FF;
    /**
     * Now Playing, Stardrops style: a little window into space. Stars drift past behind the glass, a moon circles the album cover, and
     * the song's progress is a rocket flying from the start of a dotted course to a ringed planet, its exhaust trail behind it. When
     * the song changes the rocket blasts off: the stars stretch into warp streaks, the rocket fires off the right edge, the new song
     * slides in and the rocket flies back in from the left to where the new song is.
     */
    private static void glass(GuiGraphicsExtractor g, Font font, int w, int h, float e, Spotify.Track t, long now, long since, long pos, boolean playing, String lyric, boolean art, int acc) {
        int a = Math.round(255 * e);
        float warp = since < 1100 ? (float) Math.sin(Math.PI * Math.min(1f, since / 1100f)) : 0f;                    // the jump between songs
        NebulaTex.glow(g, w / 2f, h / 2f + 6, w * 0.58f, h * 0.9f, Gfx.argb(Math.round(120 * e), 0x000000));
        round(g, 0, 0, w, h, Gfx.argb(Math.round(170 * e), 0x2C2A66));
        round(g, 1, 1, w - 2, h - 2, Gfx.argb(Math.round(240 * e), 0x0B0C26));
        NebulaTex.glow(g, w * 0.82f, h * 0.35f, w * 0.3f, h * 0.7f, Gfx.argb(Math.round(26 * e), PINK));            // faint nebula behind the glass
        NebulaTex.glow(g, w * 0.55f, h * 0.9f, w * 0.35f, h * 0.5f, Gfx.argb(Math.round(22 * e), V));
        float drift = (now % 3_600_000L) / 1000f;
        for (int i = 0; i < 26; i++) {                                                            // stars drifting past (streaks while warping)
            float sp = 3f + 9f * hash(i * 7), x = 3 + ((hash(i * 13) * (w - 6) - drift * sp * (1f + warp * 30f)) % (w - 6) + (w - 6)) % (w - 6), y = 3 + hash(i * 29) * (h - 6);
            float tw = 0.4f + 0.6f * Math.abs((float) Math.sin(drift * (1 + hash(i)) + i)), len = 1 + warp * 18 * (0.5f + hash(i * 3));
            len = Math.min(len, w - 3 - x); if (len < 1) continue;
            g.fill(Math.round(x), Math.round(y), Math.round(x + len), Math.round(y) + 1, Gfx.argb(Math.round(a * (0.25f + 0.35f * tw + 0.3f * warp)), i % 6 == 0 ? 0xD8C8FF : STAR));
        }
        g.fill(4, 1, w - 4, 2, Gfx.argb(Math.round(26 * e), 0xFFFFFF));
        // the cover, with a violet rim and a moon circling it (behind it on the far side of its orbit)
        int as = 40, ax = 8, ay = 8; float ocx = ax + as / 2f, ocy = ay + as / 2f, mo = drift * 1.1f;
        float mx = ocx + (float) Math.cos(mo) * 27, my = ocy + (float) Math.sin(mo) * 7; boolean behind = Math.sin(mo) < 0;
        if (behind) disc(g, mx, my, 2.2f, Gfx.argb(Math.round(a * 0.7f), 0xC8C4E8));
        NebulaTex.glow(g, ocx, ocy + 2, as * 0.7f, as * 0.7f, Gfx.argb(Math.round(e * 140), 0x000000));
        NebulaTex.glow(g, ocx, ocy, as * 0.62f, as * 0.62f, Gfx.argb(Math.round(e * 60), V));
        float flip = Math.min(1f, since / 400f), sx = Math.abs((float) Math.cos(Math.PI * 0.5 * (1f - flip)));
        Gfx.push(g); Gfx.translate(g, ax + as / 2f, ay); Gfx.scale(g, Math.max(0.02f, sx), 1f); Gfx.translate(g, -as / 2f, 0);
        cover(g, font, 0, 0, as, a, art, acc);
        for (int[] q : new int[][]{{0, 0}, {as - 1, 0}, {0, as - 1}, {as - 1, as - 1}}) g.fill(q[0], q[1], q[0] + 1, q[1] + 1, Gfx.argb(Math.round(240 * e), 0x0B0C26));
        if (!playing) { g.fill(0, 0, as, as, Gfx.argb(Math.round(130 * e), 0x000000)); g.fill(as / 2 - 5, as / 2 - 6, as / 2 - 2, as / 2 + 6, Gfx.argb(a, 0xFFFFFF)); g.fill(as / 2 + 2, as / 2 - 6, as / 2 + 5, as / 2 + 6, Gfx.argb(a, 0xFFFFFF)); }
        Gfx.pop(g);
        rout1(g, ax - 1, ay - 1, as + 2, as + 2, Gfx.argb(Math.round(a * 0.55f), V));
        if (!behind) { disc(g, mx, my, 2.2f, Gfx.argb(a, 0xE8E4FF)); NebulaTex.glow(g, mx, my, 4, 4, Gfx.argb(Math.round(a * 0.3f), 0xFFFFFF)); }
        // the words
        float in = ease(Math.min(1f, Math.max(0f, since - 250) / 380f)), sl = (1f - in) * 14; int tx = 58, eqW = 16, tw = w - tx - eqW - 10;
        small(g, font, playing ? "NOW PLAYING" : "PAUSED", tx + sl * 0.4f, 8, 0.55f, Gfx.argb(Math.round(a * 0.85f * in), 0xA89CE8));
        Gfx.txt(g, font, marquee(font, t.name(), tw, since), Math.round(tx + sl), 15, Gfx.argb(Math.round(a * in), 0xFFFFFF), true);
        small(g, font, cut(font, t.artists(), tw / 0.8f), tx + sl * 0.6f, 26, 0.8f, Gfx.argb(Math.round(a * 0.75f * in), 0xB9B6E6));
        int ex = w - eqW - 7, ey = 15;                                                            // the signal: five bars, violet into pink
        for (int i = 0; i < 5; i++) {
            float amp = playing ? 0.2f + 0.8f * Math.abs((float) Math.sin(now / (150.0 + i * 37) + i * 1.7) * (float) Math.sin(now / (380.0 + i * 53) + i * 0.6)) : 0.08f;
            int bh = Math.max(1, Math.round(8 * amp)), bx = ex + i * 3;
            g.fill(bx, ey - bh, bx + 2, ey, Gfx.argb(Math.round(a * 0.9f), Ritual.mixc(V, PINK, i / 4f)));
        }
        // the course: a dotted path to a ringed planet, the exhaust trail behind the rocket
        long dur = Math.max(1, t.durationMs()); float f = Math.min(1f, pos / (float) dur);
        int by = h - 13, b0 = tx + 2, b1 = w - 20; float rxp = b0 + (b1 - b0) * f;
        if (since < 1100) {                                                                       // blast off and fly back in
            float u = since / 1100f;
            rxp = u < 0.4f ? b0 + (b1 - b0) * f + (w + 30 - (b0 + (b1 - b0) * f)) * (float) Math.pow(u / 0.4f, 2.2) : -20 + (b0 + (b1 - b0) * f + 20) * ease((u - 0.4f) / 0.6f);
        }
        for (int x = b0; x < b1; x += 3) if (x > rxp + 6) g.fill(x, by, x + 1, by + 1, Gfx.argb(Math.round(a * 0.35f), 0xB9B6E6));
        float tail0 = Math.max(b0, rxp - Math.max(18, (b1 - b0) * 0.6f));
        for (int i = 0; i < 16; i++) {                                                            // the trail: pink near the rocket, violet, then gone
            float u0 = i / 16f, xa = rxp - 3 - (rxp - 3 - tail0) * (u0 + 1f / 16f), xb = rxp - 3 - (rxp - 3 - tail0) * u0; if (xb <= b0) continue;
            g.fill(Math.round(Math.max(b0, xa)), by, Math.round(xb), by + 1, Gfx.argb(Math.round(a * 0.85f * (1f - u0)), Ritual.mixc(PINK, V, u0)));
        }
        planet(g, b1 + 7, by + 0.5f, a);
        rocket(g, rxp, by + 0.5f, a, now, playing || since < 1100, since < 1100 ? 1f + warp * 2f : 1f);
        String l = mmss(pos), r = "-" + mmss(Math.max(0, dur - pos)); float rw = font.width(r) * 0.55f;
        small(g, font, l, b0, by + 4, 0.55f, Gfx.argb(Math.round(a * 0.55f), 0xFFFFFF));
        small(g, font, r, b1 - rw, by + 4, 0.55f, Gfx.argb(Math.round(a * 0.55f), 0xFFFFFF));
        if (since < 500) { float fl = 1f - since / 500f; NebulaTex.glow(g, w - 4, by, 18, 10, Gfx.argb(Math.round(a * 0.6f * fl * Math.min(1f, since / 120f)), PINK)); }   // where the rocket left
    }
    /** A little rocket pointing right, its nose at x+5: white body, a round window, pink fins, a flickering flame (longer with boost). */
    private static void rocket(GuiGraphicsExtractor g, float x, float y, int a, long now, boolean firing, float boost) {
        int cx = Math.round(x), cy = Math.round(y);
        if (firing) {
            float fl = 0.7f + 0.3f * (float) Math.sin(now / 45.0) * (float) Math.sin(now / 17.0); int len = Math.round((3 + 2 * fl) * boost);
            NebulaTex.glow(g, cx - 5 - len / 2f, cy, 3 + len * 0.6f, 3, Gfx.argb(Math.round(a * 0.5f), 0xFF9A4A));
            g.fill(cx - 4 - len, cy - 1, cx - 4, cy + 1, Gfx.argb(a, 0xFF8A3A)); g.fill(cx - 4 - Math.round(len * 0.6f), cy, cx - 4, cy + 1, Gfx.argb(a, 0xFFE07A)); g.fill(cx - 4 - Math.round(len * 0.6f), cy - 1, cx - 4, cy, Gfx.argb(a, 0xFFD060));
        }
        g.fill(cx - 4, cy - 2, cx + 3, cy + 2, Gfx.argb(a, 0xE8ECFF));                             // body
        g.fill(cx + 3, cy - 1, cx + 5, cy + 1, Gfx.argb(a, 0xE8ECFF)); g.fill(cx + 5, cy - 1, cx + 6, cy + 1, Gfx.argb(a, PINK));   // nose cone
        g.fill(cx - 4, cy - 2, cx + 3, cy - 1, Gfx.argb(a, 0xFFFFFF));
        g.fill(cx, cy - 1, cx + 2, cy + 1, Gfx.argb(a, 0x7AD8FF));                                 // window
        g.fill(cx - 5, cy - 4, cx - 2, cy - 2, Gfx.argb(a, PINK)); g.fill(cx - 5, cy + 2, cx - 2, cy + 4, Gfx.argb(a, PINK));        // fins
        g.fill(cx - 4, cy + 1, cx + 3, cy + 2, Gfx.argb(a, 0xB8B4D8));
    }
    /** The destination: a small ringed planet at the end of the course. */
    private static void planet(GuiGraphicsExtractor g, float x, float y, int a) {
        NebulaTex.glow(g, x, y, 7, 7, Gfx.argb(Math.round(a * 0.35f), V));
        disc(g, x, y, 3.5f, Gfx.argb(a, 0x5A3FAA)); disc(g, x - 0.8f, y - 0.8f, 2.6f, Gfx.argb(a, 0x8A6AE8)); disc(g, x - 1.4f, y - 1.4f, 1f, Gfx.argb(Math.round(a * 0.7f), 0xFFFFFF));
        g.fill(Math.round(x - 5), Math.round(y), Math.round(x + 6), Math.round(y) + 1, Gfx.argb(Math.round(a * 0.8f), 0xFFB8EC));
    }
    private static void rout1(GuiGraphicsExtractor g, int x, int y, int w, int h, int c) { g.fill(x + 1, y, x + w - 1, y + 1, c); g.fill(x + 1, y + h - 1, x + w - 1, y + h, c); g.fill(x, y + 1, x + 1, y + h - 1, c); g.fill(x + w - 1, y + 1, x + w, y + h - 1, c); }
    private static float hash(int i) { int h = i * 374761393 + 668265263; h = (h ^ (h >>> 13)) * 1274126177; return ((h ^ (h >>> 16)) & 0xFFFF) / 65535f; }
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
