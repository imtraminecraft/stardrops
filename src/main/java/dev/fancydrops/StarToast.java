package dev.fancydrops;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

import java.util.ArrayDeque;

/**
 * A new star: a chat line you can click (it runs /sd star &lt;id&gt;, which opens the sky flown to that star, blinking) and a card
 * that slides in at the top right. The card is a little patch of night sky: the star ignites in its constellation's colour with
 * a flash and rays, "NEW STAR LIT" in the tier's colour, the achievement's name, and which constellation it belongs to.
 * Several in a row queue up and show one after another.
 */
final class StarToast {
    private StarToast() {}

    private record Lit(String id, String name, String group, int col, int tier, String reward) {}
    private static final ArrayDeque<Lit> QUEUE = new ArrayDeque<>();
    private static Lit cur; private static long curAt;
    private static final long LIFE = 5200;
    private static final java.util.Set<String> SEEN = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** A star was lit: say so in chat (clickable) and queue the card. Call on the client thread. */
    static void lit(String id, String name, String ch, int tier, String reward) {
        Minecraft mc = Minecraft.getInstance(); if (mc.player == null || !SEEN.add(id)) return;          // once per star, whoever lit it first
        int col = Codex.colourOf(ch); String group = Codex.nameOf(ch); int t = Math.max(0, Math.min(2, tier));
        var hover = Component.literal("Click to see this star").withStyle(s -> s.withColor(0xFFE9B8));
        var msg = Chat.prefix().append(" ").append(Chat.color("New star lit: ", Chat.TEXT))
            .append(Chat.color(name, col & 0xFFFFFF).withStyle(s -> s.withBold(true)))
            .append(Chat.color("  ·  " + group + "  ·  " + LocalLedger.TIER[t], Chat.DIM));
        if (!reward.isEmpty()) { CosmeticData.Def d = CosmeticData.byId(reward); if (d != null) msg.append(Chat.color("  ·  unlocks " + d.name(), Chat.DIM)); }
        msg.append(Chat.color("  [View]", 0xFFE9B8).withStyle(s -> s.withUnderlined(true)));
        msg.withStyle(s -> s.withClickEvent(new ClickEvent.RunCommand("/sd star " + id)).withHoverEvent(new HoverEvent.ShowText(hover)));
        mc.player.sendSystemMessage(msg);
        SoundFx.achievement();
        QUEUE.add(new Lit(id, name, group, col, t, reward));
    }

    /** From the HUD. */
    static void render(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance(); if (mc.level == null) return;
        long now = System.currentTimeMillis();
        if (cur == null || now - curAt > LIFE) { cur = QUEUE.poll(); curAt = now; if (cur == null) return; }
        float u = (now - curAt) / 1000f, life = LIFE / 1000f;
        float in = ease(Math.min(1f, u / 0.45f)), out = ease(Math.min(1f, Math.max(0f, (u - (life - 0.5f)) / 0.5f)));
        float e = in * (1f - out); if (e < 0.01f) return;
        Font f = mc.font; int W = g.guiWidth(), w = 168, h = 40;
        float x = W - w - 8 + (1f - in) * (w + 20) + out * (w + 20), y = 8;
        draw(g, f, x, y, w, h, u, e, cur);
    }

    static void draw(GuiGraphicsExtractor g, Font f, float x, float y, int w, int h, float u, float e, Lit l) {
        int a = Math.round(255 * e), col = l.col & 0xFFFFFF, tc = LocalLedger.TIER_COL[l.tier] & 0xFFFFFF;
        Gfx.push(g); Gfx.translate(g, x, y);
        NebulaTex.glow(g, w / 2f, h / 2f + 4, w * 0.6f, h, Gfx.argb(Math.round(90 * e), 0x000000));
        NowPlayingHud.round(g, 0, 0, w, h, Gfx.argb(Math.round(238 * e), 0x070914));
        NebulaTex.glow(g, 22, h / 2f, 46, 30, Gfx.argb(Math.round(70 * e), col));                           // the constellation's light
        NebulaTex.glow(g, w * 0.7f, h * 0.3f, w * 0.5f, h * 0.6f, Gfx.argb(Math.round(28 * e), 0x6A5AD8));   // a wisp of nebula
        for (int i = 0; i < 14; i++) {                                                                   // a patch of sky: tiny stars twinkling
            float sx = 4 + Ritual.rnd(i * 7 + 3) * (w - 8), sy = 3 + Ritual.rnd(i * 13 + 5) * (h - 6);
            float tw = 0.4f + 0.6f * Math.abs((float) Math.sin(u * (1.5 + Ritual.rnd(i) * 2) + i));
            g.fill(Math.round(sx), Math.round(sy), Math.round(sx) + 1, Math.round(sy) + 1, Gfx.argb(Math.round(a * 0.5f * tw), 0xFFFFFF));
        }
        g.fill(3, 0, w - 3, 1, Gfx.argb(Math.round(a * 0.7f), tc));                                     // the edge in the tier's colour
        g.fill(0, 3, 2, h - 3, Gfx.argb(a, col));
        // the star igniting: a flash, an expanding ring, rays that settle and slowly turn
        float sx = 22, sy = h / 2f, ig = Math.min(1f, Math.max(0f, (u - 0.25f) / 0.6f)), flash = (float) Math.max(0, 1 - Math.abs(u - 0.4f) / 0.35f);
        NebulaTex.glow(g, sx, sy, 6 + 22 * flash, 6 + 22 * flash, Gfx.argb(Math.round(a * 0.8f * flash), 0xFFFFFF));
        if (ig > 0 && ig < 1) { float rr = 4 + 22 * ease(ig); int n = 24; for (int i = 0; i < n; i++) { double an = i * Math.PI * 2 / n; g.fill(Math.round(sx + (float) Math.cos(an) * rr), Math.round(sy + (float) Math.sin(an) * rr), Math.round(sx + (float) Math.cos(an) * rr) + 1, Math.round(sy + (float) Math.sin(an) * rr) + 1, Gfx.argb(Math.round(a * (1 - ig)), col)); } }
        float s = ease(ig);
        NebulaTex.glow(g, sx, sy, 14 * s, 14 * s, Gfx.argb(Math.round(a * 0.6f), col));
        NebulaTex.glow(g, sx, sy, 6 * s, 6 * s, Gfx.argb(a, Ritual.mixc(col, 0xFFFFFF, 0.5f)));
        int rays = l.tier == 0 ? 2 : l.tier == 1 ? 3 : 4; float spin = u * 0.6f, len = 11 * s * (0.9f + 0.1f * (float) Math.sin(u * 4));
        for (int i = 0; i < rays; i++) {
            float an = spin + i * (float) Math.PI / rays, ln = i % 2 == 1 ? len * 0.6f : len;
            Gfx.push(g); Gfx.translate(g, sx, sy); Gfx.rotate(g, an); g.fill(Math.round(-ln), 0, Math.round(ln), 1, Gfx.argb(Math.round(a * 0.85f), Ritual.mixc(l.tier >= 1 ? tc : col, 0xFFFFFF, 0.5f))); Gfx.pop(g);
        }
        NebulaTex.glow(g, sx, sy, 2.2f * s, 2.2f * s, Gfx.argb(a, 0xFFFFFF));
        // the words
        float tin = ease(Math.min(1f, Math.max(0f, (u - 0.2f) / 0.4f))), sl = (1f - tin) * 6;
        small(g, f, "NEW STAR LIT", 42 + sl, 6, 0.55f, Gfx.argb(Math.round(a * tin), Ritual.mixc(tc, 0xFFFFFF, 0.2f)));
        String nm = l.name; if (f.width(nm) > w - 50) nm = f.plainSubstrByWidth(nm, w - 50 - f.width("…")) + "…";
        Gfx.txt(g, f, nm, Math.round(42 + sl), 14, Gfx.argb(Math.round(a * tin), 0xFFFFFF), true);
        small(g, f, l.group + "  ·  " + LocalLedger.TIER[l.tier] + (l.reward.isEmpty() ? "" : "  ·  reward"), 42 + sl * 0.6f, 26, 0.6f, Gfx.argb(Math.round(a * 0.7f * tin), Ritual.mixc(col, 0xFFFFFF, 0.5f)));
        small(g, f, "click it in chat", w - 6 - f.width("click it in chat") * 0.5f, 33, 0.5f, Gfx.argb(Math.round(a * 0.4f * tin), 0xFFFFFF));
        float bar = 1f - Math.min(1f, u / (LIFE / 1000f));                                           // time left on the card
        g.fill(3, h - 1, 3 + Math.round((w - 6) * bar), h, Gfx.argb(Math.round(a * 0.6f), col));
        Gfx.pop(g);
    }

    /** The bench: draw one card at u seconds. */
    static void demo(GuiGraphicsExtractor g, Font f, float x, float y, float u, String name, String ch, int tier, String reward) {
        draw(g, f, x, y, 168, 40, u, 1f, new Lit("x", name, Codex.nameOf(ch), Codex.colourOf(ch), tier, reward));
    }
    private static void small(GuiGraphicsExtractor g, Font f, String s, float x, float y, float k, int c) { Gfx.push(g); Gfx.translate(g, x, y); Gfx.scale(g, k); Gfx.txt(g, f, s, 0, 0, c, false); Gfx.pop(g); }
    private static float ease(float v) { return 1f - (1f - v) * (1f - v) * (1f - v); }
}
