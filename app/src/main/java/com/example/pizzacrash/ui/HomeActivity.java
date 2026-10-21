package com.example.pizzacrash.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pizzacrash.R;
import com.example.pizzacrash.data.SaveRepository;

/**
 * Main home screen scaffold. Final visual design will follow the supplied
 * UI reference images. Currently wires: Play/Continue -> WorldMap, and shows
 * the live coin balance from the save system.
 */
public class HomeActivity extends AppCompatActivity {

    private SaveRepository save;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        save = new SaveRepository(this);

        TextView coins = findViewById(R.id.text_coins);
        coins.setText(String.valueOf(save.getCoins()));

        findViewById(R.id.button_play).setOnClickListener(v -> openWorldMap());
        findViewById(R.id.button_levels).setOnClickListener(v -> openWorldMap());
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((TextView) findViewById(R.id.text_coins)).setText(String.valueOf(save.getCoins()));
    }

    private void openWorldMap() {
        Intent i = new Intent(this, WorldMapActivity.class);
        i.putExtra(WorldMapActivity.EXTRA_WORLD, SaveRepository.worldOf(save.getUnlockedLevel()));
        startActivity(i);
    }
}
