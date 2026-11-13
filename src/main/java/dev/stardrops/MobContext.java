package dev.stardrops;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Which mob a drop really came from. Many items drop from more than one mob (a Foraging Exp Boost from the Honeymite, the Drybark,
 * the Soul of the Alpha and the Loch Emperor; a Summoning Eye from Zealots and Voidling Extremists), and the "RARE DROP!" line never
 * says which. So when a drop matches several mobs, Stardrops looks around: a mob of that name close by (Hypixel names every mob over
 * its head) wins; else the mob of the area you are in (the sidebar's "⏣ Moonglade Marsh", the tab list's "Area: Galatea"); else the
 * first one listed.
 */
public final class MobContext {
    private MobContext() {}

    private static long namesAt; private static final List<String[]> NAMES = new ArrayList<>();      // {name, distance}: mobs named near you, a moment old at most
    private static long areaAt; private static String area = "";

    /** The best of several sources a drop could come from. */
    static DropData.Source pick(List<DropData.Source> cands) {
        if (cands.size() == 1) return cands.get(0);
        try {
            DropData.Source best = null; double bestD = 1e9;
            for (DropData.Source s : cands) { double d = nearest(s.name); if (d < bestD) { bestD = d; best = s; } }
            if (best != null) return best;
            String a = area();
            if (!a.isEmpty()) for (DropData.Source s : cands) { String sa = s.area == null ? "" : s.area.toLowerCase(Locale.ROOT); if (!sa.isEmpty() && a.contains(sa)) return s; }
            if (!a.isEmpty()) for (DropData.Source s : cands) if (areaMatches(s.group, a)) return s;
        } catch (Throwable ignored) { }
        return cands.get(0);
    }

    /** How far the closest mob carrying this name is (blocks), or a huge number when none is near. */
    private static double nearest(String mobName) {
        refreshNames();
        String want = clean(mobName); if (want.startsWith("the ")) want = want.substring(4);
        double best = 1e9;
        for (String[] n : NAMES) if (n[0].contains(want)) best = Math.min(best, Double.parseDouble(n[1]));
        return best;
    }
    private static void refreshNames() {
        long now = System.currentTimeMillis(); if (now - namesAt < 400) return; namesAt = now; NAMES.clear();
        Minecraft mc = Minecraft.getInstance(); if (mc.level == null || mc.player == null) return;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e == mc.player) continue; double d = e.distanceTo(mc.player); if (d > 48) continue;
            Component cn = e.getCustomName(); if (cn == null) continue;
            String s = clean(cn.getString()); if (!s.isEmpty()) NAMES.add(new String[]{s, String.valueOf(d)});
        }
    }
    /** Where you are, lower case: the sidebar's location line and the tab list's area, joined. */
    static String area() {
        long now = System.currentTimeMillis(); if (now - areaAt < 2000) return area; areaAt = now;
        Minecraft mc = Minecraft.getInstance(); StringBuilder b = new StringBuilder();
        try {
            if (mc.level != null) { Scoreboard sb = mc.level.getScoreboard(); Objective o = sb.getDisplayObjective(DisplaySlot.SIDEBAR);
                if (o != null) for (PlayerScoreEntry en : sb.listPlayerScores(o)) { PlayerTeam t = sb.getPlayersTeam(en.owner());
                    String line = clean(PlayerTeam.formatNameForTeam(t, Component.literal(en.owner())).getString()); if (line.contains("⏣")) b.append(line).append(' '); } }
            if (mc.getConnection() != null) for (var info : mc.getConnection().getOnlinePlayers()) { Component c = info.getTabListDisplayName(); if (c == null) continue;
                String s = clean(c.getString()); if (s.startsWith("area:") || s.startsWith("dungeon:")) b.append(s).append(' '); }
        } catch (Throwable ignored) { }
        area = b.toString(); return area;
    }
    /** A Mob Drops group's place, as the game names it. */
    private static boolean areaMatches(String group, String a) {
        String[] keys = switch (group == null ? "" : group) {
            case "mobs_hub" -> new String[]{"area: hub", "graveyard", "ruins", "coal mine", "wilderness"};
            case "mobs_spider" -> new String[]{"spider's den", "spiders den"}; case "dragon" -> new String[]{"the end", "dragon's nest", "void sepulture"};
            case "crimson" -> new String[]{"crimson isle"}; case "mobs_deep" -> new String[]{"deep caverns"}; case "mobs" -> new String[]{"dwarven mines", "glacite", "mineshaft"};
            case "mobs_hollows" -> new String[]{"crystal hollows"}; case "mobs_park" -> new String[]{"the park", "howling cave"};
            case "mobs_marsh" -> new String[]{"moonglade", "marsh"}; case "mobs_canyon" -> new String[]{"torrhus", "canyon"}; case "mobs_spooky" -> new String[]{"spooky"};
            case "mobs_dungeon" -> new String[]{"catacombs", "dungeon"}; case "slayer" -> new String[]{};
            default -> new String[]{}; };
        for (String k : keys) if (a.contains(k)) return true;
        return false;
    }
    private static String clean(String s) { return s == null ? "" : s.replaceAll("§.", "").toLowerCase(Locale.ROOT).trim(); }
}
