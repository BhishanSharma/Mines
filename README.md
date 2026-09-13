# Mines — modular project structure

This project was restructured from a single `:app` module (64 Kotlin files in one
compile unit) into a **multi-module Gradle project**, so that different teams can own
different modules, build/test them independently, and stop stepping on each other in
`MainActivity.kt`.

## Module map

```
Mines/
├── app/                     — thin shell: MainActivity, app-level ViewModel, manifest,
│                              app icon/theme/launcher resources, Firebase config.
│                              Wires every feature together. Owned by a "platform/app" team.
│
├── core/
│   ├── theme/               — Color.kt, Type.kt, Theme.kt (Compose MaterialTheme). No feature deps.
│   ├── ui/                  — Shared, reusable composables with no business logic:
│   │                          BottomNavbar/BottomNavItem, ConfettiOverlay.
│   └── session/             — SessionManager / UserSession (Firebase auth session state).
│
└── feature/
    ├── auth/                — Login screen, Google sign-in, session persistence.
    ├── settings/            — Settings screen + DataStore-backed preferences.
    ├── store/                — In-app store catalog, themes/skins.
    ├── wallet/               — Coins/diamonds wallet (local + Firestore).
    ├── game/                 — Core Minesweeper game: board logic, Room (guest) +
    │                          Firestore (signed-in) game history, GameScreen/HistoryScreen.
    ├── achievements/         — Badge/achievement calculation + screen (depends on :feature:game).
    ├── celebration/          — Win/level-up celebration screen (depends on :feature:achievements).
    ├── profile/              — Profile screen, avatars, stats (depends on :feature:game, :feature:achievements).
    ├── home/                 — Home screen (depends on :feature:game, :feature:profile).
    ├── tournament/           — Tournament screen. No feature deps.
    ├── moregames/            — "More games" screen. No feature deps.
    └── userfeedback/         — Feedback form + Firestore submission. No feature deps.
```

## Dependency rules

- **`core:*` modules never depend on a `feature:*` module.** They are the shared
  foundation every feature can safely build on.
- **`feature:*` modules only depend "downward"** on `core:*` and, where there's a real
  product relationship, on one other feature (e.g. `achievements` needs game history, so
  it depends on `feature:game`; `home` composes `feature:game` + `feature:profile`).
  There are no dependency cycles — this was checked and one existing cycle
  (`achievements → game → celebration → achievements`) was removed by moving the shared
  `ConfettiOverlay` composable into `core:ui`, which every screen can use directly.
- **`app` is the only module allowed to depend on *everything*.** It just wires screens
  together (navigation `when` block, shared `MinesweeperViewModel`) — it should not
  contain feature logic.

This means, for example, the store/wallet team can work entirely inside
`feature:store` and `feature:wallet` without ever touching, recompiling, or merge-
conflicting with the auth or achievements teams' code — Gradle will only rebuild the
modules that actually changed.

## What moved where (if you're looking for something)

| Old location (single `:app` module)                  | New location                          |
|--------------------------------------------------------|----------------------------------------|
| `core/theme/*`                                          | `core/theme`                           |
| `shared/ui/components/BottomNavbar.kt`                  | `core/ui` (package `core.ui.components`) |
| `celebration/ui/ConfettiOverlay.kt`                     | `core/ui` (package `core.ui.components`) |
| `session/*`                                             | `core/session`                         |
| `auth/*` + `mine_logo.png`, `ic_google_logo.xml`        | `feature/auth`                         |
| `settings/*`                                            | `feature/settings`                     |
| `store/*`                                                | `feature/store`                        |
| `wallet/*`                                               | `feature/wallet`                       |
| `game/*`                                                 | `feature/game`                         |
| `achievements/*`                                         | `feature/achievements`                 |
| `celebration/*` (minus `ConfettiOverlay`)                | `feature/celebration`                  |
| `profile/*` + avatar drawables                          | `feature/profile`                      |
| `home/*` + `home_banner.png`                            | `feature/home`                         |
| `tournament/*`, `moregames/*`, `userfeedback/*`          | matching `feature/*` module            |
| `MainActivity.kt`, `presentation/viewmodel/*`            | `app`                                   |

No package names changed (still `com.genoma.mines.*`), so any external references,
bookmarks, or code review comments pointing at a class name still work — only the
Gradle module and folder it physically lives in changed.

## Known simplification (please tighten before shipping)

To keep everything compiling without needing to hand-audit every method signature,
several third-party dependencies (Firebase, Room, DataStore, Credentials, Coil,
extended Material icons) are declared with Gradle's `api` configuration instead of
`implementation` in the modules that use them. `api` leaks the dependency to every
module downstream, which is safe but slightly weakens the isolation multi-module
projects are meant to provide. As each team touches their module, downgrade `api` to
`implementation` wherever nothing from that library actually appears in the module's
public function signatures — Android Studio's "Unused dependency" / Gradle's
`buildHealth`-style lint will flag the ones that are safe to tighten.

## Suggested next step

`MinesweeperViewModel.kt` (in `app`) still reaches into `game`, `wallet`, `store`,
`settings`, `achievements`, `celebration`, and `userfeedback` from one 650-line class.
The module boundaries above stop teams from having merge conflicts in feature code, but
this shared ViewModel is still a single file multiple teams will edit. When you have
bandwidth, splitting it into one `ViewModel` per feature (each living inside that
feature's own module, e.g. `GameViewModel` in `feature:game`) and having `app` just
compose them would remove this last shared bottleneck.

## Opening the project

Re-open the project root in Android Studio (Hedgehog+ recommended for AGP 9.x) and let
it sync — Gradle will pick up the module graph from `settings.gradle.kts` automatically.
Any stale `.idea/` project files from the old single-module layout were intentionally
not carried over, so Android Studio will regenerate them cleanly on first sync.
