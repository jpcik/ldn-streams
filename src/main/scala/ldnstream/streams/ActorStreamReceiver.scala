package ldnstream.streams

import akka.actor.Actor
import akka.stream.ActorMaterializer
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.ContentType
import ldnstream.core.NewStream
import akka.http.scaladsl.model.MediaRange
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes

trait ActorStreamReceiver extends Actor with StreamReceiver{
  implicit val system=this.context.system
  implicit val materializer:ActorMaterializer= ActorMaterializer()
  
  def receive = {
    case all:RetrieveAllStreams=>
      getAllStreams(all.range).map{tup=>
        sender ! tup
      }
    case str:CreateStream=>
      postInputStream(str.msg.body,str.msg.ct).map{resp=>
        sender ! resp
      }
    case str:RetrieveStream=>
      getStream(str.uri, str.range).map{r=>
        sender ! r
      }
    case msg:SendStreamItem=>
      val r=postStreamItem(msg.uri, msg.msg.body, msg.msg.ct)
      sender ! r
    case q:CreateQuery=>
      val r=postQuery(q.msg.body, q.msg.ct)
      sender ! r
    case p:PushStreamItems=>
      pushStreamItems(p.uri)
  }
}



