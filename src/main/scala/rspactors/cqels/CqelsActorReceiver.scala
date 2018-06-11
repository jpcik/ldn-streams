package rspactors.cqels

import akka.http.scaladsl.model.Uri
import rspactors.ActorStreamReceiver

class CqelsActorReceiver(iri:String,val id:Int) 
  extends ActorStreamReceiver with CqelsReceiver{

  val serverIri=Uri(iri)
 
}