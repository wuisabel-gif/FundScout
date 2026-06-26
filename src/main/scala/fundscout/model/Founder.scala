package fundscout.model

/** A company founder.
  *
  * Founders are modeled because for a whole class of companies — the "World Labs
  * pattern" of a startup built around a renowned researcher — the founder's
  * reputation is the dominant fundraising signal, one that investor tiers and
  * round sizes don't capture on their own.
  *
  * `pedigree` is a coarse, deliberately transparent, manually-assigned tier. It
  * is a judgment call, not a measurement, and the scoring engine treats it as
  * exactly that: one explainable input among many, never a black box.
  */
final case class Founder(name: String, pedigree: FounderPedigree = FounderPedigree.Unknown):
  require(name.trim.nonEmpty, "founder name must not be blank")

/** Coarse reputation tier for a founder.
  *
  *   - `Luminary`  — field-defining: a Turing Award winner or the originator of a
  *     foundational technology (e.g. ImageNet, CNNs, LLVM).
  *   - `Notable`   — a widely recognized researcher or operator with a strong
  *     public track record.
  *   - `Standard`  — an experienced but not publicly prominent founder.
  *   - `Unknown`   — reputation not assessed.
  *
  * The numeric [[weight]] mirrors [[InvestorTier]] so the two reputation signals
  * compose consistently.
  */
enum FounderPedigree(val weight: Double):
  case Luminary extends FounderPedigree(1.0)
  case Notable  extends FounderPedigree(0.6)
  case Standard extends FounderPedigree(0.3)
  case Unknown  extends FounderPedigree(0.1)

object FounderPedigree:
  /** Parse a pedigree from input like `"Luminary"` or `"notable"`. Blank/absent
    * input is [[Unknown]]; other unrecognized input yields `None` so callers can
    * flag it.
    */
  def fromLabel(raw: String): Option[FounderPedigree] =
    raw.trim.toLowerCase.filter(_.isLetterOrDigit) match
      case ""         => Some(Unknown)
      case "unknown"  => Some(Unknown)
      case "luminary" => Some(Luminary)
      case "notable"  => Some(Notable)
      case "standard" => Some(Standard)
      case _          => None
