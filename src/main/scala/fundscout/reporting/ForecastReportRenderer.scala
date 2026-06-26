package fundscout.reporting

import fundscout.prediction.CompanyForecast

/** Renders a [[fundscout.prediction.CompanyForecast]] as plain text: the next-
  * round outlook followed by independent outcome probabilities.
  */
object ForecastReportRenderer:

  def render(companyName: String, forecast: CompanyForecast): String =
    val lines = scala.collection.mutable.ArrayBuffer.empty[String]
    def add(line: String): Unit = lines += line

    val nr = forecast.nextRound
    add(s"$companyName — Forecast")
    add("")
    add(s"Next round: ${nr.probability.display} likely")
    nr.expectedAmount.foreach { case (low, high) =>
      add(s"  Expected size  : ${low.display}–${high.display}")
    }
    nr.expectedMonths.foreach { case (low, high) =>
      add(s"  Expected timing: $low–$high months")
    }
    nr.reasons.foreach(r => add(s"  • $r"))

    add("")
    add("Outcome probabilities:")
    forecast.outcomes.foreach { o =>
      add(f"  ${o.outcome.label}%-22s ${o.probability.display}%4s")
      o.reasons.headOption.foreach(r => add(s"      ($r)"))
    }

    lines.mkString("\n")
