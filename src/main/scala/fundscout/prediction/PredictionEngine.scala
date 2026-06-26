package fundscout.prediction

import fundscout.analytics.MarketReport
import fundscout.model.*

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Estimates probabilistic future outcomes for a company using transparent,
  * rule-based logic (the "future prediction" objective — see the README).
  *
  * Every figure is a probability or a range, never a deterministic claim, and
  * every figure is accompanied by the observable signals behind it. Outcome
  * probabilities are independent likelihoods, not a partition that sums to one.
  *
  * Sector strength is read from a [[MarketReport]]; `asOf` anchors recency.
  */
object PredictionEngine:

  private val UnicornThreshold = BigDecimal(1_000_000_000)

  def forecast(
      profile: CompanyProfile,
      market: MarketReport,
      asOf: LocalDate
  ): CompanyForecast =
    CompanyForecast(
      nextRound = nextRound(profile, asOf),
      outcomes = List(
        unicorn(profile, market),
        ipo(profile),
        acquisition(profile, asOf),
        inactivity(profile, asOf)
      )
    )

  // --- Next round -----------------------------------------------------------

  private def nextRound(profile: CompanyProfile, asOf: LocalDate): NextRoundForecast =
    if profile.hasExited then
      NextRoundForecast(
        Probability.Zero,
        None,
        None,
        List("Company has already exited — no further private rounds expected")
      )
    else
      val (probability, reasons) = nextRoundProbability(profile, asOf)
      NextRoundForecast(
        probability = probability,
        expectedAmount = expectedNextAmount(profile),
        expectedMonths = expectedNextTiming(profile),
        reasons = reasons
      )

  private def nextRoundProbability(
      profile: CompanyProfile,
      asOf: LocalDate
  ): (Probability, List[String]) =
    val signals = List.newBuilder[(Double, String)]
    def signal(delta: Double, reason: String): Unit = signals += (delta -> reason)

    profile.latestStage.foreach {
      case s if s.progressionRank.exists(_ <= 3) =>
        signal(0.15, "Early stage — more rounds typically lie ahead")
      case FundingStage.SeriesCPlus =>
        signal(-0.20, "Late stage — likelier to exit than raise again")
      case _ => ()
    }

    if profile.investors.exists(_.tier == InvestorTier.Tier1) then
      signal(0.10, "Well-backed by a tier-1 investor")

    if profile.founders.exists(_.pedigree.weight >= FounderPedigree.Notable.weight) then
      signal(0.05, "Built around a renowned founder")

    profile.riskFlags.foreach { f =>
      f.severity match
        case RiskSeverity.Critical => signal(-0.25, f.description)
        case RiskSeverity.Serious  => signal(-0.12, f.description)
        case RiskSeverity.Watch    => ()
    }

    profile.daysSinceLastFunding(asOf).foreach { days =>
      if days <= 365 then signal(0.05, "Recently active")
      else if days > 1095 then
        signal(-0.30, "No funding in over three years — may be dormant")
    }

    val deltas = signals.result()
    val probability = Probability.clamp(0.6 + deltas.map(_._1).sum)
    (probability, deltas.map(_._2))

  /** Next rounds typically step up; estimate 1.5x–3x the last round. */
  private def expectedNextAmount(profile: CompanyProfile): Option[(Money, Money)] =
    lastCapitalRound(profile).map { last =>
      (last * 1.5, last * 3.0)
    }

  /** Estimate months to the next round from historical cadence, or a stage
    * benchmark when there is too little history.
    */
  private def expectedNextTiming(profile: CompanyProfile): Option[(Int, Int)] =
    val dates = profile.events.map(_.date)
    val gaps =
      dates.zip(dates.drop(1)).map((a, b) => ChronoUnit.DAYS.between(a, b))
    val typicalMonths =
      if gaps.nonEmpty then (gaps.sum / gaps.size / 30).toInt
      else stageGapMonths(profile.latestStage)
    Some((math.max(1, typicalMonths * 3 / 4), typicalMonths * 5 / 4))

  private def stageGapMonths(stage: Option[FundingStage]): Int =
    stage match
      case Some(FundingStage.SeriesCPlus) => 24
      case _                              => 18

  // --- Outcomes -------------------------------------------------------------

  private def unicorn(profile: CompanyProfile, market: MarketReport): OutcomeForecast =
    profile.latestValuation match
      case Some(v) if v.amount >= UnicornThreshold =>
        OutcomeForecast(Outcome.Unicorn, Probability.One,
          List(s"Already at a unicorn valuation (${v.display})"))
      case _ =>
        val signals = List.newBuilder[(Double, String)]
        def signal(delta: Double, reason: String): Unit = signals += (delta -> reason)

        profile.latestValuation.foreach { v =>
          if v.amount >= BigDecimal(500_000_000) then
            signal(0.30, s"Valuation already past ${v.display}")
          else if v.amount >= BigDecimal(250_000_000) then
            signal(0.15, s"Valuation approaching unicorn scale (${v.display})")
        }
        if market.topSectors(3).map(_._1).contains(profile.sector) then
          signal(0.10, s"Operates in a hot sector (${profile.sector.label})")
        if profile.investors.exists(_.tier == InvestorTier.Tier1) then
          signal(0.10, "Backed by a tier-1 investor")
        profile.founders.maxByOption(_.pedigree.weight).foreach { f =>
          f.pedigree match
            case FounderPedigree.Luminary =>
              signal(0.15, s"Field-defining founder (${f.name})")
            case FounderPedigree.Notable =>
              signal(0.08, s"Renowned founder (${f.name})")
            case _ => ()
        }

        val deltas = signals.result()
        OutcomeForecast(
          Outcome.Unicorn,
          Probability.clamp(0.05 + deltas.map(_._1).sum),
          if deltas.isEmpty then List("Limited signals of unicorn trajectory")
          else deltas.map(_._2)
        )

  private def ipo(profile: CompanyProfile): OutcomeForecast =
    if profile.latestStage.contains(FundingStage.IPO) then
      OutcomeForecast(Outcome.Ipo, Probability.One, List("Has already gone public"))
    else
      val signals = List.newBuilder[(Double, String)]
      def signal(delta: Double, reason: String): Unit = signals += (delta -> reason)

      profile.latestStage.foreach {
        case FundingStage.SeriesCPlus => signal(0.20, "Late-stage company")
        case FundingStage.SeriesB     => signal(0.08, "Growth-stage company")
        case _                        => ()
      }
      if profile.totalCapitalRaised.amount >= BigDecimal(100_000_000) then
        signal(0.10, "Has raised substantial capital")
      if profile.latestValuation.exists(_.amount >= UnicornThreshold) then
        signal(0.15, "Unicorn-scale valuation")

      val deltas = signals.result()
      OutcomeForecast(
        Outcome.Ipo,
        Probability.clamp(0.03 + deltas.map(_._1).sum),
        if deltas.isEmpty then List("Too early to indicate an IPO path")
        else deltas.map(_._2)
      )

  private def acquisition(profile: CompanyProfile, asOf: LocalDate): OutcomeForecast =
    if profile.latestStage.contains(FundingStage.Acquisition) then
      OutcomeForecast(Outcome.Acquisition, Probability.One,
        List("Has already been acquired"))
    else
      val signals = List.newBuilder[(Double, String)]
      def signal(delta: Double, reason: String): Unit = signals += (delta -> reason)

      if !profile.latestValuation.exists(_.amount >= UnicornThreshold) then
        signal(0.10, "Sub-unicorn companies are common acquisition targets")
      profile.daysSinceLastFunding(asOf).foreach { days =>
        if days > 1095 then
          signal(0.10, "A long funding gap can precede an acquihire")
      }
      if profile.latestStage.contains(FundingStage.SeriesCPlus) then
        signal(-0.05, "Late-stage companies lean toward IPO over acquisition")

      val deltas = signals.result()
      OutcomeForecast(
        Outcome.Acquisition,
        Probability.clamp(0.25 + deltas.map(_._1).sum),
        ("Acquisition is the most common startup exit" :: deltas.map(_._2))
      )

  private def inactivity(profile: CompanyProfile, asOf: LocalDate): OutcomeForecast =
    if profile.hasExited then
      OutcomeForecast(Outcome.Inactivity, Probability.Zero,
        List("Company has already exited"))
    else
      val days = profile.daysSinceLastFunding(asOf).getOrElse(0L)
      val (base, recencyReason) =
        if days <= 365 then (0.05, "Raised within the last year")
        else if days <= 730 then (0.15, "Over a year since the last round")
        else if days <= 1095 then (0.35, "Over two years since the last round")
        else (0.60, "No funding in over three years")

      val weakBacking = !profile.investors.exists(_.tier == InvestorTier.Tier1)
      val penalty = if weakBacking then 0.10 else 0.0
      val backingReasons =
        if weakBacking then List(recencyReason, "No tier-1 investor backing")
        else List(recencyReason)

      // Alarming developments raise the odds the company stalls or fails.
      val riskBump = profile.riskFlags.map { f =>
        f.severity match
          case RiskSeverity.Critical => 0.25
          case RiskSeverity.Serious  => 0.15
          case RiskSeverity.Watch    => 0.05
      }.sum
      val reasons = backingReasons ++ profile.riskFlags.map(_.description)

      OutcomeForecast(
        Outcome.Inactivity,
        Probability.clamp(base + penalty + riskBump),
        reasons
      )

  // --- Helpers --------------------------------------------------------------

  private def lastCapitalRound(profile: CompanyProfile): Option[Money] =
    profile.events.filter(_.stage.raisesCapital).lastOption.map(_.amount)
