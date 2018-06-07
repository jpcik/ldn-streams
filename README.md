# LDN-Streams and RSP actors

This project is an implementation of Linked Data Notifications for RDF streams.

It includes an implementation of plain LDN, which can be run as follows:

## Running an LDN receiver
The LDN receiver is implemented using Akka HTTP.

The requirements for running the ldn-streams are:

 * install JDK8
 * install sbt

### Running from source code 

 * first clone the repo
 * type `sbt`
 * type `run`

An Http server will run on localhost:8080.

For RSP actors examples, check the test classes under src/test for a couple of running examples for CQELS and C-SPARQL implementations.