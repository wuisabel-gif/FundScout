package fundscout.util

import fundscout.util.Json.*

class JsonParserSpec extends munit.FunSuite:

  test("parses primitives") {
    assertEquals(JsonParser.parse("true"), Right(JBool(true)))
    assertEquals(JsonParser.parse("false"), Right(JBool(false)))
    assertEquals(JsonParser.parse("null"), Right(JNull))
    assertEquals(JsonParser.parse("42"), Right(JNumber("42")))
    assertEquals(JsonParser.parse("-3.14"), Right(JNumber("-3.14")))
    assertEquals(JsonParser.parse("\"hi\""), Right(JString("hi")))
  }

  test("parses objects and arrays with whitespace") {
    val parsed = JsonParser.parse("""  { "a" : 1, "b" : [true, "x"] }  """)
    assertEquals(
      parsed,
      Right(JObject(List(
        "a" -> JNumber("1"),
        "b" -> JArray(List(JBool(true), JString("x")))
      )))
    )
  }

  test("handles escapes and unicode") {
    assertEquals(JsonParser.parse("\"a\\nb\""), Right(JString("a\nb")))
    assertEquals(JsonParser.parse("\"quote: \\\"\""), Right(JString("quote: \"")))
    assertEquals(JsonParser.parse("\"\\u0041\""), Right(JString("A")))
  }

  test("parses empty containers") {
    assertEquals(JsonParser.parse("{}"), Right(JObject(Nil)))
    assertEquals(JsonParser.parse("[]"), Right(JArray(Nil)))
  }

  test("round-trips with the writer") {
    val value = obj(
      "name" -> str("Acme \"Inc\""),
      "rounds" -> num(3),
      "raised" -> num(BigDecimal("12000000")),
      "tags" -> arr(List(str("ai"), str("usa"))),
      "lead" -> JNull
    )
    assertEquals(JsonParser.parse(value.render), Right(value))
  }

  test("reports errors instead of throwing") {
    assert(JsonParser.parse("{ \"a\": }").isLeft)
    assert(JsonParser.parse("[1, 2").isLeft)
    assert(JsonParser.parse("nope").isLeft)
    assert(JsonParser.parse("1 2").isLeft) // trailing content
  }
