package fundscout.valuation

import fundscout.model.Money

/** How much confidence to place in a valuation estimate.
  *
  *   - `High` — anchored on a recently disclosed post-money valuation;
  *   - `Medium` — derived from comparable-company multiples on recent data;
  *   - `Low` — derived from sparse signals or stale data.
  */
enum Confidence:
  case Low, Medium, High

/** An explainable valuation estimate expressed as a range.
  *
  * Version 1 of FundScout deliberately produces a *range* with an explicit
  * [[confidence]] and a list of plain-language [[reasoning]] bullets rather than
  * a single point number — valuing a private company from observable signals is
  * inherently uncertain, and the model should say so transparently.
  *
  * @param low
  *   lower bound of the estimated range
  * @param high
  *   upper bound of the estimated range (same currency as `low`)
  * @param confidence
  *   how much to trust the estimate
  * @param reasoning
  *   the observable signals behind the estimate, most important first
  */
final case class ValuationEstimate(
    low: Money,
    high: Money,
    confidence: Confidence,
    reasoning: List[String]
):
  /** The midpoint of the range — a convenience point estimate. */
  def midpoint: Money = Money((low.amount + high.amount) / 2, low.currency)
