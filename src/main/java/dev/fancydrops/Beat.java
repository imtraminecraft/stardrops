package dev.fancydrops;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Locale;

/**
 * The beat of the song playing now, for the line under the lyrics. The tempo (BPM) comes from Deezer's public catalogue (no
 * account needed); where the beats fall is worked out from the lyric lines, which mostly start on a beat. With no tempo the line
 * just breathes slowly instead of pretending.
 */
final class Beat {
    private Beat() {}

    static volatile float bpm;                 // 0 = not known
    private static volatile String forTrack = "";
    private static volatile long phase; private static Object phaseFor; private static float phaseBpm;
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NORMAL).build();

    static void load(Spotify.Track t) {
        if (t == null || t.id().equals(forTrack)) return;
        forTrack = t.id(); bpm = 0; phaseFor = null;
        Thread th = new Thread(() -> {
            float got = 0;
            try {
                String artist = t.artists().contains(",") ? t.artists().substring(0, t.artists().indexOf(',')) : t.artists();
                String q = "track:\"" + t.name().replace("\"", "") + "\" artist:\"" + artist.replace("\"", "") + "\"";
                JsonElement s = get("https://api.deezer.com/search?q=" + Spotify.enc(q));
                if (s == null || !s.isJsonObject() || !s.getAsJsonObject().has("data")) s = get("https://api.deezer.com/search?q=" + Spotify.enc(t.name() + " " + artist));
                long best = -1; double bestD = 6;
                if (s != null && s.isJsonObject() && s.getAsJsonObject().has("data"))
                    for (JsonElement x : s.getAsJsonObject().getAsJsonArray("data")) {
                        JsonObject o = x.getAsJsonObject(); double d = Math.abs(o.get("duration").getAsDouble() - t.durationMs() / 1000.0);
                        if (d < bestD) { bestD = d; best = o.get("id").getAsLong(); }
                    }
                if (best >= 0) { JsonElement tr = get("https://api.deezer.com/track/" + best); if (tr != null && tr.isJsonObject() && tr.getAsJsonObject().has("bpm")) got = tr.getAsJsonObject().get("bpm").getAsFloat(); }
            } catch (Throwable e) { System.out.println("[FancyDrops] beat: " + e); }
            while (got > 0 && got < 70) got *= 2; while (got > 190) got /= 2;                       // a comfortable pulse to watch
            if (forTrack.equals(t.id())) bpm = got;
            System.out.println("[FancyDrops] beat: " + t.name() + ": " + (got > 0 ? String.format(Locale.ROOT, "%.1f BPM", got) : "tempo unknown"));
        }, "FancyDrops beat");
        th.setDaemon(true); th.start();
    }
    private static JsonElement get(String url) throws Exception {
        HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(java.time.Duration.ofSeconds(8)).header("User-Agent", "FancyDrops").GET().build(), HttpResponse.BodyHandlers.ofString());
        return r.statusCode() == 200 ? JsonParser.parseString(r.body()) : null;
    }

    /** Where the beats fall: the average place of the lyric lines' starts inside one beat (lines mostly start on a beat). */
    private static long phase(List<Lyrics.Line> L, float b) {
        if (phaseFor == L && phaseBpm == b) return phase;
        double per = 60000.0 / b, sx = 0, sy = 0;
        for (Lyrics.Line l : L) if (!l.text().isBlank()) { double an = (l.atMs() % per) / per * Math.PI * 2; sx += Math.cos(an); sy += Math.sin(an); }
        double an = Math.atan2(sy, sx); if (an < 0) an += Math.PI * 2;
        phase = Math.round(an / (Math.PI * 2) * per); phaseFor = L; phaseBpm = b;
        return phase;
    }

    /**
     * The line under the lyrics: four short bars, one for each beat of the bar; the bar for the beat you're on lights up and fades
     * till the next, the first beat of each bar a little brighter. cx, y: its middle; w: its width; a: how visible; acc: the colour.
     */
    static void line(GuiGraphicsExtractor g, float cx, float y, float w, float k, float a, int acc, long pos) {
        if (a < 0.02f || w < 8) return;
        List<Lyrics.Line> L = Lyrics.lines; float b = bpm;
        float gap = Math.max(2f, 3 * k), sw = (w - gap * 3) / 4f, x0 = cx - w / 2f;
        int lite = Ritual.mixc(acc, 0xFFFFFF, 0.35f);
        if (b <= 0) {                                                                            // no tempo known: a slow breath, no fake beat
            float br = 0.5f + 0.5f * (float) Math.sin(pos / 900.0);
            g.fill(Math.round(x0), Math.round(y), Math.round(x0 + w), Math.round(y) + 1, Gfx.argb(Math.round((40 + 50 * br) * a), lite));
            return;
        }
        double per = 60000.0 / b, beats = (pos - phase(L, b)) / per;
        int cur = (int) Math.floorMod((long) Math.floor(beats), 4L); float fr = (float) (beats - Math.floor(beats)), pulse = (float) Math.exp(-fr * 4.0);
        for (int i = 0; i < 4; i++) {
            float sx = x0 + i * (sw + gap); boolean on = i == cur;
            float al = on ? 0.45f + 0.55f * pulse : 0.16f;
            int col = on ? (i == 0 ? 0xFFFFFF : lite) : 0xB8BCC8;
            float th = on ? 1f + pulse * 0.8f * k : 1f;
            g.fill(Math.round(sx), Math.round(y - (th - 1) / 2f), Math.round(sx + sw), Math.round(y + 1 + (th - 1) / 2f), Gfx.argb(Math.round(255 * al * a), col));
            if (on) NebulaTex.glow(g, sx + sw / 2f, y + 0.5f, sw * 0.6f + 2, 3 * k + 2 * pulse, Gfx.argb(Math.round(110 * pulse * a), acc));
        }
    }
}
