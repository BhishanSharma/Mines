# Next Version Implementation Plan

**Repository:** `BhishanSharma/Mines`\
**Target branch:** `next-version`\
**Status:** Planning

## Overview --- what's new

-   **Coins** are the gameplay currency. A win grants Coins; a loss
    costs Coins and one Life.
-   **Gems** are the premium currency. Players can exchange Coins and
    Gems in either direction.
-   **Conversion limit:** 3 successful conversions per day in total
    across both directions.
-   **Direct real-money currency purchases** sell Gems only. There is no
    standalone Coins purchase.
-   **Combo packages** may include Coins, Gems, and a cosmetic item.
-   **Cosmetics** remain owned permanently and can be equipped,
    unequipped, and re-equipped for free.
-   **Rewarded ads** can double a winning game's Coin reward or provide
    a random Luck reward.
-   The Play button must not display a `-1 Life` indicator. Life is
    still deducted internally after a loss.

## Agreed economy rules

  -----------------------------------------------------------------------
  Action                              Result
  ----------------------------------- -----------------------------------
  Start a game                        Require at least 20 Coins
                                      available; do not deduct Coins yet

  Win a game                          Award 50 Coins; do not charge the
                                      20-Coin loss amount

  Win and complete the double-reward  Award 50 base Coins plus 50 bonus
  ad                                  Coins (100 total)

  Lose a game                         Deduct 20 Coins and 1 Life

  Coins → Gems                        500 Coins = 1 Gem

  Gems → Coins                        1 Gem = 500 Coins

  Daily conversion allowance          3 successful conversions total
                                      across both directions

  Conversion reset                    Midnight IST

  Direct currency purchase            Gems only; minimum purchase ₹100

  Direct purchase rate                ₹1 = 5 Gems

  Standalone Coins purchase           Not offered

  Theme purchase                      500 Gems or ₹100

  Launch combo                        ₹129 for 10,000 Coins + 200 Gems +
                                      1 designated theme

  Launch combo eligibility            Once per account, as a launch offer
  -----------------------------------------------------------------------

**Gameplay interpretation:** The 20-Coin amount is a loss penalty, not
an upfront entry charge. Check that the player has at least 20 Coins
before starting a game so a loss can be paid. Deduct the penalty only
after a confirmed loss. A win receives the full 50-Coin reward.

**Conversion accounting:** Count only successful conversions. A failed
or rejected conversion must not consume an attempt. Enforce the
allowance atomically on the backend so concurrent requests cannot exceed
it.

## Implementation stages

A stage is complete only after its code, tests, and relevant UI behavior
have been checked.

### Stage 0 --- Baseline and design review

-   [ ] Re-check the current `next-version` branch and identify wallet,
    game-result, life, store, and ad-related code paths.
-   [ ] Map guest/local wallet behavior and authenticated/Firestore
    wallet behavior.
-   [ ] Identify existing data fields and preserve backward
    compatibility (including the existing Firestore `diamonds` field if
    it remains the stored name for Gems).
-   [ ] Document migration and security risks before changing wallet
    behavior.

**Completion criteria:** Implementation touchpoints and compatibility
requirements are documented; no existing wallet balances or cosmetic
ownership are lost.

### Stage 1 --- Wallet and currency conversion

-   [ ] Standardize player-facing terminology as **Coins** and **Gems**,
    preserving compatible persisted field names where necessary.
-   [ ] Implement Coins → Gems conversion at 500:1.
-   [ ] Implement Gems → Coins conversion at 1:500.
-   [ ] Enforce a maximum of 3 successful conversions per account per
    IST calendar day, shared across both directions.
-   [ ] Reset the daily allowance at midnight IST.
-   [ ] Make balance updates and conversion-attempt accounting atomic.
-   [ ] Add insufficient-balance handling, confirmation UI, and
    conversion-limit messaging.
-   [ ] Add transaction history entries for both conversion directions.
-   [ ] Cover guest local storage and authenticated Firestore storage
    consistently.

**Completion criteria:** Both directions work at the agreed rate; no
more than 3 successful conversions can occur per account per IST day,
including concurrent attempts.

### Stage 2 --- Gameplay rewards, loss penalty, and lives

-   [ ] Award 50 Coins for each confirmed win.
-   [ ] Do not deduct 20 Coins from a winning game.
-   [ ] Deduct 20 Coins and 1 Life after a confirmed loss.
-   [ ] Do not deduct the 20 Coins at game start.
-   [ ] Require at least 20 Coins before a player starts a game, since a
    loss must be payable.
-   [ ] Ensure retries, restarts, reconnects, and repeated result events
    cannot duplicate or bypass a reward or loss penalty.
-   [ ] Remove the `-1 Life` indicator from the Play button without
    removing the underlying life deduction.
-   [ ] Show clear win/loss and balance feedback.

**Completion criteria:** A win grants exactly 50 Coins and no life or
loss-fee deduction; a loss deducts exactly 20 Coins and 1 Life exactly
once; the Play button does not show `-1 Life`.

### Stage 3 --- Rewarded ads and Luck Ads

-   [ ] Add an optional post-win rewarded ad that grants an additional
    50 Coins after verified completion.
-   [ ] Ensure the base 50-Coin win reward is granted even when the ad
    is skipped, unavailable, or not completed.
-   [ ] Prevent duplicate bonus claims for the same game result.
-   [ ] Add Luck Ads with three possible outcomes: Coins, Gems, or no
    reward.
-   [ ] Keep Luck Ad reward amounts and probabilities configurable;
    finalize and document actual values before release.
-   [ ] Grant ad rewards only after a trusted ad-network completion
    callback and server-side validation where supported.
-   [ ] Handle ad load failures, early exits, duplicate callbacks, and
    offline states safely.

**Completion criteria:** Base and bonus rewards are each granted at most
once; no reward is granted for an incomplete or unverified ad; Luck Ads
can produce all three configured outcomes.

### Stage 4 --- Store, cosmetics, and unequip/reset behavior

-   [ ] Keep cosmetic ownership separate from equipped state.
-   [ ] Allow owned themes, cell skins, and avatars to be equipped and
    unequipped.
-   [ ] Provide an explicit reset to the default board theme, default
    cell skin, and default avatar.
-   [ ] Keep unequipped items owned; never charge again to re-equip
    them.
-   [ ] Add clear **Apply / Applied / Unequip** actions as appropriate.
-   [ ] Ensure the Frost Cell skin can be removed and the default cell
    appearance is restored.
-   [ ] Ensure local guest and Firestore-backed store state behave
    consistently.
-   [ ] Fix misleading store price/unit labels so the UI matches the
    currency actually charged.

**Completion criteria:** Every supported cosmetic can be reset to its
default state; ownership persists after unequipping and across app
restarts/sign-in synchronization.

### Stage 5 --- Real-money store and launch combo

-   [ ] Add direct Gems-only purchase packages, with a minimum purchase
    of ₹100.
-   [ ] Use the agreed base rate of ₹1 = 5 Gems; do not offer standalone
    Coins purchases.
-   [ ] Allow Coins to be included in a combo package only.
-   [ ] Add the launch combo: ₹129 for 10,000 Coins, 200 Gems, and the
    designated theme.
-   [ ] Enforce once-per-account launch-combo eligibility.
-   [ ] Keep the separate theme purchase option: 500 Gems or ₹100.
-   [ ] Clearly show package contents, price, and one-time/launch
    eligibility before purchase.
-   [ ] Integrate Google Play Billing and verify purchases before
    crediting currency or granting cosmetics.
-   [ ] Make purchase processing idempotent so a purchase cannot be
    credited twice.
-   [ ] Handle pending, cancelled, failed, restored, and refunded
    purchases.

**Completion criteria:** Only verified purchases grant entitlements;
direct currency purchases grant Gems only; the combo grants its listed
Coins, Gems, and theme once per eligible account.

### Stage 6 --- Backend security and economy integrity

-   [ ] Move or protect currency-changing operations so clients cannot
    freely mint Coins or Gems.
-   [ ] Validate game outcomes and apply rewards/penalties through
    trusted logic.
-   [ ] Validate conversion balances, rates, daily allowance, and
    transaction identity atomically.
-   [ ] Validate rewarded-ad claims and purchase tokens server-side
    where supported.
-   [ ] Add idempotency keys or equivalent transaction identifiers for
    game results, ads, conversions, and purchases.
-   [ ] Record auditable wallet transactions with reason, amount,
    currency, timestamp, and source.
-   [ ] Ensure guest-to-authenticated account migration cannot duplicate
    balances or entitlements.

**Completion criteria:** Client-side tampering, duplicate events,
retries, and concurrent requests cannot create currency or duplicate
entitlements.

### Stage 7 --- QA, regression, and release readiness

-   [ ] Test win/loss flows for every difficulty.
-   [ ] Test zero, insufficient, exact, and high Coin/Gem balances.
-   [ ] Test both conversion directions, the third allowed conversion,
    the fourth rejected conversion, and the next-day reset in IST.
-   [ ] Test conversion attempts from multiple devices/sessions and
    concurrent requests.
-   [ ] Test rewarded-ad success, skip, failure, duplicate callback, and
    unavailable-ad scenarios.
-   [ ] Test buying, equipping, unequipping, resetting, and re-equipping
    every cosmetic type.
-   [ ] Test guest and signed-in wallets, app restarts, and account
    synchronization.
-   [ ] Test purchase success, cancellation, pending state, restore,
    refund, and duplicate verification.
-   [ ] Run relevant unit tests, integration tests, and Android build
    checks.
-   [ ] Review UI copy and currency labels for consistency.

**Completion criteria:** All critical economy and purchase paths pass;
existing users' balances and owned items remain intact; release build
and regression checks pass.

## Decisions intentionally left configurable / to finalize

-   Luck Ad reward amounts and probability distribution.
-   Exact Google Play Gem package denominations beyond the ₹100 minimum
    and agreed rate.
-   Which theme is included in the launch combo and the offer's end
    date.
-   Whether future promotional offers or bonus Gems are introduced.

These are not blockers for the initial wallet and gameplay stages. Do
not invent final Luck Ad odds or additional purchase bonuses during
implementation.

## Engineering guardrails

1.  Preserve existing user balances and ownership; do not reset
    production wallet data.
2.  Keep the existing Firestore `diamonds` field compatible unless a
    deliberate migration is designed and tested.
3.  Do not trust the Android client alone for currency grants, ad
    rewards, or purchase validation.
4.  Make wallet mutations atomic and idempotent.
5.  Keep UI display changes separate from underlying gameplay rules:
    hide the Play-button life indicator, but still deduct a Life on a
    loss.
6.  Implement and verify one stage at a time; update this file's
    checkboxes only after the stage's completion criteria are met.
