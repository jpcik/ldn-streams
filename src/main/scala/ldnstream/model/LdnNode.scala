package ldnstream.model

import java.io.StringReader

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.implicitConversions

import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.RDFDataMgr

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.headers._
import ldnstream.Receiver
import rdftools.rdf.jena._
import rspactors.vocab.LDP

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
      val inboxProp=toJenaProperty(LDP.inbox)
      val inboxNode= m.listStatements(null, inboxProp, null).asScala.toSeq.headOption
      inboxNode.map(stm=>Receiver(Uri(stm.getObject.asResource.getURI)))
    }}
  }
}


