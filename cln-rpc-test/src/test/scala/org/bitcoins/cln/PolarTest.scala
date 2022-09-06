package org.bitcoins.cln

import org.bitcoins.cln.config.ClnInstanceLocal
import org.bitcoins.core.config.RegTest
import org.bitcoins.testkit.util.BitcoinSAsyncTest

import java.net.URI
import java.nio.file.Paths

class PolarTest extends BitcoinSAsyncTest {

  private val instance = ClnInstanceLocal(
    Paths.get(
      "/home/ben/.polar/networks/1/volumes/c-lightning/carol/lightningd"),
    network = RegTest,
    11003,
    new URI("tcp://127.0.0.1:9837"),
    None
  )

  val cln: ClnRpcClient = new ClnRpcClient(instance, instance.certFile.toFile)

  it must "get info" in {
    cln.getInfo.map { info =>
      assert(info.version.nonEmpty)
    }
  }
}
