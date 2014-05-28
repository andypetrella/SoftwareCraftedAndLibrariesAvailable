name := "vote-client"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache
)

play.Project.playScalaSettings

libraryDependencies += "com.typesafe.akka" %% "akka-remote" % "2.2.0"