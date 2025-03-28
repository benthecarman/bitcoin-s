package org.bitcoins.testkit.fixtures

import org.bitcoins.core.util.FutureUtil
import org.bitcoins.dlc.wallet.DLCAppConfig
import org.bitcoins.dlc.wallet.models.{IncomingDLCOfferDAO, _}
import org.bitcoins.server.BitcoinSAppConfig
import org.bitcoins.testkit.{BitcoinSTestAppConfig, EmbeddedPg}
import org.scalatest._

import scala.concurrent.{Await, Future}

trait DLCDAOFixture extends BitcoinSFixture with EmbeddedPg {

  private lazy val daos: DLCDAOs = {
    val announcementDAO = OracleAnnouncementDataDAO()
    val nonceDAO = OracleNonceDAO()
    val dlcAnnouncementDAO = DLCAnnouncementDAO()
    val dlc = DLCDAO()
    val contractDataDAO = DLCContractDataDAO()
    val dlcOfferDAO = DLCOfferDAO()
    val dlcAcceptDAO = DLCAcceptDAO()
    val dlcInputsDAO = DLCFundingInputDAO()
    val dlcSigsDAO = DLCCETSignaturesDAO()
    val dlcRefundSigDAO = DLCRefundSigsDAO()
    val dlcRemoteTxDAO = DLCRemoteTxDAO()
    val incomingDLCOfferDAO = IncomingDLCOfferDAO()
    val contactDAO = DLCContactDAO()
    DLCDAOs(
      announcementDAO = announcementDAO,
      nonceDAO = nonceDAO,
      dlcAnnouncementDAO = dlcAnnouncementDAO,
      dlcDAO = dlc,
      contractDataDAO = contractDataDAO,
      dlcOfferDAO = dlcOfferDAO,
      dlcAcceptDAO = dlcAcceptDAO,
      dlcInputsDAO = dlcInputsDAO,
      dlcSigsDAO = dlcSigsDAO,
      dlcRefundSigDAO = dlcRefundSigDAO,
      dlcRemoteTxDAO = dlcRemoteTxDAO,
      incomingDLCOfferDAO = incomingDLCOfferDAO,
      contactDAO = contactDAO
    )
  }

  final override type FixtureParam = DLCDAOs

  implicit protected val config: BitcoinSAppConfig =
    BitcoinSTestAppConfig
      .getNeutrinoWithEmbeddedDbTestConfig(() => pgUrl(), Vector.empty)

  implicit private val dlcConfig: DLCAppConfig = config.dlcConf

  override def afterAll(): Unit = {
    val stoppedF = config.stop()
    val _ = Await.ready(stoppedF, akkaTimeout.duration)
    super[EmbeddedPg].afterAll()
  }

  def withFixture(test: OneArgAsyncTest): FutureOutcome =
    makeFixture(
      build = () => Future(dlcConfig.migrate()).map(_ => daos),
      destroy = () => dropAll()
    )(test)

  private def dropAll(): Future[Unit] = {
    val res = for {
      _ <- FutureUtil.sequentially(daos.list.reverse)(dao => dao.deleteAll())
    } yield ()
    res.failed.foreach { ex =>
      ex.printStackTrace()
    }
    res
  }
}
