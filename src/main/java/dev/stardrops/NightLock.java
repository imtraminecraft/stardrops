package dev.stardrops;

import net.minecraft.world.level.Level;

/**
 * Always night: while it is on, ClockMixin answers every client clock question with midnight, so the world has no sun and the
 * galaxies of the sky box make sense. Client side only; one constant value, so the sky cannot flicker between two times.
 */
public final class NightLock {
    private NightLock() {}

    /** Midnight of day 0: the moon is full and straight overhead. */
    public static final long MIDNIGHT = 18000L;
    private static boolean raw;

    public static boolean on() {
        if (raw) return false;
        try { StardropsConfig c = StardropsConfig.get(); return c.cosNight && c.cosOnlyNight; } catch (Throwable t) { return false; }
    }

    /** The world's real clock (what the server says), for the things that show the time: the day and night clock, the flicker diary. */
    static long real(Level level) {
        raw = true;
        try { return level.getOverworldClockTime(); } finally { raw = false; }
    }
}
