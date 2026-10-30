package com.home.peppagames;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The game picker. A local page (assets/launcher.html) shows the tiles; behind it a second
 * WebView ("scout") loads the nick.com Peppa page and reads the list of games from it.
 */
public class MainActivity extends Activity {
    private static final long REFRESH_AFTER_MS = 24L * 60 * 60 * 1000;
    private static final long SCOUT_TIMEOUT_MS = 40_000;
    private static final long POLL_MS = 1_500;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private Prefs prefs;
    private WebView launcher;
    private WebView scout;

    private boolean scouting;
    private boolean scoutFailed;
    private boolean polling;
    private int lastCount;
    private int stablePolls;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = new Prefs(this);

        FrameLayout root = new FrameLayout(this);
        // The scout sits underneath the launcher at full size so the page lays out
        // and lazy-loads like it would on screen; the launcher covers it completely.
        scout = new WebView(this);
        root.addView(scout, new FrameLayout.LayoutParams(-1, -1));

        launcher = new WebView(this);
        launcher.setBackgroundColor(Color.parseColor("#8FD3FF"));
        Web.configure(launcher, prefs);
        launcher.getSettings().setAllowFileAccess(true);
        launcher.addJavascriptInterface(new Bridge(), "App");
        launcher.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                return true; // the launcher never navigates anywhere
            }
        });
        root.addView(launcher, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        Web.fullScreen(this);

        launcher.loadUrl("file:///android_asset/launcher.html");
        if ("[]".equals(prefs.games()) || prefs.gamesAgeMillis() > REFRESH_AFTER_MS) startScout();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) Web.fullScreen(this);
    }

    @Override
    protected void onDestroy() {
        ui.removeCallbacksAndMessages(null);
        scout.destroy();
        launcher.destroy();
        super.onDestroy();
    }

    // ---- finding games --------------------------------------------------------------

    private void startScout() {
        if (scouting) return;
        scouting = true;
        scoutFailed = false;
        polling = false;
        lastCount = -1;
        stablePolls = 0;
        Web.configure(scout, prefs);
        scout.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                return Web.maybeBlock(prefs, r);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                return r.isForMainFrame() && !Web.isNick(r.getUrl());
            }

            @Override
            public void onPageFinished(WebView v, String url) {
                if (scouting && !polling && Web.isNick(android.net.Uri.parse(url))) {
                    polling = true;
                    ui.postDelayed(poll, POLL_MS);
                }
            }
        });
        scout.loadUrl(Web.GAMES_PAGE);
        ui.postDelayed(timeout, SCOUT_TIMEOUT_MS);
        refreshLauncher();
    }

    private final Runnable poll = new Runnable() {
        @Override
        public void run() {
            if (!scouting) return;
            scout.evaluateJavascript(Scripts.FIND_GAMES, value -> {
                if (!scouting) return;
                JSONArray games = parse(value);
                int n = games.length();
                stablePolls = (n > 0 && n == lastCount) ? stablePolls + 1 : 0;
                lastCount = n;
                if (stablePolls >= 2) {
                    finishScout(games);
                } else {
                    ui.postDelayed(poll, POLL_MS);
                }
            });
        }
    };

    private final Runnable timeout = () -> {
        if (!scouting) return;
        // Use whatever we have so far, if anything.
        scout.evaluateJavascript(Scripts.FIND_GAMES, value -> {
            if (scouting) finishScout(parse(value));
        });
    };

    private void finishScout(JSONArray games) {
        scouting = false;
        ui.removeCallbacks(poll);
        ui.removeCallbacks(timeout);
        scout.stopLoading();
        scout.loadUrl("about:blank");
        if (games.length() > 0) {
            prefs.saveGames(games.toString());
        } else {
            scoutFailed = true;
        }
        refreshLauncher();
    }

    private static JSONArray parse(String value) {
        try {
            return new JSONArray(value);
        } catch (JSONException | NullPointerException e) {
            return new JSONArray();
        }
    }

    // ---- launcher page --------------------------------------------------------------

    private void refreshLauncher() {
        launcher.evaluateJavascript("window.render&&render()", null);
    }

    private String state() {
        JSONObject o = new JSONObject();
        try {
            o.put("games", new JSONArray(prefs.games()));
            o.put("scouting", scouting);
            o.put("failed", scoutFailed);
            o.put("settings", prefs.switches());
            o.put("gamesPage", Web.GAMES_PAGE);
        } catch (JSONException ignored) {
        }
        return o.toString();
    }

    private void play(String url, boolean focus) {
        Intent i = new Intent(this, GameActivity.class);
        i.putExtra(GameActivity.EXTRA_URL, url);
        i.putExtra(GameActivity.EXTRA_FOCUS, focus);
        startActivity(i);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    /** Called from launcher.html. Runs on a binder thread, so hop to the UI thread. */
    private final class Bridge {
        @JavascriptInterface
        public String state() {
            return MainActivity.this.state();
        }

        @JavascriptInterface
        public void play(String url) {
            ui.post(() -> MainActivity.this.play(url, prefs.get(Prefs.FOCUS)));
        }

        @JavascriptInterface
        public void playPage(String url) {
            ui.post(() -> MainActivity.this.play(url, false));
        }

        @JavascriptInterface
        public void refresh() {
            ui.post(MainActivity.this::startScout);
        }

        @JavascriptInterface
        public void set(String key, boolean value) {
            for (String k : Prefs.SWITCHES) {
                if (k.equals(key)) prefs.set(k, value);
            }
        }

        @JavascriptInterface
        public void exit() {
            ui.post(MainActivity.this::finishAndRemoveTask);
        }
    }
}
