package dev.stardrops;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Move the HUDs: drag a card anywhere, scroll over it to resize it, R puts the one under the mouse back (or all of them), Esc saves.
 * Opened for one card from its settings page, or for every HUD at once with /sd edit (Now Playing, the lyrics, the celestial clock;
 * the drop animation is not a HUD you place).
 */
final class HudEditScreen extends Screen {
    static final int NOW_PLAYING = 0, LINE = 1, CLOCK = 2, ALL = -1;
    private static final String[] NAME = {"Now Playing", "Lyrics", "Celestial clock"};
    private final Screen back; private final int which; private int drag = -1; private double grabX, grabY;
    HudEditScreen(Screen back) { this(back, NOW_PLAYING); }
    HudEditScreen(Screen back, int which) { super(Component.literal("Move HUD")); this.back = back; this.which = which; }
    @Override public boolean isPauseScreen() { return false; }
    @Override protected void init() { NowPlayingHud.preview = shows(NOW_PLAYING); LyricsHud.preview = shows(LINE); CosmosHud.preview = shows(CLOCK); }
    @Override public void removed() { NowPlayingHud.preview = false; LyricsHud.preview = false; CosmosHud.preview = false; StardropsConfig.save(); }
    @Override public void onClose() { Minecraft.getInstance().setScreenAndShow(back); }
    private boolean shows(int i) { return which == ALL || which == i; }

    private int[] box(int i) {
        return switch (i) {
            case LINE -> new int[]{LyricsHud.bx0, LyricsHud.by0, LyricsHud.bx1, LyricsHud.by1};
            case CLOCK -> CosmosHud.clockBox();
            default -> new int[]{NowPlayingHud.bx0, NowPlayingHud.by0, NowPlayingHud.bx1, NowPlayingHud.by1};
        };
    }
    private float x(int i) { StardropsConfig c = StardropsConfig.get(); return i == LINE ? c.lineX : i == CLOCK ? c.cosClockX / (float) Math.max(1, width) : c.npX; }
    private int y(int i) { StardropsConfig c = StardropsConfig.get(); return i == LINE ? Math.round(c.lyY * height) : i == CLOCK ? c.cosClockY : c.npY; }
    private int scale(int i) { StardropsConfig c = StardropsConfig.get(); return i == LINE ? c.lyricsSize : i == CLOCK ? c.cosClockSize : c.npScale; }
    private void set(int i, float x, int y, int s) {
        StardropsConfig c = StardropsConfig.get();
        if (i == LINE) { c.lineX = x; c.lyY = y / (float) Math.max(1, height); c.lyricsSize = s; }
        else if (i == CLOCK) { c.cosClockX = Math.round(x * width); c.cosClockY = y; c.cosClockSize = s; }
        else { c.npX = x; c.npY = y; c.npScale = s; }
    }
    private void reset(int i) { set(i, i == CLOCK ? 26f / Math.max(1, width) : 0.5f, i == LINE ? Math.round(height * 0.64f) : i == CLOCK ? 24 : 4, 100); }
    private boolean over(int i, double mx, double my) { int[] b = box(i); return b[2] > b[0] && mx >= b[0] - 3 && mx < b[2] + 3 && my >= b[1] - 3 && my < b[3] + 3; }
    /** The card under the mouse (the one drawn last wins), or -1. */
    private int under(double mx, double my) { for (int i = 2; i >= 0; i--) if (shows(i) && over(i, mx, my)) return i; return -1; }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        g.fill(0, 0, width, height, 0x55000000);
        for (int x = 0; x < width; x += 20) g.fill(x, 0, x + 1, height, 0x10FFFFFF);
        for (int y = 0; y < height; y += 20) g.fill(0, y, width, y + 1, 0x10FFFFFF);
        g.fill(width / 2, 0, width / 2 + 1, height, 0x309B7BFF);
        if (shows(CLOCK)) CosmosHud.render(g);
        if (shows(LINE)) LyricsHud.render(g);
        if (shows(NOW_PLAYING)) NowPlayingHud.render(g);
        int hot = drag >= 0 ? drag : under(mx, my);
        for (int i = 0; i < 3; i++) {
            if (!shows(i)) continue; int[] b = box(i); if (b[2] <= b[0]) continue;
            int c = i == hot ? 0xFFC8B4FF : 0x66FFFFFF;
            for (int x = b[0] - 3; x < b[2] + 3; x += 4) { g.fill(x, b[1] - 3, x + 2, b[1] - 2, c); g.fill(x, b[3] + 2, x + 2, b[3] + 3, c); }
            for (int y = b[1] - 3; y < b[3] + 3; y += 4) { g.fill(b[0] - 3, y, b[0] - 2, y + 2, c); g.fill(b[2] + 2, y, b[2] + 3, y + 2, c); }
            if (which == ALL) g.text(font, NAME[i] + "  " + scale(i) + "%", b[0] - 2, b[1] - 13 < 0 ? b[3] + 5 : b[1] - 13, i == hot ? 0xFFE6DCFF : 0xAAFFFFFF);
        }
        String h1 = which == ALL ? "Drag any HUD  •  scroll over one to resize  •  R resets the one under the mouse  •  Esc when done"
                                 : "Drag the " + NAME[which] + "  •  scroll to resize  •  R to reset  •  Esc when done";
        g.centeredText(font, h1, width / 2, height - 24, 0xFFFFFFFF);
        if (which != ALL) g.centeredText(font, "Size " + scale(which) + "%", width / 2, height - 12, 0xFFB9B6E6);
    }
    @Override public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
        int i = which == ALL ? under(e.x(), e.y()) : over(which, e.x(), e.y()) ? which : -1;
        if (i >= 0) { drag = i; grabX = e.x() - x(i) * width; grabY = e.y() - y(i); return true; }
        return super.mouseClicked(e, dbl);
    }
    @Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (drag < 0) return super.mouseDragged(e, dx, dy);
        double nx = (e.x() - grabX) / width; if (drag != CLOCK && Math.abs(nx - 0.5) < 0.012) nx = 0.5;      // snaps to the middle
        set(drag, (float) Math.max(drag == CLOCK ? 0.0 : 0.05, Math.min(drag == CLOCK ? 1.0 : 0.95, nx)), (int) Math.max(0, Math.min(height - 20, e.y() - grabY)), scale(drag));
        return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent e) { drag = -1; return super.mouseReleased(e); }
    @Override public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int i = which == ALL ? under(mx, my) : which; if (i < 0) return true;
        set(i, x(i), y(i), (int) Math.max(60, Math.min(160, scale(i) + Math.signum(sy) * 5))); return true;
    }
    @Override public boolean keyPressed(KeyEvent e) {
        if (e.key() == 82) {
            if (which != ALL) reset(which);
            else { int i = under(Minecraft.getInstance().mouseHandler.xpos() * width / Math.max(1, Minecraft.getInstance().getWindow().getScreenWidth()), Minecraft.getInstance().mouseHandler.ypos() * height / Math.max(1, Minecraft.getInstance().getWindow().getScreenHeight()));
                   if (i >= 0) reset(i); else for (int k = 0; k < 3; k++) reset(k); }
            return true;
        }
        return super.keyPressed(e);
    }
}
