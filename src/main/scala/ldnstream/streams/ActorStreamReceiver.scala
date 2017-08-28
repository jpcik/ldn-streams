package ldnstream.streams

import scala.concurrent.Future
import concurrent.duration._
import language.postfixOps

import akka.actor.Actor
import akka.stream.ActorMaterializer

trait ActorStreamReceiver extends Actor with StreamReceiver{
  implicit val system=context.system
  implicit val materializer:ActorMaterializer= ActorMaterializer()

  def answer(f:Future[ResponseMsg])={
    val s=sender
    f .map{a=> s ! a}        
  }

  def answer(f:ResponseMsg)={
    val s=sender
    s ! f        
  }

  def receive = {
    case req:RetrieveAllStreams=>
      answer (getAllStreams(req.range))

    case req:CreateStream=>
      answer (postInputStream(req.msg.body,req.msg.ct))
      
    case req:RetrieveStream=>
      answer (getStream(req.uri, req.range))
      
    case req:SendStreamItem=>
      answer (postStreamItem(req.uri, req.msg.body, req.msg.ct))

    case r:RetrieveStreamItem=>
      retrieveStreamItem(r.uri, None, r.range)
      
    case q:CreateQuery=>
      answer (postQuery(q.msg.body, q.msg.ct))

    case p:PushStreamItems=>
      val s=sender
      val (handler,qu)=pushStreamItems(p.uri)
      context.system.scheduler.schedule(0 seconds,5 seconds){
        p.actor ! ok(stringize(qu.dequeueAll(a=>true)))
      }
      
  }
}



