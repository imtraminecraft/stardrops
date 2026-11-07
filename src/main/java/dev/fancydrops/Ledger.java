package dev.fancydrops;

import java.util.*;

/** The ledger's numbers and names, shared by the achievements sky (Codex): the stats of each chapter, reward names and badge icons. */
final class Ledger {
    private Ledger() {}

    static String fmt(double v) { return CosmeticData.fmtN(v); }

    /** Minutes played as hours, minutes and seconds. */
    static String hms(double m) { long t = (long) (m * 60); return String.format(java.util.Locale.ROOT, "%d:%02d:%02d", t / 3600, t / 60 % 60, t % 60); }
    static String hm(double m) { return m < 60 ? Math.round(m) + " min" : String.format(java.util.Locale.ROOT, "%.1f h", m / 60); }
    private static String nextGoal(double m) { for (LocalLedger.Def g : LocalLedger.timeGoals()) if (m < g.goal()) return hms(g.goal() - m); return "All lit"; }
    static List<String[]> statRows(String ch, Achievements.Profile p) {
        Map<String, Double> s = p.stats(); List<String[]> o = new ArrayList<>();
        java.util.function.Function<String, Double> st = k -> s.getOrDefault(k, 0.0);
        java.util.function.BiFunction<String, Integer, Double> gr = (k, i) -> p.groups().containsKey(k) ? p.groups().get(k)[i] : 0.0;
        switch (ch) {
            case "fortune" -> { o.add(new String[]{"RNG drops", fmt(st.apply("rng"))}); o.add(new String[]{"RNG worth 1M+", fmt(st.apply("bigRng"))}); o.add(new String[]{"Pray RNGesus+", fmt(st.apply("r3"))}); o.add(new String[]{"Coins found", fmt(st.apply("value"))}); o.add(new String[]{"Best drop", fmt(st.apply("best"))}); }
            case "myth" -> { o.add(new String[]{"Treasure finds", fmt(gr.apply("treasure", 0))}); o.add(new String[]{"Creature drops", fmt(gr.apply("myth", 0))}); o.add(new String[]{"Creature RNG", fmt(gr.apply("myth", 1))}); }
            case "rare", "mob" -> { o.add(new String[]{"Slayer drops", fmt(gr.apply("slayer", 0))}); o.add(new String[]{"Slayer RNG", fmt(gr.apply("slayer", 1))}); o.add(new String[]{"Bosses", fmt(st.apply("bosses"))});
                o.add(new String[]{"Crimson Isle drops", fmt(gr.apply("crimson", 0))}); o.add(new String[]{"Dragon drops", fmt(gr.apply("dragon", 0))}); o.add(new String[]{"Mines drops", fmt(gr.apply("mobs", 0))}); }
            case "mine" -> { o.add(new String[]{"Crystal Nucleus", fmt(gr.apply("nucleus", 0))}); o.add(new String[]{"Frozen Corpses", fmt(gr.apply("corpse", 0))}); o.add(new String[]{"Mining Events", fmt(gr.apply("fiesta", 0))});
                double v = 0; for (String k : new String[]{"nucleus", "corpse", "fiesta"}) if (p.groups().containsKey(k) && p.groups().get(k).length > 2) v += p.groups().get(k)[2]; o.add(new String[]{"Coins found", fmt(v)}); }
            case "farm" -> { o.add(new String[]{"Rare Crops", fmt(gr.apply("crops", 0))}); o.add(new String[]{"Harvest Feast", fmt(gr.apply("feast", 0))}); o.add(new String[]{"Harvest Bounty", fmt(gr.apply("bounty", 0))}); o.add(new String[]{"Pest drops", fmt(gr.apply("pests", 0))}); o.add(new String[]{"Vinyls", fmt(st.apply("vinyls"))}); }
            case "fish" -> { double fd = 0, fr = 0; for (String k : new String[]{"fish_water", "fish_lava", "fish_spooky", "fish_winter", "fish_treasure"}) { fd += gr.apply(k, 0); fr += gr.apply(k, 1); }
                o.add(new String[]{"Sea creature drops", fmt(fd)}); o.add(new String[]{"Sea creature RNG", fmt(fr)}); o.add(new String[]{"Water", fmt(gr.apply("fish_water", 0))}); o.add(new String[]{"Lava", fmt(gr.apply("fish_lava", 0))}); o.add(new String[]{"Festivals", fmt(gr.apply("fish_spooky", 0) + gr.apply("fish_winter", 0))}); }
            case "time" -> { double m = FancyConfig.get().playMinutes; /* live, to the second */ o.add(new String[]{"Played", hms(m)}); o.add(new String[]{"First day", m < 1440 ? hms(1440 - m) + " left" : "Over"}); o.add(new String[]{"Next star in", nextGoal(m)}); }
            case "dye" -> { o.add(new String[]{"Dyes found", fmt(gr.apply("dyes", 0))}); o.add(new String[]{"Different dyes", fmt(st.apply("dyes"))}); o.add(new String[]{"Table rewards", fmt(gr.apply("exp", 0))}); o.add(new String[]{"Table RNG", fmt(gr.apply("exp", 1))}); }
            default -> { o.add(new String[]{"Rare drops", fmt(st.apply("drops"))}); o.add(new String[]{"Different items", fmt(st.apply("unique"))}); o.add(new String[]{"RNG drops", fmt(st.apply("rng"))}); o.add(new String[]{"Coins found", fmt(st.apply("value"))}); o.add(new String[]{"Pray RNGesus+", fmt(st.apply("r3"))}); o.add(new String[]{"Best drop", fmt(st.apply("best"))}); o.add(new String[]{"Days with drops", fmt(st.apply("days"))});
                if (p.since() > 0) o.add(new String[]{"Counting since", new java.text.SimpleDateFormat("d MMM yyyy").format(new Date(p.since()))}); }
        }
        return o;
    }
    static String rewardName(String id) { CosmeticData.Def d = CosmeticData.byId(id); return d == null ? id : d.name() + " " + CosmeticData.kindLabel(d.kind()).toLowerCase(Locale.ROOT); }
}
