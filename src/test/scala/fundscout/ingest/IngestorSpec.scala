package fundscout.ingest

import fundscout.model.*

class IngestorSpec extends munit.FunSuite:

  private def record(
      id: String,
      companyId: String,
      companyName: String,
      stage: String,
      amount: String,
      date: String,
      currency: String = "USD"
  ): RawFundingRecord =
    RawFundingRecord(
      id = id,
      companyId = companyId,
      companyName = companyName,
      stage = stage,
      amount = amount,
      date = date,
      sector = "AI",
      country = "USA",
      currency = currency
    )

  test("builds evolving profiles across multiple companies and rounds") {
    val records = List(
      record("e1", "acme", "Acme", "Seed", "2000000", "2022-01-01"),
      record("e2", "acme", "Acme", "Series A", "12000000", "2023-06-01"),
      record("e3", "globex", "Globex", "Seed", "1500000", "2023-02-01")
    )
    val result = Ingestor.run(records)
    assert(result.isClean, result)
    assertEquals(result.registry.size, 2)

    val acme = result.registry.get("acme").get
    assertEquals(acme.roundCount, 2)
    assertEquals(acme.totalCapitalRaised, Money.usd(14_000_000))
    assertEquals(acme.latestStage, Some(FundingStage.SeriesA))

    val globex = result.registry.get("globex").get
    assertEquals(globex.roundCount, 1)
  }

  test("a parse failure does not abort the batch") {
    val records = List(
      record("e1", "acme", "Acme", "Seed", "2000000", "2022-01-01"),
      record("e2", "acme", "Acme", "not-a-stage", "1", "2023-01-01")
    )
    val result = Ingestor.run(records)
    assertEquals(result.registry.size, 1)
    assertEquals(result.parseFailures.map(_.record.id), List("e2"))
    assertEquals(result.registry.get("acme").get.roundCount, 1)
  }

  test("a currency-inconsistent event is reported as a profile failure") {
    val records = List(
      record("e1", "acme", "Acme", "Seed", "2000000", "2022-01-01", "USD"),
      record("e2", "acme", "Acme", "Series A", "5000000", "2023-01-01", "EUR")
    )
    val result = Ingestor.run(records)
    assertEquals(result.parseFailures, Nil)
    assertEquals(result.profileFailures.map(_._1.id), List("e2"))
    result.profileFailures.head._2 match
      case ProfileError.CurrencyMismatch(Currency.USD, Currency.EUR) => ()
      case other => fail(s"expected CurrencyMismatch, got $other")
    assertEquals(result.registry.get("acme").get.roundCount, 1)
  }

  test("ingesting into an existing registry accumulates") {
    val first = Ingestor.run(
      List(record("e1", "acme", "Acme", "Seed", "2000000", "2022-01-01"))
    )
    val second = Ingestor.run(
      List(record("e2", "acme", "Acme", "Series A", "12000000", "2023-06-01")),
      into = first.registry
    )
    assertEquals(second.registry.get("acme").get.roundCount, 2)
  }
