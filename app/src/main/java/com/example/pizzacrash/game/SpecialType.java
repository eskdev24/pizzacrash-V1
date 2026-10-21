package com.example.pizzacrash.game;

/**
 * Special-tile kinds created by 4+ matches (original PizzaCrash identity):
 *  - ROW_BLAST  : clears its entire row when removed (created by a horizontal 4)
 *  - COL_CRUSH  : clears its entire column when removed (created by a vertical 4)
 *  - PIZZA_BOMB : clears a 3x3 area when removed (created by an L/T shape or 5+)
 * Stored on BoardModel as numTypes + kind so plain ingredient ids stay 0..numTypes-1.
 */
public enum SpecialType {
    NONE(0), ROW_BLAST(1), COL_CRUSH(2), PIZZA_BOMB(3);

    public final int kind;

    SpecialType(int kind) { this.kind = kind; }

    public static SpecialType fromKind(int kind) {
        for (SpecialType s : values()) if (s.kind == kind) return s;
        return NONE;
    }
}
