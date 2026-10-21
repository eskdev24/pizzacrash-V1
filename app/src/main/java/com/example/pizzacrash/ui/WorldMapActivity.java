package com.example.pizzacrash.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pizzacrash.R;
import com.example.pizzacrash.data.SaveRepository;

/**
 * Reusable world map — ONE implementation for all 10 worlds, driven by the
 * EXTRA_WORLD configuration + SaveRepository progression data (per spec §6).
 *
 * Contains: world name banner, star-progress counter (world stars / 60),
 * 20 level nodes (locked/unlocked/current/completed states with stars),
 * gift button, prev/next world navigation, back navigation.
 */
public class WorldMapActivity extends AppCompatActivity {

    public static final String EXTRA_WORLD = "world";

    private int world;
    private SaveRepository save;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_world_map);

        world = Math.max(1, Math.min(SaveRepository.TOTAL_WORLDS,
                getIntent().getIntExtra(EXTRA_WORLD, 1)));
        save = new SaveRepository(this);

        // Per-world themed background — configuration-driven, one map impl (§6).
        findViewById(R.id.map_root)
                .setBackground(WorldTheme.backgroundFor(this, world));

        findViewById(R.id.button_back).setOnClickListener(v -> finish());
        findViewById(R.id.button_gift).setOnClickListener(v ->
                startActivity(new Intent(this, DailyRewardActivity.class)));
        findViewById(R.id.button_prev_world).setOnClickListener(v -> goToWorld(world - 1));
        findViewById(R.id.button_next_world).setOnClickListener(v -> goToWorld(world + 1));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        ((TextView) findViewById(R.id.text_world_title))
                .setText(getString(WorldTheme.nameRes(world)));
        ((TextView) findViewById(R.id.text_world_subtitle))
                .setText(getString(R.string.label_world, world));

        int earned = 0;
        for (int local = 1; local <= SaveRepository.LEVELS_PER_WORLD; local++) {
            earned += save.getStars(SaveRepository.globalLevelId(world, local));
        }
        ((TextView) findViewById(R.id.text_star_progress)).setText(
                getString(R.string.label_star_progress, earned,
                        SaveRepository.LEVELS_PER_WORLD * 3));

        LevelNodeBuilder.populate(this, findViewById(R.id.map_path), world);

        // Prev/next world availability follows unlock rule: a world is unlocked
        // when any of its levels is unlocked.
        findViewById(R.id.button_prev_world).setEnabled(world > 1);
        boolean nextUnlocked = world < SaveRepository.TOTAL_WORLDS
                && save.isLevelUnlocked(SaveRepository.globalLevelId(world + 1, 1));
        findViewById(R.id.button_next_world).setEnabled(nextUnlocked);

        // Auto-scroll near the current level so players land on their frontier.
        ScrollView scroll = findViewById(R.id.map_scroll);
        int unlocked = save.getUnlockedLevel();
        if (SaveRepository.worldOf(unlocked) == world) {
            int local = SaveRepository.localLevelOf(unlocked);
            scroll.post(() -> {
                float density = getResources().getDisplayMetrics().density;
                int rowHeightPx = Math.round(112 * density); // node+stars+gaps
                int target = Math.max(0, (local - 1) * rowHeightPx
                        - scroll.getHeight() / 2);
                scroll.smoothScrollTo(0, target);
            });
        }
    }

    private void goToWorld(int target) {
        if (target < 1 || target > SaveRepository.TOTAL_WORLDS) return;
        Intent i = new Intent(this, WorldMapActivity.class);
        i.putExtra(EXTRA_WORLD, target);
        startActivity(i);
    }

}
