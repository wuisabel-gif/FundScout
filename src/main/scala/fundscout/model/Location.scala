package fundscout.model

/** A geographic location, used for company headquarters and ecosystem
  * analytics (funding by country / city / region).
  *
  * `country` is required; `city` is optional because some sources only report
  * country-level data. `country` is expected to be a stable identifier (a
  * country name or ISO code) so that grouping is consistent.
  */
final case class Location(country: String, city: Option[String] = None):
  require(country.trim.nonEmpty, "location country must not be blank")

  /** `"San Francisco, USA"` or just `"USA"` when no city is known. */
  def display: String = city match
    case Some(c) => s"$c, $country"
    case None    => country

object Location:
  def apply(country: String, city: String): Location =
    Location(country, Some(city))
