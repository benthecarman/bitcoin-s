package org.bitcoins.tlv.node

import akka.NotUsed
import akka.stream.scaladsl.Source
import org.bitcoins.core.protocol.ln.node._
import org.bitcoins.core.protocol.tlv._

import scala.concurrent._

abstract class CustomMessageSubscriberApi {

  implicit def ec: ExecutionContext

  def nodeId: Future[NodeId]

  def connect(nodeUri: NodeUri): Future[Unit]

  def sendCustomMessage(nodeId: NodeId, tlv: TLV): Future[Unit]

  def subscribeCustomMessages(): Source[(NodeId, TLV), NotUsed]
}
