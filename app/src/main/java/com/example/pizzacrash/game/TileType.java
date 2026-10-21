package com.example.pizzacrash.game;

/**
 * Ingredient tile identities. Each type has a stable id (used in level JSON and
 * objectives) plus a solid color + emoji glyph fallback so the board renders
 * before final raster artwork is approved.
 */
public enum TileType {
    PIZZA(0, 0xFFFF7043, "\uD83C\uDF55"),      // 🍕
    PEPPERONI(1, 0xFFE53935, "\uD83E\uDE93"),  // 🥓 (pepperoni-ish)
    TOMATO(2, 0xFFF44336, "\uD83C\uDF45"),     // 🍅
    CHEESE(3, 0xFFFFCA28, "\uD83E\uDDC0"),     // 🧀
    MUSHROOM(4, 0xFF8D6E63, "\uD83C\uDF44"),   // 🍄
    ONION(5, 0xFFBA68C8, "\uD83C\uDFF7"),      // 🍷-ish purple onion -> use 🧅 if font supports
    PEPPER(6, 0xFF66BB6A, "\uD83E\uDED0");     // 🥐? no — green pepper fallback 🥬

    public final int id;
    public final int color;
    public final String glyph;

    TileType(int id, int color, String glyph) {
        this.id = id;
        this.color = color;
        this.glyph = glyph;
    }

    public static TileType fromId(int id) {
        for (TileType t : values()) if (t.id == id) return t;
        return PIZZA;
    }
}
