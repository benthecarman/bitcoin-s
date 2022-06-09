package org.bitcoins.lnd.rpc

import lnrpc.Invoice.InvoiceState
import lnrpc.SendRequest
import org.bitcoins.core.currency.Satoshis
import org.bitcoins.core.number.UInt32
import org.bitcoins.testkit.fixtures.TripleLndFixture

class LndTripleClientTest extends TripleLndFixture with LndUtils {

  it must "get info from all lnds" in { param =>
    val (_, lndA, lndB, lndC) = param

    for {
      infoA <- lndA.getInfo
      infoB <- lndB.getInfo
      infoC <- lndC.getInfo
    } yield {
      assert(infoA.identityPubkey != infoB.identityPubkey)
      assert(infoA.identityPubkey != infoC.identityPubkey)
      assert(infoB.identityPubkey != infoC.identityPubkey)

      assert(infoA.blockHeight >= UInt32.zero)
      assert(infoB.blockHeight >= UInt32.zero)
      assert(infoC.blockHeight >= UInt32.zero)
    }
  }

  it must "make a routed payment" in { param =>
    val (_, lndA, lndB, lndC) = param

    val intF = lndB.startHTLCInterceptor()

    for {
      inv <- lndC.addInvoice("hello world", Satoshis(100), 3600)
      _ = println(inv.invoice.lnTags.routingInfo)

      pay <- lndA.lnd.sendPaymentSync(
        SendRequest(paymentRequest = inv.invoice.toString))

      _ = println(pay.paymentError)

      resp <- intF
      _ = println(s"resp: $resp")
      inv <- lndC.lookupInvoice(inv.rHash)
    } yield {
      assert(inv.amtPaidSat == 100)
      assert(inv.state == InvoiceState.SETTLED)
    }
  }
}
