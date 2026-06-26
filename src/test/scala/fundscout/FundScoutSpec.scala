package fundscout

class FundScoutSpec extends munit.FunSuite:

  test("chooses format from extension, then content type, then defaults to CSV") {
    assertEquals(FundScout.formatFor("data.json", None), FundScout.Format.Json)
    assertEquals(FundScout.formatFor("data.csv", None), FundScout.Format.Csv)
    assertEquals(
      FundScout.formatFor("https://x/api/feed", Some("application/json; charset=utf-8")),
      FundScout.Format.Json
    )
    assertEquals(
      FundScout.formatFor("https://x/export", Some("text/csv")),
      FundScout.Format.Csv
    )
    assertEquals(FundScout.formatFor("https://x/unknown", None), FundScout.Format.Csv)
  }

  test("reports from CSV text (as if fetched online)") {
    val csv =
      """id,companyId,companyName,stage,amount,date,sector,country
        |a1,acme,Acme,Seed,2000000,2021-01-01,AI,USA
        |a2,acme,Acme,Series A,10000000,2022-06-01,AI,USA
        |""".stripMargin
    val report = FundScout.report("https://example.test/feed.csv", None, csv)
    assert(report.contains("Loaded 1 companies"), report)
    assert(report.contains("Total capital raised : $12M"), report)
  }

  test("reports from JSON text and selects the decoder by content type") {
    val json =
      """[{"id":"a1","companyId":"acme","companyName":"Acme","stage":"Seed",
        |"amount":2000000,"date":"2021-01-01","sector":"AI","country":"USA"}]""".stripMargin
    val report = FundScout.report("https://example.test/api", Some("application/json"), json)
    assert(report.contains("Loaded 1 companies"), report)
  }

  test("surfaces decode issues in the report rather than failing") {
    val badCsv = "id,companyId,stage\nx,acme,Seed\n" // missing required columns
    val report = FundScout.report("feed.csv", None, badCsv)
    assert(report.contains("Decode issues"), report)
    assert(report.contains("Loaded 0 companies"), report)
  }
