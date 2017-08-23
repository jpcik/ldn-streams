package ldnstream.vocab

import rdftools.rdf.Vocab

import rdftools.rdf._

import rdftools.rdf.RdfTools._

object RSPS extends Vocab {
  override val iri: Iri = "http://w3id.org/rsp/streams#"
  val OutputStream = clazz("OutputStream")
  val InputStream = clazz("InputStream")
  val Stream = clazz("Stream")
  val output = prop("output")
  val input = prop("input")
  val query = prop("query")
}