package fundscout.ingest

/** An untrusted, loosely-typed funding record as it arrives from an external
  * source (CSV row, JSON object, scraped announcement).
  *
  * Everything is a `String`/`Option[String]` because nothing has been validated
  * yet — fields may be blank, misspelled, or malformed. The ingest layer's job
  * is to turn one of these into a well-formed [[fundscout.model.FundingEvent]]
  * or a list of [[IngestError]]s explaining why it could not.
  *
  * Keeping this type free of any parsing library (no JSON/CSV dependency) means
  * any source can map into it; format-specific decoders live above this layer.
  */
final case class RawFundingRecord(
    id: String,
    companyId: String,
    companyName: String,
    stage: String,
    amount: String,
    date: String,
    sector: String,
    country: String,
    city: Option[String] = None,
    currency: String = "USD",
    investors: List[RawInvestor] = Nil,
    leadInvestorId: Option[String] = None,
    postMoneyValuation: Option[String] = None
)

/** An untrusted investor reference within a [[RawFundingRecord]]. */
final case class RawInvestor(
    id: String,
    name: String,
    tier: Option[String] = None
)
