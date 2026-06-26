package fundscout.reporting

import fundscout.scoring.{Polarity, StartupScore}

/** Renders an explainable [[fundscout.scoring.StartupScore]] as plain text, in
  * the "score + reasons" shape.
  */
object ScoreReportRenderer:

  /** Render the score for a named company, including the reasons that drove it
    * and the component breakdown.
    */
  def render(companyName: String, score: StartupScore): String =
    val lines = scala.collection.mutable.ArrayBuffer.empty[String]
    def add(line: String): Unit = lines += line

    add(s"$companyName — Startup Score: ${score.overall}/100")

    val drivers = score.reasons.filter(_.polarity != Polarity.Neutral)
    if drivers.nonEmpty then
      add("")
      add("Reasons:")
      drivers.foreach(r => add(s"  ${r.polarity.sign} ${r.description}"))

    add("")
    add("Components:")
    score.components.foreach { c =>
      add(f"  ${c.component.label}%-20s ${c.value}%3d")
    }

    lines.mkString("\n")
