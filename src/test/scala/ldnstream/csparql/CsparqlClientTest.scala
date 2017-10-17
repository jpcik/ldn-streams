package ldnstream.csparql

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
import ldnstream.streams.csparql.CsparqlReceiver

object CsparqlClientTest {

  def testClient={
    val sys=ActorSystem("testSys")
    implicit val ctx=sys.dispatcher
    
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
    val csparql=sys.actorOf(Props(new CsparqlReceiver(serverIri,0)), "csparql")
    
    client.postStream(csparql, "s1")
        
    val ev="""{  "@context": "http://schema.org/",  "@type": "Event", "name": "Nice concert"} """
    sys.scheduler.schedule(0 seconds, 5 seconds){
      client.postStreamItem(csparql, s"$serverIri/s1", ev, ct)
    }
        client.getStreams(csparql)

        
    client.postQuery(csparql,"q1", s"REGISTER query q1 AS SELECT ?s ?p ?o FROM STREAM <$serverIri/s1> [RANGE 2s STEP 2s] WHERE {?s ?p ?o}",ct)

    
    Thread.sleep(10000)
    
    //client.getStreamItem(cqels, s"$serverIri/q1/output")
    client.getStreamItemsPush(csparql, s"$serverIri/q1/push")
  }
  
  
  def main(args:Array[String]):Unit={
    testClient    
  }
}