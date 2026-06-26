package fundscout.api

class DashboardSpec extends munit.FunSuite:

  test("dashboard is a self-contained HTML document") {
    val html = Dashboard.html
    assert(html.startsWith("<!doctype html"), html.take(40))
    assert(html.contains("</html>"))
    assert(html.contains("<title>FundScout"), "has a title")
  }

  test("dashboard wires itself to the JSON API endpoints") {
    val html = Dashboard.html
    assert(html.contains("'/market'"), "fetches the market report")
    assert(html.contains("'/companies'"), "fetches the company list")
    assert(html.contains("/score"), "fetches scores")
    assert(html.contains("/valuation"), "fetches valuations")
    assert(html.contains("/forecast"), "fetches forecasts")
  }
