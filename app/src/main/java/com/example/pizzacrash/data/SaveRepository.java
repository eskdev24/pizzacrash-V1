package com.example.pizzacrash.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Local save data for PizzaCrash, backed by SharedPreferences (no new dependencies).
 *
 * Stores player progression: unlocked level, per-level stars & best scores, coins,
 * booster inventory counts, and audio/haptics preferences.
 *
 * Level ids are global (1..200). World w contains levels (w-1)*20+1 .. w*20.
 */
public final class SaveRepository {

    private static final String PREFS_NAME = "pizzacrash_save";
    private static final String KEY_VERSION = "save_version";
    private static final String KEY_UNLOCKED_LEVEL = "unlocked_level";
    private static final String KEY_COINS = "coins";
    private static final String KEY_SOUND = "sound_enabled";
    private static final String KEY_MUSIC = "music_enabled";
    private static final String KEY_HAPTICS = "haptics_enabled";
    private static final String KEY_STARS_PREFIX = "stars_";      // + levelId
    private static final String KEY_BEST_PREFIX = "best_";        // + levelId
    private static final String KEY_BOOSTER_PREFIX = "booster_";  // + boosterId
    private static final String KEY_DAILY_LAST_CLAIM = "daily_last_claim";
    private static final String KEY_DAILY_STREAK = "daily_streak";
    private static final String KEY_ONBOARDING_DONE = "onboarding_completed";

    /** Current save schema version; bump when migrating. */
    private static final int SAVE_VERSION = 1;

    /** Default starting economy for a fresh install. */
    private static final int STARTING_COINS = 100;

    public static final int TOTAL_LEVELS = 200;
    public static final int LEVELS_PER_WORLD = 20;
    public static final int TOTAL_WORLDS = 10;

    private final SharedPreferences prefs;
    /** SQLite is the durable source of truth; prefs act as a fast cache mirror. */
    private final GameDatabase db;

    public SaveRepository(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.db = new GameDatabase(context);
        if (prefs.getInt(KEY_VERSION, -1) != SAVE_VERSION) {
            initFreshSave();
        }
        migrateLegacyProgressToDb();
    }

    /** Direct access to the SQLite layer (profile fields not cached in prefs). */
    public GameDatabase getDatabase() {
        return db;
    }

    private void initFreshSave() {
        prefs.edit()
                .putInt(KEY_VERSION, SAVE_VERSION)
                .putInt(KEY_UNLOCKED_LEVEL, 1)   // World 1, Level 1 unlocked at install
                .putInt(KEY_COINS, STARTING_COINS)
                .putBoolean(KEY_SOUND, true)
                .putBoolean(KEY_MUSIC, true)
                .putBoolean(KEY_HAPTICS, true)
                .apply();
    }

    /**
     * One-time backfill: copies any pre-SQLite SharedPreferences progress into the
     * database. Safe to run repeatedly — DB keeps only best values and the meta
     * key "legacy_migrated" prevents re-copying after coins/settings diverge.
     */
    private void migrateLegacyProgressToDb() {
        if ("1".equals(db.getMeta("legacy_migrated", null))) return;
        for (int levelId = 1; levelId <= TOTAL_LEVELS; levelId++) {
            int stars = prefs.getInt(KEY_STARS_PREFIX + levelId, 0);
            int best = prefs.getInt(KEY_BEST_PREFIX + levelId, 0);
            if (stars > 0 || best > 0) {
                db.recordResult(levelId, stars, best);
            }
        }
        db.putMetaInt("coins", getCoins());
        db.putMetaInt("unlocked_level", getUnlockedLevel());
        db.putMeta("sound_enabled", String.valueOf(isSoundEnabled()));
        db.putMeta("music_enabled", String.valueOf(isMusicEnabled()));
        db.putMeta("haptics_enabled", String.valueOf(isHapticsEnabled()));
        db.putMetaInt("daily_last_claim", getLastDailyClaimMillis());
        db.putMetaInt("daily_streak", getDailyStreak());
        db.putMetaInt("onboarding_completed", isOnboardingCompleted() ? 1 : 0);
        db.putMeta("legacy_migrated", "1");
    }

    // ---------- Onboarding ----------

    public boolean isOnboardingCompleted() {
        return prefs.getBoolean(KEY_ONBOARDING_DONE, false);
    }

    public void setOnboardingCompleted(boolean done) {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, done).apply();
        db.putMetaInt("onboarding_completed", done ? 1 : 0);
    }

    // ---------- Progression ----------

    /** Highest level the player may start (1..TOTAL_LEVELS). */
    public int getUnlockedLevel() {
        return Math.max(1, Math.min(TOTAL_LEVELS, prefs.getInt(KEY_UNLOCKED_LEVEL, 1)));
    }

    /** Unlocks {@code levelId} if it is beyond the current frontier (clamped to 200). */
    public void unlockLevel(int levelId) {
        int clamped = Math.max(1, Math.min(TOTAL_LEVELS, levelId));
        if (clamped > getUnlockedLevel()) {
            prefs.edit().putInt(KEY_UNLOCKED_LEVEL, clamped).apply();
            db.putMetaInt("unlocked_level", clamped);
        }
    }

    public boolean isLevelUnlocked(int levelId) {
        return levelId <= getUnlockedLevel();
    }

    public static int worldOf(int levelId) {
        return (levelId - 1) / LEVELS_PER_WORLD + 1;
    }

    public static int localLevelOf(int levelId) {
        return (levelId - 1) % LEVELS_PER_WORLD + 1;
    }

    public static int globalLevelId(int world, int localLevel) {
        return (world - 1) * LEVELS_PER_WORLD + localLevel;
    }

    // ---------- Per-level results ----------

    /** 0 = not completed yet; otherwise 1..3. */
    public int getStars(int levelId) {
        return prefs.getInt(KEY_STARS_PREFIX + levelId, 0);
    }

    public int getBestScore(int levelId) {
        return prefs.getInt(KEY_BEST_PREFIX + levelId, 0);
    }

    /**
     * Records a completed level. Only keeps the BEST result (replaying with a lower
     * score never lowers stars/best). Returns whether the record improved, so callers
     * can avoid awarding duplicate rewards.
     */
    public boolean recordResult(int levelId, int stars, int score) {
        boolean improved = false;
        SharedPreferences.Editor editor = prefs.edit();
        if (stars > getStars(levelId)) {
            editor.putInt(KEY_STARS_PREFIX + levelId, stars);
            improved = true;
        }
        if (score > getBestScore(levelId)) {
            editor.putInt(KEY_BEST_PREFIX + levelId, score);
            improved = true;
        }
        editor.apply();
        // Durable copy in SQLite (DB itself keeps only best values).
        db.recordResult(levelId, Math.max(getStars(levelId), stars),
                Math.max(getBestScore(levelId), score));
        return improved;
    }

    /** Total stars earned across all 200 levels. */
    public int getTotalStars() {
        int total = 0;
        for (int i = 1; i <= TOTAL_LEVELS; i++) {
            total += getStars(i);
        }
        return total;
    }

    // ---------- Economy ----------

    public int getCoins() {
        return prefs.getInt(KEY_COINS, 0);
    }

    public void addCoins(int amount) {
        if (amount <= 0) return;
        int balance = getCoins() + amount;
        prefs.edit().putInt(KEY_COINS, balance).apply();
        db.putMetaInt("coins", balance);
    }

    /** @return true if deducted; false if balance was insufficient (no change made). */
    public boolean spendCoins(int amount) {
        if (amount < 0) return false;
        int balance = getCoins();
        if (balance < amount) return false;
        prefs.edit().putInt(KEY_COINS, balance - amount).apply();
        db.putMetaInt("coins", balance - amount);
        return true;
    }

    // ---------- Boosters ----------

    public int getBoosterCount(String boosterId) {
        return prefs.getInt(KEY_BOOSTER_PREFIX + boosterId, 0);
    }

    public void addBoosters(String boosterId, int amount) {
        if (amount <= 0) return;
        int total = getBoosterCount(boosterId) + amount;
        prefs.edit().putInt(KEY_BOOSTER_PREFIX + boosterId, total).apply();
        db.putMetaInt("booster_" + boosterId, total); // durable mirror
    }

    /** @return true if consumed; false if none available. */
    public boolean consumeBooster(String boosterId) {
        int count = getBoosterCount(boosterId);
        if (count <= 0) return false;
        prefs.edit().putInt(KEY_BOOSTER_PREFIX + boosterId, count - 1).apply();
        db.putMetaInt("booster_" + boosterId, count - 1); // durable mirror
        return true;
    }

    // ---------- Settings ----------

    public boolean isSoundEnabled() { return prefs.getBoolean(KEY_SOUND, true); }
    public boolean isMusicEnabled() { return prefs.getBoolean(KEY_MUSIC, true); }
    public boolean isHapticsEnabled() { return prefs.getBoolean(KEY_HAPTICS, true); }

    public void setSoundEnabled(boolean v) {
        prefs.edit().putBoolean(KEY_SOUND, v).apply();
        db.putMeta("sound_enabled", String.valueOf(v));
    }
    public void setMusicEnabled(boolean v) {
        prefs.edit().putBoolean(KEY_MUSIC, v).apply();
        db.putMeta("music_enabled", String.valueOf(v));
    }
    public void setHapticsEnabled(boolean v) {
        prefs.edit().putBoolean(KEY_HAPTICS, v).apply();
        db.putMeta("haptics_enabled", String.valueOf(v));
    }

    // ---------- Daily reward ----------

    /** Timestamp (millis) of the last daily-reward claim; 0 = never claimed. */
    public long getLastDailyClaimMillis() {
        return prefs.getLong(KEY_DAILY_LAST_CLAIM, 0L);
    }

    public void setLastDailyClaimMillis(long millis) {
        prefs.edit().putLong(KEY_DAILY_LAST_CLAIM, millis).apply();
        db.putMetaInt("daily_last_claim", millis);
    }

    /** Consecutive-day claim streak (>=1 once claimed at least once). */
    public int getDailyStreak() {
        return prefs.getInt(KEY_DAILY_STREAK, 0);
    }

    public void setDailyStreak(int streak) {
        int clamped = Math.max(0, streak);
        prefs.edit().putInt(KEY_DAILY_STREAK, clamped).apply();
        db.putMetaInt("daily_streak", clamped);
    }
}
