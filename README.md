# Dragon Trainer

A client for [Dragons of Mugloar](https://dragonsofmugloar.com), a game where you send a dragon on
missions from a message board, earn gold and score, and buy items from a shop to stay alive.

The client talks to the public Mugloar REST API and keeps a local copy of every game in SQLite,
so a game can be resumed later.

> **Work in progress:** the app is being converted from a terminal client into a single page web application.
> The terminal menu has been removed and the backend is a REST API that any front end can use. The web pages
> are not built yet.

## Requirements
- Java 21
- Network access to `https://dragonsofmugloar.com`
- The `sqlite3` command-line tool, to create the database the first time

Maven does not need to be installed. The project ships with the Maven wrapper (`./mvnw`).

## Setup
Your games are stored in a local SQLite database, `data/drogontrainapp.db`. It is not part of the repository
(`data/*.db` is in `.gitignore`), so each checkout creates its own from the DDL script:

```bash
sqlite3 data/drogontrainapp.db < src/main/resources/data/mugloar_schema.sql
```

The path is set in `src/main/resources/application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:sqlite:${DRAGONTRAINAPP_DB_PATH:data/drogontrainapp.db}
```

`data/drogontrainapp.db` is relative to the folder the app is started from, which is the project root when you
use `./mvnw spring-boot:run`. To use a database somewhere else, for example when running a built jar
from another folder, set `DRAGONTRAINAPP_DB_PATH`:

```bash
DRAGONTRAINAPP_DB_PATH=/path/to/drogontrainapp.db ./mvnw spring-boot:run
```

If the database file is missing, SQLite creates an empty one without tables, and the first action fails
with "no such table". Run the `sqlite3` command above to fix it.

## Running
```bash
./mvnw spring-boot:run
```

The app starts a web server on port 8080 with the REST API under `/api/games`.
To see and try every endpoint, open Swagger UI at http://localhost:8080/swagger-ui.html
(the OpenAPI spec is at http://localhost:8080/v3/api-docs).

To build a jar:

```bash
./mvnw package
java -jar target/dragon-trainer-app-1.0.0.jar
```

## REST API
Every button or command in a front end is one HTTP call. The game ID is part of the URL, so the server keeps
no per-browser state and several front ends can play different games at once.

| Button / command | Request | Answer |
|---|---|---|
| Start game | `POST /api/games` | `201`, the game status, and a `Location` header |
| Resume last game | `GET /api/games/current` | the status of the last game started |
| Check status | `GET /api/games/{gameId}` | the game status |
| View board | `GET /api/games/{gameId}/ads` | the stored ads (empty until downloaded) |
| Download ads | `POST /api/games/{gameId}/ads/refresh` | the new board |
| Solve ad | `POST /api/games/{gameId}/ads/{adId}/solve` | the outcome and the updated status |
| Reputation | `POST /api/games/{gameId}/reputation` | the reputation and the updated status (costs a turn) |
| Market | `GET /api/games/{gameId}/shop` | the shop items (downloaded the first time per game) |
| Buy item | `POST /api/games/{gameId}/shop/{itemId}/buy` | the outcome and the updated status |
| View logs | `GET /api/games/{gameId}/history` | missions and purchases, newest first |

Ads and items are identified by their ID, not their position, so a click still hits the right ad after the
board is downloaded again.

### What the answers contain
The answers carry everything a page needs to show, so no front end repeats the game rules:

- **Status:** turn, lives, gold, level, score, high score, `lifeState` (`HEALTHY`, `CRITICAL` or `GAME_OVER`)
  and a `warning` to show, or `null`.
- **Ad:** ID, message, reward, probability, `risk` (`{"code":"SAFE","icon":"🟢","label":"Safe"}`), `turnsLeft`,
  `expired`, `used`, and `solvable` to enable or disable the Solve button.
- **Shop item:** ID, name, cost and `affordable` to enable or disable the Buy button.
- **History entry:** time, `type` (`MISSION` or `PURCHASE`), ad or item ID, risk, `goldChange` (`+60`, `-50`, or
  `0` when it failed), success and message.
- **Actions** (solve, reputation, buy) answer `{"outcome": …, "status": …}`, so the page updates its numbers
  without a second request.

```json
POST /api/games/cWMtNkmc/ads/cEg1Uvof/solve
{"outcome":{"adId":"cEg1Uvof","success":true,"reward":8,"message":"You successfully solved the mission!"},
 "status":{"gameId":"cWMtNkmc","turn":1,"lives":3,"gold":8,"level":0,"score":8,"highScore":8,"lifeState":"HEALTHY","warning":null}}
```

### Errors
Every error has the same shape, Spring's `ProblemDetail` (RFC 9457) with an extra `code` to switch on:

```json
{"status":409,"title":"Not enough gold","code":"NOT_ENOUGH_GOLD",
 "detail":"Claw Honing costs 300 gold, but you only have 8. You need 292 more.",
 "instance":"/api/games/cWMtNkmc/shop/ch/buy"}
```

| Status | Codes |
|---|---|
| 404 | `NO_ACTIVE_GAME`, `PLAYER_STATUS_MISSING`, `AD_NOT_FOUND`, `ITEM_NOT_FOUND`, `GAME_NOT_FOUND` |
| 409 | `GAME_OVER`, `AD_ALREADY_USED`, `AD_EXPIRED`, `NOT_ENOUGH_GOLD`, `BOARD_EMPTY`, `MARKET_EMPTY` |
| 400 / 410 / 502 / 503 | The Mugloar server's errors, see [Server errors](#server-errors); `mugloarStatus` holds its own status |
| 500 | `INTERNAL_ERROR`: anything unexpected. The details are logged, not sent |

A rule broken before the call, such as an expired ad or too little gold, is refused without contacting
the Mugloar server.

### Front ends on another origin (CORS)
A front end served from another origin may call `/api/**` with `GET` and `POST` if its origin is allowed.
By default these are the Vite (`http://localhost:5173`) and Create React App / Next.js (`http://localhost:3000`)
dev servers. Set your own as a comma-separated list:

```bash
DRAGONTRAINAPP_CORS_ORIGINS=https://dragons.example.com ./mvnw spring-boot:run
```

## Game rules
The rules live in the `game` package as plain Java classes, with no Spring or web code, so the web
controllers and the tests use them the same way.

### Risk categories
Each ad's probability text from the server maps to a `RiskCategory` enum, from safest to deadliest.
The category is stored by name in the `risk_category` column of `cached_ad` and `mission_log`.

| Category | Shown as | Probability text from the server |
|---|---|---|
| `SAFE` | 🟢 Safe | Piece of cake, Sure thing, Walk in the park |
| `LIKELY` | 🟡 Likely | Quite likely, Hmmm.... |
| `GAMBLE` | 🎲 Gamble | Gamble |
| `RISKY` | 🟠 Risky | Risky, Playing with fire |
| `DANGEROUS` | 🔴 Dangerous | Rather detrimental |
| `FATAL` | ☠️ Fatal | Suicide mission, Impossible |
| `UNKNOWN` | ⚪ Unknown | Any other text, or none |

The text is matched without regard to case or surrounding spaces. Some ads arrive encrypted, in Base64
or ROT13; `MugloarService` decodes them when it downloads the board, so they map like any other ad.

### Ad expiry
The server sends `expiresIn` as the turns an ad has left when the board is downloaded. The turn of the download
is stored with each ad (`fetched_turn`), and every action that costs a turn brings the ads one step closer to
expiring:

```
turns left = expiresIn - (current turn - fetched turn)
```

At 0 or less the ad is expired, and the server no longer accepts it. `AdExpiry` does this calculation, and
`CachedAd.remainingExpiresIn(turn)` and `CachedAd.isExpired(turn)` use it.

### Message board and market
`BoardFactory` builds the stored boards from what the server sends:

- **Message board:** every download replaces the game's stored board. Each ad gets its board position, its risk
  category and the current turn. Ads already used keep their `used` flag if the server lists them again.
  If the download returns nothing, the stored board is kept.
- **Market:** the shop list doesn't change during a game, so it is downloaded only the first time it is opened
  for a game, and read from the database after that.

Ads and items are picked by their number on the board, which starts from 1.

### Gold, lives and history
- **Gold:** an item can be bought when the player's gold is at least its cost (`GoldRules`).
- **Lives** (`LifeState`): at **1 life** the player is warned to buy a Healing potion. At **0 lives** the game is over.
- **Player status** (`PlayerStatusUpdater`): the stored status is updated from every server response. Investigating
  reputation costs a turn, but the response has no turn field, so the turn is counted locally. The server always
  reports a high score of 0, so the high score is the best score across all stored games.
- **History** (`HistoryEntryFactory`): every mission and purchase is saved to `mission_log`. A mission earns its
  reward and a purchase spends the item cost, but only when it succeeds; a failed one counts as 0 gold.

### Checks before an action
Before an action is sent to the server, it must pass the game's rules. Each rule is a `GameCheck` (Strategy
pattern), and rules chain with `and()` so the player sees the first one broken (Chain of Responsibility).
`GameChecks` has them ready-made:

| Check | Used before | Refused with |
|---|---|---|
| `gameSelected()` | any action | `NO_ACTIVE_GAME` |
| `canPlay()` | every action that calls the server | `PLAYER_STATUS_MISSING`, `GAME_OVER` |
| `canSolve(turn)` | solving an ad | `AD_ALREADY_USED`, `AD_EXPIRED` |
| `canAfford(status)` | buying an item | `NOT_ENOUGH_GOLD` |
| `pick(entries, number, …)` | choosing an ad or item by its board number | `BOARD_EMPTY`, `MARKET_EMPTY`, `INDEX_OUT_OF_RANGE` |

Reading the board, status, history or market needs no checks, so the stored data stays visible after a game
is over.

```java
GameChecks.canPlay().check(status).orThrow();
CachedAd ad = GameChecks.pick(ads, number, CheckFailure.BOARD_EMPTY);
GameChecks.canSolve(status.getTurn()).check(ad).orThrow();
```

`orThrow()` throws a `GameRuleException` carrying the `CheckFailure` and a message for the player, e.g.
"Healing potion costs 50 gold, but you only have 30. You need 20 more."

### Server errors
Every call to the Mugloar API goes through one method in `MugloarService`. A successful (2xx) response is read
into its record; anything else throws a `MugloarApiException`, so no endpoint handles errors on its own and
nothing is printed to the console. The exception carries:

- a `MugloarError` reason, with the status the web app should answer with
- the `MugloarOperation` that failed, such as `SOLVE_AD` or `BUY_ITEM`
- the Mugloar server's own status, if it answered
- a message for the player, e.g. "Mugloar server error (503) while solving ad XFyAyerI."

| Reason | When | Web status |
|---|---|---|
| `BAD_REQUEST` | The server answers 400, e.g. an unknown or expired ad, or an item that can't be bought | 400 |
| `GAME_NOT_FOUND` | 404: the game doesn't exist or is already over | 404 |
| `GAME_EXPIRED` | 410: the game session has expired | 410 |
| `SERVER_ERROR` | Any 5xx | 502 |
| `UNEXPECTED_STATUS` | Any other non-2xx status | 502 |
| `NETWORK_UNAVAILABLE` | No internet, a timeout or a DNS failure | 503 |
| `UNREADABLE_RESPONSE` | The body can't be read into the expected record | 502 |
| `MISSING_GAME_ID` | Starting a game succeeded, but the response has no game ID | 502 |

A successful message board or shop download with an empty body returns an empty list.

## Project layout
```
src/main/java/com/akbori/dgtrainapp/
  DragonTrainerApplication.java   Spring Boot entry point
  web/                            GameController (REST API), ApiExceptionHandler (error format), WebConfig (CORS, API docs)
    dto/                          the JSON answers: status, ads, shop items, history, action results
  game/                           game rules: expiry, gold, lives, boards, player status, history
    check/                        checks run before an action, and the reasons they refuse it
  service/                        GameService: one method per player action; MugloarService: calls to the Mugloar REST API
    api/                          MugloarApiException, and the MugloarError and MugloarOperation enums
  dto/                            records for API requests and responses
  model/                          JPA entities stored in SQLite, and the EntryType and RiskCategory enums
  repository/                     Spring Data repositories
src/main/resources/data/
  mugloar_schema.sql              DDL to create the database
data/
  drogontrainapp.db               your local SQLite database (not in git)
```

### Database tables
| Table | Holds |
|---|---|
| `player_status` | Latest lives, gold, level, score, high score and turn per game |
| `cached_ad` | The downloaded message board per game, with its risk category, the download turn and a `used` flag for ads already attempted |
| `cached_item` | The shop items per game |
| `mission_log` | Every mission attempt and purchase, with its result. `entry_type` is `MISSION` or `PURCHASE`; for a purchase, `ad_id` holds the item ID and `reward` the gold spent |
| `app_config` | Settings, such as `LAST_GAME_ID` for resuming |

## Branches
| Branch | Purpose |
|---|---|
| `release` | The default branch: the code that is released |
| `stage` | Testing before a release |
| `develop` | Finished features, merged together |
| `feature/<ticket>_<description>` | One feature, e.g. `feature/BGBNK_1017_build_dragon_trainer_app_game_frontend` |

Changes flow `feature/...` → `develop` → `stage` → `release`.

## Tests
```bash
./mvnw test
```

| Test class | What it covers |
|---|---|
| `GameControllerTest` | Each route, the JSON shape, the error format for broken rules, server and unexpected errors, and CORS |
| `ApiFlowIntegrationTest` | A whole game through the API (start, download ads, solve, shop, buy, history) with real storage and a mocked Mugloar server; the Swagger docs |
| `GameServiceTest` | Each action checks the rules, calls the server only when allowed, and stores the result |
| `MugloarServiceTest` | Every API call against a mocked server: success, each error status, network failure, an unreadable body and a missing game ID, plus ad decoding |
| `MugloarErrorTest` | Each server status maps to the right `MugloarError` |
| `MugloarServiceLiveTest` | The same endpoints against the real Mugloar server (needs network; tagged `live`) |
| `RiskCategoryTest` | Every probability text maps to the right risk category, ignoring case and spaces; unknown text is `UNKNOWN` |
| `GameRulesTest` | Ad expiry, affording an item, gold won or spent per history entry, and the life states |
| `BoardFactoryTest` | The message board keeps used flags and stamps the download turn; the market is downloaded once per game |
| `PlayerStatusUpdaterTest` | The stored status after a game start, a solved ad, a purchase and a reputation check; the high score; purchase history entries |
| `GameChecksTest` | Every check and the order failures are reported in, and picking by board number |
| `*RepositoryTest` | Each repository, against a throwaway in-memory SQLite database |
| `DragonTrainerApplicationTests` | The Spring context starts, using an in-memory database |

To run one test class:

```bash
./mvnw test -Dtest=GameChecksTest
```

To skip the live test, which needs the network:

```bash
./mvnw test -Dtest='!MugloarServiceLiveTest' -Dsurefire.failIfNoSpecifiedTests=false
```