package fundscout.model

/** The market sector a company operates in.
  *
  * A closed enum keeps sector analytics consistent across the platform. Inputs
  * that don't map to a known sector become [[Sector.Other]] rather than silently
  * inventing new categories; use [[Sector.fromLabel]] to parse free-text input.
  */
enum Sector(val label: String):
  case AI         extends Sector("Artificial Intelligence")
  case Fintech    extends Sector("Fintech")
  case Healthtech extends Sector("Healthtech")
  case Biotech    extends Sector("Biotech")
  case SaaS       extends Sector("SaaS")
  case Ecommerce  extends Sector("E-commerce")
  case Climate    extends Sector("Climate")
  case Crypto     extends Sector("Crypto")
  case Gaming     extends Sector("Gaming")
  case Hardware   extends Sector("Hardware")
  case Robotics   extends Sector("Robotics")
  case Other      extends Sector("Other")

object Sector:

  /** Case-insensitive lookup by enum name or display label; unknown input maps
    * to [[Other]].
    */
  def fromLabel(raw: String): Sector =
    val normalized = raw.trim.toLowerCase
    values
      .find { s =>
        s.toString.toLowerCase == normalized || s.label.toLowerCase == normalized
      }
      .getOrElse(Other)
