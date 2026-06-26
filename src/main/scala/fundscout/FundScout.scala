package fundscout

import fundscout.analytics.MarketAnalytics
import fundscout.ingest.{CsvRecordDecoder, DecodeResult, Ingestor, JsonRecordDecoder}
import fundscout.reporting.MarketReportRenderer

/** High-level entry point that ties the pipeline together: given raw text from
  * any source (a file, a URL, a string), pick a decoder, validate, build
  * profiles, and render a market report.
  *
  * Keeping this as a pure `text => report` function means the file importer and
  * the online fetcher share identical logic and it can be tested without any I/O.
  */
object FundScout:

  enum Format:
    case Csv, Json

  /** Choose a decoder from the source name/extension first, then the HTTP
    * content type, defaulting to CSV.
    */
  def formatFor(sourceLabel: String, contentType: Option[String]): Format =
    val lower = sourceLabel.toLowerCase
    if lower.endsWith(".json") then Format.Json
    else if lower.endsWith(".csv") then Format.Csv
    else
      contentType.map(_.toLowerCase) match
        case Some(ct) if ct.contains("json") => Format.Json
        case Some(ct) if ct.contains("csv")  => Format.Csv
        case _                               => Format.Csv

  def decode(sourceLabel: String, contentType: Option[String], text: String): DecodeResult =
    formatFor(sourceLabel, contentType) match
      case Format.Csv  => CsvRecordDecoder.decode(text)
      case Format.Json => JsonRecordDecoder.decode(text)

  /** Decode, validate, and render a full market report (including any decode and
    * validation issues) as text.
    */
  def report(sourceLabel: String, contentType: Option[String], text: String): String =
    val decoded = decode(sourceLabel, contentType, text)
    val result = Ingestor.run(decoded.records)

    val lines = scala.collection.mutable.ArrayBuffer.empty[String]
    def add(line: String): Unit = lines += line

    if decoded.errors.nonEmpty then
      add(s"Decode issues (${decoded.errors.size}):")
      decoded.errors.foreach(e => add(s"  ${e.describe}"))
      add("")

    if !result.isClean then
      add("Validation issues:")
      result.parseFailures.foreach(f =>
        add(s"  ${f.record.id}: ${f.errors.map(_.message).mkString("; ")}")
      )
      result.profileFailures.foreach { case (event, error) =>
        add(s"  ${event.id}: ${error.message}")
      }
      add("")

    add(s"Loaded ${result.registry.size} companies from $sourceLabel")
    add("")
    add(MarketReportRenderer.render(MarketAnalytics.of(result.registry)))
    lines.mkString("\n")
