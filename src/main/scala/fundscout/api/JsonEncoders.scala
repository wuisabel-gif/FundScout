package fundscout.api

import fundscout.analytics.{InvestorActivity, MarketReport, UnicornStat}
import fundscout.model.*
import fundscout.prediction.{CompanyForecast, NextRoundForecast, OutcomeForecast}
import fundscout.scoring.{ComponentScore, ScoreReason, StartupScore}
import fundscout.util.Json
import fundscout.util.Json.*
import fundscout.valuation.ValuationEstimate

/** Converts FundScout's domain and report types into [[Json]].
  *
  * Encoders are plain functions — no implicit machinery — so the exact shape of
  * every response is obvious and easy to evolve. Monetary values are encoded
  * with both their raw amount and a human-readable `display` string.
  */
object JsonEncoders:

  def money(m: Money): Json = obj(
    "amount" -> num(m.amount),
    "currency" -> str(m.currency.toString),
    "display" -> str(m.display)
  )

  def investor(i: Investor): Json = obj(
    "id" -> str(i.id),
    "name" -> str(i.name),
    "tier" -> str(i.tier.toString)
  )

  def event(e: FundingEvent): Json = obj(
    "id" -> str(e.id),
    "stage" -> str(e.stage.label),
    "amount" -> money(e.amount),
    "date" -> str(e.date.toString),
    "sector" -> str(e.sector.label),
    "headquarters" -> str(e.headquarters.display),
    "investors" -> arr(e.investors.map(investor)),
    "leadInvestor" -> opt(e.leadInvestor.map(i => str(i.name))),
    "postMoneyValuation" -> opt(e.postMoneyValuation.map(money))
  )

  /** A compact company view for list endpoints. */
  def profileSummary(p: CompanyProfile): Json = obj(
    "companyId" -> str(p.companyId),
    "name" -> str(p.name),
    "sector" -> str(p.sector.label),
    "headquarters" -> str(p.headquarters.display),
    "rounds" -> num(p.roundCount),
    "totalCapitalRaised" -> money(p.totalCapitalRaised),
    "latestStage" -> opt(p.latestStage.map(s => str(s.label))),
    "hasExited" -> bool(p.hasExited)
  )

  /** A full company view, including event history. */
  def profile(p: CompanyProfile): Json = obj(
    "companyId" -> str(p.companyId),
    "name" -> str(p.name),
    "sector" -> str(p.sector.label),
    "headquarters" -> str(p.headquarters.display),
    "rounds" -> num(p.roundCount),
    "totalCapitalRaised" -> money(p.totalCapitalRaised),
    "latestStage" -> opt(p.latestStage.map(s => str(s.label))),
    "latestValuation" -> opt(p.latestValuation.map(money)),
    "investors" -> arr(p.investors.toList.sortBy(_.name).map(investor)),
    "hasExited" -> bool(p.hasExited),
    "events" -> arr(p.events.map(event))
  )

  def marketReport(r: MarketReport): Json =
    def fundingMap[K](m: Map[K, Money])(key: K => String): Json =
      arr(
        m.toList
          .sortBy(e => -e._2.amount)
          .map((k, v) => obj("key" -> str(key(k)), "funding" -> money(v)))
      )
    obj(
      "reportingCurrency" -> str(r.reportingCurrency.toString),
      "companiesTracked" -> num(r.companiesTracked),
      "totalRounds" -> num(r.totalRounds),
      "totalCapitalRaised" -> money(r.totalCapitalRaised),
      "medianRoundSize" -> opt(r.medianRoundSize.map(money)),
      "averageSeriesASize" -> opt(r.averageSeriesASize.map(money)),
      "fundingVelocityPerYear" -> opt(r.fundingVelocityPerYear.map(num)),
      "unicornCount" -> num(r.unicornCount),
      "exitedCompanies" -> num(r.exitedCompanies),
      "fundingBySector" -> fundingMap(r.fundingBySector)(_.label),
      "fundingByCountry" -> fundingMap(r.fundingByCountry)(identity),
      "fundingByCity" -> fundingMap(r.fundingByCity)(identity),
      "topInvestors" -> arr(r.investorActivity.take(10).map(investorActivity)),
      "unicorns" -> arr(r.unicorns.map(unicorn))
    )

  def investorActivity(a: InvestorActivity): Json = obj(
    "investor" -> str(a.investor.name),
    "deals" -> num(a.deals)
  )

  def unicorn(u: UnicornStat): Json = obj(
    "companyId" -> str(u.companyId),
    "name" -> str(u.name),
    "valuation" -> money(u.valuation)
  )

  def scoreReason(r: ScoreReason): Json = obj(
    "polarity" -> str(r.polarity.toString),
    "description" -> str(r.description)
  )

  def componentScore(c: ComponentScore): Json = obj(
    "component" -> str(c.component.label),
    "value" -> num(c.value),
    "reasons" -> arr(c.reasons.map(scoreReason))
  )

  def score(s: StartupScore): Json = obj(
    "overall" -> num(s.overall),
    "components" -> arr(s.components.map(componentScore)),
    "positives" -> arr(s.positives.map(r => str(r.description))),
    "negatives" -> arr(s.negatives.map(r => str(r.description)))
  )

  def valuation(e: ValuationEstimate): Json = obj(
    "low" -> money(e.low),
    "high" -> money(e.high),
    "midpoint" -> money(e.midpoint),
    "confidence" -> str(e.confidence.toString),
    "reasoning" -> arr(e.reasoning.map(str))
  )

  def nextRound(n: NextRoundForecast): Json = obj(
    "probability" -> num(n.probability.value),
    "probabilityPercent" -> num(n.probability.percent),
    "expectedAmount" -> opt(n.expectedAmount.map { case (low, high) =>
      obj("low" -> money(low), "high" -> money(high))
    }),
    "expectedMonths" -> opt(n.expectedMonths.map { case (low, high) =>
      obj("low" -> num(low), "high" -> num(high))
    }),
    "reasons" -> arr(n.reasons.map(str))
  )

  def outcomeForecast(o: OutcomeForecast): Json = obj(
    "outcome" -> str(o.outcome.label),
    "probability" -> num(o.probability.value),
    "probabilityPercent" -> num(o.probability.percent),
    "reasons" -> arr(o.reasons.map(str))
  )

  def forecast(f: CompanyForecast): Json = obj(
    "nextRound" -> nextRound(f.nextRound),
    "outcomes" -> arr(f.outcomes.map(outcomeForecast))
  )
