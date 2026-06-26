# FundScout

**A Scala platform for understanding how startups are built, funded, and valued.**

The public markets have a price for everything. Every second, millions of trades
establish a consensus about what a company is worth. Analysts can download decades
of historical prices, earnings, and filings with a few API calls.

The startup ecosystem works differently.

Most of the world's most valuable companies spent years as private businesses,
growing largely out of public view. During that time there is no stock ticker, no
continuously updated market price, and often no reliable estimate of what the
company is worth. Instead, information emerges gradually through funding
announcements, investor interviews, press releases, regulatory filings, founder
posts, and industry reporting. Each announcement reveals a small piece of a much
larger story.

Viewed individually, these events are little more than news.

Viewed together over years, they describe how industries emerge, how investors
recognize opportunities, how founders build enduring companies, and how capital
flows through innovation.

FundScout is built around a simple idea: startup funding announcements are not
isolated events — they are signals. If those signals are collected, validated, and
connected over time, they begin to reveal patterns that are difficult to see by
reading individual articles or spreadsheets.

The platform continuously transforms fundraising events into evolving company
profiles, sector trends, investor networks, valuation estimates, and probabilistic
forecasts. Every prediction is accompanied by an explanation of the evidence that
produced it. The goal is not to replace human judgment, but to augment it with
transparent analytics that scale across thousands of companies and millions of
events.

At its core, FundScout is an exploration of a broader engineering question:

> Can we build an explainable intelligence system for private markets using only
> observable public signals?

The answer is unlikely to be perfect — and it doesn't need to be. Venture investing
has always combined quantitative evidence with qualitative intuition. FundScout
simply attempts to make more of the quantitative side explicit, reproducible, and
searchable.

Technically, FundScout is also a vehicle for exploring modern Scala. The project
emphasizes immutable domain models, event-driven architecture, functional
programming, concurrent stream processing, and explainable statistical modeling.
Rather than using finance as an end goal, it uses the venture ecosystem as a
realistic domain for building scalable backend software.

FundScout is not a trading platform, an investment advisor, or a source of
financial advice. It is an educational software engineering project that explores
how structured business events can be transformed into transparent analytics for
one of the most opaque markets in the world. See [Non-goals](#non-goals).

## Vision

FundScout aims to answer questions such as:

- Which startup sectors are growing the fastest?
- Which investors consistently identify successful companies early?
- Which startups resemble previous unicorns?
- Which funding rounds are unusually large?
- Which geographic regions are becoming new startup hubs?
- How has venture funding changed over time?
- Which companies are most likely to raise another round?
- What factors contribute most to startup valuation?

The platform doesn't just collect data — it learns from historical fundraising
events and turns raw information into **explainable** insights, where every
score, valuation, and prediction can say *why* it is what it is.

## In practice

Two design choices follow directly from how opaque this data is.

Every output is a **range with a confidence level**, not a number with false
precision. When a valuation is disclosed, the model anchors on it
(`Confidence: High`). When it isn't, it estimates from comparable companies and
observable signals and says so (`Confidence: Medium`, with the reasoning
attached). The point isn't to replace the intuition that closes deals — it's to
give that intuition a memory and a second opinion, and to make explicit the
patterns a human tracks informally across hundreds of announcements.

And the ingest layer treats every announcement as an *untrusted* record to be
validated rather than trusted on sight — exactly the shape you want when the raw
material is scraped from noisy, half-reliable public sources. Today those records
come from CSV/JSON files or any HTTP(S) URL serving them; press and social-media
monitoring are a natural next feed into the same pipeline.

## What it's built to catch

The same event-driven pipeline points at two outreach-grade signals — the moments
a venture investor most wants to act on:

**New founders, before the round.** When a researcher or operator leaves a top
lab, university, or company to start something, that departure is itself a signal
— often visible in public professional-network and press activity before any
funding is announced. FundScout already models **founders as first-class data**
(`Founder` + a reputation tier), so the architecture is built to ingest these
departure/launch signals from public sources and surface emerging teams — *"this
person just left to found a company"* — while they're still reachable, before
every fund has them on a list. The goal is a ranked, explainable shortlist of
**who to reach out to, and why**.

**Companies that just raised.** Every new round is an event, and the platform is
event-native: it can flag companies whose latest round is fresh (a brand-new
Series A, say) so an investor can engage while the round — and the relationships
— are current, rather than reading about it months later.

The thesis is the same throughout: turn scattered public breadcrumbs into a
timely, explainable list of *who* and *when*, not just a database of *what
happened*.

> **Honest scope.** Founder modeling and event-driven ingestion are implemented
> today; the live datasets are loaded from files/URLs. Automated
> professional-network and press feeds are roadmap, and would only be built
> within each source's terms of service — FundScout is designed to *receive*
> those signals, not to scrape against a platform's rules.

## Status

The full pipeline is implemented and tested — raw input through analytics,
scoring, valuation, and prediction, to JSON/console output. There is no web
dashboard yet (see [Roadmap](#roadmap)).

- **Domain model** (`fundscout.model`) — immutable core types: funding events,
  funding stages, investors, sectors, locations, money, and evolving company
  profiles.
- **Ingest / validation** (`fundscout.ingest`) — turns untrusted raw records
  into validated funding events (accumulating *all* errors per record) and folds
  them into an immutable registry of company profiles. `Ingestor.run` is the
  end-to-end entry point. **CSV and JSON decoders** (`CsvRecordDecoder`,
  `JsonRecordDecoder`) load files/strings into `RawFundingRecord`s, backed by a
  dependency-free CSV tokenizer and JSON parser in `fundscout.util`, and data can
  be fetched from any HTTP(S) URL (`Http.fetch`) — so a report can be generated
  straight from an online source.
- **Analytics** (`fundscout.analytics`) — `MarketAnalytics.of(registry)`
  computes an ecosystem-wide `MarketReport`: funding by sector/year/country/city,
  median round, average Series A, funding velocity, investor concentration,
  unicorn and exit counts.
- **Scoring** (`fundscout.scoring`) — `StartupScorer.score(profile, market,
  asOf)` produces an explainable 0–100 `StartupScore` from seven weighted
  components (growth, funding momentum, investor strength, ecosystem strength,
  market confidence, capital efficiency, expansion potential), each with signed
  `ScoreReason`s explaining *why* the score is what it is.
- **Valuation** (`fundscout.valuation`) — `ValuationEngine.estimate` produces an
  explainable valuation *range* with a `Confidence` level, anchored on a
  disclosed valuation when available or derived from comparable-company multiples
  and signals otherwise.
- **Prediction** (`fundscout.prediction`) — `PredictionEngine.forecast` estimates
  probabilistic future outcomes: next-round likelihood (with expected size and
  timing) plus independent unicorn / IPO / acquisition / inactivity
  probabilities, each with reasoning.
- **Reporting** (`fundscout.reporting`) — renders the market report, startup
  score, valuation estimate, and forecast to plain text for the console.
- **API** (`fundscout.api`) — a transport-agnostic JSON router (`FundScoutApi`)
  plus a real HTTP server built on the JDK's `com.sun.net.httpserver` (no web
  framework dependency). JSON is produced by a small hand-rolled writer
  (`fundscout.util.Json`).

## Layout

```
build.sbt
src/main/scala/fundscout/
  model/                    # immutable domain model  (implemented)
  ingest/                   # ingestion + validation + CSV/JSON decoders + HTTP fetch  (implemented)
  analytics/                # ecosystem statistics  (implemented)
  scoring/                  # explainable startup scoring  (implemented)
  valuation/                # rule-based valuation  (implemented)
  prediction/               # probabilistic forecasts  (implemented)
  reporting/                # report rendering  (implemented)
  api/                      # JSON REST API + HTTP server  (implemented)
  util/                     # shared helpers (JSON writer/parser, CSV)  (implemented)
src/test/scala/fundscout/   # mirrors the package layout
data/                       # sample / historical / generated datasets
```

The architecture is a one-directional pipeline; every module is independently
testable:

```text
Funding events → Ingest/validation → Company profiles → Analytics
                                                       → Scoring
                                                       → Valuation
                                                       → Prediction
                                                       → Reports / API
```

## Build & test

Requires a JDK (11+) and [sbt](https://www.scala-sbt.org/).

```bash
sbt compile     # compile
sbt test        # run the test suite
sbt run         # run the end-to-end demo: ingest sample data, print a market report
sbt "runMain fundscout.api.fundscoutServer 8080"   # start the JSON HTTP API
sbt "runMain fundscout.fundscoutImport data/sample/funding_rounds.csv"   # import a CSV/JSON file
sbt "runMain fundscout.fundscoutFetch https://example.com/rounds.csv"    # fetch data from a URL
```

### Importing data

`fundscout.fundscoutImport <file>` loads a `.csv` or `.json` file, and
`fundscout.fundscoutFetch <url>` pulls the same formats from an HTTP(S) URL — both
validate the data and print a market report. Online fetching uses the JDK's
`java.net.http.HttpClient` (no dependency); the format is chosen from the URL
extension or the response's `Content-Type`. The shared `text => report` logic
lives in `fundscout.FundScout` and is tested without any network.

CSV columns are matched by name; required columns are
`id, companyId, companyName, stage, amount, date, sector, country`, with optional
`city, currency, investors, leadInvestorId, postMoneyValuation`. Investors are
encoded in one cell as `;`-separated `id:name:tier` entries. JSON input is an
array of record objects (or `{ "records": [...] }`) with the same field names and
an `investors` array of `{ "id", "name", "tier" }`. See
[data/sample/funding_rounds.csv](data/sample/funding_rounds.csv).

### API

With the server running (default port 8080):

```
GET /health                        liveness check
GET /market                        ecosystem analytics report
GET /companies                     list of company summaries
GET /companies/{id}                full company profile + event history
GET /companies/{id}/score          explainable startup score
GET /companies/{id}/valuation      valuation estimate
GET /companies/{id}/forecast       probabilistic forecast
```

```bash
curl http://localhost:8080/market
curl http://localhost:8080/companies/acme/score
```

The routing lives in `fundscout.api.FundScoutApi` as a pure
`(method, path) => ApiResponse` function and is unit-tested without a network;
the server is only a thin transport binding.

## Engineering principles

Prefer: immutable data, strong typing, pure functions, explainable algorithms,
modular design, comprehensive testing, clear documentation.

Avoid: hidden heuristics, global mutable state, black-box predictions, premature
optimization, tight coupling.

Every valuation and prediction is rule-based and transparent — version 1
deliberately uses no machine learning, so each result is traceable to the signals
behind it.

## Why Scala?

The domain is naturally algebraic, and Scala 3 lets the code say so directly:

- **The model mirrors the problem.** A funding round is exactly one of a closed
  set of stages; a future outcome is one of a few cases; validation either
  succeeds or fails with reasons. `enum`s, sealed types, and `case class`es
  express these precisely, and pattern matching makes the rule logic read like
  the rules themselves — the scoring and valuation engines are essentially
  `match` over the signals, which is exactly why they're explainable.
- **Immutability is the path of least resistance.** A company profile is the
  *fold* of its events; with case classes and persistent collections, "update =
  new value" is the default, which fits the event-sourced design and removes a
  whole class of shared-mutable-state bugs.
- **The type system documents and enforces invariants.** Illegal states are hard
  to even construct — `Money` can't be negative, a lead investor must be among
  the participants, a profile's events share one currency — so bad data is caught
  at compile time or construction, not in production.
- **Explainability falls out of pure functions.** Every engine is a pure
  `inputs => result-with-reasons` function: no I/O, no mocks, deterministic. That
  is why the test suite is fast and why a score can be asserted to the exact
  point. Functional error handling (`Either`, accumulation) gives the
  "report *all* problems at once" validation without exceptions.
- **It starts simple and scales.** The JVM, mature tooling, and a clear path to
  concurrent and streaming event processing are there when needed — yet the
  project runs today with zero third-party runtime dependencies (only a test
  library). Learning modern Scala is also an explicit goal of the project.

## Non-goals

FundScout is **not** intended to recommend investments, replace professional
financial analysis, execute trades, predict public stock prices, guarantee
startup success, or provide financial advice. All valuation estimates and
predictions are educational analyses generated from historical patterns and
observable data.

## Roadmap

Next up:

- **Emerging-founder discovery** — ingest public founder-departure and launch
  signals so new teams surface for outreach *before* the round (see
  [What it's built to catch](#what-its-built-to-catch)).
- **Fresh-round alerts** — flag companies whose latest round is recent (a new
  Series A, say) the moment it lands.
- **Historical-replay datasets** under `data/`.

Longer term: investor relationship graph, founder network visualization, sector
trend explorer, similar-company search, scenario simulation ("what if this
company raises another $50M?"), time-series visualization, multi-country
ecosystem comparison, and a machine-learning valuation model.
