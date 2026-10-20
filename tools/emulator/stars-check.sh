#!/usr/bin/env bash
# Five-star rule on the debug build. Expected labels:
#   s1: "5 of 5 stars", "Perfect — no hearts lost, no hints"
#   s2: "3 of 5 stars", "1 heart lost · 1 hint used"
#   s3: "4 of 5 stars", "No hearts lost · 1 hint used"   (Daily Bloom)
#   s4: Home shows the Daily card with 4 of 5 stars and garden stars out of 100
. "$(dirname "$0")/common.sh"

fresh
tapon Play 3
autotap 6
shot s1_perfect
tapon "Next level" 3
tapon Hint 1
lp $HOOK_WRONGTAP 1.6
autotap 10
shot s2_hint_and_wrong
tapon Home 2.5
tapon "Play today's Bloom" 4
tapon Hint 1
autotap 45
shot s3_daily_hint 360
tapon Home 3
shot s4_home 520
crashcheck end
