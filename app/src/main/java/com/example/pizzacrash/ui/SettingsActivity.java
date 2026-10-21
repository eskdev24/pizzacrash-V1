package com.example.pizzacrash.ui;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pizzacrash.R;
import com.example.pizzacrash.data.SaveRepository;
import com.google.android.material.materialswitch.MaterialSwitch;

/**
 * Settings screen (UI_Refs/settings.png): sound effects, music and haptics
 * toggles backed directly by SaveRepository so they persist across restarts
 * and will drive the audio/haptics systems once those are wired (Phase 8).
 */
public class SettingsActivity extends AppCompatActivity {

    private SaveRepository save;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        save = new SaveRepository(this);

        findViewById(R.id.button_back).setOnClickListener(v -> finish());

        bindToggle(R.id.switch_sound, save.isSoundEnabled(), save::setSoundEnabled);
        bindToggle(R.id.switch_music, save.isMusicEnabled(), save::setMusicEnabled);
        bindToggle(R.id.switch_haptics, save.isHapticsEnabled(), save::setHapticsEnabled);

        ((TextView) findViewById(R.id.text_total_stars)).setText(
                String.valueOf(save.getTotalStars()));
        ((TextView) findViewById(R.id.text_coin_balance)).setText(
                String.valueOf(save.getCoins()));
    }

    private void bindToggle(int viewId, boolean initial, java.util.function.Consumer<Boolean> setter) {
        MaterialSwitch sw = findViewById(viewId);
        sw.setChecked(initial);
        sw.setOnCheckedChangeListener((buttonView, checked) -> setter.accept(checked));
    }
}
