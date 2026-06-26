package fundscout.scoring

/** One of the seven explainable signals that make up a startup score.
  *
  * Each component carries the `weight` it contributes to the overall score; the
  * weights sum to `1.0` so the overall is a plain weighted average of the
  * component values. Keeping the weights here (rather than buried in the engine)
  * makes the blend transparent and easy to tune.
  */
enum ScoreComponent(val label: String, val weight: Double):
  case GrowthScore        extends ScoreComponent("Growth", 0.12)
  case FundingMomentum    extends ScoreComponent("Funding Momentum", 0.16)
  case InvestorStrength   extends ScoreComponent("Investor Strength", 0.16)
  case FounderPedigree    extends ScoreComponent("Founder Pedigree", 0.15)
  case EcosystemStrength  extends ScoreComponent("Ecosystem Strength", 0.12)
  case MarketConfidence   extends ScoreComponent("Market Confidence", 0.10)
  case CapitalEfficiency  extends ScoreComponent("Capital Efficiency", 0.09)
  case ExpansionPotential extends ScoreComponent("Expansion Potential", 0.10)

/** Whether a reason pushed a score up, down, or is purely informational. */
enum Polarity:
  case Positive, Negative, Neutral

  def sign: String = this match
    case Positive => "+"
    case Negative => "−"
    case Neutral  => "•"

/** A single human-readable reason behind a component score — the "why" that
  * makes the score explainable rather than a black box.
  */
final case class ScoreReason(polarity: Polarity, description: String):
  override def toString: String = s"${polarity.sign} $description"

/** The 0–100 value of one component, together with the reasons that produced
  * it.
  */
final case class ComponentScore(
    component: ScoreComponent,
    value: Int,
    reasons: List[ScoreReason]
)

/** A company's overall startup score (0–100) and its component breakdown.
  *
  * The overall value is the weighted average of the component values using each
  * [[ScoreComponent.weight]]. Every contributing reason is reachable via
  * [[reasons]] / [[positives]] / [[negatives]], so the score can always explain
  * itself.
  */
final case class StartupScore(overall: Int, components: List[ComponentScore]):

  def component(c: ScoreComponent): Option[ComponentScore] =
    components.find(_.component == c)

  def reasons: List[ScoreReason] = components.flatMap(_.reasons)
  def positives: List[ScoreReason] = reasons.filter(_.polarity == Polarity.Positive)
  def negatives: List[ScoreReason] = reasons.filter(_.polarity == Polarity.Negative)
