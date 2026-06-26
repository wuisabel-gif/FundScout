package fundscout.ingest

import fundscout.model.*

import java.time.LocalDate
import scala.util.Try

/** Validates untrusted [[RawFundingRecord]]s into well-formed
  * [[fundscout.model.FundingEvent]]s.
  *
  * This is FundScout's validation layer. It is pure (no I/O, no shared state)
  * and *accumulating*: a single record reports every problem at once rather than
  * failing on the first, so bad data can be corrected in one pass.
  *
  * Sectors are intentionally lenient — unrecognized sectors map to
  * [[fundscout.model.Sector.Other]] rather than failing — because a missing
  * sector label should not discard an otherwise valid funding event.
  */
object FundingEventParser:

  private type Validated[A] = Either[List[IngestError], A]

  /** A record that failed validation, paired with every reason it failed. */
  final case class FailedRecord(record: RawFundingRecord, errors: List[IngestError])

  /** Validate a single record. */
  def parse(r: RawFundingRecord): Validated[FundingEvent] =
    val vId          = required("id", r.id)
    val vCompanyId   = required("companyId", r.companyId)
    val vCompanyName = required("companyName", r.companyName)
    val vCountry     = required("country", r.country)
    val vStage       = parseStage(r.stage)
    val vAmount      = parseAmount(r.amount)
    val vCurrency    = parseCurrency(r.currency)
    val vDate        = parseDate(r.date)
    val vInvestors   = parseInvestors(r.investors)
    val vValuation   = parseValuation(r.postMoneyValuation)
    val vLead        = validateLead(r.leadInvestorId, r.investors.map(_.id.trim).toSet)

    val errors = List[Validated[?]](
      vId, vCompanyId, vCompanyName, vCountry, vStage, vAmount, vCurrency,
      vDate, vInvestors, vValuation, vLead
    ).flatMap(leftErrors)

    if errors.nonEmpty then Left(errors)
    else
      val currency  = vCurrency.toOption.get
      val investors = vInvestors.toOption.get
      val leadId    = vLead.toOption.get
      Right(
        FundingEvent(
          id = vId.toOption.get,
          companyId = vCompanyId.toOption.get,
          companyName = vCompanyName.toOption.get,
          stage = vStage.toOption.get,
          amount = Money(vAmount.toOption.get, currency),
          date = vDate.toOption.get,
          sector = Sector.fromLabel(r.sector),
          headquarters =
            Location(vCountry.toOption.get, r.city.map(_.trim).filter(_.nonEmpty)),
          investors = investors,
          leadInvestor = leadId.flatMap(id => investors.find(_.id == id)),
          postMoneyValuation = vValuation.toOption.get.map(Money(_, currency))
        )
      )

  /** Validate a batch, separating successes from failures. */
  def parseAll(
      records: Iterable[RawFundingRecord]
  ): (List[FundingEvent], List[FailedRecord]) =
    val (events, failures) =
      records.foldLeft((List.empty[FundingEvent], List.empty[FailedRecord])) {
        case ((evs, fails), record) =>
          parse(record) match
            case Right(event) => (event :: evs, fails)
            case Left(errs)   => (evs, FailedRecord(record, errs) :: fails)
      }
    (events.reverse, failures.reverse)

  private def leftErrors(v: Either[List[IngestError], ?]): List[IngestError] =
    v.fold(identity, _ => Nil)

  private def required(name: String, value: String): Validated[String] =
    val trimmed = value.trim
    if trimmed.isEmpty then Left(List(IngestError.MissingField(name)))
    else Right(trimmed)

  private def parseStage(raw: String): Validated[FundingStage] =
    if raw.trim.isEmpty then Left(List(IngestError.MissingField("stage")))
    else FundingStage.fromLabel(raw).toRight(List(IngestError.UnknownStage(raw)))

  private def parseCurrency(raw: String): Validated[Currency] =
    Currency.fromCode(raw).toRight(List(IngestError.UnknownCurrency(raw)))

  private def parseDate(raw: String): Validated[LocalDate] =
    val trimmed = raw.trim
    if trimmed.isEmpty then Left(List(IngestError.MissingField("date")))
    else
      Try(LocalDate.parse(trimmed)).toEither.left
        .map(_ => List(IngestError.InvalidDate(raw)))

  /** Parse a numeric money amount, tolerating `$`, commas, and whitespace. */
  private def parseNumber(raw: String): Option[BigDecimal] =
    val cleaned = raw.replaceAll("[,$\\s]", "")
    if cleaned.isEmpty then None
    else Try(BigDecimal(cleaned)).toOption.filter(_ >= 0)

  private def parseAmount(raw: String): Validated[BigDecimal] =
    if raw.trim.isEmpty then Left(List(IngestError.MissingField("amount")))
    else parseNumber(raw).toRight(List(IngestError.InvalidAmount(raw)))

  private def parseValuation(raw: Option[String]): Validated[Option[BigDecimal]] =
    raw.map(_.trim).filter(_.nonEmpty) match
      case None => Right(None)
      case Some(s) =>
        parseNumber(s).map(Some(_)).toRight(List(IngestError.InvalidValuation(s)))

  private def parseInvestors(raws: List[RawInvestor]): Validated[List[Investor]] =
    val parsed = raws.map(parseInvestor)
    val errors = parsed.flatMap(leftErrors)
    if errors.nonEmpty then Left(errors)
    else Right(parsed.flatMap(_.toOption))

  private def parseInvestor(ri: RawInvestor): Validated[Investor] =
    val idErr =
      if ri.id.trim.isEmpty then List(IngestError.MissingField("investor.id"))
      else Nil
    val nameErr =
      if ri.name.trim.isEmpty then List(IngestError.MissingField("investor.name"))
      else Nil
    val tier = InvestorTier.fromLabel(ri.tier.getOrElse(""))
    val tierErr =
      if tier.isEmpty then List(IngestError.UnknownInvestorTier(ri.tier.getOrElse("")))
      else Nil
    val errors = idErr ++ nameErr ++ tierErr
    if errors.nonEmpty then Left(errors)
    else Right(Investor(ri.id.trim, ri.name.trim, tier.get))

  private def validateLead(
      leadId: Option[String],
      investorIds: Set[String]
  ): Validated[Option[String]] =
    leadId.map(_.trim).filter(_.nonEmpty) match
      case None => Right(None)
      case Some(id) =>
        if investorIds.contains(id) then Right(Some(id))
        else Left(List(IngestError.LeadInvestorNotListed(id)))
