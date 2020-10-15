name := "ldn-streams"
organization := "ch.hevs"
version := "1.0.2"
scalaVersion := "2.12.3"
  
libraryDependencies ++= Seq(
  "net.sourceforge.owlapi" % "owlapi-distribution" % "5.1.16", // from "https://repo1.maven.org/maven2/net/sourceforge/owlapi/owlapi-distribution/5.1.16/owlapi-distribution-5.1.16.jar",
  "com.github.jsonld-java" % "jsonld-java" % "0.9.0",
  "ch.hevs" %% "rdf-tools" % "0.1.2", 
  "ch.hevs" %% "rdf-tools-owlapi" % "0.1.2",
  "ch.hevs" %% "rdf-tools-jena" % "0.1.2",
  "com.typesafe.akka" %% "akka-http" % "10.0.7", 
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.7",
  "de.heikoseeberger" %% "akka-sse" % "3.0.0",
  "rsp" % "cqels_2.12" % "1.2.2",
  "eu.larkc.csparql" % "csparql-core" % "0.9.9",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "junit" % "junit" % "4.12" % "test",
  "org.apache.commons" % "commons-rdf-api" % "0.5.0"
)

resolvers ++= Seq(
  Resolver.mavenLocal,
  "typesafe" at "https://dl.bintray.com/typesafe/maven-releases/",
  Resolver.sonatypeRepo("public"),
  "jitpack" at "https://jitpack.io",
  "mvn" at "https://mvnrepository.com/artifact/",
  "plord" at "http://homepages.cs.ncl.ac.uk/phillip.lord/maven"
)

unmanagedJars in Compile += file(System.getProperty("java.home")).getParentFile / "lib"

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

scalacOptions ++= Seq("-feature","-deprecation")

enablePlugins(JavaAppPackaging)
