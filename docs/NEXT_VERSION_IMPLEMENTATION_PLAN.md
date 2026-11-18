# Mines — Next Version Implementation Plan

**Repository:** `BhishanSharma/Mines`  
**Target branch:** `next-version`  
**Status:** Implementation plan

---

## 1. Overview

This document defines the implementation plan for the next version of Mines, covering the in-game economy, wallet, gameplay rewards, lives, advertisements, store, cosmetics, real-money purchases, Firebase Remote Config, and backend security.

The implementation must preserve existing player data and remain compatible with the current Firestore structure.

### 1.1 Currency system

- **Coins:** Gameplay currency.
- **Gems:** Premium currency.
- Existing Firestore fields such as `coins` and `diamonds` must remain compatible. The player-facing name for Diamonds will be Gems.

### 1.2 Conversion rates

- 500 Coins = 1 Gem.
- 1 Gem = 500 Coins.
- Players can complete a maximum of 3 successful conversions per day.
- The limit is shared across both conversion directions, not 3 per direction.
- The daily limit resets at midnight IST.
- Failed or rejected conversions do not count.
- Conversion balances and daily usage must be updated atomically.
- The backend must be authoritative for authenticated accounts.

### 1.3 Gameplay economy

- Starting a game does not deduct Coins.
- A player must have at least 20 Coins available before starting.
- Winning grants 50 Coins.
- Winning does not deduct the 20-Coin loss fee.
- After winning, the player can optionally watch a rewarded advertisement to earn an additional 50 Coins.
- The maximum win reward with the optional advertisement is 100 Coins.
- Losing deducts 20 Coins and 1 Life.
- The 20-Coin fee is deducted only after a confirmed loss.
- The Life deduction happens internally and must not be displayed as `-1 Life` on the Play button.
- Game results must never grant duplicate rewards or apply duplicate penalties.

### 1.4 Advertisements

- Rewarded advertisements after winning provide an optional additional 50 Coins.
- The base 50-Coin win reward must always be granted, even if the advertisement is skipped or unavailable.
- Luck Ads can produce one of three outcomes:
    - Coins reward.
    - Gems reward.
    - No reward.
- Luck Ad amounts and probabilities must be configurable.
- Rewards must be granted only after verified advertisement completion.

### 1.5 Real-money purchases

- Direct currency purchases sell Gems only.
- Minimum purchase amount: ₹100.
- Conversion rate for real-money purchases: ₹1 = 5 Gems.
- No standalone Coins purchases.
- Coins may be included in combo packages.
- A theme can be purchased for either 500 Gems or ₹100.
- Launch combo:
    - Price: ₹129.
    - 10,000 Coins.
    - 200 Gems.
    - One designated theme.
    - Available once per account.
- Google Play Billing must be used for Android purchases.
- Purchase verification must happen on a trusted backend before entitlements are granted.

### 1.6 Cosmetics

- Purchased cosmetics are permanently owned.
- Equipping, unequipping, and re-equipping owned cosmetics are free.
- Players can explicitly reset to the default theme, cell skin, and avatar.
- Frost Cells must be unequippable and return to the default cell appearance.
- Store UI must correctly display Apply, Applied, Owned, and Unequip states.
- Unequipping must never remove ownership or charge the player again.

---

## 2. Implementation stages

### Stage 0 — Baseline and compatibility

- [ ] Re-check the `next-version` branch and map wallet, game-result, life, store, ad, and Firebase code paths.
- [ ] Preserve existing Firestore fields, including `diamonds` if used for Gems.
- [ ] Preserve existing balances and cosmetic ownership.
- [ ] Document guest/local versus authenticated/Firestore behavior.
- [ ] Identify migration risks.
- [ ] Identify existing Firebase initialization and dependency configuration.
- [ ] Review existing wallet, life, store, and gameplay tests.
- [ ] Establish a baseline build before making changes.

### Stage 0.5 — Firebase Remote Config foundation

**Goal:** Allow supported configuration changes to be published through Firebase without requiring a new Play Store release for every minor adjustment.

#### SDK and architecture

- [ ] Add the Firebase Remote Config SDK using the existing Firebase BoM and Gradle version catalog.
- [ ] Create a centralized Remote Config manager/repository.
- [ ] Avoid scattering Firebase Remote Config calls throughout ViewModels and UI screens.
- [ ] Define safe in-app default values so the game continues to work if Remote Config is unavailable.
- [ ] Implement background fetching and activation of configuration values.
- [ ] Implement Firebase real-time Remote Config updates.
- [ ] Define activation timing for each parameter:
    - Immediately.
    - At the next game.
    - At the next app launch.
- [ ] Add parameter validation and safe fallback behavior for missing or invalid values.
- [ ] Handle network failures and offline operation.

#### Initial Remote Config parameters

| Parameter | Type | Purpose |
|---|---|---|
| `maintenance_mode` | Boolean | Temporarily disable gameplay and show a maintenance screen |
| `maintenance_message` | String | Change the maintenance message |
| `easy_win_reward` | Number | Configure Easy win reward |
| `medium_win_reward` | Number | Configure Medium win reward if difficulty-specific rewards are enabled |
| `hard_win_reward` | Number | Configure Hard win reward if difficulty-specific rewards are enabled |
| `loss_coin_fee` | Number | Configure the Coin fee after a confirmed loss |
| `minimum_start_coins` | Number | Configure the minimum Coins required to start |
| `conversion_limit_daily` | Number | Configure the daily conversion allowance |
| `rewarded_ad_bonus` | Number | Configure the optional post-win ad bonus |
| `luck_ad_enabled` | Boolean | Enable or disable Luck Ads |
| `luck_ad_coin_reward` | Number | Configure a Luck Ad Coin reward |
| `luck_ad_gem_reward` | Number | Configure a Luck Ad Gem reward |
| `luck_ad_coin_probability` | Number | Configure the probability of a Coin reward |
| `luck_ad_gem_probability` | Number | Configure the probability of a Gem reward |
| `luck_ad_no_reward_probability` | Number | Configure the probability of no reward |
| `event_banner_enabled` | Boolean | Show or hide an event banner |
| `event_banner_text` | String | Configure event banner text |
| `store_featured_item` | String | Select an already-implemented featured store item |
| `new_theme_enabled` | Boolean | Enable or disable an already-implemented theme |
| `feature_*_enabled` | Boolean | Feature flags for functionality already implemented in the app |

The initial economy defaults must preserve the agreed rules:

- Standard win reward: 50 Coins.
- Loss fee: 20 Coins.
- Minimum start balance: 20 Coins.
- Daily successful conversions: 3.
- Optional post-win rewarded ad bonus: 50 Coins.

#### Remote Config implementation rules

1. Remote Config may change values and enable or disable functionality already implemented in the installed app.
2. Remote Config must not download or execute new application code.
3. Do not use Remote Config to hide unfinished or unreviewed functionality during Google Play review and activate it afterward to evade review.
4. Wallet balances, currency transactions, purchase verification, conversion limits, and reward claims must remain protected by trusted backend logic.
5. Google Play Billing must remain the source of truth for real-money product purchases and prices.
6. Critical gameplay changes should activate at safe boundaries so an active match is not unexpectedly disrupted.
7. Remote Config must have safe local defaults.
8. Invalid configuration values must never cause crashes or corrupt player balances.
9. Remote Config availability must not be required for the game to start.
10. Document parameter names, types, defaults, and intended behavior.

#### Testing

- [ ] Test default values when no Remote Config values have been fetched.
- [ ] Test successful configuration fetching.
- [ ] Test real-time updates.
- [ ] Test activation timing.
- [ ] Test offline behavior.
- [ ] Test fetch failures.
- [ ] Test invalid parameter values.
- [ ] Test app restart behavior.
- [ ] Verify that Remote Config cannot bypass backend economy rules.

### Stage 1 — Wallet and conversions

#### Wallet

- [ ] Standardize player-facing labels to Coins and Gems.
- [ ] Preserve existing Firestore wallet fields and data.
- [ ] Keep guest/local wallet behavior consistent with authenticated wallet behavior.
- [ ] Ensure wallet balances update correctly after every transaction.
- [ ] Handle insufficient balances and network errors.
- [ ] Prevent duplicate wallet transactions.
- [ ] Add transaction history where appropriate.

#### Conversion

- [ ] Implement Coins-to-Gems conversion at 500:1.
- [ ] Implement Gems-to-Coins conversion at 1:500.
- [ ] Enforce 3 successful conversions total per account per IST calendar day.
- [ ] Reset the daily allowance at midnight IST.
- [ ] Do not count failed or rejected conversions.
- [ ] Make balance updates and conversion allowance accounting atomic.
- [ ] Prevent concurrent requests from exceeding the daily limit.
- [ ] Provide clear UI feedback for insufficient balance and exhausted daily allowance.
- [ ] Read supported configuration values through the centralized Remote Config layer, while enforcing conversion limits on the backend.

### Stage 2 — Gameplay economy

#### Game start

- [ ] Require at least 20 Coins before starting a game.
- [ ] Do not deduct Coins when a game starts.
- [ ] Do not deduct a Life when a game starts.
- [ ] Show an appropriate insufficient-Coins message when the player has fewer than 20 Coins.

#### Winning

- [ ] Award exactly 50 Coins for a confirmed win.
- [ ] Do not deduct the 20-Coin loss fee on a win.
- [ ] Do not deduct a Life on a win.
- [ ] Always grant the base win reward regardless of rewarded-ad availability.
- [ ] Make win reward processing idempotent.

#### Losing

- [ ] Deduct exactly 20 Coins after a confirmed loss.
- [ ] Deduct exactly 1 Life after a confirmed loss.
- [ ] Do not deduct the loss fee before the result is confirmed.
- [ ] Do not show `-1 Life` on the Play button.
- [ ] Ensure duplicate callbacks, retries, reconnects, and app restarts cannot apply duplicate penalties.

#### Remote Config integration

- [ ] Use Remote Config for supported gameplay tuning values.
- [ ] Keep safe defaults in the app.
- [ ] Validate authoritative rewards and deductions on the backend.
- [ ] Apply configuration changes at safe gameplay boundaries.

### Stage 3 — Rewarded Ads and Luck Ads

#### Post-win rewarded advertisement

- [ ] Offer an optional rewarded advertisement after a confirmed win.
- [ ] Grant an additional 50 Coins after verified ad completion.
- [ ] Ensure the player receives the base 50-Coin win reward even if the ad is skipped.
- [ ] Do not grant the bonus if the advertisement fails or is not completed.
- [ ] Prevent duplicate bonus claims.
- [ ] Handle unavailable advertisements gracefully.
- [ ] Use Remote Config for the supported bonus amount.

#### Luck Ads

- [ ] Add a Luck Ad feature.
- [ ] Support Coin rewards.
- [ ] Support Gem rewards.
- [ ] Support no-reward outcomes.
- [ ] Configure reward amounts through Remote Config.
- [ ] Configure outcome probabilities through Remote Config.
- [ ] Validate probability configuration and ensure the total probability is valid.
- [ ] Add a Remote Config feature flag to enable or disable Luck Ads.
- [ ] Grant rewards only after verified ad completion and trusted validation.
- [ ] Prevent duplicate claims.
- [ ] Handle early exits, failed loads, duplicate callbacks, and offline states.

### Stage 4 — Store and cosmetics

#### Ownership

- [ ] Separate permanent ownership from equipped state.
- [ ] Preserve existing owned cosmetic items.
- [ ] Ensure purchased items remain owned after unequipping.
- [ ] Ensure re-equipping an owned item is free.
- [ ] Prevent duplicate charges for already-owned items.

#### Board themes

- [ ] Support purchasing board themes.
- [ ] Support applying owned themes.
- [ ] Support unequipping the current theme.
- [ ] Support resetting to the default theme.
- [ ] Add the missing unequip functionality to the repository and data-store layers.

#### Cell skins

- [ ] Support purchasing cell skins.
- [ ] Support applying owned cell skins.
- [ ] Support unequipping cell skins.
- [ ] Ensure Frost Cells can be unequipped.
- [ ] Restore the default cell appearance when Frost Cells are unequipped.

#### Avatars

- [ ] Integrate avatar ownership with the actual equipped avatar state.
- [ ] Support purchasing avatars.
- [ ] Support applying owned avatars.
- [ ] Support unequipping and resetting to the default avatar.
- [ ] Persist avatar state correctly for guest and authenticated users.

#### Store UI

- [ ] Display Buy for unowned affordable items.
- [ ] Display the price and insufficient-currency requirement when the player cannot afford an item.
- [ ] Display Apply for owned and unequipped items.
- [ ] Display Applied for currently equipped items.
- [ ] Provide an Unequip action where applicable.
- [ ] Display Owned for cosmetics that are owned but cannot be applied directly from the store.
- [ ] Correct misleading currency labels and price units.
- [ ] Ensure store state refreshes correctly after purchase, equip, unequip, and reset.

#### Remote Config integration

- [ ] Support featured store items.
- [ ] Support promotional visibility.
- [ ] Support availability flags for already-implemented cosmetics.
- [ ] Do not use Remote Config to introduce unimplemented cosmetic functionality.

### Stage 5 — Real-money purchases and launch combo

#### Gems purchases

- [ ] Add Gems-only real-money purchase packages.
- [ ] Enforce the ₹100 minimum purchase amount.
- [ ] Use the agreed rate of ₹1 = 5 Gems.
- [ ] Do not offer standalone Coins purchases.
- [ ] Display purchase products and prices using Google Play Billing product information.

#### Theme purchases

- [ ] Allow theme purchases for 500 Gems.
- [ ] Allow theme purchases for ₹100 through an appropriate Google Play product.
- [ ] Ensure the same theme cannot be purchased repeatedly if it is permanently owned.

#### Launch combo

- [ ] Add the launch combo priced at ₹129.
- [ ] Include 10,000 Coins.
- [ ] Include 200 Gems.
- [ ] Include one designated theme.
- [ ] Limit the launch combo to one purchase per account.
- [ ] Define the designated theme before release.
- [ ] Define the offer end date before release.
- [ ] Ensure the combo cannot be claimed more than once through retries or multiple devices.

#### Billing integration

- [ ] Integrate Google Play Billing.
- [ ] Verify purchase tokens on a trusted backend before granting entitlements.
- [ ] Handle pending purchases.
- [ ] Handle successful purchases.
- [ ] Handle cancelled purchases.
- [ ] Handle failed purchases.
- [ ] Handle restored purchases.
- [ ] Handle refunds and revoked purchases.
- [ ] Prevent duplicate purchase grants.
- [ ] Provide appropriate purchase feedback and error handling.
- [ ] Ensure purchase history and ownership remain consistent across devices.

#### Remote Config integration

- [ ] Use Remote Config for supported promotional presentation and offer visibility.
- [ ] Do not use Remote Config as purchase verification.
- [ ] Do not treat Remote Config as the authoritative source of billing prices.
- [ ] Ensure offer eligibility and one-time purchase enforcement are handled by trusted backend logic.

### Stage 6 — Backend security and economy integrity

- [ ] Protect all currency-changing operations from client-side minting or tampering.
- [ ] Validate game outcomes in trusted backend logic.
- [ ] Validate conversion requests and daily limits on the backend.
- [ ] Validate rewarded-ad claims before granting rewards.
- [ ] Verify Google Play purchase tokens on the backend.
- [ ] Use transaction identifiers and idempotency keys.
- [ ] Maintain auditable wallet transaction history.
- [ ] Prevent duplicate rewards and penalties.
- [ ] Prevent guest-to-account synchronization from duplicating balances or entitlements.
- [ ] Ensure concurrent wallet requests cannot create inconsistent balances.
- [ ] Review Firebase Authentication configuration.
- [ ] Review Firestore Security Rules.
- [ ] Review Cloud Functions or other trusted backend endpoints involved in economy operations.
- [ ] Ensure client-side Remote Config values cannot override backend security rules or authoritative transaction limits.
- [ ] Preserve existing player data during all migrations.

### Stage 7 — QA and release

#### Gameplay tests

- [ ] Test wins across all difficulties.
- [ ] Test losses across all difficulties.
- [ ] Verify the exact 50-Coin win reward.
- [ ] Verify the exact 20-Coin loss fee.
- [ ] Verify the Life deduction occurs only after a confirmed loss.
- [ ] Verify the minimum 20-Coin start requirement.
- [ ] Verify that no Coins are deducted at game start.
- [ ] Verify that no duplicate rewards or penalties occur.

#### Wallet tests

- [ ] Test Coins-to-Gems conversion.
- [ ] Test Gems-to-Coins conversion.
- [ ] Test insufficient balances.
- [ ] Test the 3-conversion daily limit.
- [ ] Test midnight IST reset.
- [ ] Test failed conversions.
- [ ] Test concurrent sessions.
- [ ] Test guest and authenticated wallet behavior.
- [ ] Test persistence after app restart.

#### Advertisement tests

- [ ] Test successful rewarded ads.
- [ ] Test skipped advertisements.
- [ ] Test failed advertisements.
- [ ] Test unavailable advertisements.
- [ ] Test duplicate callbacks.
- [ ] Test Luck Ad Coin rewards.
- [ ] Test Luck Ad Gem rewards.
- [ ] Test Luck Ad no-reward outcomes.
- [ ] Verify that rewards cannot be claimed without verified ad completion.

#### Store and cosmetic tests

- [ ] Test cosmetic purchases.
- [ ] Test already-owned items.
- [ ] Test equip and unequip.
- [ ] Test re-equipping owned items.
- [ ] Test resetting to defaults.
- [ ] Test Frost Cells reset behavior.
- [ ] Test avatar persistence.
- [ ] Test guest and authenticated cosmetic ownership.
- [ ] Test store UI states.

#### Billing tests

- [ ] Test Gems package purchases.
- [ ] Test the ₹100 minimum.
- [ ] Test theme purchases.
- [ ] Test the ₹129 launch combo.
- [ ] Test one-time purchase enforcement.
- [ ] Test pending, cancelled, failed, restored, and refunded purchases.
- [ ] Test duplicate purchase callbacks.
- [ ] Test backend purchase verification.

#### Remote Config tests

- [ ] Test default values.
- [ ] Test successful fetching.
- [ ] Test real-time updates.
- [ ] Test activation timing.
- [ ] Test offline behavior.
- [ ] Test fetch failures.
- [ ] Test invalid parameter values.
- [ ] Test app restart behavior.
- [ ] Test maintenance mode.
- [ ] Test feature flags.
- [ ] Verify that configuration changes affect only supported functionality.
- [ ] Verify that Remote Config cannot bypass backend security or Google Play policies.

#### Release checks

- [ ] Run unit tests.
- [ ] Run integration tests.
- [ ] Run Android release build checks.
- [ ] Verify Firestore compatibility.
- [ ] Verify Firebase Security Rules.
- [ ] Verify billing configuration.
- [ ] Verify Remote Config defaults and production values.
- [ ] Confirm no existing player balances or cosmetic ownership have been lost.
- [ ] Complete regression testing before release.

---

## 3. Final implementation order

1. **Stage 0 — Baseline and compatibility**
2. **Stage 0.5 — Firebase Remote Config foundation**
3. **Stage 1 — Wallet and conversions**
4. **Stage 2 — Gameplay economy**
5. **Stage 3 — Rewarded Ads and Luck Ads**
6. **Stage 4 — Store and cosmetics**
7. **Stage 5 — Real-money purchases and launch combo**
8. **Stage 6 — Backend security and economy integrity**
9. **Stage 7 — QA and release**

---

## 4. Decisions still to be finalized

The following decisions remain configurable and must be finalized before the relevant feature is released.

- Luck Ad Coin reward amount.
- Luck Ad Gem reward amount.
- Luck Ad probability distribution.
- Exact Gem package denominations beyond the ₹100 minimum.
- Designated theme included in the launch combo.
- Launch combo offer end date.
- Whether win rewards remain the same across all difficulties or become difficulty-specific.
- Final Remote Config parameter defaults and production values.

---

## 5. Engineering guardrails

1. Never reset existing player balances.
2. Never remove existing cosmetic ownership.
3. Preserve Firestore schema compatibility unless a migration is explicitly designed and tested.
4. Do not trust the Android client alone for currency grants, ad rewards, purchase validation, or conversion limits.
5. Make wallet mutations atomic and idempotent.
6. Keep guest and authenticated behavior consistent.
7. Use Remote Config only for supported configuration values and functionality already implemented in the installed app.
8. Always provide safe local defaults for Remote Config.
9. Never use Remote Config to bypass Google Play review or policy requirements.
10. Verify all real-money purchases through Google Play Billing and trusted backend validation.
11. Implement changes in small stages.
12. Test each stage before proceeding to the next.
13. Mark tasks complete only after implementation and verification.
14. Work only on the `next-version` branch. Do not modify `master` as part of this implementation.

---

## 6. Completion criteria

The next version is considered ready when:

- The agreed Coins and Gems economy works correctly.
- Conversions enforce the shared daily limit and IST reset.
- Wins and losses grant or deduct the correct amounts exactly once.
- Lives are deducted only after confirmed losses.
- Rewarded Ads and Luck Ads grant only verified rewards.
- Cosmetics remain permanently owned and can be equipped or unequipped freely.
- Real-money purchases are processed and verified securely.
- The launch combo is restricted to one purchase per account.
- Remote Config supports safe configuration updates without requiring an app release for every supported minor change.
- Backend security prevents client-side manipulation of balances and entitlements.
- Existing player data remains intact.
- All relevant tests and release checks pass.