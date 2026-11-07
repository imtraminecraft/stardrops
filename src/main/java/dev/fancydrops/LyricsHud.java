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

    static final int POP = 0, KARAOKE = 1, SCROLL = 2;

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
        long pos = demo ? now % 9000 : Spotify.position() + c.lyricsOffset;
        draw(g, mc.font, c, L, on, demo, pos, now, vis);
    }
    static void draw(GuiGraphicsExtractor g, Font f, FancyConfig c, List<Lyrics.Line> L, boolean on, boolean demo, long pos, long now, float vis) {
        int W = g.guiWidth(), H = g.guiHeight(); float k = c.lyricsSize / 100f, cx = W * c.lineX, cy = H * c.lyY;
        if (!demo && c.lyricsIntro) titleCard(g, f, W, H, now, vis, k);
        if (!demo && on && L.isEmpty() && Lyrics.status.startsWith("No synced")) { noLyrics(g, f, now, vis, cx, cy, k); return; }
        if (L.isEmpty() || !on) { bx0 = bx1 = 0; return; }
        int li = at(L, pos);
        if (li != lastLine) { prevLine = lastLine; lastLine = li; lineAt = now; }
        float since = (now - lineAt) / 1000f, out = Math.min(1f, since / 0.35f);
        int maxW = Math.round(Math.min(W * 0.72f, 360f) / k);
        bx0 = Math.round(cx - maxW / 2f * k); bx1 = Math.round(cx + maxW / 2f * k); by0 = Math.round(cy - 6 * k); by1 = Math.round(cy + 22 * k);
        if (c.lyricsStyle == KARAOKE) {
            if (prevLine >= 0 && prevLine < L.size() && out < 1f) karaoke(g, f, L, prevLine, Long.MAX_VALUE / 4, cx, cy - 12 * k * ease(out), k, (1f - out) * vis, maxW);
            if (li >= 0 && li < L.size()) karaoke(g, f, L, li, pos, cx, cy + 4 * k * (1f - ease(out)), k, ease(out) * vis, maxW);
            if (c.lyricsNext && li + 1 < L.size()) faint(g, f, L.get(li + 1).text(), cx, cy + 17 * k, k * 0.75f, 0.45f * vis, maxW);
        } else if (c.lyricsStyle == SCROLL) {
            float glide = 1f - ease(out), step = 22 * k;                                     // the sheet moves up one line as the new one arrives
            for (int d = -2; d <= 2; d++) {
                int idx = li + d; if (idx < 0 || idx >= L.size()) continue;
                if (d == 2 && glide < 0.01f) continue;
                float y = cy + d * step + glide * step, dist = Math.abs(d + glide);
                float a = Math.max(0f, 1f - dist * 0.55f) * vis, sc = k * (1.15f - Math.min(1f, dist) * 0.4f);
                if (a < 0.03f) continue;
                if (d == 0) { line(g, f, L, idx, pos, cx, y, sc, a, maxW, c, true); }
                else faint(g, f, L.get(idx).text(), cx, y - 2 * k, sc, a * 0.75f, Math.round(maxW * k / Math.max(0.3f, sc)));
            }
        } else {
            if (prevLine >= 0 && prevLine < L.size() && out < 1f) line(g, f, L, prevLine, Long.MAX_VALUE / 4, cx, cy - 14 * k * ease(out), k, (1f - out) * vis, maxW, c, false);
            if (li >= 0 && li < L.size()) line(g, f, L, li, pos, cx, cy, k, vis, maxW, c, false);
            if (c.lyricsNext && li + 1 < L.size()) faint(g, f, L.get(li + 1).text(), cx, cy + 17 * k, k * 0.7f, 0.35f * vis, maxW);
        }
    }
    private static int at(List<Lyrics.Line> l, long ms) { int i = -1; for (int k = 0; k < l.size(); k++) { if (l.get(k).atMs() <= ms) i = k; else break; } return i; }
    private static final List<Lyrics.Line> DEMO = List.of(new Lyrics.Line(0, "The words pop in as they're sung"), new Lyrics.Line(3000, "light and easy to read"), new Lyrics.Line(6000, "drag them anywhere you like"));

    /** When the letter at offset ch (of total) is sung: spread over how long the line takes to sing, not the whole gap. */
    private static long wordAt(long start, long end, int ch, int total) {
        long span = Math.max(300, Math.min((long) ((end - start) * 0.85f), total * 85L + 250));
        return start + span * ch / Math.max(1, total);
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
                long wa = wordAt(start, end, ch, total), nx = wordAt(start, end, ch + w.length() + 1, total);
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
        if (barX0 >= 0) {                                                                    // the bar under the word being sung, gliding to it
            if (BAR_LINE != li) { BAR_LINE = li; barA = barX0; barB = barX1; }
            barA += (barX0 - barA) * 0.35f; barB += (barX1 - barB) * 0.35f;
            g.fill(Math.round(barA), Math.round(barY), Math.round(barB), Math.round(barY) + 1, Gfx.argb(Math.round(230 * a), acc));
            NebulaTex.glow(g, (barA + barB) / 2f, barY + 0.5f, (barB - barA) * 0.6f + 2, 2.5f, Gfx.argb(Math.round(120 * a), acc));
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
                long ca = wordAt(start, end, ch, total), cb = wordAt(start, end, ch + 1, total);
                float fill = Math.max(0f, Math.min(1f, (pos - ca) / (float) Math.max(1, cb - ca)));
                if (cs.equals(" ")) { x += cw; ch++; continue; }
                outlined(g, f, cs, x + cw / 2f, y0, kk, a * 0.7f, 0x8A8EA0);
                if (fill > 0f) outlined(g, f, cs, x + cw / 2f, y0, kk, a * fill, fill >= 1f ? Ritual.mixc(acc, 0xFFFFFF, 0.25f) : acc);
                if (fill > 0f && fill < 1f) { dotX = x + cw / 2f; dotY = y0; }
                x += cw; ch++;
            }
            ch++; y0 += rowH;
        }
        if (dotX >= 0) {                                                                     // the bouncing dot
            float b = Math.abs((float) Math.sin(pos / 140.0)) * 3 * kk;
            NebulaTex.glow(g, dotX, dotY - 4 * kk - b, 3.5f * kk, 3.5f * kk, Gfx.argb(Math.round(200 * a), acc));
            g.fill(Math.round(dotX - kk), Math.round(dotY - 5 * kk - b), Math.round(dotX + kk), Math.round(dotY - 3 * kk - b), Gfx.argb(Math.round(255 * a), 0xFFFFFF));
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
