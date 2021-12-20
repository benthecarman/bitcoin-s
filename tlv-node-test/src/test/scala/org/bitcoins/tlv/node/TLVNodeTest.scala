package org.bitcoins.tlv.node

import org.bitcoins.core.protocol.BigSizeUInt
import org.bitcoins.core.protocol.ln.node.NodeId
import org.bitcoins.core.protocol.tlv._
import org.bitcoins.testkit.fixtures.DualLndFixture
import scodec.bits._

import scala.concurrent.{Future, Promise}

class TLVNodeTest extends DualLndFixture {

  it must "send and receive tlv messages" in { case (_, lndA, lndB) =>
    val promise = Promise[(NodeId, TLV)]()

    val subscriberA = LndCustomMessageSubscriber(lndA)
    val subscriberB = LndCustomMessageSubscriber(lndB)

    def handler(nodeId: NodeId, tlv: TLV): Future[Unit] = {
      promise.success((nodeId, tlv))
      Future.unit
    }

    val tlvNodeA = TLVNode(subscriberA, handler)
    val tlvNodeB = TLVNode(subscriberB, (_, _) => Future.unit)

    for {
      _ <- tlvNodeA.start()
      _ <- tlvNodeB.start()

      nodeIdA <- tlvNodeA.nodeId
      nodeIdB <- tlvNodeB.nodeId

      // random message
      message = UnknownTLV(BigSizeUInt(48001), hex"00FF11AA")
      _ <- tlvNodeB.sendToPeer(nodeIdA, message)
      (peer, received) <- promise.future
    } yield {
      assert(peer == nodeIdB)
      assert(message == received)
    }
  }
}
