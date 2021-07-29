ThisBuild / scalaVersion := "2.13.6"
ThisBuild / name := (server / name).value

lazy val commonSettings: Seq[Setting[_]] = Seq(
  version := {
    val Tag = "refs/tags/(.*)".r
    sys.env.get("CI_VERSION").collect { case Tag(tag) => tag }
      .getOrElse("0.0.1-SNAPSHOT")
  },

  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
)

lazy val root = project.in(file("."))
  .settings(commonSettings)
  .settings(
    publishArtifact := false
  )
  .aggregate(server)

lazy val common = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core" % "2.6.1",
      "io.monix" %%% "monix" % "3.4.0",
      "io.circe" %%% "circe-core" % "0.14.1",
      "io.circe" %%% "circe-generic" % "0.14.1",
      "io.circe" %%% "circe-parser" % "0.14.1",
      "org.scodec" %%% "scodec-bits" % "1.1.27",
    )
  )

lazy val commonJvm = common.jvm
lazy val commonJs = common.js

lazy val frontend = project
  .enablePlugins(ScalaJSWebjarPlugin)
  .dependsOn(commonJs)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "1.1.0",
      "com.github.japgolly.scalajs-react" %%% "core" % "1.7.7",
      "com.github.japgolly.scalajs-react" %%% "extra" % "1.7.7",
      "com.github.japgolly.scalajs-react" %%% "ext-cats" % "1.7.7",
    ),

    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
    },
    scalaJSUseMainModuleInitializer := true,
  )

lazy val server = project
  .enablePlugins(BuildInfoPlugin)
  .dependsOn(commonJvm, frontend.webjar)
  .settings(commonSettings)
  .settings(
    name := "server-desktop",

    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.2.5",
      "org.http4s" %% "http4s-blaze-server" % "0.22.0",
      "org.http4s" %% "http4s-circe" % "0.22.0",
      "org.http4s" %% "http4s-dsl" % "0.22.0",
      "org.http4s" %% "http4s-scalatags" % "0.22.0",
      "org.http4s" %% "http4s-jdk-http-client" % "0.4.0",
      "org.apache.commons" % "commons-imaging" % "1.0-alpha2",
    ),

    buildInfoKeys := Seq(
      "frontendAsset" -> (frontend / Compile / webjarMainResourceName).value,
      "frontendName" -> (frontend / normalizedName).value,
      "frontendVersion" -> (frontend / version).value,
    ),
  )
