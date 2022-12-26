import Dependencies._

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.8"
ThisBuild / organization := "dev.profunktor"
ThisBuild / organizationName := "Profunktor"

ThisBuild / evictionErrorLevel := Level.Warn
ThisBuild / scalafixDependencies += Libraries.organizeImports

val scalafixCommonSettings = inConfig(IntegrationTest)(scalafixConfigSettings(IntegrationTest))

lazy val root = (project in file("."))
  .settings(
    name := "DenPracticalFPScala"
  )
  .aggregate(core, tests)

lazy val tests = (project in file("modules/tests"))
  .settings(
    name := "shopping-cart-test-suite",
    scalacOptions ++= List("-Ymacro-annotations", "-Yrangepos", "-Wconf:cat=unused:info"),
    testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
    Defaults.itSettings,
    scalafixCommonSettings,
    libraryDependencies ++= Seq(
      CompilerPlugin.kindProjector,
      CompilerPlugin.betterMonadicFor,
      CompilerPlugin.semanticDB,
    ),
    libraryDependencies ++= testingDependencies
  )
  .dependsOn(core)

lazy val core = (project in file("modules/core"))
  .enablePlugins(DockerPlugin)
  .enablePlugins(AshScriptPlugin)
  .settings(
    name := "shopping-cart-core",
    Docker / packageName := "shopping-cart",
    scalacOptions ++= List("-Ymacro-annotations", "-Yrangepos", "-Wconf:cat=unused:info"),
    scalafmtOnCompile := true,
    resolvers ++= Resolver.sonatypeOssRepos("snapshots"),
    Defaults.itSettings,
    scalafixCommonSettings,
    dockerBaseImage := "openjdk:11-jre-slim-buster",
    dockerExposedPorts ++= Seq(8080),
    makeBatScripts := Seq(),
    dockerUpdateLatest := true,
    libraryDependencies ++= Seq(
      CompilerPlugin.kindProjector,
      CompilerPlugin.betterMonadicFor,
      CompilerPlugin.semanticDB,
    ),
    libraryDependencies ++= mainDependencies
  )

addCommandAlias("runLinter", ";scalafixAll --rules OrganizeImports")
