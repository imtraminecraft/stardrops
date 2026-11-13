package dev.stardrops;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.Minecraft;

import java.nio.ByteBuffer;

/**
 * Standing rule (author): no update may ship with the sky flicker, and the author must never have to report it again.
 *
 * So Stardrops watches for it itself, every frame, while you play. It reads two thin strips of the finished picture where the sky
 * usually is (a copy on the graphics card, read back a frame or two later: no stall) and looks for the flicker's fingerprint: the
 * brightness jumping up, down, up, down on frame after frame. Moving the camera, lightning or a drop flash never do that.
 *
 * When it sees it, it finds the cause on its own:
 *   1. it pauses every part of Stardrops that touches the world or the HUD (the same groups as /sd flicker) for a moment;
 *   2. flicker gone: it turns the parts back on one at a time; the part that brings it back stays off for this session, and chat says
 *      which one (so it can be fixed for good);
 *   3. flicker still there with all of Stardrops paused: Stardrops is not the cause. Everything comes back on and chat says so once,
 *      naming the usual suspects (mods that change the time or the sky).
 * Nothing is saved: a restart starts clean, and finds it again within seconds if it is still there.
 */
final class FlickerGuard {
    private FlickerGuard() {}

    private static final int RING = 6, STRIPS = 2;
    private static final GpuBuffer[] BUF = new GpuBuffer[RING];
    private static final boolean[] BUSY = new boolean[RING];
    private static int slot, bufLen, stripW;
    private static boolean broken;

    // the last brightness values read back, oldest first (a little ring)
    private static final float[] LUM = new float[16]; private static int lumN, lumAt;

    private static final int WATCH = 0, PAUSED = 1, BISECT = 2, COOL = 3;
    private static int state = WATCH, bisectAt; private static long stateAt, coolUntil;
    private static int userOff;                                  // what /sd flicker had switched off before we stepped in
    private static int foundOff;                                 // parts this session keeps off because they caused it
    private static boolean toldNotUs;
    private static final int[] PARTS = {Flicker.SKY, Flicker.AURAS, Flicker.NAMES, Flicker.HUD, Flicker.SLIDE, Flicker.TICKS};
    private static final String[] PART_NAMES = {"the night sky box (Visuals > Cosmos)", "auras", "name tags, tab list and chat effects", "the Stardrops HUD", "the hotbar slide", "background work"};
    static final int ALL = Flicker.AURAS | Flicker.NAMES | Flicker.HUD | Flicker.SLIDE | Flicker.TICKS | Flicker.SKY;

    /** Every frame, before the world is drawn: the screen still holds the last finished frame. */
    static void frame() {
        if (broken) return;
        try {
            Minecraft mc = Minecraft.getInstance();
            long now = System.currentTimeMillis();
            boolean quiet = mc.level == null || mc.gui.screen() != null || AnimationManager.isPlaying() || CosmosHud.momentPlaying() || !StardropsConfig.get().flickerGuard;
            if (quiet) { lumN = 0; if (state == PAUSED || state == BISECT) stateAt = now; return; }        // a menu or a drop's flash on screen: not a fair look; a test in progress just waits
            try { clock(mc.level.getOverworldClockTime()); } catch (Throwable ignored) { }
            step(now);
            sample(mc);
        } catch (Throwable t) { broken = true; Flicker.off = userOff | foundOff; System.err.println("[Stardrops] flicker guard off: " + t); }
    }

    private static void sample(Minecraft mc) {
        RenderTarget rt = mc.gameRenderer.mainRenderTarget(); GpuTexture tex = rt.getColorTexture(); if (tex == null) return;
        int w = rt.width, h = rt.height; if (w < 64 || h < 64) return;
        int sw = Math.max(16, w * 2 / 5); int len = sw * 4 * STRIPS;
        if (len != bufLen) { for (int i = 0; i < RING; i++) { if (BUF[i] != null && !BUSY[i]) BUF[i].close(); BUF[i] = null; BUSY[i] = false; } bufLen = len; stripW = sw; }
        int i = slot; if (BUSY[i]) return; slot = (slot + 1) % RING;
        if (BUF[i] == null) BUF[i] = RenderSystem.getDevice().createBuffer(() -> "stardrops flicker guard", GpuBuffer.USAGE_MAP_READ | GpuBuffer.USAGE_COPY_DST, len);
        final GpuBuffer b = BUF[i]; final int fi = i, fsw = sw; BUSY[i] = true;
        int x0 = (w - sw) / 2, y1 = (int) (h * 0.62f), y2 = (int) (h * 0.80f);                     // texture rows count from the bottom: 38% and 20% down the screen, where the sky is
        var enc = RenderSystem.getDevice().createCommandEncoder();
        enc.copyTextureToBuffer(tex, b, 0L, () -> { }, 0, x0, y1, sw, 1);
        enc.copyTextureToBuffer(tex, b, (long) sw * 4, () -> read(b, fi, fsw), 0, x0, y2, sw, 1);
    }

    private static void read(GpuBuffer b, int i, int sw) {
        try {
            if (b.isClosed()) return;
            try (GpuBufferSlice.MappedView v = b.map(true, false)) {
                ByteBuffer d = v.data(); long sum = 0; int n = 0;
                for (int k = 0; k < sw * STRIPS; k += 3) { int p = d.getInt(k * 4); int r = p & 0xFF, g = (p >>> 8) & 0xFF, bl = (p >>> 16) & 0xFF; sum += 2 * r + 5 * g + bl; n++; }
                push(n == 0 ? 0 : sum / (8f * n));
            }
        } catch (Throwable t) { broken = true; System.err.println("[Stardrops] flicker guard off: " + t); }
        finally { BUSY[i] = false; }
    }

    private static void push(float l) { LUM[lumAt] = l; lumAt = (lumAt + 1) % LUM.length; if (lumN < LUM.length) lumN++; diag(l); }

    // A diary for the author's log: one line a second while the picture or the clock jumps, so a flicker the guard did not catch can be read afterwards.
    private static long secAt, beatAt = -1, total; private static int secN, secTurns, lines; private static float secMin = 1e9f, secMax = -1, secSum, secPrevD, secPrevL = -1;
    private static long clkMin = Long.MAX_VALUE, clkMax = Long.MIN_VALUE;
    static void clock(long t) { clkMin = Math.min(clkMin, t); clkMax = Math.max(clkMax, t); }
    private static void diag(float l) {
        total++; long now = System.currentTimeMillis();
        if (secPrevL >= 0) { float d = l - secPrevL; if (Math.abs(d) >= 4f) { if (secPrevD != 0 && Math.signum(d) != Math.signum(secPrevD)) secTurns++; secPrevD = d; } }
        secPrevL = l; secN++; secSum += l; secMin = Math.min(secMin, l); secMax = Math.max(secMax, l);
        if (beatAt < 0) beatAt = now + 20_000L;
        if (beatAt > 0 && now >= beatAt) { beatAt = 0; System.err.println("[Stardrops] flicker guard watching: " + total + " frames read, sky brightness now " + Math.round(l)); }
        if (secAt == 0) secAt = now;
        if (now - secAt >= 1000) {
            boolean jumpy = secMax - secMin >= 10 || secTurns >= 4 || clkMax - clkMin > 40 && clkMax != Long.MIN_VALUE;
            if (jumpy && lines < 900) { lines++;
                System.err.println("[Stardrops] flicker diary: frames=" + secN + " bright=" + Math.round(secMin) + ".." + Math.round(secMax) + " avg=" + Math.round(secSum / Math.max(1, secN)) + " turns=" + secTurns
                    + " clock=" + (clkMax == Long.MIN_VALUE ? "?" : clkMin + ".." + clkMax) + " state=" + state + " off=" + Flicker.off + " sky=" + SkyBox.diag()); }
            secAt = now; secN = 0; secTurns = 0; secMin = 1e9f; secMax = -1; secSum = 0; clkMin = Long.MAX_VALUE; clkMax = Long.MIN_VALUE;
        }
    }

    /** The fingerprint: of the last 12 frames, at least 8 steps of 12+ (of 255) that keep turning round (up, down, up...). */
    static boolean flickering() {
        if (lumN < 13) return false;
        int turns = 0; float prev = 0;
        for (int k = 0; k < 12; k++) {
            float a = LUM[(lumAt - 13 + k + LUM.length * 2) % LUM.length], b = LUM[(lumAt - 12 + k + LUM.length * 2) % LUM.length], d = b - a;
            if (Math.abs(d) >= 12f && prev != 0 && Math.signum(d) != Math.signum(prev)) turns++;
            if (Math.abs(d) >= 12f) prev = d;
        }
        return turns >= 8;
    }

    private static void step(long now) {
        switch (state) {
            case WATCH -> {
                if (!flickering()) return;
                userOff = Flicker.off; Flicker.off = ALL; lumN = 0; state = PAUSED; stateAt = now;
                System.err.println("[Stardrops] flicker guard: the sky is flickering; pausing Stardrops to find out why");
            }
            case PAUSED -> {
                if (now - stateAt < 1500) return;
                if (flickering()) {                                                                 // still there with all of Stardrops paused: not ours
                    Flicker.off = userOff | foundOff; state = COOL; coolUntil = now + 10 * 60_000L; lumN = 0;
                    System.err.println("[Stardrops] flicker guard: it goes on with Stardrops paused, so Stardrops is not the cause");
                    if (!toldNotUs) { toldNotUs = true; say("The sky is flickering, but not because of Stardrops: it kept flickering with all of Stardrops paused. Something else is changing the time or the sky on every frame (a time changer, Celestial, Nuit, Skyboxify or the server). Stardrops' own stars ignore it."); }
                } else { bisectAt = 0; Flicker.off = ALL & ~PARTS[0] | userOff | foundOff; lumN = 0; state = BISECT; stateAt = now; }
            }
            case BISECT -> {
                if (now - stateAt < 1500) return;
                if (flickering()) {                                                                 // this part brought it back: it stays off
                    foundOff |= PARTS[bisectAt]; Flicker.off = userOff | foundOff; state = COOL; coolUntil = now + 60_000L; lumN = 0;
                    System.err.println("[Stardrops] flicker guard: " + PART_NAMES[bisectAt] + " made the sky flicker; switched off for this session");
                    say("Stardrops found that " + PART_NAMES[bisectAt] + " made your sky flicker, and switched it off until you restart. Please tell the author, so it can be fixed for good.");
                    return;
                }
                bisectAt++;
                if (bisectAt >= PARTS.length) { Flicker.off = userOff | foundOff; state = COOL; coolUntil = now + 60_000L; lumN = 0; return; }   // it never came back: a passing thing
                int on = 0; for (int k = 0; k <= bisectAt; k++) on |= PARTS[k];
                Flicker.off = ALL & ~on | userOff | foundOff; lumN = 0; stateAt = now;
            }
            case COOL -> { if (now >= coolUntil) { state = WATCH; lumN = 0; } }
            default -> state = WATCH;
        }
    }

    private static void say(String s) {
        try { Minecraft mc = Minecraft.getInstance(); if (mc.player != null) mc.player.sendSystemMessage(Chat.msg(s)); } catch (Throwable ignored) { }
    }
}
