package dev.stardrops;

import dev.stardrops.jaad.aac.Decoder;
import dev.stardrops.jaad.aac.SampleBuffer;
import dev.stardrops.jaad.mp4.MP4Container;
import dev.stardrops.jaad.mp4.api.AudioTrack;
import dev.stardrops.jaad.mp4.api.Frame;
import dev.stardrops.jaad.mp4.api.MetaData;
import dev.stardrops.jaad.mp4.api.Movie;
import dev.stardrops.jaad.mp4.api.Track;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

/**
 * Visuals > Music > Your files: your own music, played by Stardrops itself. Drop .mp4 or .m4a files (a music video is fine: only its sound is
 * used) or .wav files into config/stardrops/music and play them from the menu or with the music keys.
 *
 * While one of your files plays it is "the song": it is put into Spotify.track / Spotify.playing (and Spotify.position() asks us), so Now
 * Playing, the lyrics (looked up by the file's title and artist), the ambience and the song intro all work with it unchanged. Spotify is not
 * asked anything meanwhile; pressing Stop hands the HUD back to Spotify.
 *
 * MP4 sound is AAC, which Java cannot read by itself: it is decoded by JAAD (public domain, bundled in dev.stardrops.jaad) on a worker thread
 * and written to a Java sound line. The title, artist and cover come from the file's own tags; without tags the file name is the title
 * ("Artist - Title.mp4" is split at the dash).
 */
final class LocalMusic {
    private LocalMusic() {}

    record Song(Path file, String title, String artist, long durationMs) {}
    private static volatile List<Song> songs = List.of(); private static long scannedAt;
    static volatile boolean active;                              // one of your files is the song right now
    static volatile String status = "";
    private static volatile int index = -1; private static volatile Thread worker; private static volatile boolean paused, stop; private static volatile long seekTo = -1;
    private static volatile long posMs; private static volatile int serial;

    static Path dir() { return DataFiles.dir().resolve("music"); }
    static List<Song> songs() { if (System.currentTimeMillis() - scannedAt > 5000) scan(); return songs; }
    static int count() { return songs().size(); }
    static int playingIndex() { return active ? index : -1; }
    static boolean paused() { return paused; }

    /** Reads the music folder (made if missing). Titles and lengths come from the files' own tags, read once per file. */
    static synchronized void scan() {
        scannedAt = System.currentTimeMillis();
        try {
            Path d = dir(); Files.createDirectories(d); List<Song> out = new ArrayList<>(); List<Song> old = songs;
            try (var st = Files.list(d)) {
                for (Path p : st.sorted().toList()) {
                    String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                    if (!(n.endsWith(".mp4") || n.endsWith(".m4a") || n.endsWith(".m4b") || n.endsWith(".mov") || n.endsWith(".wav"))) continue;
                    Song known = null; for (Song s : old) if (s.file().equals(p)) known = s;
                    out.add(known != null ? known : describe(p));
                }
            }
            songs = out;
        } catch (Throwable e) { status = "Couldn't read the music folder: " + e.getMessage(); }
    }
    private static Song describe(Path p) {
        String name = p.getFileName().toString(); int dot = name.lastIndexOf('.'); String base = dot > 0 ? name.substring(0, dot) : name, title = base, artist = ""; long dur = 0;
        int dash = base.indexOf(" - "); if (dash > 0) { artist = base.substring(0, dash).trim(); title = base.substring(dash + 3).trim(); }
        if (!name.toLowerCase(Locale.ROOT).endsWith(".wav")) try (RandomAccessFile f = new RandomAccessFile(p.toFile(), "r")) {
            Movie m = new MP4Container(f).getMovie(); dur = Math.round(m.getDuration() * 1000);
            if (m.containsMetaData()) { MetaData md = m.getMetaData(); String t = md.get(MetaData.Field.TITLE), a = md.get(MetaData.Field.ARTIST); if (t != null && !t.isBlank()) title = t.trim(); if (a != null && !a.isBlank()) artist = a.trim(); }
        } catch (Throwable ignored) { }
        else try (var in = AudioSystem.getAudioInputStream(p.toFile())) { dur = Math.round(in.getFrameLength() * 1000.0 / in.getFormat().getFrameRate()); } catch (Throwable ignored) { }
        return new Song(p, title, artist, dur);
    }

    // ------------------------------------------------------------------ controls
    static void play(int i) {
        List<Song> l = songs(); if (l.isEmpty()) { status = "No music files yet: put .mp4 or .m4a files in the music folder."; return; }
        i = Math.floorMod(i, l.size()); halt(); index = i; Song s = l.get(i);
        stop = false; paused = false; posMs = 0; seekTo = -1; active = true; final int my = ++serial;
        Spotify.Track tr = new Spotify.Track("local:" + s.file().getFileName(), s.title(), s.artist().isEmpty() ? "Your music" : s.artist(), "", Math.max(1, s.durationMs()), "");
        Spotify.track = tr; Spotify.playing = true; AlbumArt.loadBytes(tr.id(), null);
        if (!s.artist().isEmpty()) try { Lyrics.load(tr); } catch (Throwable ignored) { }
        status = "Playing " + s.title();
        Thread t = new Thread(() -> run(s, my), "Stardrops music"); t.setDaemon(true); worker = t; t.start();
    }
    /** "toggle", "next" or "previous" (the music keys and the menu). */
    static void control(String what) {
        if (!active) { play(Math.max(0, index)); return; }
        switch (what) {
            case "next" -> play(next(1));
            case "previous" -> { if (posMs > 4000) seekTo = 0; else play(next(-1)); }
            default -> { paused = !paused; Spotify.playing = !paused; }
        }
    }
    private static int next(int step) {
        int n = songs().size(); if (n == 0) return 0; int mode = StardropsConfig.get().localMode;
        if (mode == 1 && n > 1) { int r; do r = (int) (Math.random() * n); while (r == index); return r; }
        return Math.floorMod(index + step, n);
    }
    /** Stops your files and hands Now Playing back to Spotify. */
    static void stopAll() { halt(); active = false; Spotify.playing = false; if (Spotify.track != null && Spotify.track.id().startsWith("local:")) Spotify.track = null; status = "Stopped."; }
    private static void halt() { stop = true; serial++; Thread t = worker; worker = null; if (t != null) { t.interrupt(); try { t.join(400); } catch (InterruptedException ignored) { } } }
    static long position() { return posMs; }
    /** 0..1: your volume slider times the game's master volume. */
    private static float volume() {
        float v = StardropsConfig.get().localVolume / 100f;
        try { v *= net.minecraft.client.Minecraft.getInstance().options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.MASTER); } catch (Throwable ignored) { }
        return Math.max(0f, Math.min(1f, v));
    }

    // ------------------------------------------------------------------ the worker: decode and play
    private static void run(Song s, int my) {
        SourceDataLine line = null; boolean ended = false;
        try {
            boolean wav = s.file().toString().toLowerCase(Locale.ROOT).endsWith(".wav");
            if (wav) {
                try (var in = AudioSystem.getAudioInputStream(s.file().toFile())) {
                    AudioFormat f = in.getFormat(); line = AudioSystem.getSourceDataLine(f); line.open(f, 16384); line.start(); byte[] buf = new byte[4096]; long frames = 0; int fs = f.getFrameSize(); float last = -1;
                    for (int n; my == serial && !stop && (n = in.read(buf)) > 0; ) {
                        while (paused && my == serial && !stop) { if (line.isRunning()) line.stop(); Thread.sleep(30); } if (!line.isRunning()) line.start();
                        float v = volume(); if (v != last) { gain(line, v); last = v; }
                        line.write(buf, 0, n); frames += n / fs; posMs = Math.max(0, Math.round(frames * 1000.0 / f.getFrameRate()) - Math.round((line.getBufferSize() - line.available()) / (double) fs * 1000.0 / f.getFrameRate()));
                    }
                    ended = my == serial && !stop;
                }
            } else try (RandomAccessFile file = new RandomAccessFile(s.file().toFile(), "r")) {
                Movie movie = new MP4Container(file).getMovie();
                List<Track> tracks = movie.getTracks(AudioTrack.AudioCodec.AAC);
                if (tracks.isEmpty()) { status = s.title() + ": no AAC sound in this file."; ended = true; return; }
                AudioTrack track = (AudioTrack) tracks.get(0);
                try { if (movie.containsMetaData()) { var arts = movie.getMetaData().get(MetaData.Field.COVER_ARTWORKS); if (arts != null && !arts.isEmpty()) { Spotify.Track tr = Spotify.track; if (tr != null && my == serial) { AlbumArt.loadBytes(tr.id() + "#art", arts.get(0).getData()); } } } } catch (Throwable ignored) { }
                Decoder dec = new Decoder(track.getDecoderSpecificInfo()); SampleBuffer sb = new SampleBuffer(); float last = -1; double base = 0; long written = 0; AudioFormat fmt = null; int bad = 0;
                while (my == serial && !stop && track.hasMoreFrames()) {
                    long sk = seekTo; if (sk >= 0) { seekTo = -1; base = track.seek(sk / 1000.0); written = 0; if (line != null) line.flush(); }
                    while (paused && my == serial && !stop) { if (line != null && line.isRunning()) line.stop(); Thread.sleep(30); }
                    Frame fr = track.readNextFrame(); if (fr == null) break;
                    try { dec.decodeFrame(fr.getData(), sb); bad = 0; } catch (Throwable e) { if (++bad > 40) throw e; continue; }       // a broken frame is skipped, a broken file gives up
                    byte[] pcm = sb.getData(); if (pcm == null || pcm.length == 0) continue;
                    if (line == null) { fmt = new AudioFormat(sb.getSampleRate(), sb.getBitsPerSample(), sb.getChannels(), true, sb.isBigEndian()); line = AudioSystem.getSourceDataLine(fmt); line.open(fmt, 16384); }
                    if (!line.isRunning()) line.start();
                    float v = volume(); if (v != last) { gain(line, v); last = v; }
                    line.write(pcm, 0, pcm.length); written += pcm.length / fmt.getFrameSize();
                    long queued = (line.getBufferSize() - line.available()) / fmt.getFrameSize();
                    posMs = Math.max(0, Math.round(base * 1000 + (written - queued) * 1000.0 / fmt.getFrameRate()));
                }
                ended = my == serial && !stop;
            }
            if (ended && line != null) line.drain();
        } catch (InterruptedException ignored) {
        } catch (Throwable e) { status = s.title() + ": couldn't play it (" + e + ")"; System.err.println("[Stardrops] music: " + s.file() + ": " + e); ended = my == serial && !stop; }
        finally { if (line != null) try { line.stop(); line.close(); } catch (Throwable ignored) { } }
        if (ended && my == serial) {                                                                 // the song ran out: the next one (or the same again)
            int mode = StardropsConfig.get().localMode; final int nx = mode == 2 ? index : next(1);
            net.minecraft.client.Minecraft.getInstance().execute(() -> { if (my == serial) play(nx); });
        }
    }
    private static void gain(SourceDataLine line, float v) {
        try { FloatControl g = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN); float db = v <= 0.001f ? g.getMinimum() : (float) (20 * Math.log10(v)); g.setValue(Math.max(g.getMinimum(), Math.min(g.getMaximum(), db))); } catch (Throwable ignored) { }
    }
}
