package org.bitcoins.dlc.wallet.networking.peer

import akka.actor.ActorRefFactory
import grizzled.slf4j.Logging
import org.bitcoins.core.protocol.tlv._
import org.bitcoins.dlc.wallet.networking.P2PClient
import org.bitcoins.dlc.wallet.networking.peer.PeerMessageReceiverState._

import scala.concurrent.Future

/** Responsible for receiving messages from a peer on the
  * p2p network. This is the entry point for handling all received
  * DLC Messages
  */
class PeerMessageReceiver(
    dlcMessageHandler: DLCMessageHandler,
    val state: PeerMessageReceiverState,
    peer: Peer
)(implicit ref: ActorRefFactory)
    extends Logging {
  import ref.dispatcher

  /** This method is called when we have received
    * a [[akka.io.Tcp.Connected]] message from our peer
    * This means we have opened a Tcp connection,
    * but have NOT started the handshake
    * This method will initiate the handshake
    */
  protected[networking] def connect(client: P2PClient): PeerMessageReceiver = {

    state match {
      case bad @ (_: Normal | _: Disconnected) =>
        throw new RuntimeException(s"Cannot call connect when in state=$bad")
      case Preconnection =>
        logger.info(s"Connection established with peer=$peer")

        val newState = Preconnection.toNormal(client)
        val newRecv = toState(newState)

        newRecv
    }
  }

  protected[networking] def disconnect(): PeerMessageReceiver = {
    logger.trace(s"Disconnecting with internal state=$state")
    state match {
      case bad @ (_: Disconnected | Preconnection) =>
        throw new RuntimeException(
          s"Cannot disconnect from peer=$peer when in state=$bad")

      case good: Normal =>
        logger.debug(s"Disconnected bitcoin peer=$peer")
        val newState = Disconnected(
          clientConnectP = good.clientConnectP,
          clientDisconnectP = good.clientDisconnectP.success(())
        )

        val newRecv = toState(newState)

        newRecv
    }
  }

  private[networking] def isConnected: Boolean = state.isConnected

  private[networking] def isDisconnected: Boolean = state.isDisconnected

  def handleLnMessageReceived(
      networkMsgRecv: PeerMessageReceiver.LnMessageReceived): Future[
    PeerMessageReceiver] = {

    val client = networkMsgRecv.client

    //create a way to send a response if we need too
    val peerMsgSender = PeerMessageSender(client)

    logger.debug(
      s"Received message=${networkMsgRecv.msg.typeName} from peer=${client.peer} state=$state")
    networkMsgRecv.msg.tlv match {
      case setupTLV: DLCSetupTLV =>
        dlcMessageHandler.handlePayload(setupTLV, peerMsgSender).map {
          newHandler =>
            new PeerMessageReceiver(newHandler, state, peer)
        }
      case unhandled @ (_: UnknownTLV | _: ErrorTLV | _: PingTLV | _: PongTLV |
          _: DLCSetupPieceTLV | _: DLCOracleTLV) =>
        logger.debug(s"Received unhandled message: $unhandled")
        Future.successful(this)
    }
  }

  /** Transitions our PeerMessageReceiver to a new state */
  def toState(newState: PeerMessageReceiverState): PeerMessageReceiver = {
    new PeerMessageReceiver(
      dlcMessageHandler = dlcMessageHandler,
      state = newState,
      peer = peer
    )
  }
}

object PeerMessageReceiver {

  sealed abstract class PeerMessageReceiverMsg {

    /** Who we need to use to send a reply to our peer
      * if a response is needed for this message
      */
    def client: P2PClient
  }

  case class LnMessageReceived(msg: LnMessage[TLV], client: P2PClient)
      extends PeerMessageReceiverMsg

  def apply(
      state: PeerMessageReceiverState,
      dlcMessageHandler: DLCMessageHandler,
      peer: Peer)(implicit
      ref: ActorRefFactory
  ): PeerMessageReceiver = {
    new PeerMessageReceiver(dlcMessageHandler: DLCMessageHandler,
                            state = state,
                            peer = peer)
  }

  /** Creates a peer message receiver that is ready
    * to be connected to a peer. This can be given to [[org.bitcoins.dlc.wallet.networking.P2PClient.props() P2PClient]]
    * to connect to a peer on the network
    */
  def preConnection(peer: Peer, dlcMessageHandler: DLCMessageHandler)(implicit
      ref: ActorRefFactory
  ): PeerMessageReceiver = {
    PeerMessageReceiver(dlcMessageHandler = dlcMessageHandler,
                        state = PeerMessageReceiverState.fresh(),
                        peer = peer)
  }

  def newReceiver(dlcMessageHandler: DLCMessageHandler, peer: Peer)(implicit
      ref: ActorRefFactory): PeerMessageReceiver = {
    PeerMessageReceiver(dlcMessageHandler = dlcMessageHandler,
                        state = PeerMessageReceiverState.fresh(),
                        peer = peer)
  }
}
