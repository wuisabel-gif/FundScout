package fundscout.util

/** A small, dependency-free CSV tokenizer (RFC 4180 style).
  *
  * Handles quoted fields, embedded commas and newlines inside quotes, and the
  * `""` escape for a literal quote. It only splits text into rows of string
  * cells — interpreting those cells (headers, types) is the decoder's job.
  */
object Csv:

  /** Split CSV `text` into rows of cells. Blank trailing lines are dropped; a
    * `\r` before `\n` is ignored so both LF and CRLF inputs work.
    */
  def parseRows(text: String): List[List[String]] =
    val rows = scala.collection.mutable.ListBuffer.empty[List[String]]
    val current = scala.collection.mutable.ListBuffer.empty[String]
    val field = new StringBuilder
    var inQuotes = false
    var i = 0

    def emit(c: Char): Unit = field.append(c)
    def endField(): Unit =
      val _ = current += field.toString
      field.setLength(0)
    def endRow(): Unit =
      endField()
      val _ = rows += current.toList
      current.clear()

    while i < text.length do
      val c = text.charAt(i)
      if inQuotes then
        if c == '"' then
          if i + 1 < text.length && text.charAt(i + 1) == '"' then { emit('"'); i += 2 }
          else { inQuotes = false; i += 1 }
        else { emit(c); i += 1 }
      else
        c match
          case '"'  => inQuotes = true; i += 1
          case ','  => endField(); i += 1
          case '\r' => i += 1
          case '\n' => endRow(); i += 1
          case other => emit(other); i += 1

    if field.length > 0 || current.nonEmpty then endRow()
    rows.toList
