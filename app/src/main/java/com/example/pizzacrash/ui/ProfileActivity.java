package com.example.pizzacrash.ui;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pizzacrash.R;
import com.example.pizzacrash.data.GameDatabase;
import com.example.pizzacrash.data.SaveRepository;

import java.text.DateFormat;
import java.util.Date;

/**
 * User profile screen. Shows the chef display name (editable, stored in SQLite)
 * and live progression stats (total stars, levels completed, coins, current world,
 * member-since date) read from the save system.
 */
public class ProfileActivity extends AppCompatActivity {

    private SaveRepository save;
    private GameDatabase db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        save = new SaveRepository(this);
        db = save.getDatabase();

        EditText nameInput = findViewById(R.id.input_display_name);
        nameInput.setText(db.getDisplayName());

        findViewById(R.id.button_save_name).setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, R.string.profile_name_hint, Toast.LENGTH_SHORT).show();
                return;
            }
            db.setDisplayName(name);
            ((TextView) findViewById(R.id.text_profile_name)).setText(name);
            Toast.makeText(this, R.string.profile_name_saved, Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.button_back).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStats();
    }

    /** DB reads are small indexed queries; safe for main thread at this scale. */
    private void refreshStats() {
        ((TextView) findViewById(R.id.text_profile_name)).setText(db.getDisplayName());
        ((TextView) findViewById(R.id.text_total_stars))
                .setText(String.valueOf(save.getTotalStars()));
        ((TextView) findViewById(R.id.text_levels_completed))
                .setText(String.valueOf(db.getCompletedLevelCount()));
        ((TextView) findViewById(R.id.text_coins))
                .setText(String.valueOf(save.getCoins()));
        int world = SaveRepository.worldOf(save.getUnlockedLevel());
        int worldNameRes = getResources().getIdentifier("world_" + world, "string", getPackageName());
        ((TextView) findViewById(R.id.text_current_world)).setText(
                worldNameRes != 0 ? getString(worldNameRes) : getString(R.string.map_world_fallback, world));
        long created = db.getCreatedMillis();
        ((TextView) findViewById(R.id.text_member_since)).setText(
                created > 0
                        ? getString(R.string.profile_since_prefix,
                                DateFormat.getDateInstance().format(new Date(created)))
                        : "");
    }
}
