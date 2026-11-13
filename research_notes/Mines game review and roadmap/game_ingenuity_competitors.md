# Mines: Game Ingenuity vs. the Minesweeper Competitive Landscape (as of Nov 2026)

## What are Mines' actual design pillars, and what is distinctive vs. generic?

### Takeaway
Mines is a faithful classic Minesweeper core on a single 9x9 grid with standard free-to-play retention systems around it: hearts/energy, coins/gems, cosmetics, achievements and a placeholder tournament. Its only real differentiator is the animated ocean visual identity. Every gameplay and meta system has a direct precedent among competitors.

### Cited Findings
Repo evidence (paths relative to C:\Users\hp\AndroidStudioProjects\Mines):
- **Board:** All three difficulties use the same 9x9 grid. EASY has 10 mines (12% density), MEDIUM 20 (25%), HARD 30 (37%). There is no 16x16 or 30x16 board and no custom size (feature/game/src/main/java/com/genoma/mines/game/domain/Difficulty.kt).
- **Rules engine:** Mines are placed after the first tap, outside the tapped cell and its neighbours, so the first tap always opens an area. Chording and flagging are supported. There is no no-guess or solvability generator, so random 50/50 guesses can still happen (feature/game/.../domain/MinesweeperGame.kt, `placeMines(excludingIndex)`, `chord()`).
- **Hearts:** Maximum 5 hearts. One heart regenerates every 60 minutes, and refills cost 5 gems ("diamonds") per heart. This is a classic casual-game energy gate (feature/life/.../domain/LifeRules.kt).
- **Scoring:** Wins are scored in fast/average/slow tiers. Early losses are penalised, and a partly cleared board earns survival points (feature/game/.../domain/ScoreConfig.kt, ScoreCalculator.kt, LevelSystem.kt).
- **Tournament:** The screen is a static teaser ("THE WINTER CUP … COMING SOON … Compete with other players for the top spot. Warm up with a practice game in the meantime."). There is no competitive backend yet (feature/tournament/.../ui/TournamentScreen.kt).
- **Meta and cosmetics:** Coin wallet; store catalog with Classic Teal and Deep Ocean board themes, a Frost cell skin and a Robo avatar; achievements with badge tiers; celebration/confetti; game history; Google sign-in with Firestore sync; guest mode on Room (feature/store/.../data/catalog/*, feature/achievements/.../domain/BadgeTier.kt, feature/game/.../data/local/GuestGameDatabase.kt).

How competitors handle the same features:
- Daily/weekly quests, coins, XP, login-streak bonuses, chests, trophies and a ticket-gated PvP arena already exist on minesweeper.now — [minesweeper.now Arena help](https://minesweeper.now/help/arena)
- Seasonal events, quests, arena coins, PvP and trophies appear on minesweeper.online player profiles — [minesweeper.online profile](https://minesweeper.online/player/19176606)

### Inferences
- Distinctive: the ocean art direction (AmbientOceanBackdrop, Deep Ocean theme) and polished Compose UI. That is presentation, not mechanics.
- Generic: the classic rules, difficulty set by density, energy hearts, coins, cosmetics and achievements. The progression systems mirror minesweeper.now/online, and the hearts mirror the casual F2P puzzle playbook.
- Weaknesses relative to the genre:
  - Without a no-guess generator, a forced 50/50 can cost a paid heart. That likely makes losses feel unfair and erodes trust in the monetisation.
  - The single 9x9 grid caps the skill ceiling for experienced players: HARD is dense, not big.
- The tournament placeholder is the clearest unused hook. A same-seed async competition would turn it into a real feature.

### Gaps
- I didn't look at analytics, retention or install data for Mines itself. None was in scope or available.
- I didn't check whether the ocean theme reaches gameplay (for example, sea creatures as mines) or is purely backdrop. The skim was brief.

## Competitor matrix (15-25 titles)

### Takeaway
On Google Play the genre is crowded with near-identical classic clones. The leaders have about 1M+ installs with 4.6-4.8 ratings and compete on polish, no-guess boards and being ad-light. Real genre innovation (variant rules, RPG/roguelite hybrids, hand-crafted deduction) sits on PC/web and sells modestly but earns very high review scores.

### Cited Findings
| # | Title | Platform | Scale / reception | Monetisation | Key differentiator | Source |
|---|---|---|---|---|---|---|
| 1 | Minesweeper Pro (Mindware) | Android | about 1.3M installs, 20.1K ratings, 4.72 stars; updated Apr 20, 2026 | Paid "Pro" edition (free sibling exists) | Mature classic; the free version (mindware.minegames) has about 39.8K installs and 4.41 stars | [AppGoblin](https://appgoblin.info/apps/mindware.minegamespro); [AppGoblin free](https://appgoblin.info/apps/mindware.minegames); [netzwelt](https://www.netzwelt.de/news/223301-kostenlose-app-diesen-spieleklassiker-sichert-euch-gratis-google-play-store.html) |
| 2 | Minesweeper - The Clean One (Dustland Design) | Android | 4.7 stars in Play search | Free | Guess-free, offline, minimalist design | [Play search](https://play.google.com/store/search?q=minesweeper&c=apps); [AppFollow](https://apps.appfollow.io/android/minesweeper-the-clean-one/ee.dustland.android.minesweeper?country=ru) |
| 3 | Minesweeper (Evkar games) | Android | 4.8 stars; first in Play search | not captured | Classic | [Play search](https://play.google.com/store/search?q=minesweeper&c=apps) |
| 4 | Minesweeper (Infinità Game Studio) | Android | 4.2 stars | not captured | Classic | [Play search](https://play.google.com/store/search?q=minesweeper&c=apps) |
| 5 | MineSweeper (Production Panic) | Android | 3.6 stars | not captured | Classic | [Play search](https://play.google.com/store/search?q=minesweeper&c=apps) |
| 6 | Minesweeper Classic: Retro | Android | 1M+ installs | not captured | Retro look plus a no-guess mode | [APKCombo](https://apkcombo.com/minesweeper-classic-retro/com.still57.minesweeper/) |
| 7 | Antimine (open source) | Android/F-Droid | Mixed user reviews (one user found it too buggy) | Free, open source | No-guess algorithm, under 9MB | [IzzyOnDroid](https://apt.izzysoft.de/fdroid/index/apk/dev.lucanlm.antimine?repo=main); [README](https://git.wbrawner.com/wbrawner/antimine-android/raw/branch/master/README.md); [Gamebrain](https://gamebrain.co/game/antimine-no-guess-minesweeper) |
| 8 | Mines+ | Android | n/a | No data collection | Every board solvable, custom difficulty | [Product Hunt](https://www.producthunt.com/p/mines-plus/mines-3) |
| 9 | Minesweeper Classic (Maple Media) | Android | 100K+ installs | not captured | Classic | [search snippet via appgoblin listing](https://appgoblin.info/apps/com.simplefishgames.minesweeper) (unverified) |
| 10 | minesweeper.online | Web/app | Large community (profiles show seasonal events) | F2P with arena coins, quests | Ranked PvP, seasonal events, quests, trophies | [profile](https://minesweeper.online/player/19176606) |
| 11 | minesweeper.now | Web | n/a | Tickets, chests, event shop | Two-player same-board PvP Arena, daily/weekly quests, login streaks | [Arena help](https://minesweeper.now/help/arena) |
| 12 | Dragonsweeper (Daniel Benmergui, 2025) | Browser (itch.io) | Called "my favorite game of 2025 (so far)"; Ars Technica's Kyle Orland was "mildly obsessed" with it | Free | Minesweeper/RPG hybrid: monsters with HP, levelling, one hand-tuned board; the developer kept iterating on it | [Kowatek](https://blog.kowatek.com/?p=97027); [waxy.org](https://waxy.org/2025/02/kyle-orland-on-the-genius-of-dragonsweeper); [Product Hunt](https://www.producthunt.com/products/dragonsweeper) |
| 13 | Mamono Sweeper (Hojamaka Games) | Browser/PC | Cult classic | Free | Numbers show the summed level of adjacent monsters; XP, levelling and HP replace instant death | [mzrg notes](https://mzrg.com/mines/mamono.shtml); [MetaFilter](https://metafilter.com/mefi/92517) |
| 14 | DemonCrawl (2019) | Steam | about 74% of 1,311 reviews positive (aggregator) | Premium | Minesweeper roguelite with hundreds of items, stages and abilities | search snippet (Gamebrain/aggregator); not verified on Steam |
| 15 | 14 Minesweeper Variants (2022) | Steam | about 97% of about 1.5-1.6K reviews positive; estimates of 48K-112K copies and $253K-$281K revenue (sources conflict) | Premium, $6.99 | 14 rule variants, procedural puzzles, every board solvable by logic | [GameRevenueData](https://gamerevenuedata.pages.dev/games/14-minesweeper-variants); [SteamPulse](https://steampulse.org/game/1865060); [VaporLens](https://vaporlens.app/app/1865060/14_minesweeper_variants) |
| 16 | 14 Minesweeper Variants 2 (Jun 14, 2024) | Steam | 96% of 264-347 reviews positive | Premium | 14 new variants, 4 difficulties each | [Steam](https://store.steampowered.com/app/2631960/); [wasdland](https://www.wasdland.com/game/14-minesweeper-variants-2-2631960) |
| 17 | Tametsi | Steam | about 97% of about 1.2-1.4K reviews positive (aggregators disagree) | Premium | Hand-crafted zero-guess puzzles; squares, rectangles and hexagons | [wasdland](https://www.wasdland.com/game/tametsi-709920); [Buried Treasure](https://buried-treasure.org/2020/02/tametsi/); [Kotaku](https://kotaku.com/what-to-play-mario-rpg-final-fantasy-16-metal-gear-1851029774/8) |
| 18 | Hexcells series | Steam | The genre's reference point | Premium | Stylish hex-grid deduction with hand-made levels | [Buried Treasure](https://buried-treasure.org/2020/02/tametsi/) |
| 19 | Minesweeper Genius | iOS/Android/Xbox | OpenCritic 75; 148Apps calls it "an evolution of Minesweeper" | Premium | Minesweeper x Picross hybrid with a story mode; readers say levels feel random | [OpenCritic](https://opencritic.com/game/7464/view/reviews); [148Apps](https://148apps.com/reviews/minesweeper-genius-review); [AppSpy](https://www.appspy.com/news/minesweeper-genius-combines-the-logical-challenges-of-picross-and-minesweeper-available-on-ios-and-android/) |
| 20 | Dragonsweeper-likes (Minesweeper & Dungeon RPG, Dungeon Minesweeper, DungeonSweeper, Dungeon Sweeper Plus, Infinity Sweeper) | Steam/itch, 2025-26 | Mostly small: Mostly Positive (28 reviews) and Mixed (13 reviews); Infinity Sweeper launched May 7, 2026 | Premium | Dungeon/RPG/roguelite reskins of the sweeper core | [Playpile](https://playpile.gg/games/minesweeper-dungeon-rpg); [Gamebrain](https://gamebrain.co/game/dungeon-minesweeper); [Gematsu](https://www.gematsu.com/2026/05/fantasy-globetrotting-minesweeper-game-dungeon-sweeper-plus-announced-for-pc); [jeu.video](https://jeu.video/article/infinity-sweeper-trailer-en); [Steam](https://store.steampowered.com/app/4109840) |
| 21 | LinkedIn Games (Queens, Tango, Zip, Mini Sudoku, Patches, and others) | Web/app | 7-8 daily games by 2026; 84% next-day return; 830K newsletter subscribers | Free, for engagement | One puzzle per day at midnight PT, streaks and milestones, shareable results | [LinkedIn Help](https://www.linkedin.com/help/linkedin/answer/a6863543); [Beebom](https://beebom.com/puzzle/all-linkedin-games-explained/); [Engadget](https://www.engadget.com/linkedin-now-has-wordle-style-games-you-can-play-every-day-133035046.html) |
| 22 | NYT Games (Wordle and others) | Web/app | Daily-player figures are only from weak sources (one claims 12-15M a day, unverified) | Subscription bundle | The daily-ritual template | [Digiday](https://digiday.com/media/new-york-times-looks-to-gaming-vertical-to-grow-subscriptions); [pulse.bot (weak)](https://www.pulse.bot/media/news/nyt-games-surge-wordle-connections-and-strands-hints-draw-millions-daily-ede6207d-fc09-4c9b-b293-efa3c9b41e96/) |

- None of the third-party snippets found a classic Minesweeper app with a 10M+ or 50M+ download tier. The highest figure found was about 1M+ — search across [AppGoblin listings](https://appgoblin.info/apps/com.inspiredsquare.minesweeper)

### Inferences
- Mobile Minesweeper is a long tail of clones rated around 4.2-4.8. Without a hook, a new classic entrant competes on store-listing optimisation alone.
- Innovation earns premium PC reviews of 96-97% but modest unit sales (tens of thousands to about 100K copies). Novel rules win acclaim more readily than scale.
- The 2025-26 wave of Dragonsweeper-likes is crowding the RPG-sweeper niche on Steam, but the review counts found are small. Mobile versions of these mechanics appear thin, though I didn't verify this.

### Gaps
- I couldn't get Play install counts, rating counts or ad/IAP flags for most top Android titles. AppGoblin returned 503 and the Play detail pages wouldn't render. Microsoft's own Minesweeper on mobile and Google's built-in search game weren't checked.
- I found no Dragonsweeper player or sales numbers, and no confirmed Steam or mobile release.
- DemonCrawl and Hexcells review and sales figures weren't verified on Steam directly.
- No AppMagic or Sensor Tower revenue estimates were obtained.
- I found no source for the name collision with casino "Mines" games (Stake-style crash/mines gambling), so this note is unsourced. Searching "Mines" on Play will likely surface gambling titles, which is a discoverability risk for the name.

## Novelty rating (1-10) with justification

### Takeaway
**3/10.** The execution is competent and the ocean presentation is pleasant, but no mechanic is new. The core is classic and narrower than competitors (9x9 only, guess-prone), and the meta systems are standard F2P scaffolding already used by minesweeper.now/online.

### Cited Findings
- Quests, coins, streaks, chests, trophies and PvP already exist in browser competitors — [minesweeper.now](https://minesweeper.now/help/arena)
- No-guess boards are offered by several free Android apps — [Mines+](https://www.producthunt.com/p/mines-plus/mines-3); [Antimine](https://apt.izzysoft.de/fdroid/index/apk/dev.lucanlm.antimine?repo=main); [Clean One](https://apps.appfollow.io/android/minesweeper-the-clean-one/ee.dustland.android.minesweeper?country=ru)
- Innovators changed the rules themselves:
  - Variant constraints — [14MV](https://vaporlens.app/app/1865060/14_minesweeper_variants)
  - Monster levels, HP and XP — [Mamono notes](https://mzrg.com/mines/mamono.shtml)
  - Roguelite items — DemonCrawl (aggregator snippet)
  - Hand-crafted geometry — [Tametsi](https://buried-treasure.org/2020/02/tametsi/)
- Repo: 9x9-only Difficulty.kt, no solvability check in MinesweeperGame.kt, a "Coming Soon" tournament, and a heart energy gate in LifeRules.kt.

### Inferences
- Scoring rubric (inference): core mechanics 1/3, meta/progression 1/3, presentation and theme 1-1.5/2, social/competitive 0/2 (not shipped). Total about 3.
- Raising this to 6-7 would need one rule-level twist (ocean creatures, depth or variants) plus one social loop (daily seed or async cup).

### Gaps
- I did no user testing or review sentiment for Mines itself, so the rating rests on design, not player perception.

## White-space and differentiation opportunities (feasibility for a 3-person team)

### Takeaway
The best near-term moves are:
1. **No-guess generation:** table stakes.
2. **A daily seeded "Tide" puzzle with streaks and share cards:** cheap, proven for retention.
3. **The Winter Cup as a same-seed async tournament:** reuses existing Firestore.

The bigger differentiation bet is an ocean-dive roguelite layer in the Mamono/Dragonsweeper mould, which would turn the visual identity into mechanics. This is under-served on mobile.

### Cited Findings
- Daily-puzzle formats drive habit: LinkedIn reports 84% next-day return, with streaks, milestones and a fixed midnight reset — [Engadget](https://www.engadget.com/linkedin-now-has-wordle-style-games-you-can-play-every-day-133035046.html); [LinkedIn Help](https://www.linkedin.com/help/linkedin/answer/a6863543)
- Same-board PvP and quests already work in browser Minesweeper — [minesweeper.now](https://minesweeper.now/help/arena)
- Variant rules have strong demand: 96-97% positive, with a sequel — [GameRevenueData](https://gamerevenuedata.pages.dev/games/14-minesweeper-variants); [Steam 14MV2](https://store.steampowered.com/app/2631960/)
- RPG-sweeper hybrids get critical buzz — [Kowatek](https://blog.kowatek.com/?p=97027); [waxy.org](https://waxy.org/2025/02/kyle-orland-on-the-genius-of-dragonsweeper)
- The niche is filling on PC — [Gematsu](https://www.gematsu.com/2026/05/fantasy-globetrotting-minesweeper-game-dungeon-sweeper-plus-announced-for-pc); [jeu.video](https://jeu.video/article/infinity-sweeper-trailer-en)
- No-guess is a widely expected feature; an open-source reference implementation exists — [Antimine README](https://git.wbrawner.com/wbrawner/antimine-android/raw/branch/master/README.md)
- Common complaints about variant/hybrid games: obtuse hints and "random"-feeling levels — [VaporLens](https://vaporlens.app/app/1865060/14_minesweeper_variants); [TrueAchievements](https://www.trueachievements.com/game/Minesweeper-Genius/reviews)

### Inferences
Each opportunity is listed with an estimated feasibility for a 3-person team:
1. **No-guess generator** (solve from the first click with a constraint solver; regenerate until solvable).
   - Effort: about 1-2 weeks. Feasibility: high.
   - Differentiation: low, it is table stakes.
   - Why: needed so that heart losses never come from coin flips.
2. **Bigger boards and modes** (16x16 and 30x16 expert, custom; possibly zoom/pan).
   - Effort: about 1-2 weeks of UI work. Feasibility: high.
   - Why: restores the skill ceiling for veterans.
3. **Daily seeded "Tide of the Day."**
   - What: date-based seed, the same board worldwide, one attempt, a streak, a spoiler-free emoji/ocean share card, and a Firestore daily leaderboard.
   - Effort: about 2-3 weeks. Feasibility: high.
   - Differentiation: medium-high on mobile Minesweeper; proven by LinkedIn and NYT.
4. **Winter Cup async tournament.**
   - What: weekly same-seed sets, best-time leaderboard, cosmetic-only rewards, optional ghost replay of the top run.
   - Effort: about 3-5 weeks. Feasibility: medium-high.
   - Needs: server-side validation via Cloud Functions to limit cheating.
5. **Ocean-dive roguelite ("Abyss" mode).**
   - What: depth layers as boards; sea creatures with levels or HP instead of binary mines (Mamono/Dragonsweeper); oxygen as a resource; relics and pearls between dives.
   - Effort: about 2-4 months. Feasibility: medium.
   - Differentiation: high. It turns the art identity into mechanics, and there appear to be few polished mobile entries.
   - Risks: design tuning, and a crowding PC niche.
6. **Variant-rule packs** (for example, hex cells, "numbers count diagonals only," and rules for the total in a row, column or region, inspired by LinkedIn Queens).
   - Effort: about 1-2 weeks per variant with a shared solver. Feasibility: medium.
   - Differentiation: medium-high on mobile.
   - Fits: monetises naturally as unlockables or premium packs.
   - Watch: clear hints are needed (14MV's main complaint).
7. **Rethink hearts.**
   - Option A: hearts gate only competitive/daily modes.
   - Option B: a "second chance" (reveal a mine, keep playing) replaces energy gating.
   - Effort: low. Why: energy gating in pure logic games is a likely source of negative reviews (inference, unverified).
8. **Light narrative and collection** (an ocean bestiary or map of discovered "wrecks"; cosmetics earned through exploration).
   - Effort: about 3-6 weeks of art and content. Feasibility: medium.
   - Fits: pairs with #5.

Suggested sequence: #1 → #3 → #4 → #2 within one quarter; then #5 or #6 as the signature differentiator.

### Gaps
- I found no hard data on what retention or revenue lift daily modes or no-guess boards bring to mobile Minesweeper specifically.
- I didn't measure how many mobile titles already offer RPG-sweeper or variant modes, because the store data tools were unavailable.
- Effort estimates are judgment-based, not sourced.
