package com.home.peppagames;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Plays one game full screen. Back does nothing; a grown-up leaves by holding a finger
 * on the top-left corner for a few seconds. Touches there still reach the game.
 */
public class GameActivity extends Activity {
    static final String EXTRA_URL = "url";
    static final String EXTRA_FOCUS = "focus";

    private static final long HOLD_TO_EXIT_MS = 3_000;
    private static final long HINT_AFTER_MS = 700;
    private static final int CORNER_DP = 80;
    /** When to re-run the focus script after a page loads; games often build their frame late. */
    private static final long[] FOCUS_AT_MS = {0, 800, 2_000, 4_000, 7_000, 11_000, 16_000, 25_000};

    private final Handler ui = new Handler(Looper.getMainLooper());
    private Prefs prefs;
    private WebView web;
    private TextView hint;
    private String gameHost;
    private boolean focus;
    private float cornerPx;
    private boolean holding;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = new Prefs(this);
        String url = getIntent().getStringExtra(EXTRA_URL);
        focus = getIntent().getBooleanExtra(EXTRA_FOCUS, true);
        gameHost = url == null ? null : Uri.parse(url).getHost();
        cornerPx = dp(CORNER_DP);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        web = new WebView(this);
        web.setBackgroundColor(Color.BLACK);
        Web.configure(web, prefs);
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                if (!r.isForMainFrame()) return false;
                return !allowed(r.getUrl()); // ads and links to other sites go nowhere
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                return Web.maybeBlock(prefs, r);
            }

            @Override
            public void onPageFinished(WebView v, String u) {
                scheduleFocus();
            }
        });
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));

        hint = new TextView(this);
        hint.setText("Keep holding to go back to the games…");
        hint.setTextColor(Color.WHITE);
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        int pad = (int) dp(14);
        hint.setPadding(pad * 2, pad, pad * 2, pad);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xCC000000);
        bg.setCornerRadius(dp(24));
        hint.setBackground(bg);
        hint.setVisibility(View.GONE);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER);
        root.addView(hint, lp);

        setContentView(root);
        Web.fullScreen(this);

        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, () -> { });
        }
        if (url != null) web.loadUrl(url);
    }

    private boolean allowed(Uri u) {
        String scheme = u.getScheme();
        if (!"https".equals(scheme) && !"http".equals(scheme)) return false;
        return Web.isNick(u) || (gameHost != null && gameHost.equals(u.getHost()));
    }

    private void scheduleFocus() {
        ui.removeCallbacks(focusNow);
        if (!focus) return;
        for (long t : FOCUS_AT_MS) ui.postDelayed(focusNow, t);
    }

    private final Runnable focusNow = () -> web.evaluateJavascript(Scripts.FOCUS_GAME, null);

    // ---- hidden exit -----------------------------------------------------------------

    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (e.getX() < cornerPx && e.getY() < cornerPx) startHold();
                break;
            case MotionEvent.ACTION_MOVE:
                if (holding && (e.getX() > cornerPx * 1.5f || e.getY() > cornerPx * 1.5f)) stopHold();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                stopHold();
                break;
            default:
                break;
        }
        return super.dispatchTouchEvent(e); // the game still gets every touch
    }

    private void startHold() {
        holding = true;
        ui.postDelayed(showHint, HINT_AFTER_MS);
        ui.postDelayed(exit, HOLD_TO_EXIT_MS);
    }

    private void stopHold() {
        holding = false;
        ui.removeCallbacks(showHint);
        ui.removeCallbacks(exit);
        hint.setVisibility(View.GONE);
    }

    private final Runnable showHint = () -> hint.setVisibility(View.VISIBLE);

    private final Runnable exit = () -> {
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    };

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        // Ignored on purpose: little thumbs hit Back all the time.
    }

    // ---- lifecycle -------------------------------------------------------------------

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) Web.fullScreen(this);
    }

    @Override
    protected void onPause() {
        web.onPause(); // stops game audio when the screen turns off or the app is left
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
    }

    @Override
    protected void onDestroy() {
        ui.removeCallbacksAndMessages(null);
        web.destroy();
        super.onDestroy();
    }

    private float dp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
