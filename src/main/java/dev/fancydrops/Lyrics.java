package dev.fancydrops;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Time-synced lyrics for the song Spotify is playing, from LRCLIB (lrclib.net, a free public lyrics library). Nothing is stored in the mod:
 * each song's lines are fetched when it starts and kept in memory for the session.
 */
final class Lyrics {
    private Lyrics() {}

    record Line(long atMs, String text) {}
    private static final Map<String, List<Line>> CACHE = new HashMap<>();
    static volatile List<Line> lines = List.of();
    static volatile String forTrack = "", status = "";
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NORMAL).build();
    private static final Pattern TAG = Pattern.compile("\\[(\\d+):(\\d+(?:\\.\\d+)?)\\]");
    private static final Pattern WORD = Pattern.compile("<(\\d+):(\\d+(?:\\.\\d+)?)>");
    /** Lines that came with word timing: "atMs|text" -> when each letter is sung. */
    static final Map<String, long[]> WORDS = new java.util.concurrent.ConcurrentHashMap<>();
    static long[] wordTimes(long atMs, String text) { return WORDS.isEmpty() ? null : WORDS.get(atMs + "|" + text); }

    static void load(Spotify.Track t) {
        forTrack = t.id(); lines = List.of();
        synchronized (CACHE) { if (CACHE.containsKey(t.id())) { lines = CACHE.get(t.id()); status = lines.isEmpty() ? "No synced lyrics for this song." : ""; return; } }
        status = "Looking for the lyrics...";
        Thread th = new Thread(() -> {
            List<Line> got = List.of(); String src = "";
            try { got = netease(t); if (!got.isEmpty()) src = "Word-synced lyrics from NetEase"; } catch (Throwable e) { System.out.println("[FancyDrops] lyrics: NetEase: " + e); }
            if (got.isEmpty()) try {
                String q = "track_name=" + Spotify.enc(t.name()) + "&artist_name=" + Spotify.enc(firstArtist(t.artists())) + "&album_name=" + Spotify.enc(t.album()) + "&duration=" + Math.round(t.durationMs() / 1000.0);
                JsonElement e = get("https://lrclib.net/api/get?" + q);
                if (e != null && e.isJsonObject()) got = parse(e.getAsJsonObject());
                if (got.isEmpty()) {                                                          // not found exactly: search, and take the closest length that has synced lines
                    JsonElement s = get("https://lrclib.net/api/search?track_name=" + Spotify.enc(t.name()) + "&artist_name=" + Spotify.enc(firstArtist(t.artists())));
                    if (s != null && s.isJsonArray()) {
                        JsonArray arr = s.getAsJsonArray(); double best = Double.MAX_VALUE;
                        for (JsonElement x : arr) {
                            JsonObject o = x.getAsJsonObject(); List<Line> l = parse(o); if (l.isEmpty()) continue;
                            double d = Math.abs((o.has("duration") && !o.get("duration").isJsonNull() ? o.get("duration").getAsDouble() : 0) - t.durationMs() / 1000.0);
                            if (d < best) { best = d; got = l; }
                        }
                    }
                }
            } catch (Throwable ignored) { }
            synchronized (CACHE) { CACHE.put(t.id(), got); }
            System.out.println("[FancyDrops] lyrics: " + t.name() + ": " + (got.isEmpty() ? "none" : src.isEmpty() ? "LRCLIB, " + got.size() + " lines" : src));
            if (forTrack.equals(t.id())) { lines = got; status = got.isEmpty() ? "No synced lyrics for this song." : src; }
        }, "FancyDrops lyrics");
        th.setDaemon(true); th.start();
    }
    private static String firstArtist(String a) { int c = a.indexOf(','); return c < 0 ? a : a.substring(0, c); }
    private static JsonElement get(String url) throws Exception {
        HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(url)).header("User-Agent", "FancyDrops (github.com/imtraminecraft/fancydrops)").GET().build(), HttpResponse.BodyHandlers.ofString());
        return r.statusCode() == 200 ? JsonParser.parseString(r.body()) : null;
    }
    // ------------------------------------------------------------------ NetEase Cloud Music: many songs have every word timed (their "yrc" lyrics)
    private static String norm(String s) { return s.toLowerCase(Locale.ROOT).replaceAll("\\(.*?\\)|\\[.*?\\]|-.*remaster.*|feat\\..*", "").replaceAll("[^a-z0-9\\u00C0-\\uFFFF]", ""); }
    private static JsonElement ne(String url) throws Exception {
        HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(java.time.Duration.ofSeconds(8)).header("User-Agent", "Mozilla/5.0").header("Referer", "https://music.163.com/").GET().build(), HttpResponse.BodyHandlers.ofString());
        return r.statusCode() == 200 ? JsonParser.parseString(r.body()) : null;
    }
    /** Finds the song on NetEase (same title, same first artist, length within 4 s) and reads its word-timed lyrics; empty if it has none. */
    static List<Line> netease(Spotify.Track t) throws Exception {
        String want = norm(t.name()), artist = norm(firstArtist(t.artists()));
        JsonElement e = ne("https://music.163.com/api/search/get?type=1&limit=10&s=" + Spotify.enc(t.name() + " " + firstArtist(t.artists())));
        if (e == null || !e.isJsonObject() || !e.getAsJsonObject().has("result")) return List.of();
        JsonObject res = e.getAsJsonObject().getAsJsonObject("result"); if (!res.has("songs")) return List.of();
        long best = -1; double bestD = 4.5;
        for (JsonElement x : res.getAsJsonArray("songs")) {
            JsonObject so = x.getAsJsonObject(); if (!norm(so.get("name").getAsString()).equals(want)) continue;
            boolean art = false; if (so.has("artists")) for (JsonElement a : so.getAsJsonArray("artists")) if (norm(a.getAsJsonObject().get("name").getAsString()).equals(artist)) art = true;
            if (!art) continue;
            double d = Math.abs((so.has("duration") ? so.get("duration").getAsLong() : 0) - t.durationMs()) / 1000.0;
            if (d < bestD) { bestD = d; best = so.get("id").getAsLong(); }
        }
        if (best < 0) return List.of();
        JsonElement l = ne("https://music.163.com/api/song/lyric?id=" + best + "&lv=1&yv=1&tv=-1");
        if (l == null || !l.isJsonObject() || !l.getAsJsonObject().has("yrc")) return List.of();
        JsonObject yo = l.getAsJsonObject().getAsJsonObject("yrc"); if (!yo.has("lyric") || yo.get("lyric").isJsonNull()) return List.of();
        return parseYrc(yo.get("lyric").getAsString());
    }
    private static final Pattern YLINE = Pattern.compile("^\\[(\\d+),(\\d+)\\](.*)$"), YWORD = Pattern.compile("\\((\\d+),(\\d+),\\d+\\)([^(]*)");
    /** "[16210,3460](16210,670,0)Every(16880,410,0) day" : a line's start and length, then each word's start and length. */
    static List<Line> parseYrc(String yrc) {
        List<Line> out = new ArrayList<>();
        for (String raw : yrc.split("\\R")) {
            Matcher m = YLINE.matcher(raw.trim()); if (!m.matches()) continue;                // (credit lines are JSON: skipped)
            long st = Long.parseLong(m.group(1)), dur = Long.parseLong(m.group(2));
            Matcher w = YWORD.matcher(m.group(3)); StringBuilder sb = new StringBuilder(); List<long[]> anchors = new ArrayList<>();
            while (w.find()) { String word = w.group(3); long ws = Long.parseLong(w.group(1)), wd = Long.parseLong(w.group(2)); anchors.add(new long[]{sb.length(), ws, wd, word.length()}); sb.append(word); }
            String raw2 = sb.toString(), text = raw2.stripTrailing(); if (text.isBlank()) continue;
            if (text.startsWith(" ") || text.contains("  ")) { text = text.replaceAll("\\s+", " ").trim(); out.add(new Line(st, text)); continue; }   // (odd spacing: keep the line, guess the words)                 // (odd spacing: keep the line, guess the words)
            long[] times = new long[text.length() + 1];
            for (long[] a : anchors) for (int i = 0; i <= a[3]; i++) { int ci = (int) a[0] + i; if (ci <= text.length()) times[ci] = a[1] + (a[3] == 0 ? 0 : a[2] * i / a[3]); }   // each letter across its word's length
            for (int i = 1; i <= text.length(); i++) if (times[i] == 0) times[i] = times[i - 1];
            WORDS.put(st + "|" + text, times);
            out.add(new Line(st, text));
            if (dur > 0) out.add(new Line(st + dur + 1500, ""));                                 // a long gap after it: the line clears instead of hanging
        }
        out.sort(Comparator.comparingLong(Line::atMs));
        List<Line> clean = new ArrayList<>();                                                  // a blank only stays if nothing else starts before it ends
        for (int i = 0; i < out.size(); i++) { Line a = out.get(i); if (a.text().isEmpty() && i + 1 < out.size() && out.get(i + 1).atMs() <= a.atMs()) continue; clean.add(a); }
        return clean;
    }
    /** Nudges the words of the song playing now earlier (+) or later (-), remembered for that song, and says so above the hotbar. */
    static void nudge(net.minecraft.client.Minecraft mc, int ms) {
        Spotify.Track t = Spotify.track; if (t == null) return;
        FancyConfig c = FancyConfig.get(); if (c.songOffset == null) c.songOffset = new LinkedHashMap<>();
        int v = Math.max(-5000, Math.min(5000, c.songOffset.getOrDefault(t.id(), 0) + ms)); if (v == 0) c.songOffset.remove(t.id()); else c.songOffset.put(t.id(), v);
        FancyConfig.save();
        if (mc.player != null) mc.player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("♪ Lyrics for " + t.name() + ": " + (v > 0 ? "+" : "") + String.format(Locale.ROOT, "%.1f s", v / 1000.0)).withStyle(s -> s.withColor(0x1ED760)));
    }

    /** "[01:02.34] some words" lines, in time order; empty lines become pauses. */
    static List<Line> parse(JsonObject o) {
        if (!o.has("syncedLyrics") || o.get("syncedLyrics").isJsonNull()) return List.of();
        List<Line> out = new ArrayList<>();
        for (String raw : o.get("syncedLyrics").getAsString().split("\\R")) {
            Matcher m = TAG.matcher(raw); List<Long> at = new ArrayList<>(); int end = 0;
            while (m.find()) { at.add(Long.parseLong(m.group(1)) * 60000L + Math.round(Double.parseDouble(m.group(2)) * 1000)); end = m.end(); }
            String text = raw.substring(end).trim();
            if (text.indexOf('<') >= 0) {                                                       // word-by-word timing ("enhanced" LRC: <01:02.34>word): kept, so the words are sung exactly
                StringBuilder sb = new StringBuilder(); List<long[]> anchors = new ArrayList<>(); Matcher w = WORD.matcher(text); int last = 0;
                while (w.find()) { sb.append(text, last, w.start()); anchors.add(new long[]{sb.length(), Long.parseLong(w.group(1)) * 60000L + Math.round(Double.parseDouble(w.group(2)) * 1000)}); last = w.end(); }
                sb.append(text.substring(last)); String clean = sb.toString().replaceAll("\\s+", " ").trim();
                if (!anchors.isEmpty() && clean.length() > 0) {
                    int lead = sb.length() - sb.toString().stripLeading().length(); long[] times = new long[clean.length() + 1];
                    for (int i = 0; i <= clean.length(); i++) {
                        int ci = i + lead; long[] a0 = null, a1 = null;
                        for (long[] a : anchors) { if (a[0] <= ci) a0 = a; else { a1 = a; break; } }
                        if (a0 == null) times[i] = anchors.get(0)[1];
                        else if (a1 == null) times[i] = a0[1] + (ci - a0[0]) * 80L;
                        else times[i] = a0[1] + (a1[1] - a0[1]) * (ci - a0[0]) / Math.max(1, a1[0] - a0[0]);
                    }
                    for (long a : at) WORDS.put(a + "|" + clean, times);
                }
                text = clean;
            }
            for (long a : at) out.add(new Line(a, text));
        }
        out.sort(Comparator.comparingLong(Line::atMs));
        return out;
    }
    /** The line sung at ms (-1 before the first). */
    static int at(long ms) { List<Line> l = lines; int i = -1; for (int k = 0; k < l.size(); k++) { if (l.get(k).atMs() <= ms) i = k; else break; } return i; }
}
