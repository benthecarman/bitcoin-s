package org.bitcoins.cln

import cln.NewaddrRequest.NewaddrAddresstype
import org.bitcoins.core.number._
import org.bitcoins.core.protocol._
import org.bitcoins.core.protocol.ln.node.NodeId
import org.bitcoins.crypto.ECPublicKey
import org.bitcoins.testkit.fixtures.CLNFixture

class ClnRpcClientTest extends CLNFixture {

  it must "get info" in { client =>
    for {
      info <- client.getInfo
    } yield {
      assert(info.numPeers == UInt32.zero)
      assert(info.blockheight >= UInt32.zero)
      assert(info.id.nonEmpty)
      assert(info.version == ClnRpcClient.version)
    }
  }

  it must "get node id" in { client =>
    for {
      id <- client.nodeId
    } yield {
      assert(id.pubKey.isFullyValid)
    }
  }

  it must "get addresses" in { client =>
    for {
      addr1 <- client.getNewAddress()
      addr2 <- client.getNewAddress(NewaddrAddresstype.BECH32)
      addr3 <- client.getNewAddress(NewaddrAddresstype.P2SH_SEGWIT)
    } yield {
      assert(addr1.isInstanceOf[Bech32Address])
      assert(addr2.isInstanceOf[Bech32Address])
      assert(addr3.isInstanceOf[P2SHAddress])
    }
  }

  it must "fail to find a peer" in { client =>
    for {
      peerOpt <- client.findPeer(NodeId(ECPublicKey.freshPublicKey))
    } yield assert(peerOpt.isEmpty)
  }

  it must "list funds" in { client =>
    for {
      funds <- client.listFunds()
    } yield {
      assert(funds.outputs.isEmpty)
      assert(funds.channels.isEmpty)
    }
  }

  it must "list txs" in { client =>
    for {
      txs <- client.listTransactions()
    } yield assert(txs.isEmpty)
  }
}
