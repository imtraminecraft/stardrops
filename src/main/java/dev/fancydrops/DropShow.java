package dev.fancydrops;

import dev.fancydrops.AnimationManager.Anim;
import dev.fancydrops.AnimationManager.Item;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * The words of a drop animation, in Butcher's showcase style (the author kept these from the pixel cutscenes, Nov 7; the scenes
 * themselves are the classic Ritual ones): black-outlined like the game's own drop messages; the name typed in letter by letter,
 * the odds in grey, the price counting up in gold. RNG adds the odds as a bold banner stamped in big over the item with a flash of
 * its colour, and the price source under the price.
 */
final class DropShow {
    private DropShow() {}

    private static float cl(float v) { return v < 0 ? 0 : v > 1 ? 1 : v; }
    private static float sm(float v) { v = cl(v); return v * v * (3 - 2 * v); }
    private static float eo(float v) { v = cl(v); return 1f - (1f - v) * (1f - v) * (1f - v); }
    private static int mix(int a, int b, float f) { return Ritual.mixc(a, b, cl(f)); }
    private static int A(float a, int rgb) { return Gfx.argb(Math.round(cl(a) * 255), rgb & 0xFFFFFF); }

    /** The text scale for a GUI this tall. */
    static float scale(int H) { return Math.max(1f, Math.min(1.35f, H * 0.7f / 150f)) * Math.max(0.75f, Math.min(1.5f, FancyConfig.get().animScale)); }

    /**
     * The words. bannerY: where RNG's stamped banner goes; below: the top of the name. p: seconds since the drop appeared.
     */
    static void words(GuiGraphicsExtractor g, Font f, Anim a, Item it, float p, float cx, float bannerY, float below, float ts, float wa, boolean rng, int rc) {
        if (wa < 0.02f) return;
        // RNG: the banner, stamped - in big, down to its size, a flash of its colour as it lands
        if (rng) {
            float k = cl((p - 0.15f) / 0.26f);
            if (k > 0) {
                String ban = (it.odds == null || it.odds.isEmpty() ? "RNG DROP" : it.odds.toUpperCase(java.util.Locale.ROOT)) + "!";
                float sc = ts * (1.3f + 1.2f * (1f - eo(k))), by = bannerY - 4 * sc;
                int bc = mix(rc, 0xFFFFFF, 0.15f);
                if (k < 1f) NebulaTex.glow(g, cx, by + 4 * sc, 80 * ts, 14 * ts, A(0.5f * (1f - k) * wa, rc));
                Component c = Component.literal(ban).withStyle(ChatFormatting.BOLD);
                outlinedC(g, f, c, cx - f.width(c) * sc / 2f, by, sc, Math.min(wa, k * 2.5f), bc);
            }
        }
        // the name, a letter at a time
        List<Item> all = a.items(); int n = Math.min(4, all.size());
        float ns = (rng ? 1.6f : 1.25f) * ts * (n > 1 ? 0.8f : 1f), y = below;
        float nt = p - (rng ? 0.35f : 0.18f);
        for (int i = 0; i < n && nt > 0; i++) {
            String name = all.get(i).title(); int shown = Math.min(name.length(), (int) ((nt - i * 0.12f) * (rng ? 32 : 40)));
            if (shown > 0) {
                String part = name.substring(0, shown); float w = f.width(name) * ns;
                outlined(g, f, part, cx - w / 2f, y, ns, wa, all.get(i).color() & 0xFFFFFF);
            }
            y += 10 * ns;
        }
        // the odds, then the price counting up
        float la = sm((p - (rng ? 0.75f : 0.45f)) / 0.25f) * wa;
        if (la > 0.02f) {
            String line = (it.chance != null && !it.chance.isEmpty() ? it.chance : "") + (it.mf != null && !it.mf.isEmpty() ? (it.chance == null || it.chance.isEmpty() ? "" : "  ·  ") + it.mf + " MF" : "");
            if (!rng && it.odds != null && !it.odds.isEmpty()) line = it.odds + (line.isEmpty() ? "" : "  ·  " + line);
            if (!line.isEmpty()) { outlined(g, f, line, cx - f.width(line) * ts * 0.9f / 2f, y + 1, ts * 0.9f, la, 0xC8C8C8); y += 10 * ts; }
            if (it.price > 0) {
                float kc = cl((p - (rng ? 0.85f : 0.55f)) / (rng ? 1.1f : 0.7f));
                long total = Math.round(it.price * Math.max(1, it.amount)), shown = Math.round(total * (1f - (1f - kc) * (1f - kc) * (1f - kc)));
                String pr = String.format(java.util.Locale.ROOT, "%,d coins", shown); float ps = (rng ? 1.25f : 1f) * ts;
                outlined(g, f, pr, cx - f.width(String.format(java.util.Locale.ROOT, "%,d coins", total)) * ps / 2f, y + 1, ps, la, 0xFFD84A);
                y += 10 * ps;
                if (rng && it.priceSource != null && !it.priceSource.isEmpty()) outlined(g, f, it.priceSource, cx - f.width(it.priceSource) * ts * 0.75f / 2f, y + 1, ts * 0.75f, la * 0.7f, 0x9A9A9A);
            } else if (it.price == -2) outlined(g, f, "pricing...", cx - f.width("pricing...") * ts * 0.8f / 2f, y + 1, ts * 0.8f, la * 0.7f, 0x9A9A9A);
        }
    }


    /** Text with a black outline, like the game's own drop messages. */
    private static void outlined(GuiGraphicsExtractor g, Font f, String s, float x, float y, float sc, float a, int rgb) {
        if (a < 0.03f || s.isEmpty()) return;
        Gfx.push(g); Gfx.translate(g, x, y); Gfx.scale(g, sc);
        int o = A(a * 0.9f, 0x000000);
        Gfx.txt(g, f, s, -1, 0, o, false); Gfx.txt(g, f, s, 1, 0, o, false); Gfx.txt(g, f, s, 0, -1, o, false); Gfx.txt(g, f, s, 0, 1, o, false);
        Gfx.txt(g, f, s, 0, 0, A(a, rgb), false); Gfx.pop(g);
    }
    private static void outlinedC(GuiGraphicsExtractor g, Font f, Component s, float x, float y, float sc, float a, int rgb) {
        if (a < 0.03f) return;
        Gfx.push(g); Gfx.translate(g, x, y); Gfx.scale(g, sc);
        int o = A(a * 0.9f, 0x000000);
        Gfx.txt(g, f, s, -1, 0, o, false); Gfx.txt(g, f, s, 1, 0, o, false); Gfx.txt(g, f, s, 0, -1, o, false); Gfx.txt(g, f, s, 0, 1, o, false);
        Gfx.txt(g, f, s, 0, 0, A(a, rgb), false); Gfx.pop(g);
    }
}
