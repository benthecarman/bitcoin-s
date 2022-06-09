package org.bitcoins.lnd.rpc

import org.bitcoins.core.number.UInt32
import org.bitcoins.lnd.rpc.config._
import org.bitcoins.testkit.util.BitcoinSAsyncTest
import scodec.bits.ByteVector

import java.net.URI
import java.nio.file._
import scala.util.Properties

class PolarTest extends BitcoinSAsyncTest {

  private val polarDir =
    Paths.get(Properties.userHome, ".polar/networks/1/volumes/lnd")

  val alice: LndRpcClient = {
    val mac: String = {
      val bytes = Files.readAllBytes(
        polarDir.resolve("alice/data/chain/bitcoin/regtest/admin.macaroon"))
      ByteVector(bytes).toHex
    }

    val instance =
      LndInstanceRemote(new URI("http://127.0.0.1:10001"),
                        mac,
                        polarDir.resolve("alice/tls.cert").toFile)

    new LndRpcClient(instance)
  }

  val bob: LndRpcClient = {
    val mac: String = {
      val bytes = Files.readAllBytes(
        polarDir.resolve("bob/data/chain/bitcoin/regtest/admin.macaroon"))
      ByteVector(bytes).toHex
    }

    val instance =
      LndInstanceRemote(new URI("http://127.0.0.1:10002"),
                        mac,
                        polarDir.resolve("bob/tls.cert").toFile)

    new LndRpcClient(instance)
  }

  val carol: LndRpcClient = {
    val mac: String = {
      val bytes = Files.readAllBytes(
        polarDir.resolve("carol/data/chain/bitcoin/regtest/admin.macaroon"))
      ByteVector(bytes).toHex
    }

    val instance =
      LndInstanceRemote(new URI("http://127.0.0.1:10003"),
                        mac,
                        polarDir.resolve("carol/tls.cert").toFile)

    new LndRpcClient(instance)
  }

  it must "get alice info" in {
    alice.getInfo.map { info =>
      assert(info.blockHeight > UInt32.zero)
    }
  }

  it must "get bob info" in {
    bob.getInfo.map { info =>
      assert(info.blockHeight > UInt32.zero)
    }
  }

  it must "get carol info" in {
    carol.getInfo.map { info =>
      assert(info.blockHeight > UInt32.zero)
    }
  }

//  it must "htlc intercept" in {
//    bob.startHTLCInterceptor().map { i =>
//      println(i.action)
//      succeed
//    }
//  }
}
