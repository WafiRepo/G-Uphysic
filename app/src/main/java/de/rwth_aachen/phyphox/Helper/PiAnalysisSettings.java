package de.rwth_aachen.phyphox.Helper;

import android.content.Context;
import android.content.SharedPreferences;

public class PiAnalysisSettings {
    private static final String PREF_NAME = "pi_analysis_prefs";
    private static final String KEY_SERVER_URL = "server_url";
    private static final String KEY_API_KEY = "api_key";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void setServerUrl(Context context, String url) {
        getPrefs(context).edit().putString(KEY_SERVER_URL, url).apply();
    }

    public static String getServerUrl(Context context) {
        return getPrefs(context).getString(KEY_SERVER_URL, "");
    }

    public static void setApiKey(Context context, String key) {
        getPrefs(context).edit().putString(KEY_API_KEY, key).apply();
    }

    public static String getApiKey(Context context) {
        return getPrefs(context).getString(KEY_API_KEY, "");
    }
}
