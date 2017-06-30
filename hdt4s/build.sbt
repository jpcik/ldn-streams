name := "hdt4s"
organization := "ch.hevs"
version := "1.0.1"
scalaVersion := "2.11.11"
  
libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.3.1",
  "joda-time" % "joda-time" % "2.9.9",
//  "org.apache.jena" % "apache-jena-libs" % "3.1.0",
  "com.github.rdfhdt" % "hdt-java" % "v2.0" exclude("com.github.rdfhdt.hdt-java","hdt-fuseki") exclude("com.github.rdfhdt.hdt-java","hdt-java-cli"),
//  "com.typesafe.akka" %% "akka-http" % "10.0.7", 
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "junit" % "junit" % "4.12" % "test"
)

resolvers ++= Seq(
  "typesafe" at "http://repo.typesafe.com/typesafe/releases/",
  "jitpack" at "https://jitpack.io",
  Resolver.sonatypeRepo("public")
)

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

scalacOptions ++= Seq("-feature","-deprecation")

enablePlugins(JavaAppPackaging)
