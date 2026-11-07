package dev.fancydrops;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.*;

/**
 * Achievements as a star map. Every chapter of the ledger is a constellation in one deep sky, every achievement is a star in it.
 * Earned stars burn in the chapter's colour, and the figure's lines light up between two earned stars, so a finished chapter draws
 * its whole picture. Bottom left, someone sits on a hill with a telescope and follows the constellation under your mouse.
 *
 * Lights are one soft-glow picture each and lines are single rotated bars ({@link NebulaTex}), so the whole sky costs a few hundred
 * draws a frame. Opening warps in out of the button; click a constellation to fly in (names, progress rings, rewards, the chapter's
 * numbers); right click, Esc or "All constellations" flies out; arrows and the wheel move between constellations.
 */
final class Codex {
    private Codex() {}

    interface Host {
        Font font();
        default int width(String s) { return font().width(s); }
        void toast(String text, int col);
        void tip(String lines, int col);
    }

    // ---- the star groups: the heart of the map (every drop: the Ledger and Fortune chapters together), then one group for each main page of the
    //      Drop Library, round it like the points of a star. Lanes of light run from the heart to each group and fill as you complete it.
    private static final String[] ACH = {"core", "myth", "mob", "farm", "fish", "dye", "time", "mine"};
    private static final String[] ACH_NAME = {"Every Drop", "Mythological", "Mob Drops", "Farming", "Fishing", "Table & Dyes", "Playtime", "Mining"};
    private static final String[] FIGURE = {"The Pole Star", "The Griffin", "The Sword", "The Sheaf", "The Fish", "The Drop", "The Hourglass", "The Pickaxe"};
    private static final int[] ACH_COL = {0xFFFFD27A, 0xFF55D8FF, 0xFFFF6A6A, 0xFF8CE86A, 0xFF4FC3FF, 0xFFFF72C6, 0xFFA9B4FF, 0xFF55D8FF};
    private static final String[] QUOTE = {"Every find, written down.", "Dig, and the earth answers.", "They drop what they guard.", "The harvest remembers.", "Patience, then a splash.", "Every colour has a story.", "Time flies when the drops are good.", "Strike the rock, and listen."};
    private static final float[][] HOME = {{0.42f, 0.56f}, {0.37f, 0.17f}, {0.79f, 0.78f}, {0.59f, 0.77f}, {0.22f, 0.67f}, {0.19f, 0.27f}, {0.6f, 0.21f}, {0.85f, 0.4f}};      // fractions of the width, of the height
    /** Which group a server chapter belongs to. */
    private static int groupOf(String ch) { return switch (ch) { case "myth" -> 1; case "rare", "mob" -> 2; case "mine" -> 7; case "farm" -> 3; case "fish" -> 4; case "dye" -> 5; case "time" -> 6; default -> 0; }; }

    static boolean open;
    private static long openAt, closeAt, zoomAt, hoverAt, blinkAt;
    /** The opening film: the stargazer at the telescope (INTRO ms, then the page starts at openAt), the camera pushes into the lens, and
     *  the page opens out of it like an iris while the camera pulls back from the hill. Click or Esc skips it. */
    private static final long INTRO = 2600, IRIS = 900; private static long introFrom; private static int introSounds;
    private static String blinkId = "";
    private static int[] from = {0, 0, 0, 0};
    private static int focus = -1, prevFocus = -1, hoverCon = -1, lastHoverCon = -1;
    private static final long OPEN = 900, CLOSE = 300, ZOOM = 480;
    private static final List<Object[]> HITS = new ArrayList<>();
    private static final List<float[]> LABELS = new ArrayList<>();          // label boxes placed this frame (so they don't overlap)
    private static boolean free(float x, float y, float w, float h) { for (float[] b : LABELS) if (x < b[2] && x + w > b[0] && y < b[3] && y + h > b[1]) return false; return true; }
    private static String lastHoverStar = "";
    /** Only one star is pointed at: the nearest one under the mouse (picked over a frame, used the next), so stars close together never fight. */
    private static String hoverStar = "", hoverPick = ""; private static float hoverPickD = 1e9f; private static long hoverSoundAt;
    private static final Map<String, Long> IGNITE = new HashMap<>();
    private static final Set<String> igSounded = new HashSet<>();
    private static boolean ignitesPlanned;
    private static float scopeAng = -0.9f;                                       // where the stargazer's telescope points (radians)

    // ------------------------------------------------------------------ open / close
    static void open(int[] btn, int chapter) {
        long t0 = System.currentTimeMillis(); introFrom = t0; introSounds = 0;
        boolean intro = FancyConfig.get().achIntro;
        open = true; openAt = t0 + (intro ? INTRO : 0); closeAt = 0; from = btn.clone(); IGNITE.clear(); igSounded.clear(); ignitesPlanned = false; blinkId = "";
        focus = chapter >= 1 && chapter <= ACH.length ? chapter - 1 : -1; prevFocus = focus; zoomAt = 0;
        Achievements.refresh(true);
        sound("sky_open", 1f, 0.8f);
    }
    static int colourOf(String ch) { return ACH_COL[groupOf(ch)]; }
    static String nameOf(String ch) { return ACH_NAME[groupOf(ch)]; }
    /** Opens straight on one achievement: flies to its constellation and makes its star blink. */
    static void openAt(int[] btn, String achId) {
        Achievements.Profile p = Achievements.profile(); int c = -1;
        if (p != null) for (Achievements.Ach a : p.list()) if (a.id().equals(achId)) c = groupOf(a.ch());
        open(btn, -1);
        if (c >= 0) { prevFocus = -1; focus = c; zoomAt = openAt + OPEN - 200; }
        blinkId = achId; blinkAt = openAt + OPEN + 200;
    }
    static void close() { if (open && closeAt == 0) { closeAt = System.currentTimeMillis(); sound("sky_close", 1f, 0.6f); } }
    static boolean busy() { return open; }
    /** The sky fully covers the menu (open and settled, not closing): the menu behind it needn't be drawn at all. */
    static boolean covering() { long n = System.currentTimeMillis(); return open && closeAt == 0 && (openAt > introFrom || n - openAt > 450); }
    private static void fly(int c) { if (c == focus) return; prevFocus = focus; focus = c; zoomAt = System.currentTimeMillis(); sound("sky_zoom", c < 0 ? 0.85f : 1f, 0.7f); }

    private static void planIgnites() {
        Achievements.Profile p = Achievements.profile(); if (p == null || ignitesPlanned) return;
        ignitesPlanned = true;
        FancyConfig c = FancyConfig.get(); long at = Math.max(now, openAt + OPEN + 150); int k = 0;
        for (Achievements.Ach a : p.list()) if (a.done() && !c.seenAch.contains(a.id())) { IGNITE.put(a.id(), at + k * 380L); c.seenAch.add(a.id()); k++; }
        if (k > 0) try { FancyConfig.save(); } catch (Throwable ignored) { }
    }

    private static void sound(String name, float pitch, float gain) { try { SoundFx.Fx fx = SoundFx.byName(name); if (fx == null) fx = SoundFx.byName("std_beat"); if (fx != null) SoundFx.play(fx, pitch, gain); } catch (Throwable ignored) { } }
    private static float cl(float v) { return v < 0 ? 0 : v > 1 ? 1 : v; }
    private static float ease(float v) { v = cl(v); return v * v * v * (v * (v * 6 - 15) + 10); }
    private static int mix(int a, int b, float f) { return Canvas.mix(a, b, cl(f)); }
    private static int argb(float a, int rgb) { return Gfx.argb(Math.round(cl(a) * 255), rgb & 0xFFFFFF); }
    private static float rnd(int n) { return Ritual.rnd(n); }

    // ------------------------------------------------------------------ the sky and the camera
    private static int W, H, sx0, sy0, sw, sh;
    private static Font font; private static Host host; private static int mx, my; private static long now;
    private static float camU, camV, camS, warp, alpha = 1f;
    private static GuiGraphicsExtractor G;

    private static float aspect() { return sw / (float) sh; }
    private static float[] centre(int c) { return new float[]{HOME[c][0] * aspect(), HOME[c][1]}; }
    private static float X(float u) { return sx0 + sw / 2f + (u - camU) * sh * camS; }
    private static float Y(float v) { return sy0 + sh / 2f + (v - camV) * sh * camS; }
    private static boolean inSky(float x, float y) { return x >= sx0 + 2 && x < sx0 + sw - 2 && y >= sy0 + 2 && y < sy0 + sh - 2; }

    private static List<Achievements.Ach> achOf(String id) {
        Achievements.Profile p = Achievements.profile(); List<Achievements.Ach> l = new ArrayList<>();
        if (p != null) for (Achievements.Ach a : p.list()) if (ACH[groupOf(a.ch())].equals(id)) l.add(a);
        return l;
    }
    static void draw(GuiGraphicsExtractor g, Host h, int width, int height, int mx0, int my0) {
        if (!open) return;
        try { g.nextStratum(); } catch (Throwable ignored) { }                        // above everything the menu drew, its text included
        G = g; now = AnimationManager.clock(); host = h; font = h.font(); W = width; H = height; mx = mx0; my = my0; HITS.clear(); LABELS.clear();
        float t = now - openAt, closeT = closeAt == 0 ? -1f : now - closeAt;
        if (closeT >= CLOSE) { open = false; return; }
        boolean film = openAt > introFrom;                                              // this opening has the film
        float fu = film && closeT < 0 ? now - introFrom : 1e9f;                       // ms into the opening shot
        if (film && introSounds < 1 && fu >= 700) { introSounds = 1; sound("sky_zoom", 0.9f, 0.7f); }
        open0 = 1f - ease(fu / 650f);                                                       // out of black, like opening your eyes
        dusk = 1f - ease((fu - 200f) / 2200f);                                              // the last of the sunset low on the horizon, fading
        night = ease((fu - 150f) / 1300f);                                                  // the bright stars first, then the faint ones (eyes adjusting)
        milky = ease((fu - 700f) / 1700f);                                                  // the Milky Way and the nebulae last of all
        tilt = 1f - ease((fu - 250f) / 2300f);                                              // the head tilting up from the horizon
        focusPull = 1f - ease((fu - 1700f) / 800f);
        conA = ease((fu - 1400f) / 800f);
        lineGrow = ease((fu - 1700f) / 900f);                                               // the figures draw themselves, star to star
        float grow = closeT >= 0 ? 1f - ease(closeT / CLOSE) : film ? 1f : ease(t / 420f);
        alpha = closeT >= 0 ? 1f - cl(closeT / (CLOSE * 0.8f)) : film ? 1f : cl(t / 200f);
        if (film && introSounds < 3 && t >= 0) { introSounds = 3; sound("sky_star", 1.4f, 0.6f); }
        planIgnites();
        { float tx = cl((mx - (W / 2f)) / Math.max(1f, W) + 0.5f) - 0.5f, ty = cl((my - (H / 2f)) / Math.max(1f, H) + 0.5f) - 0.5f;   // the mouse, smoothed: the layers of the sky slide against each other by depth
          long dt = Math.min(100, Math.max(0, now - lastPar)); lastPar = now; float k = 1f - (float) Math.exp(-dt / 140.0); mpx += (tx - mpx) * k; mpy += (ty - mpy) * k; }

        int fw = Math.round(Math.min(W - 24, H * 2.0f)), fh = Math.round(Math.min(H - 24, fw / 1.7f));
        float bx = (from[0] + from[2]) / 2f, by = (from[1] + from[3]) / 2f, cxs = W / 2f, cys = H / 2f;
        float gx = bx + (cxs - bx) * grow, gy = by + (cys - by) * grow, gs = Math.max(0.02f, grow);
        sw = Math.round(fw * gs); sh = Math.round(fh * gs); sx0 = Math.round(gx - sw / 2f); sy0 = Math.round(gy - sh / 2f);
        g.fill(0, 0, W, H, argb(0.88f * alpha, 0x02010A));
        if (sw < 8 || sh < 8) return;

        float z = zoomAt == 0 || now < zoomAt ? (zoomAt > now ? 0f : 1f) : ease((now - zoomAt) / (float) ZOOM);
        int pf = zoomAt > now ? -1 : prevFocus, fc = focus;
        float[] a0 = camOf(zoomAt > now ? -1 : prevFocus), a1 = camOf(zoomAt > now ? -1 : focus);
        if (zoomAt > now) { a0 = camOf(-1); a1 = a0; }
        camU = a0[0] + (a1[0] - a0[0]) * z; camV = a0[1] + (a1[1] - a0[1]) * z; camS = a0[2] + (a1[2] - a0[2]) * (z * z * (3 - 2 * z));
        float zoomed = zoomAt > now ? 0f : fc >= 0 ? (pf >= 0 ? 1f : z) : (pf >= 0 ? 1f - z : 0f);
        float flyWarp = zoomAt == 0 || now < zoomAt ? 0f : (float) Math.sin(Math.PI * cl((now - zoomAt) / (float) ZOOM)) * (pf >= 0 && fc >= 0 ? 0.6f : 1f);
        warp = Math.max(closeT >= 0 || film ? 0f : 1f - ease((t - 120f) / (OPEN - 120f)), flyWarp * 0.7f);
        int showFocus = zoomAt > now ? -1 : fc;

        bake();                                                                          // (only when the size changed)
        g.enableScissor(sx0, sy0, sx0 + sw, sy0 + sh);
        boolean tilting = tilt > 0.001f;
        if (tilting) { g.fill(sx0, sy0, sx0 + sw, sy0 + sh, 0xFF05060F); Gfx.push(g); Gfx.translate(g, 0, tilt * sh * 0.07f); }
        sky(g);
        aurora(g, (1f - cl(zoomed * 2f)) * milky);
        scenery(g, 1f - cl(zoomed * 2.5f));
        streaks(g);
        hoverCon = -1; hoverStar = hoverPick; hoverPick = ""; hoverPickD = 1e9f;
        if (conA > 0.01f) for (int c = 0; c < ACH.length; c++) constellation(g, c, zoomed, showFocus);
        if (hoverCon != lastHoverCon) { lastHoverCon = hoverCon; hoverAt = now; if (hoverCon >= 0) sound("sky_star", 0.75f, 0.35f); }
        shootingStars(g);
        if (tilting) Gfx.pop(g);
        if (dusk > 0.001f) twilight(g);
        if (tilting) { Gfx.push(g); Gfx.translate(g, 0, tilt * sh * 0.16f); }               // the ground is nearer: it slides further
        stargazer(g, 1f - cl(zoomed * 2.5f));
        if (tilting) Gfx.pop(g);
        nearDust(g, 1f - cl(zoomed * 1.5f));
        vignette(g);
        if (open0 > 0.002f) g.fill(sx0, sy0, sx0 + sw, sy0 + sh, argb(open0, 0x020208));
        g.disableScissor();
        frame(g);
        if (alpha > 0.6f && closeT < 0) {
            float in = cl((t - 350f) / 400f);
            header(g, zoomed, in, showFocus);
            if (showFocus >= 0 && zoomed > 0.5f) panel(g, showFocus, cl((zoomed - 0.5f) * 2f));
            cards(g, in * (1f - cl(zoomed * 3f)));
        }
    }
    private static boolean isTime(Achievements.Ach a) { return LocalLedger.live(a) != a.progress() || a.ch().equals("time") && a.id().startsWith("time_"); }
    private static float[] camOf(int c) {
        if (c < 0) return new float[]{aspect() / 2f, 0.5f, 1f};
        float[] o = centre(c); float top = -0.06f, bot = 0.06f;
        for (Node n : tree(c)) { top = Math.min(top, n.dv()); bot = Math.max(bot, n.dv()); }
        float s = Math.min(3.4f, 0.82f / Math.max(0.12f, bot - top + 0.12f)), mid = (top + bot) / 2f;
        return new float[]{o[0] - (aspect() * 0.17f) / s, o[1] + mid + 0.02f, s};
    }

    // ------------------------------------------------------------------ the deep sky
    private static void sky(GuiGraphicsExtractor g) {
        g.fill(sx0, sy0, sx0 + sw, sy0 + sh, 0xFF05060F);
        float par = 0.18f;
        float ow = sw * (1.25f + 0.25f * (camS - 1f) * par), oh = sh * (1.25f + 0.25f * (camS - 1f) * par);
        float dx = (camU - aspect() / 2f) * sh * camS * par, dy = (camV - 0.5f) * sh * camS * par;
        float drift = (now % 240000L) / 240000f * sw * 0.06f;
        int x = Math.round(sx0 + sw / 2f - ow / 2f - dx - drift + sw * 0.03f), y = Math.round(sy0 + sh / 2f - oh / 2f - dy);
        if (!NebulaTex.draw(g, x, y, Math.round(ow), Math.round(oh))) for (int i = 0; i < 24; i++) { int y0 = sy0 + sh * i / 24, y1 = sy0 + sh * (i + 1) / 24; g.fill(sx0, y0, sx0 + sw, y1, mix(0xFF0A1028, 0xFF1C1236, i / 24f)); }
        if (alpha < 1f) g.fill(sx0, sy0, sx0 + sw, sy0 + sh, argb(1f - alpha, 0x05060F));
        deepSky(g);
        if (milky < 1f) g.fill(sx0, sy0 - sh / 5, sx0 + sw, sy0 + sh, argb((1f - milky) * 0.92f, 0x05060F));
        float px = (camU - aspect() / 2f) * sh * camS * 0.35f - parX(0.3f), py = (camV - 0.5f) * sh * camS * 0.35f - parY(0.3f);
        for (int i = 0; i < 70; i++) {                                           // a near layer of stars that twinkle (and move a little more)
            if (night < 1f && rnd(i * 3 + 2) > night) continue;
            float u = rnd(i * 13 + 1), v = rnd(i * 29 + 7), tw = (float) Math.max(0, Math.sin(now / (500.0 + rnd(i) * 1300) + i * 2.1));
            float sx = sx0 + (((u * sw - px) % sw) + sw) % sw, sy = sy0 + (((v * sh - py) % sh) + sh) % sh;
            int c = i % 6 == 0 ? 0xFFE6B8 : i % 4 == 0 ? 0xB8D0FF : 0xFFFFFF; float al = (0.25f + 0.75f * tw * tw) * alpha * (0.4f + 0.6f * rnd(i * 5));
            g.fill(Math.round(sx), Math.round(sy), Math.round(sx) + 1, Math.round(sy) + 1, argb(al, c));
            if (tw > 0.93f && i % 4 == 0) glow(sx + 0.5f, sy + 0.5f, 3f, c, al * 0.6f);
        }
    }
    private static void streaks(GuiGraphicsExtractor g) {
        if (warp < 0.02f) return;
        float cx = sx0 + sw / 2f, cy = sy0 + sh / 2f;
        for (int i = 0; i < 110; i++) {
            float an = rnd(i * 7 + 3) * 6.2832f, sp = 0.3f + rnd(i * 11) * 0.7f;
            float ph = (((now % 3_600_000L) / 700f) * sp + rnd(i * 5)) % 1f, r0 = ph * ph * Math.max(sw, sh) * 0.75f, len = (6f + 70f * sp) * warp * ph;
            float x0 = cx + (float) Math.cos(an) * r0, y0 = cy + (float) Math.sin(an) * r0 * 0.7f;
            int col = i % 5 == 0 ? 0xC9A0FF : i % 3 == 0 ? 0x9AD8FF : 0xFFFFFF;
            streak(x0, y0, x0 - (float) Math.cos(an) * len, y0 - (float) Math.sin(an) * len * 0.7f, col, warp * ph * 0.9f);
        }
    }
    /**
     * The deep sky behind the constellations: the Milky Way as a band of star dust across the sky, a spiral galaxy slowly turning in
     * the far corner, a few bright stars with long diffraction spikes, and a comet drifting along its tail. All of it moves with the
     * camera a little (parallax), and everything is points and glows: a few hundred fills a frame.
     */
    private static void deepSky(GuiGraphicsExtractor g) {
        float tt = (now % 3_600_000L) / 1000f;
        float px = (camU - aspect() / 2f) * sh * camS * 0.25f - parX(0.18f), py = (camV - 0.5f) * sh * camS * 0.25f - parY(0.18f);
        // the Milky Way: a soft glowing band from the lower left to the upper right, thick with dust and tiny stars
        float ax = sx0 - sw * 0.05f - px, ay = sy0 + sh * 0.95f - py, bx = sx0 + sw * 1.05f - px, by = sy0 + sh * 0.05f - py;
        for (int i = 0; i < 16; i++) {
            float u = i / 15f, x = ax + (bx - ax) * u, y = ay + (by - ay) * u, w = (float) Math.sin(Math.PI * (0.15f + 0.7f * u));
            NebulaTex.glow(g, x, y, sw * 0.1f, sh * 0.15f, argb(0.13f * w * alpha, i % 3 == 0 ? 0xB89AFF : i % 3 == 1 ? 0x9AB8FF : 0xFFD8C8));
            NebulaTex.glow(g, x, y, sw * 0.05f, sh * 0.06f, argb(0.1f * w * alpha, 0xFFF4F0));
        }
        float nx = -(by - ay), ny = bx - ax, nl = (float) Math.hypot(nx, ny); nx /= nl; ny /= nl;
        for (int i = 0; i < 90; i++) {                                                  // the dust: thick in the middle of the band, thinning out
            float u = rnd(i * 17 + 3), off = (rnd(i * 31 + 5) + rnd(i * 37 + 11) + rnd(i * 41 + 13) - 1.5f) * sh * 0.14f;
            float x = ax + (bx - ax) * u + nx * off, y = ay + (by - ay) * u + ny * off;
            if (x < sx0 || x >= sx0 + sw || y < sy0 || y >= sy0 + sh || night < 1f && rnd(i * 5 + 9) > night) continue;
            float tw = 0.6f + 0.4f * (float) Math.sin(tt * (0.6 + rnd(i) * 1.6) + i);
            int c = i % 7 == 0 ? 0xFFE0C0 : i % 5 == 0 ? 0xC8D8FF : 0xE8E8FF;
            g.fill(Math.round(x), Math.round(y), Math.round(x) + 1, Math.round(y) + 1, argb((0.3f + 0.5f * rnd(i * 3)) * tw * alpha, c));
        }
        for (int i = 0; i < 6; i++) {                                                    // dark dust lanes running along it
            float u = 0.12f + 0.13f * i, x = ax + (bx - ax) * u + nx * sh * 0.015f * (i % 2 == 0 ? 1 : -1), y = ay + (by - ay) * u + ny * sh * 0.015f * (i % 2 == 0 ? 1 : -1);
            NebulaTex.glow(g, x, y, sw * 0.05f, sh * 0.025f, argb(0.22f * alpha, 0x05060F));
        }
        // a spiral galaxy far away, turning very slowly
        float gx = sx0 + sw * 0.6f - px * 0.6f, gy = sy0 + sh * 0.47f - py * 0.6f, gr = Math.min(sw, sh) * 0.075f, rot = tt * 0.03f;
        NebulaTex.glow(g, gx, gy, gr * 1.7f, gr * 0.8f, argb(0.2f * alpha, 0xB89AFF)); NebulaTex.glow(g, gx, gy, gr * 0.9f, gr * 0.45f, argb(0.25f * alpha, 0xFF9AD8));
        NebulaTex.glow(g, gx, gy, gr * 0.35f, gr * 0.2f, argb(0.45f * alpha, 0xFFF0D8));
        for (int arm = 0; arm < 2; arm++) for (int i = 0; i < 24; i++) {
            float k = i / 23f, an = rot + arm * (float) Math.PI + k * 4.2f, r = gr * (0.12f + 0.95f * k);
            float x = gx + (float) Math.cos(an) * r, y = gy + (float) Math.sin(an) * r * 0.42f;
            g.fill(Math.round(x), Math.round(y), Math.round(x) + 1, Math.round(y) + 1, argb((0.75f - 0.5f * k) * alpha, i % 4 == 0 ? 0xFFB8E0 : i % 3 == 0 ? 0x9AD8FF : 0xD8D0FF)); if (i % 6 == 0) glow(x, y, 2.5f, 0xC8B8FF, 0.35f * (1f - k));
        }
        // bright stars with long diffraction spikes, breathing
        float[][] bright = {{0.14f, 0.12f}, {0.47f, 0.08f}, {0.72f, 0.46f}, {0.93f, 0.62f}, {0.33f, 0.4f}};
        int[] bc = {0xBFD8FF, 0xFFE6C0, 0xFFB8A8, 0xC8F0FF, 0xE8D8FF};
        for (int i = 0; i < bright.length; i++) {
            float x = sx0 + sw * bright[i][0] - px * 0.8f, y = sy0 + sh * bright[i][1] - py * 0.8f, b = 0.7f + 0.3f * (float) Math.sin(tt * 1.3 + i * 2.3);
            glow(x, y, 7f * b, bc[i], 0.35f * b);
            NebulaTex.glow(g, x, y, 14f * b, 0.7f, argb(0.6f * b * alpha, bc[i])); NebulaTex.glow(g, x, y, 0.7f, 14f * b, argb(0.6f * b * alpha, bc[i]));
            NebulaTex.glow(g, x, y, 5f * b, 0.5f, argb(0.4f * b * alpha, 0xFFFFFF));
            glow(x, y, 1.4f, 0xFFFFFF, b);
        }
        // a comet, gliding along its tail across the upper sky once every couple of minutes
        float cu = ((now % 150_000L) / 150_000f) * 1.4f - 0.2f;
        if (cu > -0.1f && cu < 1.1f) {
            float cx = sx0 + sw * (0.1f + 0.75f * cu), cy = sy0 + sh * (0.18f + 0.12f * cu), dx = -sw * 0.75f, dy = -sh * 0.12f, dl = (float) Math.hypot(dx, dy);
            for (int i = 0; i < 18; i++) { float k = i / 17f; NebulaTex.glow(g, cx + dx / dl * k * sw * 0.09f, cy + dy / dl * k * sw * 0.09f, 3f + 6f * k, 2f + 3f * k, argb(0.18f * (1f - k) * alpha, i % 2 == 0 ? 0x9AE8FF : 0xD8F4FF)); }
            glow(cx, cy, 4f, 0xD8F8FF, 0.8f); glow(cx, cy, 1.4f, 0xFFFFFF, 1f);
        }
    }

    /** The last of the sunset along the horizon at the start of the opening: soft lights, no bands. */
    private static void twilight(GuiGraphicsExtractor g) {
        float d = dusk * alpha;
        NebulaTex.glow(g, sx0 + sw * 0.62f, sy0 + sh * 1.02f, sw * 0.75f, sh * 0.3f, argb(0.55f * d, 0xE0607A));
        NebulaTex.glow(g, sx0 + sw * 0.72f, sy0 + sh * 1.03f, sw * 0.45f, sh * 0.16f, argb(0.75f * d, 0xFF9A5A));
        NebulaTex.glow(g, sx0 + sw * 0.5f, sy0 + sh * 0.9f, sw * 0.9f, sh * 0.5f, argb(0.25f * d, 0x3A3A8A));
    }
    /** The nearest layer: a few out-of-focus motes drifting close to the lens; they slide the most as you look around. */
    private static void nearDust(GuiGraphicsExtractor g, float a) {
        if (a < 0.02f) return;
        float px = parX(2.2f), py = parY(2.2f), tt = (now % 3_600_000L) / 1000f;
        for (int i = 0; i < 7; i++) {
            float u = (rnd(i * 17 + 4) + tt * (0.004f + 0.004f * rnd(i))) % 1f, v = rnd(i * 23 + 8) + 0.03f * (float) Math.sin(tt * 0.3f + i);
            float x = sx0 + u * sw + px, y = sy0 + v * sh + py, r = sh * (0.025f + 0.03f * rnd(i * 5));
            NebulaTex.glow(g, x, y, r, r, argb(0.06f * a * alpha * (0.6f + 0.4f * (float) Math.sin(tt * 0.7f + i)), i % 3 == 0 ? 0xC8B8FF : 0xBFD8FF));
        }
    }

    /** An aurora: a few soft ribbons of green and violet light waving slowly across the top of the sky. */
    private static void aurora(GuiGraphicsExtractor g, float a) {
        if (a < 0.02f) return;
        float tt = (now % 3_600_000L) / 1000f;
        int[] cols = {0x3AE8B0, 0x6A8CFF, 0xB46AFF};
        for (int b = 0; b < 3; b++) for (int i = 0; i < 8; i++) {
            float u = i / 7f, x = sx0 + sw * (0.08f + 0.84f * u), y = sy0 + sh * (0.1f + 0.05f * b) + (float) Math.sin(u * 5.0 + tt * 0.25 + b * 1.7) * sh * 0.035f;
            float al = (0.06f + 0.04f * (float) Math.sin(tt * 0.6 + i * 0.7 + b)) * (float) Math.sin(Math.PI * u) * a * alpha;
            NebulaTex.glow(g, x, y, sw * 0.05f, sh * (0.05f + 0.02f * b), argb(al, cols[b]));
        }
    }
    private static void shootingStars(GuiGraphicsExtractor g) {
        for (int lane = 0; lane < 2; lane++) {
            long per = 9000L + lane * 4100L, st = (now + lane * 3333L) / per; float u = ((now + lane * 3333L) % per) / 900f; if (u > 1f) continue;
            float x0 = sx0 + sw * (0.25f + 0.6f * rnd((int) st * 3 + lane)), y0 = sy0 + sh * (0.05f + 0.3f * rnd((int) st + 9 + lane));
            float dxs = sw * 0.3f, dys = sh * 0.16f, x = x0 + u * dxs, y = y0 + u * dys;
            streak(x, y, x - dxs * 0.22f, y - dys * 0.22f, 0xCFE4FF, (1 - u * 0.6f));
            glow(x, y, 5, 0xCFE4FF, 0.7f * (1 - u));
        }
    }
    private static void vignette(GuiGraphicsExtractor g) {
        int b = Math.max(1, Math.round(Math.min(sw, sh) * 0.012f));
        for (int i = 0; i < 12; i++) {
            int in = b * i, a = Math.round(20 * (1f - i / 12f) * alpha);
            g.fill(sx0, sy0 + in, sx0 + sw, sy0 + in + b, Gfx.argb(a, 0)); g.fill(sx0, sy0 + sh - in - b, sx0 + sw, sy0 + sh - in, Gfx.argb(a, 0));
            g.fill(sx0 + in, sy0, sx0 + in + b, sy0 + sh, Gfx.argb(a, 0)); g.fill(sx0 + sw - in - b, sy0, sx0 + sw - in, sy0 + sh, Gfx.argb(a, 0));
        }
    }
    private static void frame(GuiGraphicsExtractor g) {
        int c = argb(0.8f * alpha, 0xE8C76A), c2 = argb(0.35f * alpha, 0xE8C76A);
        g.fill(sx0 - 1, sy0 - 1, sx0 + sw + 1, sy0, c); g.fill(sx0 - 1, sy0 + sh, sx0 + sw + 1, sy0 + sh + 1, c); g.fill(sx0 - 1, sy0, sx0, sy0 + sh, c); g.fill(sx0 + sw, sy0, sx0 + sw + 1, sy0 + sh, c);
        g.fill(sx0 + 3, sy0 + 3, sx0 + sw - 3, sy0 + 4, c2); g.fill(sx0 + 3, sy0 + sh - 4, sx0 + sw - 3, sy0 + sh - 3, c2); g.fill(sx0 + 3, sy0 + 3, sx0 + 4, sy0 + sh - 3, c2); g.fill(sx0 + sw - 4, sy0 + 3, sx0 + sw - 3, sy0 + sh - 3, c2);
        for (int i = 1; i < 36; i++) { int x = sx0 + sw * i / 36, l = i % 9 == 0 ? 7 : i % 3 == 0 ? 4 : 2; g.fill(x, sy0, x + 1, sy0 + l, c); g.fill(x, sy0 + sh - l, x + 1, sy0 + sh, c); }
        for (int i = 1; i < 20; i++) { int y = sy0 + sh * i / 20, l = i % 5 == 0 ? 7 : i % 5 == 2 ? 3 : 2; g.fill(sx0, y, sx0 + l, y + 1, c); g.fill(sx0 + sw - l, y, sx0 + sw, y + 1, c); }
        int[][] cn = {{sx0, sy0}, {sx0 + sw, sy0}, {sx0, sy0 + sh}, {sx0 + sw, sy0 + sh}};
        for (int[] p : cn) { disc(g, p[0], p[1], 4, argb(alpha, 0x2A1C08)); ring(p[0], p[1], 4, c, 1f); disc(g, p[0], p[1], 1.5f, c); }
    }

    // ------------------------------------------------------------------ the stargazer: on a hill, bottom left, with a telescope that follows your mouse
    /** The hill's top at screen x (the stargazer, the tripod's feet and the lantern all stand on it). */
    private static float ground(float x) {
        float hillW = sw * 0.36f, i = x - sx0, u = i / hillW; if (u < 0 || u > 1) return sy0 + sh;
        return sy0 + sh - (sh * 0.17f * (1f - u * u) * (0.95f + 0.05f * (float) Math.sin(i * 0.15f)) + 3);
    }
    private static void stargazer(GuiGraphicsExtractor g, float a) { stargazer(g, a, false); }
    /** film: the scope holds still on the sky (the opening), and the lens is remembered for the camera. */
    private static float lensX, lensY, headXs, headYs;
    private static void stargazer(GuiGraphicsExtractor g, float a, boolean film) {
        if (a < 0.02f && !film) return;
        float k = sh / 150f, gnd = sy0 + sh, hillW = sw * 0.36f;
        int sil = argb(a * alpha, 0x05040A);
        NebulaTex.layer(g, "land", landC, sx0, sy0, sw, sh, argb(a * alpha, 0xFFFFFF));          // the hill, its grass and the firs (baked once)
        float hx = sx0 + sw * 0.12f, hy = ground(hx) + 1;
        // a blanket spread on the grass, lit warm by the lantern
        for (int i = 0; i < Math.round(20 * k); i++) { float bx = hx - 9 * k + i, by = ground(bx); int c = (i / Math.max(1, Math.round(3 * k))) % 2 == 0 ? 0x3A1620 : 0x4A2A14;
            g.fill(Math.round(bx), Math.round(by - 1.2f * k), Math.round(bx) + 1, Math.round(by + 0.5f), argb(a * alpha * 0.95f, c)); }
        // the lantern beside them: warm light, a flame that flickers
        float lx = hx - 16 * k, ly = ground(lx) - 2.5f * k, fl = 0.8f + 0.2f * (float) Math.sin(now / 90.0) * (float) Math.sin(now / 37.0);
        NebulaTex.glow(g, lx + 4 * k, ground(lx) + 1.5f * k, 26 * k * fl, 5 * k, argb(0.3f * a * alpha, 0xFFA040));          // a pool of light on the grass
        glow(lx, ly, 22 * k * fl, 0xFFB050, 0.3f * a); glow(lx, ly, 7 * k, 0xFFD890, 0.75f * a * fl);
        for (int i = 0; i < 2; i++) { float mt = now / (700f + i * 230f) + i * 2.1f; float mxp = lx + (float) Math.cos(mt) * (4 + i * 2) * k, myp = ly - 2 * k + (float) Math.sin(mt * 1.7f) * 3 * k;      // two moths circling the lantern
            g.fill(Math.round(mxp), Math.round(myp), Math.round(mxp) + 1, Math.round(myp) + 1, argb(0.8f * a * alpha, 0xFFE8C0)); }
        bar(lx - 1.5f * k, ly - 2.5f * k, lx + 1.5f * k, ly + 2.5f * k, 0x1A120A, a); bar(lx - 0.5f * k, ly - 3.5f * k, lx + 0.5f * k, ly - 2.5f * k, 0x1A120A, a);
        disc(g, lx, ly, 1.2f * k, argb(a * alpha * fl, 0xFFE8A0));
        // a mug of something hot on the blanket, steam curling up off it
        float mgx = hx - 6 * k, mgy = ground(mgx) - 1.2f * k;
        bar(mgx - 1f * k, mgy - 2.2f * k, mgx + 1f * k, mgy, 0x1A120A, a); bar(mgx + 1f * k, mgy - 1.8f * k, mgx + 1.6f * k, mgy - 0.6f * k, 0x1A120A, a);
        for (int i = 0; i < 3; i++) { float q = ((now / 1000f) * 0.45f + i / 3f) % 1f; glow(mgx + (float) Math.sin(q * 6 + i * 2) * 1.2f * k, mgy - 3 * k - q * 7 * k, (1.2f + 1.5f * q) * k, 0xD8D8E8, 0.22f * (1f - q) * a); }
        // a cat curled by the lantern, its tail swishing, its ears twitching now and then
        float cx0 = hx - 11.5f * k, cg = ground(cx0) + 0.5f, tail = (float) Math.sin(now / 600.0) * 2.5f * k, ear = (now / 3000) % 4 == 0 ? 0.4f * k : 0f;
        disc(g, cx0, cg - 2.2f * k, 2.4f * k, sil); disc(g, cx0 + 1.2f * k, cg - 5.2f * k, 1.7f * k, sil);
        thick(cx0 + 0.3f * k, cg - 6.2f * k, cx0 + 0.4f * k - ear, cg - 7.3f * k, 0.8f * k, 0x05040A, a); thick(cx0 + 2f * k, cg - 6.2f * k, cx0 + 2.1f * k, cg - 7.1f * k, 0.8f * k, 0x05040A, a);
        thick(cx0 - 2 * k, cg - 0.8f * k, cx0 - 4.5f * k + tail * 0.4f, cg - 3.5f * k - Math.abs(tail) * 0.3f, 0.7f * k, 0x05040A, a);
        // the telescope on its tripod; the tube turns to the constellation under your mouse, or sweeps slowly
        float apX = hx + 9f * k, apY = Math.min(ground(apX), hy) - 12.5f * k, want;
        for (float fx : new float[]{apX - 4.5f * k, apX + 4.5f * k, apX + 0.6f * k}) line(apX, apY, fx, ground(fx) + 0.5f, 0x05040A, a, 1);
        if (film) want = -1.05f;
        else if (hoverCon >= 0) { float[] o = centre(hoverCon); want = (float) Math.atan2(Y(o[1]) - apY, X(o[0]) - apX); }
        else want = -0.95f + 0.25f * (float) Math.sin(now / 5000.0);
        want = Math.max(-2.6f, Math.min(-0.15f, want));
        scopeAng += (want - scopeAng) * (film ? 0.2f : 0.08f);
        float ca = (float) Math.cos(scopeAng), sa = (float) Math.sin(scopeAng), len = 15 * k;
        float eyeX = apX - ca * len * 0.4f, eyeY = apY - sa * len * 0.4f, tx1 = apX + ca * len * 0.6f, ty1 = apY + sa * len * 0.6f;
        lensX = tx1; lensY = ty1;
        thick(eyeX, eyeY, tx1, ty1, 1.7f * k, 0x0A0812, a); thick(eyeX, eyeY, tx1, ty1, 0.5f * k, 0x3A2A5A, a * 0.6f);
        thick(tx1 - ca * 1.5f * k, ty1 - sa * 1.5f * k, tx1, ty1, 2.4f * k, 0x0A0812, a);
        disc(g, apX, apY, 1.1f * k, argb(a * alpha, 0x1A1424));
        glow(tx1, ty1, 4 * k, 0xBFD8FF, (hoverCon >= 0 || film ? 0.55f : 0.25f) * a);
        // the stargazer: a pack beside them, sitting with knees up; every so often they lean back from the eyepiece to look with their own eyes
        bar(hx - 6.5f * k, hy - 6 * k, hx - 2.2f * k, hy, 0x05040A, a); bar(hx - 6 * k, hy - 6.8f * k, hx - 2.8f * k, hy - 6 * k, 0x05040A, a);
        float lk = (now % 14000L) / 1000f, look = lk > 10.5f && lk < 13.5f ? (float) Math.sin(Math.PI * (lk - 10.5f) / 3f) : 0f; look = look * look * (3 - 2 * look);
        float footX = hx + 9.5f * k, footY = ground(footX) - 0.6f * k;
        thick(hx, hy - 1.5f * k, hx + 1.5f * k - look * 1.2f * k, hy - 11 * k, 5.2f * k, 0x05040A, a);                  // back
        thick(hx + 1.5f * k, hy - 2.5f * k, hx + 7 * k, hy - 7.5f * k, 3.2f * k, 0x05040A, a);                          // thigh
        thick(hx + 7 * k, hy - 7.5f * k, footX, footY, 2.8f * k, 0x05040A, a);                                          // shin
        thick(hx + 1.5f * k - look * k, hy - 10 * k, eyeX, eyeY + 1.5f * k, 2.2f * k, 0x05040A, a);                    // arm on the scope
        float headX = hx + 3 * k - look * 2.6f * k, headY = hy - 14.5f * k - look * 0.6f * k; headXs = headX; headYs = headY;
        disc(g, headX, headY, 3.1f * k, sil);
        // a knitted hat: its turned-up brim, the crown leaning back, a bobble that swings
        float tilt = -0.35f - look * 0.5f, hcx = headX - 0.4f * k, hcy = headY - 1.6f * k;
        thick(hcx - 3.2f * k, hcy + 0.6f * k, hcx + 2.8f * k, hcy + 0.2f * k, 1.6f * k, 0x05040A, a);
        thick(hcx, hcy, hcx + (float) Math.sin(tilt) * 4.2f * k, hcy - (float) Math.cos(tilt) * 4.2f * k, 3.6f * k, 0x05040A, a);
        float bob = (float) Math.sin(now / 520.0) * 0.6f * k, bxp = hcx + (float) Math.sin(tilt) * 5.6f * k + bob, byp = hcy - (float) Math.cos(tilt) * 5.6f * k;
        disc(g, bxp, byp, 1.3f * k, sil);
        // a long scarf: wound at the neck, its end streaming out behind in the wind, in one smooth curve
        float wx = hx + 1 * k, wy = hy - 11.5f * k, px0 = wx, py0 = wy;
        for (int i = 1; i <= 5; i++) { float q = i / 5f, wave = (float) Math.sin(now / 420.0 - q * 3.2) * 1.4f * k * q;
            float px1 = wx - q * 7.5f * k, py1 = wy + q * 1.5f * k + wave; thick(px0, py0, px1, py1, (1.7f - 0.5f * q) * k, 0x05040A, a); px0 = px1; py0 = py1; }
        for (int i = 0; i < (film ? 0 : 4); i++) {                                                   // a few fireflies blinking low over the grass
            float u = (now % 3_600_000L) / 1000f, fx = sx0 + sw * (0.03f + 0.28f * rnd(i * 7)) + (float) Math.sin(u * 0.35f + i) * 6 * k, fy = ground(fx) - (5 + 10 * rnd(i * 3)) * k + (float) Math.sin(u * 0.6f + i * 2) * 2 * k;
            float bl = (float) Math.max(0, Math.sin(u * 1.3f + i * 1.7f)); bl *= bl; glow(fx, fy, 3 * k, 0xD8FF8A, 0.5f * bl * a); g.fill(Math.round(fx), Math.round(fy), Math.round(fx) + 1, Math.round(fy) + 1, argb(bl * a * alpha, 0xF4FFC8));
        }
        if (hoverCon >= 0 && !film) {                                                                // a faint sight line to what they look at
            float[] o = centre(hoverCon); float ox = X(o[0]), oy = Y(o[1]), d = (float) Math.hypot(ox - tx1, oy - ty1);
            float ha = ease((now - hoverAt) / 400f);
            for (int i = 0; i < 30; i++) { float u = (i + ((now / 60) % 3) / 3f) / 30f; if (u * d < 6) continue; float xx = tx1 + (ox - tx1) * u, yy = ty1 + (oy - ty1) * u; G.fill(Math.round(xx), Math.round(yy), Math.round(xx) + 1, Math.round(yy) + 1, argb(0.25f * ha * a * (1f - u) * alpha, ACH_COL[hoverCon])); }
        }
    }
    // ------------------------------------------------------------------ the landscape, baked once into two pictures (at twice the menu's pixels)
    private static Canvas farC, landC; private static int bakedW, bakedH;
    private static float h1(int i) { int h = i * 374761393 + 668265263; h = (h ^ (h >>> 13)) * 1274126177; return ((h ^ (h >>> 16)) & 0xFFFFFF) / 16777216f; }
    private static float n1(float x) { int i = (int) Math.floor(x); float f = x - i; f = f * f * (3 - 2 * f); return h1(i) + (h1(i + 1) - h1(i)) * f; }
    /** A mountain skyline: sharp ridges (ridged noise), big shapes with smaller ones on them. 0..1. */
    private static float ridge(float x, int seed) {
        float s = 0, amp = 0.55f, f = 1f, norm = 0;
        for (int o = 0; o < 4; o++) { float n = 1f - Math.abs(n1(x * f + seed * 17.3f + o * 5.1f) * 2f - 1f); s += amp * n; norm += amp; f *= 2.3f; amp *= 0.42f; }
        return s / norm;
    }
    private static void bake() {
        if (farC != null && bakedW == sw && bakedH == sh || sw < 8 || sh < 8) return;
        bakedW = sw; bakedH = sh; int R = 2, CW = sw * R, CH = sh * R; float k = sh / 150f * R;
        Canvas f = new Canvas(CW, CH);
        // two ranges: the far one pale and blue with distance, its foot lost in haze; the near one darker
        float[][] ranges = {{3, 0.12f, 0.06f}, {9, 0.07f, 0.04f}}; int[][] cols = {{0xFF151838, 0xFF23264E}, {0xFF090A1A, 0xFF12132C}};   // dark silhouettes: it is night
        for (int ri = 0; ri < 2; ri++) {
            float[] rg = ranges[ri]; int seed = (int) rg[0], top = cols[ri][0], foot = cols[ri][1];
            for (int x = 0; x < CW; x++) {
                float u = x / (float) CW, hgt = CH * (rg[1] + rg[2] * (ridge(u * 2.4f, seed) - 0.5f) * 2f);
                float ry = CH - hgt; int y0 = (int) Math.floor(ry); float fr = 1f - (ry - y0);
                for (int y = Math.max(0, y0); y < CH; y++) {
                    float t = (y - ry) / Math.max(1f, hgt); int col = Canvas.mix(top, foot, (float) Math.pow(Math.min(1f, t), 1.4f));
                    f.set(x, y, y == y0 ? (Math.round(255 * fr) << 24) | (col & 0xFFFFFF) : col);
                }
            }
            for (int y = (int) (CH * 0.93f); y < CH; y++) { float t = (y - CH * 0.93f) / (CH * 0.07f); for (int x = 0; x < CW; x++) f.set(x, y, Canvas.rgb(Math.round(45 * t), 0x2A2A58)); }   // a little mist along the foot
        }
        farC = f;
        Canvas l = new Canvas(CW, CH); int dark = 0xFF05040A;
        float lanternX = (sw * 0.12f - 16 * k / R) * R, lightR = 34 * k;
        for (int x = 0; x < CW; x++) {                                                                // the hill
            float gy = (ground(sx0 + x / (float) R) - sy0) * R; if (gy >= CH) continue;
            int y0 = (int) Math.floor(gy); float fr = 1f - (gy - y0);
            float ld = Math.abs(x - lanternX) / lightR, warm = ld < 1f ? (1f - ld) * (1f - ld) : 0f;
            for (int y = Math.max(0, y0); y < CH; y++) {
                int col = dark; if (warm > 0f && y - y0 < 3 * R) col = Canvas.mix(dark, 0xFF6A3A18, warm * 0.75f * (1f - (y - y0) / (3f * R)));
                l.set(x, y, y == y0 ? (Math.round(255 * fr) << 24) | (col & 0xFFFFFF) : col);
            }
            float dens = 0.35f + 0.4f * n1(x * 0.05f);                                                 // grass: clumps of fine blades, each leaning its own way
            if (h1(x * 7 + 3) < dens) {
                float bh = (1.2f + 3.6f * h1(x * 13 + 1)) * k * 0.45f, lean = (h1(x * 5 + 9) - 0.5f) * bh * 0.6f;
                int col = warm > 0f ? Canvas.mix(dark, 0xFF7A4A20, warm * 0.8f) : dark;
                for (int j = 0; j < Math.round(bh); j++) { float q = j / bh; l.set(Math.round(x + lean * q * q), Math.round(gy - j), col); }
            }
        }
        float[][] firs = {{0.035f, 30, 1}, {0.062f, 19, 2}, {0.012f, 23, 3}};                       // firs: a trunk and tiers of ragged boughs
        for (float[] fz : firs) {
            float fx = (sw * fz[0]) * R, base = (ground(sx0 + sw * fz[0]) - sy0) * R + R, th = fz[1] * k; int sd = (int) fz[2];
            for (int y = Math.round(base - 4 * k); y < base; y++) for (int x = Math.round(fx - 0.7f * k); x < fx + 0.7f * k; x++) l.set(x, y, dark);
            int rows = Math.round(th - 3 * k);
            for (int r = 0; r < rows; r++) {
                float h = r / (float) rows, tier = (h * 5f) % 1f;
                float hw = (1f - h) * th * 0.27f * (0.45f + 0.55f * (1f - tier)) + 0.6f + (h1(r * 7 + sd * 31) - 0.5f) * 1.6f * R;
                int y = Math.round(base - 3 * k - r);
                for (int x = Math.round(fx - hw); x <= Math.round(fx + hw); x++) l.set(x, y, dark);
            }
        }
        landC = l;
    }

    // ------------------------------------------------------------------ the sky's furniture: a moon, a ringed planet, far hills with town lights, a satellite
    private static void scenery(GuiGraphicsExtractor g, float a) {
        if (a < 0.02f) return;
        float k = sh / 150f;
        // far planets, deep behind everything (they barely slide as you look around): real spheres, lit from one side, a soft
        // atmosphere on the lit limb; the big one has a tilted ring that passes in front of it and behind it
        float fx = parX(0.12f), fy = parY(0.12f);
        planet(g, sx0 + sw * 0.9f + fx, sy0 + sh * 0.36f + fy, 5.2f * k, 0xD8A070, 0x5A3A2A, 0xFFE0B8, -0.6f, 0.35f, a, true);
        planet(g, sx0 + sw * 0.5f + parX(0.06f), sy0 + sh * 0.09f + parY(0.06f), 3.4f * k, 0xC8CCD8, 0x2A2E3A, 0xF4F6FF, -0.9f, 0f, a, false);   // a pale moon
        planet(g, sx0 + sw * 0.27f + parX(0.09f), sy0 + sh * 0.2f + parY(0.09f), 1.6f * k, 0x6AA8E8, 0x14243A, 0xC8E8FF, -0.5f, 0f, a * 0.85f, false);  // a small blue world
        int base = sy0 + sh;
        NebulaTex.glow(g, sx0 + sw * 0.7f, base, sw * 0.5f, sh * 0.16f, argb(0.22f * a * alpha, 0x6A4A9A));          // a haze of far light along the horizon
        NebulaTex.glow(g, sx0 + sw * 0.86f, base, sw * 0.12f, sh * 0.06f, argb(0.25f * a * alpha, 0xFFB070));         // a distant town's glow
        for (int i = 0; i < 4; i++) {                                                                    // thin clouds drifting slowly across, lit faintly from below
            float tt = (now % 3_600_000L) / 1000f, u = ((rnd(i * 9 + 1) + tt * 0.003f * (1 + i * 0.4f)) % 1.4f) - 0.2f;
            float cx = sx0 + sw * u + parX(0.15f), cy = sy0 + sh * (0.55f + 0.1f * rnd(i * 5 + 2)) + parY(0.15f);
            NebulaTex.glow(g, cx, cy, sw * (0.12f + 0.06f * rnd(i)), sh * 0.018f, argb(0.12f * a * alpha, 0x3A3A6A));
            NebulaTex.glow(g, cx, cy + sh * 0.006f, sw * 0.08f, sh * 0.008f, argb(0.08f * a * alpha, 0x8A7AB8));
        }
        NebulaTex.layer(g, "far", farC, sx0, sy0 + tilt * sh * 0.05f, sw, sh, argb(a * alpha, 0xFFFFFF));   // the mountains (baked once)
        long per = 26000; float su = (now % per) / (float) per;                                           // a satellite crossing now and then, blinking
        if (su < 0.5f) { float sxp = sx0 + sw * (0.1f + su * 1.6f), syp = sy0 + sh * (0.06f + su * 0.1f); boolean on = (now / 500) % 2 == 0;
            g.fill(Math.round(sxp), Math.round(syp), Math.round(sxp) + 1, Math.round(syp) + 1, argb((on ? 0.9f : 0.35f) * a * alpha, on ? 0xFF8A8A : 0xFFFFFF)); }
    }

    /** A shaded sphere: lit colour on the light's side, falling off across a soft terminator into its night side, a rim of
     *  atmosphere on the lit limb. light = the direction the light comes from (radians, 0 = from the right). ring: a tilted ring. */
    private static void planet(GuiGraphicsExtractor g, float cx, float cy, float r, int day, int night, int rim, float light, float bands, float a, boolean ring) {
        if (a < 0.02f || r < 1f) return;
        float lx = (float) Math.cos(light), ly = (float) Math.sin(light);
        glow(cx + lx * r * 0.4f, cy + ly * r * 0.4f, r * 3.2f, day, 0.12f * a);
        if (ring) ringBand(g, cx, cy, r, rim, a, false);
        int ir = (int) Math.ceil(r), step = r > 6 ? 1 : 1;
        for (int dy = -ir; dy <= ir; dy += step) {
            float yy = dy + 0.5f; float hw = (float) Math.sqrt(Math.max(0, r * r - yy * yy)); if (hw < 0.5f) continue;
            int x0 = Math.round(cx - hw), x1 = Math.round(cx + hw), segs = Math.max(2, Math.min(4, Math.round(hw / 2.5f)));
            for (int i = 0; i < segs; i++) {                                                     // across the row: each piece shaded by how squarely it faces the light
                int sa = x0 + (x1 - x0) * i / segs, sb = x0 + (x1 - x0) * (i + 1) / segs; if (sb <= sa) continue;
                float px = ((sa + sb) / 2f - cx) / r, py = yy / r, pz = (float) Math.sqrt(Math.max(0, 1 - px * px - py * py));
                float lam = Math.max(0f, px * lx * 0.95f + py * ly * 0.95f + pz * 0.3f);
                float sh = cl((lam - 0.05f) / 0.75f); sh = sh * sh * (3 - 2 * sh);
                int col = mix(0xFF000000 | night, 0xFF000000 | day, sh);
                if (bands > 0) col = mix(col, 0xFF000000 | mix(day, 0xFF000000, 0.3f), bands * 0.5f * (0.5f + 0.5f * (float) Math.sin(py * 9.0)) * sh);
                if (lam > 0.85f) col = mix(col, 0xFF000000 | rim, (lam - 0.85f) * 2.5f);
                g.fill(sa, Math.round(cy + dy), sb, Math.round(cy + dy) + step, argb(a * alpha, col & 0xFFFFFF));
            }
        }
        for (int i = 0; i < 10; i++) {                                                          // the lit limb's thin atmosphere
            double an = light - 1.1 + i * 0.22; float ex = cx + (float) Math.cos(an) * r, ey = cy + (float) Math.sin(an) * r;
            glow(ex, ey, Math.max(1f, r * 0.35f), rim, 0.16f * a * (float) Math.sin(Math.PI * i / 9.0));
        }
        if (ring) ringBand(g, cx, cy, r, rim, a, true);
    }
    /** The ring of a planet, tilted: the far half (front = false) is drawn before the planet, the near half after it. */
    private static void ringBand(GuiGraphicsExtractor g, float cx, float cy, float r, int rgb, float a, boolean front) {
        float tilt = -0.32f, ry = 0.3f, rr = r * 1.9f; float ct = (float) Math.cos(tilt), st = (float) Math.sin(tilt);
        int n = 18;
        for (int i = 0; i < n; i++) {
            double t0 = Math.PI * 2 * i / n, t1 = Math.PI * 2 * (i + 1) / n; boolean near = Math.sin((t0 + t1) / 2) > 0; if (near != front) continue;
            for (int b = 0; b < 2; b++) {
                float R = rr * (b == 0 ? 1f : 0.82f);
                float ax = (float) Math.cos(t0) * R, ay = (float) Math.sin(t0) * R * ry, bx = (float) Math.cos(t1) * R, by = (float) Math.sin(t1) * R * ry;
                line(cx + ax * ct - ay * st, cy + ax * st + ay * ct, cx + bx * ct - by * st, cy + bx * st + by * ct, rgb, (b == 0 ? 0.55f : 0.35f) * a, -1);
            }
        }
    }

    // ------------------------------------------------------------------ the opening film
    private static void skipIntro() { long n = System.currentTimeMillis(); long d = openAt - n; if (d <= 0) return; openAt = n; if (zoomAt > 0) zoomAt -= d; blinkAt -= d; introSounds = 3; }
    /** u ms into the film: the stargazer close up, then the camera pushes into the telescope's lens until the lens fills the screen. */
    private static void intro(GuiGraphicsExtractor g, long u) {
        int fw = Math.round(Math.min(W - 24, H * 2.0f)), fh = Math.round(Math.min(H - 24, fw / 1.7f));
        sw = fw; sh = fh; sx0 = Math.round(W / 2f - sw / 2f); sy0 = Math.round(H / 2f - sh / 2f);
        float[] cam = camOf(-1); camU = cam[0]; camV = cam[1]; camS = cam[2]; warp = 0; alpha = 1f;
        g.fill(0, 0, W, H, argb(cl(u / 250f) * 0.72f, 0x02010A));
        float fadeIn = cl(u / 350f);
        g.enableScissor(sx0, sy0, sx0 + sw, sy0 + sh);
        sky(g);
        float push = cl((u - 900f) / (INTRO - 900f)), pe = push * push * push;                       // 0..900 ms: the close-up, then the push into the lens
        if (introSounds < 1 && u >= 900) { introSounds = 1; sound("sky_zoom", 1.1f, 0.8f); }
        float k0 = 3.4f + 0.5f * cl(u / 900f), kz = k0 * (float) Math.pow(70f / k0, pe);
        if (u < 40 || lensX == 0) stargazer(g, 0f, true);                                           // (nothing drawn: works out where the lens is; once)
        float fx0 = (headXs + lensX) / 2f, fy0 = (headYs + lensY) / 2f, fx = fx0 + (lensX - fx0) * ease(push * 1.6f), fy = fy0 + (lensY - fy0) * ease(push * 1.6f);
        float tx = sx0 + sw * (0.42f + 0.08f * ease(push)), ty = sy0 + sh * (0.58f - 0.08f * ease(push));
        Gfx.push(g); Gfx.translate(g, tx, ty); Gfx.scale(g, kz); Gfx.translate(g, -fx, -fy);
        stargazer(g, fadeIn, true);
        Gfx.pop(g);
        float lensGlow = cl((push - 0.55f) / 0.45f);                                                 // the lens's light swells to fill the screen
        if (lensGlow > 0f) { float lx = tx + (lensX - fx) * kz, ly = ty + (lensY - fy) * kz; glow(lx, ly, Math.max(sw, sh) * (0.2f + lensGlow), 0xCFE0FF, lensGlow); g.fill(sx0, sy0, sx0 + sw, sy0 + sh, argb(lensGlow * lensGlow * 0.85f, 0xE8F0FF)); }
        if (u < 900) {                                                                                 // a line of words while they look
            float ta = cl(u / 300f) * cl((900 - u) / 250f); String cap = "✦  looking up  ✦"; int cw = host.width(cap);
            text(g, cap, Math.round(sx0 + sw * 0.62f - cw / 2f), Math.round(sy0 + sh * 0.8f), argb(ta * 0.85f, 0xE9D9A8), true);
        }
        g.disableScissor();
        frame(g);
    }
    /** The lens's edge while the page opens out of it: dark outside the circle, a brass rim on it. */
    private static void lensMask(GuiGraphicsExtractor g, float cx, float cy, float r, float a) {
        int c = argb(cl(a * 1.6f), 0x02010A);
        int top = Math.max(sy0, Math.round(cy - r)), bot = Math.min(sy0 + sh, Math.round(cy + r));
        if (top > sy0) g.fill(sx0, sy0, sx0 + sw, top, c); if (bot < sy0 + sh) g.fill(sx0, bot, sx0 + sw, sy0 + sh, c);   // whole bands above and below: one fill each
        for (int y = top; y < bot; y += 3) {
            float dy = y + 1.5f - cy;
            int hw = Math.round((float) Math.sqrt(Math.max(0f, r * r - dy * dy)));
            g.fill(sx0, y, Math.round(cx) - hw, Math.min(bot, y + 3), c); g.fill(Math.round(cx) + hw, y, sx0 + sw, Math.min(bot, y + 3), c);
        }
        ring(cx, cy, r, argb(a, 0xE8C76A), 1f); ring(cx, cy, r + 3, argb(a * 0.5f, 0xB08A3A), 1f);
        glow(cx, cy - r, r * 0.6f, 0xE8F0FF, 0.25f * a);
    }

    // ------------------------------------------------------------------ the skill tree of a constellation
    /** One node of a chapter's tree: an achievement, where it sits (sky units from the chapter centre), the node before it on its branch (-1 = the heart). */
    private record Node(Achievements.Ach a, float du, float dv, int parent, int depth) {}
    private static final Map<String, List<Node>> TREES = new HashMap<>();
    private static Object treesFor;
    // Each group is drawn as its own figure: a faint outline (the picture) with the achievements as the bright stars on it.
    // Coordinates are in units of the figure's radius, y down. KEY lists where the stars go, most important first.
    private static final float[][][] OUTLINE = new float[8][][], KEY = new float[8][][];
    static {
        float[][] star = new float[11][], key0 = new float[16][];
        for (int i = 0; i <= 10; i++) { double an = -Math.PI / 2 + i * Math.PI / 5; float r = i % 2 == 0 ? 1f : 0.42f; star[i] = new float[]{(float) Math.cos(an) * r, (float) Math.sin(an) * r}; }
        for (int i = 0; i < 5; i++) { key0[i] = star[i * 2]; key0[5 + i] = star[i * 2 + 1]; }
        for (int i = 0; i < 5; i++) key0[10 + i] = new float[]{(star[i * 2][0] + star[i * 2 + 1][0]) / 2, (star[i * 2][1] + star[i * 2 + 1][1]) / 2};
        key0[15] = new float[]{(star[3][0] + star[4][0]) / 2, (star[3][1] + star[4][1]) / 2};
        OUTLINE[0] = new float[][]{flat(star)}; KEY[0] = key0;
        OUTLINE[1] = new float[][]{{-1f, -.45f, -.7f, -.1f, -.35f, .05f, 0f, .12f, .35f, .05f, .7f, -.1f, 1f, -.45f},          // the griffin: wings,
                {0f, .12f, 0f, .6f, -.22f, .9f}, {0f, .6f, .22f, .9f}, {0f, .12f, .05f, -.25f, .28f, -.32f}};                       // body and tail, head and beak
        KEY[1] = pts(-1f, -.45f, 1f, -.45f, .05f, -.25f, 0f, .6f, -.7f, -.1f, .7f, -.1f, -.22f, .9f, .22f, .9f, .28f, -.32f, 0f, .12f);
        OUTLINE[2] = new float[][]{{-.9f, -.95f, .55f, .5f}, {.9f, -.95f, -.55f, .5f}, {.25f, .62f, .68f, .2f}, {-.25f, .62f, -.68f, .2f}, {.55f, .5f, .85f, .8f}, {-.55f, .5f, -.85f, .8f}};   // two swords crossed
        KEY[2] = pts(-.9f, -.95f, .9f, -.95f, .85f, .8f, -.85f, .8f, 0f, -.05f, .68f, .2f, -.68f, .2f, .25f, .62f, -.25f, .62f, -.45f, -.5f, .45f, -.5f, .3f, .25f, -.3f, .25f, .7f, .65f);
        OUTLINE[3] = new float[][]{{0f, .95f, 0f, -.7f, 0f, -1f}, {0f, .5f, -.5f, -.55f, -.62f, -.9f}, {0f, .5f, .5f, -.55f, .62f, -.9f}, {-.28f, .25f, .28f, .25f}};   // the sheaf
        KEY[3] = pts(0f, -1f, -.62f, -.9f, .62f, -.9f, 0f, .95f, -.28f, .25f, .28f, .25f, 0f, -.35f, -.5f, -.55f, .5f, -.55f, 0f, .5f);
        OUTLINE[4] = new float[][]{{1f, 0f, .45f, -.42f, -.2f, -.38f, -.6f, 0f, -.2f, .38f, .45f, .42f, 1f, 0f}, {-.6f, 0f, -1f, -.45f, -.86f, 0f, -1f, .45f, -.6f, 0f}};   // the fish
        KEY[4] = pts(1f, 0f, -1f, -.45f, -1f, .45f, .45f, -.42f, .45f, .42f, -.6f, 0f, -.2f, -.38f, -.2f, .38f, .62f, -.1f, -.86f, 0f);
        OUTLINE[5] = new float[][]{{0f, -1f, .3f, -.45f, .55f, .1f, .5f, .5f, .25f, .78f, 0f, .85f, -.25f, .78f, -.5f, .5f, -.55f, .1f, -.3f, -.45f, 0f, -1f}};   // the drop
        KEY[5] = pts(0f, -1f, .5f, .5f, -.5f, .5f, 0f, .85f, .55f, .1f, -.55f, .1f, .3f, -.45f, -.3f, -.45f, .25f, .78f, -.25f, .78f);
        OUTLINE[6] = new float[][]{{-.6f, -1f, .6f, -1f}, {-.6f, 1f, .6f, 1f}, {-.5f, -1f, -.08f, -.05f, -.5f, 1f}, {.5f, -1f, .08f, -.05f, .5f, 1f}, {-.3f, .85f, 0f, .55f, .3f, .85f}};   // the hourglass, sand settling at the bottom
        KEY[6] = pts(-.5f, -1f, .5f, -1f, -.27f, -.5f, .27f, -.5f, 0f, -.05f, -.27f, .5f, .27f, .5f, 0f, .85f, -.5f, 1f, .5f, 1f);
        OUTLINE[7] = new float[][]{{-1f, -.25f, -.55f, -.7f, 0f, -.85f, .55f, -.7f, 1f, -.25f}, {0f, -.85f, 0f, 1f}};          // the pickaxe: its curved head, the handle
        KEY[7] = pts(-1f, -.25f, 1f, -.25f, 0f, -.85f, 0f, 1f, -.55f, -.7f, .55f, -.7f, 0f, -.3f, 0f, .3f, 0f, .65f, -.8f, -.48f);
    }
    private static float[] flat(float[][] p) { float[] o = new float[p.length * 2]; for (int i = 0; i < p.length; i++) { o[i * 2] = p[i][0]; o[i * 2 + 1] = p[i][1]; } return o; }
    private static float[][] pts(float... v) { float[][] o = new float[v.length / 2][]; for (int i = 0; i < o.length; i++) o[i] = new float[]{v[i * 2], v[i * 2 + 1]}; return o; }
    private static final float[] TIER_K = {0.75f, 1.0f, 1.4f};
    private static float figR(int c) { return c == 0 ? 0.16f : 0.12f; }
    /** A point on the figure's outline, t from 0 to 1 along all its strokes (for groups with more stars than key points). */
    private static float[] alongOutline(int c, float t) {
        float tot = 0; for (float[] st : OUTLINE[c]) for (int i = 2; i < st.length; i += 2) tot += Math.hypot(st[i] - st[i - 2], st[i + 1] - st[i - 1]);
        float want = t * tot;
        for (float[] st : OUTLINE[c]) for (int i = 2; i < st.length; i += 2) { float l = (float) Math.hypot(st[i] - st[i - 2], st[i + 1] - st[i - 1]); if (want <= l) { float u = want / l; return new float[]{st[i - 2] + (st[i] - st[i - 2]) * u, st[i - 1] + (st[i + 1] - st[i - 1]) * u}; } want -= l; }
        return new float[]{0, 0};
    }
    /** Every group's stars, scattered the way real stars are (seeded per group, so they never move): an irregular, stretched
     *  cloud with a minimum spacing, joined by its shortest links (a minimum spanning tree). Nothing about it reads as an order:
     *  any star can be lit at any time. parent = the star it is joined to (-1 for the first), depth = its index (a seed). */
    private static List<Node> tree(int c) {
        Achievements.Profile p = Achievements.profile();
        if (treesFor != p) { TREES.clear(); treesFor = p; }
        return TREES.computeIfAbsent(ACH[c], id -> {
            List<Achievements.Ach> l = new ArrayList<>(achOf(id));
            l.sort(Comparator.comparing(Achievements.Ach::id));
            java.util.Random rr = new java.util.Random(0x5EEDL * 31 + c * 7919L + l.size());
            java.util.Collections.shuffle(l, rr);
            int n = l.size(); float R = figR(c);
            double rot = rr.nextDouble() * Math.PI, asp = 0.5 + 0.45 * rr.nextDouble();
            float minD = (float) (R * 1.75 / Math.sqrt(Math.max(1, n)));
            float[][] at = new float[n][];
            for (int i = 0; i < n; i++) {
                float[] best = null; float bestD = -1;
                for (int k = 0; k < 80; k++) {
                    double r = R * Math.pow(rr.nextDouble(), 0.62) * (rr.nextDouble() < 0.18 ? 1.3 : 1.0), an = rr.nextDouble() * Math.PI * 2;
                    double x0 = Math.cos(an) * r, y0 = Math.sin(an) * r * asp;
                    float x = (float) (x0 * Math.cos(rot) - y0 * Math.sin(rot)), y = (float) (x0 * Math.sin(rot) + y0 * Math.cos(rot));
                    float d = c == 0 ? (float) Math.hypot(x, y) * 1.4f : 9f;                     // (Every Drop keeps its middle clear for the heart)
                    for (int j = 0; j < i; j++) d = Math.min(d, (float) Math.hypot(x - at[j][0], y - at[j][1]));
                    if (d > bestD) { bestD = d; best = new float[]{x, y}; }
                    if (d >= minD) break;
                }
                at[i] = best;
            }
            int[] par = new int[n]; boolean[] in = new boolean[n]; float[] dist = new float[n];
            java.util.Arrays.fill(par, -1); java.util.Arrays.fill(dist, Float.MAX_VALUE);
            if (n > 0) dist[0] = 0;
            for (int it = 0; it < n; it++) {                                                     // Prim: grow the tree by its shortest link each time
                int u = -1; for (int i = 0; i < n; i++) if (!in[i] && (u < 0 || dist[i] < dist[u])) u = i;
                in[u] = true;
                for (int v = 0; v < n; v++) if (!in[v]) { float d = (float) Math.hypot(at[u][0] - at[v][0], at[u][1] - at[v][1]); if (d < dist[v]) { dist[v] = d; par[v] = u; } }
            }
            List<Node> out = new ArrayList<>();
            for (int i = 0; i < n; i++) out.add(new Node(l.get(i), at[i][0], at[i][1], par[i], i));
            return out;
        });
    }
    /** How far back each constellation hangs (1 = nearest): far ones are smaller, dimmer and slide less as you look around. */
    private static final float[] DEPTH = {1.0f, 0.72f, 0.9f, 0.64f, 0.82f, 0.7f, 0.95f, 0.78f};
    private static float mpx, mpy; private static long lastPar;
    private static float dusk, night = 1f, milky = 1f, tilt, open0, focusPull, conA = 1f, lineGrow = 1f;   // the opening shot
    /** The sideways slide of a layer at depth d (pixels), from where the mouse is and a slow drift. */
    private static float parX(float d) { return 0f; }                                 // (no mouse parallax: the sky stays where it is)
    private static float parY(float d) { return 0f; }
    private static float pX, pY, dK = 1f;                                                       // the current constellation's slide and depth (size and light)

    private static void constellation(GuiGraphicsExtractor g, int c, float zoomed, int fc) {
        List<Node> t = tree(c); List<Achievements.Ach> l = achOf(ACH[c]);
        boolean other = fc >= 0 && c != fc; float fade = (other ? 1f - 0.8f * zoomed : 1f) * conA;
        float d0 = DEPTH[c], dep = fc == c ? d0 + (1f - d0) * zoomed : d0, still = 1f - zoomed;
        pX = parX(dep) * still; pY = parY(dep) * still; dK = 0.62f + 0.38f * dep;
        float[] o = centre(c); float ox = X(o[0]) + pX, oy = Y(o[1]) + pY;
        float rad = 0.16f * sh * camS;
        boolean hv = fc < 0 && zoomed < 0.05f && Math.hypot(mx - ox, my - oy) < rad;
        if (hv) hoverCon = c;
        float hk = hv ? ease((now - hoverAt) / 350f) : 0f;
        int done = 0; for (Achievements.Ach a : l) if (a.done()) done++;
        boolean full = !l.isEmpty() && done == l.size(), any = done > 0;
        int col = ACH_COL[c]; float frac = l.isEmpty() ? 0f : done / (float) l.size();
        float breathe = 0.85f + 0.15f * (float) Math.sin(now / 1800.0 + c);
        float dl = 0.6f + 0.4f * dep;                                                          // far constellations sit in a little haze
        NebulaTex.glow(g, ox, oy, rad * (1.2f + 0.25f * frac) * dK, rad * (1.0f + 0.2f * frac) * dK, argb((0.06f + 0.16f * frac + (full ? 0.12f : 0f) + 0.1f * hk) * fade * alpha * breathe * dl, col));
        // the links: bright where both ends are lit, a faint thread otherwise (no order: any star can be lit first)
        for (int i = 0; i < t.size(); i++) {
            Node n = t.get(i); if (n.parent() < 0) continue;
            Node q = t.get(n.parent());
            float x1 = X(o[0] + n.du()) + pX, y1 = Y(o[1] + n.dv()) + pY, x0 = X(o[0] + q.du()) + pX, y0 = Y(o[1] + q.dv()) + pY;
            if (lineGrow < 1f) { float gr = cl(lineGrow * 1.6f - i / (float) Math.max(1, t.size()) * 0.6f); if (gr <= 0f) continue; x1 = x0 + (x1 - x0) * gr; y1 = y0 + (y1 - y0) * gr; }
            boolean lit = q.a().done() && n.a().done(), half = q.a().done() || n.a().done();
            if (lit) { line(x0, y0, x1, y1, col, 0.45f * fade * dl, 2); line(x0, y0, x1, y1, mix(col, 0xFFFFFFFF, 0.6f), 0.9f * fade * dl, 0); spark(x0, y0, x1, y1, col, fade * dl, i + c * 9); }
            else line(x0, y0, x1, y1, half ? mix(col, 0xFF8A94B8, 0.5f) : 0xFF8A94B8, ((half ? 0.3f : 0.18f) + 0.15f * hk) * fade * dl, 0);
        }
        if (c == 0) heart(g, c, ox, oy, any, full, fade * dl, zoomed, fc);
        for (Node n : t) nodeOf(g, c, n, false, fade * dl, zoomed, fc);
        if (zoomed < 0.6f && inSky(ox, oy)) {
            float la = (1f - zoomed / 0.6f) * fade * alpha;
            String nm = spaced(ACH_NAME[c].toUpperCase(Locale.ROOT));
            float bot = 0.04f, top = -0.04f; for (Node n : t) { bot = Math.max(bot, n.dv()); top = Math.min(top, n.dv()); }
            boolean above = c == 3 || c == 4;                                          // these figures have their names above them (clear of the hill and of each other)
            int ty = above ? Math.round(Y(o[1] + top) + pY) - 26 : Math.round(Y(o[1] + bot) + pY) + 8, nw = host.width(nm);
            float hx0 = ox;
            int nc = argb(la, hv || full ? mix(col, 0xFFFFFFFF, 0.4f) : mix(col, 0xFFFFFFFF, 0.65f));
            small(g, nm, Math.round(ox - nw * 0.375f), ty + 2, nc);
            float bw0 = Math.max(26, nw * 0.6f), bx0 = ox - bw0 / 2f, by0 = ty + 11;                       // a thin bar: how much of it is lit
            g.fill(Math.round(bx0), Math.round(by0), Math.round(bx0 + bw0), Math.round(by0) + 1, argb(la * 0.35f, 0x8A94B8));
            g.fill(Math.round(bx0), Math.round(by0), Math.round(bx0 + bw0 * frac), Math.round(by0) + 1, argb(la, col));
            String sub = done + " / " + l.size(); int subW = Math.round(host.width(sub) * 0.6f);
            Gfx.push(g); Gfx.translate(g, ox - subW / 2f, by0 + 3); Gfx.scale(g, 0.6f); Gfx.txt(g, font, sub, 0, 0, argb(la * 0.8f, full ? col : 0xA8B2D0), false); Gfx.pop(g);
            if (full) small(g, "✦ COMPLETE ✦", Math.round(ox - host.width("✦ COMPLETE ✦") * 0.375f), ty - 9, argb(la, col));
            if (hk > 0f) { String fg = FIGURE[c]; small(g, fg, Math.round(ox - host.width(fg) * 0.375f), above ? ty - 10 : ty + 21, argb(la * hk, col)); }
            ox = hx0; if (fc < 0) HITS.add(new Object[]{Math.round(ox - rad), Math.round(oy - rad), Math.round(ox + rad), Math.round(oy + rad + 30), (Runnable) () -> fly(c)});
        }
    }
    /** The heart of a constellation: a big star in the chapter's colour (dim until you earn something there), with a ring of rays. */
    private static void heart(GuiGraphicsExtractor g, int c, float x, float y, boolean any, boolean full, float fade, float zoomed, int fc) {
        int col = ACH_COL[c]; float k = 0.8f + 0.4f * cl(camS - 1f), tw = 0.85f + 0.15f * (float) Math.sin(now / 520.0 + c);
        float on = any ? 1f : 0.35f;
        glow(x, y, 16 * k, col, 0.4f * on * fade);
        glow(x, y, 6 * k * tw, mix(col, 0xFFFFFFFF, 0.5f), 0.9f * on * fade);
        NebulaTex.glow(g, x, y, 14 * k * tw, 1f, argb(0.7f * on * fade * alpha, mix(col, 0xFFFFFFFF, 0.6f)));
        NebulaTex.glow(g, x, y, 1f, 14 * k * tw, argb(0.7f * on * fade * alpha, mix(col, 0xFFFFFFFF, 0.6f)));
        ring(x, y, 7 * k, argb(0.55f * fade * alpha, col), 1f);
        if (full) { float sp = (now % 3_600_000L) / 3000f; for (int i = 0; i < 8; i++) { double an = sp + i * Math.PI / 4; line(x + (float) Math.cos(an) * 9 * k, y + (float) Math.sin(an) * 9 * k, x + (float) Math.cos(an) * 12 * k, y + (float) Math.sin(an) * 12 * k, col, 0.8f * fade, 0); } }
        glow(x, y, 2.2f * k, 0xFFFFFF, on * fade);
    }
    /** A dashed bar from one point to the next, its dashes slowly running outward. */
    private static void dashed(float x0, float y0, float x1, float y1, int rgb, float a) {
        float len = (float) Math.hypot(x1 - x0, y1 - y0); if (len < 2) return;
        float off = ((now % 3_600_000L) / 400f) % 1f; int n = Math.max(2, Math.round(len / 5f));
        for (int i = 0; i < n; i++) { float u0 = (i + off) / n, u1 = Math.min(1f, (i + off + 0.5f) / n); if (u0 >= 1f) continue; line(x0 + (x1 - x0) * u0, y0 + (y1 - y0) * u0, x0 + (x1 - x0) * u1, y0 + (y1 - y0) * u1, rgb, a, 0); }
    }
    private static void nodeOf(GuiGraphicsExtractor g, int c, Node n, boolean open, float fade, float zoomed, int fc) {
        Achievements.Ach a = n.a(); float[] o = centre(c); float x = X(o[0] + n.du()) + pX, y = Y(o[1] + n.dv()) + pY;
        if (!inSky(x, y)) return;
        boolean near = fc == c && zoomed > 0.5f;
        float hr = near ? 12 : 6;
        boolean in = Math.abs(mx - x) <= hr && Math.abs(my - y) <= hr && (near || fc < 0) && fade > 0.5f;
        if (in) { float d = (float) Math.hypot(mx - x, my - y); if (d < hoverPickD) { hoverPickD = d; hoverPick = a.id(); } }
        boolean hv = in && a.id().equals(hoverStar);
        float tw = 0.82f + 0.18f * (float) Math.sin(now / 380.0 + n.depth() * 1.7 + c * 0.9);
        int col = ACH_COL[c];
        Long ig = IGNITE.get(a.id()); float igT = ig == null ? 9f : (now - ig) / 1000f;
        boolean lit = a.done() && igT >= 0f;
        if (ig != null && igT >= 0f && igT < 0.05f && igSounded.add(a.id())) sound("sky_star", 1.2f, 0.8f);
        int tier = Math.max(0, Math.min(2, a.tier())), tc = LocalLedger.TIER_COL[tier] & 0xFFFFFF;
        float tk = TIER_K[tier] * dK;
        float prog = (float) Math.min(1.0, a.goal() <= 0 ? 0 : LocalLedger.live(a) / a.goal());
        if (lit) {                                                                           // a lit star: in the group's colour; its tier sets its size and flare (Bronze: a cross, Silver: six rays, Gold: eight and a crown ring)
            float r = (near ? 4.6f : 3.2f) * tw * tk + (hv ? 1.2f : 0f);
            if (igT < 1.4f) { float u = igT / 1.4f; glow(x, y, r * (12f - 7f * u), 0xFFFFFF, 0.9f * (1f - u) * fade); ring(x, y, r * (2f + 24f * ease(u)), argb((1f - u) * 0.9f * fade * alpha, col), 1f); r *= 1f + 1.2f * (1f - u); }
            int core = tier == 0 ? 0xC87038 : tier == 1 ? 0xE8F0FF : 0xFFE24A;
            glow(x, y, r * (tier == 2 ? 7f : 5f), col, (tier == 0 ? 0.3f : 0.45f) * fade);                     // the constellation's light around it
            glow(x, y, r * 2.4f, core, (tier == 0 ? 0.7f : 0.9f) * fade);                                        // the star in its tier's colour
            if (tier == 2) { ring(x, y, r * 2.7f, argb(0.6f * fade * alpha * (0.7f + 0.3f * tw), 0xFFE24A), 1f); glow(x, y, r * 9f, 0xFFD040, 0.25f * fade); }
            float rays = r * (near ? 5f : 4.2f) * (0.85f + 0.15f * tw) * (tier == 0 ? 0.55f : 1f), sp = (now % 3_600_000L) / 4000f * (n.depth() % 2 == 0 ? 1 : -1) + n.depth();
            int nr = tier == 0 ? 2 : tier == 1 ? 2 : 4;                                                          // Bronze: a small cross; Silver: a long cross; Gold: an eight-point star
            for (int k = 0; k < nr; k++) { double an = (tier == 0 ? Math.PI / 4 : sp) + k * Math.PI / nr; float ln = tier == 2 && k % 2 == 1 ? 0.6f : 1f, dx = (float) Math.cos(an) * rays * ln, dy = (float) Math.sin(an) * rays * ln;
                line(x - dx, y - dy, x + dx, y + dy, mix(0xFF000000 | core, 0xFFFFFFFF, tier == 0 ? 0.1f : 0.5f), (tier == 0 ? 0.55f : 0.75f) * fade, 0); }
            glow(x, y, r * (tier == 0 ? 0.9f : 1.2f), tier == 0 ? 0xFFD8B0 : 0xFFFFFF, fade);
        } else {                                                                             // not lit yet: a real, small star; its rim says its tier, and an arc shows how far along you are
            plainStar(x, y, (0.55f + 0.2f * prog) * fade * dK, n.depth() + c * 13, tier == 2 || near);
            if (LocalLedger.WAITING.contains(a.id())) {                                           // earned here, waiting for the server to confirm: a slow breathing ring in its tier's colour
                float br = 0.5f + 0.5f * (float) Math.sin(now / 700.0 + n.depth());
                ring(x, y, (near ? 7.5f : 5f) * dK, argb((0.3f + 0.4f * br) * fade * alpha, tc), 1f); glow(x, y, 6f * tk, tc, 0.25f * br * fade);
            }
            glow(x, y, 3.2f * tk, tc, 0.22f * fade);
            if (prog > 0.001f && (near || hv || prog > 0.25f)) { float rr = (near ? 7f : 4.8f) * dK; ring(x, y, rr, argb(0.2f * fade * alpha, 0xC8D0F0), 1f); ring(x, y, rr, argb(0.85f * fade * alpha, col), prog); }
        }
        if (!a.reward().isEmpty() && (near || hv)) { float bx = x + (near ? 7 : 5), by = y - (near ? 7 : 5); line(bx - 2, by, bx + 2, by, lit ? 0xFFE9B8 : 0x8A94B8, fade, 0); line(bx, by - 2, bx, by + 2, lit ? 0xFFE9B8 : 0x8A94B8, fade, 0); }   // a reward waits here
        if (a.id().equals(blinkId) && now >= blinkAt && now < blinkAt + 4200) {
            float u = ((now - blinkAt) % 1400L) / 1400f, b = (float) Math.sin(Math.PI * u);
            glow(x, y, 14 + 10 * b, col, 0.6f * b * fade); ring(x, y, 6 + 26 * ease(u), argb((1f - u) * fade * alpha, 0xFFFFFF), 1f); ring(x, y, 10 + 30 * ease(u), argb((1f - u) * 0.6f * fade * alpha, col), 1f);
        }
        if (near) {                                                                          // the label sits on the outer side of the node
            String nm = a.secret() && !a.done() ? "???" : a.name();
            float dl = (float) Math.hypot(n.du(), n.dv()), ux = dl < 1e-4f ? 0f : n.du() / dl, uy = dl < 1e-4f ? 1f : n.dv() / dl;
            CosmeticData.Def rw = a.reward().isEmpty() ? null : CosmeticData.byId(a.reward());
            String r = rw == null ? null : "✦ " + rw.name();
            int ca = argb((lit ? 1f : 0.7f) * fade * alpha, lit ? 0xFFFFFF : 0xB8C0DA), cr = argb(0.85f * fade * alpha, lit ? col : 0x8A94B8);
            float nw = host.width(nm) * 0.75f, rwid = r == null ? 0 : host.width(r) * 0.75f, bw = Math.max(nw, rwid), bh = r == null ? 7 : 15;
            // where the label may go, best first: outward from the figure's middle, then the other sides; the first that is free is used
            int[] order = Math.abs(ux) > 0.7f ? (ux > 0 ? new int[]{0, 2, 3, 1} : new int[]{1, 2, 3, 0}) : uy >= 0 ? new int[]{2, 0, 1, 3} : new int[]{3, 0, 1, 2};
            float bx = 0, by = 0; boolean placed = false;
            for (int pass = 0; pass < 2 && !placed; pass++) for (int side : order) {
                bx = side == 0 ? x + 11 : side == 1 ? x - 11 - bw : x - bw / 2f;
                by = side == 2 ? y + 10 : side == 3 ? y - 10 - bh : y - bh / 2f;
                if (pass == 1 || free(bx, by, bw, bh)) { placed = true; break; }
            }
            LABELS.add(new float[]{bx - 2, by - 1, bx + bw + 2, by + bh + 1});
            boolean centre = Math.abs(bx + bw / 2f - x) < 1f;
            small(g, nm, Math.round(centre ? x - nw / 2f : bx < x ? bx + bw - nw : bx), Math.round(by), ca);
            if (r != null) small(g, r, Math.round(centre ? x - rwid / 2f : bx < x ? bx + bw - rwid : bx), Math.round(by + 8), cr);
        }
        if (hv && alpha > 0.6f) {
            if (!lastHoverStar.equals(a.id())) { lastHoverStar = a.id(); if (now - hoverSoundAt > 90) { hoverSoundAt = now; sound("sky_star", 0.85f + 0.3f * rnd(n.depth() + c * 13), 0.45f); } }      // one chime per star, never a burst
            String nm = a.secret() && !a.done() ? "???" : a.name();
            String rew = a.reward().isEmpty() ? "" : Ledger.rewardName(a.reward());
            String how = (a.secret() && !a.done() ? "A secret. Keep finding drops." : a.desc()) + (a.first() && !a.done() ? (LocalLedger.firstDay() ? "  First day only: " + LocalLedger.firstDayLeft() + " left." : "  Missed: your first day is over.") : "") + (rew.isEmpty() ? "" : "  Unlocks the " + rew + ".");
            String extra = (!a.done() && LocalLedger.WAITING.contains(a.id()) ? "Earned: waiting for the server to confirm it" : a.done() ? "Lit " + new java.text.SimpleDateFormat("d MMM yyyy").format(new Date(a.at())) : (isTime(a) ? Ledger.hms(LocalLedger.live(a)) + " / " + Ledger.hms(a.goal()) : Ledger.fmt(a.progress()) + " / " + Ledger.fmt(a.goal())));
            host.tip(nm + "\n" + ACH_NAME[c] + "\nT" + Math.max(0, Math.min(2, a.tier())) + "\n" + (a.done() ? "1" : "0") + "\n" + extra + "\n" + how, a.done() ? col : 0xFF807891);
        }
    }

    // ------------------------------------------------------------------ the chart: the sky is a star map, inked in gold

    // ------------------------------------------------------------------ the cards on the overview: what is nearly done, what was lit last, the legend
    private static void cards(GuiGraphicsExtractor g, float a) {
        Achievements.Profile p = Achievements.profile(); if (p == null || a < 0.02f) return;
        List<Achievements.Ach> next = new ArrayList<>(), recent = new ArrayList<>();
        for (Achievements.Ach q : p.list()) { if (q.done()) recent.add(q); else if (!q.secret() && q.goal() > 0 && !LocalLedger.WAITING.contains(q.id())) next.add(q); }
        next.sort(Comparator.comparingDouble(q -> -(LocalLedger.live(q) / q.goal())));
        recent.sort(Comparator.comparingLong(q -> -q.at()));
        int w = Math.max(110, Math.round(sw * 0.2f)), x = sx0 + sw - w - 10, y = sy0 + 26;
        y = card(g, "NEARLY THERE", next.subList(0, Math.min(3, next.size())), x, y, w, a, true) + 6;
        // (Recently Lit was removed to keep the sky clear; Nearly There stays)
        int lx = sx0 + Math.round(sw * 0.5f) - 120, ly = sy0 + sh - 16;                         // the key: the four tiers, and what a reward looks like
        g.fill(lx - 8, ly - 5, lx + 248, ly + 10, argb(0.5f * a * alpha, 0x060818)); g.fill(lx - 8, ly - 5, lx + 248, ly - 4, argb(0.4f * a * alpha, 0xE8C76A));
        int kx = lx;
        for (int i = 0; i < 3; i++) {
            int tc = LocalLedger.TIER_COL[i] & 0xFFFFFF; glow(kx + 3, ly + 3, 2.2f * TIER_K[i], tc, a); ring(kx + 3, ly + 3, 3.4f * TIER_K[i], argb(0.8f * a * alpha, tc), 1f);
            String tn = LocalLedger.TIER[i]; small(g, tn, kx + 10, ly + 1, argb(0.85f * a * alpha, mix(0xFFD8DCEC, 0xFF000000 | tc, 0.35f))); kx += 14 + Math.round(host.width(tn) * 0.75f) + 8;
        }
        line(kx + 1, ly + 3, kx + 7, ly + 3, 0xFFE9B8, a, 0); line(kx + 4, ly, kx + 4, ly + 6, 0xFFE9B8, a, 0); small(g, "reward", kx + 11, ly + 1, argb(0.8f * a * alpha, 0xD8DCEC));
        small(g, "click a constellation to fly in", lx + 248 - Math.round(host.width("click a constellation to fly in") * 0.75f), ly - 14, argb(0.45f * a * alpha, 0xB8C0DA));
    }
    private static int card(GuiGraphicsExtractor g, String title, List<Achievements.Ach> l, int x, int y, int w, float a, boolean bars) {
        if (l.isEmpty()) return y;
        int h = 14 + l.size() * (bars ? 18 : 12);
        g.fill(x, y, x + w, y + h, argb(0.55f * a * alpha, 0x060818)); g.fill(x, y, x + 2, y + h, argb(0.8f * a * alpha, 0xE8C76A));
        small(g, title, x + 7, y + 4, argb(0.9f * a * alpha, 0xE8C76A));
        int yy = y + 14;
        for (Achievements.Ach q : l) {
            int ci = groupOf(q.ch()), col = ACH_COL[ci];
            boolean hv = mx >= x && mx < x + w && my >= yy - 1 && my < yy + (bars ? 17 : 11);
            if (hv) g.fill(x + 2, yy - 1, x + w, yy + (bars ? 17 : 11), argb(0.12f * a * alpha, 0xFFFFFF));
            glow(x + 10, yy + 3, 2.4f, col, a);
            small(g, q.name(), x + 16, yy, argb(a * alpha, 0xFFFFFF));
            if (bars) {
                float f = (float) Math.min(1.0, LocalLedger.live(q) / q.goal()); int bw = w - 26;
                g.fill(x + 16, yy + 10, x + 16 + bw, yy + 12, argb(0.25f * a * alpha, 0x9AA6D0)); g.fill(x + 16, yy + 10, x + 16 + Math.round(bw * f), yy + 12, argb(a * alpha, col));
                String pr = isTime(q) ? Ledger.hms(LocalLedger.live(q)) + "/" + Ledger.hm(q.goal()) : Ledger.fmt(q.progress()) + "/" + Ledger.fmt(q.goal()); small(g, pr, x + w - 6 - Math.round(host.width(pr) * 0.75f), yy, argb(0.7f * a * alpha, 0xB8C0DA));
            }
            String id = q.id(); int c = ci;
            HITS.add(new Object[]{x, yy - 1, x + w, yy + (bars ? 17 : 11), (Runnable) () -> { blinkId = id; blinkAt = System.currentTimeMillis() + ZOOM; fly(c); }});
            yy += bars ? 18 : 12;
        }
        return y + h;
    }

    /** A star not yet earned: a real, small, cool-white star that twinkles. */
    private static void plainStar(float x, float y, float a, int seed, boolean big) {
        float tw = 0.7f + 0.3f * (float) Math.sin(now / (600.0 + rnd(seed) * 700) + seed);
        glow(x, y, big ? 7f : 5f, 0x9AB4FF, 0.35f * a * tw);
        if (big) { NebulaTex.glow(G, x, y, 5f, 0.6f, argb(0.5f * a * tw * alpha, 0xD8E2FF)); NebulaTex.glow(G, x, y, 0.6f, 5f, argb(0.5f * a * tw * alpha, 0xD8E2FF)); }
        glow(x, y, big ? 1.8f : 1.3f, 0xF4F6FF, a * tw);
    }
    private static void spark(float x0, float y0, float x1, float y1, int col, float fade, int seed) {
        float per = 2.6f + rnd(seed) * 1.6f, u = ((now / 1000f + rnd(seed * 3) * per) % per) / 1.1f; if (u > 1f) return;
        glow(x0 + (x1 - x0) * u, y0 + (y1 - y0) * u, 4f, mix(col, 0xFFFFFFFF, 0.6f), 0.9f * fade * (float) Math.sin(Math.PI * u));
    }
    private static String spaced(String s) { StringBuilder b = new StringBuilder(); for (int i = 0; i < s.length(); i++) { if (i > 0) b.append(' '); b.append(s.charAt(i)); } return b.toString().replace("   ", "  "); }

    // ------------------------------------------------------------------ the panel of the constellation you flew into
    private static void panel(GuiGraphicsExtractor g, int c, float a) {
        int px0 = sx0 + 16, py0 = sy0 + 30, pw = Math.round(sw * 0.3f), ph = sh - 46;
        Gfx.push(g); Gfx.translate(g, (1f - ease(a)) * -20f, 0);
        g.fill(px0, py0, px0 + pw, py0 + ph, argb(0.62f * a * alpha, 0x060818));
        g.fill(px0, py0, px0 + pw, py0 + 1, argb(0.5f * a * alpha, ACH_COL[c])); g.fill(px0, py0, px0 + 2, py0 + ph, argb(0.8f * a * alpha, ACH_COL[c]));
        int x = px0 + 12, y = py0 + 12, w = pw - 24;
        small(g, spaced(FIGURE[c].toUpperCase(Locale.ROOT)), x, y, argb(0.9f * a * alpha, ACH_COL[c]));
        Gfx.push(g); Gfx.translate(g, x, y + 9); Gfx.scale(g, 1.6f); Gfx.txt(g, font, ACH_NAME[c], 0, 0, argb(a * alpha, 0xFFFFFF), true); Gfx.pop(g);
        List<Achievements.Ach> l = achOf(ACH[c]); int d = 0; for (Achievements.Ach q : l) if (q.done()) d++;
        int sy = y + 30;
        text(g, d + " of " + l.size() + " stars lit", x, sy, argb(a * alpha, 0xD8DCEC), false);
        int by = sy + 13, seg = Math.max(1, l.size()), gap = 2, sgw = (w - gap * (seg - 1)) / seg;
        for (int i = 0; i < seg; i++) { boolean on = i < l.size() && l.get(i).done(); int sx = x + i * (sgw + gap); g.fill(sx, by, sx + sgw, by + 4, argb((on ? 1f : 0.22f) * a * alpha, on ? ACH_COL[c] : 0x9AA6D0)); }
        Achievements.Profile p = Achievements.profile(); sy = by + 14;
        if (p != null) for (String[] row : Ledger.statRows(c == 0 ? "ledger" : ACH[c], p)) {
            if (sy > py0 + ph - 30) break;
            text(g, row[0], x, sy, argb(0.8f * a * alpha, 0xB8C0DA), false); int vw = host.width(row[1]); text(g, row[1], x + w - vw, sy, argb(a * alpha, 0xFFFFFF), false);
            for (int dx = x + host.width(row[0]) + 3; dx < x + w - vw - 3; dx += 3) g.fill(dx, sy + 6, dx + 1, sy + 7, argb(0.25f * a * alpha, 0x8A94B8));
            sy += 11;
        }
        small(g, "“" + QUOTE[c] + "”", x, py0 + ph - 14, argb(0.75f * a * alpha, ACH_COL[c]));
        Gfx.pop(g);
    }

    // ------------------------------------------------------------------ the header
    private static void header(GuiGraphicsExtractor g, float zoomed, float in, int fc) {
        Achievements.Profile p = Achievements.profile();
        int all = 0, lit = 0; if (p != null) for (Achievements.Ach a : p.list()) { all++; if (a.done()) lit++; }
        float ia = in * alpha;
        int x = sx0 + 14, y = sy0 + 11;
        String ttl = spaced("ACHIEVEMENTS");
        text(g, ttl, x, y, argb(ia, 0xFFE9B8), true);
        String cnt = p == null ? (Achievements.status.isEmpty() ? "Reading the stars..." : Achievements.status) : lit + " of " + all + " stars lit";
        if (p != null && !LocalLedger.WAITING.isEmpty()) cnt += "  \u00B7  " + LocalLedger.WAITING.size() + " waiting for the server";
        if (p != null && Achievements.unsent > 0) cnt += "  \u00B7  " + Achievements.unsent + " drop" + (Achievements.unsent == 1 ? "" : "s") + " waiting to be counted";
        small(g, cnt, x + host.width(ttl) + 10, y + 1, argb(0.8f * ia, 0xB8C0DA));
        if (p != null && all > 0) { int bw = host.width(ttl); g.fill(x, y + 11, x + bw, y + 12, argb(0.2f * ia, 0xFFE9B8)); g.fill(x, y + 11, x + Math.round(bw * lit / (float) all), y + 12, argb(ia, 0xFFE9B8)); }
        if (false && zoomed < 0.3f && p != null) { String hint = ""; small(g, hint, sx0 + sw - 34 - Math.round(host.width(hint) * 0.75f), sy0 + 12, argb(0.55f * ia * (1f - zoomed / 0.3f), 0xB8C0DA)); }
        compass(g, sx0 + sw - 30, sy0 + sh - 30, 16, ia);
        float bcx = sx0 + sw - 16.5f, bcy = sy0 + 14.5f;                                       // the close button: a ring with an X through its exact middle
        boolean hc = Math.hypot(mx - bcx, my - bcy) < 9;
        glow(bcx, bcy, 9, 0xFFFFFF, (hc ? 0.35f : 0.12f) * ia);
        ring(bcx, bcy, 7.5f, argb((hc ? 0.9f : 0.5f) * ia, 0xE8C76A), 1f);
        for (int d = 0; d < 2; d++) { float s0 = d == 0 ? 1f : -1f; float x0 = bcx - 3.2f, y0 = bcy - 3.2f * s0, x1 = bcx + 3.2f, y1 = bcy + 3.2f * s0;
            int L = Math.round((float) Math.hypot(x1 - x0, y1 - y0)); Gfx.push(g); Gfx.translate(g, bcx, bcy); Gfx.rotate(g, (float) Math.atan2(y1 - y0, x1 - x0)); Gfx.translate(g, -L / 2f, -0.5f); g.fill(0, 0, L, 1, argb(ia, 0xFFFFFF)); Gfx.pop(g); }
        int cx = Math.round(bcx) - 8, cy = Math.round(bcy) - 8;
        HITS.add(new Object[]{cx, cy, cx + 17, cy + 17, (Runnable) Codex::close});
        if (fc >= 0) {
            String b = "‹  All constellations"; int bw = Math.round(host.width(b) * 0.75f) + 12, bx = sx0 + sw - 34 - bw, by = sy0 + 8;
            boolean hb = mx >= bx && mx < bx + bw && my >= by && my < by + 13;
            g.fill(bx, by, bx + bw, by + 13, argb((hb ? 0.32f : 0.14f) * ia, 0xFFFFFF)); g.fill(bx, by + 12, bx + bw, by + 13, argb(0.6f * ia, ACH_COL[fc]));
            small(g, b, bx + 6, by + 3, argb(ia, 0xFFFFFF));
            HITS.add(new Object[]{bx, by, bx + bw, by + 13, (Runnable) () -> fly(-1)});
        }
    }
    private static void compass(GuiGraphicsExtractor g, int cx, int cy, int r, float a) {
        ring(cx, cy, r, argb(0.45f * a, 0xE8C76A), 1f); ring(cx, cy, r - 3, argb(0.2f * a, 0xE8C76A), 1f);
        double rot = now / 20000.0;
        for (int k = 0; k < 8; k++) { double an = rot + k * Math.PI / 4; float len = k % 2 == 0 ? r - 1 : r * 0.55f; line(cx, cy, cx + (float) Math.cos(an) * len, cy + (float) Math.sin(an) * len, k == 0 ? 0xFFE9B8 : 0xE8C76A, (k == 0 ? 0.9f : 0.45f) * a / Math.max(0.01f, alpha), 0); }
        disc(g, cx, cy, 1.5f, argb(a, 0xFFE9B8));
    }

    // ------------------------------------------------------------------ drawing helpers: a soft light, rotated bars
    private static void text(GuiGraphicsExtractor g, String s, int x, int y, int c, boolean sh) { Gfx.txt(g, font, s, x, y, c, sh); }
    private static void small(GuiGraphicsExtractor g, String s, int x, int y, int c) { Gfx.push(g); Gfx.translate(g, x, y); Gfx.scale(g, 0.75f); Gfx.txt(g, font, s, 0, 0, c, false); Gfx.pop(g); }
    private static void disc(GuiGraphicsExtractor g, float cx, float cy, float r, int c) {
        if (r < 0.6f) { g.fill(Math.round(cx), Math.round(cy), Math.round(cx) + 1, Math.round(cy) + 1, c); return; }
        int ir = (int) Math.ceil(r);
        for (int dy = -ir; dy <= ir; dy++) { int hw = (int) Math.round(Math.sqrt(Math.max(0, r * r - dy * dy))); if (hw > 0) g.fill(Math.round(cx) - hw, Math.round(cy) + dy, Math.round(cx) + hw, Math.round(cy) + dy + 1, c); }
    }
    /** A soft round light of radius r. */
    private static void glow(float cx, float cy, float r, int rgb, float a) { float d = 1f + 3.5f * focusPull; NebulaTex.glow(G, cx, cy, r * d, r * d, argb(a * alpha / (d * 0.8f + 0.2f), rgb)); }
    /** A bar from (x0, y0) to (x1, y1), one draw however long. width 0 = a hairline, 1 = a 3-pixel soft band, 2 = a wide soft glow. */
    private static void line(float x0, float y0, float x1, float y1, int rgb, float a, int width) {
        float len = (float) Math.hypot(x1 - x0, y1 - y0); if (len < (width < 0 ? 0.3f : 1f) || a < 0.01f) return;
        float trim = width < 0 ? 0f : Math.min(4f, len * 0.2f);                    // stop short of the stars at both ends (not for width -1: a plain segment)
        Gfx.push(G); Gfx.translate(G, x0, y0); Gfx.rotate(G, (float) Math.atan2(y1 - y0, x1 - x0));
        int L = Math.max(Math.round(trim) + 1, Math.round(len - trim));
        if (width == 2) NebulaTex.glow(G, len / 2f, 0, len / 2f, 3.5f, argb(a * alpha, rgb));
        else {
            G.fill(Math.round(trim), 0, L, 1, argb(a * alpha, rgb));
            if (width == 1) { G.fill(Math.round(trim), -1, L, 0, argb(a * 0.45f * alpha, rgb)); G.fill(Math.round(trim), 1, L, 2, argb(a * 0.45f * alpha, rgb)); }
        }
        Gfx.pop(G);
    }
    /** A thick bar (the stargazer's limbs and telescope), rounded off with a disc at each end. */
    private static void thick(float x0, float y0, float x1, float y1, float w, int rgb, float a) {
        float len = (float) Math.hypot(x1 - x0, y1 - y0); int c = argb(a * alpha, rgb);
        Gfx.push(G); Gfx.translate(G, x0, y0); Gfx.rotate(G, (float) Math.atan2(y1 - y0, x1 - x0)); Gfx.translate(G, 0, -w / 2f); Gfx.scale(G, 1f);
        G.pose().scale(1f, w); G.fill(0, 0, Math.max(1, Math.round(len)), 1, c); Gfx.pop(G);
        disc(G, x0, y0, w / 2f, c); disc(G, x1, y1, w / 2f, c);
    }
    private static void bar(float x0, float y0, float x1, float y1, int rgb, float a) { G.fill(Math.round(x0), Math.round(y0), Math.round(x1), Math.round(y1), argb(a * alpha, rgb)); }
    /** A streak that fades from its head to its tail (three pieces). */
    private static void streak(float hx, float hy, float tx, float ty, int rgb, float a) {
        for (int i = 0; i < 3; i++) { float u0 = i / 3f, u1 = (i + 1) / 3f; line(hx + (tx - hx) * u0, hy + (ty - hy) * u0, hx + (tx - hx) * u1, hy + (ty - hy) * u1, rgb, a * (1f - u0), 0); }
    }
    /** A ring (or the start of one, up to `upto` of the way round) made of short rotated bars. */
    private static void ring(float cx, float cy, float r, int c, float upto) {
        int n = Math.max(8, Math.min(32, Math.round(r * 0.7f)));
        float a = (c >>> 24) / 255f / Math.max(0.01f, alpha); int rgb = c & 0xFFFFFF;
        for (int i = 0; i < n; i++) {
            float u0 = i / (float) n, u1 = (i + 1) / (float) n; if (u0 >= upto) break; u1 = Math.min(u1, upto);
            double a0 = -Math.PI / 2 + u0 * Math.PI * 2, a1 = -Math.PI / 2 + u1 * Math.PI * 2;
            float x0 = cx + (float) Math.cos(a0) * r, y0 = cy + (float) Math.sin(a0) * r, x1 = cx + (float) Math.cos(a1) * r, y1 = cy + (float) Math.sin(a1) * r;
            float len = (float) Math.hypot(x1 - x0, y1 - y0);
            Gfx.push(G); Gfx.translate(G, x0, y0); Gfx.rotate(G, (float) Math.atan2(y1 - y0, x1 - x0)); G.fill(0, 0, Math.max(1, Math.round(len + 0.5f)), 1, argb(a * alpha, rgb)); Gfx.pop(G);
        }
    }

    // ------------------------------------------------------------------ input
    static boolean click(double x, double y) { return click(x, y, 0); }
    static boolean click(double x, double y, int button) {
        if (!open) return false;
        if (closeAt != 0) return true;
        if (System.currentTimeMillis() < openAt) { skipIntro(); return true; }
        if (button == 1) { if (focus >= 0) fly(-1); else close(); return true; }
        if (button != 0) return true;
        for (int i = HITS.size() - 1; i >= 0; i--) { Object[] h = HITS.get(i); if (x >= (int) h[0] && x < (int) h[2] && y >= (int) h[1] && y < (int) h[3]) { ((Runnable) h[4]).run(); return true; } }
        boolean onSky = x >= sx0 && x < sx0 + sw && y >= sy0 && y < sy0 + sh;
        if (!onSky) close();
        return true;
    }
    static boolean scroll(double dy) {
        if (!open) return false;
        if (dy != 0 && focus >= 0) fly(Math.floorMod(focus + (dy < 0 ? 1 : -1), ACH.length));
        return true;
    }
    static boolean key(int key) {
        if (!open) return false;
        if (System.currentTimeMillis() < openAt) { skipIntro(); return true; }
        if (key == 256) { if (focus >= 0) fly(-1); else close(); }
        else if (key == 262) fly(focus < 0 ? 0 : Math.floorMod(focus + 1, ACH.length));
        else if (key == 263) fly(focus < 0 ? ACH.length - 1 : Math.floorMod(focus - 1, ACH.length));
        return true;
    }
}
