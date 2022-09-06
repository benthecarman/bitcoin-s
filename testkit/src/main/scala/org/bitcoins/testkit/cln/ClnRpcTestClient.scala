package org.bitcoins.testkit.cln

import akka.actor.ActorSystem
import org.bitcoins.cln.ClnRpcClient
import org.bitcoins.cln.config.ClnInstanceLocal
import org.bitcoins.rpc.client.common.BitcoindRpcClient
import org.bitcoins.testkit.async.TestAsyncUtil
import org.bitcoins.testkit.util.{
  RpcBinaryUtil,
  SbtBinaryFactory,
  TestkitBinaries
}

import java.io.File
import java.nio.file.{Files, Path}
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

/** Helper class to start a cln client with the given binary */
case class ClnRpcTestClient(
    override val binary: Path,
    bitcoindOpt: Option[BitcoindRpcClient])(implicit system: ActorSystem)
    extends RpcBinaryUtil[ClnRpcClient] {
  require(Files.exists(binary),
          s"Path did not exist! got=${binary.toAbsolutePath.toString}")
  import system.dispatcher

  /** Cached client. This is defined if start() has been called
    * else None
    */
  private var clientOpt: Option[ClnRpcClient] = None

  private lazy val bitcoindRpcClientF: Future[BitcoindRpcClient] = {
    bitcoindOpt match {
      case Some(bitcoindRpcClient) => Future.successful(bitcoindRpcClient)
      case None                    => ClnRpcTestUtil.startedBitcoindRpcClient()
    }
  }

  private lazy val instanceF: Future[ClnInstanceLocal] = {
    bitcoindRpcClientF.map { bitcoind =>
      ClnRpcTestUtil.cLightingInstance(bitcoind)
    }
  }

  private lazy val ClnRpcClientF: Future[ClnRpcClient] = {
    instanceF.map(new ClnRpcClient(_, binary.toFile))
  }

  override def start(): Future[ClnRpcClient] = {
    clientOpt match {
      case Some(client) => Future.successful(client)
      case None =>
        for {
          cln <- ClnRpcClientF

          _ <- cln.start()
          // wait for rpc server to start
          _ <- TestAsyncUtil.awaitCondition(
            () => cln.instance.certFile.toFile.exists(),
            interval = 1.second,
            maxTries = 10)
          _ <- TestAsyncUtil.nonBlockingSleep(7.seconds)
        } yield {
          clientOpt = Some(cln)
          cln
        }
    }
  }

  override def stop(): Future[ClnRpcClient] = {
    clientOpt match {
      case Some(cli) => cli.stop()
      case None =>
        Future.failed(new RuntimeException(s"ClnRpcClient was not defined!"))
    }
  }
}

object ClnRpcTestClient extends SbtBinaryFactory {

  /** Directory where sbt downloads cln binaries */
  override val sbtBinaryDirectory: Path =
    TestkitBinaries.baseBinaryDirectory.resolve("cln")

  def fromSbtDownloadOpt(
      bitcoindRpcClientOpt: Option[BitcoindRpcClient],
      clnVersionOpt: Option[String] = None
  )(implicit system: ActorSystem): Option[ClnRpcTestClient] = {
    val fileOpt =
      getBinary(clnVersionOpt = clnVersionOpt,
                binaryDirectory = sbtBinaryDirectory)

    for {
      file <- fileOpt
    } yield ClnRpcTestClient(binary = file.toPath, bitcoindRpcClientOpt)
  }

  def fromSbtDownload(
      bitcoindRpcClientOpt: Option[BitcoindRpcClient],
      clnVersionOpt: Option[String] = None)(implicit
      system: ActorSystem): ClnRpcTestClient = {
    val clnOpt = fromSbtDownloadOpt(bitcoindRpcClientOpt = bitcoindRpcClientOpt,
                                    clnVersionOpt = clnVersionOpt)
    clnOpt match {
      case Some(client) => client
      case None =>
        sys.error(
          s"Could not find cln that was downloaded by sbt " +
            s"with version=$clnVersionOpt " +
            s"path=${sbtBinaryDirectory.toAbsolutePath.toString}")
    }
  }

  /** Path to executable downloaded for cln, if it exists */
  def getBinary(
      clnVersionOpt: Option[String],
      binaryDirectory: Path): Option[File] = {
    val versionStr = clnVersionOpt.getOrElse(ClnRpcClient.version)

    val path = binaryDirectory
      .resolve(versionStr)
      .resolve("usr")
      .resolve("bin")
      .resolve("lightningd")

    if (Files.exists(path)) {
      Some(path.toFile)
    } else None
  }
}
