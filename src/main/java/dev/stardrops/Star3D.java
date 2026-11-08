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
        boss = bossIn; type = typeIn; reseed(t); hits = SlayerScene.HITS[type]; n = hits.length; last = hits[n - 1];
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
        farGround(th, dim);
        atmosphere(t, th, energy, dim);
        backdrop(t, th, energy, dim);
        nearGround(th, dim);
        if (boss != 4) Vox3D.weather(t, boss);
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

    // ------------------------------------------------------------------ the sky: a dusk over the planet (Butcher-dark), a pixel moon, a ringed planet, stars
    static final int[] PLANET_A = {0xD8A070, 0xA898C8, 0x8AC0D8, 0xB07AE8, 0xF0A858, 0xE07AA8}, PLANET_B = {0x9A5A48, 0x6A5A8A, 0x4A7A9A, 0x5A3A8A, 0xA8502A, 0x8A3A6A};
    static void space(float t, float aspect, int energy, Theme th, float dim) {
        int top = mixc(th.skyTop(), 0x000000, 0.5f * dim), mid = mixc(mixc(th.skyMid(), energy, 0.06f), 0x000000, 0.5f * dim), low = mixc(mixc(th.skyLow(), 0xFFFFFF, 0.06f), 0x000000, 0.45f * dim);
        sky(F.back, aspect, 400f, top, mid, low, 0.97f);
        for (int i = 0; i < 130; i++) {                                                             // stars, thinning toward the dusk at the horizon
            float sy = -0.05f + rnd(i * 13 + 2) * 1.05f; Vector3f q = onSky((rnd(i * 7 + 1) - 0.5f) * 2f, sy, 80f, aspect);
            float tw = 0.5f + 0.5f * (float) Math.sin(t * (1.3f + rnd(i) * 2.4f) + i), big = rnd(i * 31 + 9), hz = sm((sy + 0.05f) / 0.45f);
            glow(F.sky, q.x, q.y, q.z, 0.1f + big * 0.26f, i % 7 == 0 ? 0xC8D8FF : i % 11 == 0 ? 0xFFD8B0 : 0xFFFFFF, (0.3f + 0.7f * big) * tw * (1f - 0.8f * dim) * (0.25f + 0.75f * hz));
            if (big > 0.95f) glint(F.sky, q.x, q.y, q.z, 0.8f, 0xFFFFFF, 0.5f * tw * (1f - 0.8f * dim) * hz);
        }
        int[] neb = {energy, 0x6A4AE8, 0x2A6AD8, 0xD84AA8};
        for (int i = 0; i < 6; i++) { Vector3f q = onSky((rnd(i * 17 + 5) - 0.5f) * 1.6f, 0.35f + rnd(i * 23 + 1) * 0.6f, 70f, aspect); glow(F.sky, q.x, q.y, q.z, 9f + rnd(i * 41) * 10f, neb[i % neb.length], (0.04f + rnd(i * 9) * 0.035f) * (1f - 0.6f * dim)); }
        Vector3f lowGlow = onSky(0f, -0.05f, 75f, aspect);                                           // the last of the dusk, low behind the horizon
        glow(F.sky, lowGlow.x, lowGlow.y, lowGlow.z, 34f, mixc(th.skyLow(), 0xFF8A6A, 0.25f), 0.16f * (1f - 0.7f * dim));
        skyBodies(aspect, dim, th);                                                                 // a pixel moon and planets, different every time
        float per = 1.9f; int k = (int) (t / per); float u = (t - k * per) / 0.5f;                   // a shooting star now and then
        if (u < 1f) { Vector3f a = onSky(-0.2f + rnd(k * 7) * 0.6f, 0.82f - rnd(k * 3) * 0.15f, 70f, aspect), b = onSky(0.3f + rnd(k * 7) * 0.5f, 0.55f - rnd(k * 3) * 0.15f, 70f, aspect);
            for (int i = 0; i < 12; i++) { float uu = u - i * 0.03f; if (uu < 0) break; glow(F.sky, lerp(a.x, b.x, uu), lerp(a.y, b.y, uu), lerp(a.z, b.z, uu), 0.35f - i * 0.022f, 0xDDE6FF, (1f - i / 12f) * (1f - u)); } }
    }
    // ---- this run's sky: chosen afresh every time the animation starts (or with -Dstardrops.seed for the bench)
    static int runSeed; static float lastT = 1e9f; static int lastBoss = -1, lastType = -1;
    static void reseed(float t) {
        if (t < lastT - 0.05f || boss != lastBoss || type != lastType) {
            String fixed = System.getProperty("stardrops.seed");
            runSeed = fixed != null ? Integer.parseInt(fixed) : new java.util.Random().nextInt(1 << 20);
        }
        lastT = t; lastBoss = boss; lastType = type;
    }
    static float sr(int k) { return rnd(runSeed * 7919 + k * 104729 + 17); }
    static int hsv(float h, float s, float v) {
        h = ((h % 1f) + 1f) % 1f * 6f; int i = (int) h; float f = h - i, p = v * (1 - s), q = v * (1 - s * f), u = v * (1 - s * (1 - f));
        float r, g, b; switch (i) { case 0 -> { r = v; g = u; b = p; } case 1 -> { r = q; g = v; b = p; } case 2 -> { r = p; g = v; b = u; } case 3 -> { r = p; g = q; b = v; } case 4 -> { r = u; g = p; b = v; } default -> { r = v; g = p; b = q; } }
        return Math.round(r * 255) << 16 | Math.round(g * 255) << 8 | Math.round(b * 255);
    }
    /** The moon and the planets: which side each hangs on, how big, their colours, rings and bands are all rolled per run. */
    static void skyBodies(float aspect, float dim, Theme th) {
        float side = sr(1) < 0.5f ? 1f : -1f;
        float mx = -side * (0.45f + 0.18f * sr(2)), my = 0.42f + 0.14f * sr(3);
        Vox3D.moonAt(th, aspect, mx, my, 1.6f + 1.0f * sr(4), sr(5));
        float px = side * (0.36f + 0.24f * sr(6)), py = 0.22f + 0.26f * sr(7), R = 2.2f + 1.7f * sr(8);
        float hue = sr(9), hue2 = hue + (sr(10) - 0.5f) * 0.25f;
        int ca = hsv(hue, 0.3f + 0.35f * sr(11), 0.85f + 0.12f * sr(12)), cb = hsv(hue2, 0.55f + 0.35f * sr(13), 0.45f + 0.2f * sr(14));
        pixelPlanet(aspect, dim, px, py, R, 15, ca, cb, sr(15) < 0.7f, (sr(16) - 0.5f) * 1.1f, 5f + 7f * sr(17), (sr(18) - 0.5f) * 1.2f, sr(19) < 0.25f, runSeed);
        int extra = (int) (sr(20) * 3f);                                                            // up to two small far ones
        float[][] taken = {{mx, my, 0.22f}, {px, py, R * 0.06f + 0.12f}};
        for (int e = 0, tries = 0; e < extra && tries < 12; tries++) {
            float ex = (sr(30 + tries * 3) - 0.5f) * 1.5f, ey = 0.3f + 0.55f * sr(31 + tries * 3), er = 0.5f + 0.8f * sr(32 + tries * 3); boolean ok = true;
            for (float[] q : taken) if (Math.hypot(ex - q[0], (ey - q[1]) * 1.4f) < q[2] + 0.16f) ok = false;
            if (!ok) continue;
            float h = sr(40 + e);
            pixelPlanet(aspect, dim, ex, ey, er, Math.max(5, Math.round(er * 2f / 0.42f)) | 1, hsv(h, 0.35f, 0.8f), hsv(h + 0.06f, 0.6f, 0.45f), sr(44 + e) < 0.3f, (sr(46 + e) - 0.5f), 6f + 5f * sr(48 + e), 0.3f, sr(50 + e) < 0.4f, runSeed + e * 31);
            taken = java.util.Arrays.copyOf(taken, taken.length + 1); taken[taken.length - 1] = new float[]{ex, ey, er * 0.06f + 0.1f}; e++;
        }
    }
    /** Our own touch: a ringed planet hanging over the horizon, top right, in big pixels; banded, lit from the moon's side, its ring in front. */
    static void pixelPlanet(float aspect, float dim, float sx, float sy, float R, int N, int ca, int cb, boolean hasRing, float tilt, float bandF, float bandAng, boolean spotty, int seed) {
        Vector3f c = onSky(sx, sy, 70f, aspect); float px = R * 2f / N; float cs = (float) Math.cos(tilt), sn = (float) Math.sin(tilt), ba = (float) Math.cos(bandAng), bb = (float) Math.sin(bandAng);
        glow(F.sky, c.x, c.y, c.z, R * 2.6f, ca, 0.1f * (1f - 0.6f * dim));
        int W = hasRing ? Math.round(N * 1.95f) : N / 2 + 1, H = N / 2 + 1; int[] row = new int[2 * W + 1];
        for (int v = -H; v <= H; v++) {
            for (int u = -W; u <= W; u++) {
                float x = (u + 0.0f) / (N / 2f), y = (v + 0.0f) / (N / 2f), d2 = x * x + y * y;          // -1..1 inside the planet
                float rx = x * cs - y * sn, ry = (x * sn + y * cs) / 0.26f, rr = (float) Math.sqrt(rx * rx + ry * ry);
                boolean ring = hasRing && rr > 1.32f && rr < 1.85f && !(rr > 1.55f && rr < 1.62f), front = ry < 0f, inP = d2 <= 1.0f;
                int col = -1;
                if (inP && !(ring && front)) {
                    float lit = Math.max(0f, -0.62f * x + 0.42f * y + 0.66f * (float) Math.sqrt(Math.max(0f, 1f - d2)));
                    float band = (float) Math.sin((y * ba + x * bb) * bandF + seed % 7) * 0.5f + 0.5f, nz = rnd(u * 31 + v * 17 + seed);
                    if (spotty) band = vnoise(x * 2.2f + seed % 13, y * 2.2f + seed % 5) < 0.42f ? 1f : 0.15f;
                    int base = mixc(ca, cb, band * 0.75f + (nz < 0.15f ? 0.2f : 0f));
                    col = mixc(0x0C0A1A, base, Math.min(1f, 0.12f + lit * 1.05f));
                    if (d2 > 0.82f && lit > 0.3f) col = mixc(col, 0xFFFFFF, 0.12f);                       // a lit rim
                } else if (ring) {
                    float shade = front ? 1f : 0.7f, inShadow = !front && x > 0.1f ? 0.55f : 1f;
                    col = mixc(0x0C0A1A, mixc(ca, 0xFFFFFF, rr > 1.6f ? 0.35f : 0.15f), 0.75f * shade * inShadow);
                }
                row[u + W] = col;
            }
            for (int u = 0; u < row.length; ) {
                int c0 = row[u], e = u + 1; while (e < row.length && row[e] == c0) e++;
                if (c0 >= 0) {
                    int cc = argb(1f, mixc(c0, 0x000000, 0.45f * dim));
                    float a0 = (u - W - 0.5f) * px, a1 = (e - W - 0.5f) * px, b0 = (v - 0.5f) * px, b1 = (v + 0.5f) * px;
                    wquad(F.veil, c.x + right.x * a0 + up.x * b0, c.y + right.y * a0 + up.y * b0, c.z + right.z * a0 + up.z * b0, cc,
                            c.x + right.x * a1 + up.x * b0, c.y + right.y * a1 + up.y * b0, c.z + right.z * a1 + up.z * b0, cc,
                            c.x + right.x * a1 + up.x * b1, c.y + right.y * a1 + up.y * b1, c.z + right.z * a1 + up.z * b1, cc,
                            c.x + right.x * a0 + up.x * b1, c.y + right.y * a0 + up.y * b1, c.z + right.z * a0 + up.z * b1, cc);
                }
                u = e;
            }
        }
    }

    // ------------------------------------------------------------------ the giant planet
    static final float GR = 70f;                                                // its radius: big enough to stand on, small enough that the horizon curves
    static float surf(float x, float z) { float d2 = x * x + z * z; return (float) (Math.sqrt(Math.max(0, GR * GR - d2)) - GR); }
    static final float EYE_X = 2.1f, EYE_Z = 9.8f, NEAR_R = 11.5f;             // where the camera settles (for fog, detail and the backdrop's placing)
    static int fogCol(Theme th) { return mixc(th.skyLow(), 0x06060E, 0.5f); }
    /** Distance fog, from the boss outward: the ground darkens into the dusk toward the horizon (Butcher's dark round the lit patch). */
    static float fog(float x, float z) { float r = (float) Math.sqrt(x * x + z * z); return 0.85f * sm((r - 3.2f) / 8.5f); }

    // the near ground: whole blocks with 16-pixel textures (fewer pixels further out), in patches of the boss's own blocks; built once per boss
    static float[] gP; static int[] gC; static int[] gCell; static float[] gMid; static int gCells, gBoss = -1;
    /** Smooth value noise for where the patches of each block go. */
    static float vnoise(float x, float y) {
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y); float fx = x - xi, fy = y - yi; fx = fx * fx * (3 - 2 * fx); fy = fy * fy * (3 - 2 * fy);
        float a = rnd(xi * 73 + yi * 977), b = rnd((xi + 1) * 73 + yi * 977), c = rnd(xi * 73 + (yi + 1) * 977), d = rnd((xi + 1) * 73 + (yi + 1) * 977);
        return lerp(lerp(a, b, fx), lerp(c, d, fx), fy);
    }
    /** Which block a cell is, per boss: 0 = its main ground, 1 = second, 2 = third, 3 = accent. */
    static int blockAt(int i, int j) {
        float n = vnoise(i * 0.31f + 11.3f, j * 0.31f + 4.7f) * 0.75f + rnd(i * 131 + j * 71) * 0.25f, m = vnoise(i * 0.5f + 3.1f, j * 0.5f + 9.2f);
        if (m > 0.78f) return 3; return n < 0.42f ? 0 : n < 0.62f ? 1 : 2;
    }
    /** One pixel of a block's texture (Minecraft-like: four shades of its colour, with its own marks). */
    static int texel(int blk, int i, int j, int u, int v, int px) {
        int s = 16 / px, uu = u * s, vv = v * s; float nz = rnd(uu * 131 + vv * 977 + i * 7919 + j * 104729), n2 = rnd(uu * 37 + vv * 541 + i * 31 + j * 17 + 5);
        float sh = nz < 0.2f ? 0.8f : nz < 0.55f ? 0.92f : nz < 0.85f ? 1f : 1.1f;
        int c;
        switch (boss) {
            case 0 -> c = switch (blk) {                                                            // graveyard: podzol, coarse dirt, grass, a gravel path
                case 0 -> n2 < 0.18f ? 0xA0702E : n2 < 0.3f ? 0x4A3018 : 0x7A5428;
                case 1 -> n2 < 0.12f ? 0x7A7470 : 0x65452A;
                case 2 -> n2 < 0.2f ? 0x4E6A2A : 0x5E7A34;
                default -> n2 < 0.3f ? 0x6A6A70 : n2 < 0.5f ? 0x8A8A90 : 0x76767C; };
            case 1 -> c = switch (blk) {                                                            // the den: coarse dirt, gravel, stone, cobweb-white moss
                case 0 -> n2 < 0.15f ? 0x7A7470 : 0x5A4434;
                case 1 -> n2 < 0.3f ? 0x8A8480 : n2 < 0.6f ? 0x6A6460 : 0x7A746E;
                case 2 -> n2 < 0.2f ? 0x5A5A60 : 0x6E6E74;
                default -> n2 < 0.25f ? 0xB8B8C0 : 0x5E5A56; };
            case 2 -> c = switch (blk) {                                                            // the clearing: grass, podzol, moss, a dirt path
                case 0 -> n2 < 0.2f ? 0x4A7A2E : n2 < 0.28f ? 0x6EA040 : 0x5A8E36;
                case 1 -> n2 < 0.2f ? 0x4E7A2A : 0x5E8432;
                case 2 -> n2 < 0.18f ? 0x9A6A2A : 0x6A4A26;
                default -> n2 < 0.2f ? 0x7A5A36 : 0x8E6A40; };
            case 3 -> c = switch (blk) {                                                            // the End: end stone, its pits, obsidian, purpur
                case 0 -> n2 < 0.15f ? 0xBEBA82 : 0xDCD8A0;
                case 1 -> n2 < 0.3f ? 0xB8B47A : 0xCECA92;
                case 2 -> n2 < 0.12f ? 0x3A2058 : 0x140C1E;
                default -> (u % (px / 2) == 0 || v % (px / 2) == 0) ? 0x7A527A : 0xA878A8; };
            case 4 -> c = switch (blk) {                                                            // the Nether: netherrack, basalt, soul soil, magma
                case 0 -> n2 < 0.2f ? 0x5A1E1A : n2 < 0.3f ? 0x8A3A30 : 0x6E2622;
                case 1 -> n2 < 0.3f ? 0x3A3A40 : 0x4A4A52;
                case 2 -> n2 < 0.25f ? 0x3A2A22 : 0x4E3A2E;
                default -> n2 < 0.3f ? 0x02FF8A20 : n2 < 0.45f ? 0x02FFC040 : 0x5A1A10; };
            default -> c = switch (blk) {                                                           // the Rift: purple nylium, mycelium, blackstone, crying obsidian
                case 0 -> n2 < 0.2f ? 0x6A2A5A : 0x4A2A4A;
                case 1 -> n2 < 0.25f ? 0x7A6A7A : 0x5E4E62;
                case 2 -> n2 < 0.2f ? 0x2A1E2A : 0x1E161E;
                default -> n2 < 0.18f ? 0x02A040FF : 0x1A0E26; };
        }
        if ((c >>> 24) == 0x02) return c;                                                           // glowing pixels keep their colour
        return mul(c, sh);
    }
    static void buildGround(Theme th) {
        float[] p = new float[3 * 6 * 60000]; int[] col = new int[6 * 60000]; int n = 0;
        java.util.ArrayList<int[]> cells = new java.util.ArrayList<>(); java.util.ArrayList<float[]> mids = new java.util.ArrayList<>();
        int fc = fogCol(th); int R = (int) Math.ceil(NEAR_R);
        for (int i = -R; i < R; i++) for (int j = -R; j < R; j++) {
            float mx = i + 0.5f, mz = j + 0.5f; if (mx * mx + mz * mz > NEAR_R * NEAR_R) continue;
            float de = (float) Math.hypot(mx - EYE_X, mz - EYE_Z);
            if (mz > EYE_Z - 1.5f) continue;                                                            // under or behind the camera
            int px = de < 9.5f ? 16 : de < 14f ? 8 : 4, blk = blockAt(i, j); int start = n;
            float bright = 0.94f + 0.12f * rnd(i * 41 + j * 13);
            for (int v = 0; v < px; v++) {
                int[] row = new int[px];
                for (int u = 0; u < px; u++) {
                    int c = texel(blk, i, j, u, v, px);
                    float x = i + (u + 0.5f) / px, z = j + (v + 0.5f) / px;
                    if ((c >>> 24) == 0x02) row[u] = c & 0xFFFFFF;                                         // glowing: no light, little fog
                    else row[u] = mixc(mixc(mul(c, 0.84f * bright), 0x1E2A50, 0.12f), fc, fog(x, z));
                }
                float z0 = j + v / (float) px, z1 = j + (v + 1) / (float) px;
                for (int u = 0; u < px; ) {
                    int c0 = row[u], e = u + 1; while (e < px && row[e] == c0) e++;
                    float x0 = i + u / (float) px, x1 = i + e / (float) px; int cc = 0xFF000000 | c0;
                    float[] q = {x0, surf(x0, z0), z0, x1, surf(x1, z0), z0, x1, surf(x1, z1), z1, x0, surf(x0, z0), z0, x1, surf(x1, z1), z1, x0, surf(x0, z1), z1};
                    if (n + 6 > col.length) { p = java.util.Arrays.copyOf(p, p.length * 2); col = java.util.Arrays.copyOf(col, col.length * 2); }
                    System.arraycopy(q, 0, p, n * 3, 18); for (int k = 0; k < 6; k++) col[n + k] = cc; n += 6;
                    u = e;
                }
            }
            cells.add(new int[]{start, n}); mids.add(new float[]{mx, surf(mx, mz), mz});
        }
        gP = p; gC = col; gCells = cells.size(); gCell = new int[gCells * 2]; gMid = new float[gCells * 3];
        for (int k = 0; k < gCells; k++) { gCell[k * 2] = cells.get(k)[0]; gCell[k * 2 + 1] = cells.get(k)[1]; System.arraycopy(mids.get(k), 0, gMid, k * 3, 3); }
        gBoss = boss;
    }
    /** The near ground, solid in the middle, fading only at the window's soft edge (cells wholly outside it are skipped). */
    static void nearGround(Theme th, float dim) {
        if (gBoss != boss || gP == null) buildGround(th);
        for (int k = 0; k < gCells; k++) {
            float mx = gMid[k * 3], my = gMid[k * 3 + 1], mz = gMid[k * 3 + 2];
            if (edge(mx, my, mz) <= 0f && edge(mx - 0.7f, my, mz - 0.7f) <= 0f && edge(mx + 0.7f, my, mz + 0.7f) <= 0f && edge(mx + 0.7f, my, mz - 0.7f) <= 0f && edge(mx - 0.7f, my, mz + 0.7f) <= 0f) continue;
            for (int q = gCell[k * 2], e = gCell[k * 2 + 1]; q < e; q++) {
                float x = gP[q * 3], y = gP[q * 3 + 1], z = gP[q * 3 + 2];
                float vig = 0.9f * sm((edgeR(x, y, z) - 0.38f) / 0.45f) + 0.4f * dim;              // darker toward the window's edge, Butcher's vignette
                int c = gC[q]; if (vig > 0.01f) c = 0xFF000000 | mixc(c & 0xFFFFFF, mixc(fogCol(th), 0x000000, 0.5f), Math.min(1f, vig));
                F.veil.v(x, y, z, c);
            }
        }
    }
    /** The rest of the planet out to its horizon: a smooth curved surface in the fog's colour, fading at the window's edge. */
    static void farGround(Theme th, float dim) {
        int rings = 24, seg = 72; float r0 = NEAR_R - 1.2f, r1 = 34f; int fc = fogCol(th), base = mixc(mul(th.top(), 0.84f), 0x1E2A50, 0.12f);
        for (int r = 0; r < rings; r++) for (int k = 0; k < seg; k++) {
            float ra = lerp(r0, r1, (float) Math.pow(r / (float) rings, 1.6f)), rb = lerp(r0, r1, (float) Math.pow((r + 1) / (float) rings, 1.6f));
            double a0 = k * Math.PI * 2 / seg, a1 = (k + 1) * Math.PI * 2 / seg;
            if (Math.sin((a0 + a1) / 2) > 0.55 && ra < 16f) continue;                                    // the part under the camera is the near ground's
            float[][] pt = {{ra, (float) a0}, {ra, (float) a1}, {rb, (float) a1}, {rb, (float) a0}}; float[] xs = new float[12]; int[] cs = new int[4];
            for (int q = 0; q < 4; q++) {
                float x = (float) Math.cos(pt[q][1]) * pt[q][0], z = (float) Math.sin(pt[q][1]) * pt[q][0], y = surf(x, z) - 0.02f;
                float far = cl((pt[q][0] - r0) / (r1 - r0));
                int c = mixc(mixc(base, fc, fog(x, z)), mixc(fc, 0x000000, 0.3f), far * 0.6f);
                cs[q] = argb(1f, mixc(c, 0x000000, 0.4f * dim)); xs[q * 3] = x; xs[q * 3 + 1] = y; xs[q * 3 + 2] = z;
            }
            wquad(F.veil, xs[0], xs[1], xs[2], cs[0], xs[3], xs[4], xs[5], cs[1], xs[6], xs[7], xs[8], cs[2], xs[9], xs[10], xs[11], cs[3]);
        }
    }

    // ------------------------------------------------------------------ the backdrop: layered silhouettes on the horizon, haze between, bats, rain
    /** Pixel silhouettes ('X' filled, 'o' a lit edge), bottom row last. */
    static final String[][] SIL = {
        {"....XX....", "...XXXX...", "..XXXXXX..", "..XX..XX..", "..XXXXXX..", "..XXXXXX..", "..XXXXXX..", ".XXXXXXXX."},                                    // gravestone
        {"..X......X.", "...X....X..", "X..X...X..X", ".X.XX.XX.X.", "..X.XXX.X..", "....XXX....", ".....XX....", ".....XX....", ".....XX....", ".....XX....", "....XXXX..."},   // dead tree
        {"...XX...", "...XX...", "XXXXXXXX", "XXXXXXXX", "...XX...", "...XX...", "...XX...", "...XX..."},                                                // cross
        {"....X....", "...XXX...", "...XXX...", "..XXXXX..", "...XXX...", "..XXXXX..", ".XXXXXXX.", "..XXXXX..", ".XXXXXXX.", "XXXXXXXXX", "....X....", "....X...."},  // pine
        {"..X..", ".XXX.", ".XXX.", ".XXX.", "XXXXX", "XXXXX", "XXXXX", "XXXXX", "XXXXX", "XXXXX", "XXXXX", "XXXXX", "XXXXX"},                               // spire / pillar
        {".X.X.X.", ".XXXXX.", ".XXXXX.", "..XXX..", "..XXX..", "..XXX..", "..XXX..", "XXXXXXX"},                                                         // tower
        {"..XX..", ".XXXX.", "XXXXXX", "XXXXXX", "..XX..", "..XX..", "..XX.."},                                                                           // mushroom
        {"...X...", "..XXX..", "..XXX..", ".XXXXX.", ".XXXXX.", "XXXXXXX"}};                                                                              // crystal / rock
    static final int[][] SIL_SET = {{1, 0, 2, 0, 1, 2, 0}, {1, 7, 6, 7, 1, 6, 7}, {3, 3, 1, 3, 3, 7, 3}, {4, 7, 4, 4, 7, 4, 7}, {5, 4, 7, 4, 5, 4, 7}, {4, 1, 7, 6, 4, 7, 1}};
    static void sprite(Mesh m, String[] s, float x, float y, float z, float ax, float az, float px, int col, int lit) {
        int H = s.length; for (int r = 0; r < H; r++) {
            String row = s[r]; float y0 = y + (H - 1 - r) * px, y1 = y0 + px; int W = row.length();
            for (int c = 0; c < W; ) {
                char ch = row.charAt(c); int e = c + 1; while (e < W && row.charAt(e) == ch) e++;
                if (ch != '.') {
                    boolean edgeTop = r == 0 || s[r - 1].length() <= c || s[r - 1].charAt(c) == '.';
                    int cc = edgeTop && lit != 0 ? lit : col; float a0 = (c - W / 2f) * px, a1 = (e - W / 2f) * px;
                    wquad(m, x + ax * a0, y0, z + az * a0, cc, x + ax * a1, y0, z + az * a1, cc, x + ax * a1, y1, z + az * a1, cc, x + ax * a0, y1, z + az * a0, cc);
                }
                c = e;
            }
        }
    }
    static void backdrop(float t, Theme th, int energy, float dim) {
        int fc = fogCol(th), sky = mixc(th.skyLow(), 0xFFFFFF, 0.06f);
        float hDist = (float) Math.sqrt(2 * GR * 2.3f);
        float baseAng = (float) Math.atan2(-EYE_X, -EYE_Z);
        for (int layer = 0; layer < 3; layer++) {                                                      // far to near: a ridge of hills, then two rows of shapes
            float D = layer == 0 ? hDist * 1.45f : layer == 1 ? hDist * 1.18f : hDist * 0.92f;
            int col = argb(1f, mixc(mixc(fc, sky, layer == 0 ? 0.32f : layer == 1 ? 0.12f : 0f), 0x000000, 0.35f * dim + (layer == 0 ? 0.12f : layer == 1 ? 0.32f : 0.5f)));
            int lit = argb(1f, mixc(col & 0xFFFFFF, mixc(th.moon(), sky, 0.5f), layer == 2 ? 0.22f : 0.12f));
            if (layer == 0) {                                                                         // stepped pixel hills
                float step = 0.55f; int cols = 70;
                for (int k = 0; k < cols; k++) {
                    float a = baseAng + (k - cols / 2f) * step / D, a2 = baseAng + (k + 1 - cols / 2f) * step / D;
                    float x = EYE_X + (float) Math.sin(a) * D, z = EYE_Z + (float) Math.cos(a) * D, x2 = EYE_X + (float) Math.sin(a2) * D, z2 = EYE_Z + (float) Math.cos(a2) * D;
                    float gy = surf(x, z), hgt = 0.9f + 2.0f * vnoise(k * 0.13f + boss * 5f, 1.7f) + 0.4f * vnoise(k * 0.5f, 3.3f); hgt = Math.round(hgt / 0.28f) * 0.28f;
                    wquad(F.veil, x, gy - 2f, z, col, x2, gy - 2f, z2, col, x2, gy + hgt, z2, col, x, gy + hgt, z, col);
                    wquad(F.veil, x, gy + hgt - 0.28f, z, lit, x2, gy + hgt - 0.28f, z2, lit, x2, gy + hgt, z2, lit, x, gy + hgt, z, lit);
                }
            } else {
                int[] set = SIL_SET[boss]; int count = layer == 1 ? 14 : 9;
                for (int k = 0; k < count; k++) {
                    float off = (k - (count - 1) / 2f) / (count / 2f) + (rnd(k * 13 + layer * 7 + boss) - 0.5f) * 0.12f;
                    if (layer == 2 && Math.abs(off) < 0.3f) continue;                                        // keep the space straight behind the boss open
                    float a = baseAng + off * 0.62f, x = EYE_X + (float) Math.sin(a) * D, z = EYE_Z + (float) Math.cos(a) * D;
                    float ax = (float) Math.cos(a), az = -(float) Math.sin(a);                                // across the line of sight
                    String[] s = SIL[set[(k + layer * 3) % set.length]];
                    float px = (layer == 1 ? 0.24f : 0.3f) * (0.8f + 0.45f * rnd(k * 31 + layer));
                    sprite(F.veil, s, x, surf(x, z) - px * 0.5f, z, ax, az, px, col, lit);
                }
            }
            // haze in front of the layer, thickest at the ground, so each row reads further away than the next
            float hd = D - 1.6f; int hc0 = argb(0.26f * (1f - 0.5f * dim), mixc(fc, sky, 0.3f)), hc1 = argb(0f, sky);
            for (int k = 0; k < 28; k++) {
                float a = baseAng + (k - 14) * 0.06f, a2 = a + 0.06f;
                float x = EYE_X + (float) Math.sin(a) * hd, z = EYE_Z + (float) Math.cos(a) * hd, x2 = EYE_X + (float) Math.sin(a2) * hd, z2 = EYE_Z + (float) Math.cos(a2) * hd, gy = surf(x, z), gy2 = surf(x2, z2);
                wquad(F.veil, x, gy - 0.6f, z, hc0, x2, gy2 - 0.6f, z2, hc0, x2, gy2 + 1.6f, z2, hc1, x, gy + 1.6f, z, hc1);
            }
        }
        // bats flapping across the dusk, red-eyed (graveyard, den, rift)
        if (boss == 0 || boss == 1 || boss == 5) {
            String[] wUp = {"X.....X", "XX.X.XX", ".XXXXX.", "...X..."}, wDn = {"...X...", ".XXXXX.", "XX...XX", "X.....X"};
            int bc = argb(1f, 0x0A0608);
            for (int i = 0; i < 4; i++) {
                float u = ((t * (0.07f + 0.03f * rnd(i * 7)) + rnd(i * 3)) % 1f), dir = i % 2 == 0 ? 1f : -1f;
                float x = dir * (-9f + 18f * u), y = 4.3f + 1.6f * rnd(i * 11) + 0.35f * (float) Math.sin(t * 2.3f + i * 2f), z = -5f - 3f * rnd(i * 5);
                boolean up = ((int) (t * 9 + i * 3)) % 2 == 0; float px = 0.07f;
                sprite(F.veil, up ? wUp : wDn, x, y, z, 1f, 0f, px, bc, 0);
                glow(F.add, x - px * 0.5f, y + px * (up ? 1.5f : 2.5f), z + 0.02f, 0.06f, 0xFF2020, 0.9f); glow(F.add, x + px * 0.5f, y + px * (up ? 1.5f : 2.5f), z + 0.02f, 0.06f, 0xFF2020, 0.9f);
            }
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
            case 1 -> {                                                                           // the Broodfather's den: dead trees in webs, egg sacs, bones, a silk-lined burrow
                Skin bark = (f, u, v, w, hh) -> { float nz = rnd(f * 31 + u * 7 + v * 13 + 3); return f == 2 || f == 3 ? 0x4A3A30 : mul(u % 3 == 0 ? 0x2A221E : 0x3A2E28, 0.85f + nz * 0.3f); };
                Skin moss = (f, u, v, w, hh) -> { float nz = rnd(f * 17 + u * 11 + v * 5 + 9); return f == 2 && nz < 0.6f ? mul(0x4A5A34, 0.8f + nz * 0.4f) : nz < 0.2f ? 0x4A4E40 : mul(0x5E5C60, 0.85f + nz * 0.3f); };
                Skin silk = (f, u, v, w, hh) -> { float nz = rnd(f * 13 + u * 7 + v * 3 + 1); return mul(nz < 0.2f ? 0xC8C0B0 : 0xE8E4DA, 0.88f + nz * 0.2f); };
                Skin bone = (f, u, v, w, hh) -> mul(0xD8D0B8, 0.85f + rnd(f * 5 + u * 3 + v * 11) * 0.2f);
                Skin skull = (f, u, v, w, hh) -> f == 4 && ((v == 2 && (u == 1 || u == 3)) || (v == 0 && u == 2)) ? 0x1A1414 : bone.at(f, u, v, w, hh);
                int web = argb(0.5f, 0xE8ECF8);
                float[][] trees = {{-2.0f, -1.7f, 2.3f, 0.25f}, {2.1f, -1.9f, 2.0f, -0.35f}};
                for (int ti = 0; ti < 2; ti++) {
                    float[] tr = trees[ti]; float gx = tr[0], gz = tr[1], gy = surf(gx, gz), h = tr[2], sd = gx < 0 ? 1f : -1f;
                    push(); M.translate(gx, gy, gz).rotateY(tr[3]);
                    box(-0.17f, 0f, -0.17f, 0.17f, h, 0.17f, bark, 10f, 0.025f);
                    float[][] br = {{0.55f * h, sd, 0.75f, 0.85f}, {0.8f * h, -sd, 0.6f, 0.65f}, {0.95f * h, sd, 0.35f, 0.5f}};
                    for (float[] b : br) { push(); M.translate(0f, b[0], 0f).rotateZ(-b[1] * b[2]); box(-0.07f, 0f, -0.07f, 0.07f, b[3], 0.07f, bark, 10f, 0.02f); pop(); }
                    pop();
                    // a proper web from the trunk out to the lower branch, sagging
                    float wx = gx + sd * 0.15f, wy = gy + 0.55f * h + 0.05f, wz = gz + 0.2f, R = 0.95f;
                    float[] ex = new float[9], ey = new float[9];
                    for (int k = 0; k <= 8; k++) { double an = (sd > 0 ? -0.3 : Math.PI + 0.3) + (sd > 0 ? 1 : -1) * k * (Math.PI * 0.9 / 8); float rr = R * (0.8f + 0.25f * rnd(k * 7 + ti)); ex[k] = (float) Math.cos(an) * rr; ey[k] = (float) Math.sin(an) * rr; }
                    for (int k = 0; k <= 8; k++) threadLine(wx, wy, wz, wx + ex[k], wy + ey[k], wz, web);
                    for (int ring = 1; ring <= 4; ring++) for (int k = 0; k < 8; k++) { float f = ring / 4.4f, sag = 0.04f * ring; threadLine(wx + ex[k] * f, wy + ey[k] * f - sag, wz, wx + ex[k + 1] * f, wy + ey[k + 1] * f - sag, wz, web); }
                    // egg sacs hanging from the top branch on threads, swaying
                    for (int e = 0; e < 2; e++) {
                        float hx = gx + sd * (0.35f + 0.25f * e), hy = gy + 0.85f * h + 0.1f, len = 0.5f + 0.35f * e, sw = 0.12f * (float) Math.sin(t * 1.7f + e * 2 + ti);
                        float ox = hx + sw, oy = hy - len; threadLine(hx, hy, gz, ox, oy, gz, argb(0.6f, 0xE8ECF8));
                        push(); M.translate(ox, oy - 0.16f, gz).rotateZ(sw); box(-0.11f, -0.16f, -0.11f, 0.11f, 0.16f, 0.11f, silk, 14f, 0.02f); pop();
                        glow(F.add, ox, oy - 0.16f, gz + 0.12f, 0.25f, 0xC8FF9A, 0.12f + 0.06f * (float) Math.sin(t * 3 + e));
                    }
                }
                for (float[] r : new float[][]{{-1.2f, -0.9f, 0.32f}, {1.35f, -0.6f, 0.26f}, {-2.6f, -0.4f, 0.4f}}) { push(); M.translate(r[0], surf(r[0], r[1]), r[1]).rotateY(r[0]); box(-r[2], 0f, -r[2] * 0.8f, r[2], r[2] * 1.1f, r[2] * 0.8f, moss, 14f, 0.025f); pop(); }
                push(); M.translate(0.95f, surf(0.95f, 0.9f), 0.9f).rotateY(-0.5f); box(-0.12f, 0f, -0.12f, 0.12f, 0.2f, 0.12f, skull, 20f, 0.015f); pop();
                for (float[] b : new float[][]{{-0.9f, 1.0f, 0.6f}, {0.5f, 1.3f, -0.4f}, {1.3f, 0.4f, 1.2f}}) { push(); M.translate(b[0], surf(b[0], b[1]) + 0.04f, b[1]).rotateY(b[2]); box(-0.25f, 0f, -0.035f, 0.25f, 0.07f, 0.035f, bone, 20f, 0.012f); box(-0.3f, 0f, -0.06f, -0.22f, 0.09f, 0.06f, bone, 20f, 0.012f); box(0.22f, 0f, -0.06f, 0.3f, 0.09f, 0.06f, bone, 20f, 0.012f); pop(); }
                disc(F.veil, 0, 0.03f, 0, 1.15f, 0x060404, 0.9f);                                    // the burrow, lined with silk
                ring(F.veil, 0, 0.04f, 0, 1.05f, 0.08f, 0xE8E4DA, 0.55f);
                for (int k = 0; k < 10; k++) { double an = k * Math.PI * 2 / 10 + 0.2; float r0 = 1.0f, r1 = 1.6f + 0.5f * rnd(k * 3); threadLine((float) Math.cos(an) * r0, 0.04f, (float) Math.sin(an) * r0, (float) Math.cos(an) * r1, surf((float) Math.cos(an) * r1, (float) Math.sin(an) * r1) + 0.03f, (float) Math.sin(an) * r1, argb(0.4f, 0xE8ECF8)); }
                
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
    /** A thin silk thread between two world points, turned to face the camera. */
    static void threadLine(float ax, float ay, float az, float bx, float by, float bz, int c) {
        float dx = bx - ax, dy = by - ay, dz = bz - az; Vector3f side = new Vector3f(dx, dy, dz).cross(eye.x - ax, eye.y - ay, eye.z - az); float l = side.length(); if (l < 1e-5f) return; side.mul(0.012f / l);
        wquad(F.veil, ax + side.x, ay + side.y, az + side.z, c, bx + side.x, by + side.y, bz + side.z, c, bx - side.x, by - side.y, bz - side.z, c, ax - side.x, ay - side.y, az - side.z, c);
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
            case 1 -> { float d = cl(ar * 1.15f); y = 5.5f * (1f - eo(d)) - 0.12f * (float) Math.sin(Math.PI * cl((ar - 0.87f) / 0.25f));      // it drops down on its thread
                float th = 1f - sm((ar - 1.0f) / 0.5f); if (th > 0.01f) threadLine(0f, y + 1.1f, -0.3f, 0f, y + 12f, -0.3f, argb(0.7f * th, 0xE8ECF8)); }
            case 4 -> y = lerp(-2.8f, 0.3f, sm(ar)) + 0.15f * (float) Math.sin(t * 3f);
            default -> y = lerp(-2.8f, 0f, sm(ar)) + 0.08f * (float) Math.sin(Math.PI * cl(ar));
        }
        float hitT = 99f, kb = 0f; for (float hh : hits) { float d = t - hh; if (d >= 0) { hitT = Math.min(hitT, d); kb += 0.12f * (d < 0.07f ? sm(d / 0.07f) : (float) Math.exp(-(d - 0.07f) * 7f)); } }
        float face = (float) Math.atan2(eye.x, eye.z);
        if (show) {
            push(); M.translate(0, y, -kb).rotateY(face + (boss == 1 ? 0.55f : 0f)).rotateX(-0.22f * roarK);                // it rears back as it roars
            float sc = MOB_K_STAR[boss] / 16f * (1f + 0.05f * roarK); M.scale(sc);
            tint = hitT < 0.09f && !gone ? 0.22f * (1f - hitT / 0.09f) : 0f; tintCol = 0xFFE0C0;
            clipY = gone ? -99f : -0.15f;             burstD = gone ? since : -1f; partNo = 0; mobMode = true; rimCol = mixc(energy, 0xFFFFFF, 0.3f);
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
            pop(); burstD = -1f; tint = 0f; crumble = 0f; mobMode = false; clipY = -99f;
        }
        if (roarK > 0.01f) { float u = cl((t - SlayerScene.WALK - 0.15f) / 0.6f); ring(F.add, 0, 0.05f, 0, 0.5f + 4f * eo(u), 0.25f, energy, 0.7f * (1f - u)); }   // the roar's shockwave
        if (!gone && ar > 0.5f) { disc(F.veil, 0, 0.03f, 0, 0.8f * MOB_K_STAR[boss], 0x050508, 0.45f); for (int i = 0; i < 12; i++) { float u = ((t * 0.5f + i / 12f) % 1f), an = i * 2.4f + t * 2f + u * 3f, r = 0.6f * (1f - u * 0.4f); glow(F.add, (float) Math.cos(an) * r, u * 2.4f, (float) Math.sin(an) * r, 0.07f, i % 3 == 0 ? 0xFFFFFF : energy, 0.8f * (1f - u) * sm(u * 5f)); } }
        if (gone) for (int i = 0; i < 40; i++) {                                                  // shattered: the pieces turn to stardust and rise
            float u = cl((since - rnd(i) * 0.3f) / 1.3f); if (u <= 0 || u >= 1) continue;
            float an = rnd(i * 7) * 6.28f, r = 0.3f + u * (1.2f + rnd(i * 3)), yy = 0.3f + rnd(i * 11) * 1.8f + u * (type == 0 ? 0.6f : 1.4f);
            glow(F.add, (float) Math.cos(an) * r, yy, (float) Math.sin(an) * r * 0.7f, 0.06f, type == 0 ? 0x9A9AA8 : i % 3 == 0 ? 0xFFFFFF : energy, (1f - u) * 0.9f);
        }
    }
    static final float[] MOB_K_STAR = {1.4f, 1.5f, 1.85f, 0.85f, 1.4f, 1.35f};
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
