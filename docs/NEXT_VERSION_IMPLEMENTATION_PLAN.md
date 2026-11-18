## Implementation stages

### Stage 0 — Baseline and compatibility
- [ ] Re-check the `next-version` branch and map wallet, game-result, life, store, ad, and Firebase code paths.
- [ ] Preserve existing Firestore fields (including `diamonds` if used for Gems), balances, and cosmetic ownership.
- [ ] Document guest/local versus authenticated/Firestore behavior and migration risks.
- [ ] Identify existing Firebase initialization and dependency configuration.

### Stage 0.5 — Firebase Remote Config foundation
- [ ] Add the Firebase Remote Config SDK using the existing Firebase BoM and Gradle version catalog.
- [ ] Create a centralized Remote Config manager/repository rather than scattering Firebase calls throughout ViewModels and screens.
- [ ] Define safe in-app default values so the game continues to work if Remote Config is unavailable.
- [ ] Implement background fetching and activation of configuration values.
- [ ] Implement Firebase real-time Remote Config updates for supported parameters.
- [ ] Define when changes activate: immediately, at the next game, or at the next app launch.
- [ ] Add configuration parameters for maintenance mode and maintenance message.
- [ ] Add configurable gameplay values:
    - Win reward
    - Loss Coin fee
    - Minimum Coins required to start
    - Daily conversion limit
    - Rewarded Ad bonus
- [ ] Add feature flags for Luck Ads, promotional banners, featured store items, and already-implemented themes.
- [ ] Add parameter validation and safe fallback behavior for missing or invalid values.
- [ ] Ensure Remote Config cannot directly grant currency, verify purchases, or bypass backend economy rules.
- [ ] Test offline behavior, fetch failures, real-time updates, app restarts, and invalid parameter values.
- [ ] Document all Remote Config parameter names, types, defaults, and intended behavior.

**Remote Config implementation rules:**
1. Remote Config may change values and enable or disable functionality already implemented in the installed app.
2. Remote Config must not download or execute new application code.
3. Do not use Remote Config to hide unfinished or unreviewed functionality during Google Play review and activate it afterward to evade review.
4. Wallet balances, currency transactions, purchase verification, conversion limits, and reward claims must remain protected by trusted backend logic.
5. Google Play Billing must remain the source of truth for real-money product purchases and prices.
6. Critical gameplay changes should activate at safe boundaries so an active match is not unexpectedly disrupted.

### Stage 1 — Wallet and conversions
- [ ] Standardize player-facing labels to Coins and Gems.
- [ ] Implement both conversion directions at 500:1.
- [ ] Enforce 3 successful conversions total per account per IST calendar day.
- [ ] Make balance changes and allowance accounting atomic and backend-authoritative.
- [ ] Add insufficient-balance, limit, confirmation, and transaction-history UI.
- [ ] Keep guest and authenticated behavior consistent.
- [ ] Read configurable conversion-related values from the centralized Remote Config layer where appropriate, while enforcing limits on the backend.

### Stage 2 — Gameplay economy
- [ ] Award exactly 50 Coins for a confirmed win, with no loss-fee deduction.
- [ ] Deduct 20 Coins and 1 Life only after a confirmed loss.
- [ ] Require at least 20 Coins before game start; do not deduct upfront.
- [ ] Make result processing idempotent across retries, restarts, and reconnects.
- [ ] Remove the -1 Life display from the Play button while retaining the deduction.
- [ ] Use Remote Config for supported gameplay tuning values, with safe defaults and backend validation.

### Stage 3 — Rewarded Ads and Luck Ads
- [ ] Grant an optional additional 50 Coins only after verified post-win ad completion.
- [ ] Always grant the base 50-Coin win reward regardless of ad availability or completion.
- [ ] Prevent duplicate bonus claims.
- [ ] Add Luck Ads with configurable Coins, Gems, and no-reward outcomes.
- [ ] Use Remote Config for configurable Luck Ad amounts and probabilities, subject to backend validation.
- [ ] Add a Remote Config feature flag to enable or disable Luck Ads.
- [ ] Handle load failures, early exits, duplicate callbacks, and offline states.

### Stage 4 — Store and cosmetics
- [ ] Separate permanent ownership from equipped state.
- [ ] Support equip, unequip, re-equip, and explicit default reset for themes, cell skins, and avatars.
- [ ] Ensure unequipping never removes ownership or charges again.
- [ ] Add correct Apply / Applied / Unequip actions.
- [ ] Restore the default cell appearance when Frost Cells are unequipped.
- [ ] Correct misleading currency labels and price units.
- [ ] Use Remote Config to control featured items, promotional visibility, and availability of already-implemented cosmetics where appropriate.

### Stage 5 — Real-money purchases
- [ ] Add Gems-only packages with a ₹100 minimum and the agreed ₹1 = 5 Gems rate.
- [ ] Do not sell standalone Coins.
- [ ] Add the ₹129 launch combo (10,000 Coins + 200 Gems + designated theme), once per account.
- [ ] Keep theme purchase at 500 Gems or ₹100.
- [ ] Integrate Google Play Billing; verify purchase tokens server-side before granting entitlements.
- [ ] Handle pending, cancelled, failed, restored, refunded, and duplicate purchase events idempotently.
- [ ] Use Remote Config only for supported promotional presentation and offer visibility; never treat it as purchase verification or as the authoritative source of billing prices.

### Stage 6 — Backend security and economy integrity
- [ ] Protect all currency-changing operations from client-side minting or tampering.
- [ ] Validate game outcomes, conversions, ad claims, and purchases in trusted backend logic.
- [ ] Use transaction identifiers/idempotency keys and auditable wallet history.
- [ ] Prevent guest-to-account synchronization from duplicating balances or entitlements.
- [ ] Ensure client-side Remote Config values cannot override backend security rules or authoritative transaction limits.
- [ ] Review Firebase Authentication, Firestore Security Rules, and any Cloud Functions or other trusted backend endpoints involved in economy operations.

### Stage 7 — QA and release
- [ ] Test win/loss across difficulties and all balance edge cases.
- [ ] Test conversion limits, midnight IST reset, and concurrent sessions.
- [ ] Test ad success, skip, failure, duplicate callbacks, and unavailable states.
- [ ] Test cosmetic purchase/equip/unequip/reset/re-equip and persistence.
- [ ] Test guest and signed-in flows, billing lifecycle, restore, and refunds.
- [ ] Test Remote Config defaults, fetch failures, real-time updates, activation timing, and offline behavior.
- [ ] Verify that Remote Config changes affect only supported functionality and do not bypass Google Play policies.
- [ ] Run unit/integration tests and Android release build checks.

## Final implementation order

1. Stage 0 — Baseline and compatibility.
2. Stage 0.5 — Firebase Remote Config foundation.
3. Stage 1 — Wallet and conversions.
4. Stage 2 — Gameplay economy.
5. Stage 3 — Rewarded Ads and Luck Ads.
6. Stage 4 — Store and cosmetics.
7. Stage 5 — Real-money purchases and launch combo.
8. Stage 6 — Backend security and economy integrity.
9. Stage 7 — QA and release.