package fundscout.ingest

import fundscout.model.{FundingEvent, ProfileError}

/** End-to-end ingest façade: raw records in, a populated
  * [[CompanyRegistry]] out, with every failure surfaced.
  *
  * It chains the two stages — validation
  * ([[FundingEventParser]]) then profile-building ([[CompanyRegistry]]) — and
  * reports both kinds of failure separately so nothing is silently dropped.
  */
object Ingestor:

  /** Outcome of an ingest run.
    *
    * @param registry
    *   the resulting company profiles
    * @param parseFailures
    *   records that failed validation, with their reasons
    * @param profileFailures
    *   valid events that could not be folded into a profile (e.g. inconsistent
    *   currency), with the reason
    */
  final case class Result(
      registry: CompanyRegistry,
      parseFailures: List[FundingEventParser.FailedRecord],
      profileFailures: List[(FundingEvent, ProfileError)]
  ):
    def isClean: Boolean = parseFailures.isEmpty && profileFailures.isEmpty

  /** Validate and ingest `records` into the given registry (empty by default). */
  def run(
      records: Iterable[RawFundingRecord],
      into: CompanyRegistry = CompanyRegistry.empty
  ): Result =
    val (events, parseFailures) = FundingEventParser.parseAll(records)
    val (registry, profileFailures) = into.ingestAll(events)
    Result(registry, parseFailures, profileFailures)
