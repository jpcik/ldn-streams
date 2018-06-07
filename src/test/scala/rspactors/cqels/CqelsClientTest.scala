package rspactors.cqels

import rspactors.LdnStreamClient
import rspactors.StreamTarget
import akka.http.scaladsl.model.ContentType
import akka.actor._
import akka.stream.ActorMaterializer
import ldnstream.model.LdnTypes._
import rspactors.StreamClient
import rspactors.cqels.CqelsActorReceiver
import akka.http.scaladsl.model.MediaRange
import concurrent.duration._
import language.postfixOps

object CqelsClientTest {

  val sys=ActorSystem("testSys")
  implicit val ctx=sys.dispatcher

  def testClient={
    
    val client= new StreamClient{
      implicit val system=sys
      val materializer=ActorMaterializer() 
      
      def dataPushed(data:String,c:Int):Unit={
        println("finally:   "+  data)
      }
    }
    
    implicit val serverIri="http://hevs.ch/streams"
    implicit val ct:ContentType.NonBinary=`application/ld+json`
    implicit val range=MediaRange.apply(`application/ld+json`)
    val cqels=sys.actorOf(Props(new CqelsActorReceiver(serverIri,1)), "cqels")
    val cqels2=sys.actorOf(Props(new CqelsActorReceiver(serverIri,2)), "cqels2")
    
    client.postStream(cqels, "s1")
    client.postStream(cqels2, "s2")
    
    client.postQuery(cqels,"q1", s"SELECT ?s ?p ?o WHERE {STREAM <$serverIri/s1> [RANGE 2s] {?s ?p ?o}}",ct)
    client.postQuery(cqels2,"q2", s"SELECT ?s ?p ?o WHERE {STREAM <$serverIri/s2> [RANGE 2s] {?s ?p ?o}}",ct)
    
    val ev="""{  "@context": "http://schema.org/",  "@type": "Event", "name": "Nice concert"} """
    sys.scheduler.schedule(0 seconds, 5 seconds){
      client.postStreamItem(cqels, s"$serverIri/s1", ev, ct)
    }
    sys.scheduler.schedule(0 seconds, 5 seconds){
      client.postStreamItem(cqels2, s"$serverIri/s2", ev, ct)
    }
        client.getStreams(cqels)

    
    Thread.sleep(10000)
    
    //client.getStreamItem(cqels, s"$serverIri/q1/output")
    client.getStreamItemsPush(cqels, s"$serverIri/q1/push")
    client.getStreamItemsPush(cqels2, s"$serverIri/q2/push")
  }
  
  
  def main(args:Array[String]):Unit={
    testClient

  }
}