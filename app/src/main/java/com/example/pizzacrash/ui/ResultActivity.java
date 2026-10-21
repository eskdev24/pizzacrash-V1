package com.example.pizzacrash.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pizzacrash.R;
import com.example.pizzacrash.audio.AudioManager;
import com.example.pizzacrash.data.SaveRepository;

/**
 * Level results screen (UI_Refs/won-completed.png + loss-fail.png). One Activity,
 * two visual states. Rewards are duplicate-safe: coins are only awarded when the
 * record improved (SaveRepository.recordResult), matching spec §12.
 */
public class ResultActivity extends AppCompatActivity {

    public static final String EXTRA_WON = "won";
    public static final String EXTRA_GLOBAL_LEVEL = "global_level";
    public static final String EXTRA_SCORE = "score";
    public static final String EXTRA_STARS = "stars";
    public static final String EXTRA_COINS = "coins";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        boolean won = getIntent().getBooleanExtra(EXTRA_WON, false);
        int levelId = getIntent().getIntExtra(EXTRA_GLOBAL_LEVEL, 1);
        int score = getIntent().getIntExtra(EXTRA_SCORE, 0);
        int stars = getIntent().getIntExtra(EXTRA_STARS, 0);
        int coins = getIntent().getIntExtra(EXTRA_COINS, 0);

        SaveRepository save = new SaveRepository(this);
        AudioManager audio = AudioManager.get(this);

        findViewById(R.id.panel_win).setVisibility(won ? View.VISIBLE : View.GONE);
        findViewById(R.id.panel_lose).setVisibility(won ? View.GONE : View.VISIBLE);

        ((TextView) findViewById(R.id.result_title)).setText(
                won ? R.string.title_level_complete : R.string.title_out_of_moves);
        ((TextView) findViewById(R.id.result_score)).setText(
                getString(R.string.game_score_fmt, score));

        // star row (win only)
        if (won) {
            TextView starRow = findViewById(R.id.result_stars);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 3; i++) sb.append(i < stars ? "★ " : "☆ ");
            starRow.setText(sb.toString().trim());
            TextView coinView = findViewById(R.id.result_coins);
            coinView.setText(getString(R.string.label_coins_earned, coins));
        }

        boolean hasNext = won && levelId < SaveRepository.TOTAL_LEVELS
                && new SaveRepository(this).isLevelUnlocked(levelId + 1);
        findViewById(R.id.btn_next).setVisibility(hasNext ? View.VISIBLE : View.GONE);
        findViewById(R.id.btn_retry).setVisibility(won ? View.GONE : View.VISIBLE);

        findViewById(R.id.btn_home).setOnClickListener(v -> goHome());
        findViewById(R.id.btn_home_lose).setOnClickListener(v -> goHome());
        findViewById(R.id.btn_retry).setOnClickListener(v -> startLevel(levelId));
        findViewById(R.id.btn_next).setOnClickListener(v -> startLevel(levelId + 1));

        audio.playEffect(won ? AudioManager.SFX_WIN : AudioManager.SFX_LOSE);
    }

    private void startLevel(int id) {
        Intent i = new Intent(this, GameActivity.class);
        i.putExtra(GameActivity.EXTRA_GLOBAL_LEVEL, id);
        startActivity(i);
        finish();
    }

    private void goHome() {
        Intent i = new Intent(this, HomeActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }
}
