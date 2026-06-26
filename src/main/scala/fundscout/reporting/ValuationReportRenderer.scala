package fundscout.reporting

import fundscout.valuation.ValuationEstimate

/** Renders a [[fundscout.valuation.ValuationEstimate]] as plain text, in the
  * "range + confidence + reasoning" shape.
  */
object ValuationReportRenderer:

  def render(companyName: String, estimate: ValuationEstimate): String =
    val lines = scala.collection.mutable.ArrayBuffer.empty[String]
    def add(line: String): Unit = lines += line

    add(s"$companyName — Estimated valuation")
    add(s"${estimate.low.display}–${estimate.high.display}")
    add(s"Confidence: ${estimate.confidence}")
    if estimate.reasoning.nonEmpty then
      add("")
      add("Reasoning:")
      estimate.reasoning.foreach(r => add(s"  • $r"))

    lines.mkString("\n")
