package fundscout.model

import java.time.LocalDate

/** A negative or alarming development about a company — the kind of red flag that
  * shows up in press and social chatter rather than in funding data.
  *
  * FundScout can't fetch news itself, so risk flags are supplied as input (like
  * founders) and the platform's job is to surface them prominently and let them
  * weigh on the analysis, never to hide them inside an aggregate number.
  *
  * @param severity    how alarming the development is
  * @param description plain-language summary of what happened
  * @param date        when it happened, if known
  */
final case class RiskFlag(
    severity: RiskSeverity,
    description: String,
    date: Option[LocalDate] = None
):
  require(description.trim.nonEmpty, "risk flag description must not be blank")

/** How serious a [[RiskFlag]] is. The numeric [[penalty]] is the number of points
  * it removes from the Market Confidence score — transparent, not hidden.
  */
enum RiskSeverity(val penalty: Int):
  case Watch    extends RiskSeverity(5)
  case Serious  extends RiskSeverity(15)
  case Critical extends RiskSeverity(30)

object RiskSeverity:
  /** Parse a severity from input like `"Serious"` (case-insensitive); blank input
    * defaults to [[Watch]]; unrecognized input yields `None`.
    */
  def fromLabel(raw: String): Option[RiskSeverity] =
    raw.trim.toLowerCase.filter(_.isLetterOrDigit) match
      case ""         => Some(Watch)
      case "watch"    => Some(Watch)
      case "serious"  => Some(Serious)
      case "critical" => Some(Critical)
      case _          => None
