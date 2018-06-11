package rspactors.core

import akka.actor._
import scala.collection.mutable.Queue
import rdftools.rdf.Triple

class OutputStreamActor(maxSize:Int) extends Actor {
  
 val queue=new Queue[Triple]()
 val mappingQueue=new Queue[Map[String,String]]()
 
 def receive = {
   case Push(data) => 
     println("got this "+data)     
     queue+=(data)
     if (queue.size>maxSize)
       queue.dequeue
   
   case PushMapping(data)=>  
     mappingQueue+=(data)
     if (mappingQueue.size>maxSize)
       mappingQueue.dequeue
   
   case Last(n) => 
     println("asking last")
      
     val ddd=(mappingQueue.takeRight(n)).toIterator.mkString(",")
     println("tobias "+ddd)
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
