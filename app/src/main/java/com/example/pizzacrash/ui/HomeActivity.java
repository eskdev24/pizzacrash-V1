package com.example.pizzacrash.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pizzacrash.R;
import com.example.pizzacrash.data.SaveRepository;

/**
 * Main home screen, implemented from UI_Refs/home.png.
 * Wired interactions:
 *  - PLAY  -> world map of the current unlocked world
 *  - gear / bag / gift -> stubs (Settings, Shop, Daily reward screens arrive
 *    with their own approved references; they toast for now so nothing is fake-clickable silence).
 * Shows the live coin balance from the save system.
 */
public class HomeActivity extends AppCompatActivity {

    private SaveRepository save;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        save = new SaveRepository(this);

        findViewById(R.id.button_play).setOnClickListener(v -> openWorldMap());
        findViewById(R.id.button_settings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.button_shop).setOnClickListener(v ->
                startActivity(new Intent(this, ShopActivity.class)));
        findViewById(R.id.button_daily).setOnClickListener(v ->
                startActivity(new Intent(this, DailyRewardActivity.class)));
        findViewById(R.id.button_profile).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCoins();
    }

    private void refreshCoins() {
        ((TextView) findViewById(R.id.text_coins)).setText(String.valueOf(save.getCoins()));
    }

    private void openWorldMap() {
        View play = findViewById(R.id.button_play);
        play.animate().scaleX(0.94f).scaleY(0.94f).setDuration(70)
                .withEndAction(() -> play.animate().scaleX(1f).scaleY(1f).setDuration(110).start())
                .start();
        Intent i = new Intent(this, WorldMapActivity.class);
        i.putExtra(WorldMapActivity.EXTRA_WORLD, SaveRepository.worldOf(save.getUnlockedLevel()));
        startActivity(i);
    }

}
