package org.bitcoins.testkit.cln

import akka.actor.ActorSystem
import cln.FundchannelResponse
import grizzled.slf4j.Logging
import org.bitcoins.cln._
import org.bitcoins.cln.config.ClnInstanceLocal
import org.bitcoins.core.currency._
import org.bitcoins.core.protocol.ln.node.NodeId
import org.bitcoins.core.wallet.fee.SatoshisPerVirtualByte
import org.bitcoins.rpc.client.common.{BitcoindRpcClient, BitcoindVersion}
import org.bitcoins.rpc.config._
import org.bitcoins.rpc.util.RpcUtil
import org.bitcoins.testkit.async.TestAsyncUtil
import org.bitcoins.testkit.rpc.BitcoindRpcTestUtil
import org.bitcoins.testkit.util.{FileUtil, TestkitBinaries}

import java.io.{File, PrintWriter}
import java.net.InetSocketAddress
import java.nio.file.Path
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Properties

trait ClnRpcTestUtil extends Logging {

  val sbtBinaryDirectory: Path =
    TestkitBinaries.baseBinaryDirectory.resolve("cln")

  def randomCLNDir(): File =
    new File(s"/tmp/cln-test/${FileUtil.randomDirName}/")

  def canonicalDatadir = new File(s"${Properties.userHome}/.reg_lightning")

  /** Makes a best effort to get a 0.21 bitcoind instance
    */
  def startedBitcoindRpcClient(
      instanceOpt: Option[BitcoindInstanceLocal] = None)(implicit
      actorSystem: ActorSystem): Future[BitcoindRpcClient] = {
    BitcoindRpcTestUtil.startedBitcoindRpcClient(instanceOpt, Vector.newBuilder)
  }

  /** Creates a bitcoind instance with the given parameters */
  def bitcoindInstance(
      port: Int = RpcUtil.randomPort,
      rpcPort: Int = RpcUtil.randomPort,
      bitcoindV: BitcoindVersion = BitcoindVersion.V21)(implicit
      system: ActorSystem): BitcoindInstanceLocal = {
    BitcoindRpcTestUtil.getInstance(bitcoindVersion = bitcoindV,
                                    port = port,
                                    rpcPort = rpcPort)
  }

  def commonConfig(
      datadir: Path,
      bitcoindInstance: BitcoindInstance,
      port: Int = RpcUtil.randomPort,
      grpcPort: Int = RpcUtil.randomPort): String = {
    val bitcoinCliPath: Path = bitcoindInstance match {
      case local: BitcoindInstanceLocal =>
        local.binary.toPath.getParent.resolve("bitcoin-cli")
      case _: BitcoindInstanceRemote =>
        throw new RuntimeException("Local bitcoind instance required for cln")
    }
    val bitcoindAuth = bitcoindInstance.authCredentials
      .asInstanceOf[BitcoindAuthCredentials.PasswordBased]

    s"""network=regtest
       |addr=127.0.0.1:$port
       |grpc-port=$grpcPort
       |bitcoin-cli=${bitcoinCliPath.toAbsolutePath}
       |bitcoin-rpcuser=${bitcoindAuth.username}
       |bitcoin-rpcpassword=${bitcoindAuth.password}
       |bitcoin-rpcconnect=127.0.0.1
       |bitcoin-rpcport=${bitcoindInstance.rpcUri.getPort}
       |log-file=${datadir.resolve("cln.log")}
       |""".stripMargin
  }

  def clnDataDir(
      bitcoindRpcClient: BitcoindRpcClient,
      isCanonical: Boolean): File = {
    val bitcoindInstance = bitcoindRpcClient.instance
    if (isCanonical) {
      canonicalDatadir
    } else {
      //creates a random cln datadir, but still assumes that a bitcoind instance is running right now
      val datadir = randomCLNDir()
      datadir.mkdirs()
      logger.trace(s"Creating temp cln dir ${datadir.getAbsolutePath}")

      val config = commonConfig(datadir.toPath, bitcoindInstance)

      new PrintWriter(new File(datadir, "config")) {
        write(config)
        close()
      }
      datadir
    }
  }

  def cLightingInstance(bitcoindRpc: BitcoindRpcClient)(implicit
      system: ActorSystem): ClnInstanceLocal = {
    val datadir = clnDataDir(bitcoindRpc, isCanonical = false)
    cLightingInstance(datadir)
  }

  def cLightingInstance(datadir: File)(implicit
      system: ActorSystem): ClnInstanceLocal = {
    ClnInstanceLocal.fromDataDir(datadir)
  }

  /** Returns a `Future` that is completed when both cln and bitcoind have the same block height
    * Fails the future if they are not synchronized within the given timeout.
    */
  def awaitInSync(cln: ClnRpcClient, bitcoind: BitcoindRpcClient)(implicit
      system: ActorSystem): Future[Unit] = {
    import system.dispatcher
    TestAsyncUtil.retryUntilSatisfiedF(conditionF =
                                         () => clientInSync(cln, bitcoind),
                                       interval = 1.seconds)
  }

  private def clientInSync(client: ClnRpcClient, bitcoind: BitcoindRpcClient)(
      implicit ec: ExecutionContext): Future[Boolean] =
    for {
      blockCount <- bitcoind.getBlockCount
      info <- client.getInfo
    } yield info.blockheight.toInt == blockCount

  /** Shuts down an cln daemon and the bitcoind daemon it is associated with
    */
  def shutdown(clnRpcClient: ClnRpcClient)(implicit
      system: ActorSystem): Future[Unit] = {
    import system.dispatcher
    val shutdownF = for {
      bitcoindRpc <- startedBitcoindRpcClient()
      _ <- BitcoindRpcTestUtil.stopServer(bitcoindRpc)
      _ <- clnRpcClient.stop()
    } yield {
      logger.debug("Successfully shutdown cln and it's corresponding bitcoind")
    }
    shutdownF.failed.foreach { err: Throwable =>
      logger.info(
        s"Killed a bitcoind instance, but could not find an cln process to kill")
      throw err
    }
    shutdownF
  }

  def connectLNNodes(client: ClnRpcClient, otherClient: ClnRpcClient)(implicit
      ec: ExecutionContext): Future[Unit] = {
    val nodeIdF = client.nodeId
    val connectionF = nodeIdF.flatMap { nodeId =>
      val uri = otherClient.instance.listenBinding
      client.connect(nodeId, new InetSocketAddress(uri.getHost, uri.getPort))
    }

    def isConnected: Future[Boolean] = {
      for {
        nodeId <- nodeIdF
        _ <- connectionF
        res <- otherClient.isConnected(nodeId)
      } yield res
    }

    logger.debug(s"Awaiting connection between clients")
    val connected = TestAsyncUtil.retryUntilSatisfiedF(conditionF =
                                                         () => isConnected,
                                                       interval = 1.second)

    connected.map(_ => logger.debug(s"Successfully connected two clients"))

    connected
  }

  def fundLNNodes(
      bitcoind: BitcoindRpcClient,
      client: ClnRpcClient,
      otherClient: ClnRpcClient)(implicit
      ec: ExecutionContext): Future[Unit] = {
    for {
      addrA <- client.getNewAddress()
      addrB <- otherClient.getNewAddress()

      _ <- bitcoind.sendMany(Map(addrA -> Bitcoins(1), addrB -> Bitcoins(1)))
      _ <- bitcoind.getNewAddress.flatMap(bitcoind.generateToAddress(1, _))
    } yield ()
  }

  /** Creates two cln nodes that are connected together and returns their
    * respective [[org.bitcoins.cln.ClnRpcClient ClnRpcClient]]s
    */
  def createNodePair(bitcoind: BitcoindRpcClient)(implicit
      system: ActorSystem): Future[(ClnRpcClient, ClnRpcClient)] = {
    import system.dispatcher
    val clientA = ClnRpcTestClient.fromSbtDownload(Some(bitcoind))
    val clientB = ClnRpcTestClient.fromSbtDownload(Some(bitcoind))

    val startAF = clientA.start()
    val startBF = clientB.start()

    val clientsF = for {
      a <- startAF
      b <- startBF
    } yield (a, b)

    def isSynced: Future[Boolean] = for {
      (client, otherClient) <- clientsF
      height <- bitcoind.getBlockCount

      infoA <- client.getInfo
      infoB <- otherClient.getInfo
    } yield infoA.blockheight.toInt == height && infoB.blockheight.toInt == height

    def isFunded: Future[Boolean] = for {
      (client, otherClient) <- clientsF

      balA <- client.walletBalance()
      balB <- otherClient.walletBalance()
    } yield balA.confirmedBalance > Satoshis.zero && balB.confirmedBalance > Satoshis.zero

    for {
      (client, otherClient) <- clientsF

      _ <- connectLNNodes(client, otherClient)
      _ <- fundLNNodes(bitcoind, client, otherClient)

      _ <- TestAsyncUtil.awaitConditionF(() => isSynced, interval = 1.second)
      _ <- TestAsyncUtil.awaitConditionF(() => isFunded)

      _ <- openChannel(bitcoind, client, otherClient)
    } yield (client, otherClient)
  }

  private val DEFAULT_CHANNEL_AMT = Satoshis(500000L)

  /** Opens a channel from n1 -> n2 */
  def openChannel(
      bitcoind: BitcoindRpcClient,
      n1: ClnRpcClient,
      n2: ClnRpcClient,
      amt: CurrencyUnit = DEFAULT_CHANNEL_AMT,
      pushAmt: CurrencyUnit = DEFAULT_CHANNEL_AMT / Satoshis(2))(implicit
      ec: ExecutionContext): Future[FundchannelResponse] = {

    val n1NodeIdF = n1.nodeId
    val n2NodeIdF = n2.nodeId

    val nodeIdsF: Future[(NodeId, NodeId)] = {
      n1NodeIdF.flatMap(n1 => n2NodeIdF.map(n2 => (n1, n2)))
    }

    val fundedChannelIdF =
      nodeIdsF.flatMap { case (nodeId1, nodeId2) =>
        logger.debug(
          s"Opening a channel from $nodeId1 -> $nodeId2 with amount $amt")
        n1.openChannel(nodeId = nodeId2,
                       fundingAmount = amt,
                       pushAmt = pushAmt,
                       feeRate = SatoshisPerVirtualByte.fromLong(10),
                       privateChannel = false)
      }

    val genF = for {
      _ <- fundedChannelIdF
      address <- bitcoind.getNewAddress
      blocks <- bitcoind.generateToAddress(6, address)
    } yield blocks

    val openedF = {
      genF.flatMap { _ =>
        fundedChannelIdF.flatMap { result =>
          for {
            (id1, id2) <- nodeIdsF
            _ <- awaitUntilChannelActive(n1, id1)
            _ <- awaitUntilChannelActive(n2, id2)
          } yield result
        }
      }
    }

    openedF.flatMap { _ =>
      nodeIdsF.map { case (nodeId1, nodeId2) =>
        logger.debug(
          s"Channel successfully opened $nodeId1 -> $nodeId2 with amount $amt")
      }
    }

    openedF
  }

  private def awaitUntilChannelActive(
      client: ClnRpcClient,
      destination: NodeId)(implicit ec: ExecutionContext): Future[Unit] = {
    def isActive: Future[Boolean] = {
      client.listChannels().map(_.exists(_.destination == destination.bytes))
    }

    TestAsyncUtil.retryUntilSatisfiedF(conditionF = () => isActive,
                                       interval = 1.seconds)
  }
}

object ClnRpcTestUtil extends ClnRpcTestUtil
