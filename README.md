# Aqui!

Texts the people you pick when you stay in the same place (150 m circle). Each person has their own
timer (5 min or more) and their own message. A safety check-in: if you stop somewhere for a while,
the people you chose find out where.

- No account, no server. The text goes from your own phone number.
- No internet permission. Your location only leaves the phone inside the texts you set up.
- A small log stays on the phone (see "What is stored").

(Code name: DwellPing. The package and code still use that name.)

## How it works
- While it's on, the app gets a location fix about once a minute (Wi-Fi/cell, not GPS).
  An "Aqui! is on" notification shows the whole time. Android requires it for location this often.
- The first fix is where your stay starts. Each new fix is compared to that spot:
  - within 150 m -> the stay goes on (the notification shows "Here 12 min · Mom at 30 min")
  - farther, twice in a row -> a new stay starts at the new spot, at 0 min
- Fixes fuzzier than 150 m are ignored, so a bad fix can't reset your stay.
- When a person's timer runs out -> one text to them, unless:
  - you are moving faster than walking pace, or
  - a text already went out to them from within 225 m of here in the last 4 hours.
- Each person is handled once per stay. Lower someone's timer mid-stay and they get their text at the next fix.
- Saving a change to someone's number, timer or message gives them a fresh start: the 4-hour rule and
  "already texted at this stay" forget what came before. Changing only their name does not.
- Optional low-battery text: tick it for a person and they get one text with your last location when
  your battery hits 10%.

## Install
Download the APK from the website and open it on your phone (allow "install unknown apps" when asked).

## Build it yourself
1. Install Android Studio. Open this folder (File > Open). Let it sync Gradle.
2. On your phone: Settings > About phone > tap "Build number" 7 times.
   Then Developer options > turn on USB debugging.
3. Connect the phone with USB. Click Run (green triangle).
4. In the app:
   - Tap "Add person". Tap "Choose from contacts", or type their number with country code (+1...).
     Pick their timer, optionally write their message (see "Messages").
     Tap "Send test text" to check it, then "Add person".
   - Tap the gear > Permissions, tap the button until every row says Done.
     For location, choose **"Allow all the time"**.
   - Back on Start, tap the big button. It turns green and counts "N min here". Then close the app.
     To stop: hold the button for a second.
5. Settings > Apps > Aqui! > Battery > set to **Unrestricted**.
   (Samsung, Xiaomi, OnePlus kill background apps aggressively. See dontkillmyapp.com.)

Tests: `./gradlew testDebugUnitTest`.

## Messages
Each person has their own message: tap them on the Start screen. Placeholders are optional:
- `{place}` – the street address (or "this spot" if there's no location)
- `{minutes}` – their timer
- `{map}` – a Google Maps link (left out if there's no location)

Leave it empty to send the default: "Hey, I've been at {place} for {minutes}+ min." plus the map link.
The preview under the field shows what will be sent.

## Change things
The settings are in `Config.kt`:
- `DEFAULT_MINUTES` – a new person's timer (default 15)
- `MIN_MINUTES` / `MAX_MINUTES` – the timer's limits (5 to 240)
- `RADIUS_METERS` – circle size (100–200 m is best)
- `COOLDOWN_HOURS` – no second text to the same person from the same spot within this time (default 4)
- `SAME_SPOT_METERS` – how close counts as "the same spot" (default 1.5 x radius)
- `MOVING_SPEED_MPS` – faster than this = moving, no text (default 2 m/s)

To test fast, set `MIN_MINUTES = 1` and a person's timer to 1 min, then fake a position with the
emulator's Extended Controls > Location.

## What is stored
All of it in a local database (`ping_log.db`) that never leaves the phone. Backups are turned off.

| Table | What | Kept for |
|---|---|---|
| `contacts` | Your people: number, name, timer, message | Until you delete the person |
| `ping_events` | Each time a timer ran out: time, location, result (`sent`, `skipped_same_spot`, `skipped_moving`, `send_failed`), the text | 90 days |
| `location_fixes` | Each location fix: time, location, accuracy, speed, minutes at this stay | 7 days |

- Street addresses come from Android's own address lookup (Geocoder), which sends the spot to the
  phone's location service. Nothing else is sent.
- To delete everything: Settings > Apps > Aqui! > Storage > **Clear storage**, or uninstall.

## Release build
`./gradlew assembleRelease` makes a ~3 MB APK in `app/build/outputs/apk/release/`.

## License
Copyright (C) 2026 IamAnthonycruz. Aqui! was created by IamAnthonycruz (github.com/IamAnthonycruz).

GNU General Public License v3.0, see `LICENSE`. You may use, change, and share this code, but any
version you share must also be GPL-3.0 with its source published, and must keep this copyright
and author notice (GPL-3.0 section 7(b)).
