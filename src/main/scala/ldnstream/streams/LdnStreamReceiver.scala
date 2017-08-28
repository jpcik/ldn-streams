package ldnstream.streams

import scala.collection.immutable.Seq
import scala.concurrent.duration.DurationInt
import scala.language.implicitConversions
import scala.language.postfixOps

import akka.NotUsed
import akka.actor._
import akka.http.scaladsl.marshalling.ToResponseMarshallable._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl._
import de.heikoseeberger.akkasse.scaladsl.marshalling.EventStreamMarshalling._
import de.heikoseeberger.akkasse.scaladsl.model.ServerSentEvent
import ldnstream.model._
import rdftools.rdf.RdfTools._
import rdftools.rdf.Triple
import ldnstream.core.OutputStreamActor

trait LdnStreamReceiver extends  StreamReceiver with LdnNode{ 
  import StatusCodes._
  
  lazy val serverIri:Uri=s"${base}streams"
  
  def headerAccepts(mType:MediaType)=headerValue(accepts(mType))
  
  def response(respMsg:ResponseMsg)=respMsg.status match{
    case OK=>HttpResponse(OK,Seq.empty[HttpHeader],HttpEntity(respMsg.msg.ct,respMsg.msg.body))
    case _ =>HttpResponse(respMsg.status,Seq.empty[HttpHeader],HttpEntity(respMsg.msg.ct,respMsg.msg.body))
  }
    
  def pathGetAllStreams=
    (get & extractUri & headerValue(extractAccept)) {(uri,ranges)=>
      complete{ 
        getAllStreams(ranges.head).map (response)
      }            
    }
  
  def pathPostInputStream=
    (post & extractRequestEntity & entity(as[String])) {(ct,entity)=>
      complete {        
        postInputStream(entity, ct.contentType) map (response)
      }
    }

  def pathGetStream(name:String)=
    (get & headerValue(extractAccept) & extractUri) {(ranges,uri)=>
      complete {
        getStream(name,ranges.head) map (response)
      }
    } 
    
  def pathPostQuery=
    (post & extractRequestEntity & entity(as[String])) {(req,entity)=>
      complete {
        val r=postQuery(entity,req.contentType)
        response(r)
      }
    }
 
  def pathRetrieveStreamItem=
    (get & headerValue(extractAccept) & extractUri) { (ranges,uri)=> parameters('size.?) {size=>
      complete{
        retrieveStreamItem(uri, size,ranges.head) map (response)
      }
    }}
       
  def pathPostStreamItem=
    (post & extractRequestEntity & extractUri & entity(as[String])) { (req,uri,pay) =>
      complete{ 
        val r=postStreamItem(uri, pay, req.contentType) 
        response(r)
      }  
    }
       
  def pathPushStreamItems(id:String)=
    (get & extractUri) {uri=>
      val (handler,qu)=pushStreamItems(uri)
      complete {
        val ss:Source[ServerSentEvent,Cancellable]=
            Source.tick(2.seconds, 2.seconds, NotUsed)
              .map{a =>  stringize(qu.dequeueAll(t=>true)).toString}
              .map{t=>ServerSentEvent(t)}
              .keepAlive(1.second, () => ServerSentEvent.heartbeat)
              .watchTermination(){(a,b)=>b.onComplete { x => terminatePush(id,handler) };a}
        ss               
      } 
    }
   
  val receiverRoute =      
    path("streams") {
      pathGetAllStreams ~
      pathPostInputStream
    } ~     
    path("streams" /  Remaining ){ name=>
      pathGetStream(name)  
    } ~
    path("streams" / "query") {
      pathPostQuery
    } ~ 
    path("streams"/ Remaining/ "inbox") {id=>
      pathPostStreamItem ~  
      pathRetrieveStreamItem
    } ~
    path("streams" / Remaining / "push") {id=>
      pathPushStreamItems(id)
    } 
  
  def testStream={
    val strategy=akka.stream.OverflowStrategy.fail
    val out=system actorOf(Props(new OutputStreamActor(10)))
    val src=   Source.queue[Triple](Int.MaxValue, strategy).groupedWithin(10000, 3 seconds).sliding(3, 1)
      .toMat(Sink.actorRef(out,{}))(Keep.both)
    
    val q= src.run()._1
    var co=0;
    system.scheduler.schedule(0 seconds, 1 second){co+=1;q.offer(Triple("a","b",lit(co)))}
  }
}