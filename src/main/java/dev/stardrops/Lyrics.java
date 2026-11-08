package dev.stardrops;

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
    private static final Pattern CREDIT = Pattern.compile("^\\s*(作词|作曲|编曲|制作|混音|母带|和声|吉他|贝斯|鼓|弦乐|监制|出品|词|曲|lyrics?\\s+by|written\\s+by|composed\\s+by|composer|lyricist|producer|produced\\s+by|arranger)\\s*[:：]", Pattern.CASE_INSENSITIVE);
    private static final Pattern WORD = Pattern.compile("<(\\d+):(\\d+(?:\\.\\d+)?)>");

    static void load(Spotify.Track t) {
        forTrack = t.id(); lines = List.of();
        synchronized (CACHE) { if (CACHE.containsKey(t.id())) { lines = CACHE.get(t.id()); status = lines.isEmpty() ? "No synced lyrics for this song." : ""; return; } }
        status = "Looking for the lyrics...";
        Thread th = new Thread(() -> {
            List<Line> got = List.of(); String src = "";
            try {
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
            System.out.println("[Stardrops] lyrics: " + t.name() + ": " + (got.isEmpty() ? "none" : src.isEmpty() ? "LRCLIB, " + got.size() + " lines" : src));
            if (forTrack.equals(t.id())) { lines = got; status = got.isEmpty() ? "No synced lyrics for this song." : src; }
        }, "Stardrops lyrics");
        th.setDaemon(true); th.start();
    }
    private static String firstArtist(String a) { int c = a.indexOf(','); return c < 0 ? a : a.substring(0, c); }
    private static JsonElement get(String url) throws Exception {
        HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(url)).header("User-Agent", "Stardrops (github.com/imtraminecraft/stardrops)").GET().build(), HttpResponse.BodyHandlers.ofString());
        return r.statusCode() == 200 ? JsonParser.parseString(r.body()) : null;
    }
    /** Nudges the words of the song playing now earlier (+) or later (-), remembered for that song, and says so above the hotbar. */
    static void nudge(net.minecraft.client.Minecraft mc, int ms) {
        Spotify.Track t = Spotify.track; if (t == null) return;
        StardropsConfig c = StardropsConfig.get(); if (c.songOffset == null) c.songOffset = new LinkedHashMap<>();
        int v = Math.max(-5000, Math.min(5000, c.songOffset.getOrDefault(t.id(), 0) + ms)); if (v == 0) c.songOffset.remove(t.id()); else c.songOffset.put(t.id(), v);
        StardropsConfig.save();
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
            if (CREDIT.matcher(text).find()) continue;                                         // "作曲 : ...", "Composer: ...": credits, not words to sing
            if (text.indexOf('<') >= 0) text = WORD.matcher(text).replaceAll("").replaceAll("\\s+", " ").trim();   // "enhanced" LRC word tags: dropped, the line is shown whole
            for (long a : at) out.add(new Line(a, text));
        }
        out.sort(Comparator.comparingLong(Line::atMs));
        return out;
    }
    /** The line sung at ms (-1 before the first). */
    static int at(long ms) { List<Line> l = lines; int i = -1; for (int k = 0; k < l.size(); k++) { if (l.get(k).atMs() <= ms) i = k; else break; } return i; }
}
