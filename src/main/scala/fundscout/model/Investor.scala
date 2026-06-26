package fundscout.model

/** An investor participating in funding rounds (a VC firm, angel, or fund).
  *
  * Identity is the `id`, not the display name, so the same firm referenced
  * under slightly different names can be reconciled upstream. `tier` is an
  * explainable, coarse measure of reputation/track-record used by the Investor
  * Strength signal.
  */
final case class Investor(
    id: String,
    name: String,
    tier: InvestorTier = InvestorTier.Unknown
):
  require(id.trim.nonEmpty, "investor id must not be blank")
  require(name.trim.nonEmpty, "investor name must not be blank")

/** Coarse reputation tier for an investor.
  *
  * Tier 1 denotes a top-tier firm with a strong track record. The numeric
  * [[weight]] is deliberately simple and transparent so scoring can explain its
  * contribution rather than hide it inside a model.
  */
enum InvestorTier(val weight: Double):
  case Tier1   extends InvestorTier(1.0)
  case Tier2   extends InvestorTier(0.6)
  case Tier3   extends InvestorTier(0.3)
  case Unknown extends InvestorTier(0.1)

object InvestorTier:
  /** Parse a tier from input like `"Tier1"`, `"tier 1"`, or `"1"`. Blank or
    * absent input is treated as [[Unknown]]; other unrecognized input yields
    * `None` so callers can flag it.
    */
  def fromLabel(raw: String): Option[InvestorTier] =
    raw.trim.toLowerCase.filter(c => c.isLetterOrDigit) match
      case ""                  => Some(Unknown)
      case "unknown"           => Some(Unknown)
      case "tier1" | "1"       => Some(Tier1)
      case "tier2" | "2"       => Some(Tier2)
      case "tier3" | "3"       => Some(Tier3)
      case _                   => None
