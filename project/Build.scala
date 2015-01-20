import sbt.IvyConsole.Dependencies
import sbt._
import Keys._

object ApplicationBuild extends Build {
  import Dependencies._

  lazy val DbTest = config("db") extend(Test)
  lazy val UnitTest = config("unit") extend(Test)
  // (other irrelvant ".settings" calls omitted here...)


  lazy val project = Project("renesca", file("."))

    // Functional test setup.
    // See http://www.scala-sbt.org/release/docs/Detailed-Topics/Testing#additional-test-configurations-with-shared-sources
    .configs(DbTest)
    .settings(inConfig(DbTest)(Defaults.testSettings) : _*)
    .configs(UnitTest)
    .settings(inConfig(UnitTest)(Defaults.testSettings) : _*)
}
