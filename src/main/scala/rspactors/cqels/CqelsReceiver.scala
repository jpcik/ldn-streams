package rspactors.cqels

import scala.collection.JavaConverters._

import org.deri.cqels.data.Mapping
import org.deri.cqels.engine._

import akka.http.scaladsl.model.Uri
import rdftools.rdf.Graph
import rdftools.rdf.jena._
import rspactors.StreamReceiver
    
trait CqelsReceiver extends StreamReceiver{
  val id:Int
  val cqelsCtx=new ExecContext(s"./tmp/cq_$id",true)
  val cqels=new CQELSEngine(cqelsCtx)
  val selects=new collection.mutable.HashMap[String,ContinuousSelect]
  
  override def consumeGraph(uri:Uri,g:Graph)={
    g.triples.foreach { t => cqelsCtx.engine send (uri.toString,t) }
  }
  
  //"SELECT ?s ?p ?o WHERE {STREAM <e.com/stream> [RANGE 2s] {?s ?p ?o}}"
  override def query(name:String,queryStr:String,
      insert:Map[String,String]=>Unit)={
    
    val slct=cqelsCtx.registerSelect(queryStr)
    slct.register(new ContinuousListener{
      def update(map:Mapping)={
        
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
        //println("I am alive")
        val newMap=map.vars.asScala.map{v=>
          v.getVarName->cqels.decode(map.get(v)).toString
        }.toMap
        synchronized { 
          insert(newMap)
          }
      }
    }
    selects(id).register(con)
    ResultHandler(con)
  }
    
  override def terminatePush(id:String,hnd:ResultHandler)={
    selects(id).unregister(hnd.native.asInstanceOf[ContinuousListener])
  }

}


