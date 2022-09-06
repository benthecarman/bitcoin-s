package org.bitcoins.testkit.fixtures

import org.bitcoins.cln.ClnRpcClient
import org.bitcoins.rpc.client.common.BitcoindRpcClient
import org.bitcoins.testkit.async.TestAsyncUtil
import org.bitcoins.testkit.cln._
import org.bitcoins.testkit.rpc._
import org.scalatest.FutureOutcome

import scala.concurrent.duration.DurationInt

/** A trait that is useful if you need cln fixtures for your test suite */
trait CLNFixture extends BitcoinSFixture with CachedBitcoindV23 {

  override type FixtureParam = ClnRpcClient

  override def withFixture(test: OneArgAsyncTest): FutureOutcome = {
    makeDependentFixture[ClnRpcClient](
      () =>
        for {
          bitcoind <- cachedBitcoindWithFundsF

          client = ClnRpcTestClient.fromSbtDownload(Some(bitcoind))
          cln <- client.start()
        } yield cln,
      { cln =>
        for {
          // let cln clean up
          _ <- TestAsyncUtil.nonBlockingSleep(3.seconds)
          _ <- cln.stop()
        } yield ()
      }
    )(test)
  }
}

/** A trait that is useful if you need dual cln fixtures for your test suite */
trait DualClnFixture extends BitcoinSFixture with CachedBitcoindV23 {

  override type FixtureParam =
    (BitcoindRpcClient, ClnRpcClient, ClnRpcClient)

  override def withFixture(test: OneArgAsyncTest): FutureOutcome = {
    withDualCLN(test)
  }

  def withDualCLN(test: OneArgAsyncTest): FutureOutcome = {
    makeDependentFixture[FixtureParam](
      () => {
        for {
          bitcoind <- cachedBitcoindWithFundsF
          _ = logger.debug("creating cln")
          clients <- ClnRpcTestUtil.createNodePair(bitcoind)
        } yield (bitcoind, clients._1, clients._2)
      },
      { param =>
        val (_, clientA, clientB) = param
        for {
          // let cln clean up
          _ <- TestAsyncUtil.nonBlockingSleep(3.seconds)
          _ <- clientA.stop()
          _ <- clientB.stop()
        } yield ()
      }
    )(test)
  }
}

/** Creates two CLNs with no channels opened */
trait CLNChannelOpenerFixture extends BitcoinSFixture with CachedBitcoindV23 {

  override type FixtureParam =
    (BitcoindRpcClient, ClnRpcClient, ClnRpcClient)

  override def withFixture(test: OneArgAsyncTest): FutureOutcome = {
    makeDependentFixture[FixtureParam](
      () => {
        cachedBitcoindWithFundsF.flatMap { bitcoind =>
          val clientA = ClnRpcTestClient.fromSbtDownload(Some(bitcoind))
          val clientB = ClnRpcTestClient.fromSbtDownload(Some(bitcoind))

          val startAF = clientA.start()
          val startBF = clientB.start()

          for {
            a <- startAF
            b <- startBF
          } yield (bitcoind, a, b)
        }
      },
      { param =>
        val (_, clientA, clientB) = param
        for {
          // let cln clean up
          _ <- TestAsyncUtil.nonBlockingSleep(3.seconds)
          _ <- clientA.stop()
          _ <- clientB.stop()
        } yield ()
      }
    )(test)
  }
}
