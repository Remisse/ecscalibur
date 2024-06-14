ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.5.0-RC1"

lazy val root = (project in file("."))
  .settings(
    name := "ECScalibur",
    libraryDependencies += "com.github.sbt" % "junit-interface" % "0.13.3" % Test,
    // add scala test
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.18" % Test,
    // libraryDependencies += "org.scalamock" %% "scalamock" % "6.0.0" % Test,
    scalacOptions ++= Seq(
      "-Xcheck-macros", 
      "-Ycheck:all", 
      "-experimental",
      ),
  )
