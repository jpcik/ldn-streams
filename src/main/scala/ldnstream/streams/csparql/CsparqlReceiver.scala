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

class CsparqlReceiver (iri:String,name:Int) extends ActorStreamReceiver{
  //val cqelsCtx=new ExecContext("./",false)
  //val cqels=new CQELSEngine(cqelsCtx)
  val selects=new collection.mutable.HashMap[String,CsparqlQueryResultProxy]

  val csparql=new CsparqlEngineImpl
  csparql.initialize()
  
  
  val serverIri=Uri(iri)
    
  class TestStream(iri:String) extends RdfStream(iri){
    
  }
  //override def declareStream(uri:String)={
    
  
  
  private def registerStream(uri:Uri)={
     if (csparql.getStreamByIri(uri.toString)==null) {
      val st=new TestStream(uri.toString)
      csparql.registerStream(st)
    }
  }
  
  override def consumeGraph(uri:Uri,g:Graph)={
    registerStream(uri)
      
    g.triples.foreach { t =>
      println("feed cqels "+uri)
      csparql.getStreamByIri(uri.toString).put(
          new RdfQuadruple(t.s.toString,t.p.toString,t.o.toString,System.currentTimeMillis))  
    }
  }
  
  //"SELECT ?s ?p ?o WHERE {STREAM <e.com/stream> [RANGE 2s] {?s ?p ?o}}"
  override def query(name:String,queryStr:String,
      insert:Map[String,String]=>Unit)={
    
    val i=queryStr.indexOf("STREAM <")
    val subs=queryStr.substring(i)
    val j=subs.indexOf(">")
    val streamIri=subs.substring(8,j)
    registerStream(streamIri)
    //println(streamIri)
    
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
        println("got updates ")
        rdf foreach {t=>
          //println("trolo")
          val mapp=Seq("v1"->t.get(0),"v2"->t.get(1)).toMap
          //println(s"dibi $mapp")
          insert(mapp)
          
        }
      }
    }
  
  override def terminatePush(id:String,hnd:ResultHandler)={
    selects(id).deleteObserver(hnd.native.asInstanceOf[Observer])
  }
  
}