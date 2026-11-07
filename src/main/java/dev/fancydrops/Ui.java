package dev.fancydrops;

/** Shared colours (ARGB). */
final class Ui {
    // Stardrops, "deep nebula": near-black indigo panels over a starfield, violet and nebula pink for accents, starlight-white text.
    // Colour is kept for icons and states, not for chrome.
    static final int ACCENT = 0xFF9B7BFF, ACCENT2 = 0xFFFF7AD9, GOLD = 0xFFFFC94A, PANEL = 0xFF0E1030, PANEL2 = 0xFF070816, BORDER = 0xFF2C2A66, ROW = 0xFF14163E, ROW_HOVER = 0xFF1F2156,
            TEXT = 0xFFEEF0FF, SUB = 0xFFB9B6E6, DIM = 0xFF7672AE, OFF = 0xFF2A2858, GREEN = 0xFF62E6A8, RED = 0xFFFF5555,
            HAIR = 0xFF5A3F9A, SHINE = 0x12FFFFFF, ANIM_SUB = 0xFF9DB2CC,      // ANIM_SUB: the animation text keeps its own colour
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
