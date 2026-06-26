package fundscout

import fundscout.ingest.{RawFundingRecord, RawInvestor}

/** A small, illustrative dataset of funding announcements shared by the runnable
  * demo and the API server, so both tell the same story.
  *
  * Four companies: two that grow into unicorns (Acme, Hooli), one healthy
  * European startup (Globex), and one that gets acquired (Initech).
  */
object SampleData:

  private val sequoia = RawInvestor("sequoia", "Sequoia", Some("Tier1"))
  private val a16z = RawInvestor("a16z", "Andreessen Horowitz", Some("Tier1"))
  private val yc = RawInvestor("yc", "Y Combinator", Some("Tier2"))
  private val angel = RawInvestor("jane", "Jane Doe")

  val records: List[RawFundingRecord] = List(
    // Acme — AI, San Francisco; grows into a unicorn.
    raw("acme-1", "acme", "Acme", "Seed", "2,000,000", "2021-02-01", "AI", "USA",
      Some("San Francisco"), List(yc, angel), Some("yc")),
    raw("acme-2", "acme", "Acme", "Series A", "12,000,000", "2022-09-01", "AI",
      "USA", Some("San Francisco"), List(sequoia), Some("sequoia"),
      valuation = Some("60000000")),
    raw("acme-3", "acme", "Acme", "Series B", "40,000,000", "2024-05-01", "AI",
      "USA", Some("San Francisco"), List(a16z, sequoia), Some("a16z"),
      valuation = Some("1200000000")),

    // Globex — Fintech, London.
    raw("globex-1", "globex", "Globex", "Seed", "1,500,000", "2021-06-01",
      "Fintech", "UK", Some("London"), List(yc), Some("yc")),
    raw("globex-2", "globex", "Globex", "Series A", "9,000,000", "2023-03-01",
      "Fintech", "UK", Some("London"), List(sequoia), Some("sequoia")),

    // Initech — SaaS, Austin; acquired.
    raw("initech-1", "initech", "Initech", "Pre-Seed", "500,000", "2022-01-15",
      "SaaS", "USA", Some("Austin"), List(angel), None),
    raw("initech-2", "initech", "Initech", "Seed", "3,000,000", "2023-04-01",
      "SaaS", "USA", Some("Austin"), List(yc), Some("yc")),
    raw("initech-3", "initech", "Initech", "Acquisition", "300,000,000",
      "2025-02-01", "SaaS", "USA", Some("Austin"), Nil, None),

    // Hooli — AI, San Francisco; a second unicorn.
    raw("hooli-1", "hooli", "Hooli", "Series A", "20,000,000", "2023-07-01",
      "AI", "USA", Some("San Francisco"), List(a16z), Some("a16z")),
    raw("hooli-2", "hooli", "Hooli", "Series B", "60,000,000", "2024-11-01",
      "AI", "USA", Some("San Francisco"), List(a16z, sequoia), Some("a16z"),
      valuation = Some("1500000000"))
  )

  private def raw(
      id: String,
      companyId: String,
      name: String,
      stage: String,
      amount: String,
      date: String,
      sector: String,
      country: String,
      city: Option[String],
      investors: List[RawInvestor],
      lead: Option[String],
      valuation: Option[String] = None
  ): RawFundingRecord =
    RawFundingRecord(
      id = id,
      companyId = companyId,
      companyName = name,
      stage = stage,
      amount = amount,
      date = date,
      sector = sector,
      country = country,
      city = city,
      investors = investors,
      leadInvestorId = lead,
      postMoneyValuation = valuation
    )
