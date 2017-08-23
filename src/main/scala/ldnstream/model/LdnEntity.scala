package ldnstream.model

import akka.http.scaladsl.model.ContentType
import org.apache.jena.riot.Lang
import akka.http.scaladsl.model.MediaType
import akka.http.scaladsl.model.HttpCharsets
import akka.http.scaladsl.model.MediaType.WithFixedCharset
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import scala.concurrent.ExecutionContext
import akka.http.scaladsl.model.Uri
import rdftools.rdf.Iri
import scala.language.implicitConversions
import org.apache.jena.rdf.model.ModelFactory
import java.io.StringReader
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.rdf.model.Model
import java.io.StringWriter

trait LdnEntity {
  import LdnTypes._
  implicit val system:ActorSystem
  implicit val materializer:ActorMaterializer
  implicit lazy val ctx=system.dispatcher

  implicit def uri2Iri(uri:Uri)=Iri(uri.toString)

  def toRdfLang(contentType:ContentType):Lang=toRdfLang(contentType.mediaType)
  
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

object LdnTypes {
  private val utf8 = HttpCharsets.`UTF-8`
  val `text/turtle`: WithFixedCharset =
     MediaType.customWithFixedCharset("text", "turtle", utf8)
  val `application/ld+json`: WithFixedCharset =
     MediaType.customWithFixedCharset("application", "ld+json", utf8)  
}