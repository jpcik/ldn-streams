package ldnstream.streams.plain

import ldnstream.streams.StreamReceiver
import akka.http.scaladsl.model.Uri
import rdftools.rdf._
import ldnstream.streams.StreamReceiver

trait PlainReceiver extends StreamReceiver{
  val id:Int

  override def consumeGraph(uri:Uri,g:Graph)={
    //g.triples.foreach { t => cqelsCtx.engine send (uri.toString,t) }
  }
  
  //"SELECT ?s ?p ?o WHERE {STREAM <e.com/stream> [RANGE 2s] {?s ?p ?o}}"
  override def query(name:String,queryStr:String,
      insert:Map[String,String]=>Unit)={
    
  }
 
  override def push(id:String,insert:Map[String,String]=>Unit)={
      //    insert(newMap)
          
      
    
    //selects(id).register(con)
    ResultHandler(null)
  }
    
  override def terminatePush(id:String,hnd:ResultHandler)={
  }

}
