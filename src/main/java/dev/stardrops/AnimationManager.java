package dev.stardrops;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Plays the drop animations, one at a time (the rest wait in a queue). An animation is a group ("myth", "treasure") and a type
 * (No Drops, Normal, RNG); its settings are StardropsConfig.anim("myth.rng"). Categories.java lists every group.
 */
public final class AnimationManager {
    /** The animation clock in ms: from System.nanoTime (smooth and steady), not currentTimeMillis (which can step in 15 ms jumps on Windows and makes motion judder). */
    private static final long NS0 = System.nanoTime(), MS0 = System.currentTimeMillis();
    static long clock() { return MS0 + (System.nanoTime() - NS0) / 1_000_000L; }
    /** The same clock in fractional seconds since start, for the smoothest timing of a playing animation. */
    static double clockS() { return (System.nanoTime() - NS0) / 1e9; }

    /** Everything the animation and its text need about one drop. */
    public static final class Item {
        public DropData.Source source;
        public String name = "", id = "", icon = "nether_star", odds = "", chance = "", mf, stateKey;     // stateKey: the library row this drop follows
        public int rank, amount = 1;
        public boolean untradeable, announce, sim, rngPick, fixedPrice;
        public volatile double price = -2;       // -2 = still loading, -1 = no price
        public volatile String priceSource = "";
        public String extra; public int extraCol = Ui.ANIM_SUB;     // one more line under the price
        public int color() {
            return switch (rank) { case 0 -> Ui.R_RARE; case 1 -> Ui.R_VERY; case 2 -> Ui.R_CRAZY; default -> Ui.R_LEGEND; };
        }
        public String title() { return amount > 1 ? amount + " " + name : name; }
    }

    /** The three types every group has. */
    public static final int NO_DROPS = 0, NORMAL = 1, RNG = 2;
    static final String[] TYPE_IDS = {"nodrops", "normal", "rng"};
    /** The type constant of a type id ("rng" -> RNG). */
    static int typeIndex(String id) { for (int i = 0; i < TYPE_IDS.length; i++) if (TYPE_IDS[i].equals(id)) return i; return NORMAL; }

    public static final class Anim {
        final Item item;                         // null = nothing dropped
        final List<Item> more = new ArrayList<>();      // more drops shown in the same animation (one kill, one reward, or drops that arrived together): up to 8 side by side
        final String group;                      // "myth", "treasure", "slayer", "crops", "feast", "bounty", "pests" or "exp"
        int type;                                // NO_DROPS, NORMAL or RNG (a drop that joins before the scene starts can raise Normal to RNG)
        final long created = clock();
        float speed = 1f;                        // 1.8 after a cocooned slayer boss
        boolean helios;                          // the Ray of Helios pest drop: a sunbeam instead of the vacuum
        long start; float lastT = -1f; int kind = -1;                  // kind: which Mythological creature to show (-1 = the one the drop belongs to)
        int variant = new java.util.Random().nextInt(3);          // which Normal flourish and entrance this drop gets (0 rings, 1 starburst, 2 spiral), so drops don't all look the same
        Anim(String group, int type, Item item) { this.group = group; this.type = type; this.item = type == NO_DROPS ? null : item; }
        String key() { return group + "." + TYPE_IDS[type]; }
        StardropsConfig.AnimCfg cfg() { return StardropsConfig.get().anim(key()); }
        float secs() { return sceneLen(group, type); }
        /** Extra seconds the finish is held so the words of several drops can be read (0.5 s per extra drop, at most 1.5). */
        float hold() { int n = count(); return n <= 1 ? 0f : Math.min(1.5f, (n - 1) * 0.5f); }
        /** The whole length: the scene plus the hold. */
        float total() { return secs() + hold(); }
        /** The drops this animation shows (0 to 8). */
        int count() { return item == null ? 0 : 1 + more.size(); }
        /** Every drop, in the order they came. */
        List<Item> items() { List<Item> l = new ArrayList<>(); if (item != null) { l.add(item); l.addAll(more); } return l; }
        /** The drop that decides the colour of the effects: the rarest one. */
        Item lead() { Item b = item; for (Item i : more) if (b == null || i.rank > b.rank) b = i; return b; }
        /** Adds a drop to this animation; false when it is already full. */
        boolean add(Item it) { if (it == null || item == null || count() >= MAX_ITEMS) return false; more.add(it); return true; }
    }

    private static final ArrayDeque<Anim> QUEUE = new ArrayDeque<>();
    private static Anim cur;

    static final int MAX_ITEMS = 8;
    /** Plays an animation. Drops of the same kind that arrive close together stack into one animation (8 pests killed at once show their drops side by side)
     *  instead of queueing: they join a waiting animation, or the playing one while its items have not appeared yet. */
    public static void play(Anim a) {
        if (a == null) return;
        if (a.item != null) InstrumentsHud.dropLanded(a.count(), a.type == RNG);     // Visuals > Instruments: the star log's session count
        if (a.item != null) CosmosHud.meteor(a.type == RNG);                     // Visuals > Cosmos: one meteor burns across the moment a drop lands, a whole shower for an RNG drop
        if (!isSlayer(a.group)) { for (Item i : a.items()) DropListener.announceNow(i); return; }     // Stardrops: Mob Drops have their new animation; the other categories send just their chat alert for now until theirs are done
        if (a.item != null) {
            Anim last = QUEUE.peekLast();
            if (last != null && canStack(last, a)) { stack(last, a, false); return; }
            if (last == null && cur != null && canStack(cur, a) && beforeItems(cur)) { stack(cur, a, true); return; }
        }
        QUEUE.add(a);
    }
    private static boolean canStack(Anim into, Anim a) {
        return into.item != null && into.group.equals(a.group) && into.kind == a.kind && into.helios == a.helios && into.count() + a.count() <= MAX_ITEMS && clock() - into.created < 5000;
    }
    private static void stack(Anim into, Anim a, boolean started) {
        for (Item i : a.items()) into.add(i);
        if (!started && a.type == RNG) into.type = RNG;                                          // a rare one joining raises the whole scene, but only before it starts
        if (started) for (Item i : a.items()) DropListener.announceNow(i);                         // the scene already sent its alerts: send these now
    }
    /** True while the playing scene has not shown its items yet (there is still time to add more). */
    private static boolean beforeItems(Anim c) {
        float t = (clock() - c.start) / 1000f * pace(c), O = opening(c.group, c.type);
        return t < O - 0.35f;
    }
    public static boolean isPlaying() { return cur != null || !QUEUE.isEmpty(); }
    /** True while the hotbar and hearts should slide away (a scene with cinematic bars). */
    public static boolean hidesHud() {
        if (cur == null || cur.type != RNG || !cur.cfg().bars) return false;
        float t = (clock() - cur.start) / 1000f * pace(cur), S = cur.secs(), O = opening(cur.group, cur.type);
        if (isSlayer(cur.group)) return t >= O - 0.5f && t < animLen(cur.group, cur.type) + cur.hold();      // slayer: hidden from just before the supernova until the animation ends
        return t >= O - 0.8f && t < S + cur.hold() - 0.05f;                                           // only while the cinematic bars are on screen (they slide in at O - 0.8 s, out at the end)
    }

    /** Which type a drop plays: its own pick in the Catalog, else RNG when it is worth the RNG price (Settings > Prices), else Normal. -1 = it has no animation. */
    public static int typeOf(Item it, StardropsConfig.DropCfg dc) {
        StardropsConfig c = StardropsConfig.get();
        int pick = dc == null ? 0 : dc.anim;
        if (pick == 3) return -1;
        if (pick == 1) return NORMAL;
        if (pick == 2) return RNG;
        return it.price >= c.epicThreshold ? RNG : NORMAL;
    }

    /** How much faster than real time a scene plays (1.8 after a cocooned slayer boss). */
    static float pace(Anim a) { return a.speed; }

    // ---- the fixed lengths (author request): No Drops 4 s, Normal 4 s, RNG 6 s; the words stay 2 s after the animation ends
    static final float TEXT_HOLD = 2.0f;
    /** How long the words stay after the animation: RNG a little longer. */
    static float textHold(int type) { return type == NO_DROPS ? 0f : TEXT_HOLD; }
    static int typeOfKey(String key) { return key.endsWith("rng") ? RNG : key.endsWith("nodrops") ? NO_DROPS : NORMAL; }
    /** The animation itself: the opening, the drop appearing and its finish. */
    static float animLen(int type) { return type == RNG ? 6f : 4f; }
    /** The whole scene without the multi-drop hold: the animation, then the words alone for 2 s (No Drops has no words). */
    static float sceneLen(int type) { return animLen(type) + textHold(type); }
    /** When the drop appears: the opening takes all but the last 1.4 s of the animation (the finish). */
    static float opening(int type) { return animLen(type) - 1.4f; }

    // ---- Slayer (Stardrops): its own timeline. The black hole forms, the boss is pulled in (three gulps), and at 3.4 s it is gone:
    // No Drops 5.5 s (the hole swallows the scene, "nothing dropped"), a drop 6 s, RNG 7 s (the supernova); the words stay 3 s after.
    /** The groups that play the star scene (Star3D): the slayer bosses, and since 0.9 the rest of Mob Drops: the Crimson Isle, the End, the Mines. */
    static boolean isSlayer(String groupOrKey) { return groupOrKey != null && (groupOrKey.startsWith("slayer") || groupOrKey.startsWith("crimson") || groupOrKey.startsWith("dragon") || groupOrKey.startsWith("mobs")); }
    /** Which creature and world the star scene shows for a group that has only one (-1: a slayer boss, by its own theme). */
    static int starBoss(String group) {
        if (group == null) return -1;
        return switch (group) {
            case "mobs_hub" -> Star3D.ZOMBIE; case "mobs_spider" -> 1; case "mobs_deep" -> Star3D.ZOMBIE; case "mobs_hollows" -> 8; case "mobs_park" -> 2;
            case "mobs_marsh" -> Star3D.SERPENT; case "mobs_canyon" -> Star3D.BUG; case "mobs_spooky" -> Star3D.WITCH; case "mobs_dungeon" -> Star3D.SKEL;
            default -> group.startsWith("crimson") ? 6 : group.startsWith("dragon") ? 7 : group.startsWith("mobs") ? 8 : -1; };
    }
    /** The world a location's creatures stand in (Star3D.world), -1 = the creature's own. Each Mob Drops location has its own. */
    static int starWorld(String group) {
        if (group == null) return -1;
        return switch (group) {
            case "mobs_hub" -> 0; case "mobs_spider" -> 1; case "dragon" -> 7; case "crimson" -> 6; case "mobs_deep" -> 9; case "mobs" -> 8; case "mobs_hollows" -> 14;
            case "mobs_park" -> 2; case "mobs_marsh" -> 11; case "mobs_canyon" -> 12; case "mobs_spooky" -> 13; case "mobs_dungeon" -> 10; default -> -1; };
    }
    /** A creature's colouring for this mob (Star3D.ZOMBIE_SKIN, KNIGHT_PLATE, GOLEM_COL, BUG_COL; serpent 1 = crowned). */
    static int variant(String mob) {
        return switch (mob == null ? "" : mob) {
            case "golden_ghoul", "king_midas", "drybark" -> 1; case "lapis_zombie", "obsidian_defender", "golem" -> 2; case "undead", "zombie_grunt", "zombie_soldier", "tank_zombie", "super_tank_zombie", "crypt_dreadlord", "crypt_souleater", "shadow_assassin" -> 3;
            case "zombie_villager", "scary_jerry", "trick_or_treater" -> 4;
            case "queen_ant" -> 1; case "pangolin" -> 2; case "beeheemoth" -> 3; case "honeymite" -> 4; case "brineling" -> 5; case "torrid" -> 6; case "the_loch_emperor" -> 1;
            default -> 0; };
    }
    /** The creature named in a drop's "mob" (Star3D's creature numbers), or -1 if it names none we have. */
    static int creature(String mob) {
        return switch (mob == null ? "" : mob) {
            case "magma_boss" -> 6; case "dragon" -> 7; case "automaton" -> 8; case "bladesoul" -> Star3D.BLADESOUL; case "barbarian_duke" -> Star3D.DUKE; case "ashfang" -> Star3D.ASHFANG;
            case "mage_outlaw" -> Star3D.MAGE; case "ghost" -> Star3D.GHOST; case "ice_walker" -> Star3D.WALKER; case "bal" -> Star3D.BAL; case "zealot" -> Star3D.ZEALOT; case "diamond_goblin" -> Star3D.GOBLIN;
            // every mob of the Mob Drops locations, as the kind of creature it is
            case "crypt_ghoul", "golden_ghoul", "zombie_villager", "lapis_zombie", "miner_zombie", "undead", "zombie_grunt", "zombie_soldier", "tank_zombie", "super_tank_zombie", "crypt_dreadlord", "crypt_souleater", "scary_jerry", "trick_or_treater" -> Star3D.ZOMBIE;
            case "zombie_knight", "zombie_commander", "king_midas", "shadow_assassin", "obsidian_defender" -> Star3D.KNIGHT;
            case "miner_skeleton", "skeleton_grunt", "skeleton_master", "skeleton_soldier", "skeletor", "sniper", "super_archer", "scared_skeleton" -> Star3D.SKEL;
            case "wither_skeleton", "wither_spectre", "withermancer" -> Star3D.WSKEL;
            case "old_wolf", "wolf", "howling_spirit", "pack_spirit", "soul_of_the_alpha", "glacite_mutt" -> 2;
            case "arachne", "arachne_s_keeper", "broodmother", "dasher_spider", "splitter_spider", "weaver_spider" -> 1;
            case "enderman", "voidling_extremist", "voidling_fanatic" -> 3;
            case "blaze", "flare", "millennia_aged_blaze", "smoldering_blaze" -> 4;
            case "magma_cube" -> 6;
            case "emerald_slime" -> Star3D.SLIME; case "sneaky_creeper" -> Star3D.CREEPER; case "redstone_pigman" -> Star3D.PIGMAN; case "shiny_pig" -> Star3D.PIG;
            case "goblin", "treasure_hoarder", "grunt", "puck" -> Star3D.GOBLIN;
            case "glacite_bowman", "glacite_mage", "littlefoot" -> Star3D.WALKER;
            case "honeymite", "beeheemoth", "giant_isopod", "pangolin", "queen_ant", "brineling", "torrid" -> Star3D.BUG;
            case "nessie", "the_loch_emperor" -> Star3D.SERPENT;
            case "drybark", "golem" -> Star3D.GOLEM;
            case "silkbreeze", "phantom_spirit", "wraith" -> Star3D.GHOST;
            case "crazy_witch" -> Star3D.WITCH; case "bat" -> Star3D.BAT;
            default -> -1; };
    }
    static final float SL_PIVOT = 3.4f;
    static float animLen(String g, int type) { return isSlayer(g) ? (type == RNG ? 7f : type == NORMAL ? 6f : 5.5f) : animLen(type); }
    static float opening(String g, int type) { return isSlayer(g) ? SL_PIVOT : opening(type); }
    static float textHold(String g, int type) { return isSlayer(g) ? StardropsConfig.get().textHold : textHold(type); }      // the words (No Drops too) stay as long as Text Changes says
    static float sceneLen(String g, int type) { return animLen(g, type) + textHold(g, type); }
    static float sceneLen(String key) { return sceneLen(key, typeOfKey(key)); }

    /** Called every frame from the HUD. */
    public static void render(GuiGraphicsExtractor g) {
        long now = clock();
        if (cur != null && (now - cur.start) / 1000f * pace(cur) > cur.total() + 0.001f) cur = null;
        if (cur == null && !QUEUE.isEmpty()) { cur = QUEUE.poll(); cur.start = now; for (Item i : cur.items()) DropListener.announceNow(i); }          // the chat alert goes out together with the animation
        if (cur == null) return;
        Minecraft mc = Minecraft.getInstance();
        float t = (now - cur.start) / 1000f * pace(cur);                  // a scene shorter than its 3.6 s minimum plays a little faster
        cues(cur, cur.lastT, t); cur.lastT = t;
        try { g.nextStratum(); } catch (Throwable ignored) { }             // above all the HUD drawn before it (the lyrics' and Now Playing's text included)
        draw(g, mc.font, cur, t, g.guiWidth(), g.guiHeight());
    }

    /** The moments (seconds) each sound slot (chop, split, reveal, linger, end) plays in a scene of this length. */
    static float[][] cueTimes(String scene) { return cueTimes(scene, 0f); }
    static float[][] cueTimes(String scene, float hold) {
        int ty = typeOfKey(scene); float S = sceneLen(scene, ty), O = opening(scene, ty), A = animLen(scene, ty); boolean tre = scene.startsWith("treasure"), nod = scene.endsWith("nodrops");
        float[] beats = tre ? new float[]{0.215f * O, 0.505f * O, 0.795f * O} : new float[]{0.34f * O, 0.58f * O, 0.80f * O};
        if (isSlayer(scene)) {                                                                       // Slayer: the boss rises, walks, the meteors strike, then the reveal and after
            float P = SL_PIVOT, Al = animLen(scene, ty);
            // the rise and the walk (SlayerScene.RISE / WALK), then the meteors: each strike sound starts so its impacts land on SlayerScene.HITS
            if (nod) return new float[][]{{SlayerScene.RISE}, {SlayerScene.WALK}, {SlayerScene.HITS[0][0] - 0.45f}, {SlayerScene.HITS[0][0] + 0.05f}, {P + 0.6f}};
            if (ty == RNG) return new float[][]{{SlayerScene.RISE}, {SlayerScene.WALK}, {SlayerScene.HITS[2][0] - 0.35f}, {P}, {P + 0.6f}};
            return new float[][]{{SlayerScene.RISE}, {SlayerScene.WALK}, {SlayerScene.HITS[1][0] - 0.35f}, {P}, {Al - 0.7f + hold}};
        }
        return new float[][]{beats, {O}, nod ? new float[0] : new float[]{O + 0.25f}, {O + 1.0f}, {A - 0.5f + hold}};
    }

    /** The sounds of the timeline: a beat for every strike or dig, the burst when the animation pivots, then the reveal, the linger and the end. */
    private static void cues(Anim a, float prev, float t) { cues(a.key(), a.hold(), prev, t); }
    private static void cues(String scene, float hold, float prev, float t) {
        boolean big = scene.endsWith("rng"), nod = scene.endsWith("nodrops");
        float[][] tm = cueTimes(scene, hold);
        for (int k = 0; k < tm[0].length; k++) if (prev < tm[0][k] && t >= tm[0][k]) SoundFx.playSlot(scene, "chop", 0.9f + 0.12f * k, big);
        if (prev < tm[1][0] && t >= tm[1][0]) SoundFx.playSlot(scene, "split", nod && !isSlayer(scene) ? 0.8f : 1f, big);
        if (tm[2].length > 0 && prev < tm[2][0] && t >= tm[2][0]) SoundFx.playSlot(scene, "reveal", 1f, big);
        if (tm[3].length > 0 && prev < tm[3][0] && t >= tm[3][0]) SoundFx.playSlot(scene, "linger", nod ? 0.85f : 1f, big);
        if (tm[4].length > 0 && prev < tm[4][0] && t >= tm[4][0]) SoundFx.playSlot(scene, "end", 1f, big);
    }

    // ---- the Play button of the Sounds page: just the sounds, in time, no picture
    private static String sndKey; private static long sndT0; private static float sndLast; private static boolean sndOn;
    /** Plays the sounds of a scene ("myth.rng") on their real timeline. */
    public static void playSoundsOnly(String key) { SoundFx.stopPreviews(); sndKey = key; sndT0 = clock(); sndLast = -1f; sndOn = true; }
    /** Call every frame while the menu is open: fires the due sounds of a sounds-only playback. */
    public static void tickSounds() {
        if (!sndOn) return;
        float t = (clock() - sndT0) / 1000f;
        cues(sndKey, 0f, sndLast, t); sndLast = t;
        if (t > sceneLen(sndKey) + 0.4f) sndOn = false;
    }
    /** Seconds into the playback of this scene (a sounds-only playback or the real animation), or -1 when nothing of it is playing. */
    public static float playingT(String key) {
        long now = clock();
        if (sndOn && key.equals(sndKey)) return (now - sndT0) / 1000f;
        if (cur != null && key.equals(cur.key())) return (now - cur.start) / 1000f * pace(cur);
        return -1f;
    }

    /** Draws one animation at time t (seconds) inside a W x H screen. */
    static void draw(GuiGraphicsExtractor g, Font font, Anim a, float t, int W, int H) {
        Ritual.draw(g, font, a, t, W, H);
    }

    // ------------------------------------------------------------------ the menu preview

    private static Item SAMPLE;
    static Item sampleItem() { return sample(); }
    private static Item sample() {
        if (SAMPLE == null) {
            Item it = new Item(); it.name = "Daedalus Stick"; it.id = "DAEDALUS_STICK"; it.icon = "stick"; it.rank = 2; it.price = 21_000_000; it.priceSource = "Auction House"; it.mf = "+250%"; it.chance = "1 in 1,800"; it.odds = "1 in 1,800";
            SAMPLE = it;
        }
        return SAMPLE;
    }
    private static final java.util.Map<String, Item> SAMPLES = new java.util.HashMap<>();
    /** A sample drop of a group for the menu preview: the Farming groups and the Table show one of their own drops (the rarest for RNG), the rest the stick. */
    private static Item sampleFor(String group, int type) {
        String k = group + type;
        Item have = SAMPLES.get(k);
        if (have != null && have.source != null) return have;
        DropData.Source src = null;
        for (DropData.Source x : DropData.sources()) if (x.id.equals(group)) src = x;
        if (src == null || src.drops.isEmpty()) return sample();
        DropData.Drop dr = type == RNG ? src.drops.get(src.drops.size() - 1) : src.drops.get(src.drops.size() / 3);
        Item it = DropListener.dropItem(src, dr, group.equals("exp") ? null : "OB:+25", true);
        it.price = 21_000_000; it.priceSource = "Auction House";
        SAMPLES.put(k, it);
        return it;
    }
    private static Item SAMPLE_HEAD;
    /** The Item editor shows a 3D head (a flat icon cannot spin or show a glow turning), so the sliders can be seen working. */
    private static Item sampleHead() {
        if (SAMPLE_HEAD == null) {
            Item it = null;
            for (DropData.Source s : MythData.ALL) { for (DropData.Drop d : s.drops) if (d.skull) { it = new Item(); it.name = d.name; it.id = d.id; it.icon = "stick"; break; } if (it != null) break; }
            if (it == null) return sample();
            it.rank = 2; it.price = 21_000_000; it.priceSource = "Auction House"; it.mf = "+250%"; it.chance = "1 in 1,800"; it.odds = "1 in 1,800";
            SAMPLE_HEAD = it;
        }
        return SAMPLE_HEAD;
    }
    private static String previewKey; private static long previewT0;
    /** Seconds into the animation the menu preview shows, or -1 to loop on its own clock. */
    public static float syncSec = -1f;
    /** The menu's live preview: loops the animation named by key ("myth.rng") inside a W x H screen. */
    public static void drawPreview(GuiGraphicsExtractor g, int W, int H, String key) {
        if (key == null || !key.contains(".")) key = "myth.normal";
        String group = key.substring(0, key.indexOf('.')), tid = key.substring(key.indexOf('.') + 1);
        int type = NORMAL; for (int i = 0; i < TYPE_IDS.length; i++) if (TYPE_IDS[i].equals(tid)) type = i;
        Anim a = new Anim(group, type, TextFx.itemOnly ? sampleHead() : sampleFor(group, type));
        a.kind = (int) (((clock() - (key.equals(previewKey) ? previewT0 : clock())) / 1000L) / Math.max(1, (long) (sceneLen(group, type) + 1f))) % Ritual.KINDS.length;      // the preview cycles through all twelve creatures
        long now = clock();
        if (!key.equals(previewKey)) { previewKey = key; previewT0 = now; }
        float secs = a.secs();
        float t = syncSec >= 0 ? syncSec : ((now - previewT0) / 1000f) % (secs + 1f);
        float pivot = opening(group, type);
        if (TextFx.freezeT >= 0) t = pivot + TextFx.freezeT;                       // held that many seconds after the drop appears
        else if (TextFx.itemOnly || TextFx.barsOnly) t = Math.min(pivot + 1.6f, secs - 0.05f);                                // the Item editor shows the scene held, with the item living on its own clock
        else if (TextFx.textOnly) t = Math.min(pivot + 1.6f, secs - 0.05f);                                // the Text editor draws the words alone (they loop on the text clock)
        if (t > secs + 0.01f) return;
        previewing = true;
        try { draw(g, Minecraft.getInstance().font, a, t, W, H); } finally { previewing = false; }
    }
    /** True while a menu preview is being drawn (the scene may then be repainted at a lower rate). */
    static boolean previewing;

    private AnimationManager() {}
}
