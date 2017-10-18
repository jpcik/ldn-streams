package ldnstream.streams.plain

import ldnstream.streams.StreamReceiver
import akka.http.scaladsl.model.Uri
import rdftools.rdf._
import ldnstream.streams.StreamReceiver
import scala.collection.mutable.Queue
import scala.concurrent.duration._
import language.postfixOps
import ldnstream.streams.ActorStreamReceiver
import scala.concurrent.Future

class PlainReceiver(iri:String,val id:Int) extends ActorStreamReceiver{
  //val id:Int
  val serverIri=Uri(iri)

  
  val qu = new Queue[Triple]
  
  override def consumeGraph(uri:Uri,g:Graph)={
    g.triples.foreach { t =>  qu.enqueue(t)}
    
  }
  
  //"SELECT ?s ?p ?o WHERE {STREAM <e.com/stream> [RANGE 2s] {?s ?p ?o}}"
  override def query(name:String,queryStr:String,
      insert:Map[String,String]=>Unit)={
    /*system.scheduler.schedule(0 seconds, 100 milliseconds){
      if (!qu.isEmpty){
      val t=qu.dequeue //All(t=>true)
      //t.foreach{tt=>
        insert(Map("s"->t.s.toString,"o"->t.o.toString))
      //}
      }
    }*/
    
  }
 
  override def push(id:String,insert:Map[String,String]=>Unit)={
    
    system.scheduler.schedule(0 seconds, 100 milliseconds){
      if (!qu.isEmpty){
      val t=qu.dequeueAll(t=>true)
      t.foreach{tt=>
        insert(Map("s"->tt.s.toString,"o"->tt.o.toString))
      }
      }
    
    }
      //    insert(newMap)
          
      
    
    ResultHandler(null)
  }
    
  override def terminatePush(id:String,hnd:ResultHandler)={
  }

}
