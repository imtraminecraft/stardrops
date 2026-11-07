package dev.fancydrops;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Settings > Drop Data: everything you've had counted on this computer, and every drop in the library written out as a text file
 * (config/fancydrops/drop_data.txt) that opens in your text editor.
 */
final class DropDataExport {
    private DropDataExport() {}

    /** The sources of one animation group, all of them (the Mythological page hides creatures your Griffin can't call; this doesn't). */
    static List<DropData.Source> sourcesOf(Categories.Cat cat, Categories.Group g) {
        if (g.id().equals("myth")) return MythData.ALL;
        return cat.sources(g.id());
    }

    /** The whole library as text: category > group > source > drops, then what you've found. */
    static String text() {
        StringBuilder b = new StringBuilder();
        b.append("FancyDrops - drop data (written ").append(java.time.LocalDateTime.now().withNano(0)).append(")\n\n");
        LocalLedger.P p = LocalLedger.get();
        b.append("YOUR DROPS (counted on this computer)\n");
        b.append(String.format(Locale.ROOT, "  Drops: %,d   RNG: %,d   Unique items: %,d   Days: %,d%n", Math.round(p.drops), Math.round(p.rng), p.uniq.size(), p.days.size()));
        b.append(String.format(Locale.ROOT, "  Value: %s coins   Best drop: %s coins%n", PriceService.fmt(p.value), PriceService.fmt(p.best)));
        for (Map.Entry<String, double[]> e : p.g.entrySet()) b.append(String.format(Locale.ROOT, "  %-14s %,d drops (%,d RNG)%n", Categories.group(e.getKey()).name() + ":", Math.round(e.getValue()[0]), Math.round(e.getValue()[1])));
        List<String> ids = new ArrayList<>(p.uniq.keySet()); ids.sort(String::compareTo);
        b.append("  Items found: ").append(ids.isEmpty() ? "none yet" : String.join(", ", ids)).append("\n\n");
        b.append("THE DROP LIBRARY\n");
        int total = 0;
        for (Categories.Cat cat : Categories.ALL) {
            b.append("\n").append(cat.name().toUpperCase(Locale.ROOT)).append("\n");
            for (Categories.Group g : cat.groups()) {
                b.append("  ").append(g.name()).append("\n");
                for (DropData.Source s : sourcesOf(cat, g)) {
                    b.append("    ").append(s.name).append("\n");
                    for (DropData.Drop d : s.drops) {
                        String odds = (d.chance == null || d.chance.isEmpty() ? "" : d.chance) + (d.odds == null || d.odds.isEmpty() ? "" : (d.chance == null || d.chance.isEmpty() ? "" : "  ") + "(" + d.odds + ")");
                        b.append(String.format(Locale.ROOT, "      - %-34s %s%n", d.name, odds));
                        total++;
                    }
                }
            }
        }
        b.append("\n").append(total).append(" drops in all.\n");
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
