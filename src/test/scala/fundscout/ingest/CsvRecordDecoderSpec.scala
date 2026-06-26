package fundscout.ingest

import fundscout.model.{FundingStage, InvestorTier, Money}

class CsvRecordDecoderSpec extends munit.FunSuite:

  test("decodes records, matching columns by name regardless of order") {
    val csv =
      """companyName,id,companyId,stage,date,amount,sector,country,city,investors,leadInvestorId
        |Acme,e1,acme,Series A,2023-01-01,"12,000,000",AI,USA,San Francisco,sequoia:Sequoia:Tier1;jane:Jane Doe,sequoia
        |""".stripMargin
    val result = CsvRecordDecoder.decode(csv)

    assert(result.isClean, result.errors)
    assertEquals(result.records.size, 1)
    val r = result.records.head
    assertEquals(r.id, "e1")
    assertEquals(r.companyName, "Acme")
    assertEquals(r.amount, "12,000,000") // quoted comma preserved
    assertEquals(r.currency, "USD") // defaulted
    assertEquals(r.investors.map(_.id), List("sequoia", "jane"))
    assertEquals(r.investors.head.tier, Some("Tier1"))
    assertEquals(r.leadInvestorId, Some("sequoia"))
  }

  test("flags a missing required column") {
    val csv = "id,companyId,stage\nx,acme,Seed\n" // no amount/date/etc
    val result = CsvRecordDecoder.decode(csv)
    assertEquals(result.records, Nil)
    assert(result.errors.head.message.contains("missing required columns"), result.errors)
  }

  test("a ragged row is reported but does not drop good rows") {
    val csv =
      """id,companyId,companyName,stage,amount,date,sector,country
        |good,acme,Acme,Seed,1000000,2022-01-01,AI,USA
        |bad,globex,Globex,Seed,2000000
        |""".stripMargin
    val result = CsvRecordDecoder.decode(csv)
    assertEquals(result.records.map(_.id), List("good"))
    assertEquals(result.errors.size, 1)
    assert(result.errors.head.where == "row 3", result.errors)
  }

  test("decoded records flow through validation into profiles") {
    val csv =
      """id,companyId,companyName,stage,amount,date,sector,country,investors,leadInvestorId,postMoneyValuation
        |s1,acme,Acme,Seed,2000000,2021-01-01,AI,USA,yc:Y Combinator:Tier2,yc,
        |a1,acme,Acme,Series A,10000000,2022-06-01,AI,USA,sequoia:Sequoia:Tier1,sequoia,50000000
        |""".stripMargin
    val decoded = CsvRecordDecoder.decode(csv)
    assert(decoded.isClean, decoded.errors)

    val result = Ingestor.run(decoded.records)
    assert(result.isClean, (result.parseFailures, result.profileFailures))
    val acme = result.registry.get("acme").get
    assertEquals(acme.roundCount, 2)
    assertEquals(acme.totalCapitalRaised, Money.usd(12_000_000))
    assertEquals(acme.latestStage, Some(FundingStage.SeriesA))
    assertEquals(acme.investors.find(_.id == "sequoia").map(_.tier), Some(InvestorTier.Tier1))
  }

  test("reads the bundled sample CSV file end to end") {
    val text = scala.io.Source.fromFile("data/sample/funding_rounds.csv")
    val csv =
      try text.mkString
      finally text.close()
    val decoded = CsvRecordDecoder.decode(csv)
    assert(decoded.isClean, decoded.errors)
    val result = Ingestor.run(decoded.records)
    assert(result.isClean, (result.parseFailures, result.profileFailures))
    assertEquals(result.registry.size, 2)
    assertEquals(result.registry.get("nimbus").get.latestValuation, Some(Money.usd(70_000_000)))
  }
