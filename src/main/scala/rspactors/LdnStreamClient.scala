package rspactors

import ldnstream.model.LdnNode
import ldnstream.Receiver
import akka.http.scaladsl.model._
import akka.http.scaladsl.Http
import scala.concurrent.duration._
import scala.concurrent.Await
import language.postfixOps
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.RDFDataMgr
import rdftools.rdf.jena._
import rdftools.rdf.RdfTools
import rspactors.vocab.LDP
import rdftools.rdf.RdfSchema
import org.apache.jena.riot.Lang
import java.io.StringWriter
import rdftools.rdf.vocab.DCterms
import ldnstream.Target
import rdftools.rdf.vocab.RDFS
import ldnstream.model.LdnEntity
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import ldnstream.model.LdnTypes
import rdftools.rdf.Iri

class LdnStreamClient(name:String)
  (implicit val system:ActorSystem=ActorSystem(name)) extends LdnEntity{
  //implicit val system=ActorSystem(name)
  implicit val materializer= ActorMaterializer()
  
  import HttpMethods._
  import LdnTypes._
  
  def createReadStream(streamName:String)
    (implicit server:StreamTarget,
     contentType:MediaType.WithFixedCharset):Unit={
    
    implicit val lang=toRdfLang(contentType)
    val pay=createStreamPayload(server, streamName)
    val ent=HttpEntity(ContentType( contentType),pay)
    val heads=List()//headers.`Content-Type`(`application/ld+json`))
    val resp=Http().singleRequest(HttpRequest(POST,server.uri,heads,ent))
  
    val popo=Await.result(resp,5 seconds)
    println(popo.status)
  }
  
  def getStreams()
    (implicit server:StreamTarget , contentType:MediaType):Unit={
    implicit val lang=toRdfLang(contentType)
    val heads=List(headers.Accept(`application/ld+json`))
    val resp=Http().singleRequest(HttpRequest(GET,server.uri,heads))
   resp.flatMap{r=>r.entity.toStrict(5.second).map { e=>e.data}.map(_.utf8String).map { pay =>
     println(pay)
   }}
    val popo=Await.result(resp,5 seconds)
    println(popo.entity)
  }
  
  def postStreamItem(payload:String)
    (implicit server:RdfStream , contentType:MediaType.WithFixedCharset):Unit={
    val ent=HttpEntity(ContentType( contentType),payload)
    val heads=List()//headers.`Content-Type`(`application/ld+json`))
    val resp=Http().singleRequest(HttpRequest(POST,server.inputUri.get,heads,ent))
  
    val popo=Await.result(resp,5 seconds)
    println(popo.status)
  }
  
  import RdfTools._
  def createStreamPayload(target:StreamTarget,streamName:String)(implicit lang:Lang)={
    implicit val m=ModelFactory.createDefaultModel
    val streamUri:Iri=target.streamUri(streamName)
      //+=(target.uri.toString,LDP.contains,streamUri:Iri)
      +=(streamUri,RDFS.label,lit(streamName))
      val sw=new StringWriter
      RDFDataMgr.write(sw, m, lang)
      sw.toString
  }
}

case class StreamTarget(uri:Uri) extends Target {
  def streamUri(name:String)=s"$uri/stream/$name"
}