package fundscout.api

import com.sun.net.httpserver.{HttpExchange, HttpServer}
import fundscout.{FundScout, SampleData}
import fundscout.ingest.{Http, Ingestor, RawFundingRecord}
import fundscout.util.Json

import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import scala.io.Source

/** Serves [[FundScoutApi]] over HTTP using the JDK's built-in
  * `com.sun.net.httpserver` — no external web framework required.
  *
  * Run with `sbt "runMain fundscout.api.fundscoutServer [port] [source]"`. The
  * optional `source` is a CSV/JSON file path or URL to load instead of the
  * built-in sample data, so the dashboard can explore any dataset. The routing
  * logic lives in [[FundScoutApi]] and is unit-tested separately; this file is
  * only the transport binding.
  */
@main def fundscoutServer(args: String*): Unit =
  val port = args.headOption.flatMap(_.toIntOption).getOrElse(8080)
  val source = args.drop(1).headOption
  val records = source.map(loadRecords).getOrElse(SampleData.records)
  val registry = Ingestor.run(records).registry
  // Evaluate as of the most recent event in the data, not wall-clock now, so
  // recency-sensitive signals (confidence, momentum) are judged against the
  // dataset's own timeline rather than drifting stale as real time passes.
  val asOf = registry.allEvents.map(_.date).maxOption.getOrElse(LocalDate.now())
  val api = FundScoutApi(registry, asOf)

  val server = HttpServer.create(new InetSocketAddress(port), 0)
  val _ = server.createContext("/", (exchange: HttpExchange) => respond(exchange, api))
  server.setExecutor(null)
  server.start()

  val origin = source.getOrElse("built-in sample data")
  println(s"FundScout listening on http://localhost:$port  (data: $origin, as of $asOf)")
  println(s"Dashboard: http://localhost:$port/   ·   API: http://localhost:$port/market")

  // The HttpServer runs on background threads; block so the JVM stays alive
  // until the process is interrupted.
  new java.util.concurrent.CountDownLatch(1).await()

private def respond(exchange: HttpExchange, api: FundScoutApi): Unit =
  val method = exchange.getRequestMethod
  val path = exchange.getRequestURI.getPath

  // The browser dashboard is served at the root; everything else is the JSON API.
  val (status, contentType, body) =
    if method.equalsIgnoreCase("GET") && isDashboardPath(path) then
      (200, "text/html; charset=utf-8", Dashboard.html)
    else
      val response =
        try api.handle(method, path)
        catch
          case ex: Throwable =>
            ApiResponse(
              500,
              Json.obj("error" -> Json.str(Option(ex.getMessage).getOrElse("internal error")))
            )
      (response.status, "application/json", response.render)

  val bytes = body.getBytes(StandardCharsets.UTF_8)
  exchange.getResponseHeaders.set("Content-Type", contentType)
  exchange.sendResponseHeaders(status, bytes.length.toLong)
  val os = exchange.getResponseBody
  try os.write(bytes)
  finally os.close()

private def isDashboardPath(path: String): Boolean =
  path == "/" || path == "/index.html" || path == "/dashboard"

/** Load records from a CSV/JSON file path or URL, falling back to the built-in
  * sample data (with a warning) if the source can't be read or decoded.
  */
private def loadRecords(source: String): List[RawFundingRecord] =
  try
    val (contentType, text) =
      if source.startsWith("http://") || source.startsWith("https://") then
        Http.fetch(source) match
          case Right(doc)  => (doc.contentType, doc.body)
          case Left(error) => throw new RuntimeException(error)
      else
        val src = Source.fromFile(source)
        try (None, src.mkString)
        finally src.close()
    val decoded = FundScout.decode(source, contentType, text)
    decoded.errors.foreach(e => println(s"[warn] decode: ${e.describe}"))
    decoded.records
  catch
    case ex: Throwable =>
      println(s"[warn] could not load '$source' (${ex.getMessage}); using sample data")
      SampleData.records
