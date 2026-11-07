package dev.fancydrops;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.ToDoubleFunction;

/**
 * Your drops counted on this computer, the moment they happen, so the star map and the ledger always show them, even while the
 * server can't confirm who you are. The rules mirror tools/worn-worker.js exactly; the server stays the only judge of cosmetics,
 * so a hand-edited file lights stars but never unlocks a reward. Saved in config/fancydrops/ledger_local.json.
 */
final class LocalLedger {
    private LocalLedger() {}

    /** The counts, the same shape as the server's profile. g: per group {drops, RNG drops, coins}. fd / fdr: drops / RNG drops on your first day. */
    static final class P {
        double drops, rng, bigRng, value, best, r3, fd, fdr; long first = System.currentTimeMillis();
        Map<String, Integer> uniq = new HashMap<>(), days = new HashMap<>(), vinyl = new HashMap<>(), bosses = new HashMap<>(), dyes = new HashMap<>();
        Map<String, double[]> g = new HashMap<>();
        Map<String, Long> unlocked = new HashMap<>();
    }
    /** One star of the table (src/main/resources/fancydrops/achievements.json, written by tools/make_achievements.py: the server has the same). */
    record Def(String id, String ch, int tier, String name, String desc, String icon, double goal, String stat, String reward, boolean secret, boolean first) {}

    /** A drop worth this much or more is an RNG drop. */
    static final double RNG_COINS = 10_000_000;
    static final LinkedHashMap<String, String> CH = new LinkedHashMap<>();
    static final List<Def> ACH = new ArrayList<>();
    static final String[] TIER = {"Common", "Uncommon", "Rare", "Legendary"};
    static final int[] TIER_COL = {0xFFFFFFFF, 0xFF55FF55, 0xFF5555FF, 0xFFFFAA00};
    static {
        try (var in = LocalLedger.class.getResourceAsStream("/fancydrops/achievements.json")) {
            var o = JsonParser.parseString(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            for (var e : o.getAsJsonObject("chapters").entrySet()) CH.put(e.getKey(), e.getValue().getAsString());
            for (var e : o.getAsJsonArray("list")) { JsonObject a = e.getAsJsonObject();
                ACH.add(new Def(a.get("id").getAsString(), a.get("ch").getAsString(), a.get("tier").getAsInt(), a.get("name").getAsString(), a.get("desc").getAsString(), a.get("icon").getAsString(),
                    a.get("goal").getAsDouble(), a.get("stat").getAsString(), a.get("reward").getAsString(), a.get("secret").getAsBoolean(), a.get("first").getAsBoolean())); }
        } catch (Throwable t) { System.err.println("[FancyDrops] achievements table: " + t); }
    }
    static Def def(String id) { for (Def d : ACH) if (d.id.equals(id)) return d; return null; }
    /** Minutes played with FancyDrops on (Playtime). */
    static double minutes() { return FancyConfig.get().playMinutes; }
    /** Still your first day of playing with FancyDrops (the first 24 hours of playtime)? */
    static boolean firstDay() { return minutes() < 1440; }

    private static double[] grp(P p, String k) { double[] v = p.g.get(k); if (v != null && v.length < 3) { v = Arrays.copyOf(v, 3); p.g.put(k, v); } return v; }
    /** A star's number (the same words as the server's stat()). */
    static double stat(P p, String s) {
        if (s.startsWith("g:") || s.startsWith("gr:") || s.startsWith("gv:")) {
            int i = s.startsWith("g:") ? 0 : s.startsWith("gr:") ? 1 : 2; double t = 0;
            for (String k : s.substring(s.indexOf(':') + 1).split("\\+")) { double[] v = grp(p, k); if (v != null) t += v[i]; }
            return t;
        }
        return switch (s) {
            case "drops" -> p.drops; case "rng" -> p.rng; case "r3" -> p.r3; case "value" -> p.value; case "best" -> p.best;
            case "best100" -> p.best >= 100_000_000 ? 1 : 0;
            case "uniq" -> p.uniq.size(); case "days" -> p.days.size(); case "bosses" -> p.bosses.size(); case "vinyl" -> p.vinyl.size(); case "dyes" -> p.dyes.size();
            case "fd" -> p.fd; case "fdr" -> p.fdr; case "pt" -> minutes();
            default -> 0;
        };
    }

    private static P p; private static long version;
    private static Path file() { return DataFiles.dir().resolve("ledger_local.json"); }
    static synchronized P get() {
        if (p != null) return p;
        p = new P();
        try { if (Files.exists(file())) { P q = new com.google.gson.Gson().fromJson(Files.readString(file()), P.class); if (q != null) p = q; } } catch (Throwable ignored) { }
        if (p.g == null) p.g = new HashMap<>(); if (p.unlocked == null) p.unlocked = new HashMap<>();
        for (var e : FancyConfig.get().playDone.entrySet()) p.unlocked.putIfAbsent(e.getKey(), e.getValue());          // Playtime stars lit before they joined the table
        for (Def d : ACH) { Long at = p.unlocked.get(d.id); if (at != null && stat(p, d.stat) < d.goal) p.unlocked.remove(d.id); }   // the table changed: a star whose goal moved past what you have goes dark again
        check(p, System.currentTimeMillis());                                                     // and a new star you already have the numbers for lights at once
        return p;
    }
    private static synchronized void save() { try { Files.writeString(file(), new com.google.gson.Gson().toJson(p)); } catch (Throwable ignored) { } }
    static long version() { return version; }

    /** Counts one drop (the same event the server gets). Returns the ids of the achievements it just earned. */
    static synchronized List<String> add(JsonObject e) {
        P p = get(); long now = System.currentTimeMillis();
        String grp = e.has("g") ? e.get("g").getAsString() : "", id = e.has("id") ? e.get("id").getAsString() : "", item = e.has("item") ? e.get("item").getAsString() : "", src = e.has("src") ? e.get("src").getAsString() : "";
        if (grp.isEmpty() || id.isEmpty()) return List.of();
        double v = e.has("v") ? Math.max(0, e.get("v").getAsDouble()) : 0; int r = e.has("r") ? e.get("r").getAsInt() : 0; boolean rng = v >= RNG_COINS;
        boolean fresh1 = e.has("pt") ? e.get("pt").getAsDouble() < 1440 : firstDay();
        p.drops++; if (rng) p.rng++; if (r >= 3) p.r3++; if (fresh1) { p.fd++; if (rng) p.fdr++; }
        p.value += v; p.best = Math.max(p.best, v);
        p.uniq.put(id, 1); p.days.put(java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString(), 1);
        if (item.toLowerCase(Locale.ROOT).contains("vinyl")) p.vinyl.put(id, 1);
        if (grp.equals("dyes")) p.dyes.put(id, 1);
        if (grp.equals("slayer") && !src.isEmpty()) p.bosses.put(src, 1);
        double[] gg = grp(p, grp); if (gg == null) { gg = new double[3]; p.g.put(grp, gg); }
        gg[0]++; if (rng) gg[1]++; gg[2] += v;
        List<String> got = check(p, now);
        version++; save();
        return got;
    }
    /** Lights every star whose number has reached its goal. */
    private static List<String> check(P p, long now) {
        List<String> got = new ArrayList<>();
        for (Def d : ACH) if (!p.unlocked.containsKey(d.id) && stat(p, d.stat) >= d.goal) { p.unlocked.put(d.id, now); got.add(d.id); }
        return got;
    }
    /** Playtime moved on: the Playtime stars it just reached. */
    static synchronized List<String> timeTick() { P p = get(); List<String> got = new ArrayList<>(); for (Def d : ACH) if (d.stat.equals("pt") && !p.unlocked.containsKey(d.id) && minutes() >= d.goal) { p.unlocked.put(d.id, System.currentTimeMillis()); got.add(d.id); } if (!got.isEmpty()) { version++; save(); } return got; }
    /** The Playtime goals, in minutes, in order. */
    static List<Def> timeGoals() { List<Def> l = new ArrayList<>(); for (Def d : ACH) if (d.stat.equals("pt")) l.add(d); l.sort(Comparator.comparingDouble(Def::goal)); return l; }

    /** Everything counted here, as a profile (no cosmetics: only the server unlocks those). */
    static synchronized Achievements.Profile profile() {
        P p = get();
        List<Achievements.Ach> list = new ArrayList<>();
        for (Def d : ACH) {
            Long at = p.unlocked.get(d.id); boolean done = at != null;
            String desc = d.first && !done && !firstDay() ? d.desc + " (Missed: your first day has passed)" : d.first ? d.desc + " (First day only)" : d.desc;
            list.add(new Achievements.Ach(d.id, d.ch, CH.get(d.ch), d.name, desc, d.icon, d.goal, Math.min(d.goal, stat(p, d.stat)), done, at == null ? 0 : at, d.reward, d.secret, d.tier, d.first));
        }
        Map<String, Double> st = new HashMap<>();
        st.put("drops", p.drops); st.put("rng", p.rng); st.put("bigRng", p.bigRng); st.put("value", p.value); st.put("best", p.best); st.put("r3", p.r3); st.put("fd", p.fd); st.put("fdr", p.fdr);
        st.put("unique", (double) p.uniq.size()); st.put("days", (double) p.days.size()); st.put("bosses", (double) p.bosses.size()); st.put("vinyls", (double) p.vinyl.size()); st.put("dyes", (double) p.dyes.size());
        Map<String, double[]> groups = new HashMap<>(); for (var e : p.g.entrySet()) groups.put(e.getKey(), Arrays.copyOf(e.getValue(), 3));
        return new Achievements.Profile(st, groups, list, new HashSet<>(), new LinkedHashMap<>(CH), p.first);
    }

    /** The server's word and this computer's count together: each number is the larger of the two, a star is lit if either says so. */
    static Achievements.Profile merge(Achievements.Profile server, Achievements.Profile local) {
        if (server == null) return local;
        Map<String, Double> st = new HashMap<>(server.stats()); local.stats().forEach((k, v) -> st.merge(k, v, Math::max));
        Map<String, double[]> gr = new HashMap<>(); server.groups().forEach((k, v) -> gr.put(k, Arrays.copyOf(v, 3)));
        local.groups().forEach((k, v) -> gr.merge(k, v, (a, b) -> new double[]{Math.max(a[0], b[0]), Math.max(a[1], b[1]), Math.max(a[2], b[2])}));
        Map<String, Achievements.Ach> srv = new HashMap<>(); for (Achievements.Ach a : server.list()) srv.put(a.id(), a);
        List<Achievements.Ach> list = new ArrayList<>();
        for (Achievements.Ach l : local.list()) {                                                 // the table here decides the stars; the server only adds what it has seen
            Achievements.Ach a = srv.get(l.id());
            if (a == null) { list.add(l); continue; }
            boolean done = a.done() || l.done(); long at = a.done() ? a.at() : l.at();
            list.add(new Achievements.Ach(l.id(), l.ch(), l.chapter(), l.name(), l.desc(), l.icon(), l.goal(), Math.max(Math.min(l.goal(), a.progress()), l.progress()), done, at, l.reward(), l.secret(), l.tier(), l.first()));
        }
        return new Achievements.Profile(st, gr, list, server.cosmetics(), new LinkedHashMap<>(local.chapters()), server.since() == 0 ? local.since() : Math.min(server.since(), local.since()));
    }
}
