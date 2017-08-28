package ldnstream.streams.cqels

import scala.collection.JavaConverters._
import org.deri.cqels.data.Mapping
import org.deri.cqels.engine._
import akka.http.scaladsl.model.Uri
import ldnstream.streams.ActorStreamReceiver
import rdftools.rdf.Graph
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
    slct.register(setUpListener(insert))
    selects.put(name,slct)
  }
 
  override def push(id:String,insert:Map[String,String]=>Unit)={
    val con=setUpListener(insert)
    selects(id).register(con)
    ResultHandler(con)
  }
  
  private def setUpListener(insert:Map[String,String]=>Unit)=
    new ContinuousListener{
      def update(map:Mapping)={
        val newMap=map.vars.asScala.map{v=>
          v.getVarName->cqels.decode(map.get(v)).toString
        }.toMap
        insert(newMap)
      }
    }
  
  override def terminatePush(id:String,hnd:ResultHandler)={
    selects(id).unregister(hnd.native.asInstanceOf[ContinuousListener])
  }
}