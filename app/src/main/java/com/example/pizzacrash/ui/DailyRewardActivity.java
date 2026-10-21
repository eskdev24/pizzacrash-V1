package com.example.pizzacrash.ui;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pizzacrash.R;
import com.example.pizzacrash.audio.AudioManager;
import com.example.pizzacrash.data.SaveRepository;
import com.google.android.material.button.MaterialButton;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Daily reward screen (UI_Refs/dailyreward.png): one claim per calendar day,
 * streak counter, coin + booster reward. Duplicate claims are prevented by
 * comparing the stored last-claim day (spec §12: no duplicate rewards).
 */
public class DailyRewardActivity extends AppCompatActivity {

    private static final long DAY_MS = TimeUnit.DAYS.toMillis(1);
    private static final int REWARD_COINS = 50;
    private static final String STREAK_BOOSTER = "hammer";

    private SaveRepository save;
    private MaterialButton buttonClaim;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_reward);

        save = new SaveRepository(this);
        findViewById(R.id.button_back).setOnClickListener(v -> finish());
        buttonClaim = findViewById(R.id.button_claim);
        buttonClaim.setOnClickListener(v -> claim());

        refresh();
    }

    private boolean claimedToday() {
        long last = save.getLastDailyClaimMillis();
        if (last <= 0) return false;
        Calendar now = Calendar.getInstance();
        Calendar then = Calendar.getInstance();
        then.setTimeInMillis(last);
        return now.get(Calendar.YEAR) == then.get(Calendar.YEAR)
                && now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR);
    }

    private void refresh() {
        ((TextView) findViewById(R.id.text_streak))
                .setText(getString(R.string.label_streak, save.getDailyStreak()));
        boolean claimed = claimedToday();
        buttonClaim.setEnabled(!claimed);
        buttonClaim.setText(claimed ? R.string.action_claimed_today : R.string.action_claim);
    }

    private void claim() {
        if (claimedToday()) return; // guard against double callbacks

        long now = System.currentTimeMillis();
        long last = save.getLastDailyClaimMillis();
        int streak = (last > 0 && now - last <= 2 * DAY_MS)
                ? save.getDailyStreak() + 1 : 1;

        AudioManager.get(this).playEffect(AudioManager.SFX_COIN);
        save.setDailyStreak(streak);
        save.setLastDailyClaimMillis(now);
        save.addCoins(REWARD_COINS);
        // Every 3rd consecutive day also grants a hammer booster.
        if (streak % 3 == 0) save.addBoosters(STREAK_BOOSTER, 1);

        Toast.makeText(this, R.string.toast_daily_claimed, Toast.LENGTH_SHORT).show();
        refresh();
    }
}
