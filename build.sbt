ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.4.2"

lazy val root = (project in file("."))
  .settings(
    name := "ECScalibur",
    libraryDependencies += "com.google.guava" % "guava" % "33.2.1-jre",
    libraryDependencies += "dev.zio" %% "izumi-reflect" % "2.3.10",

    // Test libraries
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.18" % Test,
    // libraryDependencies += "org.scalamock" %% "scalamock" % "6.0.0" % Test,

    scalacOptions ++= Seq(
      "-deprecation",
      // "-experimental",
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
      // "-Xkind-projector",
      "-Ycheck:all", 
      "-Ykind-projector",
      "-Ysafe-init",
      // "-Yexplicit-nulls",
      ),
  )
