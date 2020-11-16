package org.bitcoins.dlc.testgen

import org.bitcoins.commons.jsonmodels.dlc.DLCMessage.{
  DLCSign,
  SingleNonceContractInfo,
  SingleNonceOracleInfo
}
import org.bitcoins.core.currency.{CurrencyUnit, Satoshis}
import org.bitcoins.core.number.UInt32
import org.bitcoins.core.protocol.script._
import org.bitcoins.core.protocol.tlv.EnumOutcome
import org.bitcoins.core.protocol.transaction._
import org.bitcoins.core.protocol.{BitcoinAddress, BlockTimeStamp}
import org.bitcoins.core.wallet.fee.SatoshisPerVirtualByte
import org.bitcoins.crypto.{CryptoUtil, ECPrivateKey, ECPublicKey}
import org.bitcoins.dlc.sign.DLCTxSigner
import scodec.bits.ByteVector

import scala.concurrent.{ExecutionContext, Future}

object DLCTxGen {
  import DLCTLVGen._

  def dlcParams(
      contractInfo: SingleNonceContractInfo = genContractInfo(),
      contractMaturityBound: BlockTimeStamp = BlockTimeStamp(100),
      contractTimeout: BlockTimeStamp = BlockTimeStamp(200),
      feeRate: SatoshisPerVirtualByte =
        SatoshisPerVirtualByte(Satoshis(5))): DLCParams = {
    val privKey = ECPrivateKey.freshPrivateKey
    val kVal = ECPrivateKey.freshPrivateKey
    val oracleInfo =
      SingleNonceOracleInfo(privKey.schnorrPublicKey, kVal.schnorrNonce)
    val realOutcome = contractInfo.keys(contractInfo.size / 2)
    val sig =
      privKey.schnorrSignWithNonce(CryptoUtil.sha256(realOutcome.outcome).bytes,
                                   kVal)
    DLCParams(
      oracleInfo,
      SerializedContractInfoEntry.fromContractInfo(contractInfo),
      contractMaturityBound,
      contractTimeout,
      feeRate,
      CryptoUtil.sha256(realOutcome.outcome),
      sig
    )
  }

  private val dummyTransactionInput = TransactionInput(
    TransactionOutPoint(CryptoUtil.doubleSHA256(ByteVector("DLC".getBytes)),
                        UInt32.zero),
    EmptyScriptSignature,
    UInt32.zero)

  def fundingInputTx(
      inputs: Vector[TransactionInput] = Vector(dummyTransactionInput),
      idx: Int = 0,
      privKeys: Vector[ECPrivateKey] = Vector(ECPrivateKey.freshPrivateKey),
      redeemScriptOpt: Option[WitnessScriptPubKeyV0] = None,
      scriptWitness: ScriptWitnessV0 = P2WPKHWitnessV0(
        ECPublicKey.freshPublicKey),
      amt: CurrencyUnit = defaultAmt * 2,
      lockTime: UInt32 = UInt32.zero): FundingInputTx = {
    val (spk, scriptWit) = redeemScriptOpt match {
      case Some(wspk) => (P2SHScriptPubKey(wspk), scriptWitness)
      case None =>
        scriptWitness match {
          case p2wpkh: P2WPKHWitnessV0 =>
            val pubKey = if (privKeys.head.publicKey != p2wpkh.pubKey) {
              privKeys.head.publicKey
            } else {
              p2wpkh.pubKey
            }
            (P2WPKHWitnessSPKV0(pubKey), P2WPKHWitnessV0(pubKey))
          case p2wsh: P2WSHWitnessV0 =>
            (P2WSHWitnessSPKV0(p2wsh.redeemScript), p2wsh)
        }
    }

    val outputs =
      Vector
        .fill(idx)(TransactionOutput(defaultAmt, EmptyScriptPubKey)) :+
        TransactionOutput(amt, spk)
    val tx = BaseTransaction(TransactionConstants.validLockVersion,
                             inputs,
                             outputs,
                             lockTime)

    FundingInputTx(tx, idx, privKeys, redeemScriptOpt, scriptWit)
  }

  def multiSigFundingInputTx(
      privKeys: Vector[ECPrivateKey] =
        Vector(ECPrivateKey.freshPrivateKey, ECPrivateKey.freshPrivateKey),
      requiredSigs: Int = 2,
      p2shNested: Boolean = false,
      idx: Int = 0,
      amt: CurrencyUnit = defaultAmt * 2,
      lockTime: UInt32 = UInt32.zero): FundingInputTx = {
    val multiSig =
      MultiSignatureScriptPubKey(requiredSigs, privKeys.map(_.publicKey))

    val redeemScriptOpt = if (p2shNested) {
      Some(P2WSHWitnessSPKV0(multiSig))
    } else None

    val scriptWitness = P2WSHWitnessV0(multiSig)

    fundingInputTx(idx = idx,
                   privKeys = privKeys,
                   redeemScriptOpt = redeemScriptOpt,
                   scriptWitness = scriptWitness,
                   amt = amt,
                   lockTime = lockTime)
  }

  def dlcPartyParams(
      collateral: CurrencyUnit = defaultAmt,
      fundingInputTxs: Vector[FundingInputTx] = Vector(fundingInputTx()),
      changeAddress: BitcoinAddress = address(),
      fundingPrivKey: ECPrivateKey = ECPrivateKey.freshPrivateKey,
      payoutAddress: BitcoinAddress = address()): DLCPartyParams = {
    DLCPartyParams(collateral,
                   fundingInputTxs,
                   changeAddress,
                   fundingPrivKey,
                   payoutAddress)
  }

  def validTestInputs(
      params: DLCParams = dlcParams(),
      offerParams: DLCPartyParams = dlcPartyParams(),
      acceptParams: DLCPartyParams = dlcPartyParams()): ValidTestInputs = {
    ValidTestInputs(params, offerParams, acceptParams)
  }

  def validTestInputsForInputs(
      offerInputs: Vector[FundingInputTx],
      acceptInputs: Vector[FundingInputTx],
      numOutcomes: Int = 3): ValidTestInputs = {
    val outcomes = DLCTestUtil.genOutcomes(numOutcomes)
    val contractInfo = genContractInfo(outcomes)

    validTestInputs(
      params = dlcParams(contractInfo = contractInfo),
      offerParams = dlcPartyParams(fundingInputTxs = offerInputs),
      acceptParams = dlcPartyParams(fundingInputTxs = acceptInputs)
    )
  }

  def vecProd[T](vec1: Vector[T], vec2: Vector[T]): Vector[(T, T)] = {
    vec1.flatMap(x => vec2.map((x, _)))
  }

  val allInputs = Vector(0, 1, 2)

  def inputFromKind(n: Int): FundingInputTx = {
    if (n == 0) fundingInputTx()
    else if (n == 1) multiSigFundingInputTx()
    else multiSigFundingInputTx(p2shNested = true)
  }

  def inputs(n: Int): Vector[FundingInputTx] = {
    (0 until n).toVector.map { _ =>
      inputFromKind(scala.util.Random.nextInt(3))
    }
  }

  def nonP2WPKHInputs: Vector[ValidTestInputs] = {
    vecProd(allInputs, allInputs).tail.map {
      case (offerInputKind, acceptInputKind) =>
        validTestInputsForInputs(
          offerInputs = Vector(inputFromKind(offerInputKind)),
          acceptInputs = Vector(inputFromKind(acceptInputKind))
        )
    }
  }

  def multiInputTests(numInputOptions: Vector[Int]): Vector[ValidTestInputs] = {
    vecProd(numInputOptions, numInputOptions).tail.map {
      case (offerNumInputs, acceptNumInputs) =>
        validTestInputsForInputs(
          offerInputs = inputs(offerNumInputs),
          acceptInputs = inputs(acceptNumInputs)
        )
    }
  }

  def dlcTxTestVector(inputs: ValidTestInputs = validTestInputs())(implicit
      ec: ExecutionContext): Future[DLCTxTestVector] = {
    DLCTxTestVector.fromInputs(inputs)
  }

  def dlcTxTestVectorWithTxInputs(
      offerInputs: Vector[FundingInputTx],
      acceptInputs: Vector[FundingInputTx],
      numOutcomes: Int = 3)(implicit
      ec: ExecutionContext): Future[DLCTxTestVector] = {
    dlcTxTestVector(
      validTestInputsForInputs(offerInputs, acceptInputs, numOutcomes))
  }

  def randomTxTestVector(numOutcomes: Int)(implicit
      ec: ExecutionContext): Future[DLCTxTestVector] = {
    val outcomes = DLCTestUtil.genOutcomes(numOutcomes)
    val contractInfo = genContractInfo(outcomes)

    dlcTxTestVector(validTestInputs(dlcParams(contractInfo = contractInfo)))
  }

  def successTestVector(inputs: ValidTestInputs = validTestInputs())(implicit
      ec: ExecutionContext): Future[SuccessTestVector] = {
    val offer = inputs.offer
    val acceptWithoutSigs = inputs.accept

    val builder = inputs.builder
    val offerSigner = DLCTxSigner(builder,
                                  isInitiator = true,
                                  inputs.offerParams.fundingPrivKey,
                                  inputs.offerParams.payoutAddress,
                                  inputs.offerParams.fundingScriptSigParams)
    val acceptSigner = DLCTxSigner(builder,
                                   isInitiator = false,
                                   inputs.acceptParams.fundingPrivKey,
                                   inputs.acceptParams.payoutAddress,
                                   inputs.acceptParams.fundingScriptSigParams)

    val outcomeStr = inputs.params.contractInfo
      .find(_.outcome == inputs.params.realOutcome)
      .map(_.preImage)
      .get
    val outcome = EnumOutcome(outcomeStr)

    for {
      accpetCETSigs <- acceptSigner.createCETSigs()
      offerCETSigs <- offerSigner.createCETSigs()
      offerFundingSigs <- offerSigner.createFundingTxSigs()

      DLCTransactions(fundingTx, cets, refundTx) <- inputs.buildTransactions

      signedFundingTx <- acceptSigner.signFundingTx(offerFundingSigs)
      signedRefundTx <- offerSigner.signRefundTx(accpetCETSigs.refundSig)
      offerSignedCET <- offerSigner.signCET(
        outcome,
        accpetCETSigs(outcome),
        Vector(inputs.params.oracleSignature))
      acceptSignedCET <- acceptSigner.signCET(
        outcome,
        offerCETSigs(outcome),
        Vector(inputs.params.oracleSignature))
    } yield {
      val accept = acceptWithoutSigs.withSigs(accpetCETSigs)

      val contractId = fundingTx.txIdBE.bytes.xor(accept.tempContractId.bytes)
      val sign = DLCSign(offerCETSigs, offerFundingSigs, contractId)

      SuccessTestVector(
        inputs,
        offer.toMessage,
        accept.toMessage,
        sign.toMessage,
        DLCTransactions(fundingTx, cets, refundTx),
        DLCTransactions(signedFundingTx,
                        Vector(offerSignedCET, acceptSignedCET),
                        signedRefundTx)
      )
    }
  }

  def randomSuccessTestVector(numOutcomes: Int)(implicit
      ec: ExecutionContext): Future[SuccessTestVector] = {
    val outcomes = DLCTestUtil.genOutcomes(numOutcomes)
    val contractInfo = genContractInfo(outcomes)

    successTestVector(validTestInputs(dlcParams(contractInfo = contractInfo)))
  }
}