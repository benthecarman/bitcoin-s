package org.bitcoins.core.wallet.builder

import org.bitcoins.core.crypto.TxSigComponent
import org.bitcoins.core.protocol.script.{
  NonWitnessScriptPubKey,
  ScriptWitness,
  TaprootScriptPubKey,
  UnassignedWitnessScriptPubKey,
  WitnessScriptPubKeyV0
}
import org.bitcoins.core.protocol.transaction.*
import org.bitcoins.core.wallet.fee.FeeUnit
import org.bitcoins.core.wallet.signer.BitcoinSigner
import org.bitcoins.core.wallet.utxo.{
  InputInfo,
  ScriptSignatureParams,
  UnassignedSegwitNativeInputInfo
}

/** Transactions that have been finalized by a RawTxFinalizer are passed as
  * inputs to a sign function here in order to generate fully signed
  * transactions.
  *
  * In the future sign methods specific to multi-party protocols will be added
  * here to support PSBT and signed transaction construction between multiple
  * parties.
  */
object RawTxSigner {

  val emptyInvariant
      : (Vector[ScriptSignatureParams[InputInfo]], Transaction) => Boolean =
    (_, _) => true

  def feeInvariant(expectedFeeRate: FeeUnit)
      : (Vector[ScriptSignatureParams[InputInfo]], Transaction) => Boolean =
    addFeeRateInvariant(expectedFeeRate, emptyInvariant)

  private def addFeeRateInvariant(
      expectedFeeRate: FeeUnit,
      userInvariants: (
          Vector[ScriptSignatureParams[InputInfo]],
          Transaction) => Boolean)
      : (Vector[ScriptSignatureParams[InputInfo]], Transaction) => Boolean = {
    (utxoInfos, signedTx) =>
      {
        userInvariants(utxoInfos, signedTx) &&
        TxUtil
          .sanityChecks(isSigned = true,
                        inputInfos = utxoInfos.map(_.inputInfo),
                        expectedFeeRate = expectedFeeRate,
                        tx = signedTx)
          .isSuccess
      }
  }

  def sign(
      utx: Transaction,
      utxoInfos: Vector[ScriptSignatureParams[InputInfo]]): Transaction = {
    sign(utx, utxoInfos, emptyInvariant)
  }

  def sign(
      txWithInfo: FinalizedTxWithSigningInfo,
      expectedFeeRate: FeeUnit): Transaction = {
    sign(txWithInfo.finalizedTx, txWithInfo.infos, expectedFeeRate)
  }

  def sign(
      utx: Transaction,
      utxoInfos: Vector[ScriptSignatureParams[InputInfo]],
      expectedFeeRate: FeeUnit): Transaction = {

    val invariants = feeInvariant(expectedFeeRate)

    sign(utx, utxoInfos, invariants)
  }

  def sign(
      utx: Transaction,
      utxoInfos: Vector[ScriptSignatureParams[InputInfo]],
      expectedFeeRate: FeeUnit,
      userInvariants: (
          Vector[ScriptSignatureParams[InputInfo]],
          Transaction) => Boolean): Transaction = {

    val invariants = addFeeRateInvariant(expectedFeeRate, userInvariants)

    sign(utx, utxoInfos, invariants)
  }

  def sign(
      txWithInfo: FinalizedTxWithSigningInfo,
      expectedFeeRate: FeeUnit,
      userInvariants: (
          Vector[ScriptSignatureParams[InputInfo]],
          Transaction) => Boolean): Transaction = {

    val invariants = addFeeRateInvariant(expectedFeeRate, userInvariants)

    sign(txWithInfo.finalizedTx, txWithInfo.infos, invariants)
  }

  def sign(
      utx: Transaction,
      utxoInfos: Vector[ScriptSignatureParams[InputInfo]],
      invariants: (
          Vector[ScriptSignatureParams[InputInfo]],
          Transaction) => Boolean): Transaction = {
    require(
      utxoInfos.length == utx.inputs.length,
      s"Must provide exactly one UTXOSatisfyingInfo per input, ${utxoInfos.length} != ${utx.inputs.length}")
    require(utxoInfos.distinct.length == utxoInfos.length,
            "All UTXOSatisfyingInfos must be unique. ")
    val utxOutPoints = utx.inputs.map(_.previousOutput)
    val sortedUtxoInfos = utxoInfos.map { u =>
      u.output.scriptPubKey match {
        case _: NonWitnessScriptPubKey | _: WitnessScriptPubKeyV0 |
            _: UnassignedWitnessScriptPubKey =>
          // no sorting needed for these spk types as the sighash algorithm
          // doesn't include all outputs
          u
        case _: TaprootScriptPubKey =>
          u.copy(inputInfo = u.inputInfo.sortPreviousOutputMap(utxOutPoints))
      }
    }

    val signedTx =
      if (
        utxoInfos.exists(
          _.inputInfo.isInstanceOf[UnassignedSegwitNativeInputInfo])
      ) {
        throw TxBuilderError.NoSigner.exception
      } else {
        val builder = RawTxBuilder()
          .setVersion(utx.version)
          .setLockTime(utx.lockTime)
          .++=(utx.outputs)

        val inputsAndWitnesses = sortedUtxoInfos.map { utxo =>
          val txSigComp =
            BitcoinSigner.sign(utxo, utx)
          val scriptWitnessOpt = TxSigComponent.getScriptWitness(txSigComp)

          (txSigComp.input, scriptWitnessOpt)
        }

        val witnessesBuilder = Vector.newBuilder[Option[ScriptWitness]]

        utx.inputs.foreach { unsignedInput =>
          val (input, witnessOpt) = inputsAndWitnesses
            .find(_._1.previousOutput == unsignedInput.previousOutput)
            .get

          witnessesBuilder += witnessOpt
          builder += input
        }

        val btx = builder.setFinalizer(RawFinalizer).buildTx()

        val txWitness =
          TransactionWitness.fromWitOpt(witnessesBuilder.result())

        txWitness match {
          case EmptyWitness(_) => btx
          case _: TransactionWitness =>
            WitnessTransaction(btx.version,
                               btx.inputs,
                               btx.outputs,
                               btx.lockTime,
                               txWitness)
        }
      }

    if (invariants(utxoInfos, signedTx)) {
      signedTx
    } else {
      throw TxBuilderError.FailedUserInvariants.exception
    }
  }
}
