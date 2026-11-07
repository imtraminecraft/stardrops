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
        NowPlayingHud.round(g, Math.round(sx), Math.round(y), Math.round(sw), Math.round(sh), Gfx.argb(Math.round(170 * e * s), 0x2C2A66));          // the same glass as the card
        NowPlayingHud.round(g, Math.round(sx) + 1, Math.round(y) + 1, Math.round(sw) - 2, Math.round(sh) - 2, Gfx.argb(Math.round(240 * e * s), 0x0B0C26));

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

    private static void titleCard(GuiGraphicsExtractor g, Font f, int W, int H, long now, float vis, float k) {
        Spotify.Track t = Spotify.track; if (t == null || !Spotify.playing) return;
        float u = (now - titleAt) / 1000f; if (u > 3.5f) return;
        float a = Math.min(1f, u / 0.4f) * Math.min(1f, (3.5f - u) / 0.7f) * vis;
        intro(g, f, W, H, t.name(), t.artists(), u, a, k);
    }
    static void intro(GuiGraphicsExtractor g, Font f, int W, int H, String name, String artists, float u, float a, float size) {
        float k = size * 1.5f, cy = H * 0.26f;
        if (f.width(name) * k > W * 0.7f) name = f.plainSubstrByWidth(name, (int) (W * 0.7f / k) - 8) + "…";
        float nw = f.width(name) * k, open = ease(Math.min(1f, u / 0.6f));
        NebulaTex.glow(g, W / 2f, cy + 8 * k, nw * 0.6f + 16, 16 * k, Gfx.argb(Math.round(90 * a), 0x000000));
        g.fill(Math.round(W / 2f - nw / 2f * open), Math.round(cy - 4), Math.round(W / 2f + nw / 2f * open), Math.round(cy - 3), Gfx.argb(Math.round(180 * a), colour()));
        text(g, f, "NOW PLAYING", W / 2f, cy - 12, 0.6f, Gfx.argb(Math.round(160 * a), colour()));
        text(g, f, name, W / 2f, cy + (1f - open) * 6, k, Gfx.argb(Math.round(255 * a), 0xFFFFFF));
        text(g, f, artists, W / 2f, cy + 12 * k, k * 0.6f, Gfx.argb(Math.round(190 * a), 0xD8D8E8));
        vinyl(g, f, W / 2f - nw / 2f - 24 * k, cy + 4 * k, u, a, size);
    }
    /** The song intro: a small ringed planet rises beside the title with a spring, its ring tilted, a moon circling it, stars glinting round. */
    private static void vinyl(GuiGraphicsExtractor g, Font font, float cx, float cy, float u, float a, float size) {
        float pop = spring(Math.min(1f, u / 0.6f)), r = 11 * size * Math.max(0f, pop); if (r < 1f || a < 0.02f) return;
        int acc = colour(), A = Math.round(255 * a), body = Ritual.mixc(acc, 0x6A4AD8, 0.35f);
        cy += (1f - Math.min(1f, u / 0.35f)) * 16 * size;
        NebulaTex.glow(g, cx, cy, r * 3f, r * 3f, Gfx.argb(Math.round(90 * a), body));
        float tilt = -0.35f, ring = r * 1.9f, mo = u * 2.2f;
        ringHalf(g, cx, cy, ring, tilt, a, false);
        NowPlayingHud.disc(g, cx, cy, r, Gfx.argb(A, Ritual.mixc(body, 0x000000, 0.45f)));                       // the planet: dark side
        NowPlayingHud.disc(g, cx - r * 0.18f, cy - r * 0.18f, r * 0.82f, Gfx.argb(A, body));                         // lit side
        NowPlayingHud.disc(g, cx - r * 0.38f, cy - r * 0.38f, r * 0.35f, Gfx.argb(Math.round(a * 140), 0xFFFFFF));    // where the light catches it
        for (int i = 0; i < 3; i++) g.fill(Math.round(cx - r * 0.7f), Math.round(cy - r * 0.2f + i * r * 0.3f), Math.round(cx + r * 0.5f), Math.round(cy - r * 0.2f + i * r * 0.3f) + 1, Gfx.argb(Math.round(a * 60), 0x000000));   // bands
        ringHalf(g, cx, cy, ring, tilt, a, true);
        float mx = cx + (float) Math.cos(mo) * r * 2.4f, my = cy + (float) Math.sin(mo) * r * 0.9f;                  // the moon
        if (Math.sin(mo) > -0.2 || Math.abs(Math.cos(mo)) > 0.45) { NowPlayingHud.disc(g, mx, my, r * 0.22f, Gfx.argb(A, 0xD8DCE6)); }
        for (int i = 0; i < 4; i++) {                                                              // glints round it
            float q = ((u * 0.7f + i / 4f) % 1f), gx = cx + (float) Math.cos(i * 1.9 + 0.5) * r * (2.2f + i * 0.3f), gy = cy + (float) Math.sin(i * 1.9 + 0.5) * r * 1.4f;
            float fl = (float) Math.sin(Math.PI * q); NebulaTex.glow(g, gx, gy, 3 * size, 3 * size, Gfx.argb(Math.round(a * 200 * fl), 0xFFFFFF));
        }
    }
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
        float u = (now - titleAt) / 1000f - 3.5f; if (u < 0f) return;
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
