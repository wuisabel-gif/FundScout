package fundscout.model

import java.time.LocalDate

/** An immutable record of a single funding announcement.
  *
  * This is FundScout's fundamental business event: every announcement becomes
  * one `FundingEvent`, and a company's profile is the fold of its events over
  * time. Events are never mutated; corrections are modeled as new events.
  *
  * Structural invariants (a non-blank id, a lead investor that actually
  * participated) are enforced here so that no malformed event can exist in the
  * system. Validation of *untrusted* input (parsing, plausibility checks) is the
  * job of the ingest layer, which should produce well-formed events or reject
  * them.
  *
  * @param id
  *   stable unique identifier for this announcement
  * @param companyId
  *   identifier of the company that raised the round
  * @param companyName
  *   company display name at the time of the event
  * @param stage
  *   the funding stage (see [[FundingStage]])
  * @param amount
  *   the headline amount; for an acquisition this is the purchase price
  * @param date
  *   the announcement date
  * @param sector
  *   the company's sector at the time of the event
  * @param headquarters
  *   the company's headquarters location at the time of the event
  * @param investors
  *   all participating investors (may be empty if undisclosed)
  * @param leadInvestor
  *   the lead investor, if disclosed; must be one of `investors`
  * @param postMoneyValuation
  *   the post-money valuation, if disclosed
  */
final case class FundingEvent(
    id: String,
    companyId: String,
    companyName: String,
    stage: FundingStage,
    amount: Money,
    date: LocalDate,
    sector: Sector,
    headquarters: Location,
    investors: List[Investor] = Nil,
    leadInvestor: Option[Investor] = None,
    postMoneyValuation: Option[Money] = None
):
  require(id.trim.nonEmpty, "funding event id must not be blank")
  require(companyId.trim.nonEmpty, "funding event companyId must not be blank")
  require(
    companyName.trim.nonEmpty,
    "funding event companyName must not be blank"
  )
  leadInvestor.foreach { lead =>
    require(
      investors.exists(_.id == lead.id),
      s"lead investor ${lead.id} must be among the participating investors"
    )
  }

  /** Identifiers of all participating investors. */
  def investorIds: Set[String] = investors.map(_.id).toSet

  def year: Int = date.getYear
