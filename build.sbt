ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.5.0-RC4"

ThisBuild / scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Wunused:implicits",
  "-Wunused:explicits",
  "-Wunused:imports",
  "-Wunused:locals",
  "-Wunused:params",
  "-Wunused:privates",
  "-Wvalue-discard",
  "-Xkind-projector",
  "-Ycheck:all", 
)

val testDependencies = Seq(
  "org.scalatest" %% "scalatest" % "3.2.18" % Test,
)

lazy val ecsutil = project
  .settings(
    name := "ecsutil",

    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,

    libraryDependencies ++= testDependencies,
  )

lazy val core = project
  .dependsOn(ecsutil)
  .settings(
    name := "ecscalibur",

    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,

    libraryDependencies ++= testDependencies,

    scalacOptions ++= Seq(
      "-experimental",
      "-language:experimental.macros",
      "-Xcheck-macros", 
    ),

    assembly / assemblyJarName := "ecscalibur.jar",
  )

lazy val demo_util = (project in file("./demo/util"))
  .settings(
    name := "Demo utils"
  )

lazy val demo_ecs = (project in file("./demo/ecs"))
  .dependsOn(core, demo_util)
  .settings(
    name := "ecscalibur Demo",
    assembly / assemblyJarName := "demo_ecs.jar",

    scalacOptions ++= Seq(
      "-experimental"
    ),

    libraryDependencies ++= testDependencies,
  )

lazy val demo_oop = (project in file("./demo/oop"))
  .dependsOn(ecsutil, demo_util)
  .settings(
    name := "OOP Demo",
    assembly / assemblyJarName := "demo_oop.jar",
  )

lazy val benchmark = project
  .dependsOn(demo_ecs, demo_oop)
  .enablePlugins(JmhPlugin)
  .settings(
    name := "Benchmark",
    scalacOptions ++= Seq(
      "-experimental"
    )
  )
