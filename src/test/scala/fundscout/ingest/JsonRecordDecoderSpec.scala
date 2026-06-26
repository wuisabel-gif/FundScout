package fundscout.ingest

import fundscout.model.{FundingStage, Money}

class JsonRecordDecoderSpec extends munit.FunSuite:

  test("decodes an array of records, accepting numeric or string amounts") {
    val json =
      """[
        |  {"id":"e1","companyId":"acme","companyName":"Acme","stage":"Seed",
        |   "amount":2000000,"date":"2021-01-01","sector":"AI","country":"USA",
        |   "investors":[{"id":"yc","name":"Y Combinator","tier":"Tier2"}],
        |   "leadInvestorId":"yc"},
        |  {"id":"e2","companyId":"acme","companyName":"Acme","stage":"Series A",
        |   "amount":"10000000","date":"2022-06-01","sector":"AI","country":"USA",
        |   "investors":[{"id":"sequoia","name":"Sequoia","tier":"Tier1"}],
        |   "leadInvestorId":"sequoia","postMoneyValuation":50000000}
        |]""".stripMargin
    val result = JsonRecordDecoder.decode(json)

    assert(result.isClean, result.errors)
    assertEquals(result.records.map(_.id), List("e1", "e2"))
    assertEquals(result.records(0).amount, "2000000")
    assertEquals(result.records(1).amount, "10000000")
    assertEquals(result.records(1).postMoneyValuation, Some("50000000"))
    assertEquals(result.records(0).investors.head.tier, Some("Tier2"))
  }

  test("accepts a single object and a { records: [...] } wrapper") {
    val single =
      """{"id":"e1","companyId":"acme","companyName":"Acme","stage":"Seed",
        |"amount":1000000,"date":"2021-01-01","sector":"AI","country":"USA"}""".stripMargin
    assertEquals(JsonRecordDecoder.decode(single).records.size, 1)

    val wrapped = s"""{"records":[$single]}"""
    assertEquals(JsonRecordDecoder.decode(wrapped).records.size, 1)
  }

  test("reports a JSON syntax error without throwing") {
    val result = JsonRecordDecoder.decode("""{"id": }""")
    assertEquals(result.records, Nil)
    assertEquals(result.errors.head.where, "json")
  }

  test("reports a record missing required fields") {
    val json = """[{"id":"e1","companyId":"acme"}]"""
    val result = JsonRecordDecoder.decode(json)
    assertEquals(result.records, Nil)
    assert(result.errors.head.message.contains("missing fields"), result.errors)
  }

  test("decoded records flow through validation into profiles") {
    val json =
      """[
        |  {"id":"e1","companyId":"acme","companyName":"Acme","stage":"Seed",
        |   "amount":2000000,"date":"2021-01-01","sector":"AI","country":"USA"},
        |  {"id":"e2","companyId":"acme","companyName":"Acme","stage":"Series A",
        |   "amount":10000000,"date":"2022-06-01","sector":"AI","country":"USA",
        |   "postMoneyValuation":60000000}
        |]""".stripMargin
    val decoded = JsonRecordDecoder.decode(json)
    assert(decoded.isClean, decoded.errors)

    val result = Ingestor.run(decoded.records)
    assert(result.isClean, (result.parseFailures, result.profileFailures))
    val acme = result.registry.get("acme").get
    assertEquals(acme.roundCount, 2)
    assertEquals(acme.totalCapitalRaised, Money.usd(12_000_000))
    assertEquals(acme.latestStage, Some(FundingStage.SeriesA))
    assertEquals(acme.latestValuation, Some(Money.usd(60_000_000)))
  }
