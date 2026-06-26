package fundscout.scoring

import fundscout.analytics.MarketReport
import fundscout.model.*
import fundscout.scoring.Polarity.*

import java.time.LocalDate

/** Computes an explainable [[StartupScore]] for a company.
  *
  * Every component starts from a neutral baseline of 50 and is nudged up or down
  * by transparent rules, each of which records a [[ScoreReason]]. There are no
  * hidden weights or opaque models: the score is exactly the sum of its stated
  * reasons, clamped to 0–100, and the overall is the weighted average of the
  * components. This satisfies the requirement that a score always explain *why*
  * it is what it is.
  *
  * Components that depend on the wider ecosystem (sector strength, market
  * confidence) read from a [[MarketReport]]; `asOf` anchors recency-based
  * signals such as fundraising momentum.
  */
object StartupScorer:

  private type Signal = (Int, ScoreReason)
  private val Baseline = 50

  def score(
      profile: CompanyProfile,
      market: MarketReport,
      asOf: LocalDate
  ): StartupScore =
    val components = List(
      growth(profile),
      momentum(profile, asOf),
      investorStrength(profile),
      founderPedigree(profile),
      ecosystemStrength(profile, market),
      marketConfidence(profile, market),
      capitalEfficiency(profile),
      expansionPotential(profile, asOf)
    )
    val overall = clamp(
      math
        .round(components.map(c => c.value * c.component.weight).sum)
        .toInt
    )
    StartupScore(overall, components)

  // --- Components -----------------------------------------------------------

  /** Are funding rounds (and valuations) growing over time? */
  private def growth(profile: CompanyProfile): ComponentScore =
    val amounts = capitalAmounts(profile)
    val roundSignal: Option[Signal] =
      if amounts.sizeIs < 2 then
        Some((0, ScoreReason(Neutral, "Too few rounds to assess growth")))
      else if isStrictlyIncreasing(amounts) then
        Some((25, ScoreReason(Positive, "Round sizes grew every round")))
      else if amounts.last > amounts.head then
        Some((10, ScoreReason(Positive, "Funding rounds growing over time")))
      else if amounts.last < amounts(amounts.size - 2) then
        Some((-15, ScoreReason(Negative, "Latest round smaller than the previous one")))
      else None

    val valuations = profile.events.flatMap(_.postMoneyValuation).map(_.amount)
    val valuationSignal: Option[Signal] =
      if valuations.sizeIs >= 2 && valuations.last > valuations.head then
        Some((15, ScoreReason(Positive, "Valuation increased round-over-round")))
      else None

    build(ScoreComponent.GrowthScore, List(roundSignal, valuationSignal).flatten)

  /** How recently and how steadily the company is raising. */
  private def momentum(profile: CompanyProfile, asOf: LocalDate): ComponentScore =
    val recencySignal: Option[Signal] =
      profile.daysSinceLastFunding(asOf).map { days =>
        if days <= 365 then
          (25, ScoreReason(Positive, "Raised within the last 12 months"))
        else if days <= 730 then
          (5, ScoreReason(Neutral, "Last raised within two years"))
        else if days <= 1095 then
          (-10, ScoreReason(Negative, "Over two years since the last round"))
        else
          (-25, ScoreReason(Negative, "Large funding gap (3+ years since last round)"))
      }

    val cadenceSignal: Option[Signal] =
      val dates = profile.events.map(_.date)
      if dates.sizeIs >= 2 then
        val lastGapDays =
          java.time.temporal.ChronoUnit.DAYS
            .between(dates(dates.size - 2), dates.last)
        if lastGapDays <= 548 then // ~18 months
          Some((10, ScoreReason(Positive, "Fast round-to-round cadence")))
        else None
      else None

    build(ScoreComponent.FundingMomentum, List(recencySignal, cadenceSignal).flatten)

  /** Quality of the cap table, weighted toward the lead investor. */
  private def investorStrength(profile: CompanyProfile): ComponentScore =
    val leadSignal: Option[Signal] = bestByTier(profile.leadInvestors) match
      case Some(inv) =>
        inv.tier match
          case InvestorTier.Tier1 =>
            Some((30, ScoreReason(Positive, s"Tier-1 lead investor (${inv.name})")))
          case InvestorTier.Tier2 =>
            Some((15, ScoreReason(Positive, s"Tier-2 lead investor (${inv.name})")))
          case InvestorTier.Tier3 =>
            Some((5, ScoreReason(Neutral, s"Tier-3 lead investor (${inv.name})")))
          case InvestorTier.Unknown =>
            Some((0, ScoreReason(Neutral, "Lead investor of unknown reputation")))
      case None =>
        if profile.investors.nonEmpty then
          Some((0, ScoreReason(Neutral, "No disclosed lead investor")))
        else Some((-20, ScoreReason(Negative, "No disclosed investors")))

    val tier1Count = profile.investors.count(_.tier == InvestorTier.Tier1)
    val depthSignal: Option[Signal] =
      if tier1Count >= 2 then
        Some((10, ScoreReason(Positive, s"$tier1Count tier-1 investors on the cap table")))
      else None

    build(ScoreComponent.InvestorStrength, List(leadSignal, depthSignal).flatten)

  /** Reputation of the founding team — the "World Labs" signal. */
  private def founderPedigree(profile: CompanyProfile): ComponentScore =
    val signal: Signal = bestByPedigree(profile.founders) match
      case Some(f) =>
        f.pedigree match
          case FounderPedigree.Luminary =>
            (35, ScoreReason(Positive, s"Field-defining founder (${f.name})"))
          case FounderPedigree.Notable =>
            (20, ScoreReason(Positive, s"Renowned founder (${f.name})"))
          case FounderPedigree.Standard =>
            (5, ScoreReason(Neutral, s"Experienced founder (${f.name})"))
          case FounderPedigree.Unknown =>
            (0, ScoreReason(Neutral, "Founder reputation not assessed"))
      case None =>
        (0, ScoreReason(Neutral, "No founder information"))
    build(ScoreComponent.FounderPedigree, List(signal))

  /** Strength of the company's sector and geography within the ecosystem. */
  private def ecosystemStrength(
      profile: CompanyProfile,
      market: MarketReport
  ): ComponentScore =
    val topSectors = market.topSectors(3).map(_._1)
    val sectorSignal: Option[Signal] =
      if topSectors.headOption.contains(profile.sector) then
        Some((25, ScoreReason(Positive, s"Leading funded sector (${profile.sector.label})")))
      else if topSectors.contains(profile.sector) then
        Some((15, ScoreReason(Positive, s"Top-3 funded sector (${profile.sector.label})")))
      else None

    val topCountries = market.topCountries(2).map(_._1)
    val geoSignal: Option[Signal] =
      if topCountries.contains(profile.headquarters.country) then
        Some((10, ScoreReason(Positive,
          s"Located in a leading funding hub (${profile.headquarters.country})")))
      else None

    build(ScoreComponent.EcosystemStrength, List(sectorSignal, geoSignal).flatten)

  /** Confidence the market is showing in the company. */
  private def marketConfidence(
      profile: CompanyProfile,
      market: MarketReport
  ): ComponentScore =
    val roundSizeSignal: Option[Signal] =
      for
        median <- market.medianRoundSize
        latest <- profile.latestEvent.filter(_.stage.raisesCapital).map(_.amount)
        if latest.currency == median.currency
      yield
        if latest.amount >= median.amount then
          (15, ScoreReason(Positive, "Latest round above the market median"))
        else (-5, ScoreReason(Negative, "Latest round below the market median"))

    val unicornSignal: Option[Signal] =
      profile.latestValuation
        .filter(_.amount >= BigDecimal(1_000_000_000))
        .map(v => (20, ScoreReason(Positive, s"Unicorn-scale valuation (${v.display})")))

    val exitSignal: Option[Signal] =
      profile.latestStage.collect {
        case FundingStage.IPO =>
          (15, ScoreReason(Positive, "Reached IPO"))
        case FundingStage.Acquisition =>
          (5, ScoreReason(Positive, "Acquired"))
      }

    // Negative news (risk flags) directly weighs on how the market sees the
    // company — surfaced as explicit negative reasons, never hidden.
    val riskSignals: List[Signal] = profile.riskFlags.map { f =>
      val when = f.date.map(d => s" (${d.getYear})").getOrElse("")
      (-f.severity.penalty, ScoreReason(Negative, s"${f.description}$when"))
    }

    build(
      ScoreComponent.MarketConfidence,
      List(roundSizeSignal, unicornSignal, exitSignal).flatten ++ riskSignals
    )

  /** Valuation achieved per dollar of capital raised. */
  private def capitalEfficiency(profile: CompanyProfile): ComponentScore =
    val raised = profile.totalCapitalRaised.amount
    val signal: Option[Signal] = profile.latestValuation match
      case Some(valuation) if raised > 0 =>
        val multiple = valuation.amount / raised
        if multiple >= 10 then
          Some((30, ScoreReason(Positive,
            f"Very high valuation-to-capital multiple (${multiple.toDouble}%.0fx)")))
        else if multiple >= 5 then
          Some((15, ScoreReason(Positive,
            f"Strong valuation-to-capital multiple (${multiple.toDouble}%.0fx)")))
        else if multiple >= 2 then
          Some((5, ScoreReason(Neutral,
            f"Moderate valuation-to-capital multiple (${multiple.toDouble}%.1fx)")))
        else
          Some((-20, ScoreReason(Negative, "Valuation at or below capital raised")))
      case _ =>
        Some((0, ScoreReason(Neutral, "No disclosed valuation to assess efficiency")))

    build(ScoreComponent.CapitalEfficiency, signal.toList)

  /** Remaining room to grow, given stage and whether the company has exited. */
  private def expansionPotential(
      profile: CompanyProfile,
      asOf: LocalDate
  ): ComponentScore =
    val stageSignal: Option[Signal] = profile.latestStage.map { stage =>
      if stage.isExit then
        (-25, ScoreReason(Negative, "Already exited — limited further expansion"))
      else
        stage.progressionRank match
          case Some(rank) if rank <= 2 =>
            (20, ScoreReason(Positive, "Early stage with room to grow"))
          case Some(_) =>
            (10, ScoreReason(Positive, "Growth stage with continued upside"))
          case None =>
            (0, ScoreReason(Neutral, s"${stage.label} round"))
    }

    val recencySignal: Option[Signal] =
      profile
        .daysSinceLastFunding(asOf)
        .filter(_ <= 365)
        .map(_ => (5, ScoreReason(Positive, "Recently active")))

    build(ScoreComponent.ExpansionPotential, List(stageSignal, recencySignal).flatten)

  // --- Helpers --------------------------------------------------------------

  private def build(component: ScoreComponent, signals: List[Signal]): ComponentScore =
    val value = clamp(Baseline + signals.map(_._1).sum)
    ComponentScore(component, value, signals.map(_._2))

  private def capitalAmounts(profile: CompanyProfile): Vector[BigDecimal] =
    profile.events.filter(_.stage.raisesCapital).map(_.amount.amount)

  private def isStrictlyIncreasing(xs: Vector[BigDecimal]): Boolean =
    xs.lazyZip(xs.drop(1)).forall(_ < _)

  private def bestByTier(investors: Set[Investor]): Option[Investor] =
    if investors.isEmpty then None else Some(investors.maxBy(_.tier.weight))

  private def bestByPedigree(founders: List[Founder]): Option[Founder] =
    if founders.isEmpty then None else Some(founders.maxBy(_.pedigree.weight))

  private def clamp(v: Int): Int = math.max(0, math.min(100, v))
