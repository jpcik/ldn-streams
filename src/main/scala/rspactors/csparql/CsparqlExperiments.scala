package rspactors.csparql

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import ldnstream.model.LdnTypes._
import scala.util.Random
import rspactors.StreamClient
import akka.stream.ActorMaterializer
import akka.actor.Props
import concurrent.duration._
import language.postfixOps
import java.io.FileWriter

object CsparqlExperiments {
   
  val sys=ActorSystem("testSys")
  implicit val ctx=sys.dispatcher
  implicit val ct:ContentType.NonBinary=`application/ld+json`
  implicit val range=MediaRange.apply(`application/ld+json`)
  implicit val serverIri="http://hevs.ch/streams"

  val query1=s"""
    REGISTER QUERY q1 AS
    PREFIX qudt: <http://qudt.org/1.1/schema/qudt#> 
    SELECT ?s ?o 
    FROM STREAM <$serverIri/s1> [RANGE 1s TUMBLING] 
    WHERE {
      ?s qudt:numericValue ?o
    }"""
 
  def randouble=Random.nextDouble
  val contextJson=""" 
    "@context": {
      "sosa": "http://www.w3.org/ns/sosa/",
      "ex": "http://example.org/vocab#",
      "qudt": "http://qudt.org/1.1/schema/qudt#",
      "unit": "http://qudt.org/1.1/vocab/unit#",
      "sosa:observedProperty": { "@type": "@id" },
      "qudt:unit": { "@type": "@id" }
    }"""  
  def observation=s"""{
      "@id": "http://example.org/someobs1",
      "@type": "sosa:Observation",
      "sosa:resultTime": "2011-09-09T22:09:09",
      "sosa:observedProperty":"ex:Temperature",
      "sosa:madeBySensor":"ex:sensor1",
      "sosa:hasResult": {"qudt:numericValue":$randouble , "qudt:unit":"unit:celsius"},
      $contextJson
    }"""  
    
    
  def createClient=
    new StreamClient{
      var count=0
      implicit val system=sys
      val materializer=ActorMaterializer() 
      
      def dataPushed(data:String,c:Int):Unit={
        
        println("got it"+data)
        //println("finally:   "+  data)
        count+=c
      }
  }

  def createEngine(name:Int)={
    sys.actorOf(Props(new CsparqlReceiver(serverIri,name)), s"csparql$name")
  }
  
  def runall={
    
    val queryNum=1
    val senderNum=100
    val consumerNum=1
    val cqelsNum=1
    val sendInterval=1000 milliseconds
    
    val leader=createClient
    
    val processors=(1 to cqelsNum) map {i=>
      createEngine(i)
    }
    
    Thread.sleep(3000)

    
    processors foreach{engine=>
      leader.postStream(engine, "s1")
    }
    
    Thread.sleep(1000)
    
    def getACqels={
      val i=Random.nextInt(cqelsNum)
      processors(i)
    }
    
    (1 to queryNum) foreach {i=>
      processors foreach{engine=>
        leader.postQuery(engine,s"q$i", query1,ct)
      }
    }
    
    Thread.sleep(1000)

    
    val senders= (1 to senderNum) map {i=>createClient}
    val consumers=(1 to consumerNum) map {i=>createClient}
    
   /*
    val scheds=senders.map{sender=>
      sys.scheduler.schedule(0 seconds, sendInterval){
        sender.postStreamItem(cqels, s"$serverIri/s1", observation, ct)
      }  
      
    }*/
   
    var j=0
    val sched =  sys.scheduler.schedule(0 seconds, sendInterval){
      senders.foreach{sender=>
        sender.postStreamItem(processors(j), s"$serverIri/s1", observation, ct)
        j+=1
        if (j>=processors.size) j=0
      }  
    }
    
    var i=0
    consumers foreach {consumer=>
      consumer.getStreamItemsPush(processors(i), s"$serverIri/q1/push")
      i+=1
    }
        
    //leader.getStreams(cqels)

    
    //Thread.sleep(10000)
    
    //client.getStreamItem(cqels, s"$serverIri/q1/output")
  
    

    Thread.sleep(10000)
        println("""
          ###################
          ###################
          ###################""")

    //scheds.foreach(_.cancel)
    
    sched.cancel
     val fw =new FileWriter("output")
    fw.write("done it\n")
    
    consumers foreach {consumer=>
      fw.write("\n########got: "+consumer.count)
    }
            
   
    fw.close

    
  }
  
  
  def main(args:Array[String]):Unit={
    runall

  }

}