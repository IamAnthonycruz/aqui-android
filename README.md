# Aqui!

Texts the people you pick when you stay in the same place (150 m circle). Each person has their own
timer (5 min or more) and their own message. A safety check-in: if you stop somewhere for a while,
the people you chose find out where.

- No account, no server. The text goes from your own phone number.
- No internet permission. Your location only leaves the phone inside the texts you set up.
- A small log stays on the phone (see "What is stored").

> **Not an emergency service.** Aqui! is a convenience, not a safety guarantee. Texts can arrive late
> or not at all. In an emergency, call 911 or your local emergency number. See "Disclaimer" below.

## Requirements
| | |
|---|---|
| Android | 10 or newer (API 29+). Built for Android 15 (API 35). |
| Phone | An Android phone with a SIM and a plan that can send SMS. Texts are normal SMS from your number, so your carrier's usual SMS rates apply. Wi-Fi-only tablets can't send texts. |
| Google Play services | Required. Location comes from Google's fused location provider, so phones without Play services (e.g. Huawei, de-Googled ROMs) won't get fixes. |
| Location | Location turned on, with Wi-Fi scanning recommended indoors. GPS is not needed. |
| Storage | About 3 MB download, about 3 MB installed. The on-phone log adds about 1–2 MB at most: old rows are deleted automatically (see "What is stored"). |
| Internet | Not needed. Street addresses in texts use the phone's own address lookup; without a connection the text says "this spot" and still includes the map link. |
| Battery | One low-power location fix a minute while it's on. Set the app's battery use to Unrestricted, or the phone may pause it. |

Tested on a Pixel 9a (Android 17) and the Android 15 emulator.

## Permissions
| Permission | Why |
|---|---|
| Location, "Allow all the time" | To notice you've stayed in one place while the app is closed. |
| SMS | To send the texts, from your own number. |
| Notifications | For the "Aqui! is on" notification Android requires while location runs. |
| Phone (optional) | Some carriers make Android check the SIM before sending a long text in parts. Aqui! never makes calls. |
| Battery unrestricted (optional) | So the phone doesn't pause it in the background. |
| Run at startup | To start again after a reboot or an update, if it was on. |

Run at startup is granted on install. The app asks for the others on first launch and says why;
you can skip any and allow it later in Settings.

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
1. Download the APK from the website on your phone.
2. Open it. Android asks to allow installing from your browser: allow it, then tap Install.
   Google Play Protect may say it doesn't recognize the app; tap "Install anyway". (It isn't on the Play Store.)
3. Open Aqui! and follow the setup pages.

To update, install the new APK over the old one. Your people and settings stay.

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

## Disclaimer
- **Provided "as is", with no warranty.** The authors are not liable for any harm, loss, or cost from
  using it, as set out in sections 15 and 16 of the GPL-3.0 (`LICENSE`).
- **Don't rely on it for your safety.** A text can be late, wrong, or never sent, for example when:
  - the phone has no signal, is off, or its battery is dead
  - Android or a battery saver stops the app in the background
  - a permission was turned off, or the carrier blocks or delays the text
  - the location is off by tens or hundreds of meters (it uses Wi-Fi and cell towers, not GPS)
  - a number or setting was entered wrong
- **Texts are sent from your number.** You pay your carrier's usual rates, and you decide who gets your location.
- **Only install it on your own phone.** It always shows a notification while it runs. Using it to track
  someone without their knowledge may be illegal where you live.
- Map links open Google Maps. Aqui! is not affiliated with Google.

## License
Copyright (C) 2026 IamAnthonycruz. Aqui! was created by IamAnthonycruz (github.com/IamAnthonycruz).

GNU General Public License v3.0, see `LICENSE`. You may use, change, and share this code, but any
version you share must also be GPL-3.0 with its source published, and must keep this copyright
and author notice (GPL-3.0 section 7(b)).
