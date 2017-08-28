package ldnstream.streams.csparql

import ldnstream.streams.ActorStreamReceiver
import akka.http.scaladsl.model.Uri
import rdftools.rdf.Graph
import eu.larkc.csparql.cep.api.RdfStream
import eu.larkc.csparql.core.engine.CsparqlEngineImpl
import eu.larkc.csparql.cep.api.RdfQuadruple
import eu.larkc.csparql.core.ResultFormatter
import eu.larkc.csparql.common.RDFTable
import java.util.Observable
import collection.JavaConverters._
import eu.larkc.csparql.core.engine.CsparqlQueryResultProxy
import java.util.Observer

class CsparqlReceiver (iri:String) extends ActorStreamReceiver{
  //val cqelsCtx=new ExecContext("./",false)
  //val cqels=new CQELSEngine(cqelsCtx)
  val selects=new collection.mutable.HashMap[String,CsparqlQueryResultProxy]

  val csparql=new CsparqlEngineImpl
  csparql.initialize()
  
  
  val serverIri=Uri(iri)
    
  class TestStream(iri:String) extends RdfStream(iri){
    
  }
  override def consumeGraph(uri:Uri,g:Graph)={
    if (csparql.getStreamByIri(uri.toString)==null) {
      val st=new TestStream(uri.toString)
      csparql.registerStream(st)
    }
      
    g.triples.foreach { t =>
      println("feed cqels "+uri)
      csparql.getStreamByIri(uri.toString).put(
          new RdfQuadruple(t.s.toString,t.p.toString,t.o.toString,System.currentTimeMillis))  
    }
  }
  
  //"SELECT ?s ?p ?o WHERE {STREAM <e.com/stream> [RANGE 2s] {?s ?p ?o}}"
  override def query(name:String,queryStr:String,
      insert:Map[String,String]=>Unit)={
    
    val slct=csparql.registerQuery(queryStr, false)
    slct.addObserver(setUpListener(insert))
    selects.put(name,slct)
  }
 
  override def push(id:String,insert:Map[String,String]=>Unit)={
    val con=setUpListener(insert)
    selects(id).addObserver(con)
    ResultHandler(con)
  }
  
  private def setUpListener(insert:Map[String,String]=>Unit)=
    new ResultFormatter{
      def update(obs:Observable,obj:Object)={
        val rdf=obj.asInstanceOf[RDFTable].asScala
        
        rdf foreach {t=>
          
          insert(Seq("1"->t.get(1),"2"->t.get(2)).toMap)
          
        }
      }
    }
  
  override def terminatePush(id:String,hnd:ResultHandler)={
    selects(id).deleteObserver(hnd.native.asInstanceOf[Observer])
  }
  
}