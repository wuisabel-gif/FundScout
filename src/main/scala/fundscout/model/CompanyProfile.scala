package fundscout.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** An evolving, immutable profile for a single company.
  *
  * A profile is the fold of a company's [[FundingEvent]]s over time: it starts
  * from seed metadata and becomes richer as events arrive. Every "update" yields
  * a new `CompanyProfile`; nothing is mutated in place.
  *
  * The `name`, `sector`, and `headquarters` fields hold the *current* best-known
  * values — they are refreshed from the most recent event, since a later
  * announcement reflects later truth. Historical values remain available in
  * [[events]].
  *
  * Construct via [[CompanyProfile.start]] (then [[addEvent]]) or
  * [[CompanyProfile.fromEvents]]; the constructor is private so the invariants
  * below always hold:
  *
  *   - all `events` share this profile's `companyId`;
  *   - `events` are ordered ascending by date;
  *   - all events use a single currency.
  */
final case class CompanyProfile private (
    companyId: String,
    name: String,
    sector: Sector,
    headquarters: Location,
    foundedDate: Option[LocalDate],
    events: Vector[FundingEvent]
):

  def isEmpty: Boolean = events.isEmpty
  def roundCount: Int = events.size

  def firstEvent: Option[FundingEvent] = events.headOption
  def latestEvent: Option[FundingEvent] = events.lastOption

  def firstFundingDate: Option[LocalDate] = firstEvent.map(_.date)
  def lastFundingDate: Option[LocalDate] = latestEvent.map(_.date)
  def latestStage: Option[FundingStage] = latestEvent.map(_.stage)

  /** The currency all of this company's events are denominated in, if any. */
  def currency: Option[Currency] = firstEvent.map(_.amount.currency)

  /** Total capital the company has raised, summing every round that represents
    * capital raised by the company (everything except acquisitions — see
    * [[FundingStage.raisesCapital]]). Returns zero when no such rounds exist.
    */
  def totalCapitalRaised: Money =
    val raising = events.filter(_.stage.raisesCapital).map(_.amount)
    raising.reduceOption(_ + _).getOrElse {
      Money.zero(currency.getOrElse(Currency.USD))
    }

  /** All distinct investors that have participated, deduplicated by id. */
  def investors: Set[Investor] =
    events.flatMap(_.investors).groupBy(_.id).values.map(_.head).toSet

  /** Distinct lead investors across all rounds. */
  def leadInvestors: Set[Investor] =
    events.flatMap(_.leadInvestor).groupBy(_.id).values.map(_.head).toSet

  /** The most recently disclosed post-money valuation, if any. */
  def latestValuation: Option[Money] =
    events.reverse.collectFirst {
      case e if e.postMoneyValuation.isDefined => e.postMoneyValuation.get
    }

  /** Whether the company has had an exit event (IPO or acquisition). */
  def hasExited: Boolean = events.exists(_.stage.isExit)

  /** Company age in whole years as of `asOf`, when the founding date is known. */
  def ageInYears(asOf: LocalDate): Option[Long] =
    foundedDate.map(ChronoUnit.YEARS.between(_, asOf))

  /** Days since the last funding event as of `asOf` — a measure of fundraising
    * recency used by momentum signals.
    */
  def daysSinceLastFunding(asOf: LocalDate): Option[Long] =
    lastFundingDate.map(ChronoUnit.DAYS.between(_, asOf))

  /** Add a funding event, returning an updated profile or a [[ProfileError]]
    * describing why the event does not belong to this profile.
    *
    * The event must reference this company, use the same currency as existing
    * events, and not predate the founding date. On success the event is inserted
    * in chronological order and the current name/sector/headquarters are
    * refreshed from whichever event is now most recent.
    */
  def addEvent(event: FundingEvent): Either[ProfileError, CompanyProfile] =
    if event.companyId != companyId then
      Left(ProfileError.CompanyMismatch(companyId, event.companyId))
    else if currency.exists(_ != event.amount.currency) then
      Left(
        ProfileError.CurrencyMismatch(currency.get, event.amount.currency)
      )
    else if foundedDate.exists(event.date.isBefore) then
      Left(ProfileError.EventBeforeFounding(foundedDate.get, event.date))
    else
      val updated = insertOrdered(event)
      val current = updated.last // most recent event by date
      Right(
        copy(
          name = current.companyName,
          sector = current.sector,
          headquarters = current.headquarters,
          events = updated
        )
      )

  /** Insert an event into [[events]] preserving ascending date order. Ties keep
    * existing events before the newcomer (stable for same-day rounds).
    */
  private def insertOrdered(event: FundingEvent): Vector[FundingEvent] =
    val idx = events.lastIndexWhere(!_.date.isAfter(event.date)) + 1
    events.patch(idx, Vector(event), 0)

object CompanyProfile:

  /** Begin an empty profile from seed metadata, before any events are known. */
  def start(
      companyId: String,
      name: String,
      sector: Sector,
      headquarters: Location,
      foundedDate: Option[LocalDate] = None
  ): CompanyProfile =
    require(companyId.trim.nonEmpty, "companyId must not be blank")
    require(name.trim.nonEmpty, "name must not be blank")
    new CompanyProfile(
      companyId,
      name,
      sector,
      headquarters,
      foundedDate,
      Vector.empty
    )

  /** Build a profile by folding a non-empty collection of events for one
    * company. The seed metadata is taken from the earliest event and refreshed
    * as later events fold in.
    */
  def fromEvents(
      events: Iterable[FundingEvent],
      foundedDate: Option[LocalDate] = None
  ): Either[ProfileError, CompanyProfile] =
    val ordered = events.toVector.sortBy(_.date)
    ordered.headOption match
      case None => Left(ProfileError.NoEvents)
      case Some(first) =>
        val seed = start(
          first.companyId,
          first.companyName,
          first.sector,
          first.headquarters,
          foundedDate
        )
        ordered.foldLeft[Either[ProfileError, CompanyProfile]](Right(seed)) {
          (acc, event) => acc.flatMap(_.addEvent(event))
        }

/** Reasons a [[FundingEvent]] cannot be folded into a [[CompanyProfile]]. */
enum ProfileError:
  /** The event belongs to a different company. */
  case CompanyMismatch(expected: String, found: String)

  /** The event uses a currency that differs from the profile's. */
  case CurrencyMismatch(expected: Currency, found: Currency)

  /** The event predates the company's founding date. */
  case EventBeforeFounding(founded: LocalDate, eventDate: LocalDate)

  /** No events were supplied to build a profile. */
  case NoEvents

  /** A human-readable explanation, for validation messages and reports. */
  def message: String = this match
    case CompanyMismatch(expected, found) =>
      s"event for company '$found' cannot be added to profile of '$expected'"
    case CurrencyMismatch(expected, found) =>
      s"event currency $found does not match profile currency $expected"
    case EventBeforeFounding(founded, eventDate) =>
      s"event dated $eventDate predates founding date $founded"
    case NoEvents =>
      "cannot build a company profile from zero events"
