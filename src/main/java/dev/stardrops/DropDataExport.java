package dev.stardrops;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Settings > Drop Data: everything you've had counted on this computer, and every drop in the library written out as a text file
 * (config/stardrops/drop_data.txt) that opens in your text editor.
 */
final class DropDataExport {
    private DropDataExport() {}

    /** The sources of one animation group, all of them (the Mythological page hides creatures your Griffin can't call; this doesn't). */
    static List<DropData.Source> sourcesOf(Categories.Cat cat, Categories.Group g) {
        if (g.id().equals("myth")) return MythData.ALL;
        return cat.sources(g.id());
    }

    private static String line(char c) { return String.valueOf(c).repeat(64) + "\n"; }
    private static String pretty(String id) {                                                // ENCHANTMENT_FIRE_ASPECT_3 -> Fire Aspect 3
        String t = id.replaceFirst("^ENCHANTMENT_(ULTIMATE_)?", "").replace(';', ' ').replace('_', ' ').toLowerCase(Locale.ROOT);
        StringBuilder o = new StringBuilder(); for (String w : t.split(" ")) if (!w.isEmpty()) o.append(o.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return o.toString();
    }

    /** The whole file: what you've found, sorted into category > page > source, then the full library with your finds ticked. */
    static String text() {
        StringBuilder b = new StringBuilder();
        LocalLedger.P p = LocalLedger.get();
        java.util.Set<String> found = new java.util.HashSet<>(); for (String id : p.uniq.keySet()) found.add(id.toUpperCase(Locale.ROOT));
        b.append(line('=')).append("  STARDROPS - DROP DATA\n  Written ").append(java.time.LocalDateTime.now().withNano(0).toString().replace('T', ' ')).append("\n").append(line('='));
        b.append("\nYOUR DROPS (counted on this computer)\n").append(line('-'));
        b.append(String.format(Locale.ROOT, "  %-16s %,d%n  %-16s %,d%n  %-16s %,d%n  %-16s %s coins%n  %-16s %s coins%n  %-16s %,d%n", "Drops", Math.round(p.drops), "RNG drops", Math.round(p.rng), "Different items", p.uniq.size(), "Total value", PriceService.fmt(p.value), "Best drop", PriceService.fmt(p.best), "Days with drops", p.days.size()));

        // where each found item lives in the library
        java.util.Map<String, String[]> where = new java.util.HashMap<>();
        for (Categories.Cat cat : Categories.ALL) for (Categories.Group g : cat.groups()) for (DropData.Source so : sourcesOf(cat, g)) for (DropData.Drop d : so.drops)
            if (d.id != null) where.putIfAbsent(d.id.toUpperCase(Locale.ROOT), new String[]{cat.name(), g.name(), so.boss == null || so.boss.isEmpty() ? so.name : so.boss, d.name});

        b.append("\nBY CATEGORY\n").append(line('-'));
        java.util.Set<String> placed = new java.util.HashSet<>();
        for (Categories.Cat cat : Categories.ALL) {
            long cd = 0, cr = 0; for (Categories.Group g : cat.groups()) { double[] v = p.g.get(g.id()); if (v != null) { cd += Math.round(v[0]); cr += Math.round(v.length > 1 ? v[1] : 0); } }
            List<String[]> items = new ArrayList<>(); for (String id : found) { String[] w = where.get(id); if (w != null && w[0].equals(cat.name())) items.add(w); }
            if (cd == 0 && items.isEmpty()) continue;
            b.append(String.format(Locale.ROOT, "%n[ %s ]  %,d drops, %,d RNG%n", cat.name().toUpperCase(Locale.ROOT), cd, cr));
            for (Categories.Group g : cat.groups()) {
                double[] v = p.g.get(g.id()); List<String[]> gi = new ArrayList<>(); for (String[] w : items) if (w[1].equals(g.name())) gi.add(w);
                if ((v == null || v[0] == 0) && gi.isEmpty()) continue;
                b.append(String.format(Locale.ROOT, "  %s  -  %,d drops, %,d RNG%s%n", g.name(), v == null ? 0 : Math.round(v[0]), v == null || v.length < 2 ? 0 : Math.round(v[1]), v != null && v.length > 2 && v[2] > 0 ? ", " + PriceService.fmt(v[2]) + " coins" : ""));
                gi.sort(java.util.Comparator.comparing((String[] w) -> w[2]).thenComparing(w -> w[3]));
                String lastSrc = "";
                for (String[] w : gi) { if (!w[2].equals(lastSrc)) { b.append("      ").append(w[2]).append(":\n"); lastSrc = w[2]; } b.append("        * ").append(w[3]).append("\n"); placed.add(w[3]); }
            }
        }
        List<String> other = new ArrayList<>(); for (String id : found) if (!where.containsKey(id)) other.add(pretty(id)); other.sort(String::compareTo);
        if (!other.isEmpty()) { b.append("\n[ OTHER FINDS ]  not in the library (enchanted books, runes and the like)\n"); for (String o : other) b.append("        * ").append(o).append("\n"); }

        b.append("\n\n").append(line('=')).append("  THE DROP LIBRARY   ([x] = you've found it)\n").append(line('='));
        int total = 0, got = 0;
        for (Categories.Cat cat : Categories.ALL) {
            b.append("\n[ ").append(cat.name().toUpperCase(Locale.ROOT)).append(" ]\n");
            for (Categories.Group g : cat.groups()) {
                b.append("  ").append(g.name()).append("\n");
                for (DropData.Source so : sourcesOf(cat, g)) {
                    b.append("    ").append(so.boss == null || so.boss.isEmpty() ? so.name : so.boss).append("\n");
                    for (DropData.Drop d : so.drops) {
                        boolean have = d.id != null && found.contains(d.id.toUpperCase(Locale.ROOT)); if (have) got++;
                        String odds = (d.chance == null || d.chance.isEmpty() ? "" : d.chance) + (d.odds == null || d.odds.isEmpty() ? "" : (d.chance == null || d.chance.isEmpty() ? "" : "  ") + "(" + d.odds + ")");
                        b.append(String.format(Locale.ROOT, "      [%s] %-34s %s%n", have ? "x" : " ", d.name, odds));
                        total++;
                    }
                }
            }
        }
        b.append("\n").append(line('-')).append(String.format(Locale.ROOT, "  %d drops in the library, %d of them found.%n", total, got));
        return b.toString();
    }

    /** Writes the file and opens it. Returns a line for the toast. */
    static String openFile() {
        try {
            Path f = DataFiles.dir().resolve("drop_data.txt");
            Files.writeString(f, text().replace("\n", System.lineSeparator()));
            net.minecraft.util.Util.getPlatform().openPath(f);
            return "Opened drop_data.txt";
        } catch (Throwable e) { return "Couldn't write the file: " + e.getMessage(); }
    }
}
