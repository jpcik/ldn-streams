package ldnstream.streams

import akka.actor.ActorRef
import org.apache.jena.riot.Lang
import org.apache.jena.rdf.model.ModelFactory
import rdftools.rdf.api.JenaTools
import rdftools.rdf.RdfTools
import rdftools.rdf.RdfSchema
import ldnstreams.vocab.LDP
import rdftools.rdf.vocab.RDFS
import ldnstream.model.LdnEntity
import akka.http.scaladsl.model.ContentType
import akka.http.scaladsl.model.MediaRange
import akka.http.scaladsl.model.Uri

trait StreamClient extends LdnEntity{
  def createReadStream(recv:ActorRef,streamName:String)
    (implicit targetUri:String,ct:ContentType.NonBinary)={
    implicit val lang=toRdfLang(ct)
    implicit val pop=MediaRange.apply(ct.mediaType)
    val pay=createStreamPayload(streamName)
    val req=CreateStream(StreamMsg.Msg(pay,ct))
    recv ! req
  }
  def getStreams(recv:ActorRef)
    (implicit targetUri:String,range:MediaRange)={
    val req=RetrieveAllStreams
    recv ! req
  }
  def getStream(recv:ActorRef,streamUri:String)
      (implicit targetUri:String,range:MediaRange)={
    val req=RetrieveStream(Uri(streamUri))
    recv ! req
  }
  def postStreamItem(recv:ActorRef,streamUri:String,item:String,ct:ContentType.NonBinary)
    (implicit targetUri:String)={
    val req=SendStreamItem(Uri(streamUri),StreamMsg.Msg(item,ct))
    recv ! req
  }
  
  
  
  import JenaTools._
  import RdfTools._
  import RdfSchema._
  private def createStreamPayload(streamName:String)(implicit targetUri:String,lang:Lang)={
    implicit val m=ModelFactory.createDefaultModel
    val streamUri=s"$targetUri/$streamName"
      +=(targetUri,LDP.contains,streamUri)
      +=(streamUri,RDFS.label,lit(streamName))
      writeRdf(m)
  }
}