lazy val avroVersion = "1.9.1"

lazy val commonSettings = Seq(
  organization := "com.julianpeeters",
  version := "1.0.0-RC26-SNAPSHOT",
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
  Test / scalacOptions ++= Seq("-Yrangepos"),
  scalaVersion := "2.13.8",
  crossScalaVersions := Seq("2.12.15", scalaVersion.value),
  libraryDependencies += "org.apache.avro" % "avro" % avroVersion,
  libraryDependencies += "org.apache.avro" % "avro-compiler" % avroVersion,
  libraryDependencies := { CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, scalaMinor)) if scalaMinor < 13 =>
      // for implementing SpecificRecord from standard case class definitions
      libraryDependencies.value ++ Seq(compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full))
    case _ =>
      // Scala 2.13 has it built-in
      libraryDependencies.value
  }},
  libraryDependencies := { CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, scalaMinor)) if scalaMinor < 13 =>
      libraryDependencies.value ++ Seq("org.scala-lang.modules" %% "scala-collection-compat" % "2.6.0")
    case _ =>
      libraryDependencies.value
  }},
  libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value,
  libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  // for testing
  libraryDependencies += "org.specs2" %% "specs2-core" % "4.8.0" % "test",
  publishMavenStyle := true,
  Test / publishArtifact := false,
  publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
  ),
  pomIncludeRepository := { _ => false },
  licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  homepage := Some(url("https://github.com/julianpeeters/avrohugger")),
  pomExtra := (
    <scm>
      <url>git://github.com/julianpeeters/avrohugger.git</url>
      <connection>scm:git://github.com/julianpeeters/avrohugger.git</connection>
    </scm>
    <developers>
      <developer>
        <id>julianpeeters</id>
        <name>Julian Peeters</name>
        <url>http://github.com/julianpeeters</url>
      </developer>
    </developers>)
)

lazy val avrohugger = (project in file("."))
  .settings(
    commonSettings,
  ).aggregate(`avrohugger-core`, `avrohugger-filesorter`, `avrohugger-tools`)


lazy val `avrohugger-core` = (project in file("avrohugger-core"))
  .settings(
    commonSettings,
    libraryDependencies += "com.eed3si9n" %% "treehugger" % "0.4.4",
    libraryDependencies += "org.apache.avro" % "avro-tools" % avroVersion,
    Compile / sourceGenerators += addScalaVersionFile.taskValue
  )

lazy val `avrohugger-filesorter` = (project in file("avrohugger-filesorter"))
  .settings(
    commonSettings,
    libraryDependencies += "io.spray" %% "spray-json" % "1.3.6"
  )

lazy val `avrohugger-tools` = (project in file("avrohugger-tools"))
  .settings(
    commonSettings,
    artifact in (Compile, assembly) := {
      val art: Artifact = (artifact in (Compile, assembly)).value
      art.withClassifier(Some("assembly"))
    },
    addArtifact(artifact in (Compile, assembly), assembly).settings,
    Test / sourceGenerators += addScalaVersionFile.taskValue
  ).dependsOn(`avrohugger-core`, `avrohugger-filesorter`)

lazy val addScalaVersionFile = Def.task {
  val partialScalaVersion =
    CrossVersion.partialVersion(scalaVersion.value)
      .map(v => v._1 + "." + v._2)

  val pv = partialScalaVersion.get
  val content =
    s"""package avrohugger
       |package internal
       |
       |object ScalaVersion {
       |  val version = "$pv"
       |}""".stripMargin

  val file = (Test / sourceManaged).value / "avrohugger" / "internal" / "ScalaVersion.scala"
  IO.write(file, content)

  Seq(file)
}