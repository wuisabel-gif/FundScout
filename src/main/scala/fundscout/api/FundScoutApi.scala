package fundscout.api

import fundscout.analytics.MarketAnalytics
import fundscout.ingest.CompanyRegistry
import fundscout.model.CompanyProfile
import fundscout.prediction.PredictionEngine
import fundscout.scoring.StartupScorer
import fundscout.util.Json
import fundscout.util.Json.*
import fundscout.valuation.{ValuationContext, ValuationEngine}

import java.time.LocalDate

/** An HTTP-style response: a status code and a JSON body. Transport-agnostic so
  * it can be unit-tested without a server and served by any binding.
  */
final case class ApiResponse(status: Int, body: Json):
  def render: String = body.render

/** The FundScout read API: maps a request (method + path) to an [[ApiResponse]].
  *
  * The router is a pure function of its inputs and the registry it was built
  * with — no I/O, no mutable state — which makes every endpoint testable in
  * isolation. The analytics report and valuation context are computed once at
  * construction and reused across requests.
  *
  * Endpoints:
  *
  * {{{
  * GET /                              service banner
  * GET /health                        liveness check
  * GET /market                        ecosystem analytics report
  * GET /companies                     list of company summaries
  * GET /companies/{id}                full company profile
  * GET /companies/{id}/score          explainable startup score
  * GET /companies/{id}/valuation      valuation estimate
  * GET /companies/{id}/forecast       probabilistic forecast
  * }}}
  *
  * @param asOf
  *   the reference date for recency-sensitive computations (score, valuation,
  *   forecast), injected for deterministic results.
  */
final class FundScoutApi(registry: CompanyRegistry, asOf: LocalDate):

  private val market = MarketAnalytics.of(registry)
  private val valuationContext = ValuationContext.from(registry, market)

  def handle(method: String, path: String): ApiResponse =
    val segments = path.split("/").iterator.filter(_.nonEmpty).toList
    (method.toUpperCase, segments) match
      case ("GET", Nil) =>
        ok(obj(
          "service" -> str("FundScout"),
          "endpoints" -> arr(Endpoints.map(str))
        ))
      case ("GET", "health" :: Nil) =>
        ok(obj("status" -> str("ok")))
      case ("GET", "market" :: Nil) =>
        ok(JsonEncoders.marketReport(market))
      case ("GET", "companies" :: Nil) =>
        ok(arr(
          registry.companies.toList.sortBy(_.name).map(JsonEncoders.profileSummary)
        ))
      case ("GET", "companies" :: id :: Nil) =>
        withCompany(id)(p => ok(JsonEncoders.profile(p)))
      case ("GET", "companies" :: id :: "score" :: Nil) =>
        withCompany(id)(p =>
          ok(JsonEncoders.score(StartupScorer.score(p, market, asOf)))
        )
      case ("GET", "companies" :: id :: "valuation" :: Nil) =>
        withCompany(id)(p =>
          ok(JsonEncoders.valuation(ValuationEngine.estimate(p, valuationContext, asOf)))
        )
      case ("GET", "companies" :: id :: "forecast" :: Nil) =>
        withCompany(id)(p =>
          ok(JsonEncoders.forecast(PredictionEngine.forecast(p, market, asOf)))
        )
      case ("GET", _) =>
        notFound(s"no such endpoint: /${segments.mkString("/")}")
      case (other, _) =>
        ApiResponse(405, obj("error" -> str(s"method not allowed: $other")))

  private def withCompany(id: String)(
      f: CompanyProfile => ApiResponse
  ): ApiResponse =
    registry.get(id) match
      case Some(profile) => f(profile)
      case None          => notFound(s"unknown company: $id")

  private def ok(body: Json): ApiResponse = ApiResponse(200, body)

  private def notFound(message: String): ApiResponse =
    ApiResponse(404, obj("error" -> str(message)))

  private val Endpoints = List(
    "GET /health",
    "GET /market",
    "GET /companies",
    "GET /companies/{id}",
    "GET /companies/{id}/score",
    "GET /companies/{id}/valuation",
    "GET /companies/{id}/forecast"
  )
