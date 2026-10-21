package com.example.pizzacrash.ui;

import android.content.Intent;
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
 * objective summary from LevelConfig and a big Play button that starts GameActivity.
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
                .setText(getString(WorldTheme.nameRes(world))
                        + " · " + getString(R.string.label_world, world));

        // Objective preview from LevelConfig (data-driven, spec §8).
        com.example.pizzacrash.game.LevelConfig cfg =
                com.example.pizzacrash.game.LevelConfig.load(this, levelId);
        StringBuilder objText = new StringBuilder();
        for (int[] obj : cfg.objectives) {
            if (objText.length() > 0) objText.append("   ");
            objText.append(obj[0] == -1
                    ? "\u2605 " + obj[1]
                    : com.example.pizzacrash.game.TileType.fromId(obj[0]).glyph + " " + obj[1]);
        }
        objText.append("  \u00b7  " + getString(R.string.game_moves_fmt, cfg.moveLimit));
        ((TextView) findViewById(R.id.text_objective)).setText(objText);

        int best = save.getBestScore(levelId);
        TextView bestView = findViewById(R.id.text_best_score);
        if (best > 0) {
            bestView.setVisibility(View.VISIBLE);
            bestView.setText(getString(R.string.label_best_score, best));
        } else {
            bestView.setVisibility(View.GONE);
        }

        findViewById(R.id.button_back).setOnClickListener(v -> finish());
        findViewById(R.id.button_play).setOnClickListener(v -> {
            Intent i = new Intent(this, GameActivity.class);
            i.putExtra(GameActivity.EXTRA_GLOBAL_LEVEL, levelId);
            startActivity(i);
        });
    }
}
