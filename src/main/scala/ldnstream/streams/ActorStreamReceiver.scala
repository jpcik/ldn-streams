package ldnstream.streams

import scala.concurrent.Future

import akka.actor.Actor
import akka.stream.ActorMaterializer

trait ActorStreamReceiver extends Actor with StreamReceiver{
  implicit val system=this.context.system
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
    case all:RetrieveAllStreams=>
      answer (getAllStreams(all.range))

    case str:CreateStream=>
      answer (postInputStream(str.msg.body,str.msg.ct))
      
    case str:RetrieveStream=>
      answer (getStream(str.uri, str.range))
      
    case msg:SendStreamItem=>
      answer (postStreamItem(msg.uri, msg.msg.body, msg.msg.ct))

    case r:RetrieveStreamItem=>
      retrieveStreamItem(r.uri, None, r.range)
      
    case q:CreateQuery=>
      answer (postQuery(q.msg.body, q.msg.ct))

    case p:PushStreamItems=>
      pushStreamItems(p.uri)
  }
}



