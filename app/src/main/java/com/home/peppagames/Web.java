package com.home.peppagames;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.io.ByteArrayInputStream;

/** WebView setup, ad blocking and full-screen helpers shared by both screens. */
final class Web {
    static final String GAMES_PAGE = "https://www.nick.com/games/peppa-pig-games";

    private static final String DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36";

    /** Ad and tracking hosts. Requests to these (or their subdomains) get an empty response. */
    private static final String[] AD_HOSTS = {
            "doubleclick.net", "googlesyndication.com", "googleadservices.com",
            "adservice.google.com", "googletagservices.com", "imasdk.googleapis.com",
            "google-analytics.com", "amazon-adsystem.com", "adnxs.com", "moatads.com",
            "scorecardresearch.com", "taboola.com", "outbrain.com", "criteo.com",
            "criteo.net", "pubmatic.com", "rubiconproject.com", "openx.net",
            "casalemedia.com", "teads.tv", "adsafeprotected.com", "doubleverify.com",
            "chartbeat.com", "chartbeat.net", "adsrvr.org", "yieldmo.com",
            "33across.com", "sharethrough.com", "indexww.com", "smartadserver.com",
            "innovid.com", "spotxchange.com", "springserve.com", "freewheel.tv",
            "fwmrm.net", "connect.facebook.net", "branch.io", "bluekai.com",
            "krxd.net", "quantserve.com", "omtrdc.net", "demdex.net", "everesttech.net",
    };

    private Web() {
    }

    @SuppressLint("SetJavaScriptEnabled")
    static void configure(WebView v, Prefs prefs) {
        WebSettings s = v.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setSupportMultipleWindows(false); // window.open() stays in this view, behind our URL guard
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        if (prefs.get(Prefs.DESKTOP)) s.setUserAgentString(DESKTOP_UA);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(v, true);
        v.setOverScrollMode(View.OVER_SCROLL_NEVER);
    }

    static WebResourceResponse maybeBlock(Prefs prefs, WebResourceRequest req) {
        if (!prefs.get(Prefs.BLOCK_ADS)) return null;
        String host = req.getUrl().getHost();
        if (host == null) return null;
        for (String ad : AD_HOSTS) {
            if (host.equals(ad) || host.endsWith("." + ad)) {
                return new WebResourceResponse("text/plain", "utf-8",
                        new ByteArrayInputStream(new byte[0]));
            }
        }
        return null;
    }

    static boolean isNick(Uri u) {
        String host = u.getHost();
        return host != null && (host.equals("nick.com") || host.endsWith(".nick.com"));
    }

    static void fullScreen(Activity a) {
        Window w = a.getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 28) {
            w.getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        if (Build.VERSION.SDK_INT >= 30) {
            w.setDecorFitsSystemWindows(false);
            WindowInsetsController c = w.getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.systemBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }
}
