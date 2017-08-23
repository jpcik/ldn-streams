package ldnstream.streams

import java.io._

import scala.collection.JavaConverters._
import scala.collection.mutable.Queue
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.sparql.graph.GraphFactory

import akka.actor._
import akka.http.scaladsl.model._
import akka.pattern.ask
import akka.stream.scaladsl.SourceQueueWithComplete
import akka.util.Timeout
import ldnstream.core._
import ldnstream.model._
import ldnstream.vocab.LDP
import ldnstream.vocab.RSPS
import rdftools.rdf._
import rdftools.rdf.RdfSchema._
import rdftools.rdf.api.JenaGraphs
import rdftools.rdf.api.JenaTools._
import rdftools.rdf.vocab.RDFS
import spray.json._
import org.apache.jena.rdf.model.Model

trait StreamReceiver extends LdnEntity{
  import StatusCodes._
  import LdnTypes._
  import RdfTools._
  import StreamMsg._

  val serverIri:Uri//=s"${base}streams"

  private lazy val streamsHandler=system.actorOf(Props(new StreamsHandler(serverIri)))
  
  def query(name:String,queryStr:String,
        queue:SourceQueueWithComplete[Map[String,String]]):Unit
  def consumeGraph(uri:Uri,g:Graph):Unit
  def push(id:String,qu:Queue[Map[String,String]]):ResultHandler
  def terminatePush(id:String,hnd:ResultHandler):Unit
  
  case class ResultHandler(native:Any)
  
  implicit val timeout=Timeout(5 seconds)
  
  private def error(error:Error)={
     val payload=s"""{
       "@context": "http://example.org/",
       "@type": "Error",
       "message": "${error.msg}",
       "code": 500
       }"""
    
    ErrorMsg(Msg(payload),StatusCodes.InternalServerError)
  }
  
  private def ok(payload:String,contentType:ContentType.NonBinary=`application/ld+json`)=
    OkMsg(Msg(payload,contentType))
    
  private def asRdf(stream:RdfStream)={
    implicit val m=ModelFactory.createDefaultModel
    +=(serverIri,LDP.contains,stream.uri)
    if (stream.inputUri.isDefined)
      +=(stream.uri,RSPS.input,stream.inputUri.get)
    if (stream.outputUri.isDefined)
      +=(stream.uri,RSPS.output,stream.outputUri.get) 
    m
  }
  
  private def allStreams(lang:Lang)={
    implicit val m=ModelFactory.createDefaultModel
    val streamsFut=(streamsHandler ? FetchAllStreams).mapTo[Seq[RdfStream]]
    println("now all")
    streamsFut.map{streams=>
      streams foreach { str=> m.add(asRdf(str)) }
      writeRdf(m)(lang)   
    }
  }
    
  private def fetchStream(uri:Uri)(implicit lang:Lang)={
    val streamFut=(streamsHandler ? FetchStream(uri)).mapTo[Option[RdfStream]]
    streamFut.map{op=>op.map { str=> 
      val m=asRdf(str)
      writeRdf(m)
    }}   
  }
 
  val supportedTypes=Seq(`application/ld+json`,`text/turtle`)
  
  private def matching(range:MediaRange)={
    supportedTypes.find { mt=> range.matches(mt) }
  }
  
  private def matching(ct:ContentType)={
    supportedTypes find {mt=>mt.matches(ct.mediaType)}
  }
  
  private def mediaTypeNotSupported(range:MediaRange)=
    ErrorMsg(Msg(s"Media type not supported: $range"),BadRequest)
    
  private def mediaTypeNotSupported(ct:ContentType)=
    ErrorMsg(Msg(s"Media type not supported: $ct"),BadRequest)
  
  def getAllStreams(range:MediaRange):Future[ResponseMsg] = 
    matching(range) match {
      case Some(mt)=>allStreams(toRdfLang(mt)).map{pay=>
        println("body "+pay)
        ok(pay,mt)
      }
      case None => Future(mediaTypeNotSupported(range))           
    }
  
  private def parseStreamName(json:String)(implicit lang:Lang)={
    val m=loadRdf(json)
    val stat=m.listStatements(serverIri.toString:Iri, LDP.contains, null)
    val streamIri=stat.asScala.toSeq.head.getObject
    val stm=m.listStatements(streamIri.asResource, RDFS.label, null).asScala.toSeq.head
    val name=stm.getObject.asLiteral().getString
    name
  }
  
  def postInputStream(entity:String,ct:ContentType):Future[ResponseMsg]=
    matching(ct) match {
      case None => Future(mediaTypeNotSupported(ct))
      case Some(mt)=>
        implicit val lang=toRdfLang(ct)
        val streamName=parseStreamName(entity)
        val streamUri=s"$serverIri/$streamName"
        val inputIri=s"$streamUri/input"
        val inputStr=RdfInputStream(streamUri,inputIri)
        (streamsHandler ? NewStream(inputStr)).map {
          case Ok(msg) => ok(streamUri)
          case e:Error => error(e)
        }
    }
  
  def getStream(uri:Uri,range:MediaRange):Future[ResponseMsg]= 
    matching(range) match {
      case Some(mt)=>fetchStream(uri)(toRdfLang(mt)).map{
        case Some(body)=>ok(body,mt)
        case None=>ErrorMsg(Msg(s"Stream not found: $uri"),NotFound)
      }
      case None => Future(mediaTypeNotSupported(range)) 
    } 
    
  private def getQuerySource(streamName:String)={
    val out=system actorOf(Props(new OutputStreamActor(10)))
    val queryUri=s"${serverIri}/$streamName"
    val outputUri=s"${serverIri}/$streamName/output"
    val outputStr=RdfOutputStream(queryUri,outputUri,out)
    val streamQueue=RdfStream.createMappingSource(outputStr).run()._1  
    streamsHandler ! NewStream(outputStr)
    streamQueue
  }
 
  private def parseQuery(json:String)(implicit lang:Lang)={
    val m=loadRdf(json)
    val stm=m.listStatements(null, RSPS.query, null).asScala.toSeq.head
    val query=stm.getObject.asLiteral().getString
    val stm2=m.listStatements(stm.getSubject,RDFS.label,null).asScala.toSeq.head
    val name=stm2.getObject.asLiteral.getString
    (name,query)
  }
    
  def postQuery(entity:String,ct:ContentType):ResponseMsg=
    matching(ct) match {
      case None => mediaTypeNotSupported(ct)
      case Some(mt) => 
        implicit val lang=toRdfLang(ct)
        val (streamName,q)=parseQuery(entity)
        val streamUri=s"${serverIri}/$streamName"      
        val queue=getQuerySource(streamName)
        query(streamName,q,queue)
        ok(streamUri)
  }
 
  private def writeRdf(trips:Iterator[Triple])(implicit lang:Lang)={
    val sw=new StringWriter
    val g =GraphFactory.createDefaultGraph
    trips.foreach { t => g.add(t) }
    RDFDataMgr.write(sw, g, lang)
    sw.toString
  }
   
  def retrieveStreamItem(uri:Uri,size:Option[String],range:MediaRange)=
    matching(range) match {
      case None=>Future(mediaTypeNotSupported(range))
      case Some(mt) =>
        implicit val lang=toRdfLang(mt)
        
        println("get this item " +uri)
        val streamUri=uri.toString.replace("/output","")
        val strr=(streamsHandler ? FetchStream(streamUri)).mapTo[Option[RdfStream]].flatMap {ss=>
          val pip=(ss.get.outputRef.get ? Last(size.getOrElse("10").toInt)).mapTo[Iterator[Triple]]
          pip.map(a=>ok(writeRdf(a)))
        }
        strr
    }
        
  import JenaGraphs._
  def postStreamItem(uri:Uri,pay:String,ct:ContentType):ResponseMsg=
    matching(ct) match {
      case None => mediaTypeNotSupported(ct)
      case Some(mt) => 
        println("feed this rdf: "+pay)
        val m=loadRdf(pay)(toRdfLang(ct))
        consumeGraph(uri,m.getGraph)
        ok("done")
    }
           
     
  def pushStreamItems(uri:Uri)={
    val streamUri=uri.toString.replace("streams/push","streams")
    val qu:Queue[collection.immutable.Map[String,String]]=new Queue[Map[String,String]]
    val handler=push(uri.toString,qu)
    (handler,qu)          
      
  }
    
}