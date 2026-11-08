package dev.stardrops;

import static dev.stardrops.Ritual.*;
import static dev.stardrops.Vox3D.*;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * The Stardrops slayer scene: our own, in space. The boss stands on its own tiny planet (its ground in little blocks), far out among
 * nebulae. It is drawn into being as a constellation, walks while its planet turns under its feet, and falling stars strike it on every
 * beat (SlayerScene.HITS, so the sounds land with them). The last one shatters it into stardust; the dust gathers into a star where
 * your drop appears, and every strike is left in the sky as a star of the drop's constellation.
 *   No drops: one star strikes, the boss crumbles to grey dust that drifts away; the lone star flickers out.
 *   A drop: the strikes come in a run, the constellation joins up in the drop's colour round it.
 *   RNG: the stars climb through the rarity colours, the sky holds its breath, the last one is gold; a gold constellation, rays, confetti.
 */
final class Star3D {
    private Star3D() {}

    static final float FOV = 32f, PR = 1.0f;                     // the planet's radius
    static final Vector3f PIN = new Vector3f(0f, 1.3f, 0f);       // where the drop forms (pinned under the item on screen)
    static float[] hits; static float last; static int type, boss, n;
    static Mesh planetMesh; static int planetBoss = -1;

    static int done(float t) { int k = 0; for (float h : hits) if (t >= h) k++; return k; }
    static int starCol(int i, int energy) { return type == 2 ? (i == n - 1 ? 0xFFD24A : SlayerScene.TIER[Math.min(i, SlayerScene.TIER.length - 1)]) : type == 0 ? 0xDDE6FF : i == n - 1 ? mixc(energy, 0xFFFFFF, 0.3f) : 0xFFE9A0; }

    static Frame build(float t, float A, int bossIn, int typeIn, int energy, float aspect, float pinY, boolean z01) {
        boss = bossIn; type = typeIn; hits = SlayerScene.HITS[type]; n = hits.length; last = hits[n - 1];
        F = new Frame(); tint = 0f; M = new Matrix4f(); STACK.clear(); mobMode = false; pass = 0; dyn = false; fovNow = FOV;
        float O = AnimationManager.SL_PIVOT, p = t - O;

        // ---- the camera: low on the planet, looking at the boss with the curved horizon and space behind it; a slow push in, a kick on hits
        float kick = 0f; for (int i = 0; i < n; i++) { float d = t - hits[i]; if (d >= 0) kick += (i == n - 1 ? 1f : 0.4f) * (float) Math.exp(-d * 10f); }
        float roarK = roar(t);
        float dist = lerp(12.5f, 10f, eo(t / 1.6f)) - 0.6f * sm((t - SlayerScene.WALK) / 1.4f);
        if (type == 2) dist -= 1.2f * sm((t - 2.95f) / 0.42f) * (1f - sm((t - last) / 0.3f));
        dist += 0.7f * sm(p / 0.7f);
        float yaw = (float) Math.toRadians(12 + 5 * Math.sin(t * 0.3) - 10 * (1f - eo(t / 1.6f)));
        float eyeY = 2.3f + 0.4f * (1f - eo(t / 1.6f));
        Vector3f target = new Vector3f(0f, 1.35f, 0f);
        eye.set((float) Math.sin(yaw) * dist, eyeY, (float) Math.cos(yaw) * dist);
        float sh = Math.min(1.2f, kick + 0.35f * roarK), jx = 0.06f * sh * (float) (Math.sin(t * 53) + Math.sin(t * 31 + 1.3)), jy = 0.06f * sh * (float) (Math.sin(t * 47 + 2.1) + Math.sin(t * 37 + 0.4));
        Matrix4f view = new Matrix4f().lookAt(eye.x + jx, eye.y + jy, eye.z, target.x + jx * 0.5f, target.y + jy * 0.5f, target.z, 0, 1, 0);
        right.set(view.m00(), view.m10(), view.m20()); up.set(view.m01(), view.m11(), view.m21());
        Matrix4f persp = new Matrix4f().setPerspective((float) Math.toRadians(FOV), aspect, 1000f, 0.05f, z01);
        Vector4f cr = new Matrix4f(persp).mul(view).transform(new Vector4f(PIN.x, PIN.y, PIN.z, 1f));
        shiftX = -cr.x / cr.w; shiftY = pinY - cr.y / cr.w;
        Matrix4f proj = new Matrix4f().translation(shiftX, shiftY, 0f).mul(persp);
        projView = new Matrix4f(proj).mul(view); F.proj = proj; F.view = view;

        float dim = type == 2 ? sm((t - 2.95f) / 0.3f) * (1f - sm((t - last) / 0.25f)) : 0f;
        Theme th = THEMES[boss];
        space(t, aspect, energy, th, dim);

        // ---- the giant planet: its curved surface under the boss, the near ground in blocks (built once per boss)
        F.ground = new Mesh(false);
        nearGround(th, dim); farGround(th, dim);
        atmosphere(t, th, energy, dim);
        spawn(t, energy, th);

        // ---- the boss
        boss(t, p, energy, roarK);

        // ---- falling stars, and the constellation they leave
        for (int i = 0; i < n; i++) fallingStar(t, i, energy);
        constellation(t, p, A, energy);
        if (p > -0.2f && type > 0) gather(t, p, A, energy);
        return F;
    }
    /** The roar once it has risen: 0..1..0 over a little under a second from the build-up beat. */
    static float roar(float t) { float u = (t - SlayerScene.WALK) / 0.9f; return u <= 0 || u >= 1 ? 0f : (float) Math.sin(Math.PI * u) * (u < 0.3f ? sm(u / 0.3f) : 1f); }

    // ------------------------------------------------------------------ deep space above the planet
    static void space(float t, float aspect, int energy, Theme th, float dim) {
        sky(F.back, aspect, 400f, mixc(0x04051A, 0x000000, 0.5f * dim), mixc(mixc(0x0C0C2E, energy, 0.1f), 0x000000, 0.5f * dim), mixc(mixc(th.skyLow(), 0x1A1040, 0.5f), 0x000000, 0.4f * dim), 0.97f);
        for (int i = 0; i < 120; i++) {
            Vector3f q = onSky((rnd(i * 7 + 1) - 0.5f) * 2f, -0.1f + rnd(i * 13 + 2) * 1.1f, 80f, aspect);
            float tw = 0.5f + 0.5f * (float) Math.sin(t * (1.3f + rnd(i) * 2.4f) + i), big = rnd(i * 31 + 9);
            glow(F.sky, q.x, q.y, q.z, 0.1f + big * 0.28f, i % 7 == 0 ? 0xC8D8FF : i % 11 == 0 ? 0xFFD8B0 : 0xFFFFFF, (0.3f + 0.7f * big) * tw * (1f - 0.8f * dim));
            if (big > 0.94f) glint(F.sky, q.x, q.y, q.z, 0.9f, 0xFFFFFF, 0.55f * tw * (1f - 0.8f * dim));
        }
        int[] neb = {energy, 0x6A4AE8, 0x2A6AD8, 0xD84AA8};
        for (int i = 0; i < 9; i++) { Vector3f q = onSky((rnd(i * 17 + 5) - 0.5f) * 1.6f, 0.1f + rnd(i * 23 + 1) * 0.8f, 70f, aspect); glow(F.sky, q.x, q.y, q.z, 10f + rnd(i * 41) * 12f, neb[i % neb.length], (0.06f + rnd(i * 9) * 0.05f) * (1f - 0.6f * dim)); }
        // a moon of the planet, high on the left, lit from the side
        Vector3f mp = onSky(-0.42f, 0.5f, 60f, aspect); float mr = 2.6f;
        glow(F.sky, mp.x, mp.y, mp.z, mr * 2.4f, th.moon(), 0.12f);
        for (int r = 0; r < 8; r++) for (int k = 0; k < 28; k++) {
            float r0 = r / 8f, r1 = (r + 1) / 8f; double a0 = k * Math.PI * 2 / 28, a1 = (k + 1) * Math.PI * 2 / 28; float[] xs = new float[12]; int[] cs = new int[4];
            float[][] pt = {{r0, (float) a0}, {r0, (float) a1}, {r1, (float) a1}, {r1, (float) a0}};
            for (int q = 0; q < 4; q++) {
                float rr = pt[q][0], cx = (float) Math.cos(pt[q][1]) * rr, cy = (float) Math.sin(pt[q][1]) * rr, cz = (float) Math.sqrt(Math.max(0, 1 - rr * rr));
                float lit = Math.max(0f, -0.55f * cx + 0.35f * cy + 0.75f * cz); cs[q] = 0xFF000000 | mixc(0x1A1A2E, th.moon(), Math.min(1f, lit * 1.1f));
                xs[q * 3] = mp.x + (right.x * cx + up.x * cy) * mr; xs[q * 3 + 1] = mp.y + (right.y * cx + up.y * cy) * mr; xs[q * 3 + 2] = mp.z + (right.z * cx + up.z * cy) * mr;
            }
            wquad(F.veil, xs[0], xs[1], xs[2], cs[0], xs[3], xs[4], xs[5], cs[1], xs[6], xs[7], xs[8], cs[2], xs[9], xs[10], xs[11], cs[3]);
        }
        float per = 1.7f; int k = (int) (t / per); float u = (t - k * per) / 0.5f;
        if (u < 1f) { Vector3f a = onSky(-0.9f + rnd(k * 7) * 0.6f, 0.75f - rnd(k * 3) * 0.2f, 70f, aspect), b = onSky(-0.2f + rnd(k * 7) * 0.6f, 0.45f - rnd(k * 3) * 0.2f, 70f, aspect);
            for (int i = 0; i < 12; i++) { float uu = u - i * 0.03f; if (uu < 0) break; glow(F.sky, lerp(a.x, b.x, uu), lerp(a.y, b.y, uu), lerp(a.z, b.z, uu), 0.4f - i * 0.025f, 0xDDE6FF, (1f - i / 12f) * (1f - u)); } }
    }

    // ------------------------------------------------------------------ the giant planet
    static final float GR = 70f;                                                // its radius: big enough to stand on, small enough that the horizon curves
    static float surf(float x, float z) { float d2 = x * x + z * z; return (float) (Math.sqrt(Math.max(0, GR * GR - d2)) - GR); }
    static int groundCol(Theme th, int i, int j, int f, int u, int v, int h, int k) {
        float nz = rnd(i * 977 + j * 131 + k * 7 + f * 31 + u * 17 + v * 3);
        int base;
        if (f == 2) base = nz < 0.18f ? th.top2() : th.top();
        else if (k == 0 && v >= h - 1) base = th.top();
        else base = k == 0 ? th.dirt() : th.stone();
        if (boss == 4 && nz < 0.05f) base = 0xFF8A20;
        return mul(base, 0.86f + nz * 0.24f);
    }
    /** The ground near the boss: pixel-sized tiles of its ground on the planet's curve (soft at the window's edge, so no hard border). */
    static void nearGround(Theme th, float dim) {
        float c = 0.5f; int N = 16;
        for (int i = -N; i < N; i++) for (int j = -N; j < N; j++) {
            float x0 = i * c, z0 = j * c, x1 = x0 + c, z1 = z0 + c, mx = x0 + c / 2, mz = z0 + c / 2;
            if (mx * mx + mz * mz > 7.8f * 7.8f) continue;
            float nz = rnd(i * 977 + j * 131 + 7), blk = rnd(Math.floorDiv(i, 2) * 31 + Math.floorDiv(j, 2) * 17);
            int base = blk < 0.22f ? th.top2() : th.top(); if (boss == 4 && nz < 0.05f) base = 0xFF8A20;
            float d = (float) Math.sqrt(mx * mx + mz * mz);
            int col = argb(1f, mixc(mixc(mul(base, 0.8f + nz * 0.22f + (blk - 0.5f) * 0.1f), 0x1E2A50, 0.12f), 0x000000, 0.4f * dim + 0.02f * d));
            wquad(F.veil, x0, surf(x0, z0), z0, col, x0, surf(x0, z1), z1, col, x1, surf(x1, z1), z1, col, x1, surf(x1, z0), z0, col);
        }
    }
    /** The rest of the planet out to its horizon: a smooth curved surface, darker with distance, fading at the window's edge. */
    static void farGround(Theme th, float dim) {
        int rings = 36, seg = 96; float r0 = 7.8f, r1 = (float) Math.sqrt(2 * GR * 3.2f) * 1.6f;
        for (int r = 0; r < rings; r++) for (int k = 0; k < seg; k++) {
            float ra = lerp(r0, r1, (float) Math.pow(r / (float) rings, 1.7f)), rb = lerp(r0, r1, (float) Math.pow((r + 1) / (float) rings, 1.7f));
            double a0 = k * Math.PI * 2 / seg, a1 = (k + 1) * Math.PI * 2 / seg;
            float[][] pt = {{ra, (float) a0}, {ra, (float) a1}, {rb, (float) a1}, {rb, (float) a0}}; float[] xs = new float[12]; int[] cs = new int[4];
            for (int q = 0; q < 4; q++) {
                float x = (float) Math.cos(pt[q][1]) * pt[q][0], z = (float) Math.sin(pt[q][1]) * pt[q][0], y = surf(x, z) - 0.01f;
                float far = cl((pt[q][0] - r0) / (r1 - r0)), nz = rnd(Math.round(x * 0.7f) * 31 + Math.round(z * 0.7f) * 17);
                int c = mixc(mul(nz < 0.3f ? th.top2() : th.top(), 0.8f + nz * 0.2f), mixc(th.skyLow(), 0x0A0A1E, 0.4f), 0.15f + 0.7f * far);
                cs[q] = argb(1f, mixc(c, 0x000000, 0.4f * dim)); xs[q * 3] = x; xs[q * 3 + 1] = y; xs[q * 3 + 2] = z;
            }
            wquad(F.veil, xs[0], xs[1], xs[2], cs[0], xs[3], xs[4], xs[5], cs[1], xs[6], xs[7], xs[8], cs[2], xs[9], xs[10], xs[11], cs[3]);
        }
    }
    /** The planet's atmosphere: a glowing rim along the horizon as we see it, and a faint haze above it. */
    static void atmosphere(float t, Theme th, int energy, float dim) {
        float h = eye.y - surf(eye.x, eye.z), dh = (float) Math.sqrt(2 * GR * Math.max(0.5f, h));                  // how far the horizon is
        float base = (float) Math.atan2(-eye.x, -eye.z);
        int atmo = mixc(mixc(th.skyLow(), energy, 0.35f), 0x6A8AFF, 0.25f);
        for (int k = -40; k <= 40; k++) {
            float a = base + k * 0.03f, x = eye.x + (float) Math.sin(a) * dh, z = eye.z + (float) Math.cos(a) * dh, y = surf(x, z);
            glow(F.add, x, y + 0.6f, z, 3.2f, atmo, 0.14f * (1f - 0.6f * dim));
            glow(F.add, x, y + 0.05f, z, 0.6f, mixc(atmo, 0xFFFFFF, 0.5f), 0.2f * (1f - 0.6f * dim));
        }
    }

    // ------------------------------------------------------------------ where it rises from, and what stands round it
    static void spawn(float t, int energy, Theme th) {
        float rise = (t - SlayerScene.RISE) / 0.95f;
        switch (boss) {
            case 0 -> {                                                                           // its headstone behind it, a cross or two; the grave bursts open
                Skin stone = (f, u, v, w, hh) -> { float nz = rnd(f * 131 + u * 17 + v * 31 + 5); int c = nz < 0.22f ? 0x6E8A5A : 0x8A918A;
                    if (f == 4 && w >= 12 && hh >= 10) { int ry = hh - 4 - v, rx = u - 1; if (ry >= 0 && ry < 6 && rx >= 0 && rx < 12 && rx + 16 * ry < RIP.length() && RIP.charAt(rx + 16 * ry) == 'X') c = 0x3A3E3A; }
                    return mul(c, 0.9f + nz * 0.2f); };
                push(); M.translate(0.1f, surf(0, -1.5f), -1.5f); box(-0.7f, 0f, -0.16f, 0.7f, 1.35f, 0.16f, stone, 12f, 0.03f); box(-0.5f, 1.35f, -0.16f, 0.5f, 1.58f, 0.16f, stone, 12f, 0.03f); pop();
                Skin wood = noisy(0x4A321E, 0.14f, 9);
                for (float[] c : new float[][]{{-1.9f, -1.6f, 0.2f}, {2.0f, -2.0f, -0.25f}}) { push(); M.translate(c[0], surf(c[0], c[1]), c[1]).rotateY(c[2]); box(-0.06f, 0f, -0.06f, 0.06f, 0.95f, 0.06f, wood, 10f, 0.02f); box(-0.28f, 0.6f, -0.05f, 0.28f, 0.72f, 0.05f, wood, 10f, 0.02f); pop(); }
                for (float[] g : new float[][]{{1.7f, -0.6f, -0.3f}, {-1.4f, -2.8f, 0.3f}}) { push(); M.translate(g[0], surf(g[0], g[1]), g[1]).rotateY(g[2]); box(-0.3f, 0f, -0.09f, 0.3f, 0.62f, 0.09f, stone, 10f, 0.025f); pop(); }
                burstUp(t, rise, new int[]{0x6A4A2C, 0x4E3620, 0x7A5A34});
            }
            case 1 -> {                                                                           // a burrow, webs strung between rocks
                Skin rock = noisy(0x6E6A72, 0.18f, 41);
                for (float[] r : new float[][]{{-1.9f, -1.5f, 0.8f}, {2.0f, -1.3f, 0.6f}}) { push(); M.translate(r[0], surf(r[0], r[1]), r[1]); box(-r[2] * 0.6f, 0f, -r[2] * 0.6f, r[2] * 0.6f, r[2] * 1.4f, r[2] * 0.6f, rock, 6f, 0.03f); pop(); }
                disc(F.veil, 0, 0.03f, 0, 1.1f, 0x0A0808, 0.85f);
                int c = argb(0.45f, 0xE8ECF8);
                for (float[] w : new float[][]{{-1.9f, 1.3f, -1.5f, 1.0f}, {2.0f, 1.0f, -1.3f, 0.8f}}) for (int k = 0; k < 8; k++) { double an = k * Math.PI / 4; float ex = w[0] + (float) Math.cos(an) * w[3], ey = w[1] + (float) Math.sin(an) * w[3]; wquad(F.veil, w[0], w[1], w[2], c, ex, ey, w[2], c, ex + 0.025f, ey + 0.025f, w[2], c, w[0] + 0.025f, w[1] + 0.025f, w[2], c); }
                burstUp(t, rise, new int[]{0x6E6A66, 0x46423E, 0xE8ECF8});
            }
            case 2 -> {                                                                           // trees and a rocky den
                Vox3D.tree(-2.1f, -1.9f, 1.8f, 11); Vox3D.tree(2.2f, -2.3f, 2.0f, 23);
                Skin rock = noisy(0x7A7A82, 0.16f, 5);
                push(); M.translate(0, surf(0, -1.4f), -1.4f); box(-0.9f, 0f, -0.35f, -0.4f, 0.9f, 0.35f, rock, 6f, 0.03f); box(0.4f, 0f, -0.35f, 0.9f, 0.9f, 0.35f, rock, 6f, 0.03f); box(-0.9f, 0.9f, -0.35f, 0.9f, 1.3f, 0.35f, rock, 6f, 0.03f); pop();
                burstUp(t, rise, new int[]{0x5A3E26, 0x4E8A34, 0x7A7A82});
            }
            case 3 -> {                                                                           // obsidian pillars with crystals, purple light
                Skin obs = (f, u, v, w, hh) -> { float nz = rnd(f * 31 + u * 7 + v * 13); return nz < 0.15f ? 0x3A1E5A : mul(0x140C1E, 0.8f + nz * 0.4f); };
                for (float[] pl : new float[][]{{-2.0f, -1.7f, 1.9f}, {2.1f, -2.0f, 1.6f}}) {
                    float gy = surf(pl[0], pl[1]); push(); M.translate(pl[0], gy, pl[1]); box(-0.42f, 0f, -0.42f, 0.42f, pl[2], 0.42f, obs, 6f, 0.03f); box(-0.16f, pl[2], -0.16f, 0.16f, pl[2] + 0.32f, 0.16f, (f, u, v, w, hh) -> 0x02E070FF, 4f, 0.025f); pop();
                }
                for (int i = 0; i < 16; i++) { float u = ((t * 0.6f + i / 16f) % 1f), an = i * 0.9f + t * 1.8f; glow(F.add, (float) Math.cos(an) * (1f - u) * 1.1f, 0.1f + u * 3f, (float) Math.sin(an) * (1f - u) * 0.9f, 0.07f, 0xD060FF, (1f - u) * sm(rise * 3f)); }
            }
            case 4 -> {                                                                           // a lava pool it rises from, fire pillars
                Skin brick = (f, u, v, w, hh) -> { boolean mortar = v % 3 == 0 || (u + (v / 3) * 2) % 5 == 0; return mortar ? 0x1E0E10 : mul(0x4A1A1E, 0.85f + rnd(u * 7 + v * 13 + f) * 0.3f); };
                for (float[] pl : new float[][]{{-2.0f, -1.6f, 1.6f}, {2.1f, -1.9f, 1.8f}}) { float gy = surf(pl[0], pl[1]); push(); M.translate(pl[0], gy, pl[1]); box(-0.4f, 0f, -0.4f, 0.4f, pl[2], 0.4f, brick, 8f, 0.03f); pop(); Vox3D.fire(t, pl[0], gy + pl[2], pl[1], 0.9f); }
                float pulse = 0.5f + 0.5f * (float) Math.sin(t * 3f);
                disc(F.add, 0, 0.04f, 0, 1.5f, mixc(0xFF6A10, 0xFFC040, pulse), 0.8f); glow(F.add, 0, 0.5f, 0, 2.2f, 0xFF7A20, 0.3f);
                for (int i = 0; i < 18; i++) { float u = ((t * 0.3f + rnd(i * 3)) % 1f); glow(F.add, (rnd(i * 7) - 0.5f) * 7f, u * 5f, (rnd(i * 11) - 0.5f) * 4f, 0.05f, i % 2 == 0 ? 0xFFB040 : 0xFF6020, (1f - u) * 0.9f); }
            }
            default -> {                                                                          // crying obsidian, a red rift it steps out of
                Skin cry = (f, u, v, w, hh) -> { float nz = rnd(f * 31 + u * 7 + v * 13 + 99); return nz < 0.2f ? 0x8A2AD8 : mul(0x1A0E26, 0.8f + nz * 0.4f); };
                for (float[] pl : new float[][]{{-1.9f, -1.5f, 1.3f}, {2.0f, -1.8f, 1.5f}}) { push(); M.translate(pl[0], surf(pl[0], pl[1]), pl[1]); box(-0.42f, 0f, -0.42f, 0.42f, pl[2], 0.42f, cry, 6f, 0.03f); pop(); }
                float op = sm((t - 0.15f) / 0.4f) * (1f - 0.6f * sm((t - SlayerScene.WALK) / 0.6f));
                if (op > 0.01f) { column(F.add, 0, 0f, -0.5f, 3f, 0.6f * op, 0xFF2050, 0.5f * op); column(F.add, 0, 0f, -0.5f, 2.8f, 0.14f * op, 0xFFC0D0, 0.9f * op); }
            }
        }
    }
    /** Ground thrown up as the boss breaks through it. */
    static void burstUp(float t, float rise, int[] cols) {
        if (rise <= 0 || rise > 1.4f) return;
        for (int i = 0; i < 16; i++) {
            float d = (t - SlayerScene.RISE) - rnd(i * 3) * 0.25f; if (d < 0 || d > 1.1f) continue;
            float an = rnd(i * 7) * 6.2832f, v = 1.2f + rnd(i * 11) * 1.8f, vy = 3f + rnd(i * 13) * 3f, y = 0.2f + vy * d - 9.8f * d * d * 0.5f; if (y < 0.05f) y = 0.05f;
            push(); M.translate((float) Math.cos(an) * v * d, y, (float) Math.sin(an) * v * d * 0.7f).rotateXYZ(d * 6 + i, d * 4, i);
            float s = (0.08f + 0.06f * rnd(i)) * (1f - sm((d - 0.8f) / 0.3f)); box(-s, -s, -s, s, s, s, flat(cols[i % cols.length]), 2f, 0.02f); pop();
        }
    }

    // ------------------------------------------------------------------ the boss: rises out of its spawn, roars, stands and takes the stars
    static void boss(float t, float p, int energy, float roarK) {
        float ar = (t - SlayerScene.RISE) / 0.95f; if (ar <= 0) return;
        float since = t - last; boolean gone = since >= 0;
        if (gone && since > 1.6f) { return; }
        float y = 0f; boolean show = true;
        switch (boss) {
            case 3 -> { show = ar > 0.6f || ((int) (t * 24)) % 3 != 0; if (ar < 0.6f) for (int i = 0; i < 10; i++) glow(F.add, (rnd(i * 3 + (int) (t * 20)) - 0.5f) * 1.2f, rnd(i * 7 + (int) (t * 20)) * 2.6f, (rnd(i * 5) - 0.5f) * 1.2f, 0.12f, 0xC060FF, 1f - ar); }
            case 5 -> y = lerp(-0.6f, 0f, sm(ar));
            case 4 -> y = lerp(-2.8f, 0.3f, sm(ar)) + 0.15f * (float) Math.sin(t * 3f);
            default -> y = lerp(-2.8f, 0f, sm(ar)) + 0.08f * (float) Math.sin(Math.PI * cl(ar));
        }
        float hitT = 99f, kb = 0f; for (float hh : hits) { float d = t - hh; if (d >= 0) { hitT = Math.min(hitT, d); kb += 0.12f * (d < 0.07f ? sm(d / 0.07f) : (float) Math.exp(-(d - 0.07f) * 7f)); } }
        float face = (float) Math.atan2(eye.x, eye.z);
        if (show) {
            push(); M.translate(0, y, -kb).rotateY(face).rotateX(-0.22f * roarK);                // it rears back as it roars
            float sc = MOB_K_STAR[boss] / 16f * (1f + 0.05f * roarK); M.scale(sc);
            tint = hitT < 0.09f && !gone ? 0.22f * (1f - hitT / 0.09f) : 0f; tintCol = 0xFFE0C0;
            burstD = gone ? since : -1f; partNo = 0; mobMode = true; rimCol = mixc(energy, 0xFFFFFF, 0.3f);
            crumble = type == 0 && gone ? Math.min(1f, since / 0.5f) : 0f;
            float idle = 0.25f * (float) Math.sin(t * 2.2f) * (1f - roarK);
            switch (boss) {
                case 0 -> revenant(t, idle);
                case 1 -> spider(t, idle);
                case 2 -> wolf(t, idle);
                case 3 -> enderman(t, idle);
                case 4 -> blaze(t);
                default -> humanoid(t, idle, false, VAMP_HEAD, VAMP_BODY, VAMP_ARM, VAMP_LEG, true);
            }
            pop(); burstD = -1f; tint = 0f; crumble = 0f; mobMode = false;
        }
        if (roarK > 0.01f) { float u = cl((t - SlayerScene.WALK - 0.15f) / 0.6f); ring(F.add, 0, 0.05f, 0, 0.5f + 4f * eo(u), 0.25f, energy, 0.7f * (1f - u)); }   // the roar's shockwave
        if (!gone && ar > 0.5f) { disc(F.veil, 0, 0.03f, 0, 0.8f * MOB_K_STAR[boss], 0x050508, 0.45f); for (int i = 0; i < 12; i++) { float u = ((t * 0.5f + i / 12f) % 1f), an = i * 2.4f + t * 2f + u * 3f, r = 0.6f * (1f - u * 0.4f); glow(F.add, (float) Math.cos(an) * r, u * 2.4f, (float) Math.sin(an) * r, 0.07f, i % 3 == 0 ? 0xFFFFFF : energy, 0.8f * (1f - u) * sm(u * 5f)); } }
        if (gone) for (int i = 0; i < 40; i++) {                                                  // shattered: the pieces turn to stardust and rise
            float u = cl((since - rnd(i) * 0.3f) / 1.3f); if (u <= 0 || u >= 1) continue;
            float an = rnd(i * 7) * 6.28f, r = 0.3f + u * (1.2f + rnd(i * 3)), yy = 0.3f + rnd(i * 11) * 1.8f + u * (type == 0 ? 0.6f : 1.4f);
            glow(F.add, (float) Math.cos(an) * r, yy, (float) Math.sin(an) * r * 0.7f, 0.06f, type == 0 ? 0x9A9AA8 : i % 3 == 0 ? 0xFFFFFF : energy, (1f - u) * 0.9f);
        }
    }
    static final float[] MOB_K_STAR = {1.4f, 1.6f, 1.85f, 0.85f, 1.4f, 1.35f};
    static void line(float x0, float y0, float x1, float y1, float z, int rgb, float a) {
        float dx = x1 - x0, dy = y1 - y0, l = (float) Math.sqrt(dx * dx + dy * dy); if (l < 1e-4f) return; float w = 0.025f, nx = -dy / l * w, ny = dx / l * w;
        int c = argb(a, rgb);
        wquad(F.add, x0 + nx, y0 + ny, z, c, x1 + nx, y1 + ny, z, c, x1 - nx, y1 - ny, z, c, x0 - nx, y0 - ny, z, c);
    }
    static void line3(Vector3f a, Vector3f b, int rgb, float al) {
        Vector3f d = new Vector3f(b).sub(a); Vector3f side = new Vector3f(d).cross(new Vector3f(eye).sub(a)).normalize().mul(0.03f); int c = argb(al, rgb);
        wquad(F.add, a.x + side.x, a.y + side.y, a.z + side.z, c, b.x + side.x, b.y + side.y, b.z + side.z, c, b.x - side.x, b.y - side.y, b.z - side.z, c, a.x - side.x, a.y - side.y, a.z - side.z, c);
    }

    // ------------------------------------------------------------------ falling stars
    static Vector3f impact(int i) {
        boolean big = type == 0 || i == n - 1;
        return big ? new Vector3f(0f, 1.4f, 0.1f) : new Vector3f((rnd(i * 31 + 3) - 0.5f) * 0.9f, 0.6f + rnd(i * 17 + 5) * 1.5f, 0.15f);
    }
    static void fallingStar(float t, int i, int energy) {
        boolean big = type == 0 || i == n - 1; float h = hits[i], fl = SlayerScene.flight(type, i) + 0.08f, t0 = h - fl;
        if (t < t0) return;
        Vector3f a = impact(i); float k = big ? (type == 2 ? 1.5f : 1.2f) : 0.85f; int col = starCol(i, energy);
        float side = i % 2 == 0 ? 1f : -1f;
        Vector3f s0 = new Vector3f(a.x + side * (3.5f + 1.5f * rnd(i * 7 + 1)), a.y + 7f + 2f * rnd(i * 5), a.z - 1.5f);
        if (t < h) {
            float u = (t - t0) / fl;
            for (int q = 20; q >= 1; q--) {                                                       // a sparkling tail
                float uu = u - q * 0.03f; if (uu < 0) continue; float e = uu * uu * (0.4f + 0.6f * uu);
                float px = lerp(s0.x, a.x, e), py = lerp(s0.y, a.y, e), pz = lerp(s0.z, a.z, e), f = 1f - q / 21f;
                int tc = type == 2 && big ? Vox3D.rainbow(q / 21f + u) : mixc(col, 0xFFFFFF, f * 0.5f);
                glow(F.add, px, py, pz, (0.08f + 0.3f * f) * k, tc, 0.7f * f);
                if (q % 4 == 0) glint(F.add, px + (rnd(i * 13 + q) - 0.5f) * 0.3f, py, pz, 0.15f * k * f, 0xFFFFFF, f);
            }
            float e = u * u * (0.4f + 0.6f * u), px = lerp(s0.x, a.x, e), py = lerp(s0.y, a.y, e), pz = lerp(s0.z, a.z, e);
            glow(F.add, px, py, pz, 1.0f * k, col, 0.5f); glint(F.add, px, py, pz, 0.7f * k, 0xFFFFFF, 1f); glow(F.add, px, py, pz, 0.25f * k, 0xFFFFFF, 1f);
            push(); M.translate(px, py, pz).rotateXYZ(t * 6f + i, t * 4f, i); float r = 0.09f * k; box(-r, -r, -r, r, r, r, (f, uu2, vv, w, hh) -> 0xFFFFFF, 1f, 0f); pop();
            return;
        }
        float d = t - h; if (d > 1.0f) return;
        float fl2 = Math.max(0f, 1f - d / 0.25f);
        if (fl2 > 0) { glow(F.add, a.x, a.y, a.z + 0.3f, 0.9f * k, 0xFFFFFF, 0.45f * fl2); glow(F.add, a.x, a.y, a.z + 0.3f, 1.8f * k, col, 0.25f * fl2); }
        for (int q = 0; q < (big ? 16 : 8); q++) {                                                  // a burst of star sparks
            float an = rnd(i * 41 + q) * 6.2832f, v = 1.5f + 2f * rnd(i * 23 + q), tt = Math.min(d, 0.7f);
            float px = a.x + (float) Math.cos(an) * v * tt, py = a.y + (float) Math.sin(an) * v * tt - 2f * tt * tt, pz = a.z + 0.3f;
            if (q % 3 == 0) glint(F.add, px, py, pz, 0.18f * k, col, 1f - tt / 0.7f); else glow(F.add, px, py, pz, 0.05f, q % 2 == 0 ? 0xFFFFFF : col, 1f - tt / 0.7f);
        }
        float rw = d / 0.5f; if (rw < 1f) for (int q = 0; q < 24; q++) { double an = q * Math.PI * 2 / 24; float r = (0.2f + 1.4f * eo(rw)) * k; glow(F.add, a.x + right.x * (float) Math.cos(an) * r + up.x * (float) Math.sin(an) * r, a.y + right.y * (float) Math.cos(an) * r + up.y * (float) Math.sin(an) * r, a.z + 0.3f + right.z * (float) Math.cos(an) * r, 0.06f, col, 0.9f * (1f - rw)); }
    }

    // ------------------------------------------------------------------ the constellation and the drop's star
    /** Where strike i is kept in the sky: round the drop's star. */
    static Vector3f skySpot(int i) {
        double an = Math.PI / 2 + (i - (n - 1) / 2f) * (Math.PI * 1.7 / Math.max(1, n)); float r = 1.35f + 0.18f * rnd(i * 13);
        return new Vector3f(PIN.x + (float) Math.cos(an) * r * 1.3f, PIN.y + 0.5f + (float) Math.sin(an) * r * 0.9f, PIN.z + 0.4f);
    }
    static void constellation(float t, float p, float A, int energy) {
        float end = 1f - sm((t - (A - 0.8f)) / 0.6f);
        if (type == 0) {                                                                            // a lone star that flickers out
            float d = t - last; if (d < 0) return; Vector3f s = skySpot(0); float a = Math.max(0f, 1f - d / 1.4f) * (0.6f + 0.4f * (float) Math.sin(t * 30));
            glint(F.add, s.x, s.y, s.z, 0.3f, 0xB8C0D8, a); return;
        }
        int col = type == 2 ? 0xFFD24A : mixc(energy, 0xFFFFFF, 0.35f);
        for (int i = 0; i < n; i++) {
            float d = t - hits[i]; if (d < 0) continue;
            Vector3f s = skySpot(i); float a = sm(d / 0.2f) * end, tw = 0.75f + 0.25f * (float) Math.sin(t * 5 + i);
            glint(F.add, s.x, s.y, s.z, (i == n - 1 ? 0.45f : 0.3f) * tw, i == n - 1 ? col : starCol(i, energy), a); glow(F.add, s.x, s.y, s.z, 0.35f, starCol(i, energy), 0.4f * a);
            if (i > 0 && p > 0) line3(skySpot(i - 1), s, col, 0.55f * sm((p - i * 0.05f) / 0.3f) * end);
        }
        if (p > 0) for (int i = 0; i < n; i++) { Vector3f s = skySpot(i); float a = 0.25f * sm((p - 0.3f) / 0.3f) * end; line3(s, PIN, col, a); }
    }
    /** The stardust gathering into the drop's star, then its light: a beam of its colour, or RNG's gold with rays and confetti. */
    static void gather(float t, float p, float A, int energy) {
        float end = 1f - sm((t - (A - 0.8f)) / 0.6f), g = sm((p + 0.2f) / 0.45f);
        int col = type == 2 ? 0xFFD24A : energy;
        for (int i = 0; i < 30; i++) {                                                              // dust spiralling in
            float u = cl(g + rnd(i) * 0.2f - 0.2f), an = i * 0.7f + u * 5f, r = (1f - u) * (1.2f + rnd(i * 3));
            glow(F.add, PIN.x + (float) Math.cos(an) * r, PIN.y + 0.3f + (float) Math.abs(Math.sin(an)) * r * 0.9f, PIN.z + 0.3f, 0.06f, i % 3 == 0 ? 0xFFFFFF : col, (1f - u * 0.6f) * 0.9f * (1f - sm((p - 0.4f) / 0.3f)));
        }
        if (p <= 0) return;
        float a = sm(p / 0.25f) * end;
        glow(F.add, PIN.x, PIN.y, PIN.z + 0.3f, 1.4f, col, 0.45f * a); glint(F.add, PIN.x, PIN.y, PIN.z + 0.3f, 1.3f + 0.15f * (float) Math.sin(t * 6), 0xFFFFFF, 0.6f * a);
        if (type == 2) {
            for (int k = 0; k < 14; k++) {
                float an = k * 0.4488f + t * 0.3f, len = 3.6f + 1.2f * (float) Math.sin(t * 2 + k), ax = (float) Math.cos(an), ay = (float) Math.sin(an), w = 0.14f;
                float ex = PIN.x + (right.x * ax + up.x * ay) * len, ey = PIN.y + (right.y * ax + up.y * ay) * len, ez = PIN.z + (right.z * ax + up.z * ay) * len;
                float qx = (right.x * -ay + up.x * ax) * w, qy = (right.y * -ay + up.y * ax) * w, qz = (right.z * -ay + up.z * ax) * w;
                int c0 = argb(0.22f * a, 0xFFE9A0), c1 = argb(0, 0xFFE9A0);
                wquad(F.add, PIN.x, PIN.y, PIN.z, c0, PIN.x, PIN.y, PIN.z, c0, ex + qx, ey + qy, ez + qz, c1, ex - qx, ey - qy, ez - qz, c1);
            }
            for (int k = 0; k < 40; k++) {
                float u = ((p * 0.35f + rnd(k * 7)) % 1f), cx = (rnd(k * 13) - 0.5f) * 6f, cz = (rnd(k * 19) - 0.5f) * 3f, cy = 4f - u * 6f;
                int cc = k % 4 == 0 ? 0xFF7AD9 : k % 4 == 1 ? 0x6AB8FF : k % 4 == 2 ? 0xFFE04A : 0x7AE88A;
                push(); M.translate(cx + (float) Math.sin(t * 3 + k) * 0.2f, cy, cz).rotateXYZ(t * 4 + k, t * 3, k); float s = 0.045f; box(-s, -s * 0.3f, -s, s, s * 0.3f, s, flat(cc), 1f, 0f); pop();
            }
            for (int k = 0; k < 3; k++) { float u = ((p * 0.8f + k / 3f) % 1f); ring(F.add, 0, 0.05f, 0, 0.6f + 2.4f * u, 0.12f, 0xFFD24A, (1f - u) * 0.7f * a); }
        }
    }
}
