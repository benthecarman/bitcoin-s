package org.bitcoins.tlv.node

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import grizzled.slf4j.Logging
import org.bitcoins.core.api.tlv.node.TLVNodeApi
import org.bitcoins.core.protocol.ln.node.{NodeId, NodeUri}
import org.bitcoins.core.protocol.tlv._

import scala.concurrent._

case class TLVNode(
    customMessageSubscriber: CustomMessageSubscriberApi,
    override val tlvHandler: (NodeId, TLV) => Future[Unit])(implicit
    system: ActorSystem)
    extends TLVNodeApi
    with Logging {
  implicit val ec: ExecutionContext = system.dispatcher

  override def start(): Future[Unit] = {
    val _ = customMessageSubscriber
      .subscribeCustomMessages()
      .map { case (nodeId, tlv) =>
        tlvHandler(nodeId, tlv)
      }
      .runWith(Sink.ignore)

    Future.unit
  }

  def nodeId: Future[NodeId] = customMessageSubscriber.nodeId

  def connectAndSendToPeer(nodeUri: NodeUri, message: TLV): Future[Unit] = {
    for {
      _ <- customMessageSubscriber.connect(nodeUri)
      _ <- sendToPeer(nodeUri.nodeId, message)
    } yield ()
  }

  def sendToPeer(nodeId: NodeId, message: TLV): Future[Unit] = {
    customMessageSubscriber.sendCustomMessage(nodeId, message)
  }

  override def stop(): Future[Unit] = Future.unit
}
