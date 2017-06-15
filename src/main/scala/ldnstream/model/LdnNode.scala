package ldnstream.model

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.HttpCharsets
import akka.http.scaladsl.model.MediaType.WithFixedCharset
import akka.http.scaladsl.model.MediaType
import collection.JavaConversions._

trait LdnNode extends LdnTypes{
  def host:String
  def port:Int
  def base=
    if (port==0) s"http://${host}/"
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
  
  def matchWithoutParams(cType:ContentType,mType:MediaType)={
    val m=cType.mediaType
      println("type "+m.mainType+" "+m.subType)
      println("params: "+m.params.keys.mkString)  
    m.mainType==mType.mainType && m.subType==mType.subType
  }
  
}

object LdnVocab {
  val inbox="http://www.w3.org/ns/ldp#inbox"
  val contains="http://www.w3.org/ns/ldp#contains"
}

trait LdnTypes {
  private val utf8 = HttpCharsets.`UTF-8`
  val `text/turtle`: WithFixedCharset =
     MediaType.customWithFixedCharset("text", "turtle", utf8)
  val `application/ld+json`: WithFixedCharset =
     MediaType.customWithFixedCharset("application", "ld+json", utf8)  
}
