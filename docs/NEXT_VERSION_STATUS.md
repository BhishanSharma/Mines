# Mines — Next Version Status

**Branch:** `next-version` (uncommitted working tree on top of `6b788bf`)
**Status checked:** 21 November 2026

> "Implemented" means the code is in the working tree, it builds, and its
> rules are covered by unit tests. Nothing here has been tested on a physical
> device yet. Items that need devices, Play Console/AdMob setup or product
> decisions stay open.

## 1. Decisions recorded in this round

| Decision | Outcome |
|---|---|
| Trusted backend (Cloud Functions) | **Not used.** The economy is administered through Firebase Remote Config. Client code applies wallet changes in atomic, idempotent Firestore transactions. |
| Firestore Security Rules | **Skipped** for now (not part of this round). |
| Real-money purchases (Stage 5) | **Skipped** for now. Without server verification, paid grants can't be protected. |
| Store prices | **Kept** (Deep Ocean 10, Frost Cells 20, Robo 15 Gems). The plan's 500-Gem theme price is still an open product decision. |
| Entry fee | **Charged on every finished game, win or loss** (product owner, 21 Nov 2026). Defaults: win +50 − 20 = **+30**; loss **−20 Coins and −1 Life**. This replaces plan §1.3's "winning does not deduct the fee". |
| Stage 3E "double win" | Implemented as a rule of the existing win bonus: the optional ad can **at most double** the base win reward, so with defaults a win plus the ad pays 50 + 50 Coins (+80 net of the entry fee). There is no separate second ad. |
| Product name | **Luck Spin** for the Luck Ads feature. |

## 2. Verification evidence

| Check | Result |
|---|---|
| Baseline build before changes (`assembleDebug testDebugUnitTest`) | Passed; 2 test classes existed (LifeCalculatorTest, ExampleUnitTest) |
| Clean `assembleDebug` + `assembleRelease` (R8) after changes | Passed |
| Unit tests (`testDebugUnitTest`) | **90 tests, 0 failures** across 11 suites |
| Instrumentation / device tests | Not run (no device or emulator image available) |
| `master` modified | No; all work is on `next-version`, nothing committed |

Unit-test suites: `IstCalendarTest`, `ConversionRulesTest`, `GameSettlementRulesTest`,
`CosmeticPurchaseRulesTest`, `AdFrequencyRulesTest`, `LuckPrizeTableTest`,
`EconomyCodecTest` (core:economy); `RemoteConfigParserTest`; `LifeCalculatorTest`;
`StoreItemStatesTest`; `ExampleUnitTest`.

## 3. Stage status

| Stage | Scope | Status |
|---|---|---|
| 0 | Baseline and compatibility | ✅ Build/test baseline recorded; behaviour and schema documented (§5, §6) |
| 0.5 | Remote Config | ✅ Validation, activation timing, all flags wired; documented in `docs/REMOTE_CONFIG.md` |
| 1 | Wallet and conversions | ✅ Client logic + tests; 🟡 device-clock and tampering limits (no backend) |
| 2 | Gameplay economy | ✅ Fixed and idempotent; 🟡 device QA |
| 3A | Rewarded Ads SDK + consent | ✅ Reload with backoff, UMP consent; 🟡 AdMob privacy message, production IDs |
| 3B/3F | Luck Spin | ✅ UI, caps, crediting; 🟡 final prize table (product) |
| 3C | Post-win bonus | ✅ Idempotent, caps, offer captured at game start |
| 3D | Life recovery ad | ✅ Implemented |
| 3E | Double win | ✅ Resolved as the ≤ 2× cap on the win bonus |
| 3G | Caps and cooldowns | ✅ Per placement and shared, via Remote Config (per device) |
| 3H | Ads QA | ⬜ Needs devices |
| 4 | Store and cosmetics | ✅ Atomic purchase, Unequip, Reset, Robo avatar, avatar sync |
| 5 | Real-money purchases | ⬜ Skipped by decision |
| 6 | Backend security | ⬜ Not pursued by decision (see §7 risks) |
| 7 | QA and release | 🟡 Unit tests + builds done; device, policy and sign-off open |

## 4. What changed, by stage

### Bugs fixed from the review
- Game settlement could charge the entry fee and then drop the reward, or skip
  the Life on a loss, when a step failed. The fee, reward and Life now apply
  together as one settlement (win +30, loss −20 and −1 Life with defaults).
- A failed or offline wallet step dropped the whole game result. Settlement and
  history saving are now independent.
- Offline conversions crashed the app. All conversion and status calls are
  guarded, and the Store shows cached status offline.
- Ads stayed unavailable after one failed load. Loads now retry with backoff
  (5 s → 5 min) and on demand when a win screen, Hearts dialog or Luck Spin opens.
- Signed-in conversions ignored the old `redeemTimestamps`, so update day gave
  extra conversions. Fixed; the guest counter now also carries over at sign-in.
- `conversion_limit_daily` was ignored. It is now wired, and the Store
  fallback shows the configured limit instead of 2.
- The bonus could silently fail if config changed mid-ad, and one failure
  showed two toasts. Both fixed.

### Stage 0.5 — Remote Config
- [x] Validation and range clamping for every parameter (`RemoteConfigParser`, 12 tests).
- [x] Activation timing defined (economy: next game; flags and caps: immediate).
- [x] Offline/invalid/partial config falls back safely (tested).
- [x] `maintenance_mode`/`maintenance_message` block new games (never a running one).
- [x] `event_banner_*` banner on Home; `store_featured_item` FEATURED tag.
- [x] `new_theme_enabled` wired to a gated catalog list (empty this release).
- [x] Added `life_recovery_ad_enabled` and the cap/cooldown parameters.
- [x] Parameters, defaults, ranges and timing documented (`docs/REMOTE_CONFIG.md`).
- [ ] Test real-time update and app-restart behaviour on a device.
- [ ] Fill in parameter owners.

### Stage 1 — Wallet and conversions
- [x] Shared rules (`ConversionRules`) used by guest and Firestore paths.
- [x] Exactly N successful conversions per IST day, shared; rejections don't count (tested).
- [x] Midnight-IST boundary computed exactly (tested against the platform calendar).
- [x] Firestore conversions atomic in one transaction with the ledger entry.
- [x] Duplicate taps/retries can't convert twice (request-id idempotency, button lock).
- [x] Wallet transaction history (ledger) and **Wallet history** screen (Settings).
- [x] Offline errors handled without crashing.
- [ ] Multi-device and offline scenarios on real devices.
- Limitation: the IST day uses the device clock, so changing the date can reset
  the allowance. A client-only design can't prevent this.

### Stage 2 — Gameplay economy
- [x] Entry fee on every finished game; win: reward (net +30); loss: fee + exactly 1 Life;
  nothing deducted at the start (tested).
- [x] Settled exactly once (`game-<id>` idempotency key; duplicate callbacks are a no-op).
- [x] Offline / process death: settlements are queued on disk and retried on
  launch, sign-in, foreground and reconnect.
- [x] Remote Config changes can't alter a running game (rules captured at start).
- [x] No "−1 Life" shown on Start or Play again (plan §1.3); How to Play copy corrected.
- [ ] Device QA of all result paths.
- Note: quitting, resetting or the 30-minute timeout costs nothing, as the plan
  requires. Players can therefore abandon a game to avoid a loss; revisit if
  needed (see Continue/resume in §8).

### Stage 3 — Ads
- [x] 3A UMP consent before any ad request; Settings → Privacy options when required.
- [x] 3A reload after failure/dismissal; still Google test IDs.
- [x] 3B/3F Luck Spin: Home entry, wheel dialog, every outcome with its chance,
  reduced-motion support, the shown slot is the credited slot, idempotent claim.
- [x] 3C win bonus: offer and amount captured at game start, credited once.
- [x] 3D Life recovery: +1 Life (never above max), loading/cooldown/limit states.
- [x] 3E ≤ 2× rule.
- [x] 3G per-placement daily caps, cooldowns and a shared total, all in Remote Config.
- [ ] Configure the GDPR/US-states message in AdMob → Privacy & messaging.
- [ ] Replace test app/ad unit IDs with production IDs at release.
- [ ] 3H device QA: reward, dismiss, fail to load/show, backgrounding, slow network.

### Stage 4 — Store and cosmetics
- [x] Ownership separate from equipped state (guest and Firestore).
- [x] Purchases create permanent ownership exactly once (`purchase-<itemId>`),
  charge and grant atomic for accounts; guests self-repair after interruption.
- [x] Re-equipping is free; Unequip never removes ownership or charges.
- [x] Frost Cells Unequip restores default cells; theme Unequip returns to Classic Teal.
- [x] **Reset to default look** (theme, cells, avatar).
- [x] Robo avatar has artwork, can be equipped from the Store or Profile, and is
  only selectable when owned. The equipped avatar now syncs with the account.
- [x] UI states Buy / Need N more / Apply / Applied / Owned / Unequip (11 tests).
- [x] Guest → account migration merges ownership and equipped avatar.
- [ ] Device QA of all transitions.

## 5. Guest vs signed-in behaviour

| Data | Guest (on device) | Signed in (Firestore `users/{uid}`) |
|---|---|---|
| Coins, Gems | DataStore `wallet` | `coins`, `diamonds` |
| Conversion allowance | DataStore `conversion_date_ist` / `conversions_used_today` | `conversionDateIst` / `conversionsUsedToday` (legacy `redeemTimestamps` migrated) |
| Lives | DataStore `life` | `hearts`, `heartsRegenAnchor` |
| Wallet history | DataStore (last 100) | `users/{uid}/walletTransactions/{operationId}` |
| Pending settlements | DataStore `pending_settlements` (device) | same queue, applied when that account is signed in |
| Cosmetics | DataStore `store_inventory` | `ownedStoreItemIds`, `equippedBoardThemeId`, `equippedCellSkinId`, `equippedAvatarId` |
| Game history | Room | `users/{uid}/games` |
| Ad caps | DataStore `rewarded_ad_usage` (per device) | same (per device) |

On first sign-in, guest Coins/Gems (once, keyed by a migration id), conversions
used today, games and cosmetics are merged into the account, then cleared locally.

## 6. Firestore schema changes and migration risks

New fields and collections: `users/{uid}/walletTransactions/*` (ledger and
idempotency marker), `equippedAvatarId`, `conversionDateIst`,
`conversionsUsedToday`. Existing fields (`coins`, `diamonds`, `hearts`,
`ownedStoreItemIds`, …) are unchanged and still compatible.

| Risk | Mitigation |
|---|---|
| Players updating mid-day get a fresh allowance | Legacy `redeemTimestamps` counted on first read (both paths). |
| Double-crediting guest Coins if a sign-in is retried | Migration is idempotent via its migration id. |
| Ledger left behind after account deletion | Deletion now also removes `walletTransactions`. |
| Older app versions still write balances directly | Expected while both versions are live; no rules enforce otherwise. |

## 7. Known limitations of the client-only design

Without a backend or Firestore Security Rules, a modified app or a direct
Firestore client can change its own balances, and device-clock changes can reset
daily allowances and ad caps. The client code is atomic and idempotent against
honest failures (crashes, retries, offline), but it can't stop deliberate
tampering. Revisit before introducing anything with real-money value.

## 8. Still open

- Device QA for Stages 0.5, 1, 2, 3 and 4; release sign-off (Stage 7).
- AdMob privacy message configuration; production ad IDs; Play policy review.
- Stage 5 real-money purchases (skipped by decision).
- Firestore Security Rules (skipped by decision).
- UX carry-overs that need product decisions: localisation (strings to
  resources, plurals, RTL), hint/undo, continue/resume (Life-forfeit rule),
  daily reward/streak (amounts), onboarding (content).
- Product decisions: Luck Spin prize table, theme Gem price, combo theme/end date.
