package fundscout.prediction

import fundscout.analytics.MarketAnalytics
import fundscout.ingest.CompanyRegistry
import fundscout.model.*
import java.time.LocalDate

class PredictionEngineSpec extends munit.FunSuite:

  private val sequoia = Investor("sequoia", "Sequoia", InvestorTier.Tier1)
  private val yc = Investor("yc", "Y Combinator", InvestorTier.Tier2)

  private def event(
      id: String,
      companyId: String,
      name: String,
      stage: FundingStage,
      amountUsd: BigDecimal,
      date: LocalDate,
      sector: Sector = Sector.AI,
      investors: List[Investor] = Nil,
      lead: Option[Investor] = None,
      postMoney: Option[BigDecimal] = None
  ): FundingEvent =
    FundingEvent(id, companyId, name, stage, Money.usd(amountUsd), date, sector,
      Location("USA", "City"), investors, lead, postMoney.map(Money.usd))

  private def marketOf(events: List[FundingEvent]) =
    val (reg, _) = CompanyRegistry.empty.ingestAll(events)
    MarketAnalytics.of(reg)

  private def profileOf(events: List[FundingEvent]): CompanyProfile =
    CompanyProfile.fromEvents(events).toOption.get

  test("a healthy early-stage company is likely to raise again, with a stepped-up round") {
    val events = List(
      event("e1", "newco", "NewCo", FundingStage.Seed, 2_000_000,
        LocalDate.of(2021, 1, 1), investors = List(yc), lead = Some(yc)),
      event("e2", "newco", "NewCo", FundingStage.SeriesA, 10_000_000,
        LocalDate.of(2022, 1, 1), investors = List(sequoia), lead = Some(sequoia))
    )
    val forecast = PredictionEngine.forecast(
      profileOf(events), marketOf(events), LocalDate.of(2022, 6, 1)
    )

    // base 0.6 + early(0.15) + tier1(0.10) + recent(0.05) = 0.90
    assertEquals(forecast.nextRound.probability.percent, 90)
    assertEquals(forecast.nextRound.expectedAmount, Some((Money.usd(15_000_000), Money.usd(30_000_000))))
    // one 365-day gap -> ~12 months -> 9..15
    assertEquals(forecast.nextRound.expectedMonths, Some((9, 15)))
  }

  test("an already-acquired company has no next round and a certain acquisition outcome") {
    val events = List(
      event("e1", "exitco", "ExitCo", FundingStage.SeriesA, 10_000_000, LocalDate.of(2021, 1, 1)),
      event("e2", "exitco", "ExitCo", FundingStage.Acquisition, 300_000_000, LocalDate.of(2023, 1, 1))
    )
    val forecast = PredictionEngine.forecast(
      profileOf(events), marketOf(events), LocalDate.of(2024, 1, 1)
    )

    assertEquals(forecast.nextRound.probability, Probability.Zero)
    assertEquals(forecast.nextRound.expectedAmount, None)
    assertEquals(forecast.outcome(Outcome.Acquisition).get.probability, Probability.One)
    assertEquals(forecast.outcome(Outcome.Inactivity).get.probability, Probability.Zero)
  }

  test("a unicorn-valued company has a certain unicorn outcome and an IPO signal") {
    val events = List(
      event("e1", "uni", "UniCo", FundingStage.SeriesB, 50_000_000,
        LocalDate.of(2023, 1, 1), postMoney = Some(1_200_000_000))
    )
    val forecast = PredictionEngine.forecast(
      profileOf(events), marketOf(events), LocalDate.of(2023, 6, 1)
    )

    assertEquals(forecast.outcome(Outcome.Unicorn).get.probability, Probability.One)
    val ipo = forecast.outcome(Outcome.Ipo).get
    assert(ipo.reasons.contains("Unicorn-scale valuation"), ipo.reasons)
  }

  test("a long-dormant company skews toward inactivity and away from raising") {
    val events = List(
      event("e1", "ghost", "GhostCo", FundingStage.SeriesA, 8_000_000, LocalDate.of(2018, 1, 1))
    )
    val forecast = PredictionEngine.forecast(
      profileOf(events), marketOf(events), LocalDate.of(2025, 1, 1)
    )

    val inactivity = forecast.outcome(Outcome.Inactivity).get
    assert(inactivity.probability.value >= 0.6, inactivity.probability)
    assert(inactivity.reasons.exists(_.contains("over three years")), inactivity.reasons)
    // early-stage bump (+0.15) is outweighed by the 3-year gap penalty (-0.30)
    assert(forecast.nextRound.probability.value < 0.6, forecast.nextRound.probability)
    assert(
      forecast.nextRound.reasons.exists(_.contains("may be dormant")),
      forecast.nextRound.reasons
    )
  }

  test("a critical risk flag raises failure odds and lowers next-round likelihood") {
    def co(flags: List[fundscout.model.RiskFlag]) = CompanyProfile
      .fromEvents(List(
        event("e1", "x", "X", FundingStage.SeriesA, 20_000_000,
          LocalDate.of(2024, 1, 1), investors = List(sequoia), lead = Some(sequoia))
          .copy(riskFlags = flags)))
      .toOption.get
    val asOfRecent = LocalDate.of(2024, 6, 1)
    val flag = fundscout.model.RiskFlag(
      fundscout.model.RiskSeverity.Critical, "Key team departed", Some(LocalDate.of(2024, 5, 1)))

    val flagged = PredictionEngine.forecast(co(List(flag)), marketOf(Nil), asOfRecent)
    val clean = PredictionEngine.forecast(co(Nil), marketOf(Nil), asOfRecent)

    assert(
      flagged.outcome(Outcome.Inactivity).get.probability.value >
        clean.outcome(Outcome.Inactivity).get.probability.value
    )
    assert(flagged.nextRound.probability.value < clean.nextRound.probability.value)
    assert(
      flagged.nextRound.reasons.exists(_.contains("Key team departed")),
      flagged.nextRound.reasons
    )
  }

  test("probabilities never escape [0,1]") {
    assertEquals(Probability.clamp(1.7), Probability.One)
    assertEquals(Probability.clamp(-0.3), Probability.Zero)
    intercept[IllegalArgumentException](Probability(1.5))
  }
