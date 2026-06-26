package fundscout.api

import com.sun.net.httpserver.{HttpExchange, HttpServer}
import fundscout.SampleData
import fundscout.ingest.Ingestor
import fundscout.util.Json

import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.time.LocalDate

/** Serves [[FundScoutApi]] over HTTP using the JDK's built-in
  * `com.sun.net.httpserver` — no external web framework required.
  *
  * Run with `sbt "runMain fundscout.api.fundscoutServer"` (optionally pass a
  * port as an argument).
  * The routing logic lives in [[FundScoutApi]] and is unit-tested separately;
  * this file is only the transport binding.
  */
@main def fundscoutServer(args: String*): Unit =
  val port = args.headOption.flatMap(_.toIntOption).getOrElse(8080)
  val registry = Ingestor.run(SampleData.records).registry
  val api = FundScoutApi(registry, LocalDate.now())

  val server = HttpServer.create(new InetSocketAddress(port), 0)
  val _ = server.createContext("/", (exchange: HttpExchange) => respond(exchange, api))
  server.setExecutor(null)
  server.start()

  println(s"FundScout API listening on http://localhost:$port")
  println(s"Try: curl http://localhost:$port/market")

  // The HttpServer runs on background threads; block so the JVM stays alive
  // until the process is interrupted.
  new java.util.concurrent.CountDownLatch(1).await()

private def respond(exchange: HttpExchange, api: FundScoutApi): Unit =
  val response =
    try api.handle(exchange.getRequestMethod, exchange.getRequestURI.getPath)
    catch
      case ex: Throwable =>
        ApiResponse(
          500,
          Json.obj("error" -> Json.str(Option(ex.getMessage).getOrElse("internal error")))
        )
  val bytes = response.render.getBytes(StandardCharsets.UTF_8)
  exchange.getResponseHeaders.set("Content-Type", "application/json")
  exchange.sendResponseHeaders(response.status, bytes.length.toLong)
  val os = exchange.getResponseBody
  try os.write(bytes)
  finally os.close()
