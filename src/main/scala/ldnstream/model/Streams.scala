package ldnstream.model

import akka.http.scaladsl.model.Uri
import akka.actor.ActorRef
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Keep
import ldnstream.core.Push
import rdftools.rdf.Triple
import ldnstream.core.PushMapping

case class RdfStream(uri:Uri,inputUri:Option[Uri],outputUri:Option[Uri],
    outputRef:Option[ActorRef]=None) {
}
  
object RdfInputStream{
  def apply(uri:Uri,input:Uri)=
    RdfStream(uri,Some(input),None,None)
}

object RdfOutputStream{
  def apply(uri:Uri,output:Uri,ref:ActorRef)=
    RdfStream(uri,None,Some(output),Some(ref))
}

object RdfStream {
  
  val strategy=akka.stream.OverflowStrategy.fail

  private def createRdfStreamSource[T](stream:RdfStream,f:T=>Any)={
    if (stream.outputRef.isEmpty) throw new IllegalArgumentException("Stream has no output actor ref")
    Source.queue[T](Int.MaxValue, strategy)
    .map(f).toMat(Sink.actorRef(stream.outputRef.get,{}))(Keep.both)
  }
  
  
  def createTripleSource(stream:RdfStream)=
    createRdfStreamSource[Triple](stream,Push)
  
  def createMappingSource(stream:RdfStream)=
    createRdfStreamSource[Map[String,String]](stream,PushMapping)
  
}