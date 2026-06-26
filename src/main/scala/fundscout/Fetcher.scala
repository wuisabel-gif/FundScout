package fundscout

import fundscout.ingest.Http

/** Fetches funding data from a URL over HTTP and prints a market report.
  *
  * Run with `sbt "runMain fundscout.fundscoutFetch <url>"`, where the URL serves
  * CSV or JSON in the same shape the file importer accepts. The format is chosen
  * from the URL extension or the response's content type. Exits non-zero on a
  * fetch failure.
  */
@main def fundscoutFetch(url: String): Unit =
  Http.fetch(url) match
    case Left(error) =>
      System.err.println(s"Failed to fetch $url: $error")
      System.exit(1)
    case Right(document) =>
      println(FundScout.report(url, document.contentType, document.body))
