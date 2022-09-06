package org.bitcoins.cln.config

import akka.actor.ActorSystem
import org.bitcoins.core.api.commons.InstanceFactoryLocal
import org.bitcoins.core.config._

import java.io.File
import java.net.URI
import java.nio.file.{Path, Paths}
import scala.util.Properties

case class ClnInstanceLocal(
    datadir: Path,
    network: BitcoinNetwork,
    grpcPort: Int,
    listenBinding: URI,
    logFileOpt: Option[File]) {

  lazy val certFile: Path = {
    val netFolder = ClnConfig.networkToConfigString(network)
    datadir.resolve(netFolder).resolve("client.pem")
  }

  lazy val serverCertFile: Path = {
    val netFolder = ClnConfig.networkToConfigString(network)
    datadir.resolve(netFolder).resolve("server.pem")
  }
}

object ClnInstanceLocal
    extends InstanceFactoryLocal[ClnInstanceLocal, ActorSystem] {

  override val DEFAULT_DATADIR: Path =
    Paths.get(Properties.userHome, ".lightning")

  override val DEFAULT_CONF_FILE: Path = DEFAULT_DATADIR.resolve("config")

  private[cln] def getNetworkDirName(network: BitcoinNetwork): String = {
    network match {
      case MainNet  => ""
      case TestNet3 => "testnet"
      case SigNet   => "signet"
      case RegTest  => "regtest"
    }
  }

  override def fromConfigFile(file: File = DEFAULT_CONF_FILE.toFile)(implicit
      system: ActorSystem): ClnInstanceLocal = {
    require(file.exists, s"${file.getPath} does not exist!")
    require(file.isFile, s"${file.getPath} is not a file!")

    val config = ClnConfig(file, file.getParentFile)

    fromConfig(config)
  }

  override def fromDataDir(dir: File = DEFAULT_DATADIR.toFile)(implicit
      system: ActorSystem): ClnInstanceLocal = {
    require(dir.exists, s"${dir.getPath} does not exist!")
    require(dir.isDirectory, s"${dir.getPath} is not a directory!")

    val confFile = dir.toPath.resolve("config").toFile
    val config = ClnConfig(confFile, dir)

    fromConfig(config)
  }

  def fromConfig(config: ClnConfig): ClnInstanceLocal = {
    config.instance
  }
}
