package de.rwth_aachen.phyphox.Helper;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    static final String KEY_LOGIN =
            "login",
            KEY_ID = "id",
            KEY_NAME = "name",
            KEY_EMAIL = "email",
            KEY_VISITING_INTO = "totalVisitingIntroduction",
            KEY_VISITING_DURATION = "totalVisitDurationMillis";

    public static SharedPreferences getSharedPreference(Context context) {
        return context.getSharedPreferences(
                "zona", Context.MODE_PRIVATE);
    }

    public static void setCustData(Context context, String name, String custId, String email, Long totalVisiting ,Long durationTotalVisiting) {
        SharedPreferences.Editor editor = getSharedPreference(context).edit();
        editor.putBoolean(KEY_LOGIN, true);
        editor.putString(KEY_ID, custId);
        editor.putString(KEY_NAME, name);
        editor.putString(KEY_EMAIL, email);
        editor.putLong(KEY_VISITING_INTO, totalVisiting);
        editor.putLong(KEY_VISITING_DURATION, durationTotalVisiting);
        editor.apply();
    }
    public static void setKeyVisitingInto(Context context, Long totalVisiting) {
        SharedPreferences.Editor editor = getSharedPreference(context).edit();
        editor.putLong(KEY_VISITING_INTO, totalVisiting);
        editor.apply();
    }


    public static String getId(Context context) {
        return getSharedPreference(context).getString(KEY_ID, "");
    }



    public static String getName(Context context) {
        return getSharedPreference(context).getString(KEY_NAME, "");
    }
    public static String getEmail(Context context) {
        return getSharedPreference(context).getString(KEY_EMAIL, "");
    }
    public static Integer getKeyVisitingInto(Context context) {
        return getSharedPreference(context).getInt(KEY_VISITING_INTO,0);
    }

    public static Boolean getIsLogin(Context context) {
        return getSharedPreference(context).getBoolean(KEY_LOGIN, false);
    }

    public static void clearData(Context context) {
        getSharedPreference(context).edit().clear().apply();
    }
    public static void setTotalVisitDuration(Context context, long durationMillis) {
        SharedPreferences.Editor editor = getSharedPreference(context).edit();
        editor.putLong(KEY_VISITING_DURATION, durationMillis);
        editor.apply();
    }

    public static long getTotalVisitDuration(Context context) {
        return getSharedPreference(context).getLong(KEY_VISITING_DURATION, 0);
    }

}
