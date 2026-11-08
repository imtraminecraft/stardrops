package dev.stardrops;

/** A test switch for hunting the world flicker: /sd flicker turns Stardrops' parts off one group at a time, so the player can see which one
 *  (if any) makes the sky blink. Nothing is saved; a restart turns everything back on. */
final class Flicker {
    private Flicker() {}
    static final int AURAS = 1, NAMES = 2, HUD = 4, SLIDE = 8, TICKS = 16;
    static int off;
    private static int step;
    private static final int[] STEPS = {0, AURAS, NAMES, HUD, SLIDE, TICKS, AURAS | NAMES | HUD | SLIDE | TICKS};
    private static final String[] SAY = {"everything on", "auras off", "name tags, tab list and chat effects off", "Stardrops HUD off (lyrics, Now Playing, clock, ambience, animations)",
        "hotbar slide off", "background work off (drops, Spotify, playtime, achievements)", "ALL of Stardrops off"};
    static boolean on(int part) { return (off & part) == 0; }
    static String next() { step = (step + 1) % STEPS.length; off = STEPS[step]; return "Flicker test " + step + "/" + (STEPS.length - 1) + ": " + SAY[step] + ". Watch the sky for 20 seconds, then run /sd flicker again."; }
}
