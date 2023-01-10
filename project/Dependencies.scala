import sbt._

object Dependencies {

  val testingDependencies: Seq[ModuleID] = Seq(
    Libraries.catsLaws,
    Libraries.log4catsNoOp,
    Libraries.monocleLaw,
    Libraries.refinedScalacheck,
    Libraries.weaverCats,
    Libraries.weaverDiscipline,
    Libraries.weaverScalaCheck
  )

  val mainDependencies: Seq[sbt.ModuleID] = Seq(
    Libraries.bcrypt,
    Libraries.cats,
    Libraries.catsEffect,
    Libraries.catsRetry,
    Libraries.circeCore,
    Libraries.circeGeneric,
    Libraries.circeParser,
    Libraries.circeRefined,
    Libraries.cirisCore,
    Libraries.cirisEnum,
    Libraries.cirisRefined,
    Libraries.derevoCore,
    Libraries.derevoCats,
    Libraries.derevoCirce,
    Libraries.fs2,
    Libraries.http4sDsl,
    Libraries.http4sServer,
    Libraries.http4sClient,
    Libraries.http4sCirce,
    Libraries.http4sJwtAuth,
    Libraries.log4cats,
    Libraries.logback % Runtime,
    Libraries.monocleCore,
    Libraries.newtype,
    Libraries.redis4catsEffects,
    Libraries.redis4catsLog4cats,
    Libraries.refinedCore,
    Libraries.refinedCats,
    Libraries.skunkCore,
    Libraries.skunkCirce,
    Libraries.squants
  )

  object V {
    val bcrypt        = "4.3.0"
    val cats          = "2.9.0"
    val catsEffect    = "3.4.4"
    val catsRetry     = "3.1.0"
    val circe         = "0.14.2"
    val ciris         = "2.3.2"
    val derevo        = "0.13.0"
    val fs2           = "3.4.0"
    val http4s        = "0.23.1"
    val http4sJwtAuth = "1.0.0"
    val log4cats      = "2.5.0"
    val monocle       = "3.1.0"
    val newtype       = "0.4.4"
    val refined       = "0.10.1"
    val redis4cats    = "1.3.0"
    val skunk         = "0.3.2"
    val squants       = "1.8.3"

    val betterMonadicFor = "0.3.1"
    val kindProjector    = "0.13.2"
    val logback          = "1.4.5"
    val organizeImports  = "0.6.0"
    val semanticDB       = "4.5.8"

    val weaver = "0.8.1"
  }

  object Libraries {
    def circe(artifact: String): ModuleID  = "io.circe"   %% s"circe-$artifact"  % V.circe
    def ciris(artifact: String): ModuleID  = "is.cir"     %% artifact            % V.ciris
    def derevo(artifact: String): ModuleID = "tf.tofu"    %% s"derevo-$artifact" % V.derevo
    def http4s(artifact: String): ModuleID = "org.http4s" %% s"http4s-$artifact" % V.http4s

    lazy val bcrypt     = "com.github.t3hnar" %% "scala-bcrypt" % V.bcrypt
    lazy val cats       = "org.typelevel"     %% "cats-core"    % V.cats
    lazy val catsEffect = "org.typelevel"     %% "cats-effect"  % V.catsEffect
    lazy val catsRetry  = "com.github.cb372"  %% "cats-retry"   % V.catsRetry
    lazy val squants    = "org.typelevel"     %% "squants"      % V.squants
    lazy val fs2        = "co.fs2"            %% "fs2-core"     % V.fs2

    lazy val circeCore    = circe("core")
    lazy val circeGeneric = circe("generic")
    lazy val circeParser  = circe("parser")
    lazy val circeRefined = circe("refined")

    lazy val cirisCore    = ciris("ciris")
    lazy val cirisEnum    = ciris("ciris-enumeratum")
    lazy val cirisRefined = ciris("ciris-refined")

    lazy val derevoCore  = derevo("core")
    lazy val derevoCats  = derevo("cats")
    lazy val derevoCirce = derevo("circe-magnolia")

    lazy val http4sDsl    = http4s("dsl")
    lazy val http4sServer = http4s("ember-server")
    lazy val http4sClient = http4s("ember-client")
    lazy val http4sCirce  = http4s("circe")

    lazy val http4sJwtAuth = "dev.profunktor" %% "http4s-jwt-auth" % V.http4sJwtAuth

    lazy val monocleCore = "dev.optics" %% "monocle-core" % V.monocle

    lazy val refinedCore = "eu.timepit" %% "refined"      % V.refined
    lazy val refinedCats = "eu.timepit" %% "refined-cats" % V.refined

    lazy val log4cats = "org.typelevel" %% "log4cats-slf4j" % V.log4cats
    lazy val newtype  = "io.estatico"   %% "newtype"        % V.newtype

    lazy val redis4catsEffects  = "dev.profunktor" %% "redis4cats-effects"  % V.redis4cats
    lazy val redis4catsLog4cats = "dev.profunktor" %% "redis4cats-log4cats" % V.redis4cats

    lazy val skunkCore  = "org.tpolecat" %% "skunk-core"  % V.skunk
    lazy val skunkCirce = "org.tpolecat" %% "skunk-circe" % V.skunk

    lazy val logback = "ch.qos.logback" % "logback-classic" % V.logback

    // Test
    lazy val catsLaws          = "org.typelevel"       %% "cats-laws"          % V.cats
    lazy val log4catsNoOp      = "org.typelevel"       %% "log4cats-noop"      % V.log4cats
    lazy val monocleLaw        = "dev.optics"          %% "monocle-law"        % V.monocle
    lazy val refinedScalacheck = "eu.timepit"          %% "refined-scalacheck" % V.refined
    lazy val weaverCats        = "com.disneystreaming" %% "weaver-cats"        % V.weaver
    lazy val weaverDiscipline  = "com.disneystreaming" %% "weaver-discipline"  % V.weaver
    lazy val weaverScalaCheck  = "com.disneystreaming" %% "weaver-scalacheck"  % V.weaver

    lazy val organizeImports = "com.github.liancheng" %% "organize-imports" % V.organizeImports
  }

  object CompilerPlugin {
    lazy val betterMonadicFor = compilerPlugin(
      "com.olegpy" %% "better-monadic-for" % V.betterMonadicFor
    )
    lazy val kindProjector = compilerPlugin(
      "org.typelevel" % "kind-projector" % V.kindProjector cross CrossVersion.full
    )
    lazy val semanticDB = compilerPlugin(
      "org.scalameta" % "semanticdb-scalac" % V.semanticDB cross CrossVersion.full
    )
  }

}
