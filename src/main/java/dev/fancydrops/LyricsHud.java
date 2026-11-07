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
            if (li >= 0 && li < L.size()) karaoke(g, f, L, li, pos, cx, cy + 4 * k * (1f - ease(out)), k, ease(out) * vis, maxW);
            beatUnder(g, f, L, li, cx, cy + rowsBelow(f, L, li, maxW, k) + 3 * k, k, vis, maxW, pos);
            if (c.lyricsNext && li + 1 < L.size()) faint(g, f, L.get(li + 1).text(), cx, cy + rowsBelow(f, L, li, maxW, k) + 9 * k, k * 0.75f, 0.45f * vis, maxW);
        } else if (c.lyricsStyle == SCROLL) {
            float glide = 1f - ease(out), step = 22 * k;                                     // the sheet moves up one line as the new one arrives
            for (int d = -2; d <= 2; d++) {
                int idx = li + d; if (idx < 0 || idx >= L.size()) continue;
                if (d == 2 && glide < 0.01f) continue;
                if (d < 0 && !c.lyricsPrev) continue;                                       // the lines already sung, only if you want them
                float y = cy + d * step + glide * step, dist = Math.abs(d + glide);
                float a = Math.max(0f, 1f - dist * 0.55f) * vis, sc = k * (1.15f - Math.min(1f, dist) * 0.4f);
                if (a < 0.03f) continue;
                if (d == 0) { line(g, f, L, idx, pos, cx, y, sc, a, maxW, c, true); beatUnder(g, f, L, idx, cx, y + rowsBelow(f, L, idx, maxW, sc) + 2 * k, sc, a, maxW, pos); }
                else faint(g, f, L.get(idx).text(), cx, y - 2 * k, sc, a * 0.75f, Math.round(maxW * k / Math.max(0.3f, sc)));
            }
        } else {
            if (c.lyricsPrev && prevLine >= 0 && prevLine < L.size() && out < 1f) line(g, f, L, prevLine, Long.MAX_VALUE / 4, cx, cy - 14 * k * ease(out), k, (1f - out) * vis, maxW, c, false);
            if (li >= 0 && li < L.size()) line(g, f, L, li, pos, cx, cy, k, vis, maxW, c, false);
            beatUnder(g, f, L, li, cx, cy + rowsBelow(f, L, li, maxW, k) + 3 * k, k, vis, maxW, pos);
            if (c.lyricsNext && li + 1 < L.size()) faint(g, f, L.get(li + 1).text(), cx, cy + rowsBelow(f, L, li, maxW, k) + 9 * k, k * 0.7f, 0.35f * vis, maxW);
        }
    }
    /** How far below the line's middle its last row ends. */
    private static float rowsBelow(Font f, List<Lyrics.Line> L, int li, int maxW, float k) {
        if (li < 0 || li >= L.size()) return 10 * k;
        float[] q = new float[2]; List<String> rows = rowsOf(f, L.get(li).text(), maxW, k * 1.2f, q); float kk = q[0];
        return (rows.size() - 1) * 12 * kk / 2f + 10 * kk;
    }
    /** The beat line under a line of lyrics, about as wide as the words. */
    private static void beatUnder(GuiGraphicsExtractor g, Font f, List<Lyrics.Line> L, int li, float cx, float y, float k, float a, int maxW, long pos) {
        float w = li >= 0 && li < L.size() ? Math.min(f.width(L.get(li).text()) * k * 1.2f, maxW * k) * 0.6f : 60 * k;
        Beat.line(g, cx, y, Math.max(48 * k, Math.min(150 * k, w)), k, a, colour(), pos);
    }
    private static int at(List<Lyrics.Line> l, long ms) { int i = -1; for (int k = 0; k < l.size(); k++) { if (l.get(k).atMs() <= ms) i = k; else break; } return i; }
    private static final List<Lyrics.Line> DEMO = List.of(new Lyrics.Line(0, "The words pop in as they're sung"), new Lyrics.Line(3000, "light and easy to read"), new Lyrics.Line(6000, "drag them anywhere you like"));

    /**
     * When the letter at offset ch of a line is sung. LRCLIB only times whole lines, so the words inside are paced the way people
     * sing: every letter takes a beat, every syllable a little more, and a comma, dash or full stop is a held note or a breath
     * ("Home, home, where I wanted to go" waits on each "home"). The line is spread over the time until the next line (most of it),
     * except after a long gap (an instrumental), where it is sung at a natural pace and then simply waits.
     */
    private static long wordAt(String text, long start, long end, int ch) {
        long[] exact = Lyrics.wordTimes(start, text);                                        // word-timed lyrics: exactly when each letter is sung
        if (exact != null) return exact[Math.max(0, Math.min(exact.length - 1, ch))];
        float[] cum = PACE.computeIfAbsent(text, LyricsHud::pace);
        int n = cum.length - 1; float tot = Math.max(1f, cum[n]);
        long gap = Math.max(300, end - start);
        long span = Math.max(300, Math.min((long) (gap * 0.92f), (long) (tot * 105 + 600)));
        if (gap <= 7000) span = Math.max(span, (long) (gap * 0.85f));
        return start + (long) (span * cum[Math.max(0, Math.min(n, ch))] / tot);
    }
    private static final java.util.Map<String, float[]> PACE = new java.util.HashMap<>();
    /** cum[i] = how much of the line is sung before letter i (in letter-beats). */
    private static float[] pace(String t) {
        float[] cum = new float[t.length() + 1]; float acc = 0; boolean prevVowel = false;
        for (int i = 0; i < t.length(); i++) {
            cum[i] = acc; char c = Character.toLowerCase(t.charAt(i));
            boolean vowel = "aeiouy".indexOf(c) >= 0;
            if (Character.isLetterOrDigit(c)) { acc += 1f; if (vowel && !prevVowel) acc += 1.6f; }       // a letter; a new syllable
            else if (c == ' ') acc += 0.6f;
            else if (c == ',' || c == ';' || c == ':') acc += 7f;                                             // a held note / a breath
            else if (c == '.' || c == '!' || c == '?' || c == '-' || c == '\u2014' || c == '\u2026') acc += 5f;
            prevVowel = vowel;
        }
        cum[t.length()] = acc;
        if (PACE.size() > 400) PACE.clear();
        return cum;
    }

    /** The rows of a line (at most two) and the scale that fits them. */
    private static List<String> rowsOf(Font f, String text, int maxW, float k, float[] kk) {
        List<String> rows = wrap(f, text, maxW); int widest = 0; for (String r : rows) widest = Math.max(widest, f.width(r));
        kk[0] = widest > maxW ? k * maxW / (float) widest : k; kk[1] = widest; return rows;
    }
    /** The shade behind a line: soft, wide, never a box. */
    private static void shade(GuiGraphicsExtractor g, float cx, float cy, float w, float kk, int rows, float a) {
        NebulaTex.glow(g, cx, cy + 4 * kk, w * 0.58f + 14, 10 * kk + rows * 6 * kk, Gfx.argb(Math.round(95 * a), 0x000000));
    }
    /** Pop (and Scroll's middle line): words arrive as they're sung; the sung word glows in the album's colour with a bar sliding under it. */
    private static void line(GuiGraphicsExtractor g, Font f, List<Lyrics.Line> L, int li, long pos, float cx, float cy, float k, float a, int maxW, FancyConfig c, boolean all) {
        if (a < 0.02f) return;
        String text = L.get(li).text(); if (text.isBlank()) return;
        long start = L.get(li).atMs(), end = li + 1 < L.size() ? L.get(li + 1).atMs() : start + 5000;
        float[] q = new float[2]; List<String> rows = rowsOf(f, text, maxW, k * 1.2f, q); float kk = q[0];
        int acc = colour(), total = text.length(), ch = 0;
        float rowH = 12 * kk, y0 = cy - (rows.size() - 1) * rowH / 2f;
        shade(g, cx, cy, q[1] * kk, kk, rows.size(), a);
        float barX0 = -1, barX1 = -1, barY = 0;
        for (String row : rows) {
            float x = cx - f.width(row) * kk / 2f, y = y0;
            for (String w : row.split(" ")) {
                if (w.isEmpty()) { x += f.width(" ") * kk; ch++; continue; }
                long wa = wordAt(text, start, end, ch), nx = wordAt(text, start, end, ch + w.length() + 1);
                float u = (pos - wa) / 1000f, ww = f.width(w) * kk; boolean singing = pos >= wa && pos < nx;
                if (all || u >= 0f) {
                    float p = all ? 1f : Math.min(1f, u / 0.22f), s = p < 1f ? 0.6f + 0.4f * spring(p) : 1f;
                    boolean sung = pos >= wa;
                    int col = singing ? Ritual.mixc(acc, 0xFFFFFF, 0.15f) : sung ? Ritual.mixc(acc, 0xFFFFFF, Math.min(1f, (pos - nx) / 500f + 0.55f)) : 0xB8BCC8;
                    if (singing) { NebulaTex.glow(g, x + ww / 2f, y + 4 * kk, ww * 0.7f + 4, 7 * kk, Gfx.argb(Math.round(70 * a), acc)); barX0 = x; barX1 = x + ww; barY = y + 10 * kk; }
                    outlined(g, f, w, x + ww / 2f, y + (1f - p) * 4 * kk, kk * s, a * Math.min(1f, p * 2f) * (sung || all ? 1f : 0.6f), col);
                    if (c.lyricsStars && !all && u >= 0 && u < 0.6f) AmbienceHud.sparkle(g, x + ww / 2f, y + 3 * kk, ww, u / 0.6f, a * 0.8f, (li * 31 + ch) & 0xFFFF, kk * 0.8f);
                }
                x += ww + f.width(" ") * kk; ch += w.length() + 1;
            }
            y0 += rowH;
        }
    }
    private static int BAR_LINE = -9; private static float barA, barB;
    /** Karaoke: the line in grey, a wipe of colour running through it letter by letter, a dot bouncing over the word being sung. */
    private static void karaoke(GuiGraphicsExtractor g, Font f, List<Lyrics.Line> L, int li, long pos, float cx, float cy, float k, float a, int maxW) {
        if (a < 0.02f) return;
        String text = L.get(li).text(); if (text.isBlank()) return;
        long start = L.get(li).atMs(), end = li + 1 < L.size() ? L.get(li + 1).atMs() : start + 5000;
        float[] q = new float[2]; List<String> rows = rowsOf(f, text, maxW, k * 1.2f, q); float kk = q[0];
        int acc = colour(), total = text.length(), ch = 0;
        float rowH = 12 * kk, y0 = cy - (rows.size() - 1) * rowH / 2f;
        shade(g, cx, cy, q[1] * kk, kk, rows.size(), a);
        float dotX = -1, dotY = 0, dotU = 0;
        for (String row : rows) {
            float x = cx - f.width(row) * kk / 2f;
            for (int i = 0; i < row.length(); i++) {
                String cs = String.valueOf(row.charAt(i)); float cw = f.width(cs) * kk;
                long ca = wordAt(text, start, end, ch), cb = wordAt(text, start, end, ch + 1);
                float fill = Math.max(0f, Math.min(1f, (pos - ca) / (float) Math.max(1, cb - ca)));
                if (cs.equals(" ")) { x += cw; ch++; continue; }
                outlined(g, f, cs, x + cw / 2f, y0, kk, a * 0.7f, 0x8A8EA0);
                if (fill > 0f) outlined(g, f, cs, x + cw / 2f, y0, kk, a * fill, fill >= 1f ? Ritual.mixc(acc, 0xFFFFFF, 0.25f) : acc);
                if (fill > 0f && fill < 1f) { dotX = x + cw / 2f; dotY = y0; }
                x += cw; ch++;
            }
            ch++; y0 += rowH;
        }
    }
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
        outlined(g, f, s, cx, y, k, a, 0xD8DCE6);
    }
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
        NowPlayingHud.round(g, Math.round(sx), Math.round(y), Math.round(sw), Math.round(sh), Gfx.argb(Math.round(150 * e * s), 0x2A2C38));          // the same plain glass as the card
        NowPlayingHud.round(g, Math.round(sx) + 1, Math.round(y) + 1, Math.round(sw) - 2, Math.round(sh) - 2, Gfx.argb(Math.round(242 * e * s), 0x0D0E13));

        g.fill(Math.round(sx + 3), Math.round(above ? y : y + sh - 1), Math.round(sx + sw - 3), Math.round(above ? y + 1 : y + sh), Gfx.argb(Math.round(40 * e * s), 0xFFFFFF));   // the lit outer edge
        float bob = (float) Math.sin(now / 260.0) * 1f;                                       // the note, in a soft round light
        NebulaTex.glow(g, sx + 9, y + sh / 2f, 6, 6, Gfx.argb(Math.round(a * 0.55f), acc));
        Gfx.push(g); Gfx.translate(g, sx + 6.5f, y + sh / 2f - 4 + bob); Gfx.txt(g, f, "♪", 0, 0, Gfx.argb(a, Ritual.mixc(acc, 0xFFFFFF, 0.6f)), false); Gfx.pop(g);
        // the words: the old line rolls up and away, the new one rolls in from below
        float roll = Math.min(1f, (now - dockAt) / 280f); roll = 1f - (1f - roll) * (1f - roll) * (1f - roll);
        int tx0 = Math.round(sx + 17), tw = Math.round(sw - 24);
        if (dockPrev >= 0 && dockPrev < L.size() && roll < 1f) dockText(g, f, L, dockPrev, Long.MAX_VALUE / 4, tx0, tw, y + sh / 2f - 4 - 6 * roll, Math.round(a * (1f - roll)), acc);
        if (li >= 0 && li < L.size()) dockText(g, f, L, li, pos, tx0, tw, y + sh / 2f - 4 + 6 * (1f - roll), Math.round(a * roll), acc);
        Beat.line(g, sx + 17 + (sw - 24) / 2f, above ? y + sh - 2.5f : y + 1.5f, (sw - 24) * 0.55f, 0.7f, a / 255f, acc, pos);        // the beat, along the inner edge
    }
    /** One line in the strip, left to right: sung letters in the album's colour, the rest grey, scaled down to fit. */
    private static void dockText(GuiGraphicsExtractor g, Font f, List<Lyrics.Line> L, int li, long pos, int x, int tw, float y, int a, int acc) {
        if (a < 6) return;
        String text = L.get(li).text(); long start = L.get(li).atMs(), end = li + 1 < L.size() ? L.get(li + 1).atMs() : start + 5000;
        float k = Math.max(0.62f, Math.min(0.9f, tw / (float) Math.max(1, f.width(text))));
        if (f.width(text) * k > tw) text = f.plainSubstrByWidth(text, Math.round(tw / k) - f.width("…")) + "…";
        float cx = x + (tw - f.width(text) * k) / 2f; int total = text.length(); int sung = Ritual.mixc(acc, 0xFFFFFF, 0.3f);
        Gfx.push(g); Gfx.translate(g, cx, y + (1f - k) * 4); Gfx.scale(g, k);
        int px = 0; StringBuilder run = new StringBuilder(); int runCol = 0, runX = 0;
        for (int i = 0; i <= total; i++) {
            int col = 0;
            if (i < total) { long ca = wordAt(text, start, end, i); col = pos >= ca ? 1 : 2; }
            if (i == total || col != runCol) {
                if (run.length() > 0) Gfx.txt(g, f, run.toString(), runX, 0, Gfx.argb(runCol == 1 ? a : Math.round(a * 0.55f), runCol == 1 ? sung : 0xC6CAD6), true);
                run.setLength(0); runCol = col; runX = px;
            }
            if (i < total) { String ch = String.valueOf(text.charAt(i)); run.append(ch); px += f.width(ch); }
        }
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
    /** The song intro's record: it pops up beside the title, spins, a tone arm swings onto it and notes drift off it. */
    private static void vinyl(GuiGraphicsExtractor g, Font font, float cx, float cy, float u, float a, float size) {
        float pop = spring(Math.min(1f, u / 0.6f)), r = 17 * size * Math.max(0f, pop); if (r < 1f || a < 0.02f) return;
        int acc = colour(), A = Math.round(255 * a);
        cy += (1f - Math.min(1f, u / 0.35f)) * 18 * size;                                    // it rises as it pops
        float spin = u * 5.5f;
        NebulaTex.glow(g, cx, cy, r * 2.2f, r * 2.2f, Gfx.argb(Math.round(110 * a), acc));
        NowPlayingHud.disc(g, cx, cy + 2, r, Gfx.argb(Math.round(90 * a), 0x000000));
        NowPlayingHud.disc(g, cx, cy, r, Gfx.argb(A, 0x111116));
        for (int i = 0; i < 4; i++) NowPlayingHud.circle(g, cx, cy, r * (0.5f + i * 0.13f), Gfx.argb(Math.round(70 * a), 0x50566A));
        NowPlayingHud.disc(g, cx, cy, r * 0.36f, Gfx.argb(A, acc));
        NowPlayingHud.disc(g, cx, cy, r * 0.36f * 0.55f, Gfx.argb(Math.round(90 * a), 0xFFFFFF));
        NowPlayingHud.disc(g, cx, cy, Math.max(1f, r * 0.06f), Gfx.argb(A, 0x111116));
        for (int i = 0; i < 2; i++) {                                                        // the shine sweeping round with the spin
            double an = spin + i * Math.PI; float gx = cx + (float) Math.cos(an) * r * 0.72f, gy = cy + (float) Math.sin(an) * r * 0.72f;
            NebulaTex.glow(g, gx, gy, r * 0.35f, r * 0.18f, Gfx.argb(Math.round(70 * a), 0xFFFFFF));
        }
        // the tone arm: from a pivot at the top right it swings onto the record
        float sw = Math.min(1f, Math.max(0f, (u - 0.35f) / 0.5f)); sw = 1f - (1f - sw) * (1f - sw);
        float px = cx + r * 1.05f, py = cy - r * 0.95f, an = (float) Math.toRadians(150 - 35 * sw), len = r * 1.15f;
        float ex = px + (float) Math.cos(an) * len, ey = py + (float) Math.sin(an) * len;
        Gfx.push(g); Gfx.translate(g, px, py); Gfx.rotate(g, an); g.fill(0, -1, Math.round(len), 1, Gfx.argb(A, 0xC8CCD8)); Gfx.pop(g);
        g.fill(Math.round(ex) - 2, Math.round(ey) - 2, Math.round(ex) + 2, Math.round(ey) + 2, Gfx.argb(A, 0xE8ECF4));
        NowPlayingHud.disc(g, px, py, Math.max(1.5f, r * 0.16f), Gfx.argb(A, 0x6A7084));
        if (sw >= 1f) for (int i = 0; i < 3; i++) {                                           // notes drifting up off it
            float q = ((u - 0.85f) * 0.8f + i / 3f) % 1f; if (q < 0) continue;
            float nx = cx - r * 0.4f + i * r * 0.4f + (float) Math.sin(q * 6 + i) * 3 * size, ny = cy - r - q * 22 * size;
            Gfx.push(g); Gfx.translate(g, nx, ny); Gfx.scale(g, size * 0.8f); Gfx.txt(g, font, i % 2 == 0 ? "♪" : "♫", 0, 0, Gfx.argb(Math.round(220 * a * (1f - q)), Ritual.mixc(acc, 0xFFFFFF, 0.4f)), true); Gfx.pop(g);
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
