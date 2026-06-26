package fundscout.valuation

import fundscout.analytics.MarketAnalytics
import fundscout.ingest.CompanyRegistry
import fundscout.model.*
import java.time.LocalDate

class ValuationEngineSpec extends munit.FunSuite:

  private val sequoia = Investor("sequoia", "Sequoia", InvestorTier.Tier1)

  private def event(
      id: String,
      companyId: String,
      name: String,
      stage: FundingStage,
      amountUsd: BigDecimal,
      date: LocalDate,
      sector: Sector = Sector.AI,
      country: String = "USA",
      investors: List[Investor] = Nil,
      lead: Option[Investor] = None,
      postMoney: Option[BigDecimal] = None
  ): FundingEvent =
    FundingEvent(id, companyId, name, stage, Money.usd(amountUsd), date, sector,
      Location(country, "City"), investors, lead, postMoney.map(Money.usd))

  private def contextFrom(registry: CompanyRegistry): ValuationContext =
    ValuationContext.from(registry, MarketAnalytics.of(registry))

  private def registryOf(events: List[FundingEvent]): CompanyRegistry =
    val (reg, errors) = CompanyRegistry.empty.ingestAll(events)
    assertEquals(errors, Nil)
    reg

  test("anchors on a recently disclosed valuation with a tight, high-confidence range") {
    val registry = registryOf(
      List(
        event("a1", "acme", "Acme", FundingStage.Seed, 2_000_000, LocalDate.of(2021, 1, 1)),
        event("a2", "acme", "Acme", FundingStage.SeriesA, 10_000_000, LocalDate.of(2022, 1, 1)),
        event("a3", "acme", "Acme", FundingStage.SeriesB, 50_000_000,
          LocalDate.of(2023, 1, 1), postMoney = Some(1_200_000_000))
      )
    )
    val acme = registry.get("acme").get
    val estimate = ValuationEngine.estimate(acme, contextFrom(registry), LocalDate.of(2023, 4, 1))

    assertEquals(estimate.confidence, Confidence.High)
    assertEquals(estimate.low, Money.usd(1_080_000_000)) // 1.2B * 0.90
    assertEquals(estimate.high, Money.usd(1_320_000_000)) // 1.2B * 1.10
    assert(estimate.reasoning.contains("Series B"), estimate.reasoning)
    assert(
      estimate.reasoning.exists(_.contains("Disclosed post-money valuation")),
      estimate.reasoning
    )
  }

  test("derives a valuation from comparable multiples and signals") {
    // Comparable AI company sets the sector multiple at 5x (50M / 10M).
    val baseco =
      event("b1", "baseco", "BaseCo", FundingStage.SeriesA, 10_000_000,
        LocalDate.of(2022, 6, 1), postMoney = Some(50_000_000))
    // Target: 20M raised, tier-1 lead, recent, in the top AI sector & top USA hub.
    val newco =
      event("n1", "newco", "NewCo", FundingStage.SeriesA, 20_000_000,
        LocalDate.of(2023, 1, 1), investors = List(sequoia), lead = Some(sequoia))
    val registry = registryOf(List(baseco, newco))
    val estimate = ValuationEngine.estimate(
      registry.get("newco").get,
      contextFrom(registry),
      LocalDate.of(2023, 4, 1)
    )

    assertEquals(estimate.confidence, Confidence.Medium)
    // 20M * 5x * (1.15 * 1.10 * 1.05 * 1.05) = 139,466,250 midpoint.
    assertEquals(estimate.midpoint, Money.usd(139_466_250))
    assertEquals(estimate.low, Money.usd(111_573_000)) // midpoint-ish * 0.8
    assertEquals(estimate.high, Money.usd(167_359_500)) // * 1.2
    assert(estimate.reasoning.exists(_.contains("Comparable Artificial Intelligence")), estimate.reasoning)
    assert(estimate.reasoning.contains("Top-tier lead investor"), estimate.reasoning)
    assert(estimate.reasoning.contains("Strong Artificial Intelligence market"), estimate.reasoning)
  }

  test("stale funding lowers confidence and applies a quiet-funding penalty") {
    val baseco =
      event("b1", "baseco", "BaseCo", FundingStage.SeriesA, 10_000_000,
        LocalDate.of(2018, 1, 1), postMoney = Some(50_000_000))
    val stale =
      event("s1", "stale", "StaleCo", FundingStage.SeriesA, 20_000_000,
        LocalDate.of(2019, 1, 1), investors = List(sequoia), lead = Some(sequoia))
    val registry = registryOf(List(baseco, stale))
    val estimate = ValuationEngine.estimate(
      registry.get("stale").get,
      contextFrom(registry),
      LocalDate.of(2025, 1, 1) // ~6 years later
    )

    assertEquals(estimate.confidence, Confidence.Low)
    assert(
      estimate.reasoning.exists(_.contains("Funding has gone quiet")),
      estimate.reasoning
    )
  }

  test("a company with no capital-raising rounds cannot be valued") {
    val registry = registryOf(
      List(
        event("x1", "x", "X", FundingStage.Acquisition, 300_000_000, LocalDate.of(2024, 1, 1))
      )
    )
    val estimate = ValuationEngine.estimate(
      registry.get("x").get,
      contextFrom(registry),
      LocalDate.of(2024, 6, 1)
    )
    assertEquals(estimate.confidence, Confidence.Low)
    assertEquals(estimate.low, Money.usd(0))
    assertEquals(estimate.reasoning, List("No capital-raising rounds to value"))
  }
