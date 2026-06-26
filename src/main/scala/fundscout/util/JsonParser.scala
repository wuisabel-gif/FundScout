package fundscout.util

import fundscout.util.Json.*

import scala.util.Try

/** A small recursive-descent JSON parser producing the [[Json]] AST.
  *
  * It is the read counterpart to [[Json]]'s writer, keeping FundScout's JSON
  * support fully self-contained (no external library). On failure it returns a
  * `Left` with a message and the offending position rather than throwing.
  */
object JsonParser:

  def parse(input: String): Either[String, Json] =
    val parser = new Parser(input)
    parser.parseValue().flatMap { value =>
      parser.skipWhitespace()
      if parser.atEnd then Right(value)
      else Left(s"unexpected trailing content at position ${parser.position}")
    }

  private final class Parser(s: String):
    private var pos = 0

    def position: Int = pos
    def atEnd: Boolean = pos >= s.length
    private def peek: Char = s.charAt(pos)

    def skipWhitespace(): Unit =
      while pos < s.length && s.charAt(pos).isWhitespace do pos += 1

    def parseValue(): Either[String, Json] =
      skipWhitespace()
      if atEnd then Left("unexpected end of input")
      else
        peek match
          case '{'              => parseObject()
          case '['              => parseArray()
          case '"'              => parseString().map(JString.apply)
          case 't' | 'f'        => parseBoolean()
          case 'n'              => parseNull()
          case c if c == '-' || c.isDigit => parseNumber()
          case c                => Left(s"unexpected character '$c' at position $pos")

    private def parseObject(): Either[String, Json] =
      pos += 1 // consume '{'
      skipWhitespace()
      if !atEnd && peek == '}' then { pos += 1; Right(JObject(Nil)) }
      else parseMembers(Nil).map(fields => JObject(fields.reverse))

    private def parseMembers(
        acc: List[(String, Json)]
    ): Either[String, List[(String, Json)]] =
      skipWhitespace()
      if atEnd || peek != '"' then Left(s"expected string key at position $pos")
      else
        parseString().flatMap { key =>
          skipWhitespace()
          if atEnd || peek != ':' then Left(s"expected ':' at position $pos")
          else
            pos += 1
            parseValue().flatMap { value =>
              skipWhitespace()
              if atEnd then Left("unterminated object")
              else
                peek match
                  case ',' => pos += 1; parseMembers((key -> value) :: acc)
                  case '}' => pos += 1; Right((key -> value) :: acc)
                  case c   => Left(s"expected ',' or '}' at position $pos, got '$c'")
            }
        }

    private def parseArray(): Either[String, Json] =
      pos += 1 // consume '['
      skipWhitespace()
      if !atEnd && peek == ']' then { pos += 1; Right(JArray(Nil)) }
      else parseElements(Nil).map(items => JArray(items.reverse))

    private def parseElements(acc: List[Json]): Either[String, List[Json]] =
      parseValue().flatMap { value =>
        skipWhitespace()
        if atEnd then Left("unterminated array")
        else
          peek match
            case ',' => pos += 1; parseElements(value :: acc)
            case ']' => pos += 1; Right(value :: acc)
            case c   => Left(s"expected ',' or ']' at position $pos, got '$c'")
      }

    private def parseString(): Either[String, String] =
      pos += 1 // consume opening quote
      val sb = new StringBuilder
      def emit(c: Char): Unit = sb.append(c)
      var result: Option[Either[String, String]] = None
      while result.isEmpty do
        if atEnd then result = Some(Left("unterminated string"))
        else
          val c = s.charAt(pos)
          pos += 1
          if c == '"' then result = Some(Right(sb.toString))
          else if c == '\\' then
            if atEnd then result = Some(Left("unterminated escape"))
            else
              val e = s.charAt(pos)
              pos += 1
              e match
                case '"'  => emit('"')
                case '\\' => emit('\\')
                case '/'  => emit('/')
                case 'n'  => emit('\n')
                case 't'  => emit('\t')
                case 'r'  => emit('\r')
                case 'b'  => emit('\b')
                case 'f'  => emit('\f')
                case 'u'  =>
                  if pos + 4 <= s.length then
                    val hex = s.substring(pos, pos + 4)
                    pos += 4
                    Try(Integer.parseInt(hex, 16)).toOption match
                      case Some(code) => emit(code.toChar)
                      case None       => result = Some(Left(s"invalid unicode escape '$hex'"))
                  else result = Some(Left("truncated unicode escape"))
                case other => result = Some(Left(s"invalid escape '\\$other'"))
          else emit(c)
      result.get

    private def parseNumber(): Either[String, Json] =
      val start = pos
      if !atEnd && peek == '-' then pos += 1
      while !atEnd && peek.isDigit do pos += 1
      if !atEnd && peek == '.' then
        pos += 1
        while !atEnd && peek.isDigit do pos += 1
      if !atEnd && (peek == 'e' || peek == 'E') then
        pos += 1
        if !atEnd && (peek == '+' || peek == '-') then pos += 1
        while !atEnd && peek.isDigit do pos += 1
      val literal = s.substring(start, pos)
      Try(BigDecimal(literal)).toOption match
        case Some(_) => Right(JNumber(literal))
        case None    => Left(s"invalid number '$literal' at position $start")

    private def parseBoolean(): Either[String, Json] =
      if s.startsWith("true", pos) then { pos += 4; Right(JBool(true)) }
      else if s.startsWith("false", pos) then { pos += 5; Right(JBool(false)) }
      else Left(s"invalid literal at position $pos")

    private def parseNull(): Either[String, Json] =
      if s.startsWith("null", pos) then { pos += 4; Right(JNull) }
      else Left(s"invalid literal at position $pos")
