package fundscout.ingest

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.time.Duration

/** A document fetched over HTTP: its body text and, if reported, content type. */
final case class FetchedDocument(contentType: Option[String], body: String)

/** Fetches raw funding data from a URL over HTTP, so reports can be generated
  * from online sources rather than only local files.
  *
  * Built on the JDK's `java.net.http.HttpClient` (no external dependency). It is
  * the one genuinely side-effecting piece of ingestion — the decoders and the
  * report facade it feeds remain pure — and it never throws: network and HTTP
  * failures come back as `Left`.
  */
object Http:

  def fetch(url: String): Either[String, FetchedDocument] =
    try
      val client = HttpClient
        .newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()
      val request = HttpRequest
        .newBuilder()
        .uri(URI.create(url))
        .timeout(Duration.ofSeconds(30))
        .header("Accept", "text/csv, application/json, text/plain")
        .GET()
        .build()
      val response = client.send(request, HttpResponse.BodyHandlers.ofString())
      val status = response.statusCode()
      if status >= 200 && status < 300 then
        val contentType =
          Option(response.headers().firstValue("content-type").orElse(null))
        Right(FetchedDocument(contentType, response.body()))
      else Left(s"HTTP $status from $url")
    catch
      case ex: Throwable =>
        Left(s"fetch failed: ${Option(ex.getMessage).getOrElse(ex.getClass.getName)}")
