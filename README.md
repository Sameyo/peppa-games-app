# Peppa Games (home app)

A small Android app for playing the Peppa Pig web games from
[nick.com](https://www.nick.com/games/peppa-pig-games) on a phone, without the
fiddly website around them. For private use at home only; not for release.

## What it does

- **Big picture tiles.** Opens to a grid with one tile per game. The list is read
  from the nick.com Peppa page when the app starts, so new games show up by themselves.
  It is refreshed once a day.
- **The game fills the screen**, in landscape, with the site's header, ads and footer hidden.
- **Stays on nick.com.** Links and ads that try to go to other sites do nothing, and
  common ad and tracking servers are blocked.
- **Toddler-proof exit.** The Back button does nothing during a game. To get back to
  the tiles, **hold a finger on the top-left corner for 3 seconds.** Taps there still
  reach the game.
- **Grown-up settings.** Press and hold the ⚙ button (bottom right of the tiles screen)
  for 2 seconds.

| Setting | Use it when |
| --- | --- |
| Only Peppa games | On by default. Turn off to see other shows' games listed on the page too. |
| Game fills the screen | Turn off if a game stays black or never starts. You'll see the normal web page instead. |
| Block ads | Turn off if a game gets stuck before it starts (some games wait for an ad). |
| Desktop version of the site | Try if a game says it only works on a computer, then tap *Find games again*. |

## Install on the phone

1. On the phone, open this repository's **Releases** page in the browser (sign in to
   GitHub, since the repository is private) and download `PeppaGames.apk` from the newest release.
2. Open the downloaded file. Android will ask to allow installs from your browser:
   allow it, go back, and tap **Install**.
3. Updates: download the newer APK and install it over the old one. Your settings are kept.

Every push to `main` builds a new APK automatically (see `.github/workflows/build.yml`).
You can also start a build by hand from the **Actions** tab, under *Build APK* → *Run workflow*.

### Stop little ones leaving the app

The Home and Recents gestures still work. To lock the phone to this app, use Android's
**App pinning**: Settings → Security (or *Security & privacy* → *More security settings*)
→ **App pinning**, turn it on, and turn on *Ask for PIN before unpinning*. Then open
Recents, tap the app icon above Peppa Games, and choose **Pin**.

## Build it yourself

Open the folder in Android Studio and press Run, or from a terminal with the Android SDK installed:

```sh
./gradlew assembleRelease
# app/build/outputs/apk/release/app-release.apk
```

## Good to know

- The app depends on how nick.com builds its pages. If they change their site, the game
  list or the full-screen view may stop working until the scripts in
  `app/src/main/java/com/home/peppagames/Scripts.java` are updated.
- Hiding the site's ads is against nick.com's terms of use. This is meant for your own
  family's use only.
- The signing key in `app/home.keystore` is committed on purpose so every build installs over
  the previous one. Keep the repository private, and never use that key for anything you publish.

## How it works

- `MainActivity` shows `assets/launcher.html` (the tiles). Behind it, a hidden WebView
  loads the nick.com Peppa page and runs `Scripts.FIND_GAMES` to collect game links and
  pictures. The list is saved and shown straight away the next time the app starts.
- `GameActivity` opens a game page and runs `Scripts.FOCUS_GAME` several times while it
  loads. It finds the biggest game frame (iframe, canvas or video), stretches it over the
  screen and hides everything else.
- `Web` holds the shared WebView settings, the ad server list and the full-screen setup.
