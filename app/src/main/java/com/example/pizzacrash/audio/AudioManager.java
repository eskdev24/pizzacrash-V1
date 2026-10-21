package com.example.pizzacrash.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.View;

import com.example.pizzacrash.data.SaveRepository;

/**
 * Lightweight native audio + haptics manager (SoundPool for SFX, MediaPlayer
 * for music, Vibrator/HapticFeedback for touch response).
 *
 * No approved audio asset files exist in the repo yet, so sound slots resolve
 * to "not registered" and playback is intentionally silent. What matters now
 * (per the outstanding-issues list) is that the Settings toggles genuinely
 * gate every feedback path: each call re-reads the persisted preferences
 * before touching any system API, and haptics work today with no assets.
 * When approved sounds are added to res/raw they can be registered in
 * {@link #load(Context)} without changing any call sites.
 */
public final class AudioManager {

    public static final int SFX_TAP = 0;
    public static final int SFX_MATCH = 1;
    public static final int SFX_SWAP = 2;
    public static final int SFX_INVALID = 3;
    public static final int SFX_WIN = 4;
    public static final int SFX_LOSE = 5;
    public static final int SFX_COIN = 6;
    public static final int SFX_BOOSTER = 7;

    private static volatile AudioManager instance;

    private final SaveRepository save;
    private SoundPool soundPool;
    private MediaPlayer musicPlayer;

    /** SoundPool sound ids per effect; 0 means "asset not registered yet". */
    private final int[] soundIds = new int[8];

    private AudioManager(Context context) {
        save = new SaveRepository(context);
        load(context.getApplicationContext());
    }

    public static AudioManager get(Context context) {
        if (instance == null) {
            synchronized (AudioManager.class) {
                if (instance == null) {
                    instance = new AudioManager(context);
                }
            }
        }
        return instance;
    }

    private void load(Context app) {
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder().setMaxStreams(6).setAudioAttributes(attrs).build();

        // Register effects here once approved assets exist, e.g.:
        // soundIds[SFX_TAP] = soundPool.load(app, R.raw.tap, 1);
    }

    /** Plays an effect only when the user's Sound preference is enabled. */
    public void playEffect(int effectId) {
        if (!save.isSoundEnabled()) return;
        if (effectId < 0 || effectId >= soundIds.length) return;
        int id = soundIds[effectId];
        if (id != 0 && soundPool != null) {
            soundPool.play(id, 1f, 1f, 1, 0, 1f);
        }
        // No asset registered: intentionally silent until audio art is approved.
    }

    /**
     * Starts looping background music from a raw resource if the Music
     * preference is enabled. Safe no-op while no track is approved.
     */
    public void startMusic(Context context, int rawResId) {
        stopMusic();
        if (!save.isMusicEnabled() || rawResId == 0) return;
        musicPlayer = MediaPlayer.create(context.getApplicationContext(), rawResId);
        if (musicPlayer != null) {
            musicPlayer.setLooping(true);
            musicPlayer.start();
        }
    }

    public void stopMusic() {
        if (musicPlayer != null) {
            musicPlayer.release();
            musicPlayer = null;
        }
    }

    /** Call from Settings when the Music toggle changes. */
    public void onMusicPreferenceChanged(boolean enabled) {
        if (!enabled) stopMusic();
    }

    /** View-level haptic tick, gated by the Vibration preference. */
    public void hapticTap(View view) {
        if (!save.isHapticsEnabled() || view == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }
    }

    /** Stronger vibration for events like level win/lose, pref-gated. */
    public void vibrateLong(Context context) {
        if (!save.isHapticsEnabled()) return;
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (v == null || !v.hasVibrator()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            // Deprecated but required for minSdk 24-25 devices.
            v.vibrate(120);
        }
    }

    public void release() {
        stopMusic();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}
