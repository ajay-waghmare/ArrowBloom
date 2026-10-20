#!/usr/bin/env bash
# Raw captures for the Play phone screenshots, written to store/screenshots/src/ (1080x2400).
# Then frame them:  node tools/store-screenshots.js   (1080x1920 with captions)
# The debug build has the same UI as release; the hooks are only used to navigate.
. "$(dirname "$0")/common.sh"
OUT="$REPO/store/screenshots/src"
mkdir -p "$OUT"
grab() { timeout 30 "$ADB" pull /sdcard/s.png "$(cygpath -m "$OUT")/$1.png" > /dev/null 2>&1; echo "captured $1"; }
cap() { timeout 20 "$ADB" shell screencap -p /sdcard/s.png; grab "$1"; }

fresh
tapon Play 3
autotap 6
tapon "Next level" 3
# a wrong tap, captured mid-bump
timeout 10 "$ADB" shell "input swipe ${HOOK_WRONGTAP% *} ${HOOK_WRONGTAP#* } ${HOOK_WRONGTAP% *} ${HOOK_WRONGTAP#* } 560; screencap -p /sdcard/s.png"; grab 04-hearts
sleep 1
lp $HOOK_SKIPFIVE 3; lp $HOOK_SKIPFIVE 3; lp $HOOK_SKIPFIVE 3; lp $HOOK_SKIPFIVE 3     # 5, 10, 15, 20 (heart)
cap 03-shapes
lp $HOOK_NEXTGARDEN 3; lp $HOOK_NEXTGARDEN 3; lp $HOOK_NEXTGARDEN 3; lp $HOOK_NEXTGARDEN 3   # 21, 41, 61, 81 (Aurora)
tapon Hint 0.4
cap 02-hint
lp $HOOK_NEXTGARDEN 3                                                                     # 101 (Infinite)
cap 01-board
tapon Home 2.5
tapon "Play today's Bloom" 4
cap 05-daily
tapon Home 3
cap 06-home
crashcheck end
