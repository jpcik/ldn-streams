package ldnstream.model

import akka.http.scaladsl.model.headers.Link
import akka.actor._
import akka.http.scaladsl.model._
import org.apache.jena.riot.Lang
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.HttpMethods._
import scala.concurrent.Future
import java.io.StringReader
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.rdf.model.ModelFactory
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.collection.JavaConversions._
import ldnstream.Sender
import ldnstream.ReceiverRef
import ldnstream.Receiver

trait LdnSender extends Sender with LdnNode {
  import scala.language.postfixOps
val host=""
val port=0
  def findInbox(links:Seq[Link])={
    links.find (_.values.find ( _.params.find { linkPar => 
      linkPar.key=="rel" && linkPar.value==LdnVocab.inbox }
      .isDefined ).isDefined ).map(v=>ReceiverRef(v.values.head.uri))
  }
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  def discover(url:String)={
    discoverByLink(url).flatMap {uriOpt=> 
      if (uriOpt.isEmpty)
        discoverByRdf(url,`text/turtle`)
      else Future(uriOpt)
    }
  }
  
  def discoverByRdf(url:String,mediaType:MediaType)={
    val lang=mediaType match{
      case `text/turtle` => Lang.TURTLE
      case `application/ld+json` => Lang.JSONLD
      case _ => Lang.JSONLD
    }
    val resp = Http().singleRequest(HttpRequest(GET,url,List(Accept(mediaType))))
    resp.flatMap{r=>r.entity.toStrict(5.second).map { e=>e.data}.map(_.utf8String).map { pay =>
      val m=ModelFactory.createDefaultModel
      val sr=new StringReader(pay)
      RDFDataMgr.read(m, sr, "",lang)
      val inboxProp=ResourceFactory.createProperty(LdnVocab.inbox)
      val inboxNode= m.listStatements(null, inboxProp, null).toSeq.headOption
      inboxNode.map(stm=>ReceiverRef(Uri(stm.getObject.asResource.getURI)))
    }}
  }
  
  def discoverByLink(url:String)={
    val resp = Http().singleRequest(HttpRequest(HEAD,url))    
    resp.map{res=>
      println(res.status)
      val inbox=findInbox(getLinks(res))
      println(inbox)
      inbox
    }
  }

  def send(receiver:Receiver,payload:Any):Unit={
    val ent=HttpEntity(`application/ld+json`,payload.toString)
    val heads=List(headers.`Content-Type`(`application/ld+json`))
    val resp=Http().singleRequest(HttpRequest(POST,receiver.inboxUri,heads,ent))
  
    val popo=Await.result(resp,5 seconds)
    println(popo.status)
  }
  
  def discoverAndSend(targetUrl:String,payload:String)={
    discover(targetUrl).map{inboxUri=>
      inboxUri.map{uri=>
        send(uri,payload)
      }
    }
  }
   
}
