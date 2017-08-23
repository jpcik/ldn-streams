package ldnstream

import scala.concurrent.Future
import akka.http.scaladsl.model.Uri

object ConsumerTest {
  import concurrent.ExecutionContext.Implicits.global
  
  def printInbox(targetUri:String)={
    LdnClient.discover(targetUri).map{_.map{inbox=>
      println(s"target: $targetUri")
      println(s"inbox: ${inbox.inboxUri}")
    }}
  }
  
  def printNotificationUris(targetUri:String)={ 
    LdnClient.discover(targetUri).map{_.map{inbox=>
      println(s"notification uris for target: $targetUri")
      val uris=LdnClient.getNotificationUris(inbox.inboxUri)
      uris.map { seq => println(seq.mkString(" ")) }
    }}
  }
  
  def printNotifications(targetUri:String)={
    LdnClient.discover(targetUri).map{_.map{inbox=>
      val uris=LdnClient.getNotificationUris(inbox.inboxUri)
      println(s"notifications for target $targetUri")
      uris.map { _.foreach { uri => 
          LdnClient.getNotification(Uri(uri)).map{notif=>
            println(s"notificaiton uri: $uri")
            println(notif)
          }          
      }}
    }}    
  }
  
  // Impl report inbox: https://linkedresearch.org/ldn/tests/reports/5edc5f50-55bd-11e7-a741-cb560322e93d
  // consumer impl report: https://linkedresearch.org/ldn/tests/reports/4608d2a0-55be-11e7-a741-cb560322e93d
  
  def main(args:Array[String])={
    val targetA="https://linkedresearch.org/ldn/tests/discover-inbox-link-header"
    val targetB="https://linkedresearch.org/ldn/tests/discover-inbox-rdf-body"
    //printInbox(targetB)
    //printInbox(targetA)
    //printNotificationUris(targetA)
    //printNotificationUris(targetB)
    //printNotifications(targetA)
    //printNotifications(targetB)
    
   
    
  }
}