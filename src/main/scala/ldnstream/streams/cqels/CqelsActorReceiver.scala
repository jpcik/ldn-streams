package ldnstream.streams.cqels

import ldnstream.streams.ActorStreamReceiver
import org.deri.cqels.engine._
import org.deri.cqels.data.Mapping
import scala.collection.mutable.Queue
import akka.http.scaladsl.model.Uri
import rdftools.rdf.Graph
import akka.stream.scaladsl.SourceQueueWithComplete
import collection.JavaConverters._
import rdftools.rdf.api.JenaTools._

class CqelsActorReceiver(iri:String) extends ActorStreamReceiver{
  val cqelsCtx=new ExecContext("./",false)
  val cqels=new CQELSEngine(cqelsCtx)
  val selects=new collection.mutable.HashMap[String,ContinuousSelect]

  val serverIri=Uri(iri)
    
  override def consumeGraph(uri:Uri,g:Graph)={
    g.triples.foreach { t =>
      println("feed cqels "+uri)
      cqels send (uri.toString,t) 
    }
  }
  
  //"SELECT ?s ?p ?o WHERE {STREAM <e.com/stream> [RANGE 2s] {?s ?p ?o}}"
  override def query(name:String,queryStr:String,
      insert:Map[String,String]=>Unit)={
    
    val slct=cqelsCtx.registerSelect(queryStr)
    slct.register(new ContinuousListener{
      def update(map:Mapping)={
                println("new data")

        val newMap=map.vars.asScala.map{v=>
          v.getVarName->cqels.decode(map.get(v)).toString
        }.toMap
        insert(newMap)
      }
    })
    selects.put(name,slct)
  }
 
  override def push(id:String,insert:Map[String,String]=>Unit)={
    val con=new ContinuousListener{
      def update(map:Mapping)={
        println("I am alive")
        val newMap=map.vars.asScala.map{v=>
          v.getVarName->cqels.decode(map.get(v)).toString
        }.toMap
        insert(newMap)
      }
    }
    selects(id).register(con)
    ResultHandler(con)
  }
    
  override def terminatePush(id:String,hnd:ResultHandler)={
    selects(id).unregister(hnd.native.asInstanceOf[ContinuousListener])
  }
}