package org.bitcoins.cln

import akka.actor.ActorSystem
import akka.grpc.{GrpcClientSettings, SSLContextUtils}
import cln.ListfundsOutputs.ListfundsOutputsStatus
import cln.NewaddrRequest.NewaddrAddresstype
import cln._
import grizzled.slf4j.Logging
import org.bitcoins.cln.config.ClnInstanceLocal
import org.bitcoins.commons.jsonmodels.clightning.CLightningJsonModels.WalletBalances
import org.bitcoins.commons.util.NativeProcessFactory
import org.bitcoins.core.currency._
import org.bitcoins.core.number.UInt32
import org.bitcoins.core.protocol.BitcoinAddress
import org.bitcoins.core.protocol.ln.channel.ShortChannelId
import org.bitcoins.core.protocol.ln.currency.MilliSatoshis
import org.bitcoins.core.protocol.ln.node.NodeId
import org.bitcoins.core.util.StartStopAsync
import org.bitcoins.core.wallet.fee._

import java.io._
import java.net.InetSocketAddress
import scala.concurrent._
import scala.io.Source

class ClnRpcClient(val instance: ClnInstanceLocal, binary: File)(implicit
    val system: ActorSystem)
    extends NativeProcessFactory
    with ClnUtils
    with StartStopAsync[ClnRpcClient]
    with Logging {
  implicit val executionContext: ExecutionContext = system.dispatcher

  override lazy val cmd: String = {
    require(binary.exists(),
            s"Could not find cln binary at ${binary.getAbsolutePath}")

    val logFileConf = instance.logFileOpt
      .map(f => s"--log-file=${f.getAbsolutePath}")
      .getOrElse("")

    s"$binary --lightning-dir=${instance.datadir.toAbsolutePath} --grpc-port=${instance.grpcPort} $logFileConf"
  }

  // Configure the client
  private lazy val clientSettings: GrpcClientSettings = {
    val certFile = instance.certFile.toFile
    // get certificate as a string
    val clientCert = {
      val file = instance.certFile.toFile
      val source = Source.fromFile(file)
      val str = source.getLines().toVector.mkString("\n")
      source.close()

      str
    }
//    val serverCert = {
//      val file = instance.serverCertFile.toFile
//      val source = Source.fromFile(file)
//      val str = source.getLines().toVector.mkString("\n")
//      source.close()
//
//      str
//    }
    val certs = s"$clientCert"
    println(certs)
    require(certFile.exists(),
            s"Could not find cert file at ${certFile.toPath.toAbsolutePath}")

//    val stream = new FileInputStream(certFile)
    val stream = new ByteArrayInputStream(
      certs.getBytes(java.nio.charset.StandardCharsets.UTF_8.name))

    val trustManager = SSLContextUtils.trustManagerFromStream(stream)

    GrpcClientSettings
      .connectToServiceAt("localhost", instance.grpcPort)
      .withTrustManager(trustManager)
  }

  lazy val nodeClient: NodeClient = NodeClient(clientSettings)

  def getInfo: Future[GetinfoResponse] = {
    nodeClient.getinfo(GetinfoRequest())
  }

  def nodeId: Future[NodeId] = getInfo.map(_.id).map(NodeId(_))

  def connect(
      nodeId: NodeId,
      addr: InetSocketAddress): Future[ConnectResponse] = {
    nodeClient.connectPeer(
      ConnectRequest(nodeId.hex,
                     Some(addr.getHostName),
                     Some(UInt32(addr.getPort))))
  }

  def findPeer(nodeId: NodeId): Future[Option[ListpeersPeers]] = {
    nodeClient.listPeers(ListpeersRequest()).map { peers =>
      peers.peers.find(_.id == nodeId.bytes)
    }
  }

  def isConnected(nodeId: NodeId): Future[Boolean] =
    findPeer(nodeId).map(_.exists(_.connected))

  def getNewAddress(addrType: NewaddrAddresstype): Future[BitcoinAddress] = {
    getNewAddress(NewaddrRequest(Some(addrType)))
  }

  def getNewAddress(): Future[BitcoinAddress] = {
    getNewAddress(NewaddrRequest())
  }

  def getNewAddress(request: NewaddrRequest): Future[BitcoinAddress] = {
    nodeClient.newAddr(request).map { resp =>
      val str = resp.bech32.orElse(resp.p2ShSegwit).get
      BitcoinAddress.fromString(str)
    }
  }

  def listFunds(request: ListfundsRequest): Future[ListfundsResponse] = {
    nodeClient.listFunds(request)
  }

  def listFunds(): Future[ListfundsResponse] = listFunds(
    ListfundsRequest(Some(false)))

  def walletBalance(): Future[WalletBalances] = {
    listFunds().map { funds =>
      val start = WalletBalances(Satoshis.zero, Satoshis.zero, Satoshis.zero)
      funds.outputs.foldLeft(start) { case (balances, utxo) =>
        val amt = MilliSatoshis(utxo.amountMsat.get.msat.toBigInt).toSatoshis
        val newTotal = balances.balance + amt

        utxo.status match {
          case ListfundsOutputsStatus.UNCONFIRMED =>
            val newUnconfirmed = balances.unconfirmedBalance + amt
            balances.copy(balance = newTotal,
                          unconfirmedBalance = newUnconfirmed)
          case ListfundsOutputsStatus.CONFIRMED =>
            val newConfirmed = balances.confirmedBalance + amt
            balances.copy(balance = newTotal, confirmedBalance = newConfirmed)
          case ListfundsOutputsStatus.SPENT           => balances
          case ListfundsOutputsStatus.Unrecognized(_) => balances
        }
      }
    }
  }

  def listTransactions(): Future[Vector[ListtransactionsTransactions]] = {
    val req = ListtransactionsRequest()
    listTransactions(req)
  }

  def listTransactions(request: ListtransactionsRequest): Future[
    Vector[ListtransactionsTransactions]] = {
    nodeClient.listTransactions(request).map(_.transactions.toVector)
  }

  def listChannels(): Future[Vector[ListchannelsChannels]] = {
    listChannels(ListchannelsRequest())
  }

  def listChannels(
      request: ListchannelsRequest): Future[Vector[ListchannelsChannels]] = {
    nodeClient.listChannels(request).map(_.channels.toVector)
  }

  def findChannel(
      shortChanId: ShortChannelId): Future[Option[ListchannelsChannels]] = {
    val request = ListchannelsRequest(Some(shortChanId.hex))
    listChannels(request).map(_.headOption)
  }

  def openChannel(
      nodeId: NodeId,
      fundingAmount: CurrencyUnit,
      pushAmt: CurrencyUnit,
      feeRate: FeeUnit,
      privateChannel: Boolean): Future[FundchannelResponse] = {
    val amount =
      AmountOrAll.Value.Amount(Amount(MilliSatoshis(fundingAmount).toUInt64))
    val pushMsat = Amount(MilliSatoshis(pushAmt).toUInt64)
    val feerateStyle = feeUnitMapper(feeRate)

    val request = FundchannelRequest(
      nodeId.bytes,
      Some(AmountOrAll(amount)),
      feerate = Some(Feerate(feerateStyle)),
      pushMsat = Some(pushMsat),
      announce = Some(!privateChannel)
    )
    nodeClient.fundChannel(request)
  }

  /** Starts lnd on the local system.
    */
  override def start(): Future[ClnRpcClient] = {
    startBinary().map(_ => this)
  }

  override def stop(): Future[ClnRpcClient] = Future.successful(this)
}

object ClnRpcClient {

  /** The current version we support of CLN */
  private[bitcoins] val version = "0.12.0"

  /** THe name we use to create actor systems. We use this to know which
    * actor systems to shut down on node shutdown
    */
  private[cln] val ActorSystemName = "cln-rpc-client-created-by-bitcoin-s"

  /** Creates an RPC client from the given instance,
    * together with the given actor system. This is for
    * advanced users, where you need fine grained control
    * over the RPC client.
    */
  def apply(instance: ClnInstanceLocal, binary: File): ClnRpcClient = {
    implicit val system: ActorSystem = ActorSystem.create(ActorSystemName)
    withActorSystem(instance, binary)
  }

  /** Constructs a RPC client from the given datadir, or
    * the default datadir if no directory is provided
    */
  def withActorSystem(instance: ClnInstanceLocal, binary: File)(implicit
      system: ActorSystem): ClnRpcClient = new ClnRpcClient(instance, binary)
}
