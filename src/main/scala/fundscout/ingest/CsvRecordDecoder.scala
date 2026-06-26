package fundscout.ingest

import fundscout.util.Csv

/** Decodes CSV text into [[RawFundingRecord]]s, ready for the validation layer.
  *
  * The first non-blank row is the header; columns are matched by name, so column
  * order is irrelevant and extra columns are ignored. Required columns: `id`,
  * `companyId`, `companyName`, `stage`, `amount`, `date`, `sector`, `country`.
  * Optional: `city`, `currency`, `investors`, `leadInvestorId`,
  * `postMoneyValuation`.
  *
  * Investors are encoded in a single `investors` cell as `;`-separated entries
  * of the form `id:name:tier` (tier optional), e.g.
  * `sequoia:Sequoia:Tier1;jane:Jane Doe`.
  */
object CsvRecordDecoder:

  private val Required =
    List("id", "companyId", "companyName", "stage", "amount", "date", "sector", "country")

  def decode(text: String): DecodeResult =
    val rows = Csv.parseRows(text).filterNot(_.forall(_.trim.isEmpty))
    rows match
      case Nil =>
        DecodeResult(Nil, List(DecodeError("input", "no rows found")))
      case header :: dataRows =>
        val columns = header.map(_.trim)
        val missing = Required.filterNot(columns.contains)
        if missing.nonEmpty then
          DecodeResult(
            Nil,
            List(DecodeError("header", s"missing required columns: ${missing.mkString(", ")}"))
          )
        else
          val index = columns.zipWithIndex.toMap
          val (records, errors) =
            dataRows.zipWithIndex.foldLeft(
              (List.empty[RawFundingRecord], List.empty[DecodeError])
            ) { case ((recs, errs), (row, i)) =>
              decodeRow(row, index, rowNumber = i + 2) match
                case Right(record) => (record :: recs, errs)
                case Left(error)   => (recs, error :: errs)
            }
          DecodeResult(records.reverse, errors.reverse)

  private def decodeRow(
      row: List[String],
      index: Map[String, Int],
      rowNumber: Int
  ): Either[DecodeError, RawFundingRecord] =
    if row.size != index.size then
      Left(DecodeError(
        s"row $rowNumber",
        s"expected ${index.size} fields, found ${row.size}"
      ))
    else
      def col(name: String): String =
        index.get(name).map(row).map(_.trim).getOrElse("")
      def optCol(name: String): Option[String] =
        Some(col(name)).filter(_.nonEmpty)

      Right(RawFundingRecord(
        id = col("id"),
        companyId = col("companyId"),
        companyName = col("companyName"),
        stage = col("stage"),
        amount = col("amount"),
        date = col("date"),
        sector = col("sector"),
        country = col("country"),
        city = optCol("city"),
        currency = optCol("currency").getOrElse("USD"),
        investors = parseInvestors(col("investors")),
        leadInvestorId = optCol("leadInvestorId"),
        postMoneyValuation = optCol("postMoneyValuation")
      ))

  private def parseInvestors(cell: String): List[RawInvestor] =
    if cell.trim.isEmpty then Nil
    else
      cell
        .split(';')
        .toList
        .map(_.trim)
        .filter(_.nonEmpty)
        .map { entry =>
          val parts = entry.split(':').map(_.trim)
          RawInvestor(
            id = parts.lift(0).getOrElse(""),
            name = parts.lift(1).getOrElse(""),
            tier = parts.lift(2).filter(_.nonEmpty)
          )
        }
