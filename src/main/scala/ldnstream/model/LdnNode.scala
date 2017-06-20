package ldnstream.model

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.HttpCharsets
import akka.http.scaladsl.model.MediaType.WithFixedCharset
import akka.http.scaladsl.model.MediaType
import akka.http.scaladsl.model.HttpMethods._

import collection.JavaConversions._
import org.apache.jena.riot.Lang
import akka.http.scaladsl.Http
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import concurrent.duration._
import java.io.StringReader
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.rdf.model.ModelFactory
import ldnstream.ReceiverRef
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.language.implicitConversions
import org.apache.jena.rdf.model.Property

trait LdnNode extends LdnTypes{
  implicit val system:ActorSystem
  implicit val materializer:ActorMaterializer
  implicit val ctx:ExecutionContext

  def host:String
  def port:Int
  def base=
    if (host!="localhost") s"http://${host}/"
    else s"http://${host}:${port}/"  
    
  implicit def toJenaRef(uri:Uri)=
    ResourceFactory.createResource(uri.toString)
  
  implicit def toJenaProp(uri:String):Property=
    ResourceFactory.createProperty(uri)
    
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
  
  def matchWithoutParams(cType:ContentType,mType:MediaType)={
    val m=cType.mediaType
      println("type "+m.mainType+" "+m.subType)
      println("params: "+m.params.keys.mkString)  
    m.mainType==mType.mainType && m.subType==mType.subType
  }
  
  def toRdfLang(mediaType:MediaType)= mediaType match{
    case `text/turtle` => Lang.TURTLE
    case `application/ld+json` => Lang.JSONLD
    case _ => Lang.JSONLD
  }
  
  def findInbox(links:Seq[Link])={
    links.find (_.values.find ( _.params.find { linkPar => 
      linkPar.key=="rel" && linkPar.value==LdnVocab.inbox }
      .isDefined ).isDefined ).map(v=>ReceiverRef(v.values.head.uri))
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
      val inboxProp=ResourceFactory.createProperty(LdnVocab.inbox)
      val inboxNode= m.listStatements(null, inboxProp, null).toSeq.headOption
      inboxNode.map(stm=>ReceiverRef(Uri(stm.getObject.asResource.getURI)))
    }}
  }
}

object LdnVocab {
  val inbox="http://www.w3.org/ns/ldp#inbox"
  val contains="http://www.w3.org/ns/ldp#contains"
  val Container="http://www.w3.org/ns/ldp#Container"
}

trait LdnTypes {
  private val utf8 = HttpCharsets.`UTF-8`
  val `text/turtle`: WithFixedCharset =
     MediaType.customWithFixedCharset("text", "turtle", utf8)
  val `application/ld+json`: WithFixedCharset =
     MediaType.customWithFixedCharset("application", "ld+json", utf8)  
}
