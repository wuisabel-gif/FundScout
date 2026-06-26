package fundscout.reporting

import fundscout.analytics.MarketReport

/** Renders a [[fundscout.analytics.MarketReport]] as plain text for the console.
  *
  * This is the simplest realization of the "Reports" stage of the architecture:
  * a pure `MarketReport => String` with no formatting dependencies. Richer
  * outputs (JSON, an API, a dashboard) can render the same report later.
  */
object MarketReportRenderer:

  /** Produce a human-readable, multi-line summary of the report. */
  def render(report: MarketReport): String =
    val cur = report.reportingCurrency
    val lines = scala.collection.mutable.ArrayBuffer.empty[String]
    def add(line: String): Unit = lines += line

    add("FundScout — Market Report")
    add("=" * 40)
    add(f"Companies tracked    : ${report.companiesTracked}%,d")
    add(f"Funding rounds       : ${report.totalRounds}%,d")
    add(s"Reporting currency   : $cur")
    if report.excludedOtherCurrencyRounds > 0 then
      add(s"  (excluded ${report.excludedOtherCurrencyRounds} rounds in other currencies)")
    add(s"Total capital raised : ${report.totalCapitalRaised.display}")
    report.medianRoundSize.foreach(m => add(s"Median round size    : ${m.display}"))
    report.averageSeriesASize.foreach(a => add(s"Average Series A     : ${a.display}"))
    report.fundingVelocityPerYear.foreach(v =>
      add(f"Funding velocity     : $v%.1f rounds/year")
    )
    add(s"Unicorns             : ${report.unicornCount}")
    add(s"Exited companies     : ${report.exitedCompanies}")

    section(add, "Top sectors by funding")
    report.topSectors(5).foreach { case (sector, money) =>
      add(f"  ${sector.label}%-26s ${money.display}")
    }

    section(add, "Top countries by funding")
    report.topCountries(5).foreach { case (country, money) =>
      add(f"  $country%-26s ${money.display}")
    }

    section(add, "Most active investors")
    report.topInvestors(5).foreach { a =>
      add(f"  ${a.investor.name}%-26s ${a.deals}%d deals")
    }

    if report.unicorns.nonEmpty then
      section(add, "Unicorns")
      report.unicorns.foreach { u =>
        add(f"  ${u.name}%-26s ${u.valuation.display}")
      }

    lines.mkString("\n")

  private def section(add: String => Unit, title: String): Unit =
    add("")
    add(s"$title:")
