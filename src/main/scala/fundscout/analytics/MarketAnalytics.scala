package fundscout.analytics

import fundscout.ingest.CompanyRegistry
import fundscout.model.*

/** Computes ecosystem-wide statistics from company profiles.
  *
  * The engine is a pure fold over funding events: given the same data it always
  * produces the same [[MarketReport]], and it performs no I/O. All monetary
  * aggregates are computed in one reporting currency (the most common in the
  * data) so that [[fundscout.model.Money]] addition is always well-defined.
  */
object MarketAnalytics:

  /** Companies are considered unicorn-scale at a $1B-equivalent valuation. */
  val UnicornThreshold: BigDecimal = BigDecimal(1_000_000_000)

  /** Build a market report from every company in `registry`. */
  def of(registry: CompanyRegistry): MarketReport =
    val profiles = registry.companies.toList
    val allEvents = registry.allEvents

    val reportingCurrency =
      allEvents
        .groupBy(_.amount.currency)
        .toList
        .sortBy(e => -e._2.size)
        .headOption
        .map(_._1)
        .getOrElse(Currency.USD)

    val events = allEvents.filter(_.amount.currency == reportingCurrency)
    val excluded = allEvents.size - events.size
    val raising = events.filter(_.stage.raisesCapital)

    def sumAmounts(amounts: Iterable[Money]): Money =
      amounts.foldLeft(Money.zero(reportingCurrency))(_ + _)

    def groupedFunding[K](pairs: Iterable[(K, Money)]): Map[K, Money] =
      pairs.groupMapReduce(_._1)(_._2)(_ + _)

    val fundingBySector =
      groupedFunding(raising.map(e => e.sector -> e.amount))
    val fundingByYear =
      groupedFunding(raising.map(e => e.year -> e.amount))
    val fundingByCountry =
      groupedFunding(raising.map(e => e.headquarters.country -> e.amount))
    val fundingByCity =
      groupedFunding(
        raising.flatMap(e => e.headquarters.city.map(_ -> e.amount))
      )

    val roundsByStage = events.groupMapReduce(_.stage)(_ => 1)(_ + _)

    val medianRoundSize =
      median(raising.map(_.amount.amount)).map(Money(_, reportingCurrency))

    val seriesA = events.collect {
      case e if e.stage == FundingStage.SeriesA => e.amount.amount
    }
    val averageSeriesASize =
      if seriesA.isEmpty then None
      else Some(Money(seriesA.sum / seriesA.size, reportingCurrency))

    val fundingVelocityPerYear =
      if events.isEmpty then None
      else
        val years = events.map(_.year)
        val span = years.max - years.min + 1
        Some(events.size.toDouble / span)

    val investorActivity =
      events
        .flatMap(_.investors)
        .groupBy(_.id)
        .values
        .map(group => InvestorActivity(group.head, group.size))
        .toList
        .sortBy(a => (-a.deals, a.investor.name))

    val unicorns =
      profiles
        .flatMap { p =>
          p.latestValuation
            .filter(v => v.currency == reportingCurrency && v.amount >= UnicornThreshold)
            .map(v => UnicornStat(p.companyId, p.name, v))
        }
        .sortBy(u => -u.valuation.amount)

    MarketReport(
      reportingCurrency = reportingCurrency,
      companiesTracked = profiles.size,
      totalRounds = events.size,
      excludedOtherCurrencyRounds = excluded,
      totalCapitalRaised = sumAmounts(raising.map(_.amount)),
      fundingBySector = fundingBySector,
      fundingByYear = fundingByYear,
      fundingByCountry = fundingByCountry,
      fundingByCity = fundingByCity,
      roundsByStage = roundsByStage,
      medianRoundSize = medianRoundSize,
      averageSeriesASize = averageSeriesASize,
      fundingVelocityPerYear = fundingVelocityPerYear,
      investorActivity = investorActivity,
      unicorns = unicorns,
      exitedCompanies = profiles.count(_.hasExited)
    )

  /** Median of a collection of values, or `None` when empty. For an even count
    * it averages the two middle values.
    */
  private def median(values: Iterable[BigDecimal]): Option[BigDecimal] =
    val sorted = values.toVector.sorted
    val n = sorted.size
    if n == 0 then None
    else if n % 2 == 1 then Some(sorted(n / 2))
    else Some((sorted(n / 2 - 1) + sorted(n / 2)) / 2)
