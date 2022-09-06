import java.nio.file._
import java.security.MessageDigest
import scala.util.Properties

name := "bitcoin-s-cln-rpc"

libraryDependencies ++= Deps.lndRpc

CommonSettings.prodSettings

enablePlugins(AkkaGrpcPlugin)

// Disable deprecation and unused imports warning otherwise generated files will cause errors
Compile / scalacOptions ++= Seq(
  "-Wconf:cat=deprecation:site=cln\\..*:silent",
  "-Wconf:cat=unused-imports:site=cln:silent"
)

TaskKeys.downloadCLN := {
  val logger = streams.value.log
  import scala.sys.process._

  val binaryDir = CommonSettings.binariesPath.resolve("cln")

  if (Files.notExists(binaryDir)) {
    logger.info(s"Creating directory for CLN binaries: $binaryDir")
    Files.createDirectories(binaryDir)
  }

  require(Properties.isLinux, "CLN binaries are only available for Linux")

  val version = "0.12.0"

  val ubuntuVersion = "lsb_release -rs".!!.trim

  val (platform, suffix) =
    if (ubuntuVersion == "22.04") ("Ubuntu-22.04", "tar.xz")
    else if (ubuntuVersion == "20.04") ("Ubuntu-20.04", "tar.xz")
    else if (ubuntuVersion == "18.04") ("Ubuntu-18.04", "tar.xz")
    else sys.error(s"Unsupported OS: ${Properties.osName}")

  logger.debug(s"(Maybe) downloading CLN binaries for version: $version")

  val versionDir = binaryDir resolve version
  val location =
    s"https://github.com/ElementsProject/lightning/releases/download/v$version/clightning-v$version-$platform.tar.xz"

  if (Files.exists(versionDir)) {
    logger.debug(
      s"Directory $versionDir already exists, skipping download of CLN $version")
  } else {
    Files.createDirectories(versionDir)
    val archiveLocation = binaryDir resolve s"$version.$suffix"
    logger.info(s"Downloading CLN version $version from location: $location")
    logger.info(s"Placing the file in $archiveLocation")
    val downloadCommand = url(location) #> archiveLocation.toFile
    downloadCommand.!!

    val bytes = Files.readAllBytes(archiveLocation)
    val hash = MessageDigest
      .getInstance("SHA-256")
      .digest(bytes)
      .map("%02x" format _)
      .mkString

    val expectedHash =
      if (ubuntuVersion == "22.04")
        "70831b70ea6216fd882ecb604e02816433ef7c4d66dd6c60bb268d472fb13a90"
      else if (ubuntuVersion == "20.04")
        "4a71565d77b9c91d32e7b9b17b608f79da8cac469cf0e2134e80a397e65bc0fa"
      else if (ubuntuVersion == "18.04")
        "b77415cdde1ca5dd8ec8e9e6619a93f3333ae69ea7dc63064077d3f2e2a6575d"
      else sys.error(s"Unsupported OS: ${Properties.osName}")

    if (hash.equalsIgnoreCase(expectedHash)) {
      logger.info(s"Download complete and verified, unzipping result")

      val extractCommand = s"tar -xf $archiveLocation --directory $versionDir"
      logger.info(s"Extracting archive with command: $extractCommand")
      extractCommand.!!
    } else {
      logger.error(
        s"Downloaded invalid version of CLN, got $hash, expected $expectedHash")
    }

    logger.info(s"Deleting archive")
    Files.delete(archiveLocation)
  }
}
