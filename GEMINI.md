# ARROW BLOOM — ENGINEERING, MEMORY & SESSION HANDOFF CONSTITUTION

> **MANDATE:** These rules govern every single response, diagnostic, build, release, and console change for **Arrow Bloom** (`com.aweagle.arrowbloom`, AW EagleCraft). Inherit and enforce the studio constitution in `../GEMINI.md` and `../BlockBloom/GEMINI.md` alongside this file.

## 🏛️ NON-NEGOTIABLE RULES FOR EVERY TASK

1. **Read Memory First (Session Start):** Before modifying code or Play Console state, read:
   - `../blockbloom-notes/memory/MEMORY.md` (`topic_arrow_bloom.md`, `topic_console_audit_2026_11_25.md`, `topic_personal_life.md`)
   - `../arrowbloom-notes/00-START-HERE.md` and latest `../arrowbloom-notes/journal/YYYY-MM-DD.md`
   - `../arrowbloom-notes/claude-memory/project_arrow_bloom.md`
2. **Persist to Local + GitHub Memory (Before Ending Any Task):**
   - Update `../arrowbloom-notes/journal/YYYY-MM-DD.md`, `../arrowbloom-notes/claude-memory/project_arrow_bloom.md`, `../blockbloom-notes/memory/topic_arrow_bloom.md`, and `../blockbloom-notes/journal/YYYY-MM-DD.md` with exact live state (tracks, `versionCode`, tester count, console warnings, next steps) in clean UTF-8.
   - Commit and push `arrowbloom-notes` (`journal`), `blockbloom-notes` (`journal`), and `ArrowBloom` (`release/2026-10-20-line-arrows`; never merge to `main` — Ajay merges).
3. **Production Safety for `1.0.0`:**
   - Keep `Arrow Bloom 1.0.0` strictly offline/no-ads/no-permissions beyond `VIBRATE` until the 12-tester × 14-day Production access gate clears on Play Console.
   - When adding Firebase (`arrow-bloom` dedicated project recommended) and AdMob (`pub-1693732810683011`) in `1.1.0+`, update Data Safety, Ads declaration, Ad ID declaration, `arrow-bloom-privacy.html`, and `app-ads.txt` **before** rolling out to any public track.
