package org.bitcoins.dlc.testgen

import org.bitcoins.commons.jsonmodels.dlc.DLCMessage.{ContractInfo, OracleInfo}
import org.bitcoins.commons.jsonmodels.dlc._
import org.bitcoins.core.config.{BitcoinNetwork, RegTest}
import org.bitcoins.core.currency.CurrencyUnit
import org.bitcoins.core.protocol.BitcoinAddress
import org.bitcoins.core.protocol.script.ScriptPubKey
import org.bitcoins.core.protocol.tlv.DLCOutcomeType
import org.bitcoins.core.protocol.transaction.Transaction
import org.bitcoins.core.util.BitcoinSLogger
import org.bitcoins.core.wallet.fee.SatoshisPerVirtualByte
import org.bitcoins.core.wallet.utxo.{InputInfo, ScriptSignatureParams}
import org.bitcoins.crypto._
import org.bitcoins.dlc.builder.DLCTxBuilder
import org.bitcoins.dlc.execution.{
  DLCExecutor,
  ExecutedDLCOutcome,
  RefundDLCOutcome,
  SetupDLC
}
import org.bitcoins.dlc.sign.DLCTxSigner

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

/** This case class allows for the construction and execution of
  * Discreet Log Contracts between two parties running on this machine (for tests).
  *
  * @param offer The DLCOffer associated with this DLC
  * @param accept The DLCAccept (without sigs) associated with this DLC
  * @param isInitiator True if this client sends the offer message
  * @param fundingPrivKey This client's funding private key for this event
  * @param payoutPrivKey This client's payout private key for this event
  * @param fundingUtxos This client's funding BitcoinUTXOSpendingInfo collection
  */
case class TestDLCClient(
    offer: DLCMessage.DLCOffer,
    accept: DLCMessage.DLCAcceptWithoutSigs,
    isInitiator: Boolean,
    fundingPrivKey: ECPrivateKey,
    payoutPrivKey: ECPrivateKey,
    fundingUtxos: Vector[ScriptSignatureParams[InputInfo]])(implicit
    ec: ExecutionContext)
    extends BitcoinSLogger {
  val dlcTxBuilder: DLCTxBuilder = DLCTxBuilder(offer, accept)

  val dlcTxSigner: DLCTxSigner = DLCTxSigner(dlcTxBuilder,
                                             isInitiator,
                                             fundingPrivKey,
                                             payoutPrivKey,
                                             RegTest,
                                             fundingUtxos)

  private val dlcExecutor = DLCExecutor(dlcTxSigner)

  val messages: Vector[DLCOutcomeType] = offer.oracleAndContractInfo.allOutcomes

  val timeouts: DLCTimeouts = offer.timeouts

  lazy val fundingTx: Transaction =
    Await.result(dlcTxBuilder.buildFundingTx, 5.seconds)

  lazy val fundingTxIdBE: DoubleSha256DigestBE = fundingTx.txIdBE

  /** Sets up the non-initiator's DLC given functions for sending
    * CETSignatures to the initiator as well as receiving CETSignatures
    * and FundingSignatures from them
    */
  def setupDLCAccept(
      sendSigs: CETSignatures => Future[Unit],
      getSigs: Future[(CETSignatures, FundingSignatures)]): Future[SetupDLC] = {
    require(!isInitiator, "You should call setupDLCOffer")

    for {
      remoteCetSigs <- dlcTxSigner.createCETSigs()
      _ <- sendSigs(remoteCetSigs)
      (cetSigs, fundingSigs) <- getSigs
      setupDLC <- dlcExecutor.setupDLCAccept(cetSigs, fundingSigs)
    } yield {
      setupDLC
    }
  }

  /** Sets up the initiator's DLC given functions for getting CETSignatures
    * from the non-initiator as well as sending signatures to them, and lastly
    * a Future which will be populated with the broadcasted (or relayed) fully
    * signed funding transaction
    */
  def setupDLCOffer(
      getSigs: Future[CETSignatures],
      sendSigs: (CETSignatures, FundingSignatures) => Future[Unit],
      getFundingTx: Future[Transaction]): Future[SetupDLC] = {
    require(isInitiator, "You should call setupDLCAccept")

    for {
      cetSigs <- getSigs
      setupDLCWithoutFundingTxSigs <- dlcExecutor.setupDLCOffer(cetSigs)
      cetSigs <- dlcTxSigner.createCETSigs()
      localFundingSigs <- dlcTxSigner.createFundingTxSigs()
      _ <- sendSigs(cetSigs, localFundingSigs)
      fundingTx <- getFundingTx
    } yield {
      setupDLCWithoutFundingTxSigs.copy(fundingTx = fundingTx)
    }
  }

  def executeDLC(
      dlcSetup: SetupDLC,
      oracleSigsF: Future[Vector[SchnorrDigitalSignature]]): Future[
    ExecutedDLCOutcome] = {
    oracleSigsF.flatMap { oracleSigs =>
      dlcExecutor.executeDLC(dlcSetup, oracleSigs)
    }
  }

  def executeRefundDLC(dlcSetup: SetupDLC): RefundDLCOutcome = {
    dlcExecutor.executeRefundDLC(dlcSetup)
  }
}

object TestDLCClient {

  def apply(
      outcomes: ContractInfo,
      oracleInfo: OracleInfo,
      isInitiator: Boolean,
      fundingPrivKey: ECPrivateKey,
      payoutPrivKey: ECPrivateKey,
      remotePubKeys: DLCPublicKeys,
      input: CurrencyUnit,
      remoteInput: CurrencyUnit,
      fundingUtxos: Vector[ScriptSignatureParams[InputInfo]],
      remoteFundingInputs: Vector[DLCFundingInput],
      timeouts: DLCTimeouts,
      feeRate: SatoshisPerVirtualByte,
      changeSPK: ScriptPubKey,
      remoteChangeSPK: ScriptPubKey,
      network: BitcoinNetwork)(implicit ec: ExecutionContext): TestDLCClient = {
    val pubKeys = DLCPublicKeys.fromPrivKeys(
      fundingPrivKey,
      payoutPrivKey,
      network
    )

    val remoteOutcomes: ContractInfo =
      outcomes.flip((input + remoteInput).satoshis)

    val changeAddress = BitcoinAddress.fromScriptPubKey(changeSPK, network)
    val remoteChangeAddress =
      BitcoinAddress.fromScriptPubKey(remoteChangeSPK, network)

    val (offerOutcomes,
         offerPubKeys,
         offerInput,
         offerFundingInputs,
         offerChangeAddress,
         acceptPubKeys,
         acceptInput,
         acceptFundingInputs,
         acceptChangeAddress) = if (isInitiator) {
      (outcomes,
       pubKeys,
       input,
       fundingUtxos.map(DLCFundingInput.fromInputSigningInfo(_)),
       changeAddress,
       remotePubKeys,
       remoteInput,
       remoteFundingInputs,
       remoteChangeAddress)
    } else {
      (remoteOutcomes,
       remotePubKeys,
       remoteInput,
       remoteFundingInputs,
       remoteChangeAddress,
       pubKeys,
       input,
       fundingUtxos.map(DLCFundingInput.fromInputSigningInfo(_)),
       changeAddress)
    }

    val offer = DLCMessage.DLCOffer(
      oracleAndContractInfo =
        DLCMessage.OracleAndContractInfo(oracleInfo, offerOutcomes),
      pubKeys = offerPubKeys,
      totalCollateral = offerInput.satoshis,
      fundingInputs = offerFundingInputs,
      changeAddress = offerChangeAddress,
      feeRate = feeRate,
      timeouts = timeouts
    )

    val accept = DLCMessage.DLCAcceptWithoutSigs(
      totalCollateral = acceptInput.satoshis,
      pubKeys = acceptPubKeys,
      fundingInputs = acceptFundingInputs,
      changeAddress = acceptChangeAddress,
      tempContractId = offer.tempContractId
    )

    TestDLCClient(offer,
                  accept,
                  isInitiator,
                  fundingPrivKey,
                  payoutPrivKey,
                  fundingUtxos)
  }
}