package ldnstream.core

import akka.actor._
import akka.stream.scaladsl.SourceQueueWithComplete

/*
class RdfStreamActor(q:SourceQueueWithComplete[String]) extends Actor{
  def receive = {
    case Push(data)=>
      q offer data
    case Pull =>
      
  }
}


case class Pull()
case class Push(data:String)
*/