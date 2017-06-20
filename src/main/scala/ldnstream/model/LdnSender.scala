package ldnstream.model

import java.io.StringReader

import scala.collection.JavaConversions._
import scala.concurrent._
import scala.concurrent.duration._
import scala.language.postfixOps

import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.headers._
import akka.stream.ActorMaterializer
import ldnstream.Receiver
import ldnstream.ReceiverRef
import ldnstream.Sender

trait LdnSender extends Sender with LdnNode {
  
  val host=""
  val port=0


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
