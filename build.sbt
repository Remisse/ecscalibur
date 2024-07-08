ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.5.0-RC2"

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

val runtimeDependencies = Seq(
  "com.google.guava" % "guava" % "33.2.1-jre",
)

val testDependencies = Seq(
  "org.scalatest" %% "scalatest" % "3.2.18" % Test,
)

lazy val ecsutil = project
  .settings(
    name := "ecsutil",

    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,

    libraryDependencies ++= runtimeDependencies,
    libraryDependencies ++= testDependencies,
  )

lazy val core = project
  .dependsOn(ecsutil)
  .settings(
    name := "ecscalibur",

    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,

    libraryDependencies ++= runtimeDependencies,
    libraryDependencies ++= testDependencies,

    scalacOptions ++= Seq(
      "-experimental",
      "-language:experimental.macros",
      "-Xcheck-macros", 
    ),

    assembly / assemblyJarName := "ecscalibur.jar",
  )

lazy val demo_ecs = (project in file("./demo/ecs"))
  .dependsOn(core)
  .settings(
    name := "ecscalibur Demo",
    assembly / assemblyJarName := "demo_ecs.jar",
  )

lazy val demo_oop = (project in file("./demo/oop"))
  .settings(
    name := "OOP Demo",
    assembly / assemblyJarName := "demo_oop.jar",
  )
