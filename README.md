# Samsung AC Remote (IR)

Android app that turns a phone with an IR blaster into a remote for a Samsung
split AC (made for AS122USB, 2010). Controls: ON/OFF, temperature 16-30 C,
mode (Auto/Cool/Dry/Fan), fan speed, Turbo, Quiet, Swing, and a sleep timer
(30 min / 1 / 2 / 4 hr) that sends OFF from the phone.

## Build the APK (no local Android setup needed)
1. Create an empty GitHub repo, push the contents of this folder as-is.
2. Open the repo's **Actions** tab -> "Build APK" run -> download the
   `ac-remote-apk` artifact (contains `app-debug.apk`).
3. Copy the APK to the phone and install it (allow "install unknown apps").

## Using it
Point the phone's **top edge** (the IR window) at the AC's receiver, 1-3 m away.
The IR format defaults to **14-byte**, which is confirmed working on the AS122USB.
The bottom button can switch formats if a different AC needs it.

Sleep timer: tap 30 min / 1 hr / 2 hr / 4 hr. At the set time the app sends OFF
(twice) even if closed, so keep the phone pointed at the AC. It shows as an
alarm in the status bar (that is what keeps it exact). Pressing OFF cancels it.

## Updating
`debug.keystore` is committed on purpose so every build has the same signature
and a new APK installs over the old one. (Builds from before v1.1 used a random
CI key, so uninstall that one once.)

## Notes
- The AC can't report its state back, so the app remembers what it last sent.
- `tools/verify_encoder.py` checks the encoder against the known-good
  IRremoteESP8266 ON/OFF frames (also runs in CI before building).
- Protocol source: IRremoteESP8266 `ir_Samsung.cpp` (Samsung A/C, 38 kHz).
