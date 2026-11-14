package dev.stardrops;

import static dev.stardrops.Ritual.*;
import static dev.stardrops.Vox3D.*;
import static dev.stardrops.Star3D.*;

import org.joml.Vector3f;

/**
 * The Mythological scene (author, 14 Nov): you, digging. An explorer in a star suit digs the meadow of a little planet among marble ruins.
 *   A creature's burrow: three digs, the ground bursts and the creature of the drop climbs out (each of the twelve has its own body);
 *     the explorer draws a sword of light and cuts it down (one cut, a run of three, or RNG's five in the rarity colours with a golden last one);
 *     it shatters into stardust and the drop forms where it stood. Nothing dropped: one cut, it crumbles to grey dust, and that is all.
 *   Treasure: four digs, the spade rings on wood, a chest rises out of the hole shedding dirt, light leaking from under its lid;
 *     the camera pushes in over it, the lid swings open, light pours out and the drop rises from inside (RNG: a golden chest, a fountain of coins).
 * It is drawn by Star3D's own world (ground, sky, planets, fog), so it sits in space like every other Stardrops scene.
 * Creature numbers: Star3D.MYTH + the index in Ritual.KINDS; Star3D.CHEST is the treasure.
 */
final class Dig3D {
    private Dig3D() {}

    static final float[] DIGS = {0.5f, 0.9f, 1.3f}, DIGS_T = {0.5f, 0.85f, 1.2f, 1.55f};
    static final float RISE0 = 1.45f, RISE1 = 2.15f, CLANG = 1.72f, CHEST_UP0 = 1.8f, CHEST_UP1 = 2.6f;
    private static final float[][] CUTS = {{3.0f}, {2.7f, 3.0f, 3.3f}, {2.3f, 2.55f, 2.8f, 3.02f, 3.36f}}, LID = {{3.0f}, {3.3f}, {3.36f}};
    /** The beats the camera kicks on and the scene ends its fight with: the sword cuts, or the moment the lid flies open. */
    static float[] hits(boolean treasure, int type) { return (treasure ? LID : CUTS)[Math.max(0, Math.min(2, type))]; }
    static final String[] NAMES = {"Minotaur", "Minos Hunter", "Siamese Lynxes", "Stranded Nymph", "Cretan Bull", "Harpy", "Gaia Construct", "Minos Champion", "Sphinx", "Minos Inquisitor", "Manticore", "King Minos", "Treasure"};
    /** How big each creature stands (world units per 16 model pixels). */
    private static final float[] KS = {1.25f, 1.15f, 1.0f, 1.05f, 1.25f, 1.1f, 1.2f, 1.3f, 1.2f, 1.4f, 1.3f, 1.55f};
    private static final Vector3f HERO = new Vector3f(-1.35f, 0f, 0.95f);

    /** {distance, yaw, eye height, target height}. The treasure's camera pushes in over the chest as it opens. */
    static float[] camera(float t, float p, boolean treasure, int type) {
        // The dig is filmed close and low, swinging round the digger: every bite of the spade punches the camera in and shakes it.
        // When the ground gives, it sweeps back and up to the wide shot the fight (or the chest) plays in.
        float[] digs = treasure ? DIGS_T : DIGS; float open = treasure ? CHEST_UP0 : RISE0;
        float wide = sm((t - (open - 0.25f)) / 0.7f), in = eo(t / 1.6f);
        float punch = 0f, shake = 0f; for (int i = 0; i < digs.length; i++) { float d = t - digs[i]; if (d >= 0f && d < 0.5f) { float e = (float) Math.exp(-d * 9f) * (0.75f + 0.2f * i); punch += e; shake += e * (float) Math.sin(d * 70f); } }
        { float d = t - open; if (d >= 0f && d < 0.6f) shake += 1.4f * (float) Math.exp(-d * 7f) * (float) Math.sin(d * 60f); }
        float yawWide = (float) Math.toRadians(13 + 4 * Math.sin(t * 0.3)), yawDig = (float) Math.toRadians(64 - 46 * sm(t / Math.max(0.5f, open)));
        float yaw = lerp(yawDig, yawWide, wide), dist = lerp(lerp(9.0f, 7.2f, sm(t / open)), 10.2f, wide) - 0.55f * punch, eye = lerp(lerp(0.95f, 1.5f, sm(t / open)), 2.3f, wide) + 0.07f * shake, tgt = lerp(0.75f, 1.35f, wide) + 0.05f * shake;
        if (!treasure) return new float[]{dist - 0.5f * sm((t - RISE0) / 1.0f) + 0.7f * sm(p / 0.7f), yaw, eye, tgt};
        float z = sm((t - 2.25f) / 0.95f) * (1f - 0.5f * sm(p / 0.9f));
        return new float[]{dist - 0.2f - 3.4f * z, yaw * (1f - 0.5f * z), eye + 0.8f * z, tgt - 0.35f * z};
    }

    static void scene(float t, float p, float A, int energy, Theme th, float aspect) {
        boolean treasure = boss == CHEST; int kind = Math.max(0, Math.min(11, boss - MYTH));
        float[] digs = treasure ? DIGS_T : DIGS;
        hole(t, digs, energy, th, treasure);
        if (treasure) chest(t, p, A, energy, th); else creature(t, p, kind, energy);
        hero(t, p, digs, energy, treasure);
        if (!treasure) for (int i = 0; i < n; i++) cut(t, i, energy);
        if (p > -0.2f && type > 0) gather(t, p, A, energy);
    }

    // ------------------------------------------------------------------ the hole
    static void hole(float t, float[] digs, int energy, Theme th, boolean treasure) {
        int done = 0; for (float d : digs) if (t >= d) done++;
        float open = treasure ? CHEST_UP0 : RISE0;
        float r = 0.22f + 0.17f * done + (t > open ? 0.25f * sm((t - open) / 0.3f) : 0f);
        if (done > 0) {
            disc(F.veil, 0, 0.035f, 0, r, 0x040306, 0.92f); ring(F.veil, 0, 0.04f, 0, r, 0.07f, mul(th.dirt(), 0.55f), 0.9f);
            Skin soil = noisy(th.dirt(), 0.25f, 7);                                                   // the spoil: little heaps round the rim, one more ring of them per dig
            for (int i = 0; i < done * 5; i++) { float an = i * 2.4f + 0.6f, rr = r + 0.14f + 0.1f * rnd(i * 7), s = 0.07f + 0.05f * rnd(i * 3);
                float x = (float) Math.cos(an) * rr, z = (float) Math.sin(an) * rr * 0.9f; push(); M.translate(x, surf(x, z), z).rotateY(i); box(-s, 0f, -s, s, s * 1.3f, s, soil, 16f, 0.015f); pop(); }
            int lc = treasure ? 0xFFD25A : energy; float charge = done / (float) digs.length;                     // what waits below shines up through the hole, brighter with every dig
            float fade = 1f - sm((t - (open + 0.9f)) / 0.6f);
            glow(F.add, 0, 0.12f, 0, 0.5f + 0.5f * charge, lc, 0.3f * charge * fade * (0.8f + 0.2f * (float) Math.sin(t * 9f)));
            if (charge >= 1f && t < open + 0.5f) column(F.add, 0, 0.03f, 0, 1.6f, 0.3f, lc, 0.35f * fade);
        }
        for (int i = 0; i < digs.length; i++) {                                                       // every dig throws clods of the planet's own ground, and a puff of dust
            float d = t - digs[i]; if (d < 0) continue;
            boolean lastDig = i == digs.length - 1; float pw = Math.min(1.25f, 0.8f + 0.16f * i + (lastDig ? 0.2f : 0f)); int lc2 = treasure ? 0xFFD25A : energy;
            debris(0.05f, 0.05f, d, 0.75f * pw, 9 + 3 * i, 40 + i * 13, th); debris(-0.1f, 0.12f, d, 1.15f * pw, 6 + 2 * i, 140 + i * 17, th);
            if (d < 0.6f) for (int q = 0; q < 10; q++) { float an = q * 0.63f + i, sp = 0.8f + 0.6f * rnd(q * 5 + i);                               // dust rolling out along the ground, and up
                glow(F.add, (float) Math.cos(an) * (0.25f + d * 1.9f * sp * pw), 0.12f + d * (0.3f + 0.7f * rnd(q + i * 3)), (float) Math.sin(an) * (0.25f + d * 1.6f * sp * pw), 0.2f + d * 0.35f, mixc(th.dirt(), 0xFFFFFF, 0.3f), 0.4f * (1f - d / 0.6f)); }
            if (d < 0.45f) { float e = 1f - (1f - d / 0.45f) * (1f - d / 0.45f);                                                                  // the blow runs out through the ground: a ring of the light below
                ring(F.add, 0, 0.06f, 0, 0.3f + 2.6f * pw * e, 0.1f * (1f - e) + 0.03f, lc2, 0.75f * (1f - d / 0.45f));
                if (lastDig) ring(F.add, 0, 0.07f, 0, 0.2f + 4.2f * e, 0.16f * (1f - e) + 0.03f, 0xFFFFFF, 0.6f * (1f - d / 0.45f)); }
            if (d < 0.14f) glint(F.add, 0.05f, 0.14f, 0.05f, (0.5f + 0.3f * i) * (1f - d / 0.14f) + 0.1f, 0xFFFFFF, 1f - d / 0.14f);                 // the bite itself: a white spark at the blade
            if (d < 0.7f) for (int q = 0; q < 5 + 2 * i; q++) { float an = q * 1.9f + i * 0.7f, sp = 1.2f + 2.6f * rnd(q * 9 + i * 5), g = d * sp, yy = 0.15f + d * (3.2f + 2f * rnd(q + i)) - 5.5f * d * d;   // sparks of that light thrown up with the earth
                if (yy > 0.05f) glow(F.add, (float) Math.cos(an) * g * 0.6f, yy, (float) Math.sin(an) * g * 0.6f, 0.07f, mixc(lc2, 0xFFFFFF, 0.5f), 1f - d / 0.7f); }
        }
        if (done > 0) {                                                                               // the ground cracks further with every dig, and the light below leaks up through the cracks and the hole
            int lc3 = treasure ? 0xFFD25A : energy; float open2 = treasure ? CHEST_UP0 : RISE0, fade2 = 1f - sm((t - (open2 + 0.7f)) / 0.6f), since = t - digs[done - 1], grow = done - 1 + eo(since / 0.25f);
            if (fade2 > 0.01f) {
                for (int c = 0; c < 7; c++) { float an = c * 0.9f + 0.3f, len = (0.35f + 0.33f * rnd(c * 3 + 1)) * grow; int steps = Math.round(len / 0.11f); float x = (float) Math.cos(an) * r * 0.9f, z = (float) Math.sin(an) * r * 0.8f, a2 = an;
                    for (int q = 0; q < steps; q++) { a2 += (rnd(c * 11 + q * 7) - 0.5f) * 0.9f; x += (float) Math.cos(a2) * 0.11f; z += (float) Math.sin(a2) * 0.1f;
                        glow(F.add, x, surf(x, z) + 0.05f, z, 0.075f, mixc(lc3, 0xFFFFFF, 0.35f), fade2 * (0.55f + 0.3f * (float) Math.sin(t * 13f + q + c)) * (1f - q / (float) (steps + 2))); } }
                float ch = done / (float) digs.length, flick = 0.85f + 0.15f * (float) Math.sin(t * 17f);
                column(F.add, 0, 0.03f, 0, 0.7f + 1.3f * grow, 0.1f + 0.07f * grow, lc3, 0.3f * ch * fade2 * flick);                                 // a shaft of it standing in the hole, taller every time
                for (int q = 0; q < 4 + 3 * done; q++) { float ph = (t * (0.7f + 0.5f * rnd(q * 3)) + rnd(q * 7)) % 1f, an = q * 2.1f; glow(F.add, (float) Math.cos(an) * r * 0.5f, 0.1f + ph * (0.8f + 0.9f * grow), (float) Math.sin(an) * r * 0.4f, 0.05f, mixc(lc3, 0xFFFFFF, 0.6f), fade2 * (1f - ph)); }   // motes climbing the shaft
                float d2 = t - open2; if (d2 >= 0f && d2 < 0.7f) { float e = 1f - (1f - d2 / 0.7f) * (1f - d2 / 0.7f);                               // it breaks: a pillar of light to the sky, two rings out across the ground
                    column(F.add, 0, 0.03f, 0, 7f, 0.55f * (1f - 0.6f * e), 0xFFFFFF, 0.6f * (1f - d2 / 0.7f)); column(F.add, 0, 0.03f, 0, 7f, 0.9f, lc3, 0.4f * (1f - d2 / 0.7f));
                    ring(F.add, 0, 0.07f, 0, 0.4f + 5f * e, 0.2f * (1f - e) + 0.04f, lc3, 0.9f * (1f - d2 / 0.7f)); ring(F.add, 0, 0.08f, 0, 0.3f + 3.2f * e, 0.1f, 0xFFFFFF, 0.7f * (1f - d2 / 0.7f));
                    glint(F.add, 0, 0.6f, 0, 1.6f * (1f - d2 / 0.7f) + 0.2f, 0xFFFFFF, 1f - d2 / 0.7f); }
            }
        }
        if (treasure) { float d = t - CLANG; if (d >= 0 && d < 0.35f) { glint(F.add, 0.05f, 0.1f, 0.1f, 0.55f * (1f - d / 0.35f) + 0.15f, 0xFFFFFF, 1f - d / 0.35f);            // the spade rings on the chest
            for (int q = 0; q < 7; q++) { float an = q * 0.9f; glow(F.add, (float) Math.cos(an) * d * 2.2f, 0.1f + d * (1.5f + rnd(q)) , (float) Math.sin(an) * d * 1.6f, 0.05f, 0xFFE9A0, 1f - d / 0.35f); } } }
    }

    // ------------------------------------------------------------------ you: an explorer in a star suit
    private static final Skin SUIT = (f, u, v, w, h) -> { float nz = rnd(f * 17 + u * 5 + v * 11 + 3); return v == h - 1 || (f == 4 && u == w / 2 && h > 8) ? 0x7A5AE0 : mul(0xE4E6F4, 0.9f + nz * 0.14f); };
    private static final Skin SUIT_LEG = (f, u, v, w, h) -> v < 3 ? 0x3A3E66 : mul(0xD4D6EA, 0.9f + rnd(f * 3 + u * 7 + v) * 0.14f);
    private static final Skin SUIT_ARM = (f, u, v, w, h) -> v < 2 ? 0x7A5AE0 : mul(0xE4E6F4, 0.9f + rnd(f * 5 + u * 3 + v * 7) * 0.14f);
    private static final Skin HELMET = (f, u, v, w, h) -> {
        if (f == 4 && w == 8 && h == 8 && u >= 1 && u <= 6 && v >= 2 && v <= 5) return (u + v) % 5 == 0 ? 0x02FFF4C8 : v >= 4 ? 0x02FFC040 : 0x02E89A20;      // the gold visor, catching the light
        return mul(0xF0F2FF, 0.9f + rnd(f * 13 + u * 7 + v * 5) * 0.12f); };

    static void hero(float t, float p, float[] digs, int energy, boolean treasure) {
        float open = treasure ? CHEST_UP0 : RISE0;
        // ---- what the arms do: the spade swings for every dig; then the sword, raised and brought down on every cut
        float aR = -0.35f, aL = 0.15f, lean = 0f, hop = 0f, back = 0f, dash = 0f, look = 0f; boolean sword = !treasure && t > RISE0 + 0.25f;
        for (float d0 : digs) { float d = t - d0; if (d > -0.3f && d < 0.22f) { float up = sm((d + 0.3f) / 0.2f) * (1f - sm((d + 0.06f) / 0.07f)); aR = lerp(-0.5f, -2.95f, up); aL = aR * 0.85f; lean = lerp(0.55f, -0.22f, up); hop = Math.max(hop, 0.42f * up); } }
        if (t > digs[digs.length - 1] + 0.22f || t < digs[0] - 0.3f) { aR = -0.35f; lean = 0f; }
        if (t > open) { float u = cl((t - open) / 0.45f); back = sm(u) * 0.75f; hop = (float) Math.sin(Math.PI * u) * 0.35f; }                              // it comes up: a jump back
        if (!treasure) {
            for (int i = 0; i < n; i++) { float d = t - hits[i]; if (d > -0.24f && d < 0.3f) { float wind = sm((d + 0.24f) / 0.14f), fall = sm((d + 0.05f) / 0.09f);
                aR = lerp(lerp(-0.6f, -2.75f, wind), -0.25f, fall); aL = -0.5f; dash = Math.max(dash, sm((d + 0.24f) / 0.18f) * (1f - sm((d - 0.08f) / 0.2f))); lean = 0.3f * fall * (1f - sm((d - 0.1f) / 0.2f)); } }
            if (t > RISE0 + 0.3f && t < hits[0] - 0.24f) aR = -1.15f + 0.06f * (float) Math.sin(t * 5f);                                          // on guard
            if (t > last + 0.3f) { aR = lerp(-0.25f, -0.2f, sm((t - last - 0.3f) / 0.4f)); look = -0.45f * sm((p - 0.1f) / 0.4f); }
        } else {
            float cheer = sm((t - (last - 0.05f)) / 0.25f); if (cheer > 0f) { aR = lerp(aR, -2.9f + 0.1f * (float) Math.sin(t * 9f), cheer); aL = lerp(aL, -2.9f + 0.1f * (float) Math.cos(t * 9f), cheer); hop += 0.18f * (float) Math.abs(Math.sin(t * 7f)) * cheer * (1f - sm((p - 1.2f) / 0.4f)); }
            look = 0.25f * sm((t - CHEST_UP0) / 0.4f) * (1f - sm(p / 0.3f)) - 0.4f * sm((p - 0.1f) / 0.4f);
        }
        float hx = HERO.x, hz = HERO.z, len = (float) Math.sqrt(hx * hx + hz * hz), ux = hx / len, uz = hz / len;                                 // away from the hole
        float reach = treasure ? 0f : 0.55f * dash;
        float x = hx + ux * (back - reach), z = hz + uz * (back - reach), y = surf(x, z) + hop;
        float in = 1f - eo(t / 0.4f); x += ux * 1.6f * in; z += uz * 1.6f * in;                                                                      // walks in at the start
        float faceY = (float) Math.atan2(-hx, -hz);
        float walk = in > 0.02f ? (float) Math.sin(t * 22f) * in : dash > 0.05f ? (float) Math.sin(t * 30f) * 0.7f * dash : 0f;
        final boolean sw = sword; final int en = energy; final float tt = t; final boolean gold = type == 2;
        Runnable tool = () -> {
            if (sw) {                                                                                                                           // a sword of light in the scene's colour (gold when RNG)
                int bc = gold ? 0xFFD24A : en;
                part(-0.6f, -0.6f, -1.5f, 0.6f, 0.6f, 2.5f, (f, u, v, w, h) -> 0x3A3E66); part(-2.2f, -0.7f, 2.5f, 2.2f, 0.7f, 3.5f, (f, u, v, w, h) -> 0xC8A030);
                part(-0.8f, -0.35f, 3.5f, 0.8f, 0.35f, 17f, (f, u, v, w, h) -> 0x02000000 | mixc(bc, 0xFFFFFF, u == 0 || u == w - 1 ? 0.35f : 0.75f));
                Vector3f tip = M.transformPosition(new Vector3f(0, 0, 11f)); glow(F.add, tip.x, tip.y, tip.z, 0.55f, bc, 0.4f + 0.1f * (float) Math.sin(tt * 11f));
            } else {                                                                                                                            // the spade: an ash shaft, an iron blade
                push(); M.rotateX(-0.52f);
                part(-0.5f, -13f, -0.5f, 0.5f, 3f, 0.5f, (f, u, v, w, h) -> mul(0x8A6A3E, 0.85f + rnd(u * 3 + v * 7) * 0.25f));
                part(-2.2f, -18.5f, -0.5f, 2.2f, -13f, 0.5f, (f, u, v, w, h) -> v == 0 ? 0xF0F4FF : mul(0xA8B0C0, 0.85f + rnd(u * 5 + v * 3 + f) * 0.25f)); pop();
            }
        };
        push(); M.translate(x, y, z).rotateY(faceY).rotateX(lean).scale(1.0f / 16f);
        float st = tint; tint = 0f; float bd = burstD, cr = crumble, cy = clipY; burstD = -1f; crumble = 0f; clipY = -99f; mobMode = true; rimCol = 0xC8C0FF; ambient = 0.3f;
        for (int s = -1; s <= 1; s += 2) { push(); M.translate(s * 2f, 12f, 0f).rotateX(walk * 0.7f * s - lean * 0.6f); part(-2f, -12f, -2f, 2f, 0f, 2f, SUIT_LEG); pop(); }
        part(-4f, 12f, -2f, 4f, 24f, 2f, SUIT);
        part(-3f, 14f, -4.5f, 3f, 23f, -2f, (f, u, v, w, h) -> f == 5 && u % 2 == 0 && v > 5 ? 0x0240E0FF : mul(0x5A5E8A, 0.85f + rnd(u * 7 + v * 3 + f) * 0.25f));       // the pack on its back, two lamps
        for (int s = -1; s <= 1; s += 2) { push(); M.translate(s * 6f, 22f, 0f).rotateX(s < 0 ? aL : aR); part(-2f, -10f, -2f, 2f, 2f, 2f, SUIT_ARM); if (s > 0) { M.translate(0f, -9f, 0f); tool.run(); } pop(); }
        push(); M.translate(0, 24f, 0).rotateX(look); part(-4.5f, 0f, -4.5f, 4.5f, 9f, 4.5f, HELMET); part(-0.5f, 9f, -0.5f, 0.5f, 12f, 0.5f, (f, u, v, w, h) -> v == h - 1 ? 0x02FF5A5A : 0x8A8EA8); pop();
        pop(); tint = st; burstD = bd; crumble = cr; clipY = cy; mobMode = false; ambient = 0f;
        disc(F.veil, x, surf(x, z) + 0.03f, z, 0.45f, 0x050508, 0.4f * (1f - 0.5f * cl(hop * 3f)));
    }

    // ------------------------------------------------------------------ a cut: the arc the sword leaves, the sparks where it lands
    static void cut(float t, int i, int energy) {
        float d = t - hits[i]; if (d < -0.04f || d > 0.4f) return;
        boolean lastCut = i == n - 1; int col = type == 2 ? (lastCut ? 0xFFD24A : SlayerScene.TIER[Math.min(i + 1, SlayerScene.TIER.length - 1)]) : type == 0 ? 0xDDE6FF : mixc(energy, 0xFFFFFF, 0.35f);
        float k = lastCut ? (type == 2 ? 1.6f : 1.25f) : 1f, a = d < 0 ? 0.6f : 1f - (d / 0.4f) * (d / 0.4f), sweep = cl((d + 0.04f) / 0.1f);
        float cy = 1.25f, R = 1.35f * k, a0 = (i % 2 == 0 ? 2.5f : 0.65f), dir = i % 2 == 0 ? -1f : 1f, span = 2.6f * sweep; int seg = 14;
        for (int q = 0; q < seg; q++) {
            float u0 = q / (float) seg, u1 = (q + 1) / (float) seg, an0 = a0 + dir * span * u0, an1 = a0 + dir * span * u1;
            float w0 = (float) Math.sin(Math.PI * u0) * 0.5f * k, w1 = (float) Math.sin(Math.PI * u1) * 0.5f * k;
            float[] c0 = arc(an0, R, cy), c1 = arc(an1, R, cy), i0 = arc(an0, R - w0, cy), i1 = arc(an1, R - w1, cy);
            int ca = argb(0.95f * a * u0, 0xFFFFFF), cb = argb(0.95f * a * u1, 0xFFFFFF), ia = argb(0f, col), ib = argb(0f, col);
            wquad(F.add, c0[0], c0[1], c0[2], ca, c1[0], c1[1], c1[2], cb, i1[0], i1[1], i1[2], ib, i0[0], i0[1], i0[2], ia);
            float[] o0 = arc(an0, R + w0 * 0.5f, cy), o1 = arc(an1, R + w1 * 0.5f, cy);
            wquad(F.add, o0[0], o0[1], o0[2], argb(0f, col), o1[0], o1[1], o1[2], argb(0f, col), c1[0], c1[1], c1[2], argb(0.8f * a * u1, col), c0[0], c0[1], c0[2], argb(0.8f * a * u0, col));
        }
        if (d >= 0) {
            glint(F.add, 0, cy, 0.4f, (0.9f + 0.6f * k) * (1f - d / 0.4f), 0xFFFFFF, a); glow(F.add, 0, cy, 0.3f, 1.2f * k, col, 0.5f * a);
            for (int q = 0; q < 12; q++) { float an = rnd(i * 31 + q * 7) * 6.2832f, v = 2.5f + rnd(i * 13 + q) * 3.5f; glow(F.add, (float) Math.cos(an) * v * d, cy + (float) Math.sin(an) * v * d * 0.8f - 2f * d * d, 0.35f, 0.06f, q % 3 == 0 ? 0xFFFFFF : col, a); }
            if (lastCut && type > 0) ring(F.add, 0, 0.06f, 0, 0.4f + 5f * eo(d / 0.4f), 0.2f, col, 0.7f * a);
        }
    }
    /** A point of a circle that faces the camera, round the creature. */
    private static float[] arc(float an, float r, float cy) { float cx = (float) Math.cos(an) * r, sy = (float) Math.sin(an) * r; return new float[]{right.x * cx + up.x * sy, cy + right.y * cx + up.y * sy, 0.35f + right.z * cx + up.z * sy}; }

    // ------------------------------------------------------------------ the creature
    static void creature(float t, float p, int kind, int energy) {
        float ar = (t - RISE0) / (RISE1 - RISE0); if (ar <= 0) return;
        float since = t - last; boolean gone = since >= 0; if (gone && since > 1.6f) return;
        float hitT = 99f, kb = 0f; for (float hh : hits) { float d = t - hh; if (d >= 0) { hitT = Math.min(hitT, d); kb += 0.14f * (d < 0.07f ? sm(d / 0.07f) : (float) Math.exp(-(d - 0.07f) * 7f)); } }
        float y = lerp(-2.9f, 0f, sm(ar)) + 0.1f * (float) Math.sin(Math.PI * cl(ar));
        if (kind == 5) y = Math.max(y, lerp(-2.9f, 0.55f, sm(ar)) + 0.12f * (float) Math.sin(t * 4f));                // the Harpy hangs in the air
        if (kind == 3) y += 0.12f + 0.06f * (float) Math.sin(t * 2.4f) * cl(ar);                                       // the Nymph drifts a little off the ground
        if (type == 0 && gone) y -= since * 0.9f;
        float toHero = (float) Math.atan2(HERO.x, HERO.z), toCam = (float) Math.atan2(eye.x, eye.z), face = alerp(toCam, toHero, 0.45f * sm((ar - 0.6f) / 0.4f));
        float roar = (float) Math.sin(Math.PI * cl((t - RISE1) / 0.5f));
        push(); M.translate(0, y, -kb).rotateY(face).rotateX(-0.2f * roar).scale(KS[kind] / 16f * (1f + 0.04f * roar));
        tint = hitT < 0.1f && !gone ? 0.5f * (1f - hitT / 0.1f) : 0f; tintCol = 0xFFFFFF;
        clipY = gone ? -99f : -0.15f; burstD = gone && type > 0 ? since : -1f; partNo = 0; mobMode = true; rimCol = mixc(energy, 0xFFFFFF, 0.3f);
        crumble = type == 0 && gone ? Math.min(1f, since / 0.5f) : 0f;
        float idle = 0.25f * (float) Math.sin(t * 2.2f) * (1f - roar) + (ar < 1f ? 0.5f * (float) Math.sin(t * 14f) * (1f - ar) : 0f);
        switch (kind) {
            case 0 -> minotaur(t, idle, false);
            case 1 -> hunter(t, idle);
            case 2 -> lynxes(t, idle);
            case 3 -> nymph(t, idle);
            case 4 -> bull(t, idle);
            case 5 -> harpy(t, idle);
            case 6 -> { golem(t, idle, 0x8A8272, 0x5E9A3E); Vector3f q = M.transformPosition(new Vector3f(0, 20f, 4.5f)); glow(F.add, q.x, q.y, q.z, 0.5f, 0x7AE860, 0.6f + 0.2f * (float) Math.sin(t * 5f)); }
            case 7 -> champion(t, idle);
            case 8 -> sphinx(t, idle);
            case 9 -> inquisitor(t, idle);
            case 10 -> manticore(t, idle);
            default -> minotaur(t, idle, true);
        }
        pop(); burstD = -1f; tint = 0f; crumble = 0f; mobMode = false; clipY = -99f;
        if (ar < 1.3f) { float d = t - RISE0; for (int i = 0; i < 16; i++) { float dd = d - rnd(i * 3) * 0.25f; if (dd < 0 || dd > 1.1f) continue;            // the ground it breaks through
            float an = rnd(i * 7) * 6.2832f, v = 1.2f + rnd(i * 11) * 1.8f, vy = 3f + rnd(i * 13) * 3f, yy = 0.2f + vy * dd - 4.9f * dd * dd; if (yy < 0.05f) yy = 0.05f;
            push(); M.translate((float) Math.cos(an) * v * dd, yy, (float) Math.sin(an) * v * dd * 0.7f).rotateXYZ(dd * 6 + i, dd * 4, i); float s = (0.08f + 0.06f * rnd(i)) * (1f - sm((dd - 0.8f) / 0.3f));
            box(-s, -s, -s, s, s, s, flat(i % 3 == 0 ? 0x5E9A3E : i % 3 == 1 ? 0x6A4A2A : 0x8A6A3E), 2f, 0.02f); pop(); } }
        if (roar > 0.01f) { float u = cl((t - RISE1) / 0.5f); ring(F.add, 0, 0.05f, 0, 0.5f + 3.6f * eo(u), 0.22f, energy, 0.6f * (1f - u)); }
        if (!gone && ar > 0.5f) { disc(F.veil, 0, 0.03f, 0, 0.75f * KS[kind], 0x050508, 0.45f);
            for (int i = 0; i < 10; i++) { float u = ((t * 0.5f + i / 10f) % 1f), an = i * 2.4f + t * 2f + u * 3f, r = 0.6f * (1f - u * 0.4f); glow(F.add, (float) Math.cos(an) * r, u * 2.2f, (float) Math.sin(an) * r, 0.06f, i % 3 == 0 ? 0xFFFFFF : energy, 0.7f * (1f - u) * sm(u * 5f)); } }
        if (gone) for (int i = 0; i < 40; i++) {                                                                       // cut down: stardust (grey dust when it leaves nothing)
            float u = cl((since - rnd(i) * 0.3f) / 1.3f); if (u <= 0 || u >= 1) continue;
            float an = rnd(i * 7) * 6.28f, r = 0.3f + u * (1.2f + rnd(i * 3)), yy = 0.3f + rnd(i * 11) * 1.8f + u * (type == 0 ? 0.5f : 1.4f);
            glow(F.add, (float) Math.cos(an) * r, yy, (float) Math.sin(an) * r * 0.7f, 0.06f, type == 0 ? 0x9A9AA8 : i % 3 == 0 ? 0xFFFFFF : energy, (1f - u) * 0.9f);
        }
    }

    // ---- skins
    private static Skin fur(int c, int seed) { return (f, u, v, w, h) -> mul(c, 0.82f + rnd(f * 13 + u * 7 + v * 3 + seed) * 0.3f); }
    private static Skin eyes(Skin base, int eye) { return (f, u, v, w, h) -> f == 4 && w == 8 && h == 8 && v == 4 && (u == 1 || u == 2 || u == 5 || u == 6) ? eye : base.at(f, u, v, w, h); }
    private static final int RED_EYE = 0x02FF4030, GOLD_EYE = 0x02FFD040, CYAN_EYE = 0x0260E8FF;

    /** A two-legged body in model pixels, feet at the origin, facing +z. Arms at their own angles; extra pieces on the head and in the right hand. */
    private static void man(float t, float walk, Skin head, Skin body, Skin arm, Skin leg, float aL, float aR, Runnable onHead, Runnable hand) {
        for (int s = -1; s <= 1; s += 2) { push(); M.translate(s * 2f, 12f, 0f).rotateX(walk * 0.7f * s); part(-2f, -12f, -2f, 2f, 0f, 2f, leg); pop(); }
        part(-4f, 12f, -2f, 4f, 24f, 2f, body);
        for (int s = -1; s <= 1; s += 2) { push(); M.translate(s * 6f, 22f, 0f).rotateX((s < 0 ? aL : aR) - walk * 0.4f * s); part(-2f, -10f, -2f, 2f, 2f, 2f, arm); if (s > 0 && hand != null) { M.translate(0f, -9f, 0f); hand.run(); } pop(); }
        push(); M.translate(0, 24f, 0).rotateY(0.15f * (float) Math.sin(t * 1.3f)); part(-4f, 0f, -4f, 4f, 8f, 4f, head); if (onHead != null) onHead.run(); pop();
    }
    /** A four-legged body: L long (z), W wide, H tall, standing on legs legH tall. Its head is added by the caller at the front (z = L / 2). */
    private static void beast(float walk, float L, float W, float H, float legH, float legT, Skin body, Skin leg) {
        for (int[] lg : new int[][]{{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) { push(); M.translate(lg[0] * (W / 2f - legT), legH, lg[1] * (L / 2f - legT - 0.5f)).rotateX(walk * 0.8f * (lg[0] == lg[1] ? 1 : -1)); part(-legT, -legH, -legT, legT, 0f, legT, leg); pop(); }
        part(-W / 2f, legH, -L / 2f, W / 2f, legH + H, L / 2f, body);
    }
    private static void horns(float span, float up, int col) {
        Skin hn = (f, u, v, w, h) -> v >= h - 1 ? 0xFFFFFF : mul(col, 0.85f + rnd(u * 3 + v * 5 + f) * 0.25f);
        for (int s = -1; s <= 1; s += 2) { part(s > 0 ? 4f : -4f - span, 5f, -1f, s > 0 ? 4f + span : -4f, 7f, 1f, hn); part(s > 0 ? 3f + span : -5f - span, 7f, -1f, s > 0 ? 5f + span : -3f - span, 7f + up, 1f, hn); }
    }
    private static void axe(int metal, boolean twin) {
        part(-0.7f, -0.7f, -3f, 0.7f, 0.7f, 20f, (f, u, v, w, h) -> mul(0x6A4A2A, 0.85f + rnd(u * 3 + v * 7) * 0.25f));
        Skin bl = (f, u, v, w, h) -> v == h - 1 || v == 0 ? 0xFFFFFF : mul(metal, 0.85f + rnd(u * 5 + v * 3 + f) * 0.25f);
        part(-0.4f, 0.7f, 13f, 0.4f, 6f, 19f, bl); if (twin) part(-0.4f, -6f, 13f, 0.4f, -0.7f, 19f, bl);
    }

    /** The Minotaur: a bull's head on a man's body, brown hide, a loincloth, horns, a nose ring and an axe. King Minos: bigger horns, a crown, a cape and a golden twin axe. */
    static void minotaur(float t, float idle, boolean king) {
        Skin hide = fur(king ? 0x4A2E22 : 0x6A4630, 3), cloth = (f, u, v, w, h) -> v < 5 ? mul(king ? 0x7A1A8A : 0x8A2A1E, 0.85f + rnd(u * 7 + v * 3) * 0.25f) : v == 5 ? 0xC8A030 : hide.at(f, u, v, w, h);
        Skin head = eyes(hide, king ? GOLD_EYE : RED_EYE);
        if (king) { push(); M.translate(0, 24f, -2.2f).rotateX(0.2f + 0.08f * (float) Math.sin(t * 3f)); part(-5.5f, -20f, -1f, 5.5f, 0f, 0f, (f, u, v, w, h) -> u == 0 || u == w - 1 || v == 0 ? 0xE8C040 : mul(0x7A1A8A, 0.85f + rnd(u * 3 + v) * 0.3f)); pop(); }
        man(t, idle, head, cloth, hide, hide, 0.1f, -0.9f + 0.2f * (float) Math.sin(t * 2.2f), () -> {
            part(-2.5f, 0.5f, 4f, 2.5f, 4f, 6.5f, (f, u, v, w, h) -> f == 4 && v == 1 && (u == 1 || u == 3) ? 0x1A1010 : f == 4 && v == 0 && u == 2 ? 0xE8C040 : mul(0x8A6A52, 0.9f + rnd(u + v * 3) * 0.15f));
            horns(king ? 4f : 3f, king ? 5f : 3.5f, 0xE8E0C8);
            if (king) part(-4.5f, 8f, -4.5f, 4.5f, 10.5f, 4.5f, (f, u, v, w, h) -> v == h - 1 && u % 2 == 1 ? 0x02000000 : u % 4 == 1 && v == 1 ? 0x02FF4060 : mul(0xE8C040, 0.9f + rnd(u * 3 + f) * 0.15f));
        }, () -> axe(king ? 0xE8C040 : 0xB8BCC8, king));
        if (king) for (int s = -1; s <= 1; s += 2) part(s > 0 ? 3.5f : -7f, 22f, -2.5f, s > 0 ? 7f : -3.5f, 25f, 2.5f, (f, u, v, w, h) -> mul(0xE8C040, 0.85f + rnd(u * 3 + v * 5 + f) * 0.2f));
    }
    /** The Minos Hunter: a lean archer in green leather and a hood, a quiver on its back, a bow drawn. */
    static void hunter(float t, float idle) {
        Skin leather = fur(0x3E6A34, 5), skin = fur(0xC89A72, 7);
        Skin hood = (f, u, v, w, h) -> f == 4 && w == 8 && h == 8 && v >= 1 && v <= 5 && u >= 1 && u <= 6 ? (v == 4 && (u == 2 || u == 5) ? 0x1A1010 : skin.at(f, u, v, w, h)) : leather.at(f, u, v, w, h);
        part(1f, 13f, -4f, 3.5f, 25f, -2f, (f, u, v, w, h) -> v >= h - 2 ? 0xE8E8F0 : 0x5A3A22);                             // the quiver
        man(t, idle, hood, leather, leather, fur(0x4A3A2A, 9), -1.45f, -1.2f + 0.05f * (float) Math.sin(t * 6f), null, () -> {
            Skin wood = (f, u, v, w, h) -> 0x7A5230;
            part(-0.5f, -0.5f, 2f, 0.5f, 9f, 3f, wood); part(-0.5f, -0.5f, 2f, 0.5f, -9f, 3f, wood); part(-0.5f, 8f, 0f, 0.5f, 9f, 2f, wood); part(-0.5f, -9f, 0f, 0.5f, -8f, 2f, wood);
            part(-0.15f, -8.5f, -0.2f, 0.15f, 8.5f, 0.2f, (f, u, v, w, h) -> 0xE8E8F0);
        });
    }
    /** The Siamese Lynxes: two big cats that move as one, a gold one and a grey one, tufted ears, short tails. */
    static void lynxes(float t, float idle) {
        for (int k0 = 0; k0 < 2; k0++) {
            final int k = k0; int c = k == 0 ? 0xD8A040 : 0x9AA0B0; Skin fr = (f, u, v, w, h) -> { float nz = rnd(f * 13 + u * 7 + v * 3 + k * 9); return nz < 0.14f ? mul(c, 0.55f) : mul(c, 0.85f + nz * 0.25f); };
            push(); M.translate(k == 0 ? -6f : 6f, 0f, k == 0 ? 1f : -1.5f).rotateY(k == 0 ? 0.25f : -0.3f);
            beast(idle * (k == 0 ? 1f : -1f), 16f, 7f, 6f, 6f, 1.3f, fr, fr);
            push(); M.translate(0, 9f, 8f).rotateX(0.15f * (float) Math.sin(t * 3f + k));
            part(-3.5f, 0f, 0f, 3.5f, 7f, 6f, (f, u, v, w, h) -> f == 4 && v == 4 && (u == 1 || u == 5) ? (k == 0 ? CYAN_EYE : GOLD_EYE) : f == 4 && v == 2 && u == 3 ? 0x2A1A1A : fr.at(f, u, v, w, h));
            part(-3.5f, 7f, 2f, -2f, 9.5f, 3.5f, fr); part(2f, 7f, 2f, 3.5f, 9.5f, 3.5f, fr); pop();
            push(); M.translate(0, 11f, -8f).rotateX(-0.6f + 0.3f * (float) Math.sin(t * 5f + k)); part(-1f, 0f, -4f, 1f, 2f, 0f, fr); pop();
            pop();
        }
    }
    /** The Stranded Nymph: slender, sea-green skin, a dress of leaves, flowers glowing in her long hair. */
    static void nymph(float t, float idle) {
        Skin skin = fur(0x8AD8B8, 3), dress = (f, u, v, w, h) -> (u + v) % 3 == 0 ? 0x3E8A3A : mul(0x5EB04A, 0.85f + rnd(u * 7 + v * 5 + f) * 0.25f);
        Skin head = (f, u, v, w, h) -> f == 4 && w == 8 && h == 8 && v == 4 && (u == 2 || u == 5) ? 0x02FFF0A0 : v >= h - 2 || (f != 4 && f != 3) ? ((u * 3 + v) % 7 == 0 ? 0x02FF8AD8 : mul(0x2A6A4A, 0.85f + rnd(u * 3 + v * 7) * 0.3f)) : skin.at(f, u, v, w, h);
        push(); M.translate(0, 0f, 0f); part(-4.5f, 4f, -2.5f, 4.5f, 13f, 2.5f, dress); pop();
        man(t, idle * 0.5f, head, dress, skin, skin, -0.4f + 0.2f * (float) Math.sin(t * 2f), -0.4f - 0.2f * (float) Math.sin(t * 2f), () -> part(-4.5f, -8f, -4.8f, 4.5f, 6f, -3.5f, (f, u, v, w, h) -> (u + v * 2) % 6 == 0 ? 0x02FF8AD8 : mul(0x2A6A4A, 0.85f + rnd(u * 5 + v * 3) * 0.3f)), null);
        for (int i = 0; i < 6; i++) { float an = t * 1.6f + i * 1.05f; Vector3f q = M.transformPosition(new Vector3f((float) Math.cos(an) * 9f, 10f + 8f * ((t * 0.3f + i / 6f) % 1f), (float) Math.sin(an) * 9f)); glow(F.add, q.x, q.y, q.z, 0.07f, i % 2 == 0 ? 0x8AFFD8 : 0xFF8AD8, 0.8f); }
    }
    /** The Cretan Bull: a huge dark bull, a hump of muscle, long white horns, a gold ring in its nose, a hoof pawing. */
    static void bull(float t, float idle) {
        Skin hide = fur(0x3A2A26, 3);
        beast(idle, 22f, 11f, 10f, 8f, 1.8f, hide, (f, u, v, w, h) -> v < 2 ? 0x1A1414 : hide.at(f, u, v, w, h));
        part(-4.5f, 18f, 2f, 4.5f, 21f, 10f, hide);                                                                          // the hump
        push(); M.translate(0, 11f, 11f).rotateX(0.25f + 0.12f * (float) Math.sin(t * 3f));
        part(-4f, 0f, 0f, 4f, 8f, 7f, (f, u, v, w, h) -> f == 4 && v == 5 && (u == 1 || u == 6) ? RED_EYE : hide.at(f, u, v, w, h));
        part(-2.5f, 0f, 7f, 2.5f, 4f, 9f, (f, u, v, w, h) -> f == 4 && v == 2 && (u == 1 || u == 3) ? 0x1A1010 : f == 4 && v == 0 && u == 2 ? 0xE8C040 : mul(0x6A4A42, 0.9f + rnd(u + v * 3) * 0.15f));
        push(); M.translate(0, 1f, 3.5f); horns(4f, 4f, 0xF0EAD8); pop(); pop();
        push(); M.translate(0, 16f, -11f).rotateX(0.5f + 0.3f * (float) Math.sin(t * 4f)); part(-0.6f, -9f, -0.6f, 0.6f, 0f, 0.6f, hide); part(-1f, -11f, -1f, 1f, -9f, 1f, (f, u, v, w, h) -> 0x1A1414); pop();
    }
    /** The Harpy: a bird-woman, violet feathers, great wings beating, talons, a sharp beak. */
    static void harpy(float t, float idle) {
        Skin fea = (f, u, v, w, h) -> (u + v) % 4 == 0 ? 0x4A2A8A : mul(0x7A4AC8, 0.82f + rnd(f * 11 + u * 7 + v * 3) * 0.3f), skin = fur(0xD8B090, 5);
        Skin head = (f, u, v, w, h) -> f == 4 && w == 8 && h == 8 && v == 4 && (u == 2 || u == 5) ? GOLD_EYE : f == 4 && v < 6 && u >= 1 && u <= 6 ? skin.at(f, u, v, w, h) : fea.at(f, u, v, w, h);
        float flap = 0.5f + 0.55f * (float) Math.sin(t * 9f);
        for (int s = -1; s <= 1; s += 2) { push(); M.translate(s * 4f, 22f, -1.5f).rotateZ(s * flap).rotateY(s * -0.25f);
            part(s > 0 ? 0f : -13f, -3f, -0.6f, s > 0 ? 13f : 0f, 2f, 0.6f, fea); part(s > 0 ? 2f : -12f, -8f, -0.6f, s > 0 ? 12f : -2f, -3f, 0.6f, fea); part(s > 0 ? 5f : -10f, -12f, -0.6f, s > 0 ? 10f : -5f, -8f, 0.6f, fea); pop(); }
        Skin talon = (f, u, v, w, h) -> v < 3 ? 0xE8C040 : fea.at(f, u, v, w, h);
        man(t, 0.3f + idle * 0.3f, head, fea, fea, talon, 0.5f, 0.5f, () -> { part(-1f, 2f, 4f, 1f, 4f, 6.5f, (f, u, v, w, h) -> 0xE8A030); part(-2f, 8f, -3f, 2f, 11f, 1f, fea); }, null);
    }
    /** The Minos Champion: a hoplite in bronze, a crested helm, a round shield and a spear. */
    static void champion(float t, float idle) {
        int bz = 0xC8862A; Skin pl = (f, u, v, w, h) -> { float nz = rnd(f * 17 + u * 7 + v * 13 + 5); return (u == 0 || v == 0) ? mul(bz, 0.65f) : nz < 0.12f ? mixc(bz, 0xFFFFFF, 0.5f) : mul(bz, 0.85f + nz * 0.3f); };
        Skin helm = (f, u, v, w, h) -> f == 4 && w == 8 && h == 8 && ((v >= 3 && v <= 4 && u >= 1 && u <= 6) || (u >= 3 && u <= 4 && v < 3)) ? (v == 4 && (u == 2 || u == 5) ? RED_EYE : 0x101010) : pl.at(f, u, v, w, h);
        Skin skirt = (f, u, v, w, h) -> u % 2 == 0 ? 0x8A1E1E : 0x5A1212;
        part(-4.5f, 8f, -2.5f, 4.5f, 13f, 2.5f, skirt);
        push(); M.translate(-7.5f, 16f, 2.5f); part(-1f, -5f, -0.6f, 1f, 5f, 0.6f, (f, u, v, w, h) -> { float dx = u - w / 2f + 0.5f, dy = v - h / 2f + 0.5f; return dx * dx + dy * dy < 3 ? 0xE8C040 : pl.at(f, u, v, w, h); }); part(-5f, -5f, 0.6f, 5f, 5f, 1.4f, (f, u, v, w, h) -> { float dx = u - 4.5f, dy = v - 4.5f, r = dx * dx + dy * dy; return r > 26 ? 0x02000000 : r < 3 ? 0xE8C040 : r > 18 ? mul(bz, 0.7f) : 0x8A1E1E; }); pop();
        man(t, idle, helm, pl, pl, pl, -0.5f, -0.75f + 0.12f * (float) Math.sin(t * 2.4f), () -> part(-0.7f, 8f, -5f, 0.7f, 12f, 5f, (f, u, v, w, h) -> mul(0xC81E2A, 0.85f + rnd(u * 3 + v * 7) * 0.3f)),
            () -> { part(-0.5f, -0.5f, -8f, 0.5f, 0.5f, 24f, (f, u, v, w, h) -> 0x6A4A2A); part(-1.2f, -0.3f, 24f, 1.2f, 0.3f, 29f, (f, u, v, w, h) -> u == 0 || u == w - 1 ? 0xFFFFFF : 0xC8CCD8); });
    }
    /** The Sphinx: a lion's body of sand-gold, folded wings, a woman's face under a striped royal headcloth. */
    static void sphinx(float t, float idle) {
        Skin sand = fur(0xD8B060, 3);
        beast(idle * 0.6f, 20f, 9f, 8f, 6f, 1.6f, sand, sand);
        for (int s = -1; s <= 1; s += 2) { push(); M.translate(s * 4.5f, 14f, 2f).rotateZ(s * (0.5f + 0.1f * (float) Math.sin(t * 2f))).rotateY(s * -0.2f); part(s > 0 ? 0f : -1f, 0f, -9f, s > 0 ? 1f : 0f, 7f, 1f, (f, u, v, w, h) -> (u + v) % 3 == 0 ? 0xB8903A : 0xF0E0A8); pop(); }
        push(); M.translate(0, 12f, 9f).rotateY(0.12f * (float) Math.sin(t * 1.3f));
        Skin cloth = (f, u, v, w, h) -> u % 2 == 0 ? 0x2A4AB8 : 0xE8C040;
        part(-4f, 0f, 0f, 4f, 8f, 6f, (f, u, v, w, h) -> f == 4 && u >= 1 && u <= 6 && v <= 5 ? (v == 4 && (u == 2 || u == 5) ? CYAN_EYE : v == 1 && u >= 3 && u <= 4 ? 0x8A4A3A : 0xD8A878) : cloth.at(f, u, v, w, h));
        part(-5.5f, -4f, 0.5f, -4f, 7f, 4f, cloth); part(4f, -4f, 0.5f, 5.5f, 7f, 4f, cloth); pop();
        push(); M.translate(0, 13f, -10f).rotateX(-0.9f + 0.25f * (float) Math.sin(t * 3f)); part(-0.6f, 0f, -8f, 0.6f, 1.2f, 0f, sand); part(-1.1f, -0.3f, -10f, 1.1f, 1.6f, -8f, fur(0x8A5A2A, 5)); pop();
    }
    /** The Minos Inquisitor: tall, in a black and crimson robe, a deep hood with burning eyes, a great curved blade. */
    static void inquisitor(float t, float idle) {
        Skin robe = (f, u, v, w, h) -> u == 0 || u == w - 1 ? 0x8A1420 : mul(0x1E1A26, 0.82f + rnd(f * 13 + u * 7 + v * 3) * 0.3f);
        Skin hood = (f, u, v, w, h) -> f == 4 && w == 8 && h == 8 && u >= 1 && u <= 6 && v >= 1 && v <= 5 ? (v == 4 && (u == 2 || u == 5) ? RED_EYE : 0x06040A) : robe.at(f, u, v, w, h);
        part(-5f, 0f, -3f, 5f, 13f, 3f, robe);
        man(t, idle * 0.3f, hood, robe, robe, robe, 0.2f, -1.0f + 0.15f * (float) Math.sin(t * 2f), () -> part(-4.5f, 8f, -4.5f, 4.5f, 9.5f, 2f, robe),
            () -> { part(-0.6f, -0.6f, -4f, 0.6f, 0.6f, 22f, (f, u, v, w, h) -> 0x2A2A34); Skin bl = (f, u, v, w, h) -> v == h - 1 ? 0x02FF6070 : mul(0xC8CCD8, 0.85f + rnd(u * 3 + v * 5) * 0.2f); part(-0.4f, 0.6f, 18f, 0.4f, 9f, 22f, bl); part(-0.4f, 6f, 12f, 0.4f, 9f, 18f, bl); });
        Vector3f q = M.transformPosition(new Vector3f(0, 28f, 4.5f)); glow(F.add, q.x, q.y, q.z, 0.35f, 0xFF4030, 0.5f + 0.2f * (float) Math.sin(t * 6f));
    }
    /** The Manticore: a red lion with a dark mane, bat wings, and a scorpion's tail arched over its back, its sting glowing. */
    static void manticore(float t, float idle) {
        Skin red = fur(0xA83A22, 3), mane = fur(0x4A1A12, 7);
        beast(idle, 21f, 10f, 8f, 7f, 1.7f, red, red);
        float flap = 0.35f + 0.3f * (float) Math.sin(t * 5f);
        for (int s = -1; s <= 1; s += 2) { push(); M.translate(s * 5f, 15f, 1f).rotateZ(s * flap).rotateY(s * -0.4f);
            part(s > 0 ? 0f : -12f, 0f, -0.5f, s > 0 ? 12f : 0f, 1.2f, 0.5f, mane); part(s > 0 ? 1f : -11f, -6f, -0.3f, s > 0 ? 11f : -1f, 0f, 0.3f, (f, u, v, w, h) -> u % 4 == 0 ? 0x4A1A12 : mul(0x7A2A1E, 0.85f + rnd(u * 3 + v * 7) * 0.25f)); pop(); }
        push(); M.translate(0, 9f, 10.5f).rotateX(0.12f * (float) Math.sin(t * 3f));
        part(-5.5f, -1.5f, -2f, 5.5f, 9.5f, 2.5f, mane);
        part(-3.5f, 0f, 2.5f, 3.5f, 7f, 7f, (f, u, v, w, h) -> f == 4 && v == 5 && (u == 1 || u == 5) ? GOLD_EYE : f == 4 && v == 1 && u >= 1 && u <= 5 ? (u % 2 == 1 ? 0xFFFFFF : 0x2A0A0A) : red.at(f, u, v, w, h)); pop();
        Skin shell = fur(0x2A1A1E, 9);                                                                                       // the tail: five segments curling up and over
        push(); M.translate(0, 13f, -10.5f); float curl = 0.62f + 0.08f * (float) Math.sin(t * 3f);
        for (int k = 0; k < 5; k++) { M.rotateX(-curl); part(-1.3f + k * 0.12f, -1.3f + k * 0.12f, -5f, 1.3f - k * 0.12f, 1.3f - k * 0.12f, 0f, shell); M.translate(0, 0, -5f); }
        part(-0.6f, -0.6f, -3.5f, 0.6f, 0.6f, 0f, (f, u, v, w, h) -> 0x0280FF60); Vector3f q = M.transformPosition(new Vector3f(0, 0, -3f)); glow(F.add, q.x, q.y, q.z, 0.3f, 0x80FF60, 0.7f); pop();
    }

    // ------------------------------------------------------------------ the chest
    static void chest(float t, float p, float A, int energy, Theme th) {
        float rise = sm((t - CHEST_UP0) / (CHEST_UP1 - CHEST_UP0)); if (t < CHEST_UP0) return;
        boolean rng = type == 2, empty = type == 0; int gold = 0xFFD25A;
        float y = lerp(-0.85f, 0.02f, eob(cl((t - CHEST_UP0) / (CHEST_UP1 - CHEST_UP0)))) + 0.03f * (float) Math.sin(t * 30f) * (1f - rise) * 0.5f;
        float openT = (t - (last - 0.5f)) / 0.5f, lid = openT <= 0 ? 0f : openT < 1f ? 0.12f * sm(openT) + 0.05f * (float) Math.sin(t * 40f) * openT : 0.12f + 1.75f * eob(cl((t - last) / 0.3f));      // it strains against the lid, then it flies open
        float since = t - last, end = 1f - sm((t - (A - 0.8f)) / 0.6f);
        Skin wood = (f, u, v, w, hh) -> { float nz = rnd(f * 19 + u * 7 + v * 13 + 5); boolean band = u == 1 || u == w - 2 || v == 0 || v == hh - 1;
            if (rng) return band ? mul(0x7A2AB8, 0.9f + nz * 0.2f) : nz < 0.1f ? 0xFFF4C8 : mul(0xE8B020, 0.85f + nz * 0.25f);
            return band ? mul(0x3A3E48, 0.9f + nz * 0.2f) : v % 3 == 0 ? mul(0x6A4420, 0.9f + nz * 0.15f) : mul(0x8A5A2A, 0.88f + nz * 0.2f); };
        push(); M.translate(0, y, 0.05f).rotateY((float) Math.atan2(eye.x, eye.z) * 0.6f).rotateZ(0.05f * (float) Math.sin(t * 25f) * (1f - rise));
        float bd = burstD, cy = clipY; burstD = -1f; clipY = -99f;
        box(-0.6f, 0f, -0.4f, 0.6f, 0.5f, 0.4f, wood, 14f, 0.03f);
        box(-0.07f, 0.3f, 0.4f, 0.07f, 0.52f, 0.46f, (f, u, v, w, hh) -> rng ? 0x02FF5AD8 : 0xE8C040, 14f, 0.02f);                          // the lock
        if (lid > 0.15f) { box(-0.52f, 0.36f, -0.32f, 0.52f, 0.47f, 0.32f, (f, u, v, w, hh) -> f == 2 ? (empty ? 0x1A1410 : (u * 3 + v * 5) % 4 == 0 ? 0x02FFF4C8 : 0x02FFC040) : 0x2A1C10, 12f, 0f); }   // what is inside: gold, or bare boards
        push(); M.translate(0, 0.5f, -0.4f).rotateX(-lid); box(-0.6f, 0f, 0f, 0.6f, 0.2f, 0.8f, wood, 14f, 0.03f); pop();
        pop(); burstD = bd; clipY = cy;
        for (int i = 0; i < 14; i++) { float d = (t - CHEST_UP0) - rnd(i * 3) * 0.5f; if (d < 0 || d > 0.9f) continue;                              // dirt sliding off it as it comes up
            float an = rnd(i * 7) * 6.2832f, r = 0.5f + 0.4f * rnd(i * 5); float s = 0.05f * (1f - d / 0.9f); push(); M.translate((float) Math.cos(an) * r, Math.max(0.04f, y + 0.6f - 2.2f * d * d), (float) Math.sin(an) * r * 0.6f).rotateXYZ(d * 5 + i, d * 3, i); box(-s, -s, -s, s, s, s, flat(i % 2 == 0 ? th.dirt() : th.top()), 2f, 0.015f); pop(); }
        disc(F.veil, 0, 0.035f, 0.05f, 0.85f * rise, 0x050508, 0.45f);
        if (empty) {                                                                                                                       // nothing inside: a breath of dust, and a moth
            if (since > 0 && since < 1.6f) for (int i = 0; i < 18; i++) { float u = cl((since - rnd(i) * 0.3f) / 1.2f); if (u <= 0 || u >= 1) continue; glow(F.add, (rnd(i * 7) - 0.5f) * (0.6f + u), 0.6f + u * (0.8f + rnd(i * 3)), 0.1f + (rnd(i * 11) - 0.5f) * 0.5f, 0.12f, 0x9A9AA8, (1f - u) * 0.6f); }
            return;
        }
        float seep = sm((t - (CHEST_UP0 + 0.2f)) / 0.5f) * (since < 0 ? 1f : 0f);                                                          // light under the lid before it opens
        if (seep > 0.01f) { glow(F.add, 0, y + 0.5f, 0.3f, 0.9f, gold, 0.35f * seep * (0.7f + 0.3f * (float) Math.sin(t * 13f))); for (int k = -2; k <= 2; k++) glint(F.add, k * 0.24f, y + 0.5f, 0.45f, 0.14f + 0.05f * (float) Math.sin(t * 15f + k), 0xFFF4C8, seep * (0.5f + 0.5f * sm(openT))); }
        if (since >= 0) {
            float a = sm(since / 0.12f) * end, burst = (float) Math.exp(-since * 3.5f);
            glow(F.add, 0, 0.75f, 0.2f, 1.5f + 1.2f * burst, gold, (0.35f + 0.5f * burst) * a); glint(F.add, 0, 0.7f, 0.3f, 1.2f + 1.5f * burst, 0xFFFFFF, 0.7f * burst * a);
            column(F.add, 0, 0.5f, 0, rng ? 14f : 4.2f, rng ? 0.9f : 0.6f, gold, (rng ? 0.34f : 0.26f) * a); column(F.add, 0, 0.5f, 0, rng ? 14f : 3.6f, 0.2f, 0xFFF4C8, 0.5f * a);
            for (int k = 0; k < 9; k++) {                                                                                                  // rays fanning out of the chest
                float an = 0.35f + k * 0.3f + 0.05f * (float) Math.sin(t * 2f + k), len = (2.2f + 0.8f * (float) Math.sin(t * 3f + k * 1.7f)) * (rng ? 1.5f : 1f), ax = (float) Math.cos(an), ay = (float) Math.sin(an), w = 0.1f;
                float ox = 0, oy = 0.6f, oz = 0.1f, ex = ox + (right.x * ax + up.x * ay) * len, ey = oy + (right.y * ax + up.y * ay) * len, ez = oz + (right.z * ax + up.z * ay) * len;
                float qx = (right.x * -ay + up.x * ax) * w * len, qy = (right.y * -ay + up.y * ax) * w * len, qz = (right.z * -ay + up.z * ax) * w * len; int c0 = argb(0.3f * a, 0xFFE9A0), c1 = argb(0, 0xFFE9A0);
                wquad(F.add, ox, oy, oz, c0, ox, oy, oz, c0, ex + qx, ey + qy, ez + qz, c1, ex - qx, ey - qy, ez - qz, c1);
            }
            int coins = rng ? 34 : 12;                                                                                                     // coins leap out (a fountain of them for RNG)
            for (int i = 0; i < coins; i++) { float d = since - rnd(i * 3) * (rng ? 1.6f : 0.25f); if (d < 0 || d > 1.3f) continue;
                float an = rnd(i * 7) * 6.2832f, v = 0.5f + rnd(i * 11) * 1.4f, vy = 3.4f + rnd(i * 13) * 2.4f, cx = (float) Math.cos(an) * v * d, cz = 0.1f + (float) Math.sin(an) * v * d * 0.7f, yy = 0.6f + vy * d - 4.9f * d * d; if (yy < 0.05f) continue;
                push(); M.translate(cx, yy, cz).rotateXYZ(d * 9 + i, d * 7, i); float s = 0.06f; box(-s, -s * 0.25f, -s, s, s * 0.25f, s, (f, u, v2, w, hh) -> f == 2 || f == 3 ? 0x02FFD24A : 0xC88A10, 6f, 0.012f); pop();
                if (i % 3 == 0) glint(F.add, cx, yy, cz, 0.16f, 0xFFF4C8, 0.8f * (1f - d / 1.3f)); }
            for (int q = 0; q < 16; q++) { float u = ((since * 0.6f + q / 16f) % 1f), an = q * 2.4f + t * 2f; glint(F.add, (float) Math.cos(an) * 0.4f, 0.6f + u * (rng ? 5f : 2.6f), 0.1f + (float) Math.sin(an) * 0.4f, 0.13f, q % 2 == 0 ? 0xFFFFFF : gold, (1f - u) * a); }
        }
    }
}
