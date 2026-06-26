package fundscout.valuation

import fundscout.analytics.MarketReport
import fundscout.ingest.CompanyRegistry
import fundscout.model.*
import fundscout.valuation.Confidence.*

import java.time.LocalDate

/** Precomputed market context for valuation: the valuation-to-capital multiples
  * that comparable companies command, plus which sectors and countries are
  * strongest.
  *
  * Multiples are derived from companies that actually disclosed a valuation, so
  * estimates are grounded in observable comparables ("companies like this trade
  * at ~5x capital raised") rather than invented constants.
  */
final case class ValuationContext(
    multipleBySector: Map[Sector, BigDecimal],
    overallMultiple: Option[BigDecimal],
    topSectors: Set[Sector],
    topCountries: Set[String]
)

object ValuationContext:

  /** Build context from every company in `registry` and the market `report`. */
  def from(registry: CompanyRegistry, report: MarketReport): ValuationContext =
    val multiples: List[(Sector, BigDecimal)] =
      registry.companies.toList.flatMap { p =>
        val raised = p.totalCapitalRaised
        p.latestValuation
          .filter(v => raised.amount > 0 && v.currency == raised.currency)
          .map(v => p.sector -> v.amount / raised.amount)
      }
    val bySector =
      multiples
        .groupBy(_._1)
        .collect { case (sector, ms) if ms.nonEmpty => sector -> median(ms.map(_._2)).get }
    val overall = median(multiples.map(_._2))
    ValuationContext(
      multipleBySector = bySector,
      overallMultiple = overall,
      topSectors = report.topSectors(3).map(_._1).toSet,
      topCountries = report.topCountries(2).map(_._1).toSet
    )

  private def median(values: List[BigDecimal]): Option[BigDecimal] =
    val sorted = values.sorted
    val n = sorted.size
    if n == 0 then None
    else if n % 2 == 1 then Some(sorted(n / 2))
    else Some((sorted(n / 2 - 1) + sorted(n / 2)) / 2)

/** Estimates a valuation range from observable signals using transparent,
  * rule-based logic (the "valuation estimation" objective, version 1 — see the
  * README).
  *
  * Two paths:
  *
  *   - If the company disclosed a post-money valuation, the estimate is
  *     *anchored* on it, with a range that widens (and confidence that falls) as
  *     that figure ages.
  *   - Otherwise the estimate is *derived*: capital raised × a base multiple from
  *     comparable companies, adjusted by investor quality, sector and geographic
  *     strength, and fundraising momentum — every adjustment recorded as a
  *     reasoning bullet.
  */
object ValuationEngine:

  def estimate(
      profile: CompanyProfile,
      context: ValuationContext,
      asOf: LocalDate
  ): ValuationEstimate =
    val currency = profile.currency.getOrElse(Currency.USD)
    if profile.isEmpty then
      empty(currency, "No funding history to value")
    else
      profile.latestValuation match
        case Some(disclosed) => anchored(profile, disclosed, asOf)
        case None            => derived(profile, context, asOf, currency)

  // --- Anchored on a disclosed valuation ------------------------------------

  private def anchored(
      profile: CompanyProfile,
      disclosed: Money,
      asOf: LocalDate
  ): ValuationEstimate =
    val months = profile.daysSinceLastFunding(asOf).getOrElse(0L) / 30
    val (spread, confidence, recencyNote) =
      if months <= 12 then
        (BigDecimal(0.10), High, "Recently disclosed post-money valuation")
      else if months <= 30 then
        (BigDecimal(0.20), Medium, "Disclosed valuation is over a year old")
      else
        (BigDecimal(0.35), Low, "Disclosed valuation is stale (2+ years old)")

    val reasoning =
      stageNote(profile).toList ++ List(
        s"Disclosed post-money valuation of ${disclosed.display}",
        recencyNote
      )

    ValuationEstimate(
      low = disclosed * (1 - spread),
      high = disclosed * (1 + spread),
      confidence = confidence,
      reasoning = reasoning
    )

  // --- Derived from comparables and signals ---------------------------------

  private def derived(
      profile: CompanyProfile,
      context: ValuationContext,
      asOf: LocalDate,
      currency: Currency
  ): ValuationEstimate =
    val raised = profile.totalCapitalRaised
    if raised.isZero then
      return empty(currency, "No capital-raising rounds to value")

    val stage = profile.latestStage.getOrElse(FundingStage.Seed)
    val (baseMultiple, baseReason) = baseMultipleFor(profile.sector, stage, context)

    val tier1Lead = profile.leadInvestors.exists(_.tier == InvestorTier.Tier1)
    val topSector = context.topSectors.contains(profile.sector)
    val topCountry = context.topCountries.contains(profile.headquarters.country)

    val adjustments: List[(BigDecimal, String)] = List(
      Option.when(tier1Lead)(
        (BigDecimal(1.15), "Top-tier lead investor")
      ),
      Option.when(topSector)(
        (BigDecimal(1.10), s"Strong ${profile.sector.label} market")
      ),
      Option.when(topCountry)(
        (BigDecimal(1.05),
          s"Located in a major funding hub (${profile.headquarters.country})")
      ),
      momentumAdjustment(profile, asOf)
    ).flatten

    val multiplier = adjustments.map(_._1).product
    val point = raised * (baseMultiple * multiplier)

    val hasComparables =
      context.multipleBySector.contains(profile.sector) ||
        context.overallMultiple.isDefined
    val recent = profile.daysSinceLastFunding(asOf).exists(_ <= 730)
    val confidence = if hasComparables && recent then Medium else Low
    val spread = if confidence == Medium then BigDecimal(0.20) else BigDecimal(0.35)

    val reasoning =
      (stageNote(profile).toList :+ baseReason) ++ adjustments.map(_._2)

    ValuationEstimate(
      low = point * (1 - spread),
      high = point * (1 + spread),
      confidence = confidence,
      reasoning = reasoning
    )

  // --- Helpers --------------------------------------------------------------

  /** The base valuation-to-capital multiple: comparable companies in the same
    * sector if available, then the overall market, then a stage benchmark.
    */
  private def baseMultipleFor(
      sector: Sector,
      stage: FundingStage,
      context: ValuationContext
  ): (BigDecimal, String) =
    context.multipleBySector.get(sector) match
      case Some(m) =>
        (m, f"Comparable ${sector.label} companies at ~${m.toDouble}%.1fx capital raised")
      case None =>
        context.overallMultiple match
          case Some(m) =>
            (m, f"Market companies at ~${m.toDouble}%.1fx capital raised")
          case None =>
            val m = stageDefaultMultiple(stage)
            (m, f"${stage.label} stage benchmark of ~${m.toDouble}%.1fx capital raised")

  /** Typical valuation-to-capital multiples by stage, used only when no
    * comparable companies have disclosed a valuation.
    */
  private def stageDefaultMultiple(stage: FundingStage): BigDecimal =
    stage match
      case FundingStage.PreSeed | FundingStage.Angel => BigDecimal(6)
      case FundingStage.Seed                         => BigDecimal(5)
      case FundingStage.SeriesA                      => BigDecimal(5)
      case FundingStage.SeriesB                      => BigDecimal(4)
      case FundingStage.SeriesCPlus                  => BigDecimal(3)
      case _                                         => BigDecimal(3)

  private def momentumAdjustment(
      profile: CompanyProfile,
      asOf: LocalDate
  ): Option[(BigDecimal, String)] =
    profile.daysSinceLastFunding(asOf).flatMap { days =>
      if days <= 365 then
        Some((BigDecimal(1.05), "Above-average funding momentum"))
      else if days > 1095 then
        Some((BigDecimal(0.90), "Funding has gone quiet (3+ years)"))
      else None
    }

  private def stageNote(profile: CompanyProfile): Option[String] =
    profile.latestStage.map(_.label)

  private def empty(currency: Currency, reason: String): ValuationEstimate =
    ValuationEstimate(Money.zero(currency), Money.zero(currency), Low, List(reason))
