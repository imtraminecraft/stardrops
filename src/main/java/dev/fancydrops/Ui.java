package dev.fancydrops;

/** Shared colours (ARGB). */
final class Ui {
    // The observatory look (space theme): deep night-blue panels over a starfield, brass (the gold of an astrolabe) for accents,
    // starlight-white text. Colour is kept for icons and states, not for chrome.
    static final int ACCENT = 0xFFE8C26A, GOLD = 0xFFFFC94A, PANEL = 0xFF0B0E1C, PANEL2 = 0xFF080A15, BORDER = 0xFF262C4E, ROW = 0xFF121630, ROW_HOVER = 0xFF1B2144,
            TEXT = 0xFFEEF2FF, SUB = 0xFFB4BCDC, DIM = 0xFF6F78A0, OFF = 0xFF2A3052, GREEN = 0xFF62E6A8, RED = 0xFFFF5555,
            HAIR = 0xFF4A4428, SHINE = 0x12FFFFFF, ANIM_SUB = 0xFF9DB2CC,      // ANIM_SUB: the animation text keeps its own colour
            PRICE = 0xFFFFFFFF, PRICE_SRC = 0xFFAAAAAA, MAGIC_FIND = 0xFF55FFFF,
            R_RARE = 0xFF5555FF, R_VERY = 0xFFAA00AA, R_CRAZY = 0xFFFF55FF, R_LEGEND = 0xFFFFAA00;

    /** Colour for a Hypixel drop-odds label ("Occasional", "Pray RNGesus", ...). */
    static int odds(String label) {
        if (label == null) return ANIM_SUB;
        return switch (label) {
            case "Guaranteed" -> 0xFF9AA5B1; case "Occasional" -> 0xFF8FD694; case "Rare" -> 0xFF6FA8FF;
            case "Extraordinary" -> 0xFFC77DFF; case "Pray RNGesus" -> 0xFFFFC040; case "RNGesus Incarnate" -> 0xFFFF6EC7;
            default -> ANIM_SUB;
        };
    }
    private Ui() {}
}
