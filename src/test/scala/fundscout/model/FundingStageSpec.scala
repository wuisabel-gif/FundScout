package fundscout.model

class FundingStageSpec extends munit.FunSuite:

  test("priced rounds expose a progression rank in ladder order") {
    val ladder = List(
      FundingStage.PreSeed,
      FundingStage.Angel,
      FundingStage.Seed,
      FundingStage.SeriesA,
      FundingStage.SeriesB,
      FundingStage.SeriesCPlus
    )
    val ranks = ladder.flatMap(_.progressionRank)
    assertEquals(ranks, ranks.sorted)
    assertEquals(ranks, List(0, 1, 2, 3, 4, 5))
  }

  test("bridge, debt, and exits have no progression rank") {
    assertEquals(FundingStage.Bridge.progressionRank, None)
    assertEquals(FundingStage.VentureDebt.progressionRank, None)
    assertEquals(FundingStage.IPO.progressionRank, None)
    assertEquals(FundingStage.Acquisition.progressionRank, None)
  }

  test("exits are flagged") {
    assert(FundingStage.IPO.isExit)
    assert(FundingStage.Acquisition.isExit)
    assert(!FundingStage.SeriesA.isExit)
  }

  test("acquisitions do not count as capital raised") {
    assert(!FundingStage.Acquisition.raisesCapital)
    assert(FundingStage.IPO.raisesCapital)
    assert(FundingStage.SeriesB.raisesCapital)
  }

  test("ordering runs priced rounds, then interim, then exits") {
    val shuffled = List(
      FundingStage.Acquisition,
      FundingStage.SeriesA,
      FundingStage.VentureDebt,
      FundingStage.PreSeed,
      FundingStage.Bridge,
      FundingStage.IPO
    )
    assertEquals(
      shuffled.sorted,
      List(
        FundingStage.PreSeed,
        FundingStage.SeriesA,
        FundingStage.Bridge,
        FundingStage.VentureDebt,
        FundingStage.Acquisition,
        FundingStage.IPO
      )
    )
  }
