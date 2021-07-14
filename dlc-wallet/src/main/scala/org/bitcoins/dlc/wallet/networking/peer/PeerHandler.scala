package org.bitcoins.dlc.wallet.networking.peer

import org.bitcoins.dlc.wallet.networking.P2PClient

case class PeerHandler(p2pClient: P2PClient, peerMsgSender: PeerMessageSender)
