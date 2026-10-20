#!/usr/bin/env bash
# Shared setup for the emulator scripts (Git Bash on Windows). Source it: . "$(dirname "$0")/common.sh"
# Needs: a running emulator (AVD "bloomtest", 1080x2400, 420 dpi), Node.js, Android SDK platform-tools.
# Coordinates below are for that AVD; on another size, re-read them from a dump (node uitap.js ui.xml).

export MSYS_NO_PATHCONV=1
export ANDROID_HOME="${ANDROID_HOME:-D:/DevTools/Android/Sdk}"
ADB="$ANDROID_HOME/platform-tools/adb.exe"
# Windows-style (C:/...) paths: node.exe and adb.exe do not understand /d/... with path conversion off.
HERE="$(cygpath -m "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)")"
REPO="$(cygpath -m "$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)")"
# Windows-style temp folder, so adb.exe and node agree on the path.
WORK="$(cygpath -m "${TEMP:-${TMP:-/tmp}}")/arrowbloom-emu"
mkdir -p "$WORK/shots"
PKG="${PKG:-com.aweagle.arrowbloom.debug}"
DEBUG_APK="${DEBUG_APK:-$(cygpath -m "$HOME")/.build-arrowbloom/app/outputs/apk/debug/app-debug.apk}"

# Debug-build-only long-press hooks (BuildConfig.DEBUG; R8 removes them from release builds):
HOOK_AUTOTAP="113 331"     # arrows chip  -> tap the hinted arrow
HOOK_WRONGTAP="537 331"    # hearts       -> a wrong tap (costs a heart)
HOOK_NEXTGARDEN="541 171"  # title        -> first level of the next garden
HOOK_SKIPFIVE="540 234"    # subtitle     -> next level that is a multiple of 5 (a shaped board)

dump() { timeout 30 "$ADB" shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1; timeout 30 "$ADB" pull /sdcard/ui.xml "$WORK/ui.xml" > /dev/null 2>&1; }
labels() { node "$HERE/uitap.js" "$WORK/ui.xml" | cut -c1-"${1:-300}"; }
# shot NAME [width]: screenshot into $WORK/shots and print the on-screen labels
shot() { timeout 20 "$ADB" shell screencap -p /sdcard/s.png; timeout 30 "$ADB" pull /sdcard/s.png "$WORK/shots/$1.png" > /dev/null 2>&1; dump; echo "[$1] $(labels "${2:-300}")"; }
# tapon TEXT [sleep]: tap the first element whose text or description contains TEXT
tapon() { dump; local xy; xy=$(node "$HERE/uitap.js" "$WORK/ui.xml" "$1" 2>/dev/null) && timeout 10 "$ADB" shell input tap $xy && sleep "${2:-2.5}"; }
# lp X Y [sleep]: long-press
lp() { timeout 10 "$ADB" shell input swipe "$1" "$2" "$1" "$2" 650; sleep "${3:-0.6}"; }
autotap() { local i; for i in $(seq 1 "$1"); do lp $HOOK_AUTOTAP 0.55; done; sleep 1.4; }
launch() { timeout 20 "$ADB" shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 > /dev/null 2>&1; sleep 6; }
fresh() { timeout 120 "$ADB" install -r "$DEBUG_APK" | tail -1; timeout 20 "$ADB" shell pm clear "$PKG" > /dev/null; timeout 15 "$ADB" logcat -c; launch; }
crashcheck() { echo "(crash lines: $(timeout 15 "$ADB" logcat -d -s AndroidRuntime:E 2>/dev/null | grep -c "$PKG") at $1)"; }
