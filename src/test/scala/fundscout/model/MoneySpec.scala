package fundscout.model

class MoneySpec extends munit.FunSuite:

  test("rejects negative amounts") {
    interceptMessage[IllegalArgumentException](
      "requirement failed: money amount must be non-negative, was -1"
    )(Money.usd(-1))
  }

  test("adds amounts of the same currency") {
    assertEquals(Money.usd(10) + Money.usd(5), Money.usd(15))
  }

  test("refuses to add mismatched currencies") {
    intercept[IllegalArgumentException](
      Money.usd(10) + Money(5, Currency.EUR)
    )
  }

  test("scales by a non-negative factor") {
    assertEquals(Money.usd(100) * BigDecimal(1.5), Money.usd(150))
  }

  test("display abbreviates magnitudes") {
    assertEquals(Money.usd(500).display, "$500")
    assertEquals(Money.usd(1_500).display, "$1.5K")
    assertEquals(Money.usd(2_000_000).display, "$2M")
    assertEquals(Money.usd(180_000_000).display, "$180M")
    assertEquals(Money.usd(1_200_000_000).display, "$1.2B")
  }

  test("ordering compares by amount") {
    val sorted = List(Money.usd(30), Money.usd(10), Money.usd(20)).sorted
    assertEquals(sorted, List(Money.usd(10), Money.usd(20), Money.usd(30)))
  }
