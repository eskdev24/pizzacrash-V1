package com.example.pizzacrash.ui;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.example.pizzacrash.R;

/**
 * Pause overlay (UI_Refs/pause.png) implemented as a content-view layer instead of
 * a Dialog, so it survives rotation-free Activities cleanly and blocks board input.
 * Resume hides it; Restart relaunches GameActivity fresh; Quit returns to Home.
 */
public final class DialogPauseController {

    public interface Callback { void run(); }

    private final Activity activity;
    private FrameLayout overlay;

    public DialogPauseController(Activity activity) {
        this.activity = activity;
    }

    public void show(Callback onResume, Callback onQuit) {
        if (overlay == null) {
            overlay = (FrameLayout) LayoutInflater.from(activity)
                    .inflate(R.layout.dialog_pause, null);
        }
        View root = (View) overlay.getParent();
        if (root != null) ((android.view.ViewGroup) root).removeView(overlay);
        activity.addContentView(overlay,
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
        overlay.bringToFront();

        overlay.findViewById(R.id.btn_resume).setOnClickListener(v -> hide());
        overlay.findViewById(R.id.btn_restart).setOnClickListener(v -> {
            Intent i = activity.getIntent();
            activity.finish();
            activity.startActivity(i);
        });
        overlay.findViewById(R.id.btn_quit).setOnClickListener(v -> {
            hide();
            onQuit.run();
        });
        onResume.run(); // hook for callers that need "resumed" bookkeeping on show
    }

    /** True if the overlay is currently visible (used to auto-pause on background). */
    public boolean isShowing() {
        return overlay != null && overlay.getParent() != null;
    }

    public void hide() {
        if (overlay != null && overlay.getParent() != null) {
            ((android.view.ViewGroup) overlay.getParent()).removeView(overlay);
        }
    }
}
