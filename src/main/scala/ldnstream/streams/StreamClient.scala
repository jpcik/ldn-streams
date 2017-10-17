package ldnstream.streams

import akka.actor.ActorRef
import akka.pattern.ask
import org.apache.jena.riot.Lang
import org.apache.jena.rdf.model.ModelFactory
import rdftools.rdf.jena._
import rdftools.rdf.RdfTools
import rdftools.rdf.RdfSchema
import ldnstream.vocab.LDP
import rdftools.rdf.vocab.RDFS
import ldnstream.model.LdnEntity
import akka.http.scaladsl.model.ContentType
import akka.http.scaladsl.model.MediaRange
import akka.http.scaladsl.model.Uri
import rdftools.rdf.Iri
import akka.util.Timeout
import concurrent.duration._
import language.postfixOps
import scala.concurrent.Await
import ldnstream.vocab.RSPS
import akka.actor.Actor
import akka.actor.Props

trait StreamClient extends LdnEntity{
  implicit val timeout=Timeout(5 seconds)
  import StreamMsg._

  def dataPushed(data:String,count:Int):Unit

  def postStream(recv:ActorRef,streamName:String)
    (implicit targetUri:String,ct:ContentType.NonBinary)={
    implicit val lang=toRdfLang(ct)
    implicit val pop=MediaRange.apply(ct.mediaType)
    val pay=createStreamPayload(streamName)
    val req=CreateStream(Msg(pay,ct))
    recv ! req
  }
  def getStreams(recv:ActorRef)
    (implicit targetUri:String,range:MediaRange)={
    val req=RetrieveAllStreams()
    val t=(recv ? req).mapTo[ResponseMsg]
    val cop=Await.result(t,5 seconds)
  }
  def getStream(recv:ActorRef,streamUri:String)
      (implicit targetUri:String,range:MediaRange)={
    val req=RetrieveStream(Uri(streamUri))
    recv ! req
  }
  def postStreamItem(recv:ActorRef,streamUri:String,item:String,ct:ContentType.NonBinary)
    (implicit targetUri:String)={
    val req=SendStreamItem(Uri(streamUri),Msg(item,ct))
    recv ! req
  }
  def postQuery(recv:ActorRef,name:String,query:String,ct:ContentType.NonBinary)
    (implicit range:MediaRange)={
    implicit val lang=toRdfLang(ct)
    val req=CreateQuery(Msg(createQueryPayload(name, query),ct))
    recv ! req
  }
  def getStreamItem(recv:ActorRef,uri:Uri)
    (implicit range:MediaRange)={
    val req=RetrieveStreamItem(uri)
    val p=(recv ? req).mapTo[ResponseMsg]
    p.map(println)
    
  }
  def getStreamItemsPush(recv:ActorRef,uri:Uri)
   (implicit range:MediaRange)={
    val actor=system.actorOf(Props(new StreamListener))
    val req=PushStreamItems(uri,actor)
    recv ! req
  }
  import RdfTools._
  private def createStreamPayload(streamName:String)(implicit targetUri:String,lang:Lang)={
    implicit val m=ModelFactory.createDefaultModel
    val streamUri=s"$targetUri/$streamName"
      +=(targetUri:Iri,LDP.contains:Iri,streamUri:Iri)
      +=(streamUri,RDFS.label,lit(streamName))
      writeRdf(m)
  }
  import RdfTools._
  private def createQueryPayload(name:String,q:String)(implicit lang:Lang)={
    implicit val m=ModelFactory.createDefaultModel
    +=(iri("http://a.com"),RSPS.query,lit(q) )
    +=(iri("http://a.com"),RDFS.label,lit(name))
    writeRdf(m)    
  }
  
  class StreamListener extends Actor {
    def receive= {
      case resp:ResponseMsg=>
        
        dataPushed(resp.msg.body,resp.msg.count)
    }
  }
  
}