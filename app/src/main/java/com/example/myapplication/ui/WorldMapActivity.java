package com.example.myapplication.ui;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.data.SaveRepository;

/**
 * Reusable world-map scaffold driven by configuration + progression data.
 * One implementation serves all 10 worlds; only world id/name/theme changes.
 * Level nodes with locked/uncompleted states will be implemented when the
 * world-map UI reference is approved.
 */
public class WorldMapActivity extends AppCompatActivity {

    public static final String EXTRA_WORLD = "world";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_world_map);

        int world = Math.max(1, Math.min(SaveRepository.TOTAL_WORLDS,
                getIntent().getIntExtra(EXTRA_WORLD, 1)));

        SaveRepository save = new SaveRepository(this);

        TextView title = findViewById(R.id.text_world_title);
        title.setText(getString(worldNameRes(world)));

        TextView subtitle = findViewById(R.id.text_world_subtitle);
        subtitle.setText(getString(R.string.label_world, world)
                + " · " + getString(R.string.label_level,
                SaveRepository.localLevelOf(save.getUnlockedLevel())));

        findViewById(R.id.button_back).setOnClickListener(v -> finish());
    }

    /** Resolves world_1..world_10 string resources from the world number. */
    private static int worldNameRes(int world) {
        switch (world) {
            case 2:  return R.string.world_2;
            case 3:  return R.string.world_3;
            case 4:  return R.string.world_4;
            case 5:  return R.string.world_5;
            case 6:  return R.string.world_6;
            case 7:  return R.string.world_7;
            case 8:  return R.string.world_8;
            case 9:  return R.string.world_9;
            case 10: return R.string.world_10;
            default: return R.string.world_1;
        }
    }
}
