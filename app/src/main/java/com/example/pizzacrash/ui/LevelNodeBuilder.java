package com.example.pizzacrash.ui;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.pizzacrash.R;
import com.example.pizzacrash.data.SaveRepository;

/**
 * Builds the 20 level nodes of a world map programmatically and places them on
 * a meandering path (alternating left/right offsets) inside the given container.
 *
 * One implementation serves all 10 worlds — only the world number changes.
 * Node states: completed (green + stars), current (red highlight), unlocked
 * (white/gold), locked (grey + lock icon, not tappable into gameplay).
 */
final class LevelNodeBuilder {

    private static final int[] OFFSETS_DP = {-72, -36, 36, 72};

    private LevelNodeBuilder() {}

    /** Appends 20 configured level nodes to {@code container}. */
    static void populate(Context context, LinearLayout container, int world) {
        SaveRepository save = new SaveRepository(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        float density = context.getResources().getDisplayMetrics().density;

        for (int local = 1; local <= SaveRepository.LEVELS_PER_WORLD; local++) {
            int globalId = SaveRepository.globalLevelId(world, local);
            boolean unlocked = save.isLevelUnlocked(globalId);
            int stars = save.getStars(globalId);
            boolean current = globalId == save.getUnlockedLevel();

            View node = inflater.inflate(R.layout.item_level_node, container, false);

            TextView number = node.findViewById(R.id.node_number);
            ImageView lock = node.findViewById(R.id.node_lock);
            View circleView = node.findViewById(R.id.node_circle);

            number.setText(String.valueOf(local));

            if (!unlocked) {
                circleView.setBackgroundResource(R.drawable.bg_node_locked);
                number.setVisibility(View.INVISIBLE);
                lock.setVisibility(View.VISIBLE);
                node.findViewById(R.id.node_stars).setVisibility(View.INVISIBLE);
            } else {
                if (stars > 0) {
                    circleView.setBackgroundResource(R.drawable.bg_node_done);
                    number.setTextColor(0xFFFFFFFF);
                } else if (current) {
                    circleView.setBackgroundResource(R.drawable.bg_node_current);
                    number.setTextColor(0xFFFFFFFF);
                } else {
                    // Open node: white/gold fill needs the dark default text.
                    circleView.setBackgroundResource(R.drawable.bg_node_open);
                    number.setTextColor(context.getColor(R.color.text_primary));
                }
                bindStars(node.findViewById(R.id.node_star_1),
                        node.findViewById(R.id.node_star_2),
                        node.findViewById(R.id.node_star_3), stars);
                node.setOnClickListener(v -> {
                    com.example.pizzacrash.audio.AudioManager.get(context)
                            .hapticTap(v);
                    openLevelIntro(context, globalId);
                });
                node.setClickable(true);
                node.setFocusable(true);
                node.setContentDescription(
                        context.getString(R.string.cd_level_node, local, world));
            }

            // Meandering horizontal placement along the vertical path.
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) node.getLayoutParams();
            int offsetDp = OFFSETS_DP[(local - 1) % OFFSETS_DP.length];
            if (offsetDp >= 0) {
                lp.gravity = android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL;
                lp.rightMargin = Math.round((88 - offsetDp) * density);
                lp.leftMargin = 0;
            } else {
                lp.gravity = android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL;
                lp.leftMargin = Math.round((88 + offsetDp) * density);
                lp.rightMargin = 0;
            }
            int vGap = Math.round(14 * density);
            lp.topMargin = vGap;
            lp.bottomMargin = vGap;
            node.setLayoutParams(lp);

            container.addView(node);
        }
    }

    private static void bindStars(ImageView s1, ImageView s2, ImageView s3, int filled) {
        s1.setImageResource(filled >= 1 ? R.drawable.ic_star_small : R.drawable.ic_star_empty);
        s2.setImageResource(filled >= 2 ? R.drawable.ic_star_small : R.drawable.ic_star_empty);
        s3.setImageResource(filled >= 3 ? R.drawable.ic_star_small : R.drawable.ic_star_empty);
    }

    private static void openLevelIntro(Context context, int globalLevelId) {
        Intent i = new Intent(context, LevelIntroActivity.class);
        i.putExtra(LevelIntroActivity.EXTRA_LEVEL, globalLevelId);
        context.startActivity(i);
    }
}
