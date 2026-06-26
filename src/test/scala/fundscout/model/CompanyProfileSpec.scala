package fundscout.model

import java.time.LocalDate

class CompanyProfileSpec extends munit.FunSuite:

  private val sequoia = Investor("sequoia", "Sequoia", InvestorTier.Tier1)
  private val a16z = Investor("a16z", "Andreessen Horowitz", InvestorTier.Tier1)
  private val angel = Investor("jane", "Jane Doe", InvestorTier.Tier3)
  private val sf = Location("USA", "San Francisco")

  /** Build a funding event with sensible defaults for the fields under test. */
  private def event(
      id: String,
      stage: FundingStage,
      amountUsd: BigDecimal,
      date: LocalDate,
      companyId: String = "acme",
      companyName: String = "Acme",
      sector: Sector = Sector.AI,
      hq: Location = sf,
      investors: List[Investor] = Nil,
      lead: Option[Investor] = None,
      postMoney: Option[BigDecimal] = None
  ): FundingEvent =
    FundingEvent(
      id = id,
      companyId = companyId,
      companyName = companyName,
      stage = stage,
      amount = Money.usd(amountUsd),
      date = date,
      sector = sector,
      headquarters = hq,
      investors = investors,
      leadInvestor = lead,
      postMoneyValuation = postMoney.map(Money.usd)
    )

  test("an empty profile has no events and zero capital") {
    val p = CompanyProfile.start("acme", "Acme", Sector.AI, sf)
    assert(p.isEmpty)
    assertEquals(p.roundCount, 0)
    assertEquals(p.totalCapitalRaised, Money.usd(0))
    assertEquals(p.latestStage, None)
    assert(!p.hasExited)
  }

  test("folding events accumulates capital and history") {
    val events = List(
      event("e1", FundingStage.Seed, 2_000_000, LocalDate.of(2022, 1, 1),
        investors = List(angel)),
      event("e2", FundingStage.SeriesA, 12_000_000, LocalDate.of(2023, 6, 1),
        investors = List(sequoia), lead = Some(sequoia),
        postMoney = Some(60_000_000)),
      event("e3", FundingStage.SeriesB, 40_000_000, LocalDate.of(2024, 9, 1),
        investors = List(a16z, sequoia), lead = Some(a16z),
        postMoney = Some(240_000_000))
    )
    val profile = CompanyProfile.fromEvents(events).toOption.get

    assertEquals(profile.roundCount, 3)
    assertEquals(profile.totalCapitalRaised, Money.usd(54_000_000))
    assertEquals(profile.latestStage, Some(FundingStage.SeriesB))
    assertEquals(profile.firstFundingDate, Some(LocalDate.of(2022, 1, 1)))
    assertEquals(profile.lastFundingDate, Some(LocalDate.of(2024, 9, 1)))
    assertEquals(profile.latestValuation, Some(Money.usd(240_000_000)))
    assertEquals(profile.investors.map(_.id), Set("jane", "sequoia", "a16z"))
    assertEquals(profile.leadInvestors.map(_.id), Set("sequoia", "a16z"))
  }

  test("events are kept in chronological order regardless of insert order") {
    val seed = CompanyProfile.start("acme", "Acme", Sector.AI, sf)
    val late = event("e2", FundingStage.SeriesA, 10_000_000, LocalDate.of(2024, 1, 1))
    val early = event("e1", FundingStage.Seed, 2_000_000, LocalDate.of(2022, 1, 1))

    val profile = (for
      p1 <- seed.addEvent(late)
      p2 <- p1.addEvent(early)
    yield p2).toOption.get

    assertEquals(profile.events.map(_.id), Vector("e1", "e2"))
    assertEquals(profile.latestStage, Some(FundingStage.SeriesA))
  }

  test("current name/sector/headquarters track the latest event") {
    val seed = CompanyProfile.start("acme", "Acme", Sector.Other, sf)
    val london = Location("UK", "London")
    val renamed = event("e1", FundingStage.SeriesA, 10_000_000,
      LocalDate.of(2024, 1, 1), companyName = "Acme AI",
      sector = Sector.AI, hq = london)

    val profile = seed.addEvent(renamed).toOption.get
    assertEquals(profile.name, "Acme AI")
    assertEquals(profile.sector, Sector.AI)
    assertEquals(profile.headquarters, london)
  }

  test("acquisition counts as an exit but not as capital raised") {
    val events = List(
      event("e1", FundingStage.SeriesA, 10_000_000, LocalDate.of(2023, 1, 1)),
      event("e2", FundingStage.Acquisition, 300_000_000, LocalDate.of(2025, 1, 1))
    )
    val profile = CompanyProfile.fromEvents(events).toOption.get
    assert(profile.hasExited)
    assertEquals(profile.totalCapitalRaised, Money.usd(10_000_000))
  }

  test("age and recency are computed from known dates") {
    val founded = LocalDate.of(2020, 1, 1)
    val events = List(
      event("e1", FundingStage.Seed, 1_000_000, LocalDate.of(2021, 1, 1))
    )
    val profile = CompanyProfile.fromEvents(events, Some(founded)).toOption.get
    val asOf = LocalDate.of(2025, 1, 1)
    assertEquals(profile.ageInYears(asOf), Some(5L))
    assertEquals(profile.daysSinceLastFunding(asOf), Some(1461L))
  }

  test("rejects an event for a different company") {
    val seed = CompanyProfile.start("acme", "Acme", Sector.AI, sf)
    val wrong = event("e1", FundingStage.Seed, 1_000_000,
      LocalDate.of(2022, 1, 1), companyId = "globex")
    seed.addEvent(wrong) match
      case Left(ProfileError.CompanyMismatch("acme", "globex")) => ()
      case other => fail(s"expected CompanyMismatch, got $other")
  }

  test("rejects an event before the founding date") {
    val founded = LocalDate.of(2022, 1, 1)
    val seed = CompanyProfile.start("acme", "Acme", Sector.AI, sf, Some(founded))
    val tooEarly = event("e1", FundingStage.Seed, 1_000_000,
      LocalDate.of(2021, 12, 31))
    assert(seed.addEvent(tooEarly).isLeft)
  }

  test("rejects an event in a different currency") {
    val seed = CompanyProfile.start("acme", "Acme", Sector.AI, sf)
    val usd = event("e1", FundingStage.Seed, 1_000_000, LocalDate.of(2022, 1, 1))
    val eur = FundingEvent(
      id = "e2",
      companyId = "acme",
      companyName = "Acme",
      stage = FundingStage.SeriesA,
      amount = Money(5_000_000, Currency.EUR),
      date = LocalDate.of(2023, 1, 1),
      sector = Sector.AI,
      headquarters = sf
    )
    val result = seed.addEvent(usd).flatMap(_.addEvent(eur))
    result match
      case Left(ProfileError.CurrencyMismatch(Currency.USD, Currency.EUR)) => ()
      case other => fail(s"expected CurrencyMismatch, got $other")
  }

  test("fromEvents fails on an empty collection") {
    assertEquals(CompanyProfile.fromEvents(Nil), Left(ProfileError.NoEvents))
  }
