# PizzaCrash — UI Reference Tracker

Source: GitHub `eskdev24/pizzacrash-V1` → `/UI_Refs/` (fetched 2026-10-21, copied into local `/workspace/UI_Refs/`).
Status legend: `pending` = not started · `spec` = analyzed/specified · `implementing` · `built` = code complete + build passes · `reviewed` = user reviewed render · `approved` = user formally approved.

## Screen references

| # | File | Screen | Implementation status | Approval |
|---|------|--------|-----------------------|----------|
| 1 | splash.png | Splash / loading (variant A) | pending | — |
| 2 | splash-new.png | Splash / loading (variant B — newer; confirm which is authoritative) | pending | — |
| 3 | onboard1.png | Onboarding page 1 | pending | — |
| 4 | onboard2.png | Onboarding page 2 | pending | — |
| 5 | onboard3.png | Onboarding page 3 | pending | — |
| 6 | onboarding1.png | Onboarding alt set page 1 (possible duplicate of onboard1 — needs confirmation) | pending | — |
| 7 | onboarding2.png | Onboarding alt set page 2 | pending | — |
| 8 | onboarding3.png | Onboarding alt set page 3 | pending | — |
| 9 | home.png | Main home screen | built (native recreation; awaiting user review) | pending |
| 10 | map.png | World selection / level map entry | built (reusable WorldMapActivity, all 10 worlds data-driven; awaiting user review) | pending |
| 11 | w1.png | World 1 — Pizza Town map | built via reusable map (theme artwork gaps noted in PROJECT_STATUS) | pending |
| 12 | w2.png | World 2 — Italian Village map | pending | — |
| 13 | w3.png | World 3 — Desert Oasis map | pending | — |
| 14 | w4.png | World 4 — Ocean Bay map | pending | — |
| 15 | w5.png | World 5 — Snowy Mountains map | pending | — |
| 16 | level.png | Level introduction screen | built (LevelIntroActivity; gameplay Play button honest stub until Phase 3) | pending |
| 17 | gameboard.png | Main gameplay screen | pending | — |
| 18 | pause.png | Pause dialog | pending | — |
| 19 | won-completed.png | Level completed screen | pending | — |
| 20 | loss-fail.png | Game-over / fail screen | pending | — |
| 21 | shop.png | Shop | built (coin-only booster shop; layout simplified vs ref) | pending |
| 22 | dailyreward.png | Daily rewards | built (once-per-day claim + streak; calendar grid deferred) | pending |
| 23 | settings.png | Settings | built (sound/music/haptics toggles persisted; audio engine wiring in Phase 8) | pending |
| 24 | file_0000000039108210b2e17d92897aef87.png | Unlabeled 1536×1024 landscape image (likely art/reference sheet — needs classification) | pending | — |

## Gaps vs. required scope (to request from user later)
- Worlds 6–10 map references (City Skyline, Tropical Island, Volcano Land, Candy Forest, Pizza Kingdom).
- Booster selection/inventory UI, reward dialogs, retry confirmation, level-unlock feedback.
- Tile/ingredient artwork, chef mascot, sound assets.

## Notes
- References are low-resolution thumbnails (~200–300 px wide); implement proportionally and request high-res exports if pixel-level fidelity is needed.
- No reference images will be placed in `app/src/main/res/` as backgrounds-as-screenshots; every control is implemented natively per the master prompt.
