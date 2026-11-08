package dev.stardrops;

import static dev.stardrops.Ritual.*;
import static dev.stardrops.Vox3D.*;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Slayer cutscenes, Butcher-style: a close little night scene per slayer where a meteor strikes a themed object on every beat
 * (SlayerScene.HITS, so the sounds land with it) until it breaks open and the drop rises out of it. Drawn with Vox3D's chunky
 * outlined boxes; the ground is soft and fades into the dark at the window's edge, no floating island.
 *   Revenant: meteors blast the grave in front of a mossy R.I.P. headstone away; the coffin under it flips open.
 *   Tarantula: meteors hit a silk cocoon hanging in a web; it splits in two.
 *   Sven: meteors knock a cairn of stones apart under the trees, wolf eyes glowing in the dark.
 *   Voidgloom: each meteor lights an eye in a ring of portal frames; the portal opens.
 *   Inferno: meteors crack a magma core between two fire pillars; it erupts.
 *   Riftstalker: meteors break the chains round a standing coffin; its door bursts open.
 * No drops: the same beats, one strike, and it opens on nothing (a puff of dust). RNG: the strikes spark through the rarities, the sky
 * holds its breath, the last strike is gold and the drop rises in a gold pillar.
 */
final class Cut3D {
    private Cut3D() {}

    static final float FOV = 34f;
    /** Where the drop comes out, per slayer (it is pinned under the item on screen). */
    static final Vector3f[] PIN = {new Vector3f(0f, 0.35f, 0.35f), new Vector3f(0f, 1.55f, 0f), new Vector3f(0f, 0.9f, 0f),
        new Vector3f(0f, 0.15f, 0f), new Vector3f(0f, 1.2f, 0f), new Vector3f(0f, 1.0f, 0.1f)};

    static float[] hits; static float last; static int type, boss, n;
    /** How many strikes have landed by t. */
    static int done(float t) { int k = 0; for (float h : hits) if (t >= h) k++; return k; }
    /** Seconds since the latest strike (big if none). */
    static float since(float t) { float s = 99f; for (float h : hits) if (t >= h) s = Math.min(s, t - h); return s; }
    static int sparkCol(int i, int energy) { return type == 2 ? SlayerScene.TIER[Math.min(i, SlayerScene.TIER.length - 1)] : type == 0 ? 0xC8C8D0 : mixc(energy, 0xFFFFFF, 0.3f); }

    static Frame build(float t, float A, int bossIn, int typeIn, int energy, float aspect, float pinY, boolean z01) {
        boss = bossIn; type = typeIn; hits = SlayerScene.HITS[type]; n = hits.length; last = hits[n - 1];
        F = new Frame(); tint = 0f; M = new Matrix4f(); STACK.clear(); mobMode = false; pass = 0; dyn = false; fovNow = FOV;
        float O = AnimationManager.SL_PIVOT, p = t - O;
        Vector3f pin = PIN[boss];

        // ---- the camera: close, a slow push and drift, a kick on every strike, a lean in before RNG's last one
        float sh = Math.min(1.2f, kick(t));
        float dist = lerp(9.2f, 7.6f, eo(t / 1.4f)) - 0.5f * sm((t - SlayerScene.WALK) / 1.5f);
        if (type == 2) dist -= 1.1f * sm((t - 2.95f) / 0.42f) * (1f - sm((t - last) / 0.3f));
        dist += 0.6f * sm(p / 0.6f);
        float yaw = (float) Math.toRadians(18 + 6 * Math.sin(t * 0.35) + 10 * (1f - eo(t / 1.4f)));
        float pitch = (float) Math.toRadians(17 + 2 * Math.sin(t * 0.28));
        Vector3f target = new Vector3f(pin.x + 0.2f, pin.y + 0.35f, pin.z);
        eye.set(target.x + (float) (Math.sin(yaw) * Math.cos(pitch)) * dist, target.y + (float) Math.sin(pitch) * dist, target.z + (float) (Math.cos(yaw) * Math.cos(pitch)) * dist);
        float jx = 0.06f * sh * (float) (Math.sin(t * 53) + Math.sin(t * 31 + 1.3)), jy = 0.06f * sh * (float) (Math.sin(t * 47 + 2.1) + Math.sin(t * 37 + 0.4));
        Matrix4f view = new Matrix4f().lookAt(eye.x + jx, eye.y + jy, eye.z, target.x + jx * 0.5f, target.y + jy * 0.5f, target.z, 0, 1, 0);
        if (sh > 0.01f) view = new Matrix4f().rotateZ(0.015f * sh * (float) Math.sin(t * 29 + 0.7)).mul(view);
        right.set(view.m00(), view.m10(), view.m20()); up.set(view.m01(), view.m11(), view.m21());
        Matrix4f persp = new Matrix4f().setPerspective((float) Math.toRadians(FOV), aspect, 1000f, 0.05f, z01);
        Vector4f cr = new Matrix4f(persp).mul(view).transform(new Vector4f(pin.x, pin.y, pin.z, 1f));
        shiftX = -cr.x / cr.w; shiftY = pinY - cr.y / cr.w;
        Matrix4f proj = new Matrix4f().translation(shiftX, shiftY, 0f).mul(persp);
        projView = new Matrix4f(proj).mul(view); F.proj = proj; F.view = view;

        float dim = type == 2 ? sm((t - 2.95f) / 0.3f) * (1f - sm((t - last) / 0.25f)) : 0f;
        Theme th = THEMES[boss];
        sky(F.back, aspect, 400f, mixc(th.skyTop(), 0x000000, 0.5f * dim), mixc(th.skyMid(), 0x000000, 0.5f * dim), mixc(th.skyLow(), 0x000000, 0.4f * dim), 0.97f);
        Vox3D.stars(t, dim, aspect); Vox3D.moon(th, aspect);
        ground(th);
        F.ground = new Mesh(false);
        switch (boss) {
            case 0 -> revenant(t, p, energy, th);
            case 1 -> tarantula(t, p, energy, th);
            case 2 -> sven(t, p, energy, th);
            case 3 -> voidgloom(t, p, energy, th);
            case 4 -> inferno(t, p, energy, th);
            default -> riftstalker(t, p, energy, th);
        }
        if (p > 0 && type > 0) rise(t, p, A, energy, pin);
        if (type == 0 && p > -0.4f) dust(t - last, pin);
        Vox3D.weather(t, boss);
        return F;
    }
    static float kick(float t) {
        float s = 0f;
        for (int i = 0; i < n; i++) { float d = t - hits[i]; if (d < 0) continue; s += (i == n - 1 ? 1f : 0.45f) * (float) Math.exp(-d * 10f); }
        return s;
    }

    // ------------------------------------------------------------------ the ground: soft, fading into the dark
    static void ground(Theme th) {
        int R = 7;
        for (int i = -R; i <= R; i++) for (int j = -R; j <= R; j++) {
            float d = (float) Math.sqrt(i * i + j * j * 1.3f); if (d > R) continue;
            float fade = 1f - sm((d - 4.2f) / 2.8f);
            for (int a = 0; a < 2; a++) for (int b = 0; b < 2; b++) {
                float nz = rnd(i * 977 + j * 131 + a * 7 + b * 13);
                int base = rnd(i * 5 + j * 11) < (boss == 2 ? 0.8f : boss == 0 ? 0.3f : 0f) ? th.top2() : th.top();
                int c = argb(fade, mixc(mul(base, 0.72f + nz * 0.26f), 0x101830, 0.25f + 0.4f * (1f - fade)));
                float x0 = i - 0.5f + a * 0.5f, z0 = j - 0.5f + b * 0.5f;
                wquad(F.veil, x0, 0f, z0, c, x0, 0f, z0 + 0.5f, c, x0 + 0.5f, 0f, z0 + 0.5f, c, x0 + 0.5f, 0f, z0, c);
            }
        }
    }

    // ------------------------------------------------------------------ strike effects
    /** Bits thrown off at a strike: pixel cubes of the given colours, sparks of the strike's colour, a flash. */
    static void burst(float t, Vector3f at, float dirX, int[] cols, int energy, float scale) {
        for (int i = 0; i < n; i++) {
            float d = t - hits[i]; if (d < 0 || d > 1.0f) continue;
            boolean big = i == n - 1; float s = big ? 1.5f * scale : scale; int sc = sparkCol(i, energy);
            float fl = Math.max(0f, 1f - d / 0.18f);
            if (fl > 0) { glow(F.add, at.x, at.y, at.z, (big ? 1.6f : 0.9f) * s, 0xFFFFFF, 0.6f * fl); glow(F.add, at.x, at.y, at.z, (big ? 2.6f : 1.4f) * s, sc, 0.35f * fl); }
            for (int q = 0; q < (big ? 14 : 7); q++) {
                float an = rnd(i * 37 + q) * 3.1416f - 1.57f + (dirX > 0 ? 0 : 3.1416f), v = (1.0f + 1.8f * rnd(i * 13 + q)) * s, vy = 2f + 2.6f * rnd(i * 19 + q);
                float px = at.x + (float) Math.cos(an) * v * d, pz = at.z + (float) Math.sin(an) * v * d * 0.6f, py = at.y + vy * d - 9.8f * d * d * 0.5f;
                if (py < 0.04f) py = 0.04f;
                float k = (0.05f + 0.04f * rnd(q * 3 + i)) * (1f - sm((d - 0.6f) / 0.4f));
                push(); M.translate(px, py, pz).rotateXYZ(d * 7 + q, d * 5, q); box(-k, -k, -k, k, k, k, flat(cols[q % cols.length]), 1f, 0.015f); pop();
            }
            for (int q = 0; q < (big ? 12 : 6); q++) {
                float an = rnd(i * 41 + q) * 6.2832f, v = 1.5f + 2.5f * rnd(i * 23 + q), vy = 1.5f + 2.5f * rnd(i * 29 + q), tt = Math.min(d, 0.6f);
                glow(F.add, at.x + (float) Math.cos(an) * v * tt, at.y + vy * tt - 4.9f * tt * tt, at.z + (float) Math.sin(an) * v * tt * 0.6f, 0.05f, q % 2 == 0 ? 0xFFF0C0 : sc, 1f - tt / 0.6f);
            }
        }
    }
    /** No drops: a grey puff where the drop would have been. */
    static void dust(float d, Vector3f at) {
        if (d < 0.25f || d > 2.2f) return; float u = (d - 0.25f) / 1.95f;
        for (int i = 0; i < 14; i++) {
            float an = i * 0.45f, r = (0.3f + u * 1.4f) * (0.6f + rnd(i) * 0.6f);
            glow(F.veil, at.x + (float) Math.cos(an) * r, at.y + 0.2f + u * 0.9f * rnd(i * 3), at.z + (float) Math.sin(an) * r * 0.6f, 0.35f + u * 0.5f, 0x8A8A96, 0.35f * (1f - u));
        }
    }
    /** The drop's light: a beam of its colour, or RNG's gold pillar with rings, glints, rays and confetti. */
    static void rise(float t, float p, float A, int energy, Vector3f at) {
        float x = at.x, z = at.z, y0 = at.y, beam = sm(p / 0.25f) * (1f - sm((t - (A - 0.8f)) / 0.6f));
        if (beam < 0.01f) return;
        if (type == 2) {
            float pil = beam * (0.75f + 0.25f * (float) Math.sin(t * 9f));
            column(F.add, x, y0, z, y0 + 10f, 0.9f * pil, 0xFFD24A, 0.4f * pil); column(F.add, x, y0, z, y0 + 10f, 0.3f * pil, 0xFFF4C8, 0.85f * pil);
            for (int k = 0; k < 3; k++) { float u = ((p * 0.8f + k / 3f) % 1f); ring(F.add, x, 0.03f, z, 0.4f + 3f * u, 0.16f, 0xFFD24A, (1f - u) * 0.8f * beam); }
            for (int k = 0; k < 10; k++) { float an = t * 0.9f + k * 0.628f, rr = 1.0f + 0.2f * (float) Math.sin(t * 3 + k); glint(F.add, x + (float) Math.cos(an) * rr, y0 + 1.2f + (float) Math.sin(an * 2) * 0.3f, z + (float) Math.sin(an) * rr, k % 3 == 0 ? 0.36f : 0.22f, k % 2 == 0 ? 0xFFF4C8 : 0xFFD24A, beam); }
            for (int k = 0; k < 44; k++) {
                float u = ((p * 0.35f + rnd(k * 7)) % 1f), cx = x + (rnd(k * 13) - 0.5f) * 5f, cz = z + (rnd(k * 19) - 0.5f) * 3f, cy = 4.5f - u * 4.8f;
                int cc = k % 4 == 0 ? 0xFF7AD9 : k % 4 == 1 ? 0x6AB8FF : k % 4 == 2 ? 0xFFE04A : 0x7AE88A;
                push(); M.translate(cx + (float) Math.sin(t * 3 + k) * 0.2f, cy, cz).rotateXYZ(t * 4 + k, t * 3, k); float s = 0.045f; box(-s, -s * 0.3f, -s, s, s * 0.3f, s, flat(cc), 1f, 0f); pop();
            }
            for (int k = 0; k < 12; k++) {
                float an = k * 0.5236f + t * 0.25f, len = 3.6f + 1.2f * (float) Math.sin(t * 2 + k), ax = (float) Math.cos(an), ay = (float) Math.sin(an), cy = y0 + 1.2f, w = 0.15f;
                float ex = x + (right.x * ax + up.x * ay) * len, ey = cy + (right.y * ax + up.y * ay) * len, ez = z + (right.z * ax + up.z * ay) * len;
                float qx = (right.x * -ay + up.x * ax) * w, qy = (right.y * -ay + up.y * ax) * w, qz = (right.z * -ay + up.z * ax) * w;
                int c0 = argb(0.2f * beam, 0xFFE9A0), c1 = argb(0, 0xFFE9A0);
                wquad(F.add, x, cy, z, c0, x, cy, z, c0, ex + qx, ey + qy, ez + qz, c1, ex - qx, ey - qy, ez - qz, c1);
            }
        } else {
            column(F.add, x, y0, z, y0 + 7f, 0.5f * beam, energy, 0.35f * beam); column(F.add, x, y0, z, y0 + 7f, 0.16f * beam, mixc(energy, 0xFFFFFF, 0.55f), 0.75f * beam);
            for (int k = 0; k < 14; k++) { float u = ((p * 0.6f + rnd(k * 7)) % 1f); glow(F.add, x + (rnd(k * 5) - 0.5f) * 0.8f, y0 + u * 3f, z + (rnd(k * 9) - 0.5f) * 0.8f, 0.05f, k % 2 == 0 ? 0xFFFFFF : energy, (1f - u) * beam); }
        }
        glow(F.add, x, y0 + 0.2f, z, 1.4f, type == 2 ? 0xFFD24A : energy, 0.35f * beam);
    }

    // ------------------------------------------------------------------ meteors: one falls onto the target on every beat
    /** Each strike is a meteor: a burning outlined rock with a long trail, falling from the upper right onto target(i) and landing on hits[i]. */
    static void meteors(float t, java.util.function.IntFunction<Vector3f> target, float spread, int energy) {
        for (int i = 0; i < n; i++) {
            boolean big = type == 0 || i == n - 1; float h = hits[i], fl = SlayerScene.flight(type, i), t0 = h - fl;
            if (t < t0) continue;
            Vector3f a = target.apply(i); if (!big) a.add((rnd(i * 31 + 3) - 0.5f) * spread, 0f, (rnd(i * 17 + 5) - 0.5f) * spread);
            float k = big ? (type == 2 ? 1.4f : 1.15f) : 0.8f;
            Vector3f s0 = new Vector3f(a.x + (3.2f + 1.2f * rnd(i * 7 + 1)) * k, a.y + (7.5f + 2f * rnd(i * 5)) * k, a.z - (2.2f + 1.2f * rnd(i * 3)) * k);
            int col = type == 2 ? (big ? 0xFFD24A : SlayerScene.TIER[Math.min(i, SlayerScene.TIER.length - 1)]) : big ? 0xFFB040 : 0xFF8A30;
            if (t < h) {
                float u = (t - t0) / fl;
                for (int q = 16; q >= 1; q--) {
                    float uu = u - q * 0.035f; if (uu < 0) continue; float e = uu * uu * (0.55f + 0.45f * uu);
                    float px = lerp(s0.x, a.x, e), py = lerp(s0.y, a.y, e), pz = lerp(s0.z, a.z, e), f = 1f - q / 17f;
                    int tc = type == 2 && big ? Vox3D.rainbow(q / 17f + u) : mixc(col, 0xFF3A10, 1f - f);
                    glow(F.add, px, py, pz, (0.18f + 0.5f * f) * k, tc, 0.75f * f); if (q % 2 == 0) glow(F.add, px, py, pz, 0.08f * k, 0xFFF4D0, 0.8f * f);
                }
                float e = u * u * (0.55f + 0.45f * u), px = lerp(s0.x, a.x, e), py = lerp(s0.y, a.y, e), pz = lerp(s0.z, a.z, e);
                glow(F.add, px, py, pz, 1.1f * k, col, 0.45f);
                float vis = sm(edge(px, py, pz) * 1.8f);
                if (vis > 0.05f) { push(); M.translate(px, py, pz).rotateXYZ(t * 7f + i, t * 5f, i); float r = 0.2f * k * vis; box(-r, -r, -r, r, r, r, type == 2 && big ? Vox3D.GOLD_ROCK : Vox3D.ROCK, 24f, 0.025f); pop(); }
                for (int q = 0; q < 5; q++) { float v = ((u * 3f + q / 5f) % 1f); glow(F.add, px + v * 0.6f * k + (rnd(i * 11 + q) - 0.5f) * 0.3f, py + v * 1.4f * k, pz - v * 0.4f, 0.05f, q % 2 == 0 ? 0xFFE07A : col, (1f - v)); }
                glow(F.add, a.x, Math.max(0.03f, a.y - 0.05f), a.z, 0.9f * k * u, col, 0.3f * u);
            } else {
                float d = t - h; if (d > 1.2f) continue;
                float rw = d / 0.5f; if (rw < 1f) ring(F.add, a.x, 0.03f, a.z, (0.25f + 2.2f * eo(rw)) * k, 0.16f * k, mixc(col, 0xFFFFFF, 0.4f), 0.85f * (1f - rw));
                float fl2 = Math.max(0f, 1f - d / 0.22f); if (fl2 > 0) glow(F.add, a.x, a.y + 0.2f, a.z, 2.2f * k, col, 0.45f * fl2);
                for (int q = 0; q < 4; q++) { float u = ((d * 0.7f + q / 4f) % 1f); glow(F.veil, a.x + (rnd(i * 5 + q) - 0.5f) * 0.5f, a.y + 0.2f + u * 1.6f, a.z, 0.25f + u * 0.4f, 0x6A6A76, (1f - u) * 0.28f * (1f - d / 1.2f)); }
            }
        }
    }

    // ------------------------------------------------------------------ the six scenes
    static void revenant(float t, float p, int energy, Theme th) {
        Skin stone = (f, u, v, w, h) -> {
            float nz = rnd(f * 131 + u * 17 + v * 31 + 5); int c = nz < 0.22f ? 0x6E8A5A : 0x8A918A;
            if (f == 4 && w >= 12 && h >= 10) { int ry = h - 4 - v, rx = u - 1; if (ry >= 0 && ry < 6 && rx >= 0 && rx < 12 && rx + 16 * ry < RIP.length() && RIP.charAt(rx + 16 * ry) == 'X') c = 0x3A3E3A; }
            return mul(c, 0.9f + nz * 0.2f);
        };
        // the headstone, side graves, crosses, a lantern
        push(); M.translate(0, 0, -1.25f); box(-0.7f, 0f, -0.16f, 0.7f, 1.35f, 0.16f, stone, 12f, 0.03f); box(-0.5f, 1.35f, -0.16f, 0.5f, 1.58f, 0.16f, stone, 12f, 0.03f); pop();
        float[][] g = {{-2.4f, -1.9f, 0.35f}, {2.5f, -2.1f, -0.3f}, {-3.3f, 0.2f, 0.15f}};
        for (float[] q : g) { push(); M.translate(q[0], 0, q[1]).rotateY(q[2]); box(-0.3f, 0f, -0.09f, 0.3f, 0.65f, 0.09f, stone, 10f, 0.025f); box(-0.2f, 0.65f, -0.09f, 0.2f, 0.76f, 0.09f, stone, 10f, 0.025f); pop(); }
        Skin dark = noisy(0x4A321E, 0.14f, 9);
        for (float[] c : new float[][]{{-1.5f, -2.6f, 0.2f}, {1.6f, -2.8f, -0.25f}}) { push(); M.translate(c[0], 0, c[1]).rotateY(c[2]); box(-0.06f, 0f, -0.06f, 0.06f, 0.95f, 0.06f, dark, 10f, 0.02f); box(-0.28f, 0.6f, -0.05f, 0.28f, 0.72f, 0.05f, dark, 10f, 0.02f); pop(); }
        push(); M.translate(-1.7f, 0, 0.9f); box(-0.05f, 0f, -0.05f, 0.05f, 1.1f, 0.05f, flat(0x2A2A30), 4f, 0.02f); box(-0.14f, 1.1f, -0.14f, 0.14f, 1.38f, 0.14f, (f, u, v, w, h) -> u == 0 || u == w - 1 || v == 0 || v == h - 1 ? 0x2A2A30 : 0xFFD890, 6f, 0.02f); pop();
        glow(F.add, -1.7f, 1.24f, 0.9f, 0.7f, 0xFFB050, 0.3f + 0.04f * (float) Math.sin(t * 7));
        // the grave: a mound over the coffin, dug away strike by strike
        int k = done(t); float dug = k / (float) n, sinceS = since(t);
        float bump = sinceS < 0.15f ? (1f - sinceS / 0.15f) * 0.04f : 0f;
        float mh = 0.32f * (1f - dug) - bump;
        if (mh > 0.02f) { push(); M.translate(0, 0, 0.35f); box(-0.55f, 0f, -0.95f, 0.55f, mh, 0.95f, noisy(0x6A4A2C, 0.16f, 3), 10f, 0.025f); pop(); }
        // the coffin under it: dark wood, a gold cross; the lid flips open at the pivot (no drops: it lifts and drops back)
        Skin coffin = (f, u, v, w, h) -> { if (f == 2 && (u == w / 2 || v == h * 2 / 3) && u > 2 && u < w - 3 && v > 3 && v < h - 3) return 0xE8C050; return mul(0x3A2418, 0.85f + rnd(u * 7 + v * 3 + f) * 0.25f); };
        push(); M.translate(0, 0, 0.35f); box(-0.45f, -0.02f, -0.85f, 0.45f, 0.14f, 0.85f, coffin, 12f, 0.02f, 63 & ~4); pop();
        float open = type == 0 ? 0.35f * (float) Math.sin(Math.PI * cl((p + 0.15f) / 0.5f)) : eo((p + 0.1f) / 0.35f) * 2.1f;
        if (dug > 0.99f || open > 0.001f) { push(); M.translate(0.45f, 0.14f, 0.35f).rotateZ(open).translate(-0.45f, 0, 0); box(-0.45f, 0f, -0.85f, 0.45f, 0.08f, 0.85f, coffin, 12f, 0.02f); pop(); }
        if (type > 0 && p > -0.1f) glow(F.add, 0, 0.3f, 0.35f, 1.4f, energy, 0.45f * sm((p + 0.1f) / 0.3f));
        // the shovel digs at the near end of the grave
        meteors(t, i -> new Vector3f(0.05f, 0.25f, 0.35f), 0.35f, energy);
        burst(t, new Vector3f(0.05f, 0.25f, 0.35f), 1f, new int[]{0x6A4A2C, 0x4E3620, 0x7A5A34}, energy, 1f);
    }

    static void tarantula(float t, float p, int energy, Theme th) {
        Vox3D.webs(t);
        Skin rock = noisy(0x6E6A72, 0.18f, 41);
        push(); M.translate(-1.9f, 0, -1.1f); box(-0.45f, 0f, -0.45f, 0.45f, 0.8f, 0.45f, rock, 6f, 0.03f); pop();
        // the cocoon on its thread, swinging on every slash, splitting at the pivot
        float k = done(t) / (float) n, s0 = since(t), swingA = s0 < 1.2f ? 0.22f * (float) Math.exp(-s0 * 3.5f) * (float) Math.sin(s0 * 11f) : 0f;
        float split = type == 0 ? 0.12f * (float) Math.sin(Math.PI * cl((p + 0.15f) / 0.5f)) : eo((p + 0.08f) / 0.4f);
        column(F.veil, 0f, 2.3f, 0f, 6f, 0.015f, 0xE8ECF8, 0.8f);
        final float crack = k;
        Skin silk = (f, u, v, w, h) -> {
            float nz = rnd(f * 31 + u * 7 + v * 13 + 5);
            if ((v + u / 3) % 4 == 0) return mul(0xC8C8D0, 0.95f);
            if (crack > 0 && f == 4 && Math.abs(u - w / 2) <= 0 && rnd(v * 7) < crack) return 0x2A1A1A;
            return mul(0xECECF2, 0.86f + nz * 0.18f);
        };
        int[][] rows = {{4, 0}, {6, 2}, {8, 4}, {9, 7}, {9, 10}, {8, 13}, {6, 15}, {4, 17}};       // width, bottom (px), each 3 px tall
        for (int half = -1; half <= 1; half += 2) {
            push(); M.translate(0, 2.3f, 0).rotateZ(swingA).translate(0, -2.3f, 0).translate(half * split * 0.9f, 0.8f - split * 0.3f, 0).rotateZ(-half * split * 0.9f).scale(1 / 16f * 1.6f);
            for (int[] r : rows) { float hw = r[0] / 2f; box(half < 0 ? -hw : 0f, r[1], -hw, half < 0 ? 0f : hw, r[1] + 3f, hw, silk, 1f, 0.35f); }
            pop();
        }
        if (type > 0 && p > -0.1f) glow(F.add, 0, 1.55f, 0, 1.2f, energy, 0.5f * sm((p + 0.1f) / 0.3f));
        // the sword slashes across it
        meteors(t, i -> new Vector3f(0f, 1.5f, 0f), 0.15f, energy);
        burst(t, new Vector3f(0.1f, 1.55f, 0.4f), 1f, new int[]{0xECECF2, 0xC8C8D0, 0x3A2E28}, energy, 0.8f);
    }

    static void sven(float t, float p, int energy, Theme th) {
        Vox3D.tree(-2.3f, -1.2f, 1.7f, 11); Vox3D.tree(2.4f, -1.5f, 1.9f, 23);
        for (int i = 0; i < 3; i++) {                                                               // wolves' eyes in the dark between the trees
            float ex = -1.2f + i * 1.3f, ez = -3.4f - (i % 2) * 0.6f, blink = ((int) (t * 1.7f + i * 7)) % 9 == 0 ? 0f : 1f;
            glow(F.add, ex - 0.09f, 0.55f, ez, 0.12f, 0xFF3030, 0.9f * blink); glow(F.add, ex + 0.09f, 0.55f, ez, 0.12f, 0xFF3030, 0.9f * blink);
        }
        // a cairn of stones and bones, broken from the top down
        int k = done(t);
        float[][] st = {{0, 0, 0, 1.3f, 0.5f, 1.0f}, {-0.1f, 0.5f, 0.05f, 1.0f, 0.4f, 0.8f}, {0.05f, 0.9f, 0f, 0.8f, 0.35f, 0.7f}, {0f, 1.25f, 0.05f, 0.55f, 0.3f, 0.5f}, {0.02f, 1.55f, 0f, 0.35f, 0.25f, 0.35f}};
        Skin rock = noisy(0x7A7A82, 0.16f, 5);
        int left = Math.max(0, st.length - (int) Math.ceil(k * st.length / (float) n));
        for (int i = 0; i < st.length; i++) {
            float[] s = st[i]; boolean gone = i >= left;
            if (gone) {
                float d = t - hits[Math.min(n - 1, (int) ((st.length - 1 - i) * n / (float) st.length))]; if (d > 1.2f || d < 0) continue;
                push(); M.translate(s[0] + d * 1.6f * (i % 2 == 0 ? 1 : -1), s[1] + s[4] / 2 + 2.5f * d - 4.9f * d * d, s[2] + d * 0.6f).rotateXYZ(d * 5, d * 3, d * 2).scale(1f - sm((d - 0.7f) / 0.5f));
                box(-s[3] / 2, -s[4] / 2, -s[5] / 2, s[3] / 2, s[4] / 2, s[5] / 2, rock, 8f, 0.025f); pop();
            } else { push(); M.translate(s[0], s[1], s[2]); box(-s[3] / 2, 0, -s[5] / 2, s[3] / 2, s[4], s[5] / 2, rock, 8f, 0.025f); pop(); }
        }
        push(); M.translate(-0.8f, 0.06f, 0.6f).rotateY(0.6f); box(-0.35f, -0.04f, -0.04f, 0.35f, 0.04f, 0.04f, flat(0xE8E4D0), 8f, 0.015f); box(-0.4f, -0.07f, -0.07f, -0.3f, 0.07f, 0.07f, flat(0xE8E4D0), 8f, 0.015f); box(0.3f, -0.07f, -0.07f, 0.4f, 0.07f, 0.07f, flat(0xE8E4D0), 8f, 0.015f); pop();
        if (type > 0 && p > -0.1f) glow(F.add, 0, 0.9f, 0, 1.3f, energy, 0.5f * sm((p + 0.1f) / 0.3f));
        int top = Math.max(0, left - 1); float hy = st[top][1] + st[top][4];
        meteors(t, i -> { int lft = Math.max(0, st.length - (int) Math.ceil((i + 1) * st.length / (float) n)); int tp = Math.min(st.length - 1, lft); return new Vector3f(st[tp][0], st[tp][1] + st[tp][4] * 0.5f, st[tp][2]); }, 0.05f, energy);
        burst(t, new Vector3f(0.2f, hy, 0.3f), 1f, new int[]{0x7A7A82, 0x5A5A62, 0xE8E4D0}, energy, 1f);
    }

    static void voidgloom(float t, float p, int energy, Theme th) {
        Skin obs = (f, u, v, w, h) -> { float nz = rnd(f * 31 + u * 7 + v * 13); return nz < 0.15f ? 0x3A1E5A : mul(0x140C1E, 0.8f + nz * 0.4f); };
        for (float[] pl : new float[][]{{-2.3f, -1.3f, 1.7f}, {2.4f, -1.5f, 1.4f}}) {
            push(); M.translate(pl[0], 0, pl[1]); box(-0.45f, 0f, -0.45f, 0.45f, pl[2], 0.45f, obs, 6f, 0.03f); box(-0.18f, pl[2], -0.18f, 0.18f, pl[2] + 0.35f, 0.18f, (f, u, v, w, h) -> 0x02E070FF, 4f, 0.025f); pop();
        }
        // a ring of eight portal frames; an eye flies into one on every strike (the last strike fills the rest)
        int k = done(t); int filled = k >= n ? 8 : Math.min(7, k);
        Skin frame = (f, u, v, w, h) -> f == 2 ? (u > 0 && u < w - 1 && v > 0 && v < h - 1 ? 0x2A5A4A : 0x7AE8C8) : mul(0xC8D4A0, 0.85f + rnd(u * 7 + v) * 0.2f);
        Skin eyeS = (f, u, v, w, h) -> (u == 1 || u == 2) && (v == 1 || v == 2) ? 0x02102018 : 0x02287A5A;
        for (int i = 0; i < 8; i++) {
            double an = i * Math.PI / 4 + Math.PI / 8; float fx = (float) Math.cos(an) * 1.15f, fz = (float) Math.sin(an) * 1.15f;
            push(); M.translate(fx, 0, fz).rotateY((float) -an); box(-0.3f, 0f, -0.3f, 0.3f, 0.36f, 0.3f, frame, 8f, 0.02f); pop();
            int hitI = i < 7 ? i : n - 1; if (type == 0 && i > 0) continue;
            float landT = hits[Math.min(hitI, n - 1)]; if (i >= 7 && k < n) continue; if (i < 7 && i >= n - 1 && type != 0 && i > 0 && i >= n - 1) landT = last;
            if (i < filled || (t > landT - 0.35f && t < landT)) {
                if (t >= landT) { push(); M.translate(fx, 0.42f, fz).rotateY(t * 3 + i); box(-0.11f, -0.07f, -0.11f, 0.11f, 0.07f, 0.11f, eyeS, 18f, 0.015f); pop(); }
            }
        }
        // the portal: dark until it opens, then a starry void
        float open = type == 0 ? 0.6f * (float) Math.sin(Math.PI * cl((p + 0.1f) / 0.6f)) : sm((p + 0.1f) / 0.3f);
        disc(F.veil, 0, 0.012f, 0, 1.0f, 0x05030A, 0.9f);
        if (open > 0.01f) {
            disc(F.add, 0, 0.02f, 0, 0.95f, 0x2A6A6A, 0.6f * open);
            for (int i = 0; i < 24; i++) { float an = i * 2.4f + t * 0.8f, r = 0.8f * (float) Math.sqrt(rnd(i * 7)); glow(F.add, (float) Math.cos(an) * r, 0.03f, (float) Math.sin(an) * r, 0.05f, i % 3 == 0 ? 0xE070FF : 0xC8FFF0, open); }
        }
        if (type > 0 && p > -0.1f) glow(F.add, 0, 0.4f, 0, 1.3f, energy, 0.5f * sm((p + 0.1f) / 0.3f));
        meteors(t, i -> { double an = Math.min(i, 7) * Math.PI / 4 + Math.PI / 8; return i == n - 1 && type > 0 ? new Vector3f(0f, 0.1f, 0f) : new Vector3f((float) Math.cos(an) * 1.15f, 0.42f, (float) Math.sin(an) * 1.15f); }, 0f, energy);
        for (int i = 0; i < n; i++) { float d = t - hits[i]; if (d >= 0 && d < 0.3f) { float an = (float) (Math.min(i, 7) * Math.PI / 4 + Math.PI / 8); glow(F.add, (float) Math.cos(an) * 1.15f, 0.45f, (float) Math.sin(an) * 1.15f, 0.9f, sparkCol(i, energy), 0.7f * (1f - d / 0.3f)); } }
    }

    static void inferno(float t, float p, int energy, Theme th) {
        Skin brick = (f, u, v, w, h) -> { boolean mortar = v % 3 == 0 || (u + (v / 3) * 2) % 5 == 0; return mortar ? 0x1E0E10 : mul(0x4A1A1E, 0.85f + rnd(u * 7 + v * 13 + f) * 0.3f); };
        for (float[] pl : new float[][]{{-2.1f, -1.2f, 1.6f}, {2.2f, -1.4f, 1.8f}}) { push(); M.translate(pl[0], 0, pl[1]); box(-0.4f, 0f, -0.4f, 0.4f, pl[2], 0.4f, brick, 8f, 0.03f); pop(); Vox3D.fire(t, pl[0], pl[2], pl[1], 0.9f); }
        // the brazier and the magma core on it, cracking brighter with every strike
        push(); box(-0.55f, 0f, -0.55f, 0.55f, 0.5f, 0.55f, brick, 8f, 0.03f); box(-0.7f, 0.5f, -0.7f, 0.7f, 0.65f, 0.7f, brick, 8f, 0.03f); pop();
        final float heat = done(t) / (float) n; float s0 = since(t), jig = s0 < 0.2f ? (1f - s0 / 0.2f) * 0.05f : 0f;
        float erupt = type == 0 ? 0f : eo((p + 0.05f) / 0.35f);
        if (erupt < 0.98f) {
            Skin core = (f, u, v, w, h) -> { float nz = rnd(f * 31 + u * 7 + v * 13 + 3); if (nz < 0.12f + heat * 0.35f) return 0x02FFB040; return nz < 0.3f ? 0x8A2A10 : mul(0x3A1A14, 0.8f + nz * 0.3f); };
            push(); M.translate(0, 1.2f + jig, 0).rotateY(t * 0.4f).scale(1f - erupt * 0.3f); box(-0.45f, -0.45f, -0.45f, 0.45f, 0.45f, 0.45f, core, 10f, 0.03f); pop();
        }
        glow(F.add, 0, 1.2f, 0, 1.4f + heat * 1.2f, 0xFF7A20, 0.25f + heat * 0.3f);
        if (erupt > 0.01f) for (int i = 0; i < 16; i++) {
            float an = rnd(i * 13) * 6.2832f, d = erupt * 1.4f; push(); M.translate((float) Math.cos(an) * d, 1.2f + d * (0.5f + rnd(i) ), (float) Math.sin(an) * d * 0.7f).rotateXYZ(d * 4 + i, d, i).scale(1f - erupt * 0.6f);
            float k = 0.12f; box(-k, -k, -k, k, k, k, flat(i % 2 == 0 ? 0x3A1A14 : 0xFF8A20), 2f, 0.015f); pop();
        }
        if (type > 0 && p > -0.1f) glow(F.add, 0, 1.2f, 0, 1.6f, energy, 0.5f * sm((p + 0.1f) / 0.3f));
        meteors(t, i -> new Vector3f(0f, 1.25f, 0f), 0.1f, energy);
        burst(t, new Vector3f(0.25f, 1.45f, 0.3f), 1f, new int[]{0xFF8A20, 0x3A1A14, 0xFFD060}, energy, 1f);
    }

    static void riftstalker(float t, float p, int energy, Theme th) {
        Skin cry = (f, u, v, w, h) -> { float nz = rnd(f * 31 + u * 7 + v * 13 + 99); return nz < 0.2f ? 0x8A2AD8 : mul(0x1A0E26, 0.8f + nz * 0.4f); };
        for (float[] pl : new float[][]{{-2.1f, -1.3f, 1.3f}, {2.2f, -1.5f, 1.6f}}) { push(); M.translate(pl[0], 0, pl[1]); box(-0.45f, 0f, -0.45f, 0.45f, pl[2], 0.45f, cry, 6f, 0.03f); pop(); }
        // the standing coffin, chained; one chain breaks on each strike, the door bursts open at the pivot
        Skin wood = (f, u, v, w, h) -> { if (f == 4 && (u == w / 2 || v == h * 2 / 3) && u > 2 && u < w - 3 && v > 3 && v < h - 3) return 0xC01828; return mul(0x2A1418, 0.85f + rnd(u * 7 + v * 3 + f) * 0.25f); };
        push(); M.translate(0, 0, -0.3f); box(-0.5f, 0f, -0.25f, 0.5f, 2.0f, 0.2f, wood, 12f, 0.03f); pop();
        float open = type == 0 ? 0.3f * (float) Math.sin(Math.PI * cl((p + 0.15f) / 0.5f)) : eo((p + 0.1f) / 0.35f) * 1.9f;
        push(); M.translate(-0.5f, 0, -0.08f).rotateY(-open).translate(0.5f, 0, 0); box(-0.5f, 0.02f, 0f, 0.5f, 1.98f, 0.1f, wood, 12f, 0.03f); pop();
        if (open > 0.05f) { glow(F.add, 0, 1.0f, -0.1f, 1.3f, 0xFF2040, 0.45f * Math.min(1f, open)); }
        int k = done(t); Skin chain = (f, u, v, w, h) -> (u + v) % 2 == 0 ? 0x8A8A96 : 0x5A5A66;
        float[] cy = {0.45f, 0.95f, 1.45f};
        for (int i = 0; i < 3; i++) {
            int breakAt = Math.min(n - 1, (int) ((i + 1) * n / 3f) - 1); float d = t - hits[breakAt];
            if (d < 0) { push(); M.translate(0, cy[i], 0.15f).rotateZ(0.12f * (i - 1)); box(-0.62f, -0.04f, -0.04f, 0.62f, 0.04f, 0.04f, chain, 16f, 0.015f); pop(); }
            else if (d < 1.0f) for (int sd = -1; sd <= 1; sd += 2) { push(); M.translate(sd * (0.3f + d * 1.4f), cy[i] + 1.5f * d - 4.9f * d * d, 0.15f + d * 0.5f).rotateZ(sd * d * 4); box(-0.3f, -0.04f, -0.04f, 0.3f, 0.04f, 0.04f, chain, 16f, 0.015f); pop(); }
        }
        if (type > 0 && p > 0) for (int i = 0; i < 8; i++) {                                         // bats burst out
            float d = p * (0.8f + rnd(i) * 0.4f), an = rnd(i * 7) * 3.1f - 1.55f; if (d > 1.6f) continue;
            float bx = (float) Math.sin(an) * d * 2.2f, by = 1f + d * 1.6f + (float) Math.sin(t * 20 + i) * 0.05f, bz = d * 1.2f, flap = (float) Math.sin(t * 30 + i) * 0.6f;
            push(); M.translate(bx, by, bz).rotateY(an); box(-0.06f, -0.05f, -0.06f, 0.06f, 0.05f, 0.06f, flat(0x1A1218), 2f, 0.012f);
            for (int sd = -1; sd <= 1; sd += 2) { push(); M.rotateZ(sd * flap); box(sd > 0 ? 0.06f : -0.2f, -0.01f, -0.05f, sd > 0 ? 0.2f : -0.06f, 0.01f, 0.05f, flat(0x2A1A22), 2f, 0.01f); pop(); }
            pop();
        }
        if (type > 0 && p > -0.1f) glow(F.add, 0, 1.0f, 0.1f, 1.3f, energy, 0.45f * sm((p + 0.1f) / 0.3f));
        int nextChain = Math.min(2, k * 3 / Math.max(1, n));
        meteors(t, i -> new Vector3f(0.1f, cy[Math.min(2, i * 3 / Math.max(1, n))], 0.2f), 0.2f, energy);
        burst(t, new Vector3f(0.4f, cy[Math.min(2, nextChain)], 0.25f), 1f, new int[]{0x8A8A96, 0x2A1418, 0xC01828}, energy, 0.9f);
    }
}
