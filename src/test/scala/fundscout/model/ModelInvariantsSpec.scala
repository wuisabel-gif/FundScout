package fundscout.model

import java.time.LocalDate

class ModelInvariantsSpec extends munit.FunSuite:

  private val sequoia = Investor("sequoia", "Sequoia", InvestorTier.Tier1)
  private val angel = Investor("jane", "Jane Doe", InvestorTier.Unknown)

  test("Location displays with and without a city") {
    assertEquals(Location("USA", "San Francisco").display, "San Francisco, USA")
    assertEquals(Location("USA").display, "USA")
  }

  test("Location rejects a blank country") {
    intercept[IllegalArgumentException](Location("   "))
  }

  test("Investor rejects blank id or name") {
    intercept[IllegalArgumentException](Investor("", "X"))
    intercept[IllegalArgumentException](Investor("x", "  "))
  }

  test("FundingEvent requires the lead investor to participate") {
    intercept[IllegalArgumentException] {
      FundingEvent(
        id = "evt-1",
        companyId = "acme",
        companyName = "Acme",
        stage = FundingStage.SeriesA,
        amount = Money.usd(10_000_000),
        date = LocalDate.of(2025, 1, 1),
        sector = Sector.AI,
        headquarters = Location("USA", "San Francisco"),
        investors = List(angel),
        leadInvestor = Some(sequoia) // not in investors
      )
    }
  }

  test("FundingEvent accepts a lead investor that participates") {
    val event = FundingEvent(
      id = "evt-1",
      companyId = "acme",
      companyName = "Acme",
      stage = FundingStage.SeriesA,
      amount = Money.usd(10_000_000),
      date = LocalDate.of(2025, 1, 1),
      sector = Sector.AI,
      headquarters = Location("USA", "San Francisco"),
      investors = List(sequoia, angel),
      leadInvestor = Some(sequoia)
    )
    assertEquals(event.investorIds, Set("sequoia", "jane"))
    assertEquals(event.year, 2025)
  }
