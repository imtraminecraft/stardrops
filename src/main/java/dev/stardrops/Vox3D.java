package dev.stardrops;

import static dev.stardrops.Ritual.*;

import java.util.ArrayDeque;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * The slayer scene as our own chunky 3D (Butcher-like): every object is built from boxes with pixel-textured faces, lit by the moon,
 * and wrapped in a black outline (an inside-out copy of the box, so only its rim shows). A floating diorama of the boss's ground in a
 * soft-edged window of night sky (a moon, stars, rain or embers), small on the screen. Pure geometry: Slayer3D hands the meshes to the
 * GPU in game, and the bench draws the very same triangles in software for previews.
 *
 * Timeline (SlayerScene.HITS / RISE / WALK, so the sounds stay in step): the boss arrives its own way at its spawn (the Revenant claws
 * out of the grave), walks toward you, meteors strike (one: no drops; a barrage: a drop; a shower climbing the rarities: RNG), and it
 * shatters into its own cubes or (nothing dropped) topples and sinks into ash. The drop rises from the crater in a beam (RNG: gold).
 */
final class Vox3D {
    private Vox3D() {}

    // ------------------------------------------------------------------ meshes: x, y, z, argb per vertex, three per triangle
    static final class Mesh {
        float[] p = new float[3 * 4096]; int[] c = new int[4096]; int n; final boolean fade;
        Mesh(boolean fade) { this.fade = fade; }
        void v(float x, float y, float z, int argb) {
            if (fade) { float e = edge(x, y, z); argb = Math.round((argb >>> 24) * e) << 24 | (argb & 0xFFFFFF); }
            if (n == c.length) { p = java.util.Arrays.copyOf(p, p.length * 2); c = java.util.Arrays.copyOf(c, c.length * 2); }
            p[n * 3] = x; p[n * 3 + 1] = y; p[n * 3 + 2] = z; c[n++] = argb;
        }
    }
    /** One frame: the camera and the meshes in draw order (opaque, then the view-space sky, sky glows, veils, glows). */
    static final class Frame {
        Matrix4f proj, view, groundMat = new Matrix4f(); Mesh ground; Mesh opq = new Mesh(false), back = new Mesh(false), sky = new Mesh(true), veil = new Mesh(true), add = new Mesh(true);
    }

    static int argb(float a, int rgb) { return Math.round(cl(a) * 255) << 24 | (rgb & 0xFFFFFF); }
    static int mul(int rgb, float s) {
        int r = Math.min(255, Math.round(((rgb >> 16) & 255) * s)), g = Math.min(255, Math.round(((rgb >> 8) & 255) * s)), b = Math.min(255, Math.round((rgb & 255) * s));
        return r << 16 | g << 8 | b;
    }

    // ------------------------------------------------------------------ per frame state
    static Frame F; static Matrix4f projView; static final Vector3f eye = new Vector3f(), right = new Vector3f(), up = new Vector3f();
    static Matrix4f M = new Matrix4f(); static final ArrayDeque<Matrix4f> STACK = new ArrayDeque<>();
    static boolean mobMode; static int rimCol;
    static int pass; static boolean dyn;                         // pass 1: build the still scenery once; pass 2: only what moves (dyn)
    static float fovNow = 30f;
    static float shiftX, shiftY;                                 // the NDC shift that pins the drop's spot; the sky is drawn against it so it stays centred
    static float tint; static int tintCol;                      // a flash over everything drawn (hits: white / red)
    static final float FOV = 30f;
    static float dbgDist; static Vector3f dbgTarget;                 // bench close-ups only
    static final Vector3f FRONT = new Vector3f(0f, 0f, 1.25f), SPAWN = new Vector3f(0.4f, 0f, -1.1f);
    static final Vector3f LIGHT = new Vector3f(-0.45f, 0.8f, 0.55f).normalize();

    static void push() { STACK.push(new Matrix4f(M)); }
    static void pop() { M = STACK.pop(); }

    /** How far a world point is toward the window's soft edge: 1 inside, 0 at the edge (or behind us). */
    /** The window-space radius of a world point (0 centre, 1 the window's edge; 9 behind us). */
    static float edgeR(float x, float y, float z) {
        Matrix4f m = projView; float w = m.m03() * x + m.m13() * y + m.m23() * z + m.m33(); if (w <= 0.01f) return 9f;
        float nx = (m.m00() * x + m.m10() * y + m.m20() * z + m.m30()) / w, ny = (m.m01() * x + m.m11() * y + m.m21() * z + m.m31()) / w;
        return (float) Math.sqrt(nx * nx * 0.9f + ny * ny);
    }
    static float edge(float x, float y, float z) {
        Matrix4f m = projView;                                                                      // by hand: no allocation (called for every faded vertex)
        float w = m.m03() * x + m.m13() * y + m.m23() * z + m.m33(); if (w <= 0.01f) return 0f;
        float nx = (m.m00() * x + m.m10() * y + m.m20() * z + m.m30()) / w, ny = (m.m01() * x + m.m11() * y + m.m21() * z + m.m31()) / w, r = (float) Math.sqrt(nx * nx * 0.9f + ny * ny);
        return 1f - sm((r - 0.72f) / 0.27f);
    }

    // ------------------------------------------------------------------ boxes
    /** Colours of a box's faces, pixel by pixel. face: 0 +x, 1 -x, 2 +y (top), 3 -y, 4 +z (front), 5 -z (back); u to the right, v up as you look at the face. */
    interface Skin { int at(int face, int u, int v, int w, int h); }
    static Skin flat(int c) { return (f, u, v, w, h) -> c; }
    static Skin noisy(int c, float amt, int seed) { return (f, u, v, w, h) -> { float n = rnd(seed + f * 7919 + u * 131 + v * 977); return mul(c, 1f - amt + n * amt * 2f); }; }

    // face frames: origin corner, u axis, v axis (u x v = outward normal), as fractions of the box
    private static final float[][] FACE = {
        {1, 0, 1, 0, 0, -1, 0, 1, 0}, {0, 0, 0, 0, 0, 1, 0, 1, 0}, {0, 1, 1, 1, 0, 0, 0, 0, -1},
        {0, 0, 0, 1, 0, 0, 0, 0, 1}, {0, 0, 1, 1, 0, 0, 0, 1, 0}, {1, 0, 0, -1, 0, 0, 0, 1, 0}};
    private static final float[][] NRM = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    /** A box from (x0,y0,z0) to (x1,y1,z1) in the current transform, its faces cut into pixels (px per unit), with a black rim if ol > 0. faces: bit mask of faces to draw. */
    static void box(float x0, float y0, float z0, float x1, float y1, float z1, Skin skin, float px, float ol, int faces) {
        if ((pass == 1 && dyn) || (pass == 2 && !dyn)) return;                                   // the cached still parts / the moving parts
        float sx = x1 - x0, sy = y1 - y0, sz = z1 - z0;
        float[] dims = {sx, sy, sz};
        for (int f = 0; f < 6; f++) {
            if ((faces & (1 << f)) == 0) continue;
            float[] d = FACE[f];
            float ox = x0 + d[0] * sx, oy = y0 + d[1] * sy, oz = z0 + d[2] * sz;
            float ux = d[3] * sx, uy = d[4] * sy, uz = d[5] * sz, vx = d[6] * sx, vy = d[7] * sy, vz = d[8] * sz;
            float ul = Math.abs(ux + uy + uz), vl = Math.abs(vx + vy + vz);
            int w = Math.max(1, Math.round(ul * px)), h = Math.max(1, Math.round(vl * px));
            Vector3f n = M.transformDirection(new Vector3f(NRM[f][0], NRM[f][1], NRM[f][2])).normalize();
            float s = 0.36f + 0.5f * Math.max(0f, n.dot(LIGHT)) + 0.06f * Math.max(0f, n.y);                // moonlight: soft, a little blue
            float rim = 0f, pxw = 0f;
            if (mobMode) {                                                                          // the boss: a rim of its energy where it turns away from us
                Vector3f fc = M.transformPosition(new Vector3f(ox + (ux + vx) / 2, oy + (uy + vy) / 2, oz + (uz + vz) / 2));
                Vector3f vd = new Vector3f(eye).sub(fc).normalize(); float nd = Math.abs(n.dot(vd));
                rim = (float) Math.pow(1f - nd, 1.6f) * 0.55f * (0.35f + Math.max(0f, -n.dot(LIGHT)));
                pxw = M.transformDirection(new Vector3f(ux, uy, uz)).length() / w;
            }
            Vector3f WO = M.transformPosition(new Vector3f(ox, oy, oz)), WU = M.transformDirection(new Vector3f(ux, uy, uz)), WV = M.transformDirection(new Vector3f(vx, vy, vz));
            int[] row = new int[w];
            for (int v = 0; v < h; v++) {
                for (int u = 0; u < w; u++) {
                    int col = skin.at(f, u, v, w, h); int tag = col >>> 24;
                    if (tag == 0x01) { row[u] = -1; continue; }                                         // 0x01xxxxxx: a hole
                    if (tag == 0x02) {                                                                  // 0x02xxxxxx: glows (eyes, embers)
                        col &= 0xFFFFFF;
                        if (pxw > 0) { float cu = (u + 0.5f) / w, cv = (v + 0.5f) / h; glow(F.add, WO.x + WU.x * cu + WV.x * cv + n.x * 0.02f, WO.y + WU.y * cu + WV.y * cv + n.y * 0.02f, WO.z + WU.z * cu + WV.z * cv + n.z * 0.02f, pxw * 2.6f, col, 0.55f); }
                    } else {
                        float ss = s;
                        if (mobMode && f != 2 && f != 3) ss *= 0.74f + 0.26f * (v + 0.5f) / h + (v == h - 1 ? 0.06f : 0f);   // darker toward the bottom of each part, a lit top edge
                        col = mixc(mul(col & 0xFFFFFF, ss), 0x1E2A50, 0.12f);
                        if (rim > 0) col = mixc(col, rimCol, rim);
                    }
                    if (tint > 0) col = mixc(col, tintCol, tint);
                    row[u] = col & 0xFFFFFF;
                }
                float b0 = v / (float) h, b1 = (v + 1) / (float) h;
                for (int u = 0; u < w; ) {                                                              // runs of one colour become one quad
                    int c0 = row[u], e = u + 1; while (e < w && row[e] == c0) e++;
                    if (c0 >= 0) {
                        float a0 = u / (float) w, a1 = e / (float) w; int col = 0xFF000000 | c0;
                        float ax = WO.x + WU.x * a0 + WV.x * b0, ay = WO.y + WU.y * a0 + WV.y * b0, az = WO.z + WU.z * a0 + WV.z * b0;
                        float bx = WO.x + WU.x * a1 + WV.x * b0, by = WO.y + WU.y * a1 + WV.y * b0, bz = WO.z + WU.z * a1 + WV.z * b0;
                        float cx = WO.x + WU.x * a1 + WV.x * b1, cy = WO.y + WU.y * a1 + WV.y * b1, cz = WO.z + WU.z * a1 + WV.z * b1;
                        float dx = WO.x + WU.x * a0 + WV.x * b1, dy = WO.y + WU.y * a0 + WV.y * b1, dz = WO.z + WU.z * a0 + WV.z * b1;
                        F.opq.v(ax, ay, az, col); F.opq.v(bx, by, bz, col); F.opq.v(cx, cy, cz, col); F.opq.v(ax, ay, az, col); F.opq.v(cx, cy, cz, col); F.opq.v(dx, dy, dz, col);
                    }
                    u = e;
                }
            }
        }
        if (ol > 0) outline(x0 - ol, y0 - ol, z0 - ol, x1 + ol, y1 + ol, z1 + ol);
    }
    static void box(float x0, float y0, float z0, float x1, float y1, float z1, Skin skin, float px, float ol) { box(x0, y0, z0, x1, y1, z1, skin, px, ol, 63); }
    /** The inside-out black shell: its near faces are culled, its far faces show as a rim round the box. */
    static void outline(float x0, float y0, float z0, float x1, float y1, float z1) {
        float sx = x1 - x0, sy = y1 - y0, sz = z1 - z0;
        for (int f = 0; f < 6; f++) {
            float[] d = FACE[f];
            float ox = x0 + d[0] * sx, oy = y0 + d[1] * sy, oz = z0 + d[2] * sz;
            float ux = d[3] * sx, uy = d[4] * sy, uz = d[5] * sz, vx = d[6] * sx, vy = d[7] * sy, vz = d[8] * sz;
            quad(F.opq, ox, oy, oz, ox + ux, oy + uy, oz + uz, ox + ux + vx, oy + uy + vy, oz + uz + vz, ox + vx, oy + vy, oz + vz, 0xFF0A0A0E, true);
        }
    }
    /** A quad in model space (transformed by M) into a mesh; inverted = wound the other way. */
    static void quad(Mesh m, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz, int col, boolean inverted) {
        Vector3f a = M.transformPosition(new Vector3f(ax, ay, az)), b = M.transformPosition(new Vector3f(bx, by, bz)),
                c = M.transformPosition(new Vector3f(cx, cy, cz)), d = M.transformPosition(new Vector3f(dx, dy, dz));
        if (!inverted) { tri(m, a, b, c, col); tri(m, a, c, d, col); }
        else { tri(m, a, c, b, col); tri(m, a, d, c, col); }
    }
    static void tri(Mesh m, Vector3f a, Vector3f b, Vector3f c, int col) { m.v(a.x, a.y, a.z, col); m.v(b.x, b.y, b.z, col); m.v(c.x, c.y, c.z, col); }

    // ------------------------------------------------------------------ glows and shapes (world space, facing the camera)
    static void glow(Mesh m, float x, float y, float z, float r, int rgb, float a) {
        if (a < 0.005f || r <= 0) return;
        int seg = r > 1.2f ? 20 : 10, rings = r > 1.5f ? 4 : 1;                                       // big glows in rings, so the window's soft edge is followed closely
        float[] rr = new float[rings + 1], aa = new float[rings + 1];
        for (int k = 0; k <= rings; k++) { float f = k / (float) rings; rr[k] = r * f; aa[k] = a * (1f - f) * (1f - f * 0.35f); }
        for (int q = 0; q < rings; q++) {
            int c0 = argb(aa[q], rgb), c1 = argb(aa[q + 1], rgb);
            for (int k = 0; k < seg; k++) {
                double a0 = k * Math.PI * 2 / seg, a1 = (k + 1) * Math.PI * 2 / seg;
                float x0 = (float) Math.cos(a0), y0 = (float) Math.sin(a0), x1 = (float) Math.cos(a1), y1 = (float) Math.sin(a1);
                float ia = rr[q], oa = rr[q + 1];
                float px0 = x + (right.x * x0 + up.x * y0) * ia, py0 = y + (right.y * x0 + up.y * y0) * ia, pz0 = z + (right.z * x0 + up.z * y0) * ia;
                float px1 = x + (right.x * x1 + up.x * y1) * ia, py1 = y + (right.y * x1 + up.y * y1) * ia, pz1 = z + (right.z * x1 + up.z * y1) * ia;
                float qx0 = x + (right.x * x0 + up.x * y0) * oa, qy0 = y + (right.y * x0 + up.y * y0) * oa, qz0 = z + (right.z * x0 + up.z * y0) * oa;
                float qx1 = x + (right.x * x1 + up.x * y1) * oa, qy1 = y + (right.y * x1 + up.y * y1) * oa, qz1 = z + (right.z * x1 + up.z * y1) * oa;
                if (q == 0) { m.v(x, y, z, c0); m.v(qx0, qy0, qz0, c1); m.v(qx1, qy1, qz1, c1); }
                else wquad(m, px0, py0, pz0, c0, qx0, qy0, qz0, c1, qx1, qy1, qz1, c1, px1, py1, pz1, c0);
            }
        }
    }
    static void wquad(Mesh m, float ax, float ay, float az, int ca, float bx, float by, float bz, int cb, float cx, float cy, float cz, int cc, float dx, float dy, float dz, int cd) {
        m.v(ax, ay, az, ca); m.v(bx, by, bz, cb); m.v(cx, cy, cz, cc); m.v(ax, ay, az, ca); m.v(cx, cy, cz, cc); m.v(dx, dy, dz, cd);
    }
    static void glint(Mesh m, float x, float y, float z, float r, int rgb, float a) {
        if (a < 0.01f) return; int ci = argb(a, rgb), co = argb(0, rgb); float w = r * 0.16f;
        for (int k = 0; k < 2; k++) {
            float ax = k == 0 ? 1 : 0, ay = k == 0 ? 0 : 1;
            float lx = (right.x * ax + up.x * ay) * r, ly = (right.y * ax + up.y * ay) * r, lz = (right.z * ax + up.z * ay) * r;
            float wx = (right.x * ay + up.x * ax) * w, wy = (right.y * ay + up.y * ax) * w, wz = (right.z * ay + up.z * ax) * w;
            m.v(x - lx, y - ly, z - lz, co); m.v(x + wx, y + wy, z + wz, ci); m.v(x + lx, y + ly, z + lz, co);
            m.v(x - lx, y - ly, z - lz, co); m.v(x - wx, y - wy, z - wz, ci); m.v(x + lx, y + ly, z + lz, co);
        }
        glow(m, x, y, z, r * 0.35f, rgb, a * 0.8f);
    }
    static void ring(Mesh m, float x, float y, float z, float r, float w, int rgb, float a) {
        if (a < 0.01f || r <= 0) return; int seg = 40, cm = argb(a, rgb), c0 = argb(0, rgb); float ri = Math.max(0, r - w), ro = r + w;
        for (int k = 0; k < seg; k++) {
            double a0 = k * Math.PI * 2 / seg, a1 = (k + 1) * Math.PI * 2 / seg;
            float c0x = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1x = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            wquad(m, x + c0x * ri, y, z + s0 * ri, c0, x + c1x * ri, y, z + s1 * ri, c0, x + c1x * r, y, z + s1 * r, cm, x + c0x * r, y, z + s0 * r, cm);
            wquad(m, x + c0x * r, y, z + s0 * r, cm, x + c1x * r, y, z + s1 * r, cm, x + c1x * ro, y, z + s1 * ro, c0, x + c0x * ro, y, z + s0 * ro, c0);
        }
    }
    static void disc(Mesh m, float x, float y, float z, float r, int rgb, float a) {
        if (a < 0.01f) return; int seg = 28, ci = argb(a, rgb), co = argb(0, rgb); float rm = r * 0.6f;
        for (int k = 0; k < seg; k++) {
            double a0 = k * Math.PI * 2 / seg, a1 = (k + 1) * Math.PI * 2 / seg;
            float c0x = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1x = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            m.v(x, y, z, ci); m.v(x + c0x * rm, y, z + s0 * rm, ci); m.v(x + c1x * rm, y, z + s1 * rm, ci);
            wquad(m, x + c0x * rm, y, z + s0 * rm, ci, x + c1x * rm, y, z + s1 * rm, ci, x + c1x * r, y, z + s1 * r, co, x + c0x * r, y, z + s0 * r, co);
        }
    }
    /** A column of light from y0 up to y1, turned to face the camera, soft at the sides and the top. */
    static void column(Mesh m, float x, float y0, float z, float y1, float w, int rgb, float a) {
        if (a < 0.01f) return;
        float hx = right.x, hz = right.z, l = (float) Math.sqrt(hx * hx + hz * hz); if (l < 1e-4f) return; hx /= l; hz /= l;
        int cb = argb(a, rgb), ct = argb(0, rgb);
        for (int sd = -1; sd <= 1; sd += 2) wquad(m, x, y0, z, cb, x + hx * w * sd, y0, z + hz * w * sd, ct, x + hx * w * sd, y1, z + hz * w * sd, ct, x, y1, z, ct);
    }
    /** A view-space disc at depth d filling the window, soft at the edge; colour by height (top..bottom). */
    static void sky(Mesh m, float aspect, float d, int top, int mid, int low, float a) {
        float hh = d * (float) Math.tan(Math.toRadians(fovNow / 2)), hw = hh * aspect; int seg = 56, rings = 12;
        for (int r = 0; r < rings; r++) for (int k = 0; k < seg; k++) {
            float r0 = r / (float) rings, r1 = (r + 1) / (float) rings; double t0 = k * Math.PI * 2 / seg, t1 = (k + 1) * Math.PI * 2 / seg;
            float[][] pt = {{r0, (float) t0}, {r0, (float) t1}, {r1, (float) t1}, {r1, (float) t0}}; float[] xs = new float[8]; int[] cs = new int[4];
            for (int q = 0; q < 4; q++) {
                float rr = pt[q][0], x = (float) Math.cos(pt[q][1]) * hw * rr - shiftX * hw, y = (float) Math.sin(pt[q][1]) * hh * rr - shiftY * hh, yy = (y + shiftY * hh) / hh;
                float al = a * (1f - sm((rr - 0.72f) / 0.27f));
                int col = yy > 0 ? mixc(mid, top, yy) : mixc(mid, low, -yy);
                xs[q * 2] = x; xs[q * 2 + 1] = y; cs[q] = argb(al, col);
            }
            wquad(m, xs[0], xs[1], -d, cs[0], xs[2], xs[3], -d, cs[1], xs[4], xs[5], -d, cs[2], xs[6], xs[7], -d, cs[3]);
        }
    }

    // ------------------------------------------------------------------ the frame
    static Frame build(float t, float A, int boss, int type, int energy, float aspect, float craterY, boolean z01) {
        F = new Frame(); tint = 0f; M = new Matrix4f(); STACK.clear(); fovNow = FOV;
        float[] hits = SlayerScene.HITS[type]; float last = hits[hits.length - 1], O = AnimationManager.SL_PIVOT, p = t - O;

        // ---- the camera: a gentle push in, a lean before RNG's big one, a kick on every hit
        float sh = SlayerScene.shake(type, t);
        float dist = lerp(19.5f, 16.2f, eo(t / 1.2f)) - 1.4f * sm((t - SlayerScene.WALK) / (SlayerScene.WALK_END - SlayerScene.WALK));
        if (type == 2) dist -= 2.4f * sm((t - 2.95f) / 0.42f) * (1f - sm((t - last) / 0.3f));
        dist += 1.4f * sm((t - last) / 0.5f) * (type == 0 ? 0.3f : 1f);
        float yaw = (float) Math.toRadians(16 + 5 * Math.sin(t * 0.4) + 8 * (1f - eo(t / 1.2f)));
        float pitch = (float) Math.toRadians(27 + 2 * Math.sin(t * 0.3));
        Vector3f target = new Vector3f(0.2f, 0.85f, 0f);
        if (dbgDist > 0) { dist = dbgDist; target = dbgTarget; }
        eye.set(target.x + (float) (Math.sin(yaw) * Math.cos(pitch)) * dist, target.y + (float) Math.sin(pitch) * dist, target.z + (float) (Math.cos(yaw) * Math.cos(pitch)) * dist);
        float jx = 0.11f * sh * (float) (Math.sin(t * 53) + Math.sin(t * 31 + 1.3)) * 0.5f, jy = 0.11f * sh * (float) (Math.sin(t * 47 + 2.1) + Math.sin(t * 37 + 0.4)) * 0.5f;   // smooth shake, no steps
        Matrix4f view = new Matrix4f().lookAt(eye.x + jx, eye.y + jy, eye.z, target.x + jx * 0.5f, target.y + jy * 0.5f, target.z, 0, 1, 0);
        if (sh > 0.01f) view = new Matrix4f().rotateZ(0.02f * sh * (float) Math.sin(t * 29 + 0.7)).mul(view);
        right.set(view.m00(), view.m10(), view.m20()); up.set(view.m01(), view.m11(), view.m21());
        Matrix4f persp = new Matrix4f().setPerspective((float) Math.toRadians(FOV), aspect, 1000f, 0.05f, z01);         // reversed depth, like the game's
        Vector4f cr = new Matrix4f(persp).mul(view).transform(new Vector4f(FRONT.x, FRONT.y, FRONT.z, 1f));
        shiftX = -cr.x / cr.w; shiftY = craterY - cr.y / cr.w;
        Matrix4f proj = new Matrix4f().translation(shiftX, shiftY, 0f).mul(persp);       // the crater sits under the item
        projView = new Matrix4f(proj).mul(view); F.proj = proj; F.view = view;

        float dim = type == 2 ? sm((t - 2.95f) / 0.3f) * (1f - sm((t - last) / 0.25f)) : 0f;           // RNG: the sky holds its breath
        Theme th = THEMES[boss];
        sky(F.back, aspect, 400f, mixc(th.skyTop, 0x000000, 0.5f * dim), mixc(th.skyMid, 0x000000, 0.5f * dim), mixc(th.skyLow, 0x000000, 0.4f * dim), 0.96f);
        stars(t, dim, aspect); moon(th, aspect);
        if (groundBoss != boss || groundMesh == null) {                                              // the island never changes: built once per boss
            Mesh keep = F.opq, ka = F.add, kv = F.veil; F.opq = new Mesh(false); F.add = new Mesh(false); F.veil = new Mesh(false);
            pass = 1; island(boss, th); props(0f, boss, th); pass = 0;
            groundMesh = F.opq; F.opq = keep; F.add = ka; F.veil = kv; groundBoss = boss;
        }
        F.ground = groundMesh;
        pass = 2; props(t, boss, th); pass = 0;
        mob(t, type, boss, hits, last, energy, th);
        for (int i = 0; i < hits.length; i++) meteor(t, type, boss, i, hits, th);
        if (p > 0 && type > 0) reveal(t, p, type, A, energy);
        weather(t, boss);
        return F;
    }

    // ------------------------------------------------------------------ themes: sky, ground, weather
    record Theme(int skyTop, int skyMid, int skyLow, int top, int top2, int dirt, int stone, int moon) {}
    static final Theme[] THEMES = {
        new Theme(0x080B1E, 0x1A1838, 0x5A2638, 0x7A5A2E, 0x5E7A34, 0x5A3E26, 0x4E4E56, 0xEEF2FF),   // Revenant: the graveyard, a red dusk under the night
        new Theme(0x07080E, 0x14141E, 0x2A1E26, 0x6E6A66, 0x55524E, 0x46423E, 0x34323A, 0xE8E4F0),   // Tarantula: a grey den
        new Theme(0x081020, 0x152A3A, 0x2E4A44, 0x4E8A34, 0x5E9A3E, 0x5A3E26, 0x55565E, 0xEEF6FF),   // Sven: a forest clearing
        new Theme(0x05030C, 0x140A26, 0x2E1440, 0xD8D49A, 0xC8C48A, 0xA8A472, 0x1E1428, 0xE8D8FF),   // Voidgloom: the End
        new Theme(0x120404, 0x3A0E08, 0x7A2A0E, 0x7A2A22, 0x8E3A2E, 0x5A1A16, 0x2E1A18, 0xFFD8A0),   // Inferno: the Nether
        new Theme(0x0C030E, 0x2A0A2A, 0x5A1236, 0x4A2A4A, 0x5A2050, 0x3A1E3A, 0x1A101E, 0xFFC8D8)};  // Riftstalker: the Rift

    /** A world point at a fixed place in the window: (sx, sy) in -1..1 of the window, d deep. */
    static Vector3f onSky(float sx, float sy, float d, float aspect) {
        float hh = d * (float) Math.tan(Math.toRadians(fovNow / 2)), hw = hh * aspect;
        return new Matrix4f(F.view).invert().transformPosition(new Vector3f((sx - shiftX) * hw, (sy - shiftY) * hh, -d));
    }
    static void stars(float t, float dim, float aspect) {
        for (int i = 0; i < 90; i++) {
            Vector3f p = onSky((rnd(i * 7 + 1) - 0.5f) * 1.9f, -0.1f + rnd(i * 13 + 2) * 1.0f, 80f, aspect);
            float tw = 0.5f + 0.5f * (float) Math.sin(t * (1.3f + rnd(i) * 2.4f) + i), big = rnd(i * 31 + 9);
            glow(F.sky, p.x, p.y, p.z, 0.12f + big * 0.3f, i % 9 == 0 ? 0xFFE0B0 : 0xFFFFFF, (0.35f + 0.65f * big) * tw * (1f - 0.8f * dim));
            if (big > 0.93f) glint(F.sky, p.x, p.y, p.z, 0.9f, 0xFFFFFF, 0.6f * tw * (1f - 0.8f * dim));
        }
    }
    /** A pixel moon high on the left of the window. */
    static void moon(Theme th, float aspect) {
        float d = 60f; Vector3f c = onSky(-0.58f, 0.5f, d, aspect); int n = 10; float R = 2.2f;
        for (int u = 0; u < n; u++) for (int v = 0; v < n; v++) {
            float fx = (u + 0.5f) / n * 2 - 1, fy = (v + 0.5f) / n * 2 - 1; if (fx * fx + fy * fy > 1.05f) continue;
            boolean crater = (u == 3 && v == 6) || (u == 6 && v == 3) || (u == 6 && v == 4) || (u == 2 && v == 3) || (u == 7 && v == 6);
            int ca = 0xFF000000 | (crater ? mul(th.moon(), 0.8f) : mul(th.moon(), 0.92f + 0.08f * fy));
            float a0 = (fx - 1f / n) * R, a1 = (fx + 1f / n) * R, b0 = (fy - 1f / n) * R, b1 = (fy + 1f / n) * R;
            wquad(F.veil, c.x + right.x * a0 + up.x * b0, c.y + right.y * a0 + up.y * b0, c.z + right.z * a0 + up.z * b0, ca,
                    c.x + right.x * a1 + up.x * b0, c.y + right.y * a1 + up.y * b0, c.z + right.z * a1 + up.z * b0, ca,
                    c.x + right.x * a1 + up.x * b1, c.y + right.y * a1 + up.y * b1, c.z + right.z * a1 + up.z * b1, ca,
                    c.x + right.x * a0 + up.x * b1, c.y + right.y * a0 + up.y * b1, c.z + right.z * a0 + up.z * b1, ca);
        }
        glow(F.sky, c.x, c.y, c.z, R * 3.4f, th.moon(), 0.22f);
    }

    // ------------------------------------------------------------------ the island
    static final float IRX = 4.6f, IRZ = 3.4f;
    static Mesh groundMesh; static int groundBoss = -1;
    static int depth(int i, int j) {
        float r = (float) Math.sqrt((i / IRX) * (i / IRX) + (j / IRZ) * (j / IRZ)) + (rnd(i * 31 + j * 17) - 0.5f) * 0.16f;
        if (r > 1f) return 0;
        return 1 + Math.round((1f - r) * 1.9f + rnd(i * 13 + j * 7) * 0.8f);
    }
    static void island(int boss, Theme th) {
        int R = 6;
        for (int i = -R; i <= R; i++) for (int j = -R; j <= R; j++) {
            int dp = depth(i, j); if (dp == 0) continue;
            int dl = depth(i - 1, j), dr = depth(i + 1, j), df = depth(i, j + 1), db = depth(i, j - 1);
            final int ii = i, jj = j;
            for (int k = 0; k < dp; k++) {
                int faces = 0;
                if (k == 0) faces |= 4;
                if (k == dp - 1) faces |= 8;
                if (dr <= k) faces |= 1; if (dl <= k) faces |= 2; if (df <= k) faces |= 16; if (db <= k) faces |= 32;
                if (faces == 0) continue;
                final int kk = k;
                boolean grass = boss == 2 || (boss == 0 && rnd(i * 5 + j * 11) < 0.35f);
                Skin s = (f, u, v, w, h) -> {
                    float n = rnd(ii * 977 + jj * 131 + kk * 7 + f * 31 + u * 17 + v * 3);
                    int base;
                    if (f == 2) base = grass ? th.top2() : th.top();
                    else if (kk == 0 && v >= h - 1) base = grass ? th.top2() : th.top();                 // the grass lip on the side
                    else base = kk <= 1 ? th.dirt() : th.stone();
                    return mul(base, 0.84f + n * 0.3f);
                };
                box(i - 0.5f, -1f - k, j - 0.5f, i + 0.5f, -k, j + 0.5f, s, 4f, 0f, faces);
            }
        }
    }

    // ------------------------------------------------------------------ the spawn and the dressing round it
    static final String RIP = "................................XX.XXX.XXX...X.X.X.X.X...XX..X.XXX...X.X.X.X.....X.X.X.XXX.X..";
    static void props(float t, int boss, Theme th) {
        float sx = SPAWN.x, sz = SPAWN.z;
        switch (boss) {
            case 0 -> {
                // the big headstone: mossy, rounded, R.I.P. on its face
                Skin stone = (f, u, v, w, h) -> {
                    float n = rnd(f * 131 + u * 17 + v * 31 + 5); int c = n < 0.22f ? 0x6E8A5A : 0x8A918A;
                    if (f == 4 && w >= 12 && h >= 10) { int ry = h - 4 - v, rx = u - 1; if (ry >= 0 && ry < 6 && rx >= 0 && rx < 16 && rx + 16 * ry < RIP.length() && RIP.charAt(Math.min(RIP.length() - 1, rx + 16 * ry)) == 'X' && rx < 12) c = 0x3A3E3A; }
                    return mul(c, 0.9f + n * 0.2f);
                };
                push(); M.translate(sx, 0, sz - 0.95f);
                box(-0.62f, 0f, -0.14f, 0.62f, 1.25f, 0.14f, stone, 12f, 0.035f);
                box(-0.45f, 1.25f, -0.14f, 0.45f, 1.45f, 0.14f, stone, 12f, 0.035f);
                pop();
                // the grave in front: a mound of dirt the boss bursts out of
                dyn = true;
                float burst = sm((t - SlayerScene.RISE) / 0.25f);
                if (burst < 1f) { push(); M.translate(sx, 0, sz - 0.05f); box(-0.5f, 0f, -0.75f, 0.5f, 0.28f * (1f - burst), 0.75f, noisy(0x6A4A2C, 0.15f, 3), 10f, 0.03f); pop(); }
                if (t > SlayerScene.RISE) for (int i = 0; i < 14; i++) {                                  // the dirt thrown up
                    float d = t - SlayerScene.RISE - rnd(i * 3) * 0.3f; if (d < 0 || d > 1.1f) continue;
                    float an = rnd(i * 7) * 6.2832f, v = 1.2f + rnd(i * 11) * 1.6f, vy = 3f + rnd(i * 13) * 2.5f;
                    float y = 0.2f + vy * d - 9f * d * d * 0.5f; if (y < 0.05f) y = 0.05f;
                    push(); M.translate(sx + (float) Math.cos(an) * v * d, y, sz + (float) Math.sin(an) * v * d * 0.7f).rotateXYZ(d * 6 + i, d * 4, i);
                    float s = 0.1f + 0.06f * rnd(i); box(-s, -s, -s, s, s, s, flat(i % 2 == 0 ? 0x6A4A2C : 0x4E3620), 2f, 0.02f); pop();
                }
                dyn = false;
                // more graves round the edge, wooden crosses
                float[][] g = {{-2.9f, -1.6f, 0.4f}, {-3.4f, 0.6f, -0.2f}, {2.9f, -1.9f, -0.3f}, {3.3f, 0.4f, 0.25f}};
                for (int k = 0; k < g.length; k++) { push(); M.translate(g[k][0], 0, g[k][1]).rotateY(g[k][2]); box(-0.32f, 0f, -0.1f, 0.32f, 0.7f, 0.1f, stone, 10f, 0.03f); box(-0.22f, 0.7f, -0.1f, 0.22f, 0.82f, 0.1f, stone, 10f, 0.03f); box(-0.35f, 0f, 0.15f, 0.35f, 0.12f, 1.0f, noisy(0x5E4428, 0.15f, k), 8f, 0.025f); pop(); }
                float[][] cxs = {{-1.8f, -2.4f, 0.2f}, {1.9f, -2.6f, -0.25f}};
                for (float[] c : cxs) { push(); M.translate(c[0], 0, c[1]).rotateY(c[2]); Skin wood = noisy(0x5A3A22, 0.14f, 9); box(-0.07f, 0f, -0.07f, 0.07f, 1.0f, 0.07f, wood, 10f, 0.025f); box(-0.32f, 0.62f, -0.06f, 0.32f, 0.76f, 0.06f, wood, 10f, 0.025f); pop(); }
            }
            case 1 -> {
                webs(t);
                Skin rock = noisy(0x6E6A72, 0.18f, 41);
                push(); M.translate(-2.6f, 0, -1.5f); box(-0.5f, 0f, -0.5f, 0.5f, 1f, 0.5f, rock, 6f, 0.03f); box(-0.4f, 1f, -0.3f, 0.3f, 1.6f, 0.4f, rock, 6f, 0.03f); pop();
                push(); M.translate(2.8f, 0, -1.2f); box(-0.45f, 0f, -0.45f, 0.45f, 0.8f, 0.45f, rock, 6f, 0.03f); pop();
            }
            case 2 -> {
                tree(-3f, -1.4f, 2.6f, 11); tree(3.1f, -1.8f, 3.0f, 23); tree(-1.4f, -2.6f, 2.2f, 37);
                Skin rock = noisy(0x7A7A82, 0.16f, 5);
                push(); M.translate(sx, 0, sz - 1.05f); box(-0.95f, 0f, -0.4f, -0.45f, 1.1f, 0.4f, rock, 6f, 0.03f); box(0.45f, 0f, -0.4f, 0.95f, 1.1f, 0.4f, rock, 6f, 0.03f); box(-0.95f, 1.1f, -0.4f, 0.95f, 1.55f, 0.4f, rock, 6f, 0.03f); box(-0.45f, 0f, -0.4f, 0.45f, 1.1f, -0.3f, flat(0x0E0E12), 2f, 0f); pop();
            }
            case 3 -> {
                Skin obs = (f, u, v, w, h) -> { float n = rnd(f * 31 + u * 7 + v * 13); return n < 0.15f ? 0x3A1E5A : mul(0x140C1E, 0.8f + n * 0.4f); };
                for (float[] pl : new float[][]{{-3f, -1.4f, 3.2f}, {3.1f, -1.6f, 2.6f}}) {
                    push(); M.translate(pl[0], 0, pl[1]); box(-0.5f, 0f, -0.5f, 0.5f, pl[2], 0.5f, obs, 6f, 0.035f);
                    box(-0.2f, pl[2], -0.2f, 0.2f, pl[2] + 0.4f, 0.2f, flat(0xE070FF), 4f, 0.03f); pop();
                    glow(F.add, pl[0], pl[2] + 0.2f, pl[1], 1.2f, 0xD060FF, 0.45f + 0.15f * (float) Math.sin(t * 3 + pl[0]));
                }
                Skin frame = (f, u, v, w, h) -> f == 2 ? (u > 0 && u < w - 1 && v > 0 && v < h - 1 ? 0x2A5A4A : 0x7AE8C8) : mul(0xC8D4A0, 0.85f + rnd(u * 7 + v) * 0.2f);
                for (int k = 0; k < 8; k++) { double an = k * Math.PI / 4; push(); M.translate(sx + (float) Math.cos(an) * 1.1f, 0, sz + (float) Math.sin(an) * 0.9f); box(-0.25f, 0f, -0.25f, 0.25f, 0.3f, 0.25f, frame, 8f, 0.025f); pop(); }
                for (int i = 0; i < 14; i++) { float u = ((t * 0.6f + i / 14f) % 1f), an = i * 0.9f + t * 1.8f; glow(F.add, sx + (float) Math.cos(an) * (1f - u) * 0.9f, 0.2f + u * 3f, sz + (float) Math.sin(an) * (1f - u) * 0.8f, 0.07f, 0xD060FF, (1f - u)); }
            }
            case 4 -> {
                Skin brick = (f, u, v, w, h) -> { boolean mortar = v % 3 == 0 || (u + (v / 3) * 2) % 5 == 0; return mortar ? 0x1E0E10 : mul(0x4A1A1E, 0.85f + rnd(u * 7 + v * 13 + f) * 0.3f); };
                for (float[] pl : new float[][]{{-2.9f, -1.3f, 2.4f}, {3.0f, -1.5f, 2.8f}}) {
                    push(); M.translate(pl[0], 0, pl[1]); box(-0.45f, 0f, -0.45f, 0.45f, pl[2], 0.45f, brick, 8f, 0.035f); pop();
                    dyn = true; fire(t, pl[0], pl[2], pl[1], 1f); dyn = false;
                }
                dyn = true; lava(t, sx, sz); dyn = false;
            }
            default -> {
                Skin cry = (f, u, v, w, h) -> { float n = rnd(f * 31 + u * 7 + v * 13 + 99); return n < 0.2f ? 0x8A2AD8 : mul(0x1A0E26, 0.8f + n * 0.4f); };
                for (float[] pl : new float[][]{{-2.8f, -1.2f, 1.8f}, {2.9f, -1.6f, 2.2f}}) { push(); M.translate(pl[0], 0, pl[1]); box(-0.5f, 0f, -0.5f, 0.5f, pl[2], 0.5f, cry, 6f, 0.035f); pop(); }
                float op = sm((t - 0.15f) / 0.4f) * (1f - 0.5f * sm((t - SlayerScene.WALK) / 0.6f));
                if (op > 0.01f) {
                    column(F.add, sx, 0f, sz - 0.5f, 2.6f, 0.5f * op, 0xFF2050, 0.55f * op); column(F.add, sx, 0f, sz - 0.5f, 2.4f, 0.12f * op, 0xFFC0D0, 0.9f * op);
                    glow(F.add, sx, 1.2f, sz - 0.5f, 1.6f * op, 0xFF2050, 0.3f * op);
                }
            }
        }
    }
    static void tree(float x, float z, float hgt, int seed) {
        Skin bark = (f, u, v, w, h) -> f == 2 || f == 3 ? 0x8A6A3E : mul(u % 2 == 0 ? 0x5A3E22 : 0x4A321C, 0.9f + rnd(seed + u * 7 + v * 13) * 0.2f);
        Skin leaf = (f, u, v, w, h) -> { float n = rnd(seed * 3 + f * 31 + u * 17 + v * 7); return n < 0.12f ? 0x1E4A1A : mul(0x3E7A2A, 0.8f + n * 0.4f); };
        push(); M.translate(x, 0, z); box(-0.25f, 0f, -0.25f, 0.25f, hgt, 0.25f, bark, 8f, 0.03f);
        box(-0.95f, hgt - 0.6f, -0.95f, 0.95f, hgt + 0.5f, 0.95f, leaf, 6f, 0.035f); box(-0.55f, hgt + 0.5f, -0.55f, 0.55f, hgt + 1.0f, 0.55f, leaf, 6f, 0.035f); pop();
    }
    static void webs(float t) {
        float[][] w = {{-2.2f, 2.4f, -2.6f, 2.0f}, {2.4f, 2.0f, -2.4f, 1.6f}, {0.3f, 4.2f, -2.2f, 2.6f}};
        for (float[] q : w) {
            float cx = q[0], cy = q[1], cz = q[2], r = q[3]; int c = argb(0.55f, 0xE8ECF8);
            for (int k = 0; k < 8; k++) { double an = k * Math.PI / 4; float ex = cx + (float) Math.cos(an) * r, ey = cy + (float) Math.sin(an) * r; wquad(F.veil, cx, cy, cz, c, ex, ey, cz, c, ex + 0.03f, ey + 0.03f, cz, c, cx + 0.03f, cy + 0.03f, cz, c); }
            for (int ringI = 1; ringI <= 3; ringI++) for (int k = 0; k < 8; k++) {
                double a0 = k * Math.PI / 4, a1 = (k + 1) * Math.PI / 4; float rr = r * ringI / 3.4f;
                float x0 = cx + (float) Math.cos(a0) * rr, y0 = cy + (float) Math.sin(a0) * rr, x1 = cx + (float) Math.cos(a1) * rr, y1 = cy + (float) Math.sin(a1) * rr;
                wquad(F.veil, x0, y0, cz, c, x1, y1, cz, c, x1, y1 + 0.035f, cz, c, x0, y0 + 0.035f, cz, c);
            }
        }
    }
    static void fire(float t, float x, float y, float z, float k) {
        for (int i = 0; i < 6; i++) {
            float u = ((t * 1.8f + i / 6f) % 1f), s = (0.16f - u * 0.12f) * k;
            push(); M.translate(x + (rnd(i * 7) - 0.5f) * 0.4f * k, y + u * 0.9f * k, z + (rnd(i * 11) - 0.5f) * 0.4f * k);
            box(-s, -s, -s, s, s, s, flat(u < 0.4f ? 0xFFE07A : u < 0.7f ? 0xFF9A30 : 0xD8401A), 2f, 0f); pop();
        }
        glow(F.add, x, y + 0.4f * k, z, 1.3f * k, 0xFF7A20, 0.45f);
    }
    static void lava(float t, float x, float z) {
        float pulse = 0.5f + 0.5f * (float) Math.sin(t * 3f);
        for (int[] b : new int[][]{{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {1, 1}}) {
            final int bx = b[0], bz = b[1];
            push(); M.translate(x + bx, 0, z + bz);
            box(-0.5f, -0.12f, -0.5f, 0.5f, 0.005f, 0.5f, (f, u, v, w, h) -> mixc(0xFF6A10, 0xFFC040, rnd(bx * 7 + bz * 13 + u * 5 + v * 11 + (int) (t * 4)) * 0.7f + 0.3f * pulse), 4f, 0f, 4);
            pop();
        }
        glow(F.add, x + 0.2f, 0.4f, z + 0.3f, 2.4f, 0xFF7A20, 0.3f + 0.1f * pulse);
    }

    // ------------------------------------------------------------------ the boss
    /** Angles blended the short way round. */
    static float alerp(float a, float b, float f) { float d = (float) Math.atan2(Math.sin(b - a), Math.cos(b - a)); return a + d * f; }
    static Vector3f bossAt(float t) {
        float w = sm((t - SlayerScene.WALK) / (SlayerScene.WALK_END - SlayerScene.WALK));
        return new Vector3f(lerp(SPAWN.x, FRONT.x, w), 0f, lerp(SPAWN.z, FRONT.z, w));
    }
    static float burstD = -1f; static int partNo; static float crumble;
    /** One body part (a box in model pixels): after the last hit it flies off with a spin (shatter) or sinks and greys (crumble). */
    static void part(float x0, float y0, float z0, float x1, float y1, float z1, Skin s) {
        partNo++;
        if (burstD >= 0f) {
            float d = burstD; if (d > 1.5f) return; int n = partNo;
            float cx = (x0 + x1) / 2, cy = (y0 + y1) / 2, cz = (z0 + z1) / 2;
            float an = rnd(n * 13 + 1) * 6.2832f, sp = 20f + rnd(n * 7) * 30f, vy = 25f + rnd(n * 11) * 35f;
            float dx = (float) Math.cos(an) * sp * d + cx * 2f * d, dz = (float) Math.sin(an) * sp * d * 0.7f, dy = vy * d - 150f * d * d;
            float sc = 1f - sm((d - 0.9f) / 0.6f);
            Vector3f wc = M.transformPosition(new Vector3f(cx + (float) Math.cos(an) * sp * d, cy + vy * d - 150f * d * d, cz));
            sc *= sm(edge(wc.x, wc.y, wc.z) * 1.6f);                                                      // gone before the window's soft edge
            if (sc < 0.02f) return;
            push(); M.translate(cx + dx, Math.max(cy + dy, (y1 - y0) / 2f), cz + dz).rotateXYZ(d * (4 + n % 5), d * (3 + n % 3), d * 2).scale(sc).translate(-cx, -cy, -cz);
            box(x0, y0, z0, x1, y1, z1, s, 1f, 0.6f); pop();
            return;
        }
        Skin g = crumble <= 0 ? s : (f, u, v, w, h) -> mixc(s.at(f, u, v, w, h), 0x6A6A70, crumble);
        box(x0, y0, z0, x1, y1, z1, g, 1f, 0.6f);
    }
    static void mob(float t, int type, int boss, float[] hits, float last, int energy, Theme th) {
        if (t < SlayerScene.RISE - 0.1f) return;
        Vector3f at = bossAt(t); float x = at.x, y = 0f, z = at.z;
        float ar = (t - SlayerScene.RISE) / 0.9f;
        switch (boss) {
            case 0 -> y = lerp(-2.0f, 0f, sm(ar)) + 0.06f * (float) Math.sin(Math.PI * cl(ar));
            case 1 -> { y = lerp(5.5f, 0f, eo(ar)); if (ar < 1.2f) column(F.veil, x, y + 0.5f, z, 9f, 0.02f, 0xE8ECF8, 0.7f); }
            case 2 -> z -= 1.4f * (1f - eo(ar / 0.8f));
            case 3 -> { if (ar < 0.6f && ((int) (t * 24)) % 3 == 0) return; }
            case 4 -> y = lerp(-2.4f, 0.35f, eo(ar)) + 0.15f * (float) Math.sin(t * 3f);
            default -> { if (ar < 0.2f) return; x += 0.6f * (1f - eo((ar - 0.2f) / 0.6f)); }
        }
        float since = t - last;
        boolean shatter = since >= 0 && type > 0;
        float fall = type == 0 && since > 0 ? sm(since / 0.45f) : 0f, sink = type == 0 && since > 0 ? sm((since - 0.6f) / 0.7f) : 0f;
        if (sink >= 1f) { ash(since, x, z); return; }
        float hitT = 1e9f, kb = 0f; for (float h : hits) { float d = t - h; if (d >= 0) { hitT = Math.min(hitT, d); kb += 0.16f * (d < 0.07f ? sm(d / 0.07f) : (float) Math.exp(-(d - 0.07f) * 7f)); } }
        tint = hitT < 0.12f && !shatter ? 0.3f * (1f - hitT / 0.12f) : 0f; tintCol = 0xFFB0A0;
        crumble = type == 0 && since > 0 ? Math.min(1f, since / 0.8f) : 0f;
        float walking = sm((t - SlayerScene.WALK + 0.1f) / 0.3f) * (1f - sm((t - SlayerScene.WALK_END + 0.25f) / 0.35f));
        if (boss == 2) walking = Math.max(walking, sm(ar / 0.15f) * (1f - sm((ar - 0.6f) / 0.25f)));
        float dx = FRONT.x - SPAWN.x, dz = FRONT.z - SPAWN.z, face = (float) Math.atan2(dx, dz), toCam = (float) Math.atan2(eye.x - x, eye.z - z);
        float turnIn = sm((t - SlayerScene.WALK + 0.35f) / 0.4f), turnOut = sm((t - SlayerScene.WALK_END + 0.1f) / 0.55f);
        float yawNow = alerp(alerp(toCam, face, turnIn), toCam, turnOut);
        float walk = walking * (float) Math.sin((t - SlayerScene.WALK) * 8.5f);
        push(); M.translate(x, y - sink * 1.6f + walking * 0.05f * Math.abs((float) Math.sin((t - SlayerScene.WALK) * 8.5f)), z - kb).rotateY(yawNow);
        if (fall > 0) M.rotateX(-fall * 1.45f);
        float sc = MOB_K[boss] / 16f; M.scale(sc);
        burstD = shatter ? since : -1f; partNo = 0; mobMode = true; rimCol = mixc(energy, 0xFFFFFF, 0.25f);
        switch (boss) {
            case 0 -> revenant(t, walk);
            case 1 -> spider(t, walk);
            case 2 -> wolf(t, walk);
            case 3 -> enderman(t, walk);
            case 4 -> blaze(t);
            default -> humanoid(t, walk, false, VAMP_HEAD, VAMP_BODY, VAMP_ARM, VAMP_LEG, true);
        }
        pop(); burstD = -1f; tint = 0f; crumble = 0f; mobMode = false;
        if (!shatter) aura(t, x, y - sink * 1.6f, z, MOB_K[boss], energy, boss);
        if (shatter && since < 0.5f) glow(F.add, x, 1f, z, 3f * (1f + since), energy, 0.7f * (1f - since / 0.5f));
        if (type == 0 && since > 0.3f) ash(since, x, z);
    }
    static final float[] MOB_K = {1.5f, 1.8f, 2.1f, 1.2f, 1.5f, 1.45f};
    /** The boss's presence: a shadow under it, dark smoke at its feet, wisps of its energy spiralling up round it. */
    static void aura(float t, float x, float y, float z, float k, int energy, int boss) {
        disc(F.veil, x, 0.014f, z, 0.95f * k, 0x050508, 0.5f);
        for (int i = 0; i < 7; i++) { float u = ((t * 0.5f + i / 7f) % 1f), an = i * 0.9f + t * 0.7f; glow(F.veil, x + (float) Math.cos(an) * 0.7f * k, y + 0.15f + u * 0.6f, z + (float) Math.sin(an) * 0.5f * k, 0.45f * k * (0.6f + u), 0x0A0A12, 0.3f * (1f - u)); }
        for (int i = 0; i < 16; i++) {
            float u = ((t * 0.55f + i / 16f) % 1f), an = i * 2.4f + t * 2.2f + u * 3f, r = (0.55f + 0.25f * (float) Math.sin(i + t)) * k * (1f - u * 0.4f);
            glow(F.add, x + (float) Math.cos(an) * r, y + u * 2.3f * k, z + (float) Math.sin(an) * r, 0.1f + 0.08f * (1f - u), i % 3 == 0 ? 0xFFFFFF : energy, 0.95f * (1f - u) * sm(u * 5f));
        }
        Vector3f back = new Vector3f(x, y + 1.2f * k, z).sub(eye).normalize().mul(0.9f).add(x, y + 1.2f * k, z);   // a backlight just behind it: its silhouette glows
        glow(F.add, back.x, back.y, back.z, 2.2f * k, energy, 0.28f);
    }
    static void ash(float since, float x, float z) {
        for (int i = 0; i < 22; i++) {
            float u = (since - 0.3f) * 0.75f - rnd(i * 3) * 0.4f; if (u <= 0 || u > 1f) continue;
            glow(F.veil, x + (rnd(i * 7) - 0.5f) * 1.0f + u * 1.2f, 0.3f + rnd(i * 11) * 1.2f + u * 1.8f, z + (rnd(i * 5) - 0.5f) * 0.6f, 0.2f + u * 0.4f, 0x8A8A96, (1f - u) * 0.4f);
        }
    }

    // ---- skins (model pixels)
    // the Revenant: a rotting zombie in diamond armour, sockets with burning red eyes, a torn grin
    static final String RFACE = "DDgDGDgD" + "GgGGgGDG" + "GGgGGgGG" + "GKRGGRKG" + "gGGNNGGg" + "GgGGGGgG" + "GKWKWKWG" + "GGKKKKGG";
    static int rot(float n) { return n < 0.18f ? 0x3A5A2C : n < 0.3f ? 0x5E5A36 : n < 0.38f ? 0x2E4A24 : 0x4E7A3C; }
    static final Skin REV_HEAD = (f, u, v, w, h) -> {
        int row = h - 1 - v; float n = rnd(f * 31 + u * 7 + v * 13 + 1);
        if (f == 4 && w == 8 && h == 8) {
            switch (RFACE.charAt(row * 8 + u)) {
                case 'R': return 0x02FF3A2A;
                case 'K': return 0x141C10;
                case 'N': return 0x2A3A20;
                case 'W': return 0xC8C8A8;
                case 'D': return 0x2E4A24;
                case 'g': return 0x3E6230;
                default: return mul(0x4E7A3C, 0.92f + n * 0.16f);
            }
        }
        return mul(rot(n), 0.9f + n * 0.2f);
    };
    static int diamond(int f, int u, int v, int w, int h, int seed) {
        float n = rnd(seed + f * 17 + u * 5 + v * 11);
        if (v == h - 1 || (f == 2)) return mul(0xA8FFF6, 0.92f + n * 0.1f);                        // the lit top edge
        if (u == 0 || u == w - 1 || v == 0) return 0x1E8A8C;                                        // dark rims
        if (f == 4 && u > 1 && u < w - 2 && v > 2 && v < h - 3 && (u + v) % 4 == 0) return 0xE8FFFC;  // a sparkle on the plate
        return mul(v > h / 2 ? 0x5AE6E0 : 0x3EC4C2, 0.9f + n * 0.18f);
    }
    static final Skin REV_CHEST = (f, u, v, w, h) -> diamond(f, u, v, w, h, 2);
    static final Skin REV_PAD = (f, u, v, w, h) -> diamond(f, u, v, w, h, 9);
    static final Skin REV_ARM = (f, u, v, w, h) -> { float n = rnd(f * 19 + u * 3 + v * 7 + 3); if (v < 2) return 0x1E2A16; if (v < 4 && (u + f) % 3 == 0) return 0xD8D4C0; return mul(rot(n), 0.9f + n * 0.2f); };
    static final Skin REV_LEG = (f, u, v, w, h) -> v < 2 ? mul(0x262638, 0.9f + rnd(u * 9 + v * 5 + f) * 0.2f) : diamond(f, u, v, w, h, 4);

    static final String VFACE = "HHHHHHHH" + "HHHHHHHH" + "SSSSSSSS" + "SRRSSRRS" + "SSSSSSSS" + "SSSSSSSS" + "SSFSSFSS" + "SSSSSSSS";
    static final Skin VAMP_HEAD = (f, u, v, w, h) -> {
        int row = h - 1 - v;
        if (f == 4 && w == 8 && h == 8) { char ch = VFACE.charAt(row * 8 + u); return switch (ch) { case 'H' -> 0x1A1418; case 'R' -> 0x02FF2030; case 'F' -> 0xF8F8F8; default -> 0xE0D0C8; }; }
        return row < 3 || f == 2 || f == 5 ? 0x1A1418 : 0xE0D0C8;
    };
    static final Skin VAMP_BODY = (f, u, v, w, h) -> f == 4 && u >= 3 && u <= 4 && v > 4 ? 0xE8E8E8 : f == 4 && (u == 3 || u == 4) ? 0xA01020 : mul(0x24162A, 0.9f + rnd(u * 7 + v) * 0.2f);
    static final Skin VAMP_ARM = (f, u, v, w, h) -> v < 2 ? 0xE0D0C8 : mul(0x24162A, 0.9f + rnd(u * 5 + v * 3) * 0.2f);
    static final Skin VAMP_LEG = (f, u, v, w, h) -> v < 2 ? 0x0E0A0E : mul(0x1E1420, 0.9f + rnd(u * 3 + v * 9) * 0.2f);

    /** The Revenant: hunched forward, diamond shoulder pads, arms reaching for you. */
    static void revenant(float t, float walk) {
        for (int sd = -1; sd <= 1; sd += 2) { push(); M.translate(sd * 2.1f, 12f, 0f).rotateX(walk * 0.7f * sd); part(-2.3f, -12f, -2.3f, 2.3f, 0.3f, 2.3f, REV_LEG); pop(); }
        push(); M.translate(0, 12f, 0).rotateX(0.22f + 0.03f * (float) Math.sin(t * 2.2f));                     // the hunch, breathing
        part(-4.6f, -0.4f, -2.6f, 4.6f, 12.6f, 2.6f, REV_CHEST);
        for (int sd = -1; sd <= 1; sd += 2) {
            push(); M.translate(sd * 6.4f, 10.5f, 0f).rotateX(-1.35f + 0.16f * (float) Math.sin(t * 4.5f + sd)).rotateZ(sd * 0.08f);
            part(-2f, -11f, -2f, 2f, 1f, 2f, REV_ARM);
            part(-2.8f, -2.5f, -2.8f, 2.8f, 2.4f, 2.8f, REV_PAD); pop();
        }
        push(); M.translate(0, 12.6f, 0.4f).rotateX(-0.28f).rotateY(0.18f * (float) Math.sin(t * 1.3f)).rotateZ(0.08f * (float) Math.sin(t * 0.9f)); part(-4f, 0f, -4f, 4f, 8f, 4f, REV_HEAD); pop();
        pop();
    }
    /** A Minecraft humanoid, feet at the origin, facing +z, in model pixels. */
    static void humanoid(float t, float walk, boolean reach, Skin head, Skin body, Skin arm, Skin leg, boolean cape) {
        for (int s = -1; s <= 1; s += 2) { push(); M.translate(s * 2f, 12f, 0f).rotateX(walk * 0.7f * s); part(-2f, -12f, -2f, 2f, 0f, 2f, leg); pop(); }
        part(-4f, 12f, -2f, 4f, 24f, 2f, body);
        if (cape) { push(); M.translate(0, 24f, -2.2f).rotateX(0.22f + 0.1f * (float) Math.sin(t * 4) + walk * 0.1f); part(-5f, -19f, -1f, 5f, 0f, 0f, (f, u, v, w, h) -> v < 2 ? 0x5A0810 : mul(f == 4 ? 0x3A0A14 : 0xB01020, 0.85f + rnd(u * 3 + v) * 0.3f)); pop(); }
        for (int s = -1; s <= 1; s += 2) {
            push(); M.translate(s * 6f, 22f, 0f);
            if (reach) M.rotateX(-1.45f + 0.12f * (float) Math.sin(t * 5 + s)); else M.rotateX(-walk * 0.6f * s);
            part(-2f, -10f, -2f, 2f, 2f, 2f, arm); pop();
        }
        push(); M.translate(0, 24f, 0).rotateY(0.15f * (float) Math.sin(t * 1.3f)); part(-4f, 0f, -4f, 4f, 8f, 4f, head); pop();
        if (cape) { push(); M.translate(0, 23f, 0); part(-4.5f, 0f, -2.5f, 4.5f, 2.5f, 2.5f, (f, u, v, w, h) -> mul(0x8A0A18, 0.85f + rnd(u * 7 + v) * 0.3f)); pop(); }   // the high collar
    }
    static void spider(float t, float walk) {
        Skin dark = (f, u, v, w, h) -> (v == 0 || u == 0) && w > 8 ? 0x5A1A1A : mul(0x241E22, 0.8f + rnd(f * 7 + u * 13 + v * 5) * 0.35f);
        Skin headS = (f, u, v, w, h) -> { if (f == 4 && h == 8 && w == 8 && (v == 4 || v == 5) && (u == 1 || u == 2 || u == 5 || u == 6)) return 0x02FF2A2A; if (f == 4 && v == 6 && (u == 3 || u == 4)) return 0x02FF2A2A; return mul(0x2E2420, 0.8f + rnd(u * 7 + v * 3 + f) * 0.3f); };
        Skin belly = (f, u, v, w, h) -> {                                                            // Broodfather black, a glowing red mark on its back
            float n = rnd(f * 7 + u * 13 + v * 5 + 3);
            if (f == 2 && ((u == w / 2 || u == w / 2 - 1) && v > 2 && v < h - 2 || (v == h / 2 && u > 2 && u < w - 3))) return 0x02E01818;
            if (f == 5 && (u == w / 2 || u == w / 2 - 1) && v > 1 && v < h - 1) return 0x02C01010;
            return mul(n < 0.15f ? 0x3A1414 : 0x1C181C, 0.8f + n * 0.4f);
        };
        part(-5f, 4f, -15f, 5f, 12f, -3f, belly);
        part(-3f, 5f, -3f, 3f, 11f, 3f, dark);
        part(-4f, 4f, 3f, 4f, 12f, 11f, headS);
        for (int s = -1; s <= 1; s += 2) for (int k = 0; k < 4; k++) {
            float yaw = (k - 1.5f) * 0.42f * s + (s > 0 ? 0 : (float) Math.PI), lift = 0.5f + 0.18f * walk * ((k % 2 == 0) ? 1 : -1);
            push(); M.translate(s * 3f, 8f, -1f + k * 1.2f).rotateY(yaw).rotateZ(-lift);
            part(0f, -1f, -1f, 15f, 1f, 1f, dark); pop();
        }
    }
    static void wolf(float t, float walk) {
        Skin fur = (f, u, v, w, h) -> mul(0xE6E6EA, 0.82f + rnd(f * 11 + u * 7 + v * 3) * 0.28f);
        Skin headS = (f, u, v, w, h) -> { if (f == 4 && w == 6 && h == 6 && v == 3 && (u == 1 || u == 4)) return 0x02FF3030; return mul(0xE6E6EA, 0.82f + rnd(f * 13 + u * 5 + v * 9) * 0.28f); };
        for (int[] lg : new int[][]{{-2, -5}, {2, -5}, {-2, 5}, {2, 5}}) { push(); M.translate(lg[0] * 0.75f, 8f, lg[1]).rotateX(walk * 0.7f * (lg[0] > 0 == lg[1] > 0 ? 1 : -1)); part(-1f, -8f, -1f, 1f, 0f, 1f, fur); pop(); }
        part(-3f, 8f, -7f, 3f, 14f, 4f, fur);
        part(-4f, 8f, 1f, 4f, 15f, 7f, fur);
        push(); M.translate(0, 12f, 7f); part(-3f, -2f, 0f, 3f, 4f, 4f, headS); part(-1.5f, -2f, 4f, 1.5f, 1f, 7f, (f, u, v, w, h) -> f == 4 && v == 2 ? 0x1A1A1A : 0xD8D8DC); part(-3f, 4f, 1f, -1f, 6f, 2f, fur); part(1f, 4f, 1f, 3f, 6f, 2f, fur); pop();
        push(); M.translate(0, 13f, -7f).rotateX(0.9f + 0.15f * (float) Math.sin(t * 6)); part(-1f, -8f, -1f, 1f, 0f, 1f, fur); pop();
    }
    static void enderman(float t, float walk) {
        Skin black = (f, u, v, w, h) -> mul(0x161418, 0.8f + rnd(f * 11 + u * 7 + v * 3) * 0.4f);
        Skin headS = (f, u, v, w, h) -> { if (f == 4 && w == 8 && h == 8 && v == 3) return (u <= 2 || u >= 5) ? (u == 1 || u == 6 ? 0x02F090FF : 0x02C040E0) : 0x161418; return mul(0x161418, 0.8f + rnd(u * 7 + v * 3 + f) * 0.4f); };
        for (int s = -1; s <= 1; s += 2) { push(); M.translate(s * 2f, 30f, 0f).rotateX(walk * 0.4f * s); part(-1f, -30f, -1f, 1f, 0f, 1f, black); pop(); }
        part(-4f, 30f, -2f, 4f, 42f, 2f, black);
        for (int s = -1; s <= 1; s += 2) { push(); M.translate(s * 5f, 41f, 0f).rotateX(-walk * 0.3f * s).rotateZ(s * 0.06f); part(-1f, -29f, -1f, 1f, 1f, 1f, black); pop(); }
        push(); M.translate(0, 42f, 0); part(-4f, 0f, -4f, 4f, 8f, 4f, headS); pop();
        for (int i = 0; i < 6; i++) { float u = ((t * 0.8f + i / 6f) % 1f); Vector3f p = M.transformPosition(new Vector3f((rnd(i * 5) - 0.5f) * 14f, u * 46f, (rnd(i * 9) - 0.5f) * 10f)); glow(F.add, p.x, p.y, p.z, 0.08f, 0xD060FF, 1f - u); }
    }
    static void blaze(float t) {
        Skin yel = (f, u, v, w, h) -> { if (f == 4 && w == 8 && h == 8 && v == 4 && (u == 1 || u == 2 || u == 5 || u == 6)) return 0x02FFF0B0; return mul(0xF2C230, 0.8f + rnd(f * 11 + u * 7 + v * 3) * 0.35f); };
        Skin rod = (f, u, v, w, h) -> mul(0xF0A020, 0.85f + rnd(u * 5 + v * 9 + f) * 0.3f);
        part(-4f, 16f, -4f, 4f, 24f, 4f, yel);
        for (int ringI = 0; ringI < 3; ringI++) for (int k = 0; k < 4; k++) {
            float an = (float) (k * Math.PI / 2 + t * (1.4f - ringI * 0.4f) * (ringI % 2 == 0 ? 1 : -1)), r = 8f - ringI * 2f;
            push(); M.translate((float) Math.cos(an) * r, 13f - ringI * 5f + (float) Math.sin(t * 3 + k) * 0.6f, (float) Math.sin(an) * r); part(-1f, -4f, -1f, 1f, 4f, 1f, rod); pop();
        }
        Vector3f c = M.transformPosition(new Vector3f(0, 14f, 0)); glow(F.add, c.x, c.y, c.z, 1.4f, 0xFF9A30, 0.35f);
    }

    // ------------------------------------------------------------------ meteors
    static Vector3f aim(int type, int i, int n) {
        Vector3f b = bossAt(SlayerScene.HITS[type][i]);
        if (type == 0 || i == n - 1) return b;
        return b.add((rnd(i * 31 + 3) - 0.5f) * 3.0f, 0f, (rnd(i * 17 + 5) - 0.5f) * 2.0f);
    }
    static final Skin ROCK = (f, u, v, w, h) -> { float n = rnd(f * 31 + u * 7 + v * 13 + 77); return n < 0.2f ? 0xFF8A20 : n < 0.3f ? 0xFFD060 : mul(0x3A3036, 0.8f + n * 0.4f); };
    static final Skin GOLD_ROCK = (f, u, v, w, h) -> { float n = rnd(f * 31 + u * 7 + v * 13 + 79); return n < 0.25f ? 0xFFF4C8 : mul(0xE8B020, 0.85f + n * 0.3f); };
    static void meteor(float t, int type, int boss, int i, float[] hits, Theme th) {
        int n = hits.length; boolean big = type == 0 || i == n - 1; float h = hits[i], fl = SlayerScene.flight(type, i), t0 = h - fl;
        Vector3f a = aim(type, i, n); float k = big ? (type == 2 ? 1.5f : 1.25f) : 0.85f;
        Vector3f s0 = new Vector3f(a.x + (4.5f + 2f * rnd(i * 7 + 1)) * k, a.y + (11f + 3f * rnd(i * 5)) * k, a.z - (3f + 2f * rnd(i * 3)) * k);
        int col = type == 2 ? (big ? 0xFFD24A : SlayerScene.TIER[Math.min(i, SlayerScene.TIER.length - 1)]) : big ? 0xFFB040 : 0xFF8A30;
        if (t >= t0 && t < h) {
            float u = (t - t0) / fl;
            for (int q = 18; q >= 1; q--) {
                float uu = u - q * 0.035f; if (uu < 0) continue; float e = uu * uu * (0.55f + 0.45f * uu);
                float px = lerp(s0.x, a.x, e), py = lerp(s0.y, a.y, e), pz = lerp(s0.z, a.z, e), f = 1f - q / 19f;
                int tc = type == 2 && big ? rainbow(q / 19f + u) : mixc(col, 0xFF3A10, 1f - f);
                glow(F.add, px, py, pz, (0.18f + 0.5f * f) * k, tc, 0.6f * f);
            }
            float e = u * u * (0.55f + 0.45f * u), px = lerp(s0.x, a.x, e), py = lerp(s0.y, a.y, e), pz = lerp(s0.z, a.z, e);
            glow(F.add, px, py, pz, 1.6f * k, col, 0.4f);
            push(); M.translate(px, py, pz).rotateXYZ(t * 7f + i, t * 5f, i); float s = 0.32f * k; box(-s, -s, -s, s, s, s, type == 2 && big ? GOLD_ROCK : ROCK, 16f, 0.035f); pop();
            for (int q = 0; q < 5; q++) { float v = ((u * 3f + q / 5f) % 1f); push(); M.translate(px + v * 0.9f * k + (rnd(i * 11 + q) - 0.5f) * 0.4f, py + v * 2f * k, pz - v * 0.5f).rotateXYZ(v * 5, q, v * 3); float s2 = 0.06f * (1f - v); box(-s2, -s2, -s2, s2, s2, s2, flat(q % 2 == 0 ? 0xFFE07A : col), 1f, 0f); pop(); }
            glow(F.add, a.x, 0.04f, a.z, 1.2f * k * u, col, 0.35f * u);
        }
        float d = t - h; if (d < 0 || d > 2.2f) return;
        impact(d, a, i, type, big, col, th);
    }
    static int rainbow(float u) { int[] r = {0xFF5A5A, 0xFFB04A, 0xFFE04A, 0x6AE87A, 0x6AB8FF, 0xB56BFF}; float v = ((u % 1f) + 1f) % 1f * r.length; int i = (int) v; return mixc(r[i % r.length], r[(i + 1) % r.length], v - i); }
    static void impact(float d, Vector3f a, int i, int type, boolean big, int col, Theme th) {
        float s = big ? (type == 2 ? 1.9f : 1.45f) : 0.85f;
        float fl = Math.max(0f, 1f - d / 0.25f);
        if (fl > 0) { glow(F.add, a.x, 0.7f * s, a.z, 1.8f * s, 0xFFFFFF, 0.6f * fl); glow(F.add, a.x, 0.3f, a.z, 3.4f * s, col, 0.35f * fl); }
        float rw = d / 0.6f; if (rw < 1f) ring(F.add, a.x, 0.03f, a.z, (0.3f + 3.2f * eo(rw)) * s, 0.22f * s, mixc(col, 0xFFFFFF, 0.4f), 0.9f * (1f - rw));
        int nd = big ? 16 : 7;
        for (int q = 0; q < nd; q++) {                                                              // chunks of ground thrown up
            float an = rnd(i * 37 + q) * 6.2832f, v = (1.1f + 2.2f * rnd(i * 13 + q)) * Math.min(1.3f, s), vy = 2.6f + 3f * rnd(i * 19 + q);
            float px = a.x + (float) Math.cos(an) * v * d, pz = a.z + (float) Math.sin(an) * v * d * 0.8f, py = 0.15f + vy * d - 9.8f * d * d * 0.5f;
            if (d > 1.4f) continue; if (py < 0.06f) py = 0.06f;
            float sc = (0.09f + 0.07f * rnd(q * 3 + i)) * (1f - sm((d - 1.0f) / 0.4f));
            push(); M.translate(px, py, pz).rotateXYZ(d * 6 + q, d * 4, q); box(-sc, -sc, -sc, sc, sc, sc, flat(q % 3 == 0 ? th.dirt() : th.top()), 2f, 0.02f); pop();
        }
        for (int q = 0; q < (big ? 12 : 5); q++) {
            float an = rnd(i * 41 + q) * 6.2832f, v = 1.5f + 2.5f * rnd(i * 23 + q), vy = 3f + 2.5f * rnd(i * 29 + q), tt = Math.min(d, 1.0f);
            float py = 0.3f + vy * tt - 9.8f * tt * tt * 0.5f; if (py < 0) continue;
            glow(F.add, a.x + (float) Math.cos(an) * v * tt, py, a.z + (float) Math.sin(an) * v * tt, 0.07f, q % 2 == 0 ? 0xFFE07A : col, 1f - tt);
        }
        float cool = Math.max(0f, 1f - d / 1.8f);
        disc(F.veil, a.x, 0.012f, a.z, 0.55f * s, 0x120A08, 0.85f * Math.min(1f, d * 8));
        ring(F.add, a.x, 0.02f, a.z, 0.42f * s, 0.12f * s, col, 0.8f * cool);
        for (int q = 0; q < 5; q++) { float u = ((d * 0.6f + q / 5f) % 1f); glow(F.veil, a.x + (rnd(i * 5 + q) - 0.5f) * 0.7f * s, 0.3f + u * 2.2f, a.z, 0.3f + u * 0.5f, 0x6A6A76, (1f - u) * 0.3f * cool); }
    }

    // ------------------------------------------------------------------ the drop
    static void reveal(float t, float p, int type, float A, int energy) {
        float x = FRONT.x, z = FRONT.z, beam = sm(p / 0.25f) * (1f - sm((t - (A - 0.8f)) / 0.6f));
        if (beam < 0.01f) return;
        if (type == 2) {
            float pil = beam * (0.75f + 0.25f * (float) Math.sin(t * 9f));
            column(F.add, x, 0f, z, 14f, 1.2f * pil, 0xFFD24A, 0.4f * pil); column(F.add, x, 0f, z, 14f, 0.4f * pil, 0xFFF4C8, 0.85f * pil);
            for (int k = 0; k < 3; k++) { float u = ((p * 0.8f + k / 3f) % 1f); ring(F.add, x, 0.05f + u * 0.3f, z, 0.5f + 4f * u, 0.2f, 0xFFD24A, (1f - u) * 0.8f * beam); }
            for (int k = 0; k < 10; k++) { float an = t * 0.9f + k * 0.628f, rr = 1.3f + 0.25f * (float) Math.sin(t * 3 + k); glint(F.add, x + (float) Math.cos(an) * rr, 1.9f + (float) Math.sin(an * 2) * 0.35f, z + (float) Math.sin(an) * rr, k % 3 == 0 ? 0.45f : 0.28f, k % 2 == 0 ? 0xFFF4C8 : 0xFFD24A, beam); }
            for (int k = 0; k < 50; k++) {
                float u = ((p * 0.35f + rnd(k * 7)) % 1f), cx = x + (rnd(k * 13) - 0.5f) * 7f, cz = z + (rnd(k * 19) - 0.5f) * 4f, cy = 6f - u * 6.5f;
                int cc = k % 4 == 0 ? 0xFF7AD9 : k % 4 == 1 ? 0x6AB8FF : k % 4 == 2 ? 0xFFE04A : 0x7AE88A;
                push(); M.translate(cx + (float) Math.sin(t * 3 + k) * 0.25f, cy, cz).rotateXYZ(t * 4 + k, t * 3, k); float s = 0.06f; box(-s, -s * 0.3f, -s, s, s * 0.3f, s, flat(cc), 1f, 0f); pop();
            }
            for (int k = 0; k < 12; k++) {
                float an = k * 0.5236f + t * 0.25f, len = 5f + 1.5f * (float) Math.sin(t * 2 + k), ax = (float) Math.cos(an), ay = (float) Math.sin(an), cy = 2f, w = 0.2f;
                float ex = x + (right.x * ax + up.x * ay) * len, ey = cy + (right.y * ax + up.y * ay) * len, ez = z + (right.z * ax + up.z * ay) * len;
                float qx = (right.x * -ay + up.x * ax) * w, qy = (right.y * -ay + up.y * ax) * w, qz = (right.z * -ay + up.z * ax) * w;
                int c0 = argb(0.2f * beam, 0xFFE9A0), c1 = argb(0, 0xFFE9A0);
                wquad(F.add, x, cy, z, c0, x, cy, z, c0, ex + qx, ey + qy, ez + qz, c1, ex - qx, ey - qy, ez - qz, c1);
            }
        } else {
            column(F.add, x, 0f, z, 10f, 0.65f * beam, energy, 0.35f * beam); column(F.add, x, 0f, z, 10f, 0.2f * beam, mixc(energy, 0xFFFFFF, 0.55f), 0.75f * beam);
            ring(F.add, x, 0.05f, z, 0.9f + 0.15f * (float) Math.sin(t * 4), 0.2f, energy, 0.6f * beam);
            for (int k = 0; k < 14; k++) { float u = ((p * 0.6f + rnd(k * 7)) % 1f); glow(F.add, x + (rnd(k * 5) - 0.5f) * 1f, u * 4f, z + (rnd(k * 9) - 0.5f) * 1f, 0.07f, k % 2 == 0 ? 0xFFFFFF : energy, (1f - u) * beam); }
        }
    }

    // ------------------------------------------------------------------ weather: rain at the graveyard, embers in the Nether, motes elsewhere
    static void weather(float t, int boss) {
        if (boss == 0 || boss == 2) {
            int c = argb(boss == 0 ? 0.32f : 0.18f, 0xB8C8E8);
            for (int i = 0; i < 110; i++) {
                float x = (rnd(i * 7) - 0.5f) * 13f, z = (rnd(i * 11) - 0.5f) * 9f, y = 9f - ((t * 13f + rnd(i * 3) * 11f) % 11f);
                float l = 0.45f, w = 0.018f;
                wquad(F.veil, x - right.x * w, y, z - right.z * w, c, x + right.x * w, y, z + right.z * w, c, x + right.x * w, y + l, z + right.z * w, c, x - right.x * w, y + l, z - right.z * w, c);
            }
        } else if (boss == 4) {
            for (int i = 0; i < 40; i++) { float u = ((t * 0.25f + rnd(i * 3)) % 1f); glow(F.add, (rnd(i * 7) - 0.5f) * 11f + (float) Math.sin(t + i) * 0.4f, -1f + u * 7f, (rnd(i * 11) - 0.5f) * 7f, 0.05f, i % 2 == 0 ? 0xFFB040 : 0xFF6020, (1f - u) * 0.9f); }
        } else {
            int col = boss == 1 ? 0xC8C8D8 : boss == 3 ? 0xD060FF : 0xFF4A8A;
            for (int i = 0; i < 30; i++) { float u = ((t * 0.15f + rnd(i * 3)) % 1f); glow(F.add, (rnd(i * 7) - 0.5f) * 11f + (float) Math.sin(t * 0.7f + i) * 0.5f, u * 6f, (rnd(i * 11) - 0.5f) * 7f, 0.05f, col, (1f - u) * 0.6f); }
        }
    }
}
