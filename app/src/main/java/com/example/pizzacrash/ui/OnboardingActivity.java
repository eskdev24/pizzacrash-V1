package com.example.pizzacrash.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.pizzacrash.R;
import com.example.pizzacrash.audio.AudioManager;
import com.example.pizzacrash.data.SaveRepository;
import com.google.android.material.button.MaterialButton;

/**
 * Three-page onboarding flow (UI_Refs/onboard1-3.png): swipeable intro pages
 * teaching matching, objectives and boosters, with Skip / Next controls.
 * Completion is persisted in SaveRepository so returning users go straight
 * to Home (routing handled by SplashActivity).
 */
public class OnboardingActivity extends AppCompatActivity {

    private static final int PAGE_COUNT = 3;

    private ViewPager2 pager;
    private LinearLayout dotsContainer;
    private MaterialButton buttonNext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        pager = findViewById(R.id.onboard_pager);
        dotsContainer = findViewById(R.id.dots_container);
        buttonNext = findViewById(R.id.button_next);
        MaterialButton buttonSkip = findViewById(R.id.button_skip);

        pager.setAdapter(new PageAdapter());
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateDots(position);
                buttonNext.setText(position == PAGE_COUNT - 1
                        ? R.string.action_get_playing : R.string.action_next);
                findViewById(R.id.onboard_root)
                        .setBackgroundColor(pageBackground(position));
            }
        });

        buildDots();
        updateDots(0);
        findViewById(R.id.onboard_root).setBackgroundColor(pageBackground(0));

        buttonSkip.setOnClickListener(v -> finishFlow());
        buttonNext.setOnClickListener(v -> {
            AudioManager.get(this).hapticTap(v);
            if (pager.getCurrentItem() < PAGE_COUNT - 1) {
                pager.setCurrentItem(pager.getCurrentItem() + 1, true);
            } else {
                finishFlow();
            }
        });
    }

    private void finishFlow() {
        new SaveRepository(this).setOnboardingCompleted(true);
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    /** Soft pastel background per page, consistent with the brand palette. */
    private int pageBackground(int position) {
        switch (position) {
            case 1:  return Color.parseColor("#FFF3E0");
            case 2:  return Color.parseColor("#FCE4EC");
            default: return Color.parseColor("#FFE9C7"); // board cream token
        }
    }

    private void buildDots() {
        dotsContainer.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < PAGE_COUNT; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    Math.round(12 * density), Math.round(12 * density));
            lp.setMargins(Math.round(6 * density), 0, Math.round(6 * density), 0);
            dot.setLayoutParams(lp);
            dot.setBackgroundResource(R.drawable.dot_onboard_page);
            dotsContainer.addView(dot);
        }
    }

    private void updateDots(int selected) {
        for (int i = 0; i < dotsContainer.getChildCount(); i++) {
            dotsContainer.getChildAt(i).setSelected(i == selected);
        }
    }

    /** Static content model for the three approved pages. */
    private static final int[] TITLES = {
            R.string.onboard_1_title, R.string.onboard_2_title, R.string.onboard_3_title };
    private static final int[] BODIES = {
            R.string.onboard_1_body, R.string.onboard_2_body, R.string.onboard_3_body };
    private static final int[] ILLUS = {
            R.drawable.ill_onboard_match, R.drawable.ill_onboard_objectives,
            R.drawable.ill_onboard_boosters };

    private static class PageAdapter
            extends RecyclerView.Adapter<PageAdapter.PageHolder> {

        @NonNull
        @Override
        public PageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_onboard_page, parent, false);
            return new PageHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull PageHolder h, int position) {
            h.image.setImageResource(ILLUS[position]);
            h.title.setText(TITLES[position]);
            h.body.setText(BODIES[position]);
        }

        @Override
        public int getItemCount() {
            return PAGE_COUNT;
        }

        static class PageHolder extends RecyclerView.ViewHolder {
            final ImageView image;
            final TextView title;
            final TextView body;

            PageHolder(@NonNull View itemView) {
                super(itemView);
                image = itemView.findViewById(R.id.image_illustration);
                title = itemView.findViewById(R.id.text_title);
                body = itemView.findViewById(R.id.text_body);
            }
        }
    }
}
