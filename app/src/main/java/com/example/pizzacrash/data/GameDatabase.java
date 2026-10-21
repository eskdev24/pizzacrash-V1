package com.example.pizzacrash.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * SQLite storage for PizzaCrash persistent game data.
 *
 * Tables:
 *  - profile        : single-row user profile (name, avatar id, created timestamp)
 *  - level_progress : one row per completed/attempted level (stars, best score)
 *  - meta           : key/value store for coins, unlocked level, daily reward state, settings
 *
 * This database is the durable source of truth for progression. SaveRepository
 * (SharedPreferences) acts as a fast in-memory cache and mirrors writes here.
 */
public final class GameDatabase extends SQLiteOpenHelper {

    private static final String DB_NAME = "pizzacrash_game.db";
    private static final int DB_VERSION = 1;

    public static final String T_PROFILE = "profile";
    public static final String T_PROGRESS = "level_progress";
    public static final String T_META = "meta";

    private static final String SQL_CREATE_PROFILE =
            "CREATE TABLE " + T_PROFILE + " (" +
                    "_id INTEGER PRIMARY KEY CHECK (_id = 1), " +
                    "display_name TEXT NOT NULL DEFAULT 'Chef', " +
                    "avatar_id INTEGER NOT NULL DEFAULT 0, " +
                    "created_at INTEGER NOT NULL)";

    private static final String SQL_CREATE_PROGRESS =
            "CREATE TABLE " + T_PROGRESS + " (" +
                    "level_id INTEGER PRIMARY KEY, " +
                    "stars INTEGER NOT NULL DEFAULT 0, " +
                    "best_score INTEGER NOT NULL DEFAULT 0, " +
                    "completed_at INTEGER NOT NULL DEFAULT 0)";

    private static final String SQL_CREATE_META =
            "CREATE TABLE " + T_META + " (" +
                    "key TEXT PRIMARY KEY, " +
                    "value TEXT NOT NULL)";

    public GameDatabase(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_PROFILE);
        db.execSQL(SQL_CREATE_PROGRESS);
        db.execSQL(SQL_CREATE_META);
        // Ensure a profile row exists from first launch.
        ContentValues cv = new ContentValues();
        cv.put("display_name", "Chef");
        cv.put("avatar_id", 0);
        cv.put("created_at", System.currentTimeMillis());
        db.insertWithOnConflict(T_PROFILE, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Future schema migrations go here; v1 is initial.
    }

    // ------------------------------------------------------------------
    // Profile
    // ------------------------------------------------------------------

    /** Returns display name; creates default profile if missing. */
    public String getDisplayName() {
        try (Cursor c = getReadableDatabase().query(T_PROFILE,
                new String[]{"display_name"}, null, null, null, null, null)) {
            if (c.moveToFirst()) return c.getString(0);
        }
        setDisplayName("Chef");
        return "Chef";
    }

    public void setDisplayName(String name) {
        ContentValues cv = new ContentValues();
        cv.put("display_name", name == null || name.trim().isEmpty() ? "Chef" : name.trim());
        cv.put("created_at", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict(T_PROFILE, null, cv,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public int getAvatarId() {
        try (Cursor c = getReadableDatabase().query(T_PROFILE,
                new String[]{"avatar_id"}, null, null, null, null, null)) {
            if (c.moveToFirst()) return c.getInt(0);
        }
        return 0;
    }

    public void setAvatarId(int avatarId) {
        ContentValues cv = new ContentValues();
        cv.put("avatar_id", avatarId);
        getWritableDatabase().update(T_PROFILE, cv, null, null);
    }

    public long getCreatedMillis() {
        try (Cursor c = getReadableDatabase().query(T_PROFILE,
                new String[]{"created_at"}, null, null, null, null, null)) {
            if (c.moveToFirst()) return c.getLong(0);
        }
        return 0L;
    }

    // ------------------------------------------------------------------
    // Level progress
    // ------------------------------------------------------------------

    /**
     * Records a level result. Keeps the highest stars and best score only.
     * Runs inside a transaction so repeated callbacks can never downgrade data.
     */
    public void recordResult(int levelId, int stars, int score) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            int oldStars = 0, oldBest = 0;
            try (Cursor c = db.query(T_PROGRESS, new String[]{"stars", "best_score"},
                    "level_id=?", new String[]{String.valueOf(levelId)}, null, null, null)) {
                if (c.moveToFirst()) {
                    oldStars = c.getInt(0);
                    oldBest = c.getInt(1);
                }
            }
            ContentValues cv = new ContentValues();
            cv.put("level_id", levelId);
            cv.put("stars", Math.max(oldStars, stars));
            cv.put("best_score", Math.max(oldBest, score));
            cv.put("completed_at", System.currentTimeMillis());
            db.insertWithOnConflict(T_PROGRESS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /** Returns {stars, bestScore} for a level, or null if never played. */
    public int[] getProgress(int levelId) {
        try (Cursor c = getReadableDatabase().query(T_PROGRESS,
                new String[]{"stars", "best_score"}, "level_id=?",
                new String[]{String.valueOf(levelId)}, null, null, null)) {
            if (c.moveToFirst()) return new int[]{c.getInt(0), c.getInt(1)};
        }
        return null;
    }

    /** Sum of stars across all recorded levels. */
    public int getTotalStars() {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(stars),0) FROM " + T_PROGRESS, null)) {
            if (c.moveToFirst()) return c.getInt(0);
        }
        return 0;
    }

    public int getCompletedLevelCount() {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM " + T_PROGRESS + " WHERE stars > 0", null)) {
            if (c.moveToFirst()) return c.getInt(0);
        }
        return 0;
    }

    // ------------------------------------------------------------------
    // Meta key/value (coins, unlocks, daily state, settings)
    // ------------------------------------------------------------------

    public void putMeta(String key, String value) {
        ContentValues cv = new ContentValues();
        cv.put("key", key);
        cv.put("value", value == null ? "" : value);
        getWritableDatabase().insertWithOnConflict(T_META, null, cv,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public String getMeta(String key, String def) {
        try (Cursor c = getReadableDatabase().query(T_META, new String[]{"value"},
                "key=?", new String[]{key}, null, null, null)) {
            if (c.moveToFirst()) return c.getString(0);
        }
        return def;
    }

    public void putMetaInt(String key, long value) {
        putMeta(key, String.valueOf(value));
    }

    public long getMetaLong(String key, long def) {
        String v = getMeta(key, null);
        if (v == null) return def;
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
