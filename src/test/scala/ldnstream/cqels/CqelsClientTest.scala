package ldnstream.cqels

import ldnstream.streams.LdnStreamClient
import ldnstream.streams.StreamTarget
import akka.http.scaladsl.model.ContentType
import akka.actor._
import akka.stream.ActorMaterializer
import ldnstream.model.LdnTypes._
import ldnstream.streams.StreamClient
import ldnstream.streams.cqels.CqelsActorReceiver
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
      
      def dataPushed(data:String):Unit={
        println("finally:   "+  data)
      }
    }
    
    implicit val serverIri="http://hevs.ch/streams"
    implicit val ct:ContentType.NonBinary=`application/ld+json`
    implicit val range=MediaRange.apply(`application/ld+json`)
    val cqels=sys.actorOf(Props(new CqelsActorReceiver(serverIri)), "cqels")
    
    client.postStream(cqels, "s1")
    
    client.postQuery(cqels,"q1", s"SELECT ?s ?p ?o WHERE {STREAM <$serverIri/s1> [RANGE 2s] {?s ?p ?o}}",ct)
    
    val ev="""{  "@context": "http://schema.org/",  "@type": "Event", "name": "Nice concert"} """
    sys.scheduler.schedule(0 seconds, 5 seconds){
      client.postStreamItem(cqels, s"$serverIri/s1", ev, ct)
    }
        client.getStreams(cqels)

    
    Thread.sleep(10000)
    
    //client.getStreamItem(cqels, s"$serverIri/q1/output")
    client.getStreamItemsPush(cqels, s"$serverIri/q1/push")
  }
  
  
  def main(args:Array[String]):Unit={
    testClient

  }
}