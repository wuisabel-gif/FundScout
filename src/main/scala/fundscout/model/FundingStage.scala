package fundscout.model

/** The stage of a funding announcement.
  *
  * Stages fall into a few categories that downstream engines care about:
  *
  *   - **priced equity rounds** progress along a ladder (Pre-Seed → Series C+)
  *     and carry a [[progressionRank]];
  *   - **bridge** rounds are interim raises that do not advance the ladder;
  *   - **non-dilutive** funding (venture debt) raises capital without equity;
  *   - **exits** (IPO, Acquisition) end the private fundraising lifecycle.
  *
  * Keeping these distinctions explicit (rather than hidden inside heuristics)
  * lets scoring and prediction explain *why* a stage matters.
  */
enum FundingStage(val label: String, val category: StageCategory):
  case PreSeed     extends FundingStage("Pre-Seed", StageCategory.PricedRound)
  case Angel       extends FundingStage("Angel", StageCategory.PricedRound)
  case Seed        extends FundingStage("Seed", StageCategory.PricedRound)
  case SeriesA     extends FundingStage("Series A", StageCategory.PricedRound)
  case SeriesB     extends FundingStage("Series B", StageCategory.PricedRound)
  case SeriesCPlus extends FundingStage("Series C+", StageCategory.PricedRound)
  case Bridge      extends FundingStage("Bridge", StageCategory.Bridge)
  case VentureDebt extends FundingStage("Venture Debt", StageCategory.NonDilutive)
  case IPO         extends FundingStage("IPO", StageCategory.Exit)
  case Acquisition extends FundingStage("Acquisition", StageCategory.Exit)

  /** Position on the standard priced-equity ladder, or `None` for stages that
    * do not advance it (bridge, venture debt, exits).
    */
  def progressionRank: Option[Int] = this match
    case PreSeed     => Some(0)
    case Angel       => Some(1)
    case Seed        => Some(2)
    case SeriesA     => Some(3)
    case SeriesB     => Some(4)
    case SeriesCPlus => Some(5)
    case _           => None

  def isExit: Boolean = category == StageCategory.Exit

  /** Whether this stage represents capital raised *by* the company. An
    * acquisition is a purchase of the company, not capital it raises, so it is
    * excluded; an IPO does raise primary capital.
    */
  def raisesCapital: Boolean = this != Acquisition

object FundingStage:

  /** Case-insensitive lookup by enum name or display label, tolerant of spaces,
    * hyphens, and a trailing `+` (so `"Series C+"`, `"series c"`, and
    * `"SeriesCPlus"` all resolve). Returns `None` for unrecognized input.
    */
  def fromLabel(raw: String): Option[FundingStage] =
    val normalized = normalize(raw)
    values.find { s =>
      normalize(s.toString) == normalized || normalize(s.label) == normalized
    }

  private def normalize(s: String): String =
    s.filter(_.isLetterOrDigit).toLowerCase

  /** Order stages by how far along the lifecycle they sit. Priced rounds order
    * by their ladder rank; bridge and non-dilutive funding sort just after the
    * priced rounds; exits come last.
    */
  given Ordering[FundingStage] = Ordering.by[FundingStage, Int] { stage =>
    stage.progressionRank.getOrElse {
      stage.category match
        case StageCategory.Bridge      => 6
        case StageCategory.NonDilutive => 7
        case StageCategory.Exit        => 8
        case StageCategory.PricedRound => 9 // unreachable: priced rounds have a rank
    }
  }

/** Coarse grouping of funding stages used by analytics and scoring. */
enum StageCategory:
  case PricedRound, Bridge, NonDilutive, Exit
