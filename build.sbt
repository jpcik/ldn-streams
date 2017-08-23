name := "ldn-streams"
organization := "ch.hevs"
version := "1.0.1"
scalaVersion := "2.11.8"
  
libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.3.1",
  "joda-time" % "joda-time" % "2.9.9",
  "ch.hevs" %% "rdf-tools" % "0.0.1",
  "org.apache.jena" % "apache-jena-libs" % "3.1.0",
  "com.typesafe.akka" %% "akka-http" % "10.0.7", 
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.7",
  "de.heikoseeberger" %% "akka-sse" % "3.0.0",
  "rsp" %% "cqels" % "1.2.0",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "junit" % "junit" % "4.12" % "test"
)

resolvers ++= Seq(
  "typesafe" at "http://repo.typesafe.com/typesafe/releases/",
  Resolver.sonatypeRepo("public")
)

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

scalacOptions ++= Seq("-feature","-deprecation")

enablePlugins(JavaAppPackaging)