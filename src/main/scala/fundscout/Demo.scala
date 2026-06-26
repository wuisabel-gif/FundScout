package fundscout

import fundscout.analytics.MarketAnalytics
import fundscout.ingest.Ingestor
import fundscout.prediction.PredictionEngine
import fundscout.reporting.{ForecastReportRenderer, MarketReportRenderer, ScoreReportRenderer, ValuationReportRenderer}
import fundscout.scoring.StartupScorer
import fundscout.valuation.{ValuationContext, ValuationEngine}

import java.time.LocalDate

/** A runnable end-to-end demo: ingest a small sample of funding announcements,
  * compute ecosystem analytics, and print a market report.
  *
  * Run with `sbt run`. This is the closest thing to a "see it work" entry point
  * until a real API or dashboard exists.
  */
@main def fundscoutDemo(): Unit =
  val result = Ingestor.run(SampleData.records)

  if !result.isClean then
    println("Ingest issues:")
    result.parseFailures.foreach { f =>
      println(s"  ${f.record.id}: ${f.errors.map(_.message).mkString("; ")}")
    }
    result.profileFailures.foreach { case (event, error) =>
      println(s"  ${event.id}: ${error.message}")
    }
    println()

  val report = MarketAnalytics.of(result.registry)
  println(MarketReportRenderer.render(report))

  // Score every company against the ecosystem, strongest first.
  val asOf = LocalDate.of(2025, 6, 1)
  val scored = result.registry.companies.toList
    .map(profile => profile -> StartupScorer.score(profile, report, asOf))
    .sortBy { case (_, score) => -score.overall }

  println()
  println("=" * 40)
  println("Startup Scores")
  println("=" * 40)
  scored.foreach { case (profile, score) =>
    println()
    println(ScoreReportRenderer.render(profile.name, score))
  }

  // Estimate a valuation range for each company from observable signals.
  val valuationContext = ValuationContext.from(result.registry, report)
  println()
  println("=" * 40)
  println("Valuation Estimates")
  println("=" * 40)
  result.registry.companies.toList
    .sortBy(_.name)
    .foreach { profile =>
      val estimate = ValuationEngine.estimate(profile, valuationContext, asOf)
      println()
      println(ValuationReportRenderer.render(profile.name, estimate))
    }

  // Forecast probabilistic future outcomes for each company.
  println()
  println("=" * 40)
  println("Forecasts")
  println("=" * 40)
  result.registry.companies.toList
    .sortBy(_.name)
    .foreach { profile =>
      val forecast = PredictionEngine.forecast(profile, report, asOf)
      println()
      println(ForecastReportRenderer.render(profile.name, forecast))
    }
