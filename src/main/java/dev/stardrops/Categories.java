package dev.stardrops;

import java.util.ArrayList;
import java.util.List;

/**
 * Every drop category the mod knows (Mythological, Slayers, Farming...) and what lives inside it. The menu builds the Drop Library,
 * Animations, Sounds and Chat pages from this list, so a category, an animation group or a mob added here shows up on all of them.
 */
final class Categories {
    /** One animation type of a group: what the Animation Editor lets you edit (No Drops, Normal, RNG...). */
    record Type(String id, String name, String icon, int color, String blurb) {}
    /** One thing that has its own animations: "Mythological Mobs", "Treasure". Its keys look like "myth.rng". */
    record Group(String id, String name, String icon, int color, String blurb, List<Type> types, boolean wip) {
        Group(String id, String name, String icon, int color, String blurb, List<Type> types) { this(id, name, icon, color, blurb, types, false); }
        String key(Type t) { return id + "." + t.id(); }
        Type type(String typeId) { for (Type t : types) if (t.id().equals(typeId)) return t; return types.get(0); }
    }
    /** A sub page of the menu. wip = nothing built yet (shows the construction sign). */
    record Cat(String id, String name, String icon, int color, boolean wip, String blurb, List<Group> groups) {
        /** Where this category's drops come from: a creature, a chest... (the Drop Library's sub-sub pages). */
        List<DropData.Source> sources() {
            List<DropData.Source> l = new ArrayList<>();
            for (Group g : groups) if (!g.wip()) l.addAll(sources(g.id()));
            return l;
        }
        /** The sources inside one group: the creatures of "myth", the burrow of "treasure", the sea creatures of "fish_lava". */
        List<DropData.Source> sources(String groupId) {
            List<DropData.Source> l = new ArrayList<>();
            if (ExtraData.BY_GROUP.containsKey(groupId)) { l.addAll(ExtraData.group(groupId)); return l; }
            if (groupId.equals("exp")) { if (!ExperimentData.SOURCE.id.isEmpty()) l.add(ExperimentData.SOURCE); return l; }
            if (id.equals("bosses")) { if (groupId.equals("slayer")) l.addAll(SlayerData.ALL); return l; }
            if (id.equals("farming")) { DropData.Source f = FarmData.byId(groupId); if (f != null) l.add(f); return l; }
            if (!id.equals("mythological")) return l;
            if (groupId.equals("myth")) l.addAll(MythData.spawnable(StardropsConfig.get().mythPet));
            if (groupId.equals("treasure")) {
                TreasureData.SOURCE.drops = new ArrayList<>(TreasureData.drops(StardropsConfig.get().mythSpade));          // the spade you picked decides what a burrow can hold
                l.add(TreasureData.SOURCE);
            }
            return l;
        }
    }

    static final Type NO_DROPS = new Type("nodrops", "No Drops", ":nodrops:", 0xFF9AA3B5, "The ritual ends and nothing drops.");
    static final Type NORMAL = new Type("normal", "Normal", ":normal:", 0xFF55D8FF, "A drop: the item shows with a glow and its text.");
    static final Type RNG = new Type("rng", "RNG", ":dice:", 0xFFFF72C6, "A rare drop: cinematic bars, blur, the big finish and a stamped banner.");
    private static final List<Type> MYTH_TYPES = List.of(NO_DROPS, NORMAL, RNG);
    private static final List<Type> SLAYER_TYPES = List.of(
        new Type("nodrops", "No Drops", ":nodrops:", 0xFF9AA3B5, "The boss's place stirs three times, then falls quiet: nothing dropped."),
        new Type("normal", "Normal", ":normal:", 0xFF55D8FF, "A drop: the boss's own place stirs three times (hands from a grave, a cocoon, a howl, crystal beams, fire vents, a coffin) and the item bursts out with its text."),
        new Type("rng", "RNG", ":dice:", 0xFFFF72C6, "A rare drop: cinematic bars, blur and the boss's own big finish."));

    /** Farming and Experiments: each group has the normal and the RNG animation only (a crop or a book always drops something). */
    private static final Type FARM_NORMAL = new Type("normal", "Normal", ":normal:", 0xFF55D8FF, "A drop: the plant grows and the item blooms with its text.");
    private static final Type FARM_RNG = new Type("rng", "RNG", ":dice:", 0xFFFF72C6, "A rare drop: cinematic bars, blur and a golden bloom.");
    private static final List<Type> FARM_TYPES = List.of(FARM_NORMAL, FARM_RNG);
    private static final List<Type> PEST_TYPES = List.of(
        new Type("normal", "Normal", ":normal:", 0xFF55D8FF, "A drop: a pest scuttles through the barn, the vacuum pulls it up and the item springs out with its text."),
        new Type("rng", "RNG", ":dice:", 0xFFFF72C6, "A rare drop: cinematic bars, blur and a big finish."));
    private static final List<Type> EXP_TYPES = List.of(
        new Type("normal", "Normal", ":normal:", 0xFF55D8FF, "A book opens and turns three pages: the items rise from its light with their text."),
        new Type("rng", "RNG", ":dice:", 0xFFFF72C6, "An RNG item: the book turns three pages in a storm of light, with cinematic bars and blur."));

    /** Fishing, the Crimson Isle, the End and the Mines: the creature's world rises around the item (the Showcase scene). */
    private static final List<Type> SHOW_TYPES = List.of(
        new Type("normal", "Normal", ":normal:", 0xFF55D8FF, "A drop: the creature's world rises around the item, then its text."),
        new Type("rng", "RNG", ":dice:", 0xFFFF72C6, "A rare drop: cinematic bars, blur and a big finish in the creature's colours."));
    /** The Crimson Isle, the End and the Mines (Mob Drops): the star scene with their own creature and world. */
    private static final List<Type> STAR_TYPES = List.of(
        new Type("normal", "Normal", ":normal:", 0xFF55D8FF, "A drop: its creature rises in its own world on the curve of a giant planet, burning meteorites rain down, the last one shatters it and its stardust gathers into your drop."),
        new Type("rng", "RNG", ":dice:", 0xFFFF72C6, "RNG: six meteorites climb the rarity colours and leave star-fires burning round the creature, everything holds its breath, and a golden star falls like a hammer. Your drop appears in a pillar of gold."));
    private static final List<Type> MINE_TYPES = List.of(
        new Type("normal", "Normal", ":normal:", 0xFF55D8FF, "A find: three strikes, then the item rises from the rock with its text."),
        new Type("rng", "RNG", ":dice:", 0xFFFF72C6, "A rare find: cinematic bars, blur and the rock bursts in a shower of gems."));
    private static final List<Type> CORPSE_TYPES = List.of(
        new Type("normal", "Normal", ":normal:", 0xFF55D8FF, "A find: a pickaxe strikes the Frozen Corpse's ice three times, its eyes start to glow, the ice shatters and the item rises."),
        new Type("rng", "RNG", ":dice:", 0xFFFF72C6, "A rare find: cinematic bars, blur, and the ice bursts in a blizzard of shards under a stamped banner."));
    private static final List<Type> DYE_TYPES = List.of(
        new Type("normal", "Normal", ":normal:", 0xFF55D8FF, "A dye: a splash of its own colour fills the screen around the vial."),
        new Type("rng", "RNG", ":dice:", 0xFFFF72C6, "A rare dye: cinematic bars and a flood of its colour."));

    /** True for a group whose Drop Library page is one list of drops (no creatures to pick first). */
    static boolean single(Group g) { return !hasSubs(g); }
    /** True for a group whose Drop Library page lists creatures, each with its own page. */
    static boolean hasSubs(Group g) { return g.id().equals("myth") || g.id().equals("slayer") || ExtraData.group(g.id()).size() > 1; }

    static final List<Cat> ALL = List.of(
        new Cat("bosses", "Mob Drops", ":raredrops:", 0xFFFF5A5A, false, "Every mob's rare drops, sorted by where the mob spawns, then by the mob.", List.of(
            new Group("slayer", "Slayers", ":grp_slayers:", 0xFFFF5A5A, "What plays when a slayer boss drops something.", SLAYER_TYPES),
            new Group("mobs_hub", "Hub", "rotten_flesh", 0xFF8FD0FF, "The Hub's graveyard, wolves and the Shiny Pig.", STAR_TYPES),
            new Group("mobs_spider", "Spider's Den", "spider_eye", 0xFFC46AFF, "Spiders, Arachne and her keepers.", STAR_TYPES),
            new Group("dragon", "The End", ":grp_end:", 0xFFC77DFF, "Ender Dragons, Zealots, Endermen, Voidlings and the Obsidian Defender.", STAR_TYPES),
            new Group("crimson", "Crimson Isle", ":grp_crimson:", 0xFFFF6A3A, "The minibosses, the Magma Boss and every creature of the Crimson Isle.", STAR_TYPES),
            new Group("mobs_deep", "Deep Caverns", "lapis_lazuli", 0xFF6FA8FF, "The miners, slimes and pigmen of the Deep Caverns.", STAR_TYPES),
            new Group("mobs", "Dwarven Mines", ":grp_mines:", 0xFF7FE8D8, "Ghosts, goblins, Treasure Hoarders and the Glacite creatures.", STAR_TYPES),
            new Group("mobs_hollows", "Crystal Hollows", "amethyst_shard", 0xFFC59BFF, "Automatons, Grunts and Bal.", STAR_TYPES),
            new Group("mobs_park", "The Park", "oak_log", 0xFF7BE05A, "The spirits of the Howling Cave.", STAR_TYPES),
            new Group("mobs_marsh", "Moonglade Marsh", "lily_pad", 0xFF5BD3B0, "Galatea: Nessie, the Loch Emperor and the marsh.", STAR_TYPES),
            new Group("mobs_canyon", "Torrhus Canyon", "honeycomb", 0xFFE0A060, "Galatea: the canyon's ants, isopods and spirits.", STAR_TYPES),
            new Group("mobs_spooky", "Spooky Festival", "jack_o_lantern", 0xFFFF9A3A, "The Spooky Festival's creatures.", STAR_TYPES),
            new Group("mobs_dungeon", "Catacombs", "skeleton_skull", 0xFFB0B6C2, "Dungeon mobs' rare drops.", STAR_TYPES))),
        new Cat("mythological", "Mythological", ":mytho:", 0xFF55D8FF, false, "Mythological Ritual is an event that occurs with the Mythological Ritual perk.", List.of(
            new Group("myth", "Mythological Mobs", ":mytho:", 0xFF55D8FF, "What plays when a ritual creature dies.", MYTH_TYPES),
            new Group("treasure", "Treasure", ":treasure:", 0xFFFFD25A, "What plays when a burrow holds treasure.", MYTH_TYPES))),
        new Cat("mining", "Mining", ":mining:", 0xFF55D8FF, false, "The Crystal Nucleus, Frozen Corpses and the Mining Fiesta.", List.of(
            new Group("nucleus", "Crystal Nucleus", ":grp_nucleus:", 0xFF9AE6FF, "What plays when the Nucleus loot bundle holds something rare.", MINE_TYPES),
            new Group("corpse", "Frozen Corpses", ":grp_corpse:", 0xFF8FD8FF, "What plays when a Frozen Corpse in a Glacite Mineshaft holds something rare.", CORPSE_TYPES),
            new Group("fiesta", "Mining Events", ":grp_fiesta:", 0xFFFFC857, "Rare finds while mining during a Mining Fiesta.", MINE_TYPES))),
        new Cat("farming", "Farming", ":farm:", 0xFF7BE05A, false, "Every rare drop you can find in the Garden.", List.of(
            new Group("crops", "Rare Crops", ":grp_crops:", 0xFF7CCB4F, "What plays when a crop turns out rare.", FARM_TYPES),
            new Group("feast", "Harvest Feast", ":grp_feast:", 0xFFFFA63A, "What plays when the Harvest Feast serves a rare crop.", FARM_TYPES),
            new Group("bounty", "Harvest Bounty", ":grp_bounty:", 0xFFB8E860, "What plays when a Harvest Bounty drops something.", FARM_TYPES),
            new Group("pests", "Pest Drops", ":grp_pests:", 0xFFD2603F, "What plays when a pest drops something.", PEST_TYPES))),
        new Cat("fishing", "Fishing", ":fishing:", 0xFF4FC3FF, false, "Sea creatures, the rare things they carry, and treasure.", List.of(
            new Group("fish_water", "Water", ":grp_water:", 0xFF4FC3FF, "Open water, Backwater Bayou, Galatea's waters, the Fishing Festival, hotspots and special catches.", SHOW_TYPES),
            new Group("fish_lava", "Lava", ":grp_lava:", 0xFFFF7A2E, "The Crimson Isle, its hotspots, the Magma Fields and the Plhlegblast Pool.", SHOW_TYPES),
            new Group("fish_spooky", "Spooky", ":grp_spooky:", 0xFFFF9A2E, "The Spooky Festival's sea creatures.", SHOW_TYPES),
            new Group("fish_winter", "Winter", ":grp_winter:", 0xFF9FE4FF, "Jerry's Workshop's sea creatures.", SHOW_TYPES),
            new Group("fish_treasure", "Treasure", ":grp_fish_treasure:", 0xFFFFC857, "Great and Outstanding Catches: the treasure your rod pulls up.", SHOW_TYPES))),
        new Cat("dyes", "Table & Dyes", ":cat_table:", 0xFFFF72C6, false, "The Experimentation Table's rewards, and every dye in the game.", List.of(
            new Group("exp", "Experimentation Table", ":grp_exp:", 0xFFB56BFF, "What plays when the Table gives out rewards.", EXP_TYPES),
            new Group("dyes", "Dyes", ":dye:", 0xFFFF72C6, "What plays when a dye drops.", DYE_TYPES))));

    static Cat byId(String id) { for (Cat c : ALL) if (c.id().equals(id)) return c; return ALL.get(0); }
    static Group group(String key) {
        String g = key.contains(".") ? key.substring(0, key.indexOf('.')) : key;
        for (Cat c : ALL) for (Group gr : c.groups()) if (gr.id().equals(g)) return gr;
        return ALL.get(0).groups().get(0);
    }

    private Categories() {}
}
