# Mines — Firebase Remote Config reference

Remote Config is the admin control panel for the next-version economy, ads and
promotions. Every parameter below is read by `RemoteConfigManager` and validated
by `RemoteConfigParser` (`core/remoteconfig`). This document lists names, types,
defaults, allowed ranges and when each change takes effect.

## How the app treats Remote Config

- **Safe defaults.** If Remote Config was never fetched, is offline or fails, the
  app uses the in-app defaults below (or the last values it activated). The game
  never waits for Remote Config before it starts.
- **Validation.** A malformed value (for example `"fifty"`) falls back to its
  default. A number outside its range is clamped to the nearest bound. Strings
  are trimmed and length-limited. Every correction is logged as a warning
  (`RemoteConfigManager` tag), and none of them can crash the app.
- **Real-time updates.** The real-time listener activates published changes
  within seconds while the app is open. *Activation* below says when a change
  affects gameplay.
- **Not a security boundary.** Remote Config values live on the device and a
  modified app can ignore them. Use them to tune and switch shipped features, not
  to protect balances. (No trusted backend is in use, by decision.)
- **Play policy.** Only switch features that are already in the released build.
  Never use a flag to hide unfinished or unreviewed functionality from Google Play
  review.

## Parameters

### Maintenance and messaging

| Key | Type | Default | Range | Activation | Effect |
|---|---|---|---|---|---|
| `maintenance_mode` | Boolean | `false` | — | Immediate | Disables **Start Game** and shows a maintenance card on Home. A game already in progress is never interrupted. |
| `maintenance_message` | String | "The game is temporarily unavailable. Please try again soon." | ≤ 300 chars; blank → default | Immediate | Text of the maintenance card and toast. |
| `event_banner_enabled` | Boolean | `false` | — | Immediate | Shows the event banner on Home (only if the text is not blank). |
| `event_banner_text` | String | `""` | ≤ 140 chars (truncated) | Immediate | Event banner text. |

### Gameplay economy (plan §1.3)

| Key | Type | Default | Range | Activation | Effect |
|---|---|---|---|---|---|
| `easy_win_reward` | Number | `50` | 0–1000 | Next game | Coins for a confirmed win on Easy. |
| `medium_win_reward` | Number | `50` | 0–1000 | Next game | Coins for a win on Medium. |
| `hard_win_reward` | Number | `50` | 0–1000 | Next game | Coins for a win on Hard. |
| `loss_coin_fee` | Number | `20` | 0–1000 | Next game | **Entry fee** taken from every finished game, win or loss (never below 0), when the result is settled. Defaults: a win nets +30 (50 − 20); a loss is −20 Coins and −1 Life. (The key keeps its original name.) |
| `minimum_start_coins` | Number | `20` | 0–10000 | Next Start tap | Coins needed to start a game. Nothing is deducted at the start; the entry fee is settled with the result. |

"Next game" means the values are captured when a game starts, so changing them
never alters the payout of a game in progress.

### Coins ↔ Gems conversion (plan §1.2)

| Key | Type | Default | Range | Activation | Effect |
|---|---|---|---|---|---|
| `conversion_limit_daily` | Number | `3` | 0–20 | Next conversion | Successful conversions per IST day, shared by both directions. `0` disables conversions. Failed attempts never count. |

The rate is fixed in code at 500 Coins = 1 Gem.

### Post-win rewarded ad bonus (Stage 3C/3E)

| Key | Type | Default | Range | Activation | Effect |
|---|---|---|---|---|---|
| `rewarded_ad_bonus_enabled` | Boolean | `false` | — | Next game | Offers "Watch ad · +N Coins" after a win. |
| `rewarded_ad_bonus` | Number | `50` | 0–1000 | Next game | Bonus Coins. **Capped at the base win reward** for that difficulty (the ad can at most double a win), so with defaults a win plus the ad pays 50 + 50 Coins (+80 net of the entry fee). |

### Life recovery ad (Stage 3D)

| Key | Type | Default | Range | Activation | Effect |
|---|---|---|---|---|---|
| `life_recovery_ad_enabled` | Boolean | `false` | — | Immediate | Shows "Watch ad · Recover 1 Life" in the Hearts dialog while below 5 hearts. Grants exactly 1 Life, never above the maximum. Gem refill and hourly regeneration are unchanged. |

### Luck Spin (Stage 3B/3F)

Luck Spin is offered only when **both** `feature_luck_ads_enabled` and
`luck_ad_enabled` are `true` **and** the prize table has at least one Coin or
Gem slot.

| Key | Type | Default | Range | Activation | Effect |
|---|---|---|---|---|---|
| `feature_luck_ads_enabled` | Boolean | `false` | — | Immediate | Feature switch for the Luck Spin entry on Home. |
| `luck_ad_enabled` | Boolean | `false` | — | Immediate | Second switch (kept for compatibility). |
| `luck_ad_prizes` | String (JSON) | `""` | ≤ 8192 chars; ≤ 12 slots | Next spin | Preferred prize table, see below. |
| `luck_ad_coin_reward` | Number | `0` | 0–100000 | Next spin | Fallback table: Coin slot amount. |
| `luck_ad_gem_reward` | Number | `0` | 0–1000 | Next spin | Fallback table: Gem slot amount. |
| `luck_ad_coin_probability` | Number | `0` | 0–100 | Next spin | Fallback table: Coin slot weight. |
| `luck_ad_gem_probability` | Number | `0` | 0–100 | Next spin | Fallback table: Gem slot weight. |
| `luck_ad_no_reward_probability` | Number | `100` | 0–100 | Next spin | Fallback table: "No reward" slot weight. |

`luck_ad_prizes` format (weights are relative, not percentages):

```json
[
  {"type": "COINS", "amount": 25, "weight": 40},
  {"type": "COINS", "amount": 100, "weight": 10},
  {"type": "GEMS",  "amount": 1,  "weight": 10},
  {"type": "NONE",  "amount": 0,  "weight": 40}
]
```

Rules: `type` is `COINS`, `GEMS` or `NONE`; Coins 1–100000, Gems 1–1000, `NONE`
must have amount 0; weight > 0. Invalid slots are skipped. If no valid slot is
left, the fallback (`luck_ad_*_reward` / `*_probability`) table is used. The
dialog lists every outcome with its real chance, and the wheel always lands on
the slot that is credited.

### Rewarded-ad caps and cooldowns (Stage 3G)

Counted per device per IST day; only completed (rewarded) ads count.

| Key | Type | Default | Range | Activation | Effect |
|---|---|---|---|---|---|
| `win_bonus_ad_daily_cap` | Number | `10` | 0–100 | Immediate | Max post-win bonus ads per day. `0` disables. |
| `life_recovery_ad_daily_cap` | Number | `3` | 0–100 | Immediate | Max Life recovery ads per day. |
| `life_recovery_ad_cooldown_minutes` | Number | `30` | 0–1440 | Immediate | Wait between Life recovery ads. |
| `luck_ad_daily_cap` | Number | `5` | 0–100 | Immediate | Max Luck Spins per day. |
| `luck_ad_cooldown_minutes` | Number | `60` | 0–1440 | Immediate | Wait between spins. |
| `rewarded_ads_daily_cap_total` | Number | `15` | 0–100 | Immediate | Shared cap across all three placements. `0` disables all rewarded ads. |

Moving the device clock back never lengthens a cooldown. Reinstalling the app or
moving the clock to a new day resets the local counts.

### Store

| Key | Type | Default | Range | Activation | Effect |
|---|---|---|---|---|---|
| `store_featured_item` | String | `""` | ≤ 64 chars; must be a catalog id | Immediate | Puts a FEATURED tag on that item and lists it first in its category. Unknown ids are ignored. Ids: `board_classic_teal`, `board_deep_ocean`, `cellskin_frost`, `avatar_robo`. |
| `new_theme_enabled` | Boolean | `false` | — | Immediate | Shows catalog items listed in `StoreCatalog.newThemeItemIds` (empty in this release, so it currently has no effect). Owners always keep seeing what they own. |

## Recommended production rollout

1. Publish the defaults first (or publish nothing; the in-app defaults apply).
2. Turn on `rewarded_ad_bonus_enabled` with test ads in a test build and verify.
3. Turn on `life_recovery_ad_enabled`.
4. Publish a reviewed `luck_ad_prizes` table, then turn on both Luck flags.
5. Use Remote Config conditions (app version, percentage) for gradual rollouts.

## Owners

| Area | Owner |
|---|---|
| Economy values | TBD |
| Ads (flags, caps, prize table) | TBD |
| Maintenance / banner | TBD |
| Store promotion | TBD |
