package fundscout.ingest

/** A single reason a [[RawFundingRecord]] failed validation.
  *
  * Each case carries the offending field and value so the source of bad data is
  * always explainable — never a generic "invalid record". The parser collects
  * *all* errors in a record rather than stopping at the first, so a caller can
  * fix everything in one pass.
  */
enum IngestError(val field: String):
  /** A required field was blank or missing. */
  case MissingField(name: String) extends IngestError(name)

  /** The funding stage did not match any known [[fundscout.model.FundingStage]]. */
  case UnknownStage(raw: String) extends IngestError("stage")

  /** The amount could not be parsed as a non-negative number. */
  case InvalidAmount(raw: String) extends IngestError("amount")

  /** The valuation could not be parsed as a non-negative number. */
  case InvalidValuation(raw: String) extends IngestError("postMoneyValuation")

  /** The currency code/symbol was not recognized. */
  case UnknownCurrency(raw: String) extends IngestError("currency")

  /** The date was not a valid ISO-8601 (`yyyy-MM-dd`) date. */
  case InvalidDate(raw: String) extends IngestError("date")

  /** An investor tier string was present but unrecognized. */
  case UnknownInvestorTier(raw: String) extends IngestError("investor.tier")

  /** A founder pedigree string was present but unrecognized. */
  case UnknownFounderPedigree(raw: String) extends IngestError("founder.pedigree")

  /** A risk-flag severity string was present but unrecognized. */
  case UnknownRiskSeverity(raw: String) extends IngestError("riskFlag.severity")

  /** The lead investor id does not appear among the listed investors. */
  case LeadInvestorNotListed(id: String) extends IngestError("leadInvestorId")

  /** A human-readable explanation suitable for logs and reports. */
  def message: String = this match
    case MissingField(name)       => s"required field '$name' is missing or blank"
    case UnknownStage(raw)        => s"unknown funding stage: '$raw'"
    case InvalidAmount(raw)       => s"amount is not a non-negative number: '$raw'"
    case InvalidValuation(raw)    => s"valuation is not a non-negative number: '$raw'"
    case UnknownCurrency(raw)     => s"unknown currency: '$raw'"
    case InvalidDate(raw)         => s"date is not a valid yyyy-MM-dd date: '$raw'"
    case UnknownInvestorTier(raw) => s"unknown investor tier: '$raw'"
    case UnknownFounderPedigree(raw) => s"unknown founder pedigree: '$raw'"
    case UnknownRiskSeverity(raw) => s"unknown risk severity: '$raw'"
    case LeadInvestorNotListed(id) =>
      s"lead investor '$id' is not among the listed investors"
