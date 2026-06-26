package fundscout

import scala.io.Source

/** Loads funding records from a local CSV or JSON file and prints a market
  * report.
  *
  * Run with `sbt "runMain fundscout.fundscoutImport data/sample/funding_rounds.csv"`.
  * Format is chosen from the file extension; the actual work is shared with the
  * online fetcher via [[FundScout.report]].
  */
@main def fundscoutImport(path: String): Unit =
  val source = Source.fromFile(path)
  val text =
    try source.mkString
    finally source.close()
  println(FundScout.report(path, contentType = None, text))
