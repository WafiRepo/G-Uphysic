package de.rwth_aachen.phyphox.Helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class PiAnalysisSettings {
    private static final String TAG = "PiAnalysisSettings";
    private static final String PREF_NAME = "pi_analysis_prefs";
    private static final String KEY_SERVER_URL = "server_url";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_MIGRATED = "migrated_v1";

    private static final String DEFAULT_SERVER_URL = "http://140.115.126.111:8082";
    private static final String DEFAULT_API_KEY = "changeme-random-secret-key-12345";

    private static SharedPreferences getPrefs(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        migrateIfNeeded(prefs);
        return prefs;
    }

    private static void migrateIfNeeded(SharedPreferences prefs) {
        if (prefs.getBoolean(KEY_MIGRATED, false)) return;

        String url = prefs.getString(KEY_SERVER_URL, null);
        if (url != null) {
            Log.d(TAG, "Migrating old server URL: " + url);
            prefs.edit().remove(KEY_SERVER_URL).apply();
        }
        String key = prefs.getString(KEY_API_KEY, null);
        if (key != null) {
            Log.d(TAG, "Migrating old API key");
            prefs.edit().remove(KEY_API_KEY).apply();
        }

        prefs.edit().putBoolean(KEY_MIGRATED, true).apply();
        Log.d(TAG, "Migration complete — defaults will apply on next get");
    }

    public static void setServerUrl(Context context, String url) {
        getPrefs(context).edit().putString(KEY_SERVER_URL, url).apply();
    }

    public static String getServerUrl(Context context) {
        String stored = getPrefs(context).getString(KEY_SERVER_URL, null);
        if (stored == null) return DEFAULT_SERVER_URL;
        return stored;
    }

    public static void setApiKey(Context context, String key) {
        getPrefs(context).edit().putString(KEY_API_KEY, key).apply();
    }

    public static String getApiKey(Context context) {
        String stored = getPrefs(context).getString(KEY_API_KEY, null);
        if (stored == null) return DEFAULT_API_KEY;
        return stored;
    }
}
