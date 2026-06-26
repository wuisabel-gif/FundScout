package fundscout.ingest

/** A structural problem encountered while decoding raw input into
  * [[RawFundingRecord]]s — malformed CSV/JSON, a missing column, a ragged row.
  *
  * This is distinct from [[IngestError]]: decoders only check *shape*, leaving
  * value-level validation (bad stages, amounts, dates) to
  * [[FundingEventParser]]. `where` locates the problem (e.g. `"row 3"`).
  */
final case class DecodeError(where: String, message: String):
  def describe: String = s"$where: $message"

/** The outcome of decoding a batch of raw input: the records that decoded
  * structurally, plus any locations that did not. Decoders never throw.
  */
final case class DecodeResult(
    records: List[RawFundingRecord],
    errors: List[DecodeError]
):
  def isClean: Boolean = errors.isEmpty
