package ldnstream.vocab

import rdftools.rdf.Vocab

import rdftools.rdf._

import rdftools.rdf.RdfTools._

object LDP extends Vocab {
  override val iri: Iri = "http://www.w3.org/ns/ldp#"
  val Page = clazz("Page")
  val PageSortCriterion = clazz("PageSortCriterion")
  val IndirectContainer = clazz("IndirectContainer")
  val DirectContainer = clazz("DirectContainer")
  val BasicContainer = clazz("BasicContainer")
  val Container = clazz("Container")
  val NonRDFSource = clazz("NonRDFSource")
  val RDFSource = clazz("RDFSource")
  val Resource = clazz("Resource")
  val constrainedBy = prop("constrainedBy")
  val hasMemberRelation = prop("hasMemberRelation")
  val pageSortCollation = prop("pageSortCollation")
  val contains = prop("contains")
  val insertedContentRelation = prop("insertedContentRelation")
  val pageSortCriteria = prop("pageSortCriteria")
  val inbox = prop("inbox")
  val membershipResource = prop("membershipResource")
  val pageSequence = prop("pageSequence")
  val member = prop("member")
  val isMemberOfRelation = prop("isMemberOfRelation")
  val pageSortPredicate = prop("pageSortPredicate")
  val pageSortOrder = prop("pageSortOrder")
}