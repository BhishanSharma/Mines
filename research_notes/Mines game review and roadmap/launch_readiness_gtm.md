# Mines: Google Play Launch Readiness and Go-to-Market (as of 2026-11-06)

Scope: what Play requires of com.genoma.mines (Kotlin/Compose, Firebase Auth/Firestore/Analytics, Google Sign-In, virtual coins, hearts, cosmetic store, tournament screen, 3-person team), plus a launch and growth plan. Research budget was about 17 tool calls. Items marked "UNVERIFIED THIS SESSION" come from background knowledge and need checking against the linked Google page before anyone relies on them.

## Play requirements checklist with dates, and repo gaps

### Takeaway
On the technical side the repo is ahead: targetSdk 37 is past the API 36 bar that applied from Aug 31 2026, and in-app account deletion already exists. The real blockers are process and policy. If the account is a personal one created after Nov 13 2023, it needs a 12-tester, 14-day closed test. It also needs a web account-deletion URL and privacy policy, a Data safety form that matches Firebase's data collection, an IARC rating, and tournament and coin mechanics designed so nothing bought with money can win a prize of real-world value.

### Cited Findings
**Repo snapshot (read directly, read-only):**
- `app/build.gradle.kts` sets applicationId/namespace `com.genoma.mines`, compileSdk 37, **targetSdk 37**, minSdk 24, versionCode 1, versionName "1.0". The release build has `optimization { enable = false }`, so R8 shrinking and obfuscation are off. The file has no signing config. (Repo: C:\Users\hp\AndroidStudioProjects\Mines\app\build.gradle.kts)
- `AndroidManifest.xml` declares only the VIBRATE, INTERNET and ACCESS_NETWORK_STATE permissions. It sets `allowBackup="true"` with data extraction rules and has a single exported MainActivity. (Repo: app/src/main/AndroidManifest.xml)
- Firebase Auth and Analytics are in the app module, and Firestore is used in feature modules (store, game). A grep found **no AdMob/ads SDK, no Play Billing, and no Play Integrity**. (Repo grep)
- **In-app account deletion exists.** SettingsScreen.kt has a "Delete Account" row and a confirmation dialog ("permanently deletes your account, your game..."). GoogleAuthManager.deleteAccount() calls FirebaseUser.delete() and retries after reauthentication. (Repo: feature/settings/.../SettingsScreen.kt, feature/auth/.../GoogleAuthManager.kt)
- A grep of Kotlin sources found **no privacy-policy link**. (Repo grep)
- Recent commit "Handle camera cutout window insets per screen" suggests edge-to-edge/inset work is in progress. (git log)

**Target API level**
- Since **Aug 31 2026**, new apps and app updates must target Android 16 (API 36). Existing apps must target API 35 or higher to stay available to new users on newer OS versions. Apps targeting API 34 or lower are only available on devices at or below their target. — [Android Developers: Target API requirements](https://developer.android.com/google/play/requirements/target-sdk); [Play Console Help 11926878](https://support.google.com/googleplay/android-developer/answer/11926878)
- A secondary source mentions a one-time extension to Nov 1 (2026) that could be requested. It is not confirmed from a Google page. — [DEV Community migration checklist](https://dev.to/mr_manushukla/android-target-api-36-is-due-aug-31-2026-api-37-is-next-the-play-store-migration-checklist-2f9i) (secondary)

**Closed testing for new personal accounts**
- Personal developer accounts created after **Nov 13 2023** must run a closed test with **at least 12 testers opted in continuously for at least 14 days** before they can apply for production access. Google tells developers to make sure testers stay opted in. — [Play Console Help 14151465](https://support.google.com/googleplay/android-developer/answer/14151465)
- The minimum was 20 testers until it was reportedly lowered to 12 on **Dec 11 2024**. Organization accounts are reported to be exempt. — [theclosedtest guide](https://theclosedtest.neerajlovecyber.com/blog/google-play-12-testers-policy); [Choicely](https://www.choicely.com/blog/google-play-12-tester-rule) (secondary; organization exemption not confirmed on the Google page in this session)
- Developers report production-access rejections even after 14 days of testing, typically over low tester engagement. — [DEV: rejected after 14 days](https://dev.to/testerscommunity/google-play-rejected-my-app-after-14-days-of-testing-here-is-what-i-did-wrong-3c21) (anecdotal)

**Android developer verification**
- **Aug 2026:** developer APIs, limited-distribution accounts (students/hobbyists, up to 20 devices, no ID or fee) and an advanced "power user" flow for installing unverified apps all launched. **Sep 30 2026:** protections took effect for installs on certified Android 7+ devices in **Brazil, Indonesia, Singapore and Thailand**. **2027:** global expansion, with no specific date given. "Google Play automatically registers 99% of apps", while any remaining apps and apps distributed outside Play must be registered manually in Play Console. — [Android Developers: Developer verification](https://developer.android.com/developer-verification); [Play Console guide](https://developer.android.com/developer-verification/guides/google-play-console)
- Earlier reporting had global enforcement in Sept 2026. The June 2026 revision changed that to the four-country start. — [Android Authority timeline](https://www.androidauthority.com/android-sideloading-changes-timeline-3679204/)

**16 KB memory page size**
- Updates without 16 KB support were blocked from **Nov 1 2025**, with an extension available to **May 31 2026** (one forum quotes May 30). — [Adalo forum quoting Google email](https://forum.adalo.com/t/important-republish-your-android-apps-by-november-1-2025/59040); [Broadcom KB](https://knowledge.broadcom.com/external/article/411558/guidance-on-google-plays-16-kb-page-size.html)
- An unverified Reddit claim says the deadline was pushed to Feb 1 2027. No Google source was found. — [Reddit snapshot](https://reddit.sentinel-team.org/posts/1tqd2t8/snapshots/2026-05-29T08%3A38%3A19.794864Z)

**Edge-to-edge**
- Apps targeting Android 16 can no longer opt out of edge-to-edge. The opt-out flag is deprecated and disabled. — [Yahoo Tech](https://tech.yahoo.com/phones/articles/android-16-force-apps-transparent-190010571.html); [appsops.store](https://appsops.store/news/android-16-edge-to-edge-screenshot-aso-impact-2026) (secondary)

**Account deletion and Data safety**
- Apps that let users create accounts must offer deletion both in the app and through a **web link** declared in the Data safety form. The web resource must work without reinstalling the app, must name the app or developer, and can be a link, a form or a support email. Deletion questions became mandatory **Dec 7 2023**, with extensions to **May 31 2024**. — [Play Console Help 13327111](https://support.google.com/googleplay/android-developer/answer/13327111); [Android Developers Blog, Apr 2023](https://android-developers.googleblog.com/2023/04/giving-people-more-control-over-their-data.html)

**Gambling, contests and virtual currency** (the policy that matters most for coins and tournaments)
- Apps that are not licensed gambling apps may not let users "wager, stake, or participate using real money (**including in-app items purchased with money**) to obtain a prize of real world monetary value." A listed violation is "Games that accept money in exchange for an opportunity to win a physical or monetary prize." — [Play Policy: Real-Money Gambling, Games, and Contests (9877032)](https://support.google.com/googleplay/android-developer/answer/9877032)
- Loyalty programs in game apps: purchase-linked rewards "may only be awarded and redeemed on a fixed ratio basis", with the ratio disclosed, and they "may not be wagered, awarded or exponentiated by game performance or chance-based outcomes." — same source
- Gambling ads must not offer simulated gambling such as social casino or virtual slots. — same source
- The policy says nothing directly about free-entry tournaments with only virtual prizes. — same source (gap)

**Is the app already on Play?**
- `play.google.com/store/apps/details?id=com.genoma.mines` returned **HTTP 404** on 2026-11-06, so there is no public listing. — [Play listing URL](https://play.google.com/store/apps/details?id=com.genoma.mines)

### Inferences
- **targetSdk 37 already clears the API 36 bar.** The trade-off is that the app takes on Android 17 behavior changes now, so test on API 37 images. Edge-to-edge is mandatory from API 36 onward, and the recent inset commits are the right move. Screenshots should show edge-to-edge UI.
- **16 KB:** the app appears to be pure Kotlin/Compose plus Firebase, so the risk is low. The team should still check the release AAB with the Play Console/App Bundle Explorer 16 KB check or Android Studio's APK Analyzer, because any transitive native .so files must be aligned.
- **Developer verification:** a Play-only app is almost certainly auto-registered. Confirm the identity verification status in Play Console. This only becomes work if they distribute APKs outside Play, for example to testers.
- **Tournament and coin design rules:**
  1. Tournaments are free to enter, or entered with coins earned only through gameplay.
  2. Prizes are never of real-world value: no cash, gift cards or crypto, and nothing redeemable outside the app.
  3. If coins can be bought, purchased coins must not be used as tournament entry fees for prizes that could be construed as real-world value.
  4. No chance-based payouts tied to purchases.
  5. Avoid casino styling such as "cash out" or multiplier ladders. These keep the game well clear of 9877032.
- **Name collision with gambling:** "Mines" is the name of a Stake Original casino game and many clones. A "pick tiles, avoid bombs, cash out" UI could get flagged by reviewers or confused by users, and it also hurts ASO.
- **Release config:** turning on R8 (minify + resource shrink) and adding signing via Play App Signing is advisable. Unrelated to compliance, but `allowBackup=true` should be reviewed so session or auth tokens are not backed up.
- **Remaining checklist items. UNVERIFIED THIS SESSION; check each linked Play Console Help page:**
  - **Privacy policy URL**, required in the Play Console store listing and linked in-app. It is required for any app handling personal data, and Google Sign-In collects email and name.
  - **Data safety form**: declare email and name (account), user IDs, app interactions and diagnostics from Firebase Analytics/Crashlytics, purchase history if Billing is added, encryption in transit, and the deletion web link.
  - **IARC content rating questionnaire**: answer the simulated-gambling question honestly. A minesweeper-type game is not gambling, but a casino-like presentation could push the rating up.
  - **Ads declaration**: currently "No ads". Update it if AdMob is added.
  - **Target audience and content**: choosing 13+ or 18+ avoids the Families Policy requirements (Families-certified ad SDKs, no Google Sign-In for kids, and so on). Including under-13s triggers them.
  - **App access**: give reviewers a test account or instructions, since a sign-in wall can cause a rejection.
  - **Financial features / government apps / health declarations**: answer N/A.
  - **Play Integrity API**: not mandatory. It is recommended to protect Firestore coin balances and leaderboards from tampered clients, together with Firebase App Check (which can use the Play Integrity provider).
  - **Play Billing Library**: if coins or cosmetics are sold, Play Billing is required and must be a current version. Play's Billing Library deprecation schedule applies.

### Gaps
- Fetches of the official pages for IARC, Families Policy, Ads declaration, Data safety and Play Integrity were not done within budget. Those items are listed above as unverified.
- Whether the Nov 1 2026 target-API extension really existed and whether it applies to new apps. It most likely doesn't matter, since targetSdk is 37.
- Whether the 16 KB deadline was extended past May 2026. The only source is Reddit.
- The account type (personal or organization) and creation date of the team's Play Console account are unknown, so it is unknown whether the 12-tester rule applies.
- The policy text does not say whether free tournaments with virtual-only prizes need official rules.

## ASO, name collision, and naming alternatives

### Takeaway
"Mines" as a standalone title collides head-on with the crypto-casino genre (Stake Mines and its clones). It is a poor keyword to compete for and risks being associated with gambling. Rename to a distinctive brand plus a descriptive "Minesweeper" keyword.

### Cited Findings
- "Mines" is a Stake Original casino game based on Minesweeper: pick gems without hitting bombs, provably fair, crypto wagering. Stake has no official Play app. It is browser-based, while many unrelated apps use the "Stake Casino" / Mines naming. — [win.gg](https://win.gg/crypto-casinos/mines/); [igamingfuture](https://igamingfuture.com/stake/mines/); [AppGoblin listing examples](https://appgoblin.info/apps/com.dih.sdkyb.skubi) (affiliate and SEO-quality sources; used only to show the name collision)
- Search results for "Mines game" are dominated by casino and affiliate pages such as "Best Mobile Versions and Apps for Mines Casino Game: The 2026 Guide". — [mines.it.com](https://mines.it.com/?p=2379); [sportsgrid](https://www.sportsgrid.com/social-casinos/stake-us-mines-strategy)

### Inferences
- **Keyword strategy:**
  - Lead with "Minesweeper" as the high-intent generic term. Supporting long-tail terms: "classic minesweeper", "minesweeper puzzle", "logic puzzle", "offline puzzle", "brain game", "daily challenge", "no guess minesweeper" if a no-guess board generator is implemented, and the game's cosmetic or theme hooks such as "ocean".
  - Play indexes the title (30 characters), short description (80) and full description (4000). Use the keyword naturally 3 to 5 times in the full description and avoid stuffing. These are ASO conventions, not verified this session.
  - Do not use "bet", "cash", "win money" or "casino" anywhere, including in the tag selection.
- **Naming alternatives**, in the format "<Brand>: Minesweeper Puzzle" (check trademark and Play title collisions first):
  - "Genoma Minesweeper"
  - "Deep Sweep: Minesweeper", which plays on the ocean theme in AmbientOceanBackdrop
  - "Mine Tide"
  - "Sonar Sweep"
  - "Reef Mines: Classic Minesweeper"
  - "Tidal Mines"
  - The package name can stay com.genoma.mines because users never see it.
- **Store category:** Puzzle. Screenshots should make the logic-puzzle framing obvious: number grid, flags, themes. They should not look like a gems-and-bombs multiplier.
- **Localize** the listing for the soft-launch countries and the top-5 locales once data shows where traction is.

### Gaps
- No ASO tool data (AppTweak or Sensor Tower keyword volume or difficulty for "minesweeper" vs. "mines") was gathered within budget.
- No live search was done of current Play titles named "Mines", "Minesweeper" and similar, so competitors' install counts are unknown.

## Launch sequencing and KPI gates

### Takeaway
Plan for about 8 to 12 weeks from the first internal build to global launch:
- Internal testing at once.
- Closed test for 14+ days with 12+ engaged testers, which is mandatory for new personal accounts.
- An optional open test.
- A 4 to 6 week soft launch in 1 to 3 cheaper English-speaking markets, gated on retention.
- Global launch.

### Cited Findings
- Per GameAnalytics 2025 data, median D1 retention across games was about 22% (about 40% for the top 10%) and median D7 about 4% (about 12% for the top 10%). Baseline retention kept falling through 2025. — [GameDevReports summary of GameAnalytics 2025](https://gamedevreports.substack.com/p/gameanalytics-mobile-gaming-benchmarks); [GameAnalytics 2025 PDF](https://investgame.net/wp-content/uploads/2025/02/2025-GameAnalytics-Mobile-Gaming-Benchmarks.pdf)
- Puzzle-genre retention is reported at about 31.85% D1, 12.18% D7 and 5.35% D30 (Mistplay data via Segwise; secondary). — [Segwise](https://segwise.ai/blog/mobile-gaming-app-user-retention-strategies.md)
- Practitioner soft-launch gates for F2P are roughly D1 35 to 50%+, D7 15 to 25%+, D30 5 to 15%+ (vendor or practitioner guides, inconsistent sourcing). — [Matej Lancaric, Soft Launch Bible 2025](https://lancaric.substack.com/p/the-soft-launch-bible-full-2025-blueprint); [Bruin go/no-go gate](https://getbruin.com/use-cases/mobile-gaming/soft-launch-d1-d7-retention-go-no-go-gate/)
- A practitioner comment on an indie launch thread says the usual method is to "soft launch in a single cheaper region and buy enough ads to measure retention." — [Reddit snapshot](https://reddit.sentinel-team.org/posts/1tjlt6v/snapshots/2026-05-21T19%3A40%3A57.540612Z) (anecdotal)

### Inferences
**Proposed sequence and gates** (gates calibrated to the puzzle numbers above, set between the medians and the practitioner targets):

1. **Internal testing** (up to 100 testers, instant availability): crash-free sessions of at least 99.5%, Android Vitals ANR rate below 0.47% and crash rate below 1.09% (Play's bad-behavior thresholds, unverified this session), all Play Console declarations complete, and the pre-launch report clean.
2. **Closed testing** (14+ days, 12 to 30 engaged testers): recruit from friends, r/AndroidClosedTesting and puzzle Discords. Gather qualitative feedback on the first-time user experience, hearts pacing and the store. Gate: no P0 bugs, and each tester has meaningful sessions so Google sees engagement. Then apply for production access.
3. **Open testing** (optional, 2 to 4 weeks): public opt-in from the Play listing, useful for collecting early reviews and wishlists. Gate: average rating of 4.3 or higher, and D1 above 30% among organic testers.
4. **Soft launch** with a production release limited by country, for example Philippines, Canada, Australia/NZ or India (for volume), over 4 to 6 weeks. Spend a small UA budget ($300 to $1,000) to get 1,000+ installs per cohort.
   - Gates: D1 at or above 35%, D7 at or above 12 to 15%, D30 at or above 5%.
   - Average session length at or above 5 minutes and 2 or more sessions per day.
   - Store-listing conversion at or above 25 to 30%, measured with A/B tests in Play Store listing experiments.
   - If monetized, early payer conversion of at least 1 to 2% and ARPDAU tracked.
   - Iterate weekly. Missing the gates after two iterations means rethinking the core loop or meta before going global.
5. **Global launch**: expand to all countries, submit to Play featuring (Play Console "featuring" nomination form, unverified this session), and coordinate a content push in the same week.

Track everything in Firebase Analytics with custom events: game_start, game_win, heart_depleted, store_view, purchase, tournament_join.

### Gaps
- No primary, puzzle-specific 2025/2026 soft-launch benchmark was found. The gate values are derived judgment.
- The Android Vitals threshold values come from background knowledge and were not verified in this session.

## Growth channels, indie case studies, and Google Play programs

### Takeaway
For a 3-person team with no UA budget, short-form video (TikTok/Shorts/Reels) of satisfying board clears and themed cosmetics is the highest-upside channel. Reddit and puzzle communities work for early feedback and testers. The evidence suggests organic launches usually fail without sustained content or store placement, and the success stories are survivorship-heavy. Google's indie programs come in yearly cycles. No 2026 Accelerator or Festival call was found.

### Cited Findings
- Storyteller, an indie puzzle game, saw a lukewarm launch and then "great" success a year later driven by TikTok virality. — [GamesRadar](https://gamesradar.com/games/puzzle/indie-dev-thought-their-puzzle-game-would-just-die-out-but-a-weird-sales-curve-and-tiktok-virality-led-to-great-success-a-year-after-launch)
- Stacky Dash became the most downloaded game on TikTok in December and reached more than 100,000 downloads a day. — [PocketGamer.biz](https://www.pocketgamer.biz/how-tiktok-helped-stacky-dash-achieve-100000-downloads-a-day)
- Bacon: The Game reached 18.5M downloads after going viral on TikTok. — [mobilegamer.biz](https://mobilegamer.biz/bacon-the-game-hits-18-5m-downloads-after-going-viral-on-tiktok/)
- A Little to the Left: tidying-themed puzzles plus TikTok made it a hit. — [GameDiscoverCo](https://newsletter.gamediscover.co/p/a-little-to-the-left-how-tidying)
- Hamster Chase got 10k downloads in 8 days (indie postmortem). — [Game Developer](https://gamedeveloper.com/production/hamster-chase-released-how-i-got-ten-thousand-downloads-in-eight-days)
- Counter-examples: the GravityShot solo postmortem reports a launch with almost no announcement and weak numbers in a saturated market. The Frantic Architect developer found no organic tail once store featuring ended. Another indie dev reported about 100 downloads on launch day. — [GravityShot postmortem](https://gamedo-live.pages.dev/news/gravityshot-postmortem-shipping-mobile-game-godot); [ExpertBeacon](https://expertbeacon.com/the-up-and-down-journey-of-developing-a-viral-mobile-game/); [Reddit snapshot](https://reddit.sentinel-team.org/posts/1tjlt6v/snapshots/2026-05-21T19%3A40%3A57.540612Z); [Game Developer: a pretty decent game failing](https://www.gamedeveloper.com/business/the-story-of-a-pretty-decent-game-failing-)
- Puzzlit reportedly grew by working with puzzle enthusiasts and streamers on authentic content. The article covering it is critical in tone. — [mwm.ai](https://mwm.ai/articles/puzzlit-puzzles-made-social-explodes-2026-04-13)
- The Google Play Indie Games Accelerator and Festival were most recently documented here for 2021 (June submissions, July 1 deadline, a 10 to 12 week accelerator). A 2026 Indie Games Fund for Sub-Saharan Africa ($1M total, $50k to $200k per studio) closed **Jul 31 2026**. — [Android Developers Blog 2021](https://android-developers.googleblog.com/2021/06/grow-your-indie-game-with-help-from.html); [thenote.app on the Africa fund](https://thenote.app/post/en/google-play-launches-the-first-indie-games-fund-in-africa-iqdzmkhqdi)

### Inferences
**Channel plan**, about 10 to 15 team-hours a week:

1. **TikTok/Shorts/Reels.** Post 3 to 5 clips a week, 15 to 30 seconds each. Formats:
   - "Satisfying cascade clear"
   - "Can you solve this?" boards that drive comments
   - Hard-board fails
   - Cosmetic theme showcases (the ocean theme)
   - Devlog clips of a "3-person team builds a minesweeper"

   Add in-app share-a-board or replay export to make posting easy.
2. **Reddit.** Use r/AndroidGaming, r/puzzles, r/Minesweeper, r/IndieGaming, r/playmygame and r/AndroidClosedTesting (for recruiting testers). Value-first posts work best, such as a "no-guess" generator story or a dev postmortem. Follow each subreddit's self-promotion rules.
3. **Puzzle and Minesweeper communities.** Target the competitive Minesweeper community (Minesweeper.online players, speedrunners), puzzle Discords and logic-puzzle newsletters. Tournaments and daily challenges are a natural hook for them.
4. **Product Hunt.** Low value for mobile games. Use it only as a one-day launch-week boost and for backlinks, if at all.
5. **Press and curators.** Pitch Pocket Gamer, Droid Gamers and Android Police "apps of the week", plus small YouTube mobile-game reviewers. Send a press kit with a GIF, trailer and APK or promo access.
6. **Play-native levers.**
   - Store listing experiments for icon and screenshots.
   - Custom store listings per country.
   - Pre-registration with a reward, such as an exclusive cosmetic.
   - Play Games Services achievements and leaderboards (the app already has achievements).
   - LiveOps "Promotional content" cards for tournaments.
   - The Play Console featuring nomination after launch.
   - Watch the Android Developers Blog for the 2027 Indie Games Accelerator/Festival call; historically it opened mid-year.
7. **Retention loops** that feed growth: daily challenge, streaks, weekly free tournaments with cosmetic prizes, and share cards.

**Overall:** treat virality as optional upside, not the plan. Make the decision with the soft-launch retention gates, and only then invest in content at volume.

### Gaps
- No confirmed 2026/2027 Google Play Indie Games Accelerator or Festival call was found. The official Play "Indie Corner" and featuring pages were not fetched.
- No Minesweeper-specific indie case study was found.
- Product Hunt effectiveness for mobile games was not researched with sources.
