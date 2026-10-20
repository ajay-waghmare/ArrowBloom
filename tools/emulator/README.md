# Emulator scripts

Scripted checks on the **debug** build, run from Git Bash on Windows with an emulator already running
(AVD `bloomtest`: 1080×2400, 420 dpi). They tap by on-screen text (`uitap.js` reads a `uiautomator` dump)
and use four debug-only long-press hooks to jump around the game quickly:

| Long-press | Does | Where (1080×2400) |
|---|---|---|
| arrows-left chip | taps the hinted arrow | 113, 331 |
| hearts | makes a wrong tap (costs a heart) | 537, 331 |
| level title | jumps to the next garden's first level | 541, 171 |
| garden subtitle | jumps to the next level that is a multiple of 5 (a shaped board) | 540, 234 |

The hooks exist only when `BuildConfig.DEBUG` is true; R8 removes them from release builds.

```bash
./gradlew :app:assembleDebug           # APK lands in ~/.build-arrowbloom/app/outputs/apk/debug/
bash tools/emulator/walkthrough.sh     # 29 steps, prints labels per step, screenshots in %TEMP%/arrowbloom-emu/shots
bash tools/emulator/stars-check.sh     # the five-star rule on real boards
bash tools/emulator/store-shots.sh     # raw store captures into store/screenshots/src/
node tools/store-screenshots.js        # frames them as 1080×1920 Play screenshots
```

Each script reinstalls the debug APK and clears its data first, and ends with `(crash lines: N ...)`,
which must be 0. `common.sh` holds the paths (`ANDROID_HOME`, the debug APK, the temp folder) and the helpers.
For a release-build check, install `outputs/apk/release/app-release.apk` over the previous version and
play by hand: the hooks are not there.
