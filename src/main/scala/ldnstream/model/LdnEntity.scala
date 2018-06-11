package ldnstream.model

import java.io.StringReader
import java.io.StringWriter

import scala.language.implicitConversions

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.MediaType.WithFixedCharset
import akka.stream.ActorMaterializer
import rdftools.rdf.Iri

/**
 * Represents a LDN base class. Includes common attributes including actor system, context and RDF utils.
 */
trait LdnEntity {
  import LdnTypes._
  
  /** Underlying actor system */
  implicit val system:ActorSystem
  
  /** Underlying actor materializer */
  implicit val materializer:ActorMaterializer
  
  /** Underlying context */
  implicit lazy val ctx=system.dispatcher

  /** Convert Uri to Iri */
  implicit def uri2Iri(uri:Uri)=Iri(uri.toString)

  /** Transform ContentType to RDF lang */
  def toRdfLang(contentType:ContentType):Lang=toRdfLang(contentType.mediaType)
  
  /** Transform MediaType to RDF lang */
  def toRdfLang(mediaType:MediaType)= mediaType match{
    case `text/turtle` => Lang.TURTLE
    case `application/ld+json` => Lang.JSONLD
    case _ => Lang.JSONLD
  }
  
  def loadRdf(rdfString:String)(implicit lang:Lang)={
    val m=ModelFactory.createDefaultModel
    val sr=new StringReader(rdfString)
    RDFDataMgr.read(m, sr,null, lang)
    m
  }
  
  def writeRdf(m:Model)(implicit lang:Lang)={
    val sw=new StringWriter
    RDFDataMgr.write(sw, m, lang)
    sw.toString
  }
  
}

/**
 * Represents LDN Media types
 */
object LdnTypes {
  private val utf8 = HttpCharsets.`UTF-8`
  val `text/turtle`: WithFixedCharset =
     MediaType.customWithFixedCharset("text", "turtle", utf8)
  val `application/ld+json`: WithFixedCharset =
     MediaType.customWithFixedCharset("application", "ld+json", utf8)  
}