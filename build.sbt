ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.5.0-RC2"

lazy val root = (project in file("."))
  .settings(
    name := "ECScalibur",

    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,

    libraryDependencies ++= Seq(
      "com.google.guava" % "guava" % "33.2.1-jre",
      "dev.zio" %% "izumi-reflect" % "2.3.10",
    ),
    // Test libraries
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.18" % Test,
      // "org.scalamock" %% "scalamock" % "6.0.0" % Test,
    ),

    scalacOptions ++= Seq(
      "-deprecation",
      "-experimental",
      "-feature",
      "-language:experimental.macros",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-unchecked",
      // "-Wsafe-init",
      "-Wunused:implicits",
      "-Wunused:explicits",
      "-Wunused:imports",
      "-Wunused:locals",
      "-Wunused:params",
      "-Wunused:privates",
      "-Wvalue-discard",
      // "-Xfatal-warnings",
      "-Xcheck-macros", 
      "-Xkind-projector",
      "-Ycheck:all", 
      // "-Yexplicit-nulls",
      ),
  )
