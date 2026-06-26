package fundscout.ingest

import fundscout.model.*
import java.time.LocalDate

class FundingEventParserSpec extends munit.FunSuite:

  /** A fully valid record; tests override only the field under test. */
  private def validRecord: RawFundingRecord = RawFundingRecord(
    id = "evt-1",
    companyId = "acme",
    companyName = "Acme",
    stage = "Series A",
    amount = "12,000,000",
    date = "2024-03-01",
    sector = "AI",
    country = "USA",
    city = Some("San Francisco"),
    investors = List(
      RawInvestor("sequoia", "Sequoia", Some("Tier1")),
      RawInvestor("jane", "Jane Doe")
    ),
    leadInvestorId = Some("sequoia"),
    postMoneyValuation = Some("60000000")
  )

  test("parses a valid record, tolerating $/comma formatting") {
    val event = FundingEventParser.parse(validRecord).toOption.get
    assertEquals(event.id, "evt-1")
    assertEquals(event.stage, FundingStage.SeriesA)
    assertEquals(event.amount, Money.usd(12_000_000))
    assertEquals(event.date, LocalDate.of(2024, 3, 1))
    assertEquals(event.sector, Sector.AI)
    assertEquals(event.headquarters, Location("USA", "San Francisco"))
    assertEquals(event.leadInvestor.map(_.id), Some("sequoia"))
    assertEquals(event.investors.find(_.id == "jane").map(_.tier), Some(InvestorTier.Unknown))
  }

  test("'$60M' valuation requires no abbreviation expansion — only digits") {
    // The parser strips $ and commas but does not expand 'M'; ensure plain
    // numeric valuations parse and abbreviated ones are rejected loudly.
    val ok = FundingEventParser.parse(validRecord.copy(postMoneyValuation = Some("60000000")))
    assertEquals(ok.toOption.get.postMoneyValuation, Some(Money.usd(60_000_000)))

    val bad = FundingEventParser.parse(validRecord.copy(postMoneyValuation = Some("$60M")))
    assertEquals(bad, Left(List(IngestError.InvalidValuation("$60M"))))
  }

  test("unknown sector falls back to Other rather than failing") {
    val event = FundingEventParser
      .parse(validRecord.copy(sector = "Underwater Basket Weaving"))
      .toOption
      .get
    assertEquals(event.sector, Sector.Other)
  }

  test("accumulates every error in one record") {
    val broken = validRecord.copy(
      id = "  ",
      stage = "Series Q",
      amount = "lots",
      date = "March 2024",
      leadInvestorId = Some("ghost")
    )
    val errors = FundingEventParser.parse(broken).left.toOption.get.toSet
    assert(errors.contains(IngestError.MissingField("id")), errors)
    assert(errors.contains(IngestError.UnknownStage("Series Q")), errors)
    assert(errors.contains(IngestError.InvalidAmount("lots")), errors)
    assert(errors.contains(IngestError.InvalidDate("March 2024")), errors)
    assert(errors.contains(IngestError.LeadInvestorNotListed("ghost")), errors)
  }

  test("lead investor must be among listed investors") {
    val r = validRecord.copy(leadInvestorId = Some("a16z"))
    assertEquals(
      FundingEventParser.parse(r),
      Left(List(IngestError.LeadInvestorNotListed("a16z")))
    )
  }

  test("rejects an unrecognized investor tier") {
    val r = validRecord.copy(
      investors = List(RawInvestor("x", "X", Some("platinum"))),
      leadInvestorId = None
    )
    assertEquals(
      FundingEventParser.parse(r),
      Left(List(IngestError.UnknownInvestorTier("platinum")))
    )
  }

  test("parseAll separates successes from failures, preserving order") {
    val good = validRecord
    val bad = validRecord.copy(id = "evt-2", stage = "nope")
    val (events, failures) = FundingEventParser.parseAll(List(good, bad))
    assertEquals(events.map(_.id), List("evt-1"))
    assertEquals(failures.map(_.record.id), List("evt-2"))
    assertEquals(failures.head.errors, List(IngestError.UnknownStage("nope")))
  }
