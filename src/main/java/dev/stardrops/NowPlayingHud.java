package dev.stardrops;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

/**
 * Now Playing (Visuals > Now Playing), placed wherever you drag it (Visuals > Move HUD): one clean glass card. The cover on the left (it
 * flips in when the song changes and dims under a pause sign when you pause), the title and artists (two as "A & B", more as the first and a "+n" pill), the time under the voyage. A small equalizer sits top right. It takes its accent from the
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
        StardropsConfig c = StardropsConfig.get(); Minecraft mc = Minecraft.getInstance();
        long now = System.currentTimeMillis(); float dt = Math.min(0.1f, (now - last) / 1000f); last = now;
        Spotify.Track t = preview && Spotify.track == null ? new Spotify.Track("demo", "Song title", "Artist", "", 200_000, "") : Spotify.track;
        boolean playing = preview || Spotify.playing;
        if (!Spotify.playing && lastPlayingSeen) pausedSince = now; lastPlayingSeen = Spotify.playing;
        long hideAfter = c.npHidePaused == 0 ? Long.MAX_VALUE : c.npHidePaused == 1 ? 8000 : 0;
        if (t != null && !t.id().equals(lastId)) { lastId = t.id(); changedAt = now; peekAt = now; }
        if (Spotify.playing != peekPlaying) { peekPlaying = Spotify.playing; peekAt = now; }               // Peek: it slides in when the song changes, or you pause or play, then away again
        boolean want = preview || (c.npOn && (!c.npPeek || now - peekAt < PEEK_MS) && (Spotify.linked() || LocalMusic.active) && t != null && (Spotify.playing || now - pausedSince < hideAfter) && !AnimationManager.hidesHud() && mc.level != null);
        show += ((want ? 1f : 0f) - show) * Math.min(1f, dt * 8f);
        if (show < 0.01f || t == null) return;
        int W = g.guiWidth(); float k = c.npScale / 100f, e = show * show * (3 - 2 * show);
        float cx = W * c.npX, h = height(false) * k;
        float top = below(mc, W, cx, width() * k, c.npY) - (1f - e) * (h + 14);
        long pos = preview && Spotify.track == null ? (now / 10) % 200_000 : Spotify.position();
        draw(g, mc.font, cx, top, k, e, t, now, now - changedAt, pos, playing, dt, "", AlbumArt.ready && !(preview && Spotify.track == null), AlbumArt.accent());
    }
    private static long peekAt; private static boolean peekPlaying; static final long PEEK_MS = 9000;
    /** The menu's preview: the real card, as it is on your screen (your song, cover, colours and settings), fitted into the box. */
    static void drawIn(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h) {
        Spotify.Track t = Spotify.track != null ? Spotify.track : new Spotify.Track("demo", "Song title", "Artist", "", 200_000, "");
        boolean demo = Spotify.track == null; long now = System.currentTimeMillis();
        float k = Math.min((h - 6) / (float) height(false), (w - 8) / (float) width()) * StardropsConfig.get().npScale / 100f;
        k = Math.min(k, Math.min((h - 6) / (float) height(false), (w - 8) / (float) width()));
        long pos = demo ? (now / 10) % 200_000 : Spotify.position();
        draw(g, font, x + w / 2f, y + (h - height(false) * k) / 2f, k, 1f, t, now, now - changedAt, pos, demo || Spotify.playing, 0.016f, "", AlbumArt.ready && !demo, AlbumArt.accent());
    }
    static int width() { return 214; }
    /** How solid the glass is (Visuals > Music > Now Playing > Background), 0.2..0.7. */
    static float bg() { return StardropsConfig.get().npBg / 100f; }
    static int height(boolean lyric) { return lyric ? 66 : 58; }

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
        int a = Math.round(255 * e); float op = bg();
        float warp = 0f;                                                                                            // (no warp streaks: the voyage pans instead)
        NebulaTex.glow(g, w / 2f, h / 2f + 9, w * 0.6f, h * 0.95f, Gfx.argb(Math.round(150 * e), 0x000000));          // depth: it floats above the game, its shadow falling below
        round(g, 2, 3, w - 4, h, Gfx.argb(Math.round(70 * e), 0x000000));
        round(g, 0, 0, w, h, Gfx.argb(Math.round((70 + 100 * op) * e), 0x2C2A66));
        round(g, 1, 1, w - 2, h - 2, Gfx.argb(Math.round(255 * e * op), 0x0B0C26));
        NebulaTex.glow(g, w * 0.82f, h * 0.35f, w * 0.3f, h * 0.7f, Gfx.argb(Math.round(26 * e), PINK));            // faint nebula behind the glass
        NebulaTex.glow(g, w * 0.55f, h * 0.9f, w * 0.35f, h * 0.5f, Gfx.argb(Math.round(22 * e), V));
        float drift = (now % 3_600_000L) / 1000f;
        stars(g, w, h, a, drift, 0);                                                              // the far stars, behind the world
        // far behind: a huge world rising out of the corner, lit from the upper left (a thin bright rim, a dark body, a soft atmosphere)
        float gx = w - 30 + (float) Math.sin(drift * 0.05f) * 3, gy = h + 14; int wc = Ritual.mixc(acc, V, 0.5f);
        cap(g, gx, gy, 36, w, h, Gfx.argb(Math.round(18 * e), wc));
        cap(g, gx, gy, 34, w, h, Gfx.argb(Math.round(80 * e), Ritual.mixc(wc, 0xFFFFFF, 0.35f)));
        cap(g, gx + 2.5f, gy + 3f, 33, w, h, Gfx.argb(Math.round(225 * e * Math.max(0.6f, op)), Ritual.mixc(wc, 0x07081A, 0.78f)));
        cap(g, gx + 6f, gy + 7f, 30, w, h, Gfx.argb(Math.round(60 * e), 0x000000));                 // its night side
        stars(g, w, h, a, drift, 1); stars(g, w, h, a, drift, 2);                                 // the nearer stars pass in front of it, faster
        // a bevel: light along the top of the glass, shade along the bottom
        for (int j = 0; j < 7; j++) g.fill(3, 2 + j, w - 3, 3 + j, Gfx.argb(Math.round(16 * e * (1f - j / 7f)), 0xFFFFFF));
        for (int j = 0; j < 9; j++) g.fill(3, h - 3 - j, w - 3, h - 2 - j, Gfx.argb(Math.round(55 * e * (1f - j / 9f)), 0x000000));
        float[][] cst = {{0.36f, 0.14f}, {0.44f, 0.3f}, {0.53f, 0.2f}, {0.6f, 0.36f}, {0.7f, 0.16f}};
        for (int i = 0; i < cst.length; i++) {
            float px = cst[i][0] * w, py = cst[i][1] * h; g.fill(Math.round(px), Math.round(py), Math.round(px) + 1, Math.round(py) + 1, Gfx.argb(Math.round(a * (0.35f + 0.25f * (float) Math.sin(drift * 1.3f + i * 1.9f))), 0xFFFFFF));
        }
        float cu = (drift % 11f) / 1.2f; if (cu < 1f) { float hx = w * (0.95f - 0.6f * cu), hy = 4 + 16 * cu; for (int q = 0; q < 9; q++) dot(g, hx + q * 2.2f, hy - q * 0.6f, Gfx.argb(Math.round(a * (float) Math.sin(Math.PI * cu) * (1f - q / 9f) * 0.8f), q == 0 ? 0xFFFFFF : 0xC8B4FF)); }
        // the cover, with a violet rim and a moon circling it (behind it on the far side of its orbit)
        int as = 40, ax = 8, ay = 8; float ocx = ax + as / 2f, ocy = ay + as / 2f, mo = drift * 1.1f;
        float mx = ocx + (float) Math.cos(mo) * 23, my = ocy + (float) Math.sin(mo) * 6; boolean behind = Math.sin(mo) < 0;
        if (behind) disc(g, mx, my, 2.2f, Gfx.argb(Math.round(a * 0.7f), 0xC8C4E8));
        long fnow = System.nanoTime(); float fdt = lastDraw == 0 ? 0f : Math.min(0.1f, (fnow - lastDraw) / 1e9f); lastDraw = fnow; live += ((playing ? 1f : 0f) - live) * Math.min(1f, fdt * 5f);
        float vr = as / 2f - 3f, vx = ax + as - 9 + 9 * ease(live), spinA = drift * 3.2f;                // the record: out a little while it plays, back in when paused
        disc(g, vx, ocy, vr, Gfx.argb(a, 0x0C0C14)); circle(g, vx, ocy, vr, Gfx.argb(Math.round(a * 0.5f), 0x3A3A52)); circle(g, vx, ocy, vr - 4, Gfx.argb(Math.round(a * 0.2f), 0x8A88B0));
        for (int k = 0; k < 2; k++) for (int q = -3; q <= 3; q++) { double an = spinA + k * Math.PI + q * 0.11; dot(g, vx + (float) Math.cos(an) * (vr - 2.5f), ocy + (float) Math.sin(an) * (vr - 2.5f), Gfx.argb(Math.round(a * 0.45f * (1f - Math.abs(q) / 4f)), 0xFFFFFF)); }
        NebulaTex.glow(g, ocx, ocy + 2, as * 0.7f, as * 0.7f, Gfx.argb(Math.round(e * 140), 0x000000));
        NebulaTex.glow(g, ocx, ocy, as * 0.62f, as * 0.62f, Gfx.argb(Math.round(e * 60), V));
        g.fill(ax + 2, ay + 3, ax + as + 2, ay + as + 3, Gfx.argb(Math.round(110 * e), 0x000000));           // the cover's own shadow: it sits above the glass
        g.fill(ax + 1, ay + as + 3, ax + as + 3, ay + as + 4, Gfx.argb(Math.round(45 * e), 0x000000));
        float flip = Math.min(1f, since / 400f), sx = Math.abs((float) Math.cos(Math.PI * 0.5 * (1f - flip)));
        Gfx.push(g); Gfx.translate(g, ax + as / 2f, ay); Gfx.scale(g, Math.max(0.02f, sx), 1f); Gfx.translate(g, -as / 2f, 0);
        cover(g, font, 0, 0, as, a, art, acc);
        for (int[] q : new int[][]{{0, 0}, {as - 1, 0}, {0, as - 1}, {as - 1, as - 1}}) g.fill(q[0], q[1], q[0] + 1, q[1] + 1, Gfx.argb(Math.round(255 * e * op), 0x0B0C26));
        float sh = (drift % 8f) / 0.9f; if (sh < 1f && playing) { int sxp = Math.round(-6 + (as + 12) * sh); for (int k = 0; k < 5; k++) { int q0 = Math.max(0, sxp + k), q1 = Math.min(as, sxp + k + 1); if (q1 > q0) g.fill(q0, 0, q1, as, Gfx.argb(Math.round(46 * e * (1f - Math.abs(k - 2) / 3f)), 0xFFFFFF)); } }   // a shine passes over it
        if (!playing) { g.fill(0, 0, as, as, Gfx.argb(Math.round(130 * e), 0x000000)); g.fill(as / 2 - 5, as / 2 - 6, as / 2 - 2, as / 2 + 6, Gfx.argb(a, 0xFFFFFF)); g.fill(as / 2 + 2, as / 2 - 6, as / 2 + 5, as / 2 + 6, Gfx.argb(a, 0xFFFFFF)); }
        Gfx.pop(g);
        rout1(g, ax - 1, ay - 1, as + 2, as + 2, Gfx.argb(Math.round(a * 0.55f), V));
        g.fill(ax, ay, ax + as, ay + 1, Gfx.argb(Math.round(a * 0.35f), 0xFFFFFF)); g.fill(ax, ay + 1, ax + 1, ay + as, Gfx.argb(Math.round(a * 0.18f), 0xFFFFFF));   // light catches its top and left edges
        if (!behind) { disc(g, mx, my, 2.2f, Gfx.argb(a, 0xE8E4FF)); NebulaTex.glow(g, mx, my, 4, 4, Gfx.argb(Math.round(a * 0.3f), 0xFFFFFF)); }
        // the words
        float in = ease(Math.min(1f, Math.max(0f, since - 250) / 380f)), sl = (1f - in) * 14; int tx = 58, eqW = 16, tw = w - tx - eqW - 10;
        boolean mine = t.id().startsWith("local:");
        small(g, font, playing ? (mine ? "YOUR MUSIC" : "NOW PLAYING") : "PAUSED", tx + sl * 0.4f, 8, 0.55f, Gfx.argb(Math.round(a * 0.85f * in), mine ? 0x8AF0C0 : 0xA89CE8));
        Gfx.txt(g, font, marquee(font, t.name(), tw, since), Math.round(tx + sl), 15, Gfx.argb(Math.round(a * in), 0xFFFFFF), true);
        artists(g, font, t.artists(), tx + sl * 0.6f, 26, tw, Math.round(a * in));
        int ex = w - eqW - 7, ey = 15;                                                            // the signal: five bars, violet into pink
        for (int i = 0; i < 5; i++) {
            float amp = playing ? 0.2f + 0.8f * Math.abs((float) Math.sin(now / (150.0 + i * 37) + i * 1.7) * (float) Math.sin(now / (380.0 + i * 53) + i * 0.6)) : 0.08f;
            int bh = Math.max(1, Math.round(8 * amp)), bx = ex + i * 3;
            g.fill(bx, ey - bh, bx + 2, ey, Gfx.argb(Math.round(a * 0.9f), Ritual.mixc(V, PINK, i / 4f)));
        }
        String left = "-" + mmss(Math.max(0, t.durationMs() - pos)); small(g, font, left, w - 7 - font.width(left) * 0.5f, 19, 0.5f, Gfx.argb(Math.round(a * 0.6f), 0xB9B6E6));   // what is left, under the signal
        course(g, font, w, h, tx, a, t, now, since, pos, playing, warp);
    }

    /**
     * The artists. One: as it is. Two that fit: "A & B". Otherwise the first artist, and a small pill with how many more
     * ("+2"), so a long list never ends in a cut-off word.
     */
    private static void artists(GuiGraphicsExtractor g, Font f, String all, float x, float y, int maxW, int a) {
        int col = Gfx.argb(Math.round(a * 0.75f), 0xB9B6E6); float k = 0.8f, room = maxW / k;
        String[] p = all.split(", "); 
        if (p.length == 1) { small(g, f, cut(f, all, room), x, y, k, col); return; }
        String two = p[0] + " & " + p[1];
        if (p.length == 2 && f.width(two) <= room) { small(g, f, two, x, y, k, col); return; }
        String more = "+" + (p.length - 1); float pw = f.width(more) * 0.6f + 6;
        String first = cut(f, p[0], room - (pw + 4) / k); float fw = f.width(first) * k;
        small(g, f, first, x, y, k, col);
        int bx = Math.round(x + fw + 4), by = Math.round(y - 1);
        round(g, bx, by, Math.round(pw), 8, Gfx.argb(Math.round(a * 0.35f), 0x6A5AC8));
        small(g, f, more, bx + 3, by + 2, 0.6f, Gfx.argb(Math.round(a * 0.9f), 0xE8E4FF));
    }
    /*
     * The voyage. Every song is a world (picked from the track's id): the rocket sets off from the last song's world on the left and
     * flies an arc, up and over, to this song's world on the right. Ahead of it the course is dotted; behind it, a fading trail. In
     * the last seconds it falls into orbit round the world it reached. When the next song starts, the view pans: the world it is
     * circling slides over to the left and becomes the departure, a new world drifts in from the right, and the rocket sets off again.
     */
    private static String curId, prevId, prev2Id; private static float live; private static long lastDraw;
    private static final long PAN = 1300;
    private static int seed(String id) { return id == null ? 7 : id.hashCode(); }
    private static void course(GuiGraphicsExtractor g, Font font, int w, int h, int tx, int a, Spotify.Track t, long now, long since, long pos, boolean playing, float warp) {
        if (!t.id().equals(curId)) { prev2Id = prevId == null ? t.id() + "~" : prevId; prevId = curId == null ? t.id() + "*" : curId; curId = t.id(); }
        long dur = Math.max(1, t.durationMs()); float f = Math.min(1f, pos / (float) dur), spin = (now % 3_600_000L) / 1000f;
        int yb = h - 12; float x0 = tx + 7, x1 = w - 17, D = x1 - x0, r0 = 5f, r1 = 6.5f;
        float pan = since < PAN ? ease(since / (float) PAN) : 1f, off = (1f - pan) * D;
        float dx = x0 + off, dX = x1 + off, ox = x0 - D + off;                                     // the departure, the destination, the one left behind
        // the arc: from just off the departure world, up and over, to just short of the destination
        float sx = dx + r0 + 2, sy = yb - 3, ex = dX - r1 - 3, ey = yb - 3, cxp = (sx + ex) / 2f, cyp = yb - 15;                     // a low arc: its top stays under the artist line
        float show = Math.max(0f, Math.min(1f, (since - PAN * 0.7f) / 400f));                   // the course appears once the pan settles
        float end = Math.max(0f, Math.min(1f, 1f - (dur - pos) / 3000f)), orbit = ease(end);
        float tail = Math.max(0f, f - 0.3f);
        int N = 90;
        for (int i = 0; i <= N; i++) {
            float u = i / (float) N, px = bz(sx, cxp, ex, u), py = bz(sy, cyp, ey, u);
            if (u > f + 0.03f) { if (i % 3 == 0) dot(g, px, py, Gfx.argb(Math.round(a * 0.4f * show), 0xB9B6E6)); }
            else if (u >= tail && u <= f) { float q = (f - u) / Math.max(0.001f, f - tail); dot(g, px, py, Gfx.argb(Math.round(a * 0.85f * (1f - q) * show * (1f - orbit)), Ritual.mixc(PINK, V, q))); }
        }
        // the worlds (faded at the card's edges as they pan in and out)
        Worlds.draw(g, ox, yb, r0, Math.round(a * edge(ox, tx, w)), seed(prev2Id), spin);
        // the rocket: circling the departure world during the pan, then off along the arc, then into orbit at the end
        float rx, ry, ang; boolean behind = false, firing = playing;
        float pa = spin * 2.2f;
        float bu = Math.max(0.001f, f), bx = bz(sx, cxp, ex, bu), by2 = bz(sy, cyp, ey, bu), tgx = bd(sx, cxp, ex, bu), tgy = bd(sy, cyp, ey, bu);
        rx = bx; ry = by2; ang = (float) Math.atan2(tgy, tgx);
        if (end > 0f) {                                                                          // arriving: into orbit round the destination
            float ex2 = dX + (float) Math.cos(pa) * (r1 + 5), ey2 = yb + (float) Math.sin(pa) * (r1 + 5) * 0.4f, oang = (float) Math.atan2(Math.cos(pa) * 0.4f, -Math.sin(pa));
            rx += (ex2 - rx) * orbit; ry += (ey2 - ry) * orbit; ang = lerpAng(ang, oang, orbit); behind = orbit > 0.6f && Math.sin(pa) < 0; firing = playing && orbit < 0.9f;
        }
        float leave = Math.max(0f, Math.min(1f, (since - PAN * 0.8f) / 700f));
        if (leave < 1f) {                                                                        // the new song: still circling the old world while the view pans
            float ox2 = dx + (float) Math.cos(pa) * (r0 + 5), oy2 = yb + (float) Math.sin(pa) * (r0 + 5) * 0.4f, oang = (float) Math.atan2(Math.cos(pa) * 0.4f, -Math.sin(pa));
            float u = ease(leave); rx = ox2 + (rx - ox2) * u; ry = oy2 + (ry - oy2) * u; ang = lerpAng(oang, ang, u);
            behind = u < 0.4f && Math.sin(pa) < 0; firing = true;
        }
        if (behind) flyer(g, rx, ry, ang, a, now, firing, 1f);
        Worlds.draw(g, dx, yb, r0 + (r1 - r0) * (1f - pan), Math.round(a * edge(dx, tx, w)), seed(prevId), spin);
        if (playing) { float bp = (now % 500L) / 500f, bt = (1f - bp) * (1f - bp); circle(g, dX, yb, r1 + 2f + 5f * bp, Gfx.argb(Math.round(a * 0.45f * bt * edge(dX, tx, w)), 0xD8C8FF)); }   // it pulses with the beat
        Worlds.draw(g, dX, yb, r1, Math.round(a * edge(dX, tx, w)), seed(curId), spin);
        if (!behind) flyer(g, rx, ry, ang, a, now, firing, leave < 1f && leave > 0f ? 1.8f : 1f);
        // the time, small, inside the arc (the card ends right under the worlds)
        String tm = mmss(pos) + "  /  " + mmss(dur); float tw2 = font.width(tm) * 0.55f;          // the time, small, under the middle of the arc
        small(g, font, tm, (sx + ex) / 2f - tw2 / 2f, yb + 3, 0.55f, Gfx.argb(Math.round(a * 0.6f), 0xFFFFFF));
    }
    private static float edge(float x, int tx, int w) { return Math.max(0f, Math.min(1f, Math.min((x - (tx - 6)) / 12f, (w - 3 - x) / 10f))); }
    private static float bz(float p0, float p1, float p2, float u) { float v = 1f - u; return v * v * p0 + 2 * v * u * p1 + u * u * p2; }
    private static float bd(float p0, float p1, float p2, float u) { return 2 * (1f - u) * (p1 - p0) + 2 * u * (p2 - p1); }
    private static float lerpAng(float a0, float a1, float u) { float d = a1 - a0; while (d > Math.PI) d -= (float) (2 * Math.PI); while (d < -Math.PI) d += (float) (2 * Math.PI); return a0 + d * u; }
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
    /** The rocket turned to ang (0 = flying right). */
    private static void flyer(GuiGraphicsExtractor g, float x, float y, float ang, int a, long now, boolean firing, float boost) {
        Gfx.push(g); Gfx.translate(g, x, y); if (ang != 0f) Gfx.rotate(g, ang); rocket(g, 0, 0, a, now, firing, boost); Gfx.pop(g);
    }
    /** One layer of drifting stars: 0 far (faint, slow, tiny), 1 middle, 2 near (bright, quick, some two pixels with a glint). */
    private static void stars(GuiGraphicsExtractor g, int w, int h, int a, float drift, int layer) {
        int n = layer == 0 ? 22 : layer == 1 ? 12 : 6; float sp0 = layer == 0 ? 1.5f : layer == 1 ? 5f : 13f, al = layer == 0 ? 0.35f : layer == 1 ? 0.6f : 0.9f;
        for (int i = 0; i < n; i++) {
            int sd = i * 31 + layer * 977; float sp = sp0 * (0.8f + 0.5f * hash(sd + 7));
            float x = 3 + ((hash(sd + 13) * (w - 6) - drift * sp) % (w - 6) + (w - 6)) % (w - 6), y = 3 + hash(sd + 29) * (h - 6);
            float tw = 0.5f + 0.5f * Math.abs((float) Math.sin(drift * (1 + hash(sd)) + i));
            int col = Gfx.argb(Math.round(a * al * (0.55f + 0.45f * tw)), i % 5 == 0 ? 0xD8C8FF : STAR), sz = layer == 2 && i % 2 == 0 ? 2 : 1;
            int ix = Math.round(x), iy = Math.round(y); if (ix + sz > w - 3) continue;
            g.fill(ix, iy, ix + sz, iy + sz, col);
            if (layer == 2 && sz == 2) { int gl = Gfx.argb(Math.round(a * 0.25f * tw), 0xFFFFFF); g.fill(ix - 1, iy, ix, iy + 2, gl); g.fill(ix + 2, iy, ix + 3, iy + 2, gl); g.fill(ix, iy - 1, ix + 2, iy, gl); g.fill(ix, iy + 2, ix + 2, iy + 3, gl); }
        }
    }
    /** A disc cut off at the card's edges (the big world behind the glass must not spill out of it). */
    private static void cap(GuiGraphicsExtractor g, float cx, float cy, float r, int w, int h, int col) {
        for (int y = Math.max(2, Math.round(cy - r)); y < Math.min(h - 2, Math.round(cy + r)); y++) { float dy = y + 0.5f - cy, half = (float) Math.sqrt(Math.max(0f, r * r - dy * dy)); int x0 = Math.max(2, Math.round(cx - half)), x1 = Math.min(w - 2, Math.round(cx + half)); if (x1 > x0) g.fill(x0, y, x1, y + 1, col); }
    }
    private static void rout1(GuiGraphicsExtractor g, int x, int y, int w, int h, int c) { g.fill(x + 1, y, x + w - 1, y + 1, c); g.fill(x + 1, y + h - 1, x + w - 1, y + h, c); g.fill(x, y + 1, x + 1, y + h - 1, c); g.fill(x + w - 1, y + 1, x + w, y + h - 1, c); }
    /** One pixel at a fractional place: it slides smoothly instead of hopping from pixel to pixel. */
    static void dot(GuiGraphicsExtractor g, float x, float y, int c) { Gfx.push(g); Gfx.translate(g, x - 0.5f, y - 0.5f); g.fill(0, 0, 1, 1, c); Gfx.pop(g); }
    private static float hash(int i) { int h = i * 374761393 + 668265263; h = (h ^ (h >>> 13)) * 1274126177; return ((h ^ (h >>> 16)) & 0xFFFF) / 65535f; }
    private static float rec = 0f;
    static void disc(GuiGraphicsExtractor g, float cx, float cy, float r, int c) {
        for (int y = Math.round(-r); y <= Math.round(r); y++) { float hw = (float) Math.sqrt(Math.max(0, r * r - y * y)); g.fill(Math.round(cx - hw), Math.round(cy + y), Math.round(cx + hw), Math.round(cy + y) + 1, c); }
    }
    static void circle(GuiGraphicsExtractor g, float cx, float cy, float r, int c) {
        int n = Math.max(12, Math.round(r * 4)); for (int i = 0; i < n; i++) { double an = i * Math.PI * 2 / n; int x = Math.round(cx + (float) Math.cos(an) * r), y = Math.round(cy + (float) Math.sin(an) * r); g.fill(x, y, x + 1, y + 1, c); }
    }

    // ------------------------------------------------------------------ bits
    static void cover(GuiGraphicsExtractor g, Font font, int x, int y, int s, int a, boolean art, int acc) {
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
