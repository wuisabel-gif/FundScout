package fundscout.model

class SectorSpec extends munit.FunSuite:

  test("parses by enum name, case-insensitively") {
    assertEquals(Sector.fromLabel("ai"), Sector.AI)
    assertEquals(Sector.fromLabel("Fintech"), Sector.Fintech)
  }

  test("parses by display label") {
    assertEquals(Sector.fromLabel("Artificial Intelligence"), Sector.AI)
    assertEquals(Sector.fromLabel("e-commerce"), Sector.Ecommerce)
  }

  test("unknown input falls back to Other") {
    assertEquals(Sector.fromLabel("quantum teleportation"), Sector.Other)
  }
