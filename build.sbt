ThisBuild / scalaVersion := "3.3.4"
ThisBuild / organization := "com.fundscout"
ThisBuild / version      := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(
    name := "fundscout",
    // `sbt run` launches the console demo; the HTTP server runs via
    // `sbt "runMain fundscout.fundscoutServer"`.
    Compile / mainClass := Some("fundscout.fundscoutDemo"),
    // Educational project: prioritize readability and safety over cleverness.
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-Wunused:all",
      "-Wvalue-discard",
      "-Xfatal-warnings"
    ),
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "1.0.2" % Test
    )
  )
