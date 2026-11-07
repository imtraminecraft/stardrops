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
        FancyConfig c = FancyConfig.get();
        ts *= c.textK();
        // RNG: the banner, stamped - in big, down to its size, a flash of its colour as it lands
        if (rng && c.wBanner) {
            float k = cl((p - 0.15f) / 0.26f);
            if (k > 0) {
                String ban = (it.odds == null || it.odds.isEmpty() ? "RNG DROP" : it.odds.toUpperCase(java.util.Locale.ROOT)) + "!";
                float sc = ts * c.titleScale * (1.3f + 1.2f * (1f - eo(k))), by = bannerY - 4 * sc;
                if (k < 1f) NebulaTex.glow(g, cx, by + 4 * sc, 80 * ts, 14 * ts, A(0.5f * (1f - k) * wa, rc));
                Component cc = txt(ban, true);
                out(g, f, cc, cx - f.width(cc) * sc / 2f, by, sc, Math.min(wa, k * 2.5f), mix(rc, 0xFFFFFF, 0.15f));
            }
        }
        // the name, a letter at a time (or fading in)
        List<Item> all = a.items(); int n = Math.min(4, all.size());
        float ns = (rng ? 1.6f : 1.25f) * ts * c.titleScale * (n > 1 ? 0.8f : 1f), y = below;
        float nt = p - (rng ? 0.35f : 0.18f);
        for (int i = 0; i < n && nt > 0; i++) {
            String name = all.get(i).title(); float li = nt - i * 0.12f;
            int shown = c.wType ? Math.min(name.length(), (int) (li * (rng ? 32 : 40))) : name.length();
            float na = c.wType ? wa : wa * sm(li / 0.35f);
            if (shown > 0 && li > 0) {
                Component full = txt(name, c.textBold), part = txt(name.substring(0, shown), c.textBold);
                out(g, f, part, cx - f.width(full) * ns / 2f, y, ns, na, all.get(i).color() & 0xFFFFFF);
            }
            y += 10 * ns;
        }
        // the odds, then the price counting up
        float sd = c.subDelay - 0.35f, ss = ts * c.subScale;
        float la = sm((p - (rng ? 0.75f : 0.45f) - sd) / 0.25f) * wa;
        if (la > 0.02f) {
            String chance = c.showOdds && it.chance != null ? it.chance : "", mf = c.showMagicFind && it.mf != null && !it.mf.isEmpty() ? it.mf + " MF" : "";
            String line = chance + (!chance.isEmpty() && !mf.isEmpty() ? "  ·  " : "") + mf;
            if (!rng && c.showOdds && it.odds != null && !it.odds.isEmpty()) line = it.odds + (line.isEmpty() ? "" : "  ·  " + line);
            if (!line.isEmpty()) { Component lc = txt(line, false); out(g, f, lc, cx - f.width(lc) * ss * 0.9f / 2f, y + 1, ss * 0.9f, la, 0xC8C8C8); y += 10 * ss; }
            if (c.showPrice && it.price > 0) {
                float kc = c.wCount ? cl((p - (rng ? 0.85f : 0.55f) - sd) / (rng ? 1.1f : 0.7f)) : 1f;
                long total = Math.round(it.price * Math.max(1, it.amount)), shown = Math.round(total * (1f - (1f - kc) * (1f - kc) * (1f - kc)));
                float ps = (rng ? 1.25f : 1f) * ss;
                Component pc = txt(String.format(java.util.Locale.ROOT, "%,d coins", shown), false), pw = txt(String.format(java.util.Locale.ROOT, "%,d coins", total), false);
                out(g, f, pc, cx - f.width(pw) * ps / 2f, y + 1, ps, la, 0xFFD84A);
                y += 10 * ps;
                if (c.showPriceSource && it.priceSource != null && !it.priceSource.isEmpty()) { Component sc2 = txt(it.priceSource, false); out(g, f, sc2, cx - f.width(sc2) * ss * 0.75f / 2f, y + 1, ss * 0.75f, la * 0.7f, 0x9A9A9A); }
            } else if (c.showPrice && it.price == -2) { Component pc = txt("pricing...", false); out(g, f, pc, cx - f.width(pc) * ss * 0.8f / 2f, y + 1, ss * 0.8f, la * 0.7f, 0x9A9A9A); }
        }
    }

    /** A piece of the words in the chosen font (Text Changes > Font), bold when asked. */
    private static Component txt(String s, boolean bold) { Component c = Fonts.text(s, FancyConfig.get().textFont); return bold ? c.copy().withStyle(ChatFormatting.BOLD) : c; }
    /** Outlined or not, as Text Changes > Font says. */
    private static void out(GuiGraphicsExtractor g, Font f, Component s, float x, float y, float sc, float a, int rgb) {
        if (FancyConfig.get().textOutline) outlinedC(g, f, s, x, y, sc, a, rgb);
        else { if (a < 0.03f) return; Gfx.push(g); Gfx.translate(g, x, y); Gfx.scale(g, sc); Gfx.txt(g, f, s, 0, 0, A(a, rgb), true); Gfx.pop(g); }
    }

    /** Text with a black outline, like the game's own drop messages. */
    private static void outlinedC(GuiGraphicsExtractor g, Font f, Component s, float x, float y, float sc, float a, int rgb) {
        if (a < 0.03f) return;
        Gfx.push(g); Gfx.translate(g, x, y); Gfx.scale(g, sc);
        int o = A(a * 0.9f, 0x000000);
        Gfx.txt(g, f, s, -1, 0, o, false); Gfx.txt(g, f, s, 1, 0, o, false); Gfx.txt(g, f, s, 0, -1, o, false); Gfx.txt(g, f, s, 0, 1, o, false);
        Gfx.txt(g, f, s, 0, 0, A(a, rgb), false); Gfx.pop(g);
    }
}
