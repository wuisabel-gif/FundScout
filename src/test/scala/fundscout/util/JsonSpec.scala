package fundscout.util

import fundscout.util.Json.*

class JsonSpec extends munit.FunSuite:

  test("renders primitives") {
    assertEquals(str("hi").render, "\"hi\"")
    assertEquals(num(42).render, "42")
    assertEquals(num(BigDecimal("12000000")).render, "12000000")
    assertEquals(bool(true).render, "true")
    assertEquals(JNull.render, "null")
  }

  test("formats doubles without float noise") {
    assertEquals(num(0.15).render, "0.15")
    assertEquals(num(2.0).render, "2")
    assertEquals(num(1.25).render, "1.25")
  }

  test("escapes special characters in strings") {
    assertEquals(str("a\"b").render, "\"a\\\"b\"")
    assertEquals(str("line\nbreak").render, "\"line\\nbreak\"")
    assertEquals(str("tab\tend").render, "\"tab\\tend\"")
    assertEquals(str("ctrl").render, "\"ctrl\\u0001\"")
  }

  test("renders arrays and objects with stable order") {
    val json = obj(
      "name" -> str("Acme"),
      "rounds" -> num(3),
      "tags" -> arr(List(str("ai"), str("usa")))
    )
    assertEquals(
      json.render,
      "{\"name\":\"Acme\",\"rounds\":3,\"tags\":[\"ai\",\"usa\"]}"
    )
  }

  test("opt maps None to null") {
    assertEquals(opt(None).render, "null")
    assertEquals(opt(Some(num(1))).render, "1")
  }
