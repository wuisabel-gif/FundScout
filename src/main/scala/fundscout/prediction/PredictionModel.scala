package fundscout.prediction

import fundscout.model.Money

/** A probability in the closed interval [0, 1].
  *
  * A dedicated type keeps every forecast in valid bounds and gives a single
  * place to format it. Construct via [[Probability.clamp]] (which saturates out-
  * of-range inputs) or [[Probability.apply]] (which requires a valid value).
  */
final case class Probability private (value: Double):
  /** Rounded whole-percent form, e.g. `42`. */
  def percent: Int = math.round(value * 100).toInt
  def display: String = s"$percent%"

object Probability:
  val Zero: Probability = new Probability(0.0)
  val One: Probability = new Probability(1.0)

  def apply(value: Double): Probability =
    require(value >= 0.0 && value <= 1.0, s"probability must be in [0,1], was $value")
    new Probability(value)

  /** Saturate any input into [0, 1]. */
  def clamp(value: Double): Probability =
    new Probability(math.max(0.0, math.min(1.0, value)))

  given Ordering[Probability] = Ordering.by(_.value)

/** A future company outcome that FundScout estimates a likelihood for. These are
  * independent likelihoods, not a mutually-exclusive partition — a company can
  * be both a likely unicorn and a likely acquisition target.
  */
enum Outcome(val label: String):
  case Unicorn     extends Outcome("Unicorn")
  case Ipo         extends Outcome("IPO")
  case Acquisition extends Outcome("Acquisition")
  case Inactivity  extends Outcome("Inactivity / failure")

/** The estimated likelihood of a single [[Outcome]], with its reasoning. */
final case class OutcomeForecast(
    outcome: Outcome,
    probability: Probability,
    reasons: List[String]
)

/** A forecast for the company's next funding round: how likely it is, and — when
  * meaningful — the expected size and timing, all as ranges.
  *
  * `expectedAmount` and `expectedMonths` are `None` for companies that have
  * already exited, where a "next round" is not meaningful.
  */
final case class NextRoundForecast(
    probability: Probability,
    expectedAmount: Option[(Money, Money)],
    expectedMonths: Option[(Int, Int)],
    reasons: List[String]
)

/** The complete probabilistic forecast for a company. */
final case class CompanyForecast(
    nextRound: NextRoundForecast,
    outcomes: List[OutcomeForecast]
):
  def outcome(o: Outcome): Option[OutcomeForecast] = outcomes.find(_.outcome == o)
