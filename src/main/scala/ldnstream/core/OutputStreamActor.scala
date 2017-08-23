package ldnstream.core

import akka.actor._
import scala.collection.mutable.Queue
import ldnstream.model.RdfStream
import rdftools.rdf.Triple
import org.apache.jena.riot.RDFDataMgr
import java.io.StringWriter
import rdftools.rdf.api.JenaTools
import collection.JavaConversions._
import java.io.ObjectOutputStream

class OutputStreamActor(maxSize:Int) extends Actor {
  
 val queue=new Queue[Triple]()
 
 def receive = {
   case Push(data) => 
     println("got this "+data)
     
     queue+=(data)
     if (queue.size>maxSize)
       queue.dequeue
   case Last(n) => 
      println("asking last")
      
      val ddd=(queue.takeRight(n)).toIterator
      //.mkString(","))
      
       sender ! ddd
       
   case t:Triple=>
     println(t)
    case s:Seq[Seq[Triple]]=>
      println("dribilin "+s.size)
      println(s.map{ss=>ss.mkString(",")}.mkString("\n"))
    }
   
}

case class Push(t:Triple)

case class PushMapping(m:Map[String,String])
case class Last(n:Int)
