#!/usr/bin/env bash
# Full debug-build walkthrough (29 steps): hint, outcome, grid, wrong tap, resume after force-stop, every
# shape and garden, settings from the game, Daily Bloom, How to play, German, out of hearts.
# Screenshots land in $WORK/shots (printed at the end). Expect "(crash lines: 0 ...)" twice.
. "$(dirname "$0")/common.sh"

fresh
shot w01_home
tapon Play 3
shot w02_level1
tapon Hint 1.2
shot w03_hint
autotap 6
shot w04_outcome
tapon "Next level" 3
shot w05_level2
tapon Grid 1.2
shot w06_grid
tapon Grid 1
lp $HOOK_WRONGTAP 1.6
shot w07_heart_lost
autotap 2
tapon Home 2.5
shot w08_home_continue
timeout 15 "$ADB" shell am force-stop "$PKG"; sleep 1.5
launch
shot w09_home_after_restart
tapon Continue 3
shot w10_level2_resumed
lp $HOOK_SKIPFIVE 3; shot w11_level5_flower
lp $HOOK_SKIPFIVE 3; shot w12_level10_diamond
lp $HOOK_SKIPFIVE 3; shot w13_level15_circle
lp $HOOK_SKIPFIVE 3; shot w14_level20_heart
lp $HOOK_NEXTGARDEN 3; shot w15_level21_orchard
lp $HOOK_NEXTGARDEN 3; shot w16_level41_lagoon
lp $HOOK_NEXTGARDEN 3; shot w17_level61_dusk
lp $HOOK_NEXTGARDEN 3; shot w18_level81_aurora
lp $HOOK_NEXTGARDEN 3; shot w19_level101_infinite
crashcheck gardens
tapon Settings 2
shot w20_settings_from_game
tapon Back 2
shot w21_back_in_game
tapon Home 2.5
tapon "Play today's Bloom" 4
shot w22_daily
autotap 45
shot w23_daily_outcome 320
tapon Home 3
shot w24_home_daily_done 400
tapon Settings 2
shot w25_settings
tapon "How to play" 2
shot w26_howto 400
tapon "Got it" 1.5
tapon Back 2
timeout 10 "$ADB" shell cmd locale set-app-locales "$PKG" --user 0 --locales de-DE; sleep 4
shot w27_home_de 300
tapon Spielen 3.5 || tapon Weiter 3.5
shot w28_game_de 300
lp $HOOK_WRONGTAP 1.6; lp $HOOK_WRONGTAP 1.6; lp $HOOK_WRONGTAP 2.2
shot w29_fail_de 300
timeout 10 "$ADB" shell cmd locale set-app-locales "$PKG" --user 0 --locales ""; sleep 2
crashcheck end
echo "screenshots: $WORK/shots"
