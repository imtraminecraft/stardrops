package dev.stardrops;

/**
 * Gnetum (a HUD performance mod) redraws the HUD only every few frames and shows the cached picture in between. Our HUD moves every
 * frame (the drop animations, Now Playing, the meteors), so under Gnetum it looked choppy, about 20 updates a second. Gnetum has a
 * call for exactly this: while one of our HUD elements is being drawn we ask it to stop caching that element. Done once per element,
 * through reflection, so Stardrops does not need Gnetum and nothing happens without it.
 */
final class GnetumCompat {
    private GnetumCompat() {}
    private static java.lang.reflect.Method disable; private static boolean looked;
    private static final java.util.Set<String> DONE = new java.util.HashSet<>();

    /** Call at the start of one of our HUD elements' drawing. */
    private static java.lang.reflect.Field current;
    static void uncache(String element) {
        if (DONE.contains(element)) return;
        if (!looked) { looked = true; try { Class<?> g = Class.forName("me.decce.gnetum.Gnetum"); disable = g.getMethod("disableCachingForCurrentElement", String.class); current = g.getField("currentElement"); } catch (Throwable ignored) { } }
        if (disable == null || current == null) { DONE.add(element); return; }
        try { if (current.get(null) == null) return;                                         // Gnetum is not drawing an element of its own right now: try again next frame
            disable.invoke(null, "Stardrops " + element + " moves every frame"); DONE.add(element); } catch (Throwable e) { DONE.add(element); }
    }
}
