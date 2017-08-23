package ldnstream.streams.cqels

import scala.collection.JavaConversions._
import scala.collection.mutable.Queue
import scala.io.StdIn
import scala.language.implicitConversions
import scala.language.postfixOps

import org.deri.cqels.data.Mapping
import org.deri.cqels.engine._

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.SourceQueueWithComplete
import ldnstream.streams.LdnStreamReceiver
import rdftools.rdf.Graph
import rdftools.rdf.api.JenaTools._
    
object CqelsReceiver extends LdnStreamReceiver{
  implicit val system = ActorSystem("LdnCqels")
  implicit val materializer= ActorMaterializer()
  val host="localhost"
  val port=8080 
 
  val cqelsCtx=new ExecContext("./",false)
  val cqels=new CQELSEngine(cqelsCtx)
  val selects=new collection.mutable.HashMap[String,ContinuousSelect]

  override def consumeGraph(uri:Uri,g:Graph)={
    g.triples.foreach { t => cqels send (uri.toString,t) }
  }
  
  //"SELECT ?s ?p ?o WHERE {STREAM <e.com/stream> [RANGE 2s] {?s ?p ?o}}"
  override def query(name:String,queryStr:String,
      queue:SourceQueueWithComplete[Map[String,String]])={
    
    val slct=cqelsCtx.registerSelect(queryStr)
    slct.register(new ContinuousListener{
      def update(map:Mapping)={
        val newMap=map.vars.map{v=>
          v.getVarName->cqels.decode(map.get(v)).toString
        }.toMap
        queue offer newMap
      }
    })
    selects.put(name,slct)
  }
 
  override def push(id:String,qu:Queue[Map[String,String]])={
    val con=new ContinuousListener{
      def update(map:Mapping)={
        println("I am alive")
        val newMap=map.vars.map{v=>
          v.getVarName->cqels.decode(map.get(v)).toString
        }.toMap
        qu.enqueue(newMap)
      }
    }
    selects(id).register(con)
    ResultHandler(con)
  }
    
  override def terminatePush(id:String,hnd:ResultHandler)={
    selects(id).unregister(hnd.native.asInstanceOf[ContinuousListener])
  }
  
  def main(args:Array[String]):Unit={
    val bindingFuture = Http().bindAndHandle(receiverRoute, host, port)
    //testStream
//sendSomeData
//query
    println(s"Server online at $base\nPress RETURN to stop...")
    StdIn.readLine()
    bindingFuture.flatMap(_.unbind()).onComplete(_ ⇒ system.terminate())
  }
}