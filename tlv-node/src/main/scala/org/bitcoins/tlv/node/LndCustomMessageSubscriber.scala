package org.bitcoins.tlv.node

import akka.NotUsed
import akka.stream.scaladsl.Source
import grizzled.slf4j.Logging
import org.bitcoins.core.protocol.ln.node.{NodeId, NodeUri}
import org.bitcoins.core.protocol.tlv.TLV
import org.bitcoins.lnd.rpc.LndRpcClient

import scala.concurrent.{ExecutionContext, Future}

case class LndCustomMessageSubscriber(lndRpc: LndRpcClient)
    extends CustomMessageSubscriberApi
    with Logging {
  implicit override val ec: ExecutionContext = lndRpc.executionContext

  override def nodeId: Future[NodeId] = lndRpc.nodeId

  override def connect(nodeUri: NodeUri): Future[Unit] =
    lndRpc.connectPeer(nodeUri)

  override def sendCustomMessage(nodeId: NodeId, tlv: TLV): Future[Unit] = {
    lndRpc.sendCustomMessage(nodeId, tlv)
  }

  override def subscribeCustomMessages(): Source[(NodeId, TLV), NotUsed] =
    lndRpc.subscribeCustomMessages()

}
