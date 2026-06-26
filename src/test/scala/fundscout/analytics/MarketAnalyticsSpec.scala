package fundscout.analytics

import fundscout.ingest.CompanyRegistry
import fundscout.model.*
import java.time.LocalDate

class MarketAnalyticsSpec extends munit.FunSuite:

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
      city: String,
      investors: List[Investor] = Nil,
      lead: Option[Investor] = None,
      postMoney: Option[BigDecimal] = None
  ): FundingEvent =
    FundingEvent(
      id = id,
      companyId = companyId,
      companyName = name,
      stage = stage,
      amount = Money.usd(amountUsd),
      date = date,
      sector = sector,
      headquarters = Location(country, city),
      investors = investors,
      leadInvestor = lead,
      postMoneyValuation = postMoney.map(Money.usd)
    )

  /** Registry: Acme (AI/SF, becomes a unicorn) + Globex (Fintech/London,
    * acquired).
    */
  private def registry: CompanyRegistry =
    val events = List(
      event("a1", "acme", "Acme", FundingStage.Seed, 2_000_000,
        LocalDate.of(2021, 1, 1), Sector.AI, "USA", "San Francisco",
        List(yc), Some(yc)),
      event("a2", "acme", "Acme", FundingStage.SeriesA, 10_000_000,
        LocalDate.of(2022, 1, 1), Sector.AI, "USA", "San Francisco",
        List(sequoia), Some(sequoia)),
      event("a3", "acme", "Acme", FundingStage.SeriesB, 50_000_000,
        LocalDate.of(2023, 1, 1), Sector.AI, "USA", "San Francisco",
        List(a16z, sequoia), Some(a16z), postMoney = Some(1_200_000_000)),
      event("g1", "globex", "Globex", FundingStage.SeriesA, 6_000_000,
        LocalDate.of(2022, 1, 1), Sector.Fintech, "UK", "London",
        List(sequoia), Some(sequoia)),
      event("g2", "globex", "Globex", FundingStage.Acquisition, 300_000_000,
        LocalDate.of(2024, 1, 1), Sector.Fintech, "UK", "London")
    )
    val (reg, errors) = CompanyRegistry.empty.ingestAll(events)
    assertEquals(errors, Nil)
    reg

  test("aggregates capital, excluding acquisitions") {
    val r = MarketAnalytics.of(registry)
    // 2M + 10M + 50M + 6M = 68M; the 300M acquisition is not capital raised.
    assertEquals(r.totalCapitalRaised, Money.usd(68_000_000))
    assertEquals(r.companiesTracked, 2)
    assertEquals(r.reportingCurrency, Currency.USD)
  }

  test("groups funding by sector, country, and city") {
    val r = MarketAnalytics.of(registry)
    assertEquals(r.fundingBySector(Sector.AI), Money.usd(62_000_000))
    assertEquals(r.fundingBySector(Sector.Fintech), Money.usd(6_000_000))
    assertEquals(r.fundingByCountry("USA"), Money.usd(62_000_000))
    assertEquals(r.fundingByCity("London"), Money.usd(6_000_000))
    assertEquals(r.topSectors(1), List(Sector.AI -> Money.usd(62_000_000)))
  }

  test("counts rounds by stage, including exits") {
    val r = MarketAnalytics.of(registry)
    assertEquals(r.roundsByStage(FundingStage.SeriesA), 2)
    assertEquals(r.roundsByStage(FundingStage.Acquisition), 1)
    assertEquals(r.totalRounds, 5)
  }

  test("computes median round and average Series A over capital rounds") {
    val r = MarketAnalytics.of(registry)
    // capital rounds: 2M, 10M, 50M, 6M -> sorted 2,6,10,50 -> median (6+10)/2 = 8M
    assertEquals(r.medianRoundSize, Some(Money.usd(8_000_000)))
    // Series A rounds: 10M and 6M -> average 8M
    assertEquals(r.averageSeriesASize, Some(Money.usd(8_000_000)))
  }

  test("ranks investors by deal count") {
    val r = MarketAnalytics.of(registry)
    assertEquals(r.topInvestors(1).map(a => (a.investor.id, a.deals)), List("sequoia" -> 3))
    assertEquals(r.investorActivity.find(_.investor.id == "a16z").map(_.deals), Some(1))
  }

  test("identifies unicorns and exits") {
    val r = MarketAnalytics.of(registry)
    assertEquals(r.unicornCount, 1)
    assertEquals(r.unicorns.head.companyId, "acme")
    assertEquals(r.exitedCompanies, 1)
  }

  test("funding velocity spans the data's years") {
    val r = MarketAnalytics.of(registry)
    // 5 rounds across 2021..2024 -> 4-year span -> 1.25 rounds/year
    assertEquals(r.fundingVelocityPerYear, Some(1.25))
  }

  test("an empty registry yields an empty report") {
    val r = MarketAnalytics.of(CompanyRegistry.empty)
    assertEquals(r.companiesTracked, 0)
    assertEquals(r.totalCapitalRaised, Money.usd(0))
    assertEquals(r.medianRoundSize, None)
    assertEquals(r.fundingVelocityPerYear, None)
    assertEquals(r.unicornCount, 0)
  }
