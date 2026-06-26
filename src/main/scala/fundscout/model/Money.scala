package fundscout.model

/** A non-negative amount of money in a single currency.
  *
  * FundScout deals in funding amounts and valuations that are always
  * non-negative, so the constructor enforces that invariant. Amounts are stored
  * as [[BigDecimal]] to avoid binary floating-point rounding error on monetary
  * values.
  *
  * Use the [[Money.usd]] / [[Money.apply]] factories rather than the raw
  * constructor; they validate the amount.
  */
final case class Money private (amount: BigDecimal, currency: Currency):

  /** Add two amounts of the same currency. */
  def +(that: Money): Money =
    require(
      currency == that.currency,
      s"cannot add $currency and ${that.currency}"
    )
    Money(amount + that.amount, currency)

  /** Scale an amount by a non-negative factor (e.g. a valuation multiple). */
  def *(factor: BigDecimal): Money =
    require(factor >= 0, s"factor must be non-negative, was $factor")
    Money(amount * factor, currency)

  def isZero: Boolean = amount == 0

  /** Human-readable form, abbreviated for large amounts: `$1.5M`, `$240M`,
    * `$1.2B`. Intended for reports and explanations, not for serialization.
    */
  def display: String =
    val sym = currency.symbol
    val abs = amount.abs
    def fmt(v: BigDecimal, suffix: String): String =
      val rounded = v.setScale(1, BigDecimal.RoundingMode.HALF_UP)
      val trimmed =
        if rounded.isWhole then rounded.toBigInt.toString
        else rounded.bigDecimal.stripTrailingZeros.toPlainString
      s"$sym$trimmed$suffix"
    if abs >= 1_000_000_000 then fmt(amount / 1_000_000_000, "B")
    else if abs >= 1_000_000 then fmt(amount / 1_000_000, "M")
    else if abs >= 1_000 then fmt(amount / 1_000, "K")
    else s"$sym${amount.bigDecimal.stripTrailingZeros.toPlainString}"

object Money:

  def apply(amount: BigDecimal, currency: Currency): Money =
    require(amount >= 0, s"money amount must be non-negative, was $amount")
    new Money(amount, currency)

  /** Convenience for USD, FundScout's default reporting currency. */
  def usd(amount: BigDecimal): Money = apply(amount, Currency.USD)

  def zero(currency: Currency): Money = new Money(0, currency)

  given Ordering[Money] = Ordering.by(_.amount)

/** Supported reporting currencies. USD is the platform default. */
enum Currency(val symbol: String):
  case USD extends Currency("$")
  case EUR extends Currency("€")
  case GBP extends Currency("£")

object Currency:
  /** Look up a currency by ISO code (case-insensitive) or symbol; `None` if
    * unrecognized.
    */
  def fromCode(raw: String): Option[Currency] =
    val normalized = raw.trim.toUpperCase
    values.find(c => c.toString == normalized || c.symbol == raw.trim)
