package com.example.pizzacrash.ui;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import com.example.pizzacrash.R;

/**
 * Per-world visual configuration for the reusable world map (spec §6:
 * "Change the artwork, title, theme ... through configuration rather than
 * duplicating the implementation").
 *
 * Backgrounds are stylized vector scenes generated from two flat brand colors
 * per world (see {@link #backgroundFor(int)}), so no raster artwork is
 * required until final art is approved.
 */
public final class WorldTheme {

    public static final int COUNT = 10;

    private WorldTheme() {
    }

    @StringRes
    public static int nameRes(int world) {
        switch (world) {
            case 2:  return R.string.world_2;
            case 3:  return R.string.world_3;
            case 4:  return R.string.world_4;
            case 5:  return R.string.world_5;
            case 6:  return R.string.world_6;
            case 7:  return R.string.world_7;
            case 8:  return R.string.world_8;
            case 9:  return R.string.world_9;
            case 10: return R.string.world_10;
            default: return R.string.world_1;
        }
    }

    /** Sky / upper scene color for the world background gradient. */
    @ColorRes
    public static int skyColor(int world) {
        switch (world) {
            case 2:  return R.color.w2_sky;
            case 3:  return R.color.w3_sky;
            case 4:  return R.color.w4_sky;
            case 5:  return R.color.w5_sky;
            case 6:  return R.color.w6_sky;
            case 7:  return R.color.w7_sky;
            case 8:  return R.color.w8_sky;
            case 9:  return R.color.w9_sky;
            case 10: return R.color.w10_sky;
            default: return R.color.w1_sky;
        }
    }

    /** Ground / lower scene color for the world background gradient. */
    @ColorRes
    public static int groundColor(int world) {
        switch (world) {
            case 2:  return R.color.w2_ground;
            case 3:  return R.color.w3_ground;
            case 4:  return R.color.w4_ground;
            case 5:  return R.color.w5_ground;
            case 6:  return R.color.w6_ground;
            case 7:  return R.color.w7_ground;
            case 8:  return R.color.w8_ground;
            case 9:  return R.color.w9_ground;
            case 10: return R.color.w10_ground;
            default: return R.color.w1_ground;
        }
    }

    /** Accent color used for banners/decor tinting per world. */
    @ColorRes
    public static int accentColor(int world) {
        switch (world) {
            case 2:  return R.color.w2_accent;
            case 3:  return R.color.w3_accent;
            case 4:  return R.color.w4_accent;
            case 5:  return R.color.w5_accent;
            case 6:  return R.color.w6_accent;
            case 7:  return R.color.w7_accent;
            case 8:  return R.color.w8_accent;
            case 9:  return R.color.w9_accent;
            case 10: return R.color.w10_accent;
            default: return R.color.w1_accent;
        }
    }

    /**
     * Builds a full-screen gradient drawable for the given world at runtime.
     * Allocation happens once per activity creation — not per frame — so this
     * stays cheap while keeping ONE reusable map implementation for all worlds.
     */
    public static android.graphics.drawable.GradientDrawable backgroundFor(
            android.content.Context context, int world) {
        int[] colors = new int[]{
                context.getColor(skyColor(world)),
                context.getColor(groundColor(world))
        };
        android.graphics.drawable.GradientDrawable gd =
                new android.graphics.drawable.GradientDrawable();
        gd.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        gd.setColors(colors);
        gd.setOrientation(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM);
        return gd;
    }
}
