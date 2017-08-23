package ldnstream.model

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.HttpCharsets
import akka.http.scaladsl.model.MediaType.WithFixedCharset
import akka.http.scaladsl.model.MediaType
import akka.http.scaladsl.model.HttpMethods._

import collection.JavaConverters._
import org.apache.jena.riot.Lang
import akka.http.scaladsl.Http
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import concurrent.duration._
import java.io.StringReader
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.rdf.model.ModelFactory
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.language.implicitConversions
import org.apache.jena.rdf.model.Property
import ldnstream.vocab.LDP
import rdftools.rdf.api.JenaTools
import ldnstream.Receiver
import rdftools.rdf.Iri

trait LdnNode extends LdnEntity{

  import LdnTypes._
  def host:String
  def port:Int
  def base=
    if (host!="localhost") s"http://${host}/"
    else s"http://${host}:${port}/"  

  def getLinks(res:HttpResponse)=
    res.headers.filter(_.is("link")).map(_.asInstanceOf[Link])

  def accepts(accept:Accept,mediaType:MediaType)={
    val types=Seq(MediaRange(mediaType))
  
    //println(accept.mediaRanges.head.getParams().mkString)
    !accept.mediaRanges.intersect(types).isEmpty
  }
  
  def accepts(mType:MediaType): HttpHeader => Option[String] = {
    case h: headers.Accept => 
      if (accepts(h,mType)) Some("") 
      else None
    case x  => None
  }
  
  def extractAccept: HttpHeader => Option[Seq[MediaRange]] = {
    case h:headers.Accept =>
      Some(h.mediaRanges)
    case _ => None
  }
  
  def matchWithoutParams(cType:ContentType,mType:MediaType)={
    val m=cType.mediaType
      println("type "+m.mainType+" "+m.subType)
      println("params: "+m.params.keys.mkString)  
    m.mainType==mType.mainType && m.subType==mType.subType
  }
  
  def findInbox(links:Seq[Link])={
    links.find (_.values.find ( _.params.find { linkPar => 
      linkPar.key=="rel" && linkPar.value==LDP.inbox.iri.path }
      .isDefined ).isDefined ).map(v=>Receiver(v.values.head.uri))
  }
  
  def discover(url:String)={
    discoverByLink(url).flatMap {uriOpt=> 
      if (uriOpt.isEmpty)
        discoverByRdf(url,`text/turtle`)
      else Future(uriOpt)
    }
  }

  def discoverByLink(url:String)={
    val resp = Http().singleRequest(HttpRequest(HEAD,url))    
    resp.map{res=>
      println(res.status)
      val inbox=findInbox(getLinks(res))
      println(inbox)
      inbox
    }
  }
    
  def discoverByRdf(url:String,mediaType:MediaType)={
    val lang=toRdfLang(mediaType)
    val resp = Http().singleRequest(HttpRequest(GET,url,List(Accept(mediaType))))
    resp.flatMap{r=>r.entity.toStrict(5.second).map { e=>e.data}.map(_.utf8String).map { pay =>
      val m=ModelFactory.createDefaultModel
      val sr=new StringReader(pay)
      RDFDataMgr.read(m, sr, "",lang)
      val inboxProp=JenaTools.toJenaProperty(LDP.inbox)
      val inboxNode= m.listStatements(null, inboxProp, null).asScala.toSeq.headOption
      inboxNode.map(stm=>Receiver(Uri(stm.getObject.asResource.getURI)))
    }}
  }
}


