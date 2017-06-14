package ldnstream

import ldnstream.model.LdnSender

object SenderTest {
  val base="https://linkedresearch.org/ldn/tests"
  val id="69b347f0-4c93-11e7-a741-cb560322e93d"
  val testUrlLink=s"$base/target/${id}?discovery=link-header"
  val testUrlRdf= s"$base/target/${id}?discovery=rdf-body"
  val testUrlInbox=s"$base/inbox-sender/?id=${id}&discovery=link-header"
  val payload="""
{
  "@context": "https://www.w3.org/ns/activitystreams",
  "@id": "",
  "@type": "Announce",
  "actor": "https://jeanpi.org/#me",
  "object": "http://example.net/note",
  "target": "http://example.org/article",
  "updated": "2016-06-28T19:56:20.114Z"
}
    """
  def main(args:Array[String]):Unit={
    val p= new LdnSender{} 
    p.discoverAndSend(testUrlLink,payload)
    p.discoverAndSend(testUrlRdf, payload)
    //LdnSender.send(testUrlInbox, payload)
  }
  
}