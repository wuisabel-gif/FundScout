package fundscout.api

import fundscout.SampleData
import fundscout.ingest.Ingestor
import java.time.LocalDate

class FundScoutApiSpec extends munit.FunSuite:

  private val api =
    FundScoutApi(Ingestor.run(SampleData.records).registry, LocalDate.of(2025, 6, 1))

  private def get(path: String): ApiResponse = api.handle("GET", path)

  test("health check responds ok") {
    val r = get("/health")
    assertEquals(r.status, 200)
    assertEquals(r.render, "{\"status\":\"ok\"}")
  }

  test("root lists endpoints") {
    val r = get("/")
    assertEquals(r.status, 200)
    assert(r.render.contains("FundScout"), r.render)
    assert(r.render.contains("/companies/{id}/score"), r.render)
  }

  test("market endpoint returns ecosystem analytics") {
    val r = get("/market")
    assertEquals(r.status, 200)
    assert(r.render.contains("\"companiesTracked\":4"), r.render)
    assert(r.render.contains("\"unicornCount\":2"), r.render)
  }

  test("companies endpoint lists summaries sorted by name") {
    val r = get("/companies")
    assertEquals(r.status, 200)
    val body = r.render
    assert(body.contains("Acme"), body)
    // Acme should appear before Globex (alphabetical order).
    assert(body.indexOf("Acme") < body.indexOf("Globex"), body)
  }

  test("a known company returns a full profile with events") {
    val r = get("/companies/acme")
    assertEquals(r.status, 200)
    assert(r.render.contains("\"companyId\":\"acme\""), r.render)
    assert(r.render.contains("\"events\":["), r.render)
  }

  test("an unknown company returns 404") {
    val r = get("/companies/nope")
    assertEquals(r.status, 404)
    assert(r.render.contains("unknown company"), r.render)
  }

  test("score, valuation, and forecast endpoints respond") {
    val score = get("/companies/acme/score")
    assertEquals(score.status, 200)
    assert(score.render.contains("\"overall\":"), score.render)

    val valuation = get("/companies/acme/valuation")
    assertEquals(valuation.status, 200)
    assert(valuation.render.contains("\"confidence\":"), valuation.render)

    val forecast = get("/companies/acme/forecast")
    assertEquals(forecast.status, 200)
    assert(forecast.render.contains("\"nextRound\":"), forecast.render)
    assert(forecast.render.contains("\"outcomes\":"), forecast.render)
  }

  test("unknown path is a 404 and non-GET is a 405") {
    assertEquals(get("/bogus").status, 404)
    assertEquals(api.handle("POST", "/market").status, 405)
  }
