package fundscout.util

/** A minimal, dependency-free JSON model and writer.
  *
  * FundScout deliberately hand-rolls a tiny JSON encoder rather than pulling in
  * a serialization library: the API only ever *produces* JSON from values it
  * controls, so a small, well-tested writer keeps the build self-contained and
  * the behaviour fully transparent. Numbers are stored as their already-rendered
  * textual form so formatting is explicit and never subject to float surprises.
  */
sealed trait Json:
  def render: String = Json.render(this)

object Json:
  case object JNull extends Json
  final case class JBool(value: Boolean) extends Json
  final case class JNumber(literal: String) extends Json
  final case class JString(value: String) extends Json
  final case class JArray(items: List[Json]) extends Json
  final case class JObject(fields: List[(String, Json)]) extends Json

  // Constructors ------------------------------------------------------------

  def str(s: String): Json = JString(s)
  def bool(b: Boolean): Json = JBool(b)
  def num(n: Int): Json = JNumber(n.toString)
  def num(n: Long): Json = JNumber(n.toString)
  def num(n: BigDecimal): Json =
    JNumber(n.bigDecimal.stripTrailingZeros.toPlainString)
  def num(n: Double): Json =
    if n.isWhole then JNumber(n.toLong.toString)
    else
      // Round to a sane precision and strip trailing zeros so accumulated
      // floating-point noise (e.g. 0.15000000000000002) never reaches output.
      val stripped = BigDecimal(n)
        .setScale(6, BigDecimal.RoundingMode.HALF_UP)
        .bigDecimal
        .stripTrailingZeros
      JNumber(stripped.toPlainString)
  def arr(items: Iterable[Json]): Json = JArray(items.toList)
  def obj(fields: (String, Json)*): Json = JObject(fields.toList)

  /** Convenience for an optional field: `None` becomes JSON `null`. */
  def opt(value: Option[Json]): Json = value.getOrElse(JNull)

  // Rendering ---------------------------------------------------------------

  def render(json: Json): String = json match
    case JNull          => "null"
    case JBool(b)       => b.toString
    case JNumber(lit)   => lit
    case JString(s)     => quote(s)
    case JArray(items)  => items.map(render).mkString("[", ",", "]")
    case JObject(fields) =>
      fields.map((k, v) => s"${quote(k)}:${render(v)}").mkString("{", ",", "}")

  private def quote(s: String): String =
    "\"" + s.flatMap(escapeChar) + "\""

  private def escapeChar(c: Char): String = c match
    case '"'           => "\\\""
    case '\\'          => "\\\\"
    case '\n'          => "\\n"
    case '\r'          => "\\r"
    case '\t'          => "\\t"
    case '\b'          => "\\b"
    case '\f'          => "\\f"
    case c if c < ' '  => f"\\u${c.toInt}%04x"
    case c             => c.toString
