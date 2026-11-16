package dev.stardrops;

import net.minecraft.network.chat.Style;

/** Effects for a player's name (IGN). Special effects have colours of their own; every other one keeps the colour the server gave the name and only adds white. */
final class NameFx {
    private NameFx() {}

    /** Does it move? Every effect does. */
    static boolean animated(String fx) { return fx != null && !fx.isEmpty(); }

    private static int mix(int a, int b, float k) {
        k = Math.max(0f, Math.min(1f, k));
        int r = Math.round(((a >> 16) & 255) * (1 - k) + ((b >> 16) & 255) * k), g = Math.round(((a >> 8) & 255) * (1 - k) + ((b >> 8) & 255) * k), bl = Math.round((a & 255) * (1 - k) + (b & 255) * k);
        return (r << 16) | (g << 8) | bl;
    }
    private static int hash(int i, long step) {
        int h = i * 73856093 ^ (int) step * 19349663;
        h ^= h >>> 13; h *= 0x5bd1e995; h ^= h >>> 15;
        return Math.abs(h);
    }


    /** The colour (RGB) of letter i of n at time t (ms).
     *  The colour rule: a Special effect (and Beta, a Special set) may give the name colours of its own;
     *  every other effect only moves between the name's own colour and white. */
    static int color(String fx, int i, int n, int base, long t) {
        base &= 0xFFFFFF;
        StardropsConfig cf = StardropsConfig.get();
        t = cf.cosCalm ? 1200L : t * Math.max(10, cf.cosSpeed) / 100;          // calm mode: every name stands still (one quiet frame); otherwise the speed setting applies, in chat, tab, name tags and the menu alike
        float ph;
        switch (fx) {
            // ---------------- Special: colours of their own
            case "pulse": {                                                 // Signal (Beta): mission-control green; a bright scan runs along the name and leaves a fading trace
                int c = 0x3FD16A;
                float pos = (t % 2200L) / 2200f * (n + 9) - 2, d = pos - i;
                if (d >= -0.5f && d < 0.6f) return mix(c, 0xFFFFFF, 0.9f);
                if (d >= 0.6f && d < 5f) return mix(mix(c, 0xFFFFFF, 0.55f), c, (d - 0.6f) / 4.4f);
                return mix(c, 0x1E8A4A, 0.25f + 0.25f * (float) Math.sin(t / 700.0 + i * 0.9));
            }
            case "sparklefx": {                                             // Starlight: pale starlight blue; a white comet runs along it and single letters twinkle white
                int main = 0xA9BBFF;
                ph = 0.5f + 0.5f * (float) Math.sin(t / 900.0 - i * 0.5);
                int c = mix(main, 0xFFFFFF, 0.18f * ph);
                float pos = (t % 2800L) / 2800f * (n + 10) - 3, d = pos - i;                       // d > 0: behind the comet's head
                if (d >= -0.4f && d < 0.9f) c = 0xFFFFFF;
                else if (d >= 0.9f && d < 4f) c = mix(0xFFFFFF, c, (d - 0.9f) / 3.1f);
                long st = t / 700L; float en = (float) Math.sin(Math.PI * ((t % 700L) / 700f));        // one letter at a time swells to white and fades back
                if (n > 0 && hash(7, st) % n == i) c = mix(c, 0xFFFFFF, 0.9f * en);
                return c;
            }
            case "glitch": {                                                // Critical: red, with a heartbeat: twice a beat ("lub-dub") a white pulse bursts from the middle of the name and runs out to both ends
                int c = 0xFF3B30; float best = 0f;
                long cyc = t % 1600L; float mid = (n - 1) / 2f, dist = Math.abs(i - mid);
                for (long b : new long[]{0L, 240L}) {
                    long dt = cyc - b; if (dt < 0 || dt >= 520L) continue;
                    float u = dt / 520f, front = u * (mid + 2.5f), k = Math.max(0f, 1f - Math.abs(dist - front) / 1.7f) * (1f - u);
                    best = Math.max(best, b == 0L ? k : k * 0.75f);
                }
                return mix(c, 0xFFFFFF, 0.92f * best);
            }
            case "contribfx": {                                             // Nebula: teal, violet and pink clouds drift slowly through the name; now and then a letter lights up like a new star
                float u = 0.5f + 0.5f * (float) Math.sin(t / 1300.0 - i * 0.55), v = 0.5f + 0.5f * (float) Math.sin(t / 2100.0 + i * 0.35 + 1.7);
                int c = mix(mix(0x3FE0D0, 0x9B7BFF, u), 0xFF7AC8, 0.7f * v * u);
                long st = t / 900L; float en = (float) Math.sin(Math.PI * ((t % 900L) / 900f));
                if (n > 0 && hash(13, st) % n == i) c = mix(c, 0xFFFFFF, 0.85f * en);
                return c;
            }
            case "royal": {                                                 // Solar: gold that glows down to orange and back, and a white flare that sweeps along the name
                ph = 0.5f + 0.5f * (float) Math.sin(t / 650.0 - i * 0.7);
                int c = mix(0xFFC21A, 0xFF7A1E, 0.55f * ph);
                long cyc = t % 2600L;
                if (cyc < 1300L) { float pos = cyc / 1300f * (n + 8) - 4, k = Math.max(0f, 1f - Math.abs(i - pos) / 2.4f); c = mix(c, 0xFFFFFF, k * k * (3 - 2 * k)); }
                return c;
            }
            // ---------------- earned in Achievements: only the name's own colour and white
            case "sweep": {                                                 // Comet Trail: a white head crosses your name and drags a long fading tail
                long cyc = t % 3000L; if (cyc > 1900L) return base;
                float pos = cyc / 1900f * (n + 12) - 1, d = pos - i;
                if (d >= -0.5f && d < 0.7f) return 0xFFFFFF;
                if (d >= 0.7f && d < 7f) { float k = 1f - (d - 0.7f) / 6.3f; return mix(base, 0xFFFFFF, 0.8f * k * k); }
                return base;
            }
            case "ripple": {                                                // Tide: white rises through the name from the left like water coming in, then draws back
                float lvl = (0.5f - 0.5f * (float) Math.cos(t / 1500.0 * Math.PI)) * (n + 3) - 1.5f;          // where the water's edge is
                float d = lvl - i, k = d > 1.5f ? 0.55f : d > -1.5f ? 0.55f * (d + 1.5f) / 3f : 0f;
                if (Math.abs(d) < 0.8f) k = Math.max(k, 0.95f);                                                // the foam at the edge
                return mix(base, 0xFFFFFF, k);
            }
            case "spark": {                                                 // Twinkle: single letters flash white for a moment, two at a time, here and there
                int c = base;
                for (int k = 0; k < 2; k++) {
                    long per = 520L, off = k * 260L, st = (t + off) / per; float u = ((t + off) % per) / (float) per;
                    if (n > 0 && hash(k * 11 + 3, st) % n == i) c = mix(c, 0xFFFFFF, 0.95f * (float) Math.sin(Math.PI * Math.min(1f, u * 1.6f)));
                }
                return c;
            }
            case "neon": {                                                  // Eclipse: white slides over your name from one side, covers it for a moment, and slides off the other side
                long cyc = t % 5200L; if (cyc > 3000L) return base;
                float u = cyc / 3000f, lead = u * (n + 6) - 1, trail = lead - (n * 0.6f + 2.5f), d1 = lead - i, d2 = i - trail;
                float k = Math.max(0f, Math.min(1f, Math.min(d1, d2) / 1.4f));
                return mix(base, 0xFFFFFF, 0.92f * k * k * (3 - 2 * k));
            }
            default: return base;
        }
    }

    /** The style for letter i: the name's own style with the effect's colour. live = animated; false = a frozen look (only used if chat can't animate). */
    static Style style(String fx, int i, int n, Style base, boolean live, long t) {
        int b = base.getColor() == null ? 0xFFFFFF : base.getColor().getValue();
        long tt = live ? t : 1500L + i * 90L;
        int c = color(fx, i, n, b, tt);
        Style st = base.withColor(c);
        return st;
    }
}
