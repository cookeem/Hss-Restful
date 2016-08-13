name := "hss-restful"

version := "1.0"

scalaVersion := "2.11.8"
libraryDependencies ++= {
  val akkaV = "2.4.8"
  Seq(
    "com.typesafe.akka" %% "akka-http-core" % akkaV,
    "com.typesafe.akka" %% "akka-http-experimental" % akkaV,
    "org.scala-lang.modules" %% "scala-async" % "0.9.5",
    "joda-time" % "joda-time" % "2.9.4",
    "org.joda" % "joda-convert" % "1.8.1",
    "org.elasticsearch" % "elasticsearch" % "2.3.1",
    "com.typesafe.play" %% "play-json" % "2.5.4"
  )
}
