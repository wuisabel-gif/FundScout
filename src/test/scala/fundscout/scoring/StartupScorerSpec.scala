package fundscout.scoring

import fundscout.analytics.MarketAnalytics
import fundscout.ingest.CompanyRegistry
import fundscout.model.*
import java.time.LocalDate

class StartupScorerSpec extends munit.FunSuite:

  private val sequoia = Investor("sequoia", "Sequoia", InvestorTier.Tier1)
  private val a16z = Investor("a16z", "a16z", InvestorTier.Tier1)
  private val yc = Investor("yc", "Y Combinator", InvestorTier.Tier2)

  private def event(
      id: String,
      companyId: String,
      name: String,
      stage: FundingStage,
      amountUsd: BigDecimal,
      date: LocalDate,
      sector: Sector,
      country: String,
      investors: List[Investor] = Nil,
      lead: Option[Investor] = None,
      postMoney: Option[BigDecimal] = None
  ): FundingEvent =
    FundingEvent(id, companyId, name, stage, Money.usd(amountUsd), date,
      sector, Location(country, "City"), investors, lead, postMoney.map(Money.usd))

  /** Acme (a strong AI company) plus Globex to populate the market. */
  private val registry: CompanyRegistry =
    val events = List(
      event("a1", "acme", "Acme", FundingStage.Seed, 2_000_000,
        LocalDate.of(2021, 1, 1), Sector.AI, "USA", List(yc), Some(yc)),
      event("a2", "acme", "Acme", FundingStage.SeriesA, 10_000_000,
        LocalDate.of(2022, 1, 1), Sector.AI, "USA", List(sequoia), Some(sequoia)),
      event("a3", "acme", "Acme", FundingStage.SeriesB, 50_000_000,
        LocalDate.of(2023, 1, 1), Sector.AI, "USA", List(a16z, sequoia),
        Some(a16z), postMoney = Some(1_200_000_000)),
      event("g1", "globex", "Globex", FundingStage.SeriesA, 6_000_000,
        LocalDate.of(2022, 1, 1), Sector.Fintech, "UK", List(sequoia), Some(sequoia))
    )
    val (reg, errors) = CompanyRegistry.empty.ingestAll(events)
    assertEquals(errors, Nil)
    reg

  private val market = MarketAnalytics.of(registry)
  private val asOf = LocalDate.of(2023, 6, 1)

  test("a strong company scores high with deterministic components") {
    val acme = registry.get("acme").get
    val score = StartupScorer.score(acme, market, asOf)

    def value(c: ScoreComponent): Int = score.component(c).get.value
    assertEquals(value(ScoreComponent.GrowthScore), 75)
    assertEquals(value(ScoreComponent.FundingMomentum), 85)
    assertEquals(value(ScoreComponent.InvestorStrength), 90)
    assertEquals(value(ScoreComponent.EcosystemStrength), 85)
    assertEquals(value(ScoreComponent.MarketConfidence), 85)
    assertEquals(value(ScoreComponent.CapitalEfficiency), 80)
    assertEquals(value(ScoreComponent.ExpansionPotential), 65)
    // Acme has no founder data, so the founder component stays neutral.
    assertEquals(value(ScoreComponent.FounderPedigree), 50)

    // Weighted average of the components lands at 77/100.
    assertEquals(score.overall, 77)
  }

  test("a strong company's reasons explain the score") {
    val acme = registry.get("acme").get
    val score = StartupScorer.score(acme, market, asOf)
    val positives = score.positives.map(_.description)
    assert(positives.exists(_.startsWith("Tier-1 lead investor")), positives)
    assert(positives.contains("Round sizes grew every round"), positives)
    assert(positives.exists(_.contains("Unicorn-scale valuation")), positives)
    assert(score.negatives.isEmpty, score.negatives)
  }

  test("a stale, investor-less company scores low with negative reasons") {
    val zombie = CompanyProfile
      .fromEvents(
        List(
          event("z1", "zombie", "Zombie", FundingStage.Seed, 1_000_000,
            LocalDate.of(2018, 1, 1), Sector.Other, "Canada")
        )
      )
      .toOption
      .get
    val score = StartupScorer.score(zombie, market, asOf)

    assert(score.overall < 50, score.overall)
    val negatives = score.negatives.map(_.description)
    assert(negatives.contains("No disclosed investors"), negatives)
    assert(
      negatives.exists(_.contains("Large funding gap")),
      negatives
    )
  }

  test("a down round is flagged as a growth negative") {
    val down = CompanyProfile
      .fromEvents(
        List(
          event("d1", "down", "DownCo", FundingStage.SeriesA, 30_000_000,
            LocalDate.of(2021, 1, 1), Sector.AI, "USA"),
          event("d2", "down", "DownCo", FundingStage.SeriesB, 12_000_000,
            LocalDate.of(2023, 1, 1), Sector.AI, "USA")
        )
      )
      .toOption
      .get
    val growth = StartupScorer
      .score(down, market, asOf)
      .component(ScoreComponent.GrowthScore)
      .get
    assert(growth.value < 50, growth)
    assert(
      growth.reasons.exists(_.description.contains("smaller than the previous")),
      growth.reasons
    )
  }

  test("a field-defining founder lifts the score, with an explaining reason") {
    val withFounder = CompanyProfile
      .fromEvents(
        List(
          event("w1", "worldlike", "WorldLike", FundingStage.Seed, 5_000_000,
            LocalDate.of(2024, 1, 1), Sector.AI, "USA",
            investors = List(sequoia), lead = Some(sequoia))
            .copy(founders = List(Founder("Famous Professor", FounderPedigree.Luminary)))
        )
      )
      .toOption
      .get
    val noFounder = CompanyProfile
      .fromEvents(
        List(
          event("n1", "plain", "PlainCo", FundingStage.Seed, 5_000_000,
            LocalDate.of(2024, 1, 1), Sector.AI, "USA",
            investors = List(sequoia), lead = Some(sequoia))
        )
      )
      .toOption
      .get
    val asOfRecent = LocalDate.of(2024, 6, 1)

    val founderComp = StartupScorer.score(withFounder, market, asOfRecent)
      .component(ScoreComponent.FounderPedigree).get
    assertEquals(founderComp.value, 85) // 50 baseline + 35 for a luminary
    assert(
      founderComp.reasons.exists(_.description.contains("Field-defining founder")),
      founderComp.reasons
    )
    assert(
      StartupScorer.score(withFounder, market, asOfRecent).overall >
        StartupScorer.score(noFounder, market, asOfRecent).overall
    )
  }

  test("a critical risk flag lowers market confidence with a negative reason") {
    val flagged = CompanyProfile
      .fromEvents(
        List(
          event("r1", "risky", "RiskyCo", FundingStage.SeriesB, 50_000_000,
            LocalDate.of(2024, 1, 1), Sector.AI, "USA",
            investors = List(a16z), lead = Some(a16z),
            postMoney = Some(1_200_000_000))
            .copy(riskFlags = List(
              RiskFlag(RiskSeverity.Critical, "Founders left for a competitor",
                Some(LocalDate.of(2024, 8, 1)))))
        )
      )
      .toOption
      .get
    val clean = CompanyProfile
      .fromEvents(
        List(
          event("c1", "clean", "CleanCo", FundingStage.SeriesB, 50_000_000,
            LocalDate.of(2024, 1, 1), Sector.AI, "USA",
            investors = List(a16z), lead = Some(a16z),
            postMoney = Some(1_200_000_000))
        )
      )
      .toOption
      .get
    val asOfRecent = LocalDate.of(2024, 6, 1)

    val flaggedMc = StartupScorer.score(flagged, market, asOfRecent)
      .component(ScoreComponent.MarketConfidence).get
    val cleanMc = StartupScorer.score(clean, market, asOfRecent)
      .component(ScoreComponent.MarketConfidence).get

    assert(flaggedMc.value < cleanMc.value, (flaggedMc.value, cleanMc.value))
    assert(
      flaggedMc.reasons.exists(r =>
        r.polarity == Polarity.Negative && r.description.contains("Founders left")),
      flaggedMc.reasons
    )
  }

  test("component weights sum to one") {
    assert(math.abs(ScoreComponent.values.map(_.weight).sum - 1.0) < 1e-9)
  }
