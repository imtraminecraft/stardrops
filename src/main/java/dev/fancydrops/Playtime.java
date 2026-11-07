package dev.fancydrops;

import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/**
 * Playtime: how long you've played with FancyDrops on (counted while you're in a world and the game isn't paused), kept in your config.
 * It feeds the Playtime constellation (The Hourglass): its stars are the "pt" rows of the achievements table.
 */
final class Playtime {
    private Playtime() {}

    private static long last, savedAt;

    /** Every client tick. */
    static void tick(Minecraft mc) {
        long now = System.currentTimeMillis();
        long dt = last == 0 ? 0 : Math.min(1000, now - last); last = now;
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        FancyConfig c = FancyConfig.get();
        double before = c.playMinutes; c.playMinutes += dt / 60000.0;
        if ((long) before != (long) c.playMinutes) for (String id : LocalLedger.timeTick()) {                // a Playtime star is earned: note when, and say so
            LocalLedger.Def g = LocalLedger.def(id); if (g == null) continue;
            c.playDone.put(id, now);
            var msg = Chat.prefix().append(" ").append(Chat.color("New star lit: ", Chat.TEXT)).append(Chat.color(g.name(), Chat.NUMBER).withStyle(st -> st.withBold(true))).append(Chat.color("  ·  Playtime", Chat.DIM));
            mc.player.sendSystemMessage(msg); SoundFx.achievement();
        }
        if (now - savedAt > 120_000) { savedAt = now; try { FancyConfig.save(); } catch (Throwable ignored) { } }
    }

    /** Played time down to the second: "12:04:37" (hours can pass 99). */
    static String clock() { long sec = (long) (FancyConfig.get().playMinutes * 60); return String.format(java.util.Locale.ROOT, "%d:%02d:%02d", sec / 3600, sec / 60 % 60, sec % 60); }
    static String hours() { double m = FancyConfig.get().playMinutes; return m < 60 ? Math.round(m) + " min" : String.format(java.util.Locale.ROOT, "%.1f h", m / 60); }
}
