package com.home.peppagames;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

/** Parent settings plus the cached game list. */
final class Prefs {
    static final String PEPPA_ONLY = "peppaOnly";
    static final String FOCUS = "focus";
    static final String BLOCK_ADS = "blockAds";
    static final String DESKTOP = "desktop";
    static final String[] SWITCHES = {PEPPA_ONLY, FOCUS, BLOCK_ADS, DESKTOP};

    private static final String GAMES = "games";
    private static final String GAMES_AT = "gamesAt";

    private final SharedPreferences sp;

    Prefs(Context c) {
        sp = c.getSharedPreferences("peppa", Context.MODE_PRIVATE);
    }

    boolean get(String key) {
        return sp.getBoolean(key, !DESKTOP.equals(key));
    }

    void set(String key, boolean value) {
        sp.edit().putBoolean(key, value).apply();
    }

    JSONObject switches() {
        JSONObject o = new JSONObject();
        try {
            for (String k : SWITCHES) o.put(k, get(k));
        } catch (JSONException ignored) {
        }
        return o;
    }

    String games() {
        return sp.getString(GAMES, "[]");
    }

    long gamesAgeMillis() {
        return System.currentTimeMillis() - sp.getLong(GAMES_AT, 0);
    }

    void saveGames(String json) {
        sp.edit().putString(GAMES, json).putLong(GAMES_AT, System.currentTimeMillis()).apply();
    }
}
