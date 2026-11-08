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
    static final Vector3f PIN = new Vector3f(0f, 0.45f, 0f);       // where the drop forms (pinned under the item on screen)
    static float[] hits; static float last; static int type, boss, n;
    static Mesh planetMesh; static int planetBoss = -1;

    static int done(float t) { int k = 0; for (float h : hits) if (t >= h) k++; return k; }
    static int starCol(int i, int energy) { return type == 2 ? (i == n - 1 ? 0xFFD24A : SlayerScene.TIER[Math.min(i, SlayerScene.TIER.length - 1)]) : type == 0 ? 0xDDE6FF : i == n - 1 ? mixc(energy, 0xFFFFFF, 0.3f) : 0xFFE9A0; }

    static Frame build(float t, float A, int bossIn, int typeIn, int energy, float aspect, float pinY, boolean z01) {
        boss = bossIn; type = typeIn; hits = SlayerScene.HITS[type]; n = hits.length; last = hits[n - 1];
        F = new Frame(); tint = 0f; M = new Matrix4f(); STACK.clear(); mobMode = false; pass = 0; dyn = false; fovNow = FOV;
        float O = AnimationManager.SL_PIVOT, p = t - O;

        // ---- the camera: a slow drift round the little planet, closer as the stars come, a kick on every strike
        float kick = 0f; for (int i = 0; i < n; i++) { float d = t - hits[i]; if (d >= 0) kick += (i == n - 1 ? 1f : 0.4f) * (float) Math.exp(-d * 10f); }
        float dist = lerp(16f, 13.6f, eo(t / 1.5f)) - 0.8f * sm((t - SlayerScene.WALK) / 1.4f);
        if (type == 2) dist -= 1.2f * sm((t - 2.95f) / 0.42f) * (1f - sm((t - last) / 0.3f));
        dist += 0.8f * sm(p / 0.7f);
        float yaw = (float) Math.toRadians(-14 + 12 * Math.sin(t * 0.3) + 18 * (1f - eo(t / 1.5f)));
        float pitch = (float) Math.toRadians(24 + 3 * Math.sin(t * 0.25));
        Vector3f target = new Vector3f(0f, 0.6f, 0f);
        eye.set(target.x + (float) (Math.sin(yaw) * Math.cos(pitch)) * dist, target.y + (float) Math.sin(pitch) * dist, target.z + (float) (Math.cos(yaw) * Math.cos(pitch)) * dist);
        float sh = Math.min(1.2f, kick), jx = 0.07f * sh * (float) (Math.sin(t * 53) + Math.sin(t * 31 + 1.3)), jy = 0.07f * sh * (float) (Math.sin(t * 47 + 2.1) + Math.sin(t * 37 + 0.4));
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

        // ---- the little planet, turning under the boss's feet while it walks
        float walkAmt = sm((t - SlayerScene.WALK + 0.1f) / 0.3f) * (1f - sm((t - SlayerScene.WALK_END + 0.25f) / 0.35f));
        float travel = 0f; for (float s = 0f; s < t; s += 0.02f) travel += 0.02f * sm((s - SlayerScene.WALK + 0.1f) / 0.3f) * (1f - sm((s - SlayerScene.WALK_END + 0.25f) / 0.35f));
        if (planetBoss != boss || planetMesh == null) { Mesh keep = F.opq; F.opq = new Mesh(false); planet(th); planetMesh = F.opq; F.opq = keep; planetBoss = boss; }
        F.ground = planetMesh;
        F.groundMat = new Matrix4f().translate(0, -PR, 0).rotateX(-travel * 0.55f / PR * 2.2f).rotateY(t * 0.08f);
        orbitRing(t, energy);

        // ---- the boss
        boss(t, p, energy, walkAmt);

        // ---- falling stars, and the constellation they leave
        for (int i = 0; i < n; i++) fallingStar(t, i, energy);
        constellation(t, p, A, energy);
        if (p > -0.2f && type > 0) gather(t, p, A, energy);
        return F;
    }

    // ------------------------------------------------------------------ deep space
    static void space(float t, float aspect, int energy, Theme th, float dim) {
        sky(F.back, aspect, 400f, mixc(0x05061A, 0x000000, 0.5f * dim), mixc(mixc(0x0E0E30, energy, 0.12f), 0x000000, 0.5f * dim), mixc(0x140A2A, 0x000000, 0.4f * dim), 0.97f);
        for (int i = 0; i < 110; i++) {
            Vector3f q = onSky((rnd(i * 7 + 1) - 0.5f) * 2f, (rnd(i * 13 + 2) - 0.5f) * 2f, 80f, aspect);
            float tw = 0.5f + 0.5f * (float) Math.sin(t * (1.3f + rnd(i) * 2.4f) + i), big = rnd(i * 31 + 9);
            glow(F.sky, q.x, q.y, q.z, 0.1f + big * 0.28f, i % 7 == 0 ? 0xC8D8FF : i % 11 == 0 ? 0xFFD8B0 : 0xFFFFFF, (0.3f + 0.7f * big) * tw * (1f - 0.8f * dim));
            if (big > 0.94f) glint(F.sky, q.x, q.y, q.z, 0.9f, 0xFFFFFF, 0.55f * tw * (1f - 0.8f * dim));
        }
        int[] neb = {energy, 0x6A4AE8, 0x2A6AD8, 0xD84AA8};
        for (int i = 0; i < 9; i++) { Vector3f q = onSky((rnd(i * 17 + 5) - 0.5f) * 1.6f, (rnd(i * 23 + 1) - 0.5f) * 1.2f, 70f, aspect); glow(F.sky, q.x, q.y, q.z, 10f + rnd(i * 41) * 12f, neb[i % neb.length], (0.06f + rnd(i * 9) * 0.05f) * (1f - 0.6f * dim)); }
        // a far ringed planet for depth
        Vector3f fp = onSky(0.62f, 0.45f, 60f, aspect); float fr = 2.6f;
        for (int k = 0; k < 4; k++) glow(F.sky, fp.x, fp.y, fp.z, fr * (1.0f - k * 0.18f), k == 0 ? 0x6A4AE8 : 0x2A1E5A, k == 0 ? 0.12f : 0.6f);
        for (int k = 0; k < 40; k++) { double a = k * Math.PI * 2 / 40; float rx = (float) Math.cos(a) * fr * 1.9f, ry = (float) Math.sin(a) * fr * 0.45f; float rr = rx * 0.95f - ry * 0.3f, uu = rx * 0.3f + ry * 0.95f; glow(F.sky, fp.x + right.x * rr + up.x * uu, fp.y + right.y * rr + up.y * uu, fp.z + right.z * rr + up.z * uu, 0.25f, 0xB8A8F8, 0.25f); }
        // a shooting star now and then
        float per = 1.7f; int k = (int) (t / per); float u = (t - k * per) / 0.5f;
        if (u < 1f) { Vector3f a = onSky(-0.9f + rnd(k * 7) * 0.6f, 0.75f - rnd(k * 3) * 0.2f, 70f, aspect), b = onSky(-0.2f + rnd(k * 7) * 0.6f, 0.45f - rnd(k * 3) * 0.2f, 70f, aspect);
            for (int i = 0; i < 12; i++) { float uu = u - i * 0.03f; if (uu < 0) break; glow(F.sky, lerp(a.x, b.x, uu), lerp(a.y, b.y, uu), lerp(a.z, b.z, uu), 0.4f - i * 0.025f, 0xDDE6FF, (1f - i / 12f) * (1f - u)); } }
    }

    // ------------------------------------------------------------------ the little planet (half-block voxels, built once per boss)
    static void planet(Theme th) {
        float v = VOX; int R = (int) Math.ceil(PR / v) + 1;
        for (int i = -R; i <= R; i++) for (int j = -R; j <= R; j++) for (int k = -R; k <= R; k++) {
            if (!in(i, j, k)) continue;
            int faces = (!in(i + 1, j, k) ? 1 : 0) | (!in(i - 1, j, k) ? 2 : 0) | (!in(i, j + 1, k) ? 4 : 0) | (!in(i, j - 1, k) ? 8 : 0) | (!in(i, j, k + 1) ? 16 : 0) | (!in(i, j, k - 1) ? 32 : 0);
            if (faces == 0) continue;
            float r = (float) Math.sqrt(i * i + j * j + k * k) * v; final float upness = j * v / Math.max(0.01f, r);
            final int ii = i, jj = j, kk = k;
            Skin s = (f, u, w2, w, h) -> {
                float nz = rnd(ii * 977 + jj * 131 + kk * 7 + f * 31 + u * 17 + w2 * 3);
                int base = upness > 0.45f ? (nz < 0.12f ? th.top2() : th.top()) : upness > 0.3f ? (nz < 0.5f ? th.top() : th.dirt()) : upness > -0.3f ? th.dirt() : th.stone();
                if (boss == 4 && nz < 0.08f) base = 0xFF8A20;
                if (boss == 3 && upness < 0f && nz < 0.06f) base = 0xB060E0;
                return mul(base, 0.9f + nz * 0.16f);
            };
            box(i * v - v / 2, j * v - v / 2, k * v - v / 2, i * v + v / 2, j * v + v / 2, k * v + v / 2, s, 4f, 0f, faces);
        }
        // a few tiny things living on it, standing out from the surface
        float[][] spots = {{0.75f, 0.5f}, {-0.85f, 2.3f}, {0.95f, 3.9f}, {-0.7f, 5.2f}};
        for (int q = 0; q < spots.length; q++) {
            push(); M.rotateY(spots[q][1]).rotateX(spots[q][0]).translate(0, PR - 0.04f, 0);
            switch (boss) {
                case 0 -> { if (q % 2 == 0) { Skin st = noisy(0x8A918A, 0.15f, q); box(-0.13f, 0f, -0.04f, 0.13f, 0.32f, 0.04f, st, 24f, 0.012f); box(-0.09f, 0.32f, -0.04f, 0.09f, 0.37f, 0.04f, st, 24f, 0.012f); }
                            else { Skin w = noisy(0x5A3A22, 0.14f, q); box(-0.025f, 0f, -0.025f, 0.025f, 0.38f, 0.025f, w, 24f, 0.01f); box(-0.11f, 0.24f, -0.02f, 0.11f, 0.29f, 0.02f, w, 24f, 0.01f); } }
                case 1 -> box(-0.12f, 0f, -0.12f, 0.12f, 0.2f, 0.12f, noisy(0x6E6A72, 0.18f, q), 24f, 0.012f);
                case 2 -> { box(-0.05f, 0f, -0.05f, 0.05f, 0.45f, 0.05f, noisy(0x5A3E22, 0.12f, q), 24f, 0.01f); box(-0.2f, 0.4f, -0.2f, 0.2f, 0.72f, 0.2f, noisy(0x3E7A2A, 0.2f, q), 24f, 0.012f); }
                case 3 -> { box(-0.08f, 0f, -0.08f, 0.08f, 0.5f, 0.08f, noisy(0x140C1E, 0.2f, q), 24f, 0.012f); box(-0.04f, 0.5f, -0.04f, 0.04f, 0.58f, 0.04f, flat(0xE070FF), 24f, 0.01f); }
                case 4 -> box(-0.1f, 0f, -0.1f, 0.1f, 0.3f, 0.1f, (f, u, w2, w, h) -> (u + w2) % 3 == 0 ? 0x1E0E10 : 0x4A1A1E, 24f, 0.012f);
                default -> box(-0.1f, 0f, -0.1f, 0.1f, 0.26f, 0.1f, (f, u, w2, w, h) -> rnd(u * 7 + w2 * 3 + f) < 0.25f ? 0x8A2AD8 : 0x1A0E26, 24f, 0.012f);
            }
            pop();
        }
    }
    static final float VOX = 0.25f;
    static boolean in(int i, int j, int k) {
        float v = VOX; float r = (float) Math.sqrt(i * i + j * j + k * k) * v;
        return r <= PR + (rnd(i * 31 + j * 17 + k * 7) - 0.5f) * 0.12f;
    }
    /** A ring of little lights orbiting the planet, tilted. */
    static void orbitRing(float t, int energy) {
        for (int k = 0; k < 48; k++) {
            double a = k * Math.PI * 2 / 48 + t * 0.4; float x = (float) Math.cos(a) * PR * 2.1f, z = (float) Math.sin(a) * PR * 2.1f, y = -PR + x * 0.22f + z * 0.1f;
            glow(F.add, x, y, z, k % 6 == 0 ? 0.1f : 0.05f, k % 6 == 0 ? 0xFFFFFF : energy, 0.75f);
        }
    }

    // ------------------------------------------------------------------ the boss
    /** Points round the boss's outline: its constellation as it is drawn into being. */
    static final float[][] OUTLINE = {{0, 2.25f}, {-0.35f, 1.95f}, {0.35f, 1.95f}, {-0.75f, 1.5f}, {0.75f, 1.5f}, {-0.3f, 0.95f}, {0.3f, 0.95f}, {-0.25f, 0.05f}, {0.25f, 0.05f}};
    static final int[][] LINKS = {{0, 1}, {0, 2}, {1, 3}, {2, 4}, {1, 5}, {2, 6}, {5, 6}, {5, 7}, {6, 8}};
    static void boss(float t, float p, int energy, float walkAmt) {
        float form = sm((t - SlayerScene.RISE - 0.5f) / 0.45f), drawIn = (t - SlayerScene.RISE) / 0.6f;
        float h = MOB_K_STAR[boss] * (boss == 1 || boss == 2 ? 1.1f : 2.0f);
        // the constellation that draws it: stars pop in one by one, lines join them, then it fills in
        if (drawIn > 0 && form < 1f) {
            float fade = 1f - form;
            for (int i = 0; i < OUTLINE.length; i++) {
                float a = sm((drawIn - i * 0.08f) / 0.15f) * fade; if (a <= 0) continue;
                glint(F.add, OUTLINE[i][0] * h * 0.5f, 0.05f + OUTLINE[i][1] * h / 2.25f, 0.3f, 0.22f, 0xFFFFFF, a); glow(F.add, OUTLINE[i][0] * h * 0.5f, 0.05f + OUTLINE[i][1] * h / 2.25f, 0.3f, 0.3f, energy, a * 0.6f);
            }
            for (int[] l : LINKS) {
                float a = sm((drawIn - 0.5f - l[1] * 0.04f) / 0.2f) * fade * 0.7f; if (a <= 0) continue;
                line(OUTLINE[l[0]][0] * h * 0.5f, 0.05f + OUTLINE[l[0]][1] * h / 2.25f, OUTLINE[l[1]][0] * h * 0.5f, 0.05f + OUTLINE[l[1]][1] * h / 2.25f, 0.3f, mixc(energy, 0xFFFFFF, 0.5f), a);
            }
        }
        if (form <= 0.01f) return;
        float since = t - last; boolean gone = since >= 0;
        if (gone && since > 1.6f) return;
        float hitT = 99f, kb = 0f; for (float hh : hits) { float d = t - hh; if (d >= 0) { hitT = Math.min(hitT, d); kb += 0.12f * (d < 0.07f ? sm(d / 0.07f) : (float) Math.exp(-(d - 0.07f) * 7f)); } }
        float walk = walkAmt * (float) Math.sin((t - SlayerScene.WALK) * 8.5f);
        push(); M.translate(0, walkAmt * 0.04f * Math.abs((float) Math.sin((t - SlayerScene.WALK) * 8.5f)), -kb).rotateY((float) Math.atan2(eye.x, eye.z) * 0.6f);
        float pop = form < 1f ? 0.7f + 0.3f * eob(form) : 1f; float sc = MOB_K_STAR[boss] / 16f * pop; M.scale(sc);
        tint = form < 1f ? 1f - form : hitT < 0.09f && !gone ? 0.22f * (1f - hitT / 0.09f) : 0f; tintCol = form < 1f ? mixc(energy, 0xFFFFFF, 0.6f) : 0xFFE0C0;
        burstD = gone ? since : -1f; partNo = 0; mobMode = true; rimCol = mixc(energy, 0xFFFFFF, 0.3f);
        crumble = type == 0 && gone ? Math.min(1f, since / 0.5f) : 0f;
        switch (boss) {
            case 0 -> revenant(t, walk);
            case 1 -> spider(t, walk);
            case 2 -> wolf(t, walk);
            case 3 -> enderman(t, walk);
            case 4 -> blaze(t);
            default -> humanoid(t, walk, false, VAMP_HEAD, VAMP_BODY, VAMP_ARM, VAMP_LEG, true);
        }
        pop(); burstD = -1f; tint = 0f; crumble = 0f; mobMode = false;
        if (!gone) { disc(F.veil, 0, 0.02f, 0, 0.75f * MOB_K_STAR[boss], 0x050508, 0.45f); for (int i = 0; i < 12; i++) { float u = ((t * 0.5f + i / 12f) % 1f), an = i * 2.4f + t * 2f + u * 3f, r = 0.55f * (1f - u * 0.4f); glow(F.add, (float) Math.cos(an) * r, u * 2.2f, (float) Math.sin(an) * r, 0.07f, i % 3 == 0 ? 0xFFFFFF : energy, 0.8f * (1f - u) * sm(u * 5f)); } }
        // shattered: the pieces turn to stardust and rise
        if (gone) for (int i = 0; i < 40; i++) {
            float u = cl((since - rnd(i) * 0.3f) / 1.3f); if (u <= 0 || u >= 1) continue;
            float an = rnd(i * 7) * 6.28f, r = 0.3f + u * (1.2f + rnd(i * 3)), y = 0.3f + rnd(i * 11) * 1.8f + u * (type == 0 ? 0.6f : 1.4f);
            glow(F.add, (float) Math.cos(an) * r, y, (float) Math.sin(an) * r * 0.7f, 0.06f, type == 0 ? 0x9A9AA8 : i % 3 == 0 ? 0xFFFFFF : energy, (1f - u) * 0.9f);
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
        if (fl2 > 0) { glow(F.add, a.x, a.y, a.z + 0.3f, 1.6f * k, 0xFFFFFF, 0.6f * fl2); glow(F.add, a.x, a.y, a.z + 0.3f, 2.6f * k, col, 0.35f * fl2); }
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
