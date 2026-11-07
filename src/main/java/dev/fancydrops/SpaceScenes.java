package dev.fancydrops;

import static dev.fancydrops.Ritual.*;

/**
 * The Stardrops drop scenes, painted on Ritual's pixel canvas (640x360, item at icx, icy). The item and the words are drawn by
 * Ritual on top, as before; the sound beats fall where they always did (beats = the chop cues of the scene).
 *
 *   SLAYER: a cosmic bounty hunt. Warp in to a hostile world glowing in the boss's colour, ringed by debris; a targeting reticle
 *     closes on it and locks; three laser strikes (on the beats) crack it open; it shudders, implodes to a point and goes
 *     supernova, and the drop is what is left. RNG: a bigger blast, an accretion disk round the drop, rays and stars spiralling
 *     in. No drops: it shrugs off the last strike and warps away.
 *   WIP (every other group, for now): a stargate under construction. Hazard-striped segments of a ring drift in and lock together
 *     while a little drone welds them (sparks on the beats) beside a "WIP" sign; the gate powers up, its middle swirls into a
 *     nebula and the drop flies out of it. RNG: the gate blazes, rays, a burst of stars. No drops: the gate sputters and the drone
 *     shrugs.
 */
final class SpaceScenes {
    private SpaceScenes() {}
    /** The scene's beats (the chop cues, in seconds), set by Ritual.begin. */
    static float[] beats = {1.0f, 1.5f, 2.0f};
    static boolean ON = true;                                                                   // the space scenes replace the old ones

    private static final int VOID = 0x05040E, WHITE_ = 0xFFFFFF;

    // ------------------------------------------------------------------ shared: a starfield that warps in, then hangs there
    private static void stars(float t, float O, float icx, float icy, float sz, float a, int tint) {
        float warp = 1f - sm(t / (0.25f * O + 0.05f));                                          // streaks at the start, settling into points
        for (int i = 0; i < 90; i++) {
            float an = rnd(i * 7 + 1) * 6.2832f, d = (30 + rnd(i * 13 + 2) * 300) * sz * 0.9f;
            float x = icx + (float) Math.cos(an) * d * 1.6f, y = icy + (float) Math.sin(an) * d * 0.9f;
            if (x < 0 || x >= CW || y < 0 || y >= CH) continue;
            float tw = 0.5f + 0.5f * (float) Math.sin(t * (2 + rnd(i) * 3) + i);
            int c = i % 9 == 0 ? tint : i % 5 == 0 ? 0xC8D8FF : WHITE_;
            if (warp > 0.02f) { float len = warp * 26 * sz * (0.4f + rnd(i * 3)); stroke(x, y, x + (float) Math.cos(an) * len * 1.6f, y + (float) Math.sin(an) * len * 0.9f, 0.5f, c, a * 0.8f * warp); }
            dot(x, y, c, a * (0.35f + 0.5f * tw) * (1f - warp * 0.6f));
        }
    }

    // ------------------------------------------------------------------ SLAYER
    static void slayer(float t, float O, float p, float icx, float gy, float icy, float sz, float isc, int boss, int energy, int rc,
                       boolean rng, boolean nod, float itemA, float bg, float[] offs, float cy0) {
        float a = bg * sm(t / (0.12f * O + 0.05f));
        glowE(icx, icy, 190 * sz, 120 * sz, VOID, 0.92f * a); glowE(icx, icy, 120 * sz, 80 * sz, VOID, 0.5f * a);                                   // a pool of dark space behind it all
        glowE(icx, icy, 120 * sz, 70 * sz, mixc(energy, VOID, 0.6f), 0.35f * a);                // its colour, faintly, in the dust
        stars(t, O, icx, icy, sz, a, energy);
        float R = 26 * sz;                                                                      // the hostile world
        float rise = eob((t - 0.12f * O) / (0.3f * O));
        float b0 = beats.length > 0 ? beats[0] : 0.34f * O, bl = beats.length > 0 ? beats[beats.length - 1] : 0.8f * O;
        int hits = 0; for (float b : beats) if (t >= b) hits++;
        float shud = sm((t - bl) / Math.max(0.05f, O - bl)), implode = p < 0 ? 0f : sm(p / 0.18f);
        float jx = (rnd((int) (t * 50)) - 0.5f) * 3 * shud * (p < 0 ? 1 : 0), jy = (rnd((int) (t * 50) + 7) - 0.5f) * 3 * shud * (p < 0 ? 1 : 0);
        float wx = icx + jx, wy = icy + jy, r = R * rise * (1f - 0.25f * shud) * (1f - implode);
        boolean away = nod && p >= 0;
        if (away) { float u = sm(p / 0.7f); wx += u * 180 * sz; wy -= u * 70 * sz; r = R * (1f - 0.7f * u); }
        float wa = a * (away ? 1f - sm((p - 0.4f) / 0.4f) : 1f);
        if (r > 0.6f && wa > 0.02f) {
            // debris ring, back half
            ring(t, wx, wy, r, energy, wa, true, boss);
            glowE(wx, wy, r * 2.2f, r * 2.2f, energy, 0.35f * wa * (1f + shud));
            fillE(wx, wy, r, r, 0x0B0812, wa);                                                   // the world: dark
            glowE(wx - r * 0.25f, wy - r * 0.25f, r * 0.9f, r * 0.9f, mixc(energy, VOID, 0.45f), 0.55f * wa);
            ringE(wx, wy, r, r, mixc(energy, WHITE_, 0.3f), 0.85f * wa, 0, 6.2832f, 0, 0);       // its glowing limb
            ringE(wx, wy, r + 1, r + 1, energy, 0.4f * wa, 0, 6.2832f, 0, 0);
            motif(t, wx, wy, r, boss, energy, wa);
            // the eye: a slit that opens, watching
            float eye = sm((t - 0.25f * O) / (0.15f * O)) * (((long) (t * 10)) % 37 == 0 ? 0.2f : 1f);
            if (eye > 0.02f) { fillE(wx, wy, r * 0.42f, r * 0.12f * eye, energy, 0.9f * wa); fillE(wx, wy, r * 0.12f, r * 0.1f * eye, WHITE_, wa); glowE(wx, wy, r * 0.8f, r * 0.35f, energy, 0.5f * wa * eye); }
            // cracks of light from each strike
            for (int k = 0; k < hits; k++) for (int c = 0; c < 3; c++) {
                float an = rnd(k * 31 + c * 7 + boss) * 6.2832f, len = r * (0.55f + 0.45f * rnd(k * 5 + c)) * Math.min(1f, (t - beats[k]) / 0.25f + 0.3f);
                stroke(wx, wy, wx + (float) Math.cos(an) * len, wy + (float) Math.sin(an) * len, 0.6f + 0.6f * shud, mixc(energy, WHITE_, 0.5f + 0.4f * shud), wa * (0.7f + 0.3f * shud));
            }
            ring(t, wx, wy, r, energy, wa, false, boss);                                         // debris ring, front half
        }
        // the reticle: four brackets closing in, then locked (it turns red-white and pulses)
        float lock = sm((t - 0.18f * O) / Math.max(0.05f, b0 - 0.18f * O)), ra = a * (p < 0 ? 1f : 1f - sm(p / 0.2f));
        if (lock > 0.01f && !away && ra > 0.02f) {
            float rr = R * (2.4f - 1.0f * lock) * (0.95f + 0.05f * (float) Math.sin(t * 10)), rot = (1f - lock) * 1.2f;
            int rc2 = lock >= 0.999f ? mixc(0xFF4A5A, WHITE_, 0.3f + 0.3f * (float) Math.sin(t * 14)) : 0xC8D4E8;
            for (int q = 0; q < 4; q++) {
                float an = rot + q * 1.5708f + 0.785f, cx = icx + (float) Math.cos(an) * rr, cy = icy + (float) Math.sin(an) * rr;
                float tx = -(float) Math.sin(an), ty = (float) Math.cos(an), ix = -(float) Math.cos(an), iy = -(float) Math.sin(an), L = 6 * sz;
                stroke(cx - tx * L, cy - ty * L, cx + tx * L, cy + ty * L, 0.7f, rc2, 0.9f * ra);
                stroke(cx, cy, cx + ix * L, cy + iy * L, 0.7f, rc2, 0.9f * ra);
            }
            if (lock >= 0.999f) { stroke(icx - rr * 1.15f, icy, icx - rr * 0.55f, icy, 0.4f, rc2, 0.6f * ra); stroke(icx + rr * 0.55f, icy, icx + rr * 1.15f, icy, 0.4f, rc2, 0.6f * ra); }
        }
        // the three strikes: beams from the left, the right, then both, each a flash and sparks at the impact
        for (int k = 0; k < beats.length; k++) {
            float tau = t - beats[k]; if (tau < 0 || tau > 0.45f) continue;
            float f = 1f - tau / 0.45f;
            for (int sdir = -1; sdir <= 1; sdir += 2) {
                if (k == 0 && sdir > 0 || k == 1 && sdir < 0) continue;
                float sx = sdir < 0 ? 0 : CW, sy = icy - 60 * sz + k * 18 * sz;
                float hx = icx + sdir * r * 0.4f, hy = icy + (rnd(k * 3) - 0.5f) * r * 0.6f;
                stroke(sx, sy, hx, hy, 3.2f * f, energy, 0.35f * f * a); stroke(sx, sy, hx, hy, 1.2f * f, WHITE_, 0.95f * f * a);
                glowE(hx, hy, 18 * sz * f, 18 * sz * f, WHITE_, 0.8f * f * a);
                for (int j = 0; j < 10; j++) { float vx = (rnd(k * 40 + j) - 0.5f) * 160 - sdir * 60, vy = (rnd(k * 70 + j) - 0.6f) * 120; dot(hx + vx * tau, hy + vy * tau + 120 * tau * tau, j % 3 == 0 ? WHITE_ : energy, f * a); }
            }
        }
        if (p < 0) return;
        // ---- the pivot
        if (away) {                                                                              // it shrugs it off and warps away: a streak, smoke
            float u = cl(p / 0.7f); for (int j = 0; j < 6; j++) stroke(wx - (40 + j * 6) * sz * u, wy + (j - 3) * 2 * sz + 20 * sz * u, wx, wy + (j - 3) * 1.5f * sz, 0.5f, energy, 0.5f * (1f - u) * a);
            glowE(icx, icy, 30 * sz * (1f - u), 20 * sz * (1f - u), 0x8A8A9A, 0.5f * (1f - u) * a);
            return;
        }
        float fl = 1f - cl(p / 0.35f), Rs = 46 * sz * isc * (rng ? 1.25f : 1f);
        glowE(icx, icy, Rs * (1.2f + fl), Rs * (1.2f + fl), WHITE_, 0.9f * fl);                 // the supernova's flash
        for (int ring = 0; ring < (rng ? 2 : 1); ring++) {                                      // the shockwave
            float sw = (p - ring * 0.12f) / 0.9f; if (sw < 0 || sw > 1) continue;
            ringE(icx, icy, Rs * (0.2f + 2.2f * eo(sw)), Rs * (0.2f + 2.2f * eo(sw)) * 0.55f, mixc(energy, WHITE_, 0.5f), (1f - sw) * 0.9f, 0, 6.2832f, 0, 0);
            ringE(icx, icy, Rs * (0.2f + 2.2f * eo(sw)) - 2, Rs * (0.2f + 2.2f * eo(sw)) * 0.55f - 1, energy, (1f - sw) * 0.5f, 0, 6.2832f, 0, 0);
        }
        for (int j = 0; j < (rng ? 46 : 26); j++) {                                              // debris flung out
            float an = rnd(j * 11 + 5) * 6.2832f, v = (60 + rnd(j * 3) * 140) * sz, tau = Math.min(p, 1.6f), d = v * tau * (1f - 0.25f * tau);
            dot(icx + (float) Math.cos(an) * d, icy + (float) Math.sin(an) * d * 0.6f, j % 4 == 0 ? WHITE_ : j % 2 == 0 ? energy : rc, (1f - cl(p / 1.6f)) * 0.95f);
        }
        float hold = itemA;
        for (float off : offs) {
            float cx = icx + off;
            glowE(cx, cy0, 30 * sz * isc, 30 * sz * isc, mixc(energy, rc, 0.5f), 0.5f * hold);
            glowE(cx, cy0, 13 * sz * isc, 13 * sz * isc, WHITE_, 0.25f * hold);
            if (!rng) { orbitDots(t, cx, cy0, 24 * sz * isc, 7 * sz * isc, 6, energy, 0.85f * hold); continue; }
            // RNG: an accretion disk (rings turning), rays and stars spiralling in
            for (int k = 0; k < 4; k++) { float rr = (22 + k * 7) * sz * isc; ringE(cx, cy0, rr, rr * 0.28f, k % 2 == 0 ? mixc(energy, WHITE_, 0.4f) : rc, (0.7f - 0.12f * k) * hold, 0, 6.2832f, t * (1.6f - k * 0.3f), 3); }
            for (int k = 0; k < 14; k++) { float an = k * 6.2832f / 14 + t * 0.4f, l0 = 18 * sz * isc, l1 = (36 + 10 * (float) Math.sin(t * 3 + k)) * sz * isc;
                stroke(cx + (float) Math.cos(an) * l0, cy0 + (float) Math.sin(an) * l0 * 0.8f, cx + (float) Math.cos(an) * l1, cy0 + (float) Math.sin(an) * l1 * 0.8f, 0.6f, k % 2 == 0 ? WHITE_ : energy, 0.45f * hold); }
            for (int k = 0; k < 16; k++) { float ph = (t * 0.5f + k / 16f) % 1f, an = k * 2.4f + ph * 5f, rr = (50 - 42 * ph) * sz * isc;
                glint(cx + (float) Math.cos(an) * rr, cy0 + (float) Math.sin(an) * rr * 0.6f, ph > 0.7f ? 2 : 1, k % 3 == 0 ? energy : WHITE_, (float) Math.sin(Math.PI * ph) * hold); }
        }
    }
    /** The debris ring round the world (tilted): the back half before the world, the front half over it. */
    private static void ring(float t, float x, float y, float r, int energy, float a, boolean back, int boss) {
        int n = 40;
        for (int i = 0; i < n; i++) {
            float an = i * 6.2832f / n + t * (0.35f + boss * 0.04f) + rnd(i) * 0.1f; float sy = (float) Math.sin(an);
            if (back != (sy < 0)) continue;
            float rr = r * (1.7f + 0.25f * rnd(i * 3)), px = x + (float) Math.cos(an) * rr, py = y + sy * rr * 0.28f - (float) Math.cos(an) * rr * 0.12f;
            dot(px, py, i % 5 == 0 ? WHITE_ : mixc(energy, 0x9A9AB0, 0.4f), a * (back ? 0.5f : 0.95f));
        }
    }
    /** Each boss's world has its own look on top of the dark sphere. */
    private static void motif(float t, float x, float y, float r, int boss, int energy, float a) {
        switch (boss) {
            case 0 -> { for (int i = 0; i < 7; i++) { float an = rnd(i * 5) * 6.2832f, d = r * 0.6f * rnd(i * 9); glowE(x + (float) Math.cos(an) * d, y + (float) Math.sin(an) * d, r * 0.18f, r * 0.18f, energy, 0.6f * a * (0.6f + 0.4f * (float) Math.sin(t * 3 + i))); } }   // spore blooms
            case 1 -> { for (int i = 0; i < 8; i++) { float an = i * 0.785f + 0.2f, l = r * (1.35f + 0.1f * (float) Math.sin(t * 6 + i)); stroke(x + (float) Math.cos(an) * r, y + (float) Math.sin(an) * r, x + (float) Math.cos(an) * l, y + (float) Math.sin(an) * l, 0.8f, energy, 0.8f * a); } }   // spines
            case 2 -> { for (int i = 0; i < 6; i++) { float an = i * 1.047f + t * 0.3f; stroke(x, y, x + (float) Math.cos(an) * r * 0.9f, y + (float) Math.sin(an) * r * 0.9f, 0.5f, 0xE8F4FF, 0.5f * a); } }   // frost lines
            case 3 -> { for (int k = 0; k < 2; k++) for (int i = 0; i < 20; i++) { float u = i / 20f, an = t * 1.5f + k * 3.1416f + u * 5f, d = r * 0.85f * (1f - u); dot(x + (float) Math.cos(an) * d, y + (float) Math.sin(an) * d, energy, a * u); } }   // a swirl
            case 4 -> { for (int i = 0; i < 4; i++) { float an = rnd(i * 13) * 6.2832f + t * 0.2f, h = r * (0.3f + 0.25f * (float) Math.sin(t * 4 + i)); ringE(x + (float) Math.cos(an) * r, y + (float) Math.sin(an) * r, h, h * 0.6f, 0xFFD060, 0.7f * a, 3.1416f, 6.2832f, an + 1.5708f, 0); } }   // solar flares
            default -> { for (int i = 0; i < 5; i++) { float an = rnd(i * 17) * 6.2832f; fillE(x + (float) Math.cos(an) * r * 0.55f, y + (float) Math.sin(an) * r * 0.55f, r * 0.12f, r * 0.12f, energy, 0.7f * a); } }   // glowing craters
        }
    }
    private static void orbitDots(float t, float cx, float cy, float rx, float ry, int n, int rgb, float a) {
        for (int i = 0; i < n; i++) { float an = t * 2f + i * 6.2832f / n; glint(cx + (float) Math.cos(an) * rx, cy + (float) Math.sin(an) * ry, (float) Math.sin(an) > 0 ? 2 : 1, i % 2 == 0 ? WHITE_ : rgb, a); }
    }

    // ------------------------------------------------------------------ WIP: a stargate under construction
    private static final int HAZ = 0xFFC830, HAZ_D = 0x1A1A24, STEEL = 0x8A90A8;
    static void wip(float t, float O, float p, float icx, float gy, float icy, float sz, float isc, int rc, boolean rng, boolean nod,
                    float itemA, float bg, float[] offs, float cy0) {
        float a = bg * sm(t / (0.12f * O + 0.05f));
        glowE(icx, icy, 190 * sz, 120 * sz, VOID, 0.9f * a); glowE(icx, icy, 120 * sz, 80 * sz, VOID, 0.5f * a);
        glowE(icx + 40 * sz, icy - 20 * sz, 110 * sz, 60 * sz, 0x6A4AD8, 0.25f * a);            // a wisp of nebula behind
        stars(t, O, icx, icy, sz, a, 0xC8B4FF);
        float rx = 34 * sz * isc, ry = 34 * sz * isc * 0.92f;
        int segs = 12; float built = sm(t / (0.85f * O));
        float power = p < 0 ? 0f : sm(p / 0.25f), sputter = nod && p >= 0 ? 1f : 0f;
        // inside the gate: dark until it powers up, then a swirling nebula
        if (power > 0.01f && sputter == 0f) {
            glowE(icx, icy, rx, ry, mixc(rc, 0x9B7BFF, 0.5f), 0.8f * power * itemA);
            for (int k = 0; k < 3; k++) for (int i = 0; i < 26; i++) { float u = i / 26f, an = t * (1.8f + k * 0.4f) + k * 2.1f + u * 6f, d = rx * 0.9f * (1f - u); dot(icx + (float) Math.cos(an) * d, icy + (float) Math.sin(an) * d * 0.92f, k == 0 ? WHITE_ : k == 1 ? rc : 0xFF7AD9, power * itemA * (0.4f + 0.6f * u)); }
        }
        // the ring, segment by segment: each drifts in from outside and locks into place; hazard stripes on it
        for (int sgi = 0; sgi < segs; sgi++) {
            float arrive = sm((built * (segs + 3) - sgi) / 3f); if (arrive <= 0.01f) continue;
            float a0 = sgi * 6.2832f / segs, a1 = a0 + 6.2832f / segs * 0.86f, mid = (a0 + a1) / 2f;
            float push = (1f - arrive) * 60 * sz, ox = (float) Math.cos(mid) * push, oy = (float) Math.sin(mid) * push;
            int steps = 10;
            for (int i = 0; i < steps; i++) {
                float an = a0 + (a1 - a0) * i / (steps - 1f), cs = (float) Math.cos(an), sn = (float) Math.sin(an);
                boolean stripe = ((int) ((an * 8 + t * (power > 0 ? 4 : 0)) % 2 + 2) % 2) == 0;
                int col = power > 0 && sputter == 0f ? mixc(stripe ? HAZ : STEEL, rc, 0.3f * power) : stripe ? HAZ : HAZ_D;
                fillE(icx + ox + cs * rx, icy + oy + sn * ry, 2.6f * sz, 2.6f * sz, col, a * arrive);
            }
            ringE(icx + ox, icy + oy, rx + 3 * sz, ry + 3 * sz, STEEL, 0.6f * a * arrive, a0, a1, 0, 0);
            ringE(icx + ox, icy + oy, rx - 3 * sz, ry - 3 * sz, STEEL, 0.5f * a * arrive, a0, a1, 0, 0);
        }
        if (power > 0.01f && sputter == 0f) { ringE(icx, icy, rx + 4 * sz, ry + 4 * sz, mixc(rc, WHITE_, 0.5f), 0.8f * power * itemA, 0, 6.2832f, 0, 0); glowE(icx, icy, rx * 1.5f, ry * 1.5f, rc, 0.3f * power * itemA); }
        // the drone: hovering round the ring, welding the newest segment; sparks on the beats
        int cur = Math.min(segs - 1, (int) (built * segs)); float dan = (cur + 0.5f) * 6.2832f / segs;
        float dbob = (float) Math.sin(t * 4) * 2 * sz, dx = icx + (float) Math.cos(dan) * (rx + 16 * sz), dy = icy + (float) Math.sin(dan) * (ry + 16 * sz) + dbob;
        if (p >= 0) { float u = sm(p / 0.6f); dx = lerp(dx, icx + rx + 40 * sz, u); dy = lerp(dy, icy - ry * 0.6f, u) + dbob; }
        float shrug = sputter > 0 ? (float) Math.sin(p * 9) * 0.3f * (1f - cl(p / 1.2f)) : 0f;
        drone(t, dx, dy, sz, a, shrug, p < 0);
        for (float b : beats) {
            float tau = t - b; if (tau < 0 || tau > 0.5f) continue; float f = 1f - tau / 0.5f;
            float wx = icx + (float) Math.cos(dan) * rx, wy = icy + (float) Math.sin(dan) * ry;
            glowE(wx, wy, 14 * sz * f, 14 * sz * f, 0xBFE8FF, 0.9f * f * a); glint(wx, wy, 2, WHITE_, f * a);
            for (int j = 0; j < 12; j++) { float vx = (rnd((int) (b * 100) + j) - 0.5f) * 120, vy = -(rnd((int) (b * 100) + j * 3) * 90); dot(wx + vx * tau, wy + vy * tau + 200 * tau * tau, j % 2 == 0 ? 0xFFE07A : WHITE_, f * a); }
        }
        // the WIP sign on its little post (pixel letters)
        float sx = icx - rx - 36 * sz, sy = icy + ry * 0.4f;
        sign(sx, sy, sz, a * (1f - cl(p / 1.2f) * 0.6f), t);
        if (p < 0) return;
        if (sputter > 0f) {                                                                      // no drop: the gate sputters out
            for (int i = 0; i < 3; i++) { float tau = p - i * 0.25f; if (tau < 0 || tau > 0.3f) continue; glowE(icx, icy, rx * 0.6f, ry * 0.6f, 0x9A9AB0, 0.5f * (1f - tau / 0.3f) * a); }
            return;
        }
        float fl = 1f - cl(p / 0.35f);
        glowE(icx, icy, rx * (1.4f + fl), ry * (1.4f + fl), WHITE_, 0.85f * fl);
        float sw = p / 0.9f; if (sw < 1f) ringE(icx, icy, rx * (1f + 1.6f * eo(sw)), ry * (1f + 1.6f * eo(sw)), mixc(rc, WHITE_, 0.5f), (1f - sw) * 0.85f, 0, 6.2832f, 0, 0);
        for (float off : offs) {
            float cx = icx + off;
            glowE(cx, cy0, 26 * sz * isc, 26 * sz * isc, rc, 0.45f * itemA);
            if (!rng) continue;
            for (int k = 0; k < 16; k++) { float an = k * 6.2832f / 16 + t * 0.3f, l0 = rx * 1.05f, l1 = rx * (1.6f + 0.2f * (float) Math.sin(t * 3 + k));
                stroke(cx + (float) Math.cos(an) * l0, cy0 + (float) Math.sin(an) * l0 * 0.92f, cx + (float) Math.cos(an) * l1, cy0 + (float) Math.sin(an) * l1 * 0.92f, 0.7f, k % 2 == 0 ? WHITE_ : k % 4 == 1 ? 0xFF7AD9 : rc, 0.5f * itemA); }
            for (int j = 0; j < 30; j++) { float ph = (p * 0.6f + rnd(j * 7)) % 1f, an = rnd(j * 3) * 6.2832f, d = rx * (1f + 1.4f * ph); glint(cx + (float) Math.cos(an) * d, cy0 + (float) Math.sin(an) * d * 0.7f, 1, j % 3 == 0 ? 0xFFE07A : WHITE_, (float) Math.sin(Math.PI * ph) * itemA); }
        }
    }
    /** A little welding drone: a round body, a blinking eye light, a thruster flame, an arm with the torch. */
    private static void drone(float t, float x, float y, float sz, float a, float tilt, boolean working) {
        if (a < 0.02f) return;
        float k = sz * 1.1f;
        glowE(x, y + 7 * k, 5 * k, 3 * k, 0x7AD8FF, 0.6f * a * (0.7f + 0.3f * (float) Math.sin(t * 30)));     // thruster
        fillE(x, y, 6 * k, 5 * k, 0x2A2E44, a); fillE(x - 1 * k, y - 1 * k, 5 * k, 4 * k, STEEL, a);
        fillE(x + 1.5f * k + tilt * 6 * k, y - 0.5f * k, 1.6f * k, 1.6f * k, (((long) (t * 3)) % 2 == 0) ? 0x7AF0B0 : 0x2A6A4A, a);   // its eye light
        stroke(x - 5 * k, y + 1 * k, x - 10 * k, y + 4 * k + tilt * 8 * k, 0.6f * k, STEEL, a);   // the arm
        if (working) dot(x - 10 * k, y + 4 * k, 0xBFE8FF, a);
        stroke(x - 3 * k, y - 4 * k, x - 2 * k + tilt * 4 * k, y - 8 * k, 0.4f * k, STEEL, a); dot(x - 2 * k + tilt * 4 * k, y - 8.5f * k, 0xFF5A6A, a);   // an aerial
    }
    private static final String[] WIP = {"X...X.XXX.XXX.", "X...X..X..X..X", "X.X.X..X..XXX.", "XX.XX..X..X...", "X...X.XXX.X..."};
    /** The sign: a little board on a post, hazard stripes along its top, "WIP" in pixel letters. */
    private static void sign(float x, float y, float sz, float a, float t) {
        if (a < 0.02f) return;
        float px = Math.max(1f, 1.4f * sz), w = 16 * px, h = 10 * px;
        stroke(x, y + h, x, y + h + 14 * px, 0.8f * px, STEEL, a);                                  // the post
        for (float yy = y; yy < y + h; yy += 1) for (float xx = x - w / 2; xx < x + w / 2; xx += 1) dot(xx, yy, 0x1A1A24, a);
        for (float xx = x - w / 2; xx < x + w / 2; xx += 1) dot(xx, y, ((int) ((xx - x + t * 6) / 2)) % 2 == 0 ? HAZ : 0x1A1A24, a);
        for (int r = 0; r < 5; r++) for (int c = 0; c < 14; c++) if (WIP[r].charAt(c) == 'X') {
            float gx = x - 7 * px + c * px, gy = y + 2.5f * px + r * px;
            for (float yy = 0; yy < px; yy += 1) for (float xx = 0; xx < px; xx += 1) dot(gx + xx, gy + yy, HAZ, a);
        }
        if (((long) (t * 2)) % 2 == 0) glowE(x + w / 2, y, 2.5f * px, 2.5f * px, 0xFF8A3A, 0.8f * a);   // a warning light on its corner
    }
}
