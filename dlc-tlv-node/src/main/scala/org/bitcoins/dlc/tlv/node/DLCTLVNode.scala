package org.bitcoins.dlc.tlv.node

import akka.actor.ActorSystem
import grizzled.slf4j.Logging
import org.bitcoins.core.api.dlc.wallet.DLCWalletApi
import org.bitcoins.core.protocol.ln.node.{NodeId, NodeUri}
import org.bitcoins.core.protocol.tlv._
import org.bitcoins.core.util.StartStopAsync
import org.bitcoins.tlv.node.{CustomMessageSubscriberApi, TLVNode}

import scala.concurrent._

case class DLCTLVNode(
    dlcWalletApi: DLCWalletApi,
    customMessageSubscriber: CustomMessageSubscriberApi)(implicit
    system: ActorSystem)
    extends StartStopAsync[Unit]
    with Logging {
  implicit val ec: ExecutionContext = system.dispatcher

  val tlvNode: TLVNode = TLVNode(customMessageSubscriber, handleTLV)

  override def start(): Future[Unit] = tlvNode.start()

  override def stop(): Future[Unit] = Future.unit

  def acceptDLCOffer(
      peer: NodeUri,
      dlcOffer: LnMessage[DLCOfferTLV]): Future[Unit] = {
    for {
      accept <- dlcWalletApi.acceptDLCOffer(dlcOffer.tlv)
      _ <- tlvNode.connectAndSendToPeer(peer, accept.toTLV)
    } yield ()
  }

  def handleTLV(nodeId: NodeId, tlv: TLV): Future[Unit] = {
    tlv match {
      // ignore these messages
      case msg @ (_: UnknownTLV | _: DLCOracleTLV | _: DLCSetupPieceTLV |
          _: InitTLV | _: ErrorTLV | _: PingTLV | _: PongTLV) =>
        logger.warn(s"Received unhandled tlv $msg")
        Future.unit
      case _: DLCOfferTLV =>
        // We don't want to accept an offer without user confirmation
        // maybe can prompt to the user in the future
        Future.unit
      case dlcAccept: DLCAcceptTLV =>
        for {
          sign <- dlcWalletApi.signDLC(dlcAccept)
          _ <- customMessageSubscriber.sendCustomMessage(nodeId, sign.toTLV)
        } yield ()
      case dlcSign: DLCSignTLV =>
        for {
          _ <- dlcWalletApi.addDLCSigs(dlcSign)
          _ <- dlcWalletApi.broadcastDLCFundingTx(dlcSign.contractId)
        } yield ()
    }
  }
}
