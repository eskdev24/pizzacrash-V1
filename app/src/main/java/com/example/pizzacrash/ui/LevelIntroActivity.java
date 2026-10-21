package com.example.pizzacrash.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pizzacrash.R;
import com.example.pizzacrash.data.SaveRepository;

/**
 * Level introduction screen (UI_Refs/level.png): shows the level number, world,
 * objective summary placeholder and a big Play button. The match-3 board arrives
 * in Phase 3; Play currently reports that gameplay is not yet built (honest stub).
 */
public class LevelIntroActivity extends AppCompatActivity {

    public static final String EXTRA_LEVEL = "level";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level_intro);

        int levelId = Math.max(1, Math.min(SaveRepository.TOTAL_LEVELS,
                getIntent().getIntExtra(EXTRA_LEVEL, 1)));
        SaveRepository save = new SaveRepository(this);

        // Guard: locked levels must never be startable (spec §10).
        if (!save.isLevelUnlocked(levelId)) {
            Toast.makeText(this, R.string.toast_level_locked, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        int world = SaveRepository.worldOf(levelId);
        int local = SaveRepository.localLevelOf(levelId);

        ((TextView) findViewById(R.id.text_level_number))
                .setText(getString(R.string.label_level, local));
        ((TextView) findViewById(R.id.text_world_name))
                .setText(getString(WorldMapActivity.worldNameRes(world))
                        + " · " + getString(R.string.label_world, world));

        int best = save.getBestScore(levelId);
        TextView bestView = findViewById(R.id.text_best_score);
        if (best > 0) {
            bestView.setVisibility(View.VISIBLE);
            bestView.setText(getString(R.string.label_best_score, best));
        } else {
            bestView.setVisibility(View.GONE);
        }

        findViewById(R.id.button_back).setOnClickListener(v -> finish());
        findViewById(R.id.button_play).setOnClickListener(v ->
                // Gameplay board is Phase 3 — do not fake functionality.
                Toast.makeText(this, R.string.toast_gameplay_pending, Toast.LENGTH_SHORT).show());
    }
}
