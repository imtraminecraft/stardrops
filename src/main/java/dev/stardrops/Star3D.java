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
        boss = Math.floorMod(bossIn, CREATURES); world = boss < 9 ? boss : WORLD_OF[boss - 9]; type = typeIn; Vox3D.lastAspect = aspect; reseed(t); hits = SlayerScene.HITS[type]; n = hits.length; last = hits[n - 1];
        F = new Frame(); tint = 0f; M = new Matrix4f(); STACK.clear(); mobMode = false; pass = 0; dyn = false; fovNow = FOV;
        float O = AnimationManager.SL_PIVOT, p = t - O;

        // ---- the camera: low on the planet, looking at the boss with the curved horizon and space behind it; a slow push in, a kick on hits
        float kick = 0f; for (int i = 0; i < n; i++) { float d = t - hits[i]; if (d >= 0) kick += (i == n - 1 ? (type == 2 ? 2.2f : 1.1f) : 0.4f) * (float) Math.exp(-d * (i == n - 1 && type == 2 ? 5f : 10f)); }
        float roarK = roar(t);
        float dist = lerp(12.5f, 10f, eo(t / 1.6f)) - 0.6f * sm((t - SlayerScene.WALK) / 1.4f);
        if (type == 2) dist -= 1.2f * sm((t - 2.95f) / 0.42f) * (1f - sm((t - last) / 0.3f));
        dist += 0.7f * sm(p / 0.7f) + (type == 2 && t > last ? 1.6f * (float) Math.exp(-(t - last) * 3.5f) * sm((t - last) / 0.05f) : 0f);   // RNG's last strike blows the camera back
        float yaw = (float) Math.toRadians(12 + 5 * Math.sin(t * 0.3) - 10 * (1f - eo(t / 1.6f)));
        float eyeY = 2.3f + 0.4f * (1f - eo(t / 1.6f));
        Vector3f target = new Vector3f(0f, 1.35f, 0f);
        eye.set((float) Math.sin(yaw) * dist, eyeY, (float) Math.cos(yaw) * dist);
        float sh = Math.min(2.2f, kick + 0.35f * roarK + (type == 2 ? 0.12f * sm((t - HUSH) / 0.4f) * (t < last ? 1f : 0f) : 0f)), jx = 0.06f * sh * (float) (Math.sin(t * 53) + Math.sin(t * 31 + 1.3)), jy = 0.06f * sh * (float) (Math.sin(t * 47 + 2.1) + Math.sin(t * 37 + 0.4));
        Matrix4f view = new Matrix4f().lookAt(eye.x + jx, eye.y + jy, eye.z, target.x + jx * 0.5f, target.y + jy * 0.5f, target.z, 0, 1, 0);
        right.set(view.m00(), view.m10(), view.m20()); up.set(view.m01(), view.m11(), view.m21());
        Matrix4f persp = new Matrix4f().setPerspective((float) Math.toRadians(FOV), aspect, 1000f, 0.05f, z01);
        Vector4f cr = new Matrix4f(persp).mul(view).transform(new Vector4f(PIN.x, PIN.y, PIN.z, 1f));
        shiftX = -cr.x / cr.w; shiftY = pinY - cr.y / cr.w;
        Matrix4f proj = new Matrix4f().translation(shiftX, shiftY, 0f).mul(persp);
        projView = new Matrix4f(proj).mul(view); F.proj = proj; F.view = view;

        float dim = type == 2 ? sm((t - 2.95f) / 0.3f) * (1f - sm((t - last) / 0.25f)) : 0f;
        float sl = t - last; rippleAmp = type > 0 && sl > 0 ? (type == 2 ? 0.42f : 0.2f) * (float) Math.exp(-sl * 1.7f) * sm(sl / 0.05f) : 0f; rippleR = 0.5f + sl * 10.5f; rippleCol = type == 2 ? 0xFFD24A : mixc(energy, 0xFFFFFF, 0.4f);
        goldK = type == 2 && sl > 0 ? sm(sl / 0.2f) * (1f - sm((sl - 3.4f) / 0.8f)) : 0f;
        Theme th = THEMES[world];
        space(t, aspect, energy, th, dim);

        // ---- the giant planet: its curved surface under the boss, the near ground in blocks (built once per boss)
        F.ground = new Mesh(false);
        farGround(th, dim);
        atmosphere(t, th, energy, dim);
        backdrop(t, th, energy, dim);
        nearGround(th, dim);
        if (world != 4 && world != 6) Vox3D.weather(t, world);
        spawn(t, energy, th);

        // ---- the boss
        boss(t, p, energy, roarK);

        // ---- falling stars, and the constellation they leave
        for (int i = 0; i < n; i++) meteorite(t, i, energy, th);
        skyShow(t, aspect);
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
            glow(F.sky, q.x, q.y, q.z, (0.1f + big * 0.26f) * (1f + 0.7f * goldK), mixc(i % 7 == 0 ? 0xC8D8FF : i % 11 == 0 ? 0xFFD8B0 : 0xFFFFFF, 0xFFD24A, 0.8f * goldK), Math.min(1f, (0.3f + 0.7f * big) * tw * (1f - 0.8f * dim) * (0.25f + 0.75f * hz) * (1f + goldK)));
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
        switch (world) {
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
            case 6 -> c = switch (blk) {                                                            // the Crimson Isle: blackstone, basalt, crimson nylium, magma
                case 0 -> n2 < 0.2f ? 0x2A2026 : n2 < 0.3f ? 0x4A3A40 : 0x34282E;
                case 1 -> n2 < 0.3f ? 0x3A3A40 : 0x4A4A52;
                case 2 -> n2 < 0.25f ? 0xA02A2A : 0x7A1E22;
                default -> n2 < 0.3f ? 0x02FF8A20 : n2 < 0.45f ? 0x02FFC040 : 0x5A1A10; };
            case 7 -> c = switch (blk) {                                                            // the dragon's island: end stone, obsidian, a bedrock scar
                case 0 -> n2 < 0.15f ? 0xBEBA82 : 0xDCD8A0;
                case 1 -> n2 < 0.3f ? 0xB8B47A : 0xCECA92;
                case 2 -> n2 < 0.12f ? 0x3A2058 : 0x140C1E;
                default -> n2 < 0.3f ? 0x3A3A3E : 0x55555A; };
            case 8 -> c = switch (blk) {                                                            // the Mines: stone, mithril, cobble, a seam of gemstone
                case 0 -> n2 < 0.2f ? 0x5E646C : 0x70767E;
                case 1 -> n2 < 0.22f ? 0x4AA8A0 : n2 < 0.4f ? 0x3A7A80 : 0x5A8A90;
                case 2 -> (u % (px / 2) == 0 || v % (px / 2) == 0) ? 0x44484E : 0x666A72;
                default -> n2 < 0.05f ? 0x02E040A0 : n2 < 0.1f ? 0x0240E8D8 : n2 < 0.13f ? 0x02FFD040 : 0x4A4E56; };
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
            int px = de < 9.5f ? 8 : 4, blk = blockAt(i, j); int start = n;
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
        gBoss = world;
    }
    /** The near ground, solid in the middle, fading only at the window's soft edge (cells wholly outside it are skipped). */
    static void nearGround(Theme th, float dim) {
        if (gBoss != world || gP == null) buildGround(th);
        int vigCol = mixc(fogCol(th), 0x000000, 0.5f);
        for (int k = 0; k < gCells; k++) {
            float mx = gMid[k * 3], my = gMid[k * 3 + 1], mz = gMid[k * 3 + 2];
            if (edge(mx, my, mz) <= 0f && edge(mx - 0.7f, my, mz - 0.7f) <= 0f && edge(mx + 0.7f, my, mz + 0.7f) <= 0f && edge(mx + 0.7f, my, mz - 0.7f) <= 0f && edge(mx - 0.7f, my, mz + 0.7f) <= 0f) continue;
            for (int q = gCell[k * 2], e = gCell[k * 2 + 1]; q < e; q++) {
                float x = gP[q * 3], y = gP[q * 3 + 1], z = gP[q * 3 + 2];
                float er = edgeR(x, y, z), vig = 0.9f * sm((er - 0.38f) / 0.45f) + 0.4f * dim;       // darker toward the window's edge, Butcher's vignette
                int c = gC[q]; if (vig > 0.01f) c = 0xFF000000 | mixc(c & 0xFFFFFF, vigCol, Math.min(1f, vig));
                if (rippleAmp > 0f) { float rp = ripple((float) Math.sqrt(x * x + z * z)); if (rp > 0.004f) { y += rp; c = 0xFF000000 | mixc(c & 0xFFFFFF, rippleCol, Math.min(0.7f, rp / rippleAmp * 0.7f)); } }   // the last strike's wave lifts the ground and lights its crest
                F.veil.vf(x, y, z, c, er >= 9f ? 0f : 1f - sm((er - 0.72f) / 0.27f));             // the fade from the same projection (edge() would project the point again)
            }
        }
    }
    /** The rest of the planet out to its horizon: a smooth curved surface in the fog's colour, fading at the window's edge. */
    static void farGround(Theme th, float dim) {
        int rings = 18, seg = 48; float r0 = NEAR_R - 1.2f, r1 = 34f; int fc = fogCol(th), base = mixc(mul(th.top(), 0.84f), 0x1E2A50, 0.12f);
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
    static final int[][] SIL_SET = {{1, 0, 2, 0, 1, 2, 0}, {1, 7, 6, 7, 1, 6, 7}, {3, 3, 1, 3, 3, 7, 3}, {4, 7, 4, 4, 7, 4, 7}, {5, 4, 7, 4, 5, 4, 7}, {4, 1, 7, 6, 4, 7, 1},
        {5, 4, 6, 4, 5, 7, 4}, {4, 4, 7, 4, 4, 7, 4}, {7, 5, 7, 4, 7, 7, 5}};
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
                    float gy = surf(x, z), hgt = 0.9f + 2.0f * vnoise(k * 0.13f + world * 5f, 1.7f) + 0.4f * vnoise(k * 0.5f, 3.3f); hgt = Math.round(hgt / 0.28f) * 0.28f;
                    wquad(F.veil, x, gy - 2f, z, col, x2, gy - 2f, z2, col, x2, gy + hgt, z2, col, x, gy + hgt, z, col);
                    wquad(F.veil, x, gy + hgt - 0.28f, z, lit, x2, gy + hgt - 0.28f, z2, lit, x2, gy + hgt, z2, lit, x, gy + hgt, z, lit);
                }
            } else {
                int[] set = SIL_SET[world]; int count = layer == 1 ? 14 : 9;
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
        if (world == 0 || world == 1 || world == 5 || world == 8) {
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
        switch (world) {
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
            case 6 -> {                                                                           // the Crimson Isle: basalt columns, a lava pool it climbs out of, lava falling behind, huge crimson fungi
                Skin basalt = (f, u, v, w, hh) -> { float nz = rnd(f * 13 + u * 31 + v * 3); return f == 2 ? 0x55555E : mul(u % 2 == 0 ? 0x3A3A42 : 0x2E2E36, 0.85f + nz * 0.3f); };
                Skin stem = (f, u, v, w, hh) -> { float nz = rnd(f * 7 + u * 13 + v * 5); return nz < 0.15f ? 0x02FF6A3A : mul(0x6A2A4A, 0.85f + nz * 0.3f); };
                Skin cap = (f, u, v, w, hh) -> { float nz = rnd(f * 11 + u * 5 + v * 9); return nz < 0.1f ? 0x02FFB040 : mul(0xA02828, 0.8f + nz * 0.35f); };
                for (float[] pl : new float[][]{{-2.3f, -1.4f, 1.7f}, {-1.75f, -1.9f, 2.4f}, {2.5f, -1.5f, 1.4f}, {-2.75f, -0.9f, 0.9f}}) { float gy = surf(pl[0], pl[1]); push(); M.translate(pl[0], gy, pl[1]); box(-0.27f, 0f, -0.27f, 0.27f, pl[2], 0.27f, basalt, 8f, 0.03f); pop(); }
                { float fx = 2.0f, fz = -2.1f, gy = surf(fx, fz); push(); M.translate(fx, gy, fz); box(-0.14f, 0f, -0.14f, 0.14f, 1.7f, 0.14f, stem, 10f, 0.025f); box(-0.7f, 1.7f, -0.7f, 0.7f, 2.1f, 0.7f, cap, 6f, 0.03f); box(-0.45f, 2.1f, -0.45f, 0.45f, 2.4f, 0.45f, cap, 6f, 0.03f); pop(); }
                float pulse = 0.5f + 0.5f * (float) Math.sin(t * 3f);
                if (boss == 6 || boss == BAL) { disc(F.add, 0, 0.04f, 0, 1.6f, mixc(0xFF6A10, 0xFFC040, pulse), 0.8f); glow(F.add, 0, 0.5f, 0, 2.4f, 0xFF7A20, 0.3f); ring(F.add, 0, 0.05f, 0, 1.6f, 0.12f, 0xFFE080, 0.6f); }
                else { disc(F.veil, 0, 0.03f, 0, 1.5f, 0x0A0606, 0.7f); ring(F.add, 0, 0.05f, 0, 1.45f, 0.1f, mixc(0xFF6A10, 0xFFC040, pulse), 0.7f); for (int k = 0; k < 8; k++) { double an = k * Math.PI / 4 + t * 0.4; glow(F.add, (float) Math.cos(an) * 1.45f, 0.12f, (float) Math.sin(an) * 1.45f, 0.16f + 0.05f * pulse, 0xFF8A20, 0.8f); } }   // the minibosses stand in a ring of embers
                for (int k = 0; k < 2; k++) { float lx = k == 0 ? -1.75f : 2.5f, lz = k == 0 ? -1.9f : -1.5f, top = surf(lx, lz) + (k == 0 ? 2.4f : 1.4f);      // lava running down two of the columns
                    column(F.add, lx, surf(lx, lz), lz + 0.29f, top, 0.16f, 0xFF7A1A, 0.75f); for (int q = 0; q < 5; q++) { float u = ((t * 0.8f + q / 5f + k * 0.3f) % 1f); glow(F.add, lx, top - u * (top - surf(lx, lz)), lz + 0.3f, 0.1f, 0xFFE080, 0.9f); } }
                for (int i = 0; i < 22; i++) { float u = ((t * 0.3f + rnd(i * 3)) % 1f); glow(F.add, (rnd(i * 7) - 0.5f) * 7f, u * 5f, (rnd(i * 11) - 0.5f) * 4f, 0.05f, i % 2 == 0 ? 0xFFB040 : 0xFF6020, (1f - u) * 0.9f); }
                burstUp(t, rise, new int[]{0xFF8A20, 0x34282E, 0xFFC040});
            }
            case 7 -> {                                                                           // the dragon's island: obsidian towers, their crystals beaming at the dragon, the dead portal behind
                Skin obs = (f, u, v, w, hh) -> { float nz = rnd(f * 31 + u * 7 + v * 13); return nz < 0.15f ? 0x3A1E5A : mul(0x140C1E, 0.8f + nz * 0.4f); };
                Skin bed = (f, u, v, w, hh) -> { float nz = rnd(f * 5 + u * 17 + v * 3); return mul(nz < 0.3f ? 0x2A2A2E : 0x4A4A50, 0.85f + nz * 0.3f); };
                float fly = cl((t - SlayerScene.RISE) / 0.95f), dy = 7.5f * (1f - eo(cl(fly * 0.95f))) + 1.9f;
                for (float[] pl : new float[][]{{-2.3f, -1.8f, 2.6f}, {2.4f, -2.1f, 2.1f}, {-1.2f, -3.0f, 3.3f}}) {
                    float gy = surf(pl[0], pl[1]); push(); M.translate(pl[0], gy, pl[1]); box(-0.4f, 0f, -0.4f, 0.4f, pl[2], 0.4f, obs, 6f, 0.03f);
                    M.translate(0f, pl[2] + 0.35f + 0.06f * (float) Math.sin(t * 2 + pl[0]), 0f).rotateXYZ(t * 1.3f, t * 1.7f, 0.6f); box(-0.17f, -0.17f, -0.17f, 0.17f, 0.17f, 0.17f, (f, u, v, w, hh) -> (u + v) % 2 == 0 ? 0x02FF80F0 : 0x02FFFFFF, 4f, 0.02f); pop();
                    glow(F.add, pl[0], gy + pl[2] + 0.35f, pl[1], 0.6f, 0xE070FF, 0.5f);
                    if (t < last && boss == 7) { Vector3f a = new Vector3f(pl[0], gy + pl[2] + 0.35f, pl[1]), b = new Vector3f(0f, dy, -0.2f); line3(a, b, 0xFF90F0, 0.55f + 0.2f * (float) Math.sin(t * 20 + pl[0])); line3(new Vector3f(a).add(0, 0.03f, 0), new Vector3f(b).add(0, 0.03f, 0), 0xFFFFFF, 0.35f); }   // the crystals heal it, until it is gone
                }
                push(); M.translate(0.2f, surf(0.2f, -1.6f), -1.6f); box(-0.5f, 0f, -0.5f, 0.5f, 0.28f, 0.5f, bed, 8f, 0.03f); box(-0.14f, 0.28f, -0.14f, 0.14f, 1.1f, 0.14f, bed, 8f, 0.03f); pop();
                for (int i = 0; i < 26; i++) { float u = ((t * 0.25f + rnd(i * 3)) % 1f); glow(F.add, (rnd(i * 7) - 0.5f) * 8f, u * 5f, (rnd(i * 11) - 0.5f) * 5f, 0.05f, i % 3 == 0 ? 0xFFFFFF : 0xD060FF, (1f - u) * 0.8f); }
            }
            case 8 -> {                                                                           // the Mines: gemstone clusters glowing in the rock, mithril ore, a mine cart on its rails, a lantern
                Skin rock = (f, u, v, w, hh) -> { float nz = rnd(f * 17 + u * 11 + v * 5 + 9); return nz < 0.12f ? 0x4AA8A0 : mul(0x5E646C, 0.8f + nz * 0.35f); };
                Skin wood = noisy(0x5A4028, 0.14f, 9); Skin rail = (f, u, v, w, hh) -> u % 3 == 0 ? 0x5A4028 : 0x9A9AA2;
                int[] gems = {0xE04060, 0xA050F0, 0x40D8A0, 0x40A0F0, 0xFFC040};
                float[][] cl = {{-2.2f, -1.5f, 0.9f, 0}, {2.3f, -1.8f, 1.1f, 1}, {-1.3f, -2.6f, 0.8f, 2}, {2.9f, -0.5f, 0.6f, 3}, {-2.9f, -0.3f, 0.55f, 4}};
                for (float[] g : cl) { float gy = surf(g[0], g[1]); int gc = gems[(int) g[3]]; push(); M.translate(g[0], gy, g[1]); box(-0.42f, 0f, -0.36f, 0.42f, 0.32f, 0.36f, rock, 10f, 0.03f);
                    for (int q = 0; q < 4; q++) { push(); M.translate((q - 1.5f) * 0.17f, 0.25f, (rnd(q + (int) g[3] * 3) - 0.5f) * 0.3f).rotateZ((q - 1.5f) * 0.28f).rotateX((rnd(q * 7 + (int) g[3]) - 0.5f) * 0.5f); float hh2 = g[2] * (0.55f + 0.5f * rnd(q * 5 + (int) g[3]));
                        box(-0.075f, 0f, -0.075f, 0.075f, hh2, 0.075f, (f, u, v, w, hh) -> (u + v + f) % 3 == 0 ? 0x02000000 | mixc(gc, 0xFFFFFF, 0.5f) : 0x02000000 | gc, 6f, 0.02f); pop(); }
                    pop(); glow(F.add, g[0], gy + 0.6f, g[1], 0.9f, gc, 0.35f + 0.12f * (float) Math.sin(t * 2.5f + g[3])); }
                push(); M.translate(1.35f, surf(1.35f, 1.0f), 1.0f).rotateY(0.5f); box(-0.9f, 0f, -0.2f, 0.9f, 0.04f, -0.16f, rail, 10f, 0.012f); box(-0.9f, 0f, 0.16f, 0.9f, 0.04f, 0.2f, rail, 10f, 0.012f);
                box(-0.26f, 0.06f, -0.2f, 0.26f, 0.36f, 0.2f, (f, u, v, w, hh) -> f == 2 ? 0x2A2A30 : mul(0x7A7A84, 0.85f + rnd(u * 3 + v * 7) * 0.3f), 14f, 0.02f); box(-0.16f, 0.34f, -0.12f, 0.16f, 0.46f, 0.12f, (f, u, v, w, hh) -> (u + v) % 2 == 0 ? 0x0240E8D8 : 0x4AA8A0, 12f, 0.015f); pop();
                push(); M.translate(-1.2f, surf(-1.2f, 0.9f), 0.9f); box(-0.04f, 0f, -0.04f, 0.04f, 1.0f, 0.04f, wood, 12f, 0.015f); box(-0.04f, 0.94f, -0.04f, 0.26f, 1.0f, 0.04f, wood, 12f, 0.015f); box(0.14f, 0.7f, -0.07f, 0.28f, 0.9f, 0.07f, (f, u, v, w, hh) -> v == 0 || v == hh - 1 ? 0x3A3A40 : 0x02FFC860, 12f, 0.015f); pop();
                glow(F.add, -0.99f, surf(-1.2f, 0.9f) + 0.8f, 0.9f, 0.7f, 0xFFC860, 0.4f);
                burstUp(t, rise, new int[]{0x5E646C, 0x4AA8A0, 0x70767E});
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
            case 6, BAL -> y = lerp(-2.6f, 0f, sm(ar));                                                  // out of the lava
            case ASHFANG -> y = lerp(-2.8f, 0.3f, sm(ar)) + 0.15f * (float) Math.sin(t * 3f);
            case GHOST -> { show = ar > 0.7f || ((int) (t * 20)) % 2 == 0; y = 0.1f; }                      // it flickers into being
            case ZEALOT -> show = ar > 0.6f || ((int) (t * 24)) % 3 != 0;
            case 7 -> { float d = cl(ar * 0.95f); y = 7.5f * (1f - eo(d)); }                             // the dragon swoops down out of the sky
            default -> y = lerp(-2.8f, 0f, sm(ar)) + 0.08f * (float) Math.sin(Math.PI * cl(ar));
        }
        float hitT = 99f, kb = 0f; for (float hh : hits) { float d = t - hh; if (d >= 0) { hitT = Math.min(hitT, d); kb += 0.12f * (d < 0.07f ? sm(d / 0.07f) : (float) Math.exp(-(d - 0.07f) * 7f)); } }
        float face = (float) Math.atan2(eye.x, eye.z);
        if (show) {
            float lookUp = type == 2 && !gone ? sm((t - HUSH) / 0.25f) : 0f;                                             // RNG: it looks up at what is gathering over it
            push(); M.translate(0, y, -kb).rotateY(face + (boss == 1 ? 0.55f : boss == 7 ? 0.42f : 0f)).rotateX(-0.22f * roarK - 0.3f * lookUp);                // it rears back as it roars
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
                case 6 -> Vox3D.magma(t, idle);
                case 7 -> Vox3D.dragon(t, idle);
                case 8 -> Vox3D.automaton(t, idle);
                case BLADESOUL -> Vox3D.bladesoul(t, idle);
                case DUKE -> Vox3D.duke(t, idle);
                case ASHFANG -> Vox3D.ashfang(t);
                case MAGE -> Vox3D.mage(t, idle);
                case GHOST -> Vox3D.ghost(t, idle);
                case WALKER -> Vox3D.walker(t, idle);
                case BAL -> Vox3D.magma(t, idle);
                case ZEALOT -> Vox3D.zealot(t, idle);
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
    static final float[] MOB_K_STAR = {1.4f, 1.5f, 1.85f, 0.85f, 1.4f, 1.35f, 1.45f, 1.0f, 1.2f, 1.3f, 1.35f, 1.25f, 1.35f, 1.4f, 1.4f, 1.75f, 0.85f};
    /** Creatures 9 and up stand in one of the first nine worlds: Bladesoul, Barbarian Duke X, Ashfang, the Mage Outlaw (Crimson Isle), a Ghost, an Ice Walker (the Mines), Bal (his lava), a special Zealot (the End). */
    static final int BLADESOUL = 9, DUKE = 10, ASHFANG = 11, MAGE = 12, GHOST = 13, WALKER = 14, BAL = 15, ZEALOT = 16, CREATURES = 17;
    static final int[] WORLD_OF = {6, 6, 6, 6, 8, 8, 6, 7};
    static int world;                                                            // the scenery, ground and sky (0..8); boss is the creature
    static void line(float x0, float y0, float x1, float y1, float z, int rgb, float a) {
        float dx = x1 - x0, dy = y1 - y0, l = (float) Math.sqrt(dx * dx + dy * dy); if (l < 1e-4f) return; float w = 0.025f, nx = -dy / l * w, ny = dx / l * w;
        int c = argb(a, rgb);
        wquad(F.add, x0 + nx, y0 + ny, z, c, x1 + nx, y1 + ny, z, c, x1 - nx, y1 - ny, z, c, x0 - nx, y0 - ny, z, c);
    }
    static void line3(Vector3f a, Vector3f b, int rgb, float al) {
        Vector3f d = new Vector3f(b).sub(a); Vector3f side = new Vector3f(d).cross(new Vector3f(eye).sub(a)).normalize().mul(0.03f); int c = argb(al, rgb);
        wquad(F.add, a.x + side.x, a.y + side.y, a.z + side.z, c, b.x + side.x, b.y + side.y, b.z + side.z, c, b.x - side.x, b.y - side.y, b.z - side.z, c, a.x - side.x, a.y - side.y, a.z - side.z, c);
    }

    // ------------------------------------------------------------------ meteorites
    static final float[][] RING6 = {{180, 2.25f}, {0, 2.25f}, {236, 2.5f}, {304, 2.5f}, {128, 2.05f}, {52, 2.05f}};      // RNG: six star-fires round the boss (angle, radius)
    static final float[][] NEAR3 = {{-1.75f, 0.75f}, {1.8f, 0.5f}, {-0.95f, 1.6f}};                                    // a drop: the near misses
    static final Vector3f GOLD = new Vector3f(0f, 3.55f, -0.2f);                                                      // where RNG's last one gathers before it falls
    static final float HUSH = 2.94f, SLAM = 0.13f;
    /** Strike i lands on the boss itself (the last one always does), or on the ground beside it. */
    static boolean onBoss(int i) { return type == 0 || i == n - 1 || (type == 1 && i % 2 == 1); }
    static Vector3f impact(int i) {
        boolean big = type == 0 || i == n - 1;
        if (big) return new Vector3f(0f, 1.4f, 0.1f);
        if (onBoss(i)) return new Vector3f((rnd(i * 31 + 3) - 0.5f) * 0.9f, 0.7f + rnd(i * 17 + 5) * 1.4f, 0.15f);
        if (type == 2) { double an = Math.toRadians(RING6[i][0]); float x = (float) Math.cos(an) * RING6[i][1], z = (float) Math.sin(an) * RING6[i][1]; return new Vector3f(x, surf(x, z), z); }
        float[] q = NEAR3[(i / 2) % NEAR3.length]; return new Vector3f(q[0], surf(q[0], q[1]), q[1]);
    }
    static float size(int i) { boolean big = type == 0 || i == n - 1; return big ? (type == 2 ? 2.3f : type == 1 ? 1.45f : 1.25f) : type == 2 ? 0.8f + 0.09f * i : 0.85f; }
    /** Where meteorite i comes from: high, to one side and behind, so it burns across the sky toward us. */
    static Vector3f from(int i, Vector3f a) {
        float side = type == 2 && i < n - 1 ? (a.x < 0 ? -1f : 1f) : i % 2 == 0 ? 1f : -1f;
        return new Vector3f(a.x + side * (3.2f + 1.6f * rnd(i * 7 + 1)), a.y + 7.5f + 2f * rnd(i * 5), a.z - 3.5f - 1.5f * rnd(i * 3));
    }
    /** A chunk of star-rock: a lumpy outlined boulder with glowing cracks in its colour, spinning. */
    static void rock(float x, float y, float z, float r, int crack, float spin, int seed, boolean gold) {
        int stone = gold ? 0x7A5A1E : 0x3E2E2A, stone2 = gold ? 0xC89A2E : 0x58423A;
        Skin sk = (f, u, v, w, h) -> { float nz = rnd(seed * 13 + f * 31 + u * 7 + v * 13); boolean cr = (u * 3 + v * 5 + f * 2 + seed) % 7 == 0 || nz < 0.1f; return cr ? 0x02000000 | crack : nz > 0.8f ? stone2 : mul(stone, 0.8f + nz * 0.4f); };
        push(); M.translate(x, y, z).rotateXYZ(spin * 5f + seed, spin * 3.4f, seed * 1.7f);
        float px = 5f / (2f * r), ol = Math.max(0.018f, r * 0.11f);
        box(-r, -r, -r, r, r, r, sk, px, ol);
        float[][] lumps = {{0.7f, 0.5f, 0.2f, 0.55f}, {-0.6f, -0.4f, 0.6f, 0.5f}, {0.1f, -0.7f, -0.6f, 0.45f}, {-0.5f, 0.6f, -0.5f, 0.4f}};
        for (float[] l : lumps) { float lr = r * l[3]; box(l[0] * r - lr, l[1] * r - lr, l[2] * r - lr, l[0] * r + lr, l[1] * r + lr, l[2] * r + lr, sk, 3f / (2f * lr), ol); }
        pop();
        glow(F.add, x, y, z, r * 2.2f, crack, 0.55f); glow(F.add, x, y, z, r * 1.1f, 0xFFFFFF, 0.45f);
    }
    static Vector3f along(Vector3f s0, Vector3f a, float u) { float e = u <= 0 ? 0 : u * u * (0.35f + 0.65f * u); return new Vector3f(lerp(s0.x, a.x, e), lerp(s0.y, a.y, e), lerp(s0.z, a.z, e)); }
    /** The burning trail behind a meteorite at u (0 far .. 1 landed): a white-hot core, flame in its colour, pixel embers, smoke that hangs. */
    static void trail(Vector3f s0, Vector3f a, float u, float k, int col, int i, float t, float fade, boolean rainbowTail) {
        int deep = mixc(col, 0xFF4A1A, 0.45f);
        for (int q = 30; q >= 1; q--) {
            float uu = u - q * 0.022f; if (uu < 0) continue; Vector3f p = along(s0, a, uu); float f = 1f - q / 31f;
            float wob = 0.05f * k * (1f - f) * 3f, ox = wob * (float) Math.sin(t * 31 + q * 1.7f + i), oy = wob * (float) Math.cos(t * 27 + q * 2.3f + i);
            if (q > 12 && q % 2 == 0) glow(F.veil, p.x + ox, p.y + oy + (1f - f) * 0.25f, p.z, (0.2f + 0.45f * (1f - f)) * k, 0x1A1416, 0.32f * f * fade);                 // smoke
            int fc = rainbowTail ? Vox3D.rainbow(q / 31f + t * 1.5f) : q < 7 ? mixc(col, 0xFFFFFF, 0.55f) : q < 16 ? col : deep;
            glow(F.add, p.x + ox, p.y + oy, p.z, (0.1f + 0.34f * f * f) * k, fc, 0.75f * f * fade);
            if (q % 3 == 0) {                                                                       // embers: little squares shed from the rock
                float e = (rnd(i * 19 + q) - 0.5f), s = 0.028f * k * (0.5f + f);
                push(); M.translate(p.x + e * 0.5f * k * (1f - f), p.y + (rnd(i * 23 + q) - 0.5f) * 0.4f * k * (1f - f) - (1f - f) * 0.3f, p.z).rotateXYZ(t * 9 + q, t * 7, q);
                box(-s, -s, -s, s, s, s, flat(0x02000000 | (q % 2 == 0 ? 0xFFFFFF : col)), 1f, 0f); pop();
            }
            if (q % 5 == 0) glint(F.add, p.x + (rnd(i * 13 + q) - 0.5f) * 0.35f * k, p.y + (rnd(i * 29 + q) - 0.5f) * 0.3f * k, p.z, 0.16f * k * f, 0xFFFFFF, f * fade);
        }
    }
    /** Ground thrown up by a strike at (x, z): outlined chunks of the planet's own blocks. */
    static void debris(float x, float z, float d, float k, int count, int seed, Theme th) {
        if (d < 0 || d > 1.3f) return; int[] cols = {th.top(), th.dirt(), mul(th.top(), 0.7f), th.dirt()};
        for (int q = 0; q < count; q++) {
            float dd = d - rnd(seed + q * 3) * 0.06f; if (dd < 0) continue;
            float an = rnd(seed * 7 + q * 7) * 6.2832f, v = (0.8f + rnd(seed + q * 11) * 1.9f) * k, vy = (2.6f + rnd(seed + q * 13) * 3.4f) * (0.7f + 0.3f * k);
            float px = x + (float) Math.cos(an) * v * dd, pz = z + (float) Math.sin(an) * v * dd * 0.8f, y = surf(px, pz) + 0.1f + vy * dd - 9.8f * dd * dd * 0.5f;
            if (y < surf(px, pz) + 0.04f) y = surf(px, pz) + 0.04f;
            float s = (0.06f + 0.07f * rnd(seed + q)) * k * (1f - sm((dd - 0.85f) / 0.4f)) * sm(edge(px, y, pz) * 2f); if (s < 0.01f) continue;
            push(); M.translate(px, y, pz).rotateXYZ(dd * 7 + q, dd * 5, q); box(-s, -s, -s, s, s, s, flat(cols[q % cols.length]), 2f, 0.018f); pop();
        }
    }
    /** How far RNG's ground wave has run, and its height at radius r (0 when there is none). */
    static float rippleAmp, rippleR, goldK; static int rippleCol;
    static float ripple(float r) { if (rippleAmp <= 0f) return 0f; float q = (r - rippleR) / 0.85f; return rippleAmp * (float) Math.exp(-q * q); }
    /** Cracks of light running out over the ground from the last strike. */
    static void fissures(float d, int col, int count, float len, float a) {
        if (d < 0 || a < 0.01f) return; float grow = eo(d / 0.3f);
        for (int k = 0; k < count; k++) {
            float an = k * 6.2832f / count + (rnd(k * 17 + 3) - 0.5f) * 0.5f, L = len * (0.6f + 0.6f * rnd(k * 5 + 1)) * grow; int steps = 7;
            Vector3f prev = null;
            for (int s = 0; s <= steps; s++) {
                float f = s / (float) steps, r = 0.35f + L * f, aa = an + (s == 0 ? 0 : (rnd(k * 31 + s * 7) - 0.5f) * 0.42f);
                float x = (float) Math.cos(aa) * r, z = (float) Math.sin(aa) * r; Vector3f pt = new Vector3f(x, surf(x, z) + 0.035f + ripple(r), z);
                if (prev != null) { float fa = a * (1f - f * 0.75f); wideLine(prev, pt, 0.085f * (1f - f * 0.6f), col, 0.35f * fa); wideLine(prev, pt, 0.028f * (1f - f * 0.5f), 0xFFFFFF, 0.9f * fa); }
                if (s == 3 && k % 2 == 0) {                                                             // a branch
                    float ba = aa + (k % 4 == 0 ? 0.7f : -0.7f), bl = L * 0.3f; float bx = x + (float) Math.cos(ba) * bl, bz = z + (float) Math.sin(ba) * bl;
                    wideLine(pt, new Vector3f(bx, surf(bx, bz) + 0.035f, bz), 0.04f, col, 0.5f * a);
                }
                prev = pt;
            }
        }
    }
    /** Light over the whole window, soft at its edge like everything else (built on the window's own rings, so no hard rectangle). */
    static void flash(int rgb, float a) {
        if (a < 0.01f) return; int rings = 7, seg = 28; float asp = Vox3D.lastAspect;
        for (int r = 0; r < rings; r++) for (int k = 0; k < seg; k++) {
            float r0 = r / (float) rings, r1 = (r + 1) / (float) rings; double a0 = k * Math.PI * 2 / seg, a1 = (k + 1) * Math.PI * 2 / seg;
            Vector3f p0 = onSky((float) Math.cos(a0) * r0, (float) Math.sin(a0) * r0, 3f, asp), p1 = onSky((float) Math.cos(a1) * r0, (float) Math.sin(a1) * r0, 3f, asp), q0 = onSky((float) Math.cos(a0) * r1, (float) Math.sin(a0) * r1, 3f, asp), q1 = onSky((float) Math.cos(a1) * r1, (float) Math.sin(a1) * r1, 3f, asp);
            int c0 = argb(a * (1f - sm((r0 - 0.35f) / 0.6f)), rgb), c1 = argb(a * (1f - sm((r1 - 0.35f) / 0.6f)), rgb);
            wquad(F.add, p0.x, p0.y, p0.z, c0, p1.x, p1.y, p1.z, c0, q1.x, q1.y, q1.z, c1, q0.x, q0.y, q0.z, c1);
        }
    }
    /** A flat line of light lying on the ground. */
    static void wideLine(Vector3f a, Vector3f b, float w, int rgb, float al) {
        float dx = b.x - a.x, dz = b.z - a.z, l = (float) Math.sqrt(dx * dx + dz * dz); if (l < 1e-4f || al < 0.01f) return; float nx = -dz / l * w, nz = dx / l * w; int c = argb(al, rgb), c0 = argb(0, rgb);
        wquad(F.add, a.x, a.y, a.z, c, b.x, b.y, b.z, c, b.x + nx, b.y, b.z + nz, c0, a.x + nx, a.y, a.z + nz, c0);
        wquad(F.add, a.x, a.y, a.z, c, b.x, b.y, b.z, c, b.x - nx, b.y, b.z - nz, c0, a.x - nx, a.y, a.z - nz, c0);
    }
    /** A star-fire: the pillar of light a tier's strike leaves burning (RNG), which the last strike drinks. */
    static void starFire(float t, int i, Vector3f a, int col, float since) {
        float hush = sm((t - HUSH) / 0.3f), goldD = t - last, alive = sm(since / 0.18f) * (1f - sm((goldD - 0.5f) / 0.9f)); if (alive < 0.01f) return;
        int c = goldD > 0 ? mixc(col, 0xFFD24A, sm(goldD / 0.15f)) : col; float flare = goldD > 0 ? 1f + 1.2f * (float) Math.exp(-goldD * 5f) : 1f;
        float h = 2.6f + 0.25f * (float) Math.sin(t * 5 + i) + 0.6f * hush;
        column(F.add, a.x, a.y, a.z, a.y + h, 0.3f * flare, c, 0.5f * alive); column(F.add, a.x, a.y, a.z, a.y + h * 0.8f, 0.09f * flare, 0xFFFFFF, 0.7f * alive);
        disc(F.add, a.x, a.y + 0.05f, a.z, 0.85f, c, 0.4f * alive);
        for (int q = 0; q < 7; q++) { float u = ((t * 1.1f + q / 7f + i * 0.13f) % 1f), an = q * 2.1f + t * 3f; glow(F.add, a.x + (float) Math.cos(an) * 0.16f, a.y + u * h, a.z + (float) Math.sin(an) * 0.16f, 0.07f, q % 2 == 0 ? 0xFFFFFF : c, (1f - u) * alive); }
        for (int q = 0; q < 4; q++) { float fl = 0.22f + 0.1f * (float) Math.sin(t * 17 + q * 2 + i); glow(F.add, a.x + (rnd(q + i * 3) - 0.5f) * 0.2f, a.y + 0.12f + q * 0.11f, a.z, fl * (1f - q * 0.18f), q == 0 ? 0xFFFFFF : c, 0.6f * alive); }
        if (hush > 0.01f && goldD < 0) for (int q = 0; q < 9; q++) {                                    // its light streaming up into the gathering star
            float u = ((t * 2.2f + q / 9f + i * 0.21f) % 1f), e = u * u; Vector3f top = new Vector3f(a.x, a.y + h, a.z);
            float mx = lerp(top.x, GOLD.x, e), my = lerp(top.y, GOLD.y, (float) Math.sqrt(u)), mz = lerp(top.z, GOLD.z, e);
            glow(F.add, mx, my, mz, 0.08f * (1f - u * 0.5f), q % 3 == 0 ? 0xFFFFFF : c, hush * (0.4f + 0.6f * u));
        }
    }
    static void meteorite(float t, int i, int energy, Theme th) {
        boolean big = type == 0 || i == n - 1, gold = type == 2 && big, ground = !onBoss(i);
        float h = hits[i], fl = SlayerScene.flight(type, i) + 0.14f, t0 = h - fl, k = size(i); int col = starCol(i, energy);
        Vector3f a = impact(i), s0 = from(i, a);
        if (gold) {                                                                                 // RNG's last: it gathers over the boss while everything holds its breath, then falls like a hammer
            float g = sm((t - HUSH) / (last - SLAM - HUSH)); if (t < HUSH) return;
            if (t < last - SLAM) {
                float pulse = 1f + 0.12f * (float) Math.sin(t * 40);
                glow(F.add, GOLD.x, GOLD.y, GOLD.z, (0.5f + 2.3f * g) * pulse, 0xFFD24A, 0.5f * g); glow(F.add, GOLD.x, GOLD.y, GOLD.z, 0.2f + 0.7f * g, 0xFFFFFF, 0.8f * g);
                glint(F.add, GOLD.x, GOLD.y, GOLD.z, (0.6f + 1.5f * g) * pulse, 0xFFF4C8, g);
                rock(GOLD.x, GOLD.y, GOLD.z, 0.1f + 0.34f * g, 0xFFE89A, t * 2.2f, 7, true);
                for (int q = 0; q < 16; q++) { float u = ((t * 2.6f + q / 16f) % 1f), an = q * 2.4f + t * 2f, r = (1f - u) * 2.2f; glow(F.add, GOLD.x + (right.x * (float) Math.cos(an) + up.x * (float) Math.sin(an)) * r, GOLD.y + (right.y * (float) Math.cos(an) + up.y * (float) Math.sin(an)) * r, GOLD.z, 0.06f, q % 3 == 0 ? 0xFFFFFF : 0xFFD24A, u * g); }
                disc(F.add, 0f, 0.05f, 0f, 0.6f + 1.6f * g, 0xFFD24A, 0.3f * g);
                for (int q = 0; q < 14; q++) {                                                          // pebbles lifting off the ground
                    float an = q * 2.39f, r = 0.9f + 1.9f * rnd(q * 7 + 2), x = (float) Math.cos(an) * r, z = (float) Math.sin(an) * r * 0.8f, yy = surf(x, z) + 0.05f + g * g * (0.5f + 1.4f * rnd(q * 5)), s = 0.035f + 0.04f * rnd(q);
                    push(); M.translate(x, yy, z).rotateXYZ(t * 2 + q, t * 1.4f, q); box(-s, -s, -s, s, s, s, flat(q % 2 == 0 ? th.top() : th.dirt()), 2f, 0.014f); pop();
                }
                return;
            }
            if (t < last) {                                                                         // the fall: a bar of gold light
                float u = (t - (last - SLAM)) / SLAM, e = u * u; Vector3f p = new Vector3f(lerp(GOLD.x, a.x, e), lerp(GOLD.y, a.y, e), lerp(GOLD.z, a.z, e));
                for (int q = 0; q < 10; q++) { float f = q / 10f; glow(F.add, lerp(p.x, GOLD.x, f), lerp(p.y, GOLD.y, f), lerp(p.z, GOLD.z, f), 0.75f - 0.4f * f, q < 3 ? 0xFFFFFF : 0xFFD24A, 0.8f * (1f - f * 0.6f)); }
                rock(p.x, p.y, p.z, 0.46f, 0xFFE89A, t * 3f, 7, true); glint(F.add, p.x, p.y, p.z, 1.9f, 0xFFFFFF, 1f);
                return;
            }
        } else if (t < t0) return;
        else if (t < h) {
            float u = (t - t0) / fl; Vector3f p = along(s0, a, u);
            trail(s0, a, u, k, col, i, t, 1f, false);
            rock(p.x, p.y, p.z, 0.2f * k, mixc(col, 0xFFFFFF, 0.3f), t, i, false);
            glint(F.add, p.x, p.y, p.z, 0.8f * k, 0xFFFFFF, 0.9f);
            Vector3f dir = new Vector3f(a).sub(s0).normalize();                                        // a bow of heat in front of it
            for (int q = 0; q < 3; q++) glow(F.add, p.x + dir.x * 0.12f * k * q, p.y + dir.y * 0.12f * k * q, p.z + dir.z * 0.12f * k * q, (0.42f - 0.1f * q) * k, mixc(col, 0xFFFFFF, 0.5f), 0.35f);
            float gy = ground ? a.y : surf(a.x, a.z); disc(F.add, a.x, gy + 0.04f, a.z, (0.3f + 1.3f * u) * k, col, 0.4f * u * u);          // its light growing where it will land
            return;
        }
        // ---- landed
        float d = t - h; if (d > (type == 2 ? 3.2f : 1.6f)) return;
        if (!gold && d < 0.45f) trail(s0, a, 1f + d * 0.6f, k, col, i, t, 1f - d / 0.45f, false);            // the trail burns out behind it
        float f1 = Math.max(0f, 1f - d / 0.22f), kk = gold ? 2.6f : k;
        if (f1 > 0) { glow(F.add, a.x, a.y, a.z + 0.3f, 1.1f * kk, 0xFFFFFF, 0.55f * f1); glow(F.add, a.x, a.y, a.z + 0.3f, 2.3f * kk, col, 0.3f * f1); glint(F.add, a.x, a.y, a.z + 0.3f, Math.min(2.4f, 1.8f * kk * (1f + d * 3f)), 0xFFFFFF, f1); }
        for (int q = 0; q < (big ? 22 : 10); q++) {                                                   // sparks
            float an = rnd(i * 41 + q) * 6.2832f, v = (1.6f + 2.6f * rnd(i * 23 + q)) * (0.7f + 0.3f * kk), tt = Math.min(d, 0.75f);
            float px = a.x + (float) Math.cos(an) * v * tt, py = a.y + Math.abs((float) Math.sin(an)) * v * tt - 2.4f * tt * tt, pz = a.z + 0.3f;
            if (q % 3 == 0) glint(F.add, px, py, pz, 0.2f * k, col, 1f - tt / 0.75f); else glow(F.add, px, py, pz, 0.055f, q % 2 == 0 ? 0xFFFFFF : col, 1f - tt / 0.75f);
        }
        float gy = surf(a.x, a.z);
        if (ground) {
            float rw = d / 0.55f; if (rw < 1f) { ring(F.add, a.x, gy + 0.05f, a.z, (0.25f + 1.7f * eo(rw)) * k, 0.16f * k, col, 0.85f * (1f - rw)); ring(F.add, a.x, gy + 0.05f, a.z, (0.2f + 1.1f * eo(rw)) * k, 0.07f * k, 0xFFFFFF, 0.7f * (1f - rw)); }
            debris(a.x, a.z, d, k, 9, i * 97 + 5, th);
            float cool = sm(d / 1.4f), cr = 0.42f * k;                                                  // a crater whose rim cools
            disc(F.veil, a.x, gy + 0.03f, a.z, cr * 1.35f, 0x0A0606, 0.8f * sm(d / 0.08f));
            ring(F.add, a.x, gy + 0.045f, a.z, cr, 0.07f, mixc(mixc(0xFFFFFF, col, sm(d / 0.25f)), 0xC8401A, cool * 0.7f), 0.9f - 0.55f * cool);
            for (int q = 0; q < 5; q++) { float u = cl((d - q * 0.04f) / 1.2f); if (u <= 0 || u >= 1) continue; glow(F.veil, a.x + (rnd(i * 5 + q) - 0.5f) * 0.6f * k, gy + 0.2f + u * 1.3f, a.z + (rnd(i * 9 + q) - 0.5f) * 0.4f, (0.3f + 0.5f * u) * k, mixc(th.dirt(), 0x4A4648, 0.6f), 0.4f * (1f - u)); }   // dust
            if (type == 2) starFire(t, i, new Vector3f(a.x, gy, a.z), col, d);
            else for (int q = 0; q < 3; q++) { float life = 1f - sm((d - 0.5f) / 0.7f), fl2 = 0.2f + 0.08f * (float) Math.sin(t * 19 + q * 2 + i); glow(F.add, a.x + (rnd(q + i) - 0.5f) * 0.2f, gy + 0.12f + q * 0.1f, a.z, fl2 * (1f - q * 0.2f) * k, q == 0 ? 0xFFFFFF : col, 0.6f * life); }
        } else {
            float rw = d / 0.5f; if (rw < 1f) for (int q = 0; q < 28; q++) { double an = q * Math.PI * 2 / 28; float r = (0.2f + 1.5f * eo(rw)) * kk; glow(F.add, a.x + right.x * (float) Math.cos(an) * r + up.x * (float) Math.sin(an) * r, a.y + right.y * (float) Math.cos(an) * r + up.y * (float) Math.sin(an) * r, a.z + 0.3f + right.z * (float) Math.cos(an) * r, 0.07f, col, 0.9f * (1f - rw)); }
        }
        if (big && type > 0) {                                                                      // the last strike: the ground breaks under it
            float rw = d / 0.8f;
            if (rw < 1f) { ring(F.add, 0, 0.06f, 0, (0.4f + (gold ? 7.5f : 4.2f) * eo(rw)), gold ? 0.45f : 0.28f, col, (1f - rw)); ring(F.add, 0, 0.07f, 0, (0.3f + (gold ? 5.2f : 3f) * eo(rw)), 0.12f, 0xFFFFFF, 0.8f * (1f - rw)); }
            debris(0f, 0.2f, d, gold ? 1.35f : 1.15f, gold ? 22 : 12, 991, th);
            fissures(d, col, gold ? 11 : 6, gold ? 4.4f : 2.4f, (gold ? 1f : 0.8f) * (1f - sm((d - (gold ? 2.2f : 0.9f)) / 0.9f)) * (0.8f + 0.2f * (float) Math.sin(t * 9)));
            if (gold) {
                float dome = d / 0.7f;                                                                 // a dome of gold light swelling out from the strike
                if (dome < 1f) for (int el = 0; el < 5; el++) { double e = Math.toRadians(8 + el * 18); float R = 0.5f + 5.5f * eo(dome); ring(F.add, 0, 0.05f + R * (float) Math.sin(e), 0, R * (float) Math.cos(e), 0.14f, el % 2 == 0 ? 0xFFD24A : 0xFFF4C8, 0.55f * (1f - dome) * (1f - el * 0.12f)); }
                float wo = Math.max(0f, 1f - d / 0.3f); if (wo > 0) { flash(0xFFFFFF, 0.8f * wo * wo); flash(0xFFD24A, 0.35f * wo); }   // white-out
                for (int q = 0; q < 26; q++) {                                                          // gold dust thrown high, hanging
                    float u = cl(d / 2.2f - rnd(q) * 0.1f); if (u <= 0 || u >= 1) continue; float an = rnd(q * 7 + 1) * 6.2832f, r = (0.4f + 2.6f * rnd(q * 3)) * eo(u * 2.5f), yy = 0.4f + (1.5f + 2.6f * rnd(q * 11)) * eo(u * 2.2f) - 0.5f * u * u;
                    glow(F.add, (float) Math.cos(an) * r, yy, (float) Math.sin(an) * r * 0.7f, 0.07f + 0.05f * rnd(q), q % 3 == 0 ? 0xFFFFFF : 0xFFD24A, (1f - u) * 0.9f);
                }
            }
        }
    }
    /** The sky answers RNG's last strike: far meteors streaking down behind everything, and curtains of gold light. */
    static void skyShow(float t, float aspect) {
        float d = t - last; if (type != 2 || d < 0) return; float life = sm(d / 0.3f) * (1f - sm((d - 3.4f) / 0.8f));
        for (int k = 0; k < 46; k++) {
            float st = rnd(k * 13 + 1) * 3.2f, u = (d - st) / 0.42f; if (u < 0 || u > 1) continue;
            float sx = -0.9f + rnd(k * 7 + 3) * 2.3f, sy = 0.45f + rnd(k * 11) * 0.6f, len = 0.16f + 0.2f * rnd(k * 5);
            float hx = sx - (0.25f + len) * u, hy = sy - (0.4f + len) * u; int col = k % 5 == 0 ? 0xFFFFFF : k % 2 == 0 ? 0xFFD24A : 0xFFF0C0;
            Vector3f hd = onSky(hx, hy, 78f, aspect), tl = onSky(hx + len * 0.55f, hy + len * 0.9f, 78f, aspect); float w = 0.12f + 0.14f * rnd(k * 3), a = life * (float) Math.sin(Math.PI * u);
            int c1 = argb(a, col), c0 = argb(0, col);
            wquad(F.sky, hd.x + up.x * w, hd.y + up.y * w, hd.z + up.z * w, c1, hd.x - up.x * w, hd.y - up.y * w, hd.z - up.z * w, c1, tl.x, tl.y, tl.z, c0, tl.x, tl.y, tl.z, c0);
            glow(F.sky, hd.x, hd.y, hd.z, 0.5f, 0xFFFFFF, a);
        }
        for (int b = 0; b < 3; b++) for (int k = 0; k < 22; k++) {                                     // aurora curtains
            float x0 = -1.1f + k * 0.1f, x1 = x0 + 0.1f, by = 0.38f + 0.1f * b;
            float y0 = by + 0.07f * (float) Math.sin(x0 * 5 + t * 0.9f + b * 2), y1 = by + 0.07f * (float) Math.sin(x1 * 5 + t * 0.9f + b * 2);
            float h0 = 0.3f + 0.12f * (float) Math.sin(x0 * 9 + t * 1.6f + b), h1 = 0.3f + 0.12f * (float) Math.sin(x1 * 9 + t * 1.6f + b);
            Vector3f p0 = onSky(x0, y0, 82f, aspect), p1 = onSky(x1, y1, 82f, aspect), q0 = onSky(x0, y0 + h0, 82f, aspect), q1 = onSky(x1, y1 + h1, 82f, aspect);
            int col = b == 0 ? 0xFFD24A : b == 1 ? 0xFF9AD0 : 0xA0FFD8; float a0 = 0.16f * life * (0.6f + 0.4f * (float) Math.sin(x0 * 13 + t * 2 + b)), a1 = 0.16f * life * (0.6f + 0.4f * (float) Math.sin(x1 * 13 + t * 2 + b));
            wquad(F.sky, p0.x, p0.y, p0.z, argb(a0, col), p1.x, p1.y, p1.z, argb(a1, col), q1.x, q1.y, q1.z, argb(0, col), q0.x, q0.y, q0.z, argb(0, col));
        }
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
            float pw = 0.55f + 0.08f * (float) Math.sin(t * 7);                                        // a pillar of gold through the sky
            column(F.add, PIN.x, 0.02f, PIN.z, 14f, pw * 1.6f, 0xFFD24A, 0.32f * a); column(F.add, PIN.x, 0.02f, PIN.z, 14f, pw * 0.5f, 0xFFF4C8, 0.55f * a); column(F.add, PIN.x, 0.02f, PIN.z, 14f, 0.12f, 0xFFFFFF, 0.8f * a);
            for (int q = 0; q < 18; q++) { float u = ((p * 0.9f + q / 18f) % 1f), an = q * 2.4f + t * 2.5f; glint(F.add, PIN.x + (float) Math.cos(an) * 0.45f, u * 6f, PIN.z + (float) Math.sin(an) * 0.45f, 0.16f, q % 2 == 0 ? 0xFFFFFF : 0xFFD24A, (1f - u) * a); }
            for (int o = 0; o < 3; o++) {                                                              // three tilted orbits of light round the drop
                float tiltA = 0.5f + o * 1.05f, spin = t * (1.6f + 0.5f * o) + o * 2f, R = 1.15f + 0.3f * o; Vector3f prev = null;
                for (int q = 0; q <= 40; q++) {
                    double an = q * Math.PI * 2 / 40; float cx = (float) Math.cos(an) * R, cz = (float) Math.sin(an) * R, cy = cz * (float) Math.sin(tiltA); cz *= (float) Math.cos(tiltA);
                    float rx = cx * (float) Math.cos(spin * 0.3f) - cz * (float) Math.sin(spin * 0.3f), rz = cx * (float) Math.sin(spin * 0.3f) + cz * (float) Math.cos(spin * 0.3f);
                    Vector3f pt = new Vector3f(PIN.x + rx, PIN.y + 0.2f + cy, PIN.z + rz);
                    if (prev != null) line3(prev, pt, 0xFFD24A, 0.35f * a * sm((p - 0.2f - o * 0.1f) / 0.3f));
                    prev = pt;
                }
                double an = spin; float cx = (float) Math.cos(an) * R, cz = (float) Math.sin(an) * R, cy = cz * (float) Math.sin(tiltA); cz *= (float) Math.cos(tiltA);
                float rx = cx * (float) Math.cos(spin * 0.3f) - cz * (float) Math.sin(spin * 0.3f), rz = cx * (float) Math.sin(spin * 0.3f) + cz * (float) Math.cos(spin * 0.3f);
                glint(F.add, PIN.x + rx, PIN.y + 0.2f + cy, PIN.z + rz, 0.4f, 0xFFFFFF, a * sm((p - 0.2f - o * 0.1f) / 0.3f));
            }
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
