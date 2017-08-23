package ldnstream.core

import org.apache.jena.rdf.model.ModelFactory
import akka.http.scaladsl.model.Uri
import ldnstream.model.RdfStream
import akka.actor._
import collection.mutable.{Map=>MutableMap}
import org.apache.jena.rdf.model.ResourceFactory
import java.io.StringWriter
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import concurrent.duration._
import scala.language.postfixOps
import ldnstreams.vocab.LDP
import rdftools.rdf.api.JenaTools._
import rdftools.rdf._
import rdftools.rdf.RdfSchema._
import ldnstreams.vocab.RSPS
import scala.language.implicitConversions
import akka.http.scaladsl.model.ContentType
import ldnstream.model.LdnTypes

class StreamsHandler(targetUri:Uri) extends Actor{
  
  val streams=MutableMap[Uri,RdfStream]()
  implicit val timeout=Timeout(5 seconds)
  
  implicit val ctx=context.dispatcher
  
  def receive = {
    case NewStream(stream) =>
      if (streams.contains(stream.uri))
        sender ! Error(s"Stream uri alread exists ${stream.uri}")
      else {
        streams+=(stream.uri->stream)
        sender ! Ok(s"Created stream ${stream.uri}")
      }
    case FetchStream(uri) => 
      val stream=streams.get(uri)
      sender ! stream
    case RemoveStream(uri)=>
      streams.remove(uri)
    case FetchAllStreams=>
      //implicit val m=ModelFactory.createDefaultModel
      sender ! streams.values.toSeq
      
    case LastFromStream(uri,n)=>
      println(uri)
      streams.get(uri).map{stream=>
        println("getting this")
        val data=(stream.outputRef.get ? Last(n)).mapTo[String]
        data.map { x => sender ! data }
      }
    case FetchStreamInbox(uri)=>
      val opInbox=streams.get(uri) map {stream=>
        uri.toString.replace("/streams","/streams/inbox")
      }
      sender ! opInbox
  }
  
  
}


trait Ack
case class Error(msg:String) extends Ack
case class Ok(msg:String) extends Ack



import LdnTypes._

case class FetchAllStreams() 
case class RemoveStream(uri:Uri)
case class NewStream(stream:RdfStream)
case class FetchStream(uri:Uri)
case class FetchStreamInbox(streamUri:Uri)
case class LastFromStream(uri:Uri,n:Int)