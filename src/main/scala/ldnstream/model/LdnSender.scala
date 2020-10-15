package ldnstream.model

import scala.concurrent._
import scala.concurrent.duration._
import scala.language.postfixOps

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.HttpMethods._
import ldnstream.Receiver

trait LdnSender extends LdnNode {
  import LdnTypes._
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
