package fundscout.ingest

import fundscout.util.Json.*
import fundscout.util.{Json, JsonParser}

/** Decodes JSON text into [[RawFundingRecord]]s, ready for the validation layer.
  *
  * Accepts a top-level array of record objects, a single record object, or an
  * object with a `records` array. Each record object uses the same field names
  * as [[RawFundingRecord]]; `amount`/`postMoneyValuation` may be a JSON number or
  * string. `investors` is an array of `{ "id", "name", "tier"? }` objects.
  *
  * As with the CSV decoder, only structure is checked here — value validation is
  * left to [[FundingEventParser]].
  */
object JsonRecordDecoder:

  private val Required =
    List("id", "companyId", "companyName", "stage", "amount", "date", "sector", "country")

  def decode(text: String): DecodeResult =
    JsonParser.parse(text) match
      case Left(error) =>
        DecodeResult(Nil, List(DecodeError("json", error)))
      case Right(json) =>
        val elements = json match
          case JArray(items) => items
          case JObject(fields) =>
            fields.toMap.get("records") match
              case Some(JArray(items)) => items
              case _                   => List(json)
          case other => List(other)

        val (records, errors) =
          elements.zipWithIndex.foldLeft(
            (List.empty[RawFundingRecord], List.empty[DecodeError])
          ) { case ((recs, errs), (element, i)) =>
            decodeElement(element, i) match
              case Right(record) => (record :: recs, errs)
              case Left(error)   => (recs, error :: errs)
          }
        DecodeResult(records.reverse, errors.reverse)

  private def decodeElement(json: Json, idx: Int): Either[DecodeError, RawFundingRecord] =
    json match
      case JObject(fields) =>
        val m = fields.toMap
        val missing = Required.filterNot(k => m.get(k).flatMap(scalar).isDefined)
        if missing.nonEmpty then
          Left(DecodeError(s"record $idx", s"missing fields: ${missing.mkString(", ")}"))
        else
          decodeInvestors(m.get("investors")) match
            case Left(message) => Left(DecodeError(s"record $idx", message))
            case Right(investors) =>
              Right(RawFundingRecord(
                id = str(m, "id"),
                companyId = str(m, "companyId"),
                companyName = str(m, "companyName"),
                stage = str(m, "stage"),
                amount = str(m, "amount"),
                date = str(m, "date"),
                sector = str(m, "sector"),
                country = str(m, "country"),
                city = strOpt(m, "city"),
                currency = strOpt(m, "currency").getOrElse("USD"),
                investors = investors,
                leadInvestorId = strOpt(m, "leadInvestorId"),
                postMoneyValuation = strOpt(m, "postMoneyValuation")
              ))
      case _ =>
        Left(DecodeError(s"record $idx", "expected a JSON object"))

  private def decodeInvestors(value: Option[Json]): Either[String, List[RawInvestor]] =
    value match
      case None | Some(JNull) => Right(Nil)
      case Some(JArray(items)) =>
        val decoded = items.map {
          case JObject(fs) =>
            val im = fs.toMap
            Right(RawInvestor(
              id = im.get("id").flatMap(scalar).getOrElse(""),
              name = im.get("name").flatMap(scalar).getOrElse(""),
              tier = im.get("tier").flatMap(scalar)
            ))
          case _ => Left("each investor must be a JSON object")
        }
        decoded.collectFirst { case Left(message) => message } match
          case Some(message) => Left(message)
          case None          => Right(decoded.collect { case Right(investor) => investor })
      case Some(_) => Left("'investors' must be an array")

  /** A scalar JSON value rendered as a string, or `None` for null/containers. */
  private def scalar(json: Json): Option[String] = json match
    case JString(s)   => Some(s)
    case JNumber(lit) => Some(lit)
    case JBool(b)     => Some(b.toString)
    case _            => None

  private def str(m: Map[String, Json], key: String): String =
    m.get(key).flatMap(scalar).getOrElse("")

  private def strOpt(m: Map[String, Json], key: String): Option[String] =
    m.get(key).flatMap(scalar).filter(_.nonEmpty)
