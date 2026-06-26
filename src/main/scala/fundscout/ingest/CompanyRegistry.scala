package fundscout.ingest

import fundscout.model.{CompanyProfile, FundingEvent, ProfileError}

/** An immutable registry of [[fundscout.model.CompanyProfile]]s keyed by
  * company id.
  *
  * This is the "Company Profiles" stage of the pipeline: it accepts validated
  * events and folds each into the right company's profile, creating a profile on
  * first sight of a company. Like everything below it, ingestion is pure —
  * `ingest` returns a *new* registry rather than mutating this one.
  */
final case class CompanyRegistry private (profiles: Map[String, CompanyProfile]):

  def get(companyId: String): Option[CompanyProfile] = profiles.get(companyId)
  def companies: Iterable[CompanyProfile] = profiles.values
  def size: Int = profiles.size

  /** Every funding event across all tracked companies, in no particular order. */
  def allEvents: List[FundingEvent] = profiles.values.flatMap(_.events).toList

  /** Fold a single event into the matching profile, starting a new profile if
    * the company has not been seen before. Returns a [[ProfileError]] if the
    * event is inconsistent with the existing profile (see
    * [[fundscout.model.CompanyProfile.addEvent]]).
    */
  def ingest(event: FundingEvent): Either[ProfileError, CompanyRegistry] =
    val base = profiles.getOrElse(
      event.companyId,
      CompanyProfile.start(
        event.companyId,
        event.companyName,
        event.sector,
        event.headquarters
      )
    )
    base
      .addEvent(event)
      .map(updated => copy(profiles = profiles.updated(event.companyId, updated)))

  /** Fold a batch of events, collecting any that could not be applied. Events
    * are processed in the given order; a rejected event leaves the registry
    * unchanged and is reported, so one bad event does not abort the batch.
    */
  def ingestAll(
      events: Iterable[FundingEvent]
  ): (CompanyRegistry, List[(FundingEvent, ProfileError)]) =
    events.foldLeft((this, List.empty[(FundingEvent, ProfileError)])) {
      case ((registry, errors), event) =>
        registry.ingest(event) match
          case Right(updated) => (updated, errors)
          case Left(error)    => (registry, errors :+ (event, error))
    }

object CompanyRegistry:
  val empty: CompanyRegistry = CompanyRegistry(Map.empty)
