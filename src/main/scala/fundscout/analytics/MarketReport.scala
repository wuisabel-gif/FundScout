package fundscout.analytics

import fundscout.model.*

/** How often a single investor has participated in funding rounds. */
final case class InvestorActivity(investor: Investor, deals: Int)

/** A company whose most recent disclosed valuation reaches unicorn scale. */
final case class UnicornStat(companyId: String, name: String, valuation: Money)

/** An immutable, ecosystem-wide snapshot computed from a set of funding events.
  *
  * Every figure is derived purely from observable events — there are no hidden
  * heuristics. Monetary aggregates are all expressed in a single
  * [[reportingCurrency]] (the most common currency in the data); events in other
  * currencies are excluded and counted in [[excludedOtherCurrencyRounds]] so the
  * omission is explicit rather than silent.
  *
  * @param reportingCurrency
  *   the currency all monetary figures below are expressed in
  * @param companiesTracked
  *   number of distinct companies
  * @param totalRounds
  *   number of funding rounds counted (in the reporting currency)
  * @param excludedOtherCurrencyRounds
  *   rounds skipped because they used a different currency
  * @param totalCapitalRaised
  *   sum of all capital-raising rounds (excludes acquisitions)
  * @param fundingBySector
  *   capital raised, grouped by sector
  * @param fundingByYear
  *   capital raised, grouped by calendar year
  * @param fundingByCountry
  *   capital raised, grouped by headquarters country
  * @param fundingByCity
  *   capital raised, grouped by headquarters city (rounds without a city omitted)
  * @param roundsByStage
  *   count of rounds at each stage
  * @param medianRoundSize
  *   median capital-raising round size, if any rounds exist
  * @param averageSeriesASize
  *   mean Series A round size, if any Series A rounds exist
  * @param fundingVelocityPerYear
  *   average number of rounds per calendar year spanned by the data
  * @param investorActivity
  *   investors ranked by number of deals (most active first)
  * @param unicorns
  *   companies at unicorn-scale valuation, most valuable first
  * @param exitedCompanies
  *   number of companies that have had an IPO or acquisition
  */
final case class MarketReport(
    reportingCurrency: Currency,
    companiesTracked: Int,
    totalRounds: Int,
    excludedOtherCurrencyRounds: Int,
    totalCapitalRaised: Money,
    fundingBySector: Map[Sector, Money],
    fundingByYear: Map[Int, Money],
    fundingByCountry: Map[String, Money],
    fundingByCity: Map[String, Money],
    roundsByStage: Map[FundingStage, Int],
    medianRoundSize: Option[Money],
    averageSeriesASize: Option[Money],
    fundingVelocityPerYear: Option[Double],
    investorActivity: List[InvestorActivity],
    unicorns: List[UnicornStat],
    exitedCompanies: Int
):

  /** The `n` sectors that have attracted the most capital, highest first. */
  def topSectors(n: Int): List[(Sector, Money)] =
    fundingBySector.toList.sortBy(e => -e._2.amount).take(n)

  /** The `n` countries that have attracted the most capital, highest first. */
  def topCountries(n: Int): List[(String, Money)] =
    fundingByCountry.toList.sortBy(e => -e._2.amount).take(n)

  /** The `n` most active investors by deal count. */
  def topInvestors(n: Int): List[InvestorActivity] = investorActivity.take(n)

  def unicornCount: Int = unicorns.size
