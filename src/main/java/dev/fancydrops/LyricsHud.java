package dev.fancydrops;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/**
 * The lyrics (Visuals > Lyrics): big, clean words a little below the crosshair (Move them to put them anywhere), outlined so they read
 * on anything, on a soft shade. Three styles:
 *
 *   POP (default): the words pop in one by one as they're sung, each with a spring; the word being sung glows in the album's colour with
 *     a little bar sliding under it from word to word; sung words settle to white. The old line drifts up and fades.
 *   KARAOKE: the whole line waits in grey and a wipe of the album's colour runs through it letter by letter, smooth as the song; a dot
 *     bounces from word to word above it. The next line waits faintly below.
 *   SCROLL (new): three lines stacked like a lyric sheet - the last one small and dim above, the current one big and bright with its
 *     glow, the next one below - and the sheet glides up when the line changes.
 *
 * Long lines wrap onto a second row (and shrink a little if they still don't fit). Words are timed from the line's start, spread over
 * how long the line would take to sing (about 85 ms a letter). Everything steps aside while a drop animation plays.
 */
final class LyricsHud {
    private LyricsHud() {}

    static final int POP = 0, KARAOKE = 1, SCROLL = 2, SUNG = 3;

    /** The colour of the sung words: from the album cover, made light enough to read. */
    static int colour() { return Ritual.mixc(AlbumArt.accent(), 0xFFFFFF, 0.25f); }

    static boolean preview;
    static int bx0, by0, bx1, by1;
    private static int lastLine = -2, prevLine = -2; private static String lastTrack = "";
    private static long lineAt, titleAt, lastFrame;
    private static float away;

    /** From the HUD (not while the editor draws its own). */
    static void hud(GuiGraphicsExtractor g) { if (!preview) render(g); }

    static void render(GuiGraphicsExtractor g) {
        FancyConfig c = FancyConfig.get(); Minecraft mc = Minecraft.getInstance();
        long now = System.currentTimeMillis();
        float dt = Math.min(0.1f, (now - (lastFrame == 0 ? now : lastFrame)) / 1000f); lastFrame = now;
        if (!preview && (!c.lyricsOn || mc.level == null)) { bx0 = bx1 = 0; return; }
        away += ((AnimationManager.isPlaying() && !preview ? 1f : 0f) - away) * Math.min(1f, dt * 8f);
        float vis = 1f - away; if (vis < 0.02f) return;
        Spotify.Track t = Spotify.track;
        boolean on = t != null && Spotify.playing;
        List<Lyrics.Line> L = Lyrics.lines;
        boolean demo = preview && (!on || L.isEmpty());
        if (demo) { L = DEMO; on = true; }
        if (!demo && on && !t.id().equals(lastTrack)) { lastTrack = t.id(); lastLine = -2; prevLine = -2; titleAt = now; }
        long pos = demo ? now % 9000 : Spotify.position() + FancyConfig.lyricsShift();
        draw(g, mc.font, c, L, on, demo, pos, now, vis);
    }
    static void draw(GuiGraphicsExtractor g, Font f, FancyConfig c, List<Lyrics.Line> L, boolean on, boolean demo, long pos, long now, float vis) {
        int W = g.guiWidth(), H = g.guiHeight(); float k = c.lyricsSize / 100f, cx = W * c.lineX, cy = H * c.lyY;
        if (!demo && c.lyricsIntro) titleCard(g, f, W, H, now, vis, k);
        if (!demo && on && L.isEmpty() && Lyrics.status.startsWith("No synced")) { noLyrics(g, f, now, vis, cx, cy, k); return; }
        if (L.isEmpty() || !on || c.lyricsStyle == SUNG && !demo) { bx0 = bx1 = 0; return; }       // (Sung line: the strip on the Now Playing card draws them)
        int li = at(L, pos);
        if (li != lastLine) { prevLine = lastLine; lastLine = li; lineAt = now; }
        float since = (now - lineAt) / 1000f, out = Math.min(1f, since / 0.35f);
        int maxW = Math.round(Math.min(W * 0.72f, 360f) / k);
        bx0 = Math.round(cx - maxW / 2f * k); bx1 = Math.round(cx + maxW / 2f * k); by0 = Math.round(cy - 6 * k); by1 = Math.round(cy + 22 * k);
        if (c.lyricsStyle == KARAOKE) {
            if (c.lyricsPrev && prevLine >= 0 && prevLine < L.size() && out < 1f) karaoke(g, f, L, prevLine, Long.MAX_VALUE / 4, cx, cy - 12 * k * ease(out), k, (1f - out) * vis, maxW);
            if (li >= 0 && li < L.size()) karaoke(g, f, L, li, Long.MAX_VALUE / 4, cx, cy + 4 * k * (1f - ease(out)), k, ease(out) * vis, maxW);
            beatUnder(g, f, L, li, cx, cy + rowsBelow(f, L, li, maxW, k) + 3 * k, k, vis, maxW, pos);
            if (c.lyricsNext && li + 1 < L.size()) faint(g, f, L.get(li + 1).text(), cx, cy + rowsBelow(f, L, li, maxW, k) + 9 * k, k * 0.75f, 0.45f * vis, maxW);
        } else if (c.lyricsStyle == SCROLL) {
            float glide = 1f - ease(out), step = 22 * k;                                     // the sheet moves up one line as the new one arrives
            for (int d = -2; d <= 2; d++) {
                int idx = li + d; if (idx < 0 || idx >= L.size()) continue;
                if (d == 2 && glide < 0.01f) continue;
                if (d < 0 && !c.lyricsPrev) continue;                                       // the lines already sung, only if you want them
                float extra = li >= 0 && li < L.size() ? Math.max(0f, rowsBelow(f, L, li, maxW, k * 1.15f) - 10 * k * 1.15f * 1.2f) : 0f;   // a middle line on two rows pushes its neighbours apart
                float y = cy + d * step + glide * step + Math.signum(d) * extra, dist = Math.abs(d + glide);
                float a = Math.max(0f, 1f - dist * 0.55f) * vis, sc = k * (1.15f - Math.min(1f, dist) * 0.4f);
                if (a < 0.03f) continue;
                if (d == 0) { line(g, f, L, idx, Long.MAX_VALUE / 4, cx, y, sc, a, maxW, c, true); beatUnder(g, f, L, idx, cx, y + rowsBelow(f, L, idx, maxW, sc) + 2 * k, sc, a, maxW, pos); }
                else faint(g, f, L.get(idx).text(), cx, y - 2 * k, sc, a * 0.75f, Math.round(maxW * k / Math.max(0.3f, sc)));
            }
        } else {
            if (c.lyricsPrev && prevLine >= 0 && prevLine < L.size() && out < 1f) line(g, f, L, prevLine, Long.MAX_VALUE / 4, cx, cy - 14 * k * ease(out), k, (1f - out) * vis, maxW, c, false);
            if (li >= 0 && li < L.size()) line(g, f, L, li, Long.MAX_VALUE / 4, cx, cy, k, vis, maxW, c, false);
            beatUnder(g, f, L, li, cx, cy + rowsBelow(f, L, li, maxW, k) + 3 * k, k, vis, maxW, pos);
            if (c.lyricsNext && li + 1 < L.size()) faint(g, f, L.get(li + 1).text(), cx, cy + rowsBelow(f, L, li, maxW, k) + 9 * k, k * 0.7f, 0.35f * vis, maxW);
        }
    }
    /** How far below the line's middle its last row ends. */
    private static final float ROW_C = 20;                                                   // Constellation's row height: room for the stars between rows
    private static float rowsBelow(Font f, List<Lyrics.Line> L, int li, int maxW, float k) {
        if (li < 0 || li >= L.size()) return 10 * k;
        float[] q = new float[2]; List<String> rows = rowsOf(f, L.get(li).text(), maxW, k * 1.2f, q); float kk = q[0];
        float rowH = FancyConfig.get().lyricsStyle == KARAOKE ? ROW_C : 12;
        return (rows.size() - 1) * rowH * kk / 2f + 10 * kk;
    }
    /** The beat line under a line of lyrics, about as wide as the words. */
    private static void beatUnder(GuiGraphicsExtractor g, Font f, List<Lyrics.Line> L, int li, float cx, float y, float k, float a, int maxW, long pos) {
        if (!FancyConfig.get().lyricsBar) return;
        float w = li >= 0 && li < L.size() ? Math.min(f.width(L.get(li).text()) * k * 1.2f, maxW * k) * 0.6f : 60 * k;
        nextBar(g, L, li, cx, y, Math.max(48 * k, Math.min(150 * k, w)), k, a, colour(), pos);
    }
    /** The countdown: a comet crossing a faint dotted path to a small star; the star brightens as the next line nears. */
    private static void nextBar(GuiGraphicsExtractor g, List<Lyrics.Line> L, int li, float cx, float y, float w, float k, float a, int acc, long pos) {
        if (a < 0.02f || li < 0 || li >= L.size()) return;
        long st = L.get(li).atMs(), en = li + 1 < L.size() ? L.get(li + 1).atMs() : st + 5000;
        float p = Math.max(0f, Math.min(1f, (pos - st) / (float) Math.max(1, en - st)));
        float x0 = cx - w / 2f, x1 = cx + w / 2f, hx = x0 + (x1 - x0) * p; int yy = Math.round(y);
        for (float dx = x0; dx < x1; dx += Math.max(3f, 3 * k)) if (dx > hx) g.fill(Math.round(dx), yy, Math.round(dx) + 1, yy + 1, Gfx.argb(Math.round(55 * a), 0xFFFFFF));   // the path ahead
        float tail = Math.min(hx - x0, w * 0.35f); int segs = 10;
        for (int i = 0; i < segs; i++) {                                                         // the tail, fading behind
            float u0 = i / (float) segs, u1 = (i + 1f) / segs, xa = hx - tail * u1, xb = hx - tail * u0;
            g.fill(Math.round(xa), yy, Math.round(xb), yy + 1, Gfx.argb(Math.round(a * 200 * (1f - u0) * (1f - u0)), Ritual.mixc(acc, 0xFFFFFF, 0.5f * (1f - u0))));
        }
        NebulaTex.glow(g, hx, yy + 0.5f, 4 * k + 1, 2.5f * k + 1, Gfx.argb(Math.round(170 * a), acc));
        NebulaTex.glow(g, hx, yy + 0.5f, 1.6f * k + 1, 1.6f * k + 1, Gfx.argb(Math.round(240 * a), 0xFFFFFF));
        float near = Math.max(0f, (p - 0.75f) / 0.25f), tw = 0.6f + 0.4f * (float) Math.sin(System.currentTimeMillis() / 200.0);
        NebulaTex.glow(g, x1, yy + 0.5f, (2 + 4 * near) * k, (2 + 4 * near) * k, Gfx.argb(Math.round(a * (60 + 180 * near) * tw), 0xFFFFFF));   // the star it is heading for
        g.fill(Math.round(x1), yy, Math.round(x1) + 1, yy + 1, Gfx.argb(Math.round(a * 255), 0xFFFFFF));
    }
    private static int at(List<Lyrics.Line> l, long ms) { int i = -1; for (int k = 0; k < l.size(); k++) { if (l.get(k).atMs() <= ms) i = k; else break; } return i; }
    private static final List<Lyrics.Line> DEMO = List.of(new Lyrics.Line(0, "The words of the song you're playing"), new Lyrics.Line(3000, "light and easy to read"), new Lyrics.Line(6000, "drag them anywhere you like"));


    /** The rows of a line (at most two) and the scale that fits them. */
    private static String wrapFor; private static int wrapW; private static List<String> wrapRows; private static int wrapWidest;
    private static List<String> rowsOf(Font f, String text, int maxW, float k, float[] kk) {
        List<String> rows; int widest;
        if (text.equals(wrapFor) && maxW == wrapW) { rows = wrapRows; widest = wrapWidest; }                // (measured once per line, not several times a frame)
        else { rows = wrap(f, text, Math.round(maxW / 1.2f)); widest = 0; for (String r : rows) widest = Math.max(widest, f.width(r)); wrapFor = text; wrapW = maxW; wrapRows = rows; wrapWidest = widest; }
        float fit = maxW / 1.2f; kk[0] = widest > fit ? k * fit / (float) widest : k; kk[1] = widest; return rows;   // (k is 1.2x the base size: rows never pass the lyric area)
    }
    /** The shade behind a line: soft, wide, never a box. */
    private static void shade(GuiGraphicsExtractor g, float cx, float cy, float w, float kk, int rows, float a) {
        NebulaTex.glow(g, cx, cy + 4 * kk, w * 0.58f + 14, 10 * kk + rows * 6 * kk, Gfx.argb(Math.round(95 * a), 0x000000));
    }
    /** Pop (and Scroll's middle line): words arrive as they're sung; the sung word glows in the album's colour with a bar sliding under it. */
    /** How long the line li has been on screen (s): the current line from when it arrived, any other as long settled. */
    private static float age(int li) { return li == lastLine ? (System.currentTimeMillis() - lineAt) / 1000f : 9f; }
    private static final int STARLIGHT = 0xEEF2FF, NEBULA = 0x6A4AD8;
    /** The light behind the words: the album's colour blended into a violet nebula. */
    private static int nebula() { return Ritual.mixc(colour(), NEBULA, 0.45f); }
    /**
     * Starlight: each word is born as a star. A point of light flares where the word will be (with a little cross of rays), then
     * opens into the word, one word after another, 50 ms apart; the words settle in pale starlight. Drift's middle line uses it settled.
     */
    private static void line(GuiGraphicsExtractor g, Font f, List<Lyrics.Line> L, int li, long pos, float cx, float cy, float k, float a, int maxW, FancyConfig c, boolean all) {
        if (a < 0.02f) return;
        String text = L.get(li).text(); if (text.isBlank()) return;
        float[] q = new float[2]; List<String> rows = rowsOf(f, text, maxW, k * 1.2f, q); float kk = q[0];
        float t = all ? 9f : age(li), rowH = 12 * kk, y0 = cy - (rows.size() - 1) * rowH / 2f;
        if (c.lyricsShade) shade(g, cx, cy, q[1] * kk, kk, rows.size(), a);
        if (c.lyricsGlow) NebulaTex.glow(g, cx, cy + 4 * kk, q[1] * kk * 0.6f + 12, 9 * kk * rows.size(), Gfx.argb(Math.round(a * (45 + 60 * Math.max(0f, 1f - t / 0.8f))), nebula()));
        if (c.lyricsStars && t < 1f) AmbienceHud.sparkle(g, cx, cy + 3 * kk, q[1] * kk, t, a * 0.8f, li * 31 & 0xFFFF, kk);
        int wi = 0;
        for (String row : rows) {
            float x = cx - f.width(row) * kk / 2f;
            for (String w : words(row)) {
                float ww = f.width(w) * kk;
                if (!w.isEmpty()) {
                    float u = (t - wi * 0.05f) / 0.42f, mx = x + ww / 2f, my = y0 + 4 * kk;
                    if (u > 0f && u < 1f) {                                                       // the star it is born from
                        float fl = (float) Math.sin(Math.PI * Math.min(1f, u * 1.4f)), rr = (3 + 5 * fl) * kk;
                        NebulaTex.glow(g, mx, my, rr * 1.6f, rr * 1.6f, Gfx.argb(Math.round(a * 200 * fl), 0xFFFFFF));
                        NebulaTex.glow(g, mx, my, rr * 3.2f, 0.7f * kk, Gfx.argb(Math.round(a * 160 * fl), 0xDDE6FF)); NebulaTex.glow(g, mx, my, 0.7f * kk, rr * 2.2f, Gfx.argb(Math.round(a * 160 * fl), 0xDDE6FF));
                    }
                    float v = Math.max(0f, Math.min(1f, (u - 0.25f) / 0.75f));
                    if (v > 0f) outlined(g, f, w, mx, y0 + (1f - ease(v)) * 1.5f * kk, kk * (0.35f + 0.65f * ease(v)), a * Math.min(1f, v * 1.6f), STARLIGHT);
                    wi++;
                }
                x += ww + f.width(" ") * kk;
            }
            y0 += rowH;
        }
    }
    /**
     * Constellation: a small star sits above every word, and as the line arrives the stars are joined one to the next by fine lines,
     * drawn left to right like a star chart; each word fades in under its star. The stars twinkle while the line stays.
     */
    private static void karaoke(GuiGraphicsExtractor g, Font f, List<Lyrics.Line> L, int li, long pos, float cx, float cy, float k, float a, int maxW) {
        if (a < 0.02f) return;
        FancyConfig c = FancyConfig.get();
        String text = L.get(li).text(); if (text.isBlank()) return;
        float[] q = new float[2]; List<String> rows = rowsOf(f, text, maxW, k * 1.2f, q); float kk = q[0];
        float t = age(li), rowH = ROW_C * kk, y0 = cy - (rows.size() - 1) * rowH / 2f;
        if (c.lyricsShade) shade(g, cx, cy, q[1] * kk, kk, rows.size(), a);
        if (c.lyricsGlow) NebulaTex.glow(g, cx, cy + 2 * kk, q[1] * kk * 0.6f + 14, 11 * kk * rows.size(), Gfx.argb(Math.round(a * 70), nebula()));
        if (c.lyricsStars && t < 1f) AmbienceHud.sparkle(g, cx, cy - 3 * kk, q[1] * kk, t, a * 0.7f, li * 17 & 0xFFFF, kk);
        List<float[]> stars = new ArrayList<>(); int acc = colour(), rowIx = 0;
        for (String row : rows) {
            float x = cx - f.width(row) * kk / 2f;
            for (String w : words(row)) {
                float ww = f.width(w) * kk;
                if (!w.isEmpty()) {
                    int i = stars.size(); float sy = y0 - 4 * kk - (rnd(li * 13 + i) * 3f) * kk;       // the stars sit at slightly different heights, like a real chart
                    stars.add(new float[]{x + ww / 2f, sy, i, rowIx});
                    float v = Math.max(0f, Math.min(1f, (t - 0.1f - i * 0.06f) / 0.35f));
                    outlined(g, f, w, x + ww / 2f, y0, kk, a * v, Ritual.mixc(STARLIGHT, acc, 0.15f));
                }
                x += ww + f.width(" ") * kk;
            }
            y0 += rowH; rowIx++;
        }
        for (int i = 1; i < stars.size(); i++) {                                                // the lines of the chart, drawn left to right (each row its own)
            float[] s0 = stars.get(i - 1), s1 = stars.get(i);
            if (s0[3] != s1[3]) continue;
            float gr = Math.max(0f, Math.min(1f, (t - i * 0.06f) / 0.18f)); if (gr <= 0f) continue;
            float ex = s0[0] + (s1[0] - s0[0]) * gr, ey = s0[1] + (s1[1] - s0[1]) * gr, len = (float) Math.hypot(ex - s0[0], ey - s0[1]);
            if (len < 1f) continue;
            Gfx.push(g); Gfx.translate(g, s0[0], s0[1]); Gfx.rotate(g, (float) Math.atan2(ey - s0[1], ex - s0[0]));
            g.fill(Math.round(2 * kk), 0, Math.round(len - (gr >= 1f ? 2 * kk : 0)), 1, Gfx.argb(Math.round(a * 110), Ritual.mixc(STARLIGHT, acc, 0.3f))); Gfx.pop(g);
        }
        for (float[] st : stars) {                                                               // the stars, twinkling
            float on = Math.max(0f, Math.min(1f, (t - st[2] * 0.06f) / 0.15f)); if (on <= 0f) continue;
            float tw = 0.7f + 0.3f * (float) Math.sin(System.currentTimeMillis() / 300.0 + st[2] * 1.7);
            NebulaTex.glow(g, st[0], st[1], 4 * kk, 4 * kk, Gfx.argb(Math.round(a * 120 * on * tw), acc));
            g.fill(Math.round(st[0]) - 1, Math.round(st[1]) - 1, Math.round(st[0]) + 1, Math.round(st[1]) + 1, Gfx.argb(Math.round(a * 255 * on), 0xFFFFFF));
        }
    }
    private static float rnd(int i) { int h = i * 374761393 + 668265263; h = (h ^ (h >>> 13)) * 1274126177; return ((h ^ (h >>> 16)) & 0xFFFF) / 65535f; }
    /** A word or letter with a dark outline, so it reads on snow, sky and lava alike. mx: its middle. */
    private static void outlined(GuiGraphicsExtractor g, Font f, String w, float mx, float y, float k, float a, int rgb) {
        if (a < 0.02f) return;
        Gfx.push(g); Gfx.translate(g, mx, y + 4 * k); Gfx.scale(g, k);
        int x = -f.width(w) / 2, o = Gfx.argb(Math.round(200 * a), 0x08080C);
        Gfx.txt(g, f, w, x - 1, -4, o, false); Gfx.txt(g, f, w, x + 1, -4, o, false); Gfx.txt(g, f, w, x, -5, o, false); Gfx.txt(g, f, w, x, -3, o, false);
        Gfx.txt(g, f, w, x, -4, Gfx.argb(Math.round(255 * a), rgb), false);
        Gfx.pop(g);
    }
    private static void faint(GuiGraphicsExtractor g, Font f, String s, float cx, float y, float k, float a, int maxW) {
        if (s.isBlank() || a < 0.02f) return;
        if (f.width(s) > maxW) s = f.plainSubstrByWidth(s, maxW - 8) + "…";
        outlined(g, f, s, cx, y, k, a, 0xC8D2F0);
    }
    /** A row's words, split once and kept (the HUD draws the same rows every frame). */
    private static final java.util.Map<String, String[]> SPLIT = new java.util.HashMap<>();
    private static String[] words(String row) { String[] w = SPLIT.get(row); if (w == null) { if (SPLIT.size() > 200) SPLIT.clear(); w = row.split(" "); SPLIT.put(row, w); } return w; }
    /** Up to two rows, split at the space nearest the middle. */
    private static List<String> wrap(Font f, String s, int maxW) {
        List<String> l = new ArrayList<>();
        if (f.width(s) <= maxW) { l.add(s); return l; }
        int mid = s.length() / 2, best = -1;
        for (int d = 0; d < mid; d++) { if (mid + d < s.length() && s.charAt(mid + d) == ' ') { best = mid + d; break; } if (s.charAt(mid - d) == ' ') { best = mid - d; break; } }
        if (best < 0) { l.add(s); return l; }
        l.add(s.substring(0, best).trim()); l.add(s.substring(best + 1).trim()); return l;
    }

    // ------------------------------------------------------------------ the sung line, docked to Now Playing (Lyrics > Sung line)
    private static float dockShow, dockSwap; private static int dockLine = -2, dockPrev = -2; private static long dockAt, dockFrame;
    /**
     * A slim glass strip that slides out from behind the Now Playing card, under it (or over it when there is room and you chose
     * Above): a note that bobs, the line being sung with a wipe of the album's colour running through it, and a thin bar of how far
     * through the line you are. A new line rolls in from below as the old one rolls away. Drawn in the card's own units (w x h).
     */
    static void dock(GuiGraphicsExtractor g, Font f, int w, int h, float e, boolean roomAbove, int acc) {
        FancyConfig c = FancyConfig.get(); long now = System.currentTimeMillis();
        float dt = Math.min(0.1f, (now - (dockFrame == 0 ? now : dockFrame)) / 1000f); dockFrame = now;
        List<Lyrics.Line> L = Lyrics.lines;
        boolean demo = NowPlayingHud.preview && (Spotify.track == null || L.isEmpty()); if (demo) L = DEMO;
        boolean on = c.lyricsOn && c.lyricsStyle == SUNG && (demo || Spotify.playing) && !L.isEmpty();
        long pos = demo ? now % 9000 : Spotify.position() + FancyConfig.lyricsShift();
        int li = on ? at(L, pos) : -1;
        boolean has = li >= 0 && !L.get(li).text().isBlank();
        dockShow += ((has ? 1f : 0f) - dockShow) * Math.min(1f, dt * 7f);
        if (dockShow < 0.01f) { dockLine = -2; return; }
        if (has && li != dockLine) { dockPrev = dockLine; dockLine = li; dockAt = now; }
        boolean above = c.npLine == 2 && roomAbove;
        float s = dockShow * dockShow * (3 - 2 * dockShow), sh = 17, sw = w - 18, sx = 9;
        float y = above ? -sh * s - 2 * s + 3 : h - 3 + sh * s - sh + 2 * s;           // tucked behind the card, then out
        int a = Math.round(255 * e * s), A = Math.round(255 * e);
        NebulaTex.glow(g, w / 2f, y + sh / 2f + 2, sw * 0.55f, sh, Gfx.argb(Math.round(70 * e * s), 0x000000));
        NowPlayingHud.round(g, Math.round(sx), Math.round(y), Math.round(sw), Math.round(sh), Gfx.argb(Math.round((70 + 100 * NowPlayingHud.bg()) * e * s), 0x2C2A66));          // the same glass as the card
        NowPlayingHud.round(g, Math.round(sx) + 1, Math.round(y) + 1, Math.round(sw) - 2, Math.round(sh) - 2, Gfx.argb(Math.round(255 * NowPlayingHud.bg() * e * s), 0x0B0C26));

        g.fill(Math.round(sx + 3), Math.round(above ? y : y + sh - 1), Math.round(sx + sw - 3), Math.round(above ? y + 1 : y + sh), Gfx.argb(Math.round(40 * e * s), 0xFFFFFF));   // the lit outer edge
        float bob = (float) Math.sin(now / 260.0) * 1f;                                       // the note, in a soft round light
        NebulaTex.glow(g, sx + 9, y + sh / 2f, 6, 6, Gfx.argb(Math.round(a * 0.55f), acc));
        Gfx.push(g); Gfx.translate(g, sx + 6.5f, y + sh / 2f - 4 + bob); Gfx.txt(g, f, "♪", 0, 0, Gfx.argb(a, Ritual.mixc(acc, 0xFFFFFF, 0.6f)), false); Gfx.pop(g);
        // the words: the old line rolls up and away, the new one rolls in from below
        float roll = Math.min(1f, (now - dockAt) / 280f); roll = 1f - (1f - roll) * (1f - roll) * (1f - roll);
        int tx0 = Math.round(sx + 17), tw = Math.round(sw - 24);
        if (dockPrev >= 0 && dockPrev < L.size() && roll < 1f) dockText(g, f, L, dockPrev, Long.MAX_VALUE / 4, tx0, tw, y + sh / 2f - 4 - 6 * roll, Math.round(a * (1f - roll)), acc);
        if (li >= 0 && li < L.size()) dockText(g, f, L, li, Long.MAX_VALUE / 4, tx0, tw, y + sh / 2f - 4 + 6 * (1f - roll), Math.round(a * roll), acc);
        if (FancyConfig.get().lyricsBar) nextBar(g, L, li, sx + 17 + (sw - 24) / 2f, above ? y + sh - 2.5f : y + 1.5f, sw - 30, 0.7f, a / 255f, acc, pos);        // until the next line, along the inner edge
    }
    /** One line in the strip, lit in the album's colour, scaled down to fit. */
    private static void dockText(GuiGraphicsExtractor g, Font f, List<Lyrics.Line> L, int li, long pos, int x, int tw, float y, int a, int acc) {
        if (a < 6) return;
        String text = L.get(li).text(); long start = L.get(li).atMs(), end = li + 1 < L.size() ? L.get(li + 1).atMs() : start + 5000;
        float k = Math.max(0.62f, Math.min(0.9f, tw / (float) Math.max(1, f.width(text))));
        if (f.width(text) * k > tw) text = f.plainSubstrByWidth(text, Math.round(tw / k) - f.width("…")) + "…";
        float cx = x + (tw - f.width(text) * k) / 2f;
        Gfx.push(g); Gfx.translate(g, cx, y + (1f - k) * 4); Gfx.scale(g, k);
        Gfx.txt(g, f, text, 0, 0, Gfx.argb(a, Ritual.mixc(acc, 0xFFFFFF, 0.3f)), true);
        Gfx.pop(g);
    }

    static final float INTRO = 4.4f;                                                           // how long the song intro lasts (s)
    private static void titleCard(GuiGraphicsExtractor g, Font f, int W, int H, long now, float vis, float k) {
        Spotify.Track t = Spotify.track; if (t == null || !Spotify.playing) return;
        float u = (now - titleAt) / 1000f; if (u > INTRO) return;
        intro(g, f, W, H, t.name(), t.artists(), u, vis, k);
    }
    private static final String GLYPHS = "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789#$%&*+=<>/";
    /**
     * The song intro: a hyperspace arrival. Stars streak in toward a point; a flash and a shockwave in the album's colour; the
     * cover drops in on a glowing frame; the title decodes letter by letter out of scrambled glyphs while its spacing tightens; the
     * artist types itself out behind a blinking cursor; a little constellation draws itself round the title. Then it all jumps to
     * lightspeed and streaks away to the right.
     */
    static void intro(GuiGraphicsExtractor g, Font f, int W, int H, String name, String artists, float u, float a, float size) {
        if (a < 0.02f) return;
        int acc = colour(), lite = Ritual.mixc(acc, 0xFFFFFF, 0.5f), seed = name.hashCode();
        float k = size * 2.2f, cx = W / 2f, cy = H * 0.31f;
        if (f.width(name) * k > W * 0.62f) name = f.plainSubstrByWidth(name, (int) (W * 0.62f / k) - 8) + "\u2026";
        float out = Math.max(0f, (u - (INTRO - 0.9f)) / 0.9f), oe = out * out;                                   // leaving: 0..1
        float fade = a * (1f - Math.max(0f, (out - 0.6f) / 0.4f));
        // 1. the jump in: streaks rushing toward the point, then gone
        if (u < 0.75f) {
            float q = u / 0.75f;
            for (int i = 0; i < 48; i++) {
                double an = Math.PI * 2 * hash(seed + i * 7); float d0 = (0.25f + 0.75f * hash(seed + i * 13)) * W * 0.6f;
                float d = d0 * (1f - q) * (1f - q), len = 6 + 40 * q * (0.5f + hash(i));
                float x0 = cx + (float) Math.cos(an) * d, y0 = cy + (float) Math.sin(an) * d * 0.6f, x1 = cx + (float) Math.cos(an) * (d + len), y1 = cy + (float) Math.sin(an) * (d + len) * 0.6f;
                streak(g, x0, y0, x1, y1, Gfx.argb(Math.round(220 * a * (float) Math.sin(Math.PI * q)), i % 3 == 0 ? lite : 0xFFFFFF));
            }
        }
        // 2. arrival: a flash and a shockwave ring
        if (u > 0.6f && u < 1.6f) {
            float q = (u - 0.6f) / 1.0f, r = 10 + ease(q) * W * 0.45f, fl = Math.max(0f, 1f - q * 3f);
            NebulaTex.glow(g, cx, cy, 40 + 80 * q, 24 + 40 * q, Gfx.argb(Math.round(230 * fl * a), 0xFFFFFF));
            int n = 120;
            for (int i = 0; i < n; i++) { double an = Math.PI * 2 * i / n; float x = cx + (float) Math.cos(an) * r, y = cy + (float) Math.sin(an) * r * 0.42f;
                g.fill(Math.round(x), Math.round(y), Math.round(x) + 1, Math.round(y) + 1, Gfx.argb(Math.round(200 * (1f - q) * a), i % 2 == 0 ? lite : acc)); }
        }
        if (u < 0.65f) return;
        float t0 = u - 0.65f;                                                                                  // time since arrival
        // the name's letters and where each one sits (the spacing tightens as it lands)
        int n = name.length(); float spread = 1f + 1.2f * (1f - ease(Math.min(1f, t0 / 0.9f)));
        float nw = 0; float[] cw = new float[n]; for (int i = 0; i < n; i++) { cw[i] = f.width(String.valueOf(name.charAt(i))) * k; nw += cw[i]; }
        float gap = (spread - 1f) * 3 * k, tot = nw + gap * Math.max(0, n - 1);
        float cs = 36 * size, coverX = cx - tot / 2f - cs - 10 * size, ty = cy - 4 * k;
        // a dark band behind it all, a light line underneath that opens out from the middle
        float open = ease(Math.min(1f, t0 / 0.5f));
        NebulaTex.glow(g, cx, cy + 4 * k, (tot / 2f + cs + 40) * open, 26 * k, Gfx.argb(Math.round(200 * fade), 0x05061A));
        NebulaTex.glow(g, cx, cy + 4 * k, (tot / 2f + cs) * 0.8f * open, 14 * k, Gfx.argb(Math.round(120 * fade), 0x05061A));
        float lineW = (tot / 2f + cs * 0.6f) * open;
        g.fill(Math.round(cx - lineW + oe * W), Math.round(cy + 9 * k), Math.round(cx + lineW + oe * W * 1.3f), Math.round(cy + 9 * k) + 1, Gfx.argb(Math.round(200 * fade), acc));
        NebulaTex.glow(g, cx + oe * W, cy + 9 * k, lineW, 3, Gfx.argb(Math.round(120 * fade), acc));
        // the cover: drops in, a glowing frame round it
        float cd = ease(Math.min(1f, t0 / 0.45f)), cxo = oe * W * 1.1f;
        if (cd > 0.01f) {
            int cxp = Math.round(coverX + cxo), cyp = Math.round(cy - cs / 2f + 2 - (1f - cd) * 14);
            NebulaTex.glow(g, cxp + cs / 2f, cyp + cs / 2f, cs, cs, Gfx.argb(Math.round(110 * fade * cd), acc));
            NowPlayingHud.cover(g, f, cxp, cyp, Math.round(cs), Math.round(255 * fade * cd), AlbumArt.ready, acc);
            int fr = Gfx.argb(Math.round(220 * fade * cd), lite);
            g.fill(cxp - 1, cyp - 1, cxp + Math.round(cs) + 1, cyp, fr); g.fill(cxp - 1, cyp + Math.round(cs), cxp + Math.round(cs) + 1, cyp + Math.round(cs) + 1, fr);
            g.fill(cxp - 1, cyp, cxp, cyp + Math.round(cs), fr); g.fill(cxp + Math.round(cs), cyp, cxp + Math.round(cs) + 1, cyp + Math.round(cs), fr);
        }
        // "NOW PLAYING", spaced, sliding in
        float lab = ease(Math.min(1f, t0 / 0.4f));
        { float lk = 0.8f * size, lx0 = cx - tot / 2f + (1f - lab) * 12 + oe * W * 0.9f;                       // "Now playing", small, over the title's first letter
          Gfx.push(g); Gfx.translate(g, lx0, ty - 11 * size); Gfx.scale(g, lk); Gfx.txt(g, f, "Now playing", 0, 0, Gfx.argb(Math.round(210 * fade * lab), lite), true); Gfx.pop(g); }
        // the title, letter by letter: scrambled glyphs that settle into the real letters, left to right
        float x = cx - tot / 2f; long tick = (long) (u * 28);
        for (int i = 0; i < n; i++) {
            char ch = name.charAt(i); float li = t0 - 0.08f - i * 0.035f;                                   // this letter's own clock
            if (li > 0f) {
                boolean real = li > 0.32f || ch == ' ';
                String s = real ? String.valueOf(ch) : String.valueOf(GLYPHS.charAt(Math.floorMod((int) (hash((int) (tick * 31 + i * 17 + seed)) * 1000), GLYPHS.length())));
                float drop = (1f - ease(Math.min(1f, li / 0.3f))) * -6 * size, la = Math.min(1f, li / 0.12f);
                float ox = oe * W * (0.7f + i * 0.04f);                                                          // leaving: each letter shoots off a little after the last
                int col = real ? Gfx.argb(Math.round(255 * fade * la), 0xFFFFFF) : Gfx.argb(Math.round(220 * fade * la), lite);
                if (out > 0f) streak(g, x + ox - out * 70 * size, ty + 4 * k, x + ox, ty + 4 * k, Gfx.argb(Math.round(180 * fade), lite));
                Gfx.push(g); Gfx.translate(g, x + ox, ty + drop); Gfx.scale(g, k); Gfx.txt(g, f, s, 0, 0, col, true); Gfx.pop(g);
                if (real && li < 0.45f) NebulaTex.glow(g, x + cw[i] / 2f + ox, ty + 4 * k, 6 * size, 6 * size, Gfx.argb(Math.round(160 * (1f - (li - 0.32f) / 0.13f) * fade), lite));   // a spark as it locks in
            }
            x += cw[i] + gap;
        }
        // the artist, typed out behind a blinking cursor
        float at = t0 - 0.35f - n * 0.035f;
        if (at > 0f) {
            int shown = Math.min(artists.length(), (int) (at / 0.028f)); String s = artists.substring(0, shown);
            boolean cursor = shown < artists.length() || ((long) (u * 2.5f)) % 2 == 0;
            float ak = 1.0f * size, aw = f.width(artists) * ak, ax = cx - aw / 2f + oe * W * 1.2f, ay = cy + 13 * k;
            Gfx.push(g); Gfx.translate(g, ax, ay); Gfx.scale(g, ak); Gfx.txt(g, f, s, 0, 0, Gfx.argb(Math.round(220 * fade), 0xD8D8F0), true); Gfx.pop(g);
            if (cursor && out < 0.3f) g.fill(Math.round(ax + f.width(s) * ak + 1), Math.round(ay), Math.round(ax + f.width(s) * ak + 1 + 2 * size), Math.round(ay + 8 * ak), Gfx.argb(Math.round(220 * fade), acc));
        }
        // a little constellation round the title, its stars appearing and joining up one by one
        float ct = t0 - 0.5f;
        if (ct > 0f) {
            int m = 7; float[][] pts = new float[m][];
            for (int i = 0; i < m; i++) { boolean top = i % 2 == 0; float px = cx - tot / 2f - 4 + (tot + 8) * (i + 0.5f) / m + (hash(seed + i * 5) - 0.5f) * 14;
                float py = top ? ty - 20 * size - hash(seed + i * 3) * 10 * size : cy + 13 * k + 15 * size + hash(seed + i * 9) * 6 * size; pts[i] = new float[]{px + oe * W * (0.8f + i * 0.05f), py}; }
            for (int i = 0; i < m; i++) {
                float sa = Math.min(1f, (ct - i * 0.09f) / 0.2f); if (sa <= 0f) continue;
                if (i > 1) { float la2 = Math.min(1f, (ct - i * 0.09f) / 0.3f); float x0 = pts[i - 2][0], y0 = pts[i - 2][1], x1 = x0 + (pts[i][0] - x0) * la2, y1 = y0 + (pts[i][1] - y0) * la2;   // joined along its own row: never across the title
                    streak(g, x0, y0, x1, y1, Gfx.argb(Math.round(70 * fade), lite)); }
                float tw = 0.7f + 0.3f * (float) Math.sin(u * 5 + i);
                NebulaTex.glow(g, pts[i][0], pts[i][1], 4 * size, 4 * size, Gfx.argb(Math.round(170 * sa * tw * fade), lite));
                g.fill(Math.round(pts[i][0]), Math.round(pts[i][1]), Math.round(pts[i][0]) + 1, Math.round(pts[i][1]) + 1, Gfx.argb(Math.round(255 * sa * fade), 0xFFFFFF));
            }
        }
        // leaving: a burst of streaks off to the right
        if (out > 0f) for (int i = 0; i < 24; i++) {
            float y = cy - 30 * size + hash(seed + i * 11) * 70 * size, x0 = cx - W * 0.4f + hash(i * 3 + 1) * W * 0.5f + oe * W, len = 20 + 120 * out * (0.5f + hash(i));
            streak(g, x0, y, x0 + len, y, Gfx.argb(Math.round(160 * a * (float) Math.sin(Math.PI * out)), i % 3 == 0 ? lite : 0xFFFFFF));
        }
    }
    /** A thin line of light from (x0, y0) to (x1, y1). */
    private static void streak(GuiGraphicsExtractor g, float x0, float y0, float x1, float y1, int argb) {
        float l = (float) Math.hypot(x1 - x0, y1 - y0); if (l < 0.5f || (argb >>> 24) < 3) return;
        Gfx.push(g); Gfx.translate(g, x0, y0); Gfx.rotate(g, (float) Math.atan2(y1 - y0, x1 - x0)); g.fill(0, 0, Math.round(l), 1, argb); Gfx.pop(g);
    }
    private static float hash(int i) { int h = i * 374761393 + 668265263; h = (h ^ (h >>> 13)) * 1274126177; return ((h ^ (h >>> 16)) & 0xFFFF) / 65535f; }
    private static void ringHalf(GuiGraphicsExtractor g, float cx, float cy, float R, float tilt, float a, boolean front) {
        float ct = (float) Math.cos(tilt), st = (float) Math.sin(tilt); int n = 40;
        for (int i = 0; i < n; i++) {
            double t = Math.PI * 2 * i / n; boolean nearSide = Math.sin(t) > 0; if (nearSide != front) continue;
            float x = (float) Math.cos(t) * R, y = (float) Math.sin(t) * R * 0.28f;
            float px = cx + x * ct - y * st, py = cy + x * st + y * ct;
            g.fill(Math.round(px), Math.round(py), Math.round(px) + 1, Math.round(py) + 1, Gfx.argb(Math.round(a * (front ? 230 : 120)), 0xE8E0D0));
        }
    }
    private static void noLyrics(GuiGraphicsExtractor g, Font f, long now, float vis, float cx, float cy, float k) {
        float u = (now - titleAt) / 1000f - INTRO; if (u < 0f) return;
        float a = Math.min(1f, u / 0.6f) * Math.max(0f, Math.min(1f, (6f - u) / 1f)) * vis; if (a < 0.02f) return;
        text(g, f, "♪  No lyrics for this one  ♪", cx, cy, k, Gfx.argb(Math.round(230 * a), colour()));
        text(g, f, "Just enjoy the music", cx, cy + 12 * k, k * 0.7f, Gfx.argb(Math.round(170 * a), 0xD8D8E8));
    }

    static void text(GuiGraphicsExtractor g, Font f, String s, float cx, float y, float k, int argb) {
        Gfx.push(g); Gfx.translate(g, cx, y); Gfx.scale(g, k); Gfx.txt(g, f, s, -f.width(s) / 2, 0, argb, true); Gfx.pop(g);
    }
    private static float ease(float v) { return 1f - (1f - v) * (1f - v) * (1f - v); }
    /** 0 -> 1 with a small overshoot: the pop. */
    private static float spring(float p) { return 1f - (float) (Math.exp(-6 * p) * Math.cos(9 * p)); }
}
