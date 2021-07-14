package org.bitcoins.dlc.wallet.networking.peer

import org.bitcoins.dlc.wallet.networking.P2PClient

import scala.concurrent.{Future, Promise}

sealed abstract class PeerMessageReceiverState {

  /** This promise gets completed when we receive a
    * [[akka.io.Tcp.Connected]] message from [[org.bitcoins.dlc.wallet.networking.P2PClient P2PClient]]
    */
  def clientConnectP: Promise[P2PClient]

  /** The [[org.bitcoins.dlc.wallet.networking.P2PClient P2PClient]] we are
    * connected to. This isn't initiated until the client
    * has called [[org.bitcoins.dlc.wallet.networking.peer.PeerMessageReceiver.connect() connect()]]
    */
  private val clientConnectF: Future[P2PClient] = clientConnectP.future

  /** This promise is completed in the [[org.bitcoins.dlc.wallet.networking.peer.PeerMessageReceiver.disconnect() disconnect()]]
    * when a [[org.bitcoins.dlc.wallet.networking.P2PClient P2PClient]] initiates a disconnections from
    * our peer on the p2p network
    */
  def clientDisconnectP: Promise[Unit]

  private val clientDisconnectF: Future[Unit] = clientDisconnectP.future

  /** If this future is completed, we are
    * connected to our client. Note, there is
    * no timeout on this future and no guarantee
    * that some one has actually initiated
    * a connection with a [[org.bitcoins.dlc.wallet.networking.P2PClient P2PClient]]
    * @return
    */
  def isConnected: Boolean = {
    clientConnectF.isCompleted && !clientDisconnectF.isCompleted
  }

  def isDisconnected: Boolean = {
    clientDisconnectF.isCompleted && !isConnected
  }
}

object PeerMessageReceiverState {

  /** Represents a [[org.bitcoins.dlc.wallet.networking.peer.PeerMessageReceiverState PeerMessageReceiverState]]
    * where the peer is not connected to the p2p network
    */
  final case object Preconnection extends PeerMessageReceiverState {
    def clientConnectP: Promise[P2PClient] = Promise[P2PClient]()

    //should this be completed since the client is disconnected???
    def clientDisconnectP: Promise[Unit] = Promise[Unit]()

    def toNormal(client: P2PClient): Normal = {
      val p = clientConnectP
      p.success(client)
      Normal(
        clientConnectP = p,
        clientDisconnectP = clientDisconnectP
      )
    }
  }

  /** This represents a [[org.bitcoins.dlc.wallet.networking.peer.PeerMessageReceiverState]]
    * where the peer has been fully initialized and is ready to send messages to
    * the peer on the network
    */
  case class Normal(
      clientConnectP: Promise[P2PClient],
      clientDisconnectP: Promise[Unit]
  ) extends PeerMessageReceiverState {
    require(
      isConnected,
      s"We cannot have a PeerMessageReceiverState.Normal if the Peer is not connected")

    override def toString: String = "Normal"
  }

  case class Disconnected(
      clientConnectP: Promise[P2PClient],
      clientDisconnectP: Promise[Unit]
  ) extends PeerMessageReceiverState {
    require(
      isDisconnected,
      "We cannot be in the disconnected state if a peer is not disconnected")

    override def toString: String = "Disconnected"

  }

  def fresh(): PeerMessageReceiverState.Preconnection.type = {
    PeerMessageReceiverState.Preconnection
  }

}
