package com.example.pizzacrash.ui;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pizzacrash.R;
import com.example.pizzacrash.audio.AudioManager;
import com.example.pizzacrash.data.SaveRepository;
import com.google.android.material.materialswitch.MaterialSwitch;

/**
 * Settings screen (UI_Refs/settings.png): sound effects, music and haptics
 * toggles backed directly by SaveRepository so they persist across restarts.
 * The toggles actively gate the AudioManager feedback paths (every effect /
 * haptic call re-checks these preferences), and changing Music stops any
 * currently playing track immediately.
 */
public class SettingsActivity extends AppCompatActivity {

    private SaveRepository save;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        save = new SaveRepository(this);
        final AudioManager audio = AudioManager.get(this);

        findViewById(R.id.button_back).setOnClickListener(v -> {
            audio.playEffect(AudioManager.SFX_TAP);
            finish();
        });

        bindToggle(R.id.switch_sound, save.isSoundEnabled(), enabled -> {
            save.setSoundEnabled(enabled);
            if (enabled) audio.playEffect(AudioManager.SFX_TAP);
        });
        bindToggle(R.id.switch_music, save.isMusicEnabled(), enabled -> {
            save.setMusicEnabled(enabled);
            audio.onMusicPreferenceChanged(enabled);
        });
        bindToggle(R.id.switch_haptics, save.isHapticsEnabled(), enabled -> {
            save.setHapticsEnabled(enabled);
            if (enabled) audio.vibrateLong(this);
        });

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
