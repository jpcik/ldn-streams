name := "ldn-streams"
organization := "ch.hevs"
version := "1.0.2"
scalaVersion := "2.12.3"
  
libraryDependencies ++= Seq(
  "ch.hevs" %% "rdf-tools" % "0.1.2",
  "ch.hevs" %% "rdf-tools-owlapi" % "0.1.2",
  "ch.hevs" %% "rdf-tools-jena" % "0.1.2",
  "com.typesafe.akka" %% "akka-http" % "10.0.7", 
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.7",
  "de.heikoseeberger" %% "akka-sse" % "3.0.0",
  "com.github.jpcik" % "cqels" % "1.2.2",
  "com.github.jpcik.CSPARQL-engine" % "csparql-core" % "0.9.9",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "junit" % "junit" % "4.12" % "test"
)

resolvers ++= Seq(
  "typesafe" at "http://repo.typesafe.com/typesafe/releases/",
  Resolver.sonatypeRepo("public"),
  "jitpack" at "https://jitpack.io"
)


EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

scalacOptions ++= Seq("-feature","-deprecation")

enablePlugins(JavaAppPackaging)
