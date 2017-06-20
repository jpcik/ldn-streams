package ldnstream

import ldnstream.model.LdnSender

object SenderTest {
  
  
  // implementation report: https://linkedresearch.org/ldn/tests/reports/ed5d3ce0-55bd-11e7-a741-cb560322e93d
  
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
    LdnClient.discoverAndSend(testUrlLink,payload)
    //LdnClient.discoverAndSend(testUrlRdf, payload)
    
  }
  
}