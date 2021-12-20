package org.bitcoins.core.api.tlv.node

import org.bitcoins.core.protocol.ln.node.{NodeId, NodeUri}
import org.bitcoins.core.protocol.tlv._
import org.bitcoins.core.util.StartStopAsync

import scala.concurrent._

trait TLVNodeApi extends StartStopAsync[Unit] {

  def tlvHandler: (NodeId, TLV) => Future[Unit]

  def nodeId: Future[NodeId]

  def connectAndSendToPeer(nodeUri: NodeUri, message: TLV): Future[Unit]

  def sendToPeer(nodeId: NodeId, message: TLV): Future[Unit]
}
